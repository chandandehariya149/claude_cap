## Status
Approved

## Last Updated
2026-11-13

## Source Story
Jira issue TODO-002 — "Todo App Enhancement — UI Polish, Priority Levels & Filter/Search"
Base story: user-story.md / Jira TODO-001 (see `requirements.md`, Status: Approved)

---

## Overview

This document specifies the requirements for **Todo App v2**, an additive enhancement to the approved Todo List App (TODO-001). All base functionality defined in `requirements.md` remains in force and is unchanged. This document adds three enhancement sets on top of that approved baseline:

- **Enhancement E1 — UI Polish:** visual refinements (animations, hover effects, loading states, completed-task styling, empty-state messaging)
- **Enhancement E2 — Task Priority:** a priority field (High / Medium / Low) on each task, set at creation time, displayed as a colour-coded badge
- **Enhancement E3 — Filter & Search:** a filter/search bar with All / Active / Completed tabs and a live text-search input

No breaking changes are introduced. All 10 original Acceptance Criteria (AC-01 through AC-10 in `requirements.md`) remain in force and are restated as AC-E09.

---

## Functional Requirements

### Inherited — Unchanged (from requirements.md)

The requirements FR-01 through FR-11 from `requirements.md` are carried forward without modification. This document does not restate them in full; they apply as written.

### New — Enhancement E2: Task Priority

**FR-12** — The system shall add a `priority` field of type `String` to the `Todo` model. Valid values are `HIGH`, `MEDIUM`, and `LOW` (case-insensitive on input, stored and returned in uppercase). The default value when not supplied shall be `MEDIUM`.

**FR-13** — The system's `POST /api/todos` endpoint shall accept an optional `priority` field in the JSON request body (e.g. `{ "text": "...", "priority": "HIGH" }`). If the field is omitted or null, the server shall default to `MEDIUM`. If a value is provided that is not one of `HIGH`, `MEDIUM`, or `LOW` (case-insensitive), the server shall return HTTP 400 (Bad Request) with a JSON error body identifying the invalid field.

**FR-14** — The system's `GET /api/todos` endpoint shall include the `priority` field in every returned todo object. The field shall be present on all tasks regardless of when they were created.

**FR-15** — The frontend shall display a priority `<select>` dropdown adjacent to the task text input field. The dropdown options shall be: High, Medium (selected by default), Low. The selected priority shall be included in the POST body when a task is submitted.

**FR-16** — The frontend shall render a colour-coded priority badge on each task row. The badge colours shall be distinct for each level: a red-toned badge for High priority, a yellow-toned badge for Medium priority, and a green-toned badge for Low priority. The badge text shall be the priority label (High / Medium / Low).

### New — Enhancement E3: Filter and Search

**FR-17** — The frontend shall display a filter bar between the add-form and the task list. The filter bar shall contain three tab controls labelled "All", "Active", and "Completed". The default active tab shall be "All". Clicking a tab shall filter the displayed task list:
  - All — shows all tasks
  - Active — shows only tasks where `done` is `false`
  - Completed — shows only tasks where `done` is `true`

**FR-18** — The frontend shall display a live search text input within or adjacent to the filter bar. As the user types in the search input, the visible task list shall update in real time (client-side, no additional API calls) to show only tasks whose text contains the typed string (case-insensitive substring match). An empty search string shall not filter tasks by text.

**FR-19** — The filtering (tab selection) and the search input shall be combinable: the visible task list shall show tasks matching both the active tab's condition and the current search text simultaneously.

**FR-20** — The task counter (originally specified in FR-08 of `requirements.md`) shall reflect the count of currently visible tasks (after applying both filter tab and search), not the total count of all tasks. When no filters are active and the search is empty, the counter shall behave identically to the original FR-08 specification.

**FR-21** — When a new task is successfully added, the filter state (active tab and search text) shall be reset to defaults (All tab selected, search input cleared), and the full unfiltered list shall be rendered showing the newly added task.

### New — Enhancement E1: UI Polish

**FR-22** — The frontend shall apply a smooth CSS fade-in animation to each task row when it is added to the DOM. The animation shall be visible to the user and complete within 500 ms.

**FR-23** — The frontend shall apply a CSS hover effect (a subtle background colour highlight) to each task row when the user's pointer enters the row. The effect shall be purely presentational and require no JavaScript.

