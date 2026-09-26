# Grading load test (k6)

Submits 3 compositions for AI grading at the same moment and measures how long each
one takes to come back as `REVIEWED`. Grading is asynchronous (Kafka → orchestrator →
A2A agents → OpenAI), so the script polls `GET /api/v1/composition/{id}` until the
status leaves `REVIEWING`.

Each run makes real OpenAI calls (roughly 3–4 per essay), so keep `ESSAY_COUNT` small.

## Run locally

The stack must be running in `a2a` mode with a real `OPENAI_API_KEY` and the four
`*_AGENT_TOKEN` values set (see `.env.example`). `REVIEW_AUTO_APPROVE=true` skips the
review model.

```powershell
mkdir sllp-load-tests/load-test-reports
docker run --rm -v "${PWD}/sllp-load-tests:/work" -w /work `
  -e GATEWAY_BASE_URL=http://host.docker.internal:18080 -e REVIEW_AUTO_APPROVE=true `
  grafana/k6 run grading-load.js
```

Reports land in `sllp-load-tests/load-test-reports/`:

- `load-test-report.html` — the readable report: per-essay timeline (queue wait vs. each agent), breakdown table and API response times (styles in `load-test-report.css`)
- `load-test-summary.txt` — one-line result (shown on the Jenkins build page)
- `load-test-k6-metrics.json` — raw k6 metrics

The report is built by `report.js`; the essays submitted are defined there too.

| Variable | Default | Meaning |
|---|---|---|
| `GATEWAY_BASE_URL` | `http://localhost:8080` | api-gateway address |
| `ESSAY_COUNT` | `3` | essays submitted at once |
| `GRADING_TIMEOUT_SECONDS` | `480` | give up on an essay after this long |
| `LOAD_TEST_USERNAME` / `LOAD_TEST_PASSWORD` | seeded `testuser` | account used to submit |
| `REPORT_DIR` | `load-test-reports` | where reports are written |
| `REVIEW_AUTO_APPROVE` | unset | set to `true` to note in the report that review was auto-approved |

## In Jenkins

Tick `RUN_LOAD_TEST` in `JenkinsFile-CI`. Needs a secret-text credential with id
`openai-api-key` and the HTML Publisher plugin. The report opens from the
**Load Test Report** link on the build page; files are also archived under `load-test-reports/`.
