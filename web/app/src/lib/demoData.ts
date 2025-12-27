/**
 * Static demo data for instant loading (0ms latency)
 * Timestamps are relative offsets from "now" so data always looks fresh
 */

// Time constants (in ms)
const HOUR = 3600000;
const DAY = 86400000;

/** Get timestamp offset from now */
const ago = (days: number, hours = 0) => Date.now() - (days * DAY + hours * HOUR);

/** Generate demo dashboard data with dynamic timestamps */
export function getDemoDashboard() {
  return {
    stats: {
      total_rides: 27,
      avg_balance_left: 49.3,
      avg_balance_right: 50.7,
      avg_te_left: 73.4,
      avg_te_right: 75.3,
      avg_ps_left: 21.9,
      avg_ps_right: 23.2,
      avg_score: 76.7,
      total_duration_ms: 110700000,
      avg_zone_optimal: 69.8,
      avg_zone_attention: 21.7,
      avg_zone_problem: 8.5,
      best_score: 95,
      best_balance_diff: 0.1,
      avg_power: 161.5,
      max_power: 680,
      avg_cadence: 89.1,
      avg_hr: 139.6,
      total_distance_km: 921.8
    },
    recentRides: [
      { id: 57, timestamp: ago(0, 5), duration_ms: 5400000, balance_left: 49.8, balance_right: 50.2, te_left: 75, te_right: 76, ps_left: 23, ps_right: 24, zone_optimal: 76, zone_attention: 19, zone_problem: 5, score: 82, power_avg: 170, cadence_avg: 87, hr_avg: 142, distance_km: 45 },
      { id: 56, timestamp: ago(0, 10), duration_ms: 3600000, balance_left: 48.5, balance_right: 51.5, te_left: 71, te_right: 74, ps_left: 20, ps_right: 22, zone_optimal: 60, zone_attention: 28, zone_problem: 12, score: 68, power_avg: 185, cadence_avg: 92, hr_avg: 158, distance_km: 33 },
      { id: 55, timestamp: ago(0, 19), duration_ms: 1800000, balance_left: 50.7, balance_right: 49.3, te_left: 79, te_right: 80, ps_left: 26, ps_right: 27, zone_optimal: 90, zone_attention: 8, zone_problem: 2, score: 93, power_avg: 122, cadence_avg: 92, hr_avg: 112, distance_km: 13.3 },
      { id: 54, timestamp: ago(1, 14), duration_ms: 4500000, balance_left: 49.3, balance_right: 50.7, te_left: 74, te_right: 76, ps_left: 22, ps_right: 23, zone_optimal: 72, zone_attention: 22, zone_problem: 6, score: 78, power_avg: 178, cadence_avg: 88, hr_avg: 150, distance_km: 38.8 },
      { id: 53, timestamp: ago(2, 14), duration_ms: 2400000, balance_left: 50.4, balance_right: 49.6, te_left: 78, te_right: 79, ps_left: 25, ps_right: 26, zone_optimal: 85, zone_attention: 12, zone_problem: 3, score: 89, power_avg: 130, cadence_avg: 90, hr_avg: 118, distance_km: 18.3 },
      { id: 52, timestamp: ago(3, 14), duration_ms: 4800000, balance_left: 48, balance_right: 52, te_left: 70, te_right: 73, ps_left: 19, ps_right: 21, zone_optimal: 55, zone_attention: 30, zone_problem: 15, score: 64, power_avg: 190, cadence_avg: 80, hr_avg: 158, distance_km: 35.4 },
      { id: 51, timestamp: ago(4, 14), duration_ms: 6000000, balance_left: 50.2, balance_right: 49.8, te_left: 76, te_right: 77, ps_left: 24, ps_right: 25, zone_optimal: 80, zone_attention: 16, zone_problem: 4, score: 84, power_avg: 165, cadence_avg: 86, hr_avg: 138, distance_km: 48.7 },
      { id: 50, timestamp: ago(5, 14), duration_ms: 4200000, balance_left: 49, balance_right: 51, te_left: 73, te_right: 75, ps_left: 21, ps_right: 22, zone_optimal: 68, zone_attention: 25, zone_problem: 7, score: 76, power_avg: 182, cadence_avg: 89, hr_avg: 152, distance_km: 37.1 },
      { id: 49, timestamp: ago(6, 14), duration_ms: 1500000, balance_left: 50.9, balance_right: 49.1, te_left: 80, te_right: 81, ps_left: 27, ps_right: 28, zone_optimal: 92, zone_attention: 7, zone_problem: 1, score: 95, power_avg: 115, cadence_avg: 92, hr_avg: 108, distance_km: 10.8 },
      { id: 48, timestamp: ago(7, 14), duration_ms: 10800000, balance_left: 49.2, balance_right: 50.8, te_left: 72, te_right: 74, ps_left: 21, ps_right: 22, zone_optimal: 68, zone_attention: 24, zone_problem: 8, score: 74, power_avg: 158, cadence_avg: 84, hr_avg: 140, distance_km: 88.5 }
    ],
    weeklyComparison: {
      thisWeek: { rides_count: 7, avg_score: 79.7, avg_balance_left: 49.6, avg_te: 75.6, avg_ps: 23.4, avg_zone_optimal: 74, total_duration_ms: 28500000, avg_power: 162.9, total_distance_km: 232.5, total_elevation: 2925, total_energy_kj: 4819 },
      lastWeek: { rides_count: 7, avg_score: 79.9, avg_balance_left: 49.5, avg_te: 75.3, avg_ps: 23.1, avg_zone_optimal: 73.4, total_duration_ms: 30000000, avg_power: 151.1, total_distance_km: 248.9, total_elevation: 2665, total_energy_kj: 4782 },
      changes: { score: -0.2, zone_optimal: 0.6, rides_count: 0, power: 11.8, distance: -16.4, duration: -1500000, te: 0.3, ps: 0.3, balance: -0.1 }
    },
    trends: generateTrends(),
    lastRideSnapshots: [
      { minute_index: 0, balance_left: 50, balance_right: 50, te_left: 73, te_right: 76, ps_left: 24, ps_right: 25, power_avg: 140 },
      { minute_index: 1, balance_left: 49, balance_right: 49, te_left: 75, te_right: 74, ps_left: 23, ps_right: 24, power_avg: 149 },
      { minute_index: 2, balance_left: 50, balance_right: 50, te_left: 75, te_right: 73, ps_left: 23, ps_right: 25, power_avg: 154 },
      { minute_index: 3, balance_left: 50, balance_right: 49, te_left: 74, te_right: 73, ps_left: 24, ps_right: 25, power_avg: 153 },
      { minute_index: 4, balance_left: 49, balance_right: 50, te_left: 72, te_right: 77, ps_left: 24, ps_right: 23, power_avg: 149 },
      { minute_index: 5, balance_left: 50, balance_right: 49, te_left: 72, te_right: 75, ps_left: 24, ps_right: 23, power_avg: 150 },
      { minute_index: 10, balance_left: 48, balance_right: 52, te_left: 75, te_right: 77, ps_left: 25, ps_right: 24, power_avg: 186 },
      { minute_index: 15, balance_left: 50, balance_right: 52, te_left: 76, te_right: 76, ps_left: 24, ps_right: 24, power_avg: 191 },
      { minute_index: 20, balance_left: 48, balance_right: 51, te_left: 76, te_right: 77, ps_left: 24, ps_right: 25, power_avg: 192 },
      { minute_index: 25, balance_left: 49, balance_right: 52, te_left: 74, te_right: 77, ps_left: 23, ps_right: 24, power_avg: 177 },
      { minute_index: 30, balance_left: 49, balance_right: 50, te_left: 76, te_right: 78, ps_left: 25, ps_right: 25, power_avg: 172 },
      { minute_index: 35, balance_left: 49, balance_right: 49, te_left: 77, te_right: 77, ps_left: 24, ps_right: 25, power_avg: 165 },
      { minute_index: 40, balance_left: 50, balance_right: 50, te_left: 76, te_right: 78, ps_left: 25, ps_right: 25, power_avg: 172 },
      { minute_index: 45, balance_left: 50, balance_right: 50, te_left: 77, te_right: 78, ps_left: 24, ps_right: 26, power_avg: 174 },
      { minute_index: 50, balance_left: 48, balance_right: 51, te_left: 77, te_right: 76, ps_left: 23, ps_right: 25, power_avg: 188 },
      { minute_index: 55, balance_left: 50, balance_right: 52, te_left: 74, te_right: 76, ps_left: 25, ps_right: 25, power_avg: 183 },
      { minute_index: 60, balance_left: 50, balance_right: 50, te_left: 73, te_right: 75, ps_left: 23, ps_right: 23, power_avg: 181 },
      { minute_index: 65, balance_left: 50, balance_right: 52, te_left: 75, te_right: 76, ps_left: 24, ps_right: 23, power_avg: 192 },
      { minute_index: 70, balance_left: 50, balance_right: 50, te_left: 77, te_right: 80, ps_left: 26, ps_right: 26, power_avg: 142 },
      { minute_index: 75, balance_left: 50, balance_right: 50, te_left: 78, te_right: 80, ps_left: 26, ps_right: 26, power_avg: 136 },
      { minute_index: 80, balance_left: 50, balance_right: 49, te_left: 77, te_right: 78, ps_left: 25, ps_right: 27, power_avg: 137 },
      { minute_index: 85, balance_left: 49, balance_right: 49, te_left: 79, te_right: 80, ps_left: 25, ps_right: 27, power_avg: 140 },
      { minute_index: 89, balance_left: 49, balance_right: 50, te_left: 79, te_right: 78, ps_left: 26, ps_right: 26, power_avg: 135 }
    ]
  };
}

