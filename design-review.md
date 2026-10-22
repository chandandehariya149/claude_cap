## Status
Draft

## Last Updated
2026-10-22

## Summary
Approved with Findings — The architecture is structurally sound and all 11 functional requirements map to defined components. Four findings (two Major, two Minor) were resolved during this review by updating `architecture.md`. Two advisory findings remain open for the implementation planner. The design is ready to proceed to Stage 4 (Implementation Planning) once `architecture.md` and `requirements.md` are confirmed as Approved.

---

## Findings

### Finding 1 — Max text length undefined
- **Severity:** Major
- **Area:** TodoService data validation; architecture.md §Data Models; §Open Questions #1
- **Description:** The data model table states "no max length enforced server-side" and Open Question 1 was left unresolved. EC-04 requires the app to not crash on very long input, but without a server-side bound, a request body of several megabytes is accepted and stored, exhausting heap memory over repeated calls. The frontend also has no input length constraint.
- **Evidence:** `architecture.md` §Data Models row for `text`: "Non-blank, no max length enforced server-side"; §Open Questions #1.
- **Recommendation:** Enforce a 500-character maximum in `TodoService.add()` alongside the blank check. Return HTTP 400 with `{"error": "Text must not exceed 500 characters"}` if exceeded. Add `maxlength="500"` to the `<input>` element in `index.html` as a first-line client-side guard.
- **Resolution:** Fixed — `architecture.md` updated to specify 500-character limit with server-side validation and HTML `maxlength`.

---

### Finding 2 — Todo model implementation not committed (record vs. POJO)
- **Severity:** Major
- **Area:** `Todo.java` component definition
- **Description:** The architecture says "Java 17 record or plain POJO class" without committing. Java records are immutable by default. The `toggle` operation flips `done` in-place; if `Todo` is a record, the implementer must create a replacement instance and swap it in the list, which adds non-trivial complexity and changes the `TodoService.toggle()` contract. Leaving the choice open risks an incorrect implementation of the most complex operation.
- **Evidence:** `architecture.md` §Todo.java: "Java 17 record or plain POJO class; no external library required."
- **Recommendation:** Commit to a plain mutable POJO with a full-argument constructor, private fields, and public getters/setters. This makes `toggle` a straightforward `todo.setDone(!todo.isDone())`.
- **Resolution:** Fixed — `architecture.md` §Todo.java updated to specify plain mutable POJO with getters/setters.

---

### Finding 3 — Thread-safety strategy has two unresolved options
- **Severity:** Minor
- **Area:** `TodoService.java` in-memory store
- **Description:** The architecture offers `Collections.synchronizedList` OR `CopyOnWriteArrayList` without choosing. They have different iteration semantics: a `synchronizedList` requires the caller to hold the list's lock during `for` iteration or it is unsafe; `CopyOnWriteArrayList` is iterator-safe without explicit locking. An implementer who picks `synchronizedList` but forgets to synchronize `getAll()`'s iteration introduces a race condition.
- **Evidence:** `architecture.md` §TodoService.java: "Uses `Collections.synchronizedList` or `CopyOnWriteArrayList`".
- **Recommendation:** Commit to `CopyOnWriteArrayList`. The workload is read-heavy (GET /api/todos on every page load), writes are infrequent, and the list will remain small (tens of items). Copy-on-write overhead is negligible and the API is safer.
- **Resolution:** Fixed — `architecture.md` updated to specify `CopyOnWriteArrayList` exclusively.

---

### Finding 4 — Frontend network error banner underspecified
- **Severity:** Minor
- **Area:** `static/index.html` error handling
- **Description:** The error handling strategy says "JS catches and displays a generic error banner; list state preserved" but does not specify the message text, the DOM placement, or how and when the banner is dismissed. Without this, the implementation is ambiguous and review will be difficult.
- **Evidence:** `architecture.md` §Error Handling Strategy row: "Frontend — network error".
- **Recommendation:** Specify: message text is "Network error — please try again", banner appears above the task list in a visually distinct area, and it auto-dismisses after 5 seconds or when the next successful response arrives.
- **Resolution:** Fixed — `architecture.md` error handling strategy updated with the specified text and behavior.

---

