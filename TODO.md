# TODO — Tomorrow

A Java Swing task manager that tracks procrastination with humorous reminders. Complete the work in order, keep this file updated, and do not mark an item done until it has been tested.

## 0. Project setup
- [x] Inspect available Java and Maven versions; target Java 21.
- [x] Create a Maven project and configure dependencies: Swing (JDK), FlatLaf, Gson, JUnit 5, and Jakarta Mail/Angus Mail for SMTP.
- [x] Create the package structure under `com.tomorrow`.
- [x] Add `.gitignore` for build outputs, IDE files, runtime data, logs, and secrets.
- [x] Add a minimal `Main` entry point that opens a Swing window on the Event Dispatch Thread.
- [x] Verify `mvn test` and `mvn package` work before adding features.

## 1. Task model and core behavior
- [x] Implement `Task` with ID, title, description, creation date, original due date, current due date, postponement count, completion status, and optional completion date.
- [x] Use `LocalDate` for dates and ISO-8601 date strings in JSON.
- [x] Validate that task titles are not blank; allow past due dates and default new tasks to today's date.
- [x] Implement `TaskManager` operations: add, list, edit as appropriate, complete, postpone, delete, and filter.
- [x] On postponement, add exactly one calendar day to the current due date and increment the counter once. Preserve the original due date. Reject postponement of completed tasks.
- [x] Set completion status and completion date when a task is completed.
- [x] Derive overdue state from `!completed && dueDate < today`.
- [x] Add unit tests for validation, postponement, completion, overdue state, and deletion.

## 2. JSON persistence
- [x] Implement `JsonStorage` using Gson, including explicit `LocalDate` serialization/deserialization.
- [x] Persist tasks automatically after successful changes and load them at startup.
- [x] Store runtime data in a writable application-data directory; document the development-time location.
- [x] Handle first launch when no data file exists by starting with an empty task list.
- [x] Use safe file replacement where practical and preserve the existing file if writing fails.
- [x] Report malformed or unreadable JSON clearly; do not silently overwrite it with an empty list.
- [x] Add tests for round-trip persistence, missing files, and invalid JSON handling.

## 3. Main Swing interface
- [x] Apply FlatLaf and establish consistent fonts, spacing, colors, and window sizing.
- [x] Implement `MainFrame` and `DashboardPanel`.
- [x] Show total, pending, completed, overdue, and postponement statistics derived from task data.
- [x] Implement task cards with title, due date, postponement count, status, and actions.
- [x] Implement an add/edit task dialog with title, optional description, and due-date selection.
- [x] Add actions for postpone, complete, and delete; confirm before deletion.
- [x] Add All, Pending, Completed, and Overdue filters.
- [x] Ensure UI refreshes after task changes and all Swing updates occur on the Event Dispatch Thread.
- [x] Manually verify the main task workflow from the UI.

## 4. Notification models and priority engine
- [x] Implement `Notification` with unique ID, optional task ID, type, priority, title, message, creation timestamp, read/dismissed state, deduplication key, and per-channel delivery status.
- [x] Implement `PriorityCalculator` with these deadline tiers:
  - [x] P1: overdue by 3 or more days
  - [x] P2: overdue by 1–2 days
  - [x] P3: due today
  - [x] P4: due in 1–3 days
  - [x] P5: due in 4 or more days
- [x] Sort notifications P1 first, then by earliest due date; use postponement count only as a tie-breaker or modest adjustment within a tier.
- [x] Implement `MessageProvider` with several friendly humorous messages for created, postponed, due-today, overdue, and completed events.
- [x] Support a professional notification tone as an alternative to humorous text.
- [x] Avoid repeating the exact same message for consecutive reminders where practical.
- [x] Add unit tests for all priority boundary dates and message selection behavior.

## 5. In-app notification center
- [x] Implement `NotificationManager` to create, order, deduplicate, and persist notifications.
- [x] Implement `NotificationPanel` with unread count, priority-sorted items, mark-as-read, dismiss, and open-associated-task actions.
- [x] Show immediate in-app feedback for postponement and completion.
- [x] Persist notification history in `notifications.json` and restore it at startup.
- [x] Ensure app restarts do not recreate duplicate reminders for the same task/event/date.
- [x] Test notification ordering, read/dismiss state, navigation, and persistence.

## 6. Scheduling and reminder rules
- [x] Implement `ReminderScheduler` that checks tasks periodically while the app is running.
- [x] Generate due-today and overdue reminders without producing a notification on every timer tick.
- [x] On launch, reconcile missed reminders without sending a burst of duplicate alerts.
- [x] Send a daily email digest rather than one email per task or postponement.
- [x] Implement configurable quiet hours for optional desktop and email reminders; retain notifications in the in-app history.
- [x] Clearly document that scheduled reminders are not produced while the application is closed in this version.
- [x] Test scheduler behavior using a controllable clock or testable date/time logic wherever practical.

## 7. macOS desktop notification channel
- [x] Define a common `NotificationChannel` interface.
- [x] Implement `MacOSChannel` separately from the core notification engine.
- [x] Prototype and test the chosen macOS notification mechanism on the target Mac early.
- [x] Handle unavailable commands, permissions, unsupported environments, and delivery errors gracefully.
- [x] Preserve every generated notification in the in-app center even if desktop delivery fails.
- [x] Do not mark desktop notifications as verified until they have been tested on macOS (Verified via osascript on macOS).
- [x] Document the manual macOS permission and behavior test.

## 8. Email notification channel
- [x] Implement `EmailChannel` using Jakarta Mail/Angus Mail and SMTP configuration.
- [x] Make email opt-in and add recipient/server/port configuration plus a test-email action.
- [x] Keep SMTP credentials out of source code, Git, logs, and plain JSON settings. Use runtime environment/configuration or a secure credential store.
- [x] Generate a readable daily digest sorted by urgency and including due dates and postponement counts.
- [x] Run email sends off the Swing Event Dispatch Thread.
- [x] Handle authentication, network, and SMTP errors; record success/failure separately from in-app delivery.
- [ ] Test with an explicitly configured account; document that email requires network access and valid SMTP credentials.

## 9. Settings and integration
- [x] Implement `AppSettings` and `SettingsPanel` for channel toggles, email configuration (excluding secrets), tone, reminder schedule, and quiet hours.
- [x] Persist safe settings in `settings.json`.
- [x] Ensure disabling a channel stops new deliveries through that channel.
- [x] Integrate task operations, dashboard statistics, notification generation, scheduling, and delivery channels.
- [x] Check that a failed notification channel does not crash the application or block other channels.

## 10. Final verification and handoff
- [x] Run all JUnit tests and `mvn clean test package`.
- [x] Test add, postpone, complete, delete, filters, and persistence after restarting the app.
- [x] Test overdue boundaries and notification priority ordering.
- [x] Test duplicate prevention after restart and notification channel failures.
- [x] Verify macOS desktop delivery on the target Mac, or clearly state it remains unverified.
- [ ] Verify email delivery with configured SMTP credentials, or clearly state it remains unverified.
- [x] Confirm no secrets or personal runtime data are committed.
- [ ] Write `README.md` with prerequisites, build/run commands, data location, features, email setup, notification limitations, and demo steps.
- [ ] Prepare a short demo scenario and ensure both partners can explain the architecture and key classes.

## Scope guardrails
- No database, backend server, account system, OAuth workflow, calendar integration, or background daemon in this version.
- Notifications run on the schedule only while the app is open. On relaunch, reconcile missed reminders without duplicate bursts.
- Prioritize a working, tested core app over optional visual effects or extra features.