/** Generate trends with dynamic dates */
function generateTrends() {
  const trendData = [
    { daysAgo: 0, rides_count: 3, avg_balance_left: 49.7, avg_te: 75.8, avg_ps: 23.7, avg_score: 81, avg_zone_optimal: 75.3 },
    { daysAgo: 2, rides_count: 1, avg_balance_left: 49.3, avg_te: 75, avg_ps: 22.5, avg_score: 78, avg_zone_optimal: 72 },
    { daysAgo: 3, rides_count: 1, avg_balance_left: 50.4, avg_te: 78.5, avg_ps: 25.5, avg_score: 89, avg_zone_optimal: 85 },
    { daysAgo: 4, rides_count: 1, avg_balance_left: 48, avg_te: 71.5, avg_ps: 20, avg_score: 64, avg_zone_optimal: 55 },
    { daysAgo: 5, rides_count: 1, avg_balance_left: 50.2, avg_te: 76.5, avg_ps: 24.5, avg_score: 84, avg_zone_optimal: 80 },
    { daysAgo: 6, rides_count: 1, avg_balance_left: 49, avg_te: 74, avg_ps: 21.5, avg_score: 76, avg_zone_optimal: 68 },
    { daysAgo: 7, rides_count: 1, avg_balance_left: 50.9, avg_te: 80.5, avg_ps: 27.5, avg_score: 95, avg_zone_optimal: 92 },
    { daysAgo: 8, rides_count: 1, avg_balance_left: 49.2, avg_te: 73, avg_ps: 21.5, avg_score: 74, avg_zone_optimal: 68 },
    { daysAgo: 9, rides_count: 1, avg_balance_left: 50.6, avg_te: 79.5, avg_ps: 26.5, avg_score: 92, avg_zone_optimal: 88 },
    { daysAgo: 10, rides_count: 1, avg_balance_left: 46.8, avg_te: 67, avg_ps: 17, avg_score: 52, avg_zone_optimal: 38 },
    { daysAgo: 11, rides_count: 1, avg_balance_left: 49.5, avg_te: 74.5, avg_ps: 22.5, avg_score: 80, avg_zone_optimal: 74 },
    { daysAgo: 12, rides_count: 1, avg_balance_left: 50.3, avg_te: 78.5, avg_ps: 25.5, avg_score: 90, avg_zone_optimal: 86 },
    { daysAgo: 13, rides_count: 1, avg_balance_left: 47.2, avg_te: 69, avg_ps: 19, avg_score: 58, avg_zone_optimal: 48 },
    { daysAgo: 14, rides_count: 1, avg_balance_left: 50.8, avg_te: 79.5, avg_ps: 26.5, avg_score: 94, avg_zone_optimal: 90 },
    { daysAgo: 15, rides_count: 1, avg_balance_left: 48.8, avg_te: 72, avg_ps: 20.5, avg_score: 70, avg_zone_optimal: 62 },
    { daysAgo: 17, rides_count: 1, avg_balance_left: 49.5, avg_te: 73.5, avg_ps: 22.5, avg_score: 76, avg_zone_optimal: 70 },
    { daysAgo: 18, rides_count: 1, avg_balance_left: 50.2, avg_te: 77.5, avg_ps: 24.5, avg_score: 88, avg_zone_optimal: 82 },
    { daysAgo: 19, rides_count: 1, avg_balance_left: 47.5, avg_te: 68, avg_ps: 18, avg_score: 54, avg_zone_optimal: 42 },
    { daysAgo: 20, rides_count: 1, avg_balance_left: 48.2, avg_te: 71.5, avg_ps: 21, avg_score: 66, avg_zone_optimal: 58 },
    { daysAgo: 21, rides_count: 1, avg_balance_left: 49.8, avg_te: 74.5, avg_ps: 22.5, avg_score: 78, avg_zone_optimal: 72 },
    { daysAgo: 22, rides_count: 1, avg_balance_left: 50.5, avg_te: 78.5, avg_ps: 25.5, avg_score: 92, avg_zone_optimal: 88 },
    { daysAgo: 24, rides_count: 1, avg_balance_left: 47.8, avg_te: 70.5, avg_ps: 20, avg_score: 62, avg_zone_optimal: 52 },
    { daysAgo: 25, rides_count: 1, avg_balance_left: 50.1, avg_te: 75.5, avg_ps: 23.5, avg_score: 85, avg_zone_optimal: 78 },
    { daysAgo: 26, rides_count: 1, avg_balance_left: 48.5, avg_te: 69.5, avg_ps: 19, avg_score: 58, avg_zone_optimal: 45 },
    { daysAgo: 27, rides_count: 1, avg_balance_left: 49.2, avg_te: 73, avg_ps: 21.5, avg_score: 72, avg_zone_optimal: 65 }
  ];

  return trendData.map(t => {
    const date = new Date(Date.now() - t.daysAgo * DAY);
    return {
      date: date.toISOString().split('T')[0],
      rides_count: t.rides_count,
      avg_balance_left: t.avg_balance_left,
      avg_te: t.avg_te,
      avg_ps: t.avg_ps,
      avg_score: t.avg_score,
      avg_zone_optimal: t.avg_zone_optimal
    };
  });
}

