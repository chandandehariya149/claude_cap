## Status
Approved

## Last Updated
2026-11-13

## Overview

This plan implements **Todo App v2** (TODO-002), an additive enhancement to the approved v1 Todo List App. Three enhancement sets are layered onto the existing Spring Boot + single-HTML-file application:

- **E1 — UI Polish:** fade-in animation for newly added rows, hover highlight, reduced opacity for completed rows, improved empty-state element, Add button disabled+spinner guard
- **E2 — Task Priority:** `priority` field on `Todo` (HIGH/MEDIUM/LOW), server-side normalisation and validation, priority `<select>` in the add form, colour-coded priority badge on each task row
- **E3 — Filter and Search:** client-side filter tabs (All/Active/Completed), live text-search input, `allTodos` cache replaced wholesale on every server response

No new Maven dependencies, no new REST endpoints, no new Java files, and no new HTML files are introduced. All changes are modifications to the five existing source files and the two existing test files.

**Stack:** Java 17, Spring Boot 3.x, Maven, plain HTML5/CSS3/ES6+ (no JS framework).

---

## Task List

### Task 1 — Todo.java: add priority field and constructor

- **Description:** Add a `private String priority` field, a four-argument constructor `Todo(Long id, String text, boolean done, String priority)`, a getter `getPriority()`, and a setter `setPriority(String priority)`. The existing no-arg constructor and the three-argument constructor `Todo(Long id, String text, boolean done)` must both be retained unchanged — they are required for Jackson deserialisation and existing test backward-compatibility respectively.
- **Files:** `src/main/java/com/example/todo/Todo.java`
- **Depends on:** None
- **Acceptance condition:** `Todo.java` compiles; `new Todo(1L, "text", false, "HIGH").getPriority()` returns `"HIGH"`; `new Todo(1L, "text", false)` still compiles and `getPriority()` returns `null` (no breaking change).
- **Verification method:** `mvn compile` exits zero. All 17 existing tests continue to pass (`mvn test`).

---

### Task 2 — TodoService.java: two-arg add() method with normalise+validate

- **Description:** Add a new public method `add(String text, String priority)` that:
  1. Runs the existing blank/length checks on `text` before touching `priority`.
  2. Normalises: `priority = (priority == null || priority.isBlank()) ? "MEDIUM" : priority.trim().toUpperCase()`
  3. Validates: if the normalised value is not in `Set.of("HIGH", "MEDIUM", "LOW")`, throw `IllegalArgumentException("Invalid priority: must be HIGH, MEDIUM, or LOW")`.
  4. On success, creates `new Todo(counter.incrementAndGet(), text.trim(), false, normalizedPriority)`.

  Retain the existing one-argument overload `add(String text)` unchanged as a backward-compatibility shim delegating to `add(text, null)`. This ensures all 10 existing `TodoServiceTest` call-sites compile and pass without modification. The one-argument overload is not called from production code paths; it exists solely for existing tests.

- **Files:** `src/main/java/com/example/todo/TodoService.java`
- **Depends on:** Task 1 (four-arg `Todo` constructor required)
- **Acceptance condition:** `mvn compile` exits zero. `service.add("task", "HIGH").getPriority()` equals `"HIGH"`. `service.add("task", null).getPriority()` equals `"MEDIUM"`. `service.add("task", "URGENT")` throws `IllegalArgumentException`. All 10 original `TodoServiceTest` tests still pass.
- **Verification method:** `mvn test` — all 17 existing tests pass.

---

### Task 3 — TodoController.java: pass priority to two-arg add()

- **Description:** In the POST handler `create()`, change the single line:
  ```java
  // before
  Todo created = todoService.add(body.get("text"));
  // after
  Todo created = todoService.add(body.get("text"), body.get("priority"));
  ```
  No other changes. `body.get("priority")` returns `null` when the client omits the field, which the service normalises to `"MEDIUM"`. The existing `IllegalArgumentException` catch block already covers the new priority-invalid exception — no additional error handling is required. No other handlers (`GET`, `PUT`, `DELETE`) change.

