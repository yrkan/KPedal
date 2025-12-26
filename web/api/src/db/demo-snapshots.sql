-- ============================================
-- Demo Ride Snapshots
-- Per-minute data for timeline charts
-- ============================================

-- Helper: Generate snapshots for each ride based on duration
-- This creates realistic minute-by-minute data with variation

-- For each ride, we'll create snapshots based on duration_ms / 60000 = minutes

-- First, let's get ride IDs and create snapshots
-- We need to run this after demo-data.sql

-- Ride 1 snapshots (45 min ride)
INSERT INTO ride_snapshots (ride_id, minute_index, timestamp, balance_left, balance_right, te_left, te_right, ps_left, ps_right, power_avg, cadence_avg, hr_avg, zone_status)
SELECT
  r.id,
  m.n,
  r.timestamp + (m.n * 60000),
  49 + CAST(ABS(RANDOM() % 3) - 1 AS INTEGER),
  51 + CAST(ABS(RANDOM() % 3) - 1 AS INTEGER),
  71 + CAST(ABS(RANDOM() % 5) AS INTEGER),
  73 + CAST(ABS(RANDOM() % 5) AS INTEGER),
  20 + CAST(ABS(RANDOM() % 4) AS INTEGER),
  21 + CAST(ABS(RANDOM() % 4) AS INTEGER),
  140 + CAST(ABS(RANDOM() % 20) AS INTEGER),
  86 + CAST(ABS(RANDOM() % 6) AS INTEGER),
  122 + CAST(ABS(RANDOM() % 8) AS INTEGER),
  CASE WHEN ABS(RANDOM() % 10) < 7 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 9 THEN 'ATTENTION' ELSE 'PROBLEM' END
FROM rides r, (
  WITH RECURSIVE cnt(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM cnt WHERE n < 44)
  SELECT n FROM cnt
) m
WHERE r.user_id = 'demo-user-00000000-0000-0000-0000-000000000000'
AND r.duration_ms = 2700000;

-- Ride 2 snapshots (60 min interval training)
INSERT INTO ride_snapshots (ride_id, minute_index, timestamp, balance_left, balance_right, te_left, te_right, ps_left, ps_right, power_avg, cadence_avg, hr_avg, zone_status)
SELECT
  r.id,
  m.n,
  r.timestamp + (m.n * 60000),
  48 + CAST(ABS(RANDOM() % 4) - 1 AS INTEGER),
  52 + CAST(ABS(RANDOM() % 4) - 1 AS INTEGER),
  66 + CAST(ABS(RANDOM() % 8) AS INTEGER),
  69 + CAST(ABS(RANDOM() % 8) AS INTEGER),
  16 + CAST(ABS(RANDOM() % 6) AS INTEGER),
  18 + CAST(ABS(RANDOM() % 6) AS INTEGER),
  CASE WHEN m.n % 10 < 4 THEN 280 + CAST(ABS(RANDOM() % 40) AS INTEGER) ELSE 150 + CAST(ABS(RANDOM() % 30) AS INTEGER) END,
  90 + CAST(ABS(RANDOM() % 8) AS INTEGER),
  CASE WHEN m.n % 10 < 4 THEN 165 + CAST(ABS(RANDOM() % 12) AS INTEGER) ELSE 140 + CAST(ABS(RANDOM() % 10) AS INTEGER) END,
  CASE WHEN ABS(RANDOM() % 10) < 5 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 8 THEN 'ATTENTION' ELSE 'PROBLEM' END
FROM rides r, (
  WITH RECURSIVE cnt(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM cnt WHERE n < 59)
  SELECT n FROM cnt
) m
WHERE r.user_id = 'demo-user-00000000-0000-0000-0000-000000000000'
AND r.duration_ms = 3600000
AND r.score = 58;

