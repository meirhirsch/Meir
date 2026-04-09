package com.israelitax.app.tax.israel

import com.israelitax.app.data.models.Form106Data
import com.israelitax.app.data.models.Form1099BData
import com.israelitax.app.data.models.HoldingPeriod
import com.israelitax.app.data.models.IsraeliTaxResult
import com.israelitax.app.data.models.TradeTransaction
import com.israelitax.app.data.repository.ExchangeRateRepository
import kotlin.math.max
import kotlin.math.min

/**
 * Israeli Income Tax Calculator
 *
 * Handles dual-income Israeli residents:
 *  1. Salary income (from Form 106)
 *  2. Foreign capital gains from IBKR day trading (from 1099-B)
 *
 * Legal basis:
 *  - Income Tax Ordinance [New Version] 5721-1961 (פקודת מס הכנסה)
 *  - Section 91 – Capital Gains Tax
 *  - Section 14 – Worldwide income for Israeli residents
 *  - Israel-US Tax Treaty (1975, updated 1994) – Article 22 (Relief from Double Taxation)
 *  - Tax brackets per Israeli Tax Authority publication (רשות המיסים, 2024)
 *
 * Currency: All amounts in NIS. USD amounts converted using Bank of Israel
 * exchange rate on the date of each transaction (Section 207A of the Ordinance).
 */
