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
  power_max: number;
  cadence_avg: number;
  hr_avg: number;
  hr_max: number;
  speed_avg: number;
  distance_km: number;
  // Pro cyclist metrics
  elevation_gain: number;
  elevation_loss: number;
  grade_avg: number;
  grade_max: number;
  normalized_power: number;
  energy_kj: number;
}

export interface PeriodStats {
  rides: number;
  totalDuration: number;
  avgAsymmetry: number;
  avgTe: number;
  avgPs: number;
  avgOptimal: number;
  avgPower: number;
  totalDistance: number;
  totalElevation: number;
  avgNP: number;
  totalEnergy: number;
}

// Utility functions
export function getAsymmetry(ride: Ride): number {
  return Math.abs(ride.balance_left - 50);
}

export function getAsymmetryClass(asymmetry: number): string {
  if (asymmetry <= 2.5) return 'optimal';
  if (asymmetry <= 5) return 'attention';
  return 'problem';
}

export function getTeClass(te: number): string {
  if (te >= 70 && te <= 80) return 'optimal';
  if (te >= 60 && te <= 85) return 'attention';
  return 'problem';
}

export function getPsClass(ps: number): string {
  if (ps >= 20) return 'optimal';
  if (ps >= 15) return 'attention';
  return 'problem';
}

export function formatDuration(ms: number): string {
  const hours = Math.floor(ms / 3600000);
  const minutes = Math.floor((ms % 3600000) / 60000);
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

export function formatTotalDuration(ms: number): string {
  const hours = Math.floor(ms / 3600000);
  const minutes = Math.floor((ms % 3600000) / 60000);
  if (hours >= 100) return `${hours}h`;
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

export function getLocaleString(currentLocale: string | null | undefined): string {
  const localeMap: Record<string, string> = { en: 'en-US', es: 'es-ES' };
  return currentLocale ? localeMap[currentLocale] || currentLocale : 'en-US';
}

export function formatDate(timestamp: number, currentLocale?: string | null): string {
  const date = new Date(timestamp);
  return date.toLocaleDateString(getLocaleString(currentLocale), { month: 'short', day: 'numeric' });
}

export function formatFullDate(timestamp: number, currentLocale?: string | null): string {
  const date = new Date(timestamp);
  return date.toLocaleDateString(getLocaleString(currentLocale), { weekday: 'short', month: 'short', day: 'numeric' });
}

export function formatTime(timestamp: number, currentLocale?: string | null): string {
  return new Date(timestamp).toLocaleTimeString(getLocaleString(currentLocale), { hour: 'numeric', minute: '2-digit' });
}

export function getDominance(ride: Ride, leftLabel: string, rightLabel: string): string {
  if (Math.abs(ride.balance_left - 50) < 0.5) return '—';
  return ride.balance_left > 50 ? leftLabel : rightLabel;
}

export function calculatePeriodStats(ridesData: Ride[]): PeriodStats {
  if (ridesData.length === 0) {
    return { rides: 0, totalDuration: 0, avgAsymmetry: 0, avgTe: 0, avgPs: 0, avgOptimal: 0, avgPower: 0, totalDistance: 0, totalElevation: 0, avgNP: 0, totalEnergy: 0 };
  }

  const totalDuration = ridesData.reduce((sum, r) => sum + r.duration_ms, 0);
  const avgAsymmetry = ridesData.reduce((sum, r) => sum + Math.abs(r.balance_left - 50), 0) / ridesData.length;
  const avgTe = ridesData.reduce((sum, r) => sum + (r.te_left + r.te_right) / 2, 0) / ridesData.length;
  const avgPs = ridesData.reduce((sum, r) => sum + (r.ps_left + r.ps_right) / 2, 0) / ridesData.length;
  const avgOptimal = ridesData.reduce((sum, r) => sum + r.zone_optimal, 0) / ridesData.length;
  const ridesWithPower = ridesData.filter(r => r.power_avg > 0);
  const avgPower = ridesWithPower.length > 0 ? ridesWithPower.reduce((sum, r) => sum + r.power_avg, 0) / ridesWithPower.length : 0;
  const totalDistance = ridesData.reduce((sum, r) => sum + (r.distance_km || 0), 0);
  const totalElevation = ridesData.reduce((sum, r) => sum + (r.elevation_gain || 0), 0);
  const ridesWithNP = ridesData.filter(r => r.normalized_power > 0);
  const avgNP = ridesWithNP.length > 0 ? ridesWithNP.reduce((sum, r) => sum + r.normalized_power, 0) / ridesWithNP.length : 0;
  const totalEnergy = ridesData.reduce((sum, r) => sum + (r.energy_kj || 0), 0);

  return { rides: ridesData.length, totalDuration, avgAsymmetry, avgTe, avgPs, avgOptimal, avgPower, totalDistance, totalElevation, avgNP, totalEnergy };
}
