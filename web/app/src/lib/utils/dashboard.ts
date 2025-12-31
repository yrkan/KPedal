/**
 * Dashboard Utility Functions
 * Extracted from +page.svelte for reusability across components
 */

import type {
  Ride,
  TrendData,
  BalancePoint,
  BalanceTrendResult,
  TechniquePoint,
  TechniqueTrendResult,
  PeriodStats,
  PrevPeriodStats,
  Progress,
  Insight,
  InsightTranslations,
  WeeklyComparison,
  FatigueAnalysis
} from '$lib/types/dashboard';

// Chart constants
export const CHART_HEIGHT = 70;
export const CHART_PADDING = 6;

/**
 * Format duration from milliseconds to human-readable string
 */
export function formatDuration(ms: number): string {
  const hours = Math.floor(ms / 3600000);
  const minutes = Math.floor((ms % 3600000) / 60000);
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
}

/**
 * Convert locale code to browser-compatible locale string
 */
export function getLocaleString(currentLocale: string | null | undefined): string {
  const localeMap: Record<string, string> = { en: 'en-US', es: 'es-ES' };
  return currentLocale ? localeMap[currentLocale] || currentLocale : 'en-US';
}

/**
 * Format timestamp to localized date string
 */
export function formatDate(timestamp: number, currentLocale?: string | null): string {
  return new Date(timestamp).toLocaleDateString(getLocaleString(currentLocale), { month: 'short', day: 'numeric' });
}

/**
 * Format timestamp to relative time (today, yesterday, X days ago)
 */
export function formatRelativeTime(
  timestamp: number,
  translations: { today: string; yesterday: string; daysAgo: string },
  currentLocale?: string | null
): string {
  const now = Date.now();
  const diff = now - timestamp;
  const days = Math.floor(diff / (24 * 60 * 60 * 1000));
  if (days === 0) return translations.today;
  if (days === 1) return translations.yesterday;
  if (days < 7) return translations.daysAgo.replace('{days}', String(days));
  return formatDate(timestamp, currentLocale);
}

/**
 * Get balance status based on deviation from 50%
 */
export function getBalanceStatus(left: number): 'optimal' | 'attention' | 'problem' {
  const diff = Math.abs(left - 50);
  if (diff <= 2.5) return 'optimal';
  if (diff <= 5) return 'attention';
  return 'problem';
}

/**
 * Get score status based on value
 */
export function getScoreStatus(score: number): 'optimal' | 'attention' | 'problem' {
  if (score >= 80) return 'optimal';
  if (score >= 60) return 'attention';
  return 'problem';
}

/**
 * Extract first name from full name
 */
export function getFirstName(name: string | undefined): string {
  return name?.split(' ')[0] || 'Cyclist';
}

/**
 * Filter rides by number of days from now
 */
export function getFilteredRides(rides: Ride[], days: number): Ride[] {
  const cutoff = Date.now() - days * 24 * 60 * 60 * 1000;
  return rides.filter(r => r.timestamp >= cutoff);
}

/**
 * Get week day names for the last 7 days
 */
export function getWeekDays(currentLocale: string | null | undefined): string[] {
  const days = [];
  const localeMap: Record<string, string> = { en: 'en-US', es: 'es-ES' };
  const browserLocale = currentLocale ? localeMap[currentLocale] || currentLocale : 'en-US';
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    days.push(d.toLocaleDateString(browserLocale, { weekday: 'short' }));
  }
  return days;
}

/**
 * Get ride counts per day for the last 7 days
 */
export function getRidesPerDay(rides: Ride[]): number[] {
  const counts = [0, 0, 0, 0, 0, 0, 0];
  const now = new Date();
  rides.forEach(r => {
    const rideDate = new Date(r.timestamp);
    const diffDays = Math.floor((now.getTime() - rideDate.getTime()) / (24 * 60 * 60 * 1000));
    if (diffDays >= 0 && diffDays < 7) {
      counts[6 - diffDays]++;
    }
  });
  return counts;
}

