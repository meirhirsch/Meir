/**
 * ReportGenerator
 * Produces print-ready HTML reports for:
 *   - Israeli Form 1301 (דוח שנתי ליחיד)
 *   - US Form 1040 + Schedule D + Form 1116
 *   - Schedule of per-trade exchange-rate conversions
 */
const ReportGenerator = (() => {

  const NIS = n => `₪${_fmtNIS(n)}`;
  const USD = n => `$${_fmtUSD(n)}`;
  const PCT = n => `${(n * 100).toFixed(1)}%`;
  const SIGN_NIS = n => (n >= 0 ? `+${NIS(n)}` : `−${NIS(Math.abs(n))}`);
  const SIGN_USD = n => (n >= 0 ? `+${USD(n)}` : `−${USD(Math.abs(n))}`);

  function _fmtNIS(n) { return Math.abs(n).toLocaleString('he-IL', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  function _fmtUSD(n) { return Math.abs(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  function _rate(n)   { return n.toFixed(4); }
  function _refundClass(n) { return n >= 0 ? 'refund' : 'owed'; }
  function _refundLabel(n, lang) {
    if (lang === 'he') return n >= 0 ? 'החזר מס' : 'יתרה לתשלום';
    return n >= 0 ? 'REFUND' : 'BALANCE DUE';
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Israeli Form 1301
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * @param {object} form106        Raw Form 106 data
   * @param {object} israeliResult  Output of IsraeliTaxCalc.calculate()
   * @param {object} usResult       Output of USTaxCalc.calculate()
   * @param {object} session        { taxYear, taxpayerName, taxpayerID, filingStatus }
   * @returns {string} HTML string
   */
  function generate1301(form106, israeliResult, usResult, session) {
    const yr = session.taxYear || 2025;
    const r  = israeliResult;

    const bracketRows = IsraeliTaxCalc.bracketBreakdown(r.salaryNIS)
      .map(b => `<tr><td>${b.range} ₪</td><td>${b.rate}</td>
                    <td class="num">${NIS(b.amount)}</td>
                    <td class="num">${NIS(b.tax)}</td></tr>`)
      .join('');

    return `
<div class="report report-1301" dir="rtl" lang="he">
  <!-- ══ כותרת ══ -->
  <div class="report-header">
    <div class="report-flag">🇮🇱</div>
    <div>
      <h1>דוח שנתי ליחיד – טופס 1301</h1>
      <h2>שנת מס ${yr}</h2>
      <p class="subtitle">רשות המסים בישראל | Israel Tax Authority</p>
    </div>
  </div>

  <!-- ══ חלק א׳: פרטים אישיים ══ -->
  <section class="report-section">
    <h3>חלק א׳ – פרטים אישיים</h3>
    <table class="info-table">
      <tr><td class="label">שם הנישום</td><td>${session.taxpayerName || '—'}</td>
          <td class="label">מספר זהות</td><td>${session.taxpayerID || '—'}</td></tr>
      <tr><td class="label">מעסיק</td><td>${form106.employerName || '—'}</td>
          <td class="label">תיק ניכויים</td><td>${form106.employerId || '—'}</td></tr>
      <tr><td class="label">מצב משפחתי</td><td colspan="3">${_fsHebrew(session.filingStatus)}</td></tr>
    </table>
  </section>

  <!-- ══ חלק ב׳: הכנסות מעבודה ══ -->
  <section class="report-section">
    <h3>חלק ב׳ – הכנסות מעבודה (טופס 106)</h3>
    <table class="data-table">
      <tr><td>קוד 158 – הכנסה ברוטו</td><td class="num">${NIS(form106.grossIncome || 0)}</td></tr>
      <tr><td>קוד 172 – הכנסה חייבת</td><td class="num">${NIS(form106.taxableIncome || 0)}</td></tr>
      <tr><td>קוד 161 – הכנסה פטורה</td><td class="num">${NIS(form106.taxExemptIncome || 0)}</td></tr>
      <tr class="separator"><td>קוד 042 – מס הכנסה שנוכה</td><td class="num deduction">(${NIS(form106.incomeTaxWithheld || 0)})</td></tr>
      <tr><td>קוד 045 – ביטוח לאומי (עובד)</td><td class="num deduction">(${NIS(form106.bituachLeumiEmployee || 0)})</td></tr>
      <tr><td>קוד 047 – ביטוח בריאות</td><td class="num deduction">(${NIS(form106.healthInsurance || 0)})</td></tr>
      <tr><td>קוד 043 – קרן פנסיה (עובד)</td><td class="num deduction">(${NIS(form106.pensionEmployee || 0)})</td></tr>
      <tr><td>קוד 048 – קרן השתלמות</td><td class="num deduction">(${NIS(form106.studyFund || 0)})</td></tr>
    </table>
  </section>

  <!-- ══ חלק ה׳: הכנסות הון מחו״ל ══ -->
  <section class="report-section">
    <h3>חלק ה׳ – רווחי הון מניירות ערך זרים (סעיף 91)</h3>
    <p class="note">שיעור מס אחיד 25% על רווחי הון מניירות ערך זרים | Exchange rate: actual BOI rate on each sale date (§207A)</p>
    <table class="data-table">
      <tr><td>רווח הון ריאלי – ארוך טווח (מעל שנה)</td><td class="num ${r.ltGainsNIS < 0 ? 'loss' : ''}">${NIS(r.ltGainsNIS)}</td></tr>
      <tr><td>רווח הון ריאלי – קצר טווח (עד שנה)</td><td class="num ${r.stGainsNIS < 0 ? 'loss' : ''}">${NIS(r.stGainsNIS)}</td></tr>
      ${r.foreignInterestNIS > 0 ? `<tr><td>הכנסות ריבית מחו"ל (1099-INT)</td><td class="num">${NIS(r.foreignInterestNIS)}</td></tr>` : ''}
      ${r.foreignDividendsNIS > 0 ? `<tr><td>דיבידנדים מחו"ל (1099-DIV)</td><td class="num">${NIS(r.foreignDividendsNIS)}</td></tr>` : ''}
      ${r.necIncomeNIS > 0 ? `<tr><td>הכנסה מעצמאות מחו"ל (1099-NEC)</td><td class="num">${NIS(r.necIncomeNIS)}</td></tr>` : ''}
      <tr class="total"><td>סה"כ הכנסה עולמית חייבת</td><td class="num">${NIS(r.totalWorldwideIncome)}</td></tr>
    </table>
  </section>

  <!-- ══ חלק ח׳: חישוב המס ══ -->
  <section class="report-section">
    <h3>חלק ח׳ – חישוב המס</h3>

    <h4 class="subsection">מס על הכנסת עבודה (מדרגות)</h4>
    <table class="bracket-table">
      <thead><tr><th>טווח הכנסה</th><th>שיעור</th><th>הכנסה במדרגה</th><th>מס</th></tr></thead>
      <tbody>${bracketRows}</tbody>
      <tfoot><tr><td colspan="3"><strong>מס שכר גולמי</strong></td><td class="num"><strong>${NIS(r.salaryTaxRaw)}</strong></td></tr></tfoot>
    </table>

    <table class="data-table mt-1">
      <tr><td>נקודות זיכוי (${r.creditPts.toFixed(2)} × ₪${IsraeliTaxCalc.CREDIT_POINT_VALUE.toLocaleString()})</td>
          <td class="num deduction">(${NIS(r.creditAmt)})</td></tr>
      <tr class="total"><td>מס שכר לאחר זיכויים</td><td class="num">${NIS(r.salaryTaxAfterCredits)}</td></tr>
    </table>

    <table class="data-table mt-1">
      <tr><td>מס רווח הון ארוך טווח (25%)</td><td class="num">${NIS(r.cgTaxLT)}</td></tr>
      <tr><td>מס רווח הון קצר טווח (25%)</td><td class="num">${NIS(r.cgTaxST)}</td></tr>
      ${r.intDivTax > 0 ? `<tr><td>מס על ריבית/דיבידנד זרים (25%)</td><td class="num">${NIS(r.intDivTax)}</td></tr>` : ''}
      <tr class="total"><td>סה"כ חבות מס</td><td class="num highlight">${NIS(r.totalTaxLiability)}</td></tr>
    </table>
  </section>

  <!-- ══ חלק ט׳: ניכויים ותשלומים ══ -->
  <section class="report-section">
    <h3>חלק ט׳ – ניכויים ותשלומים</h3>
    <table class="data-table">
      <tr><td>מס הכנסה שנוכה במקור (קוד 042)</td><td class="num deduction">(${NIS(r.incomeTaxWithheld)})</td></tr>
      ${r.foreignTaxCredit > 0 ? `<tr><td>זיכוי מס זר – ארה"ב (אמנת מס, סעיף 22)</td><td class="num deduction">(${NIS(r.foreignTaxCredit)})</td></tr>` : ''}
      <tr class="total"><td>סה"כ שולם</td><td class="num">${NIS(r.totalPaid)}</td></tr>
      <tr class="result ${_refundClass(r.refundOrOwed)}">
        <td><strong>${_refundLabel(r.refundOrOwed, 'he')}</strong></td>
        <td class="num"><strong>${SIGN_NIS(r.refundOrOwed)}</strong></td>
      </tr>
    </table>
  </section>

  <!-- ══ הערות CPA ══ -->
  <section class="report-section notes">
    <h3>הערות לרואה חשבון</h3>
    <ul>
      <li>שיעורי החליפין בוצעו לפי שערי בנק ישראל ביום המכירה בפועל (סעיף 207א לפקודה).</li>
      <li>ניירות ערך זרים: שיעור מס 25% לפי סעיף 91(ב) לפקודת מס הכנסה.</li>
      <li>הזיכוי ממס זר מוגבל למס הישראלי על אותה הכנסה (אמנת מס ישראל-ארה"ב, 1994, סעיף 22).</li>
      ${usResult && usResult.requiresFBAR ? '<li class="warn">⚠ יש לדווח על חשבונות בנק זרים – FBAR (FinCEN 114) נדרש.</li>' : ''}
      ${usResult && usResult.requiresForm8938 ? '<li class="warn">⚠ FATCA – טופס 8938 נדרש לדיווח נכסים פיננסיים זרים.</li>' : ''}
    </ul>
  </section>

  <div class="report-footer">
    <p>הופק על ידי US–Israel Tax Assistant | ${new Date().toLocaleDateString('he-IL')} | לצורך עיון בלבד – יש לאמת עם רואה חשבון מוסמך</p>
  </div>
</div>`;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // US Form 1040 + Schedule D + Form 1116
  // ═══════════════════════════════════════════════════════════════════════════

  function generate1040(form106, form1099, usResult, session) {
    const yr = session.taxYear || 2025;
    const r  = usResult;

    const bracketRows = USTaxCalc.bracketBreakdown(r.salaryUSD + r.stGains, r.filingStatus)
      .map(b => `<tr><td>${b.range}</td><td>${b.rate}</td>
                    <td class="num">${USD(b.amount)}</td>
                    <td class="num">${USD(b.tax)}</td></tr>`)
      .join('');

    return `
<div class="report report-1040" dir="ltr" lang="en">
  <!-- ══ Header ══ -->
  <div class="report-header">
    <div class="report-flag">🇺🇸</div>
    <div>
      <h1>Form 1040 – U.S. Individual Income Tax Return</h1>
      <h2>Tax Year ${yr}</h2>
      <p class="subtitle">Internal Revenue Service | Department of the Treasury</p>
    </div>
  </div>

  <!-- ══ Personal Info ══ -->
  <section class="report-section">
    <h3>Taxpayer Information</h3>
    <table class="info-table">
      <tr><td class="label">Name</td><td>${session.taxpayerName || '—'}</td>
          <td class="label">SSN / ITIN</td><td>${session.ssn || '—'}</td></tr>
      <tr><td class="label">Filing Status</td><td colspan="3">${_fsEnglish(session.filingStatus)}</td></tr>
    </table>
  </section>

  <!-- ══ Income ══ -->
  <section class="report-section">
    <h3>Income (Lines 1–8)</h3>
    <table class="data-table">
      <tr><td>Line 1a – Wages, salaries (Israeli salary, converted @ ${_rate(r.salaryConversionRate)} NIS/$)</td>
          <td class="num">${USD(r.salaryUSD_raw)}</td></tr>
      ${r.feieExcluded > 0 ? `<tr><td>Form 2555 – Foreign Earned Income Exclusion (FEIE)</td><td class="num deduction">(${USD(r.feieExcluded)})</td></tr>` : ''}
      <tr><td>Line 1a – Included wages</td><td class="num">${USD(r.salaryUSD)}</td></tr>
      ${r.interest > 0 ? `<tr><td>Line 2b – Taxable interest (1099-INT)</td><td class="num">${USD(r.interest)}</td></tr>` : ''}
      ${r.ordDiv > 0 ? `<tr><td>Line 3b – Ordinary dividends (1099-DIV)</td><td class="num">${USD(r.ordDiv)}</td></tr>` : ''}
      ${r.qualDiv > 0 ? `<tr><td>Line 3a – Qualified dividends (LTCG rate)</td><td class="num">${USD(r.qualDiv)}</td></tr>` : ''}
      ${r.necIncome > 0 ? `<tr><td>Line 8 – Other income: 1099-NEC (→ Schedule C)</td><td class="num">${USD(r.necIncome)}</td></tr>` : ''}
      <tr><td>Schedule D – Short-term capital gain / (loss)</td><td class="num ${r.stGains < 0 ? 'loss' : ''}">${USD(r.stGains)}</td></tr>
      <tr><td>Schedule D – Long-term capital gain / (loss)</td><td class="num ${r.ltGains < 0 ? 'loss' : ''}">${USD(r.ltGains)}</td></tr>
      <tr class="total"><td>Line 9 – Total income (AGI)</td><td class="num">${USD(r.agi)}</td></tr>
    </table>
  </section>

  <!-- ══ Deductions & Taxable Income ══ -->
  <section class="report-section">
    <h3>Deductions (Lines 12–15)</h3>
    <table class="data-table">
      <tr><td>Line 12 – Standard deduction (${_fsEnglish(r.filingStatus)})</td><td class="num deduction">(${USD(r.standardDeduction)})</td></tr>
      <tr class="total"><td>Line 15 – Taxable income</td><td class="num">${USD(r.taxableIncome)}</td></tr>
    </table>
  </section>

  <!-- ══ Tax Computation ══ -->
  <section class="report-section">
    <h3>Tax (Lines 16–24)</h3>
    <h4 class="subsection">Ordinary Income Tax Brackets</h4>
    <table class="bracket-table">
      <thead><tr><th>Income Range</th><th>Rate</th><th>Amount in Bracket</th><th>Tax</th></tr></thead>
      <tbody>${bracketRows}</tbody>
      <tfoot><tr><td colspan="3"><strong>Ordinary income tax</strong></td>
             <td class="num"><strong>${USD(r.ordinaryTax)}</strong></td></tr></tfoot>
    </table>

    <table class="data-table mt-1">
      ${r.ltcgTax > 0 ? `<tr><td>Long-term capital gains tax (0%/15%/20%)</td><td class="num">${USD(r.ltcgTax)}</td></tr>` : ''}
      ${r.niitTax > 0 ? `<tr><td>Net Investment Income Tax (3.8%) – IRC §1411</td><td class="num">${USD(r.niitTax)}</td></tr>` : ''}
      <tr class="total"><td>Line 24 – Total tax (before credits)</td><td class="num highlight">${USD(r.totalTaxBeforeCredits)}</td></tr>
    </table>
  </section>

  <!-- ══ Form 1116 – Foreign Tax Credit ══ -->
  <section class="report-section">
    <h3>Form 1116 – Foreign Tax Credit</h3>
    <table class="data-table">
      <tr><td>Israeli tax paid (₪ → $ @ ${_rate(r.salaryConversionRate)})</td><td class="num">${USD(r.israeliTaxPaidUSD)}</td></tr>
      <tr><td>FTC limitation (foreign income basket)</td><td class="num">${USD(r.foreignTaxCredit)}</td></tr>
      <tr class="total"><td>Net US tax after FTC</td><td class="num">${USD(r.netTax)}</td></tr>
    </table>
  </section>

  <!-- ══ Payments & Result ══ -->
  <section class="report-section">
    <h3>Payments & Refund (Lines 25–38)</h3>
    <table class="data-table">
      <tr><td>Line 25 – Federal tax withheld (1099-B Box 4)</td><td class="num deduction">(${USD(r.withheld)})</td></tr>
      <tr class="result ${_refundClass(r.refundOrOwed)}">
        <td><strong>${_refundLabel(r.refundOrOwed, 'en')}</strong></td>
        <td class="num"><strong>${SIGN_USD(r.refundOrOwed)}</strong></td>
      </tr>
    </table>
  </section>

  <!-- ══ FBAR / FATCA ══ -->
  ${(r.requiresFBAR || r.requiresForm8938) ? `
  <section class="report-section compliance-section">
    <h3>Compliance Requirements</h3>
    ${r.requiresFBAR ? '<div class="compliance-item warn"><strong>FinCEN 114 (FBAR)</strong> – Foreign account balance exceeds $10,000. File by April 15 (auto-extended to October 15).</div>' : ''}
    ${r.requiresForm8938 ? '<div class="compliance-item warn"><strong>Form 8938 (FATCA)</strong> – Foreign financial assets exceed threshold. Attach to Form 1040.</div>' : ''}
  </section>` : ''}

  <!-- ══ CPA Notes ══ -->
  <section class="report-section notes">
    <h3>CPA Notes</h3>
    <ul>
      <li>Israeli salary converted at BOI annual average rate (IRS Rev. Rul. 2008-38 / Rev. Proc. 2024-40).</li>
      <li>Capital gains in USD (from IBKR 1099-B) — no additional conversion required for US reporting.</li>
      <li>Israeli capital gains tax on foreign securities (25%) reported on Israeli Form 1301 Section E.</li>
      <li>Foreign Tax Credit (Form 1116) limited to US tax attributable to foreign income basket (IRC §904).</li>
      ${r.feieExcluded > 0 ? '<li>FEIE elected (Form 2555): FTC cannot be claimed on excluded income.</li>' : ''}
    </ul>
  </section>

  <div class="report-footer">
    <p>Generated by US–Israel Tax Assistant | ${new Date().toLocaleDateString('en-US')} | For review purposes only — verify with a licensed CPA or EA</p>
  </div>
</div>`;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Schedule D – Trade-level exchange rate table
  // ═══════════════════════════════════════════════════════════════════════════

  function generateTradeSchedule(enrichedTxns, annualAvgRate) {
    if (!enrichedTxns || enrichedTxns.length === 0) {
      return '<p class="no-data">No trade transactions entered.</p>';
    }

    const rows = enrichedTxns.map((t, i) => {
      const rateOK = t.exchangeRateOnSaleDate > 0;
      const rateDisp = rateOK ? _rate(t.exchangeRateOnSaleDate) : (t.manualRate > 0 ? `${_rate(t.manualRate)}*` : '—');
      const gain = t.gainLoss;
      const gainNIS = t.gainLossNIS;
      return `
<tr class="${t.holdingPeriod === 'LONG_TERM' ? 'lt-row' : 'st-row'} ${gain < 0 ? 'loss-row' : ''}">
  <td>${i + 1}</td>
  <td class="desc">${t.description || '—'}</td>
  <td>${t.dateAcquired || 'VARIOUS'}</td>
  <td>${t.dateSold || '—'}</td>
  <td class="badge ${t.holdingPeriod === 'LONG_TERM' ? 'badge-lt' : 'badge-st'}">${t.holdingPeriod === 'LONG_TERM' ? 'LT' : 'ST'}</td>
  <td class="num">${USD(t.proceeds)}</td>
  <td class="num">${USD(t.costBasis)}</td>
  <td class="num ${gain < 0 ? 'loss' : 'gain'}">${USD(gain)}</td>
  <td class="num rate-cell ${rateOK ? 'rate-ok' : t.manualRate > 0 ? 'rate-manual' : 'rate-missing'}">${rateDisp}</td>
  <td class="num ${gainNIS < 0 ? 'loss' : 'gain'}">${NIS(gainNIS)}</td>
  <td class="num">${NIS(t.proceedsNIS)}</td>
</tr>`;
    }).join('');

    const stTxns = enrichedTxns.filter(t => t.holdingPeriod === 'SHORT_TERM');
    const ltTxns = enrichedTxns.filter(t => t.holdingPeriod === 'LONG_TERM');
    const sumGL  = (arr, f) => arr.reduce((a, t) => a + (t[f] || 0), 0);

    return `
<div class="report report-schedule" dir="ltr" lang="en">
  <div class="report-header">
    <div class="report-flag">📊</div>
    <div>
      <h1>Schedule D – Capital Gains & Exchange Rate Conversion</h1>
      <p class="subtitle">Per-trade BOI rate applied (§207A Israeli Tax Ordinance)</p>
    </div>
  </div>

  <section class="report-section">
    <div class="table-scroll">
      <table class="trade-table">
        <thead>
          <tr>
            <th>#</th><th>Security</th><th>Date Acquired</th><th>Date Sold</th><th>Term</th>
            <th>Proceeds $</th><th>Basis $</th><th>Gain/Loss $</th>
            <th>BOI Rate ₪/$</th><th>Gain/Loss ₪</th><th>Proceeds ₪</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
        <tfoot>
          <tr class="subtotal">
            <td colspan="7"><strong>Short-Term Total</strong></td>
            <td class="num ${sumGL(stTxns,'gainLoss') < 0 ? 'loss' : 'gain'}">${USD(sumGL(stTxns,'gainLoss'))}</td>
            <td>—</td>
            <td class="num ${sumGL(stTxns,'gainLossNIS') < 0 ? 'loss' : 'gain'}">${NIS(sumGL(stTxns,'gainLossNIS'))}</td>
            <td></td>
          </tr>
          <tr class="subtotal">
            <td colspan="7"><strong>Long-Term Total</strong></td>
            <td class="num ${sumGL(ltTxns,'gainLoss') < 0 ? 'loss' : 'gain'}">${USD(sumGL(ltTxns,'gainLoss'))}</td>
            <td>—</td>
            <td class="num ${sumGL(ltTxns,'gainLossNIS') < 0 ? 'loss' : 'gain'}">${NIS(sumGL(ltTxns,'gainLossNIS'))}</td>
            <td></td>
          </tr>
          <tr class="total-row">
            <td colspan="7"><strong>Grand Total</strong></td>
            <td class="num">${USD(sumGL(enrichedTxns,'gainLoss'))}</td>
            <td class="note">Annual avg: ${annualAvgRate ? _rate(annualAvgRate) : '—'}</td>
            <td class="num">${NIS(sumGL(enrichedTxns,'gainLossNIS'))}</td>
            <td></td>
          </tr>
        </tfoot>
      </table>
    </div>
    <p class="legend">
      <span class="badge badge-lt">LT</span> Long-term (&gt;1 year) ·
      <span class="badge badge-st">ST</span> Short-term (≤1 year) ·
      <span class="rate-ok-legend">■</span> BOI rate confirmed ·
      <span class="rate-manual-legend">■</span> Manual rate* ·
      <span class="rate-missing-legend">■</span> Rate missing (0)
    </p>
  </section>
  <div class="report-footer">
    <p>Generated by US–Israel Tax Assistant | ${new Date().toLocaleDateString('en-US')} | Per-date Bank of Israel official rates</p>
  </div>
</div>`;
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  function _fsHebrew(fs) {
    return { SINGLE: 'רווק/ה', MFJ: 'נשוי/אה', MFS: 'נשוי/אה – דוח נפרד', HOH: 'עומד/ת בראש משק בית' }[fs] || fs || '—';
  }
  function _fsEnglish(fs) {
    return { SINGLE: 'Single', MFJ: 'Married Filing Jointly', MFS: 'Married Filing Separately', HOH: 'Head of Household' }[fs] || fs || '—';
  }

  return { generate1301, generate1040, generateTradeSchedule };
})();
