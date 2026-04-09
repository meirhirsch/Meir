package com.israelitax.app.data.models

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────
// Form 106 – Israeli Employer Annual Salary Statement
// Fields reference: Israeli Tax Authority field codes
// ─────────────────────────────────────────────────
@Serializable
data class Form106Data(
    val taxYear: Int = 0,
    val employerName: String = "",
    val employerId: String = "",          // מספר תיק ניכויים
    val employeeId: String = "",          // ת.ז.
    val employeeName: String = "",

    // Income codes (קוד הכנסה)
    val grossIncome: Double = 0.0,        // code 158 – הכנסה ברוטו
    val taxableIncome: Double = 0.0,      // code 172 – הכנסה חייבת
    val taxExemptIncome: Double = 0.0,   // code 161 – הכנסה פטורה

    // Deductions withheld
    val incomeTaxWithheld: Double = 0.0,  // code 042 – מס הכנסה שנוכה
    val bituachLeumiEmployee: Double = 0.0, // code 045 – ביטוח לאומי עובד
    val bituachLeumiEmployer: Double = 0.0, // code 046 – ביטוח לאומי מעסיק
    val healthInsurance: Double = 0.0,    // code 047 – ביטוח בריאות
    val pensionEmployee: Double = 0.0,    // code 043 – קרן פנסיה עובד
    val pensionEmployer: Double = 0.0,    // code 044 – קרן פנסיה מעסיק
    val studyFund: Double = 0.0,          // code 048 – קרן השתלמות

    // Tax credits
    val taxCreditsPoints: Double = 0.0,   // נקודות זיכוי
    val taxCreditsAmount: Double = 0.0,   // סכום זיכויים

    // Currency
    val currencyNIS: String = "NIS"
)

// ─────────────────────────────────────────────────
// IBKR Form 1099-B – Proceeds from Broker Transactions
// ─────────────────────────────────────────────────
@Serializable
data class Form1099BData(
    val taxYear: Int = 0,
    val brokerName: String = "Interactive Brokers LLC",
    val brokerEIN: String = "",
    val taxpayerName: String = "",
    val taxpayerSSN: String = "",         // stored masked
    val transactions: List<TradeTransaction> = emptyList(),

    // Summary totals from 1099-B
    val totalProceeds: Double = 0.0,
    val totalCostBasis: Double = 0.0,
    val totalNetGainLoss: Double = 0.0,

    // Short-term vs long-term breakdown
    val shortTermGainLoss: Double = 0.0,  // Box 1a/1b – held ≤1 year, taxed as ordinary income
    val longTermGainLoss: Double = 0.0,   // Box 2a/2b – held >1 year, preferential rate

    // Wash sale disallowance
    val washSaleLossDisallowed: Double = 0.0, // Box 1g

    // Federal tax withheld
    val federalTaxWithheld: Double = 0.0  // Box 4
)

@Serializable
data class TradeTransaction(
    val description: String = "",         // stock/option description
    val cusip: String = "",
    val dateAcquired: String = "",        // MM/DD/YYYY or "VARIOUS"
    val dateSold: String = "",            // MM/DD/YYYY — used for exact exchange rate lookup
    val proceeds: Double = 0.0,           // Box 1d (USD)
    val costBasis: Double = 0.0,          // Box 1e (USD)
    val washSaleAdj: Double = 0.0,        // Box 1g
    val gainLoss: Double = 0.0,           // calculated: proceeds - costBasis + washSaleAdj
    val holdingPeriod: HoldingPeriod = HoldingPeriod.SHORT_TERM,
    val covered: Boolean = true,          // covered vs non-covered security

    // Populated after exchange rate lookup
    val exchangeRateOnSaleDate: Double = 0.0,  // NIS per 1 USD from Bank of Israel
    val proceedsNIS: Double = 0.0,
    val costBasisNIS: Double = 0.0,
    val gainLossNIS: Double = 0.0
)