**FR-24** — The frontend's existing CSS strike-through style for completed tasks (FR-09 in `requirements.md`) shall be supplemented with a reduced opacity applied to the entire task row (the row shall appear visually muted in addition to the strike-through on the text). The muted colour shall maintain sufficient contrast to remain legible.

**FR-25** — When the task list is empty (zero tasks on the server), the frontend shall display a visually distinct empty-state element (illustration, icon, or styled message) in place of the task list. The empty-state element shall not appear when at least one task exists.

**FR-26** — The Add button shall be disabled and display a spinner or loading indicator from the moment the user clicks Add (initiating the POST request) until the server response is received and the UI has been updated. The button shall return to its normal enabled state after the operation completes (success or failure).

---

## Non-Functional Requirements

### Inherited — Unchanged (from requirements.md)

NFR-01 through NFR-06 from `requirements.md` remain in force.

### New

**NFR-07** — All CSS animations and transitions introduced by Enhancement E1 shall complete within 500 ms and shall be implemented using CSS `transition` or `@keyframes`. JavaScript-driven animation (e.g. `setInterval` pixel-stepping) shall not be used.

**NFR-08** — Filter and search operations (FR-17 through FR-20) shall execute entirely on the client side using the in-memory task list. No additional HTTP requests shall be issued as a result of filter tab changes or search input events.

**NFR-09** — The priority `<select>` dropdown and the filter bar shall be visually consistent with the rest of the UI in terms of font, spacing, and colour palette.

**NFR-10** — The enhanced UI shall remain mobile-friendly (375 px minimum viewport width, per NFR-03 in `requirements.md`). The priority dropdown, filter tabs, and search input shall not introduce horizontal scrolling on small viewports.

---

## Constraints

All constraints from `requirements.md` remain in force, specifically:

- Java 17+, Spring Boot 3.x, Maven
- Plain HTML5, CSS3, vanilla JavaScript (ES6+) — no JS frameworks or build pipelines
- In-memory storage only — no database, no `localStorage`
- Server port 8080, no TLS, no authentication
- Static assets served by Spring Boot from `src/main/resources/static/`

Additional constraints for this enhancement:

- **No breaking API changes:** Existing clients that do not send `priority` in POST requests must continue to work (server defaults to MEDIUM).
- **No new Maven dependencies** may be introduced unless approved by the human. The priority field validation and filter/search logic shall use standard Java and vanilla JS respectively.
- **Priority changes after creation are out of scope** — the `PUT /api/todos/{id}` endpoint shall not be extended to accept or modify priority.

---

## Assumptions

Assumptions 1–7 from `requirements.md` are carried forward unchanged.

Additional assumptions for v2:

8. Priority values are case-insensitive on input but shall be normalised to uppercase before storage (`HIGH`, `MEDIUM`, `LOW`). Existing tests that do not supply priority will receive `MEDIUM` from the server.
9. The colour-coded priority badges may use CSS classes rather than inline styles. Exact hex colour values are not mandated by this document; the implementation shall choose accessible, visually distinct colours.
10. The fade-in animation applies only when a task is newly added to the DOM (i.e. via a successful POST), not when the page initially loads existing tasks from GET /api/todos.
11. Live search performs a client-side substring match against the task `text` field only; it does not search by priority.
12. The counter label format "X of Y done" from FR-08 / AC-06 in `requirements.md` shall be updated: when a filter or search is active, `Y` shall reflect the count of visible items and an optional indication that filters are active is acceptable but not required.
13. The empty-state element (FR-25) is determined by the total task count returned from the server, not the filtered visible count. If tasks exist but all are hidden by the current filter, a "no matching tasks" state may be shown instead; this is a UI detail for the implementation stage.

---

## Dependencies

All dependencies from `requirements.md` remain in force.

No new Maven/npm dependencies are introduced by this enhancement. All new behaviour is implemented using:

- Existing Spring Boot Starter Web (Jackson handles the new `priority` field automatically via standard serialization)
- Native CSS3 (`@keyframes`, `transition`, pseudo-classes `:hover`)
- Vanilla JavaScript ES6+ (`Array.filter`, `String.includes`, DOM event listeners)

---

## Edge Cases

Edge cases EC-01 through EC-06 from `requirements.md` are carried forward.

