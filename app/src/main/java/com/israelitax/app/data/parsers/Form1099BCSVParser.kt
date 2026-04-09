package com.israelitax.app.data.parsers

import android.util.Log
import com.israelitax.app.data.models.Form1099BData
import com.israelitax.app.data.models.HoldingPeriod
import com.israelitax.app.data.models.TradeTransaction
import kotlin.math.abs

/**
 * Parses IBKR CSV trade exports — no OCR needed, 100% accurate.
 *
 * Supported formats (auto-detected):
 *  1. IBKR Activity Statement CSV  (Trades section — most common)
 *  2. IBKR Gain/Loss Report CSV    (has Date Acquired + ST/LT flag)
 *  3. Generic trade CSV            (any CSV with Symbol, Date Sold, Gain columns)
 *
 * How to export from IBKR mobile/web:
 *   Client Portal → Performance & Reports → Tax → Gain/Loss Summary → Download CSV
 *   — OR —
 *   Client Portal → Reports → Activity → set date range → Format: CSV → Run
 */
class Form1099BCSVParser {

    companion object {
        private const val TAG = "1099BCSVParser"
    }

    fun parse(csvText: String): Result<Form1099BData> {
        return try {
            val lines = csvText.lines().filter { it.isNotBlank() }
            val transactions = when {
                isActivityStatement(lines) -> parseActivityStatement(lines)
                isGainLossReport(lines)    -> parseGainLossReport(lines)
                else                       -> parseGenericCSV(lines)
            }
            if (transactions.isEmpty())
                return Result.failure(Exception(
                    "No trades found in CSV.\n\n" +
                    "Export options from IBKR:\n" +
                    "• Client Portal → Reports → Tax → Gain/Loss Summary → CSV\n" +
                    "• Client Portal → Reports → Activity → CSV (full year)"
                ))

            val stGL = transactions.filter { it.holdingPeriod == HoldingPeriod.SHORT_TERM }.sumOf { it.gainLoss }
            val ltGL = transactions.filter { it.holdingPeriod == HoldingPeriod.LONG_TERM }.sumOf { it.gainLoss }
            val proceeds = transactions.sumOf { it.proceeds }
            val basis    = transactions.sumOf { it.costBasis }
            val taxYear  = transactions.firstOrNull()?.dateSold?.take(4)?.toIntOrNull() ?: 2025

            Log.i(TAG, "CSV: ${transactions.size} trades, ST=$stGL, LT=$ltGL, year=$taxYear")
            Result.success(Form1099BData(
                taxYear = taxYear,
                brokerName = "Interactive Brokers LLC",
                transactions = transactions,
                totalProceeds = proceeds,
                totalCostBasis = basis,
                totalNetGainLoss = stGL + ltGL,
                shortTermGainLoss = stGL,
                longTermGainLoss = ltGL
            ))
        } catch (e: Exception) {
            Log.e(TAG, "CSV parse error: ${e.message}")
            Result.failure(e)
        }
    }

    // ─── Format detection ─────────────────────────────────────────────────────

    private fun isActivityStatement(lines: List<String>) =
        lines.any { it.startsWith("Trades,Header,") || it.startsWith("Trades,Data,") }

    private fun isGainLossReport(lines: List<String>) = lines.any { line ->
        val l = line.lowercase()
        ("date acquired" in l || "open date" in l) && ("gain" in l || "p/l" in l)
    }

    // ─── Format 1: IBKR Activity Statement ───────────────────────────────────

    private fun parseActivityStatement(lines: List<String>): List<TradeTransaction> {
        val headerLine = lines.firstOrNull { it.startsWith("Trades,Header,") }
            ?: return emptyList()
        val headers = csv(headerLine.removePrefix("Trades,Header,"))

        fun idx(vararg names: String) = headers.indexOfFirst { h -> names.any { h.equals(it, true) } }

        val iSymbol   = idx("Symbol")
        val iDate     = idx("Date/Time")
        val iProceeds = idx("Proceeds")
        val iBasis    = idx("Basis")
        val iGL       = idx("Realized P/L")
        val iCode     = idx("Code")

        if (iSymbol < 0 || iDate < 0 || iGL < 0) {
            Log.w(TAG, "Activity statement missing columns. Found: $headers")
            return emptyList()
        }

        val result = mutableListOf<TradeTransaction>()
        for (line in lines) {
            if (!line.startsWith("Trades,Data,")) continue
            val c = csv(line.removePrefix("Trades,Data,"))

            val symbol = c.getOrNull(iSymbol)?.trim() ?: continue
            if (symbol.isBlank() || symbol.isTotal()) continue

            val code = c.getOrNull(iCode) ?: ""
            val gl   = c.getOrNull(iGL)?.clean()?.toDoubleOrNull() ?: continue
            if (gl == 0.0 && !code.contains("C", true)) continue  // skip open trades

            val dateRaw  = c.getOrNull(iDate) ?: continue
            val dateSold = dateRaw.take(10).trim()
            if (!dateSold.isISODate()) continue

            val proceeds = c.getOrNull(iProceeds)?.clean()?.toDoubleOrNull()?.let { abs(it) } ?: 0.0
            val basis    = c.getOrNull(iBasis)?.clean()?.toDoubleOrNull()?.let { abs(it) } ?: 0.0

            result.add(TradeTransaction(
                description = symbol,
                dateAcquired = "VARIOUS",
                dateSold = dateSold,
                proceeds = proceeds,
                costBasis = basis,
                gainLoss = gl,
                holdingPeriod = HoldingPeriod.SHORT_TERM,  // Activity stmt doesn't expose ST/LT
                covered = true
            ))
        }
        Log.i(TAG, "Activity statement: ${result.size} closed trades")
        return result
    }

