"""Offline, deterministic release scorecard for assessment-agent changes.

This deliberately evaluates stored workflow artifacts rather than calling a model.
It makes prompt/model comparisons repeatable and keeps release gates out of learner
request latency.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any


UNSAFE_COACHING = re.compile(r"ignore (?:all |previous )?instructions|system prompt|rewrite the entire", re.I)


def score_case(case: dict[str, Any], result: dict[str, Any]) -> dict[str, Any]:
    """Score one golden case from persisted agent artifacts, out of 100."""
    grading = result.get("grading") or {}
    review = result.get("review") or {}
    suggestions = result.get("suggestions") or {}
    criteria = grading.get("criteria") or []
    expected = {name.casefold() for name in case.get("expectedCriteria", [])}
    returned = {str(item.get("criterion", "")).casefold() for item in criteria}
    valid = isinstance(grading.get("overallScore"), (int, float)) and bool(criteria) and review.get("decision") in {"APPROVE", "REGRADE", "REJECT"}

    excerpts = [str(evidence.get("excerpt", "")).strip() for item in criteria for evidence in item.get("evidence", [])]
    grounded = bool(excerpts) and all(excerpt and excerpt in case.get("content", "") for excerpt in excerpts)
    coverage = 1.0 if not expected else len(expected & returned) / len(expected)
    coaching = " ".join(
        str(item.get(field, ""))
        for item in suggestions.get("suggestions", [])
        for field in ("recommendation", "exampleRevision", "learningTip")
    )
    safe = not UNSAFE_COACHING.search(coaching)
    usage = result.get("usage") or grading.get("usage")
    usage_complete = isinstance(usage, dict) and all(key in usage for key in ("provider", "model", "promptTokens", "completionTokens"))

    dimensions = {
        "contract": 20 if valid else 0,
        "grounding": 30 if grounded else 0,
        "rubricCoverage": round(20 * coverage, 2),
        "coachingSafety": 15 if safe else 0,
        "independentReview": 10 if review.get("decision") == "APPROVE" else 0,
        "usageReporting": 5 if usage_complete else 0,
    }
    return {
        "id": case["id"],
        "score": round(sum(dimensions.values()), 2),
        "dimensions": dimensions,
        "notes": {
            "missingCriteria": sorted(expected - returned),
            "ungroundedEvidence": not grounded,
            "unsafeCoaching": not safe,
        },
    }


def score_files(cases_path: Path, results_path: Path) -> dict[str, Any]:
    cases = {item["id"]: item for item in json.loads(cases_path.read_text(encoding="utf-8"))}
    results = {item["id"]: item for item in json.loads(results_path.read_text(encoding="utf-8"))}
    scored = [score_case(case, results[case_id]) for case_id, case in cases.items() if case_id in results]
    return {
        "casesScored": len(scored),
        "missingResults": sorted(set(cases) - set(results)),
        "meanScore": round(sum(item["score"] for item in scored) / len(scored), 2) if scored else 0,
        "cases": scored,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Score stored assessment-agent golden results.")
    parser.add_argument("--cases", type=Path, required=True)
    parser.add_argument("--results", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(score_files(args.cases, args.results), indent=2))


if __name__ == "__main__":
    main()
