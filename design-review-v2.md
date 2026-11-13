## Status
Approved

## Last Updated
2026-11-13

## Summary
**Needs revisions before Stage 4 can begin.**

The v2 architecture is broadly sound: the additive-only scope boundary is well-drawn, the client-side filter/search design correctly avoids server round-trips, the security controls for badge rendering are explicit and correct, and the error-handling table is complete. However, two Critical findings and two Major findings must be resolved or locked before the Implementation Plan stage begins.

The two Critical items are (1) a process violation — neither artifact's `## Status` field reads `Approved` despite the pipeline rule requiring it — and (2) a confirmed compilation-breaking gap: the architecture replaces `TodoService.add(String)` with `add(String, String)` but provides no resolution strategy for the 10 existing `TodoServiceTest` call-sites and 2 existing `TodoControllerTest` mock stubs that use the one-argument form.

---

## Findings

### Finding 1 — Artifact Status Not Advanced to Approved
- **Severity:** Critical
- **Area:** Pipeline process / artifact governance
- **Description:** Both `requirements-v2.md` (line 1: `## Status: Draft`) and `architecture-v2.md` (line 1: `## Status: Draft`) carry `Draft` status. The SDLC pipeline rule states "An artifact is not complete until it has been reviewed and approved by the human." `architecture-v2.md` itself acknowledges this in Open Question 3: "This architecture document should not be considered Approved until requirements-v2.md is first advanced to Approved by the human."
- **Evidence:** `requirements-v2.md` line 1–2; `architecture-v2.md` line 1–2 and Open Question 3; `CLAUDE.md` pipeline rules section.
- **Recommendation:** The human must explicitly confirm approval of both documents and both files must have their `## Status` heading changed from `Draft` to `Approved` before Stage 4 begins. This review treats the content as approved for analysis purposes but flags the missing status change as a blocking gate.
- **Resolution:** Open

---

### Finding 2 — `add()` Signature Change Breaks All Existing Tests
- **Severity:** Critical
- **Area:** `TodoService.java`, `TodoServiceTest.java`, `TodoControllerTest.java` / Backward compatibility
- **Description:** The architecture replaces `TodoService.add(String text)` with `add(String text, String priority)` (architecture-v2.md, Components › TodoService.java: "add(String text) → replaced by add(String text, String priority)"). No backward-compatible overload is defined. This creates two distinct breakages:

  **Breakage A — TodoServiceTest (10 call-sites):** Every existing test calls `service.add("text")` with one argument. When the one-argument overload is removed, all 10 tests fail to compile. Affected lines: `TodoServiceTest.java` lines 21, 29, 33, 38, 44, 51, 52, 65, 66, 80, 81, 82.

  **Breakage B — TodoControllerTest (2 mock stubs):** Line 38 has `when(todoService.add("New task")).thenReturn(...)` and line 48 has `when(todoService.add(any())).thenThrow(...)`. Both reference the one-argument form. When the controller calls `todoService.add(body.get("text"), body.get("priority"))` with two arguments, neither mock will match — Mockito will return `null`, the controller will attempt to serialize it, and both tests will fail at runtime (NullPointerException or empty-body assertion failure).

- **Evidence:** `TodoService.java` lines 21–31 (current signature); `TodoServiceTest.java` lines 21, 29, 33, 38, 44, 51–52, 65–66, 80–82; `TodoControllerTest.java` lines 38, 48; `architecture-v2.md` TodoService component section.
- **Recommendation:** Lock the following resolution — it is the only approach that keeps all existing tests green without mass-edits:

  **Service layer:** Add a one-argument overload that delegates:
  ```java
  public Todo add(String text) {
      return add(text, null);  // null normalises to "MEDIUM"
  }
  ```
  This preserves the existing `TodoServiceTest` call-sites exactly as written.

  **Controller layer:** The controller will call the two-argument form. The two existing controller test stubs must be updated to use two-argument matchers:
  ```java
  // was: when(todoService.add("New task")).thenReturn(...)
  when(todoService.add(eq("New task"), any())).thenReturn(new Todo(1L, "New task", false, "MEDIUM"));

  // was: when(todoService.add(any())).thenThrow(...)
  when(todoService.add(any(), any())).thenThrow(new IllegalArgumentException("Task text cannot be empty."));
  ```

  The returned `Todo` objects in controller test stubs must use the new four-argument constructor to avoid a null `priority` field in the serialized response.