**EC-07** — **Invalid priority value in POST:** A client sends `{ "text": "task", "priority": "URGENT" }`. Expected: the server validates the value against the allowed set (HIGH, MEDIUM, LOW) and returns HTTP 400 with a JSON error body. The frontend shall display an appropriate error message.

**EC-08** — **Priority omitted in POST:** A client sends `{ "text": "task" }` with no priority field. Expected: the server creates the task with `priority: "MEDIUM"` and returns it in the 201 response. The frontend shall display the Medium badge on the new task row.

**EC-09** — **Filter tab active when new task added:** The user is on the "Completed" tab and adds a new task. Expected: per FR-21, the filter resets to "All" and the new task is visible in the list.

**EC-10** — **Search yields no results:** The user types a string that matches no tasks under the current filter. Expected: the task list area displays a "no matching tasks" message (distinct from the global empty-state in FR-25); the counter shows "0 of 0 done" (or equivalent for visible count).

**EC-11** — **Task added while search is active:** The user has typed a search string, then adds a new task. Expected: per FR-21, both the filter tab (reset to All) and the search input (cleared) are reset, and all tasks including the new one are shown.

**EC-12** — **Add button double-click:** The user double-clicks the Add button before the first request completes. Expected: per FR-26, the button is disabled after the first click; the second click has no effect. Only one POST is sent.

---

## Security Considerations

Security considerations SC-01 through SC-05 from `requirements.md` are carried forward.

**SC-06** — **Priority field input validation:** The `priority` field in the POST body must be validated server-side against the allowed enum values. It must not be passed to any downstream system or rendered as HTML without sanitisation. Because the priority badge in the frontend is rendered using a predefined CSS class (not by inserting the raw server string into innerHTML), stored XSS risk via this field is low; the implementation shall enforce this pattern.

**SC-07** — **Search input XSS:** The live search input value is used only for client-side `String.includes` comparisons and is never sent to the server or injected into the DOM as HTML. The implementation shall enforce text-node–only rendering for task text (consistent with SC-01 in `requirements.md`).

---

## Acceptance Criteria

The following acceptance criteria are specific to v2. All 10 original ACs (AC-01 through AC-10 in `requirements.md`) remain in force and are collectively referenced as AC-E09 below.

**AC-E01** — Given a task exists in the list, when the task row is rendered, then a priority badge (colour-coded per FR-16) displaying "High", "Medium", or "Low" shall be visible on the row.

**AC-E02** — Given the add form is displayed, when the user opens the priority dropdown and selects no value (or the page loads fresh), then the dropdown defaults to "Medium". When the user selects "High" and submits a valid task text, the new task row shows a High badge.

**AC-E03** — Given the server is running, when a POST to `/api/todos` is made with `{ "text": "task", "priority": "INVALID" }`, then the server returns HTTP 400 with a JSON error body. When a POST is made with `{ "text": "task" }` (priority omitted), then the server returns HTTP 201 and the response body includes `"priority": "MEDIUM"`.

**AC-E04** — Given at least one task exists, when a GET to `/api/todos` is made, then every object in the returned JSON array includes a `priority` field with a value of `"HIGH"`, `"MEDIUM"`, or `"LOW"`.

**AC-E05** — Given the task list contains a mix of active and completed tasks, when the user clicks the "Active" filter tab, then only tasks with `done: false` are displayed and the counter reflects the count of active visible tasks. When the user clicks "Completed", only tasks with `done: true` are shown.

**AC-E06** — Given the "All" filter tab is active and multiple tasks exist, when the user types a substring into the search input, then only tasks whose text contains that substring (case-insensitive) are displayed. When a filter tab is also active (e.g. "Active"), only tasks matching both the tab condition and the search text are shown.

**AC-E07** — Given the add form is displayed, when the user submits a valid new task, then the new task row appears in the list with a smooth fade-in animation visible to the eye.

**AC-E08** — Given the add form is displayed, when the user clicks the Add button, then the button is immediately disabled and displays a loading indicator, and it returns to its normal enabled state only after the server responds (whether success or error).

**AC-E09** — All 10 original acceptance criteria (AC-01 through AC-10) defined in `requirements.md` continue to pass without modification.

**AC-E10** — Given a clean checkout, when the full test suite is run via `mvn test`, then all unit tests and controller tests covering the new priority field validation, default-value behaviour, and filter/search logic pass with no failures.
