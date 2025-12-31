export interface Snapshot {
  minute_index: number;
  timestamp: number;
  balance_left: number;
  balance_right: number;
  te_left: number;
  te_right: number;
  ps_left: number;
  ps_right: number;
  power_avg: number;
  cadence_avg: number;
  hr_avg: number;
  zone_status: 'OPTIMAL' | 'ATTENTION' | 'PROBLEM';
}

export interface RideDetail {
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
  power_max: number;
  cadence_avg: number;
  hr_avg: number;
  hr_max: number;
  speed_avg: number;
  distance_km: number;
  elevation_gain: number;
  elevation_loss: number;
  grade_avg: number;
  grade_max: number;
  normalized_power: number;
  energy_kj: number;
  notes: string | null;
  rating: number | null;
  snapshots: Snapshot[];
}

export interface PowerZoneStats {
  zone: string;
  label: string;
  color: string;
  minutes: number;
  avgTe: number;
  avgPs: number;
  avgBalance: number;
}

export interface FatigueMetric {
  first: number;
  last: number;
  delta: number;
}

export interface FatigueData {
  balance: FatigueMetric;
  te: FatigueMetric;
  ps: FatigueMetric;
}

export interface TechniqueStats {
  teSymmetry: number;
  psSymmetry: number;
  teStability: number;
  psStability: number;
  teOptimalTime: number;
  psOptimalTime: number;
}

export interface ChartPoint {
  x: number;
  y: number;
  value: number;
  minute: number;
}

export interface ChartData {
  path: string;
  areaPath: string;
  min: number;
  max: number;
  avg: number;
  points: ChartPoint[];
}

// Locale helper
export function getLocaleString(currentLocale: string | null | undefined): string {
  const localeMap: Record<string, string> = { en: 'en-US', es: 'es-ES' };
  return currentLocale ? localeMap[currentLocale] || currentLocale : 'en-US';
}