- **Resolution:** Open — must be locked before Stage 4

---

### Finding 3 — Fade-in Animation Triggers on Every Re-render, Not Only New Additions
- **Severity:** Major
- **Area:** `index.html` — CSS animation / Enhancement E1 (FR-22)
- **Description:** The architecture specifies the CSS rule `.todo-row { animation: fadeIn 0.4s ease; }` applied globally to all task rows (architecture-v2.md, New/changed CSS rules table). `renderTodos()` rebuilds the entire `<ul>` from scratch on every call — including calls triggered by toggle and delete operations (which call `loadTodos()` → `filterAndRender()` → `renderTodos()`). Because each call creates new DOM elements, the browser re-triggers the CSS animation on every existing row whenever any task is toggled or deleted, not only when a new task is added. This contradicts `requirements-v2.md` Assumption 10: "The fade-in animation applies only when a task is newly added to the DOM (i.e. via a successful POST), not when the page initially loads existing tasks from GET /api/todos."
- **Evidence:** `requirements-v2.md` Assumption 10; `architecture-v2.md` CSS rules table (`.todo-row { animation: fadeIn … }`); `architecture-v2.md` JS table (`loadTodos()` calls `filterAndRender()` on every server response).
- **Recommendation:** Use a targeted approach: do not apply the animation via a universal `.todo-row` rule. Instead, in `renderTodos()`, mark only the row that was most recently added with a transient CSS class (e.g., `.todo-row-new`). Concretely: after the successful POST response, before calling `loadTodos()`, record the new todo's ID; in `renderTodos()`, add `.todo-row-new` only to the `<li>` whose data-id matches the recorded ID, then clear the recorded ID. The `.todo-row-new` class carries the `@keyframes fadeIn` animation; the base `.todo-row` class does not. This ensures toggle and delete re-renders produce no animation on existing rows.
- **Resolution:** Open — architecture-v2.md must add this clarification

---

### Finding 4 — `filterAndRender()` Three-State Visibility Logic Underspecified
- **Severity:** Major
- **Area:** `index.html` — `filterAndRender()` function / Enhancement E3 (FR-25, EC-10)
- **Description:** The architecture introduces two distinct "no items" UI states — `#empty-state` (all tasks absent from server) and `#no-match-msg` (tasks exist but are all hidden by filter/search) — plus the normal task list. Three mutually exclusive display states result:
  1. `allTodos.length === 0` → show `#empty-state`, hide `#todo-list`, hide `#no-match-msg`
  2. `allTodos.length > 0` and `visibleTodos.length === 0` → hide `#empty-state`, hide `#todo-list`, show `#no-match-msg`
  3. `visibleTodos.length > 0` → hide `#empty-state`, show `#todo-list`, hide `#no-match-msg`

  The architecture's description of `filterAndRender()` (JS table: "Applies active filter and search text to `allTodos`, then calls `renderTodos(visibleTodos)` and `updateCounter(visibleTodos)`") does not document this conditional logic. `renderTodos()` only receives `visibleTodos` and therefore cannot determine whether `allTodos` is empty. The implementor must check `allTodos.length` explicitly outside of `renderTodos()`. Without a specified decision tree, this logic is likely to be implemented inconsistently or incompletely.

