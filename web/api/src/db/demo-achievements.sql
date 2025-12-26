-- ============================================
-- Demo Achievements and Drill Results
-- ============================================

-- Demo Achievements (13 unlocked)
INSERT OR IGNORE INTO achievements (user_id, achievement_id, unlocked_at) VALUES
('demo-user-00000000-0000-0000-0000-000000000000', 'first_ride', strftime('%s', datetime('now', '-27 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'perfect_balance', strftime('%s', datetime('now', '-25 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'te_master', strftime('%s', datetime('now', '-22 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'five_rides', strftime('%s', datetime('now', '-22 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'first_drill', strftime('%s', datetime('now', '-20 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'consistency_week', strftime('%s', datetime('now', '-17 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'ten_rides', strftime('%s', datetime('now', '-15 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'high_score', strftime('%s', datetime('now', '-14 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'hour_power', strftime('%s', datetime('now', '-10 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'century_km', strftime('%s', datetime('now', '-8 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'drill_master', strftime('%s', datetime('now', '-6 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'twenty_rides', strftime('%s', datetime('now', '-4 days')) * 1000),
('demo-user-00000000-0000-0000-0000-000000000000', 'zone_warrior', strftime('%s', datetime('now', '-1 days')) * 1000);

-- Demo Drill Results (8 completed drills)

-- Balance Focus drill (20 days ago)
INSERT INTO drill_results (user_id, device_id, drill_id, drill_name, timestamp, duration_ms, score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  'demo-device-00000000-0000-0000-0000-000000000000',
  'balance_focus',
  'Balance Focus',
  strftime('%s', datetime('now', '-20 days')) * 1000,
  300000,
  72,
  216000,
  72,
  1,
  '[{"phase": 1, "score": 68}, {"phase": 2, "score": 75}, {"phase": 3, "score": 73}]'
);

-- Single Leg Left (18 days ago)
INSERT INTO drill_results (user_id, device_id, drill_id, drill_name, timestamp, duration_ms, score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  'demo-device-00000000-0000-0000-0000-000000000000',
  'single_leg_left',
  'Single Leg Left',
  strftime('%s', datetime('now', '-18 days')) * 1000,
  180000,
  65,
  117000,
  65,
  1,
  '[{"phase": 1, "score": 62}, {"phase": 2, "score": 68}]'
);

-- Single Leg Right (18 days ago)
INSERT INTO drill_results (user_id, device_id, drill_id, drill_name, timestamp, duration_ms, score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  'demo-device-00000000-0000-0000-0000-000000000000',
  'single_leg_right',
  'Single Leg Right',
  strftime('%s', datetime('now', '-18 days', '+1 hour')) * 1000,
  180000,
  78,
  140400,
  78,
  1,
  '[{"phase": 1, "score": 75}, {"phase": 2, "score": 81}]'
);

-- Smooth Circles (15 days ago)
INSERT INTO drill_results (user_id, device_id, drill_id, drill_name, timestamp, duration_ms, score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  'demo-device-00000000-0000-0000-0000-000000000000',
  'smooth_circles',
  'Smooth Circles',
  strftime('%s', datetime('now', '-15 days')) * 1000,
  240000,
  82,
  196800,
  82,
  1,
  '[{"phase": 1, "score": 78}, {"phase": 2, "score": 85}, {"phase": 3, "score": 83}]'
);

-- High Cadence (12 days ago)
INSERT INTO drill_results (user_id, device_id, drill_id, drill_name, timestamp, duration_ms, score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  'demo-device-00000000-0000-0000-0000-000000000000',
  'high_cadence',
  'High Cadence Spin',
  strftime('%s', datetime('now', '-12 days')) * 1000,
  300000,
  75,
  225000,
  75,
  1,
  '[{"phase": 1, "score": 72}, {"phase": 2, "score": 78}]'
);

-- Balance Focus again (8 days ago) - improved
INSERT INTO drill_results (user_id, device_id, drill_id, drill_name, timestamp, duration_ms, score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  'demo-device-00000000-0000-0000-0000-000000000000',
  'balance_focus',
  'Balance Focus',
  strftime('%s', datetime('now', '-8 days')) * 1000,
  300000,
  85,
  255000,
  85,
  1,
  '[{"phase": 1, "score": 82}, {"phase": 2, "score": 88}, {"phase": 3, "score": 85}]'
);

-- Power Endurance (5 days ago)
INSERT INTO drill_results (user_id, device_id, drill_id, drill_name, timestamp, duration_ms, score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  'demo-device-00000000-0000-0000-0000-000000000000',
  'power_endurance',
  'Power Endurance',
  strftime('%s', datetime('now', '-5 days')) * 1000,
  600000,
  71,
  426000,
  71,
  1,
  '[{"phase": 1, "score": 68}, {"phase": 2, "score": 72}, {"phase": 3, "score": 73}]'
);

-- Smooth Circles (2 days ago) - even better
INSERT INTO drill_results (user_id, device_id, drill_id, drill_name, timestamp, duration_ms, score, time_in_target_ms, time_in_target_percent, completed, phase_scores_json)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  'demo-device-00000000-0000-0000-0000-000000000000',
  'smooth_circles',
  'Smooth Circles',
  strftime('%s', datetime('now', '-2 days')) * 1000,
  240000,
  91,
  218400,
  91,
  1,
  '[{"phase": 1, "score": 88}, {"phase": 2, "score": 93}, {"phase": 3, "score": 92}]'
);
