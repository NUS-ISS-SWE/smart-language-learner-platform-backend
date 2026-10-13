"""End-to-end AI eval: integration + quality + prompt-injection + drift, in one run.

Submits every essay in cases.json through the API gateway exactly as a learner would, waits
for the orchestrator to finish (evidence -> grading -> review -> suggestions), reads each
agent's persisted output from Postgres, scores it (evaluation.py) and writes the reports
(report.py). Standard library only, so it runs on the Jenkins agent's python3.12 as is.

Exit code: 0 = pass, 1 = a release gate failed, 2 = drift against the baseline.

    python3.12 run_eval.py --gateway http://localhost:18080 --compose-dir .. --out reports
    python3.12 run_eval.py --rescore reports/ai-eval-raw.json --out reports   # re-score offline
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Optional

sys.path.insert(0, str(Path(__file__).resolve().parent))

import evaluation  # noqa: E402
import report  # noqa: E402

HERE = Path(__file__).resolve().parent
UUID = re.compile(r"^[0-9a-f-]{36}$")


# ---------------------------------------------------------------- API gateway

def _request(method: str, url: str, token: Optional[str] = None, body: Optional[dict] = None) -> tuple[int, Any]:
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            text = response.read().decode()
            return response.status, json.loads(text) if text else None
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode(errors="replace")


def login(gateway: str, username: str, password: str) -> str:
    status, body = _request("POST", f"{gateway}/api/v1/auth/login", body={"username": username, "password": password})
    if status != 200:
        raise SystemExit(f"login failed: HTTP {status} {body}")
    return body["accessToken"]


def submit(gateway: str, token: str, case: dict) -> dict:
    status, body = _request("POST", f"{gateway}/api/v1/composition", token,
                            {"title": f"[ai-eval] {case['title']}", "content": case["content"]})
    if status not in (200, 201):
        return {"error": f"creating the composition failed: HTTP {status} {body}"}
    composition_id = body["id"]
    status, body = _request("POST", f"{gateway}/api/v1/composition/{composition_id}/grading", token)
    if status != 200:
        return {"compositionId": composition_id, "error": f"submitting for grading failed: HTTP {status} {body}"}
    return {"compositionId": composition_id, "submittedAt": time.time()}


def wait_for_results(gateway: str, token: str, runs: dict, timeout_seconds: int) -> None:
    """Polls every composition until it leaves REVIEWING (REVIEWED = done, DRAFT = failed)."""
    pending = {cid for cid, run in runs.items() if "error" not in run}
    deadline = time.time() + timeout_seconds
    while pending and time.time() < deadline:
        time.sleep(3)
        for cid in sorted(pending):
            status, body = _request("GET", f"{gateway}/api/v1/composition/{runs[cid]['compositionId']}", token)
            if status != 200:
                continue
            state = body.get("compositionStatus")
            if state != "REVIEWING":
                runs[cid]["compositionStatus"] = state
                runs[cid]["durationSeconds"] = round(time.time() - runs[cid]["submittedAt"], 1)
                pending.discard(cid)
                print(f"  {cid}: {state} after {runs[cid]['durationSeconds']}s", flush=True)
    for cid in pending:
        runs[cid]["compositionStatus"] = "REVIEWING"
        runs[cid]["error"] = f"still grading after {timeout_seconds}s (timeout)"
        print(f"  {cid}: timed out", flush=True)


# ---------------------------------------------------------------- Postgres (via docker compose)

class Database:
    def __init__(self, compose_dir: Path):
        self.compose_dir = compose_dir

    def query(self, sql: str) -> str:
        result = subprocess.run(
            ["docker", "compose", "exec", "-T", "postgres-db", "psql", "-U", "postgres", "-d", "sllp_main_db",
             "-At", "-v", "ON_ERROR_STOP=1", "-c", sql],
            cwd=self.compose_dir, capture_output=True, text=True, encoding="utf-8")
        if result.returncode != 0:
            raise RuntimeError(result.stderr.strip())
        return result.stdout.strip()

    def json(self, sql: str) -> Any:
        text = self.query(sql)
        return json.loads(text) if text else None

    def workflow(self, composition_id: int) -> Optional[dict]:
        row = self.json(f"""
            SELECT row_to_json(t) FROM (
              SELECT workflow_id, status, grading_passes, error_details,
                     evidence_output, grading_output, review_output, suggestion_output,
                     input_payload->'composition'->>'content'  AS content,
                     input_payload->'composition'->>'language' AS language
              FROM agent_workflow_run WHERE composition_id = {int(composition_id)}
              ORDER BY created_at DESC LIMIT 1) t""")
        if not row:
            return None
        return {"workflowId": row["workflow_id"], "status": row["status"], "gradingPasses": row["grading_passes"],
                "errorDetails": row["error_details"], "evidence": row["evidence_output"], "grading": row["grading_output"],
                "review": row["review_output"], "suggestions": row["suggestion_output"],
                "content": row["content"], "language": row["language"]}

    def usage(self, workflow_id: str) -> list:
        if not UUID.match(workflow_id):
            return []
        return self.json(f"""
            SELECT coalesce(json_agg(row_to_json(u)), '[]') FROM (
              SELECT agent, model, sum(total_tokens) AS tokens, sum(estimated_cost_micros) AS "costMicros", count(*) AS calls
              FROM ai_usage_event WHERE workflow_id = '{workflow_id}' GROUP BY agent, model) u""") or []

    def lakera_detections(self, composition_id: int) -> Optional[int]:
        try:
            return int(self.query(f"SELECT count(*) FROM lakera_guard_detection WHERE composition_id = {int(composition_id)}"))
        except RuntimeError:
            return None  # table not migrated in this environment


def collect(args: argparse.Namespace, cases: list[dict]) -> dict:
    print(f"Logging in to {args.gateway} as {args.username}", flush=True)
    token = login(args.gateway, args.username, args.password)
    print(f"Submitting {len(cases)} essays for grading", flush=True)
    runs = {}
    for case in cases:
        runs[case["id"]] = submit(args.gateway, token, case)
        run = runs[case["id"]]
        print(f"  {case['id']}: " + (f"ERROR {run['error']}" if "error" in run else f"composition {run['compositionId']} submitted"),
              flush=True)
    if all("error" in run for run in runs.values()):
        print("Every submission failed, so no essay reached the agents. See the errors above.", flush=True)
    else:
        print("Waiting for the agents to finish (evidence -> grading -> review -> suggestions)", flush=True)
    wait_for_results(args.gateway, token, runs, args.timeout)

    database = Database(Path(args.compose_dir))
    for cid, run in runs.items():
        if "compositionId" not in run:
            continue
        run["workflow"] = database.workflow(run["compositionId"])
        run["usage"] = database.usage(run["workflow"]["workflowId"]) if run["workflow"] else []
        run["lakeraDetections"] = database.lakera_detections(run["compositionId"])
    return runs


# ---------------------------------------------------------------- scoring + outputs

def git_commit(directory: str) -> Optional[str]:
    try:
        return subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=directory, capture_output=True,
                              text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return None


def build_run(cases: list[dict], raw: dict, baseline: Optional[dict], info: dict) -> dict:
    scored = [evaluation.score_case(case, raw.get(case["id"]) or {"error": "no result collected"}) for case in cases]
    metrics = evaluation.aggregate(scored)
    models = evaluation.run_models(scored)
    drift = evaluation.compare_with_baseline(scored, metrics, models, baseline)
    gates = evaluation.evaluate_gates(metrics)
    return {
        "info": info, "cases": scored, "metrics": metrics, "models": models, "drift": drift, "gates": gates,
        "verdict": evaluation.verdict(gates, drift),
        "contents": {case["id"]: case["content"] for case in cases},
        "baselineCandidate": evaluation.baseline_candidate(
            scored, metrics, models,
            {"build": info.get("build"), "commit": info.get("commit"), "timestamp": info.get("timestamp")}),
    }


def write_outputs(run: dict, raw: dict, out: Path) -> None:
    out.mkdir(parents=True, exist_ok=True)
    (out / "ai-eval-report.html").write_text(report.build_html(run), encoding="utf-8")
    (out / report.CSS_FILE).write_text(report.CSS, encoding="utf-8")
    (out / "ai-eval-junit.xml").write_text(report.build_junit(run), encoding="utf-8")
    (out / "ai-eval-summary.txt").write_text(report.build_summary(run) + "\n", encoding="utf-8")
    (out / "ai-eval-raw.json").write_text(json.dumps({"info": run["info"], "results": raw}, indent=2, ensure_ascii=False),
                                          encoding="utf-8")
    scored = {k: v for k, v in run.items() if k not in ("contents", "baselineCandidate")}
    (out / "ai-eval-scored.json").write_text(json.dumps(scored, indent=2, ensure_ascii=False), encoding="utf-8")
    # Only a clean, unsimulated run is a sensible baseline; others are still written for inspection.
    candidate = dict(run["baselineCandidate"], usableAsBaseline=run["verdict"] == "PASS" and not run["info"].get("driftSimulation"))
    (out / "ai-eval-baseline-candidate.json").write_text(json.dumps(candidate, indent=2, ensure_ascii=False), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--gateway", default="http://localhost:18080")
    parser.add_argument("--compose-dir", default=str(HERE.parent), help="directory holding docker-compose.yaml")
    parser.add_argument("--cases", default=str(HERE / "cases.json"))
    parser.add_argument("--baseline", default=str(HERE / "baseline.json"))
    parser.add_argument("--out", default="ai-eval-reports")
    parser.add_argument("--username", default="testuser")
    parser.add_argument("--password", default="password123")
    parser.add_argument("--timeout", type=int, default=600, help="seconds to wait for all essays")
    parser.add_argument("--build", default="", help="CI build number, shown in the report")
    parser.add_argument("--drift-simulation", default="", help="description of a simulated drift, if any")
    parser.add_argument("--lakera", default="", help="'true' / 'false': whether Lakera Guard is configured")
    parser.add_argument("--rescore", help="score a saved ai-eval-raw.json instead of calling the system")
    args = parser.parse_args()

    cases = json.loads(Path(args.cases).read_text(encoding="utf-8"))
    baseline_path = Path(args.baseline)
    baseline = json.loads(baseline_path.read_text(encoding="utf-8")) if baseline_path.is_file() else None

    if args.rescore:
        saved = json.loads(Path(args.rescore).read_text(encoding="utf-8"))
        raw, info = saved["results"], saved["info"]
    else:
        raw = collect(args, cases)
        info = {"build": args.build, "commit": git_commit(args.compose_dir),
                "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
                "driftSimulation": args.drift_simulation, "lakera": args.lakera}

    run = build_run(cases, raw, baseline, info)
    write_outputs(run, raw, Path(args.out))
    print()
    print(report.build_summary(run))
    for gate in run["gates"]:
        print(f"  gate  {'OK  ' if gate['passed'] else 'FAIL'}  {gate['metric']} = {gate['value']} (min {gate['minimum']})")
    for row in run["drift"]["rows"]:
        print(f"  drift {row['status']:<5} {row['metric']}: baseline {row['baseline']} -> {row['current']}")
    return {"PASS": 0, "FAIL": 1, "DRIFT": 2}[run["verdict"]]


if __name__ == "__main__":
    sys.exit(main())
