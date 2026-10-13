"""HTML / JUnit / one-line outputs for an AI eval run.

The HTML is plain markup + inline SVG with no scripts, and its styles live in a separate
file: Jenkins serves archived and published HTML under a Content-Security-Policy that blocks
inline styles and scripts but allows same-origin CSS (the load-test report does the same).
"""

from __future__ import annotations

from html import escape
from typing import Any, Optional
from xml.sax.saxutils import quoteattr

from evaluation import DRIFT_RATE_TOLERANCE, DRIFT_SCORE_TOLERANCE, GATES

CSS_FILE = "ai-eval-report.css"

AGENT_ORDER = ["evidence", "grading", "review", "suggestions"]
GATE_LABEL = {
    "completionRate": "Every normal essay graded end to end",
    "groundingRate": "Every evidence excerpt quoted verbatim",
    "rubricCoverage": "Rubric criteria covered",
    "injectionResistance": "Prompt injections resisted",
    "suggestionSafetyRate": "Suggestions free of injected text",
}
OUTCOME_TEXT = {
    "BLOCKED": "Blocked by Lakera Guard",
    "REFUSED": "Stopped safely",
    "HANDLED": "Ignored by the agents",
    "COMPROMISED": "Manipulated",
}


def _e(value: Any) -> str:
    return escape("" if value is None else str(value))


def _pct(value: Optional[float]) -> str:
    return "–" if value is None else f"{value * 100:.0f}%"


def _num(value: Optional[float], digits: int = 0) -> str:
    return "–" if value is None else f"{value:,.{digits}f}"


def _usd(micros: Optional[float]) -> str:
    return "–" if micros is None else f"${micros / 1_000_000:.4f}"


def _pill(text: str, tone: str) -> str:
    return f'<span class="pill {tone}">{_e(text)}</span>'


def _table(head: list[str], rows: list[list[str]], cls: str = "") -> str:
    return (f'<div class="scroll"><table class="{cls}"><thead><tr>' + "".join(f"<th>{h}</th>" for h in head)
            + "</tr></thead><tbody>" + "".join("<tr>" + "".join(f"<td>{c}</td>" for c in r) + "</tr>" for r in rows)
            + "</tbody></table></div>")


# ---------------------------------------------------------------- charts (inline SVG)

def _drift_chart(rows: list[dict]) -> str:
    """Paired bars per quality rate: baseline (grey) vs this run (green / red)."""
    rates = [r for r in rows if r["kind"] == "rate"]
    if not rates:
        return ""
    label_w, bar_w, right, row_h, top = 300, 520, 110, 34, 8
    width = label_w + bar_w + right
    height = top + len(rates) * row_h + 26
    svg = [f'<svg viewBox="0 0 {width} {height}" width="100%" role="img" aria-label="Quality rates against the baseline" '
           f'font-family="system-ui, sans-serif">']
    for tick in (0, 0.25, 0.5, 0.75, 1.0):
        x = label_w + tick * bar_w
        svg.append(f'<line x1="{x}" y1="{top}" x2="{x}" y2="{height - 22}" stroke="#e5e7eb"/>')
        svg.append(f'<text x="{x}" y="{height - 6}" font-size="11" fill="#6b7280" text-anchor="middle">{tick * 100:.0f}%</text>')
    for i, row in enumerate(rates):
        y = top + i * row_h
        drift = row["status"] == "DRIFT"
        svg.append(f'<text x="0" y="{y + 17}" font-size="13" fill="#111827">{_e(row["label"])}</text>')
        svg.append(f'<rect x="{label_w}" y="{y + 4}" width="{max(1, row["baseline"] * bar_w):.1f}" height="9" rx="2" fill="#cbd5e1">'
                   f'<title>Baseline {_pct(row["baseline"])}</title></rect>')
        svg.append(f'<rect x="{label_w}" y="{y + 15}" width="{max(1, row["current"] * bar_w):.1f}" height="9" rx="2" '
                   f'fill="{"#dc2626" if drift else "#059669"}"><title>This run {_pct(row["current"])}</title></rect>')
        floor = max(0.0, row["baseline"] - DRIFT_RATE_TOLERANCE)
        fx = label_w + floor * bar_w
        svg.append(f'<line x1="{fx:.1f}" y1="{y + 2}" x2="{fx:.1f}" y2="{y + 26}" stroke="#f59e0b" stroke-width="2" '
                   f'stroke-dasharray="3 2"><title>Drift threshold {_pct(floor)}</title></line>')
        svg.append(f'<text x="{label_w + bar_w + 10}" y="{y + 18}" font-size="13" font-weight="600" '
                   f'fill="{"#dc2626" if drift else "#111827"}">{_pct(row["current"])}</text>')
    svg.append("</svg>")
    legend = ('<div class="legend"><span class="key"><span class="sw base"></span>Baseline</span>'
              '<span class="key"><span class="sw ok"></span>This run</span>'
              '<span class="key"><span class="sw bad"></span>This run (drifted)</span>'
              f'<span class="key"><span class="sw line"></span>Drift threshold (baseline − {DRIFT_RATE_TOLERANCE * 100:.0f} pts)</span></div>')
    return "".join(svg) + legend


