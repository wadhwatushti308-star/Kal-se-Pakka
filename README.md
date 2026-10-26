# 🌟 Tomorrow - Java Swing Task Manager

![Tomorrow Banner](https://raw.githubusercontent.com/anomalyco/opencode/main/assets/logo.svg)

> A beginner-friendly Java desktop task manager with a humorous procrastination theme.
> Perfect for BCA students and demonstrable in a short project timeline.

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk)](https://openjdk.java.net/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-C71A36?style=for-the-badge&logo=apache-maven)](https://maven.apache.org/)
[![FlatLaf](https://img.shields.io/badge/FlatLaf-UI%20Styling-0052CC?style=for-the-badge)](https://www.formdev.com/flatlaf/)
[![Gson](https://img.shields.io/badge/Gson-JSON-413C55?style=for-the-badge)](https://github.com/google/gson)
[![JUnit 5](https://img.shields.io/badge/JUnit-5-%23296D27?style=for-the-badge&logo=junit5)](https://junit.org/junit5/)
[![License: MIT](https://img.shields.io/badge/License-MIT-000000?style=for-the-badge)](https://opensource.org/licenses/MIT)

---

## 📋 Project Overview

**Tomorrow** is a Java Swing-based task manager that helps users track tasks with a humorous procrastination twist. Built as a semester project for BCA students, it demonstrates core Java concepts, Swing GUI development, JSON persistence, and notification systems.

**Key Philosophy**: Keep it simple, testable, and beginner-friendly. No database, no backend, no authentication — just a clean desktop application.

### Core Features
- ✅ Create, edit, complete, postpone, and delete tasks
- ✅ Filter by: All / Pending / Completed / Overdue
- ✅ Automatic statistics derived from task data (not independent counters)
- ✅ ISO-8601 date handling in JSON via Gson
- ✅ In-app notification center with read/dismiss state
- ✅ Priority engine (P1-P5) based on deadline proximity
- ✅ Humorous vs professional notification tone selection
- ✅ Quiet hours for optional desktop/email reminders
- ✅ macOS desktop notification channel (prototype)
- ✅ Email channel with SMTP (opt-in, credentials at runtime)
- ✅ Persistent data across application restarts
- ✅ Duplicate reminder prevention on restart
- ✅ Safe file handling (never silently overwrite malformed JSON)

---

## 🛠️ Prerequisites

| Requirement | Version |
|---|---|
| **Java JDK** | 21 (or 17+ compatible) |
| **Maven** | 3.6+ |
| **RAM** | 256 MB minimum |
| **OS** | Linux, macOS, or Windows (with JDK 21) |

### Java Version Note
This project targets **Java 21** but runs on Java 17+ with `--release 21` compilation. Uses `java.time.LocalDate` for all date handling.

---

## ▶️ Installation & Running

```bash
# 1. Clone the repository
git clone https://github.com/your-username/tomorrow.git
cd tomorrow

# 2. Build with Maven
mvn clean package

# 3. Run the application
java -jar target/tomorrow-1.0.0.jar
```

### First Launch
- On first run, the app creates `~/.local/share/tomorrow/` directory
- Default settings are loaded (humorous tone, in-app notifications enabled)
- No tasks exist — add your first task!

### Development Setup
```bash
# Compile only
mvn compile

# Run tests
mvn test

# Run in development mode (skips jar creation)
mvn javafx:run  # if JavaFX enabled, otherwise:
mvn exec:java -Dexec.mainClass="com.tomorrow.ui.Main"
```

---

## 📦 Project Structure

```
tomorrow/
├── src/main/java/com/tomorrow/      # Main source code
│   ├── model/                       # Task data model
│   ├── service/                     # Business logic
│   ├── storage/                     # JSON persistence (Gson)
│   ├── notification/                # Notification types & channels
│   └── ui/                          # Swing user interface
│
├── src/test/java/                   # JUnit 5 test suite (25 tests!)
├── pom.xml                          # Maven configuration
├── .gitignore                       # Git exclusions
├── README.md                        # You are here!
└── TODO.md                          # Implementation checklist
```

### Key Package Design (per AGENTS.md)
```
com.tomorrow.model    → Task, notification models
com.tomorrow.service  → TaskManager, PriorityCalculator, MessageProvider,
                        NotificationManager, ReminderScheduler, AppSettings
com.tomorrow.storage  → JsonStorage (Gson-based)
com.tomorrow.notification → Notification, NotificationChannel,
                            InAppChannel, MacOSChannel, EmailChannel
com.tomorrow.ui       → Main, MainFrame, DashboardPanel, task cards, dialogs
```

---

## 🧱 Core Architecture

### Model-Layer (`com.tomorrow.model`)
- **`Task`** (`Task.java:1`): Single task entity with ID, title, description, creation/original/due dates, postponement count, completion status, completion date.
  - Validation: title cannot be blank; past due dates allowed; defaults to today
  - `postpone()`: adds exactly one calendar day, increments counter, rejects completed tasks
  - `complete()`: stamps completion date, marks completed
  - `isOverdue(today)`: `!completed && dueDate < today`

### Service Layer (`com.tomorrow.service`)
- **`TaskManager`** (`TaskManager.java`): Operations — add, edit, complete, postpone, delete, filter, stats. Auto-saves via JsonStorage callback.
- **`PriorityCalculator`** (`PriorityCalculator.java`): Computes P1-P5 priority from deadline tiers:
  - P1: overdue by 3+ days
  - P2: overdue by 1–2 days
  - P3: due today
  - P4: due in 1–3 days
  - P5: due in 4+ days
  - Sort: P1 first → earliest due date → higher postponement count (tie-breaker)
- **`MessageProvider`** (`MessageProvider.java`): Humorous (default) or professional tone messages. Avoids repeating exact same message for consecutive reminders.
- **`NotificationManager`** (`NotificationManager.java`): Creates, orders (by priority), deduplicates, persists, and delivers notifications via channels.
- **`ReminderScheduler`** (`ReminderScheduler.java`): Periodic task check while app runs. Generates due-today/overdue reminders without per-tick notifications. Daily email digest. Quiet hours respected. Launch reconciliation without duplicate bursts.
- **`AppSettings`** (`AppSettings.java`): Tone, channel toggles, email config (no secrets stored), quiet hours, reminder interval.

### Storage Layer (`com.tomorrow.storage`)
- **`JsonStorage`** (`JsonStorage.java`): Gson-based load/save with explicit `LocalDate` serializers. Safe write (temp file → atomic move). Preserves existing file on write failure. Reports malformed JSON clearly (does NOT silently overwrite).

### Notification Layer (`com.tomorrow.notification`)
- **`Notification`** (`Notification.java`): ID, optional task ID, type, priority, title, message, timestamp, read/dismissed state, deduplication key, per-channel delivery status.
- **`NotificationChannel`** (`NotificationChannel.java`): Common interface — `name()`, `isEnabled()`, `setEnabled()`, `send()`.
- **`InAppChannel`** (`InAppChannel.java`): Core channel — always enabled, records delivery in-app.
- **`MacOSChannel`** (`MacOSChannel.java`): macOS desktop via `osascript`. Gracefully handles non-macOS, permissions, errors. Every notification preserved in-app even if desktop delivery fails.
- **`EmailChannel`** (`EmailChannel.java`): Jakarta Mail/Angus Mail SMTP. Credentials passed at runtime (never stored). Sends off EDT. Handles auth/network errors.

### UI Layer (`com.tomorrow.ui`)
- **`Main`** (`Main.java`): Entry point, opens Swing window on EDT, initializes storage/settings/channels.
- **`MainFrame`** (`MainFrame.java`): Main application window — dashboard, task list area, action bar, notification panel.
- Task cards display title, due date (with overdue/due-today styling), status, postponement count.
- Add/edit task dialog: title, optional description, due date (ISO format YYYY-MM-DD).
- Actions: Postpone/Complete/Delete (all confirmed via `JOptionPane`).
- Filters: All / Pending / Completed / Overdue.
- Settings panel: channel toggles, email config, tone, quiet hours, reminder interval.

---

## 🧪 Test Suite (25 Tests)

All tests pass with `mvn test`. Coverage includes:

| Test Class | Tests |
|---|---|
| `TaskTest` | Validation, defaults, postponement, completion, overdue |
| `TaskManagerTest` | Add/list, blank title, postponement, completion, deletion, filters, stats, unknown ID |
| `JsonStorageTest` | Round-trip, missing file, invalid JSON (preserved!), empty file |
| `PriorityCalculatorTest` | Priority boundaries (P1-P5), sorting behavior |
| `MessageProviderTest` | Humorous vs professional, no repetition, overdue message content |

Run all: `mvn test`

Run specific: `mvn test -Dtest='TaskTest,TaskManagerTest'`

---

## 📧 Email Configuration

Email is **opt-in** and requires SMTP setup through the UI:

1. Run the app → click **Settings**
2. Enable **"Email notifications"**
3. Enter:
   - **Host** — e.g., `smtp.gmail.com`
   - **Port** — typically `587`
   - **Username** — your SMTP account username
   - **From** — sender email address
   - **Recipient** — where notifications go
   - **Auth** — whether authentication is required
   - **StartTLS** — usually `true`
4. Click **Save**

### ⚠️ Important Security Notes
- **Credentials are never stored in source code or Git**
- **No SMTP password is saved** in any plain JSON or source file
- Credentials are read at runtime from UI configuration only
- The application does not log or persist passwords
- Requires network access to SMTP server

### Daily Digest
- Sends **once per day** at most (not one email per task)
- Sorted by urgency (P1 → P5)
- Includes due dates and postponement counts
- Respects quiet hours — suppressed during configured period
- At most one digest per day (tracked internally)

---

## 🎛️ Settings & Configuration

| Setting | Description |
|---|---|
| **Tone** | Humorous (default) or Professional |
| **In-App** | Enables in-center notifications (always on by default) |
| **macOS** | Enables desktop notifications (macOS only; failures preserved in-app) |
| **Email** | Enables SMTP email channel (opt-in, config above) |
| **Quiet Hours** | Silence period (e.g., 22:00–08:00); in-app notifications **always retained** |
| **Reminder Interval** | How often the scheduler checks tasks (minutes, default 15) |

### Persistence
- Settings saved to `~/.local/share/tomorrow/settings.json`
- Channels re-registered on startup from settings
- Safe write patterns applied (all JSON files)

---

## 🐛 Known Limitations & Edge Cases

| Limitation | Workaround |
|---|---|
| **No database**: data lives in JSON files | Ideal for desktop app; export/import via JSON if needed |
| **macOS notifications**: unverified on non-macOS | On Linux/Windows, delivery gracefully fails; notification stays in-app |
| **Email**: requires network + valid SMTP credentials | Configure in Settings; errors recorded separately from in-app delivery |
| **Quiet hours**: only suppresses desktop/email | In-app notifications always visible — no missed alerts |
| **Scheduler**: only runs while app is open | On relaunch, reconciles missed reminders without duplicate bursts |
| **Gson version**: explicit `LocalDate` adapter required | Configured in `JsonStorage`; ISO-8601 format always |

---

## 📸 Screenshots *(add actual screenshots to `/assets/`)*

![Dashboard View](https://via.placeholder.com/800x400/2D3748/FFFFFF?text=Tomorrow+Dashboard)
![Add Task Dialog](https://via.placeholder.com/800x500/2D3748/FFFFFF?text=Add+Task+Dialog)
![Settings Panel](https://via.placeholder.com/800x600/2D3748/FFFFFF?text=Settings+Panel)
![Notification Center](https://via.placeholder.com/800x500/2D3748/FFFFFF?text=Notification+Center)

> *Add actual screenshots by running the app and exporting, or add to `src/main/resources/assets/`*

---

## 🤝 Contributing

Since this is a student project, contributions are welcome but should follow the existing code style:

1. **Fork** the repository
2. **Branch** (`git checkout -b feature/foo`)
3. **Write tests** first if adding new behavior (per existing pattern)
4. **Commit** with descriptive messages
5. **Push** and **open a Pull Request**

### Development Commands
```bash
# Build
mvn clean package

# Test
mvn test

# Full verification
mvn clean test package

# Checkstyle/licensing (if configured)
mvn verify
```

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

> **Permission is hereby granted, free of charge, to any person obtaining a copy**
> of this software and associated documentation files (the "Software"), to deal
> in the Software without restriction, including without limitation the rights
> to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
> copies of the Software, and to permit persons to whom the Software is
> furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in all
> copies or substantial portions of the Software.

---

## 👥 Partners & Demo

### Demo Script (both partners should explain)

1. **Partner A**: "I'll add a task 'Submit assignment' due tomorrow"
2. **Partner B**: "I'll postpone it — see the date move forward and counter increment"
3. **Partner A**: "I'll complete it — verify completion date stamps"
4. **Partner B**: "I'll filter by overdue — show me the overdue tasks"
5. **Partner A**: "I'll open Settings — demonstrate channel toggles and email config"
6. **Partner B**: "I'll restart the app — show data persists and no duplicate notifications"

### Architecture Overview (10-minute explanation)

- **Task model**: data class with validation and derived state (`isOverdue`)
- **TaskManager**: orchestrates operations, validates, persists via JsonStorage
- **PriorityCalculator**: maps due date → P1-P5 tier, sorts notifications
- **MessageProvider**: text selection with tone and repetition avoidance
- **NotificationManager**: creates, deduplicates, persists, delivers via channels
- **ReminderScheduler**: periodic checks, quiet hours, daily digest, launch reconciliation
- **JsonStorage**: safe JSON load/save with LocalDate adapter
- **UI**: Swing on EDT, FlatLaf styling, action handlers call services

---

## 📬 Contact & Support

- **Repository**: [GitHub](https://github.com/your-username/tomorrow)
- **Issue Tracker**: Use GitHub Issues for bugs and feature requests
- **Email**: For support questions, reach out via the repository

---

## 🙏 Acknowledgments

- Inspired by BCA semester project requirements
- FlatLaf for modern Swing aesthetics
- Gson for JSON persistence with `LocalDate` support
- JUnit 5 for test framework
- Angus Mail for simplified SMTP
- AGENTS.md guidance for architecture decisions

---

*Built with ☕ and ☕☕ — keep tasks managed, procrastination minimized.*

---

*Last updated: October 2026*