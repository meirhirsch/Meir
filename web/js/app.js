/**
 * app.js – US–Israel Tax Assistant
 * Main application controller: state management, navigation, UI wiring.
 */

// ─── Application State ───────────────────────────────────────────────────────

const State = {
  currentStep: 1,
  taxYear: 2025,
  filingStatus: 'SINGLE',
  taxpayerName: '',
  taxpayerID: '',
  ssn: '',
  israeliAccountBalanceUSD: 0,
  useFEIE: false,

  // Form data
  form106: {
    employerName: '', employerId: '', taxYear: 2025,
    grossIncome: 0, taxableIncome: 0, taxExemptIncome: 0,
    incomeTaxWithheld: 0,
    bituachLeumiEmployee: 0, bituachLeumiEmployer: 0,
    healthInsurance: 0, pensionEmployee: 0, pensionEmployer: 0,
    studyFund: 0, taxCreditsPoints: 0, taxCreditsAmount: 0,
  },
  form1099: {
    necIncome: 0, necWithheld: 0,
    interestIncome: 0, interestWithheld: 0,
    ordinaryDividends: 0, qualifiedDividends: 0, divWithheld: 0,
    shortTermGainLoss: 0, longTermGainLoss: 0, federalTaxWithheld: 0,
    washSaleLossDisallowed: 0,
  },
  transactions: [],         // enriched TradeTransaction[]
  annualAvgRate: 0,

  // Computed results
  israeliResult: null,
  usResult: null,
};

// ─── Navigation ──────────────────────────────────────────────────────────────

function goToStep(n) {
  if (n < 1 || n > 5) return;
  document.querySelectorAll('.step-content').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.step-nav-item').forEach(s => {
    s.classList.toggle('active',    parseInt(s.dataset.step) === n);
    s.classList.toggle('completed', parseInt(s.dataset.step) < n);
  });
  document.getElementById(`step-${n}`)?.classList.add('active');
  State.currentStep = n;
  window.scrollTo(0, 0);
}

function nextStep() {
  if (State.currentStep === 3) syncTradesFromUI();
  goToStep(State.currentStep + 1);
}
function prevStep() { goToStep(State.currentStep - 1); }

// ─── Step 1: Personal Info ───────────────────────────────────────────────────

function bindStep1() {
  const bind = (id, key, parse = v => v) =>
    document.getElementById(id)?.addEventListener('change', e => { State[key] = parse(e.target.value); });

  bind('taxYear',        'taxYear',        Number);
  bind('filingStatus',   'filingStatus');
  bind('taxpayerName',   'taxpayerName');
  bind('taxpayerID',     'taxpayerID');
  bind('ssn',            'ssn');
  bind('accountBalance', 'israeliAccountBalanceUSD', Number);
  document.getElementById('useFEIE')?.addEventListener('change', e => {
    State.useFEIE = e.target.checked;
  });
}

// ─── Step 2: Form 106 ────────────────────────────────────────────────────────

function bindStep2() {
  const fields = [
    'employerName','employerId',
    'grossIncome','taxableIncome','taxExemptIncome',
    'incomeTaxWithheld','bituachLeumiEmployee','bituachLeumiEmployer',
    'healthInsurance','pensionEmployee','pensionEmployer',
    'studyFund','taxCreditsPoints',
  ];
  fields.forEach(f => {
    const el = document.getElementById(`f106_${f}`);
    if (!el) return;
    el.addEventListener('input', () => {
      State.form106[f] = el.type === 'number' ? (parseFloat(el.value) || 0) : el.value;
      // Auto-calculate credit amount
      if (f === 'taxCreditsPoints') {
        const amt = (parseFloat(el.value) || 0) * IsraeliTaxCalc.CREDIT_POINT_VALUE;
        const amtEl = document.getElementById('f106_taxCreditsAmount');
        if (amtEl) amtEl.value = amt.toFixed(0);
        State.form106.taxCreditsAmount = amt;
      }
    });
  });
}

// ─── Step 3: Form 1099 ───────────────────────────────────────────────────────