### Finding 5 — PUT /api/todos/{id} deviates from REST idempotency
- **Severity:** Advisory
- **Area:** REST API design; `TodoController` PUT endpoint
- **Description:** REST PUT is conventionally idempotent (identical requests produce identical server state). The toggle design means two successive PUTs flip `done` twice, returning different results. This is a deliberate design choice mandated by FR-04 and assumption A-4 (toggle, not set), but it breaks the idempotency guarantee that HTTP clients and intermediaries may assume for PUT.
- **Evidence:** FR-04; requirement assumption A-4; `architecture.md` §API Contract.
- **Recommendation:** No code change. Document this as an explicit architectural constraint so implementers and reviewers are not surprised. If a future version needs PATCH-style semantics, a separate `PATCH /api/todos/{id}` accepting `{"done": true/false}` would restore idempotency.
- **Resolution:** Accepted — documented in `architecture.md` §Architectural Constraints.

---

### Finding 6 — No HTTP controller layer test defined
- **Severity:** Advisory
- **Area:** Test coverage; `architecture.md` §File Structure
- **Description:** The file structure includes only `TodoServiceTest.java`. The controller layer — exception-to-HTTP mapping, path variable parsing, `@RequestBody` deserialization, response serialization — is untested. A bug in the controller (wrong status code, wrong JSON field name) would not be caught by unit tests alone.
- **Evidence:** `architecture.md` §File Structure: only `TodoServiceTest.java` shown.
- **Recommendation:** Add `TodoControllerTest.java` using Spring Boot's `@WebMvcTest` slice. Cover at minimum: GET returns 200 with list, POST returns 201 with created item, POST with blank text returns 400, PUT with unknown id returns 404, DELETE with unknown id returns 404.
- **Resolution:** Open — recommendation passed to the implementation planner for Stage 4.

---

## Requirements Coverage Matrix

| Requirement | Implementing Component(s) | Notes |
|-------------|--------------------------|-------|
| FR-01 — Serve index.html at port 8080 | `TodoApplication.java` + Spring Boot static resource handler | Auto-configured by `spring-boot-starter-web` |
| FR-02 — GET /api/todos returns JSON array | `TodoController.getAll()` + `TodoService.getAll()` + `Todo` model | `List<Todo>` serialized by Jackson |
| FR-03 — POST /api/todos creates item, returns 201 | `TodoController.add()` + `TodoService.add()` + `AtomicLong` ID gen + `Todo` model | Blank validation in service |
| FR-04 — PUT /api/todos/{id} toggles done | `TodoController.toggle()` + `TodoService.toggle()` + `Todo` POJO setter | Returns updated `Todo` |
| FR-05 — DELETE /api/todos/{id}, returns 204 | `TodoController.delete()` + `TodoService.delete()` | 404 on unknown id |
| FR-06 — In-memory storage only | `TodoService` field `CopyOnWriteArrayList<Todo>` | Reset on server restart by design |
| FR-07 — Reject blank text (400 + client validation) | `TodoService.add()` (server); `TodoController` (400 response); `index.html` (client pre-check) | Dual-layer validation |
| FR-08 — "X of Y done" counter | `index.html` JS counter logic + DOM update after every mutating operation | Derived from current list state |
| FR-09 — CSS strike-through on done items | `index.html` CSS (`text-decoration: line-through`) + JS toggle handler | Applied/removed reactively on toggle response |
| FR-10 — All operations via Fetch API | `index.html` Fetch API calls to relative `/api/todos` paths | No full page reloads |
| FR-11 — Load todos on page load | `index.html` `DOMContentLoaded` handler → `GET /api/todos` | Renders server state on refresh |
| NFR-01 — 200 ms response time | In-memory store (no I/O); O(n) list operations on small n | No external call latency |
| NFR-02 — Clean startup via mvn spring-boot:run | `TodoApplication.java` + minimal `pom.xml` | Only `spring-boot-starter-web` required |
| NFR-03 — Mobile-friendly at 375 px | `index.html` CSS responsive layout | No framework; CSS media queries or fluid layout |
| NFR-04 — Clean visual design | `index.html` CSS | Adequate contrast, spacing, control differentiation |
| NFR-05 — No authentication | No security config present | Public access by design |
| NFR-06 — Standard Maven build | `pom.xml` with two declared dependencies only | No snapshot deps; JDK 17 + Maven 3.6+ sufficient |

