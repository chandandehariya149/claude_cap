---
name: impl-plan-stage
description: Run Stage 4 of the SDLC pipeline — Implementation Planning. Spawns the implementation-planner subagent to produce a dependency-ordered impl-plan.md.
---

# Stage 4 — Implementation Planning

You are orchestrating the Implementation Planning stage of the SDLC pipeline.

## Steps

1. Confirm `requirements.md`, `architecture.md`, and `design-review.md` all exist with Status `Approved`. If not, complete earlier stages first.
2. Spawn the **implementation-planner** subagent and instruct it to:
   - Read all three approved artifacts
   - Break the implementation into small, independently testable tasks
   - Order tasks by dependency (prerequisite tasks first)
   - For each task: state files to create/modify, acceptance condition, and verification method
   - Include test tasks alongside every business-logic task
   - Write `impl-plan.md` using its defined template
3. After the subagent presents the draft `impl-plan.md`, show it to the human and ask:
   > "Please review `impl-plan.md`. Type **APPROVED** to commit and authorize implementation to begin, or provide feedback."
4. On approval:
   - Ensure `impl-plan.md` Status is `Approved`
   - Run: `git add impl-plan.md && git commit -m "Stage 4: Add approved impl-plan.md"`
5. Confirm: "Stage 4 complete. Implementation is now authorized. Work through tasks in `impl-plan.md` in order. Use `/code-review` after implementation, then `/verify-stage`, then `/pr-stage`."

## Do not begin implementation without explicit human approval of impl-plan.md.
