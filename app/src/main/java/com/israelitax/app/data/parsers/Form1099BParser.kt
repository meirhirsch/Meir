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
 * Multi-strategy parser designed to be robust against OCR artifacts:
 *  1. Extracts section-level totals (short-term / long-term) via many label variants
 *  2. Extracts individual transaction rows via flexible regex
 *  3. Falls back gracefully – uses whatever data was found
 *
 * IRS Box references:
 *   1a – Description,  1b – Date Acquired,  1c – Date Sold
 *   1d – Proceeds,     1e – Cost Basis,     1g – Wash Sale Loss Disallowed
 *   4  – Federal Income Tax Withheld
 *   Box A/D = covered ST/LT,  B/E = covered adjusted,  C/F = non-covered
 */
class Form1099BParser {

    companion object {
        private const val TAG = "Form1099BParser"
    }

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun parsePage(bitmap: Bitmap): PageParseResult {
        val text = recognizeText(bitmap)
        Log.d(TAG, "1099-B OCR page (first 800 chars):\n${text.take(800)}")
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
        suspendCancellableCoroutine { cont ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    // ─── Page-level extraction ────────────────────────────────────────────────

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
            totalProceeds = extractAmountByLabels(text,
                "Total Proceeds", "Gross Proceeds", "Total proceeds",
                "Proceeds", "1d"),
            totalCostBasis = extractAmountByLabels(text,
                "Total Cost or Other Basis", "Total cost or other basis",
                "Cost or other basis", "Total Cost Basis", "Cost Basis", "1e"),
            totalShortTermGL = extractShortTermTotal(text),
            totalLongTermGL = extractLongTermTotal(text),
            washSaleLossDisallowed = extractAmountByLabels(text,
                "Wash Sale Loss Disallowed", "Wash sale loss disallowed",
                "Wash Sale", "1g"),
            federalTaxWithheld = extractAmountByLabels(text,
                "Federal Income Tax Withheld", "Federal income tax withheld",
                "Federal Tax Withheld", "Box 4", "4")
        )
        result.transactions.addAll(extractTransactions(text))
        Log.d(TAG, "Page result: ${result.transactions.size} trades, " +
            "ST=${result.totalShortTermGL}, LT=${result.totalLongTermGL}")
        return result
    }

    // ─── Short/Long-term total extraction (most reliable path) ───────────────

    private fun extractShortTermTotal(text: String): Double {
        // Try many IBKR label variants for short-term net gain/loss
        val labels = listOf(
            "Net short-term gain or loss",
            "Net short-term gain (loss)",
            "Net short.term gain",
            "Short-term net gain",
            "Short-term gain or loss",
            "Short-term gain/(loss)",
            "Short term gain",
            "Short term net",
            "Total short-term",
            "Subtotal.*short.term",
            "Box A.*total",
            "Box B.*total",
            "Box C.*total"
        )
        return findLabeledAmount(text, labels) ?: 0.0
    }

    private fun extractLongTermTotal(text: String): Double {
        val labels = listOf(
            "Net long-term gain or loss",
            "Net long-term gain (loss)",
            "Net long.term gain",
            "Long-term net gain",
            "Long-term gain or loss",
            "Long-term gain/(loss)",
            "Long term gain",
            "Long term net",
            "Total long-term",
            "Subtotal.*long.term",
            "Box D.*total",
            "Box E.*total",
            "Box F.*total"
        )
        return findLabeledAmount(text, labels) ?: 0.0
    }

