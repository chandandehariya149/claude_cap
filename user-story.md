# User Story: Todo List Web App

**Story ID:** TODO-001
**Title:** Simple Todo List with Java Backend

---

## As a user...

As a **user**, I want to open a web page, add tasks to a todo list, mark them as done, and delete them — so I can manage my tasks easily from any browser.

---

## What the app does

- User opens `http://localhost:8080` in their browser
- They see a clean todo list page
- They type a task in an input field and press **Add** — the task appears in the list instantly
- Each task has a **Done** checkbox — checking it strikes through the task text
- Each task has a **Delete** button — clicking it removes the task from the list
- All tasks are stored in memory on the server (resets when server restarts)
- The page shows a count of total tasks and how many are completed (e.g. "3 of 5 done")

---

## Technical requirements

- **Backend:** Java 17+ with Spring Boot 3.x
- **API:** REST — `GET /api/todos`, `POST /api/todos`, `PUT /api/todos/{id}`, `DELETE /api/todos/{id}`
- **Frontend:** Plain HTML + CSS + vanilla JavaScript (no React/Vue needed)
- **Storage:** In-memory Java List (no database needed)
- **Build:** Maven (`pom.xml`) — run with `mvn spring-boot:run`
- Frontend served as a static file from `src/main/resources/static/index.html`

---

## Acceptance Criteria

1. Visiting `http://localhost:8080` shows the todo list page
2. Submitting a task via the form adds it to the list (POST to backend)
3. The new task appears on the page without a full page reload
4. Clicking the Done checkbox marks the task complete (PUT to backend) and strikes through the text
5. Clicking Delete removes the task (DELETE to backend) and removes it from the page
6. The task counter ("X of Y done") updates whenever a task is added, completed, or deleted
7. Empty task input is rejected — user sees a validation message
8. Running `mvn spring-boot:run` starts the server with no errors
9. The page looks clean and is mobile-friendly
10. Tasks persist across page refreshes (stored on server, not in browser localStorage)

---

## Out of scope

- User login / authentication
- Persistent database storage
- Due dates or priorities
- Multiple lists / categories
