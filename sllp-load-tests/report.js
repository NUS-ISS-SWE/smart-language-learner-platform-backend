// Essays, timeline bookkeeping and the human-readable report for grading-load.js.
//
// The report is plain HTML + inline SVG with no scripts or external resources, so it
// renders the same when opened from Jenkins' build artifacts or from a local folder.

export const ESSAYS = [
  {
    title: 'My Weekend',
    content: 'Last weekend I went to the park with my family. We had a picnic under a big tree and played '
      + 'football on the grass. It was a sunny day and everyone was very happy. In the evening we went home '
      + 'tired but satisfied, and I hope we can do it again soon.',
  },
  {
    title: 'The Importance of Reading',
    content: 'Reading is one of the most useful habits a student can have. Books help us learn new words and '
      + 'understand different cultures. When I read every night before bed, I feel calmer and I sleep better. '
      + 'I think schools should give students more time to read books they choose themselves.',
  },
  {
    title: 'A Memorable Trip',
    content: 'Two years ago I travelled to Japan with my friends. We visited Tokyo and Kyoto and tried many kinds '
      + 'of food. The temples in Kyoto were beautiful, especially in autumn. The trip taught me that travelling '
      + 'is a great way to learn about the world and about yourself.',
  },
];

// Orchestrator stages as reported in the composition's gradingProgressStage.
const STAGES = ['EVIDENCING', 'GRADING', 'REGRADING', 'REVIEWING', 'SUGGESTING'];
const END_EVENTS = ['DONE', 'FAILED', 'TIMEOUT'];
export const TIMELINE_EVENTS = ['SUBMITTED'].concat(STAGES, END_EVENTS);

export function essayEventKey(essayNo, event) {
  return `essay_event{essay:${essayNo},event:${event}}`;
}

const STAGE_STYLE = {
  WAITING: { label: 'Waiting in queue', color: '#cbd5e1' },
  EVIDENCING: { label: 'Evidence agent', color: '#60a5fa' },
  GRADING: { label: 'Grading agent', color: '#8b5cf6' },
  REGRADING: { label: 'Re-grading', color: '#ec4899' },
  REVIEWING: { label: 'Review agent', color: '#f59e0b' },
  SUGGESTING: { label: 'Suggestion agent', color: '#10b981' },
};

const RESULT_LABEL = { DONE: 'Graded', FAILED: 'Failed', TIMEOUT: 'Timed out' };

function secs(ms) {
  return ms === undefined || ms === null || isNaN(ms) ? '-' : `${(ms / 1000).toFixed(1)}s`;
}

function millis(ms) {
  if (ms === undefined || ms === null || isNaN(ms)) return '-';
  return ms < 1000 ? `${Math.round(ms)}ms` : secs(ms);
}

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// Rebuild one record per essay from the tagged essay_event submetrics.
function collectEssays(data, essayCount) {
  const at = (n, event) => {
    const m = data.metrics[essayEventKey(n, event)];
    // Values are epoch milliseconds; a submetric with no samples reports 0.
    return m && m.values && m.values.max > 0 ? m.values.max : undefined;
  };
  const essays = [];
  for (let n = 1; n <= essayCount; n++) {
    const submitted = at(n, 'SUBMITTED');
    if (submitted === undefined) continue;
    const stages = STAGES.map((s) => ({ stage: s, at: at(n, s) }))
      .filter((s) => s.at !== undefined)
      .sort((a, b) => a.at - b.at);
    const endEvent = END_EVENTS.find((e) => at(n, e) !== undefined);
    const end = endEvent ? at(n, endEvent) : undefined;
    const startedGrading = stages.length ? stages[0].at : end;

    const segments = [];
    if (startedGrading !== undefined) segments.push({ stage: 'WAITING', from: submitted, to: startedGrading });
    stages.forEach((s, i) => {
      const to = i + 1 < stages.length ? stages[i + 1].at : end;
      if (to !== undefined) segments.push({ stage: s.stage, from: s.at, to });
    });
    const stageTime = {};
    segments.forEach((seg) => { stageTime[seg.stage] = (stageTime[seg.stage] || 0) + (seg.to - seg.from); });

    essays.push({
      no: n,
      title: ESSAYS[(n - 1) % ESSAYS.length].title,
      submitted,
      end,
      result: endEvent || 'UNKNOWN',
      queueWait: startedGrading !== undefined ? startedGrading - submitted : undefined,
      processing: end !== undefined && startedGrading !== undefined ? end - startedGrading : undefined,
      total: end !== undefined ? end - submitted : undefined,
      startedGrading,
      segments,
      stageTime,
    });
  }
  // Order by when grading actually started, which is the order the orchestrator served them.
  essays.sort((a, b) => (a.startedGrading || Infinity) - (b.startedGrading || Infinity));
  essays.forEach((e, i) => { e.order = i + 1; });
  return essays;
}

