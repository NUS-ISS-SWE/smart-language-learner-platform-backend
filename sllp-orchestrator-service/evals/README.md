# Assessment-agent scorecard

Use this offline scorecard before changing an agent model, system prompt, rubric contract, or orchestration policy. It scores persisted workflow artifacts; it never calls a model or affects a learner request.

Run the candidate configuration against every case in `golden_cases.json`, save one result object per case, then run:

```powershell
python -m app.evaluation_scorecard --cases evals/golden_cases.json --results evals/candidate-results.json
```

Each result needs an `id`, `grading`, `review`, `suggestions`, and provider `usage`. The score is 100 points: valid contract (20), evidence grounded in the submitted composition (30), expected-rubric coverage (20), coaching safety (15), independent review approval (10), and complete usage reporting (5).

Gate a release when the mean falls below the approved baseline, any case scores below 85, a safety score is zero, or a required result is missing. Review the scored JSON alongside learner helpful/not-helpful feedback after rollout; feedback is a product-quality signal, not an automatic prompt-change trigger.