- **Evidence:** `architecture-v2.md` New HTML elements table (`#empty-state`, `#no-match-msg`); `architecture-v2.md` Error handling table (last two rows); `requirements-v2.md` FR-25, EC-10; `requirements-v2.md` Assumption 13.
- **Recommendation:** Add an explicit pseudocode block to `architecture-v2.md` for `filterAndRender()`:
  ```
  function filterAndRender():
    let visibleTodos = applyFilterAndSearch(allTodos, activeFilter, searchText)
    renderTodos(visibleTodos)
    updateCounter(visibleTodos)
    if (allTodos.length === 0):
      show #empty-state; hide #todo-list; hide #no-match-msg
    else if (visibleTodos.length === 0):
      hide #empty-state; hide #todo-list; show #no-match-msg
    else:
      hide #empty-state; show #todo-list; hide #no-match-msg
  ```
- **Resolution:** Open — architecture-v2.md must document this logic

---

### Finding 5 — Counter Label Format Under Filter Not Locked
- **Severity:** Minor
- **Area:** `index.html` — `updateCounter()` / Enhancement E3 (FR-20)
- **Description:** `architecture-v2.md` Open Question 2 explicitly defers the counter label wording: "the implementation stage may choose either form [with or without '(filtered)' suffix]." FR-20 and Assumption 12 of `requirements-v2.md` allow but do not require the suffix. The data-flow walkthrough section of architecture-v2.md uses "0 of 2 done" without a suffix. Leaving this open creates a risk of inconsistent implementation or later rework.
- **Evidence:** `architecture-v2.md` Open Question 2; `requirements-v2.md` Assumption 12, FR-20; `architecture-v2.md` data-flow section ("0 of 2 done" examples).
- **Recommendation:** Lock the format as **"X of Y done"** where X = done count in `visibleTodos`, Y = total count in `visibleTodos`, with no additional suffix. This is the simplest form consistent with the original FR-08 counter and requires no additional state tracking. Remove Open Question 2 from `architecture-v2.md`.
- **Resolution:** Open

---

### Finding 6 — Missing Service Test: Priority with Surrounding Whitespace
- **Severity:** Minor
- **Area:** `TodoServiceTest.java` — Test coverage (architecture-v2.md test plan)
- **Description:** The normalization rule in architecture-v2.md is `priority.trim().toUpperCase()`. The test plan includes `add_withBlankPriority_defaultsToMedium` for `"  "` (all-spaces input, correctly defaulted to MEDIUM) and `add_withMixedCasePriority_normalizesToUppercase` for `"Medium"`. However, there is no test for a valid value padded with whitespace, e.g. `add("task", "  HIGH  ")`, which should normalize to `"HIGH"`. Without this test, the trim step is exercised only by the blank case (which never reaches `toUpperCase()`). A bug in the trim path for valid padded values would go undetected.
- **Evidence:** `architecture-v2.md` TodoService interfaces section (normalization rule); `architecture-v2.md` test plan (8 service tests listed).
- **Recommendation:** Add test: `add_withWhitespacePaddedPriority_normalizesToUppercase` — calls `add("task", "  high  ")`, asserts `getPriority()` equals `"HIGH"`.
- **Resolution:** Open

---

### Finding 7 — Controller Test Stubs Must Target Two-Argument Form
- **Severity:** Minor
- **Area:** `TodoControllerTest.java` — test compilation / mock matching
- **Description:** This is the controller-side consequence of Finding 2. Even if the one-argument service overload is added (resolving Finding 2 for service tests), the controller POST handler will call `todoService.add(text, priority)` with two arguments. The existing stubs `when(todoService.add("New task")).thenReturn(...)` and `when(todoService.add(any())).thenThrow(...)` target the one-argument form and will not match the actual invocation. Existing tests `create_validText_returns201` and `create_emptyText_returns400` will silently fail. This is documented here separately from Finding 2 for clarity.
- **Evidence:** `TodoControllerTest.java` lines 38, 48; `architecture-v2.md` TodoController component section (new POST handler code).
- **Recommendation:** Covered by Finding 2 recommendation. Restate for the implementation stage: both stubs must be rewritten to target the two-argument form before any new controller tests are added.
- **Resolution:** Open (tracked under Finding 2)

