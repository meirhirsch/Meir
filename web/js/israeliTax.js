/**
 * IsraeliTaxCalc – Tax Year 2025
 *
 * Legal basis:
 *   - Income Tax Ordinance [New Version] 5721-1961 (פקודת מס הכנסה)
 *   - Section 91   – Capital Gains Tax (25% flat on foreign securities)
 *   - Section 14   – Worldwide income for Israeli residents
 *   - Section 207A – Currency conversion at sale-date BOI rate
 *   - Israel-US Tax Treaty (1994) Art. 22 – relief from double taxation
 *   - ITA 2025 published brackets (רשות המסים, ינואר 2025)
 *
 * All monetary values in NIS unless otherwise noted.
 */
const IsraeliTaxCalc = (() => {

  // ─── 2025 Progressive Income Tax Brackets (NIS / year) ──────────────────────
  const BRACKETS_2025 = [
    { lower: 0,         upper: 84_120,        rate: 0.10 },
    { lower: 84_120,    upper: 120_720,       rate: 0.14 },
    { lower: 120_720,   upper: 193_800,       rate: 0.20 },
    { lower: 193_800,   upper: 269_280,       rate: 0.31 },
    { lower: 269_280,   upper: 560_280,       rate: 0.35 },
    { lower: 560_280,   upper: 721_560,       rate: 0.47 },
    { lower: 721_560,   upper: Infinity,      rate: 0.50 },
  ];

  // ─── Capital Gains (Section 91) ──────────────────────────────────────────────
  const CG_RATE_FOREIGN = 0.25;  // 25% flat – foreign securities (IBKR etc.)

  // ─── Tax Credit Points (נקודות זיכוי) ────────────────────────────────────────
  const CREDIT_POINT_VALUE = 2_994;   // NIS per point, 2025
  const DEFAULT_POINTS_SINGLE = 2.25; // resident individual
  const DEFAULT_POINTS_MARRIED = 3.5; // married couple (woman gets 0.5 extra)

  // ─── Bituach Leumi + Health 2025 ─────────────────────────────────────────────
  const BL_LOWER_CEILING  = 93_000;   // NIS/year (~60% avg wage)
  const BL_UPPER_CEILING  = 607_200;  // NIS/year (max insurable)
  const BL_RATE_LOW       = 0.035;
  const BL_RATE_HIGH      = 0.120;
  const HEALTH_RATE_LOW   = 0.031;
  const HEALTH_RATE_HIGH  = 0.050;

  // ─── Public API ──────────────────────────────────────────────────────────────

  /**
   * Main calculation.
   *
   * @param {object} form106 - parsed Form 106 data
   * @param {Array}  enrichedTxns - trade transactions with exchangeRateOnSaleDate populated
   * @param {number} annualAvgRate - BOI annual average (fallback only)
   * @param {number} usTaxPaid - US federal tax paid on same income (for FTC)
   * @param {string} filingStatus - 'SINGLE'|'MFJ'|'MFS'|'HOH'
   * @returns {object} IsraeliTaxResult
   */
  function calculate(form106, enrichedTxns, annualAvgRate = 0, usTaxPaid = 0, filingStatus = 'SINGLE') {
    // ── Step 1: NIS capital gains from foreign trades ──────────────────────────
    let ltGainsNIS = 0, stGainsNIS = 0;
    for (const txn of enrichedTxns) {
      let gainNIS;
      if (txn.exchangeRateOnSaleDate > 0) {
        // Best path: exact BOI rate on the sale date
        gainNIS = txn.gainLoss * txn.exchangeRateOnSaleDate;
      } else if (annualAvgRate > 0) {
        // Fallback: annual average (acceptable for ITA if BOI rate unavailable)
        gainNIS = txn.gainLoss * annualAvgRate;
      } else {
        gainNIS = txn.gainLoss; // last resort – value stays in USD-equiv
      }
      if (txn.holdingPeriod === 'LONG_TERM') ltGainsNIS += gainNIS;
      else                                   stGainsNIS += gainNIS;
    }

    // Include 1099-INT and 1099-DIV foreign income if present in form106 extras
    const foreignInterestNIS  = (form106.interestIncomeUSD  || 0) * (annualAvgRate || 1);
    const foreignDividendsNIS = (form106.dividendIncomeUSD  || 0) * (annualAvgRate || 1);
    const necIncomeNIS        = (form106.necIncomeUSD        || 0) * (annualAvgRate || 1);

    // ── Step 2: Salary income (already NIS from Form 106) ─────────────────────
    const salaryNIS = form106.grossIncome || 0;

    // ── Step 3: Total worldwide income ────────────────────────────────────────
    const totalIncome = salaryNIS + ltGainsNIS + stGainsNIS
                      + foreignInterestNIS + foreignDividendsNIS + necIncomeNIS;

    // ── Step 4: Progressive tax on salary + NEC (ordinary income) ────────────
    const ordinaryIncome = salaryNIS + necIncomeNIS;
    const salaryTaxRaw   = progressiveTax(ordinaryIncome);

    // ── Step 5: Capital gains tax – 25% flat (Section 91) ────────────────────
    const cgTaxLT    = Math.max(0, ltGainsNIS) * CG_RATE_FOREIGN;
    const cgTaxST    = Math.max(0, stGainsNIS) * CG_RATE_FOREIGN;
    // Foreign interest & dividends: also at 25% under Section 125C/125D
    const intDivTax  = Math.max(0, foreignInterestNIS + foreignDividendsNIS) * CG_RATE_FOREIGN;
    const totalCgTax = cgTaxLT + cgTaxST + intDivTax;

    // ── Step 6: Tax credit points (נקודות זיכוי) ──────────────────────────────
    const defaultPts = (filingStatus === 'MFJ') ? DEFAULT_POINTS_MARRIED : DEFAULT_POINTS_SINGLE;
    const creditPts  = (form106.taxCreditsPoints > 0) ? form106.taxCreditsPoints : defaultPts;
    const creditAmt  = creditPts * CREDIT_POINT_VALUE;
    const salaryTaxAfterCredits = Math.max(0, salaryTaxRaw - creditAmt);

    // ── Step 7: Total tax liability ───────────────────────────────────────────
    const totalTaxLiability = salaryTaxAfterCredits + totalCgTax;

    // ── Step 8: Foreign Tax Credit (Article 22, Israel-US Treaty) ─────────────
    // Credit for US tax paid on income also taxed in Israel;
    // limited to Israeli tax attributable to that income.
    const foreignTaxCredit = Math.min(usTaxPaid, salaryTaxAfterCredits);

    // ── Step 9: Total paid & balance ─────────────────────────────────────────
    const totalPaid   = (form106.incomeTaxWithheld || 0) + foreignTaxCredit;
    const refundOrOwed = totalPaid - totalTaxLiability; // positive = refund

    // ── Step 10: Bituach Leumi ────────────────────────────────────────────────
    const bl = computeBituachLeumi(salaryNIS);

    return {
      // Income
      salaryNIS,
      ltGainsNIS,
      stGainsNIS,
      foreignInterestNIS,
      foreignDividendsNIS,
      necIncomeNIS,
      totalWorldwideIncome: totalIncome,

      // Tax computation
      salaryTaxRaw,
      creditPts,
      creditAmt,
      salaryTaxAfterCredits,
      cgTaxLT,
      cgTaxST,
      intDivTax,
      totalCgTax,
      totalTaxLiability,

      // Credits & payments
      foreignTaxCredit,
      incomeTaxWithheld: form106.incomeTaxWithheld || 0,
      totalPaid,
      refundOrOwed,

      // Bituach Leumi (informational)
      bituachLeumiEmployee:  form106.bituachLeumiEmployee || bl.national,
      healthInsurance:       form106.healthInsurance      || bl.health,
      bituachLeumiComputed:  bl,
    };
  }

  /**
   * Apply progressive brackets to an income amount.
   */
  function progressiveTax(income, brackets = BRACKETS_2025) {
    let tax = 0, remaining = Math.max(0, income);
    for (const b of brackets) {
      if (remaining <= 0) break;
      const inBracket = Math.min(remaining, b.upper - b.lower);
      tax += inBracket * b.rate;
      remaining -= inBracket;
    }
    return tax;
  }

  /**
   * Compute employee Bituach Leumi + Health insurance.
   */
  function computeBituachLeumi(grossAnnual) {
    const capped = Math.min(grossAnnual, BL_UPPER_CEILING);
    const national = capped <= BL_LOWER_CEILING
      ? capped * BL_RATE_LOW
      : BL_LOWER_CEILING * BL_RATE_LOW + (capped - BL_LOWER_CEILING) * BL_RATE_HIGH;
    const health = capped <= BL_LOWER_CEILING
      ? capped * HEALTH_RATE_LOW
      : BL_LOWER_CEILING * HEALTH_RATE_LOW + (capped - BL_LOWER_CEILING) * HEALTH_RATE_HIGH;
    return { national, health, total: national + health };
  }

  /**
   * Generate the per-bracket salary tax breakdown for the 1301 report.
   */
  function bracketBreakdown(income) {
    const rows = [];
    let remaining = Math.max(0, income);
    for (const b of BRACKETS_2025) {
      if (remaining <= 0) break;
      const inBracket = Math.min(remaining, b.upper - b.lower);
      if (inBracket > 0) {
        rows.push({
          range: `${_fmt(b.lower)} – ${b.upper === Infinity ? '∞' : _fmt(b.upper)}`,
          rate: `${(b.rate * 100).toFixed(0)}%`,
          amount: inBracket,
          tax: inBracket * b.rate,
        });
      }
      remaining -= inBracket;
    }
    return rows;
  }

  function _fmt(n) {
    return n.toLocaleString('he-IL', { maximumFractionDigits: 0 });
  }

  return {
    calculate,
    progressiveTax,
    computeBituachLeumi,
    bracketBreakdown,
    BRACKETS_2025,
    CREDIT_POINT_VALUE,
    CG_RATE_FOREIGN,
  };
})();
