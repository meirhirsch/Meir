package com.israelitax.app.tax.israel

import com.israelitax.app.data.models.Form106Data
import com.israelitax.app.data.models.Form1099BData
import com.israelitax.app.data.models.HoldingPeriod
import com.israelitax.app.data.models.IsraeliTaxResult
import com.israelitax.app.data.models.TradeTransaction
import com.israelitax.app.data.parsers.Form1099BParser
import com.israelitax.app.data.repository.ExchangeRateRepository
import kotlin.math.max
import kotlin.math.min

/**
 * Israeli Income Tax Calculator – Tax Year 2025
 *
 * Handles dual-income Israeli residents:
 *  1. Salary income (from Form 106)
 *  2. Foreign capital gains from IBKR day trading (from 1099-B)
 *
 * Legal basis:
 *  - Income Tax Ordinance [New Version] 5721-1961 (פקודת מס הכנסה)
 *  - Section 91 – Capital Gains Tax (מס רווחי הון)
 *  - Section 14 – Worldwide income for Israeli residents
 *  - Israel-US Tax Treaty (1975, updated 1994) – Article 22 (Relief from Double Taxation)
 *  - Tax brackets per Israeli Tax Authority publication (רשות המיסים, 2025)
 *
 * Currency: All amounts in NIS. USD amounts converted using Bank of Israel
 * exchange rate on the date of each transaction (Section 207A of the Ordinance).
 */