def _score_chart(cases: list[dict]) -> str:
    """Per essay: expected band (shaded), baseline score (grey tick) and this run's score (dot)."""
    normal = [c for c in cases if c["kind"] == "normal"]
    if not normal:
        return ""
    label_w, plot_w, right, row_h, top = 220, 600, 70, 40, 8
    width = label_w + plot_w + right
    height = top + len(normal) * row_h + 26
    x = lambda score: label_w + score / 100 * plot_w  # noqa: E731
    svg = [f'<svg viewBox="0 0 {width} {height}" width="100%" role="img" aria-label="Score per essay" font-family="system-ui, sans-serif">']
    for tick in range(0, 101, 20):
        svg.append(f'<line x1="{x(tick)}" y1="{top}" x2="{x(tick)}" y2="{height - 22}" stroke="#e5e7eb"/>')
        svg.append(f'<text x="{x(tick)}" y="{height - 6}" font-size="11" fill="#6b7280" text-anchor="middle">{tick}</text>')
    for i, case in enumerate(normal):
        cy = top + i * row_h + row_h / 2
        low, high = case["expectedBand"]
        svg.append(f'<text x="0" y="{cy - 2}" font-size="13" font-weight="600" fill="#111827">{_e(case["label"])}</text>')
        svg.append(f'<text x="0" y="{cy + 13}" font-size="11" fill="#6b7280">{_e(case["id"])}</text>')
        svg.append(f'<rect x="{x(low):.1f}" y="{cy - 9}" width="{x(high) - x(low):.1f}" height="18" rx="4" fill="#e0f2fe">'
                   f'<title>Expected band {low}-{high}</title></rect>')
        if case.get("baselineScore") is not None:
            bx = x(case["baselineScore"])
            svg.append(f'<line x1="{bx:.1f}" y1="{cy - 12}" x2="{bx:.1f}" y2="{cy + 12}" stroke="#64748b" stroke-width="3">'
                       f'<title>Baseline score {case["baselineScore"]:.0f}</title></line>')
        if case["score"] is not None:
            in_band = low <= case["score"] <= high
            svg.append(f'<circle cx="{x(case["score"]):.1f}" cy="{cy}" r="7" fill="{"#059669" if in_band else "#dc2626"}" '
                       f'stroke="#fff" stroke-width="2"><title>Score {case["score"]:.0f}</title></circle>')
            svg.append(f'<text x="{label_w + plot_w + 10}" y="{cy + 5}" font-size="13" font-weight="600" fill="#111827">'
                       f'{case["score"]:.0f}</text>')
        else:
            svg.append(f'<text x="{label_w + plot_w + 10}" y="{cy + 5}" font-size="12" fill="#dc2626">no score</text>')
    svg.append("</svg>")
    legend = ('<div class="legend"><span class="key"><span class="sw band"></span>Expected band</span>'
              '<span class="key"><span class="sw dot"></span>This run</span>'
              '<span class="key"><span class="sw tick"></span>Baseline score</span></div>')
    return "".join(svg) + legend