---

### Finding 8 — Button Spinner Reset Scope in `finally` Block Not Fully Specified
- **Severity:** Advisory
- **Area:** `index.html` — `addTodo()` function (FR-26)
- **Description:** The architecture correctly specifies that the Add button is re-enabled in the `finally` block (architecture-v2.md, `addTodo()` description; error-handling table). FR-26 requires the button to "display a spinner or loading indicator" while the POST is in flight. If the implementation changes the button's text to "Adding..." or inserts a spinner element as the visual indicator, the `finally` block must also restore the original label/state — not only the `disabled` attribute. The architecture does not specify the exact restoration steps, leaving a risk that the button is re-enabled but still shows the spinner text.
- **Evidence:** `requirements-v2.md` FR-26, AC-E08; `architecture-v2.md` JS table (`addTodo()` description; error-handling table last row).
- **Recommendation:** Lock in the architecture (or the implementation plan): the `finally` block must restore both `button.disabled = false` AND the button's original textContent/innerHTML. The simplest implementation keeps the button text as "Add" at all times and uses a CSS spinner overlay or a separate `<span>` within the button, so the textContent does not change and only `disabled` needs to be managed.
- **Resolution:** Open (advisory — does not block Stage 4)

---

### Finding 9 — Non-string JSON Values for Priority Field Are Undocumented
- **Severity:** Advisory
- **Area:** `TodoController.java` — POST body deserialization
- **Description:** The controller uses `Map<String, String>` to receive the POST body. If a client sends `{"text":"task","priority":123}` (integer) or `{"text":"task","priority":true}` (boolean), Jackson's behavior when deserializing to `Map<String, String>` is inconsistent across configurations: by default it may coerce the number to `"123"` (which then fails validation and returns 400), or it may throw `HttpMessageNotReadableException` (which may or may not be caught by the existing `IllegalArgumentException` handler). If not caught, this results in a 500 response — a security-visible information leak from an unexpected stack trace.
- **Evidence:** `architecture-v2.md` TodoController section (`Map<String, String>` body approach retained); `requirements-v2.md` EC-07 (invalid priority values).
- **Recommendation:** Verify empirically during implementation that `Map<String, String>` with a numeric priority value returns 400, not 500. If it returns 500, add an `HttpMessageNotReadableException` handler to the controller's catch blocks returning 400.
- **Resolution:** Open (advisory — verify during implementation)

---

## Agreed Design Decisions

The following decisions are locked by this review and become binding implementation constraints. Any deviation requires a new design review pass.

1. **`add()` overload retained:** `TodoService.add(String text)` is kept as a one-argument overload that delegates to `add(text, null)`. This is the only method the existing `TodoServiceTest` calls and must not be removed.

2. **Controller calls two-argument form:** `TodoController.create()` always calls `todoService.add(body.get("text"), body.get("priority"))`. The one-argument overload in the service is strictly for existing test backward-compatibility and is not called from production code paths.

3. **Normalize-then-validate order is correct and locked:** The normalization `priority.trim().toUpperCase()` runs before the `Set.of("HIGH","MEDIUM","LOW")` membership check. Null/blank input bypasses normalization and directly defaults to `"MEDIUM"`. This correctly handles `"high"`, `"HIGH"`, `"High"`, `" HIGH "`, and `"  "`.

4. **Filter/search state reset on add, not on toggle or delete:** `addTodo()` resets `activeFilter = 'all'` and clears `#search-input` before calling `loadTodos()`. Toggle and delete calls do NOT reset filter or search state; they call `loadTodos()` which calls `filterAndRender()` with the existing state.

5. **`allTodos` cache replaced wholesale on every server response:** No incremental update. `loadTodos()` replaces `allTodos` with the full server response every time it is called.

