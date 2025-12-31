/**
 * Dashboard Types
 * Extracted from +page.svelte for reusability across components
 */

export interface Stats {
  total_rides: number;
  avg_balance_left: number;
  avg_balance_right: number;
  avg_te_left: number;
  avg_te_right: number;
  avg_ps_left: number;
  avg_ps_right: number;
  avg_score: number;
  total_duration_ms: number;
  avg_zone_optimal: number;
  avg_zone_attention: number;
  avg_zone_problem: number;
  best_score: number;
  best_balance_diff: number;
  avg_power: number;
  max_power: number;
  avg_cadence: number;
  avg_hr: number;
  avg_speed?: number;
  total_distance_km: number;
}

export interface Ride {
  id: number;
  timestamp: number;
  duration_ms: number;
  balance_left: number;
  balance_right: number;
  te_left: number;
  te_right: number;
  ps_left: number;
  ps_right: number;
  zone_optimal: number;
  zone_attention: number;
  zone_problem: number;
  score: number;
  power_avg: number;
  power_max?: number;
  cadence_avg: number;
  hr_avg: number;
  hr_max?: number;
  speed_avg?: number;
  distance_km: number;
  // Pro cyclist metrics (optional - not always available)
  elevation_gain?: number;
  elevation_loss?: number;
  grade_avg?: number;
  grade_max?: number;
  normalized_power?: number;
  energy_kj?: number;
}

export interface WeeklyComparison {
  thisWeek: {
    rides_count: number;
    avg_score: number;
    avg_balance_left: number;
    avg_te: number;
    avg_ps: number;
    avg_zone_optimal: number;
    total_duration_ms: number;
    avg_power: number;
    total_distance_km: number;
    total_elevation: number;
    total_energy_kj: number;
  };
  lastWeek: {
    rides_count: number;
    avg_score: number;
    avg_balance_left: number;
    avg_te: number;
    avg_ps: number;
    avg_zone_optimal: number;
    total_duration_ms: number;
    avg_power: number;
    total_distance_km: number;
    total_elevation: number;
    total_energy_kj: number;
  };
  changes: {
    score: number;
    zone_optimal: number;
    rides_count: number;
    power: number;
    distance: number;
    duration: number;
    elevation: number;
    te: number;
    ps: number;
    balance: number;
    energy: number;
  };
}

export interface TrendData {
  date: string;
  rides_count: number;
  avg_balance_left: number;
  avg_te: number;
  avg_ps: number;
  avg_score: number;
  avg_zone_optimal: number;
}

export interface FatigueAnalysis {
  hasData: boolean;
  firstThird: { balance: number; te: number; ps: number; power: number };
  lastThird: { balance: number; te: number; ps: number; power: number };
  degradation: { balance: number; te: number; ps: number };
}

export interface BalancePoint {
  x: number;
  y: number;
  balance: number;
  date: string;
  rideId: number;
  status: 'optimal' | 'attention' | 'problem';
}

export interface BalanceTrendResult {
  path: string;
  areaPath: string;
  movingAvgPath: string;
  avgLine: number;
  minBalance: number;
  maxBalance: number;
  points: BalancePoint[];
  optimalZone: { y1: number; y2: number };
}

export interface TechniquePoint {
  x: number;
  y: number;
  value: number;
  date: string;
  status: 'optimal' | 'attention' | 'problem';
}

export interface TechniqueTrendResult {
  path: string;
  areaPath: string;
  movingAvgPath: string;
  min: number;
  max: number;
  points: TechniquePoint[];
  optimalZone: { y1: number; y2: number } | null;
}

export interface PeriodStats {
  rides: number;
  duration: number;
  balance: number;
  score: number;
  zoneOptimal: number;
  zoneAttention: number;
  zoneProblem: number;
  te: number;
  ps: number;
  // Cycling metrics
  avgPower: number;
  maxPower: number;
  avgCadence: number;
  avgHr: number;
  maxHr: number;
  totalDistance: number;
  avgSpeed: number;
  // Pro cyclist metrics
  totalElevationGain: number;
  totalElevationLoss: number;
  maxGrade: number;
  avgNormalizedPower: number;
  totalEnergy: number;
}

export interface PrevPeriodStats {
  score: number;
  balance: number;
  zoneOptimal: number;
  rides: number;
  te: number;
  ps: number;
  avgPower: number;
  duration: number;
  distance: number;
  elevation: number;
}

export interface Progress {
  value: number;
  direction: 'up' | 'down' | 'same';
}

export interface Insight {
  type: 'win' | 'action' | 'tip';
  icon: string;
  text: string;
  link?: { label: string; href: string };
}

export interface InsightTranslations {
  fatigueAction: string;
  fatigueLink: string;
  balanceAction: string;
  balanceLink: string;
  leftLeg: string;
  rightLeg: string;
  leftDominant: string;
  rightDominant: string;
  lowTeAction: string;
  lowTeLink: string;
  lowPsAction: string;
  lowPsLink: string;
  greatBalance: string;
  teOptimal: string;
  excellentScore: string;
  goodConsistency: string;
  improvement: string;
  encourageRides: string;
  ride: string;
  ridesPl: string;
  nearOptimalTe: string;
}

// Snapshot type for fatigue analysis
export interface RideSnapshot {
  balance_left: number;
  te_left: number;
  te_right: number;
  ps_left: number;
  ps_right: number;
  power_avg: number;
}
