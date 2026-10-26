# Tomorrow - Codebase Documentation

## Overview
A complete Java Swing task manager with humorous procrastination theme. Built with Maven, Java 21, FlatLaf, Gson, JUnit 5, and Angus Mail.

## Directory Structure

```
tomorrow/
├── src/main/java/com/tomorrow/       # Main source code (15 files)
│   ├── model/                        # Task data model (1 file)
│   ├── service/                      # Business logic (6 files)
│   ├── storage/                      # JSON persistence (1 file)
│   ├── notification/                 # Notification system (5 files)
│   └── ui/                           # Swing user interface (2 files)
│
├── src/test/java/                    # JUnit 5 test suite (5 files)
├── pom.xml                           # Maven configuration
├── .gitignore                        # Git exclusions
├── docs/                             # Documentation folder
│   ├── README.md                     # User-facing guide
│   ├── TODO.md                       # Implementation checklist
│   ├── AGENTS.md                     # Project guidelines
│   └── CODEBASE_DOCUMENTATION.md     # This file
└── target/                           # Build output (excluded by .gitignore)
```

## Source Code Files (15 Main + 5 Test)

### Main Source Files (src/main/java/)

#### 1. `Task.java` (`com.tomorrow.model`)
**Core task data model** with validation and derived state.

| Method | Description |
|---|---|
| `Task()` | Default constructor: ID=UUID, dates=today, postponement=0, not completed |
| `Task(String title)` | Constructor with title validation |
| `getTitle()` / `setTitle(String)` | Get/set title; throws `IllegalArgumentException` if blank |
| `getDescription()` / `setDescription(String)` | Get/set optional description |
| `getCreationDate()` | Returns `LocalDate.now()` at creation |
| `getOriginalDueDate()` / `setOriginalDueDate(LocalDate)` | Preserved throughout postponements |
| `getDueDate()` / `setDueDate(LocalDate)` | Current due date |
| `getPostponementCount()` / `increment` via `postpone()` | Starts at 0, increments 1 per postpone |
| `isCompleted()` / `setCompleted(boolean)` | Completion status |
| `getCompletionDate()` / `setCompletionDate(LocalDate)` | Stamped on complete() |
| `postpone()` | Adds exactly 1 calendar day to dueDate, increments counter, throws if completed |
| `complete()` | Marks completed, stamps completionDate = LocalDate.now() |
| `isOverdue(LocalDate today)` | Returns `!completed && dueDate.isBefore(today)` |

#### 2. `TaskManager.java` (`com.tomorrow.service`)
**Task operations and validation**. Holds in-memory list, auto-saves via JsonStorage callback.

| Method | Description |
|---|---|
| `TaskManager(JsonStorage)` | Constructor with storage reference |
| `load(List<Task>)` | Load tasks from storage, auto-save |
| `add(String title, String desc, LocalDate due)` | Create task; due defaults to today |
| `edit(String id, String title, String desc, LocalDate due)` | Edit task; preserves originalDueDate if not completed |
| `complete(String id)` | Marks task complete, stamps date, auto-saves |
| `postpone(String id)` | Adds 1 day to dueDate, increments counter, throws if completed |
| `delete(String id)` | Removes task, auto-saves |
| `filter(Filter)` | Returns `List<Task>`: ALL, PENDING, COMPLETED, OVERDUE |
| `stats()` | Returns `Stats(total, pending, completed, overdue, postponements)` — **derived from current data, not maintained counters** |

**Enum `Filter`**: `ALL`, `PENDING`, `COMPLETED`, `OVERDUE`

#### 3. `JsonStorage.java` (`com.tomorrow.storage`)
**Gson-based JSON persistence** with explicit `LocalDate` serialization and safe file handling.

| Method | Description |
|---|---|
| `JsonStorage(Path dataDir)` | Constructor; creates `tasks.json`, `notifications.json`, `settings.json` |
| `loadTasks()` | Returns `List<Task>`; empty if file missing |
| `saveTasks(List<Task>)` | Safe write: write to `.tmp`, then atomic `move()`; preserves file on failure |
| `loadNotifications()` / `saveNotifications(List<Notification>)` | Same pattern for notifications |
| `loadSettings()` / `saveSettings(Map<String,Object>)` | Settings persistence |
| `loadList(Path, Type)` | Reads JSON; throws `StorageException` on malformed JSON (file NOT overwritten) |
| `loadObject(Path, Type)` | Reads single object; same malformed protection |
| `LocalDateAdapter` | Explicit Gson adapter for ISO-8601 date strings (`2025-12-25`) |
| `StorageException` | Runtime exception with clear messages; never silently overwrite |