/** Generate demo drills data with dynamic timestamps */
export function getDemoDrills() {
  return {
    drills: [
      { id: 15, drill_id: 'smooth_circles', drill_name: 'Smooth Circles', timestamp: ago(2), duration_ms: 240000, score: 91, time_in_target_ms: 218400, time_in_target_percent: 91, completed: 1 },
      { id: 14, drill_id: 'power_endurance', drill_name: 'Power Endurance', timestamp: ago(5), duration_ms: 600000, score: 71, time_in_target_ms: 426000, time_in_target_percent: 71, completed: 1 },
      { id: 13, drill_id: 'balance_focus', drill_name: 'Balance Focus', timestamp: ago(8), duration_ms: 300000, score: 85, time_in_target_ms: 255000, time_in_target_percent: 85, completed: 1 },
      { id: 12, drill_id: 'high_cadence', drill_name: 'High Cadence Spin', timestamp: ago(12), duration_ms: 300000, score: 75, time_in_target_ms: 225000, time_in_target_percent: 75, completed: 1 },
      { id: 11, drill_id: 'smooth_circles', drill_name: 'Smooth Circles', timestamp: ago(15), duration_ms: 240000, score: 82, time_in_target_ms: 196800, time_in_target_percent: 82, completed: 1 },
      { id: 10, drill_id: 'single_leg_right', drill_name: 'Single Leg Right', timestamp: ago(18), duration_ms: 180000, score: 78, time_in_target_ms: 140400, time_in_target_percent: 78, completed: 1 },
      { id: 9, drill_id: 'single_leg_left', drill_name: 'Single Leg Left', timestamp: ago(18, 1), duration_ms: 180000, score: 65, time_in_target_ms: 117000, time_in_target_percent: 65, completed: 1 },
      { id: 8, drill_id: 'balance_focus', drill_name: 'Balance Focus', timestamp: ago(20), duration_ms: 300000, score: 72, time_in_target_ms: 216000, time_in_target_percent: 72, completed: 1 }
    ],
    total: 8,
    limit: 50,
    offset: 0,
    stats: {
      summary: { total_drills: 8, completed_drills: 8, avg_score: 77.4, best_score: 91, total_duration_ms: 2340000, drill_types_tried: 6 },
      by_drill: [
        { drill_id: 'smooth_circles', drill_name: 'Smooth Circles', attempts: 2, completions: 2, avg_score: 86.5, best_score: 91, last_attempt: ago(2) },
        { drill_id: 'power_endurance', drill_name: 'Power Endurance', attempts: 1, completions: 1, avg_score: 71, best_score: 71, last_attempt: ago(5) },
        { drill_id: 'balance_focus', drill_name: 'Balance Focus', attempts: 2, completions: 2, avg_score: 78.5, best_score: 85, last_attempt: ago(8) },
        { drill_id: 'high_cadence', drill_name: 'High Cadence Spin', attempts: 1, completions: 1, avg_score: 75, best_score: 75, last_attempt: ago(12) },
        { drill_id: 'single_leg_right', drill_name: 'Single Leg Right', attempts: 1, completions: 1, avg_score: 78, best_score: 78, last_attempt: ago(18) },
        { drill_id: 'single_leg_left', drill_name: 'Single Leg Left', attempts: 1, completions: 1, avg_score: 65, best_score: 65, last_attempt: ago(18, 1) }
      ]
    }
  };
}