- **Files:** `src/main/java/com/example/todo/TodoController.java`
- **Depends on:** Task 2 (two-arg `add()` method must exist before the controller calls it)
- **Acceptance condition:** `mvn compile` exits zero. The two existing controller mock stubs (see Task 4) will need updating before `TodoControllerTest` compiles, but `TodoController.java` itself compiles as a standalone change.
- **Verification method:** `mvn compile` exits zero. Full `mvn test` deferred to after Task 4.

---

### Task 4 — TodoControllerTest.java: update 2 existing stubs + add 7 new tests

- **Description:** Two existing test stubs reference the one-argument `todoService.add()` form. Because the controller now calls the two-argument form, these stubs will not match and must be updated. Additionally, seven new controller tests covering priority behaviour are added.

  **Existing stubs to update (lines 38 and 48):**

  ```java
  // create_validText_returns201 — was:
  when(todoService.add("New task")).thenReturn(new Todo(1L, "New task", false));
  // change to:
  when(todoService.add(eq("New task"), any())).thenReturn(new Todo(1L, "New task", false, "MEDIUM"));

  // create_emptyText_returns400 — was:
  when(todoService.add(any())).thenThrow(new IllegalArgumentException("Task text cannot be empty."));
  // change to:
  when(todoService.add(any(), any())).thenThrow(new IllegalArgumentException("Task text cannot be empty."));
  ```

  Add `import static org.mockito.ArgumentMatchers.eq;` if not already present.

  **New tests to add:**

  | Test method | POST body | Expected |
  |---|---|---|
  | `createTodo_withExplicitHighPriority_returns201WithPriority` | `{"text":"task","priority":"HIGH"}` | 201; `$.priority == "HIGH"` |
  | `createTodo_withoutPriority_defaultsToMedium` | `{"text":"task"}` | 201; `$.priority == "MEDIUM"` |
  | `createTodo_withNullPriority_defaultsToMedium` | `{"text":"task","priority":null}` | 201; `$.priority == "MEDIUM"` |
  | `createTodo_withInvalidPriority_returns400` | `{"text":"task","priority":"URGENT"}` | 400; `$.error` exists |
  | `createTodo_withLowercasePriority_normalizesAndReturns201` | `{"text":"task","priority":"high"}` | 201; `$.priority == "HIGH"` |
  | `getAllTodos_includesPriorityField` | Add task; then GET | each item has `$.priority` not null |
  | `toggleTodo_doesNotChangePriority` | Add HIGH task; PUT to toggle; GET | priority still `"HIGH"` after toggle |

  For the new test stubs, use the four-argument `Todo` constructor in `thenReturn(...)` calls so that `priority` is not null in the serialized response (e.g., `new Todo(1L, "task", false, "MEDIUM")`).

- **Files:** `src/test/java/com/example/todo/TodoControllerTest.java`
- **Depends on:** Tasks 1, 2, 3 (four-arg constructor, two-arg service method, and updated controller POST handler)
- **Acceptance condition:** All 7 original controller tests pass; all 7 new controller tests pass. Total controller tests: 14.
- **Verification method:** `mvn test` — 24 tests pass (10 service + 14 controller).

---

### Task 5 — TodoServiceTest.java: add 9 new service tests

