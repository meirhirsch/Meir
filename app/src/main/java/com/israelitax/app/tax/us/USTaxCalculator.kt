package com.israelitax.app.tax.us

import com.israelitax.app.data.models.FilingStatus
import com.israelitax.app.data.models.Form106Data
import com.israelitax.app.data.models.Form1099BData
import com.israelitax.app.data.models.HoldingPeriod
import com.israelitax.app.data.models.USTaxResult
import com.israelitax.app.data.repository.ExchangeRateRepository
import kotlin.math.max
import kotlin.math.min

/**
 * US Federal Income Tax Calculator – Tax Year 2025 (Form 1040)
 *
 * Handles:
 *  1. Foreign earned income from Israeli salary (Form 106 → USD)
 *  2. Capital gains/losses from IBKR day trading (Form 1099-B)
 *  3. Foreign Tax Credit (Form 1116) for Israeli taxes paid
 *  4. FBAR / FATCA thresholds
 *
 * Legal basis:
 *  - IRC §1 (income tax rates), §1(h) (preferential cap gains rates)
 *  - IRC §63 (standard deduction), §901/§904 (FTC), §911 (FEIE)
 *  - IRS Rev. Proc. 2024-40 (2025 inflation adjustments)
 *  - Israel-US Tax Treaty (1994) – prevents double taxation
 */
