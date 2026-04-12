/**
 * USTaxCalc – US Federal Income Tax, Tax Year 2025 (Form 1040)
 *
 * Legal basis:
 *   - IRC §1        – ordinary income tax rates
 *   - IRC §1(h)     – preferential long-term capital gains rates
 *   - IRC §63       – standard deduction
 *   - IRC §901/§904 – Foreign Tax Credit (Form 1116)
 *   - IRC §911      – Foreign Earned Income Exclusion (Form 2555)
 *   - IRC §1411     – Net Investment Income Tax (3.8%)
 *   - IRS Rev. Proc. 2024-40 (2025 inflation adjustments)
 *   - Israel-US Tax Treaty (1994) – prevents double taxation
 *
 * All monetary values in USD unless noted.
 */
const USTaxCalc = (() => {

  // ─── 2025 Ordinary Income Brackets ──────────────────────────────────────────
  const BRACKETS_SINGLE_2025 = [
    { lower: 0,         upper: 11_925,        rate: 0.10 },
    { lower: 11_925,    upper: 48_475,        rate: 0.12 },
    { lower: 48_475,    upper: 103_350,       rate: 0.22 },
    { lower: 103_350,   upper: 197_300,       rate: 0.24 },
    { lower: 197_300,   upper: 250_525,       rate: 0.32 },
    { lower: 250_525,   upper: 626_350,       rate: 0.35 },
    { lower: 626_350,   upper: Infinity,      rate: 0.37 },
  ];

  const BRACKETS_MFJ_2025 = [
    { lower: 0,         upper: 23_850,        rate: 0.10 },
    { lower: 23_850,    upper: 96_950,        rate: 0.12 },
    { lower: 96_950,    upper: 206_700,       rate: 0.22 },
    { lower: 206_700,   upper: 394_600,       rate: 0.24 },
    { lower: 394_600,   upper: 501_050,       rate: 0.32 },
    { lower: 501_050,   upper: 751_600,       rate: 0.35 },
    { lower: 751_600,   upper: Infinity,      rate: 0.37 },
  ];

  const BRACKETS_HOH_2025 = [
    { lower: 0,         upper: 17_000,        rate: 0.10 },
    { lower: 17_000,    upper: 64_850,        rate: 0.12 },
    { lower: 64_850,    upper: 103_350,       rate: 0.22 },
    { lower: 103_350,   upper: 197_300,       rate: 0.24 },
    { lower: 197_300,   upper: 250_500,       rate: 0.32 },
    { lower: 250_500,   upper: 626_350,       rate: 0.35 },
    { lower: 626_350,   upper: Infinity,      rate: 0.37 },
  ];

  // ─── 2025 Standard Deductions ────────────────────────────────────────────────
  const STD_DEDUCTION = {
    SINGLE: 15_000,
    MFJ:    30_000,
    MFS:    15_000,
    HOH:    22_500,
  };

  // ─── 2025 LTCG Rate Thresholds (0% / 15% / 20%) ─────────────────────────────
  const LTCG = {
    SINGLE: { zero: 48_350,  fifteen: 533_400 },
    MFJ:    { zero: 96_700,  fifteen: 600_050 },
    MFS:    { zero: 48_350,  fifteen: 300_000 },
    HOH:    { zero: 64_750,  fifteen: 566_700 },
  };

  // ─── FEIE (Foreign Earned Income Exclusion) limit 2025 ─────────────────────
  const FEIE_LIMIT = 130_000;    // IRC §911(b)(2)

  // ─── FBAR / FATCA ────────────────────────────────────────────────────────────
  const FBAR_THRESHOLD = 10_000;
  const FATCA_SINGLE   = 50_000;
  const FATCA_MFJ      = 100_000;

  // ─── NIIT ────────────────────────────────────────────────────────────────────
  const NIIT_RATE = 0.038;
  const NIIT_THRESHOLD = { SINGLE: 200_000, MFJ: 250_000, MFS: 125_000, HOH: 200_000 };

  // ─── Public API ──────────────────────────────────────────────────────────────

  /**
   * @param {object} form106       – Israeli salary form (grossIncome in NIS)
   * @param {object} form1099      – aggregated 1099 data
   *   .shortTermGainLoss          USD – from 1099-B
   *   .longTermGainLoss           USD – from 1099-B
   *   .federalTaxWithheld         USD – from 1099-B box 4
   *   .necIncome                  USD – from 1099-NEC box 1
   *   .interestIncome             USD – from 1099-INT box 1
   *   .ordinaryDividends          USD – from 1099-DIV box 1a
   *   .qualifiedDividends         USD – from 1099-DIV box 1b
   * @param {string} filingStatus  'SINGLE'|'MFJ'|'MFS'|'HOH'
   * @param {number} israeliTaxPaid  NIS already paid to Israeli TA (for FTC)
   * @param {boolean} useFEIE       elect Foreign Earned Income Exclusion
   * @param {number} israeliAccountBalanceUSD  for FBAR/FATCA check
   * @param {number} salaryConversionRate  NIS per 1 USD (BOI annual avg)
   * @returns {object} USTaxResult
   */
  function calculate({
    form106,
    form1099,
    filingStatus      = 'SINGLE',
    israeliTaxPaidNIS = 0,
    useFEIE           = false,
    israeliAccountBalanceUSD = 0,
    salaryConversionRate     = 3.70,
  }) {
    const fs = filingStatus;

    // ── Step 1: Convert Israeli salary to USD ─────────────────────────────────
    const salaryUSD_raw = (form106.grossIncome || 0) / salaryConversionRate;

    // FEIE election (Form 2555)
    const feieExcluded = useFEIE ? Math.min(salaryUSD_raw, FEIE_LIMIT) : 0;
    const salaryUSD    = salaryUSD_raw - feieExcluded;

    // ── Step 2: Other US-source income (1099 forms) ───────────────────────────
    const stGains   = form1099.shortTermGainLoss  || 0;   // Schedule D col. (f)
    const ltGains   = form1099.longTermGainLoss   || 0;   // Schedule D col. (h)
    const necIncome = form1099.necIncome          || 0;   // 1099-NEC → Sch. C
    const interest  = form1099.interestIncome     || 0;   // 1099-INT → line 2b
    const ordDiv    = form1099.ordinaryDividends  || 0;   // 1099-DIV → line 3b
    const qualDiv   = form1099.qualifiedDividends || 0;   // 1099-DIV → line 3a (LTCG rate)

    // ── Step 3: Adjusted Gross Income ────────────────────────────────────────
    const totalOrdinaryIncome = salaryUSD + stGains + necIncome + interest
                               + (ordDiv - qualDiv);       // non-qualified portion only
    const agi = totalOrdinaryIncome + Math.max(0, ltGains) + qualDiv;

    // ── Step 4: Taxable income ────────────────────────────────────────────────
    const stdDed = STD_DEDUCTION[fs] ?? 15_000;
    const taxableIncome = Math.max(0, agi - stdDed);

    // ── Step 5: Ordinary income tax ──────────────────────────────────────────
    const brackets = _brackets(fs);
    const ordinaryBase = Math.max(0, totalOrdinaryIncome - stdDed);
    const ordinaryTax  = _progressiveTax(ordinaryBase, brackets);

    // ── Step 6: Long-term capital gains tax (stacking rule, IRC §1(h)) ───────
    const ltcgThresh = LTCG[fs] ?? LTCG.SINGLE;
    const ltGainsTaxable = Math.max(0, ltGains) + qualDiv;  // LTCG-rate income
    const ltcgTax = _ltcgTax(ltGainsTaxable, ordinaryBase, ltcgThresh);

    // ── Step 7: Net Investment Income Tax (IRC §1411) ─────────────────────────
    const niitThresh = NIIT_THRESHOLD[fs] ?? 200_000;
    const netInvIncome = Math.max(0, stGains) + Math.max(0, ltGains)
                       + interest + ordDiv;
    const niitBase = Math.max(0, agi - niitThresh);
    const niitTax  = Math.min(netInvIncome, niitBase) * NIIT_RATE;

    const totalTaxBeforeCredits = ordinaryTax + ltcgTax + niitTax;

    // ── Step 8: Foreign Tax Credit (Form 1116) ────────────────────────────────
    const israeliTaxPaidUSD = israeliTaxPaidNIS / salaryConversionRate;
    let ftc = 0;
    if (!useFEIE && israeliTaxPaidUSD > 0) {
      // Basket limitation: FTC ≤ (foreign income / total income) × US tax
      const foreignIncomeFraction = totalOrdinaryIncome > 0
        ? Math.min(1, salaryUSD / Math.max(1, agi))
        : 0;
      const ftcLimit = foreignIncomeFraction * totalTaxBeforeCredits;
      ftc = Math.min(israeliTaxPaidUSD, ftcLimit, totalTaxBeforeCredits);
    }

    // ── Step 9: Total payments & result ──────────────────────────────────────
    const withheld       = form1099.federalTaxWithheld || 0;
    const totalCredits   = ftc + withheld;
    const netTax         = Math.max(0, totalTaxBeforeCredits - ftc);
    const refundOrOwed   = withheld - netTax;  // positive = refund

    // ── Step 10: FBAR / FATCA ────────────────────────────────────────────────
    const requiresFBAR      = israeliAccountBalanceUSD > FBAR_THRESHOLD;
    const fatcaThresh       = fs === 'MFJ' ? FATCA_MFJ : FATCA_SINGLE;
    const requiresForm8938  = israeliAccountBalanceUSD > fatcaThresh;

    return {
      // Income
      salaryUSD_raw,
      feieExcluded,
      salaryUSD,
      stGains,
      ltGains,
      necIncome,
      interest,
      ordDiv,
      qualDiv,
      agi,

      // Deductions
      standardDeduction: stdDed,
      taxableIncome,

      // Tax
      ordinaryTax,
      ltcgTax,
      niitTax,
      totalTaxBeforeCredits,

      // Credits
      israeliTaxPaidUSD,
      foreignTaxCredit: ftc,
      withheld,
      totalCredits,
      netTax,
      refundOrOwed,

      // Compliance
      requiresFBAR,
      requiresForm8938,
      feieLimit: FEIE_LIMIT,

      // Meta
      filingStatus: fs,
      salaryConversionRate,
    };
  }

  // ─── Internal helpers ────────────────────────────────────────────────────────

  function _brackets(fs) {
    if (fs === 'MFJ' || fs === 'MFS') return BRACKETS_MFJ_2025;
    if (fs === 'HOH') return BRACKETS_HOH_2025;
    return BRACKETS_SINGLE_2025;
  }

  function _progressiveTax(income, brackets) {
    let tax = 0, rem = Math.max(0, income);
    for (const b of brackets) {
      if (rem <= 0) break;
      const inBracket = Math.min(rem, b.upper - b.lower);
      tax += inBracket * b.rate;
      rem -= inBracket;
    }
    return tax;
  }

  /**
   * LTCG "stacking" calculation per IRC §1(h).
   * Long-term gains are layered on top of ordinary income for threshold purposes.
   */
  function _ltcgTax(ltcgAmount, ordinaryTaxableIncome, thresholds) {
    if (ltcgAmount <= 0) return 0;
    let tax = 0, rem = ltcgAmount, base = ordinaryTaxableIncome;

    const zeroRoom    = Math.max(0, thresholds.zero    - base);
    const atZero      = Math.min(rem, zeroRoom);
    rem -= atZero; base += atZero;

    const fifteenRoom = Math.max(0, thresholds.fifteen - base);
    const atFifteen   = Math.min(rem, fifteenRoom);
    tax += atFifteen * 0.15;
    rem -= atFifteen;

    tax += rem * 0.20;  // 20% on remainder
    return tax;
  }

  /**
   * Marginal rate lookup for a given income level.
   */
  function marginalRate(income, filingStatus = 'SINGLE') {
    const brackets = _brackets(filingStatus);
    for (let i = brackets.length - 1; i >= 0; i--) {
      if (income > brackets[i].lower) return brackets[i].rate;
    }
    return 0.10;
  }

  /**
   * Per-bracket breakdown for the report.
   */
  function bracketBreakdown(income, filingStatus = 'SINGLE') {
    const brackets = _brackets(filingStatus);
    const rows = [];
    let rem = Math.max(0, income);
    for (const b of brackets) {
      if (rem <= 0) break;
      const inBracket = Math.min(rem, b.upper - b.lower);
      if (inBracket > 0) {
        rows.push({
          range: `$${_fmt(b.lower)} – ${b.upper === Infinity ? '∞' : '$' + _fmt(b.upper)}`,
          rate: `${(b.rate * 100).toFixed(0)}%`,
          amount: inBracket,
          tax: inBracket * b.rate,
        });
      }
      rem -= inBracket;
    }
    return rows;
  }

  function _fmt(n) {
    return n.toLocaleString('en-US', { maximumFractionDigits: 0 });
  }

  return {
    calculate,
    marginalRate,
    bracketBreakdown,
    BRACKETS_SINGLE_2025,
    BRACKETS_MFJ_2025,
    STD_DEDUCTION,
    LTCG,
    FEIE_LIMIT,
    FBAR_THRESHOLD,
  };
})();