function bindStep3() {
  const fields = [
    'necIncome','necWithheld',
    'interestIncome','interestWithheld',
    'ordinaryDividends','qualifiedDividends','divWithheld',
    'shortTermGainLoss','longTermGainLoss','federalTaxWithheld','washSaleLossDisallowed',
  ];
  fields.forEach(f => {
    const el = document.getElementById(`f1099_${f}`);
    if (!el) return;
    el.addEventListener('input', () => {
      State.form1099[f] = parseFloat(el.value) || 0;
    });
  });

  // CSV import button
  document.getElementById('csvImportBtn')?.addEventListener('click', () => {
    document.getElementById('csvFileInput')?.click();
  });
  document.getElementById('csvFileInput')?.addEventListener('change', handleCsvImport);

  // Add trade row button
  document.getElementById('addTradeBtn')?.addEventListener('click', addTradeRow);
}

async function handleCsvImport(e) {
  const file = e.target.files[0];
  if (!file) return;
  const statusEl = document.getElementById('csvStatus');
  statusEl.textContent = `Reading ${file.name}…`;

  try {
    const text = await file.text();
    const { transactions, warnings } = CsvParser.parse(text);
    if (transactions.length === 0) {
      statusEl.textContent = `No trades found. ${warnings.join(' ')}`;
      return;
    }
    const summary = CsvParser.summarize(transactions);
    State.transactions = transactions;
    renderTradeTable(transactions);

    // Update 1099-B summary fields
    const stEl = document.getElementById('f1099_shortTermGainLoss');
    const ltEl = document.getElementById('f1099_longTermGainLoss');
    if (stEl) { stEl.value = summary.stGL.toFixed(2); State.form1099.shortTermGainLoss = summary.stGL; }
    if (ltEl) { ltEl.value = summary.ltGL.toFixed(2); State.form1099.longTermGainLoss  = summary.ltGL; }

    statusEl.innerHTML = `<span class="ok">✓ Imported ${transactions.length} trades | ST: $${summary.stGL.toFixed(2)} | LT: $${summary.ltGL.toFixed(2)}</span>`;
    if (warnings.length) statusEl.innerHTML += `<br><span class="warn">${warnings.join('; ')}</span>`;
  } catch (err) {
    statusEl.innerHTML = `<span class="err">Error: ${err.message}</span>`;
  }
  e.target.value = '';
}

function renderTradeTable(transactions) {
  const tbody = document.getElementById('tradeTableBody');
  if (!tbody) return;
  tbody.innerHTML = '';
  transactions.forEach((t, i) => {
    const tr = document.createElement('tr');
    tr.dataset.idx = i;
    tr.innerHTML = `
      <td><input class="trade-desc" value="${escHtml(t.description)}" placeholder="Symbol"></td>
      <td><input class="trade-acq"  value="${escHtml(t.dateAcquired)}" placeholder="MM/DD/YYYY"></td>
      <td><input class="trade-sold" value="${escHtml(t.dateSold)}"     placeholder="MM/DD/YYYY"></td>
      <td><select class="trade-hp">
            <option value="SHORT_TERM" ${t.holdingPeriod==='SHORT_TERM'?'selected':''}>ST</option>
            <option value="LONG_TERM"  ${t.holdingPeriod==='LONG_TERM' ?'selected':''}>LT</option>
          </select></td>
      <td><input class="trade-proc" type="number" step="0.01" value="${t.proceeds.toFixed(2)}"></td>
      <td><input class="trade-cost" type="number" step="0.01" value="${t.costBasis.toFixed(2)}"></td>
      <td class="num trade-gl ${t.gainLoss < 0 ? 'loss' : 'gain'}">${t.gainLoss.toFixed(2)}</td>
      <td><input class="trade-wash" type="number" step="0.01" value="${t.washSaleAdj.toFixed(2)}"></td>
      <td><input class="trade-rate" type="number" step="0.0001" value="${t.exchangeRateOnSaleDate || ''}" placeholder="Auto"></td>
      <td class="rate-status">${t.rateSource === 'BOI' ? '✓' : t.rateSource === 'Manual' ? '✎' : '⏳'}</td>
      <td><button class="del-btn" onclick="deleteTrade(${i})">✕</button></td>`;

    // Live gain/loss calculation
    const recalc = () => {
      const proc = parseFloat(tr.querySelector('.trade-proc').value) || 0;
      const cost = parseFloat(tr.querySelector('.trade-cost').value) || 0;
      const wash = parseFloat(tr.querySelector('.trade-wash').value) || 0;
      const gl   = proc - cost + wash;
      const glEl = tr.querySelector('.trade-gl');
      glEl.textContent = gl.toFixed(2);
      glEl.className   = `num trade-gl ${gl < 0 ? 'loss' : 'gain'}`;
    };
    tr.querySelector('.trade-proc').addEventListener('input', recalc);
    tr.querySelector('.trade-cost').addEventListener('input', recalc);
    tr.querySelector('.trade-wash').addEventListener('input', recalc);
    tbody.appendChild(tr);
  });
  updateTradeSummary();
}

