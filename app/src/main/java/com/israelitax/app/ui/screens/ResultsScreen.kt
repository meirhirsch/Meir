package com.israelitax.app.ui.screens

import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.israelitax.app.ui.viewmodels.TaxUiState
import com.israelitax.app.ui.viewmodels.TaxViewModel
import java.io.File

@Composable
fun ResultsScreen(
    viewModel: TaxViewModel,
    uiState: TaxUiState,
    onBack: () -> Unit,
    onStartOver: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Summary") },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            StepIndicator(
                steps = listOf("Upload Docs", "Review", "Generate Forms"),
                currentStep = 2
            )

            // ─── US Tax Summary (Form 1040) ───────────────────────────────────
            uiState.usTaxResult?.let { us ->
                TaxSummaryCard(
                    title = "🇺🇸 US Federal Tax (Form 1040)",
                    year = us.taxYear,
                    items = listOf(
                        TaxItem("Wages/Salary (from Israeli employer)", "$${fmtUSD(us.wagesAndSalaries)}", false),
                        TaxItem("Short-Term Capital Gains (IBKR)", "$${fmtUSD(us.shortTermCapGains)}", false),
                        TaxItem("Long-Term Capital Gains (IBKR)", "$${fmtUSD(us.longTermCapGains)}", false),
                        TaxItem("Total Income", "$${fmtUSD(us.totalIncome)}", false),
                        TaxItem("Standard Deduction", "-$${fmtUSD(us.standardDeduction)}", false),
                        TaxItem("Taxable Income", "$${fmtUSD(us.taxableIncome)}", false),
                        TaxItem("Ordinary Income Tax", "$${fmtUSD(us.ordinaryIncomeTax)}", false),
                        TaxItem("Capital Gains Tax", "$${fmtUSD(us.capitalGainsTax)}", false),
                        TaxItem("TOTAL US TAX", "$${fmtUSD(us.totalTax)}", true),
                        TaxItem("Foreign Tax Credit (Form 1116)", "-$${fmtUSD(us.foreignTaxCredit)}", false),
                        TaxItem("Federal Tax Withheld (1099-B)", "-$${fmtUSD(us.federalTaxWithheld)}", false),
                    ),
                    result = if (us.refundOrOwed >= 0)
                        TaxResultItem("REFUND", "$${fmtUSD(us.refundOrOwed)}", Color.Green)
                    else
                        TaxResultItem("AMOUNT OWED", "$${fmtUSD(-us.refundOrOwed)}", Color.Red)
                )

                // Compliance flags
                if (us.requiresFBAR || us.requiresForm8938) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null,
                                    tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compliance Filings Required",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (us.requiresFBAR) {
                                Text("• FinCEN 114 (FBAR) required — file by April 15 (Oct 15 auto-extension)",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            if (us.requiresForm8938) {
                                Text("• Form 8938 (FATCA) — attach to your Form 1040",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // ─── Israeli Tax Summary (Form 1301) ──────────────────────────────
            uiState.israeliTaxResult?.let { il ->
                TaxSummaryCard(
                    title = "🇮🇱 Israeli Tax (טופס 1301)",
                    year = il.taxYear,
                    items = listOf(
                        TaxItem("Salary Income (from Form 106)", "₪${fmtNIS(il.salaryIncome)}", false),
                        TaxItem("Foreign Trading Income (IBKR→NIS)", "₪${fmtNIS(il.foreignTradingIncome)}", false),
                        TaxItem("Total Worldwide Income", "₪${fmtNIS(il.totalWorldwideIncome)}", false),
                        TaxItem("Progressive Tax on Salary", "₪${fmtNIS(il.salaryTax)}", false),
                        TaxItem("Capital Gains Tax (25% flat)", "₪${fmtNIS(il.totalCapGainsTax)}", false),
                        TaxItem("Tax Credit Points (נקודות זיכוי)", "-₪${fmtNIS(il.taxCreditsAmount)}", false),
                        TaxItem("Foreign Tax Credit (US→IL)", "-₪${fmtNIS(il.foreignTaxCreditFromUS)}", false),
                        TaxItem("TOTAL ISRAELI TAX", "₪${fmtNIS(il.totalTaxLiability)}", true),
                        TaxItem("Income Tax Withheld (Form 106)", "-₪${fmtNIS(il.incomeTaxWithheld)}", false),
                        TaxItem("Bituach Leumi + Health Insurance", "₪${fmtNIS(il.bituachLeumi)}", false),
                    ),
                    result = if (il.refundOrOwed >= 0)
                        TaxResultItem("החזר מס (REFUND)", "₪${fmtNIS(il.refundOrOwed)}", Color.Green)
                    else
                        TaxResultItem("לתשלום (OWED)", "₪${fmtNIS(-il.refundOrOwed)}", Color.Red)
                )
            }

            // ─── Exchange Rate Note ───────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary)
                    Column {
                        Text("Currency Conversion", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge)
                        Text("Israeli income (salary) converted using IRS average annual rate. " +
                            "IBKR trades converted using Bank of Israel rate on the exact sale date " +
                            "(per Section 207A of the Income Tax Ordinance).",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // ─── Download PDF Buttons ─────────────────────────────────────────
            Text("Generated Tax Forms", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)

            uiState.form1040Path?.let { path ->
                DownloadButton(
                    label = "Form 1040 (US Federal) — DRAFT",
                    icon = Icons.Default.FileDownload,
                    onClick = {
                        openPdf(context, path)
                    }
                )
            }

            uiState.form1301Path?.let { path ->
                DownloadButton(
                    label = "טופס 1301 (Israeli Annual) — טיוטה",
                    icon = Icons.Default.FileDownload,
                    onClick = {
                        openPdf(context, path)
                    }
                )
            }

            // Disclaimer
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                Text(
                    "DISCLAIMER: These are computer-generated drafts. They do not constitute " +
                    "legal or tax advice. Consult a licensed CPA or Enrolled Agent familiar " +
                    "with US-Israel dual taxation (preferably one registered in both countries) " +
                    "before filing. Tax law changes annually.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun TaxSummaryCard(
    title: String,
    year: Int,
    items: List<TaxItem>,
    result: TaxResultItem
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Text("$year", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
                        color = if (item.isBold) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    Text(item.value,
                        fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Result row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(result.label,
                    fontWeight = FontWeight.ExtraBold,
                    color = result.color,
                    style = MaterialTheme.typography.bodyLarge)
                Text(result.value,
                    fontWeight = FontWeight.ExtraBold,
                    color = result.color,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun DownloadButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

data class TaxItem(val label: String, val value: String, val isBold: Boolean)
data class TaxResultItem(val label: String, val value: String, val color: Color)

private fun fmtUSD(amount: Double) = "%,.0f".format(amount)
private fun fmtNIS(amount: Double) = "%,.0f".format(amount)

private fun openPdf(context: android.content.Context, path: String) {
    try {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("ResultsScreen", "Cannot open PDF: ${e.message}")
    }
}
