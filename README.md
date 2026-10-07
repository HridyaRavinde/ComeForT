# ◈ ComeFort — Developer Life OS

> **Fast. Local-First. Instant Capture. Built for developers.**

ComeFort is an offline-first productivity and task operating system designed specifically for software engineers. Instead of heavy, slow cloud web apps that lag behind your thought process, ComeFort gives you instantaneous capture, structured organization, and a dual-interface architecture: an ultra-fast **CLI** and a modern **JavaFX Desktop GUI**, both powered by the exact same shared domain and SQLite persistence engine.

---

## 🏗️ Architecture

ComeFort uses a pragmatic, production-grade **Layered Architecture** with clean separation of concerns and a unified composition root (`AppContext`):

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   ┌──────────────────────────┐  ┌──────────────────┐   │
│   │    CLI (picocli 4.7)     │  │  JavaFX Desktop  │   │
│   └─────────────┬────────────┘  └────────┬─────────┘   │
└─────────────────┼────────────────────────┼─────────────┘
                  │                        │
┌─────────────────▼────────────────────────▼─────────────┐
│             ApplicationContext (Composition Root)      │
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
│              Persistence & Migrations                  │
│    DatabaseManager (SQLite WAL mode & Foreign Keys)    │
│    MigrationRunner (Transactional schema_migrations)   │
│    Stored locally at ~/.comefort/comefort.db           │
└────────────────────────────────────────────────────────┘
```

---

## ✨ Features

- **⚡ Instant Quick Capture & Processing**: Capture raw thoughts to inbox (`cmf c "..."`). Convert captures into tasks, notes, or ideas with full metadata (`cmf inbox convert <id> --to <task|note|idea>`).
- **👯 Twin Commands (`cmf` and `comefort`)**: Use either `cmf` or `comefort` interchangeably — exactly like `clear` and `cls`. Both commands share identical subcommands, options, and behavior.
- **🚀 Project Workspaces**: Group tasks, notes, and activity under distinct projects with zero N+1 queries.
- **☐ Task Management**: Full task lifecycle with priorities (`low`, `medium`, `high`, `critical`), due dates (`today`, `tomorrow`, `yyyy-MM-dd`), clearing due dates (`--clear-due`), and status transitions (`open`, `in_progress`, `done`, `archived`).
- **📝 Project & Standalone Notes**: Markdown-ready note-taking tied to projects or standalone thoughts.
- **🔎 Cross-Entity Keyword Search**: Multi-field indexed search across tasks, projects, notes, and inbox items with disambiguation guards.
- **🔥 Today Dashboard**: Real-time overview of overdue tasks, tasks due today, high-priority work, and inbox counters.
- **🖥️ Dual CLI & JavaFX GUI**: Every CLI command, flag, and option is mirrored 1:1 in the GUI with dynamic CLI preview generation.

---

## 💻 CLI Reference

> **Tip:** You can use `cmf` or `comefort` interchangeably for every command below.

### Quick Capture & Inbox Processing
```bash
cmf c "Refactor authentication service"           # Capture thought to inbox
cmf c "task: Migrate database schema"             # Auto-categorized as task
cmf c "idea: Neural network pipeline"             # Auto-categorized as idea
cmf c "note: Redis caching strategies"            # Auto-categorized as note
cmf inbox                                         # Review unprocessed inbox items
cmf inbox convert <id> --to task -p HoneyChain -d tomorrow --priority high
cmf inbox convert <id> --to note -p HoneyChain
cmf inbox convert <id> --to idea
cmf inbox done <id>                               # Mark capture processed / archive
cmf inbox delete <id>                             # Delete capture
```

### Projects
```bash
cmf project add HoneyChain --desc "Supply chain on-chain" --path "C:\HoneyChain"
cmf project list                                  # List all projects with task/note metrics
cmf project show HoneyChain                       # Detailed view with open tasks & recent logs
```

### Tasks
```bash
cmf task add "Implement OAuth2 flow" -p HoneyChain -d today --priority high
cmf task list --project HoneyChain --status open --priority high
cmf task done "OAuth2"                            # Matches short ID or title fragment
cmf task edit <id> -t "New Title" --desc "Details" --priority critical -d tomorrow -s in_progress
cmf task edit <id> --clear-due                    # Remove due date from task
cmf task delete <id>
```

### Notes
```bash
cmf note add "Architecture decisions" -p HoneyChain -c "Chose event sourcing for audits"
cmf note list -p HoneyChain
cmf note show "Architecture"
cmf note edit <id> -t "Updated decisions" -c "New details"
```

### Search, Today & Status
```bash
cmf search "OAuth"                               # Searches across tasks, projects, notes, inbox
cmf today                                        # Daily dashboard (overdue, due today, high priority)
cmf status                                       # Global counts & recent activity log
cmf gui                                          # Launches the desktop GUI
```

---

## 🖥️ Launching the GUI

Launch the desktop interface directly from the CLI:
```bash
cmf gui
# Or:
comefort gui
# Or flag style:
cmf --gui
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
# Or using the dev runner:
.\dev today
.\dev gui
```

### Building Distribution Binaries

ComeFort can be packaged into two types of ready-to-distribute public packages:

#### 1. 🪟 Native Standalone Package (Zero-Prerequisites — No Java Needed)
Generates a standalone Windows distribution with `ComeFort.exe` (GUI), `comefort.exe` / `cmf.exe` (CLI twins), and a bundled stripped runtime:
```bash
./gradlew packageNativeZip
```
Output: `build/distributions/ComeFort-v0.1.0-windows-x64.zip`
- **GUI**: Double-click `ComeFort.exe` — launches the desktop app directly.
- **CLI**: Run `cmf.exe <command>` or `comefort.exe <command>` in PowerShell / CMD without needing Java installed on the machine.

#### 2. 📦 Portable Distribution ZIP (Lightweight ~24MB)
Generates the cross-platform bundle containing batch launchers and scripts:
```bash
./gradlew distZip
```
Output: `build/distributions/comefort-0.1.0.zip`
Inside the extracted folder:
- **`ComeFort-GUI.bat`**: Double-click to launch the GUI immediately.
- **`ComeFort-CLI.bat`**: Double-click to launch an interactive terminal with `cmf` and `comefort` pre-configured.
- **`install.bat`**: 1-click setup that adds `cmf` and `comefort` to the user's system `PATH` and creates a Desktop shortcut.
- **`uninstall.bat`**: 1-click clean removal.

