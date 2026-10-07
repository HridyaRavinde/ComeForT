# ◈ ComeFort — Developer Life OS

> **Fast. Local-First. Zero-Latency. Built for developers.**

ComeFort is an offline-first productivity and task operating system designed specifically for software engineers. Instead of heavy, slow cloud web apps that lag behind your thought process, ComeFort gives you instantaneous capture, structured organization, and a dual-interface architecture: an ultra-fast **CLI** and a modern **JavaFX Desktop GUI**, both powered by the exact same shared domain and SQLite persistence engine.

---

## 🏗️ Architecture

ComeFort follows strict **Clean Architecture** principles with clean separation of concerns and dependency inversion:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   ┌──────────────────────────┐  ┌──────────────────┐   │
│   │    CLI (picocli 4.7)     │  │ JavaFX 22.0 GUI  │   │
│   └─────────────┬────────────┘  └────────┬─────────┘   │
└─────────────────┼────────────────────────┼─────────────┘
                  │                        │
┌─────────────────▼────────────────────────▼─────────────┐
│                    Service Layer                       │
│  [TaskService] [ProjectService] [NoteService]          │
│  [CaptureService] [TodayService] [SearchService]       │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                   Repository Layer                     │
│  [TaskRepository] [ProjectRepository] [NoteRepository] │
│  [CaptureRepository] [ActivityRepository]              │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                   Persistence Layer                    │
│    DatabaseManager (SQLite with WAL mode & Foreign Keys)│
│    Stored locally at ~/.comefort/comefort.db           │
└────────────────────────────────────────────────────────┘
```

---

## ✨ Features

- **⚡ Instant Quick Capture**: Capture fleeting thoughts into an unprocessed inbox instantly with zero friction (`cf c "..."`). Supports intelligent prefixes: `task:`, `note:`, `idea:`.
- **🚀 Project Workspaces**: Group tasks, notes, and activity under distinct projects with filesystem path references.
- **☐ Task Management**: Full task lifecycle with priorities (`low`, `medium`, `high`, `critical`), due dates (`today`, `tomorrow`, `yyyy-MM-dd`), and status transitions (`open`, `in_progress`, `done`, `archived`).
- **📝 Project & Standalone Notes**: Markdown-ready note-taking tied to projects or standalone thoughts.
- **🔎 Cross-Entity Full-Text Search**: Instant search across tasks, projects, notes, and captures.
- **🔥 Today Dashboard**: Real-time overview of overdue tasks, tasks due today, high-priority work, and inbox counters.
- **🖥️ Dual CLI & JavaFX GUI**: Every CLI command, flag, and option is mirrored 1:1 in the GUI with dynamic CLI preview generation.

---

## 💻 CLI Reference

### Quick Capture
```bash
cf c "Refactor authentication service"           # Capture thought to inbox
cf c "task: Migrate database schema"             # Auto-categorized as task
cf c "idea: Neural network pipeline"             # Auto-categorized as idea
cf c "note: Redis caching strategies"            # Auto-categorized as note
cf inbox                                         # Review unprocessed inbox items
```

### Projects
```bash
cf project add HoneyChain --desc "Supply chain on-chain" --path "C:\HoneyChain"
cf project list                                  # List all projects with task/note metrics
cf project show HoneyChain                       # Detailed view with open tasks & recent logs
```

### Tasks
```bash
cf task add "Implement OAuth2 flow" -p HoneyChain -d today --priority high
cf task list --project HoneyChain --status open --priority high
cf task done "OAuth2"                            # Matches short ID or title fragment
cf task edit <id> -t "New Title" --desc "Details" --priority critical -d tomorrow -s in_progress
cf task delete <id>
```

### Notes
```bash
cf note add "Architecture decisions" -p HoneyChain -c "Chose event sourcing for audits"
cf note list -p HoneyChain
cf note show "Architecture"
cf note edit <id> -t "Updated decisions" -c "New details"
```

### Search, Today & Status
```bash
cf search "OAuth"                               # Searches across tasks, projects, notes, inbox
cf today                                        # Daily dashboard (overdue, due today, high priority)
cf status                                       # Global counts & recent activity log
cf gui                                          # Launches the desktop GUI
```

---

## 🖥️ Launching the GUI

Launch the desktop interface directly from the CLI:
```bash
cf gui
# Or:
cf --gui
```

The GUI includes:
- **Sidebar Navigation**: Instant switching between Today, Inbox, Projects, Tasks, Notes, Search, and Status.
- **Command Form Builder**: Dynamic form generation where every single CLI flag and parameter has an interactive UI control.
- **CLI Sync Preview**: Real-time display of the equivalent CLI command being executed by the GUI action.
- **Dark & Light Mode**: Clean, developer-tailored theme system.

---

## 🛠️ Build and Test

### Prerequisites
- JDK 22 or higher
- Gradle 9.6.0 (Gradle wrapper included)

### Running Tests
Execute the comprehensive JUnit 5 integration test suite:
```bash
./gradlew test
```

### Running CLI Locally
```bash
./gradlew run --args="--help"
./gradlew run --args="today"
```

### Building Distribution Binaries

ComeFort can be packaged into two types of ready-to-distribute public packages:

#### 1. 🪟 Native Standalone Package (Zero-Prerequisites — No Java Needed)
Generates a standalone Windows distribution with `ComeFort.exe` (GUI), `cf.exe` (CLI), and a bundled stripped runtime:
```bash
./gradlew packageNativeZip
```
Output: `build/distributions/ComeFort-v0.1.0-windows-x64.zip`
- **GUI**: Double-click `ComeFort.exe` — launches the desktop app directly.
- **CLI**: Run `cf.exe <command>` in PowerShell / CMD without needing Java installed on the machine.

#### 2. 📦 Portable Distribution ZIP (Lightweight ~24MB)
Generates the cross-platform bundle containing batch launchers and scripts:
```bash
./gradlew distZip
```
Output: `build/distributions/comefort-0.1.0.zip`
Inside the extracted folder:
- **`ComeFort-GUI.bat`**: Double-click to launch the GUI immediately.
- **`ComeFort-CLI.bat`**: Double-click to launch an interactive terminal with `cf` pre-configured.
- **`install.bat`**: 1-click setup that adds `cf` to the user's system `PATH` and creates a Desktop shortcut.
- **`uninstall.bat`**: 1-click clean removal.

