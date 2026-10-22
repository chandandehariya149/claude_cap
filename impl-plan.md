## Status
Draft

## Last Updated
2026-10-22

## Overview
This plan implements a single-page Todo List web application using Java 17, Spring Boot 3.x, and plain HTML/CSS/vanilla JavaScript. The backend exposes four REST endpoints backed by a thread-safe in-memory `CopyOnWriteArrayList<Todo>` with `AtomicLong` ID generation; the frontend is one static HTML file with inline CSS and JavaScript that calls those endpoints via the Fetch API. Everything deploys as a single Spring Boot JAR — no database, no frontend build pipeline, no external dependencies beyond `spring-boot-starter-web` and `spring-boot-starter-test`. The build tool is Maven (`pom.xml`). The application is started with `mvn spring-boot:run` and served at `http://localhost:8080`.

---

## Task List

### Task 1 — pom.xml (Maven project descriptor)
- **Description:** Create the Maven project descriptor at the project root. Set groupId to `com.example`, artifactId to `todo-app`, version to `1.0.0`, and packaging to `jar`. Set the Java source/target version to 17 via `<java.version>17</java.version>` in properties. Declare the Spring Boot parent POM: `org.springframework.boot:spring-boot-starter-parent:3.2.x` (or the latest 3.x release). Add two `<dependency>` entries: `org.springframework.boot:spring-boot-starter-web` (compile scope) and `org.springframework.boot:spring-boot-starter-test` (test scope). Add the `spring-boot-maven-plugin` in the `<build><plugins>` section. No other dependencies.
- **Files:** `pom.xml`
- **Depends on:** None
- **Acceptance condition:** `mvn dependency:resolve` completes without errors; `mvn compile` succeeds on an empty source tree.
- **Verification method:** Run `mvn dependency:resolve -q` — zero errors, both dependencies listed in the resolved set.

---

### Task 2 — TodoApplication.java (Spring Boot entry point)
- **Description:** Create the class `com.example.todo.TodoApplication` in `src/main/java/com/example/todo/TodoApplication.java`. Annotate the class with `@SpringBootApplication`. Add a `public static void main(String[] args)` method that calls `SpringApplication.run(TodoApplication.class, args)`. No other logic.
- **Files:** `src/main/java/com/example/todo/TodoApplication.java`
- **Depends on:** Task 1
- **Acceptance condition:** `mvn spring-boot:run` starts the application with no errors and serves requests on port 8080.
- **Verification method:** Run `mvn spring-boot:run`; confirm console shows "Started TodoApplication" and "Tomcat started on port 8080"; send `curl -s http://localhost:8080/actuator` or any GET and receive a response (not a connection refused).

---

### Task 3 — Todo.java (data model)
- **Description:** Create the class `com.example.todo.Todo` in `src/main/java/com/example/todo/Todo.java`. The class is a plain mutable POJO (not a Java record). Declare three private fields: `private Long id`, `private String text`, `private boolean done`. Provide a no-argument constructor. Provide an all-argument constructor `public Todo(Long id, String text, boolean done)`. Provide public getters and setters for all three fields: `getId()`, `setId(Long id)`, `getText()`, `setText(String text)`, `isDone()`, `setDone(boolean done)`. No annotations, no external library, no `@Entity`, no `@JsonProperty` unless Jackson requires it (it does not with standard getter/setter naming conventions).
- **Files:** `src/main/java/com/example/todo/Todo.java`
- **Depends on:** Task 1
- **Acceptance condition:** Class compiles; Jackson (via Spring Boot) can serialize and deserialize a `Todo` instance to/from `{"id":1,"text":"Buy groceries","done":false}` automatically.
- **Verification method:** `mvn compile` succeeds with no errors. Serialization is verified implicitly by the controller integration tests in Task 8.

---

