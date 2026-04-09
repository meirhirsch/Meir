package com.israelitax.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.israelitax.app.ui.viewmodels.TaxUiState
import com.israelitax.app.ui.viewmodels.TaxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: TaxViewModel,
    uiState: TaxUiState,
    onNavigateToReview: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Document pickers
    val form106Launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadForm106(context, it) }
    }

    val form1099Launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadForm1099B(context, it) }
    }

    val form1099CSVLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadForm1099BCSV(context, it) }
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🇮🇱🇺🇸 Israel-America Tax", fontWeight = FontWeight.Bold)
                        Text("Dual Filing Assistant", style = MaterialTheme.typography.labelSmall)
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

            // Step indicator
            StepIndicator(
                steps = listOf("Upload Docs", "Review", "Generate Forms"),
                currentStep = 0
            )

            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        "Upload your Israeli Form 106 and IBKR Form 1099-B. " +
                        "Exchange rates are fetched from the Bank of Israel for each trade date.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Form 106 upload card
            DocumentUploadCard(
                title = "Form 106 — Israeli Salary Statement",
                subtitle = "טופס 106 — אישור שנתי ממעביד",
                description = "Annual salary statement from your Israeli employer. " +
                    "Contains gross income, tax withheld, Bituach Leumi, and pension.",
                icon = Icons.Default.Description,
                isUploaded = uiState.form106 != null,
                isProcessing = uiState.isProcessing && uiState.form106 == null,
                uploadedSummary = uiState.form106?.let {
                    "Gross: ₪${"%,.0f".format(it.grossIncome)}  " +
                    "Tax withheld: ₪${"%,.0f".format(it.incomeTaxWithheld)}"
                },
                onUploadClick = {
                    form106Launcher.launch("*/*")
                },
                accentColor = Color(0xFF1565C0)  // Israeli blue
            )

            // 1099-B upload card — CSV preferred, PDF as fallback
            IBKRUploadCard(
                isUploaded = uiState.form1099B != null,
                isProcessing = uiState.isProcessing && uiState.form1099B == null,
                uploadedSummary = uiState.form1099B?.let {
                    "${it.transactions.size} trades  |  " +
                    "Net G/L: $${"%,.0f".format(it.totalNetGainLoss)}"
                },
                onUploadCSV = { form1099CSVLauncher.launch("*/*") },
                onUploadPDF = { form1099Launcher.launch("*/*") }
            )

            // Processing indicator
            AnimatedVisibility(uiState.isProcessing) {
                Card {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Column {
                            Text("Processing…", fontWeight = FontWeight.SemiBold)
                            Text(uiState.processingMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Supported formats info
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Tips", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Form 106: Upload PDF or photo for OCR scanning",
                        style = MaterialTheme.typography.bodySmall)
                    Text("• 1099-B: CSV export is 100% accurate — use it!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold)
                    Text("• All data is processed on-device. Nothing leaves your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
            }

            // Continue button
            Button(
                onClick = onNavigateToReview,
                enabled = uiState.form106 != null && uiState.form1099B != null && !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Review & Calculate")
            }
        }
    }
}

@Composable
fun DocumentUploadCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    isUploaded: Boolean,
    isProcessing: Boolean,
    uploadedSummary: String?,
    onUploadClick: () -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isUploaded) 2.dp else 1.dp,
                color = if (isUploaded) Color.Green.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isUploaded) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Uploaded",
                        tint = Color.Green, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            uploadedSummary?.let { summary ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        summary,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onUploadClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning…")
                } else {
                    Icon(
                        if (isUploaded) Icons.Default.Refresh else Icons.Default.Upload,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isUploaded) "Re-upload" else "Upload Document")
                }
            }
        }
    }
}

@Composable
fun IBKRUploadCard(
    isUploaded: Boolean,
    isProcessing: Boolean,
    uploadedSummary: String?,
    onUploadCSV: () -> Unit,
    onUploadPDF: () -> Unit
) {
    val accentColor = Color(0xFFB71C1C)  // US red
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isUploaded) 2.dp else 1.dp,
                color = if (isUploaded) Color.Green.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = null,
                        tint = accentColor, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Form 1099-B — IBKR Trading Report",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge)
                    Text("Interactive Brokers — Proceeds from Broker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isUploaded) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Uploaded",
                        tint = Color.Green, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CSV export instructions — prominent box
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Recommended: Export CSV from IBKR",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Text("1. Log in to IBKR Client Portal (web or app)",
                        style = MaterialTheme.typography.bodySmall)
                    Text("2. Reports → Tax → Gain/Loss Summary",
                        style = MaterialTheme.typography.bodySmall)
                    Text("3. Select year 2025, click Download CSV",
                        style = MaterialTheme.typography.bodySmall)
                    Text("4. Tap 'Upload CSV' below and select the file",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            uploadedSummary?.let { summary ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(
                    containerColor = Color.Green.copy(alpha = 0.1f)
                )) {
                    Text(summary, modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary: CSV button
            Button(
                onClick = onUploadCSV,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importing…")
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isUploaded) "Re-import CSV" else "Upload CSV (IBKR Export)")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Secondary: PDF/OCR fallback
            OutlinedButton(
                onClick = onUploadPDF,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Description, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload PDF / Photo (OCR fallback)", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StepIndicator(steps: List<String>, currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (index <= currentStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentStep) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            "${index + 1}",
                            color = if (index <= currentStep) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(step, style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = if (index <= currentStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (index < steps.lastIndex) {
                HorizontalDivider(modifier = Modifier.weight(0.5f).padding(bottom = 20.dp))
            }
        }
    }
}