function addTradeRow() {
  const blank = {
    description: '', dateAcquired: '', dateSold: '',
    proceeds: 0, costBasis: 0, washSaleAdj: 0, gainLoss: 0,
    holdingPeriod: 'SHORT_TERM', exchangeRateOnSaleDate: 0,
    manualRate: 0, proceedsNIS: 0, costBasisNIS: 0, gainLossNIS: 0, rateSource: 'Pending',
  };
  State.transactions.push(blank);
  renderTradeTable(State.transactions);
}

function deleteTrade(i) {
  State.transactions.splice(i, 1);
  renderTradeTable(State.transactions);
}

function syncTradesFromUI() {
  const tbody = document.getElementById('tradeTableBody');
  if (!tbody) return;
  State.transactions = Array.from(tbody.querySelectorAll('tr')).map(tr => {
    const gl = parseFloat(tr.querySelector('.trade-proc')?.value || 0)
             - parseFloat(tr.querySelector('.trade-cost')?.value || 0)
             + parseFloat(tr.querySelector('.trade-wash')?.value || 0);
    const manualRate = parseFloat(tr.querySelector('.trade-rate')?.value) || 0;
    return {
      description:    tr.querySelector('.trade-desc')?.value.trim()  || '',
      dateAcquired:   tr.querySelector('.trade-acq')?.value.trim()   || 'VARIOUS',
      dateSold:       tr.querySelector('.trade-sold')?.value.trim()  || '',
      holdingPeriod:  tr.querySelector('.trade-hp')?.value           || 'SHORT_TERM',
      proceeds:       parseFloat(tr.querySelector('.trade-proc')?.value) || 0,
      costBasis:      parseFloat(tr.querySelector('.trade-cost')?.value) || 0,
      washSaleAdj:    parseFloat(tr.querySelector('.trade-wash')?.value) || 0,
      gainLoss:       gl,
      manualRate,
      exchangeRateOnSaleDate: manualRate,
      proceedsNIS: 0, costBasisNIS: 0, gainLossNIS: 0,
      rateSource: manualRate > 0 ? 'Manual' : 'Pending',
    };
  });
  const summary = CsvParser.summarize(State.transactions);
  State.form1099.shortTermGainLoss = summary.stGL;
  State.form1099.longTermGainLoss  = summary.ltGL;
}

function updateTradeSummary() {
  const summary = CsvParser.summarize(State.transactions);
  const el = document.getElementById('tradeSummary');
  if (el) el.innerHTML =
    `Trades: <strong>${State.transactions.length}</strong> &nbsp;|&nbsp; ` +
    `ST: <strong class="${summary.stGL < 0 ? 'loss' : 'gain'}">$${summary.stGL.toFixed(2)}</strong> &nbsp;|&nbsp; ` +
    `LT: <strong class="${summary.ltGL < 0 ? 'loss' : 'gain'}">$${summary.ltGL.toFixed(2)}</strong>`;
}

// ─── Step 4: Fetch Rates & Calculate ─────────────────────────────────────────

