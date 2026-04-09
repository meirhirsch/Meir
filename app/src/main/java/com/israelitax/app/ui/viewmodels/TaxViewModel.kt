package com.israelitax.app.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.israelitax.app.data.models.FilingStatus
import com.israelitax.app.data.models.Form106Data
import com.israelitax.app.data.models.Form1099BData
import com.israelitax.app.data.models.IsraeliTaxResult
import com.israelitax.app.data.models.TaxSession
import com.israelitax.app.data.models.USTaxResult
import com.israelitax.app.data.parsers.Form106Parser
import com.israelitax.app.data.parsers.Form1099BParser
import com.israelitax.app.data.repository.ExchangeRateRepository
import com.israelitax.app.output.Form1040Generator
import com.israelitax.app.output.Form1301Generator
import com.israelitax.app.tax.israel.IsraeliTaxCalculator
import com.israelitax.app.tax.us.USTaxCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class TaxViewModel : ViewModel() {

    companion object {
        private const val TAG = "TaxViewModel"
    }

    // ─── Dependencies ─────────────────────────────────────────────────────────
    private val exchangeRateRepo = ExchangeRateRepository()
    private val form106Parser = Form106Parser()
    private val form1099BParser = Form1099BParser()
    private val israeliCalculator = IsraeliTaxCalculator(exchangeRateRepo)
    private val usCalculator = USTaxCalculator(exchangeRateRepo)

    // ─── UI State ─────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(TaxUiState())
    val uiState: StateFlow<TaxUiState> = _uiState.asStateFlow()

    // ─── Upload Form 106 ──────────────────────────────────────────────────────
    fun uploadForm106(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingMessage = "Scanning Form 106 (Hebrew OCR)…",
                error = null
            )
            try {
                val bitmaps = uriToBitmaps(context, uri)
                if (bitmaps.isEmpty()) throw Exception("Could not read document")

                val result = form106Parser.parse(bitmaps.first())
                if (result.isSuccess) {
                    val form106 = result.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        form106 = form106,
                        isProcessing = false,
                        processingMessage = "Form 106 scanned successfully"
                    )
                    Log.i(TAG, "Form 106 parsed: gross=${form106.grossIncome}")
                } else {
                    throw result.exceptionOrNull() ?: Exception("OCR failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Form 106 upload failed: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Failed to scan Form 106: ${e.message}"
                )
            }
        }
    }

    // ─── Upload 1099-B ────────────────────────────────────────────────────────
    fun uploadForm1099B(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingMessage = "Scanning Form 1099-B (IBKR)…",
                error = null
            )
            try {
                val bitmaps = uriToBitmaps(context, uri)
                if (bitmaps.isEmpty()) throw Exception("Could not read document")

                _uiState.value = _uiState.value.copy(
                    processingMessage = "Fetching per-trade exchange rates from Bank of Israel…"
                )

                val result = form1099BParser.parseAllPages(bitmaps)
                if (result.isSuccess) {
                    val form1099 = result.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        form1099B = form1099,
                        isProcessing = false,
                        processingMessage = "Form 1099-B scanned: ${form1099.transactions.size} transactions"
                    )
                    Log.i(TAG, "1099-B parsed: ${form1099.transactions.size} trades")
                } else {
                    throw result.exceptionOrNull() ?: Exception("OCR failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "1099-B upload failed: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Failed to scan Form 1099-B: ${e.message}"
                )
            }
        }
    }

    // ─── Calculate Taxes ──────────────────────────────────────────────────────
    fun calculateTaxes(
        context: Context,
        filingStatus: FilingStatus,
        taxpayerName: String,
        taxpayerSSN: String,
        israeliAccountBalance: Double,
        useFEIE: Boolean = false
    ) {
        val form106 = _uiState.value.form106 ?: run {
            _uiState.value = _uiState.value.copy(error = "Please upload Form 106 first")
            return
        }
        val form1099B = _uiState.value.form1099B ?: run {
            _uiState.value = _uiState.value.copy(error = "Please upload Form 1099-B first")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingMessage = "Fetching Bank of Israel exchange rates…",
                error = null
            )

            try {
                // Step 1: Fetch the BOI annual average for the tax year.
                // This is used for salary conversion (Form 106 gives annual totals, not
                // individual payroll dates). For every individual trade, the exact BOI
                // rate on the sale date is used instead (see enrichWithExchangeRates).
                val taxYear = form106.taxYear.takeIf { it > 0 }
                    ?: form1099B.taxYear.takeIf { it > 0 }
                    ?: 2025
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Fetching BOI $taxYear annual average rate…"
                )
                val boiAnnualAvg = exchangeRateRepo.getAnnualAverageRate(taxYear)
                    .takeIf { it > 0 } ?: 3.70   // network-failure fallback only
                Log.i(TAG, "BOI $taxYear annual average: $boiAnnualAvg NIS/USD")

                // Step 2: Calculate Israeli taxes
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Computing Israeli tax (Form 1301) with per-trade BOI rates…"
                )
                val israeliResult = israeliCalculator.calculate(
                    form106 = form106,
                    form1099B = form1099B,
                    averageRateUsdNis = boiAnnualAvg,
                    usTaxPaidOnIsraeliIncome = 0.0  // Will be updated after US calc
                )

                // Step 3: Calculate US taxes
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Computing US federal tax (Form 1040)…"
                )
                val usResult = usCalculator.calculate(
                    form106 = form106,
                    form1099B = form1099B,
                    filingStatus = filingStatus,
                    israeliTaxPaid = form106.incomeTaxWithheld / boiAnnualAvg,
                    useFEIE = useFEIE,
                    israeliAccountBalance = israeliAccountBalance,
                    salaryConversionRate = boiAnnualAvg
                )

                // Step 4: Re-calculate Israeli with US Foreign Tax Credit (treaty relief)
                val israeliResultFinal = israeliCalculator.calculate(
                    form106 = form106,
                    form1099B = form1099B,
                    averageRateUsdNis = boiAnnualAvg,
                    usTaxPaidOnIsraeliIncome = usResult.foreignTaxCredit * boiAnnualAvg
                )

                // Step 4: Generate PDFs
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Generating Form 1040 PDF…"
                )
                val outputDir = File(context.filesDir, "tax_output")
                val form1040Generator = Form1040Generator(context)
                val form1040File = form1040Generator.generate(
                    form106 = form106,
                    form1099B = form1099B,
                    result = usResult,
                    taxpayerName = taxpayerName,
                    taxpayerSSN = taxpayerSSN,
                    outputDir = outputDir
                )

                _uiState.value = _uiState.value.copy(
                    processingMessage = "Generating Form 1301 PDF…"
                )
                val form1301Generator = Form1301Generator(context)
                val form1301File = form1301Generator.generate(
                    form106 = form106,
                    form1099B = form1099B,
                    result = israeliResultFinal,
                    taxpayerName = taxpayerName,
                    taxpayerId = form106.employeeId,
                    outputDir = outputDir
                )

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processingMessage = "Done! Review your tax summaries below.",
                    usTaxResult = usResult,
                    israeliTaxResult = israeliResultFinal,
                    form1040Path = form1040File.absolutePath,
                    form1301Path = form1301File.absolutePath,
                    filingStatus = filingStatus,
                    taxpayerName = taxpayerName,
                    boiAnnualAvgRate = boiAnnualAvg
                )

                Log.i(TAG, "Tax calculation complete. US owed/refund: ${usResult.refundOrOwed}")
                Log.i(TAG, "Israeli owed/refund: ${israeliResultFinal.refundOrOwed}")

            } catch (e: Exception) {
                Log.e(TAG, "Calculation failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Calculation failed: ${e.message}"
                )
            }
        }
    }

    fun updateFilingStatus(status: FilingStatus) {
        _uiState.value = _uiState.value.copy(filingStatus = status)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSession() {
        _uiState.value = TaxUiState()
    }

    /**
     * Converts a document URI to a list of bitmaps.
     * Supports PDF (each page → bitmap) and image files.
     */
    private fun uriToBitmaps(context: Context, uri: Uri): List<Bitmap> {
        val mimeType = context.contentResolver.getType(uri)
        return if (mimeType == "application/pdf") {
            pdfToBitmaps(context, uri)
        } else {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return emptyList()
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            listOfNotNull(bitmap)
        }
    }

    private fun pdfToBitmaps(context: Context, uri: Uri): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            // Render at 2x density for better OCR accuracy
                            val scale = 2
                            val bitmap = Bitmap.createBitmap(
                                page.width * scale,
                                page.height * scale,
                                Bitmap.Config.ARGB_8888
                            )
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bitmap)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDF rendering failed: ${e.message}")
        }
        return bitmaps
    }
}

data class TaxUiState(
    val form106: Form106Data? = null,
    val form1099B: Form1099BData? = null,
    val usTaxResult: USTaxResult? = null,
    val israeliTaxResult: IsraeliTaxResult? = null,
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    val error: String? = null,
    val form1040Path: String? = null,
    val form1301Path: String? = null,
    val filingStatus: FilingStatus = FilingStatus.SINGLE,
    val taxpayerName: String = "",
    /** BOI annual average USD/NIS rate fetched live for the tax year. Used for salary
     *  conversion on Form 1040 and as NIS fallback for any trade missing a BOI date rate. */
    val boiAnnualAvgRate: Double = 0.0
)