- **Description:** Add nine new test methods to `TodoServiceTest`. The existing 10 tests are untouched.

  | Test method | Input to `add()` | Expected |
  |---|---|---|
  | `add_withHighPriority_returnsTodoWithHighPriority` | `add("task", "HIGH")` | `getPriority()` equals `"HIGH"` |
  | `add_withLowercasePriority_normalizesToUppercase` | `add("task", "low")` | `getPriority()` equals `"LOW"` |
  | `add_withMixedCasePriority_normalizesToUppercase` | `add("task", "Medium")` | `getPriority()` equals `"MEDIUM"` |
  | `add_withNullPriority_defaultsToMedium` | `add("task", null)` | `getPriority()` equals `"MEDIUM"` |
  | `add_withBlankPriority_defaultsToMedium` | `add("task", "  ")` | `getPriority()` equals `"MEDIUM"` |
  | `add_withInvalidPriority_throwsIllegalArgumentException` | `add("task", "URGENT")` | throws `IllegalArgumentException` |
  | `getAll_returnsTasksWithPriorityField` | Add `("t1","HIGH")` and `("t2","LOW")`; call `getAll()` | both returned todos have non-null, valid priority values |
  | `add_withLowPriority_returnsTodoWithLowPriority` | `add("task", "LOW")` | `getPriority()` equals `"LOW"` |
  | `add_withWhitespacePaddedPriority_normalizesCorrectly` | `add("task", "  HIGH  ")` | `getPriority()` equals `"HIGH"` (trims before uppercasing) |

  Note: existing one-argument calls such as `service.add("Buy milk")` in the original tests continue to work via the backward-compatible overload added in Task 2 — no changes to those tests.

- **Files:** `src/test/java/com/example/todo/TodoServiceTest.java`
- **Depends on:** Tasks 1, 2 (priority field and two-arg method must exist)
- **Acceptance condition:** All 10 original service tests pass; all 9 new service tests pass. Total service tests: 19.
- **Verification method:** `mvn test` — 33 tests pass in total (19 service + 14 controller).

---

### Task 6 — index.html: backend integration (priority UI + allTodos cache + filterAndRender plumbing)

- **Description:** Extend `index.html` with priority support and introduce the client-side cache. This step wires the backend to the new priority field and sets up the data flow infrastructure that Steps 7 and 8 build upon.

  **HTML changes:**
  - Inside `#add-form`, after `#task-input`, add `<select id="priority-select">` with three `<option>` elements: `value="HIGH"` (High), `value="MEDIUM"` (Medium, `selected`), `value="LOW"` (Low).
  - Rename `<p id="empty-msg">` to `<div id="empty-state">` with a styled message (e.g., a large emoji/icon and styled text "No tasks yet — add one above!"). Keep `display: none` as default.
  - Add `<p id="no-match-msg" style="display:none">` as a sibling of `#todo-list` for the filter-yields-zero-results case. It is hidden by default.

  **CSS additions:**
  - Priority badge base style: `.priority-badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; margin-left: 4px; }`
  - `.badge-high { background: #fed7d7; color: #c53030; }`
  - `.badge-medium { background: #fefcbf; color: #b7791f; }`
  - `.badge-low { background: #c6f6d5; color: #276749; }`
  - `#priority-select { ... }` — matching font/border/radius to `#task-input` for visual consistency (NFR-09).
  - `#add-form button:disabled { opacity: 0.6; cursor: not-allowed; }` (Add button disabled state; full spinner logic in Task 8).
  - Adjust `#empty-state` CSS to be visually distinct (centred text, larger font, icon/emoji).
  - Mobile media query: add `#priority-select` to the column-flex flow on `max-width: 480px`.

  **JS changes — new module-level variables:**
  ```javascript
  let allTodos = [];
  let activeFilter = 'all';
  let newTodoId = null;   // set before renderTodos(); cleared after
  ```

  **JS changes — helper functions:**
  ```javascript
  const PRIORITY_CLASS = { HIGH: 'badge-high', MEDIUM: 'badge-medium', LOW: 'badge-low' };
  const PRIORITY_LABEL = { HIGH: 'High', MEDIUM: 'Medium', LOW: 'Low' };

  function badgeClass(priority) {
    return PRIORITY_CLASS[priority] || 'badge-medium';
  }
  ```

  **JS changes — `renderTodos(todos)`:** Add priority badge rendering to each `<li>`. After creating the `span.todo-text`, create a `<span class="priority-badge badge-{x}">` using `badgeClass()` for the class and `span.textContent = PRIORITY_LABEL[todo.priority] || todo.priority` for the label — never `innerHTML`. Insert the badge span before the delete button. Also apply `.todo-row-new` to the `<li>` whose `todo.id === newTodoId` (then set `newTodoId = null`).

  **JS changes — `updateCounter(todos)`:** No signature change. Already receives the array to count — this function is correct as-is once called with `visibleTodos` by `filterAndRender()`.

  **JS changes — new `filterAndRender()` function (initial form, full three-state added in Task 7):**
  ```javascript
  function filterAndRender() {
    let visibleTodos = allTodos;
    // filter and search applied in Task 7
    renderTodos(visibleTodos);
    updateCounter(visibleTodos);
    // three-state visibility logic added in Task 7
  }
  ```

  **JS changes — `loadTodos()`:** Replace `renderTodos(todos)` with:
  ```javascript
  allTodos = todos;
  filterAndRender();
  ```

  **JS changes — `addTodo(text, priority)`:** Add `priority` parameter; include it in the POST body `JSON.stringify({ text, priority })`; on success, set `newTodoId = createdTodo.id` (from the 201 response body) before calling `loadTodos()`. Full disabled+finally logic added in Task 8; for now keep the existing structure.

  **JS changes — form submit handler:** Read `document.getElementById('priority-select').value` and pass it as the second argument to `addTodo(text, priority)`.

