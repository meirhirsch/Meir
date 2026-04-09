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
 * US Federal Income Tax Calculator (Form 1040)
 *
 * Handles:
 *  1. Foreign earned income from Israeli salary (Form 106 → converted USD)
 *  2. Capital gains/losses from IBKR day trading (Form 1099-B)
 *  3. Foreign Tax Credit (Form 1116) for Israeli taxes paid
 *  4. FBAR / FATCA thresholds check
 *
 * Legal basis:
 *  - IRC §1 (income tax rates)
 *  - IRC §1(h) (capital gains preferential rates)
 *  - IRC §63 (standard deduction)
 *  - IRC §901 / §904 (Foreign Tax Credit and limitation)
 *  - IRC §911 (Foreign Earned Income Exclusion – FEIE)
 *  - Israel-US Tax Treaty (1994) – prevents double taxation
 *  - Rev. Ruling 2008-5 (wash sale rules and IRAs)
 *  - IRC §1091 (wash sale rules)
 *
 * IMPORTANT: Israeli salary is foreign-source income. The taxpayer may elect either:
 *   a) Foreign Earned Income Exclusion (FEIE) – excludes up to $126,500 (2024)
 *   b) Foreign Tax Credit (FTC) – credits Israeli taxes against US liability
 * We present both options and let the user choose.
 * Most day traders choose FTC to preserve ability to use losses.
 *
 * Currency: The IRS requires using the exchange rate on the date of the transaction
 * (or an average annual rate). We use per-transaction rates matching our Israeli calc.
 */
