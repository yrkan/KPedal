-- ============================================
-- Demo Account Data
-- KPedal Demo User with 27 diverse rides
-- ============================================

-- Demo User
INSERT OR REPLACE INTO users (id, email, name, picture, google_id, created_at, updated_at)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  'demo@kpedal.com',
  'Yuri',
  'https://lh3.googleusercontent.com/a/ACg8ocLjVR1HSTrX_-I1HSu5JPnTzoknIEHFTMpjs51ncA9xXdbtkFPF=s96-c',
  'demo-google-id-000000',
  datetime('now'),
  datetime('now')
);

-- Demo Device
INSERT OR REPLACE INTO devices (id, user_id, name, type, last_sync, created_at)
VALUES (
  'demo-device-00000000-0000-0000-0000-000000000000',
  'demo-user-00000000-0000-0000-0000-000000000000',
  'Karoo 3 (Demo)',
  'karoo',
  datetime('now'),
  datetime('now')
);

-- Demo User Settings
INSERT OR REPLACE INTO user_settings (user_id, updated_at)
VALUES (
  'demo-user-00000000-0000-0000-0000-000000000000',
  datetime('now')
);

-- ============================================
-- 27 Demo Rides (last 4 weeks of training)
-- Realistic mix with varied metrics and some missing data
-- ============================================

-- Ride 1: Recovery ride (27 days ago) - Easy zone 2 spinning
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-27 days')) * 1000,
  2700000, 49.2, 50.8, 72, 74, 21, 22, 65, 25, 10, 72,
  145, 220, 88, 125, 142, 28.5, 21.4, 180, 175, 1.2, 4.5, 152, 392,
  'Easy recovery spin. Legs felt heavy from weekend.', 3);

-- Ride 2: Interval training (26 days ago) - 5x5min VO2max efforts
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-26 days')) * 1000,
  3600000, 48.5, 51.5, 68, 71, 18, 20, 45, 35, 20, 58,
  185, 380, 92, 155, 178, 32.1, 32.1, 420, 410, 2.1, 8.2, 205, 666,
  '5x5min @ 110% FTP. Form broke down on last interval.', 4);

-- Ride 3: Long endurance (25 days ago) - Steady zone 2
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-25 days')) * 1000,
  5400000, 50.1, 49.9, 75, 76, 23, 24, 78, 17, 5, 85,
  168, 295, 85, 138, 158, 29.8, 44.7, 580, 590, 1.8, 6.8, 178, 907,
  'Great steady ride. Pedaling felt smooth throughout.', 5);

-- Ride 4: Hill repeats (24 days ago) - 8x3min climbs
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-24 days')) * 1000,
  4200000, 47.8, 52.2, 69, 72, 19, 21, 52, 32, 16, 62,
  195, 420, 78, 162, 185, 24.5, 28.6, 890, 880, 4.8, 12.5, 218, 819,
  'Hill repeats on the Col. Right leg dominant on climbs.', 4);

-- Ride 5: Rest day spin (22 days ago) - NO HR DATA (forgot chest strap)
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-22 days')) * 1000,
  1800000, 50.5, 49.5, 78, 79, 25, 26, 88, 10, 2, 92,
  120, 180, 92, 0, 0, 26.2, 13.1, 95, 90, 0.8, 3.2, 128, 216,
  'Coffee ride. Forgot HR strap but felt easy.', 3);

-- Ride 6: Tempo (21 days ago) - Sweet spot 2x20min
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-21 days')) * 1000,
  4800000, 49.8, 50.2, 74, 75, 22, 23, 72, 22, 6, 78,
  175, 310, 88, 148, 168, 30.5, 40.7, 380, 375, 1.5, 5.8, 188, 840,
  '2x20min @ 88% FTP. Second interval was easier.', 4);

-- Ride 7: Group ride (20 days ago) - Variable pace with attacks
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-20 days')) * 1000,
  5100000, 48.2, 51.8, 70, 73, 20, 22, 58, 28, 14, 66,
  182, 520, 90, 152, 182, 33.2, 47.1, 620, 615, 2.2, 9.5, 212, 928,
  'Fast group ride. Lost balance during surges but held on!', 4);

