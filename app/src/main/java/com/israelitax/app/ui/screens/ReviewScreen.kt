package com.israelitax.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.israelitax.app.data.models.FilingStatus
import com.israelitax.app.ui.viewmodels.TaxUiState
import com.israelitax.app.ui.viewmodels.TaxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: TaxViewModel,
    uiState: TaxUiState,
    onBack: () -> Unit,
    onNavigateToResults: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    var taxpayerName   by remember { mutableStateOf("") }
    var taxpayerSSN    by remember { mutableStateOf("") }
    var filingStatus   by remember { mutableStateOf(FilingStatus.SINGLE) }
    var accountBalance by remember { mutableStateOf("") }
    var useFEIE        by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(uiState.usTaxResult) {
        if (uiState.usTaxResult != null) onNavigateToResults()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Review & Calculate", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepIndicator(listOf("Upload Docs", "Review", "Generate Forms"), currentStep = 1)

            // ── Form 106 summary ─────────────────────────────────────────────
            val f106 = uiState.form106
            SummaryCard(
                title = "טופס 106 — Israeli Salary",
                icon = Icons.Default.Work,
                ok = (f106?.grossIncome ?: 0.0) > 0
            ) {
                if (f106 != null && f106.grossIncome > 0) {
                    SummaryRow("Employer", f106.employerName.ifBlank { "—" })
                    SummaryRow("Tax Year", f106.taxYear.takeIf { it > 0 }?.toString() ?: "2025")
                    SummaryRow("Gross Income", "₪${"%,.0f".format(f106.grossIncome)}")
                    SummaryRow("Income Tax Withheld", "₪${"%,.0f".format(f106.incomeTaxWithheld)}")
                    SummaryRow("Bituach Leumi", "₪${"%,.0f".format(f106.bituachLeumiEmployee)}")
                } else {
                    Text(
                        "OCR could not read Form 106. Go back and try a clearer photo or PDF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ── 1099-B summary ────────────────────────────────────────────────
            val f1099 = uiState.form1099B
            SummaryCard(
                title = "Form 1099-B — IBKR Trades",
                icon = Icons.Default.ShowChart,
                ok = (f1099?.transactions?.size ?: 0) > 0 ||
                     (f1099?.totalNetGainLoss ?: 0.0) != 0.0
            ) {
                if (f1099 != null) {
                    if (f1099.transactions.isNotEmpty()) {
                        SummaryRow("Trades imported", "${f1099.transactions.size}")
                        SummaryRow("Tax Year", f1099.taxYear.takeIf { it > 0 }?.toString() ?: "2025")
                        SummaryRow("Total Proceeds", "$${"%,.2f".format(f1099.totalProceeds)}")
                        SummaryRow("Short-Term G/L", "$${"%+,.2f".format(f1099.shortTermGainLoss)}")
                        SummaryRow("Long-Term G/L", "$${"%+,.2f".format(f1099.longTermGainLoss)}")
                    } else {
                        SummaryRow("Short-Term G/L", "$${"%+,.2f".format(f1099.shortTermGainLoss)}")
                        SummaryRow("Long-Term G/L", "$${"%+,.2f".format(f1099.longTermGainLoss)}")
                    }
                } else {
                    Text(
                        "No 1099-B data. Go back and upload your IBKR CSV export.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ── Filing Information ────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Person, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                        Text("Filing Information", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall)
                    }
                    HorizontalDivider()

                    OutlinedTextField(
                        value = taxpayerName,
                        onValueChange = { taxpayerName = it },
                        label = { Text("Full Name (as on US passport)") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = taxpayerSSN,
                        onValueChange = { taxpayerSSN = it.take(11) },
                        label = { Text("US Social Security Number (XXX-XX-XXXX)") },
                        leadingIcon = { Icon(Icons.Default.Badge, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = filingStatus.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("US Filing Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            leadingIcon = { Icon(Icons.Default.FamilyRestroom, null) }
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            FilingStatus.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.displayName()) },
                                    onClick = {
                                        filingStatus = s; statusExpanded = false
                                        viewModel.updateFilingStatus(s)
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = accountBalance,
                        onValueChange = { accountBalance = it },
                        label = { Text("Max Israeli/IBKR Account Balance During Year (USD)") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        supportingText = { Text("FBAR required if > \$10,000") }
                    )
                }
            }

            // ── FEIE vs FTC ───────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CompareArrows, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                        Text("Foreign Salary Treatment", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall)
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Foreign Earned Income Exclusion (FEIE)",
                                fontWeight = FontWeight.SemiBold)
                            Text("Excludes up to \$130,000 of Israeli salary from US tax (2025).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = useFEIE, onCheckedChange = { useFEIE = it })
                    }
                    val msg = if (!useFEIE)
                        "Foreign Tax Credit (Form 1116): Israeli income tax credited against US liability. Recommended for active traders."
                    else
                        "FEIE (Form 2555): salary excluded. Note: switching from FTC to FEIE has a 6-year moratorium (IRS Rev. Rul. 83-82)."
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (!useFEIE) MaterialTheme.colorScheme.secondaryContainer
                                         else MaterialTheme.colorScheme.tertiaryContainer
                    )) {
                        Text(msg, modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // ── Calculate button ──────────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.calculateTaxes(
                        context = context,
                        filingStatus = filingStatus,
                        taxpayerName = taxpayerName,
                        taxpayerSSN = taxpayerSSN,
                        israeliAccountBalance = accountBalance.toDoubleOrNull() ?: 0.0,
                        useFEIE = useFEIE
                    )
                },
                enabled = !uiState.isProcessing &&
                          taxpayerName.isNotBlank() &&
                          uiState.form106 != null &&
                          uiState.form1099B != null,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.processingMessage, maxLines = 1)
                } else {
                    Icon(Icons.Default.Calculate, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Calculate & Generate Tax Forms")
                }
            }
        }
    }
}

// ── Composable helpers ────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    title: String,
    icon: ImageVector,
    ok: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
                Text(title, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                Icon(
                    if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (ok) Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.size(20.dp)
                )
            }
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold)
    }
}

private fun FilingStatus.displayName() = when (this) {
    FilingStatus.SINGLE                    -> "Single"
    FilingStatus.MARRIED_FILING_JOINTLY    -> "Married Filing Jointly"
    FilingStatus.MARRIED_FILING_SEPARATELY -> "Married Filing Separately"
    FilingStatus.HEAD_OF_HOUSEHOLD         -> "Head of Household"
}