# ---------------------------------------------------------------- sections

def _check_matrix(cases: list[dict]) -> str:
    names: list[str] = []
    for case in cases:
        for check in case["checks"]:
            if check["name"] not in names:
                names.append(check["name"])
    rows = []
    for case in cases:
        by_name = {c["name"]: c for c in case["checks"]}
        cells = [f'<strong>{_e(case["label"])}</strong><br><span class="muted">{_e(case["id"])}</span>']
        for name in names:
            check = by_name.get(name)
            if check is None or check["passed"] is None:
                cells.append('<span class="cell na" title="not applicable">–</span>')
            else:
                mark, tone = ("✓", "ok") if check["passed"] else ("✗", "bad")
                cells.append(f'<span class="cell {tone}" title="{_e(check["detail"])}">{mark}</span>')
        rows.append(cells)
    return _table(["Essay"] + [_e(n) for n in names], rows, "matrix")


def _essay_table(cases: list[dict]) -> str:
    rows = []
    for c in cases:
        tone = "ok" if c["completed"] or c["outcome"] in ("BLOCKED", "REFUSED", "HANDLED") else "bad"
        if c["kind"] == "injection":
            outcome = _pill(OUTCOME_TEXT.get(c["outcome"], c["outcome"]), "ok" if c["outcome"] != "COMPROMISED" else "bad")
        else:
            outcome = _pill("Graded" if c["completed"] else "Failed", tone)
        rows.append([
            f'<strong>{_e(c["label"])}</strong><br><span class="muted">{_e(c["title"])}</span>',
            outcome,
            _num(c["score"]) + (f' <span class="muted">(base {_num(c["baselineScore"])})</span>' if c.get("baselineScore") is not None else ""),
            f'{c["coveredCriteria"]}/{c["rubricCriteria"]}' if c["rubricCriteria"] else "–",
            f'{c["groundedExcerpts"]}/{c["excerpts"]}' if c["excerpts"] else "–",
            f'{c["firstPassItems"]}/{c["evidenceItems"]}' if c["evidenceItems"] else "–",
            _e(c["reviewDecision"] or "–") + (f' <span class="muted">×{c["gradingPasses"]}</span>' if c["gradingPasses"] > 1 else ""),
            _e(c["suggestionStatus"] or "–"),
            _num(c["durationSeconds"], 1) + "s" if c["durationSeconds"] is not None else "–",
            _usd(c["costMicros"]) if c["costMicros"] else "–",
        ])
    return _table(["Essay", "Result", "Score", "Rubric covered", "Excerpts verbatim", "Verified 1st pass",
                   "Review", "Suggestions", "Time", "Cost"], rows)


def _injection_cards(cases: list[dict], lakera: str) -> str:
    cards = []
    for c in (c for c in cases if c["kind"] == "injection"):
        check = next((k for k in c["checks"] if k["name"] == "Prompt injection resisted"), None)
        tone = "ok" if c["outcome"] != "COMPROMISED" else "bad"
        cards.append(f'<div class="inj {tone}"><div class="inj-head">{_pill(OUTCOME_TEXT.get(c["outcome"], c["outcome"]), tone)}'
                     f'<strong>{_e(c["label"])}</strong></div>'
                     f'<div class="muted">{_e(check["detail"] if check else "")}</div></div>')
    note = {"true": "Lakera Guard is active: flagged essays are stopped before the grading model sees them.",
            "false": "Lakera Guard is not configured in this environment (no lakera-api-key credential), so these "
                     "results show how the agents themselves resist injection."}.get(lakera, "")
    return f'<p class="muted">{_e(note)}</p><div class="injs">{"".join(cards)}</div>'


def _gates_table(gates: list[dict]) -> str:
    rows = [[_e(GATE_LABEL.get(g["metric"], g["metric"])), _pct(g["minimum"]), _pct(g["value"]),
             _pill("Met", "ok") if g["passed"] else _pill("Not met", "bad")] for g in gates]
    return _table(["Gate", "Minimum", "This run", "Status"], rows)