    /**
     * Find a dollar amount that follows any of the given label patterns.
     * Searches both same-line and next-line positions to handle OCR line breaks.
     */
    private fun findLabeledAmount(text: String, labelPatterns: List<String>): Double? {
        for (pattern in labelPatterns) {
            // Same line: label followed by optional punctuation/spaces then a number
            val sameLine = Regex(
                """$pattern[\s:$\(]*([-]?[\d,]+\.?\d*)""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            sameLine.find(text)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()
                ?.let { return it }

            // Next line: label on one line, amount on the next
            val nextLine = Regex(
                """$pattern[^\n]*\n[ \t]*([-]?[\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE
            )
            nextLine.find(text)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()
                ?.let { return it }
        }
        return null
    }

    // ─── Amount extraction by label ───────────────────────────────────────────

    private fun extractAmountByLabels(text: String, vararg labels: String): Double {
        for (label in labels) {
            val regex = Regex(
                """${Regex.escape(label)}[\s:$]*([-]?[\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE
            )
            val v = regex.find(text)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()
            if (v != null) return v

            // Try next line
            val nl = Regex(
                """${Regex.escape(label)}[^\n]*\n[ \t]*([-]?[\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE
            )
            val v2 = nl.find(text)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()
            if (v2 != null) return v2
        }
        return 0.0
    }

    // ─── Individual transaction extraction ───────────────────────────────────

    /**
     * Multi-strategy transaction row extraction.
     *
     * IBKR typical row (tabular, one line):
     *   AAPL  01/10/2025  03/15/2025  18,524.00  16,200.00  0.00  2,324.00
     *
     * Strategy 1: description + 2 dates + 4 amounts (full row)
     * Strategy 2: 2 dates + 3 amounts (no description, gained from context)
     * Strategy 3: key-value labels ("Date Sold: ...", "Proceeds: ...", etc.)
     */
    private fun extractTransactions(text: String): List<TradeTransaction> {
        val transactions = mutableListOf<TradeTransaction>()

        val dateP = """(\d{2}/\d{2}/\d{4}|VARIOUS)"""
        val amtP  = """(-?[\d,]+\.?\d{0,2})"""

        // Strategy 1: Full row - description (2+ chars) + acquired + sold + proceeds + basis + wash + gl
        val fullRow = Regex(
            """([\w][\w\s\.\-\&\/]{1,35}?)\s{2,}$dateP[ \t]+$dateP[ \t]+$amtP[ \t]+$amtP[ \t]+$amtP[ \t]+$amtP""",
            setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
        )
        for (m in fullRow.findAll(text)) {
            parseTransaction(
                description = m.groupValues[1].trim(),
                dateAcquired = m.groupValues[2],
                dateSold = m.groupValues[3],
                proceeds = parseAmt(m.groupValues[4]),
                costBasis = parseAmt(m.groupValues[5]),
                washSaleAdj = parseAmt(m.groupValues[6]),
                gainLoss = parseAmt(m.groupValues[7]),
                holdingHint = null,
                text = text
            )?.let { transactions.add(it) }
        }

        // Strategy 2: Shorter row – acquired + sold + proceeds + basis + gl (no wash col)
        if (transactions.isEmpty()) {
            val shortRow = Regex(
                """([\w][\w\s\.\-\&\/]{1,35}?)\s{2,}$dateP[ \t]+$dateP[ \t]+$amtP[ \t]+$amtP[ \t]+$amtP""",
                setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
            )
            for (m in shortRow.findAll(text)) {
                val gl = parseAmt(m.groupValues[4]) - parseAmt(m.groupValues[5])
                parseTransaction(
                    description = m.groupValues[1].trim(),
                    dateAcquired = m.groupValues[2],
                    dateSold = m.groupValues[3],
                    proceeds = parseAmt(m.groupValues[4]),
                    costBasis = parseAmt(m.groupValues[5]),
                    washSaleAdj = 0.0,
                    gainLoss = gl,
                    holdingHint = null,
                    text = text
                )?.let { transactions.add(it) }
            }
        }

        // Strategy 3: Key-value labeled format (some IBKR PDF renderings)
        if (transactions.isEmpty()) {
            transactions.addAll(extractKeyValueTransactions(text))
        }

        // Deduplicate by (dateSold + proceeds + costBasis)
        val seen = mutableSetOf<String>()
        return transactions.filter { t ->
            val key = "${t.dateSold}|${t.proceeds}|${t.costBasis}"
            seen.add(key)
        }
    }

    private fun parseTransaction(
        description: String,
        dateAcquired: String,
        dateSold: String,
        proceeds: Double,
        costBasis: Double,
        washSaleAdj: Double,
        gainLoss: Double,
        holdingHint: HoldingPeriod?,
        text: String
    ): TradeTransaction? {
        // Filter out header/total rows
        val skip = listOf("TOTAL", "SUBTOTAL", "DESCRIPTION", "SUMMARY", "PROCEEDS", "COST BASIS")
        if (skip.any { description.uppercase().contains(it) }) return null
        if (proceeds == 0.0 && costBasis == 0.0) return null

        val holding = holdingHint ?: inferHoldingPeriod(dateAcquired, dateSold)
        return TradeTransaction(
            description = description.take(60),
            dateAcquired = dateAcquired,
            dateSold = dateSold,
            proceeds = proceeds,
            costBasis = costBasis,
            washSaleAdj = washSaleAdj,
            gainLoss = gainLoss,
            holdingPeriod = holding,
            covered = true,
            exchangeRateOnSaleDate = 0.0
        )
    }

    /**
     * Strategy 3: Parse transactions written as key-value pairs over multiple lines.
     * IBKR sometimes renders PDFs this way for complex portfolios.
     */
    private fun extractKeyValueTransactions(text: String): List<TradeTransaction> {
        val result = mutableListOf<TradeTransaction>()
        // Split on blank lines to get "transaction blocks"
        val blocks = text.split(Regex("""\n\s*\n"""))
        for (block in blocks) {
            val dateSold = extractInlineValue(block, "Date Sold", "Date sold", "Sold") ?: continue
            val dateAcq  = extractInlineValue(block, "Date Acquired", "Date acquired", "Acquired") ?: "VARIOUS"
            val proceeds = extractInlineValue(block, "Proceeds", "Gross Proceeds")
                ?.replace(",", "")?.toDoubleOrNull() ?: continue
            val basis    = extractInlineValue(block, "Cost or Other Basis", "Cost Basis", "Basis")
                ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val wash     = extractInlineValue(block, "Wash Sale", "Wash sale")
                ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val gl       = extractInlineValue(block, "Net Gain", "Gain or Loss", "Gain/Loss")
                ?.replace(",", "")?.toDoubleOrNull() ?: (proceeds - basis + wash)
            val desc     = extractInlineValue(block, "Description", "Security", "Symbol") ?: ""

            result.add(TradeTransaction(
                description = desc.take(60),
                dateAcquired = dateAcq,
                dateSold = dateSold,
                proceeds = proceeds,
                costBasis = basis,
                washSaleAdj = wash,
                gainLoss = gl,
                holdingPeriod = inferHoldingPeriod(dateAcq, dateSold),
                covered = true,
                exchangeRateOnSaleDate = 0.0
            ))
        }
        return result
    }

    private fun extractInlineValue(text: String, vararg labels: String): String? {
        for (label in labels) {
            val r = Regex("""${Regex.escape(label)}\s*[:\-]?\s*(.+)""", RegexOption.IGNORE_CASE)
            r.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    // ─── Merge pages ──────────────────────────────────────────────────────────

    private fun mergeResults(pages: List<PageParseResult>): Form1099BData {
        val allTxns = pages.flatMap { it.transactions }

        // Use the summary page with the highest total proceeds value
        val best = pages.maxByOrNull { maxOf(it.totalProceeds, it.totalShortTermGL.coerceAtLeast(0.0)) }

        // Prefer transaction-derived totals; fall back to page-level summaries
        val stGL = when {
            allTxns.isNotEmpty() ->
                allTxns.filter { it.holdingPeriod == HoldingPeriod.SHORT_TERM }.sumOf { it.gainLoss }
            else -> pages.firstOrNull { it.totalShortTermGL != 0.0 }?.totalShortTermGL
                ?: best?.totalShortTermGL ?: 0.0
        }
        val ltGL = when {
            allTxns.isNotEmpty() ->
                allTxns.filter { it.holdingPeriod == HoldingPeriod.LONG_TERM }.sumOf { it.gainLoss }
            else -> pages.firstOrNull { it.totalLongTermGL != 0.0 }?.totalLongTermGL
                ?: best?.totalLongTermGL ?: 0.0
        }

        val proceeds = allTxns.sumOf { it.proceeds }
            .takeIf { it > 0 } ?: best?.totalProceeds ?: 0.0
        val basis = allTxns.sumOf { it.costBasis }
            .takeIf { it > 0 } ?: best?.totalCostBasis ?: 0.0

        Log.i(TAG, "Merged: ${allTxns.size} trades, ST=$stGL, LT=$ltGL, proceeds=$proceeds")

        return Form1099BData(
            taxYear = pages.firstOrNull { it.taxYear > 0 }?.taxYear ?: 2025,
            brokerName = "Interactive Brokers LLC",
            brokerEIN = best?.brokerEIN ?: "",
            taxpayerName = best?.taxpayerName ?: "",
            taxpayerSSN = best?.taxpayerSSN ?: "",
            transactions = allTxns,
            totalProceeds = proceeds,
            totalCostBasis = basis,
            totalNetGainLoss = stGL + ltGL,
            shortTermGainLoss = stGL,
            longTermGainLoss = ltGL,
            washSaleLossDisallowed = best?.washSaleLossDisallowed ?: 0.0,
            federalTaxWithheld = best?.federalTaxWithheld ?: 0.0
        )
    }

    // ─── Helper functions ─────────────────────────────────────────────────────

    private fun extractTaxYear(text: String): Int {
        // "2025 TAX YEAR", "Tax Year 2025", "CONSOLIDATED 1099 2025"
        val r1 = Regex("""(?:Tax\s+Year|TAX\s+YEAR|Consolidated)\s*[:\-]?\s*(20\d{2})""", RegexOption.IGNORE_CASE)
        r1.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        val r2 = Regex("""(20\d{2})\s+(?:TAX|Annual|Consolidated|1099)""", RegexOption.IGNORE_CASE)
        r2.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        // Fallback: most-common 4-digit year in the text
        val years = Regex("""(20[12]\d)""").findAll(text)
            .map { it.groupValues[1].toInt() }
            .groupBy { it }
            .maxByOrNull { it.value.size }?.key
        return years ?: 0
    }

    private fun extractEIN(text: String): String {
        val r = Regex("""(?:EIN|Employer\s+ID|Federal\s+ID)[:\s]+(\d{2}-\d{7})""", RegexOption.IGNORE_CASE)
        return r.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun extractTaxpayerName(text: String): String {
        val r = Regex("""(?:Recipient|Payee|Account\s+Name)[:\s]+([A-Z][A-Z\s,\.]{2,40})""", RegexOption.IGNORE_CASE)
        return r.find(text)?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun extractSSN(text: String): String {
        val r = Regex("""(?:SSN|TIN|Taxpayer\s+ID)[:\s]+([\dX\*]{3}-[\dX\*]{2}-\d{4})""", RegexOption.IGNORE_CASE)
        return r.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun parseAmt(raw: String): Double =
        raw.trim().replace(",", "").replace("(", "-").replace(")", "").toDoubleOrNull() ?: 0.0

    /** Converts MM/DD/YYYY → YYYY-MM-DD for Bank of Israel API */
    fun convertDateFormat(mmddyyyy: String): String {
        if (mmddyyyy == "VARIOUS" || mmddyyyy.length < 10) return mmddyyyy
        return try {
            val p = mmddyyyy.split("/")
            "${p[2]}-${p[0]}-${p[1]}"
        } catch (e: Exception) { mmddyyyy }
    }

    private fun inferHoldingPeriod(acquired: String, sold: String): HoldingPeriod {
        if (acquired == "VARIOUS") return HoldingPeriod.SHORT_TERM
        return try {
            val fmt = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
            val acq  = fmt.parse(acquired) ?: return HoldingPeriod.SHORT_TERM
            val sld  = fmt.parse(sold)     ?: return HoldingPeriod.SHORT_TERM
            if ((sld.time - acq.time) / 86_400_000L > 365) HoldingPeriod.LONG_TERM
            else HoldingPeriod.SHORT_TERM
        } catch (e: Exception) { HoldingPeriod.SHORT_TERM }
    }
}
