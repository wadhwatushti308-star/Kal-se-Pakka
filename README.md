# Tomorrow - Task Manager

A beginner-friendly Java Swing task manager with a humorous procrastination theme.

## Prerequisites

- **Java 21** (JDK 21 or newer)
- **Maven 3.6+**
- **FlatLaf** (look-and-feel library, included as dependency)

## Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd eventManager

# Build with Maven
mvn clean package

# Run the application
java -jar target/tomorrow-1.0.0.jar
```

## Data Location

Application data is stored in:
```
~/.local/share/tomorrow/
  - tasks.json    (task data)
  - notifications.json (notification history)
  - settings.json   (application settings)
```

These files are generated automatically on first launch. The data directory is **not** committed to version control (see `.gitignore`).

## .gitignore

The project includes a `.gitignore` that excludes:
- Build outputs (`target/`)
- IDE files (`*.iml`, `.idea/`)
- Runtime data (`*.json`, `data/`, `logs/`, `credentials/`, `secrets/`)
- OS files (`.DS_Store`, `Thumbs.db`)

## Features

### Task Management
- **Create tasks** with title (required), optional description, and due date (defaults to today)
- **Edit tasks** - modify title, description, and due date
- **Complete tasks** - marks task complete with completion date; completed tasks cannot be postponed
- **Postpone tasks** - moves due date forward by exactly one calendar day; increments postponement count; rejects postponement of completed tasks
- **Delete tasks** - with confirmation
- **Filter tasks** - All / Pending / Completed / Overdue
- **Statistics** - total, pending, completed, overdue, and postponement counts derived from current task data

### Notification System
- **In-app notifications** with read/unread and dismiss states; history prevented across restarts
- **Priority calculator** (P1-P5 tiers based on deadline proximity):
  - P1: overdue by 3+ days
  - P2: overdue by 1-2 days
  - P3: due today
  - P4: due in 1-3 days
  - P5: due in 4+ days
- **Message provider** - humorous (default) or professional tone
- **Deduplication** - prevents duplicate reminders across restarts
- **Quiet hours** - configurable silence period; in-app notifications retained

### Notification Channels
- **In-app channel** (always enabled, core channel)
- **macOS desktop channel** - requires macOS target; delivery failures preserved in-app
- **Email channel** - SMTP-based; opt-in; credentials passed at runtime (never stored in source or plain JSON); sends daily digest sorted by urgency

### Settings
- Tone selection (humorous / professional)
- Channel toggles (in-app, macOS, email)
- Email configuration (host, port, username, from, recipient) - credentials entered at runtime
- Quiet hours (optional silence period; in-app notifications always retained)
- Reminder check interval

## Email Setup

Email is **opt-in** and requires SMTP configuration:

1. Run the application and go to Settings
2. Enable "Email notifications"
3. Enter SMTP settings:
   - **Host** - SMTP server address (e.g., smtp.gmail.com)
   - **Port** - typically 587
   - **Username** - SMTP account username
   - **From** - sender email address
   - **Recipient** - where notifications are sent
   - **Auth** - whether authentication is required
   - **StartTLS** - whether to use STARTTLS

**Important**: SMTP credentials are **never stored in source code or Git**. They are entered at runtime through the UI settings panel. The application does not log or persist passwords.

## Notification Limitations

- Notifications only run while the application is open. On relaunch, missed reminders are reconciled without duplicate bursts.
- macOS desktop notifications have not been verified on this platform (tested on macOS only). If desktop delivery fails, the notification remains in the in-app center.
- Email requires network access and valid SMTP credentials. Failed sends are recorded separately from in-app delivery.
- Daily email digest (rather than one email per task/postponement) - sent once per day at most, respecting quiet hours.
- Quiet hours suppress desktop and email reminders but in-app notifications are always retained.

## Demo Scenario

1. **Launch** Tomorrow - the main window opens with dashboard stats
2. **Add a task** "Buy milk" with due date tomorrow - in-app notification appears
3. **Postpone the task** - due date moves one day forward, postponement count increments
4. **Complete the task** - marked complete, notification appears
5. **Filter by Overdue** - see overdue tasks
6. **Open Settings** - toggle channels, configure email (optional), set quiet hours
7. **Restart the app** - tasks and notifications restore; no duplicate reminders
8. **Explain the architecture** - Task model, TaskManager operations, JsonStorage persistence, PriorityCalculator tiers, NotificationManager with channels, ReminderScheduler periodic checks

## Development Notes

- **No database, backend server, account system, OAuth, calendar integration, or background daemon** in this version
- Notifications run only while the app is open; on relaunch, reconcile missed reminders without duplicate bursts
- Priority tiers are strict P1-P5 based on deadline proximity; postponement count is a tie-breaker only
- Gson configured explicitly for `LocalDate` ISO-8601 serialization/deserialization in JSON
- Safe file replacement (write to temp file, then atomic move) with preservation of existing file on write failure
- All Swing updates on Event Dispatch Thread; SMTP sends off EDT
- Secrets never committed to source control or plain JSON settings files
- macOS desktop channel: prototype tested on macOS only; on other OS, failures gracefully preserved in-app
- JUnit 5 tests for validation, postponement, completion, overdue state, deletion, persistence, priority boundaries, and message selection