-- Ride 3 snapshots (90 min endurance)
INSERT INTO ride_snapshots (ride_id, minute_index, timestamp, balance_left, balance_right, te_left, te_right, ps_left, ps_right, power_avg, cadence_avg, hr_avg, zone_status)
SELECT
  r.id,
  m.n,
  r.timestamp + (m.n * 60000),
  50 + CAST(ABS(RANDOM() % 2) - 1 AS INTEGER),
  50 + CAST(ABS(RANDOM() % 2) - 1 AS INTEGER),
  74 + CAST(ABS(RANDOM() % 4) AS INTEGER),
  75 + CAST(ABS(RANDOM() % 4) AS INTEGER),
  22 + CAST(ABS(RANDOM() % 4) AS INTEGER),
  23 + CAST(ABS(RANDOM() % 4) AS INTEGER),
  165 + CAST(ABS(RANDOM() % 15) AS INTEGER),
  84 + CAST(ABS(RANDOM() % 4) AS INTEGER),
  136 + CAST(ABS(RANDOM() % 6) AS INTEGER),
  CASE WHEN ABS(RANDOM() % 10) < 8 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 10 THEN 'ATTENTION' ELSE 'PROBLEM' END
FROM rides r, (
  WITH RECURSIVE cnt(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM cnt WHERE n < 89)
  SELECT n FROM cnt
) m
WHERE r.user_id = 'demo-user-00000000-0000-0000-0000-000000000000'
AND r.duration_ms = 5400000
AND r.score = 85;

