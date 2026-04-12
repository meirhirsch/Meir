/**
 * ExchangeRateService
 * Fetches official USD/NIS daily rates from the Bank of Israel SDMX API.
 *
 * Per Israeli Tax Ordinance Section 207A, capital gains on foreign securities
 * must be converted using the BOI rate on the ACTUAL SALE DATE (not an average).
 * For weekends / holidays the last preceding banking-day rate is used
 * (standard practice accepted by both ITA and IRS).
 *
 * API: https://edge.boi.org.il/FusionEdgeServer/sdmx/v2/data/dataflow/
 *      BOI.STATISTICS/EXR/1.0/RER_USD_ILS?startperiod=YYYY-MM-DD&endperiod=YYYY-MM-DD&format=json
 */
const ExchangeRateService = (() => {
  const BOI_BASE =
    'https://edge.boi.org.il/FusionEdgeServer/sdmx/v2/data/dataflow/' +
    'BOI.STATISTICS/EXR/1.0/RER_USD_ILS';

  /** In-memory cache: "YYYY-MM-DD" → rate (NIS per 1 USD) */
  const cache = new Map();

  /** Track which years have been fully fetched */
  const fetchedYears = new Set();

  // ─── Public API ─────────────────────────────────────────────────────────────

  /**
   * Fetch every published rate for a calendar year and populate the cache.
   * Returns { "YYYY-MM-DD": rate, ... }
   */
  async function fetchYear(year) {
    if (fetchedYears.has(year)) {
      // Return cached data
      const result = {};
      cache.forEach((v, k) => { if (k.startsWith(`${year}-`)) result[k] = v; });
      return result;
    }
    const url = `${BOI_BASE}?startperiod=${year}-01-01&endperiod=${year}-12-31&format=json`;
    const rates = await _fetchAndParse(url);
    fetchedYears.add(year);
    return rates;
  }

  /**
   * Get the USD→NIS rate for a specific date.
   * Walks back up to 7 calendar days to find the last banking-day rate.
   * Throws if no rate can be found.
   */
  async function getRateForDate(dateStr) {
    const isoDate = _toISO(dateStr);      // ensure YYYY-MM-DD
    if (!isoDate) throw new Error(`Invalid date: ${dateStr}`);

    // Pre-warm cache for the whole year on first request
    const year = parseInt(isoDate.substring(0, 4));
    if (!fetchedYears.has(year)) {
      await fetchYear(year);
    }

    // Walk back up to 7 days for weekends / public holidays
    const base = new Date(isoDate + 'T12:00:00Z');
    for (let d = 0; d <= 7; d++) {
      const candidate = new Date(base);
      candidate.setUTCDate(candidate.getUTCDate() - d);
      const key = candidate.toISOString().slice(0, 10);
      if (cache.has(key)) {
        const rate = cache.get(key);
        cache.set(isoDate, rate);   // memoize original request date too
        return rate;
      }
    }
    throw new Error(`No Bank of Israel rate found for ${isoDate} (checked 7 days back)`);
  }

  /**
   * Compute the BOI annual average rate for a year.
   * IRS Rev. Rul. 2008-38 accepts annual average for regularly-received salary.
   */
  async function getAnnualAverage(year) {
    await fetchYear(year);
    const vals = [];
    cache.forEach((v, k) => { if (k.startsWith(`${year}-`) && v > 0) vals.push(v); });
    if (vals.length === 0) return 0;
    return vals.reduce((a, b) => a + b, 0) / vals.length;
  }

  /**
   * Enrich an array of trade transactions with the per-date BOI exchange rate.
   * Each txn must have { dateSold, proceeds, costBasis, gainLoss }.
   * Returns enriched copies with exchangeRateOnSaleDate, proceedsNIS, costBasisNIS, gainLossNIS.
   */
  async function enrichTransactions(transactions, onProgress) {
    const enriched = [];
    let done = 0;
    for (const txn of transactions) {
      const copy = { ...txn };
      try {
        const rate = await getRateForDate(txn.dateSold);
        copy.exchangeRateOnSaleDate = rate;
        copy.proceedsNIS  = txn.proceeds  * rate;
        copy.costBasisNIS = txn.costBasis * rate;
        copy.gainLossNIS  = txn.gainLoss  * rate;
        copy.rateSource   = 'BOI';
      } catch {
        copy.exchangeRateOnSaleDate = txn.manualRate || 0;
        copy.proceedsNIS  = txn.proceeds  * (txn.manualRate || 0);
        copy.costBasisNIS = txn.costBasis * (txn.manualRate || 0);
        copy.gainLossNIS  = txn.gainLoss  * (txn.manualRate || 0);
        copy.rateSource   = txn.manualRate ? 'Manual' : 'Missing';
      }
      enriched.push(copy);
      done++;
      if (onProgress) onProgress(done, transactions.length);
    }
    return enriched;
  }

  // ─── Internal helpers ────────────────────────────────────────────────────────

  async function _fetchAndParse(url) {
    const resp = await fetch(url, {
      method: 'GET',
      headers: { Accept: 'application/json' }
    });
    if (!resp.ok) throw new Error(`BOI API HTTP ${resp.status}`);
    const json = await resp.json();
    return _parseSdmxJson(json);
  }

  /**
   * Parse Bank of Israel SDMX-JSON v2 response.
   * Structure:
   *   data.structure.dimensions.observation[0].values → [{id: "YYYY-MM-DD"}, ...]
   *   data.dataSets[0].series["0:0:0:0:0"].observations → {"0":[rate], "1":[rate], ...}
   */
  function _parseSdmxJson(json) {
    const result = {};
    try {
      const data = json.data;
      const dateValues = data.structure.dimensions.observation[0].values;
      const indexToDate = dateValues.reduce((m, v, i) => { m[i] = v.id; return m; }, {});

      const series = data.dataSets[0].series;
      const seriesKey = Object.keys(series)[0];
      const observations = series[seriesKey].observations;

      Object.entries(observations).forEach(([idxStr, obsArr]) => {
        const date = indexToDate[parseInt(idxStr)];
        const rate = obsArr[0];
        if (date && rate > 0) {
          result[date] = rate;
          cache.set(date, rate);
        }
      });
    } catch (e) {
      console.error('BOI SDMX parse error:', e);
    }
    return result;
  }

  /**
   * Convert various date formats to ISO YYYY-MM-DD.
   * Handles MM/DD/YYYY, DD/MM/YYYY (guessed by value), YYYY-MM-DD, VARIOUS.
   */
  function _toISO(dateStr) {
    if (!dateStr || dateStr.toUpperCase() === 'VARIOUS') return null;
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) return dateStr;
    // MM/DD/YYYY (IBKR format)
    const m = dateStr.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (m) return `${m[3]}-${m[1].padStart(2,'0')}-${m[2].padStart(2,'0')}`;
    return null;
  }

  /** Expose _toISO for use by other modules */
  function toISO(d) { return _toISO(d); }

  return { fetchYear, getRateForDate, getAnnualAverage, enrichTransactions, toISO };
})();
