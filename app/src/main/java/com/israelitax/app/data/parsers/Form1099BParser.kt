package com.israelitax.app.data.parsers

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.israelitax.app.data.models.Form1099BData
import com.israelitax.app.data.models.HoldingPeriod
import com.israelitax.app.data.models.TradeTransaction
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Parses IBKR Form 1099-B (Proceeds from Broker and Barter Exchange Transactions).
 *
 * IBKR typically produces a multi-page PDF. Each transaction appears in a table row:
 *   Description | Date Acquired | Date Sold | Proceeds | Cost Basis | Wash Sale Adj | Gain/Loss
 *
 * We parse both:
 *  - Summary totals from the 1099-B cover page
 *  - Individual transaction detail from the activity pages (for per-date exchange rates)
 *
 * IRS Box references:
 *   1a – Description
 *   1b – Date Acquired
 *   1c – Date Sold
 *   1d – Proceeds
 *   1e – Cost Basis
 *   1f – Accrued market discount
 *   1g – Wash Sale Loss Disallowed
 *   4  – Federal Income Tax Withheld
 *   Box categories: A/D = covered short-term, B/E = covered long-term, C/F = non-covered
 */
class Form1099BParser {

    companion object {
        private const val TAG = "Form1099BParser"
    }

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Parse a single page bitmap. Call this for each page of the 1099-B PDF.
     * Merge results from multiple pages with [mergeResults].
     */
    suspend fun parsePage(bitmap: Bitmap): PageParseResult {
        val text = recognizeText(bitmap)
        Log.d(TAG, "1099-B OCR page text:\n${text.take(500)}")
        return extractFromPage(text)
    }

