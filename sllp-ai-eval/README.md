# AI eval (end-to-end quality, security and drift)

Runs a fixed set of test essays through the real assessment pipeline
(orchestrator → evidence → grading → review → suggestions, real OpenAI calls) and scores
what every agent produced. One run answers four questions:

| Layer | Question | Examples |
|---|---|---|
| Integration | Did every agent run and return a valid result? | pipeline completed, outputs match the contract, language detected |
| Quality | Is the output any good? | evidence quoted verbatim, rubric criteria covered, score inside the expected band, review approved |
| Security | Can an essay manipulate the agents? | two prompt-injection essays (Lakera Guard block, or agents ignore the instruction) |
| Drift | Has quality moved although our code did not? | same essays compared with `baseline.json` |

Files: `cases.json` (test essays and expectations), `evaluation.py` (checks, gates, drift),
`report.py` (HTML / JUnit / summary), `run_eval.py` (runs it all). Standard library only.

## Release gates and drift rules

Gates (absolute, in `evaluation.py`): every normal essay graded, 100 % of excerpts verbatim,
≥ 95 % rubric coverage, every injection resisted, no injected text in suggestions.
A failed gate exits with 1 and fails the Jenkins build, so no image is pushed.

Drift (relative to `baseline.json`): a quality rate dropping more than 10 points, or the same
essays' scores moving more than 10 points on average. Drift exits with 2 and marks the build
UNSTABLE. Tokens, cost and latency changing by more than 50 % are flagged as WATCH.

## In Jenkins (JenkinsFile-CI)

- `RUN_AI_EVAL` — run the eval; the report appears as **AI Eval Report** on the build page,
  every check as a test case under **Test Result**, and a one-line summary in the build description.
- `SIMULATE_DRIFT` + `DRIFT_MODEL` — demo: run the chosen agent(s) on another model with the same
  code and essays, and watch the drift check catch it.
- Lakera Guard is used when a `lakera-api-key` secret-text credential exists; otherwise the
  grading agent runs unscreened and the injection tests show how the agents resist on their own.

### Setting the baseline

1. Run with `RUN_AI_EVAL` (no simulation). The first run has no baseline, so it checks gates only.
2. If the result looks right, download `ai-eval-reports/ai-eval-baseline-candidate.json` from the
   build artifacts and commit it as `sllp-ai-eval/baseline.json`.
3. Later runs are compared against it. Refresh it deliberately when a prompt or model change is
   accepted — never automatically.

## Locally

With the stack running in a2a mode (`docker compose up`, gateway on 18080):

```bash
python3 sllp-ai-eval/run_eval.py --gateway http://localhost:18080 --compose-dir . --out ai-eval-reports
python3 sllp-ai-eval/run_eval.py --rescore ai-eval-reports/ai-eval-raw.json --out ai-eval-reports   # re-score only
```