**Key Policy**: "Report malformed/unreadable JSON clearly; do not silently overwrite with empty list."

#### 4. `PriorityCalculator.java` (`com.tomorrow.service`)
**Deadline tier priority engine**. Sort: P1 first → earliest due date → postponement count (tie-breaker).

| Method | Description |
|---|---|
| `priorityFor(LocalDate dueDate, LocalDate today)` | Computes `Notification.Priority`:
- P1: days <= -3 (overdue 3+ days)
- P2: days in [-2, -1] (overdue 1-2 days)
- P3: days == 0 (due today)
- P4: days in [1, 3] (due in 1-3 days)
- P5: days >= 4 (due in 4+ days) |
| `sort(List<Notification>)` | Sorts by: priority tier (1-5) → due date (earliest first) → postponement count (higher first, as tie-breaker) |
| `tier(Notification.Priority)` | Maps P1→1, P2→2, ..., P5→5 for sorting |

**Boundary Dates** (tested):
- today → P3
- today±1 → P2/P4
- today±2,±3 → P2/P1 respectively
- today+4+ → P5

#### 5. `MessageProvider.java` (`com.tomorrow.service`)
**Friendly notification text selection**. Humorous (default) or professional tone.

| Method | Description |
|---|---|
| `MessageProvider()` | Default humorous tone |
| `MessageProvider(Tone)` | Constructor with tone selection |
| `created(Task)` / `postponed(Task)` / `dueToday(Task)` / `overdue(Task, long days)` / `completed(Task)` | Returns text for each event type |
| `getTone()` | Returns current `Tone` (`HUMOROUS` or `PROFESSIONAL`) |
| **Repetition avoidance**: Keeps last message per `(taskId + ":" + type)`; picks alternate if consecutive call would repeat exact same message |

**Tone Options**:
- `HUMOROUS`: Funny phrases like `"Task \"X\" created. Procrastination officially scheduled."`
- `PROFESSIONAL`: Plain text like `"Task created: X"`

#### 6. `NotificationManager.java` (`com.tomorrow.service`)
**Creates, orders, deduplicates, persists, and delivers notifications**.

| Method | Description |
|---|---|
| `notifyTaskEvent(Task, Type)` | Creates notification with dedup check, priority, message; delivers via channels; persists |
| `deliver(Notification)` | Sends via all enabled channels; catches `Throwable`; records success/failure per channel |
| `markRead(String id)` / `dismiss(String id)` | State changes; persist |
| `getNotifications()` | Returns `List<Notification>` sorted by priority |
| `unreadCount()` | Count of `!read && !dismissed` |
| `register(NotificationChannel)` | Add channel to delivery map |
| `getChannels()` | `Map<String, NotificationChannel>` — `name` → channel |
| `persist()` | Saves notification list to `notifications.json` |

**Deduplication**: Key = `taskId + ":" + type + ":" + dueDate`. If a non-dismissed notification with same key exists, new one is suppressed (prevents duplicate reminders on restart).

#### 7. `ReminderScheduler.java` (`com.tomorrow.service`)
**Periodic task check while app runs**. Generates due-today/overdue reminders without per-tick spam. Daily email digest. Quiet hours. Launch reconciliation.

| Method | Description |
|---|---|
| `Clock` (inner interface) | `today()` and `now()` — `SystemClock` uses `LocalDate.now()` / `LocalTime.now()` |
| `start()` | Schedules `run()` every N minutes (default 15) via `ScheduledExecutorService` (daemon thread) |
| `stop()` / `shutdown()` | Cancels scheduler, shutdown hook |
| `check()` | Iterates pending tasks; if P1/P2/P3 and not in quiet hours → `notifyTaskEvent()` |
| `reconcileOnLaunch()` | Calls `check()` at startup without duplicate burst (dedup handles this) |
| `sendDailyDigest()` | Sends email digest once per day (tracks `lastDigestDate`); sorted by urgency; respects quiet hours |
| `inQuietHours(LocalTime)` | If `quietHoursEnabled` and current time within start-end (handles overnight span 22:00-08:00) |