// Format duration
export function formatDuration(ms: number): string {
  const h = Math.floor(ms / 3600000);
  const m = Math.floor((ms % 3600000) / 60000);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

// Format date
export function formatDate(ts: number, currentLocale?: string | null): string {
  return new Date(ts).toLocaleDateString(getLocaleString(currentLocale), {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric'
  });
}

// Format time
export function formatTime(ts: number, currentLocale?: string | null): string {
  return new Date(ts).toLocaleTimeString(getLocaleString(currentLocale), {
    hour: 'numeric',
    minute: '2-digit'
  });
}

// Get asymmetry value
export function getAsymmetry(bal: number): number {
  return Math.abs(bal - 50);
}

// Status helpers
export function getBalanceStatus(bal: number): string {
  const a = Math.abs(bal - 50);
  if (a <= 2.5) return 'optimal';
  if (a <= 5) return 'attention';
  return 'problem';
}

export function getTeStatus(te: number): string {
  if (te >= 70 && te <= 80) return 'optimal';
  if (te >= 60 && te <= 85) return 'attention';
  return 'problem';
}

export function getPsStatus(ps: number): string {
  if (ps >= 20) return 'optimal';
  if (ps >= 15) return 'attention';
  return 'problem';
}

export function getScoreStatus(s: number): string {
  if (s >= 85) return 'optimal';
  if (s >= 70) return 'attention';
  return 'problem';
}

// Fatigue status
export function getFatigueStatus(metric: 'balance' | 'te' | 'ps', delta: number): 'improved' | 'stable' | 'degraded' {
  if (metric === 'balance') {
    if (delta <= -0.5) return 'improved';
    if (delta >= 0.5) return 'degraded';
    return 'stable';
  } else {
    if (delta >= 1) return 'improved';
    if (delta <= -1) return 'degraded';
    return 'stable';
  }
}

// Calculate fatigue data
export function calculateFatigue(snapshots: Snapshot[]): FatigueData {
  const third = Math.floor(snapshots.length / 3);
  const first = snapshots.slice(0, third);
  const last = snapshots.slice(-third);
  const avg = (arr: Snapshot[], fn: (s: Snapshot) => number) =>
    arr.reduce((a, s) => a + fn(s), 0) / arr.length;

  const firstBal = avg(first, s => Math.abs(s.balance_left - 50));
  const lastBal = avg(last, s => Math.abs(s.balance_left - 50));
  const firstTe = avg(first, s => (s.te_left + s.te_right) / 2);
  const lastTe = avg(last, s => (s.te_left + s.te_right) / 2);
  const firstPs = avg(first, s => (s.ps_left + s.ps_right) / 2);
  const lastPs = avg(last, s => (s.ps_left + s.ps_right) / 2);

  return {
    balance: { first: firstBal, last: lastBal, delta: lastBal - firstBal },
    te: { first: firstTe, last: lastTe, delta: lastTe - firstTe },
    ps: { first: firstPs, last: lastPs, delta: lastPs - firstPs }
  };
}

// Calculate technique stats
export function calculateTechniqueStats(ride: RideDetail): TechniqueStats {
  const teSymmetry = Math.abs(ride.te_left - ride.te_right);
  const psSymmetry = Math.abs(ride.ps_left - ride.ps_right);

  let teStability = 0;
  let psStability = 0;

  if (ride.snapshots?.length >= 3) {
    const teValues = ride.snapshots.map(s => (s.te_left + s.te_right) / 2);
    const psValues = ride.snapshots.map(s => (s.ps_left + s.ps_right) / 2);

    const teAvg = teValues.reduce((a, b) => a + b, 0) / teValues.length;
    const psAvg = psValues.reduce((a, b) => a + b, 0) / psValues.length;

    const teStdDev = Math.sqrt(teValues.reduce((sum, v) => sum + (v - teAvg) ** 2, 0) / teValues.length);
    const psStdDev = Math.sqrt(psValues.reduce((sum, v) => sum + (v - psAvg) ** 2, 0) / psValues.length);

    teStability = Math.max(0, 100 - (teStdDev / teAvg) * 100);
    psStability = Math.max(0, 100 - (psStdDev / psAvg) * 100);
  }

  let teOptimalTime = 0;
  let psOptimalTime = 0;

  if (ride.snapshots?.length > 0) {
    const teOptimal = ride.snapshots.filter(s => {
      const te = (s.te_left + s.te_right) / 2;
      return te >= 70 && te <= 80;
    }).length;
    const psOptimal = ride.snapshots.filter(s => {
      const ps = (s.ps_left + s.ps_right) / 2;
      return ps >= 20;
    }).length;

    teOptimalTime = (teOptimal / ride.snapshots.length) * 100;
    psOptimalTime = (psOptimal / ride.snapshots.length) * 100;
  }

  return { teSymmetry, psSymmetry, teStability, psStability, teOptimalTime, psOptimalTime };
}

// Calculate power zones
export function calculatePowerZones(snapshots: Snapshot[]): PowerZoneStats[] {
  const withPower = snapshots.filter(s => s.power_avg > 0);
  if (withPower.length < 3) return [];

  const avgPower = withPower.reduce((a, s) => a + s.power_avg, 0) / withPower.length;
  const estimatedFTP = avgPower / 0.75;

  const zones = [
    { name: 'recovery', label: 'Recovery', color: '#94A3B8', min: 0, max: 0.55 },
    { name: 'endurance', label: 'Endurance', color: '#3B82F6', min: 0.55, max: 0.75 },
    { name: 'tempo', label: 'Tempo', color: '#22C55E', min: 0.75, max: 0.90 },
    { name: 'threshold', label: 'Threshold', color: '#F59E0B', min: 0.90, max: 1.05 },
    { name: 'vo2max', label: 'VO2max', color: '#EF4444', min: 1.05, max: 1.20 },
    { name: 'anaerobic', label: 'Anaerobic', color: '#7C3AED', min: 1.20, max: Infinity }
  ];

  const result: PowerZoneStats[] = [];

  for (const zone of zones) {
    const inZone = withPower.filter(s => {
      const pctFTP = s.power_avg / estimatedFTP;
      return pctFTP >= zone.min && pctFTP < zone.max;
    });

    if (inZone.length === 0) continue;

    const avgTe = inZone.reduce((a, s) => a + (s.te_left + s.te_right) / 2, 0) / inZone.length;
    const avgPs = inZone.reduce((a, s) => a + (s.ps_left + s.ps_right) / 2, 0) / inZone.length;
    const avgBalance = inZone.reduce((a, s) => a + Math.abs(s.balance_left - 50), 0) / inZone.length;

    result.push({
      zone: zone.name,
      label: zone.label,
      color: zone.color,
      minutes: inZone.length,
      avgTe,
      avgPs,
      avgBalance
    });
  }

  return result;
}

// Get chart data
export function getChartData(
  snapshots: Snapshot[],
  metric: string,
  width: number,
  height: number = 80,
  padding: number = 8
): ChartData {
  if (!snapshots?.length) {
    return { path: '', areaPath: '', min: 0, max: 100, avg: 0, points: [] };
  }

  const values = snapshots.map(s => {
    if (metric === 'balance') return Math.abs(s.balance_left - 50);
    if (metric === 'te') return (s.te_left + s.te_right) / 2;
    if (metric === 'ps') return (s.ps_left + s.ps_right) / 2;
    if (metric === 'power') return s.power_avg || 0;
    return s.hr_avg || 0;
  });

  const min = Math.min(...values);
  const max = Math.max(...values);
  const avg = values.reduce((a, b) => a + b, 0) / values.length;
  const range = max - min || 1;
  const pad = range * 0.15;
  const cMin = Math.max(0, min - pad);
  const cMax = max + pad;
  const cRange = cMax - cMin;
  const usableWidth = width - padding * 2;

  const coords = values.map((v, i) => ({
    x: padding + (i / Math.max(1, values.length - 1)) * usableWidth,
    y: height - padding - ((v - cMin) / cRange) * (height - padding * 2),
    value: v,
    minute: i
  }));

  const path = coords.length > 0 ? `M${coords.map(c => `${c.x},${c.y}`).join(' L')}` : '';
  const areaPath = coords.length > 0
    ? `M${coords[0].x},${height - padding} L${coords.map(c => `${c.x},${c.y}`).join(' L')} L${coords[coords.length - 1].x},${height - padding} Z`
    : '';

  return { path, areaPath, min, max, avg, points: coords };
}

// Get chart color
export function getChartColor(metric: string): string {
  if (metric === 'te') return 'var(--color-optimal)';
  if (metric === 'ps') return 'var(--color-attention)';
  if (metric === 'power') return '#8B5CF6';
  if (metric === 'hr') return '#EF4444';
  return 'var(--color-accent)';
}
