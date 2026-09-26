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
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/2.4.0/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

const BASE_URL = __ENV.GATEWAY_BASE_URL || 'http://localhost:8080';
const ESSAY_COUNT = parseInt(__ENV.ESSAY_COUNT || '3', 10);
const POLL_INTERVAL_SECONDS = 3;
const GRADING_TIMEOUT_SECONDS = parseInt(__ENV.GRADING_TIMEOUT_SECONDS || '480', 10);
const REPORT_DIR = __ENV.REPORT_DIR || 'load-test-reports';

const gradingDuration = new Trend('grading_e2e_duration', true);
const submitDuration = new Trend('grading_submit_duration', true);
const gradingSuccess = new Rate('grading_success');
const gradingTimeouts = new Counter('grading_timeouts');

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
  thresholds: {
    // Report-only targets: breaching them marks the Jenkins stage UNSTABLE, not FAILED.
    grading_success: ['rate==1'],
    grading_e2e_duration: ['max<300000'],
  },
};

const ESSAYS = [
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

function authHeaders(token) {
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };
}

// Runs once before any VU starts: log in and create one draft per VU, so that the
// grading submissions themselves go out as close to simultaneously as possible.
export function setup() {
  const login = http.post(`${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: __ENV.LOAD_TEST_USERNAME || 'testuser', password: __ENV.LOAD_TEST_PASSWORD || 'password123' }),
    { headers: { 'Content-Type': 'application/json' } });
  if (login.status !== 200) {
    fail(`login failed: ${login.status} ${login.body}`);
  }
  const token = login.json('accessToken');

  const compositionIds = [];
  for (let i = 0; i < ESSAY_COUNT; i++) {
    const essay = ESSAYS[i % ESSAYS.length];
    const res = http.post(`${BASE_URL}/api/v1/composition`,
      JSON.stringify({ title: `[load-test] ${essay.title}`, content: essay.content }), authHeaders(token));
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
  const params = authHeaders(data.token);

  const submitted = Date.now();
  const submit = http.post(`${BASE_URL}/api/v1/composition/${id}/grading`, null, params);
  submitDuration.add(submit.timings.duration);
  if (!check(submit, { 'grading request accepted': (r) => r.status === 200 })) {
    gradingSuccess.add(false);
    console.error(`essay ${essayNo} (composition ${id}): submit failed ${submit.status} ${submit.body}`);
    return;
  }

  let status = 'REVIEWING';
  let lastStage = null;
  const deadline = submitted + GRADING_TIMEOUT_SECONDS * 1000;
  while (status === 'REVIEWING' && Date.now() < deadline) {
    sleep(POLL_INTERVAL_SECONDS);
    const res = http.get(`${BASE_URL}/api/v1/composition/${id}`, params);
    if (res.status !== 200) continue;
    status = res.json('compositionStatus');
    const stage = res.json('gradingProgressStage');
    if (stage && stage !== lastStage) {
      console.log(`essay ${essayNo}: +${((Date.now() - submitted) / 1000).toFixed(1)}s  stage=${stage}`);
      lastStage = stage;
    }
  }

  const elapsed = Date.now() - submitted;
  if (status === 'REVIEWED') {
    gradingDuration.add(elapsed);
    gradingSuccess.add(true);
    console.log(`essay ${essayNo} (composition ${id}): REVIEWED in ${(elapsed / 1000).toFixed(1)}s`);
  } else if (status === 'REVIEWING') {
    gradingTimeouts.add(1);
    gradingSuccess.add(false);
    console.error(`essay ${essayNo} (composition ${id}): still REVIEWING after ${GRADING_TIMEOUT_SECONDS}s (timeout)`);
  } else {
    // Failed grading sends the composition back to DRAFT.
    gradingSuccess.add(false);
    console.error(`essay ${essayNo} (composition ${id}): grading failed, status=${status} after ${(elapsed / 1000).toFixed(1)}s`);
  }
}

function seconds(ms) {
  return ms === undefined ? '-' : `${(ms / 1000).toFixed(1)}s`;
}

export function handleSummary(data) {
  const m = data.metrics;
  const duration = m.grading_e2e_duration ? m.grading_e2e_duration.values : {};
  const passes = m.grading_success ? m.grading_success.values.passes : 0;
  const total = m.grading_success ? m.grading_success.values.passes + m.grading_success.values.fails : 0;
  // One line for the Jenkins build description.
  const oneLine = `Load (grading x${ESSAY_COUNT}): ${passes}/${total} graded, `
    + `fastest ${seconds(duration.min)}, avg ${seconds(duration.avg)}, slowest ${seconds(duration.max)}`;

  return {
    stdout: textSummary(data, { indent: ' ', enableColors: false }),
    [`${REPORT_DIR}/summary.json`]: JSON.stringify(data, null, 2),
    [`${REPORT_DIR}/summary.html`]: htmlReport(data, { title: `SLLP grading load test (${ESSAY_COUNT} essays)` }),
    [`${REPORT_DIR}/summary.txt`]: `${oneLine}\n`,
  };
}
