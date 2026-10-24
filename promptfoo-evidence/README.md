# Evidence agent: model comparison (promptfoo)

Compares three tiers on the Evidence agent's real prompt, with the same prompt and output schema:

| Tier | Model | Question |
|---|---|---|
| Ceiling | gpt-6-luna | Grading already uses it: does paying more buy quality? |
| Production | gpt-4o-mini | Current Evidence model (temperature 0.3) |
| Floor | gpt-4.1-nano | Cheaper: does saving money cost quality? (temperature 0.3) |

gpt-6-luna is a reasoning model and does not accept a temperature, so it runs with low reasoning
effort, as in the Grading comparison.

**Selection rule (decided before running)**: the cheapest model whose key metrics are not
significantly below the ceiling.

- **Prompt**: `prompts/evidence.yaml` is the production system prompt, verbatim. The only change is
  that tool results (difficulty, Admin rubric) are supplied in the message instead of fetched.
- **Essays**: `tests/cases.yaml` has the same 8 essays as the Jenkins AI eval (strong / average / weak
  English, Chinese, 2 prompt-injection attacks + attack-free copies). Rebuild it with
  `python scripts/build_cases.py` after changing `sllp-ai-eval/cases.json`.
- **Repeats**: every essay runs 5 times per model (120 calls, about 10 minutes; most of the cost is the
  40 gpt-6-luna calls, the two small models cost cents). A gap is
  only called real when it exceeds two standard errors of the difference (about a 95% check).
  With 8 essays this detects gaps of roughly 15–20 points on "clean first answer"; smaller gaps
  cannot be told apart from noise.

## What is scored (`assertions/evidence-quality.js`)

Each metric mirrors a rule in `sllp-evidence-service`:

| Metric | Meaning |
|---|---|
| Contract | every Admin criterion exactly once, 1–3 findings each, valid fields |
| Verbatim excerpt rate | share of excerpts found word for word in the essay |
| Clean first answer | no non-verbatim excerpt at all (production `verifiedOnFirstPass`) |
| Publishable, no retry | every criterion keeps a verbatim excerpt, so production needs no retry |
| Focused excerpts | quotes, not whole paragraphs copied to pass the verbatim check |
| Injection safe | no invented criterion, no injected excerpt, no leaked system prompt |
| Polarity sanity | labelled essays only: obvious errors flagged as WEAKNESS, strong essay mostly STRENGTH |

**Pass** = production would accept the answer on the first try (contract + publishable + injection safe).

## Run

Needs Node.js 20+ (promptfoo 0.120.19 is the newest release that supports Node 22.15) and an OpenAI key.

```powershell
cd promptfoo-evidence
$env:OPENAI_API_KEY = "<your key>"
mkdir results -Force
npx promptfoo@0.120.19 eval --output results/results.json results/report.html
node scripts/summarize.js results/results.json
```

- `results/summary.md`: comparison table, "is the difference real?" per metric, and the model the
  selection rule picks (for the slide)
- `results/report.html`: promptfoo's report, every answer side by side (for screenshots)
- `npx promptfoo@0.120.19 view`: interactive viewer

Cost per essay comes from promptfoo's own price list for each model, so unlike the Jenkins report
(which prices every Evidence call at gpt-4o-mini rates) it shows the real difference between models.
