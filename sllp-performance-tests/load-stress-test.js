import http from 'k6/http';
import { check, sleep } from 'k6';

// Load & stress tests (k6) against a fully deployed stack (docker-compose), through the api-gateway.
// Only non-AI endpoints are exercised: CI runs with a placeholder OpenAI key.
//
//   TEST_TYPE=load   normal traffic; thresholds act as a quality gate
//   TEST_TYPE=stress ramps well past normal traffic to find where the system starts to degrade
//
// Local run: k6 run -e BASE_URL=http://localhost:8080 -e TEST_TYPE=load load-stress-test.js

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_TYPE = __ENV.TEST_TYPE || 'load';

// Seeded by sllp-liquibase-service — same accounts the Playwright integration tests use.
const SEEDED_PASSWORD = 'password123';

const PROFILES = {
  load: {
    stages: [
      { duration: '30s', target: 20 },
      { duration: '1m', target: 20 },
      { duration: '15s', target: 0 },
    ],
    thresholds: {
      http_req_failed: ['rate<0.01'],
      // Login gets its own budget: BCrypt password hashing is deliberately slow and CPU-bound.
      'http_req_duration{type:login}': ['p(95)<1000'],
      'http_req_duration{type:api}': ['p(95)<500'],
    },
  },
  stress: {
    stages: [
      { duration: '30s', target: 50 },
      { duration: '1m', target: 100 },
      { duration: '1m', target: 150 },
      { duration: '30s', target: 0 },
    ],
    thresholds: {
      http_req_failed: ['rate<0.10'],
      'http_req_duration{type:login}': ['p(95)<3000'],
      'http_req_duration{type:api}': ['p(95)<2000'],
    },
  },
};

if (!PROFILES[TEST_TYPE]) {
  throw new Error(`Unknown TEST_TYPE "${TEST_TYPE}" (expected one of: ${Object.keys(PROFILES).join(', ')})`);
}

export const options = PROFILES[TEST_TYPE];

// `type` splits login from the other calls for the thresholds above; `name` groups URLs that
// contain an id, otherwise every created rubric would show up as a separate endpoint.
function params(type, name, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  return { headers, tags: { type, name } };
}

function login(username) {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username, password: SEEDED_PASSWORD }),
    params('login', 'POST /api/v1/auth/login'),
  );
  const ok = check(res, { 'login: status 200': (r) => r.status === 200 });
  return ok ? res.json('accessToken') : null;
}

export function setup() {
  const adminToken = login('admin');
  if (!adminToken) {
    throw new Error(`Admin login failed against ${BASE_URL} — is the stack up and seeded?`);
  }
  return { adminToken };
}

export default function (data) {
  // A regular user's visit: log in once, then browse with that token.
  const userToken = login('testuser');
  if (!userToken) {
    sleep(1);
    return;
  }

  const plans = http.get(`${BASE_URL}/api/v1/subscription/plans`, params('api', 'GET /api/v1/subscription/plans'));
  check(plans, { 'plans: status 200': (r) => r.status === 200 });

  const history = http.get(
    `${BASE_URL}/api/v1/subscription/history`,
    params('api', 'GET /api/v1/subscription/history', userToken),
  );
  check(history, { 'history: status 200': (r) => r.status === 200 });

  // Admin write path. Each iteration creates, reads and deletes its own criterion, so concurrent
  // VUs never contend for the same row. The admin token from setup() is reused throughout.
  const admin = data.adminToken;
  const created = http.post(
    `${BASE_URL}/api/v1/admin/rubrics`,
    JSON.stringify({
      language: 'ENGLISH',
      difficultyLevel: 'BEGINNER',
      criterionName: `k6 ${TEST_TYPE} VU${__VU}-${__ITER}`,
      criterionDescription: 'Created by k6 performance test',
      weightPercentage: 10,
    }),
    params('api', 'POST /api/v1/admin/rubrics', admin),
  );
  if (check(created, { 'rubric create: status 201': (r) => r.status === 201 })) {
    const id = created.json('id');

    const fetched = http.get(`${BASE_URL}/api/v1/admin/rubrics/${id}`, params('api', 'GET /api/v1/admin/rubrics/{id}', admin));
    check(fetched, { 'rubric get: status 200': (r) => r.status === 200 });

    const deleted = http.del(`${BASE_URL}/api/v1/admin/rubrics/${id}`, null, params('api', 'DELETE /api/v1/admin/rubrics/{id}', admin));
    check(deleted, { 'rubric delete: status 204': (r) => r.status === 204 });
  }

  sleep(1);
}
