---
name: architecture-stage
description: Run Stage 2 of the SDLC pipeline — Architecture. Spawns the architecture-designer subagent to read requirements.md and produce architecture.md.
---

# Stage 2 — Architecture

You are orchestrating the Architecture stage of the SDLC pipeline.

## Steps

1. Confirm `requirements.md` exists and has Status `Approved`. If not, tell the human to complete Stage 1 first.
2. Spawn the **architecture-designer** subagent and instruct it to:
   - Read `requirements.md`
   - Propose an appropriate architecture with component diagram, technology choices, data flow, and error handling strategy
   - Write `architecture.md` using its defined template
3. After the subagent presents the draft `architecture.md`, show it to the human and ask:
   > "Please review `architecture.md`. Type **APPROVED** to commit this artifact and proceed to Stage 3, or provide feedback for revisions."
4. On approval:
   - Ensure `architecture.md` Status is updated to `Approved`
   - Run: `git add architecture.md && git commit -m "Stage 2: Add approved architecture.md"`
5. Confirm: "Stage 2 complete. Artifact committed. Ready for `/design-review-stage` when you are."

## Do not proceed to Stage 3 without explicit human approval.
