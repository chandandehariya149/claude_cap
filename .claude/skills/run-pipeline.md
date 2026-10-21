---
name: run-pipeline
description: Start or resume the full 8-stage Agentic SDLC pipeline. Invokes the sdlc-orchestrator agent which drives all stages in order, pausing at each human approval gate. Run this once to kick off the entire pipeline for the user story in user-story.md.
---

# Run Pipeline

Invoke the **sdlc-orchestrator** agent to drive the complete SDLC pipeline.

## Instructions

1. Check which stage to start or resume from:
   - Read `git log --oneline` to see which stage commits exist.
   - Check for existing artifacts: `requirements.md`, `architecture.md`, `design-review.md`, `impl-plan.md`, `code-review.md`.
   - Resume from the first incomplete stage.

2. Spawn the **sdlc-orchestrator** agent with the following context:
   - The project root is the current working directory.
   - The user story is in `user-story.md`.
   - All SDLC artifacts must be committed to git after human approval.
   - Human approval gates must be respected at every stage.
   - The progress tracker must be shown at each turn.

3. The orchestrator will run until the pipeline completes (PR created) or the human stops it.

## Resuming

If the pipeline was interrupted, running `/run-pipeline` again will resume from where it left off. The orchestrator checks existing artifacts and git history to determine the current stage.

## Switching User Stories

To run the pipeline for a different user story:
1. Replace the contents of `user-story.md` with the new story.
2. Delete any existing artifact `.md` files (requirements.md, architecture.md, etc.) or create a new git branch.
3. Run `/run-pipeline`.
