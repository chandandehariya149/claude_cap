---
name: verify-stage
description: Run Stage 7 of the SDLC pipeline — Verify. Spawns the test-engineer subagent to run the full test suite, document evidence, and update code-review.md with verification results.
---

# Stage 7 — Verify

You are orchestrating the Verification stage of the SDLC pipeline.

## Steps

1. Confirm `code-review.md` exists (Stage 6 must be complete). If not, run `/code-review` first.
2. Confirm implementation files and `tests/` directory exist.
3. Spawn the **test-engineer** subagent and instruct it to:
   - Read `requirements.md` acceptance criteria and `impl-plan.md` verification methods
   - Inventory existing tests and identify coverage gaps
   - Write missing tests for any uncovered acceptance criteria
   - Run `pytest tests/ -v` and record verbatim output
   - Run `python -m py_compile` on all source files
   - Run `ruff check .` or `flake8 .` if available
   - Append a Verification Evidence section to `code-review.md`
   - Confirm all 5 PR description sections can be populated
4. After the subagent presents results, show them to the human and ask:
   > "Please review the verification evidence in `code-review.md`. If all checks pass and any issues are documented, type **APPROVED** to commit and proceed to Stage 8, or describe what needs fixing."
5. On approval:
   - Commit any new or updated test files and `code-review.md`
   - Run: `git add tests/ code-review.md && git commit -m "Stage 7: Add verification evidence and updated tests"`
6. Confirm: "Stage 7 complete. Ready for `/pr-stage` when you are — remember to ensure `gh` CLI is authenticated first."

## Do not proceed to Stage 8 without explicit human approval.
