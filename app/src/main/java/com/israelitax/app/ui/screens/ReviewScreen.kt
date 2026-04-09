package com.israelitax.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.israelitax.app.data.models.FilingStatus
import com.israelitax.app.data.models.Form106Data
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

    // ── Form 106 manual entry fields (pre-filled from OCR) ───────────────────
    val f106 = uiState.form106
    val ocrOk = (f106?.grossIncome ?: 0.0) > 0
    var editExpanded by remember { mutableStateOf(!ocrOk) }   // auto-open if OCR failed

    var f106Year      by remember { mutableStateOf(f106?.taxYear?.takeIf { it > 0 }?.toString() ?: "2025") }
    var f106Employer  by remember { mutableStateOf(f106?.employerName ?: "") }
    var f106Gross     by remember { mutableStateOf(f106?.grossIncome?.toAmtStr() ?: "") }
    var f106TaxWhd    by remember { mutableStateOf(f106?.incomeTaxWithheld?.toAmtStr() ?: "") }
    var f106BL        by remember { mutableStateOf(f106?.bituachLeumiEmployee?.toAmtStr() ?: "") }
    var f106Health    by remember { mutableStateOf(f106?.healthInsurance?.toAmtStr() ?: "") }
    var f106Pension   by remember { mutableStateOf(f106?.pensionEmployee?.toAmtStr() ?: "") }
    var f106StudyFund by remember { mutableStateOf(f106?.studyFund?.toAmtStr() ?: "") }
    var f106CreditPts by remember { mutableStateOf(f106?.taxCreditsPoints?.takeIf { it > 0 }?.toString() ?: "2.25") }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(uiState.usTaxResult) {
        if (uiState.usTaxResult != null) onNavigateToResults()
    }

    // Effective Form 106 — uses manual fields when any non-zero value entered
    fun effectiveForm106(): Form106Data {
        val gross = f106Gross.toD()
        return (f106 ?: Form106Data()).copy(
            taxYear = f106Year.toIntOrNull() ?: 2025,
            employerName = f106Employer,
            grossIncome = if (gross > 0) gross else (f106?.grossIncome ?: 0.0),
            taxableIncome = if (gross > 0) gross else (f106?.taxableIncome ?: 0.0),
            incomeTaxWithheld = f106TaxWhd.toD().takeIf { it > 0 } ?: (f106?.incomeTaxWithheld ?: 0.0),
            bituachLeumiEmployee = f106BL.toD().takeIf { it > 0 } ?: (f106?.bituachLeumiEmployee ?: 0.0),
            healthInsurance = f106Health.toD().takeIf { it > 0 } ?: (f106?.healthInsurance ?: 0.0),
            pensionEmployee = f106Pension.toD().takeIf { it > 0 } ?: (f106?.pensionEmployee ?: 0.0),
            studyFund = f106StudyFund.toD().takeIf { it > 0 } ?: (f106?.studyFund ?: 0.0),
            taxCreditsPoints = f106CreditPts.toDoubleOrNull()?.takeIf { it > 0 } ?: (f106?.taxCreditsPoints?.takeIf { it > 0 } ?: 2.25)
        )
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

            // ── Form 106 card ─────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Header row
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Work, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                        Text("טופס 106 — Israeli Salary",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f))
                        Icon(
                            if (ocrOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (ocrOk) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider()

                    // OCR summary (when it worked)
                    if (ocrOk && !editExpanded) {
                        SummaryRow("Employer", f106?.employerName?.ifBlank { "—" } ?: "—")
                        SummaryRow("Tax Year", f106?.taxYear?.takeIf { it > 0 }?.toString() ?: "2025")
                        SummaryRow("Gross Income", "₪${"%,.0f".format(f106?.grossIncome ?: 0.0)}")
                        SummaryRow("Income Tax Withheld", "₪${"%,.0f".format(f106?.incomeTaxWithheld ?: 0.0)}")
                        SummaryRow("Bituach Leumi", "₪${"%,.0f".format(f106?.bituachLeumiEmployee ?: 0.0)}")
                    }

                    // Expand/collapse button
                    TextButton(
                        onClick = { editExpanded = !editExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (editExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when {
                                !ocrOk       -> "Enter Form 106 values manually"
                                editExpanded -> "Hide manual entry"
                                else         -> "Correct scanned values"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Manual entry fields
                    AnimatedVisibility(visible = editExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!ocrOk) {
                                Card(colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF3E0)
                                )) {
                                    Row(modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.Info, null,
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(16.dp))
                                        Text(
                                            "OCR could not read your Form 106. " +
                                            "Enter the values from your paper form below. " +
                                            "All amounts in ₪ (Israeli Shekel).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFBF360C)
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                F106Field("Tax Year", f106Year, { f106Year = it },
                                    KeyboardType.Number, Modifier.weight(1f))
                                F106Field("Employer", f106Employer, { f106Employer = it },
                                    KeyboardType.Text, Modifier.weight(2f))
                            }
                            F106Field(
                                label = "Gross Income ₪  (Code 158 — הכנסה ברוטו)",
                                value = f106Gross, onValue = { f106Gross = it },
                                warn = f106Gross.toD() == 0.0
                            )
                            F106Field(
                                label = "Income Tax Withheld ₪  (Code 042 — מס הכנסה)",
                                value = f106TaxWhd, onValue = { f106TaxWhd = it }
                            )
                            F106Field(
                                label = "Bituach Leumi – Employee ₪  (Code 045 — ביטוח לאומי)",
                                value = f106BL, onValue = { f106BL = it }
                            )
                            F106Field(
                                label = "Health Insurance ₪  (Code 047 — ביטוח בריאות)",
                                value = f106Health, onValue = { f106Health = it }
                            )
                            F106Field(
                                label = "Pension – Employee ₪  (Code 043 — פנסיה עובד)",
                                value = f106Pension, onValue = { f106Pension = it }
                            )
                            F106Field(
                                label = "Study Fund ₪  (Code 048 — קרן השתלמות)",
                                value = f106StudyFund, onValue = { f106StudyFund = it }
                            )
                            F106Field(
                                label = "Credit Points  (נקודות זיכוי — 2.25 for single resident)",
                                value = f106CreditPts, onValue = { f106CreditPts = it },
                                keyboard = KeyboardType.Decimal
                            )
                        }
                    }
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            val grossAvailable = f106Gross.toD() > 0 || (f106?.grossIncome ?: 0.0) > 0
            Button(
                onClick = {
                    viewModel.updateForm106(effectiveForm106())
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
                          grossAvailable &&
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

@Composable
private fun F106Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Decimal,
    modifier: Modifier = Modifier.fillMaxWidth(),
    warn: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
        label = { Text(label, maxLines = 2) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (warn) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (warn) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                   else MaterialTheme.colorScheme.outline,
        ),
        trailingIcon = if (warn) ({
            Icon(Icons.Default.Warning, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp))
        }) else null
    )
}

private fun Double.toAmtStr() = if (this == 0.0) "" else "%.0f".format(this)
private fun String.toD() = replace(",", "").toDoubleOrNull() ?: 0.0

private fun FilingStatus.displayName() = when (this) {
    FilingStatus.SINGLE                    -> "Single"
    FilingStatus.MARRIED_FILING_JOINTLY    -> "Married Filing Jointly"
    FilingStatus.MARRIED_FILING_SEPARATELY -> "Married Filing Separately"
    FilingStatus.HEAD_OF_HOUSEHOLD         -> "Head of Household"
}
