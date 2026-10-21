# User Story

## Story ID
DOC-SYNC-001

## Title
Automated Documentation Sync

## User Story
As a developer, I want a command-line tool that monitors changes in my Python codebase and automatically updates a `docs/` folder with generated documentation extracted from docstrings and function signatures, so that my documentation stays in sync with the code without manual effort.

## Background
Currently, developers on the team forget to update documentation when they change Python modules. This causes docs to drift from the actual codebase. We need a lightweight tool that runs locally (or in CI) and keeps docs current automatically.

## Acceptance Criteria
- [ ] The tool can be pointed at a Python source directory and a target docs directory.
- [ ] When run in watch mode, it detects file save events and regenerates affected documentation.
- [ ] When run in one-shot mode, it processes all Python files in the source directory and writes docs.
- [ ] Documentation is generated from module docstrings, class docstrings, and function/method signatures + docstrings.
- [ ] Output documentation is written as Markdown files (one `.md` per Python source file).
- [ ] The tool gracefully handles Python files with no docstrings.
- [ ] The tool ignores files matching common exclusion patterns (`__pycache__`, `.venv`, `test_*.py`).
- [ ] The tool reports which files were updated and which were skipped.
- [ ] A test suite covers the core parsing and generation logic.

## Out of Scope
- Generating HTML or PDF output (Markdown only for this story)
- Integration with external documentation platforms (Confluence, ReadTheDocs)
- Parsing type annotations beyond what appears in docstrings
- Support for non-Python source files

## Constraints
- Must run on Python 3.11+
- Dependencies must be installable via `pip`
- Must not require a running server or daemon — CLI only

## Notes
- The `ast` standard library module is preferred for parsing over third-party parsers.
- `watchdog` is acceptable for the file-watch mode.
- Output format should be human-readable Markdown, not machine-generated HTML.