/** Generate demo rides list with all fields needed for rides page */
export function getDemoRides() {
  // Extended ride data with all fields
  const rides = [
    { id: 57, timestamp: ago(0, 5), duration_ms: 5400000, balance_left: 49.8, balance_right: 50.2, te_left: 75, te_right: 76, ps_left: 23, ps_right: 24, zone_optimal: 76, zone_attention: 19, zone_problem: 5, score: 82, power_avg: 170, power_max: 520, cadence_avg: 87, hr_avg: 142, hr_max: 172, speed_avg: 30.0, distance_km: 45, elevation_gain: 380, elevation_loss: 375, grade_avg: 1.2, grade_max: 8.5, normalized_power: 182, energy_kj: 918 },
    { id: 56, timestamp: ago(0, 10), duration_ms: 3600000, balance_left: 48.5, balance_right: 51.5, te_left: 71, te_right: 74, ps_left: 20, ps_right: 22, zone_optimal: 60, zone_attention: 28, zone_problem: 12, score: 68, power_avg: 185, power_max: 580, cadence_avg: 92, hr_avg: 158, hr_max: 185, speed_avg: 33.0, distance_km: 33, elevation_gain: 290, elevation_loss: 295, grade_avg: 1.8, grade_max: 9.2, normalized_power: 198, energy_kj: 666 },
    { id: 55, timestamp: ago(0, 19), duration_ms: 1800000, balance_left: 50.7, balance_right: 49.3, te_left: 79, te_right: 80, ps_left: 26, ps_right: 27, zone_optimal: 90, zone_attention: 8, zone_problem: 2, score: 93, power_avg: 122, power_max: 320, cadence_avg: 92, hr_avg: 112, hr_max: 135, speed_avg: 26.6, distance_km: 13.3, elevation_gain: 95, elevation_loss: 90, grade_avg: 0.8, grade_max: 5.2, normalized_power: 128, energy_kj: 220 },
    { id: 54, timestamp: ago(1, 14), duration_ms: 4500000, balance_left: 49.3, balance_right: 50.7, te_left: 74, te_right: 76, ps_left: 22, ps_right: 23, zone_optimal: 72, zone_attention: 22, zone_problem: 6, score: 78, power_avg: 178, power_max: 560, cadence_avg: 88, hr_avg: 150, hr_max: 178, speed_avg: 31.1, distance_km: 38.8, elevation_gain: 420, elevation_loss: 415, grade_avg: 1.5, grade_max: 10.5, normalized_power: 190, energy_kj: 801 },
    { id: 53, timestamp: ago(2, 14), duration_ms: 2400000, balance_left: 50.4, balance_right: 49.6, te_left: 78, te_right: 79, ps_left: 25, ps_right: 26, zone_optimal: 85, zone_attention: 12, zone_problem: 3, score: 89, power_avg: 130, power_max: 380, cadence_avg: 90, hr_avg: 118, hr_max: 142, speed_avg: 27.5, distance_km: 18.3, elevation_gain: 145, elevation_loss: 140, grade_avg: 0.9, grade_max: 6.1, normalized_power: 138, energy_kj: 312 },
    { id: 52, timestamp: ago(3, 14), duration_ms: 4800000, balance_left: 48, balance_right: 52, te_left: 70, te_right: 73, ps_left: 19, ps_right: 21, zone_optimal: 55, zone_attention: 30, zone_problem: 15, score: 64, power_avg: 190, power_max: 620, cadence_avg: 80, hr_avg: 158, hr_max: 188, speed_avg: 26.6, distance_km: 35.4, elevation_gain: 580, elevation_loss: 575, grade_avg: 2.8, grade_max: 14.2, normalized_power: 215, energy_kj: 912 },
    { id: 51, timestamp: ago(4, 14), duration_ms: 6000000, balance_left: 50.2, balance_right: 49.8, te_left: 76, te_right: 77, ps_left: 24, ps_right: 25, zone_optimal: 80, zone_attention: 16, zone_problem: 4, score: 84, power_avg: 165, power_max: 480, cadence_avg: 86, hr_avg: 138, hr_max: 165, speed_avg: 29.2, distance_km: 48.7, elevation_gain: 520, elevation_loss: 515, grade_avg: 1.4, grade_max: 8.8, normalized_power: 175, energy_kj: 990 },
    { id: 50, timestamp: ago(5, 14), duration_ms: 4200000, balance_left: 49, balance_right: 51, te_left: 73, te_right: 75, ps_left: 21, ps_right: 22, zone_optimal: 68, zone_attention: 25, zone_problem: 7, score: 76, power_avg: 182, power_max: 540, cadence_avg: 89, hr_avg: 152, hr_max: 180, speed_avg: 31.8, distance_km: 37.1, elevation_gain: 310, elevation_loss: 305, grade_avg: 1.2, grade_max: 7.5, normalized_power: 195, energy_kj: 764 },
    { id: 49, timestamp: ago(6, 14), duration_ms: 1500000, balance_left: 50.9, balance_right: 49.1, te_left: 80, te_right: 81, ps_left: 27, ps_right: 28, zone_optimal: 92, zone_attention: 7, zone_problem: 1, score: 95, power_avg: 115, power_max: 280, cadence_avg: 92, hr_avg: 108, hr_max: 128, speed_avg: 25.9, distance_km: 10.8, elevation_gain: 65, elevation_loss: 60, grade_avg: 0.6, grade_max: 4.2, normalized_power: 120, energy_kj: 173 },
    { id: 48, timestamp: ago(7, 14), duration_ms: 10800000, balance_left: 49.2, balance_right: 50.8, te_left: 72, te_right: 74, ps_left: 21, ps_right: 22, zone_optimal: 68, zone_attention: 24, zone_problem: 8, score: 74, power_avg: 158, power_max: 495, cadence_avg: 84, hr_avg: 140, hr_max: 168, speed_avg: 29.5, distance_km: 88.5, elevation_gain: 1120, elevation_loss: 1115, grade_avg: 1.8, grade_max: 12.5, normalized_power: 172, energy_kj: 1706 },
    { id: 47, timestamp: ago(9, 14), duration_ms: 3300000, balance_left: 50.6, balance_right: 49.4, te_left: 79, te_right: 80, ps_left: 26, ps_right: 27, zone_optimal: 88, zone_attention: 10, zone_problem: 2, score: 92, power_avg: 135, power_max: 360, cadence_avg: 91, hr_avg: 122, hr_max: 148, speed_avg: 28.4, distance_km: 26.2, elevation_gain: 180, elevation_loss: 175, grade_avg: 0.8, grade_max: 5.8, normalized_power: 142, energy_kj: 446 },
    { id: 46, timestamp: ago(10, 14), duration_ms: 4800000, balance_left: 46.8, balance_right: 53.2, te_left: 65, te_right: 69, ps_left: 16, ps_right: 18, zone_optimal: 38, zone_attention: 35, zone_problem: 27, score: 52, power_avg: 175, power_max: 610, cadence_avg: 78, hr_avg: 162, hr_max: 192, speed_avg: 25.8, distance_km: 34.4, elevation_gain: 680, elevation_loss: 675, grade_avg: 3.2, grade_max: 16.5, normalized_power: 205, energy_kj: 840 },
    { id: 45, timestamp: ago(11, 14), duration_ms: 3600000, balance_left: 49.5, balance_right: 50.5, te_left: 74, te_right: 75, ps_left: 22, ps_right: 23, zone_optimal: 74, zone_attention: 20, zone_problem: 6, score: 80, power_avg: 168, power_max: 490, cadence_avg: 88, hr_avg: 145, hr_max: 172, speed_avg: 30.5, distance_km: 30.5, elevation_gain: 265, elevation_loss: 260, grade_avg: 1.1, grade_max: 7.2, normalized_power: 178, energy_kj: 605 },
    { id: 44, timestamp: ago(12, 14), duration_ms: 5400000, balance_left: 50.3, balance_right: 49.7, te_left: 78, te_right: 79, ps_left: 25, ps_right: 26, zone_optimal: 86, zone_attention: 11, zone_problem: 3, score: 90, power_avg: 145, power_max: 420, cadence_avg: 89, hr_avg: 128, hr_max: 155, speed_avg: 28.8, distance_km: 43.2, elevation_gain: 390, elevation_loss: 385, grade_avg: 1.0, grade_max: 6.5, normalized_power: 155, energy_kj: 783 },
    { id: 43, timestamp: ago(13, 14), duration_ms: 2700000, balance_left: 47.2, balance_right: 52.8, te_left: 68, te_right: 70, ps_left: 18, ps_right: 20, zone_optimal: 48, zone_attention: 32, zone_problem: 20, score: 58, power_avg: 195, power_max: 650, cadence_avg: 82, hr_avg: 168, hr_max: 195, speed_avg: 27.2, distance_km: 20.4, elevation_gain: 480, elevation_loss: 475, grade_avg: 3.5, grade_max: 18.2, normalized_power: 225, energy_kj: 527 },
    { id: 42, timestamp: ago(14, 14), duration_ms: 4200000, balance_left: 50.8, balance_right: 49.2, te_left: 79, te_right: 80, ps_left: 26, ps_right: 27, zone_optimal: 90, zone_attention: 8, zone_problem: 2, score: 94, power_avg: 140, power_max: 385, cadence_avg: 90, hr_avg: 125, hr_max: 152, speed_avg: 28.0, distance_km: 32.7, elevation_gain: 245, elevation_loss: 240, grade_avg: 0.9, grade_max: 5.5, normalized_power: 148, energy_kj: 588 },
    { id: 41, timestamp: ago(15, 14), duration_ms: 3900000, balance_left: 48.8, balance_right: 51.2, te_left: 71, te_right: 73, ps_left: 20, ps_right: 21, zone_optimal: 62, zone_attention: 28, zone_problem: 10, score: 70, power_avg: 180, power_max: 545, cadence_avg: 85, hr_avg: 155, hr_max: 182, speed_avg: 29.8, distance_km: 32.4, elevation_gain: 355, elevation_loss: 350, grade_avg: 1.6, grade_max: 9.8, normalized_power: 192, energy_kj: 702 },
    { id: 40, timestamp: ago(17, 14), duration_ms: 3600000, balance_left: 49.5, balance_right: 50.5, te_left: 73, te_right: 74, ps_left: 22, ps_right: 23, zone_optimal: 70, zone_attention: 23, zone_problem: 7, score: 76, power_avg: 170, power_max: 510, cadence_avg: 87, hr_avg: 148, hr_max: 175, speed_avg: 30.2, distance_km: 30.2, elevation_gain: 285, elevation_loss: 280, grade_avg: 1.3, grade_max: 8.0, normalized_power: 180, energy_kj: 612 },
    { id: 39, timestamp: ago(18, 14), duration_ms: 5100000, balance_left: 50.2, balance_right: 49.8, te_left: 77, te_right: 78, ps_left: 24, ps_right: 25, zone_optimal: 82, zone_attention: 14, zone_problem: 4, score: 88, power_avg: 155, power_max: 445, cadence_avg: 88, hr_avg: 135, hr_max: 162, speed_avg: 29.0, distance_km: 41.1, elevation_gain: 410, elevation_loss: 405, grade_avg: 1.2, grade_max: 7.5, normalized_power: 165, energy_kj: 791 },
    { id: 38, timestamp: ago(19, 14), duration_ms: 2400000, balance_left: 47.5, balance_right: 52.5, te_left: 67, te_right: 69, ps_left: 17, ps_right: 19, zone_optimal: 42, zone_attention: 35, zone_problem: 23, score: 54, power_avg: 188, power_max: 625, cadence_avg: 79, hr_avg: 165, hr_max: 195, speed_avg: 26.5, distance_km: 17.7, elevation_gain: 420, elevation_loss: 415, grade_avg: 3.8, grade_max: 19.5, normalized_power: 218, energy_kj: 451 },
    { id: 37, timestamp: ago(20, 14), duration_ms: 3900000, balance_left: 48.2, balance_right: 51.8, te_left: 71, te_right: 72, ps_left: 20, ps_right: 22, zone_optimal: 58, zone_attention: 30, zone_problem: 12, score: 66, power_avg: 176, power_max: 535, cadence_avg: 83, hr_avg: 152, hr_max: 180, speed_avg: 28.5, distance_km: 30.9, elevation_gain: 380, elevation_loss: 375, grade_avg: 1.8, grade_max: 11.2, normalized_power: 188, energy_kj: 686 },
    { id: 36, timestamp: ago(21, 14), duration_ms: 4500000, balance_left: 49.8, balance_right: 50.2, te_left: 74, te_right: 75, ps_left: 22, ps_right: 23, zone_optimal: 72, zone_attention: 22, zone_problem: 6, score: 78, power_avg: 162, power_max: 475, cadence_avg: 86, hr_avg: 142, hr_max: 170, speed_avg: 29.5, distance_km: 36.9, elevation_gain: 335, elevation_loss: 330, grade_avg: 1.2, grade_max: 7.8, normalized_power: 172, energy_kj: 729 },
    { id: 35, timestamp: ago(22, 14), duration_ms: 5700000, balance_left: 50.5, balance_right: 49.5, te_left: 78, te_right: 79, ps_left: 25, ps_right: 26, zone_optimal: 88, zone_attention: 10, zone_problem: 2, score: 92, power_avg: 148, power_max: 415, cadence_avg: 89, hr_avg: 130, hr_max: 158, speed_avg: 28.5, distance_km: 45.1, elevation_gain: 425, elevation_loss: 420, grade_avg: 1.1, grade_max: 6.8, normalized_power: 158, energy_kj: 844 },
    { id: 34, timestamp: ago(24, 14), duration_ms: 3300000, balance_left: 47.8, balance_right: 52.2, te_left: 70, te_right: 71, ps_left: 19, ps_right: 21, zone_optimal: 52, zone_attention: 32, zone_problem: 16, score: 62, power_avg: 185, power_max: 580, cadence_avg: 81, hr_avg: 160, hr_max: 188, speed_avg: 27.5, distance_km: 25.2, elevation_gain: 490, elevation_loss: 485, grade_avg: 2.8, grade_max: 15.2, normalized_power: 202, energy_kj: 611 },
    { id: 33, timestamp: ago(25, 14), duration_ms: 4800000, balance_left: 50.1, balance_right: 49.9, te_left: 75, te_right: 76, ps_left: 23, ps_right: 24, zone_optimal: 78, zone_attention: 18, zone_problem: 4, score: 85, power_avg: 158, power_max: 455, cadence_avg: 87, hr_avg: 140, hr_max: 168, speed_avg: 29.2, distance_km: 39.0, elevation_gain: 365, elevation_loss: 360, grade_avg: 1.2, grade_max: 7.5, normalized_power: 168, energy_kj: 758 },
    { id: 32, timestamp: ago(26, 14), duration_ms: 2700000, balance_left: 48.5, balance_right: 51.5, te_left: 69, te_right: 70, ps_left: 18, ps_right: 20, zone_optimal: 45, zone_attention: 35, zone_problem: 20, score: 58, power_avg: 192, power_max: 640, cadence_avg: 80, hr_avg: 168, hr_max: 198, speed_avg: 26.8, distance_km: 20.1, elevation_gain: 520, elevation_loss: 515, grade_avg: 3.5, grade_max: 17.8, normalized_power: 220, energy_kj: 518 },
    { id: 31, timestamp: ago(27, 14), duration_ms: 4200000, balance_left: 49.2, balance_right: 50.8, te_left: 72, te_right: 74, ps_left: 21, ps_right: 22, zone_optimal: 65, zone_attention: 26, zone_problem: 9, score: 72, power_avg: 172, power_max: 515, cadence_avg: 85, hr_avg: 148, hr_max: 176, speed_avg: 29.0, distance_km: 33.8, elevation_gain: 345, elevation_loss: 340, grade_avg: 1.5, grade_max: 9.2, normalized_power: 184, energy_kj: 722 }
  ];

  return {
    rides,
    total: 27,
    limit: 100,
    offset: 0
  };
}