### Task 4 — TodoService.java (business logic + in-memory store)
- **Description:** Create the class `com.example.todo.TodoService` in `src/main/java/com/example/todo/TodoService.java`. Annotate the class with `@Service`. Declare two private final fields:
  - `private final CopyOnWriteArrayList<Todo> todos = new CopyOnWriteArrayList<>()`
  - `private final AtomicLong counter = new AtomicLong()`

  Implement four public methods:

  1. `public List<Todo> getAll()` — returns `new ArrayList<>(todos)` (an unmodifiable snapshot). Import: `java.util.ArrayList`.

  2. `public Todo add(String text)` — validate: if `text == null` throw `IllegalArgumentException("Text must not be null")`; if `text.isBlank()` throw `IllegalArgumentException("Text must not be blank")`; if `text.length() > 500` throw `IllegalArgumentException("Text must not exceed 500 characters")`. If valid: create `Todo todo = new Todo(counter.incrementAndGet(), text, false)`, call `todos.add(todo)`, return `todo`.

  3. `public Todo toggle(Long id)` — iterate `todos` to find a `Todo` where `todo.getId().equals(id)`; if not found throw `NoSuchElementException("Todo not found: " + id)`; if found call `todo.setDone(!todo.isDone())` and return the same `todo` reference.

  4. `public void delete(Long id)` — iterate `todos` to find the matching `Todo`; if not found throw `NoSuchElementException("Todo not found: " + id)`; if found call `todos.remove(todo)`.

  Imports needed: `java.util.List`, `java.util.ArrayList`, `java.util.NoSuchElementException`, `java.util.concurrent.CopyOnWriteArrayList`, `java.util.concurrent.atomic.AtomicLong`, `org.springframework.stereotype.Service`.
- **Files:** `src/main/java/com/example/todo/TodoService.java`
- **Depends on:** Task 3
- **Acceptance condition:** All four methods behave as described; validation exceptions are thrown for invalid input; `NoSuchElementException` is thrown for unknown IDs; the list is mutated correctly by add/toggle/delete.
- **Verification method:** Unit tests in Task 7 (`TodoServiceTest.java`) provide full coverage. `mvn test -pl . -Dtest=TodoServiceTest` must pass with zero failures.

---

### Task 5 — TodoController.java (REST API layer)
- **Description:** Create the class `com.example.todo.TodoController` in `src/main/java/com/example/todo/TodoController.java`. Annotate the class with `@RestController` and `@RequestMapping("/api/todos")`. Inject `TodoService` via constructor injection: `private final TodoService service; public TodoController(TodoService service) { this.service = service; }`.

  Implement four endpoint methods, each mapping to a specific HTTP method and path:

  1. `@GetMapping` method `getAllTodos()` — calls `service.getAll()`, returns `ResponseEntity.ok(list)` (HTTP 200).

  2. `@PostMapping` method `addTodo(@RequestBody Map<String,String> body)` — extracts `body.get("text")`, calls `service.add(text)` inside a `try` block. On success returns `ResponseEntity.status(HttpStatus.CREATED).body(todo)` (HTTP 201). Catches `IllegalArgumentException e` and returns `ResponseEntity.badRequest().body(Map.of("error", e.getMessage()))` (HTTP 400).

  3. `@PutMapping("/{id}")` method `toggleTodo(@PathVariable Long id)` — calls `service.toggle(id)` inside a `try` block. On success returns `ResponseEntity.ok(todo)` (HTTP 200). Catches `NoSuchElementException e` and returns `ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()))` (HTTP 404).

  4. `@DeleteMapping("/{id}")` method `deleteTodo(@PathVariable Long id)` — calls `service.delete(id)` inside a `try` block. On success returns `ResponseEntity.noContent().build()` (HTTP 204). Catches `NoSuchElementException e` and returns `ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()))` (HTTP 404).

  Imports needed: `java.util.List`, `java.util.Map`, `java.util.NoSuchElementException`, `org.springframework.http.HttpStatus`, `org.springframework.http.ResponseEntity`, `org.springframework.web.bind.annotation.*`.
