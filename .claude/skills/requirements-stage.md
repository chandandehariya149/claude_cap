---
name: requirements-stage
description: Run Stage 1 of the SDLC pipeline — Requirements. Spawns the requirements-analyst subagent to read user-story.md, ask clarifying questions, and produce requirements.md.
---

# Stage 1 — Requirements

You are orchestrating the Requirements stage of the SDLC pipeline.

## Steps

1. Confirm `user-story.md` exists in the project root. If it does not, ask the human to create it.
2. Spawn the **requirements-analyst** subagent and instruct it to:
   - Read `user-story.md`
   - Identify ambiguities, missing information, assumptions, and edge cases
   - Ask the human a numbered list of clarifying questions
   - Wait for answers before producing the artifact
   - Write `requirements.md` using its defined template
3. After the subagent presents the draft `requirements.md`, show it to the human and ask:
   > "Please review `requirements.md`. Type **APPROVED** to commit this artifact and proceed to Stage 2, or provide feedback for revisions."
4. On approval:
   - Ensure `requirements.md` Status is updated to `Approved`
   - Run: `git add requirements.md && git commit -m "Stage 1: Add approved requirements.md"`
5. Confirm: "Stage 1 complete. Artifact committed. Ready for `/architecture-stage` when you are."

## Do not proceed to Stage 2 without explicit human approval.