/** Generate demo achievements data with dynamic timestamps */
export function getDemoAchievements() {
  return {
    achievements: [
      { id: 20, achievement_id: 'zone_warrior', unlocked_at: ago(1) },
      { id: 19, achievement_id: 'twenty_rides', unlocked_at: ago(4) },
      { id: 18, achievement_id: 'drill_master', unlocked_at: ago(6) },
      { id: 17, achievement_id: 'century_km', unlocked_at: ago(8) },
      { id: 16, achievement_id: 'hour_power', unlocked_at: ago(10) },
      { id: 15, achievement_id: 'high_score', unlocked_at: ago(14) },
      { id: 14, achievement_id: 'ten_rides', unlocked_at: ago(15) },
      { id: 13, achievement_id: 'consistency_week', unlocked_at: ago(17) },
      { id: 12, achievement_id: 'first_drill', unlocked_at: ago(20) },
      { id: 10, achievement_id: 'te_master', unlocked_at: ago(22) },
      { id: 11, achievement_id: 'five_rides', unlocked_at: ago(22) },
      { id: 9, achievement_id: 'perfect_balance', unlocked_at: ago(25) },
      { id: 8, achievement_id: 'first_ride', unlocked_at: ago(27) }
    ],
    total: 13,
    stats: {
      summary: { unlocked_count: 13, first_unlocked_at: ago(27), last_unlocked_at: ago(1) },
      recent: [
        { achievement_id: 'zone_warrior', unlocked_at: ago(1) },
        { achievement_id: 'twenty_rides', unlocked_at: ago(4) },
        { achievement_id: 'drill_master', unlocked_at: ago(6) },
        { achievement_id: 'century_km', unlocked_at: ago(8) },
        { achievement_id: 'hour_power', unlocked_at: ago(10) }
      ]
    }
  };
}