enum class HoldingPeriod { SHORT_TERM, LONG_TERM }

// ─────────────────────────────────────────────────
// Exchange Rate Record – from Bank of Israel API
// ─────────────────────────────────────────────────
@Serializable
data class ExchangeRate(
    val date: String = "",        // YYYY-MM-DD
    val usdToNis: Double = 0.0,   // how many NIS per 1 USD
    val source: String = "Bank of Israel"
)

// ─────────────────────────────────────────────────
// Computed Tax Results
// ─────────────────────────────────────────────────
@Serializable
data class USTaxResult(
    val taxYear: Int = 0,
    val filingStatus: FilingStatus = FilingStatus.SINGLE,

    // Income
    val wagesAndSalaries: Double = 0.0,   // Line 1a (from Form 106 converted to USD)
    val shortTermCapGains: Double = 0.0,  // Schedule D
    val longTermCapGains: Double = 0.0,
    val totalIncome: Double = 0.0,        // Line 9

    // Deductions
    val standardDeduction: Double = 0.0,
    val agi: Double = 0.0,                // Adjusted Gross Income
    val taxableIncome: Double = 0.0,

    // Tax
    val ordinaryIncomeTax: Double = 0.0,
    val capitalGainsTax: Double = 0.0,
    val totalTax: Double = 0.0,           // Line 24

    // Credits & payments
    val foreignTaxCredit: Double = 0.0,   // Form 1116 – Israeli tax paid credited against US
    val federalTaxWithheld: Double = 0.0,
    val totalCreditsAndPayments: Double = 0.0,

    // Result
    val refundOrOwed: Double = 0.0,       // positive = refund, negative = owed
    val estimatedTaxPenalty: Double = 0.0,

    // FBAR flag
    val requiresFBAR: Boolean = false,    // if foreign accounts > $10,000
    val requiresForm8938: Boolean = false // FATCA if foreign assets > threshold
)

@Serializable
data class IsraeliTaxResult(
    val taxYear: Int = 0,

    // Income (all in NIS)
    val salaryIncome: Double = 0.0,       // from Form 106 code 158
    val foreignTradingIncome: Double = 0.0, // from IBKR converted to NIS
    val totalWorldwideIncome: Double = 0.0,

    // Israeli tax on salary (progressive brackets)
    val salaryTax: Double = 0.0,

    // Israeli tax on foreign capital gains
    // Long-term foreign securities: 25% flat rate
    // Short-term (if marked as business income): marginal rate
    val capitalGainsTaxLongTerm: Double = 0.0,  // 25%
    val capitalGainsTaxShortTerm: Double = 0.0, // marginal rate
    val totalCapGainsTax: Double = 0.0,

    // Credits
    val taxCreditsPoints: Double = 0.0,
    val taxCreditsAmount: Double = 0.0,
    val foreignTaxCreditFromUS: Double = 0.0, // credit for US tax paid on same income

    // Already withheld
    val incomeTaxWithheld: Double = 0.0,  // from Form 106 code 042
    val bituachLeumi: Double = 0.0,

    // Result
    val totalTaxLiability: Double = 0.0,
    val totalTaxPaid: Double = 0.0,
    val refundOrOwed: Double = 0.0        // positive = refund (החזר), negative = תשלום
)

enum class FilingStatus {
    SINGLE, MARRIED_FILING_JOINTLY, MARRIED_FILING_SEPARATELY, HEAD_OF_HOUSEHOLD
}

// ─────────────────────────────────────────────────
// App State – holds all loaded form data
// ─────────────────────────────────────────────────
@Serializable
data class TaxSession(
    val taxYear: Int = 2024,
    val form106: Form106Data? = null,
    val form1099B: Form1099BData? = null,
    val filingStatus: FilingStatus = FilingStatus.SINGLE,
    val averageUsdNisRate: Double = 0.0,  // fallback only
    val usTaxResult: USTaxResult? = null,
    val israeliTaxResult: IsraeliTaxResult? = null
)
