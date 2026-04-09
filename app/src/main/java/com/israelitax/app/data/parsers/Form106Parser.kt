package com.israelitax.app.data.parsers

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.hebrew.HebrewTextRecognizerOptions
import com.israelitax.app.data.models.Form106Data
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Parses Israeli Form 106 (טופס 106 – אישור שנתי ממעביד) using ML Kit Hebrew OCR.
 *
 * The 106 is structured with field codes (קוד) followed by amounts (סכום).
 * Key field codes we extract:
 *   158 – ברוטו לצורכי מס (gross taxable income)
 *   172 – הכנסה חייבת (taxable income)
 *   161 – הכנסה פטורה (tax-exempt income)
 *   042 – מס הכנסה שנוכה (income tax withheld)
 *   045 – ביטוח לאומי חלק עובד (Bituach Leumi – employee share)
 *   046 – ביטוח לאומי חלק מעסיק (Bituach Leumi – employer share)
 *   047 – ביטוח בריאות (health insurance)
 *   043 – פנסיה עובד (employee pension)
 *   044 – פנסיה מעסיק (employer pension)
 *   048 – קרן השתלמות (study fund)
 */
class Form106Parser {

    companion object {
        private const val TAG = "Form106Parser"
    }

    private val recognizer = TextRecognition.getClient(HebrewTextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Main entry: scan a bitmap of the Form 106 and extract all key fields.
     */
    suspend fun parse(bitmap: Bitmap): Result<Form106Data> {
        return try {
            val rawText = recognizeText(bitmap)
            Log.d(TAG, "OCR raw text:\n$rawText")
            val data = extractFields(rawText)
            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Parse failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun recognizeText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    private fun extractFields(text: String): Form106Data {
        val lines = text.lines()

        // Build a map of code → amount by scanning each line for patterns like:
        //   "158    45,320.00" or "קוד 158 סכום 45,320"
        val fieldMap = mutableMapOf<String, Double>()
        val codeAmountRegex = Regex("""(\d{3})\s+([0-9,]+(?:\.[0-9]{1,2})?)""")

        for (line in lines) {
            val matches = codeAmountRegex.findAll(line)
            for (match in matches) {
                val code = match.groupValues[1]
                val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: continue
                // Only keep the first occurrence of each code (some forms repeat)
                if (!fieldMap.containsKey(code)) {
                    fieldMap[code] = amount
                }
            }
        }

        Log.d(TAG, "Extracted field map: $fieldMap")

        // Extract employer/employee metadata
        val taxYear = extractTaxYear(text)
        val employerName = extractEntityName(text, patterns = listOf("שם המעביד", "מעביד"))
        val employerId = extractIdentifier(text, patterns = listOf("ח.פ", "עוסק מורשה", "תיק ניכויים"))
        val employeeName = extractEntityName(text, patterns = listOf("שם העובד", "עובד"))
        val employeeId = extractIdentifier(text, patterns = listOf("ת.ז", "מספר זהות"))

        // Tax credits: look for "נקודות זיכוי" near a number
        val creditPoints = extractCreditPoints(text)
        val creditAmount = fieldMap.getOrDefault("048", 0.0) // some forms put credit amount at 048

        return Form106Data(
            taxYear = taxYear,
            employerName = employerName,
            employerId = employerId,
            employeeName = employeeName,
            employeeId = employeeId,
            grossIncome = fieldMap.getOrDefault("158", 0.0),
            taxableIncome = fieldMap.getOrDefault("172", 0.0),
            taxExemptIncome = fieldMap.getOrDefault("161", 0.0),
            incomeTaxWithheld = fieldMap.getOrDefault("042", 0.0),
            bituachLeumiEmployee = fieldMap.getOrDefault("045", 0.0),
            bituachLeumiEmployer = fieldMap.getOrDefault("046", 0.0),
            healthInsurance = fieldMap.getOrDefault("047", 0.0),
            pensionEmployee = fieldMap.getOrDefault("043", 0.0),
            pensionEmployer = fieldMap.getOrDefault("044", 0.0),
            studyFund = extractStudyFund(fieldMap),
            taxCreditsPoints = creditPoints,
            taxCreditsAmount = creditAmount
        )
    }

    private fun extractTaxYear(text: String): Int {
        // Look for 4-digit years like "2024", "שנת 2024"
        val yearRegex = Regex("""שנת?\s*(\d{4})""")
        yearRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        // Fallback: any standalone 4-digit year between 2018-2030
        val standaloneYear = Regex("""(?<!\d)(20[12]\d)(?!\d)""")
        standaloneYear.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        return 2024 // default
    }

    private fun extractEntityName(text: String, patterns: List<String>): String {
        for (pattern in patterns) {
            val regex = Regex("""$pattern\s*[:\-]?\s*(.{2,40})""")
            regex.find(text)?.groupValues?.get(1)?.trim()?.let { name ->
                if (name.isNotBlank()) return name.take(60)
            }
        }
        return ""
    }

    private fun extractIdentifier(text: String, patterns: List<String>): String {
        for (pattern in patterns) {
            val regex = Regex("""$pattern\s*[:\-]?\s*(\d[\d\-]{5,12})""")
            regex.find(text)?.groupValues?.get(1)?.trim()?.let { return it }
        }
        return ""
    }

    private fun extractCreditPoints(text: String): Double {
        val regex = Regex("""נקודות זיכוי\s*[:\-]?\s*(\d+(?:\.\d{1,2})?)""")
        return regex.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun extractStudyFund(fieldMap: Map<String, Double>): Double {
        // Study fund (קרן השתלמות) can appear at different codes depending on employer
        // Common codes: 048, 049
        return fieldMap.getOrDefault("049", fieldMap.getOrDefault("048", 0.0))
    }
}
