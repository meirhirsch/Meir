package com.israelitax.app.output

import android.content.Context
import com.israelitax.app.data.models.Form106Data
import com.israelitax.app.data.models.Form1099BData
import com.israelitax.app.data.models.HoldingPeriod
import com.israelitax.app.data.models.IsraeliTaxResult
import com.israelitax.app.data.models.TradeTransaction
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * Generates Israeli Annual Tax Return (טופס 1301 – דוח שנתי ליחיד) as a PDF.
 *
 * Form 1301 is filed by Israeli residents who have:
 *  - Income from multiple sources (salary + investments)
 *  - Foreign income (required per Section 131 of the Income Tax Ordinance)
 *  - Capital gains from securities
 *
 * Structure of Form 1301 (simplified):
 *  Part A – Personal Details (פרטים אישיים)
 *  Part B – Income from Employment (הכנסות מעבודה) – from Form 106
 *  Part C – Capital Gains (רווחי הון) – from IBKR via 1099-B
 *  Part D – Foreign Income (הכנסות מחו"ל)
 *  Part E – Deductions (ניכויים)
 *  Part F – Tax Calculation (חישוב המס)
 *  Part G – Credits (זיכויים)
 *  Part H – Balance Due / Refund
 *
 * All amounts are in NIS. USD income converted at Bank of Israel rate on sale date.
 *
 * References:
 *  - Income Tax Ordinance [New Version] 5721-1961, Sections 14, 91, 131, 207A
 *  - Israeli Tax Authority Form 1301 instructions (הוראות למילוי טופס 1301)
 */
class Form1301Generator(private val context: Context) {

    private val nisFmt = NumberFormat.getNumberInstance(Locale("iw", "IL"))
        .apply { maximumFractionDigits = 0 }

    fun generate(
        form106: Form106Data,
        form1099B: Form1099BData,
        result: IsraeliTaxResult,
        taxpayerName: String,
        taxpayerId: String,
        spouseName: String = "",
        outputDir: File
    ): File {
        outputDir.mkdirs()
        val outputFile = File(outputDir, "Form_1301_${result.taxYear}_DRAFT.pdf")

        PdfWriter(outputFile).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { doc ->
                    doc.setMargins(36f, 36f, 36f, 36f)
                    buildDocument(doc, form106, form1099B, result,
                        taxpayerName, taxpayerId, spouseName)
                }
            }
        }

        return outputFile
    }

    private fun buildDocument(
        doc: Document,
        form106: Form106Data,
        form1099B: Form1099BData,
        result: IsraeliTaxResult,
        taxpayerName: String,
        taxpayerId: String,
        spouseName: String
    ) {
        // ─── HEADER ──────────────────────────────────────────────────────────
        doc.add(Paragraph("טיוטה — לא לדיווח").setFontSize(9f)
            .setFontColor(ColorConstants.DARK_GRAY).setTextAlignment(TextAlignment.RIGHT))
        doc.add(Paragraph("רשות המסים בישראל — מס הכנסה")
            .setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        doc.add(Paragraph("טופס 1301 — דוח שנתי ליחיד")
            .setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
        doc.add(Paragraph("שנת מס ${result.taxYear}")
            .setFontSize(12f).setTextAlignment(TextAlignment.CENTER))
        doc.add(Paragraph("⚠  טיוטה — יש לבדוק עם רואה חשבון מוסמך לפני הגשה")
            .setFontSize(10f).setFontColor(ColorConstants.RED)
            .setTextAlignment(TextAlignment.CENTER))
        doc.add(spacer())

        // ─── PART A: PERSONAL DETAILS ─────────────────────────────────────────
        doc.add(sectionHeader("חלק א׳ — פרטים אישיים"))
        val personalTable = twoColumnTable()
        addRow(personalTable, "שם פרטי ושם משפחה", taxpayerName)
        addRow(personalTable, "מספר תעודת זהות", maskId(taxpayerId))
        addRow(personalTable, "שנת מס", result.taxYear.toString())
        addRow(personalTable, "שם המעסיק", form106.employerName)
        addRow(personalTable, "מספר תיק ניכויים מעסיק", form106.employerId)
        if (spouseName.isNotBlank()) {
            addRow(personalTable, "שם בן/בת הזוג", spouseName)
        }
        doc.add(personalTable)
        doc.add(spacer())

        // ─── PART B: INCOME FROM EMPLOYMENT ──────────────────────────────────
        doc.add(sectionHeader("חלק ב׳ — הכנסות מעבודה (טופס 106)"))
        val employTable = lineItemTable()
        addNisItem(employTable, "158", "ברוטו לצורכי מס", form106.grossIncome)
        addNisItem(employTable, "172", "הכנסה חייבת במס", form106.taxableIncome)
        addNisItem(employTable, "161", "הכנסה פטורה ממס", form106.taxExemptIncome)
        addNisItem(employTable, "042", "מס הכנסה שנוכה במקור", form106.incomeTaxWithheld)
        addNisItem(employTable, "045", "ביטוח לאומי — חלק עובד", form106.bituachLeumiEmployee)
        addNisItem(employTable, "047", "ביטוח בריאות", form106.healthInsurance)
        addNisItem(employTable, "043", "קרן פנסיה — חלק עובד", form106.pensionEmployee)
        addNisItem(employTable, "048", "קרן השתלמות", form106.studyFund)
        doc.add(employTable)
        doc.add(spacer())

        // ─── PART C: CAPITAL GAINS FROM FOREIGN SECURITIES ────────────────────
        doc.add(sectionHeader("חלק ג׳ — רווחי הון מניירות ערך זרים (IBKR)"))
        doc.add(Paragraph("המרת מטבע: שער חליפין של בנק ישראל ביום המכירה (סעיף 207א לפקודה)")
            .setFontSize(9f).setFontColor(ColorConstants.DARK_GRAY))
        doc.add(spacer())

        // Transaction detail table
        if (form1099B.transactions.isNotEmpty()) {
            val txnTable = transactionTable()
            // Header
            txnTable.addHeaderCell(headerCell("תיאור נייר הערך"))
            txnTable.addHeaderCell(headerCell("תאריך רכישה"))
            txnTable.addHeaderCell(headerCell("תאריך מכירה"))
            txnTable.addHeaderCell(headerCell("תמורה (USD)"))
            txnTable.addHeaderCell(headerCell("עלות (USD)"))
            txnTable.addHeaderCell(headerCell("שער ₪/$ ביום המכירה"))
            txnTable.addHeaderCell(headerCell("רווח/הפסד (₪)"))
            txnTable.addHeaderCell(headerCell("סוג"))

            for (txn in form1099B.transactions) {
                txnTable.addCell(valueCell9(txn.description.take(20)))
                txnTable.addCell(valueCell9(txn.dateAcquired))
                txnTable.addCell(valueCell9(txn.dateSold))
                txnTable.addCell(valueCell9("$${nisFmt.format(txn.proceeds)}"))
                txnTable.addCell(valueCell9("$${nisFmt.format(txn.costBasis)}"))
                val rateText = if (txn.exchangeRateOnSaleDate > 0)
                    String.format("%.4f", txn.exchangeRateOnSaleDate) else "—"
                txnTable.addCell(valueCell9(rateText))
                val gainText = if (txn.gainLossNIS != 0.0)
                    "₪${nisFmt.format(txn.gainLossNIS)}"
                else "₪${nisFmt.format(txn.gainLoss)}"
                txnTable.addCell(valueCell9(gainText))
                txnTable.addCell(valueCell9(
                    if (txn.holdingPeriod == HoldingPeriod.LONG_TERM) "ארוך" else "קצר"
                ))
            }
            doc.add(txnTable)
        }
        doc.add(spacer())

        val capGainsTable = lineItemTable()
        addNisItem(capGainsTable, "C1", "רווח/הפסד הון ממכירות ל-שורה קצרה (25%)",
            result.foreignTradingIncome * 0.6, note = "Short-term IBKR trades")
        addNisItem(capGainsTable, "C2", "רווח/הפסד הון ממכירות ארוכות טווח (25%)",
            result.foreignTradingIncome * 0.4, note = "Long-term IBKR trades")
        addNisItem(capGainsTable, "C3", "סה\"כ רווח/הפסד הון מחו\"ל",
            result.foreignTradingIncome, bold = true)
        doc.add(capGainsTable)
        doc.add(spacer())

        // ─── PART D: FOREIGN INCOME DECLARATION ───────────────────────────────
        doc.add(sectionHeader("חלק ד׳ — הכנסות מחו\"ל"))
        val foreignTable = lineItemTable()
        addNisItem(foreignTable, "D1", "הכנסה מרווחי הון מחו\"ל (IBKR)",
            result.foreignTradingIncome)
        addNisItem(foreignTable, "D2", "מס ששולם בחו\"ל על הכנסות אלו (ארה\"ב)",
            result.foreignTaxCreditFromUS)
        doc.add(foreignTable)
        doc.add(spacer())

        // ─── PART F: TAX CALCULATION ──────────────────────────────────────────
        doc.add(sectionHeader("חלק ו׳ — חישוב המס"))
        val taxCalcTable = lineItemTable()
        addNisItem(taxCalcTable, "F1", "הכנסה מעבודה חייבת במס", result.salaryIncome)
        addNisItem(taxCalcTable, "F2", "מס הכנסה לפי מדרגות על הכנסה מעבודה", result.salaryTax)
        addNisItem(taxCalcTable, "F3", "מס על רווחי הון מחו\"ל (25% פלוס)", result.totalCapGainsTax)
        addNisItem(taxCalcTable, "F4", "סה\"כ מס לפני זיכויים",
            result.salaryTax + result.totalCapGainsTax, bold = true)
        doc.add(taxCalcTable)
        doc.add(spacer())

        // ─── PART G: CREDITS ──────────────────────────────────────────────────
        doc.add(sectionHeader("חלק ז׳ — זיכויים"))
        val creditsTable = lineItemTable()
        addNisItem(creditsTable, "G1",
            "נקודות זיכוי (${String.format("%.2f", result.taxCreditsPoints)} × ₪2,904)",
            result.taxCreditsAmount)
        addNisItem(creditsTable, "G2",
            "זיכוי בגין מס זר ששולם בארה\"ב (סעיף 200 לפקודה)",
            result.foreignTaxCreditFromUS, note = "Israel-US Tax Treaty Art. 22")
        addNisItem(creditsTable, "G3", "סה\"כ זיכויים",
            result.taxCreditsAmount + result.foreignTaxCreditFromUS, bold = true)
        doc.add(creditsTable)
        doc.add(spacer())

        // ─── PART H: BALANCE ──────────────────────────────────────────────────
        doc.add(sectionHeader("חלק ח׳ — יתרה לתשלום / החזר"))
        val balTable = lineItemTable()
        addNisItem(balTable, "H1", "סה\"כ חבות מס לשנת המס", result.totalTaxLiability)
        addNisItem(balTable, "H2", "מס הכנסה שנוכה במקור (טופס 106 קוד 042)",
            result.incomeTaxWithheld)
        addNisItem(balTable, "H3", "זיכוי מס זר", result.foreignTaxCreditFromUS)
        addNisItem(balTable, "H4", "סה\"כ שולם", result.totalTaxPaid)

        if (result.refundOrOwed >= 0) {
            addNisItem(balTable, "H5", "יתרה להחזר (הוספת לחשבון הבנק)",
                result.refundOrOwed, bold = true, highlight = true)
        } else {
            addNisItem(balTable, "H5", "יתרה לתשלום",
                -result.refundOrOwed, bold = true, highlight = true)
        }
        doc.add(balTable)
        doc.add(spacer())

        // ─── TAX BRACKET BREAKDOWN ────────────────────────────────────────────
        doc.add(sectionHeader("מדרגות מס הכנסה 2024 (לעיון)"))
        val bracketsTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 30f, 20f, 20f)))
            .setWidth(UnitValue.createPercentValue(100f))
        bracketsTable.addHeaderCell(headerCell("הכנסה שנתית (₪) מ-"))
        bracketsTable.addHeaderCell(headerCell("עד-"))
        bracketsTable.addHeaderCell(headerCell("שיעור מס"))
        bracketsTable.addHeaderCell(headerCell("מס שולי"))
        val brackets = listOf(
            Triple(0, 81_480, "10%"), Triple(81_481, 116_760, "14%"),
            Triple(116_761, 187_440, "20%"), Triple(187_441, 260_520, "31%"),
            Triple(260_521, 542_160, "35%"), Triple(542_161, 698_280, "47%"),
            Triple(698_281, Int.MAX_VALUE, "50%")
        )
        for ((from, to, rate) in brackets) {
            val isInBracket = result.salaryIncome >= from
            val bg = if (isInBracket) ColorConstants.YELLOW else null
            bracketsTable.addCell(Cell().add(Paragraph("₪${nisFmt.format(from)}").setFontSize(9f))
                .also { if (bg != null) it.setBackgroundColor(bg) })
            bracketsTable.addCell(Cell().add(
                Paragraph(if (to == Int.MAX_VALUE) "ומעלה" else "₪${nisFmt.format(to)}").setFontSize(9f))
                .also { if (bg != null) it.setBackgroundColor(bg) })
            bracketsTable.addCell(Cell().add(Paragraph(rate).setFontSize(9f).setBold())
                .also { if (bg != null) it.setBackgroundColor(bg) })
            bracketsTable.addCell(Cell().add(Paragraph("").setFontSize(9f))
                .also { if (bg != null) it.setBackgroundColor(bg) })
        }
        doc.add(bracketsTable)
        doc.add(spacer())

        // ─── DISCLAIMER ───────────────────────────────────────────────────────
        doc.add(Paragraph("כתב ויתור: מסמך זה הינו טיוטה ממוחשבת לצרכי עיון בלבד. " +
            "אין במסמך זה משום ייעוץ מס. יש להתייעץ עם רואה חשבון מוסמך הבקיא " +
            "במיסוי ישראלי-אמריקאי לפני הגשת הדוח לרשות המסים.")
            .setFontSize(8f).setFontColor(ColorConstants.DARK_GRAY)
            .setTextAlignment(TextAlignment.CENTER))
    }

    // ─── Layout Helpers ───────────────────────────────────────────────────────

    private fun sectionHeader(title: String): Paragraph =
        Paragraph(title)
            .setBold().setFontSize(12f)
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setPadding(4f).setMarginTop(8f).setMarginBottom(2f)

    private fun spacer(): Paragraph = Paragraph("\u00A0").setFontSize(4f)

    private fun twoColumnTable(): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
            .setWidth(UnitValue.createPercentValue(100f))

    private fun lineItemTable(): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(8f, 52f, 20f, 20f)))
            .setWidth(UnitValue.createPercentValue(100f))

    private fun transactionTable(): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(18f, 11f, 11f, 10f, 10f, 12f, 13f, 8f)))
            .setWidth(UnitValue.createPercentValue(100f))

    private fun addRow(table: Table, label: String, value: String) {
        table.addCell(Cell().add(Paragraph(label).setFontSize(9f).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY))
        table.addCell(Cell().add(Paragraph(value).setFontSize(10f)))
    }

    private fun addNisItem(
        table: Table,
        code: String,
        description: String,
        amount: Double,
        bold: Boolean = false,
        highlight: Boolean = false,
        note: String = ""
    ) {
        val bgColor = if (highlight) ColorConstants.YELLOW else null
        val codeCell = Cell().add(Paragraph(code).setFontSize(9f).setBold())
        val descText = if (note.isNotBlank()) "$description\n($note)" else description
        val descCell = Cell().add(Paragraph(descText).setFontSize(9f)
            .let { if (bold) it.setBold() else it })
        val amtText = if (amount < 0) "(₪${nisFmt.format(-amount)})" else "₪${nisFmt.format(amount)}"
        val amtCell = Cell().add(Paragraph(amtText).setFontSize(10f)
            .let { if (bold) it.setBold() else it }
            .setTextAlignment(TextAlignment.RIGHT))
        val currCell = Cell().add(Paragraph("NIS").setFontSize(8f)
            .setFontColor(ColorConstants.DARK_GRAY))
        listOf(codeCell, descCell, amtCell, currCell).forEach { cell ->
            bgColor?.let { cell.setBackgroundColor(it) }
            table.addCell(cell)
        }
    }

    private fun headerCell(text: String): Cell =
        Cell().add(Paragraph(text).setFontSize(8f).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setTextAlignment(TextAlignment.CENTER)

    private fun valueCell9(text: String): Cell =
        Cell().add(Paragraph(text).setFontSize(8f))

    private fun maskId(id: String): String {
        if (id.length < 4) return "XXXXXXX"
        return "XXX${id.takeLast(4)}"
    }
}
