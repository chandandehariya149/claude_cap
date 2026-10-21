---
name: design-review-stage
description: Run Stage 3 of the SDLC pipeline — Design Review. Spawns the design-reviewer subagent to cross-check architecture.md against requirements.md and produce design-review.md.
---

# Stage 3 — Design Review

You are orchestrating the Design Review stage of the SDLC pipeline.

## Steps

1. Confirm both `requirements.md` and `architecture.md` exist with Status `Approved`. If not, complete earlier stages first.
2. Spawn the **design-reviewer** subagent and instruct it to:
   - Read both `requirements.md` and `architecture.md`
   - Review across all dimensions: requirement coverage, missing components, security, scalability, maintainability, error handling, dependency risks
   - Record findings with severity (Critical / Major / Minor / Advisory)
   - Write `design-review.md` using its defined template
   - For each Critical/Major finding, propose a concrete resolution
3. Present findings to the human. For each approved resolution that requires an architecture change:
   - Update `architecture.md` accordingly
   - Re-mark `architecture.md` Status as `Approved` after human confirmation
4. Ask:
   > "Please review `design-review.md` (and updated `architecture.md` if applicable). Type **APPROVED** to commit and proceed to Stage 4, or provide feedback."
5. On approval:
   - Ensure `design-review.md` Status is `Approved`
   - Run: `git add design-review.md architecture.md && git commit -m "Stage 3: Add design-review.md and update architecture.md"`
6. Confirm: "Stage 3 complete. Ready for `/impl-plan-stage` when you are."

## Do not proceed to Stage 4 without explicit human approval.