def _drift_section(drift: dict, baseline_hint: str) -> str:
    if not drift["available"]:
        return (f'<div class="callout">No baseline yet, so there is nothing to compare against. {baseline_hint}</div>')
    created = drift.get("createdFrom") or {}
    parts = [f'<p class="muted">Baseline from build #{_e(created.get("build", "?"))} · commit {_e(created.get("commit", "?"))}'
             f' · {_e(created.get("timestamp", ""))}. Quality rates may drop at most {DRIFT_RATE_TOLERANCE * 100:.0f} points; '
             f'the same essays\' scores may move at most {DRIFT_SCORE_TOLERANCE:.0f} points on average.</p>']
    if drift["modelChanges"]:
        changes = "".join(f'<li><strong>{_e(m["agent"])}</strong>: {_e(m["baseline"])} → <strong>{_e(m["current"])}</strong></li>'
                          for m in drift["modelChanges"])
        parts.append(f'<div class="callout warn"><strong>Model changed since the baseline</strong><ul>{changes}</ul></div>')
    parts.append(f'<div class="card">{_drift_chart(drift["rows"])}</div>')
    rows = []
    for r in drift["rows"]:
        if r["kind"] == "rate":
            base, cur, change = _pct(r["baseline"]), _pct(r["current"]), f'{r["change"] * 100:+.0f} pts'
        elif r["kind"] == "points":
            base, cur, change = "–", f'{r["current"]:.1f}', f'limit {r["tolerance"]:.0f}'
        else:
            base, cur, change = _num(r["baseline"], 1), _num(r["current"], 1), f'{r["change"] * 100:+.0f}%'
        tone = {"DRIFT": "bad", "WATCH": "warn"}.get(r["status"], "ok")
        rows.append([_e(r["label"]), base, cur, change, _pill(r["status"], tone)])
    parts.append(_table(["Metric", "Baseline", "This run", "Change", "Status"], rows))
    return "".join(parts)


def _versions_table(cases: list[dict], models: dict) -> str:
    versions: dict[str, Any] = {}
    for case in cases:
        for key, value in case["detail"]["versions"].items():
            if value and key not in versions:
                versions[key] = value
    agents = sorted(models, key=lambda a: next((i for i, n in enumerate(AGENT_ORDER) if n in a.lower()), 99))
    prompt_key = {"evidence": "evidencePrompt", "grading": "gradingPrompt", "review": "reviewPrompt", "suggestion": "suggestionsPrompt"}
    rows = []
    for agent in agents:
        key = next((v for k, v in prompt_key.items() if k in agent.lower()), None)
        rows.append([f"<strong>{_e(agent)}</strong>", f"<code>{_e(models[agent])}</code>",
                     f"<code>{_e(versions.get(key, '–'))}</code>" if key else "–"])
    note = f'<p class="muted">Rubric snapshot: <code>{_e(versions.get("rubric", "–"))}</code></p>'
    return _table(["Agent", "Model (provider-reported)", "Prompt version"], rows) + note


def _essay_details(cases: list[dict], contents: dict) -> str:
    blocks = []
    for c in cases:
        d = c["detail"]
        crit = []
        for item in d["criteria"]:
            ev = "".join(
                f'<li><span class="tag {"ok" if e["polarity"] == "STRENGTH" else "bad"}">{_e(e["polarity"])}</span>'
                f' <q>{_e(e["excerpt"])}</q> <span class="muted">— {_e(e["note"])}</span></li>'
                for e in item["evidence"])
            crit.append(f'<div class="crit"><div class="crit-head"><strong>{_e(item["criterion"])}</strong>'
                        f'<span>{_num(item["score"])}</span></div>'
                        f'<div class="muted">{_e(item["rationale"] or "")}</div><ul>{ev}</ul></div>')
        sugg = "".join(f'<li><span class="tag">{_e(s["priority"])}</span> <strong>{_e(s["criterion"])}</strong>: '
                       f'{_e(s["recommendation"])}</li>' for s in d["suggestions"])
        findings = "".join(f"<li>{_e(f)}</li>" for f in d["reviewFindings"])
        failure = f'<div class="callout bad">{_e(c["failure"])}</div>' if c["failure"] and not c["completed"] else ""
        blocks.append(
            f'<details><summary><strong>{_e(c["label"])}</strong> <span class="muted">· {_e(c["title"])}</span>'
            f' · score {_num(c["score"])}</summary>{failure}'
            f'<h4>Essay</h4><pre>{_e(contents.get(c["id"], ""))}</pre>'
            + (f'<h4>Evidence and grading per criterion</h4>{"".join(crit)}' if crit else "")
            + (f"<h4>Review findings</h4><ul>{findings}</ul>" if findings else "")
            + (f"<h4>Suggestions</h4><ul>{sugg}</ul>" if sugg else "")
            + "</details>")
    return "".join(blocks)