**Key Policies**:
- Reminders only while app is open
- On relaunch: reconcile missed reminders without duplicate bursts
- Daily email digest (not one email per task/postponement)
- Quiet hours suppress desktop/email but in-app always retained

#### 8. `AppSettings.java` (`com.tomorrow.service`)
**Application settings**. Secrets (SMTP password) intentionally not stored.

| Field | Description |
|---|---|
| `tone` | `MessageProvider.Tone` (`HUMOROUS` or `PROFESSIONAL`) |
| `inAppEnabled`, `macOSEnabled`, `emailEnabled` | Channel toggles |
| `emailHost`, `emailPort`, `emailUsername`, `emailFrom`, `emailRecipient` | SMTP config — **no password stored** |
| `emailAuth`, `emailStarttls` | SMTP auth/TLS flags |
| `quietHoursEnabled`, `quietHoursStart`/`LocalTime`, `quietHoursEnd` | Silence period |
| `reminderCheckIntervalMinutes` | Scheduler tick interval (default 15) |
| `inQuietHours(LocalTime)` | Returns true if within quiet period; handles overnight span |
| `toMap()` / `fromMap(Map<String,Object>)` | JSON serialization/deserialization |

**Critical**: "Store no SMTP password, app password, or other secret in source control or a plain JSON settings file. Read credentials from runtime configuration/environment or an appropriate secure credential store."

#### 9. `Main.java` (`com.tomorrow.ui`)
**Minimal main entry point**. Opens Swing window on EDT, initializes storage/settings/channels.

| Key Actions | Description |
|---|---|
| `main(String[])` | Sets FlatDarwin L&F, calls `SwingUtilities.invokeLater()` |
| Creates `JsonStorage(Path.home/.local/share/tomorrow)` | Data directory |
| Creates `TaskManager`, `NotificationManager`, `AppSettings` | Core services |
| Registers channels from settings (in-app always on) | `InAppChannel`, optionally `MacOSChannel`, `EmailChannel` |
| Creates `MainFrame` | Main application window |

#### 10. `MainFrame.java` (`com.tomorrow.ui`)
**Main application window**. Dashboard, task list, action bar, notification panel.

| Component | Description |
|---|---|
| `createDashboardPanel()` | Shows total/pending/completed/overdue/postponements stats |
| `createActionBar()` | Buttons: Add Task, Postpone Selected, Complete Selected, Delete Selected, Settings |
| `createTaskCard(Task)` | Displays title, due date (overdue/due-today styling), status, postponement count |
| `refreshTaskList()` | Rebuilds task panel from `taskManager.getTasks()` |
| `showAddEditDialog(String id)` | Dialog with title, description, due date (ISO YYYY-MM-DD input); on OK: `taskManager.add()` + `notifyTaskEvent(CREATED)` |
| `showSettings()` | Simplified settings: tone, channel toggles, email config (if enabled), quiet hours, reminder interval; save re-registers channels |
| `scheduleReminders()` | Starts `ReminderScheduler` with taskManager, notifMgr, email channel, settings |

**Note**: Task cards are display-only; all actions via the bottom action bar (postpone/complete/delete with confirmation dialogs).

#### 11. `NotificationChannel.java` (`com.tomorrow.notification`)
**Common interface for all notification channels**.

| Method | Description |
|---|---|
| `String name()` | e.g., "inApp", "macOS", "email" |
| `boolean isEnabled()` / `setEnabled(boolean)` | Channel on/off switch |
| `boolean send(Notification)` | Delivers notification; returns true if accepted; **must not throw** — failures recorded via `channelStatus` |

#### 12. `InAppChannel.java` (`com.tomorrow.notification`)
**Core in-app delivery channel**.

| Method | Description |
|---|---|
| `name()` | Returns `"inApp"` |
| `isEnabled()` / `setEnabled()` | Always true by default |
| `send(Notification)` | Marks `channelStatus("inApp", true)`; returns true |

#### 13. `MacOSChannel.java` (`com.tomorrow.notification`)
**macOS desktop notification via `osascript`**.

| Method | Description |
|---|---|
| `name()` | Returns `"macOS"` |
| `isEnabled()` / `setEnabled()` | Toggles macOS notifications |
| `send(Notification)` | Runs `osascript -e "display notification ..."` with title/message; on non-Darwin OS or exception: marks `channelStatus("macOS", false)`; returns false; **never throws** — failure preserved in-app |
| `isSupported()` | Checks `uname -s` = `"Darwin"` |
| `escape(String)` | Basic AppleScript escaping (`\\` and `\"`) |