/**
 * Calculate balance trend data for SVG chart
 */
export function getBalanceTrendData(
  rides: Ride[],
  maxPoints: number = 20,
  chartWidth: number = 300,
  currentLocale?: string | null
): BalanceTrendResult {
  const empty: BalanceTrendResult = {
    path: '', areaPath: '', movingAvgPath: '', avgLine: 30,
    minBalance: 50, maxBalance: 50, points: [],
    optimalZone: { y1: 0, y2: 0 }
  };
  if (rides.length === 0) return empty;
  const sorted = [...rides].sort((a, b) => a.timestamp - b.timestamp).slice(-maxPoints);
  if (sorted.length < 2) return empty;

  const balances = sorted.map(r => r.balance_left);
  const avgBalance = balances.reduce((a, b) => a + b, 0) / balances.length;
  const minBalance = Math.min(...balances);
  const maxBalance = Math.max(...balances);

  const width = chartWidth;
  const height = CHART_HEIGHT;
  const padding = CHART_PADDING;
  const browserLocale = getLocaleString(currentLocale);

  // Calculate optimal zone (47.5-52.5%)
  const optimalY1 = height - padding - ((52.5 - 40) / 20) * (height - 2 * padding);
  const optimalY2 = height - padding - ((47.5 - 40) / 20) * (height - 2 * padding);

  const dataPoints: BalancePoint[] = sorted.map((r, i) => {
    const x = padding + (i / (sorted.length - 1)) * (width - 2 * padding);
    const y = height - padding - ((r.balance_left - 40) / 20) * (height - 2 * padding);
    const clampedY = Math.max(padding, Math.min(height - padding, y));
    const deviation = Math.abs(r.balance_left - 50);
    return {
      x,
      y: clampedY,
      balance: r.balance_left,
      date: new Date(r.timestamp).toLocaleDateString(browserLocale, { month: 'short', day: 'numeric' }),
      rideId: r.id,
      status: deviation <= 2.5 ? 'optimal' : deviation <= 5 ? 'attention' : 'problem'
    };
  });

  // Calculate 3-point moving average
  const movingAvgPoints: { x: number; y: number }[] = [];
  for (let i = 0; i < dataPoints.length; i++) {
    const start = Math.max(0, i - 1);
    const end = Math.min(dataPoints.length, i + 2);
    const slice = balances.slice(start, end);
    const avg = slice.reduce((a, b) => a + b, 0) / slice.length;
    const y = height - padding - ((avg - 40) / 20) * (height - 2 * padding);
    movingAvgPoints.push({ x: dataPoints[i].x, y: Math.max(padding, Math.min(height - padding, y)) });
  }
  const movingAvgPath = movingAvgPoints.length >= 2
    ? `M${movingAvgPoints.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' L')}`
    : '';

  const pathPoints = dataPoints.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`);
  const linePath = `M${pathPoints.join(' L')}`;
  const areaPath = `${linePath} L${dataPoints[dataPoints.length - 1].x.toFixed(1)},${height} L${dataPoints[0].x.toFixed(1)},${height} Z`;

  return {
    path: linePath,
    areaPath,
    movingAvgPath,
    avgLine: height - padding - ((avgBalance - 40) / 20) * (height - 2 * padding),
    minBalance,
    maxBalance,
    points: dataPoints,
    optimalZone: { y1: optimalY1, y2: optimalY2 }
  };
}

/**
 * Calculate technique trend data for SVG chart
 */
export function getTechniqueTrendData(
  trends: TrendData[],
  metric: 'asymmetry' | 'te' | 'ps',
  chartWidth: number = 300,
  currentLocale?: string | null
): TechniqueTrendResult {
  const empty: TechniqueTrendResult = {
    path: '', areaPath: '', movingAvgPath: '', min: 0, max: 100, points: [], optimalZone: null
  };
  if (!trends || trends.length < 2) return empty;

  const sorted = [...trends].sort((a, b) => a.date.localeCompare(b.date));
  const browserLocale = getLocaleString(currentLocale);

  const values = sorted.map(t => {
    if (metric === 'asymmetry') return Math.abs(t.avg_balance_left - 50);
    if (metric === 'te') return t.avg_te;
    return t.avg_ps;
  });

  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const dataPad = range * 0.15;
  const chartMin = Math.max(0, min - dataPad);
  const chartMax = max + dataPad;
  const chartRange = chartMax - chartMin;

  const width = chartWidth;
  const height = CHART_HEIGHT;
  const xPad = CHART_PADDING;
  const yPad = CHART_PADDING;

  // Calculate optimal zones based on metric
  let optimalZone: { y1: number; y2: number } | null = null;
  if (metric === 'asymmetry') {
    // Optimal: 0-2.5%
    const y1 = height - yPad - ((2.5 - chartMin) / chartRange) * (height - 2 * yPad);
    const y2 = height - yPad - ((0 - chartMin) / chartRange) * (height - 2 * yPad);
    optimalZone = { y1: Math.max(yPad, y1), y2: Math.min(height - yPad, y2) };
  } else if (metric === 'te') {
    // Optimal: 70-80%
    const y1 = height - yPad - ((80 - chartMin) / chartRange) * (height - 2 * yPad);
    const y2 = height - yPad - ((70 - chartMin) / chartRange) * (height - 2 * yPad);
    if (chartMax >= 70 && chartMin <= 80) {
      optimalZone = { y1: Math.max(yPad, y1), y2: Math.min(height - yPad, y2) };
    }
  } else {
    // PS Optimal: >= 20%
    const y1 = height - yPad - ((chartMax - chartMin) / chartRange) * (height - 2 * yPad);
    const y2 = height - yPad - ((20 - chartMin) / chartRange) * (height - 2 * yPad);
    if (chartMax >= 20) {
      optimalZone = { y1: yPad, y2: Math.min(height - yPad, y2) };
    }
  }

  const getStatus = (val: number): 'optimal' | 'attention' | 'problem' => {
    if (metric === 'asymmetry') {
      return val <= 2.5 ? 'optimal' : val <= 5 ? 'attention' : 'problem';
    } else if (metric === 'te') {
      return val >= 70 && val <= 80 ? 'optimal' : val >= 60 ? 'attention' : 'problem';
    } else {
      return val >= 20 ? 'optimal' : val >= 15 ? 'attention' : 'problem';
    }
  };

  const dataPoints: TechniquePoint[] = sorted.map((t, i) => {
    const x = xPad + (i / (values.length - 1)) * (width - 2 * xPad);
    const y = height - yPad - ((values[i] - chartMin) / chartRange) * (height - 2 * yPad);
    return {
      date: new Date(t.date).toLocaleDateString(browserLocale, { month: 'short', day: 'numeric' }),
      value: values[i],
      x,
      y,
      status: getStatus(values[i])
    };
  });

  // Calculate 3-point moving average
  const movingAvgPoints: { x: number; y: number }[] = [];
  for (let i = 0; i < dataPoints.length; i++) {
    const start = Math.max(0, i - 1);
    const end = Math.min(dataPoints.length, i + 2);
    const slice = values.slice(start, end);
    const avg = slice.reduce((a, b) => a + b, 0) / slice.length;
    const y = height - yPad - ((avg - chartMin) / chartRange) * (height - 2 * yPad);
    movingAvgPoints.push({ x: dataPoints[i].x, y });
  }
  const movingAvgPath = movingAvgPoints.length >= 2
    ? `M${movingAvgPoints.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' L')}`
    : '';

  const pathPoints = dataPoints.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`);
  const linePath = `M${pathPoints.join(' L')}`;
  const areaPath = `${linePath} L${dataPoints[dataPoints.length - 1].x.toFixed(1)},${height} L${dataPoints[0].x.toFixed(1)},${height} Z`;

  return {
    path: linePath,
    areaPath,
    movingAvgPath,
    min: chartMin,
    max: chartMax,
    points: dataPoints,
    optimalZone
  };
}

