# Suggestions Agent Promptfoo evaluation

This evaluates the real `SuggestionService` and `OpenAIChatGenerator` against nine
synthetic, approved English assessments. A fixed profile and rubric snapshot
replace live User/Admin calls so a run isolates the Suggestions Agent. Model
generation and output moderation still make live API calls when there is an
improvement priority. The cases cover two writing levels, a longer composition,
weighted ranking across four criteria, skipping an ungrounded excerpt, prompt
injection in both learner writing and a grading rationale, and a perfect
criterion that produces no suggestions without a model call.

The assertions report contract/priority order, evidence grounding, optional short
local revisions, and basic coaching safety, including case-specific injection canaries.
The word-choice case requires an example that removes redundant wording.
The match case also checks for a short list of unsupported outcome claims that
appeared during manual review. These string checks cannot establish general
meaning preservation.
They do **not** measure whether advice is
pedagogically helpful; that needs human review of representative learner work.

## Run

From `sllp-suggestions-agent`, install the Python requirements, use a Node.js
version supported by your Promptfoo release (the current release requires
Node.js 22.22 or newer), and use a secure shell or secret manager to set
`SUGGESTIONS_OPENAI_API_KEY` (or `OPENAI_API_KEY`). Do not put credentials in
the YAML, case files, results, or slide screenshots.

```powershell
python -m pip install -r requirements.txt
New-Item -ItemType Directory -Force evals/results | Out-Null
npx promptfoo@0.120.19 eval -c evals/promptfooconfig.yaml --repeat 3 --no-cache --no-share --output evals/results/suggestions-v6-word-choice-repeat3.html --output evals/results/suggestions-v6-word-choice-repeat3.json
```

The output files are ignored by Git because they contain generated text. Check
each result manually before using a pass rate on a slide. Report the model,
number of cases, number of runs, date, and any failures alongside the result.
These nine synthetic cases are a demonstration set, not evidence of general
grading or teaching quality. Expand to anonymised, human-reviewed compositions
before making broader claims.

The original four-case, three-repeat run and its limitations are documented in
`RESULTS_2026-10-15.md`. Its 12/12 result does not describe the expanded suite.
The expanded baseline and `suggestion-v3` through `suggestion-v5` runs are
documented in `RESULTS_2026-10-16.md`. The current `suggestion-v6` run is in
`RESULTS_2026-10-17.md`. It permits advice without a forced rewrite, uses
editable blanks for missing learner details, and checks a concrete word-choice
edit. Results still need human review for teaching usefulness.
The 27/27 live result predates a small review fix that retains approved edits;
rerun the evaluation before citing that figure for the final code.
