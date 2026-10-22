## Status
Approved

## Last Updated
2026-10-22

## Source Story
user-story.md — Story ID: TODO-001, Title: Simple Todo List with Java Backend

---

## Functional Requirements

**FR-01** — The system shall serve the static todo list HTML page at `http://localhost:8080` via Spring Boot's embedded web server (static resource from `src/main/resources/static/index.html`).

**FR-02** — The system shall expose a `GET /api/todos` endpoint that returns the current list of all todo items as a JSON array. Each item shall include the fields: `id` (integer), `text` (string), and `done` (boolean).

**FR-03** — The system shall expose a `POST /api/todos` endpoint that accepts a JSON request body containing a `text` field and creates a new todo item with a generated unique integer `id` and `done` set to `false`. The endpoint shall return the newly created todo item as JSON with an HTTP 201 status code.

**FR-04** — The system shall expose a `PUT /api/todos/{id}` endpoint that toggles the `done` boolean field of the todo item identified by `{id}`. The endpoint shall return the updated todo item as JSON with an HTTP 200 status code.

**FR-05** — The system shall expose a `DELETE /api/todos/{id}` endpoint that removes the todo item identified by `{id}` from storage. The endpoint shall return HTTP 204 (No Content) on success.

**FR-06** — The system shall store all todo items in an in-memory Java `List` within the Spring Boot application. No database or external persistence layer shall be used. All todo data shall be lost and the list reset to empty when the server process is restarted; this behavior is by design.

**FR-07** — The system shall reject a `POST /api/todos` request where the `text` field is absent, null, or consists entirely of whitespace. The endpoint shall return HTTP 400 (Bad Request) with a JSON error body. The frontend shall display a visible validation message to the user without submitting the request.

**FR-08** — The frontend shall display a counter showing the number of completed tasks relative to the total number of tasks, in the format "X of Y done" (e.g. "3 of 5 done"). The counter shall update whenever a task is added, toggled, or deleted without a full page reload.

**FR-09** — The frontend shall apply a CSS strike-through style to the text of any todo item whose `done` field is `true`. The strike-through shall be applied or removed in real time when the user toggles the Done checkbox, without a full page reload.

**FR-10** — The frontend shall perform all data operations (load, add, toggle, delete) via the browser Fetch API calls to the REST endpoints. No operation shall trigger a full page reload (i.e., no HTML form submissions that navigate the browser).

**FR-11** — The frontend shall load and display all existing todo items (by calling `GET /api/todos`) when the page is first loaded or refreshed. Tasks stored on the server shall therefore persist across browser page refreshes (as long as the server is running).

---

## Non-Functional Requirements

**NFR-01** — The REST API shall respond to all requests within 200 ms under normal single-user development load.

**NFR-02** — The application shall start up cleanly via `mvn spring-boot:run` with zero errors or exceptions in the console log.

**NFR-03** — The frontend UI shall be mobile-friendly, rendering correctly and remaining usable on viewport widths down to 375 px (small smartphone screen) without horizontal scrolling.

**NFR-04** — The UI shall present a clean, readable visual design: adequate contrast between text and background, sufficient spacing between list items, and clear differentiation of interactive controls (input field, Add button, Done checkbox, Delete button).

**NFR-05** — The application shall not require any authentication or session management. All API endpoints are publicly accessible within the local network context.

**NFR-06** — The Maven build shall complete without downloading snapshot dependencies outside the declared `pom.xml`, and shall not require any global Maven settings beyond a standard JDK 17+ and Maven 3.6+ installation.

---

## Constraints

- **Language / Runtime:** Java 17 or higher (LTS release).
- **Framework:** Spring Boot 3.x (3.2 or later recommended).
- **Build tool:** Maven (`pom.xml`). The application is started with `mvn spring-boot:run`.
- **Frontend:** Plain HTML5, CSS3, and vanilla JavaScript (ES6+). No JavaScript frameworks, component libraries, or build pipelines (no React, Vue, Angular, Webpack, Vite, etc.).
- **Storage:** In-memory only. No relational database, no NoSQL store, no file-system persistence, no browser `localStorage` usage for persistence.
- **Server port:** 8080 (Spring Boot default). No TLS/HTTPS required.
- **Authentication:** None. No login, sessions, JWT, or OAuth.
- **Static assets:** Served directly by Spring Boot from `src/main/resources/static/`. No separate static file server.
- **Out of scope:** Due dates, priorities, multiple lists, categories, multi-user support.

---

## Assumptions

1. The application is intended for local development and demonstration use only; production hardening (rate limiting, HTTPS, CORS policies for remote origins) is out of scope.
2. Only a single user will access the app at a time; thread-safety of the in-memory list is not a hard requirement, though a `synchronized` list or `CopyOnWriteArrayList` is acceptable.
3. Todo `id` values are generated as auto-incrementing integers starting from 1; they are never reused after deletion within a single server session.
4. The `PUT /api/todos/{id}` endpoint always toggles `done` (flips the current boolean value) rather than accepting a desired state in the request body.
5. If `GET /api/todos/{id}` or `DELETE /api/todos/{id}` is called with an `id` that does not exist, the server returns HTTP 404.
6. The Maven wrapper (`mvnw`) may be provided for convenience but is not strictly required; a system Maven installation is sufficient.
7. The developer environment has an internet connection for the initial Maven dependency download.