/**
 * Get trend line color based on metric type
 */
export function getTrendColor(metric: 'asymmetry' | 'te' | 'ps'): string {
  if (metric === 'asymmetry') return 'var(--color-accent)';
  if (metric === 'te') return 'var(--color-optimal)';
  return 'var(--color-attention)';
}

/**
 * Get CSS class for asymmetry value
 */
export function getAsymmetryClass(value: number): string {
  if (value <= 2.5) return 'optimal';
  if (value <= 5) return 'attention';
  return 'problem';
}

/**
 * Calculate period statistics from rides
 */
export function getPeriodStats(rides: Ride[], days: number): PeriodStats | null {
  const filtered = getFilteredRides(rides, days);
  if (filtered.length === 0) return null;

  const totalDuration = filtered.reduce((sum, r) => sum + r.duration_ms, 0);
  const avgBalance = filtered.reduce((sum, r) => sum + r.balance_left, 0) / filtered.length;
  const avgScore = filtered.reduce((sum, r) => sum + r.score, 0) / filtered.length;
  const avgOptimal = filtered.reduce((sum, r) => sum + r.zone_optimal, 0) / filtered.length;
  const avgAttention = filtered.reduce((sum, r) => sum + r.zone_attention, 0) / filtered.length;
  const avgProblem = filtered.reduce((sum, r) => sum + r.zone_problem, 0) / filtered.length;
  const avgTe = filtered.reduce((sum, r) => sum + (r.te_left + r.te_right) / 2, 0) / filtered.length;
  const avgPs = filtered.reduce((sum, r) => sum + (r.ps_left + r.ps_right) / 2, 0) / filtered.length;

  // Cycling metrics (filter out zeros for accurate averages)
  const ridesWithPower = filtered.filter(r => r.power_avg > 0);
  const ridesWithCadence = filtered.filter(r => r.cadence_avg > 0);
  const ridesWithHr = filtered.filter(r => r.hr_avg > 0);
  const avgPower = ridesWithPower.length > 0
    ? ridesWithPower.reduce((sum, r) => sum + r.power_avg, 0) / ridesWithPower.length : 0;
  const maxPower = ridesWithPower.length > 0
    ? Math.max(...ridesWithPower.map(r => r.power_max || r.power_avg)) : 0;
  const avgCadence = ridesWithCadence.length > 0
    ? ridesWithCadence.reduce((sum, r) => sum + r.cadence_avg, 0) / ridesWithCadence.length : 0;
  const avgHr = ridesWithHr.length > 0
    ? ridesWithHr.reduce((sum, r) => sum + r.hr_avg, 0) / ridesWithHr.length : 0;
  const maxHr = ridesWithHr.length > 0
    ? Math.max(...ridesWithHr.map(r => r.hr_max || r.hr_avg)) : 0;
  const totalDistance = filtered.reduce((sum, r) => sum + (r.distance_km || 0), 0);
  const avgSpeed = totalDistance > 0 && totalDuration > 0
    ? (totalDistance / (totalDuration / 3600000)) : 0;

  // Pro cyclist metrics - Climbing
  const totalElevationGain = filtered.reduce((sum, r) => sum + (r.elevation_gain || 0), 0);
  const totalElevationLoss = filtered.reduce((sum, r) => sum + (r.elevation_loss || 0), 0);
  const ridesWithGrade = filtered.filter(r => (r.grade_max || 0) > 0);
  const maxGrade = ridesWithGrade.length > 0
    ? Math.max(...ridesWithGrade.map(r => r.grade_max || 0)) : 0;

  // Pro cyclist metrics - Power analytics
  const ridesWithNP = filtered.filter(r => (r.normalized_power || 0) > 0);
  const avgNormalizedPower = ridesWithNP.length > 0
    ? ridesWithNP.reduce((sum, r) => sum + (r.normalized_power || 0), 0) / ridesWithNP.length : 0;
  const totalEnergy = filtered.reduce((sum, r) => sum + (r.energy_kj || 0), 0);

  return {
    rides: filtered.length,
    duration: totalDuration,
    balance: avgBalance,
    score: avgScore,
    zoneOptimal: avgOptimal,
    zoneAttention: avgAttention,
    zoneProblem: avgProblem,
    te: avgTe,
    ps: avgPs,
    // Cycling metrics
    avgPower,
    maxPower,
    avgCadence,
    avgHr,
    maxHr,
    totalDistance,
    avgSpeed,
    // Pro cyclist metrics
    totalElevationGain,
    totalElevationLoss,
    maxGrade,
    avgNormalizedPower,
    totalEnergy
  };
}