function avg(values) {
  const v = values.filter((x) => x !== undefined);
  return v.length ? v.reduce((a, b) => a + b, 0) / v.length : undefined;
}

function max(values) {
  const v = values.filter((x) => x !== undefined);
  return v.length ? Math.max.apply(null, v) : undefined;
}

// Endpoint groups (the `name` tag on each request) shown in the API response-time table.
export const ENDPOINTS = ['login', 'create-essay', 'submit-grading', 'check-status'];
const ENDPOINT_LABEL = {
  login: 'Log in',
  'create-essay': 'Create essay',
  'submit-grading': 'Submit for grading',
  'check-status': 'Check grading status',
};
const HTTP_PHASES = ['http_req_duration', 'http_req_waiting', 'http_req_connecting', 'http_req_tls_handshaking',
  'http_req_sending', 'http_req_receiving', 'http_req_blocked'];
const STAT_COLS = [['avg', 'Avg'], ['min', 'Min'], ['med', 'Median'], ['p(90)', 'p90'], ['p(95)', 'p95'], ['max', 'Max']];

function msCell(v) {
  return v === undefined || v === null || isNaN(v) ? '-' : v.toFixed(2);
}

function htmlTable(head, rows) {
  return `<table><thead><tr>${head.map((h) => `<th>${h}</th>`).join('')}</tr></thead><tbody>`
    + rows.map((r) => `<tr>${r.map((c) => `<td>${c}</td>`).join('')}</tr>`).join('')
    + '</tbody></table>';
}

function apiTable(data) {
  const rows = ENDPOINTS.map((n) => ({
    label: ENDPOINT_LABEL[n],
    d: data.metrics[`http_req_duration{name:${n}}`],
    c: data.metrics[`http_reqs{name:${n}}`],
  })).concat([{ label: '<strong>All requests</strong>', d: data.metrics.http_req_duration, c: data.metrics.http_reqs }])
    .filter((r) => r.d && r.d.values);
  const maxAvg = Math.max.apply(null, rows.map((r) => r.d.values.avg || 0).concat([1]));
  return htmlTable(
    ['Endpoint', 'Requests', 'Avg'].concat(STAT_COLS.slice(1).map((c) => c[1])),
    rows.map((r) => [
      r.label,
      r.c && r.c.values ? r.c.values.count : '-',
      `<span class="bar"><svg width="80" height="10"><rect width="80" height="10" rx="2" fill="#eef2f7"/>`
        + `<rect width="${Math.max(1, (r.d.values.avg / maxAvg) * 80).toFixed(1)}" height="10" rx="2" fill="#6366f1"/></svg>`
        + `${msCell(r.d.values.avg)}</span>`,
    ].concat(STAT_COLS.slice(1).map((c) => msCell(r.d.values[c[0]])))),
  );
}

function phaseTable(data) {
  return htmlTable(
    ['Metric'].concat(STAT_COLS.map((c) => c[1])),
    HTTP_PHASES.filter((m) => data.metrics[m]).map((m) =>
      [m].concat(STAT_COLS.map((c) => msCell(data.metrics[m].values[c[0]])))),
  );
}

