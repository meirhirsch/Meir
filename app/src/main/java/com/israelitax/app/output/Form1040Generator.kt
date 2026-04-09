package com.israelitax.app.output

import android.content.Context
import com.israelitax.app.data.models.FilingStatus
import com.israelitax.app.data.models.Form106Data
import com.israelitax.app.data.models.Form1099BData
import com.israelitax.app.data.models.USTaxResult
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * Generates a completed Form 1040 (US Individual Income Tax Return) as a PDF.
 *
 * This creates a structured, field-by-field representation matching the official IRS Form 1040
 * layout, pre-populated with all computed values. The generated PDF can be printed and used
 * as a draft – the taxpayer should review with a licensed CPA before filing.
 *
 * Form 1040 structure (2024):
 *  - Filing Information (Lines 1-5)
 *  - Income (Lines 1a-11)
 *  - Adjustments (Lines 12-15)
 *  - Tax & Credits (Lines 16-24)
 *  - Other Taxes (Lines 25-37)
 *  - Payments (Lines 38-39)
 *  - Refund / Amount Owed (Lines 40-38)
 *
 * Attachments referenced:
 *  - Schedule D (Capital Gains and Losses)
 *  - Form 1116 (Foreign Tax Credit)
 *  - FinCEN 114 (FBAR) – note in PDF if required
 *  - Form 8938 (FATCA) – note in PDF if required
 */
class Form1040Generator(private val context: Context) {