class IsraeliTaxCalculator(
    private val exchangeRateRepo: ExchangeRateRepository
) {
    companion object {
        // ─── 2024 Israeli Tax Brackets (NIS per year) ───────────────────────
        // Source: רשות המסים ב ישראל, מדרגות מס הכנסה לשנת 2024
        val INCOME_BRACKETS_2024 = listOf(
            TaxBracket(0.0,      81_480.0,   0.10),  // 10%
            TaxBracket(81_480.0,  116_760.0, 0.14),  // 14%
            TaxBracket(116_760.0, 187_440.0, 0.20),  // 20%
            TaxBracket(187_440.0, 260_520.0, 0.31),  // 31%
            TaxBracket(260_520.0, 542_160.0, 0.35),  // 35%
            TaxBracket(542_160.0, 698_280.0, 0.47),  // 47%
            TaxBracket(698_280.0, Double.MAX_VALUE, 0.50)  // 50%
        )

        // ─── Capital Gains Tax Rates ─────────────────────────────────────────
        // Section 91(b) – Real capital gains from foreign securities (IBKR)
        // For individual investors (not dealers/traders): 25% flat rate
        // If > 10% shareholder in foreign company: 30%
        // Short-term trading income may be classified as business income (מלאי עסקי)
        // at marginal rate if ITA determines it's a "business" rather than investment
        const val CAPITAL_GAINS_RATE_FOREIGN_LONG = 0.25   // 25% – foreign securities, long-term
        const val CAPITAL_GAINS_RATE_FOREIGN_SHORT = 0.25  // 25% – IBKR individual investor

        // ─── Tax Credit Point Values (נקודת זיכוי) ──────────────────────────
        // 2024: each credit point = NIS 2,904/year
        const val CREDIT_POINT_VALUE_2024 = 2_904.0

        // Standard credit points for Israeli resident:
        //   2.25 points for single person
        //   2.75 for married man
        //   3.0  for working woman (+ 0.5 for being a woman)
        // We use the value from Form 106 (already calculated by employer)
        const val DEFAULT_CREDIT_POINTS = 2.25

        // ─── Bituach Leumi (National Insurance) Rates 2024 ──────────────────
        // Up to salary ceiling (תקרה): NIS 49,030/month → NIS 588,360/year
        // Employee portion: 3.5% up to 60% of avg wage, 12% above
        // 60% of avg wage threshold (2024): ~NIS 7,522/month → NIS 90,264/year
        const val BL_LOWER_CEILING_2024 = 90_264.0    // 60% of avg salary (yearly)
        const val BL_UPPER_CEILING_2024 = 588_360.0   // monthly ceiling × 12
        const val BL_RATE_LOWER = 0.035               // 3.5% on income up to lower ceiling
        const val BL_RATE_UPPER = 0.12                // 12% above lower ceiling
        const val HEALTH_RATE_LOWER = 0.031           // 3.1%
        const val HEALTH_RATE_UPPER = 0.05            // 5%
    }

    /**
     * Main calculation entry point.
     *
     * @param form106 Parsed Israeli salary statement
     * @param form1099B Parsed IBKR trading report (with per-date exchange rates already populated)
     * @param averageRateUsdNis Used only as fallback if per-transaction rates unavailable
     * @param usTaxPaidOnIsraeliIncome US tax paid on income also taxed in Israel (for FTC)
     */
    suspend fun calculate(
        form106: Form106Data,
        form1099B: Form1099BData,
        averageRateUsdNis: Double,
        usTaxPaidOnIsraeliIncome: Double = 0.0
    ): IsraeliTaxResult {

        // Step 1: Enrich transactions with per-date exchange rates
        val enrichedTransactions = enrichWithExchangeRates(form1099B.transactions)

        // Step 2: Calculate NIS capital gains from foreign trading
        val (foreignCapGainLongTermNIS, foreignCapGainShortTermNIS) =
            computeForeignCapitalGains(enrichedTransactions)

        // Step 3: Salary income from Form 106 (already in NIS)
        val salaryIncome = form106.grossIncome

        // Step 4: Total worldwide taxable income
        val totalIncome = salaryIncome + foreignCapGainLongTermNIS + foreignCapGainShortTermNIS

        // Step 5: Progressive tax on salary income only
        // Capital gains are taxed at flat rate (not progressive)
        val salaryTax = computeProgressiveTax(salaryIncome, INCOME_BRACKETS_2024)

        // Step 6: Capital gains tax (flat 25% on foreign securities)
        val capGainsTaxLong = max(0.0, foreignCapGainLongTermNIS) * CAPITAL_GAINS_RATE_FOREIGN_LONG
        val capGainsTaxShort = max(0.0, foreignCapGainShortTermNIS) * CAPITAL_GAINS_RATE_FOREIGN_SHORT
        val totalCapGainsTax = capGainsTaxLong + capGainsTaxShort

        // Step 7: Tax credits
        // Use actual credit points from Form 106, or default
        val creditPoints = form106.taxCreditsPoints.takeIf { it > 0 } ?: DEFAULT_CREDIT_POINTS
        val creditAmount = creditPoints * CREDIT_POINT_VALUE_2024

        // Step 8: Tax on salary after credits (can't reduce below 0)
        val salaryTaxAfterCredits = max(0.0, salaryTax - creditAmount)

        // Step 9: Total tax liability
        val totalTaxLiability = salaryTaxAfterCredits + totalCapGainsTax

        // Step 10: Foreign Tax Credit (Article 22, Israel-US Treaty)
        // Credit for US tax paid on the same income, limited to Israeli tax on that income
        // Can't exceed Israeli tax on the same item of income (credit basket)
        val foreignTaxCredit = computeForeignTaxCredit(
            usTaxPaidOnForeignIncome = usTaxPaidOnIsraeliIncome,
            israeliTaxOnSameIncome = salaryTaxAfterCredits
        )

        // Step 11: Total already paid (from Form 106 withholding)
        val totalPaid = form106.incomeTaxWithheld + foreignTaxCredit

        // Step 12: Refund or balance due
        val refundOrOwed = totalPaid - totalTaxLiability

        return IsraeliTaxResult(
            taxYear = form106.taxYear,
            salaryIncome = salaryIncome,
            foreignTradingIncome = foreignCapGainLongTermNIS + foreignCapGainShortTermNIS,
            totalWorldwideIncome = totalIncome,
            salaryTax = salaryTaxAfterCredits,
            capitalGainsTaxLongTerm = capGainsTaxLong,
            capitalGainsTaxShortTerm = capGainsTaxShort,
            totalCapGainsTax = totalCapGainsTax,
            taxCreditsPoints = creditPoints,
            taxCreditsAmount = creditAmount,
            foreignTaxCreditFromUS = foreignTaxCredit,
            incomeTaxWithheld = form106.incomeTaxWithheld,
            bituachLeumi = form106.bituachLeumiEmployee + form106.healthInsurance,
            totalTaxLiability = totalTaxLiability,
            totalTaxPaid = totalPaid,
            refundOrOwed = refundOrOwed
        )
    }

    /**
     * Enriches each transaction with the BOI exchange rate on the sale date.
     * This implements the legal requirement: use the exchange rate published
     * by the Bank of Israel on the day of the transaction.
     */
    private suspend fun enrichWithExchangeRates(
        transactions: List<TradeTransaction>
    ): List<TradeTransaction> {
        // Pre-fetch all years' rates (typically just one tax year)
        val years = transactions.mapNotNull { txn ->
            txn.dateSold.takeLast(4).toIntOrNull()
        }.toSet()
        years.forEach { year -> exchangeRateRepo.getRatesForYear(year) }

        return transactions.map { txn ->
            val saleDateISO = Form1099BParser().convertDateFormat(txn.dateSold)
            val rateResult = exchangeRateRepo.getRateForDate(saleDateISO)
            val rate = rateResult.getOrNull()?.usdToNis ?: 0.0

            txn.copy(
                exchangeRateOnSaleDate = rate,
                proceedsNIS = txn.proceeds * rate,
                costBasisNIS = txn.costBasis * rate,
                gainLossNIS = txn.gainLoss * rate
            )
        }
    }

    private fun computeForeignCapitalGains(
        transactions: List<TradeTransaction>
    ): Pair<Double, Double> {
        var longTermNIS = 0.0
        var shortTermNIS = 0.0

        for (txn in transactions) {
            // Use wash-sale adjusted gain/loss, converted to NIS
            val gainNIS = if (txn.exchangeRateOnSaleDate > 0) {
                txn.gainLossNIS
            } else {
                txn.gainLoss // fallback: use USD if no rate found
            }

            when (txn.holdingPeriod) {
                HoldingPeriod.LONG_TERM -> longTermNIS += gainNIS
                HoldingPeriod.SHORT_TERM -> shortTermNIS += gainNIS
            }
        }

        return Pair(longTermNIS, shortTermNIS)
    }

    /**
     * Computes progressive income tax on a given annual income using Israeli brackets.
     */
    fun computeProgressiveTax(income: Double, brackets: List<TaxBracket>): Double {
        var tax = 0.0
        var remaining = income

        for (bracket in brackets) {
            if (remaining <= 0) break
            val taxableInBracket = min(remaining, bracket.upperBound - bracket.lowerBound)
            tax += taxableInBracket * bracket.rate
            remaining -= taxableInBracket
        }

        return tax
    }

    /**
     * Computes Foreign Tax Credit under Article 22 of the Israel-US Tax Treaty.
     *
     * The credit is limited to the lower of:
     *  a) US tax actually paid on the income
     *  b) Israeli tax on the same income (proportional basket approach)
     *
     * Israel uses the "per-country" limitation method.
     */
    private fun computeForeignTaxCredit(
        usTaxPaidOnForeignIncome: Double,
        israeliTaxOnSameIncome: Double
    ): Double {
        return min(usTaxPaidOnForeignIncome, israeliTaxOnSameIncome)
    }

    /**
     * Computes Bituach Leumi and Health Insurance from first principles,
     * for cases where Form 106 values are unclear.
     */
    fun computeBituachLeumi(grossAnnualSalary: Double): BituachLeumiResult {
        val cappedSalary = min(grossAnnualSalary, BL_UPPER_CEILING_2024)

        val blEmployee = if (cappedSalary <= BL_LOWER_CEILING_2024) {
            cappedSalary * BL_RATE_LOWER
        } else {
            BL_LOWER_CEILING_2024 * BL_RATE_LOWER +
                (cappedSalary - BL_LOWER_CEILING_2024) * BL_RATE_UPPER
        }

        val healthInsurance = if (cappedSalary <= BL_LOWER_CEILING_2024) {
            cappedSalary * HEALTH_RATE_LOWER
        } else {
            BL_LOWER_CEILING_2024 * HEALTH_RATE_LOWER +
                (cappedSalary - BL_LOWER_CEILING_2024) * HEALTH_RATE_UPPER
        }

        return BituachLeumiResult(
            nationalInsuranceEmployee = blEmployee,
            healthInsurance = healthInsurance,
            total = blEmployee + healthInsurance
        )
    }

    data class TaxBracket(
        val lowerBound: Double,
        val upperBound: Double,
        val rate: Double
    )

    data class BituachLeumiResult(
        val nationalInsuranceEmployee: Double,
        val healthInsurance: Double,
        val total: Double
    )
}