-- Today's ride (Ride 27) - 90 min with detailed snapshots
INSERT INTO ride_snapshots (ride_id, minute_index, timestamp, balance_left, balance_right, te_left, te_right, ps_left, ps_right, power_avg, cadence_avg, hr_avg, zone_status)
SELECT
  r.id,
  m.n,
  r.timestamp + (m.n * 60000),
  CASE
    WHEN m.n < 10 THEN 50 + CAST(ABS(RANDOM() % 2) - 1 AS INTEGER)  -- Warmup: balanced
    WHEN m.n < 30 THEN 49 + CAST(ABS(RANDOM() % 3) - 1 AS INTEGER)  -- Main set 1
    WHEN m.n < 50 THEN 50 + CAST(ABS(RANDOM() % 2) - 1 AS INTEGER)  -- Steady
    WHEN m.n < 70 THEN 49 + CAST(ABS(RANDOM() % 3) - 1 AS INTEGER)  -- Main set 2
    ELSE 50 + CAST(ABS(RANDOM() % 2) - 1 AS INTEGER)                 -- Cooldown
  END,
  CASE
    WHEN m.n < 10 THEN 50 + CAST(ABS(RANDOM() % 2) - 1 AS INTEGER)
    WHEN m.n < 30 THEN 51 + CAST(ABS(RANDOM() % 3) - 1 AS INTEGER)
    WHEN m.n < 50 THEN 50 + CAST(ABS(RANDOM() % 2) - 1 AS INTEGER)
    WHEN m.n < 70 THEN 51 + CAST(ABS(RANDOM() % 3) - 1 AS INTEGER)
    ELSE 50 + CAST(ABS(RANDOM() % 2) - 1 AS INTEGER)
  END,
  CASE
    WHEN m.n < 10 THEN 72 + CAST(ABS(RANDOM() % 5) AS INTEGER)
    WHEN m.n < 30 THEN 74 + CAST(ABS(RANDOM() % 4) AS INTEGER)
    WHEN m.n < 50 THEN 75 + CAST(ABS(RANDOM() % 3) AS INTEGER)
    WHEN m.n < 70 THEN 73 + CAST(ABS(RANDOM() % 5) AS INTEGER)
    ELSE 77 + CAST(ABS(RANDOM() % 3) AS INTEGER)
  END,
  CASE
    WHEN m.n < 10 THEN 73 + CAST(ABS(RANDOM() % 5) AS INTEGER)
    WHEN m.n < 30 THEN 75 + CAST(ABS(RANDOM() % 4) AS INTEGER)
    WHEN m.n < 50 THEN 76 + CAST(ABS(RANDOM() % 3) AS INTEGER)
    WHEN m.n < 70 THEN 74 + CAST(ABS(RANDOM() % 5) AS INTEGER)
    ELSE 78 + CAST(ABS(RANDOM() % 3) AS INTEGER)
  END,
  CASE
    WHEN m.n < 10 THEN 22 + CAST(ABS(RANDOM() % 3) AS INTEGER)
    WHEN m.n < 30 THEN 23 + CAST(ABS(RANDOM() % 3) AS INTEGER)
    WHEN m.n < 50 THEN 24 + CAST(ABS(RANDOM() % 2) AS INTEGER)
    WHEN m.n < 70 THEN 22 + CAST(ABS(RANDOM() % 4) AS INTEGER)
    ELSE 25 + CAST(ABS(RANDOM() % 2) AS INTEGER)
  END,
  CASE
    WHEN m.n < 10 THEN 23 + CAST(ABS(RANDOM() % 3) AS INTEGER)
    WHEN m.n < 30 THEN 24 + CAST(ABS(RANDOM() % 3) AS INTEGER)
    WHEN m.n < 50 THEN 25 + CAST(ABS(RANDOM() % 2) AS INTEGER)
    WHEN m.n < 70 THEN 23 + CAST(ABS(RANDOM() % 4) AS INTEGER)
    ELSE 26 + CAST(ABS(RANDOM() % 2) AS INTEGER)
  END,
  CASE
    WHEN m.n < 10 THEN 140 + CAST(ABS(RANDOM() % 15) AS INTEGER)
    WHEN m.n < 30 THEN 175 + CAST(ABS(RANDOM() % 20) AS INTEGER)
    WHEN m.n < 50 THEN 165 + CAST(ABS(RANDOM() % 15) AS INTEGER)
    WHEN m.n < 70 THEN 180 + CAST(ABS(RANDOM() % 20) AS INTEGER)
    ELSE 135 + CAST(ABS(RANDOM() % 15) AS INTEGER)
  END,
  CASE
    WHEN m.n < 10 THEN 85 + CAST(ABS(RANDOM() % 5) AS INTEGER)
    WHEN m.n < 30 THEN 88 + CAST(ABS(RANDOM() % 4) AS INTEGER)
    WHEN m.n < 50 THEN 86 + CAST(ABS(RANDOM() % 4) AS INTEGER)
    WHEN m.n < 70 THEN 89 + CAST(ABS(RANDOM() % 4) AS INTEGER)
    ELSE 84 + CAST(ABS(RANDOM() % 4) AS INTEGER)
  END,
  CASE
    WHEN m.n < 10 THEN 125 + CAST(ABS(RANDOM() % 8) AS INTEGER)
    WHEN m.n < 30 THEN 148 + CAST(ABS(RANDOM() % 10) AS INTEGER)
    WHEN m.n < 50 THEN 140 + CAST(ABS(RANDOM() % 8) AS INTEGER)
    WHEN m.n < 70 THEN 152 + CAST(ABS(RANDOM() % 10) AS INTEGER)
    ELSE 128 + CAST(ABS(RANDOM() % 8) AS INTEGER)
  END,
  CASE
    WHEN m.n < 10 THEN CASE WHEN ABS(RANDOM() % 10) < 8 THEN 'OPTIMAL' ELSE 'ATTENTION' END
    WHEN m.n < 30 THEN CASE WHEN ABS(RANDOM() % 10) < 7 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 9 THEN 'ATTENTION' ELSE 'PROBLEM' END
    WHEN m.n < 50 THEN CASE WHEN ABS(RANDOM() % 10) < 8 THEN 'OPTIMAL' ELSE 'ATTENTION' END
    WHEN m.n < 70 THEN CASE WHEN ABS(RANDOM() % 10) < 6 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 9 THEN 'ATTENTION' ELSE 'PROBLEM' END
    ELSE CASE WHEN ABS(RANDOM() % 10) < 9 THEN 'OPTIMAL' ELSE 'ATTENTION' END
  END
FROM rides r, (
  WITH RECURSIVE cnt(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM cnt WHERE n < 89)
  SELECT n FROM cnt
) m
WHERE r.user_id = 'demo-user-00000000-0000-0000-0000-000000000000'
AND r.duration_ms = 5400000
AND r.score = 82;

