package com.israelitax.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.israelitax.app.data.models.FilingStatus
import com.israelitax.app.ui.viewmodels.TaxUiState
import com.israelitax.app.ui.viewmodels.TaxViewModel

@Composable
fun ReviewScreen(
    viewModel: TaxViewModel,
    uiState: TaxUiState,
    onBack: () -> Unit,
    onNavigateToResults: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var taxpayerName by remember { mutableStateOf("") }
    var taxpayerSSN by remember { mutableStateOf("") }
    var filingStatus by remember { mutableStateOf(FilingStatus.SINGLE) }
    var israeliAccountBalance by remember { mutableStateOf("") }
    var useFEIE by remember { mutableStateOf(false) }
    var showFEIEInfo by remember { mutableStateOf(false) }
    var filingStatusExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.usTaxResult) {
        if (uiState.usTaxResult != null) onNavigateToResults()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Review & Calculate") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                currentStep = 1
            )

            // ─── Form 106 Review ──────────────────────────────────────────────
            uiState.form106?.let { form106 ->
                ReviewCard(title = "Form 106 — Israeli Salary (טופס 106)") {
                    DataRow("Employer", form106.employerName.ifBlank { "—" })
                    DataRow("Tax Year", form106.taxYear.toString())
                    DataRow("Gross Income", "₪${"%.0f".format(form106.grossIncome)}")
                    DataRow("Taxable Income", "₪${"%.0f".format(form106.taxableIncome)}")
                    DataRow("Income Tax Withheld", "₪${"%.0f".format(form106.incomeTaxWithheld)}")
                    DataRow("Bituach Leumi (Employee)", "₪${"%.0f".format(form106.bituachLeumiEmployee)}")
                    DataRow("Health Insurance", "₪${"%.0f".format(form106.healthInsurance)}")
                    DataRow("Pension (Employee)", "₪${"%.0f".format(form106.pensionEmployee)}")
                    DataRow("Credit Points", "${"%.2f".format(form106.taxCreditsPoints)}")
                }
            }

            // ─── 1099-B Review ────────────────────────────────────────────────
            uiState.form1099B?.let { form1099 ->
                ReviewCard(title = "Form 1099-B — IBKR Trading") {
                    DataRow("Tax Year", form1099.taxYear.toString())
                    DataRow("Total Transactions", "${form1099.transactions.size}")
                    DataRow("Total Proceeds", "$${"%.0f".format(form1099.totalProceeds)}")
                    DataRow("Total Cost Basis", "$${"%.0f".format(form1099.totalCostBasis)}")
                    DataRow("Short-Term G/L", "$${"%.0f".format(form1099.shortTermGainLoss)}")
                    DataRow("Long-Term G/L", "$${"%.0f".format(form1099.longTermGainLoss)}")
                    DataRow("Net G/L", "$${"%.0f".format(form1099.totalNetGainLoss)}")
                    if (form1099.washSaleLossDisallowed > 0) {
                        DataRow("Wash Sale Disallowed", "$${"%.0f".format(form1099.washSaleLossDisallowed)}")
                    }
                    val ratedTrades = form1099.transactions.count { it.exchangeRateOnSaleDate > 0 }
                    DataRow("Trades with BOI Rate", "$ratedTrades / ${form1099.transactions.size}")
                }
            }

            // ─── Filing Information ───────────────────────────────────────────
            Card {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text("Filing Information", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = taxpayerName,
                        onValueChange = { taxpayerName = it },
                        label = { Text("Full Name (First Last)") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = taxpayerSSN,
                        onValueChange = { taxpayerSSN = it.take(11) },
                        label = { Text("US Social Security Number (XXX-XX-XXXX)") },
                        leadingIcon = { Icon(Icons.Default.Badge, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Filing Status dropdown
                    ExposedDropdownMenuBox(
                        expanded = filingStatusExpanded,
                        onExpandedChange = { filingStatusExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = filingStatus.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("US Filing Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filingStatusExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            leadingIcon = { Icon(Icons.Default.FamilyRestroom, null) }
                        )
                        ExposedDropdownMenu(
                            expanded = filingStatusExpanded,
                            onDismissRequest = { filingStatusExpanded = false }
                        ) {
                            FilingStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.displayName()) },
                                    onClick = {
                                        filingStatus = status
                                        filingStatusExpanded = false
                                        viewModel.updateFilingStatus(status)
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = israeliAccountBalance,
                        onValueChange = { israeliAccountBalance = it },
                        label = { Text("Max Israeli/IBKR Account Balance During Year (USD)") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("Used to determine FBAR ($10K) & FATCA requirements") }
                    )
                }
            }

            // ─── FEIE vs FTC Choice ───────────────────────────────────────────
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Foreign Salary Treatment", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Foreign Earned Income Exclusion (FEIE)",
                                fontWeight = FontWeight.SemiBold)
                            Text("Excludes up to \$126,500 of Israeli salary from US tax. " +
                                "Cannot combine with Foreign Tax Credit on same income.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = useFEIE, onCheckedChange = { useFEIE = it })
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!useFEIE) {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Text(
                                "Using Foreign Tax Credit (FTC): Israeli income tax will be " +
                                "credited against your US tax liability (Form 1116). " +
                                "Better for day traders who want to offset trading gains.",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Text(
                                "Using FEIE (Form 2555): Israeli salary excluded up to \$126,500. " +
                                "Cannot claim FTC on excluded amount. If you used FTC before, " +
                                "switching to FEIE has a 6-year ban (IRS Rev. Rul. 83-82).",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // ─── Calculate Button ─────────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.calculateTaxes(
                        context = context,
                        filingStatus = filingStatus,
                        taxpayerName = taxpayerName,
                        taxpayerSSN = taxpayerSSN,
                        israeliAccountBalance = israeliAccountBalance.toDoubleOrNull() ?: 0.0,
                        useFEIE = useFEIE
                    )
                },
                enabled = !uiState.isProcessing && taxpayerName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.processingMessage)
                } else {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calculate & Generate Forms")
                }
            }
        }
    }
}

@Composable
fun ReviewCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
    }
}

private fun FilingStatus.displayName() = when (this) {
    FilingStatus.SINGLE -> "Single"
    FilingStatus.MARRIED_FILING_JOINTLY -> "Married Filing Jointly"
    FilingStatus.MARRIED_FILING_SEPARATELY -> "Married Filing Separately"
    FilingStatus.HEAD_OF_HOUSEHOLD -> "Head of Household"
}