class USTaxCalculator(
    private val exchangeRateRepo: ExchangeRateRepository
) {
    companion object {
        // ─── 2025 Ordinary Income Tax Brackets ──────────────────────────────
        // Source: IRS Rev. Proc. 2024-40
        val SINGLE_BRACKETS_2025 = listOf(
            OrdinaryBracket(0.0,          11_925.0,  0.10),
            OrdinaryBracket(11_925.0,     48_475.0,  0.12),
            OrdinaryBracket(48_475.0,    103_350.0,  0.22),
            OrdinaryBracket(103_350.0,   197_300.0,  0.24),
            OrdinaryBracket(197_300.0,   250_525.0,  0.32),
            OrdinaryBracket(250_525.0,   626_350.0,  0.35),
            OrdinaryBracket(626_350.0,   Double.MAX_VALUE, 0.37)
        )

        val MFJ_BRACKETS_2025 = listOf(
            OrdinaryBracket(0.0,          23_850.0,  0.10),
            OrdinaryBracket(23_850.0,     96_950.0,  0.12),
            OrdinaryBracket(96_950.0,    206_700.0,  0.22),
            OrdinaryBracket(206_700.0,   394_600.0,  0.24),
            OrdinaryBracket(394_600.0,   501_050.0,  0.32),
            OrdinaryBracket(501_050.0,   751_600.0,  0.35),
            OrdinaryBracket(751_600.0,   Double.MAX_VALUE, 0.37)
        )

        // ─── 2025 Standard Deductions ────────────────────────────────────────
        val STANDARD_DEDUCTION_2025 = mapOf(
            FilingStatus.SINGLE                  to 15_000.0,
            FilingStatus.MARRIED_FILING_JOINTLY  to 30_000.0,
            FilingStatus.MARRIED_FILING_SEPARATELY to 15_000.0,
            FilingStatus.HEAD_OF_HOUSEHOLD       to 22_500.0
        )

        // ─── 2025 Long-Term Capital Gains Rate Thresholds ────────────────────
        // 0% / 15% / 20% stacking thresholds
        val LTCG_THRESHOLDS_SINGLE_2025 = LTCGThresholds(48_350.0, 533_400.0)
        val LTCG_THRESHOLDS_MFJ_2025    = LTCGThresholds(96_700.0, 600_050.0)

        // ─── Foreign Earned Income Exclusion (2025) ──────────────────────────
        const val FEIE_LIMIT_2025 = 130_000.0   // IRC §911(b)(2)

        // ─── FBAR / FATCA Thresholds ─────────────────────────────────────────
        const val FBAR_THRESHOLD            = 10_000.0
        const val FATCA_THRESHOLD_SINGLE    = 50_000.0
        const val FATCA_THRESHOLD_MFJ       = 100_000.0

        // ─── IRS average USD/NIS rate for 2025 ───────────────────────────────
        // Used for Israeli salary conversion (IRS-permitted average annual rate method).
        // Per-transaction rates from BOI are used for capital gains.
        // IRS 2025 approximate average: ~3.70 NIS/USD (final rate published Jan 2026)
        const val IRS_AVERAGE_RATE_2025_USD_TO_NIS = 3.70
    }

    suspend fun calculate(
        form106: Form106Data,
        form1099B: Form1099BData,
        filingStatus: FilingStatus,
        israeliTaxPaid: Double,
        useFEIE: Boolean = false,
        israeliAccountBalance: Double = 0.0
    ): USTaxResult {

        val brackets = when (filingStatus) {
            FilingStatus.MARRIED_FILING_JOINTLY -> MFJ_BRACKETS_2025
            else -> SINGLE_BRACKETS_2025
        }
        val standardDeduction = STANDARD_DEDUCTION_2025[filingStatus] ?: 15_000.0
        val ltcgThresholds = when (filingStatus) {
            FilingStatus.MARRIED_FILING_JOINTLY -> LTCG_THRESHOLDS_MFJ_2025
            else -> LTCG_THRESHOLDS_SINGLE_2025
        }

        // Step 1: Convert Israeli salary to USD using IRS average annual rate
        val salaryUSD = form106.grossIncome / IRS_AVERAGE_RATE_2025_USD_TO_NIS

        // Step 2: FEIE or FTC election for salary
        val (includedSalaryUSD, feieExcluded) = if (useFEIE) {
            val excluded = min(salaryUSD, FEIE_LIMIT_2025)
            Pair(salaryUSD - excluded, excluded)
        } else {
            Pair(salaryUSD, 0.0)
        }

        // Step 3: Capital gains from 1099-B (USD amounts – already in USD from IBKR)
        val shortTermGL = form1099B.shortTermGainLoss
        val longTermGL  = form1099B.longTermGainLoss

        // Step 4: Adjusted Gross Income (AGI)
        val grossIncome = includedSalaryUSD + shortTermGL + max(0.0, longTermGL)
        val agi = grossIncome

        // Step 5: Taxable income
        val taxableIncome = max(0.0, agi - standardDeduction)

        // Step 6: Ordinary income tax (salary + short-term gains – both at ordinary rates)
        val ordinaryBase = max(0.0, includedSalaryUSD + shortTermGL - standardDeduction)
        val ordinaryIncomeTax = computeProgressiveTax(ordinaryBase, brackets)

        // Step 7: Long-term capital gains tax (stacking rule)
        val ltcgTax = if (longTermGL > 0)
            computeLTCGTax(longTermGL, ordinaryBase, ltcgThresholds)
        else 0.0

        // Step 8: Net Investment Income Tax (IRC §1411) – 3.8% above MAGI threshold
        val niitTax = computeNIIT(
            netInvestmentIncome = max(0.0, shortTermGL) + max(0.0, longTermGL),
            agi = agi,
            filingStatus = filingStatus
        )

        val totalTaxBeforeCredits = ordinaryIncomeTax + ltcgTax + niitTax

        // Step 9: Foreign Tax Credit (Form 1116) on Israeli salary basket
        val ftcOnSalary = if (!useFEIE) {
            computeFTCBasket(
                foreignTaxPaid = israeliTaxPaid,
                foreignIncome  = includedSalaryUSD,
                totalIncome    = max(1.0, grossIncome),
                totalUSTax     = totalTaxBeforeCredits
            )
        } else 0.0
        val foreignTaxCredit = min(ftcOnSalary, totalTaxBeforeCredits)

        // Step 10: Total payments and result
        val totalPayments = form1099B.federalTaxWithheld + foreignTaxCredit
        val refundOrOwed  = totalPayments - totalTaxBeforeCredits

        // Step 11: FBAR / FATCA compliance
        val requiresFBAR = israeliAccountBalance > FBAR_THRESHOLD
        val fatcaThreshold = if (filingStatus == FilingStatus.MARRIED_FILING_JOINTLY)
            FATCA_THRESHOLD_MFJ else FATCA_THRESHOLD_SINGLE
        val requiresForm8938 = israeliAccountBalance > fatcaThreshold

        return USTaxResult(
            taxYear = form106.taxYear.takeIf { it > 0 } ?: 2025,
            filingStatus = filingStatus,
            wagesAndSalaries = includedSalaryUSD,
            shortTermCapGains = shortTermGL,
            longTermCapGains = longTermGL,
            totalIncome = grossIncome,
            standardDeduction = standardDeduction,
            agi = agi,
            taxableIncome = taxableIncome,
            ordinaryIncomeTax = ordinaryIncomeTax,
            capitalGainsTax = ltcgTax + niitTax,
            totalTax = totalTaxBeforeCredits,
            foreignTaxCredit = foreignTaxCredit,
            federalTaxWithheld = form1099B.federalTaxWithheld,
            totalCreditsAndPayments = totalPayments,
            refundOrOwed = refundOrOwed,
            requiresFBAR = requiresFBAR,
            requiresForm8938 = requiresForm8938
        )
    }

    fun computeProgressiveTax(income: Double, brackets: List<OrdinaryBracket>): Double {
        var tax = 0.0; var remaining = income
        for (b in brackets) {
            if (remaining <= 0) break
            val inBracket = min(remaining, b.upperBound - b.lowerBound)
            tax += inBracket * b.rate
            remaining -= inBracket
        }
        return tax
    }

    private fun computeLTCGTax(
        ltcGains: Double,
        ordinaryTaxableIncome: Double,
        thresholds: LTCGThresholds
    ): Double {
        var tax = 0.0; var remaining = ltcGains; var base = ordinaryTaxableIncome
        val zeroRoom = max(0.0, thresholds.zeroRateMax - base)
        val atZero   = min(remaining, zeroRoom)
        remaining -= atZero; base += atZero
        val fifteenRoom = max(0.0, thresholds.fifteenRateMax - base)
        val atFifteen   = min(remaining, fifteenRoom)
        tax += atFifteen * 0.15; remaining -= atFifteen
        tax += remaining * 0.20
        return tax
    }

    private fun computeNIIT(
        netInvestmentIncome: Double,
        agi: Double,
        filingStatus: FilingStatus
    ): Double {
        val threshold = if (filingStatus == FilingStatus.MARRIED_FILING_JOINTLY) 250_000.0 else 200_000.0
        val excess = max(0.0, agi - threshold)
        return min(netInvestmentIncome, excess) * 0.038
    }

    private fun computeFTCBasket(
        foreignTaxPaid: Double,
        foreignIncome: Double,
        totalIncome: Double,
        totalUSTax: Double
    ): Double {
        val limitation = (foreignIncome / totalIncome) * totalUSTax
        return min(foreignTaxPaid, limitation)
    }

    data class OrdinaryBracket(val lowerBound: Double, val upperBound: Double, val rate: Double)
    data class LTCGThresholds(val zeroRateMax: Double, val fifteenRateMax: Double)
}