async function runCalculation() {
  syncTradesFromUI();
  const btn    = document.getElementById('calcBtn');
  const status = document.getElementById('calcStatus');
  btn.disabled = true;
  btn.textContent = 'Calculating…';

  try {
    // 1. Fetch BOI annual average
    status.innerHTML = '<span class="info">Fetching Bank of Israel exchange rates…</span>';
    let annualAvg = 0;
    try {
      annualAvg = await ExchangeRateService.getAnnualAverage(State.taxYear);
      State.annualAvgRate = annualAvg;
      document.getElementById('annualAvgDisplay').textContent =
        annualAvg > 0 ? `₪${annualAvg.toFixed(4)} per $1` : 'Not fetched';
    } catch (e) {
      status.innerHTML += `<br><span class="warn">⚠ Could not fetch annual average: ${e.message}</span>`;
    }

    // 2. Enrich trades with per-date BOI rates
    if (State.transactions.length > 0) {
      status.innerHTML += '<br><span class="info">Fetching per-trade exchange rates…</span>';
      let done = 0;
      State.transactions = await ExchangeRateService.enrichTransactions(
        State.transactions,
        (d, total) => {
          done = d;
          status.innerHTML = `<span class="info">Exchange rates: ${d}/${total} trades enriched…</span>`;
        }
      );
    }

    // 3. US tax calculation (first pass – needed for Israeli FTC)
    const usResult = USTaxCalc.calculate({
      form106:                  State.form106,
      form1099:                 State.form1099,
      filingStatus:             State.filingStatus,
      israeliTaxPaidNIS:        State.form106.incomeTaxWithheld,
      useFEIE:                  State.useFEIE,
      israeliAccountBalanceUSD: State.israeliAccountBalanceUSD,
      salaryConversionRate:     annualAvg || 3.70,
    });
    State.usResult = usResult;

    // 4. Israeli tax calculation (uses US tax paid as FTC)
    const israeliResult = IsraeliTaxCalc.calculate(
      State.form106,
      State.transactions,
      annualAvg,
      usResult.netTax * (annualAvg || 3.70),   // US tax in NIS
      State.filingStatus
    );
    State.israeliResult = israeliResult;

    // 5. Render results
    renderResults();

    // 6. Navigate to results tab
    goToStep(5);
    status.innerHTML = '<span class="ok">✓ Calculation complete!</span>';

  } catch (err) {
    status.innerHTML = `<span class="err">Error: ${err.message}</span>`;
    console.error(err);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Calculate Taxes';
  }
}

// ─── Step 5: Results ─────────────────────────────────────────────────────────

function renderResults() {
  const session = {
    taxYear:       State.taxYear,
    filingStatus:  State.filingStatus,
    taxpayerName:  State.taxpayerName,
    taxpayerID:    State.taxpayerID,
    ssn:           State.ssn,
  };

  // Render 1301 report
  const r1301 = document.getElementById('report1301');
  if (r1301 && State.israeliResult) {
    r1301.innerHTML = ReportGenerator.generate1301(
      State.form106, State.israeliResult, State.usResult, session
    );
  }

  // Render 1040 report
  const r1040 = document.getElementById('report1040');
  if (r1040 && State.usResult) {
    r1040.innerHTML = ReportGenerator.generate1040(
      State.form106, State.form1099, State.usResult, session
    );
  }

  // Render schedule D
  const rSched = document.getElementById('reportSchedule');
  if (rSched) {
    rSched.innerHTML = ReportGenerator.generateTradeSchedule(
      State.transactions, State.annualAvgRate
    );
  }

  // Summary cards
  renderSummaryCards();
}