- **Files:** `src/main/java/com/example/todo/TodoController.java`
- **Depends on:** Task 4
- **Acceptance condition:** All four endpoints respond with the correct HTTP status codes, JSON bodies, and error bodies as specified in the API contract.
- **Verification method:** Controller tests in Task 8 (`TodoControllerTest.java`) verify all status codes and response bodies. `mvn test -Dtest=TodoControllerTest` must pass.

---

### Task 6 — index.html (single-page frontend)
- **Description:** Create `src/main/resources/static/index.html`. The file contains a complete HTML5 document with inline `<style>` and `<script>` sections. No external resources.

  **HTML structure:**
  - `<h1>My Todo List</h1>`
  - `<div id="counter">0 of 0 done</div>`
  - `<div id="error-banner" style="display:none"></div>` — error banner, hidden by default
  - `<form id="add-form">` containing `<input id="todo-input" type="text" maxlength="500" placeholder="New todo…" required>` and `<button type="submit">Add</button>`
  - `<ul id="todo-list"></ul>`

  **CSS (inline in `<style>`):**
  - CSS reset: `* { box-sizing: border-box; margin: 0; padding: 0; }`
  - Body: background color, centered content, readable font
  - Card container: max-width ~600px, centered with margin auto, padding
  - Input: width stretch, padding, border, border-radius
  - Button: padding, background color, text color, cursor pointer, border-radius
  - `li` layout: flexbox row with checkbox on left, text in middle (flex-grow), delete button on right; padding between items
  - `.done` class: `text-decoration: line-through; opacity: 0.6`
  - Error banner: red/pink background, padding, border-radius, margin-bottom
  - Mobile breakpoint at 480px: stack form elements vertically (input full width, button full width)

  **JavaScript (inline in `<script>`):**
  - On `DOMContentLoaded`: call `loadTodos()`
  - `async function loadTodos()` — fetch `GET /api/todos`; on success call `renderTodos(todos)`; on network error call `showError("Network error — please try again")`
  - `function renderTodos(todos)` — clears `#todo-list`; for each todo calls `createTodoElement(todo)` and appends it; calls `updateCounter(todos)`. Uses `createElement` and `textContent` exclusively — no `innerHTML` anywhere.
  - `function createTodoElement(todo)` — creates an `<li>`; creates a checkbox `<input type="checkbox">` with `checked = todo.done`; adds change listener that calls `toggleTodo(todo.id)`; creates a `<span>` with `textContent = todo.text` and class `done` if `todo.done`; creates a `<button>Delete</button>` with click listener that calls `deleteTodo(todo.id)`; returns the assembled `<li>`.
  - `async function addTodo()` — triggered by form submit event (prevent default); reads `#todo-input` value; if blank shows "Task text must not be blank" via `showError` and returns without fetching; sends `POST /api/todos` with `Content-Type: application/json` and body `{"text": value}`; on 201 clears input, calls `loadTodos()`; on 400 reads error JSON and calls `showError(data.error)`; on network error calls `showError("Network error — please try again")`.
  - `async function toggleTodo(id)` — sends `PUT /api/todos/${id}`; on 200 calls `loadTodos()`; on 404 calls `loadTodos()` (silently refreshes); on network error calls `showError("Network error — please try again")`.
  - `async function deleteTodo(id)` — sends `DELETE /api/todos/${id}`; on 204 calls `loadTodos()`; on 404 calls `loadTodos()` (silently refreshes); on network error calls `showError("Network error — please try again")`.
  - `function updateCounter(todos)` — counts done items; sets `#counter` textContent to `"${doneCount} of ${todos.length} done"`.
  - `function showError(msg)` — sets `#error-banner` textContent to `msg`, sets display to block, sets a `setTimeout` of 5000 ms to hide it again by setting display to none.
- **Files:** `src/main/resources/static/index.html`
- **Depends on:** Task 5
- **Acceptance condition:** Page loads at `http://localhost:8080`; all 10 acceptance criteria pass on manual walkthrough (see AC Checklist below).
- **Verification method:** Manual browser test per AC-01 through AC-10. No automated frontend tests are in scope.