---

## Dependencies

- **Spring Boot Starter Web** (`spring-boot-starter-web`) — provides embedded Tomcat, Spring MVC, and Jackson JSON serialization.
- **JDK 17+** — compile-time and runtime Java.
- **Maven 3.6+** — build and dependency management.
- **Browser with Fetch API support** — any modern browser (Chrome 42+, Firefox 40+, Safari 10.1+, Edge 14+).
- No other external services, databases, or third-party APIs are required.

---

## Edge Cases

**EC-01** — **Empty text submission:** The user clicks Add without typing anything, or types only spaces. Expected: the frontend blocks the request and shows a validation message; no POST is sent to the server. If a POST with empty/whitespace text reaches the server, it returns HTTP 400.

**EC-02** — **Delete a non-existent todo:** A DELETE or PUT request is made for an `id` that has already been deleted or never existed. Expected: the server returns HTTP 404. The frontend should handle this gracefully (e.g., silently refresh the list).

**EC-03** — **Server restart during browser session:** The user has tasks displayed in the browser, then the server restarts. On the next fetch operation, the server returns an empty list or a 404. Expected: the frontend reflects the current server state (empty list) after the next operation or page refresh.

**EC-04** — **Very long task text:** A user inputs an extremely long string (e.g. 10,000 characters). Expected: the app does not crash; the text is stored and displayed, potentially with CSS overflow handling (text wrapping or truncation).

**EC-05** — **Rapid successive actions:** The user clicks Done and Delete on the same item in quick succession before the first response returns. Expected: only one operation should win; the second should return 404 and the frontend should handle it without showing an unhandled error.

**EC-06** — **Concurrent todo IDs:** Since storage is in-memory and single-threaded in typical use, ID collisions are not expected. However, if concurrent requests are made, the ID generation must remain unique.

---

## Security Considerations

**SC-01** — **Input validation:** Task text submitted via `POST /api/todos` must be validated server-side to reject blank input. No server-side HTML escaping is strictly required for an API-only backend, but the frontend must render task text as a DOM text node (not `innerHTML`) to prevent stored XSS.

**SC-02** — **No authentication required:** The application is explicitly out-of-scope for authentication. This is acceptable only for local development use on a trusted network.

**SC-03** — **No sensitive data:** Todo item text is user-supplied plain text. No credentials, PII, or secrets should be stored in the todo list. No special data-handling policy applies.

**SC-04** — **CORS:** No cross-origin access is required. The frontend is served by the same Spring Boot server that hosts the API, so no CORS configuration is needed.

**SC-05** — **Dependency security:** The `pom.xml` shall declare only well-known Spring Boot dependencies. No third-party dependencies with known CVEs should be introduced.

---

## Acceptance Criteria

**AC-01** — Given the server is running via `mvn spring-boot:run`, when a user navigates to `http://localhost:8080` in a browser, then the todo list HTML page is displayed with an input field, an Add button, an empty task list, and a counter showing "0 of 0 done".

**AC-02** — Given the todo list page is open, when the user types a non-empty task text and clicks Add, then the frontend sends a POST to `/api/todos`, the new task appears in the list immediately without a full page reload, and the counter updates accordingly.

**AC-03** — Given a task has been added, when the page is reloaded (browser refresh), then the task is still visible in the list (because it was stored on the server, not in the browser).

**AC-04** — Given the todo list page is open, when the user clicks the Done checkbox on an existing task, then the frontend sends a PUT to `/api/todos/{id}`, the task text is displayed with a strike-through style, and the "X of Y done" counter increments — all without a full page reload.

**AC-05** — Given the todo list page is open, when the user clicks the Delete button on an existing task, then the frontend sends a DELETE to `/api/todos/{id}`, the task is removed from the displayed list, and the counter updates — all without a full page reload.

**AC-06** — Given the todo list has tasks in mixed done/undone states, when any add, toggle, or delete action completes, then the counter displayed on the page correctly reflects the current count in the format "X of Y done".

**AC-07** — Given the todo list page is open, when the user clicks Add with an empty input field (or whitespace only), then no network request is sent, and a validation message is displayed to the user on the page.

**AC-08** — Given a clean checkout of the project, when the developer runs `mvn spring-boot:run`, then the application starts with no errors or exceptions and is accessible at `http://localhost:8080` within 30 seconds.

**AC-09** — Given the todo list page is displayed, when it is viewed on a device with a 375 px wide viewport, then all elements (input field, Add button, task list, counter) are visible and usable without horizontal scrolling and without overlapping content.

**AC-10** — Given the in-memory storage design, when the server process is stopped and restarted, then all previously added todos are gone and the list starts empty — confirming that persistence is server-session-scoped, not database-backed.
