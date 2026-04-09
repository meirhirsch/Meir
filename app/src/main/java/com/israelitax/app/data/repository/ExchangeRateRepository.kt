package com.israelitax.app.data.repository

import android.util.Log
import com.israelitax.app.data.models.ExchangeRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Fetches USD/NIS exchange rates from the Bank of Israel's official API.
 *
 * Bank of Israel API docs:
 *   https://www.boi.org.il/en/economic-roles/financial-markets/exchange-rates/
 *
 * Endpoint used (JSON format):
 *   https://edge.boi.org.il/FusionEdgeServer/sdmx/v2/data/dataflow/
 *   BOI.STATISTICS/EXR/1.0/RER_USD_ILS?startperiod=YYYY-MM-DD&endperiod=YYYY-MM-DD&format=json
 *
 * The Bank of Israel publishes rates on banking days only. For weekends/holidays
 * we use the last available rate before the requested date (standard practice
 * accepted by both IRS and Israeli Tax Authority).
 */
class ExchangeRateRepository {

    companion object {
        private const val TAG = "ExchangeRateRepo"
        private const val BOI_BASE_URL =
            "https://edge.boi.org.il/FusionEdgeServer/sdmx/v2/data/dataflow/" +
            "BOI.STATISTICS/EXR/1.0/RER_USD_ILS"

        // In-memory cache: date string → ExchangeRate
        private val cache = mutableMapOf<String, ExchangeRate>()
    }

    /**
     * Returns the USD→NIS rate for [date] (format: "YYYY-MM-DD").
     *
     * Tries the exact date first. If not a banking day, walks back up to 7 days
     * to find the most recent published rate (same approach used by Israeli CPAs).
     */
    suspend fun getRateForDate(date: String): Result<ExchangeRate> = withContext(Dispatchers.IO) {
        // Check cache first
        cache[date]?.let { return@withContext Result.success(it) }

        // Try exact date, then walk back for non-banking days
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val requestedCal = Calendar.getInstance().apply { time = sdf.parse(date)!! }

        for (daysBack in 0..7) {
            val cal = requestedCal.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -daysBack)
            val queryDate = sdf.format(cal.time)

            val result = fetchFromBOI(queryDate)
            if (result.isSuccess) {
                val rate = result.getOrThrow()
                // Cache both the requested date and the actual returned date
                cache[date] = rate
                cache[queryDate] = rate
                return@withContext Result.success(rate)
            }
        }

        Result.failure(Exception("Could not retrieve exchange rate for $date from Bank of Israel"))
    }

    /**
     * Fetches rates for an entire year – useful to pre-cache all trading dates.
     */
    suspend fun getRatesForYear(year: Int): Result<Map<String, ExchangeRate>> =
        withContext(Dispatchers.IO) {
            try {
                val startPeriod = "$year-01-01"
                val endPeriod = "$year-12-31"
                val url = "$BOI_BASE_URL?startperiod=$startPeriod&endperiod=$endPeriod&format=json"
                val json = httpGet(url)
                val rates = parseBOIJsonResponse(json)

                rates.forEach { (date, rate) -> cache[date] = rate }
                Log.i(TAG, "Pre-cached ${rates.size} BOI rates for year $year")
                Result.success(rates)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch year rates: ${e.message}")
                Result.failure(e)
            }
        }

    private fun fetchFromBOI(date: String): Result<ExchangeRate> {
        return try {
            val url = "$BOI_BASE_URL?startperiod=$date&endperiod=$date&format=json"
            val json = httpGet(url)
            val rates = parseBOIJsonResponse(json)
            val rate = rates[date] ?: return Result.failure(
                Exception("No rate published for $date (possible non-banking day)")
            )
            Result.success(rate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses the Bank of Israel SDMX-JSON v2 response.
     *
     * Response structure (simplified):
     * {
     *   "data": {
     *     "dataSets": [{
     *       "series": {
     *         "0:0:0:0:0": {
     *           "observations": {
     *             "0": [3.7124, 0, null],
     *             "1": [3.7056, 0, null],
     *             ...
     *           }
     *         }
     *       }
     *     }],
     *     "structure": {
     *       "dimensions": {
     *         "observation": [{
     *           "values": [{"id": "2024-01-01"}, ...]
     *         }]
     *       }
     *     }
     *   }
     * }
     */
    private fun parseBOIJsonResponse(json: String): Map<String, ExchangeRate> {
        val result = mutableMapOf<String, ExchangeRate>()
        try {
            val root = JSONObject(json)
            val data = root.getJSONObject("data")

            // Get date dimension values
            val structure = data.getJSONObject("structure")
            val obsDimensions = structure.getJSONObject("dimensions").getJSONArray("observation")
            val timeDimension = obsDimensions.getJSONObject(0)
            val dateValues = timeDimension.getJSONArray("values")

            // Build index → date map
            val indexToDate = mutableMapOf<Int, String>()
            for (i in 0 until dateValues.length()) {
                val entry = dateValues.getJSONObject(i)
                indexToDate[i] = entry.getString("id")
            }

            // Get observations
            val dataSets = data.getJSONArray("dataSets")
            val series = dataSets.getJSONObject(0).getJSONObject("series")
            val seriesKey = series.keys().next() // e.g. "0:0:0:0:0"
            val observations = series.getJSONObject(seriesKey).getJSONObject("observations")

            for (indexStr in observations.keys()) {
                val index = indexStr.toIntOrNull() ?: continue
                val date = indexToDate[index] ?: continue
                val obsArray = observations.getJSONArray(indexStr)
                val rate = obsArray.optDouble(0, 0.0)
                if (rate > 0) {
                    result[date] = ExchangeRate(
                        date = date,
                        usdToNis = rate,
                        source = "Bank of Israel"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: ${e.message}")
        }
        return result
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "IsraelAmericaTaxApp/1.0")
        }
        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP $responseCode from Bank of Israel API")
            }
            return connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Converts an amount in USD to NIS using the rate on the sale date.
     * This is the legally required method for Israeli tax reporting of foreign trades.
     */
    suspend fun convertUsdToNis(amountUsd: Double, saleDate: String): Result<Double> {
        val rateResult = getRateForDate(saleDate)
        return rateResult.map { rate -> amountUsd * rate.usdToNis }
    }
}