---

### Task 7 — TodoServiceTest.java (unit tests for business logic)
- **Description:** Create the JUnit 5 test class `com.example.todo.TodoServiceTest` in `src/test/java/com/example/todo/TodoServiceTest.java`. This is a plain unit test — no Spring context, no `@SpringBootTest`. Annotate the class with nothing beyond `class TodoServiceTest`. Instantiate `TodoService` directly in a `@BeforeEach` method: `service = new TodoService()`.

  Write the following 9 test methods:

  1. `addTodo_success()` — call `service.add("Buy milk")`; assert returned `Todo` has `id == 1`, `text == "Buy milk"`, `done == false`; assert `service.getAll().size() == 1`.

  2. `addTodo_emptyText_throws()` — `assertThrows(IllegalArgumentException.class, () -> service.add(""))`.

  3. `addTodo_blankText_throws()` — `assertThrows(IllegalArgumentException.class, () -> service.add("   "))`.

  4. `addTodo_tooLongText_throws()` — create a string of 501 characters; `assertThrows(IllegalArgumentException.class, () -> service.add(longString))`.

  5. `toggleTodo_flipsDone()` — `service.add("Task")` returns todo with `done=false`; call `service.toggle(todo.getId())`; assert returned todo has `done=true`; call `service.toggle(todo.getId())` again; assert `done=false`.

  6. `toggleTodo_notFound_throws()` — `assertThrows(NoSuchElementException.class, () -> service.toggle(999L))`.

  7. `deleteTodo_success()` — add two todos; delete the first by id; assert `service.getAll().size() == 1`; assert the remaining todo has the second id.

  8. `deleteTodo_notFound_throws()` — `assertThrows(NoSuchElementException.class, () -> service.delete(999L))`.

  9. `getAll_returnsAllTodos()` — add three todos; assert `service.getAll().size() == 3`; assert ids are 1, 2, 3 respectively.

  Imports: `org.junit.jupiter.api.BeforeEach`, `org.junit.jupiter.api.Test`, `static org.junit.jupiter.api.Assertions.*`, `java.util.NoSuchElementException`.
- **Files:** `src/test/java/com/example/todo/TodoServiceTest.java`
- **Depends on:** Task 4
- **Acceptance condition:** `mvn test -Dtest=TodoServiceTest` runs 9 tests, all pass, zero failures or errors.
- **Verification method:** `mvn test -Dtest=TodoServiceTest` — output shows `Tests run: 9, Failures: 0, Errors: 0`.

---

### Task 8 — TodoControllerTest.java (Spring MVC slice tests)
- **Description:** Create the JUnit 5 + Spring Boot test class `com.example.todo.TodoControllerTest` in `src/test/java/com/example/todo/TodoControllerTest.java`. Annotate with `@WebMvcTest(TodoController.class)`. Inject `MockMvc mockMvc` with `@Autowired`. Declare `@MockBean TodoService service`.

  Write the following 7 test methods. All use `mockMvc.perform(...).andExpect(...)`.

  1. `getAll_returns200WithJsonArray()` — stub `service.getAll()` to return `List.of(new Todo(1L, "Test", false))`; perform `GET /api/todos`; expect status 200; expect `$.length()` equals 1; expect `$[0].text` equals `"Test"`.

  2. `addTodo_validRequest_returns201()` — stub `service.add("Buy milk")` to return `new Todo(1L, "Buy milk", false)`; perform `POST /api/todos` with content-type JSON and body `{"text":"Buy milk"}`; expect status 201; expect `$.id` equals 1; expect `$.text` equals `"Buy milk"`.

  3. `addTodo_emptyText_returns400()` — stub `service.add("")` to throw `new IllegalArgumentException("Text must not be blank")`; perform `POST /api/todos` with body `{"text":""}`; expect status 400; expect `$.error` is not empty.

  4. `toggleTodo_validId_returns200()` — stub `service.toggle(1L)` to return `new Todo(1L, "Task", true)`; perform `PUT /api/todos/1`; expect status 200; expect `$.done` equals `true`.

  5. `toggleTodo_notFoundId_returns404()` — stub `service.toggle(99L)` to throw `new NoSuchElementException("Todo not found: 99")`; perform `PUT /api/todos/99`; expect status 404; expect `$.error` is not empty.

  6. `deleteTodo_validId_returns204()` — stub `service.delete(1L)` does nothing (void); perform `DELETE /api/todos/1`; expect status 204.

  7. `deleteTodo_notFoundId_returns404()` — stub `service.delete(99L)` to throw `new NoSuchElementException("Todo not found: 99")`; perform `DELETE /api/todos/99`; expect status 404; expect `$.error` is not empty.

  Imports: `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`, `org.springframework.beans.factory.annotation.Autowired`, `org.springframework.boot.test.mock.mockito.MockBean`, `org.springframework.test.web.servlet.MockMvc`, `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*`, `static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*`, `static org.mockito.Mockito.*`, `org.springframework.http.MediaType`, `java.util.NoSuchElementException`.