- **Files:** `src/main/resources/static/index.html`
- **Depends on:** Tasks 1, 2, 3 (backend must accept and return `priority` field)
- **Acceptance condition:** A new task added via the UI shows the correct colour-coded priority badge. The `GET /api/todos` response includes `priority` on every item. Priority defaults to Medium when form loads.
- **Verification method:** `mvn spring-boot:run`; open browser at `http://localhost:8080`; add a High priority task; verify the red "High" badge appears. Run `mvn test` — still 33 tests pass.

---

### Task 7 — index.html: filter and search

- **Description:** Add the filter bar HTML, implement the three-state `filterAndRender()` logic, wire up tab clicks and search input events.

  **HTML additions:**
  - Between `#add-form` and `#todo-list`, add:
    ```html
    <div id="filter-bar">
      <div id="filter-tabs">
        <button class="filter-tab active" data-filter="all">All</button>
        <button class="filter-tab" data-filter="active">Active</button>
        <button class="filter-tab" data-filter="completed">Completed</button>
      </div>
      <input type="text" id="search-input" placeholder="Search tasks…" autocomplete="off">
    </div>
    ```

  **CSS additions:**
  - `#filter-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }`
  - `#filter-tabs { display: flex; gap: 4px; }`
  - `.filter-tab { padding: 6px 14px; border: 1px solid #e2e8f0; border-radius: 20px; background: transparent; font-size: 0.85rem; cursor: pointer; transition: background 0.15s, border-color 0.15s; }`
  - `.filter-tab.active { background: #667eea; color: white; border-color: #667eea; }`
  - `#search-input { flex: 1; min-width: 120px; padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 20px; font-size: 0.85rem; outline: none; transition: border-color 0.2s; }`
  - `#search-input:focus { border-color: #667eea; }`
  - `#no-match-msg { text-align: center; color: #a0aec0; font-size: 0.9rem; padding: 24px 0; }`
  - Mobile: add `#filter-bar` to stacked layout on `max-width: 480px`.

  **JS changes — replace `filterAndRender()` with full implementation:**
  ```javascript
  function filterAndRender() {
    const searchText = document.getElementById('search-input').value.toLowerCase();
    let visibleTodos = allTodos;

    // apply tab filter
    if (activeFilter === 'active') {
      visibleTodos = visibleTodos.filter(t => !t.done);
    } else if (activeFilter === 'completed') {
      visibleTodos = visibleTodos.filter(t => t.done);
    }

    // apply text search (case-insensitive substring)
    if (searchText) {
      visibleTodos = visibleTodos.filter(t => t.text.toLowerCase().includes(searchText));
    }

    renderTodos(visibleTodos);
    updateCounter(visibleTodos);

    // three-state visibility
    const emptyState = document.getElementById('empty-state');
    const noMatchMsg = document.getElementById('no-match-msg');
    const todoList  = document.getElementById('todo-list');

    if (allTodos.length === 0) {
      emptyState.style.display = 'block';
      noMatchMsg.style.display = 'none';
      todoList.style.display   = 'none';
    } else if (visibleTodos.length === 0) {
      emptyState.style.display = 'none';
      noMatchMsg.style.display = 'block';
      todoList.style.display   = 'none';
    } else {
      emptyState.style.display = 'none';
      noMatchMsg.style.display = 'none';
      todoList.style.display   = '';
    }
  }
  ```

  **JS changes — `addTodo()` filter/search reset:** After the successful POST response, before calling `loadTodos()`, add:
  ```javascript
  activeFilter = 'all';
  document.querySelectorAll('.filter-tab').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.filter === 'all');
  });
  document.getElementById('search-input').value = '';
  ```

  **JS changes — event listeners:**
  ```javascript
  // filter tab delegation
  document.getElementById('filter-tabs').addEventListener('click', (e) => {
    const btn = e.target.closest('.filter-tab');
    if (!btn) return;
    activeFilter = btn.dataset.filter;
    document.querySelectorAll('.filter-tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    filterAndRender();
  });

  // live search (no server call — NFR-08)
  document.getElementById('search-input').addEventListener('input', filterAndRender);
  ```

  Note: Toggle and delete do NOT reset `activeFilter` or clear `#search-input` — per Design Decision 4, only `addTodo()` resets filter state.