-- Yesterday's interval ride (Ride 26) - 60 min with interval pattern
INSERT INTO ride_snapshots (ride_id, minute_index, timestamp, balance_left, balance_right, te_left, te_right, ps_left, ps_right, power_avg, cadence_avg, hr_avg, zone_status)
SELECT
  r.id,
  m.n,
  r.timestamp + (m.n * 60000),
  CASE WHEN m.n % 8 < 3 THEN 47 + CAST(ABS(RANDOM() % 3) AS INTEGER) ELSE 49 + CAST(ABS(RANDOM() % 3) AS INTEGER) END,
  CASE WHEN m.n % 8 < 3 THEN 53 + CAST(ABS(RANDOM() % 3) AS INTEGER) ELSE 51 + CAST(ABS(RANDOM() % 3) AS INTEGER) END,
  CASE WHEN m.n % 8 < 3 THEN 68 + CAST(ABS(RANDOM() % 6) AS INTEGER) ELSE 74 + CAST(ABS(RANDOM() % 4) AS INTEGER) END,
  CASE WHEN m.n % 8 < 3 THEN 71 + CAST(ABS(RANDOM() % 6) AS INTEGER) ELSE 76 + CAST(ABS(RANDOM() % 4) AS INTEGER) END,
  CASE WHEN m.n % 8 < 3 THEN 18 + CAST(ABS(RANDOM() % 4) AS INTEGER) ELSE 23 + CAST(ABS(RANDOM() % 3) AS INTEGER) END,
  CASE WHEN m.n % 8 < 3 THEN 20 + CAST(ABS(RANDOM() % 4) AS INTEGER) ELSE 24 + CAST(ABS(RANDOM() % 3) AS INTEGER) END,
  CASE WHEN m.n % 8 < 3 THEN 280 + CAST(ABS(RANDOM() % 60) AS INTEGER) ELSE 150 + CAST(ABS(RANDOM() % 30) AS INTEGER) END,
  CASE WHEN m.n % 8 < 3 THEN 95 + CAST(ABS(RANDOM() % 5) AS INTEGER) ELSE 88 + CAST(ABS(RANDOM() % 5) AS INTEGER) END,
  CASE WHEN m.n % 8 < 3 THEN 168 + CAST(ABS(RANDOM() % 12) AS INTEGER) ELSE 145 + CAST(ABS(RANDOM() % 10) AS INTEGER) END,
  CASE WHEN m.n % 8 < 3 THEN CASE WHEN ABS(RANDOM() % 10) < 5 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 8 THEN 'ATTENTION' ELSE 'PROBLEM' END ELSE 'OPTIMAL' END
FROM rides r, (
  WITH RECURSIVE cnt(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM cnt WHERE n < 59)
  SELECT n FROM cnt
) m
WHERE r.user_id = 'demo-user-00000000-0000-0000-0000-000000000000'
AND r.duration_ms = 3600000
AND r.score = 68;

-- Gran Fondo ride (Ride 18) - 180 min long ride
INSERT INTO ride_snapshots (ride_id, minute_index, timestamp, balance_left, balance_right, te_left, te_right, ps_left, ps_right, power_avg, cadence_avg, hr_avg, zone_status)
SELECT
  r.id,
  m.n,
  r.timestamp + (m.n * 60000),
  49 + CAST(ABS(RANDOM() % 3) - 1 AS INTEGER),
  51 + CAST(ABS(RANDOM() % 3) - 1 AS INTEGER),
  71 + CAST(ABS(RANDOM() % 5) AS INTEGER),
  73 + CAST(ABS(RANDOM() % 5) AS INTEGER),
  20 + CAST(ABS(RANDOM() % 4) AS INTEGER),
  21 + CAST(ABS(RANDOM() % 4) AS INTEGER),
  CASE
    WHEN m.n < 20 THEN 145 + CAST(ABS(RANDOM() % 15) AS INTEGER)
    WHEN m.n < 60 THEN 160 + CAST(ABS(RANDOM() % 20) AS INTEGER)
    WHEN m.n < 90 THEN 170 + CAST(ABS(RANDOM() % 25) AS INTEGER)
    WHEN m.n < 120 THEN 165 + CAST(ABS(RANDOM() % 20) AS INTEGER)
    WHEN m.n < 150 THEN 155 + CAST(ABS(RANDOM() % 20) AS INTEGER)
    ELSE 140 + CAST(ABS(RANDOM() % 15) AS INTEGER)
  END,
  83 + CAST(ABS(RANDOM() % 5) AS INTEGER),
  CASE
    WHEN m.n < 20 THEN 128 + CAST(ABS(RANDOM() % 8) AS INTEGER)
    WHEN m.n < 60 THEN 140 + CAST(ABS(RANDOM() % 10) AS INTEGER)
    WHEN m.n < 90 THEN 148 + CAST(ABS(RANDOM() % 12) AS INTEGER)
    WHEN m.n < 120 THEN 145 + CAST(ABS(RANDOM() % 10) AS INTEGER)
    WHEN m.n < 150 THEN 138 + CAST(ABS(RANDOM() % 10) AS INTEGER)
    ELSE 130 + CAST(ABS(RANDOM() % 8) AS INTEGER)
  END,
  CASE
    WHEN m.n < 60 THEN CASE WHEN ABS(RANDOM() % 10) < 7 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 9 THEN 'ATTENTION' ELSE 'PROBLEM' END
    WHEN m.n < 120 THEN CASE WHEN ABS(RANDOM() % 10) < 6 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 9 THEN 'ATTENTION' ELSE 'PROBLEM' END
    ELSE CASE WHEN ABS(RANDOM() % 10) < 7 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 9 THEN 'ATTENTION' ELSE 'PROBLEM' END
  END