/**
 * Calculate previous period statistics for comparison
 */
export function getPreviousPeriodStats(rides: Ride[], days: number): PrevPeriodStats | null {
  const now = Date.now();
  const periodStart = now - days * 24 * 60 * 60 * 1000;
  const previousStart = periodStart - days * 24 * 60 * 60 * 1000;
  const filtered = rides.filter(r => r.timestamp >= previousStart && r.timestamp < periodStart);
  if (filtered.length === 0) return null;

  const avgScore = filtered.reduce((sum, r) => sum + r.score, 0) / filtered.length;
  const avgBalance = filtered.reduce((sum, r) => sum + r.balance_left, 0) / filtered.length;
  const avgOptimal = filtered.reduce((sum, r) => sum + r.zone_optimal, 0) / filtered.length;
  const avgTe = filtered.reduce((sum, r) => sum + (r.te_left + r.te_right) / 2, 0) / filtered.length;
  const avgPs = filtered.reduce((sum, r) => sum + (r.ps_left + r.ps_right) / 2, 0) / filtered.length;
  const ridesWithPower = filtered.filter(r => r.power_avg > 0);
  const avgPower = ridesWithPower.length > 0
    ? ridesWithPower.reduce((sum, r) => sum + r.power_avg, 0) / ridesWithPower.length : 0;
  const totalDuration = filtered.reduce((sum, r) => sum + r.duration_ms, 0);
  const totalDistance = filtered.reduce((sum, r) => sum + (r.distance_km || 0), 0);
  const totalElevation = filtered.reduce((sum, r) => sum + (r.elevation_gain || 0), 0);

  return {
    score: avgScore,
    balance: avgBalance,
    zoneOptimal: avgOptimal,
    rides: filtered.length,
    te: avgTe,
    ps: avgPs,
    avgPower,
    duration: totalDuration,
    distance: totalDistance,
    elevation: totalElevation,
  };
}