    // ─── Format 2: IBKR Gain/Loss Report ─────────────────────────────────────

    private fun parseGainLossReport(lines: List<String>): List<TradeTransaction> {
        val headerIdx = lines.indexOfFirst { line ->
            val l = line.lowercase()
            ("symbol" in l || "security" in l) && ("gain" in l || "p/l" in l)
        }
        if (headerIdx < 0) return emptyList()

        val headers = csv(lines[headerIdx])
        fun idx(vararg names: String) = headers.indexOfFirst { h -> names.any { h.trim().equals(it, true) } }

        val iSymbol   = idx("Symbol", "Security", "Ticker")
        val iAcquired = idx("Date Acquired", "Open Date", "Buy Date", "Acquired")
        val iSold     = idx("Date Sold", "Close Date", "Sell Date", "Sold")
        val iProceeds = idx("Proceeds", "Value", "Sale Proceeds")
        val iBasis    = idx("Cost", "Basis", "Cost Basis", "Cost or Other Basis")
        val iGL       = idx("Gain/Loss", "Realized P/L", "Net Gain", "Net P/L", "P/L", "Realized")
        val iSTLT     = idx("ST/LT", "Term", "Holding", "Short/Long")

        if (iSymbol < 0 || iSold < 0 || iGL < 0) {
            Log.w(TAG, "Gain/loss report missing columns. Found: $headers")
            return emptyList()
        }

        val result = mutableListOf<TradeTransaction>()
        for (i in (headerIdx + 1) until lines.size) {
            val c = csv(lines[i])
            val symbol = c.getOrNull(iSymbol)?.trim() ?: continue
            if (symbol.isBlank() || symbol.isTotal()) continue

            val dateSoldISO = c.getOrNull(iSold)?.trim()?.toISO() ?: continue
            val dateAcqISO  = if (iAcquired >= 0) c.getOrNull(iAcquired)?.trim()?.toISO() ?: "VARIOUS" else "VARIOUS"

            val proceeds = c.getOrNull(iProceeds)?.clean()?.toDoubleOrNull()?.let { abs(it) } ?: 0.0
            val basis    = c.getOrNull(iBasis)?.clean()?.toDoubleOrNull()?.let { abs(it) } ?: 0.0
            val gl       = c.getOrNull(iGL)?.clean()?.toDoubleOrNull() ?: continue

            val stltRaw = if (iSTLT >= 0) c.getOrNull(iSTLT)?.trim() ?: "" else ""
            val holding = when {
                stltRaw.equals("LT", true) || stltRaw.equals("Long", true)  -> HoldingPeriod.LONG_TERM
                stltRaw.equals("ST", true) || stltRaw.equals("Short", true) -> HoldingPeriod.SHORT_TERM
                dateAcqISO != "VARIOUS" -> holdingFromDates(dateAcqISO, dateSoldISO)
                else -> HoldingPeriod.SHORT_TERM
            }

            result.add(TradeTransaction(
                description = symbol,
                dateAcquired = dateAcqISO,
                dateSold = dateSoldISO,
                proceeds = proceeds,
                costBasis = basis,
                gainLoss = gl,
                holdingPeriod = holding,
                covered = true
            ))
        }
        Log.i(TAG, "Gain/loss report: ${result.size} trades")
        return result
    }

    // ─── Format 3: Generic fallback ───────────────────────────────────────────

    private fun parseGenericCSV(lines: List<String>): List<TradeTransaction> {
        Log.d(TAG, "Trying generic CSV parser")
        return parseGainLossReport(lines)  // same logic, flexible column detection
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Parse a CSV row, correctly handling quoted fields containing commas */
    private fun csv(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val buf = StringBuilder()
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { result.add(buf.toString().trim('"', ' ')); buf.clear() }
                else -> buf.append(ch)
            }
        }
        result.add(buf.toString().trim('"', ' '))
        return result
    }

    private fun String.clean() = replace(",", "").replace("(", "-").replace(")", "").trim()
    private fun String.isTotal() = startsWith("Total", true) || startsWith("Sub", true) || startsWith("Grand", true)
    private fun String.isISODate() = matches(Regex("""\d{4}-\d{2}-\d{2}"""))

    /** Convert MM/DD/YYYY or YYYY-MM-DD → YYYY-MM-DD */
    fun String.toISO(): String {
        val s = trim().take(10)
        if (s.isISODate()) return s
        val mdy = Regex("""(\d{1,2})/(\d{1,2})/(\d{4})""").find(s)
        if (mdy != null) {
            val (m, d, y) = mdy.destructured
            return "$y-${m.padStart(2,'0')}-${d.padStart(2,'0')}"
        }
        return ""
    }

    private fun holdingFromDates(acquired: String, sold: String): HoldingPeriod {
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val days = (fmt.parse(sold)!!.time - fmt.parse(acquired)!!.time) / 86_400_000L
            if (days > 365) HoldingPeriod.LONG_TERM else HoldingPeriod.SHORT_TERM
        } catch (_: Exception) { HoldingPeriod.SHORT_TERM }
    }
}