FROM rides r, (
  WITH RECURSIVE cnt(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM cnt WHERE n < 179)
  SELECT n FROM cnt
) m
WHERE r.user_id = 'demo-user-00000000-0000-0000-0000-000000000000'
AND r.duration_ms = 10800000;

-- Add snapshots for a few more key rides (keeping it manageable)

-- Hill climb ride (Ride 22) - 80 min with climbing pattern
INSERT INTO ride_snapshots (ride_id, minute_index, timestamp, balance_left, balance_right, te_left, te_right, ps_left, ps_right, power_avg, cadence_avg, hr_avg, zone_status)
SELECT
  r.id,
  m.n,
  r.timestamp + (m.n * 60000),
  CASE WHEN m.n % 15 < 8 THEN 47 + CAST(ABS(RANDOM() % 3) AS INTEGER) ELSE 49 + CAST(ABS(RANDOM() % 3) AS INTEGER) END,
  CASE WHEN m.n % 15 < 8 THEN 53 + CAST(ABS(RANDOM() % 3) AS INTEGER) ELSE 51 + CAST(ABS(RANDOM() % 3) AS INTEGER) END,
  CASE WHEN m.n % 15 < 8 THEN 68 + CAST(ABS(RANDOM() % 6) AS INTEGER) ELSE 74 + CAST(ABS(RANDOM() % 4) AS INTEGER) END,
  CASE WHEN m.n % 15 < 8 THEN 71 + CAST(ABS(RANDOM() % 6) AS INTEGER) ELSE 76 + CAST(ABS(RANDOM() % 4) AS INTEGER) END,
  CASE WHEN m.n % 15 < 8 THEN 18 + CAST(ABS(RANDOM() % 4) AS INTEGER) ELSE 22 + CAST(ABS(RANDOM() % 4) AS INTEGER) END,
  CASE WHEN m.n % 15 < 8 THEN 20 + CAST(ABS(RANDOM() % 4) AS INTEGER) ELSE 23 + CAST(ABS(RANDOM() % 4) AS INTEGER) END,
  CASE WHEN m.n % 15 < 8 THEN 220 + CAST(ABS(RANDOM() % 50) AS INTEGER) ELSE 160 + CAST(ABS(RANDOM() % 25) AS INTEGER) END,
  CASE WHEN m.n % 15 < 8 THEN 72 + CAST(ABS(RANDOM() % 8) AS INTEGER) ELSE 85 + CAST(ABS(RANDOM() % 6) AS INTEGER) END,
  CASE WHEN m.n % 15 < 8 THEN 162 + CAST(ABS(RANDOM() % 12) AS INTEGER) ELSE 145 + CAST(ABS(RANDOM() % 10) AS INTEGER) END,
  CASE WHEN m.n % 15 < 8 THEN CASE WHEN ABS(RANDOM() % 10) < 5 THEN 'OPTIMAL' WHEN ABS(RANDOM() % 10) < 8 THEN 'ATTENTION' ELSE 'PROBLEM' END ELSE CASE WHEN ABS(RANDOM() % 10) < 7 THEN 'OPTIMAL' ELSE 'ATTENTION' END END
FROM rides r, (
  WITH RECURSIVE cnt(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM cnt WHERE n < 79)
  SELECT n FROM cnt
) m
WHERE r.user_id = 'demo-user-00000000-0000-0000-0000-000000000000'
AND r.duration_ms = 4800000
AND r.elevation_gain = 980;
