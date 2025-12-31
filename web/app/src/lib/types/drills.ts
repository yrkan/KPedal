export interface DrillResult {
  id: number;
  drill_id: string;
  drill_name: string;
  timestamp: number;
  duration_ms: number;
  score: number;
  time_in_target_ms: number;
  time_in_target_percent: number;
  completed: boolean;
}

export interface DrillStats {
  total_drills: number;
  completed_drills: number;
  avg_score: number;
  best_score: number;
  total_duration_ms: number;
  drill_types_tried: number;
}

export interface DrillTypeStats {
  drill_id: string;
  drill_name: string;
  attempts: number;
  completions: number;
  avg_score: number;
  best_score: number;
  last_attempt: number;
}

export interface DrillDashboardStats {
  summary: DrillStats;
  by_drill: DrillTypeStats[];
}

// Utility functions
export function getLocaleString(currentLocale: string | null | undefined): string {
  const localeMap: Record<string, string> = { en: 'en-US', es: 'es-ES' };
  return currentLocale ? localeMap[currentLocale] || currentLocale : 'en-US';
}

export function formatDrillDuration(ms: number): string {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  if (minutes > 0) return `${minutes}m ${seconds}s`;
  return `${seconds}s`;
}

export function formatTotalDuration(ms: number): string {
  const hours = Math.floor(ms / 3600000);
  const minutes = Math.floor((ms % 3600000) / 60000);
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

export function formatDrillDate(timestamp: number, currentLocale?: string | null): string {
  return new Date(timestamp).toLocaleDateString(getLocaleString(currentLocale), { month: 'short', day: 'numeric' });
}

export function formatDrillFullDate(
  timestamp: number,
  currentLocale?: string | null,
  todayLabel?: string,
  yesterdayLabel?: string
): string {
  const date = new Date(timestamp);
  const now = new Date();
  const diffDays = Math.floor((now.getTime() - date.getTime()) / 86400000);

  if (diffDays === 0) return todayLabel || 'Today';
  if (diffDays === 1) return yesterdayLabel || 'Yesterday';
  if (diffDays < 7) return date.toLocaleDateString(getLocaleString(currentLocale), { weekday: 'short' });

  return date.toLocaleDateString(getLocaleString(currentLocale), { month: 'short', day: 'numeric' });
}

export function formatDrillTime(timestamp: number, currentLocale?: string | null): string {
  return new Date(timestamp).toLocaleTimeString(getLocaleString(currentLocale), { hour: 'numeric', minute: '2-digit' });
}

export function getTargetStatus(percent: number): string {
  if (percent >= 80) return 'optimal';
  if (percent >= 60) return 'attention';
  return 'problem';
}

export function getProgressTrend(drillStats: DrillTypeStats): 'improving' | 'stable' | 'declining' | 'unknown' {
  const completionRate = drillStats.completions / drillStats.attempts;
  if (completionRate >= 0.8) return 'improving';
  if (completionRate >= 0.5) return 'stable';
  return 'declining';
}
