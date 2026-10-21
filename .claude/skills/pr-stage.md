---
name: pr-stage
description: Run Stage 8 of the SDLC pipeline — Pull Request. Spawns the pr-creator subagent to prepare commits, push the branch, and create the GitHub PR. Requires explicit human authorization before any push or PR creation.
---

# Stage 8 — Pull Request

You are orchestrating the Pull Request stage of the SDLC pipeline.

## Steps

1. **Pre-flight checks** — confirm all of the following before proceeding:
   - `requirements.md`, `architecture.md`, `design-review.md`, `impl-plan.md`, `code-review.md` all exist
   - `pytest tests/ -v` passes (run it now to confirm)
   - `gh auth status` returns authenticated — if not, stop and tell the human to run `gh auth login`
   - A git remote named `origin` exists — check with `git remote -v`

2. If any pre-flight check fails, stop and tell the human exactly what needs to be resolved.

3. Spawn the **pr-creator** subagent and instruct it to:
   - Run `git status` and `git diff --cached` to inspect the working tree
   - Stage only implementation files, tests, and SDLC artifacts (never `.env`, `__pycache__`, secrets)
   - Commit any uncommitted changes with a meaningful message
   - **STOP before pushing** — report exactly what will be pushed and ask for human authorization

4. After the subagent reports what will be pushed, ask the human:
   > "The pr-creator subagent is ready to push branch `<branch>` to `origin` and create a Pull Request. Type **AUTHORIZED** to proceed, or type **STOP** to cancel."

5. On authorization, instruct the pr-creator to:
   - Run `git push -u origin <branch>`
   - Run `gh pr create` with the PR description template (Summary, Changes Made, Test Evidence, Known Limitations, Reviewer Checklist)
   - Report the PR URL

6. Confirm: "Stage 8 complete. PR URL: <url>. Pipeline run finished."

## This stage requires two explicit human actions: AUTHORIZED for the push, and final merge approval via GitHub.
## Never push or create a PR without the human typing AUTHORIZED in the current conversation turn.