---

## Risk Register

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|-----------|
| R-01 | In-memory data loss on server restart | High (always happens) | Low (by design; local dev only) | FR-06 and assumption A-2 explicitly accept this; documented in UI |
| R-02 | XSS via malicious todo text | Low (local dev only) | High (would allow script injection) | `index.html` uses `textContent`/`createElement` instead of `innerHTML`; enforced by SC-01 |
| R-03 | Concurrent modification of todo list | Low (single-user dev) | Medium (data corruption / ConcurrentModificationException) | `CopyOnWriteArrayList` + `AtomicLong` — both thread-safe; assumption A-2 permits this approach |
| R-04 | Excessive memory from very long text | Low (500-char limit added) | Low (bounded by Finding 1 fix) | 500-char server-side limit + HTML `maxlength=500` |
| R-05 | Stale UI after rapid successive actions (EC-05) | Low | Medium (user sees inconsistent state) | Frontend 404 handler silently re-fetches full list; recovers to correct state |
| R-06 | Build breaks on fresh checkout (missing deps) | Low | Low (first-time only) | Standard Maven Central deps; internet required for initial download (assumption A-7) |
| R-07 | PUT toggle non-idempotency surprises future maintainers | Medium | Low (dev context only) | Documented as explicit architectural constraint (Finding 5) |

---

## Design Decisions Confirmed

The following decisions are locked for implementation and must not be changed without a new design review:

1. **Todo model is a plain mutable POJO** — `Todo.java` uses private fields with public getters and setters. Java records are excluded because immutability conflicts with in-place toggle. (Resolves Finding 2.)

2. **Thread-safe list is `CopyOnWriteArrayList<Todo>`** — `TodoService` declares its store as `new CopyOnWriteArrayList<>()`. `Collections.synchronizedList` is excluded. (Resolves Finding 3.)

3. **Max text length is 500 characters** — `TodoService.add()` throws `IllegalArgumentException` if `text.length() > 500`. `TodoController` maps this to HTTP 400. `index.html` sets `maxlength="500"` on the text input. (Resolves Finding 1.)

4. **No `@CrossOrigin` annotation** — frontend is served same-origin; no CORS configuration is added anywhere. (SC-04, Architectural Constraint #5.)

5. **Toggle is always a flip** — `PUT /api/todos/{id}` always inverts `done`, regardless of current state. No request body is accepted. This is an intentional deviation from REST idempotency; documented. (FR-04, Assumption A-4, Finding 5.)

6. **Frontend network error banner** — message text is "Network error — please try again"; appears above the task list; auto-dismisses after 5 seconds or on next successful response. (Resolves Finding 4.)

7. **DELETE /api/todos/{id} returns 404 for unknown id** — not a silent 200 or 204. (Assumption A-5, Error Handling Strategy.)

8. **`AtomicLong` for ID generation** — thread-safe by design; IDs are never reused within a session; counter resets on server restart. Embedded Tomcat is multi-threaded, making `AtomicLong` necessary even for "single-user" dev use.

9. **Controller exception mapping is explicit try/catch** — no `@ControllerAdvice` global handler; each controller method catches its own service exceptions. Acceptable for four methods; avoids hidden cross-cutting magic.

10. **`TodoControllerTest.java` to be added** — `@WebMvcTest` slice covering all HTTP-layer error mappings. Recorded here so the implementation planner includes it in Stage 4. (Finding 6.)

---

## Architecture Changes Required

The following sections of `architecture.md` were updated as part of this review:

1. **§Todo.java** — "Java 17 record or plain POJO class" changed to "plain mutable POJO with private fields and public getters/setters".
2. **§TodoService.java** — "Collections.synchronizedList or CopyOnWriteArrayList" changed to "CopyOnWriteArrayList".
3. **§Data Models table** — `text` field Notes changed from "Non-blank, no max length enforced server-side" to "Non-blank, max 500 characters; validated by TodoService".
4. **§Error Handling Strategy** — network error row expanded with specific banner text and dismissal behavior.
5. **§Architectural Constraints** — added constraint #7 documenting PUT toggle non-idempotency.
6. **§Open Questions** — both questions resolved and section removed.
7. **§File Structure** — added `TodoControllerTest.java`.
8. **Status** — changed from `Draft` to `Approved`.
