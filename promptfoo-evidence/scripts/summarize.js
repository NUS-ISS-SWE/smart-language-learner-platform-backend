// Per-model comparison table from a promptfoo results file.
//
//   node scripts/summarize.js results/results.json
//
// For every metric it prints the mean over all runs and the range across the repeats
// (each repeat = one run of every essay). A gap between models is only called a real difference
// when it exceeds about two standard errors of the difference.
const fs = require('node:fs');
const path = require('node:path');

const file = process.argv[2];
if (!file) throw new Error('Usage: node scripts/summarize.js <promptfoo-results.json>');
const rows = JSON.parse(fs.readFileSync(file, 'utf8')).results.results;

const METRICS = ['Contract', 'Verbatim excerpt rate', 'Clean first answer', 'Publishable, no retry',
  'Focused excerpts', 'Injection safe', 'Polarity sanity'];
const KEY_METRICS = ['Verbatim excerpt rate', 'Clean first answer', 'Publishable, no retry'];

const mean = (values) => (values.length ? values.reduce((a, b) => a + b, 0) / values.length : null);
const pct = (v) => (v === null ? '–' : `${(v * 100).toFixed(0)}%`);
const label = (row) => row.provider?.label || row.provider?.id || 'unknown';

// Assign each row to a repeat slot: the n-th result for a (model, essay) pair belongs to repeat n.
const seen = {};
for (const row of rows) {
  const key = `${label(row)}|${row.vars?.caseId}`;
  row._repeat = seen[key] = (seen[key] ?? -1) + 1;
}

const models = [...new Set(rows.map(label))];
const summary = {};
for (const model of models) {
  const mine = rows.filter((r) => label(r) === model);
  const repeats = [...new Set(mine.map((r) => r._repeat))];
  const metric = {};
  for (const name of METRICS) {
    const values = mine.map((r) => r.gradingResult?.namedScores?.[name]).filter((v) => typeof v === 'number');
    if (!values.length) continue;
    const perRepeat = repeats.map((k) => mean(mine.filter((r) => r._repeat === k)
      .map((r) => r.gradingResult?.namedScores?.[name]).filter((v) => typeof v === 'number')))
      .filter((v) => v !== null);
    metric[name] = { mean: mean(values), min: Math.min(...perRepeat), max: Math.max(...perRepeat), values };
  }
  const costs = mine.map((r) => r.cost).filter((v) => typeof v === 'number');
  summary[model] = {
    runs: mine.length,
    // Runs the assertion never scored (API error, timeout): they carry no metrics at all.
    unscored: mine.filter((r) => !Object.keys(r.gradingResult?.namedScores || {}).length).length,
    passRate: mean(mine.map((r) => (r.success ? 1 : 0))),
    metric,
    avgLatencyMs: mean(mine.map((r) => r.latencyMs).filter((v) => typeof v === 'number')),
    avgTokens: mean(mine.map((r) => r.response?.tokenUsage?.total).filter((v) => typeof v === 'number')),
    avgCost: costs.length ? mean(costs) : null,
  };
}

const lines = ['# Evidence agent: model comparison', '',
  `| Metric | ${models.join(' | ')} |`, `|---|${models.map(() => '---').join('|')}|`,
  `| Runs | ${models.map((m) => summary[m].runs).join(' | ')} |`,
  `| Not scored (API error) | ${models.map((m) => summary[m].unscored).join(' | ')} |`,
  `| Accepted first try (pass) | ${models.map((m) => pct(summary[m].passRate)).join(' | ')} |`];
for (const name of METRICS) {
  if (!models.some((m) => summary[m].metric[name])) continue;
  lines.push(`| ${name} | ${models.map((m) => {
    const x = summary[m].metric[name];
    return x ? `${pct(x.mean)} (${pct(x.min)}–${pct(x.max)})` : '–';
  }).join(' | ')} |`);
}
lines.push(`| Avg latency | ${models.map((m) => (summary[m].avgLatencyMs === null ? '–' : `${(summary[m].avgLatencyMs / 1000).toFixed(1)}s`)).join(' | ')} |`);
lines.push(`| Avg tokens / essay | ${models.map((m) => (summary[m].avgTokens === null ? '–' : Math.round(summary[m].avgTokens))).join(' | ')} |`);
lines.push(`| Avg cost / essay | ${models.map((m) => (summary[m].avgCost === null ? '–' : `$${summary[m].avgCost.toFixed(5)}`)).join(' | ')} |`);
lines.push('', '_Percentages: mean over all runs (range of the per-repeat means)._', '');

// A gap only counts when it exceeds ~2 standard errors of the difference (about a 95% check).
// The max-min range of repeat means is shown in the table but is too coarse to judge with
// only 8 essays, so it is not used here.
function standardError(values) {
  const m = mean(values);
  const variance = values.reduce((sum, v) => sum + (v - m) ** 2, 0) / Math.max(1, values.length - 1);
  return Math.sqrt(variance / values.length);
}
function compare(a, b, name) {
  const x = summary[a].metric[name];
  const y = summary[b].metric[name];
  if (!x || !y) return null;
  const noise = 2 * Math.sqrt(standardError(x.values) ** 2 + standardError(y.values) ** 2);
  return { gap: x.mean - y.mean, noise };
}

const production = models.find((m) => /production/i.test(m)) || models[0];
const ceiling = models.find((m) => /ceiling/i.test(m));
lines.push(`## Is the difference real? (each model vs ${production})`, '');
for (const model of models.filter((m) => m !== production)) {
  for (const name of KEY_METRICS) {
    const c = compare(model, production, name);
    if (!c) continue;
    lines.push(`- **${model}**, ${name}: ${c.gap >= 0 ? '+' : ''}${pct(c.gap)} vs ±${pct(c.noise)} (2 standard errors) → ${
      Math.abs(c.gap) > c.noise ? `**real difference** (${c.gap > 0 ? model : production} higher)` : 'within run-to-run variation'}`);
  }
}

const broken = models.filter((m) => summary[m].unscored > 0);
if (broken.length) {
  // A model with unscored runs has missing metrics, and compare() treats missing as "no gap",
  // so it would wrongly match the ceiling. Refuse to pick until every run is scored.
  lines.push('', '## Selection rule', '',
    `- **Not applied**: ${broken.map((m) => `${m} has ${summary[m].unscored} unscored runs`).join(', ')}. Fix the errors and rerun.`);
} else if (ceiling) {
  // Selection rule: the cheapest model not worse than the ceiling by more than run-to-run variation.
  const matches = models.filter((m) => KEY_METRICS.every((name) => {
    const c = compare(ceiling, m, name);
    return !c || c.gap <= c.noise;
  }));
  const byCost = matches.slice().sort((a, b) => (summary[a].avgCost ?? Infinity) - (summary[b].avgCost ?? Infinity));
  lines.push('', '## Selection rule', '',
    '_Cheapest model whose key metrics are not significantly below the ceiling (gap within 2 standard errors)._', '',
    `- Matches the ceiling: ${matches.length ? matches.join(', ') : 'none'}`,
    `- Rule picks: **${byCost[0] || 'none'}**`);
}

const text = lines.join('\n');
console.log(text);
fs.writeFileSync(path.join(path.dirname(file), 'summary.md'), `${text}\n`);