def build_html(run: dict) -> str:
    cases, metrics, drift, gates = run["cases"], run["metrics"], run["drift"], run["gates"]
    info = run["info"]
    normal = [c for c in cases if c["kind"] == "normal"]
    injections = [c for c in cases if c["kind"] == "injection"]
    gates_failed = [g for g in gates if not g["passed"]]

    if drift.get("drifted"):
        tone, headline = "bad", "Drift detected"
        sub = "Quality moved away from the baseline although the code under test did not change."
        if gates_failed:
            sub += f" {len(gates_failed)} release gate(s) are also not met."
    elif gates_failed:
        tone, headline = "bad", "Quality gate failed"
        sub = f"{len(gates_failed)} release gate(s) not met — this build must not be released."
    else:
        tone, headline = "ok", "All checks passed"
        sub = ("Every essay was graded with verified evidence, all injections were resisted, and quality is within "
               "tolerance of the baseline." if drift["available"] else
               "Every essay was graded with verified evidence and all injections were resisted.")

    resisted = sum(1 for c in injections if c["outcome"] != "COMPROMISED")
    tiles = [
        ("Essays graded", f'{sum(1 for c in normal if c["completed"])} / {len(normal)}', metrics["completionRate"] == 1),
        ("Evidence quoted verbatim", _pct(metrics["groundingRate"]), metrics["groundingRate"] == 1),
        ("Rubric criteria covered", _pct(metrics["rubricCoverage"]), (metrics["rubricCoverage"] or 0) >= GATES["rubricCoverage"]),
        ("Injections resisted", f"{resisted} / {len(injections)}", resisted == len(injections)),
        ("Mean score", _num(metrics["meanScore"], 1), None),
        ("Cost per essay", _usd(metrics["avgCostMicrosPerEssay"]), None),
    ]
    tile_html = "".join(
        f'<div class="tile {"" if good is None else ("ok" if good else "bad")}"><div class="v">{v}</div><div class="l">{_e(label)}</div></div>'
        for label, v, good in tiles)

    meta = [f'Build #{_e(info.get("build") or "local")}', f'commit <code>{_e(info.get("commit") or "?")}</code>',
            _e(info.get("timestamp")), f'{len(cases)} essays']
    simulation = ""
    if info.get("driftSimulation"):
        simulation = (f'<div class="callout warn"><strong>Drift simulation:</strong> {_e(info["driftSimulation"])}. '
                      "Same code and test essays as the baseline; only the model behind the agent(s) was swapped, "
                      "to show that the drift check catches a silent provider-side model change.</div>")

    hint = ("Once a run you trust has passed, commit its <code>ai-eval-baseline-candidate.json</code> artifact as "
            "<code>sllp-ai-eval/baseline.json</code>; later runs are then compared against it.")
    return f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>AI Evaluation Report</title>
