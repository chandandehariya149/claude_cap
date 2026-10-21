# User Story

## Story ID
DOC-SYNC-001

## Title
Automated Documentation Sync

## User Story
As a Java developer, I want a command-line tool that monitors changes in my Java codebase and automatically updates a `docs/` folder with generated documentation extracted from Javadoc comments and method signatures, so that my documentation stays in sync with the code without manual effort.

## Background
Currently, developers on the team forget to update documentation when they change Java classes. This causes docs to drift from the actual codebase. We need a lightweight CLI tool that runs locally (or in CI) and keeps Markdown documentation current automatically — without requiring a full Javadoc site build.

## Acceptance Criteria
- [ ] The tool can be pointed at a Java source directory and a target docs directory via CLI arguments.
- [ ] When run in watch mode, it detects `.java` file save events and regenerates affected documentation.
- [ ] When run in one-shot mode, it processes all `.java` files in the source directory and writes docs.
- [ ] Documentation is generated from class-level Javadoc, method signatures, and method-level Javadoc comments.
- [ ] Output documentation is written as Markdown files (one `.md` per `.java` source file).
- [ ] The tool gracefully handles `.java` files with no Javadoc comments (outputs structure only).
- [ ] The tool ignores files matching common exclusion patterns (`target/`, `build/`, `*Test.java`, `package-info.java`).
- [ ] The tool reports which files were updated and which were skipped to stdout.
- [ ] A test suite covers the core parsing and Markdown generation logic.

## Out of Scope
- Generating HTML or PDF output (Markdown only for this story)
- Integration with external documentation platforms (Confluence, ReadTheDocs, GitHub Pages)
- Full Javadoc site generation (this is not a Javadoc replacement)
- Support for non-Java source files

## Constraints
- Must run on Java 17+
- Build tool: Maven (`pom.xml`); dependencies installable via Maven Central
- Must not require a running server or daemon — CLI only (runnable via `java -jar` or `mvn exec:java`)
- Must be runnable without an IDE

## Notes
- Use the Java Compiler Tree API (`com.sun.source.tree`) or a library such as JavaParser for parsing `.java` files.
- Use Java NIO `WatchService` (stdlib) for the file-watch mode — no additional dependency needed.
- Output format should be human-readable Markdown, not machine-generated HTML.
- Tests should use JUnit 5 (`junit-jupiter`).
- JavaParser (`com.github.javaparser:javaparser-core`) is acceptable if the Compiler Tree API proves insufficient.