-- Ride 8: VO2max intervals (19 days ago) - 6x3min @ 120%
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-19 days')) * 1000,
  3000000, 47.5, 52.5, 66, 70, 17, 19, 42, 38, 20, 54,
  198, 480, 95, 168, 192, 34.8, 29.0, 350, 345, 1.8, 7.2, 225, 594,
  'Hard session. Technique degraded badly on last 2 efforts.', 3);

-- Ride 9: Recovery (18 days ago)
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-18 days')) * 1000,
  2400000, 50.2, 49.8, 77, 78, 24, 25, 82, 14, 4, 88,
  135, 195, 90, 120, 135, 27.5, 18.3, 120, 115, 0.9, 3.5, 142, 324,
  NULL, 4);

-- Ride 10: Epic long ride (17 days ago) - 4+ hours endurance
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-17 days')) * 1000,
  15300000, 49.5, 50.5, 73, 74, 22, 23, 70, 23, 7, 76,
  162, 285, 86, 142, 165, 28.8, 122.4, 1820, 1810, 2.5, 11.2, 172, 2480,
  'Century ride! Technique stayed solid until last hour.', 5);

-- Ride 11: Threshold work (15 days ago) - 3x15min FTP
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-15 days')) * 1000,
  4500000, 48.8, 51.2, 71, 73, 20, 21, 62, 28, 10, 70,
  188, 345, 90, 158, 175, 31.5, 39.4, 410, 405, 1.6, 6.5, 202, 846,
  '3x15min @ FTP. Held power but balance drifted right.', 4);

-- Ride 12: Easy spin (14 days ago) - Active recovery
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-14 days')) * 1000,
  2100000, 50.8, 49.2, 79, 80, 26, 27, 90, 8, 2, 94,
  118, 175, 92, 112, 125, 26.8, 15.6, 85, 80, 0.6, 2.8, 125, 248,
  NULL, NULL);

-- Ride 13: Race simulation (13 days ago) - Hard from start
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-13 days')) * 1000,
  5400000, 47.2, 52.8, 67, 71, 18, 20, 48, 34, 18, 58,
  192, 580, 93, 165, 188, 35.2, 52.8, 780, 775, 2.8, 14.5, 228, 1037,
  'Mock race effort. Need to work on balance under fatigue!', 3);

-- Ride 14: Recovery (12 days ago) - Indoor trainer NO ELEVATION
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-12 days')) * 1000,
  1800000, 50.3, 49.7, 78, 79, 25, 26, 86, 11, 3, 90,
  125, 185, 90, 118, 132, 0, 0, 0, 0, 0, 0, 132, 225,
  'Rainy day trainer session. Focus on smooth pedaling.', 4);

-- Ride 15: Endurance with tempo (11 days ago)
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-11 days')) * 1000,
  5700000, 49.5, 50.5, 74, 75, 22, 23, 74, 20, 6, 80,
  172, 320, 87, 145, 165, 30.2, 47.9, 490, 485, 1.7, 7.8, 185, 981,
  NULL, 4);

-- Ride 16: Sprint intervals (10 days ago) - Neuromuscular work
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-10 days')) * 1000,
  3300000, 46.8, 53.2, 65, 69, 16, 18, 38, 40, 22, 52,
  178, 920, 98, 155, 185, 33.5, 30.7, 280, 275, 1.4, 5.2, 215, 587,
  '10x30s sprints. Max power new PB! Balance terrible in sprints.', 4);

-- Ride 17: Recovery (9 days ago)
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-9 days')) * 1000,
  2700000, 50.6, 49.4, 79, 80, 26, 27, 88, 10, 2, 92,
  128, 190, 91, 115, 128, 27.2, 20.4, 110, 105, 0.7, 3.0, 135, 346,
  NULL, NULL);

-- Ride 18: Gran Fondo (8 days ago) - Big event ride!
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-8 days')) * 1000,
  18000000, 49.2, 50.8, 72, 74, 21, 22, 68, 24, 8, 74,
  158, 385, 84, 140, 172, 29.5, 147.5, 2450, 2440, 3.2, 15.8, 175, 2844,
  'Gran Fondo complete! Paced well, technique held up for 5hrs!', 5);