**Note**: "Prototype and test on target Mac. Do not verify until tested on macOS."

#### 14. `EmailChannel.java` (`com.tomorrow.notification`)
**SMTP email over Jakarta Mail/Angus Mail**.

| Method | Description |
|---|---|
| `setSmtp(String host, int port, String username, char[] password, String from, String recipient, boolean auth, boolean starttls)` | Configures SMTP — **password char[] zeroed via `clearPassword()`** |
| `isConfigured()` | Returns true if host/recipient/from all set |
| `send(Notification)` | Creates `MimeMessage`, sets from/recipient/subject/text, `Transport.send()`; on success: `channelStatus("email", true)`; on `MessagingException`: `channelStatus("email", false)`; **returns false on failure** |
| `getHost()` / `getPort()` / `getUsername()` / `getFrom()` / `getRecipient()` / `isAuth()` / `isStarttls()` | Accessors |
| **Critical**: Credentials never stored in source or plain JSON; read at runtime from UI config |

#### 15. `JsonStorageTest.java` (test) / etc.
**25 JUnit 5 tests** across 5 test classes (see Test section below).

---

## Test Suite (5 test classes, 25 tests total)

### `TaskTest.java` (7 tests)
- `defaultsToTodayAndValidTitle()`: constructor sets today, valid title
- `rejectsBlankTitle()`: `IllegalArgumentException` for `""`, `"   "`, `null`, `"  "`
- `allowsPastDueDate()`: `isOverdue()` with past date
- `postponeAddsOneDayAndIncrements()`: `postpone()` adds 1 day, keeps originalDueDate, increments counter
- `completedCannotBePostponed()`: `IllegalStateException` on completed task
- `completeStampsDate()`: `complete()` stamps date, `isOverdue()` false after complete
- `notOverdueWhenDueTodayOrFuture()`: due today or future → not overdue

### `TaskManagerTest.java` (9 tests)
- `addAndList()`: add task, verify list size=1, total=1
- `rejectsBlankTitle()`: add with blank title throws
- `postpone()`: adds day, increments counter, preserves original
- `postponeRejectsCompleted()`: throws on completed task
- `complete()`: marks complete, stamps date
- `delete()`: removes task, list empty
- `filters()`: ALL/PENDING/COMPLETED/OVERDUE correct counts
- `statsDerived()`: total=3, pending=2, completed=1, overdue=1, postponements=0
- `unknownIdThrows()`: `IllegalArgumentException` for bad ID

### `JsonStorageTest.java` (4 tests)
- `roundTripTasks()`: save/load tasks via Gson; ISO dates preserved; postponement+complete persisted
- `missingFileReturnsEmpty()`: `loadTasks()` returns `empty` if file doesn't exist
- `invalidJsonNotOverwritten()`: malformed JSON throws `StorageException`; **original file preserved**
- `emptyFileReturnsEmpty()`: empty JSON → empty list

### `PriorityCalculatorTest.java` (2 tests)
- `boundaries()`: P1-P5 boundary dates against fixed `today = LocalDate.of(2026,6,15)`
- `sortsP1FirstThenDueDateThenPostponement()`: verifies P1→P2→P3→P4 order; within P1, more postponements first

### `MessageProviderTest.java` (3 tests)
- `humorousAndProfessionalDiffer()`: `HUMOROUS` tone text ≠ `PROFESSIONAL` tone text
- `noImmediateRepetition()`: consecutive `dueToday()` calls differ (when multiple options available)
- `overdueIncludesDays()`: overdue message contains day count

---

## Configuration Files

### `pom.xml` (`/`)
Maven project config with:
- Dependencies: `flatlaf`, `gson`, `junit-jupiter`, `angus-mail`
- Compiler: `--release 21` (target Java 21 on JDK 27)
- `maven-surefire-plugin`: includes `**/*Test.java`
- `maven-jar-plugin`: main class `com.tomorrow.ui.Main`

### `.gitignore` (`/`)
Excludes:
- `target/`, `*.class`, `*.iml`, `.idea/`, `*.ipr`, `*.iws`, `.vscode/`, `*.swp`, `*.swo`, `~*`
- `.DS_Store`, `Thumbs.db`
- `*.json`, `data/`, `logs/`, `credentials/`, `secrets/` (runtime data — never committed)

