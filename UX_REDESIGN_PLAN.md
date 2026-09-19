# UX redesign handoff: start here

**Implementation is approved and underway in the frontend.** Approval was granted on 7 September and continuation reaffirmed on 12 September 2026. Preserve the independent audit-logging working-tree changes in both repositories.

The canonical plan lives in the sibling frontend repository:

- [Plan, user decisions and fresh-session instructions](../smart-language-learner-platform-frontend/UX_REDESIGN_PLAN.md)
- [Visual and screen specification](../smart-language-learner-platform-frontend/docs/ux-redesign/DESIGN_SPEC.md)
- [Visual craft guide: composition, blueprints and refinement](../smart-language-learner-platform-frontend/docs/ux-redesign/VISUAL_CRAFT.md)
- [Workflow contract and functionality retention inventory](../smart-language-learner-platform-frontend/docs/ux-redesign/WORKFLOW_CONTRACT.md)
- [Execution checklist, acceptance criteria and continuation record](../smart-language-learner-platform-frontend/docs/ux-redesign/EXECUTION.md)

Read all five before continuing. Keep decisions and progress in that canonical set rather than maintaining a second divergent backend plan. If the sibling is absent, check out `smart-language-learner-platform-frontend` on `feature/calm-writing-studio-20260906` alongside this repository, or obtain those documents before implementation.

## Checkpoints

| Repository | Reviewed commit | Review branch | Redesign branch |
| --- | --- | --- | --- |
| Backend | `e8efc85` | `review/assessment-metrics-20260906` | `feature/calm-writing-studio-20260906` |
| Frontend | `3c859bf` | `review/assessment-metrics-20260906` | `feature/calm-writing-studio-20260906` |

The user's incoming changes plus review fixes were committed for separate testing/finalization. Preserve both review branches. This branch starts from the backend review checkpoint and currently adds this handoff document only. Inspect actual Git state before doing more work; do not reset new user changes. No merge, push or deployment is implied by this plan.

## Backend facts the UX implementation must respect

- Composition create/update accepts title/content and detects language. An accepted update resets the record to DRAFT and clears feedback/suggestions/progress. Avoid hydration autosaves and saves racing with grading.
- Generation returns a new GENERATING record. Review returns REVIEWING and finishes asynchronously. A revision copy can use normal create; there is no linked revision-history contract in this plan.
- Existing DTOs already expose optional suggestions/progress. Frontend types and presentation should match them; no initial migration or new endpoint is needed.
- User-owned mutations and admin inspection endpoints have different access semantics. A UI refactor must preserve those boundaries.
- Document association currently relies on frontend localStorage hints; no cross-device association guarantee should be invented.
- The English suggestions provider still needs the canonical DEEP/MOE framework source for production readiness. The UI must represent available/unavailable feedback truthfully.

Earlier verification: targeted Maven reactor BUILD SUCCESS, with 87 executed tests and **2 skipped admin repository integration tests**; orchestrator 33 passed, suggestions 6 passed; migrations 001-013 had a disposable-database SQL smoke test. These are baseline results, not redesign acceptance. Local Java log if retained: `target/review/java-tests.log` (ignored). See the canonical handoff for limitations and frontend checks.

Last updated: 12 September 2026. Next action: consult the frontend execution record and continue the first unfinished work package. No repeat approval is needed for the agreed scope.