6. **Counter label format: "X of Y done"** — X = done count in `visibleTodos`, Y = total count in `visibleTodos`. No "(filtered)" suffix or any other suffix. Applied whether or not a filter is active.

7. **Priority badge uses CSS class whitelist only:** The raw `priority` string from the server is never inserted into `innerHTML` or used as an element attribute. `badgeClass()` maps `"HIGH"` → `"badge-high"`, `"MEDIUM"` → `"badge-medium"`, `"LOW"` → `"badge-low"`. Display text uses `span.textContent` with the whitelist `{HIGH: "High", MEDIUM: "Medium", LOW: "Low"}`.

8. **Add button re-enabled in `finally` block:** Both the `disabled` attribute and the visual indicator (spinner/loading text) are restored in the `finally` block of `addTodo()`, unconditionally, regardless of success or failure.

9. **Fade-in animation is targeted, not universal:** The `.todo-row` base class does not carry the `@keyframes fadeIn` animation. Only the row for a newly POSTed task receives the animation class (e.g., `.todo-row-new`), set by JS after the successful POST, and cleared before the next render cycle. Toggle and delete re-renders do not animate existing rows.

10. **`filterAndRender()` three-state visibility logic:** As specified in Finding 4 recommendation — `#empty-state` shown only when `allTodos.length === 0`; `#no-match-msg` shown when `allTodos.length > 0` and `visibleTodos.length === 0`; task list shown when `visibleTodos.length > 0`. All three are mutually exclusive.

---

## Architecture Changes Required

The following targeted updates to `architecture-v2.md` are required before Stage 4 begins. The file must remain `Status: Approved` after each update.

1. **TodoService component — add overload documentation:** Add to the Interfaces section:
   - `add(String text)` — backward-compatible one-argument overload retained; delegates to `add(text, null)`; used only by existing tests, not by production call paths.

2. **CSS animation — replace global rule with targeted-class approach:** Replace the entry `.todo-row { animation: fadeIn 0.4s ease; }` in the CSS rules table with:
   - `.todo-row-new { animation: fadeIn 0.4s ease; }` — applied by JS only to the most recently added `<li>`; removed before next render cycle.
   - Document in the `renderTodos()` JS table row that it accepts an optional `newTodoId` parameter and adds `.todo-row-new` to the matching `<li>`.

3. **`filterAndRender()` — add pseudocode:** Add the three-state visibility pseudocode block documented in Finding 4 recommendation to the `filterAndRender()` row of the JS functions table.

4. **Counter label — close Open Question 2:** Replace Open Question 2 with the locked decision: "Counter label is `X of Y done` with no suffix (see Agreed Design Decisions §6)."

5. **Test plan — add whitespace-padding test:** Add `add_withWhitespacePaddedPriority_normalizesToUppercase` to the service test plan table (input `" high "`, expected result `"HIGH"`).

6. **Controller test plan — update existing stubs:** Note that the two existing stubs in `TodoControllerTest` for `add()` must be updated to the two-argument form (`add(anyString(), any())`) as part of the v2 implementation task.

---

## Open Questions

All open questions from `architecture-v2.md` are resolved or deferred by this review:

1. **Exact empty-state illustration** (architecture-v2.md Open Question 1) — Remains open and correctly deferred to the implementation stage. The architecture imposes no constraint beyond "visually distinct."

2. **Counter label wording** (architecture-v2.md Open Question 2) — **Resolved and locked** by Agreed Design Decision 6: `"X of Y done"`, no suffix.

3. **requirements-v2.md Status** (architecture-v2.md Open Question 3) — **Must be resolved** per Finding 1 before Stage 4 begins.

4. **Non-string JSON values for priority** (Finding 9) — Deferred to implementation stage verification. If Jackson with `Map<String, String>` returns 500 on a numeric priority value, an `HttpMessageNotReadableException` handler must be added.
