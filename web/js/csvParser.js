/**
 * CsvParser – Parse IBKR 1099-B trade exports.
 *
 * Supports three IBKR formats:
 *   1. Activity Statement (Trades section) – exported from IBKR portal
 *   2. Gain/Loss Report – ST/LT flag, Date Acquired, Date Sold
 *   3. Generic trade CSV – auto-detected column mapping
 *
 * Returns an array of TradeTransaction objects compatible with IsraeliTaxCalc.
 */
const CsvParser = (() => {

  /**
   * Main entry point.
   * @param {string} csvText  Raw CSV file text
   * @returns {{ transactions: TradeTransaction[], warnings: string[] }}
   */
  function parse(csvText) {
    const lines   = splitLines(csvText);
    const rows    = lines.map(parseCsvLine);
    const format  = detectFormat(rows);

    switch (format) {
      case 'IBKR_GL':      return parseIBKR_GL(rows);
      case 'IBKR_TRADES':  return parseIBKR_Trades(rows);
      default:             return parseGeneric(rows);
    }
  }

  // ─── Format detection ─────────────────────────────────────────────────────

  function detectFormat(rows) {
    for (const row of rows.slice(0, 20)) {
      const joined = row.join(',').toLowerCase();
      if (joined.includes('date acquired') && joined.includes('date sold')) return 'IBKR_GL';
      if (joined.includes('date/time') && joined.includes('t. price'))     return 'IBKR_TRADES';
    }
    return 'GENERIC';
  }

  // ─── IBKR Gain / Loss Report ──────────────────────────────────────────────

  function parseIBKR_GL(rows) {
    const transactions = [], warnings = [];
    let headerIdx = -1, headers = [];

    for (let i = 0; i < rows.length; i++) {
      const r = rows[i];
      if (r.some(c => /date acquired/i.test(c) && r.some(c2 => /date sold/i.test(c2)))) {
        headerIdx = i;
        headers   = r.map(h => h.trim().toLowerCase());
        break;
      }
    }

    if (headerIdx === -1) return { transactions: [], warnings: ['Could not find header row'] };

    const col = (name, alt = '') => {
      const idx = headers.indexOf(name);
      if (idx !== -1) return idx;
      const idx2 = headers.indexOf(alt);
      return idx2;
    };

    const iSymbol    = col('symbol');
    const iAcq       = col('date acquired');
    const iSold      = col('date sold');
    const iProceeds  = col('proceeds');
    const iBasis     = col('cost basis', 'basis');
    const iGL        = col('gain/loss', 'realized p/l');
    const iStLt      = col('st or lt', 'term');
    const iWash      = col('wash sale loss disallowed', 'disallowed loss');

    for (let i = headerIdx + 1; i < rows.length; i++) {
      const r = rows[i];
      if (r.length < 4 || r.every(c => !c.trim())) continue;
      // Skip IBKR subtotal/summary rows
      if (/^(total|subtotal|summary)/i.test(r[0])) continue;

      const proceeds  = parseNum(r[iProceeds]);
      const costBasis = parseNum(r[iBasis]);
      const gainLoss  = iGL >= 0 ? parseNum(r[iGL]) : proceeds - costBasis;
      const washAdj   = iWash >= 0 ? parseNum(r[iWash]) : 0;
      const dateSold  = r[iSold]?.trim() || '';
      const dateAcq   = r[iAcq]?.trim()  || 'VARIOUS';

      if (!dateSold || proceeds === 0 && costBasis === 0) continue;

      const holdingPeriod = determineHolding(dateAcq, dateSold, r[iStLt]);

      transactions.push({
        description:  r[iSymbol]?.trim() || '',
        dateAcquired: dateAcq,
        dateSold:     dateSold,
        proceeds:     proceeds,
        costBasis:    costBasis,
        washSaleAdj:  washAdj,
        gainLoss:     gainLoss + washAdj,
        holdingPeriod,
        covered:      true,
        // Exchange rate fields – populated later by ExchangeRateService
        exchangeRateOnSaleDate: 0,
        manualRate: 0,
        proceedsNIS:  0,
        costBasisNIS: 0,
        gainLossNIS:  0,
        rateSource:   'Pending',
      });
    }

    return { transactions, warnings };
  }

  // ─── IBKR Activity Statement – Trades section ────────────────────────────

  function parseIBKR_Trades(rows) {
    const transactions = [], warnings = [];
    let inTradesSection = false;
    let headers = [];

    for (const row of rows) {
      // Detect section header
      if (row[0] === 'Trades') {
        inTradesSection = true;
        continue;
      }
      if (inTradesSection && row[0] && row[0] !== 'Trades' && !row[0].startsWith('Data')) {
        inTradesSection = false;
        continue;
      }
      if (!inTradesSection) continue;
      if (row[0] === 'Data' || row[0] === 'Header') {
        if (row.some(c => /date\/time/i.test(c))) {
          headers = row.map(h => h.trim().toLowerCase());
        }
        continue;
      }

      if (headers.length === 0) continue;

      const iSymbol   = headers.indexOf('symbol');
      const iDate     = headers.indexOf('date/time');
      const iProc     = headers.indexOf('proceeds');
      const iBasis    = headers.indexOf('basis');
      const iGL       = headers.indexOf('realized p/l');

      const dateSold  = (row[iDate] || '').split(',')[0].trim();
      const proceeds  = parseNum(row[iProc]);
      const costBasis = parseNum(row[iBasis]);
      const gainLoss  = iGL >= 0 ? parseNum(row[iGL]) : proceeds - costBasis;

      if (!dateSold || (proceeds === 0 && costBasis === 0)) continue;

      transactions.push({
        description:  row[iSymbol]?.trim() || '',
        dateAcquired: 'VARIOUS',
        dateSold,
        proceeds,
        costBasis,
        washSaleAdj:  0,
        gainLoss,
        holdingPeriod: 'SHORT_TERM',   // activity statement doesn't give holding period
        covered: true,
        exchangeRateOnSaleDate: 0,
        manualRate: 0,
        proceedsNIS: 0, costBasisNIS: 0, gainLossNIS: 0,
        rateSource: 'Pending',
      });
    }

    return { transactions, warnings };
  }

  // ─── Generic CSV ─────────────────────────────────────────────────────────

  function parseGeneric(rows) {
    const transactions = [], warnings = [];
    if (rows.length < 2) return { transactions, warnings: ['Empty file'] };

    const headers = rows[0].map(h => h.trim().toLowerCase());
    const find = (...names) => {
      for (const n of names) {
        const i = headers.findIndex(h => h.includes(n));
        if (i !== -1) return i;
      }
      return -1;
    };

    const iSym    = find('symbol', 'ticker', 'security');
    const iAcq    = find('date acquired', 'acquisition', 'open date');
    const iSold   = find('date sold', 'close date', 'sell date', 'date');
    const iProc   = find('proceeds', 'sale', 'revenue');
    const iBasis  = find('cost basis', 'cost', 'basis', 'purchase');
    const iGL     = find('gain/loss', 'gain', 'p/l', 'pnl');
    const iStLt   = find('st or lt', 'term', 'holding');

    if (iSold === -1 || iProc === -1) {
      return { transactions: [], warnings: ['Cannot find required columns (Date Sold, Proceeds)'] };
    }

    for (let i = 1; i < rows.length; i++) {
      const r = rows[i];
      if (r.every(c => !c.trim())) continue;
      const proceeds  = parseNum(r[iProc]);
      const costBasis = iBasis >= 0 ? parseNum(r[iBasis]) : 0;
      const gainLoss  = iGL >= 0   ? parseNum(r[iGL]) : proceeds - costBasis;
      const dateSold  = r[iSold]?.trim() || '';
      const dateAcq   = iAcq >= 0 ? (r[iAcq]?.trim() || 'VARIOUS') : 'VARIOUS';
      if (!dateSold) continue;

      transactions.push({
        description:  iSym >= 0 ? (r[iSym]?.trim() || '') : `Row ${i}`,
        dateAcquired: dateAcq,
        dateSold,
        proceeds,
        costBasis,
        washSaleAdj: 0,
        gainLoss,
        holdingPeriod: determineHolding(dateAcq, dateSold, iStLt >= 0 ? r[iStLt] : ''),
        covered: true,
        exchangeRateOnSaleDate: 0,
        manualRate: 0,
        proceedsNIS: 0, costBasisNIS: 0, gainLossNIS: 0,
        rateSource: 'Pending',
      });
    }

    if (transactions.length === 0) {
      warnings.push('No valid trade rows found');
    }
    return { transactions, warnings };
  }

  // ─── Utilities ───────────────────────────────────────────────────────────

  /**
   * Parse a number from strings like "1,234.56" or "(500.00)" or "$3,200".
   */
  function parseNum(str) {
    if (!str) return 0;
    const s = str.toString().replace(/[$, ]/g, '');
    if (s.startsWith('(') && s.endsWith(')')) return -parseFloat(s.slice(1, -1)) || 0;
    return parseFloat(s) || 0;
  }

  /**
   * Determine holding period.
   *   - ST/LT flag from the row takes priority
   *   - Then calculate from dates if available
   *   - Default: SHORT_TERM
   */
  function determineHolding(dateAcquired, dateSold, stltFlag) {
    const f = (stltFlag || '').toString().trim().toLowerCase();
    if (f.startsWith('l')) return 'LONG_TERM';
    if (f.startsWith('s')) return 'SHORT_TERM';

    // Try date math
    const acq  = parseDate(dateAcquired);
    const sold = parseDate(dateSold);
    if (acq && sold) {
      const diffDays = (sold - acq) / 86_400_000;
      return diffDays > 365 ? 'LONG_TERM' : 'SHORT_TERM';
    }
    return 'SHORT_TERM';
  }

  /** Parse MM/DD/YYYY or YYYY-MM-DD → Date */
  function parseDate(str) {
    if (!str || str.toUpperCase() === 'VARIOUS') return null;
    const iso = /^\d{4}-\d{2}-\d{2}$/.test(str) ? str : (() => {
      const m = str.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
      return m ? `${m[3]}-${m[1].padStart(2,'0')}-${m[2].padStart(2,'0')}` : null;
    })();
    if (!iso) return null;
    const d = new Date(iso + 'T12:00:00Z');
    return isNaN(d) ? null : d;
  }

  /**
   * Split CSV into lines, handling quoted newlines.
   */
  function splitLines(text) {
    return text.split(/\r?\n/).filter(l => l.trim());
  }

  /**
   * Parse a single CSV line, respecting quoted fields.
   */
  function parseCsvLine(line) {
    const result = [];
    let cur = '', inQuote = false;
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      if (ch === '"') {
        if (inQuote && line[i + 1] === '"') { cur += '"'; i++; }
        else inQuote = !inQuote;
      } else if (ch === ',' && !inQuote) {
        result.push(cur.trim());
        cur = '';
      } else {
        cur += ch;
      }
    }
    result.push(cur.trim());
    return result;
  }

  /**
   * Aggregate totals from parsed transactions.
   */
  function summarize(transactions) {
    let stGL = 0, ltGL = 0, proceeds = 0, basis = 0, washAdj = 0;
    for (const t of transactions) {
      proceeds += t.proceeds;
      basis    += t.costBasis;
      washAdj  += t.washSaleAdj;
      if (t.holdingPeriod === 'LONG_TERM') ltGL += t.gainLoss;
      else                                 stGL += t.gainLoss;
    }
    return { stGL, ltGL, totalGL: stGL + ltGL, proceeds, basis, washAdj };
  }

  return { parse, parseNum, summarize, parseCsvLine };
})();