- **Files:** `src/test/java/com/example/todo/TodoControllerTest.java`
- **Depends on:** Task 5, Task 7
- **Acceptance condition:** `mvn test -Dtest=TodoControllerTest` runs 7 tests, all pass, zero failures or errors.
- **Verification method:** `mvn test -Dtest=TodoControllerTest` — output shows `Tests run: 7, Failures: 0, Errors: 0`.

---

### Task 9 — Run and verify (end-to-end acceptance)
- **Description:** Run the full test suite and perform a manual browser walkthrough against all 10 acceptance criteria.
  1. Run `mvn test` — all 16 tests (9 service + 7 controller) must pass.
  2. Run `mvn spring-boot:run` and open `http://localhost:8080`.
  3. Walk through each acceptance criterion in the AC Checklist below.
- **Files:** No new files. Potentially annotates test evidence in `code-review.md` at Stage 6.
- **Depends on:** Tasks 1–8
- **Acceptance condition:** `mvn test` exits with BUILD SUCCESS and zero test failures; all 10 ACs pass on manual verification.
- **Verification method:**
  - `mvn test` output: `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`
  - Browser: manually verify AC-01 through AC-10 per the checklist below.

---

## Expected File Structure

```
todo-app/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── todo/
│   │   │               ├── TodoApplication.java
│   │   │               ├── Todo.java
│   │   │               ├── TodoService.java
│   │   │               └── TodoController.java
│   │   └── resources/
│   │       └── static/
│   │           └── index.html
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── todo/
│                       ├── TodoServiceTest.java
│                       └── TodoControllerTest.java
├── requirements.md
├── architecture.md
├── design-review.md
└── impl-plan.md
```

---

## Design Decisions

These decisions are locked from `design-review.md` and must not be changed during implementation without a new design review:

1. **Todo model is a plain mutable POJO** — `Todo.java` uses private fields with public getters and setters. Java records are excluded because immutability conflicts with in-place toggle.
2. **Thread-safe list is `CopyOnWriteArrayList<Todo>`** — `TodoService` declares its store as `new CopyOnWriteArrayList<>()`. `Collections.synchronizedList` is excluded.
3. **Max text length is 500 characters** — `TodoService.add()` throws `IllegalArgumentException` if `text.length() > 500`. Controller maps this to HTTP 400. `index.html` sets `maxlength="500"` on the input.
4. **No `@CrossOrigin` annotation** — frontend is served same-origin; no CORS configuration is added anywhere.
5. **Toggle is always a flip** — `PUT /api/todos/{id}` always inverts `done`, regardless of current state. No request body is accepted. Intentional deviation from REST idempotency; documented.
6. **Frontend network error banner** — message text is "Network error — please try again"; appears above the task list; auto-dismisses after 5 seconds or on next successful response.
7. **DELETE /api/todos/{id} returns 404 for unknown id** — not a silent 200 or 204.
8. **`AtomicLong` for ID generation** — thread-safe; IDs are never reused within a session; counter resets on server restart.
9. **Controller exception mapping is explicit try/catch** — no `@ControllerAdvice` global handler; each controller method catches its own service exceptions.
10. **`TodoControllerTest.java` to be added** — `@WebMvcTest` slice covering all HTTP-layer error mappings (Finding 6 from design review).

