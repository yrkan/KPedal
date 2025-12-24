-- ============================================
-- KPedal Database Schema
-- Cloudflare D1 (SQLite)
-- ============================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    picture TEXT,
    google_id TEXT UNIQUE NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Index for Google ID lookup
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);

-- Devices table (Karoo devices linked to users)
CREATE TABLE IF NOT EXISTS devices (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL DEFAULT 'Karoo',
    type TEXT NOT NULL DEFAULT 'karoo',
    last_sync TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for user's devices
CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id);

-- Rides table (synced from KPedal app)
CREATE TABLE IF NOT EXISTS rides (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    device_id TEXT NOT NULL,
    timestamp INTEGER NOT NULL,          -- Unix timestamp in milliseconds
    duration_ms INTEGER NOT NULL,        -- Ride duration in milliseconds

    -- Balance metrics (percentages 0-100)
    balance_left REAL NOT NULL,
    balance_right REAL NOT NULL,

    -- Torque Effectiveness (percentages 0-100)
    te_left REAL NOT NULL,
    te_right REAL NOT NULL,

    -- Pedal Smoothness (percentages 0-100)
    ps_left REAL NOT NULL,
    ps_right REAL NOT NULL,

    -- Time in zone (percentages 0-100)
    zone_optimal INTEGER NOT NULL,
    zone_attention INTEGER NOT NULL,
    zone_problem INTEGER NOT NULL,

    -- Overall score (0-100)
    score INTEGER NOT NULL,

    -- Metadata
    notes TEXT,
    rating INTEGER,                       -- User rating 1-5
    created_at TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for rides
CREATE INDEX IF NOT EXISTS idx_rides_user_id ON rides(user_id);
CREATE INDEX IF NOT EXISTS idx_rides_timestamp ON rides(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_rides_device_id ON rides(device_id);

-- Unique constraint to prevent duplicates (same ride from same device)
CREATE UNIQUE INDEX IF NOT EXISTS idx_rides_unique
ON rides(user_id, device_id, timestamp);

-- Drill results (synced from app)
CREATE TABLE IF NOT EXISTS drill_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    device_id TEXT NOT NULL,
    drill_id TEXT NOT NULL,
    drill_name TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    duration_ms INTEGER NOT NULL,
    score INTEGER NOT NULL,              -- 0-100
    created_at TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for drill results
CREATE INDEX IF NOT EXISTS idx_drill_results_user_id ON drill_results(user_id);
CREATE INDEX IF NOT EXISTS idx_drill_results_timestamp ON drill_results(timestamp DESC);

-- Achievements (synced from app)
CREATE TABLE IF NOT EXISTS achievements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    achievement_id TEXT NOT NULL,
    unlocked_at INTEGER NOT NULL,        -- Unix timestamp
    created_at TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, achievement_id)
);

-- Index for achievements
CREATE INDEX IF NOT EXISTS idx_achievements_user_id ON achievements(user_id);