class IsraeliTaxCalculator(
    private val exchangeRateRepo: ExchangeRateRepository
) {
    companion object {
        // ─── 2025 Israeli Tax Brackets (NIS per year) ───────────────────────
        // Source: רשות המסים בישראל, מדרגות מס הכנסה לשנת 2025
        // Adjusted ~3.3% from 2024 per annual CPI indexation
        val INCOME_BRACKETS_2025 = listOf(
            TaxBracket(0.0,       84_120.0,  0.10),   // 10%
            TaxBracket(84_120.0,  120_720.0, 0.14),   // 14%
            TaxBracket(120_720.0, 193_800.0, 0.20),   // 20%
            TaxBracket(193_800.0, 269_280.0, 0.31),   // 31%
            TaxBracket(269_280.0, 560_280.0, 0.35),   // 35%
            TaxBracket(560_280.0, 721_560.0, 0.47),   // 47%
            TaxBracket(721_560.0, Double.MAX_VALUE, 0.50) // 50%
        )

        // ─── Capital Gains Tax Rates ─────────────────────────────────────────
        // Section 91(b) – Foreign securities (IBKR): 25% flat rate
        const val CAPITAL_GAINS_RATE_FOREIGN = 0.25

        // ─── Tax Credit Point Values (נקודת זיכוי) ──────────────────────────
        // 2025: each credit point = NIS 2,994/year (indexed from 2,904 in 2024)
        const val CREDIT_POINT_VALUE_2025 = 2_994.0
        const val DEFAULT_CREDIT_POINTS = 2.25   // single Israeli resident

        // ─── Bituach Leumi (National Insurance) Rates 2025 ──────────────────
        // Lower threshold (60% avg wage): ~NIS 7,750/month → NIS 93,000/year
        // Upper ceiling: ~NIS 50,600/month → NIS 607,200/year
        const val BL_LOWER_CEILING_2025 = 93_000.0
        const val BL_UPPER_CEILING_2025 = 607_200.0
        const val BL_RATE_LOWER   = 0.035   // 3.5%
        const val BL_RATE_UPPER   = 0.12    // 12%
        const val HEALTH_RATE_LOWER = 0.031 // 3.1%
        const val HEALTH_RATE_UPPER = 0.05  // 5%
    }

    /**
     * Main calculation entry point.
     *
     * @param form106 Parsed Israeli salary statement
     * @param form1099B IBKR trading report (individual transactions enriched per-date below)
     * @param averageRateUsdNis BOI annual average — used ONLY as fallback if a specific
     *        trade date has no BOI rate available (e.g. network failure). Primary path
     *        always uses the exact per-date BOI rate (Section 207A of the Ordinance).
     * @param usTaxPaidOnIsraeliIncome US tax paid on income also taxed in Israel (for FTC)
     */
    suspend fun calculate(
        form106: Form106Data,
        form1099B: Form1099BData,
        averageRateUsdNis: Double,
        usTaxPaidOnIsraeliIncome: Double = 0.0
    ): IsraeliTaxResult {

        // Step 1: Enrich each trade with the BOI exchange rate on its sale date.
        // The repository walks back up to 7 days for weekends/holidays.
        val enriched = enrichWithExchangeRates(form1099B.transactions)

        // Step 2: Compute NIS capital gains.
        // Uses exact date rate where available; falls back to averageRateUsdNis × USD gain
        // only for trades where BOI lookup failed (network error / no historical data).
        val (capGainLTnis, capGainSTnis) = computeForeignCapGains(enriched, averageRateUsdNis)

        // Step 3: Salary income (already in NIS from Form 106)
        val salaryIncome = form106.grossIncome

        // Step 4: Total worldwide income
        val totalIncome = salaryIncome + capGainLTnis + capGainSTnis

        // Step 5: Progressive tax on salary only (capital gains taxed separately at flat rate)
        val salaryTax = computeProgressiveTax(salaryIncome, INCOME_BRACKETS_2025)

        // Step 6: Capital gains tax – 25% flat on foreign securities (Section 91)
        val cgTaxLT = max(0.0, capGainLTnis) * CAPITAL_GAINS_RATE_FOREIGN
        val cgTaxST = max(0.0, capGainSTnis) * CAPITAL_GAINS_RATE_FOREIGN
        val totalCgTax = cgTaxLT + cgTaxST

        // Step 7: Tax credit points (נקודות זיכוי) – from Form 106, or default
        val creditPts = form106.taxCreditsPoints.takeIf { it > 0 } ?: DEFAULT_CREDIT_POINTS
        val creditAmt = creditPts * CREDIT_POINT_VALUE_2025

        // Step 8: Salary tax after credits
        val salaryTaxAfterCredits = max(0.0, salaryTax - creditAmt)

        // Step 9: Total tax liability
        val totalTaxLiability = salaryTaxAfterCredits + totalCgTax

        // Step 10: Foreign Tax Credit – Article 22, Israel-US Treaty
        // Credit for US tax paid on the same income; limited to Israeli tax on that income
        val foreignTaxCredit = min(usTaxPaidOnIsraeliIncome, salaryTaxAfterCredits)

        // Step 11: Total already paid (Form 106 withholding + treaty credit)
        val totalPaid = form106.incomeTaxWithheld + foreignTaxCredit

        // Step 12: Refund or balance due
        val refundOrOwed = totalPaid - totalTaxLiability

        return IsraeliTaxResult(
            taxYear = form106.taxYear.takeIf { it > 0 } ?: 2025,
            salaryIncome = salaryIncome,
            foreignTradingIncome = capGainLTnis + capGainSTnis,
            totalWorldwideIncome = totalIncome,
            salaryTax = salaryTaxAfterCredits,
            capitalGainsTaxLongTerm = cgTaxLT,
            capitalGainsTaxShortTerm = cgTaxST,
            totalCapGainsTax = totalCgTax,
            taxCreditsPoints = creditPts,
            taxCreditsAmount = creditAmt,
            foreignTaxCreditFromUS = foreignTaxCredit,
            incomeTaxWithheld = form106.incomeTaxWithheld,
            bituachLeumi = form106.bituachLeumiEmployee + form106.healthInsurance,
            totalTaxLiability = totalTaxLiability,
            totalTaxPaid = totalPaid,
            refundOrOwed = refundOrOwed,
            enrichedTransactions = enriched
        )
    }

    /**
     * Fetches the BOI exchange rate for every trade's sale date.
     * Walks back up to 7 days for weekends/holidays.
     */
    suspend fun enrichWithExchangeRates(
        transactions: List<TradeTransaction>
    ): List<TradeTransaction> {
        if (transactions.isEmpty()) return emptyList()

        // Pre-warm the annual rate cache for each tax year present
        transactions.mapNotNull { txn ->
            txn.dateSold.takeLast(4).toIntOrNull()
        }.toSet().forEach { year -> exchangeRateRepo.getRatesForYear(year) }

        val converter = Form1099BParser()
        return transactions.map { txn ->
            val isoDate = converter.convertDateFormat(txn.dateSold)
            val rate = exchangeRateRepo.getRateForDate(isoDate).getOrNull()?.usdToNis ?: 0.0
            txn.copy(
                exchangeRateOnSaleDate = rate,
                proceedsNIS  = txn.proceeds  * rate,
                costBasisNIS = txn.costBasis * rate,
                gainLossNIS  = txn.gainLoss  * rate
            )
        }
    }

    private fun computeForeignCapGains(
        transactions: List<TradeTransaction>,
        fallbackRateUsdNis: Double
    ): Pair<Double, Double> {
        var ltNIS = 0.0; var stNIS = 0.0
        for (txn in transactions) {
            val gainNIS = when {
                // Best path: exact BOI rate on the trade's sale date
                txn.exchangeRateOnSaleDate > 0 -> txn.gainLossNIS
                // Fallback: BOI annual average × USD gain (better than using raw USD as NIS)
                fallbackRateUsdNis > 0 -> txn.gainLoss * fallbackRateUsdNis
                // Last resort: use USD value (will show a warning in the Trades tab)
                else -> txn.gainLoss
            }
            when (txn.holdingPeriod) {
                HoldingPeriod.LONG_TERM  -> ltNIS += gainNIS
                HoldingPeriod.SHORT_TERM -> stNIS += gainNIS
            }
        }
        return Pair(ltNIS, stNIS)
    }

    fun computeProgressiveTax(income: Double, brackets: List<TaxBracket>): Double {
        var tax = 0.0; var remaining = income
        for (b in brackets) {
            if (remaining <= 0) break
            val inBracket = min(remaining, b.upperBound - b.lowerBound)
            tax += inBracket * b.rate
            remaining -= inBracket
        }
        return tax
    }

    fun computeBituachLeumi(grossAnnualSalary: Double): BituachLeumiResult {
        val capped = min(grossAnnualSalary, BL_UPPER_CEILING_2025)
        val bl = if (capped <= BL_LOWER_CEILING_2025) capped * BL_RATE_LOWER
        else BL_LOWER_CEILING_2025 * BL_RATE_LOWER + (capped - BL_LOWER_CEILING_2025) * BL_RATE_UPPER
        val health = if (capped <= BL_LOWER_CEILING_2025) capped * HEALTH_RATE_LOWER
        else BL_LOWER_CEILING_2025 * HEALTH_RATE_LOWER + (capped - BL_LOWER_CEILING_2025) * HEALTH_RATE_UPPER
        return BituachLeumiResult(bl, health, bl + health)
    }

    data class TaxBracket(val lowerBound: Double, val upperBound: Double, val rate: Double)
    data class BituachLeumiResult(
        val nationalInsuranceEmployee: Double,
        val healthInsurance: Double,
        val total: Double
    )
}
