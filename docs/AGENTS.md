# AGENTS.md — Tomorrow Project Instructions

## Project goal
Build **Tomorrow**, a beginner-friendly Java desktop task manager with a humorous procrastination theme. The application must be understandable to BCA students and feasible to implement and demonstrate in a short project timeline.

## Source of truth
- Read `TODO.md` before starting work and use it as the implementation checklist.
- Follow the decisions in this file. Do not introduce new technologies or expand the scope without a clear need.
- Keep `TODO.md` updated as work is completed. Do not mark a task complete unless it has been implemented and verified.

## Technology requirements
- Java 21
- Java Swing for the user interface
- FlatLaf for modern Swing styling
- Maven for the build and dependency management
- Gson for JSON persistence
- `java.time.LocalDate` for task dates
- JUnit 5 for unit tests
- Jakarta Mail (Angus Mail implementation is acceptable) for SMTP email
- macOS desktop notifications must be implemented behind a separate notification-channel abstraction and tested on macOS

Do not replace Swing with JavaFX, a web frontend, Electron, or another UI framework. Do not add a database, backend server, cloud service, or authentication system.

## Product requirements
- Users can create, view, edit where practical, complete, postpone, filter, and delete tasks.
- A task has a title, optional description, creation date, original due date, current due date, postponement count, completion status, and optional completion date.
- The due date defaults to today. A past due date is allowed so overdue tasks can be represented. Titles cannot be blank.
- Postponing a pending task moves its current due date forward by exactly one calendar day and increments its postponement count by one. Preserve the original due date. Completed tasks cannot be postponed.
- A task is overdue when it is incomplete and its current due date is before today's date.
- Dashboard statistics must be derived from current task data, not maintained as independent counters.
- Save changes automatically and restore data at startup. Never silently overwrite malformed JSON or report a save as successful if it failed.
- Provide a humorous, friendly tone by default and a professional tone setting. Avoid abusive or shaming messages.

## Notification requirements
- Provide in-app notifications, macOS desktop notifications, and email reminders.
- Use one shared notification engine; channels must not independently decide priority or generate their own task logic.
- Notification priority order is P1 (critical) through P5 (low). P1 is the highest priority.
- Initial urgency rules:
  - P1: overdue by 3 or more days
  - P2: overdue by 1–2 days
  - P3: due today
  - P4: due in 1–3 days
  - P5: due in 4 or more days
- Where priorities match, sort tasks by earliest current due date, then higher user-selected task priority if implemented, then greater postponement count.
- Postponement count may be used as a tie-breaker or modest urgency adjustment, but must not override the deadline tiers above. Do not silently change the defined P1–P5 rules.
- In-app notifications need read/unread and dismiss states. Keep a history and prevent duplicate reminders across restarts.
- Default to a daily email digest rather than sending a separate email for every task. Task creation, completion, and postponement should not send emails by default.
- Email is opt-in and must have a test-email action. Store no SMTP password, app password, or other secret in source control or a plain JSON settings file. Read credentials from runtime configuration/environment or an appropriate secure credential store. Never log credentials.
- Keep SMTP work off Swing's Event Dispatch Thread so the UI remains responsive. Handle network and authentication failures visibly.
- The initial scheduler runs only while the app is running. At startup, reconcile overdue tasks and missed reminders without sending a burst of duplicate notifications. Do not claim reminders will run while the application is closed.
- For macOS desktop alerts, check operating-system support and failure cases. If desktop delivery is unavailable, preserve the notification in-app and show a useful status/error. Do not claim macOS integration works until it has been tested on macOS.
- Respect quiet hours for optional desktop/email reminders. Do not suppress the in-app notification history.

## Architecture and code quality
- Prefer small, single-purpose classes and beginner-readable code.
- Separate model, business logic, persistence, notification delivery, and Swing UI. UI event handlers should call services instead of containing business rules or JSON logic.
- Suggested packages: `com.tomorrow.model`, `com.tomorrow.service`, `com.tomorrow.storage`, `com.tomorrow.notification`, and `com.tomorrow.ui`.
- Suggested responsibilities:
  - `Task`: task data and basic derived state
  - `TaskManager`: task operations and validation
  - `JsonStorage`: load/save JSON safely
  - `PriorityCalculator`: calculate notification priority
  - `MessageProvider`: select humorous/professional message text
  - `NotificationManager`: create, sort, deduplicate, and persist notifications
  - `ReminderScheduler`: schedule checks while the app is running
  - `NotificationChannel`: common channel interface
  - `InAppChannel`, `MacOSChannel`, `EmailChannel`: channel implementations
- Use stable unique IDs for tasks and notifications.
- Use ISO-8601 dates in JSON. Configure Gson explicitly for `LocalDate` serialization/deserialization.
- Use safe writes where practical (write to a temporary file, then replace the target) and preserve recoverable data on storage errors.
- Keep Swing UI updates on the Event Dispatch Thread. Long-running operations, especially SMTP, must run off that thread.
- Do not add dependencies unless they support an agreed feature and are recorded in `pom.xml`.
- Avoid premature abstractions and overengineering. Build only what the project needs.

## Working protocol for the coding agent
1. Inspect the repository and read `TODO.md` before editing files.
2. Work on one milestone at a time, following the order in `TODO.md`.
3. Before a milestone, briefly state what you will implement and which files you expect to touch.
4. Make the smallest coherent change that completes the milestone. Do not generate the entire application in one uncontrolled step.
5. Run Maven compilation and the relevant tests after each milestone. Fix failures before proceeding.
6. If a macOS-only capability cannot be verified in the current environment, state that clearly and provide the manual test steps; do not mark it verified.
7. Do not replace working code just to match a personal preference. Explain necessary design changes and update `TODO.md` if scope changes.
8. Do not fabricate test results. Report the exact commands run and whether they passed.
9. Keep dependencies and implementation suitable for a beginner to explain in a viva. Add concise comments for non-obvious logic, not comments on every line.
10. Keep secrets, local user data, build outputs, and IDE-specific files out of version control. Ensure `.gitignore` covers them.
11. Do not create additional specification documents unless explicitly requested. Keep the project guidance in `AGENTS.md` and progress in `TODO.md`.

## Completion standard
A feature is complete only when its behavior is implemented, errors are handled, relevant tests pass, and `TODO.md` reflects the verified state. The final README must explain setup, running, data-file location, notification limitations, and email configuration without exposing secrets.