function timelineSvg(essays) {
  const origin = Math.min.apply(null, essays.map((e) => e.submitted));
  const ends = essays.map((e) => (e.end !== undefined ? e.end : Math.max.apply(null, e.segments.map((s) => s.to).concat([e.submitted]))));
  const span = Math.max(1000, Math.max.apply(null, ends) - origin);

  const W = 960; const labelW = 200; const rightPad = 70; const rowH = 44; const barH = 22; const top = 10;
  const plotW = W - labelW - rightPad;
  const H = top + essays.length * rowH + 34;
  const x = (t) => labelW + ((t - origin) / span) * plotW;

  const spanS = span / 1000;
  const step = [1, 2, 5, 10, 15, 20, 30, 60, 120, 300].find((s) => spanS / s <= 10) || 600;
  let svg = `<svg viewBox="0 0 ${W} ${H}" width="100%" role="img" aria-label="Grading timeline per essay" font-family="system-ui, sans-serif">`;

  for (let s = 0; s <= spanS + 0.001; s += step) {
    const gx = labelW + (s / spanS) * plotW;
    svg += `<line x1="${gx}" y1="${top}" x2="${gx}" y2="${H - 26}" stroke="#e5e7eb"/>`;
    svg += `<text x="${gx}" y="${H - 10}" font-size="12" fill="#6b7280" text-anchor="middle">${s}s</text>`;
  }

  essays.forEach((e, i) => {
    const y = top + i * rowH + (rowH - barH) / 2;
    svg += `<text x="0" y="${y + 9}" font-size="13" font-weight="600" fill="#111827">Essay ${e.no}</text>`;
    svg += `<text x="0" y="${y + 24}" font-size="11" fill="#6b7280">${esc(e.title)}</text>`;
    e.segments.forEach((seg) => {
      const w = Math.max(1, x(seg.to) - x(seg.from));
      svg += `<rect x="${x(seg.from)}" y="${y}" width="${w}" height="${barH}" fill="${STAGE_STYLE[seg.stage].color}">`
        + `<title>${STAGE_STYLE[seg.stage].label}: ${secs(seg.to - seg.from)}</title></rect>`;
    });
    const endX = x(ends[i]);
    const endText = e.result === 'DONE' ? secs(e.total) : (RESULT_LABEL[e.result] || 'Unknown');
    svg += `<text x="${endX + 6}" y="${y + 15}" font-size="12" font-weight="600" fill="${e.result === 'DONE' ? '#111827' : '#dc2626'}">${endText}</text>`;
  });
  return `${svg}</svg>`;
}

function legend(essays) {
  const used = { WAITING: true };
  essays.forEach((e) => e.segments.forEach((s) => { used[s.stage] = true; }));
  return Object.keys(STAGE_STYLE).filter((k) => used[k]).map((k) =>
    `<span class="key"><svg width="14" height="14"><rect width="14" height="14" rx="3" fill="${STAGE_STYLE[k].color}"/></svg>${STAGE_STYLE[k].label}</span>`,
  ).join('');
}

function table(essays) {
  const stageCols = STAGES.filter((s) => essays.some((e) => e.stageTime[s] !== undefined));
  const head = ['Essay', 'Served', 'Waited in queue', 'Being graded'].concat(
    stageCols.map((s) => `↳ ${STAGE_STYLE[s].label.replace(' agent', '')}`), ['Total', 'Result']);
  const rows = essays.map((e) => [
    `Essay ${e.no}<br><span class="muted">${esc(e.title)}</span>`,
    `${e.order}${['st', 'nd', 'rd'][e.order - 1] || 'th'}`,
    secs(e.queueWait),
    secs(e.processing),
  ].concat(stageCols.map((s) => `<span class="muted">${secs(e.stageTime[s])}</span>`), [
    `<strong>${secs(e.total)}</strong>`,
    `<span class="${e.result === 'DONE' ? 'ok' : 'bad'}">${RESULT_LABEL[e.result] || 'Unknown'}</span>`,
  ]));
  return htmlTable(head, rows);
}