function renderSummaryCards() {
  const r = State.israeliResult;
  const u = State.usResult;
  const cards = document.getElementById('summaryCards');
  if (!cards) return;

  const card = (title, value, cls = '') =>
    `<div class="summary-card ${cls}"><div class="card-title">${title}</div><div class="card-value">${value}</div></div>`;

  const nilIsraeli = !r ? '<em>—</em>' : '';
  const nilUS      = !u ? '<em>—</em>' : '';

  cards.innerHTML = [
    card('🇮🇱 Israeli Tax Liability', r ? `₪${Math.abs(r.totalTaxLiability).toLocaleString('he-IL',{minimumFractionDigits:0})}` : nilIsraeli),
    card('🇮🇱 Israeli ' + (r && r.refundOrOwed >= 0 ? 'Refund' : 'Owed'),
         r ? `${r.refundOrOwed >= 0 ? '+' : '−'}₪${Math.abs(r.refundOrOwed).toLocaleString('he-IL',{minimumFractionDigits:0})}` : nilIsraeli,
         r ? (r.refundOrOwed >= 0 ? 'card-refund' : 'card-owed') : ''),
    card('🇺🇸 US Tax Liability',     u ? `$${Math.abs(u.netTax).toLocaleString('en-US',{minimumFractionDigits:0})}` : nilUS),
    card('🇺🇸 US ' + (u && u.refundOrOwed >= 0 ? 'Refund' : 'Owed'),
         u ? `${u.refundOrOwed >= 0 ? '+' : '−'}$${Math.abs(u.refundOrOwed).toLocaleString('en-US',{minimumFractionDigits:0})}` : nilUS,
         u ? (u.refundOrOwed >= 0 ? 'card-refund' : 'card-owed') : ''),
    card('BOI Annual Avg Rate', State.annualAvgRate > 0
         ? `₪${State.annualAvgRate.toFixed(4)}/$` : '—'),
    card('Total Trades', State.transactions.length.toString()),
  ].join('');
}

// ─── Tab switching (Results page) ─────────────────────────────────────────────

function bindResultTabs() {
  document.querySelectorAll('.result-tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const target = btn.dataset.tab;
      document.querySelectorAll('.result-tab-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.result-tab-pane').forEach(p => p.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById(`tab-${target}`)?.classList.add('active');
    });
  });
}

// ─── Print ────────────────────────────────────────────────────────────────────

function printReport(reportId) {
  const el = document.getElementById(reportId);
  if (!el) return;
  const win = window.open('', '_blank');
  win.document.write(`<!DOCTYPE html><html><head>
    <meta charset="UTF-8">
    <title>Tax Report</title>
    <link rel="stylesheet" href="css/styles.css">
    <style>body{padding:20px} .no-print{display:none}</style>
    </head><body>${el.innerHTML}</body></html>`);
  win.document.close();
  win.onload = () => win.print();
}

function printAll() {
  const reports = ['report1301','report1040','reportSchedule']
    .map(id => document.getElementById(id)?.innerHTML || '')
    .join('<div style="page-break-before:always"></div>');
  const win = window.open('', '_blank');
  win.document.write(`<!DOCTYPE html><html><head>
    <meta charset="UTF-8"><title>Full Tax Report</title>
    <link rel="stylesheet" href="css/styles.css">
    <style>body{padding:20px} .no-print{display:none}</style>
    </head><body>${reports}</body></html>`);
  win.document.close();
  win.onload = () => win.print();
}

// ─── Utilities ────────────────────────────────────────────────────────────────

function escHtml(str) {
  return (str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function showTab(tabName) {
  document.querySelectorAll('.tab-content').forEach(t => t.style.display = 'none');
  document.getElementById('tab-' + tabName).style.display = 'block';
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === tabName));
}

// ─── Init ─────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
  // Wire navigation
  document.querySelectorAll('.step-nav-item').forEach(el => {
    el.addEventListener('click', () => goToStep(parseInt(el.dataset.step)));
  });
  document.querySelectorAll('.btn-next').forEach(b => b.addEventListener('click', nextStep));
  document.querySelectorAll('.btn-prev').forEach(b => b.addEventListener('click', prevStep));

  // Wire steps
  bindStep1();
  bindStep2();
  bindStep3();
  bindResultTabs();

  // Calculate button
  document.getElementById('calcBtn')?.addEventListener('click', runCalculation);

  // Print buttons
  document.getElementById('printAll')?.addEventListener('click', printAll);
  document.getElementById('print1301')?.addEventListener('click', () => printReport('report1301'));
  document.getElementById('print1040')?.addEventListener('click', () => printReport('report1040'));
  document.getElementById('printSchedule')?.addEventListener('click', () => printReport('reportSchedule'));

  // Set default year
  const yearEl = document.getElementById('taxYear');
  if (yearEl) yearEl.value = State.taxYear;
});
