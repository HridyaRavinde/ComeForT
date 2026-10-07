-- ComeFort V0.1 Database Schema
-- All timestamps are ISO-8601 strings (SQLite has no native datetime type)
-- IDs are UUIDs generated in Java via java.util.UUID

CREATE TABLE IF NOT EXISTS projects (
    id          TEXT PRIMARY KEY,
    name        TEXT NOT NULL UNIQUE,
    description TEXT,
    path        TEXT,
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS tasks (
    id           TEXT PRIMARY KEY,
    project_id   TEXT REFERENCES projects(id) ON DELETE SET NULL,
    title        TEXT NOT NULL,
    description  TEXT,
    status       TEXT NOT NULL DEFAULT 'OPEN',
    priority     TEXT NOT NULL DEFAULT 'MEDIUM',
    due_date     TEXT,
    created_at   TEXT NOT NULL,
    updated_at   TEXT NOT NULL,
    completed_at TEXT
);

CREATE TABLE IF NOT EXISTS notes (
    id         TEXT PRIMARY KEY,
    project_id TEXT REFERENCES projects(id) ON DELETE SET NULL,
    title      TEXT NOT NULL,
    content    TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS captures (
    id         TEXT PRIMARY KEY,
    project_id TEXT REFERENCES projects(id) ON DELETE SET NULL,
    content    TEXT NOT NULL,
    type       TEXT NOT NULL DEFAULT 'AUTO',
    processed  INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS activity_log (
    id          TEXT PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id   TEXT NOT NULL,
    action      TEXT NOT NULL,
    summary     TEXT,
    created_at  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS config (
    key   TEXT PRIMARY KEY,
    value TEXT
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_tasks_project      ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status        ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date      ON tasks(due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_priority      ON tasks(priority);
CREATE INDEX IF NOT EXISTS idx_notes_project       ON notes(project_id);
CREATE INDEX IF NOT EXISTS idx_captures_processed  ON captures(processed);
CREATE INDEX IF NOT EXISTS idx_captures_project    ON captures(project_id);
CREATE INDEX IF NOT EXISTS idx_activity_entity     ON activity_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_activity_created    ON activity_log(created_at);