export function buildReport(data, opts) {
  const essayCount = opts.essayCount;
  const essays = collectEssays(data, essayCount);
  const graded = essays.filter((e) => e.result === 'DONE');
  const submit = data.metrics.grading_submit_duration;
  const submitAvg = submit && submit.values ? submit.values.avg : undefined;
  const origin = essays.length ? Math.min.apply(null, essays.map((e) => e.submitted)) : undefined;
  const lastEnd = max(essays.map((e) => e.end));
  const wallTime = origin !== undefined && lastEnd !== undefined ? lastEnd - origin : undefined;
  const avgProcessing = avg(graded.map((e) => e.processing));
  const maxWait = max(essays.map((e) => e.queueWait));
  const failed = data.metrics.http_req_failed ? data.metrics.http_req_failed.values.passes : 0;
  const totalReqs = data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0;

  const tiles = [
    { label: 'Essays graded', value: `${graded.length} / ${essayCount}`, tone: graded.length === essayCount ? 'ok' : 'bad' },
    { label: 'All essays done after', value: secs(wallTime) },
    { label: 'Grading time per essay (avg)', value: secs(avgProcessing) },
    { label: 'Longest wait in queue', value: secs(maxWait) },
    { label: 'HTTP requests', value: totalReqs },
    { label: 'Failed requests', value: failed, tone: failed ? 'bad' : 'ok' },
  ];

  const html = `<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Grading Load Test</title>
<style>
  body { margin: 0; background: #f8fafc; color: #111827; font: 15px/1.5 system-ui, -apple-system, "Segoe UI", sans-serif; }
  main { max-width: 1040px; margin: 0 auto; padding: 32px 16px 48px; }
  h1 { font-size: 24px; margin: 0 0 4px; }
  h2 { font-size: 17px; margin: 32px 0 12px; }
  .sub, .muted { color: #6b7280; }
  .muted { font-size: 13px; }
  .tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-top: 24px; }
  .tile { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 14px 16px; }
  .tile .v { font-size: 26px; font-weight: 700; }
  .tile .l { color: #6b7280; font-size: 13px; }
  .tile.ok .v, .ok { color: #047857; } .tile.bad .v, .bad { color: #dc2626; }
  .card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 16px 20px; }
  .legend { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 8px; font-size: 13px; color: #374151; }
  .key { display: inline-flex; align-items: center; gap: 6px; }
  .scroll { overflow-x: auto; }
  table { border-collapse: collapse; width: 100%; background: #fff; font-size: 14px; }
  th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #e5e7eb; white-space: nowrap; }
  th { background: #f1f5f9; font-weight: 600; font-size: 13px; }
  td .bar { display: inline-flex; align-items: center; gap: 8px; }
</style></head>
<body><main>
  <h1>Grading load test</h1>
  <div class="sub">${essayCount} essays submitted at once${opts.reviewAutoApproved ? ' · review auto-approved' : ''} · ${esc(new Date().toISOString().replace('T', ' ').slice(0, 19))} UTC</div>

  <div class="tiles">${tiles.map((t) => `<div class="tile ${t.tone || ''}"><div class="v">${t.value}</div><div class="l">${t.label}</div></div>`).join('')}</div>

  <h2>Timeline per essay</h2>
  <div class="card">
    ${essays.length ? timelineSvg(essays) : '<p>No timeline data.</p>'}
    <div class="legend">${legend(essays)}</div>
  </div>

  <h2>Grading breakdown</h2>
  <div class="scroll">${table(essays)}</div>

  <h2>API response time (ms)</h2>
  <div class="scroll">${apiTable(data)}</div>

  <h2>HTTP request phases (ms)</h2>
  <div class="scroll">${phaseTable(data)}</div>
</main></body></html>
`;

  const lines = ['', `Grading load test — ${graded.length}/${essayCount} essays graded`, ''];
  lines.push('  essay  served  queued    grading   total     result');
  essays.forEach((e) => {
    lines.push(`  ${String(e.no).padEnd(5)}  ${String(e.order).padEnd(6)}  ${secs(e.queueWait).padEnd(8)}  `
      + `${secs(e.processing).padEnd(8)}  ${secs(e.total).padEnd(8)}  ${RESULT_LABEL[e.result] || 'Unknown'}`);
  });
  lines.push('', `  all done after ${secs(wallTime)}, submit request avg ${millis(submitAvg)}`, '');

  const oneLine = `Load (grading x${essayCount}): ${graded.length}/${essayCount} graded, `
    + `all done after ${secs(wallTime)}, grading ~${secs(avgProcessing)}/essay, longest queue wait ${secs(maxWait)}`;

  return { html, text: lines.join('\n'), oneLine };
}