    suspend fun parseAllPages(bitmaps: List<Bitmap>): Result<Form1099BData> {
        return try {
            val pageResults = bitmaps.map { parsePage(it) }
            val merged = mergeResults(pageResults)
            Result.success(merged)
        } catch (e: Exception) {
            Log.e(TAG, "Parse failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun recognizeText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { continuation.resume(it.text) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }

    data class PageParseResult(
        val transactions: MutableList<TradeTransaction> = mutableListOf(),
        val taxYear: Int = 0,
        val brokerEIN: String = "",
        val taxpayerName: String = "",
        val taxpayerSSN: String = "",
        val totalProceeds: Double = 0.0,
        val totalCostBasis: Double = 0.0,
        val totalShortTermGL: Double = 0.0,
        val totalLongTermGL: Double = 0.0,
        val washSaleLossDisallowed: Double = 0.0,
        val federalTaxWithheld: Double = 0.0
    )

    private fun extractFromPage(text: String): PageParseResult {
        val result = PageParseResult(
            taxYear = extractTaxYear(text),
            brokerEIN = extractEIN(text),
            taxpayerName = extractTaxpayerName(text),
            taxpayerSSN = extractSSN(text),
            totalProceeds = extractSummaryField(text, "Total Proceeds", "Proceeds"),
            totalCostBasis = extractSummaryField(text, "Total Cost Basis", "Cost Basis"),
            totalShortTermGL = extractSummaryField(text, "Short-term", "Short Term"),
            totalLongTermGL = extractSummaryField(text, "Long-term", "Long Term"),
            washSaleLossDisallowed = extractSummaryField(text, "Wash Sale Loss Disallowed"),
            federalTaxWithheld = extractSummaryField(text, "Federal Income Tax Withheld", "Box 4")
        )

        result.transactions.addAll(extractTransactions(text))
        return result
    }

    /**
     * Parses individual transaction rows from the detail section of 1099-B.
     *
     * IBKR detail format (each row):
     *   AAPL | 01/15/2024 | 03/22/2024 | 18,524.00 | 16,200.00 | 0.00 | 2,324.00 | L
     * or for short-term:
     *   TSLA  03/01/2024  03/05/2024  5,200.00  4,900.00  0.00  300.00  S
     */
    private fun extractTransactions(text: String): List<TradeTransaction> {
        val transactions = mutableListOf<TradeTransaction>()

        // Match transaction rows: description, acquired date, sold date, amounts
        // Date pattern: MM/DD/YYYY or VARIOUS
        val datePattern = """(\d{2}/\d{2}/\d{4}|VARIOUS)"""
        val amountPattern = """(-?[\d,]+\.?\d*)"""
        val holdingPattern = """([LSls])"""

        // Pattern: description [date] [date] amount amount amount amount [holding]
        val txnRegex = Regex(
            """([A-Z0-9 \.\-\/]+?)\s+$datePattern\s+$datePattern\s+$amountPattern\s+$amountPattern\s+$amountPattern\s+$amountPattern(?:\s+$holdingPattern)?""",
            RegexOption.MULTILINE
        )

        for (match in txnRegex.findAll(text)) {
            try {
                val description = match.groupValues[1].trim()
                val dateAcquired = match.groupValues[2]
                val dateSold = match.groupValues[3]
                val proceeds = parseAmount(match.groupValues[4])
                val costBasis = parseAmount(match.groupValues[5])
                val washSaleAdj = parseAmount(match.groupValues[6])
                val gainLoss = parseAmount(match.groupValues[7])
                val holdingCode = match.groupValues[8].uppercase()

                // Skip header rows or totals
                if (description.contains("TOTAL", ignoreCase = true) ||
                    description.contains("SUBTOTAL", ignoreCase = true)) continue

                val holding = when {
                    holdingCode == "L" -> HoldingPeriod.LONG_TERM
                    holdingCode == "S" -> HoldingPeriod.SHORT_TERM
                    // Infer from dates if not explicit
                    else -> inferHoldingPeriod(dateAcquired, dateSold)
                }

                // Convert date from MM/DD/YYYY to YYYY-MM-DD for BOI API
                val saleDateFormatted = convertDateFormat(dateSold)

                transactions.add(
                    TradeTransaction(
                        description = description,
                        dateAcquired = dateAcquired,
                        dateSold = dateSold,
                        proceeds = proceeds,
                        costBasis = costBasis,
                        washSaleAdj = washSaleAdj,
                        gainLoss = gainLoss,
                        holdingPeriod = holding,
                        covered = true,
                        // Exchange rate fields filled later by ExchangeRateRepository
                        exchangeRateOnSaleDate = 0.0
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Skipped malformed transaction: ${e.message}")
            }
        }

        Log.i(TAG, "Extracted ${transactions.size} transactions from page")
        return transactions
    }

    private fun mergeResults(pages: List<PageParseResult>): Form1099BData {
        val allTransactions = pages.flatMap { it.transactions }

        // Prefer the page with the highest summary totals (the summary page)
        val summaryPage = pages.maxByOrNull { it.totalProceeds }

        // Recompute totals from individual transactions if we have them
        val shortTermGL = if (allTransactions.isNotEmpty()) {
            allTransactions.filter { it.holdingPeriod == HoldingPeriod.SHORT_TERM }
                .sumOf { it.gainLoss }
        } else summaryPage?.totalShortTermGL ?: 0.0

        val longTermGL = if (allTransactions.isNotEmpty()) {
            allTransactions.filter { it.holdingPeriod == HoldingPeriod.LONG_TERM }
                .sumOf { it.gainLoss }
        } else summaryPage?.totalLongTermGL ?: 0.0

        val totalProceeds = allTransactions.sumOf { it.proceeds }
            .takeIf { it > 0 } ?: summaryPage?.totalProceeds ?: 0.0
        val totalCostBasis = allTransactions.sumOf { it.costBasis }
            .takeIf { it > 0 } ?: summaryPage?.totalCostBasis ?: 0.0

        return Form1099BData(
            taxYear = pages.firstOrNull { it.taxYear > 0 }?.taxYear ?: 2024,
            brokerName = "Interactive Brokers LLC",
            brokerEIN = summaryPage?.brokerEIN ?: "",
            taxpayerName = summaryPage?.taxpayerName ?: "",
            taxpayerSSN = summaryPage?.taxpayerSSN ?: "",
            transactions = allTransactions,
            totalProceeds = totalProceeds,
            totalCostBasis = totalCostBasis,
            totalNetGainLoss = shortTermGL + longTermGL,
            shortTermGainLoss = shortTermGL,
            longTermGainLoss = longTermGL,
            washSaleLossDisallowed = summaryPage?.washSaleLossDisallowed ?: 0.0,
            federalTaxWithheld = summaryPage?.federalTaxWithheld ?: 0.0
        )
    }

    // ─── Helper Functions ─────────────────────────────────────────────────────

    private fun extractTaxYear(text: String): Int {
        val regex = Regex("""(?:Tax Year|Year)\s*[:\-]?\s*(20\d{2})""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""(20\d{2})\s+(?:TAX|Annual)""", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: 2024
    }

    private fun extractEIN(text: String): String {
        val regex = Regex("""(?:EIN|Employer ID|Federal ID)[:\s]+(\d{2}-\d{7})""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun extractTaxpayerName(text: String): String {
        val regex = Regex("""(?:Recipient|Payee|Name)[:\s]+([A-Z][A-Z\s,\.]{2,40})""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun extractSSN(text: String): String {
        // Return masked SSN only (XXX-XX-1234 pattern from IBKR)
        val regex = Regex("""(?:SSN|TIN|Taxpayer ID)[:\s]+([\dX\*]{3}-[\dX\*]{2}-\d{4})""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun extractSummaryField(text: String, vararg labels: String): Double {
        for (label in labels) {
            val regex = Regex(
                """${Regex.escape(label)}\s*[:\$]?\s*(-?[\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE
            )
            val value = regex.find(text)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()
            if (value != null) return value
        }
        return 0.0
    }

    private fun parseAmount(raw: String): Double {
        return raw.replace(",", "").replace("(", "-").replace(")", "").toDoubleOrNull() ?: 0.0
    }

    /** Converts MM/DD/YYYY to YYYY-MM-DD for Bank of Israel API queries */
    fun convertDateFormat(mmddyyyy: String): String {
        if (mmddyyyy == "VARIOUS" || mmddyyyy.length < 10) return mmddyyyy
        return try {
            val parts = mmddyyyy.split("/")
            "${parts[2]}-${parts[0]}-${parts[1]}"
        } catch (e: Exception) {
            mmddyyyy
        }
    }

    private fun inferHoldingPeriod(acquired: String, sold: String): HoldingPeriod {
        if (acquired == "VARIOUS") return HoldingPeriod.SHORT_TERM
        return try {
            val fmt = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
            val acqDate = fmt.parse(acquired) ?: return HoldingPeriod.SHORT_TERM
            val soldDate = fmt.parse(sold) ?: return HoldingPeriod.SHORT_TERM
            val diff = soldDate.time - acqDate.time
            val days = diff / (1000 * 60 * 60 * 24)
            if (days > 365) HoldingPeriod.LONG_TERM else HoldingPeriod.SHORT_TERM
        } catch (e: Exception) {
            HoldingPeriod.SHORT_TERM
        }
    }
}
