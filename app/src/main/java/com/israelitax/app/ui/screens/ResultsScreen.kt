package com.israelitax.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.israelitax.app.data.models.HoldingPeriod
import com.israelitax.app.data.models.IsraeliTaxResult
import com.israelitax.app.data.models.TradeTransaction
import com.israelitax.app.data.models.USTaxResult
import com.israelitax.app.tax.israel.IsraeliTaxCalculator
import com.israelitax.app.ui.viewmodels.TaxUiState
import com.israelitax.app.ui.viewmodels.TaxViewModel
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: TaxViewModel,
    uiState: TaxUiState,
    onBack: () -> Unit,
    onStartOver: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🇮🇱  Israeli CPA", "🇺🇸  US CPA", "📊  Trades")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Review 2025", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onStartOver) {
                        Icon(Icons.Default.Refresh, contentDescription = "Start Over")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> IsraeliCPATab(uiState)
                1 -> USCPATab(uiState)
                2 -> TradesTab(uiState)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 1 – Israeli CPA (Hebrew + English bilingual)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IsraeliCPATab(uiState: TaxUiState) {
    val il = uiState.israeliTaxResult
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        CPAHeader(
            title = "דוח מס שנתי – טופס 1301",
            subtitle = "Israeli Annual Tax Return (Form 1301) — Tax Year ${il?.taxYear ?: 2025}",
            color = Color(0xFF1565C0)
        )

        if (il == null) {
            Text("No calculation results yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        // ── Section 1: Salary Income (Form 106) ──────────────────────────────
        CPASection(title = "הכנסות מעבודה — Salary Income") {
            val f = uiState.form106
            CPABilingualRow("מעביד / Employer", f?.employerName?.ifBlank { "—" } ?: "—")
            CPABilingualRow("תיק ניכויים / Employer ID", f?.employerId?.ifBlank { "—" } ?: "—")
            CPABilingualRow("הכנסה ברוטו / Gross Income (Code 158)", nisFormat(il.salaryIncome))
            CPABilingualRow("מס הכנסה שנוכה / Tax Withheld (Code 042)", nisFormat(f?.incomeTaxWithheld ?: 0.0))
            CPABilingualRow("ביטוח לאומי עובד / Bituach Leumi (Code 045)", nisFormat(f?.bituachLeumiEmployee ?: 0.0))
            CPABilingualRow("ביטוח בריאות / Health Insurance (Code 047)", nisFormat(f?.healthInsurance ?: 0.0))
            CPABilingualRow("פנסיה עובד / Pension Employee (Code 043)", nisFormat(f?.pensionEmployee ?: 0.0))
            CPABilingualRow("קרן השתלמות / Study Fund (Code 048)", nisFormat(f?.studyFund ?: 0.0))
            CPABilingualRow("נקודות זיכוי / Credit Points", "%.2f".format(il.taxCreditsPoints))
        }

        // ── Exchange Rate Note ─────────────────────────────────────────────────
        if (uiState.boiAnnualAvgRate > 0) {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CurrencyExchange, null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp))
                    Column {
                        Text("שער BOI שנתי ממוצע / BOI Annual Average Rate",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium)
                        Text("${"%.4f".format(uiState.boiAnnualAvgRate)} ₪ לדולר (${"%.4f".format(1.0 / uiState.boiAnnualAvgRate)} USD/NIS)",
                            style = MaterialTheme.typography.bodySmall)
                        Text("שיעור זה מחושב מנתוני בנק ישראל היומיים לשנת המס. " +
                            "כל עסקת מסחר ממירה לפי שער היום הספציפי של המכירה.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // ── Section 2: Capital Gains (IBKR) ──────────────────────────────────
        CPASection(title = "רווחי הון מחו\"ל — Foreign Capital Gains (IBKR)") {
            val totalTrades = il.enrichedTransactions.size
            val ratedTrades = il.enrichedTransactions.count { it.exchangeRateOnSaleDate > 0 }
            CPABilingualRow("מספר עסקאות / Total Trades", "$totalTrades")
            CPABilingualRow("עסקאות עם שער BOI / Trades with BOI Rate", "$ratedTrades / $totalTrades")
            CPABilingualRow("רווח/הפסד קצר טווח (NIS)", nisFormat(
                il.enrichedTransactions.filter { it.holdingPeriod == HoldingPeriod.SHORT_TERM }
                    .sumOf { if (it.exchangeRateOnSaleDate > 0) it.gainLossNIS else it.gainLoss }
            ))
            CPABilingualRow("רווח/הפסד ארוך טווח (NIS)", nisFormat(
                il.enrichedTransactions.filter { it.holdingPeriod == HoldingPeriod.LONG_TERM }
                    .sumOf { if (it.exchangeRateOnSaleDate > 0) it.gainLossNIS else it.gainLoss }
            ))
            CPABilingualRow("סה\"כ רווחי הון בש\"ח / Total Cap Gains NIS", nisFormat(il.foreignTradingIncome))
        }

        // ── Section 3: Tax Calculation (Progressive Brackets) ────────────────
        CPASection(title = "חישוב מס הכנסה — Income Tax Calculation") {
            // Show bracket-by-bracket breakdown for salary
            val brackets = IsraeliTaxCalculator.INCOME_BRACKETS_2025
            var remaining = il.salaryIncome
            brackets.forEach { b ->
                if (remaining <= 0) return@forEach
                val inBracket = minOf(remaining, b.upperBound - b.lowerBound)
                if (inBracket <= 0) return@forEach
                val pct = (b.rate * 100).toInt()
                CPABilingualRow(
                    "מדרגה $pct% / Bracket $pct% (up to ${nisFormat(b.upperBound).replace("₪","")} NIS)",
                    "₪${"%,.0f".format(inBracket)} × $pct% = ${nisFormat(inBracket * b.rate)}"
                )
                remaining -= inBracket
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            CPABilingualRow("מס לפני זיכויים / Tax Before Credits",
                nisFormat(il.salaryTax + il.taxCreditsAmount))
            CPABilingualRow("זיכוי נקודות (${il.taxCreditsPoints} נקודות) / Credit Points",
                "-${nisFormat(il.taxCreditsAmount)}")
            CPABoldRow("", "מס שכר אחרי זיכויים / Salary Tax After Credits", nisFormat(il.salaryTax))
        }

        // ── Section 4: Capital Gains Tax (25% Flat) ───────────────────────────
        CPASection(title = "מס רווחי הון — Capital Gains Tax (Section 91)") {
            CPABilingualRow("שיעור מס / Rate", "25% flat (סעיף 91 לפקודה)")
            CPABilingualRow("מס על רווחים קצרי טווח / ST Cap Gains Tax",
                nisFormat(il.capitalGainsTaxShortTerm))
            CPABilingualRow("מס על רווחים ארוכי טווח / LT Cap Gains Tax",
                nisFormat(il.capitalGainsTaxLongTerm))
            CPABoldRow("", "סה\"כ מס רווחי הון / Total Capital Gains Tax", nisFormat(il.totalCapGainsTax))
        }

        // ── Section 5: Summary ────────────────────────────────────────────────
        CPASection(title = "סיכום — Summary") {
            CPABilingualRow("סה\"כ חבות מס / Total Tax Liability", nisFormat(il.totalTaxLiability))
            CPABilingualRow("מס שנוכה במקור (טופס 106) / Withheld", nisFormat(il.incomeTaxWithheld))
            CPABilingualRow("זיכוי מס זר (אמנה ישראל-ארה\"ב) / US FTC", nisFormat(il.foreignTaxCreditFromUS))
            CPABilingualRow("ביטוח לאומי + בריאות / BL + Health", nisFormat(il.bituachLeumi))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val isRefund = il.refundOrOwed >= 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isRefund) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (isRefund) "החזר מס" else "לתשלום",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isRefund) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    )
                    Text(
                        if (isRefund) "REFUND" else "AMOUNT OWED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    nisFormat(abs(il.refundOrOwed)),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (isRefund) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                )
            }
        }

        DisclaimerCard(
            "דוח זה הינו טיוטה לצרכי עיון בלבד. אינו מהווה ייעוץ מס. " +
            "יש להיוועץ ברואה חשבון מורשה לפני הגשה לרשות המסים.\n\n" +
            "DRAFT — Not a substitute for professional tax advice."
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 2 – US CPA (English)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun USCPATab(uiState: TaxUiState) {
    val context = LocalContext.current
    val us = uiState.usTaxResult
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CPAHeader(
            title = "Form 1040 — U.S. Individual Income Tax Return",
            subtitle = "Tax Year ${us?.taxYear ?: 2025} — Filing Status: ${uiState.filingStatus.displayName()}",
            color = Color(0xFFB71C1C)
        )

        if (us == null) {
            Text("No calculation results yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        // ── Income ────────────────────────────────────────────────────────────
        CPASection(title = "Income (Lines 1–8)") {
            val rateLabel = if (uiState.boiAnnualAvgRate > 0)
            "%.4f".format(uiState.boiAnnualAvgRate) else "BOI avg"
        CPALineRow("Line 1a", "Wages, Salaries, Tips (Israeli salary @ $rateLabel NIS/USD)",
                usdFormat(us.wagesAndSalaries))
            CPALineRow("Sch D", "Short-Term Capital Gains (IBKR, ordinary rate)",
                usdFormat(us.shortTermCapGains), signed = true)
            CPALineRow("Sch D", "Long-Term Capital Gains (IBKR, preferential rate)",
                usdFormat(us.longTermCapGains), signed = true)
            CPABoldRow("Line 9", "Total Income", usdFormat(us.totalIncome))
        }

        // ── Deductions ────────────────────────────────────────────────────────
        CPASection(title = "Deductions (Lines 10–15)") {
            CPALineRow("Line 12", "Standard Deduction (${us.filingStatus.displayName()})",
                "-${usdFormat(us.standardDeduction)}")
            CPABoldRow("Line 15", "Taxable Income", usdFormat(us.taxableIncome))
        }

        // ── Tax Calculation ───────────────────────────────────────────────────
        CPASection(title = "Tax & Credits (Lines 16–24)") {
            CPALineRow("Line 16", "Ordinary Income Tax (progressive brackets)", usdFormat(us.ordinaryIncomeTax))
            CPALineRow("Line 16", "Capital Gains Tax (LT rates + 3.8% NIIT)", usdFormat(us.capitalGainsTax))
            CPABoldRow("Line 24", "Total Tax Before Credits", usdFormat(us.totalTax))
        }

        // ── Credits & Payments ────────────────────────────────────────────────
        CPASection(title = "Credits & Payments (Lines 25–33)") {
            CPALineRow("Form 1116", "Foreign Tax Credit (Israeli income tax paid)",
                "-${usdFormat(us.foreignTaxCredit)}")
            CPALineRow("Line 25b", "Federal Tax Withheld (1099-B Box 4)",
                "-${usdFormat(us.federalTaxWithheld)}")
            CPABoldRow("Line 33", "Total Payments & Credits",
                usdFormat(us.totalCreditsAndPayments))
        }

        // ── Result ────────────────────────────────────────────────────────────
        CPASection(title = "Refund or Amount Owed") {
            val isRefund = us.refundOrOwed >= 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isRefund) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isRefund) "REFUND" else "AMOUNT OWED",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (isRefund) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                )
                Text(
                    usdFormat(abs(us.refundOrOwed)),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = if (isRefund) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                )
            }
        }

        // ── Compliance ────────────────────────────────────────────────────────
        if (us.requiresFBAR || us.requiresForm8938) {
            CPASection(title = "Compliance Filings Required") {
                if (us.requiresFBAR) {
                    CPAAlertRow("FinCEN 114 (FBAR) — File by April 15; automatic extension to Oct 15")
                }
                if (us.requiresForm8938) {
                    CPAAlertRow("Form 8938 (FATCA) — Attach to Form 1040")
                }
            }
        }

        // ── Download PDFs ─────────────────────────────────────────────────────
        CPASection(title = "Generated Tax Forms (DRAFT)") {
            uiState.form1040Path?.let { path ->
                FilledTonalButton(
                    onClick = { openPdf(context, path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Form 1040 PDF (DRAFT)")
                }
            }
            uiState.form1301Path?.let { path ->
                FilledTonalButton(
                    onClick = { openPdf(context, path) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open טופס 1301 PDF (טיוטה)")
                }
            }
        }

        // ── Exchange rate methodology note ────────────────────────────────────
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CurrencyExchange, null,
                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Text("Currency Conversion Methodology",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium)
                }
                if (uiState.boiAnnualAvgRate > 0) {
                    Text("Salary: converted at BOI ${"%.4f".format(uiState.boiAnnualAvgRate)} NIS/USD " +
                        "(Bank of Israel ${us?.taxYear ?: 2025} annual average, computed from daily rates)",
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("Capital gains: each trade converted at the exact BOI rate on the sale date " +
                    "(IRC §988 / IRS Notice 2000-20; Section 207A of the Israeli Income Tax Ordinance)",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        DisclaimerCard(
            "DRAFT — For review purposes only. Not filed with the IRS. " +
            "Consult a licensed CPA or Enrolled Agent familiar with US-Israel " +
            "dual taxation before filing. Tax law changes annually."
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 3 – Trade-by-Trade Breakdown with BOI Rate
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TradesTab(uiState: TaxUiState) {
    val trades = uiState.israeliTaxResult?.enrichedTransactions
        ?: uiState.form1099B?.transactions
        ?: emptyList()
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(vScroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary bar
        val stSum = trades.filter { it.holdingPeriod == HoldingPeriod.SHORT_TERM }.sumOf { it.gainLoss }
        val ltSum = trades.filter { it.holdingPeriod == HoldingPeriod.LONG_TERM }.sumOf { it.gainLoss }
        val stNIS = trades.filter { it.holdingPeriod == HoldingPeriod.SHORT_TERM }
            .sumOf { if (it.exchangeRateOnSaleDate > 0) it.gainLossNIS else it.gainLoss }
        val ltNIS = trades.filter { it.holdingPeriod == HoldingPeriod.LONG_TERM }
            .sumOf { if (it.exchangeRateOnSaleDate > 0) it.gainLossNIS else it.gainLoss }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Trade Summary", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column {
                        Text("Short-Term (ST)", style = MaterialTheme.typography.labelSmall)
                        Text(usdFormat(stSum), fontWeight = FontWeight.Bold,
                            color = if (stSum >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                        Text(nisFormat(stNIS), style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Long-Term (LT)", style = MaterialTheme.typography.labelSmall)
                        Text(usdFormat(ltSum), fontWeight = FontWeight.Bold,
                            color = if (ltSum >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                        Text(nisFormat(ltNIS), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("TOTAL NET G/L (USD)", fontWeight = FontWeight.Bold)
                    Text(usdFormat(stSum + ltSum), fontWeight = FontWeight.Bold,
                        color = if (stSum + ltSum >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("TOTAL NET G/L (NIS)", fontWeight = FontWeight.Bold)
                    Text(nisFormat(stNIS + ltNIS), fontWeight = FontWeight.Bold,
                        color = if (stNIS + ltNIS >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                }
                val noRateCount = trades.count { it.exchangeRateOnSaleDate == 0.0 }
                if (noRateCount > 0) {
                    Text("⚠ $noRateCount trade(s) missing BOI rate — using USD value as fallback",
                        color = Color(0xFFE65100),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        if (trades.isEmpty()) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No individual trades found in 1099-B scan.", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "OCR could not extract individual transaction rows. " +
                        "Summary totals from the 1099-B are still used for tax calculations. " +
                        "For per-trade BOI rates, ensure the IBKR 1099-B is scanned at high resolution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.form1099B?.let { f ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Short-Term G/L (from summary): ${usdFormat(f.shortTermGainLoss)}",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Long-Term G/L (from summary): ${usdFormat(f.longTermGainLoss)}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            return@Column
        }

        // Table header
        Row(
            modifier = Modifier
                .horizontalScroll(hScroll)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            TradeCell("Security", 120.dp, FontWeight.Bold)
            TradeCell("Date Sold", 92.dp, FontWeight.Bold)
            TradeCell("Type", 34.dp, FontWeight.Bold)
            TradeCell("Proceeds $", 78.dp, FontWeight.Bold, TextAlign.End)
            TradeCell("Cost $", 78.dp, FontWeight.Bold, TextAlign.End)
            TradeCell("G/L  $", 78.dp, FontWeight.Bold, TextAlign.End)
            TradeCell("BOI Rate", 66.dp, FontWeight.Bold, TextAlign.End)
            TradeCell("G/L ₪", 78.dp, FontWeight.Bold, TextAlign.End)
        }

        HorizontalDivider()

        trades.forEachIndexed { idx, t ->
            val rowBg = if (idx % 2 == 0) Color.Transparent
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            val glColor = if (t.gainLoss >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            val glNIS   = if (t.exchangeRateOnSaleDate > 0) t.gainLossNIS else t.gainLoss
            val glNISColor = if (glNIS >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)

            Row(
                modifier = Modifier
                    .horizontalScroll(hScroll)
                    .background(rowBg)
                    .padding(horizontal = 4.dp, vertical = 5.dp)
            ) {
                TradeCell(t.description.take(18), 120.dp)
                TradeCell(t.dateSold, 92.dp)
                TradeCell(if (t.holdingPeriod == HoldingPeriod.SHORT_TERM) "ST" else "LT", 34.dp)
                TradeCell("%,.0f".format(t.proceeds), 78.dp, align = TextAlign.End)
                TradeCell("%,.0f".format(t.costBasis), 78.dp, align = TextAlign.End)
                TradeCell("%+,.0f".format(t.gainLoss), 78.dp, align = TextAlign.End, color = glColor)
                TradeCell(
                    if (t.exchangeRateOnSaleDate > 0) "%.3f".format(t.exchangeRateOnSaleDate) else "—",
                    66.dp, align = TextAlign.End
                )
                TradeCell("%+,.0f".format(glNIS), 78.dp, align = TextAlign.End, color = glNISColor)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "BOI Rate = Bank of Israel USD/NIS rate on the exact date of sale (per Section 207A of the Income Tax Ordinance)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composable building blocks
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CPAHeader(title: String, subtitle: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, color = Color.White,
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CPASection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            content()
        }
    }
}

@Composable
private fun CPABilingualRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label,
            modifier = Modifier.weight(1f).padding(end = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CPABoldRow(ref: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$ref  $label",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f))
        Text(value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CPALineRow(ref: String, label: String, value: String, signed: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("[$ref]  $label",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(end = 4.dp))
        Text(value,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CPAAlertRow(message: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Warning, contentDescription = null,
            tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
    }
}

@Composable
private fun DisclaimerCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        )
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun TradeCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    fontWeight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    color: Color = Color.Unspecified
) {
    Text(
        text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 3.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = fontWeight,
        textAlign = align,
        color = color,
        maxLines = 1,
        fontFamily = FontFamily.Monospace
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Formatters & helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun usdFormat(v: Double): String = "$%,.0f".format(v)
private fun nisFormat(v: Double): String = "₪%,.0f".format(v)

private fun com.israelitax.app.data.models.FilingStatus.displayName() = when (this) {
    com.israelitax.app.data.models.FilingStatus.SINGLE -> "Single"
    com.israelitax.app.data.models.FilingStatus.MARRIED_FILING_JOINTLY -> "Married Filing Jointly"
    com.israelitax.app.data.models.FilingStatus.MARRIED_FILING_SEPARATELY -> "Married Filing Separately"
    com.israelitax.app.data.models.FilingStatus.HEAD_OF_HOUSEHOLD -> "Head of Household"
}

private fun openPdf(context: android.content.Context, path: String) {
    try {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        )
    } catch (e: Exception) {
        android.util.Log.e("ResultsScreen", "Cannot open PDF: ${e.message}")
    }
}