<link rel="stylesheet" href="{CSS_FILE}"></head>
<body><main>
  <h1>AI evaluation report</h1>
  <div class="sub">Assessment pipeline: evidence → grading → review → suggestions · {" · ".join(meta)}</div>
  <div class="verdict {tone}"><div class="vh">{_e(headline)}</div><div>{_e(sub)}</div></div>
  {simulation}
  <div class="tiles">{tile_html}</div>

  <h2>1 · Drift against the baseline</h2>
  {_drift_section(drift, hint)}

  <h2>2 · Score per essay</h2>
  <div class="card">{_score_chart(cases)}</div>

  <h2>3 · Checks per essay</h2>
  <p class="muted">Integration (did every agent run and return a valid result), quality and security checks. Hover a cell for details.</p>
  {_check_matrix(cases)}

  <h2>4 · Prompt-injection tests</h2>
  {_injection_cards(cases, info.get("lakera", ""))}

  <h2>5 · Release gates</h2>
  {_gates_table(gates)}

  <h2>6 · Results per essay</h2>
  {_essay_table(cases)}

  <h2>7 · Models and versions under test</h2>
  {_versions_table(cases, run["models"])}

  <h2>8 · What each agent produced</h2>
  {_essay_details(cases, run["contents"])}

  <p class="foot muted">Generated by sllp-ai-eval. Each run submits the same test essays through the API gateway, waits for the
  orchestrator to finish, then scores the persisted output of every agent.</p>
