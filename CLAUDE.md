# CLAUDE.md — Agentic SDLC Pipeline

This project implements a reusable 8-stage Agentic SDLC pipeline. Any user story placed in `user-story.md` can be run through the full pipeline below.

---

## Pipeline Stages

| Stage | Command | Artifact |
|-------|---------|----------|
| 1. Requirements | `/requirements-stage` | `requirements.md` |
| 2. Architecture | `/architecture-stage` | `architecture.md` |
| 3. Design Review | `/design-review-stage` | `design-review.md` |
| 4. Implementation Plan | `/impl-plan-stage` | `impl-plan.md` |
| 5. Implementation | CLAUDE.md-guided | source code + tests |
| 6. Code Review | `/code-review` | `code-review.md` |
| 7. Verify | `/verify-stage` | test evidence in `code-review.md` |
| 8. Pull Request | `/pr-stage` | GitHub PR URL |

---

## Human Approval Gates

**Stop and wait for explicit human approval before:**
- Committing any SDLC artifact (requirements.md, architecture.md, design-review.md, impl-plan.md)
- Any `git push` or remote operation
- Creating or updating a GitHub Pull Request
- Implementing changes that deviate from the approved architecture or requirements
- Adding or removing dependencies

Do not silently advance to the next stage. Always summarize what was produced and ask the human to approve before proceeding.

---

## Stage Progression Rules

1. Each stage must produce its artifact before the next stage begins.
2. An artifact is not complete until it has been reviewed and approved by the human.
3. If a later stage reveals a problem requiring changes to an earlier artifact, flag it and obtain approval before backtracking.
4. Do not hardcode assumptions from the current user story — all stage logic must derive from the artifact chain.

---

## Artifact Conventions

Each `.md` artifact must include:
- A `## Status` heading at the top: `Draft | Under Review | Approved`
- A `## Last Updated` line with the date
- All required content sections for that artifact type (see per-stage skill for specifics)

Artifact files live in the project root. Do not rename or relocate them.

---

## Subagent Routing

Invoke the appropriate subagent for each stage:

| Stage | Subagent |
|-------|---------|
| Requirements | `requirements-analyst` |
| Architecture | `architecture-designer` |
| Design Review | `design-reviewer` |
| Implementation Plan | `implementation-planner` |
| Code Review | `code-reviewer` |
| Verify | `test-engineer` |
| Pull Request | `pr-creator` |

---

## Allowed Tools / Permissions

The following Bash operations are approved for this project:
- `git init`, `git add`, `git commit`, `git branch`, `git checkout`, `git log`, `git status`, `git diff`
- `git push` — requires human approval first
- `gh repo create`, `gh pr create` — requires human approval first
- `python`, `python3`, `pytest`, `pip install`, `pip list`
- `cat`, `ls`, `find`, `mkdir`, `cp`, `mv`

Do not run destructive commands (`rm -rf`, `git reset --hard`, `git clean -f`) without explicit human instruction.

---

## GitHub Setup Prerequisite (Stage 8)

Before Stage 8, confirm:
1. `gh auth status` returns authenticated — if not, run `gh auth login`
2. A remote GitHub repository exists — if not, run `gh repo create claude-cap --public` and `git remote add origin <url>`
3. `gh` CLI is installed — if not, install from https://cli.github.com

---

## Security Rules

- Never commit `.env`, credentials, tokens, or API keys.
- Always run `git status` and `git diff --cached` before committing to verify what is staged.
- User input passed to CLI tools must be validated before use.
- Do not include implementation secrets in SDLC artifact markdown files.