---

## Acceptance Criteria Checklist

Manual verification steps after `mvn spring-boot:run`:

- **AC-01** — Navigate to `http://localhost:8080`. Page displays: h1 "My Todo List", input field, Add button, empty task list, counter showing "0 of 0 done".
- **AC-02** — Type a non-empty task and click Add. Task appears in the list immediately without page reload; counter updates.
- **AC-03** — Refresh the browser (F5). The task added in AC-02 is still visible (server-side persistence within session).
- **AC-04** — Click the Done checkbox on a task. Text gets strike-through style; counter increments — no page reload.
- **AC-05** — Click the Delete button on a task. Task disappears from list; counter updates — no page reload.
- **AC-06** — Add several tasks in mixed done/undone states; verify counter always reads "X of Y done" correctly after each operation.
- **AC-07** — Click Add with an empty input field (or whitespace only). No network request is sent; a validation message appears on the page.
- **AC-08** — Stop the server and restart with `mvn spring-boot:run`. Server starts with no console errors and is accessible at `http://localhost:8080` within 30 seconds.
- **AC-09** — Open browser DevTools, set viewport width to 375 px (or use a phone). All elements are visible and usable without horizontal scrolling or overlapping content.
- **AC-10** — Stop and restart the server. Previously added todos are gone; the list starts empty — confirming in-memory only, no persistence.

---

## Prerequisites

- JDK 17 or higher installed and on `PATH` (`java -version` confirms 17+)
- Maven 3.6 or higher installed and on `PATH` (`mvn -version` confirms 3.6+)
- Internet access for initial Maven dependency download from Maven Central
- A modern browser (Chrome, Firefox, Edge, or Safari) for manual AC verification

---

## Technical Risks

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|-----------|
| TR-01 | `@MockBean` is deprecated in Spring Boot 3.4+ in favour of `@MockitoBean` | Low | Low (compile warning, not a failure) | Use whichever annotation the resolved Spring Boot 3.x version supports; check deprecation notes in the starter-test release notes |
| TR-02 | `Map.of("error", msg)` as a `ResponseEntity` body requires Spring/Jackson to serialize it; Jackson must be on the classpath | Low | Low | `spring-boot-starter-web` bundles Jackson; no extra dependency needed |
| TR-03 | `CopyOnWriteArrayList.remove(Object)` uses `equals()`; `Todo` does not override `equals()` | Medium | Medium | Use `todos.removeIf(t -> t.getId().equals(id))` in `TodoService.delete()` instead of `todos.remove(todo)` to avoid reference-equality surprises |
| TR-04 | Spring Boot static resource handler may not serve `index.html` as the root (`/`) if default index page resolution is overridden | Low | Medium | Verify by running the app and navigating to `http://localhost:8080/`; Spring Boot auto-configures `index.html` as the welcome file from `static/` by default |

---

## Out of Scope

The following are explicitly not part of this implementation:

- Due dates, priorities, categories, or multiple lists
- Multi-user support or any form of authentication/authorization
- Database or file-system persistence of any kind
- Frontend framework (React, Vue, Angular) or build pipeline (npm, Webpack, Vite)
- HTTPS / TLS configuration
- CORS configuration
- Rate limiting or any production hardening
- Maven wrapper (`mvnw`) generation
- Docker or containerization
- Deployment to any remote environment
- `GET /api/todos/{id}` single-item retrieval endpoint (not required by any FR)
