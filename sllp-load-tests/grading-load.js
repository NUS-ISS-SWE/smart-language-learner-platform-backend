// Grading load test: submit several compositions for AI grading at the same time
// and measure how long each one takes to come back as REVIEWED.
//
// Grading is asynchronous — POST /grading only queues the work (Kafka → orchestrator →
// evidence / grading / review / suggestion agents → OpenAI), so the number that matters
// is the end-to-end time until the composition's status leaves REVIEWING.
//
// Every essay costs real OpenAI calls, so keep ESSAY_COUNT small.
import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import exec from 'k6/execution';
import { Trend, Rate, Counter } from 'k6/metrics';
import { ESSAYS, TIMELINE_EVENTS, ENDPOINTS, essayEventKey, buildReport } from './report.js';

const BASE_URL = __ENV.GATEWAY_BASE_URL || 'http://localhost:8080';
const ESSAY_COUNT = parseInt(__ENV.ESSAY_COUNT || '3', 10);
// Short interval so the per-essay timeline in the report is accurate to about a second.
const POLL_INTERVAL_SECONDS = 1;
const GRADING_TIMEOUT_SECONDS = parseInt(__ENV.GRADING_TIMEOUT_SECONDS || '480', 10);
const REPORT_DIR = __ENV.REPORT_DIR || 'load-test-reports';

const gradingDuration = new Trend('grading_e2e_duration', true);
const submitDuration = new Trend('grading_submit_duration', true);
const gradingSuccess = new Rate('grading_success');
const gradingTimeouts = new Counter('grading_timeouts');
// Wall-clock time (epoch ms) at which each essay first reached each stage, tagged
// {essay, event}. This is what the report's timeline is drawn from.
const essayEvent = new Trend('essay_event');

// k6 only keeps per-tag values (submetrics) in the end-of-test summary when a threshold
// names them, so every essay × event pair gets an always-true threshold.
const summaryThresholds = {};
for (let n = 1; n <= ESSAY_COUNT; n++) {
  for (const event of TIMELINE_EVENTS) {
    summaryThresholds[essayEventKey(n, event)] = ['max>=0'];
  }
}
// Same trick for per-endpoint response times in the report's API table.
for (const name of ENDPOINTS) {
  summaryThresholds[`http_req_duration{name:${name}}`] = ['max>=0'];
  summaryThresholds[`http_reqs{name:${name}}`] = ['count>=0'];
}

export const options = {
  scenarios: {
    concurrent_grading: {
      // One VU per essay, each running exactly once, all starting together —
      // i.e. ESSAY_COUNT students pressing "submit for grading" at the same moment.
      executor: 'per-vu-iterations',
      vus: ESSAY_COUNT,
      iterations: 1,
      maxDuration: `${GRADING_TIMEOUT_SECONDS + 120}s`,
    },
  },
  thresholds: Object.assign({
    // Report-only targets: breaching them marks the Jenkins stage UNSTABLE, not FAILED.
    grading_success: ['rate==1'],
    grading_e2e_duration: ['max<300000'],
  }, summaryThresholds),
};

// `name` groups requests per endpoint in the report (composition ids differ per URL).
function authHeaders(token, name) {
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, tags: { name } };
}

// Runs once before any VU starts: log in and create one draft per VU, so that the
// grading submissions themselves go out as close to simultaneously as possible.
export function setup() {
  const login = http.post(`${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: __ENV.LOAD_TEST_USERNAME || 'testuser', password: __ENV.LOAD_TEST_PASSWORD || 'password123' }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } });
  if (login.status !== 200) {
    fail(`login failed: ${login.status} ${login.body}`);
  }
  const token = login.json('accessToken');

  const compositionIds = [];
  for (let i = 0; i < ESSAY_COUNT; i++) {
    const essay = ESSAYS[i % ESSAYS.length];
    const res = http.post(`${BASE_URL}/api/v1/composition`,
      JSON.stringify({ title: `[load-test] ${essay.title}`, content: essay.content }), authHeaders(token, 'create-essay'));
    if (res.status !== 200 && res.status !== 201) {
      fail(`creating composition ${i + 1} failed: ${res.status} ${res.body}`);
    }
    compositionIds.push(res.json('id'));
  }
  return { token, compositionIds };
}

export default function (data) {
  const essayNo = exec.vu.idInTest;
  const id = data.compositionIds[essayNo - 1];
  const mark = (event, at) => essayEvent.add(at, { essay: String(essayNo), event });

  const submitted = Date.now();
  mark('SUBMITTED', submitted);
  const submit = http.post(`${BASE_URL}/api/v1/composition/${id}/grading`, null, authHeaders(data.token, 'submit-grading'));
  submitDuration.add(submit.timings.duration);
  if (!check(submit, { 'grading request accepted': (r) => r.status === 200 })) {
    gradingSuccess.add(false);
    mark('FAILED', Date.now());
    console.error(`essay ${essayNo} (composition ${id}): submit failed ${submit.status} ${submit.body}`);
    return;
  }

  let status = 'REVIEWING';
  const seen = {};
  const deadline = submitted + GRADING_TIMEOUT_SECONDS * 1000;
  while (status === 'REVIEWING' && Date.now() < deadline) {
    sleep(POLL_INTERVAL_SECONDS);
    const res = http.get(`${BASE_URL}/api/v1/composition/${id}`, authHeaders(data.token, 'check-status'));
    if (res.status !== 200) continue;
    status = res.json('compositionStatus');
    const stage = res.json('gradingProgressStage');
    // PREPARING is set by composition-service on submit, i.e. the essay is still waiting
    // for the orchestrator; only the orchestrator's own stages are timeline events.
    if (stage && stage !== 'PREPARING' && !seen[stage] && TIMELINE_EVENTS.indexOf(stage) >= 0) {
      seen[stage] = true;
      const now = Date.now();
      mark(stage, now);
      console.log(`essay ${essayNo}: +${((now - submitted) / 1000).toFixed(1)}s  stage=${stage}`);
    }
  }

  const finished = Date.now();
  const elapsed = finished - submitted;
  if (status === 'REVIEWED') {
    gradingDuration.add(elapsed);
    gradingSuccess.add(true);
    mark('DONE', finished);
    console.log(`essay ${essayNo} (composition ${id}): REVIEWED in ${(elapsed / 1000).toFixed(1)}s`);
  } else if (status === 'REVIEWING') {
    gradingTimeouts.add(1);
    gradingSuccess.add(false);
    mark('TIMEOUT', finished);
    console.error(`essay ${essayNo} (composition ${id}): still REVIEWING after ${GRADING_TIMEOUT_SECONDS}s (timeout)`);
  } else {
    // Failed grading sends the composition back to DRAFT.
    gradingSuccess.add(false);
    mark('FAILED', finished);
    console.error(`essay ${essayNo} (composition ${id}): grading failed, status=${status} after ${(elapsed / 1000).toFixed(1)}s`);
  }
}

export function handleSummary(data) {
  const report = buildReport(data, {
    essayCount: ESSAY_COUNT,
    reviewAutoApproved: __ENV.REVIEW_AUTO_APPROVE === 'true',
  });
  return {
    stdout: report.text,
    [`${REPORT_DIR}/report.html`]: report.html,
    [`${REPORT_DIR}/report.css`]: report.css,
    [`${REPORT_DIR}/summary.txt`]: `${report.oneLine}\n`,
    [`${REPORT_DIR}/summary.json`]: JSON.stringify(data, null, 2),
  };
}