-- Ride 19: Easy spin (7 days ago) - Post-event recovery
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-7 days')) * 1000,
  1500000, 50.9, 49.1, 80, 81, 27, 28, 92, 7, 1, 95,
  115, 165, 92, 108, 120, 25.8, 10.8, 65, 60, 0.5, 2.2, 122, 173,
  'Legs still tired from Fondo. Just spinning.', 3);

-- Ride 20: Sweet spot (6 days ago) - Back to training
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-6 days')) * 1000,
  4200000, 49.0, 51.0, 73, 75, 21, 22, 68, 25, 7, 76,
  182, 340, 89, 152, 172, 31.8, 37.1, 380, 375, 1.6, 6.5, 198, 764,
  'Back to quality work. Felt strong despite fatigue.', 4);

-- Ride 21: Endurance (5 days ago) - Aerobic base
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-5 days')) * 1000,
  6000000, 50.2, 49.8, 76, 77, 24, 25, 80, 16, 4, 84,
  165, 280, 86, 138, 155, 29.2, 48.7, 520, 515, 1.8, 7.5, 175, 990,
  NULL, 4);

-- Ride 22: Mountain day (4 days ago) - Big climbing!
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-4 days')) * 1000,
  5400000, 48.0, 52.0, 70, 73, 19, 21, 55, 30, 15, 64,
  190, 445, 76, 158, 180, 22.5, 33.8, 1420, 1415, 5.8, 18.2, 218, 1026,
  'Epic mountain loops! KOM attempt on Alpe segment. Right leg did the work uphill.', 5);

-- Ride 23: Recovery (3 days ago)
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-3 days')) * 1000,
  2400000, 50.4, 49.6, 78, 79, 25, 26, 85, 12, 3, 89,
  130, 195, 90, 118, 132, 27.5, 18.3, 100, 95, 0.8, 3.2, 138, 312,
  'Easy legs. Focus on single leg drills.', 4);

-- Ride 24: Tempo with intervals (2 days ago)
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-2 days')) * 1000,
  4500000, 49.3, 50.7, 74, 76, 22, 23, 72, 22, 6, 78,
  178, 365, 88, 150, 172, 31.0, 38.8, 420, 415, 1.8, 7.5, 195, 801,
  NULL, 4);

-- Ride 25: Morning spin (yesterday AM)
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-1 days', '+8 hours')) * 1000,
  1800000, 50.7, 49.3, 79, 80, 26, 27, 90, 8, 2, 93,
  122, 180, 92, 112, 125, 26.5, 13.3, 75, 70, 0.6, 2.5, 128, 220,
  'Pre-work opener. Felt snappy!', 4);

-- Ride 26: Afternoon intervals (yesterday PM) - Double day
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-1 days', '+17 hours')) * 1000,
  3600000, 48.5, 51.5, 71, 74, 20, 22, 60, 28, 12, 68,
  185, 450, 92, 158, 182, 33.0, 33.0, 350, 345, 1.6, 6.8, 208, 666,
  '4x8min threshold. Solid session, minor drift on last interval.', 4);

-- Ride 27: Today morning - Fresh legs!
INSERT INTO rides (user_id, device_id, timestamp, duration_ms, balance_left, balance_right, te_left, te_right, ps_left, ps_right, zone_optimal, zone_attention, zone_problem, score, power_avg, power_max, cadence_avg, hr_avg, hr_max, speed_avg, distance_km, elevation_gain, elevation_loss, grade_avg, grade_max, normalized_power, energy_kj, notes, rating)
VALUES ('demo-user-00000000-0000-0000-0000-000000000000', 'demo-device-00000000-0000-0000-0000-000000000000',
  strftime('%s', datetime('now', '-2 hours')) * 1000,
  5400000, 49.8, 50.2, 75, 76, 23, 24, 76, 19, 5, 82,
  170, 295, 87, 142, 162, 30.0, 45.0, 480, 475, 1.8, 8.2, 182, 918,
  'Great morning ride! Pedaling felt dialed in.', 5);