### `docs/` (`/docs/`)
- `README.md` — User guide with badges, setup, features, email setup, limitations, demo script
- `TODO.md` — Implementation checklist (original)
- `AGENTS.md` — Project governance/guidelines
- `CODEBASE_DOCUMENTATION.md` — This file (codebase reference)

---

## Build & Run Commands

```bash
# Compile
mvn compile

# Run all tests
mvn test

# Full build + test
mvn clean test

# Build jar
mvn clean package

# Run the application
java -jar target/tomorrow-1.0.0.jar
```

---

## Architecture Summary (per AGENTS.md)

```
Model Layer         → Task (data + derived state)
Service Layer       → TaskManager, PriorityCalculator, MessageProvider,
                    NotificationManager, ReminderScheduler, AppSettings
Storage Layer       → JsonStorage (Gson + LocalDate adapter)
Notification Layer  → Notification, NotificationChannel,
                    InAppChannel, MacOSChannel, EmailChannel
UI Layer            → Main, MainFrame, dashboard, task cards, dialogs
```

**Key Principles**:
- Small, single-purpose classes
- Beginner-readable code
- UI event handlers call services (not business rules directly)
- Safe file writes (temp + atomic move)
- Long-running ops (SMTP) off EDT
- No databases/backends/servers/auth in this version
- Notifications only while app is open; reconcile on relaunch without duplicate bursts
- Prioritize working tested core over optional effects

---

## Email Setup (Runtime, No Secrets in Source)

1. Run app → Settings → Enable "Email notifications"
2. Enter: Host, Port, Username, From, Recipient, Auth, StartTLS
3. Credentials: **never stored in source or plain JSON**; entered each session via UI
4. Daily digest sent once/day, sorted by urgency (P1→P5), respecting quiet hours
5. Failures recorded per-channel, separate from in-app delivery

**Critical**: "Keep SMTP credentials out of source code, Git, logs, and plain JSON settings file."

---

## Notification Priority Order (P1→P5)

| Priority | Condition |
|---|---|
| P1 | Overdue by 3+ days (`days <= -3`) |
| P2 | Overdue by 1-2 days (`days` in `[-2, -1]`) |
| P3 | Due today (`days == 0`) |
| P4 | Due in 1-3 days (`days` in `[1, 3]`) |
| P5 | Due in 4+ days (`days >= 4`) |

**Sort order**: P1 first → earliest due date → higher postponement count (tie-breaker only).

**Postponement count**: May be used as tie-breaker or modest urgency adjustment, but **must not override** the P1→P5 tier rules.

---

## Quiet Hours

- Configurable start/end (e.g., 22:00-08:00, overnight span supported)
- `inQuietHours(LocalTime)`: returns true if within period
- **Suppresses**: desktop notifications, email digests
- **Retains**: in-app notifications (always visible)
- `quietHoursEnabled` toggle in Settings

---

## Data Persistence Locations

```
Runtime data directory: ~/.local/share/tomorrow/
  - tasks.json        (task list + stats)
  - notifications.json (notification history + read/dismiss state)
  - settings.json     (tone, channels, email config, quiet hours, interval)
```

**All three files are generated on first launch.** Directory created automatically. None are committed to Git (covered by `.gitignore`).

---

## Demo Script (5-minute partner explanation)

1. **Add task**: "Submit assignment" due tomorrow → in-app notification appears
2. **Postpone**: Due date moves 1 day forward; postponement count → +1
3. **Complete**: Task marked complete; completion date stamped
4. **Filter**: View → Overdue → shows only overdue tasks
5. **Settings**: Toggle channels, configure email (optional), set quiet hours (e.g., 23:00-07:00)
6. **Restart app**: Tasks and settings restore; no duplicate notifications (dedup verified)

**Architecture talking points**:
- Task model + validation
- TaskManager operations + auto-save
- JsonStorage safe read/write with ISO dates
- PriorityCalculator P1-P5 tiers
- MessageProvider humorous/professional
- NotificationManager create/deduplicate/persist/deliver
- ReminderScheduler periodic check + quiet hours + daily digest
- Channels: inApp always, macOS (prototype), email (opt-in)

---
*Documentation generated as part of the Tomorrow project completion.*