/**
 * Calculate progress between two values
 */
export function getProgress(current: number, previous: number, threshold = 0.5): Progress {
  const diff = current - previous;
  if (Math.abs(diff) < threshold) return { value: 0, direction: 'same' };
  return { value: Math.abs(diff), direction: diff > 0 ? 'up' : 'down' };
}

/**
 * Calculate balance progress (closer to 50% is better)
 */
export function getBalanceProgress(current: number, previous: number): Progress {
  const currentDiff = Math.abs(current - 50);
  const prevDiff = Math.abs(previous - 50);
  const change = prevDiff - currentDiff; // positive = improvement
  if (Math.abs(change) < 0.3) return { value: 0, direction: 'same' };
  return { value: Math.abs(change), direction: change > 0 ? 'up' : 'down' };
}

/**
 * Generate coach-like insights based on stats
 */
export function generateInsights(
  stats: PeriodStats | null,
  prevStats: PrevPeriodStats | null,
  weekly: WeeklyComparison | null,
  fatigue: FatigueAnalysis | null,
  tr: InsightTranslations
): Insight[] {
  if (!stats || !weekly) return [];

  const insights: Insight[] = [];
  const asymmetry = Math.abs(stats.balance - 50);
  const dominant = stats.balance > 50 ? 'left' : 'right';
  const weak = stats.balance > 50 ? 'right' : 'left';
  const dominantLabel = dominant === 'left' ? tr.leftDominant : tr.rightDominant;
  const weakLabel = weak === 'left' ? tr.leftDominant : tr.rightDominant;
  const sideLabel = weak === 'left' ? tr.leftLeg : tr.rightLeg;

  // === ACTIONABLE FEEDBACK (priority) ===

  // Fatigue — most actionable insight
  if (fatigue?.hasData && (fatigue.degradation.te > 3 || fatigue.degradation.ps > 2)) {
    insights.push({
      type: 'action',
      icon: '🔋',
      text: tr.fatigueAction,
      link: { label: tr.fatigueLink, href: '/drills' }
    });
  }

  // Balance issue — specific drill for weak leg
  if (asymmetry > 4) {
    insights.push({
      type: 'action',
      icon: '⚖️',
      text: tr.balanceAction.replace('{asymmetry}', asymmetry.toFixed(1)).replace('{dominant}', dominantLabel).replace('{weak}', weakLabel),
      link: { label: tr.balanceLink.replace('{side}', sideLabel), href: '/drills' }
    });
  }

  // Low TE — specific technique tip
  if (stats.te < 65) {
    insights.push({
      type: 'action',
      icon: '⚡',
      text: tr.lowTeAction.replace('{te}', stats.te.toFixed(0)),
      link: { label: tr.lowTeLink, href: '/drills' }
    });
  }

  // Low PS — smoothness tip
  if (stats.ps < 18) {
    insights.push({
      type: 'action',
      icon: '🔄',
      text: tr.lowPsAction.replace('{ps}', stats.ps.toFixed(0)),
      link: { label: tr.lowPsLink, href: '/drills' }
    });
  }

  // === WINS (celebrate progress) ===

  // Great balance
  if (asymmetry <= 2.5) {
    insights.push({
      type: 'win',
      icon: '🎯',
      text: tr.greatBalance.replace('{asymmetry}', asymmetry.toFixed(1))
    });
  }

  // TE in optimal zone
  if (stats.te >= 70 && stats.te <= 80) {
    insights.push({
      type: 'win',
      icon: '✓',
      text: tr.teOptimal.replace('{te}', stats.te.toFixed(0))
    });
  }

  // Excellent score
  if (stats.score >= 85) {
    insights.push({
      type: 'win',
      icon: '🏆',
      text: tr.excellentScore.replace('{score}', String(stats.score))
    });
  }

  // Good consistency
  if (weekly.thisWeek.rides_count >= 4) {
    insights.push({
      type: 'win',
      icon: '🔥',
      text: tr.goodConsistency.replace('{count}', String(weekly.thisWeek.rides_count))
    });
  }

  // Improvement vs previous period
  if (prevStats && stats.score > prevStats.score + 2) {
    insights.push({
      type: 'win',
      icon: '📈',
      text: tr.improvement.replace('{points}', (stats.score - prevStats.score).toFixed(0))
    });
  }

  // === TIPS (coaching advice) ===

  // Encourage more rides
  if (weekly.thisWeek.rides_count < 2 && weekly.lastWeek.rides_count >= 2) {
    const rideWord = weekly.thisWeek.rides_count === 1 ? tr.ride : tr.ridesPl;
    insights.push({
      type: 'tip',
      icon: '📅',
      text: tr.encourageRides.replace('{count}', String(weekly.thisWeek.rides_count)).replace('{rideWord}', rideWord).replace('{lastCount}', String(weekly.lastWeek.rides_count))
    });
  }

  // Near optimal TE
  if (stats.te >= 65 && stats.te < 70) {
    insights.push({
      type: 'tip',
      icon: '💡',
      text: tr.nearOptimalTe.replace('{te}', stats.te.toFixed(0)).replace('{diff}', (70 - stats.te).toFixed(0))
    });
  }

  // Prioritize: actions first, then wins, then tips. Max 3.
  return insights
    .sort((a, b) => {
      const priority = { action: 0, win: 1, tip: 2 };
      return priority[a.type] - priority[b.type];
    })
    .slice(0, 3);
}