    private val usCurrencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }

    /**
     * Generates the Form 1040 PDF and returns the output file.
     * @param outputDir Directory to write the PDF to
     */
    fun generate(
        form106: Form106Data,
        form1099B: Form1099BData,
        result: USTaxResult,
        taxpayerName: String,
        taxpayerSSN: String,
        spouseName: String = "",
        outputDir: File
    ): File {
        outputDir.mkdirs()
        val outputFile = File(outputDir, "Form_1040_${result.taxYear}_DRAFT.pdf")

        PdfWriter(outputFile).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.LETTER).use { doc ->
                    doc.setMargins(36f, 36f, 36f, 36f)
                    buildDocument(doc, form106, form1099B, result, taxpayerName, taxpayerSSN, spouseName)
                }
            }
        }

        return outputFile
    }

    private fun buildDocument(
        doc: Document,
        form106: Form106Data,
        form1099B: Form1099BData,
        result: USTaxResult,
        taxpayerName: String,
        taxpayerSSN: String,
        spouseName: String
    ) {
        // ─── HEADER ──────────────────────────────────────────────────────────
        doc.add(draftWatermarkParagraph())
        doc.add(Paragraph("Department of the Treasury — Internal Revenue Service")
            .setFontSize(9f).setFontColor(ColorConstants.DARK_GRAY))
        doc.add(Paragraph("Form 1040 — U.S. Individual Income Tax Return")
            .setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
        doc.add(Paragraph("Tax Year ${result.taxYear}  |  OMB No. 1545-0074")
            .setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        doc.add(Paragraph("⚠  DRAFT — Review with a licensed CPA before filing")
            .setFontSize(10f).setFontColor(ColorConstants.RED)
            .setTextAlignment(TextAlignment.CENTER))
        doc.add(spacer())

        // ─── FILING INFORMATION ───────────────────────────────────────────────
        doc.add(sectionHeader("Filing Information"))
        val filingTable = twoColumnTable()
        filingTable.addCell(labelCell("Your Name (First, Last)"))
        filingTable.addCell(valueCell(taxpayerName))
        filingTable.addCell(labelCell("Social Security Number"))
        filingTable.addCell(valueCell(maskSSN(taxpayerSSN)))
        if (result.filingStatus == FilingStatus.MARRIED_FILING_JOINTLY && spouseName.isNotBlank()) {
            filingTable.addCell(labelCell("Spouse's Name"))
            filingTable.addCell(valueCell(spouseName))
        }
        filingTable.addCell(labelCell("Filing Status"))
        filingTable.addCell(valueCell(result.filingStatus.displayName()))
        filingTable.addCell(labelCell("Tax Year"))
        filingTable.addCell(valueCell(result.taxYear.toString()))
        doc.add(filingTable)
        doc.add(spacer())

        // ─── INCOME SECTION ───────────────────────────────────────────────────
        doc.add(sectionHeader("Income"))
        val incomeTable = lineItemTable()

        addLineItem(incomeTable, "1a", "Total wages, salaries, tips (from W-2 / foreign employer)",
            result.wagesAndSalaries, note = "Converted from NIS using IRS avg rate")
        addLineItem(incomeTable, "1b", "Household employee wages", 0.0)
        addLineItem(incomeTable, "7",  "Capital gain or (loss) — attach Schedule D",
            result.shortTermCapGains + result.longTermCapGains,
            note = "From IBKR Form 1099-B")
        addLineItem(incomeTable, "8",  "Other income (Schedule 1)", 0.0)
        addLineItem(incomeTable, "9",  "Total income (add lines 1a through 8)",
            result.totalIncome, bold = true)
        doc.add(incomeTable)
        doc.add(spacer())

        // ─── ADJUSTMENTS / AGI ────────────────────────────────────────────────
        doc.add(sectionHeader("Adjusted Gross Income"))
        val agiTable = lineItemTable()
        addLineItem(agiTable, "10", "Adjustments to income (Schedule 1, Part II)", 0.0)
        addLineItem(agiTable, "11", "Adjusted Gross Income (Line 9 minus Line 10)",
            result.agi, bold = true)
        doc.add(agiTable)
        doc.add(spacer())

        // ─── STANDARD DEDUCTION ───────────────────────────────────────────────
        doc.add(sectionHeader("Standard Deduction"))
        val dedTable = lineItemTable()
        addLineItem(dedTable, "12", "Standard deduction", result.standardDeduction)
        addLineItem(dedTable, "13", "Qualified business income deduction", 0.0)
        addLineItem(dedTable, "14", "Total deductions (add lines 12 and 13)",
            result.standardDeduction, bold = true)
        addLineItem(dedTable, "15", "Taxable Income (Line 11 minus Line 14)",
            result.taxableIncome, bold = true)
        doc.add(dedTable)
        doc.add(spacer())

        // ─── TAX AND CREDITS ──────────────────────────────────────────────────
        doc.add(sectionHeader("Tax and Credits"))
        val taxTable = lineItemTable()
        addLineItem(taxTable, "16", "Tax (from Tax Table / Tax Computation Worksheet)",
            result.ordinaryIncomeTax + result.capitalGainsTax)
        addLineItem(taxTable, "17", "Alternative Minimum Tax (Form 6251)", 0.0)
        addLineItem(taxTable, "18", "Add lines 16 and 17",
            result.ordinaryIncomeTax + result.capitalGainsTax)
        addLineItem(taxTable, "19", "Child Tax Credit / Credit for other dependents", 0.0)
        addLineItem(taxTable, "20", "Schedule 3, line 8 (other credits)", 0.0)
        addLineItem(taxTable, "21", "Add lines 19 and 20", 0.0)
        addLineItem(taxTable, "22", "Subtract line 21 from line 18",
            result.ordinaryIncomeTax + result.capitalGainsTax)
        addLineItem(taxTable, "23", "Other taxes (self-employment, etc.)", 0.0)
        addLineItem(taxTable, "24", "TOTAL TAX",
            result.totalTax, bold = true, highlight = true)
        doc.add(taxTable)
        doc.add(spacer())

        // ─── PAYMENTS ─────────────────────────────────────────────────────────
        doc.add(sectionHeader("Payments & Credits"))
        val payTable = lineItemTable()
        addLineItem(payTable, "25a", "Federal income tax withheld (Form 1099-B Box 4)",
            result.federalTaxWithheld)
        addLineItem(payTable, "31", "Foreign Tax Credit (Form 1116) — from Israeli taxes paid",
            result.foreignTaxCredit, note = "Israel-US Tax Treaty, Article 22")
        addLineItem(payTable, "33", "Total other payments and refundable credits", 0.0)
        addLineItem(payTable, "34", "TOTAL PAYMENTS & CREDITS",
            result.totalCreditsAndPayments, bold = true)
        doc.add(payTable)
        doc.add(spacer())

        // ─── REFUND / AMOUNT OWED ─────────────────────────────────────────────
        doc.add(sectionHeader("Refund / Amount Owed"))
        val refundTable = lineItemTable()
        if (result.refundOrOwed >= 0) {
            addLineItem(refundTable, "35a", "OVERPAID — Amount to be REFUNDED to you",
                result.refundOrOwed, bold = true, highlight = true)
        } else {
            addLineItem(refundTable, "37", "AMOUNT YOU OWE",
                -result.refundOrOwed, bold = true, highlight = true,
                note = "Pay by April 15, ${result.taxYear + 1}")
        }
        doc.add(refundTable)
        doc.add(spacer())

        // ─── SCHEDULE D SUMMARY ───────────────────────────────────────────────
        doc.add(sectionHeader("Schedule D — Capital Gains and Losses (Summary)"))
        val schedDTable = lineItemTable()
        addLineItem(schedDTable, "D1", "Short-term gains/losses (held ≤ 1 year) — taxed as ordinary income",
            result.shortTermCapGains, note = "From IBKR 1099-B Box 1a")
        addLineItem(schedDTable, "D8", "Long-term gains/losses (held > 1 year) — preferential rate",
            result.longTermCapGains, note = "From IBKR 1099-B Box 2a")
        addLineItem(schedDTable, "D16", "Total capital gain or (loss)",
            result.shortTermCapGains + result.longTermCapGains, bold = true)
        doc.add(schedDTable)
        doc.add(spacer())

        // ─── FORM 1116 SUMMARY ────────────────────────────────────────────────
        doc.add(sectionHeader("Form 1116 — Foreign Tax Credit (General Basket — Israeli Salary)"))
        val ftcTable = lineItemTable()
        addLineItem(ftcTable, "1116-A", "Foreign income subject to Israeli tax (salary)",
            result.wagesAndSalaries, note = "Converted to USD from NIS")
        addLineItem(ftcTable, "1116-B", "Israeli income tax paid on salary (NIS converted to USD)",
            result.foreignTaxCredit, note = "From Form 106 code 042")
        addLineItem(ftcTable, "1116-C", "FTC Limitation (foreign income / total income × US tax)",
            result.foreignTaxCredit)
        addLineItem(ftcTable, "1116-D", "Credit allowed (lesser of paid or limitation)",
            result.foreignTaxCredit, bold = true, highlight = true)
        doc.add(ftcTable)
        doc.add(spacer())

        // ─── COMPLIANCE NOTES ─────────────────────────────────────────────────
        doc.add(sectionHeader("Compliance Reminders"))
        val notes = mutableListOf<String>()

        if (result.requiresFBAR) {
            notes.add("✓  FBAR REQUIRED — File FinCEN Form 114 by April 15 (auto-extended to Oct 15). " +
                "Your Israeli bank/IBKR account balances exceeded \$10,000 at any point during ${result.taxYear}.")
        }
        if (result.requiresForm8938) {
            notes.add("✓  FORM 8938 REQUIRED — Attach to Form 1040. " +
                "Foreign financial assets exceeded \$50,000 (single) / \$100,000 (MFJ) on Dec 31.")
        }
        if (!result.requiresFBAR && !result.requiresForm8938) {
            notes.add("No FBAR or Form 8938 required based on entered account balance.")
        }
        notes.add("Consider Form 8949 for detailed listing of each IBKR transaction (attach to Schedule D).")
        notes.add("If you used FEIE (Form 2555) in a prior year, review IRS Rev. Rul. 83-82 before switching to FTC.")
        notes.add("Israel-US Social Security Totalization Agreement: you may be exempt from US FICA on Israeli wages.")

        for (note in notes) {
            doc.add(Paragraph(note).setFontSize(10f).setMarginBottom(4f))
        }
        doc.add(spacer())

        // ─── DISCLAIMER ───────────────────────────────────────────────────────
        doc.add(Paragraph("DISCLAIMER: This document is a computer-generated draft for review purposes only. " +
            "It does not constitute tax advice. Consult a licensed CPA or Enrolled Agent familiar with " +
            "US-Israel dual taxation before filing.")
            .setFontSize(8f).setFontColor(ColorConstants.DARK_GRAY)
            .setTextAlignment(TextAlignment.CENTER))
    }

    // ─── Layout Helpers ───────────────────────────────────────────────────────

    private fun draftWatermarkParagraph(): Paragraph =
        Paragraph("DRAFT — NOT FOR FILING")
            .setFontSize(8f).setFontColor(ColorConstants.LIGHT_GRAY)
            .setTextAlignment(TextAlignment.RIGHT)

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

    private fun addLineItem(
        table: Table,
        lineNum: String,
        description: String,
        amount: Double,
        bold: Boolean = false,
        highlight: Boolean = false,
        note: String = ""
    ) {
        val bgColor = if (highlight) ColorConstants.YELLOW else null
        val lineCell = Cell().add(Paragraph(lineNum).setFontSize(9f).setBold())
        val descText = if (note.isNotBlank()) "$description\n($note)" else description
        val descCell = Cell().add(Paragraph(descText).setFontSize(9f).let {
            if (bold) it.setBold() else it
        })
        val amtText = if (amount < 0) "(${fmt.format(-amount)})" else fmt.format(amount)
        val amtCell = Cell().add(Paragraph("$$amtText")
            .setFontSize(10f).let { if (bold) it.setBold() else it }
            .setTextAlignment(TextAlignment.RIGHT))
        val noteCell = Cell().add(Paragraph("USD").setFontSize(8f)
            .setFontColor(ColorConstants.DARK_GRAY))

        listOf(lineCell, descCell, amtCell, noteCell).forEach { cell ->
            bgColor?.let { cell.setBackgroundColor(it) }
            table.addCell(cell)
        }
    }

    private fun labelCell(text: String): Cell =
        Cell().add(Paragraph(text).setFontSize(9f).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)

    private fun valueCell(text: String): Cell =
        Cell().add(Paragraph(text).setFontSize(10f))

    private fun maskSSN(ssn: String): String {
        if (ssn.length >= 4) return "XXX-XX-${ssn.takeLast(4)}"
        return "XXX-XX-XXXX"
    }

    private fun FilingStatus.displayName() = when (this) {
        FilingStatus.SINGLE -> "Single"
        FilingStatus.MARRIED_FILING_JOINTLY -> "Married Filing Jointly"
        FilingStatus.MARRIED_FILING_SEPARATELY -> "Married Filing Separately"
        FilingStatus.HEAD_OF_HOUSEHOLD -> "Head of Household"
    }
}