</main></body></html>
"""


CSS = """body { margin: 0; background: #f8fafc; color: #111827; font: 15px/1.5 system-ui, -apple-system, "Segoe UI", sans-serif; }
main { max-width: 1080px; margin: 0 auto; padding: 32px 16px 48px; }
h1 { font-size: 26px; margin: 0 0 4px; }
h2 { font-size: 18px; margin: 36px 0 12px; padding-bottom: 6px; border-bottom: 1px solid #e5e7eb; }
h4 { margin: 16px 0 6px; font-size: 14px; }
.sub, .muted { color: #6b7280; }
.muted { font-size: 13px; }
code { font: 12px ui-monospace, SFMono-Regular, Consolas, monospace; background: #f1f5f9; padding: 1px 5px; border-radius: 4px; }
.verdict { margin-top: 20px; border-radius: 12px; padding: 18px 22px; border: 1px solid; }
.verdict .vh { font-size: 22px; font-weight: 700; }
.verdict.ok { background: #ecfdf5; border-color: #a7f3d0; color: #065f46; }
.verdict.bad { background: #fef2f2; border-color: #fecaca; color: #991b1b; }
.tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-top: 20px; }
.tile { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 14px 16px; }
.tile .v { font-size: 26px; font-weight: 700; }
.tile .l { color: #6b7280; font-size: 13px; }
.tile.ok .v, .ok { color: #047857; } .tile.bad .v, .bad { color: #dc2626; } .warn { color: #b45309; }
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 16px 20px; margin-bottom: 12px; }
.callout { background: #fff; border: 1px solid #e5e7eb; border-left: 4px solid #6366f1; border-radius: 8px; padding: 12px 16px; margin: 12px 0; color: #111827; }
.callout.warn { border-left-color: #f59e0b; background: #fffbeb; }
.callout.bad { border-left-color: #dc2626; background: #fef2f2; }
.callout ul { margin: 6px 0 0; }
.legend { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 8px; font-size: 13px; color: #374151; }
.key { display: inline-flex; align-items: center; gap: 6px; }
.sw { display: inline-block; width: 14px; height: 10px; border-radius: 2px; }
.sw.base { background: #cbd5e1; } .sw.ok { background: #059669; } .sw.bad { background: #dc2626; }
.sw.line { width: 3px; height: 14px; background: #f59e0b; } .sw.band { background: #e0f2fe; border: 1px solid #bae6fd; }
.sw.dot { width: 12px; height: 12px; border-radius: 50%; background: #059669; } .sw.tick { width: 3px; height: 14px; background: #64748b; }
.scroll { overflow-x: auto; }
table { border-collapse: collapse; width: 100%; background: #fff; font-size: 14px; border: 1px solid #e5e7eb; border-radius: 8px; }
th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #e5e7eb; vertical-align: top; }
th { background: #f1f5f9; font-weight: 600; font-size: 13px; }
table.matrix td, table.matrix th { text-align: center; }
table.matrix td:first-child, table.matrix th:first-child { text-align: left; }
.cell { display: inline-block; width: 26px; height: 26px; line-height: 26px; border-radius: 6px; font-weight: 700; cursor: help; }
.cell.ok { background: #d1fae5; } .cell.bad { background: #fee2e2; } .cell.na { color: #9ca3af; }
.pill { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; white-space: nowrap; }
.pill.ok { background: #d1fae5; color: #065f46; } .pill.bad { background: #fee2e2; color: #991b1b; } .pill.warn { background: #fef3c7; color: #92400e; }
.injs { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 12px; }
.inj { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 14px 16px; }
.inj.bad { border-color: #fecaca; }
.inj-head { display: flex; gap: 10px; align-items: center; margin-bottom: 6px; color: #111827; }
details { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; }
summary { cursor: pointer; }
pre { white-space: pre-wrap; background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px 12px; font-family: inherit; font-size: 13px; }
.crit { border-top: 1px solid #f1f5f9; padding: 8px 0; }
.crit-head { display: flex; justify-content: space-between; }
.crit ul, details ul { margin: 6px 0; padding-left: 20px; }
.tag { display: inline-block; font-size: 11px; font-weight: 700; padding: 0 6px; border-radius: 4px; background: #eef2ff; color: #3730a3; }
.tag.ok { background: #d1fae5; color: #065f46; } .tag.bad { background: #fee2e2; color: #991b1b; }
.foot { margin-top: 32px; }
"""


def build_junit(run: dict) -> str:
    """One <testcase> per check, gate and drift metric, so Jenkins tracks them across builds."""
    cases_xml = []

    def case(classname: str, name: str, passed: Optional[bool], message: str) -> None:
        body = ""
        if passed is None:
            body = "<skipped/>"
        elif not passed:
            body = f"<failure message={quoteattr(message)}>{escape(message)}</failure>"
        cases_xml.append(f"<testcase classname={quoteattr(classname)} name={quoteattr(name)} time=\"0\">{body}</testcase>")

    for c in run["cases"]:
        for check in c["checks"]:
            case(f"ai-eval.{check['group']}.{c['id']}", check["name"], check["passed"], check["detail"])
    for g in run["gates"]:
        case("ai-eval.gates", GATE_LABEL.get(g["metric"], g["metric"]), g["passed"],
             f"{g['metric']} = {g['value']} (minimum {g['minimum']})")
    for r in run["drift"]["rows"]:
        if r["status"] == "WATCH":
            continue
        case("ai-eval.drift", r["label"], r["status"] != "DRIFT",
             f"{r['metric']}: baseline {r['baseline']}, this run {r['current']} (tolerance {r['tolerance']})")
    failures = sum(1 for x in cases_xml if "<failure" in x)
    skipped = sum(1 for x in cases_xml if "<skipped" in x)
    return (f'<?xml version="1.0" encoding="UTF-8"?>\n<testsuite name="AI Eval" tests="{len(cases_xml)}" '
            f'failures="{failures}" skipped="{skipped}">\n' + "\n".join(cases_xml) + "\n</testsuite>\n")


def build_summary(run: dict) -> str:
    m, cases = run["metrics"], run["cases"]
    normal = [c for c in cases if c["kind"] == "normal"]
    inj = [c for c in cases if c["kind"] == "injection"]
    label = {"PASS": "PASS", "DRIFT": "DRIFT DETECTED", "FAIL": "GATE FAILED"}[run["verdict"]]
    if run["verdict"] == "FAIL" and run["drift"].get("drifted"):
        label = "DRIFT DETECTED + GATE FAILED"
    shift = run["drift"].get("scoreShift")
    return (f"AI eval: {label} | graded {sum(1 for c in normal if c['completed'])}/{len(normal)} | "
            f"evidence verbatim {_pct(m['groundingRate'])} | injections resisted "
            f"{sum(1 for c in inj if c['outcome'] != 'COMPROMISED')}/{len(inj)} | mean score {_num(m['meanScore'], 1)}"
            + (f" | score shift {shift:.1f} pts vs baseline" if shift is not None else " | no baseline"))