class USTaxCalculator(
    private val exchangeRateRepo: ExchangeRateRepository
) {
    companion object {
        // ─── 2024 Ordinary Income Tax Brackets ──────────────────────────────
        // Source: IRS Rev. Proc. 2023-34
        val SINGLE_BRACKETS_2024 = listOf(
            OrdinaryBracket(0.0,         11_600.0,  0.10),
            OrdinaryBracket(11_600.0,    47_150.0,  0.12),
            OrdinaryBracket(47_150.0,   100_525.0,  0.22),
            OrdinaryBracket(100_525.0,  191_950.0,  0.24),
            OrdinaryBracket(191_950.0,  243_725.0,  0.32),
            OrdinaryBracket(243_725.0,  609_350.0,  0.35),
            OrdinaryBracket(609_350.0,  Double.MAX_VALUE, 0.37)
        )

        val MFJ_BRACKETS_2024 = listOf(
            OrdinaryBracket(0.0,         23_200.0,  0.10),
            OrdinaryBracket(23_200.0,    94_300.0,  0.12),
            OrdinaryBracket(94_300.0,   201_050.0,  0.22),
            OrdinaryBracket(201_050.0,  383_900.0,  0.24),
            OrdinaryBracket(383_900.0,  487_450.0,  0.32),
            OrdinaryBracket(487_450.0,  731_200.0,  0.35),
            OrdinaryBracket(731_200.0,  Double.MAX_VALUE, 0.37)
        )

        // ─── 2024 Standard Deductions ────────────────────────────────────────
        val STANDARD_DEDUCTION_2024 = mapOf(
            FilingStatus.SINGLE to 14_600.0,
            FilingStatus.MARRIED_FILING_JOINTLY to 29_200.0,
            FilingStatus.MARRIED_FILING_SEPARATELY to 14_600.0,
            FilingStatus.HEAD_OF_HOUSEHOLD to 21_900.0
        )

        // ─── Long-Term Capital Gains Rate Thresholds (2024) ─────────────────
        // 0% rate: taxable income up to $47,025 (single) / $94,050 (MFJ)
        // 15% rate: up to $518,900 (single) / $583,750 (MFJ)
        // 20% rate: above thresholds
        val LTCG_THRESHOLDS_SINGLE_2024 = LTCGThresholds(47_025.0, 518_900.0)
        val LTCG_THRESHOLDS_MFJ_2024 = LTCGThresholds(94_050.0, 583_750.0)

        // ─── Foreign Earned Income Exclusion (2024) ──────────────────────────
        const val FEIE_LIMIT_2024 = 126_500.0  // IRC §911(b)(2)

        // ─── FBAR Threshold ──────────────────────────────────────────────────
        const val FBAR_THRESHOLD = 10_000.0    // FinCEN 114

        // ─── FATCA Form 8938 Thresholds (domestic filer, single) ─────────────
        const val FATCA_THRESHOLD_SINGLE_EOY = 50_000.0
        const val FATCA_THRESHOLD_MFJ_EOY = 100_000.0

        // ─── Average USD/NIS rate – used for salary conversion as IRS-permitted fallback
        // Per IRS guidance, you may use either spot rate on date of payment
        // or the average annual rate published by US Treasury / IRS
        // IRS 2024 average: approximately 3.73 NIS/USD (varies by year)
        const val IRS_AVERAGE_RATE_2024_USD_TO_NIS = 3.73
    }

    /**
     * Main calculation.
     *
     * @param form106 Parsed Israeli salary statement
     * @param form1099B Parsed IBKR 1099-B (transactions already enriched with NIS rates)
     * @param filingStatus US filing status
     * @param israeliTaxPaid Total Israeli income tax paid (from Form 106 + any balance due)
     * @param useFEIE If true, use Foreign Earned Income Exclusion instead of FTC for salary
     * @param israeliAccountBalance Max balance in Israeli bank/IBKR accounts during year (for FBAR)
     */
    suspend fun calculate(
        form106: Form106Data,
        form1099B: Form1099BData,
        filingStatus: FilingStatus,
        israeliTaxPaid: Double,
        useFEIE: Boolean = false,
        israeliAccountBalance: Double = 0.0
    ): USTaxResult {

        val brackets = when (filingStatus) {
            FilingStatus.MARRIED_FILING_JOINTLY -> MFJ_BRACKETS_2024
            else -> SINGLE_BRACKETS_2024
        }
        val standardDeduction = STANDARD_DEDUCTION_2024[filingStatus] ?: 14_600.0
        val ltcgThresholds = when (filingStatus) {
            FilingStatus.MARRIED_FILING_JOINTLY -> LTCG_THRESHOLDS_MFJ_2024
            else -> LTCG_THRESHOLDS_SINGLE_2024
        }

        // ─── Step 1: Convert Israeli salary to USD ───────────────────────────
        // Use average annual rate for salary (IRS-accepted method for regular salary income)
        // The BOI annual average is used since salary is paid periodically, not in one lump sum
        val salaryUSD = form106.grossIncome / IRS_AVERAGE_RATE_2024_USD_TO_NIS

        // ─── Step 2: FEIE or FTC for salary ─────────────────────────────────
        val (includedSalaryUSD, feieExcluded) = if (useFEIE) {
            val excluded = min(salaryUSD, FEIE_LIMIT_2024)
            Pair(salaryUSD - excluded, excluded)
        } else {
            Pair(salaryUSD, 0.0)
        }

        // ─── Step 3: Capital gains from 1099-B ──────────────────────────────
        // Per-transaction rates already applied; we sum USD directly from 1099-B
        val shortTermGL = form1099B.shortTermGainLoss   // ordinary income treatment
        val longTermGL = form1099B.longTermGainLoss     // preferential rate

        // Wash sales already reflected in the 1099-B reported amounts
        val netShortTerm = shortTermGL
        val netLongTerm = longTermGL

        // ─── Step 4: Adjusted Gross Income (AGI) ────────────────────────────
        val grossIncome = includedSalaryUSD + netShortTerm + max(0.0, netLongTerm)
        val agi = grossIncome  // No above-the-line deductions in base case

        // ─── Step 5: Taxable Income ──────────────────────────────────────────
        val taxableIncome = max(0.0, agi - standardDeduction)

        // ─── Step 6: Ordinary Income Tax (on salary + short-term gains) ─────
        // Short-term capital gains taxed as ordinary income per IRC §1(h)(1)
        val ordinaryTaxableIncome = includedSalaryUSD + netShortTerm - standardDeduction
        val ordinaryIncomeTax = computeProgressiveTax(
            max(0.0, ordinaryTaxableIncome), brackets
        )

        // ─── Step 7: Long-Term Capital Gains Tax ────────────────────────────
        val ltcgTax = if (netLongTerm > 0) {
            computeLTCGTax(
                ltcGains = netLongTerm,
                ordinaryTaxableIncome = max(0.0, ordinaryTaxableIncome),
                thresholds = ltcgThresholds
            )
        } else 0.0

        // Net Investment Income Tax (NIIT) – 3.8% on NII above threshold
        val niitTax = computeNIIT(
            netInvestmentIncome = max(0.0, netShortTerm) + max(0.0, netLongTerm),
            agi = agi,
            filingStatus = filingStatus
        )

        val totalTaxBeforeCredits = ordinaryIncomeTax + ltcgTax + niitTax

        // ─── Step 8: Foreign Tax Credit (Form 1116) ──────────────────────────
        // FTC limits: can't exceed US tax that would apply to the foreign income
        // Basket: general limitation (salary), passive (capital gains)
        val ftcOnSalary = if (!useFEIE) {
            computeFTCBasket(
                foreignTaxPaid = israeliTaxPaid,
                foreignIncome = includedSalaryUSD,
                totalIncome = max(1.0, grossIncome),
                totalUSTax = totalTaxBeforeCredits
            )
        } else 0.0  // Can't claim FTC on excluded income

        val foreignTaxCredit = min(ftcOnSalary, totalTaxBeforeCredits)

        // ─── Step 9: Total payments ──────────────────────────────────────────
        val totalPayments = form1099B.federalTaxWithheld + foreignTaxCredit

        // ─── Step 10: Amount owed or refund ─────────────────────────────────
        val taxAfterCredits = max(0.0, totalTaxBeforeCredits - foreignTaxCredit)
        val refundOrOwed = totalPayments - totalTaxBeforeCredits

        // ─── Step 11: FBAR / FATCA flags ─────────────────────────────────────
        val requiresFBAR = israeliAccountBalance > FBAR_THRESHOLD
        val fatcaThreshold = if (filingStatus == FilingStatus.MARRIED_FILING_JOINTLY)
            FATCA_THRESHOLD_MFJ_EOY else FATCA_THRESHOLD_SINGLE_EOY
        val requiresForm8938 = israeliAccountBalance > fatcaThreshold

        return USTaxResult(
            taxYear = form106.taxYear,
            filingStatus = filingStatus,
            wagesAndSalaries = includedSalaryUSD,
            shortTermCapGains = netShortTerm,
            longTermCapGains = netLongTerm,
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

    /**
     * Progressive ordinary income tax.
     */
    fun computeProgressiveTax(income: Double, brackets: List<OrdinaryBracket>): Double {
        var tax = 0.0
        var remaining = income
        for (bracket in brackets) {
            if (remaining <= 0) break
            val inBracket = min(remaining, bracket.upperBound - bracket.lowerBound)
            tax += inBracket * bracket.rate
            remaining -= inBracket
        }
        return tax
    }

    /**
     * Long-Term Capital Gains tax with stacking rules.
     * LTCG "stacks on top of" ordinary income for rate determination.
     */
    private fun computeLTCGTax(
        ltcGains: Double,
        ordinaryTaxableIncome: Double,
        thresholds: LTCGThresholds
    ): Double {
        var tax = 0.0
        var remainingGain = ltcGains
        var stackBase = ordinaryTaxableIncome

        // 0% band
        val zeroRoomLeft = max(0.0, thresholds.zeroRateMax - stackBase)
        val atZero = min(remainingGain, zeroRoomLeft)
        tax += atZero * 0.0
        remainingGain -= atZero
        stackBase += atZero

        // 15% band
        val fifteenRoomLeft = max(0.0, thresholds.fifteenRateMax - stackBase)
        val atFifteen = min(remainingGain, fifteenRoomLeft)
        tax += atFifteen * 0.15
        remainingGain -= atFifteen

        // 20% on remainder
        tax += remainingGain * 0.20

        return tax
    }

    /**
     * Net Investment Income Tax (IRC §1411) – 3.8% on NII above MAGI threshold.
     * Single: $200,000 / MFJ: $250,000
     */
    private fun computeNIIT(
        netInvestmentIncome: Double,
        agi: Double,
        filingStatus: FilingStatus
    ): Double {
        val threshold = if (filingStatus == FilingStatus.MARRIED_FILING_JOINTLY) 250_000.0 else 200_000.0
        val excess = max(0.0, agi - threshold)
        return min(netInvestmentIncome, excess) * 0.038
    }

    /**
     * Foreign Tax Credit limitation per IRC §904.
     * General basket formula: (foreign income / total income) × US total tax
     */
    private fun computeFTCBasket(
        foreignTaxPaid: Double,
        foreignIncome: Double,
        totalIncome: Double,
        totalUSTax: Double
    ): Double {
        val limitation = (foreignIncome / totalIncome) * totalUSTax
        return min(foreignTaxPaid, limitation)
    }

    data class OrdinaryBracket(
        val lowerBound: Double,
        val upperBound: Double,
        val rate: Double
    )

    data class LTCGThresholds(
        val zeroRateMax: Double,
        val fifteenRateMax: Double
    )
}