- **Files:** `src/main/resources/static/index.html`
- **Depends on:** Task 6 (`allTodos`, `activeFilter`, `filterAndRender()` plumbing and HTML skeleton must exist)
- **Acceptance condition:** Clicking "Active" shows only incomplete tasks; clicking "Completed" shows only done tasks; typing in search narrows results in real time. No additional HTTP requests on filter/search changes (verify in browser DevTools Network tab). Counter reflects visible count. When all tasks are hidden by filter, `#no-match-msg` shows. When no tasks exist on server, `#empty-state` shows. Filter + search state resets after adding a new task.
- **Verification method:** `mvn spring-boot:run`; manual UI walkthrough for all three tabs, search, combined filter+search, and add-resets-filter. `mvn test` — 33 tests pass (no backend regressions).

---

### Task 8 — index.html: UI polish (animations, hover, opacity, spinner)

- **Description:** Apply all Enhancement E1 visual refinements.

  **CSS additions:**
  - Fade-in animation (targeted, not global):
    ```css
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-6px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    .todo-row-new { animation: fadeIn 0.4s ease; }
    ```
    Important: the base `#todo-list li` selector does NOT carry this animation. Only `.todo-row-new` does (FR-22, Design Decision 9).
  - Hover highlight: `#todo-list li:hover { background: #f7fafc; }`
  - Completed-row muted opacity (applied to entire `<li>`): `.done { opacity: 0.5; }` — supplements the existing `.done .todo-text { text-decoration: line-through; opacity: 0.45; }` rule (FR-24). Adjust existing `.done .todo-text` opacity as needed to avoid double-muting.
  - Add button spinner/loading: add a visually inline spinner via a CSS pseudo-element or a `<span class="btn-spinner">` inside the button. Simplest approach — keep button text as "Add" at all times and use CSS `#add-form button:disabled::after { content: ''; display: inline-block; width: 12px; height: 12px; border: 2px solid rgba(255,255,255,0.4); border-top-color: white; border-radius: 50%; animation: spin 0.6s linear infinite; margin-left: 8px; vertical-align: middle; }` with `@keyframes spin { to { transform: rotate(360deg); } }`.

  **JS changes — `addTodo()` button disabled + finally:**
  ```javascript
  async function addTodo(text, priority) {
    const btn = document.querySelector('#add-form button[type="submit"]');
    btn.disabled = true;
    try {
      const res = await fetch(API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text, priority })
      });
      if (res.status === 400) {
        const data = await res.json();
        showError(data.error || 'Invalid input.');
        return;
      }
      const created = await res.json();
      newTodoId = created.id;
      // reset filter/search state (from Task 7)
      activeFilter = 'all';
      document.querySelectorAll('.filter-tab').forEach(b => {
        b.classList.toggle('active', b.dataset.filter === 'all');
      });
      document.getElementById('search-input').value = '';
      await loadTodos();
    } catch {
      showError('Network error — please try again.');
    } finally {
      btn.disabled = false;   // unconditional restore (FR-26, Design Decision 8)
    }
  }
  ```
  Note: `btn.disabled = false` is in `finally`, not in success/error branches, so it fires even if `loadTodos()` throws or the network fails.

  **JS changes — `renderTodos(todos)`:** This function already adds `.todo-row-new` to the matching `<li>` (introduced in Task 6). No additional change needed here — the `newTodoId` variable set in `addTodo()` controls which row gets the class. After Task 8, the `.todo-row-new` CSS rule causes the fade-in animation to fire on that one row only.

  **Empty-state visual improvement** (complementing Task 6's rename): Give `#empty-state` a visually distinct style — centred layout, a large emoji character (e.g., `✅`) or SVG icon above the message text, slightly larger font. The exact icon/emoji is left to implementation discretion (architecture Open Question 1).

- **Files:** `src/main/resources/static/index.html`
- **Depends on:** Tasks 6, 7 (the HTML structure, `newTodoId` variable, `addTodo()` function skeleton, and `#empty-state` element must exist)
- **Acceptance condition:** A newly added task row fades in smoothly (animation visible for ~0.4 s). Existing rows do NOT animate on toggle or delete. Hovering over a task row shows a subtle background highlight. A completed task row has reduced opacity on the entire row. The Add button is disabled with a spinner while the POST is in-flight and returns to enabled after success or error. The empty-state element is visually distinct (not plain text).
- **Verification method:** `mvn spring-boot:run`; manual visual check — add a task (fade-in), toggle a task (no animation on other rows), hover a row (highlight), complete a task (muted row), click Add without network (spinner visible then button re-enables), delete all tasks (empty-state shows). `mvn test` — still 33 tests pass.

---

## Full Test Checklist

All 33 tests must pass after all eight tasks are complete (`mvn test`).

### TodoServiceTest.java — 19 tests total

**Original 10 (must remain unchanged and passing):**
1. `addTodo_success`
2. `addTodo_emptyText_throws`
3. `addTodo_blankText_throws`
4. `addTodo_nullText_throws`
5. `addTodo_tooLongText_throws`
6. `toggleTodo_flipsDone`
7. `toggleTodo_notFound_throws`
8. `deleteTodo_success`
9. `deleteTodo_notFound_throws`
10. `getAll_returnsAllTodos`

**New 9 (added in Task 5):**
11. `add_withHighPriority_returnsTodoWithHighPriority`
12. `add_withLowercasePriority_normalizesToUppercase`
13. `add_withMixedCasePriority_normalizesToUppercase`
14. `add_withNullPriority_defaultsToMedium`
15. `add_withBlankPriority_defaultsToMedium`
16. `add_withInvalidPriority_throwsIllegalArgumentException`
17. `getAll_returnsTasksWithPriorityField`
18. `add_withLowPriority_returnsTodoWithLowPriority`
19. `add_withWhitespacePaddedPriority_normalizesCorrectly`

### TodoControllerTest.java — 14 tests total

**Original 7 (5 unchanged; 2 stubs updated in Task 4):**
1. `getAll_returns200AndJsonArray` — unchanged
2. `create_validText_returns201` — stub updated to two-arg form in Task 4
3. `create_emptyText_returns400` — stub updated to two-arg form in Task 4
4. `toggle_validId_returns200` — unchanged
5. `toggle_notFound_returns404` — unchanged
6. `delete_validId_returns204` — unchanged
7. `delete_notFound_returns404` — unchanged

**New 7 (added in Task 4):**
8. `createTodo_withExplicitHighPriority_returns201WithPriority`
9. `createTodo_withoutPriority_defaultsToMedium`
10. `createTodo_withNullPriority_defaultsToMedium`
11. `createTodo_withInvalidPriority_returns400`
12. `createTodo_withLowercasePriority_normalizesAndReturns201`
13. `getAllTodos_includesPriorityField`
14. `toggleTodo_doesNotChangePriority`

---

## Expected File Structure

After all eight tasks are complete, the repository contains the following files (modified files marked `[M]`, new files marked `[N]`, unchanged files marked `[-]`):

```
claude_cap/
├── impl-plan-v2.md                                        [N] (this file)
├── requirements-v2.md                                     [-]
├── architecture-v2.md                                     [-]
├── design-review-v2.md                                    [-]
├── requirements.md                                        [-]
├── architecture.md                                        [-]
├── design-review.md                                       [-]
├── impl-plan.md                                           [-]
├── pom.xml                                                [-]
└── src/
    ├── main/
    │   ├── java/com/example/todo/
    │   │   ├── Todo.java                                  [M] priority field + 4-arg constructor
    │   │   ├── TodoService.java                           [M] two-arg add() + one-arg overload
    │   │   ├── TodoController.java                        [M] POST passes priority
    │   │   └── TodoApplication.java                       [-]
    │   └── resources/static/
    │       └── index.html                                 [M] priority UI + filter/search + UI polish
    └── test/
        └── java/com/example/todo/
            ├── TodoServiceTest.java                       [M] 9 new test methods added
            └── TodoControllerTest.java                    [M] 2 stubs updated + 7 new test methods
```

No new Java files, no new HTML files, no new test files, no new Maven dependencies.

---

## Prerequisites

1. **Branch:** Create `feature/todo-app-v2` from `master` before beginning Task 1:
   ```
   git checkout master
   git checkout -b feature/todo-app-v2
   ```
2. **Java 17+** installed and `JAVA_HOME` set.
3. **Maven 3.6+** on `PATH` (or use `./mvnw` wrapper if present).
4. **Baseline passes:** Confirm all 17 existing tests pass on `master` before branching: `mvn test`.
5. **Spring Boot DevTools (optional):** `mvn spring-boot:run` for manual UI verification steps in Tasks 6–8.

---

## Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `Map<String, String>` with numeric `priority` value returns HTTP 500 instead of 400 | Low | Medium | Verify empirically during Task 3: `curl -X POST http://localhost:8080/api/todos -H 'Content-Type: application/json' -d '{"text":"t","priority":123}'`. If 500, add `HttpMessageNotReadableException` handler to `TodoController` (design-review-v2.md Finding 9 advisory). |
| Three-arg `Todo` constructor in existing stubs causes `priority: null` in serialized JSON | Very low | Low | Existing tests only assert on `$.text` or `$.done`, not `$.priority`. The null will appear in JSON but won't break existing assertions. New tests use the four-arg constructor explicitly. |
| CSS `opacity: 0.5` on `.done <li>` double-mutes text (outer 0.5 × inner 0.45 = ~0.22 opacity) | Medium | Low | Remove or raise the `.done .todo-text { opacity: 0.45; }` rule in Task 8 — the row-level `.done { opacity: 0.5; }` already provides sufficient muting together with the strike-through. |
| `@keyframes` fade-in still fires on toggle/delete if `newTodoId` not cleared promptly | Low | Low | Set `newTodoId = null` at the top of `renderTodos()` after applying the class — ensures it fires for exactly one render cycle. |
| Mobile layout breaks with priority dropdown + filter bar | Low | Medium | Test at 375 px viewport; both `#priority-select` and `#filter-bar` must be included in the `max-width: 480px` media query column layout. |

---

## Out of Scope

The following items are explicitly excluded from this implementation, per `requirements-v2.md` constraints and architecture scope boundary:

- Priority changes after task creation (`PUT /api/todos/{id}` extension)
- New REST endpoints for server-side filtering or searching
- URL-based or `localStorage`-persisted filter/search state
- New Maven dependencies
- Database persistence or `localStorage` task storage
- Authentication, TLS, or multi-user support
- Any changes to `TodoApplication.java`
- Priority on tasks added before this enhancement (they receive `MEDIUM` default from the service)
