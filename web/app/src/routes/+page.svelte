<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { isAuthenticated, user, authFetch } from '$lib/auth';
  import { theme } from '$lib/theme';
  import { API_URL } from '$lib/config';
  import InfoTip from '$lib/components/InfoTip.svelte';

  interface Stats {
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
    avg_speed: number;
    total_distance_km: number;
  }

  interface Ride {
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

  interface WeeklyComparison {
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

  interface TrendData {
    date: string;
    rides_count: number;
    avg_balance_left: number;
    avg_te: number;
    avg_ps: number;
    avg_score: number;
    avg_zone_optimal: number;
  }

  // State
  let stats: Stats | null = null;
  let recentRides: Ride[] = [];
  let weeklyRides: Ride[] = [];
  let weeklyComparison: WeeklyComparison | null = null;
  let fatigueData: FatigueAnalysis | null = null;
  let trendData: TrendData[] = [];
  let loading = true;
  let error: string | null = null;
  let selectedPeriod: '7' | '30' = '7';
  let activeTrendMetric: 'asymmetry' | 'te' | 'ps' = 'asymmetry';

  // Fatigue analysis data structure
  interface FatigueAnalysis {
    hasData: boolean;
    firstThird: { balance: number; te: number; ps: number; power: number };
    lastThird: { balance: number; te: number; ps: number; power: number };
    degradation: { balance: number; te: number; ps: number };
  }

  // Interactive state for landing
  let activeDataField = 0;
  let activeDrillCategory = 'focus';
  let expandedDrill: string | null = null;

  const dataFields = [
    { id: 'quick', name: 'Quick Glance', desc: 'Status indicator with balance bar. Shows checkmark when optimal or lists metrics needing attention.' },
    { id: 'balance', name: 'Power Balance', desc: 'Large L/R balance numbers with visual progress bar. Perfect for balance-focused training.' },
    { id: 'efficiency', name: 'Efficiency', desc: 'Torque Effectiveness and Pedal Smoothness with L/R splits and averages.' },
    { id: 'full', name: 'Full Overview', desc: 'All three metrics in one compact view. Complete pedaling picture at a glance.' },
    { id: 'trend', name: 'Balance Trend', desc: 'Current + 3s smoothed + 10s smoothed balance. See trends as they develop.' }
  ];

  const drills = {
    focus: [
      { id: 'left', name: 'Left Leg Focus', duration: '30s', level: 'Beginner', target: 'Balance <48%', desc: 'Build single-leg strength awareness by emphasizing your left leg.' },
      { id: 'right', name: 'Right Leg Focus', duration: '30s', level: 'Beginner', target: 'Balance >52%', desc: 'Identify and strengthen your weaker leg with focused practice.' },
      { id: 'smooth', name: 'Smooth Circles', duration: '45s', level: 'Intermediate', target: 'PS ≥22%', desc: 'Eliminate dead spots in your pedal stroke through smooth technique.' },
      { id: 'power', name: 'Power Transfer', duration: '60s', level: 'Intermediate', target: 'TE 70-80%', desc: 'Maximize power output without sacrificing efficiency.' }
    ],
    challenge: [
      { id: 'balance-c', name: 'Balance Challenge', duration: '15s hold', level: 'Intermediate', target: '48-52%', desc: 'Hold perfect 50/50 balance for 15 seconds. Tests symmetry and control.' },
      { id: 'smooth-t', name: 'Smoothness Target', duration: '20s hold', level: 'Advanced', target: 'PS ≥25%', desc: 'Elite-level pedal smoothness. The most demanding technique challenge.' },
      { id: 'cadence', name: 'High Cadence Smooth', duration: '30s', level: 'Advanced', target: 'PS ≥20% @ 100+ rpm', desc: 'Maintain smooth form at high cadence. Ultimate coordination test.' }
    ],
    workout: [
      { id: 'recovery', name: 'Balance Recovery', duration: '5 min', level: 'Beginner', phases: 3, desc: 'Alternate left focus → center → right focus. Helps correct imbalances over time.' },
      { id: 'builder', name: 'Efficiency Builder', duration: '10 min', level: 'Intermediate', phases: 6, desc: 'Sequential blocks: balance → smoothness → TE. Build complete technique.' },
      { id: 'mastery', name: 'Pedaling Mastery', duration: '15 min', level: 'Advanced', phases: 10, desc: 'Comprehensive 10-phase workout covering all aspects of pedaling technique.' }
    ]
  };

  const landingAchievements = [
    { category: 'Rides', items: ['First Ride', '10 Rides', '50 Rides', '100 Rides'] },
    { category: 'Balance', items: ['Perfect 1 min', 'Perfect 5 min', 'Perfect 10 min'] },
    { category: 'Efficiency', items: ['Efficient Rider', 'Smooth Operator'] },
    { category: 'Streaks', items: ['3-Day', '7-Day', '14-Day', '30-Day'] },
    { category: 'Drills', items: ['First Drill', '10 Drills', 'Perfect Score'] }
  ];

  const challenges = [
    { name: 'Active Week', goal: 'Complete 3 rides' },
    { name: 'Balanced Rider', goal: '48-52% balance across 3 rides' },
    { name: 'Zone Master', goal: '60%+ time in optimal zone' },
    { name: 'Technique Focus', goal: 'Complete 2 drills' },
    { name: 'Consistency', goal: 'Ride 4 days in a row' },
    { name: 'Efficient Pedaling', goal: 'Avg TE above 70%' },
    { name: 'Smooth Circles', goal: 'Avg PS above 20%' }
  ];

  async function loadDashboardData() {
    loading = true;
    try {
      // Single API call replaces 6 separate requests (7x faster)
      const res = await authFetch('/rides/dashboard');

      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          stats = data.data.stats;
          recentRides = data.data.recentRides || [];
          weeklyRides = recentRides; // Use recent rides for weekly display
          weeklyComparison = data.data.weeklyComparison;
          trendData = data.data.trends || [];

          // Calculate fatigue from snapshots (included in dashboard response)
          if (data.data.lastRideSnapshots?.length >= 6) {
            fatigueData = calculateFatigueAnalysis(data.data.lastRideSnapshots);
          }
        }
      } else {
        error = 'Failed to load data';
      }
    } catch (err) {
      error = 'Failed to load data';
    } finally {
      loading = false;
    }
  }

  // Calculate fatigue analysis from ride snapshots
  function calculateFatigueAnalysis(snapshots: any[]): FatigueAnalysis {
    const thirdLen = Math.floor(snapshots.length / 3);
    const firstThird = snapshots.slice(0, thirdLen);
    const lastThird = snapshots.slice(-thirdLen);

    const avg = (arr: any[], key: string) => arr.reduce((s, x) => s + (x[key] || 0), 0) / arr.length;

    const first = {
      balance: avg(firstThird, 'balance_left'),
      te: (avg(firstThird, 'te_left') + avg(firstThird, 'te_right')) / 2,
      ps: (avg(firstThird, 'ps_left') + avg(firstThird, 'ps_right')) / 2,
      power: avg(firstThird, 'power_avg'),
    };

    const last = {
      balance: avg(lastThird, 'balance_left'),
      te: (avg(lastThird, 'te_left') + avg(lastThird, 'te_right')) / 2,
      ps: (avg(lastThird, 'ps_left') + avg(lastThird, 'ps_right')) / 2,
      power: avg(lastThird, 'power_avg'),
    };

    return {
      hasData: true,
      firstThird: first,
      lastThird: last,
      degradation: {
        balance: Math.abs(last.balance - 50) - Math.abs(first.balance - 50), // positive = worse
        te: first.te - last.te, // positive = degraded
        ps: first.ps - last.ps, // positive = degraded
      },
    };
  }

  onMount(async () => {
    if (!$isAuthenticated) {
      // On app.kpedal.com redirect to login, on kpedal.com show landing
      const isAppSubdomain = window.location.hostname.startsWith('app.');
      if (isAppSubdomain) {
        goto('/login');
        return;
      }

      // Show landing page
      loading = false;
      const interval = setInterval(() => {
        activeDataField = (activeDataField + 1) % dataFields.length;
      }, 4000);
      return () => clearInterval(interval);
    }

    await loadDashboardData();
  });

  function handleLogin() {
    window.location.href = `${API_URL}/auth/login`;
  }

  function formatDuration(ms: number): string {
    const hours = Math.floor(ms / 3600000);
    const minutes = Math.floor((ms % 3600000) / 60000);
    return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
  }

  function formatDate(timestamp: number): string {
    return new Date(timestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  function formatRelativeTime(timestamp: number): string {
    const now = Date.now();
    const diff = now - timestamp;
    const days = Math.floor(diff / (24 * 60 * 60 * 1000));
    if (days === 0) return 'Today';
    if (days === 1) return 'Yesterday';
    if (days < 7) return `${days} days ago`;
    return formatDate(timestamp);
  }

  function getBalanceStatus(left: number): 'optimal' | 'attention' | 'problem' {
    const diff = Math.abs(left - 50);
    if (diff <= 2.5) return 'optimal';
    if (diff <= 5) return 'attention';
    return 'problem';
  }

  function getScoreStatus(score: number): 'optimal' | 'attention' | 'problem' {
    if (score >= 80) return 'optimal';
    if (score >= 60) return 'attention';
    return 'problem';
  }

  function getFirstName(name: string | undefined): string {
    return name?.split(' ')[0] || 'Cyclist';
  }

  function toggleDrill(id: string) {
    expandedDrill = expandedDrill === id ? null : id;
  }

  // Chart helpers
  function getFilteredRides(rides: Ride[], days: number): Ride[] {
    const cutoff = Date.now() - days * 24 * 60 * 60 * 1000;
    return rides.filter(r => r.timestamp >= cutoff);
  }

  function getWeekDays(): string[] {
    const days = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      days.push(d.toLocaleDateString('en-US', { weekday: 'short' }));
    }
    return days;
  }

  function getRidesPerDay(rides: Ride[]): number[] {
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

  function getBalanceTrendData(rides: Ride[], maxPoints: number = 20): { path: string; areaPath: string; avgLine: number; minBalance: number; maxBalance: number; points: Array<{x: number; y: number; balance: number; date: string}> } {
    if (rides.length === 0) return { path: '', areaPath: '', avgLine: 30, minBalance: 50, maxBalance: 50, points: [] };
    const sorted = [...rides].sort((a, b) => a.timestamp - b.timestamp).slice(-maxPoints);
    if (sorted.length < 2) return { path: '', areaPath: '', avgLine: 30, minBalance: 50, maxBalance: 50, points: [] };

    const balances = sorted.map(r => r.balance_left);
    const avgBalance = balances.reduce((a, b) => a + b, 0) / balances.length;
    const minBalance = Math.min(...balances);
    const maxBalance = Math.max(...balances);

    // Use coordinates that match viewBox="0 0 200 60"
    const width = 200;
    const height = 60;
    const padding = 8;

    const dataPoints = sorted.map((r, i) => {
      const x = padding + (i / (sorted.length - 1)) * (width - 2 * padding);
      // Y axis: 40-60% maps to chart height (inverted - higher values go up)
      const y = height - padding - ((r.balance_left - 40) / 20) * (height - 2 * padding);
      const clampedY = Math.max(padding, Math.min(height - padding, y));
      return {
        x,
        y: clampedY,
        balance: r.balance_left,
        date: new Date(r.timestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
      };
    });

    const pathPoints = dataPoints.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`);
    const linePath = `M${pathPoints.join(' L')}`;
    // Area path for gradient fill
    const areaPath = `${linePath} L${dataPoints[dataPoints.length - 1].x.toFixed(1)},${height} L${dataPoints[0].x.toFixed(1)},${height} Z`;

    return {
      path: linePath,
      areaPath,
      avgLine: height - padding - ((avgBalance - 40) / 20) * (height - 2 * padding),
      minBalance,
      maxBalance,
      points: dataPoints
    };
  }

  // Technique Trends chart helper
  function getTechniqueTrendData(trends: TrendData[], metric: 'asymmetry' | 'te' | 'ps') {
    if (!trends || trends.length < 2) return { path: '', areaPath: '', min: 0, max: 100, points: [] };

    const sorted = [...trends].sort((a, b) => a.date.localeCompare(b.date));

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

    // Use coordinates that match viewBox="0 0 200 60"
    const width = 200;
    const height = 60;
    const xPad = 8;
    const yPad = 8;

    const dataPoints = sorted.map((t, i) => {
      const x = xPad + (i / (values.length - 1)) * (width - 2 * xPad);
      const y = height - yPad - ((values[i] - chartMin) / chartRange) * (height - 2 * yPad);
      return {
        date: t.date,
        value: values[i],
        x,
        y
      };
    });

    const pathPoints = dataPoints.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`);
    const linePath = `M${pathPoints.join(' L')}`;
    // Area path for gradient fill
    const areaPath = `${linePath} L${dataPoints[dataPoints.length - 1].x.toFixed(1)},${height} L${dataPoints[0].x.toFixed(1)},${height} Z`;

    return {
      path: linePath,
      areaPath,
      min: chartMin,
      max: chartMax,
      points: dataPoints
    };
  }

  function getTrendColor(metric: 'asymmetry' | 'te' | 'ps'): string {
    if (metric === 'asymmetry') return 'var(--color-accent)';
    if (metric === 'te') return 'var(--color-optimal)';
    return 'var(--color-attention)';
  }

  function getAsymmetryClass(value: number): string {
    if (value <= 2.5) return 'optimal';
    if (value <= 5) return 'attention';
    return 'problem';
  }

  function getPeriodStats(rides: Ride[], days: number) {
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
    const ridesWithGrade = filtered.filter(r => r.grade_max > 0);
    const maxGrade = ridesWithGrade.length > 0
      ? Math.max(...ridesWithGrade.map(r => r.grade_max)) : 0;

    // Pro cyclist metrics - Power analytics
    const ridesWithNP = filtered.filter(r => r.normalized_power > 0);
    const avgNormalizedPower = ridesWithNP.length > 0
      ? ridesWithNP.reduce((sum, r) => sum + r.normalized_power, 0) / ridesWithNP.length : 0;
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

  function getPreviousPeriodStats(rides: Ride[], days: number) {
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

  function getProgress(current: number, previous: number, threshold = 0.5): { value: number; direction: 'up' | 'down' | 'same' } {
    const diff = current - previous;
    if (Math.abs(diff) < threshold) return { value: 0, direction: 'same' };
    return { value: Math.abs(diff), direction: diff > 0 ? 'up' : 'down' };
  }

  function getBalanceProgress(current: number, previous: number): { value: number; direction: 'up' | 'down' | 'same' } {
    // Balance: closer to 50 is better
    const currentDiff = Math.abs(current - 50);
    const prevDiff = Math.abs(previous - 50);
    const change = prevDiff - currentDiff; // positive = improvement
    if (Math.abs(change) < 0.3) return { value: 0, direction: 'same' };
    return { value: Math.abs(change), direction: change > 0 ? 'up' : 'down' };
  }

  $: periodStats = getPeriodStats(weeklyRides, parseInt(selectedPeriod));
  $: prevPeriodStats = getPreviousPeriodStats(weeklyRides, parseInt(selectedPeriod));
  $: scoreProgress = periodStats && prevPeriodStats ? getProgress(periodStats.score, prevPeriodStats.score) : null;
  $: optimalProgress = periodStats && prevPeriodStats ? getProgress(periodStats.zoneOptimal, prevPeriodStats.zoneOptimal) : null;
  $: teProgress = periodStats && prevPeriodStats ? getProgress(periodStats.te, prevPeriodStats.te) : null;
  $: psProgress = periodStats && prevPeriodStats ? getProgress(periodStats.ps, prevPeriodStats.ps) : null;
  $: balanceProgress = periodStats && prevPeriodStats ? getBalanceProgress(periodStats.balance, prevPeriodStats.balance) : null;
  $: powerProgress = periodStats && prevPeriodStats && periodStats.avgPower > 0 && prevPeriodStats.avgPower > 0
    ? getProgress(periodStats.avgPower, prevPeriodStats.avgPower, 2) : null;
  $: durationProgress = periodStats && prevPeriodStats
    ? getProgress(periodStats.duration / 3600000, prevPeriodStats.duration / 3600000, 0.2) : null;
  $: distanceProgress = periodStats && prevPeriodStats
    ? getProgress(periodStats.totalDistance, prevPeriodStats.distance, 5) : null;
  $: filteredRides = getFilteredRides(weeklyRides, parseInt(selectedPeriod));
  $: balanceTrendMaxPoints = selectedPeriod === '30' ? 30 : 15;
  $: balanceTrend = getBalanceTrendData(filteredRides, balanceTrendMaxPoints);
  $: weekDays = getWeekDays();
  $: ridesPerDay = getRidesPerDay(weeklyRides);
  $: maxRidesPerDay = Math.max(...ridesPerDay, 1);
  $: techniqueTrend = getTechniqueTrendData(trendData, activeTrendMetric);
</script>

<svelte:head>
  <title>{$isAuthenticated ? 'Dashboard' : 'KPedal — Pedaling Efficiency for Karoo'}</title>
  <meta name="description" content="Real-time pedaling analytics for Hammerhead Karoo. Balance, torque effectiveness, pedal smoothness — see your technique live.">
</svelte:head>

{#if !$isAuthenticated}
  <div class="landing">
    <!-- Theme Toggle -->
    <a href="/" class="site-logo">
      <span class="site-logo-dot"></span>
      <span class="site-logo-text">KPedal</span>
    </a>

    <button class="theme-toggle" on:click={() => theme.toggle()} aria-label="Toggle theme">
      {#if $theme === 'dark' || ($theme === 'system' && typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches)}
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
          <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
        </svg>
      {:else}
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
      {/if}
    </button>

    <div class="landing-container">
      <!-- Hero -->
      <section class="hero">
        <p class="hero-eyebrow">
          Karoo Extension
          <span class="hero-version">v1.0.0</span>
        </p>
        <h1 class="hero-title">
          <span class="hero-title-line">See your pedaling</span>
          <span class="hero-title-line">efficiency in real-time</span>
        </h1>
        <p class="hero-subtitle">
          Balance, Torque Effectiveness, Pedal Smoothness — displayed on your Karoo with real-time alerts and guided drills.
        </p>

        <div class="hero-actions">
          <a href="https://github.com/yrkan/kpedal/releases" class="hero-cta" target="_blank">
            Download Free
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </a>
          <button class="hero-cta-secondary" on:click={handleLogin}>
            <svg width="18" height="18" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Sign in with Google
          </button>
        </div>

        <p class="hero-note">Free & open source · No account required</p>
      </section>

      <!-- Data Fields Interactive -->
      <section id="datafields" class="section datafields-section">
        <h2 class="section-title">See It On Your Karoo</h2>
        <p class="section-subtitle">5 data field layouts. Choose the view that fits your training.</p>

        <div class="datafields-showcase">
          <div class="datafield-tabs">
            <button class="datafield-tab" class:active={activeDataField === 0} on:click={() => activeDataField = 0}>
              Quick Glance
            </button>
            <button class="datafield-tab" class:active={activeDataField === 1} on:click={() => activeDataField = 1}>
              Balance
            </button>
            <button class="datafield-tab" class:active={activeDataField === 2} on:click={() => activeDataField = 2}>
              Efficiency
            </button>
            <button class="datafield-tab" class:active={activeDataField === 3} on:click={() => activeDataField = 3}>
              Full View
            </button>
            <button class="datafield-tab" class:active={activeDataField === 4} on:click={() => activeDataField = 4}>
              Trend
            </button>
          </div>
          <div class="datafield-preview">
            <div class="preview-device">
              <div class="device-frame">
                <div class="device-notch"></div>
                <div class="preview-screen karoo">
                  {#if activeDataField === 0}
                    <!-- Quick Glance -->
                    <div class="karoo-layout quick-glance">
                      <div class="qg-status-section">
                        <svg class="qg-status-icon optimal" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                          <polyline points="20 6 9 17 4 12"/>
                        </svg>
                        <div class="qg-status-text">Optimal</div>
                      </div>
                      <div class="karoo-divider"></div>
                      <div class="qg-balance-section">
                        <div class="karoo-label">Balance</div>
                        <div class="balance-values">
                          <span class="balance-num">48</span>
                          <span class="balance-num">52</span>
                        </div>
                        <div class="balance-bar"><div class="bar-fill" style="width: 48%"></div></div>
                        <div class="balance-labels"><span>L</span><span>R</span></div>
                      </div>
                    </div>
                  {:else if activeDataField === 1}
                    <!-- Power Balance -->
                    <div class="karoo-layout power-balance">
                      <div class="pb-header">
                        <span class="karoo-label">Balance</span>
                        <span class="pb-status optimal">Optimal</span>
                      </div>
                      <div class="pb-main">
                        <div class="pb-side">
                          <span class="pb-value">48</span>
                          <span class="pb-label">Left</span>
                        </div>
                        <div class="pb-divider"></div>
                        <div class="pb-side">
                          <span class="pb-value">52</span>
                          <span class="pb-label">Right</span>
                        </div>
                      </div>
                      <div class="balance-bar"><div class="bar-fill" style="width: 48%"></div></div>
                    </div>
                  {:else if activeDataField === 2}
                    <!-- Efficiency -->
                    <div class="karoo-layout efficiency">
                      <div class="eff-section">
                        <div class="eff-header">
                          <span class="karoo-label">Torque Eff.</span>
                          <span class="eff-avg optimal">76%</span>
                        </div>
                        <div class="eff-values">
                          <div class="eff-side"><span class="eff-num">74</span><span class="eff-side-label">L</span></div>
                          <div class="eff-divider"></div>
                          <div class="eff-side"><span class="eff-num">77</span><span class="eff-side-label">R</span></div>
                        </div>
                      </div>
                      <div class="karoo-divider"></div>
                      <div class="eff-section">
                        <div class="eff-header">
                          <span class="karoo-label">Smoothness</span>
                          <span class="eff-avg optimal">24%</span>
                        </div>
                        <div class="eff-values">
                          <div class="eff-side"><span class="eff-num">23</span><span class="eff-side-label">L</span></div>
                          <div class="eff-divider"></div>
                          <div class="eff-side"><span class="eff-num">25</span><span class="eff-side-label">R</span></div>
                        </div>
                      </div>
                    </div>
                  {:else if activeDataField === 3}
                    <!-- Full Overview -->
                    <div class="karoo-layout full-overview">
                      <div class="fo-section">
                        <div class="fo-header"><span class="karoo-label">Balance</span><span class="fo-status optimal">OK</span></div>
                        <div class="fo-row"><span class="fo-num">48</span><span class="fo-sep">/</span><span class="fo-num">52</span></div>
                      </div>
                      <div class="karoo-divider"></div>
                      <div class="fo-section">
                        <div class="fo-header"><span class="karoo-label">TE</span><span class="fo-status optimal">76%</span></div>
                        <div class="fo-row"><span class="fo-num sm">74</span><span class="fo-sep">/</span><span class="fo-num sm">77</span></div>
                      </div>
                      <div class="karoo-divider"></div>
                      <div class="fo-section">
                        <div class="fo-header"><span class="karoo-label">PS</span><span class="fo-status optimal">24%</span></div>
                        <div class="fo-row"><span class="fo-num sm">23</span><span class="fo-sep">/</span><span class="fo-num sm">25</span></div>
                      </div>
                    </div>
                  {:else if activeDataField === 4}
                    <!-- Balance Trend -->
                    <div class="karoo-layout balance-trend">
                      <div class="bt-section main">
                        <div class="karoo-label">Now</div>
                        <div class="bt-values lg"><span>48</span><span class="bt-sep">:</span><span>52</span></div>
                      </div>
                      <div class="karoo-divider"></div>
                      <div class="bt-section">
                        <div class="karoo-label">3s Avg</div>
                        <div class="bt-values"><span>49</span><span class="bt-sep">:</span><span>51</span></div>
                      </div>
                      <div class="bt-section">
                        <div class="karoo-label">10s Avg</div>
                        <div class="bt-values"><span>50</span><span class="bt-sep">:</span><span>50</span></div>
                      </div>
                    </div>
                {/if}
                </div>
              </div>
            </div>
            <div class="preview-info">
              <h4>{dataFields[activeDataField].name}</h4>
              <p>{dataFields[activeDataField].desc}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Before / After - MOVED UP -->
      <section class="section before-after-section">
        <h2 class="section-title">The Difference</h2>
        <p class="section-subtitle">Stop guessing. Start improving.</p>

        <div class="comparison-table">
          <div class="comparison-header">
            <div class="comparison-col">Without KPedal</div>
            <div class="comparison-col highlight">With KPedal</div>
          </div>
          <div class="comparison-row">
            <div class="comparison-item before">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              <span>Power number, but no technique insight</span>
            </div>
            <div class="comparison-item after">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <span>Balance, TE, PS visible live</span>
            </div>
          </div>
          <div class="comparison-row">
            <div class="comparison-item before">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              <span>Discover imbalance from injury</span>
            </div>
            <div class="comparison-item after">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <span>Alert before it becomes a problem</span>
            </div>
          </div>
          <div class="comparison-row">
            <div class="comparison-item before">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              <span>No structured way to improve</span>
            </div>
            <div class="comparison-item after">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <span>10 guided drills with scoring</span>
            </div>
          </div>
          <div class="comparison-row">
            <div class="comparison-item before">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              <span>Data stuck on one device</span>
            </div>
            <div class="comparison-item after">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <span>Cloud sync with web dashboard</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Metrics Deep Dive -->
      <section id="features" class="section">
        <h2 class="section-title">What We Measure</h2>
        <p class="section-subtitle">Three metrics. Research-backed thresholds.</p>

        <div class="metrics-deep">
          <div class="metric-deep-card">
            <div class="metric-deep-header">
              <div class="metric-deep-icon balance"></div>
              <div>
                <h3>Power Balance</h3>
                <span class="metric-deep-range">48-52% optimal</span>
              </div>
            </div>
            <p class="metric-deep-desc">Left/right power distribution. Pro cyclists maintain within 2%. Imbalances above 5% increase overuse injury risk and waste energy through compensation patterns.</p>
            <div class="metric-zones">
              <div class="zone optimal"><span>48-52%</span> Optimal</div>
              <div class="zone attention"><span>45-48% / 52-55%</span> Attention</div>
              <div class="zone problem"><span>&lt;45% / &gt;55%</span> Problem</div>
            </div>
          </div>

          <div class="metric-deep-card featured">
            <div class="metric-deep-header">
              <div class="metric-deep-icon te"></div>
              <div>
                <h3>Torque Effectiveness</h3>
                <span class="metric-deep-range">70-80% sweet spot</span>
              </div>
            </div>
            <p class="metric-deep-desc">Percentage of power applied during the effective part of stroke. Based on Wattbike research — <strong>higher is NOT better</strong>. Above 80% can actually reduce total power output.</p>
            <div class="metric-zones">
              <div class="zone optimal"><span>70-80%</span> Optimal</div>
              <div class="zone attention"><span>60-70% / 80-85%</span> Attention</div>
              <div class="zone problem"><span>&lt;60% / &gt;85%</span> Problem</div>
            </div>
          </div>

          <div class="metric-deep-card">
            <div class="metric-deep-header">
              <div class="metric-deep-icon ps"></div>
              <div>
                <h3>Pedal Smoothness</h3>
                <span class="metric-deep-range">≥20% target</span>
              </div>
            </div>
            <p class="metric-deep-desc">How evenly power distributes through the entire pedal stroke. Eliminates "dead spots". Elite cyclists achieve 25-35% — this is the hardest metric to improve.</p>
            <div class="metric-zones">
              <div class="zone optimal"><span>≥20%</span> Optimal</div>
              <div class="zone attention"><span>15-20%</span> Attention</div>
              <div class="zone problem"><span>&lt;15%</span> Problem</div>
            </div>
          </div>
        </div>
      </section>

      <!-- Drills Section -->
      <section id="drills" class="section drills-section">
        <h2 class="section-title">10 Guided Drills</h2>
        <p class="section-subtitle">From 30-second exercises to 15-minute workouts. Each scored 0-100%.</p>

        <div class="drills-tabs">
          <button class="drill-tab" class:active={activeDrillCategory === 'focus'} on:click={() => activeDrillCategory = 'focus'}>
            Focus
          </button>
          <button class="drill-tab" class:active={activeDrillCategory === 'challenge'} on:click={() => activeDrillCategory = 'challenge'}>
            Challenge
          </button>
          <button class="drill-tab" class:active={activeDrillCategory === 'workout'} on:click={() => activeDrillCategory = 'workout'}>
            Workout
          </button>
        </div>

        <div class="drills-list">
          {#each drills[activeDrillCategory] as drill}
            <button class="drill-item" class:expanded={expandedDrill === drill.id} on:click={() => toggleDrill(drill.id)}>
              <div class="drill-header">
                <div class="drill-info">
                  <span class="drill-name">{drill.name}</span>
                  <span class="drill-meta">
                    <span class="drill-duration">{drill.duration}</span>
                    <span class="drill-dot"></span>
                    <span class="drill-level" class:beginner={drill.level === 'Beginner'} class:intermediate={drill.level === 'Intermediate'} class:advanced={drill.level === 'Advanced'}>{drill.level}</span>
                  </span>
                </div>
                <svg class="drill-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="6 9 12 15 18 9"/>
                </svg>
              </div>
              {#if expandedDrill === drill.id}
                <div class="drill-details">
                  <p>{drill.desc}</p>
                  {#if drill.target}
                    <div class="drill-target">Target: {drill.target}</div>
                  {/if}
                  {#if drill.phases}
                    <div class="drill-target">{drill.phases} phases</div>
                  {/if}
                </div>
              {/if}
            </button>
          {/each}
        </div>

        <div class="drills-highlight">
          <span class="highlight-label">Featured</span>
          <h4>Pedaling Mastery</h4>
          <p>15 minutes, 10 phases. Complete technique coverage. Recommended 1-2× per week.</p>
        </div>
      </section>

      <!-- Alerts Section -->
      <section class="section alerts-section">
        <h2 class="section-title">Real-Time Alerts</h2>
        <p class="section-subtitle">Get corrected mid-ride when technique drifts</p>

        <div class="alerts-demo">
          <div class="alert-banner-demo">
            <div class="alert-indicator"></div>
            <div class="alert-content">
              <span class="alert-title">Balance Alert</span>
              <span class="alert-detail">Right leg dominant: 56%</span>
            </div>
          </div>

          <div class="alerts-features">
            <div class="alert-feature">
              <h4>Vibration & Sound</h4>
              <p>Haptic feedback and audio beeps. Toggle each on/off.</p>
            </div>
            <div class="alert-feature">
              <h4>Screen Wake</h4>
              <p>Auto-wake Karoo display when alert triggers.</p>
            </div>
            <div class="alert-feature">
              <h4>Smart Cooldown</h4>
              <p>15s to 2min between alerts. Useful, not annoying.</p>
            </div>
            <div class="alert-feature">
              <h4>Per-Metric Thresholds</h4>
              <p>Separate settings for Balance, TE, and PS.</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Analytics & Dashboard -->
      <section class="section">
        <h2 class="section-title">Analytics & Web Dashboard</h2>
        <p class="section-subtitle">Track improvement over time. See trends, not just numbers.</p>

        <div class="dashboard-preview">
          <div class="dash-browser">
            <div class="browser-bar">
              <div class="browser-dots"><span></span><span></span><span></span></div>
              <div class="browser-url">kpedal.com</div>
            </div>
            <div class="browser-content">
              <div class="dash-header">
                <div class="dash-greeting">Welcome back, Alex</div>
                <div class="dash-period">Last 7 days</div>
              </div>
              <div class="dash-grid">
                <div class="dash-stat-card">
                  <span class="dash-stat-value">12</span>
                  <span class="dash-stat-label">Rides</span>
                </div>
                <div class="dash-stat-card">
                  <span class="dash-stat-value score">82</span>
                  <span class="dash-stat-label">Avg Score</span>
                </div>
                <div class="dash-stat-card">
                  <span class="dash-stat-value">49.2%</span>
                  <span class="dash-stat-label">Left Balance</span>
                </div>
                <div class="dash-stat-card">
                  <span class="dash-stat-value">8h 24m</span>
                  <span class="dash-stat-label">Total Time</span>
                </div>
              </div>
              <div class="dash-chart">
                <div class="chart-label">Balance Trend</div>
                <svg viewBox="0 0 200 50" class="trend-line">
                  <path d="M0 35 L30 30 L60 38 L90 25 L120 28 L150 22 L180 20 L200 18" fill="none" stroke="var(--color-optimal)" stroke-width="2"/>
                </svg>
              </div>
            </div>
          </div>

          <div class="dash-features">
            <div class="dash-feature">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              </svg>
              <span>7-day & 30-day trend analysis</span>
            </div>
            <div class="dash-feature">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              <span>Full ride history with filters</span>
            </div>
            <div class="dash-feature">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
              <span>Multi-device sync</span>
            </div>
            <div class="dash-feature">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
              </svg>
              <span>Sync settings across devices</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Stay Motivated -->
      <section class="section motivation-section">
        <h2 class="section-title">Stay Motivated</h2>
        <p class="section-subtitle">Track progress with achievements and weekly challenges</p>

        <div class="motivation-content">
          <div class="motivation-stats">
            <div class="motivation-stat">
              <span class="stat-number">16</span>
              <span class="stat-desc">Achievements to unlock</span>
            </div>
            <div class="motivation-divider"></div>
            <div class="motivation-stat">
              <span class="stat-number">7</span>
              <span class="stat-desc">Weekly challenges</span>
            </div>
          </div>

          <div class="motivation-examples">
            <div class="example-group">
              <span class="example-label">Achievements</span>
              <div class="example-items">
                <span>First Ride</span>
                <span>100 Rides</span>
                <span>Perfect Balance 10 min</span>
                <span>30-Day Streak</span>
              </div>
            </div>
            <div class="example-group">
              <span class="example-label">Challenges</span>
              <div class="example-items">
                <span>Balanced Rider: 48-52% across 3 rides</span>
                <span>Zone Master: 60%+ in optimal zone</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Works Everywhere - Combined Background + Sync -->
      <section class="section works-everywhere-section">
        <h2 class="section-title">Works Everywhere</h2>
        <p class="section-subtitle">Background collection. Automatic sync. Your data across all devices.</p>

        <div class="works-grid">
          <div class="works-card">
            <div class="works-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/>
              </svg>
            </div>
            <h4>Background Mode</h4>
            <p>Collects data for ALL rides, even without data fields visible. Starts automatically on boot. Saves when you stop.</p>
          </div>
          <div class="works-card">
            <div class="works-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/>
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
              </svg>
            </div>
            <h4>Cloud Sync</h4>
            <p>Rides sync after completion. Link device at link.kpedal.com with a simple code — no password needed on Karoo.</p>
          </div>
          <div class="works-card">
            <div class="works-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>
            <h4>Multi-Device</h4>
            <p>Link multiple Karoos. All rides go to one account. Settings sync bidirectionally between app and web.</p>
          </div>
        </div>
      </section>

      <!-- What You Need -->
      <section class="section requirements-section">
        <h2 class="section-title">What You Need</h2>
        <p class="section-subtitle">Karoo 2 or 3 + dual-sided power pedals</p>

        <div class="requirements-grid">
          <div class="req-card">
            <div class="req-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/>
              </svg>
            </div>
            <h4>Hammerhead Karoo</h4>
            <p>Karoo 2 or Karoo 3. KPedal uses the native Karoo SDK — not available on other computers.</p>
          </div>
          <div class="req-card">
            <div class="req-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/>
              </svg>
            </div>
            <h4>Dual Power Pedals</h4>
            <p>Left + right power measurement with ANT+ Cycling Dynamics for Balance, TE, and PS.</p>
          </div>
        </div>

        <div class="compatible-pedals">
          <span class="pedals-label">Compatible</span>
          <div class="pedals-list">
            <span>Garmin Rally</span>
            <span class="pedals-sep">·</span>
            <span>Garmin Vector 3</span>
            <span class="pedals-sep">·</span>
            <span>Favero Assioma DUO</span>
            <span class="pedals-sep">·</span>
            <span>SRM X-Power</span>
            <span class="pedals-sep">·</span>
            <span>Rotor 2INpower</span>
            <span class="pedals-sep">·</span>
            <span class="pedals-partial">Wahoo POWRLINK</span>
          </div>
          <p class="pedals-note">Wahoo POWRLINK: balance only</p>
        </div>
      </section>

      <!-- FAQ -->
      <section class="section faq-section">
        <h2 class="section-title">Frequently Asked Questions</h2>

        <div class="faq-list">
          <details class="faq-item">
            <summary>Does KPedal work offline?</summary>
            <p>Yes! All core features work without internet. Cloud sync is optional — your data is always stored locally on the Karoo first.</p>
          </details>

          <details class="faq-item">
            <summary>Will it drain my Karoo battery?</summary>
            <p>Minimal impact. KPedal only runs during active rides and uses efficient data collection. Background mode uses ~1-2% extra battery per hour.</p>
          </details>

          <details class="faq-item">
            <summary>Do I need to add data fields to see metrics?</summary>
            <p>No. With Background Mode enabled, KPedal collects data for all rides automatically. Data fields are just for viewing live — all rides are saved regardless.</p>
          </details>

          <details class="faq-item">
            <summary>What if I only have balance data (no TE/PS)?</summary>
            <p>KPedal works with whatever your pedals provide. Balance-only pedals like Wahoo POWRLINK still get balance metrics, alerts, and drills.</p>
          </details>

          <details class="faq-item">
            <summary>Is my data private?</summary>
            <p>Your data is stored locally on your Karoo. Cloud sync is opt-in and uses Google Sign-In for authentication. We don't sell or share your data.</p>
          </details>

          <details class="faq-item">
            <summary>How do I install on Karoo?</summary>
            <p>Download the APK from GitHub Releases. Transfer to Karoo via USB or use Karoo's built-in file browser with a direct download link. Open the APK to install.</p>
          </details>

          <details class="faq-item">
            <summary>Can I use KPedal with Garmin/Wahoo computers?</summary>
            <p>No. KPedal is built specifically for Hammerhead Karoo using their SDK. It uses native Karoo features not available on other platforms.</p>
          </details>

          <details class="faq-item">
            <summary>What's the difference between TE zones?</summary>
            <p>70-80% is optimal based on Wattbike research. Higher isn't better — above 80% can reduce total power output. Below 60% indicates significant technique issues.</p>
          </details>
        </div>
      </section>

      <!-- Final CTA -->
      <section class="section cta-section">
        <h2 class="cta-headline">Start improving your pedaling today</h2>
        <p class="cta-subtext">Free. Open source. Syncs to the cloud.</p>
        <div class="cta-actions">
          <a href="https://github.com/yrkan/kpedal/releases" class="cta-btn primary large" target="_blank">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            Download for Karoo
          </a>
          <button class="cta-btn secondary" on:click={handleLogin}>
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Sign in with Google
          </button>
        </div>
      </section>

      <!-- Footer -->
      <footer class="landing-footer">
        <div class="footer-top">
          <div class="footer-brand">
            <div class="footer-logo">
              <span class="logo-dot small"></span>
              <span class="footer-brand-name">KPedal</span>
            </div>
            <p class="footer-tagline">Pedaling efficiency for Karoo</p>
          </div>

          <div class="footer-nav">
            <div class="footer-col">
              <h4>Product</h4>
              <a href="#features">Features</a>
              <a href="#datafields">Data Fields</a>
              <a href="#drills">Drills</a>
            </div>
            <div class="footer-col">
              <h4>Resources</h4>
              <a href="https://github.com/yrkan/kpedal" target="_blank">GitHub</a>
              <a href="https://github.com/yrkan/kpedal/releases" target="_blank">Releases</a>
              <a href="https://github.com/yrkan/kpedal/issues" target="_blank">Issues</a>
            </div>
            <div class="footer-col">
              <h4>Legal</h4>
              <a href="/privacy">Privacy</a>
              <button class="footer-link-btn" on:click={handleLogin}>Sign in</button>
            </div>
          </div>
        </div>

        <div class="footer-bottom">
          <span>Made with ❤️ for the Karoo community</span>
          <a href="https://github.com/yrkan/kpedal" target="_blank" class="footer-github" aria-label="GitHub">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>
          </a>
        </div>
      </footer>
    </div>
  </div>
{:else}
  <!-- Dashboard (authenticated) -->
  <div class="dashboard">
    <div class="container">
      {#if loading}
        <div class="loading-state"><div class="spinner"></div></div>
      {:else if error}
        <div class="error-state">
          <p>{error}</p>
          <button class="btn btn-primary" on:click={() => location.reload()}>Retry</button>
        </div>
      {:else}
        <header class="dash-header animate-in">
          <div class="dash-greeting">
            <h1>Hello, {getFirstName($user?.name)}!</h1>
          </div>
          <div class="dash-controls">
            <div class="period-selector">
              <button class="period-btn" class:active={selectedPeriod === '7'} on:click={() => selectedPeriod = '7'}>7d</button>
              <button class="period-btn" class:active={selectedPeriod === '30'} on:click={() => selectedPeriod = '30'}>30d</button>
            </div>
          </div>
        </header>

        {#if stats && stats.total_rides > 0}
          <!-- Hero Stats Row -->
          <div class="hero-stats animate-in">
            <div class="hero-stat asymmetry">
              <div class="hero-stat-main">
                <span class="hero-stat-value {getBalanceStatus(periodStats?.balance || 50)}">{periodStats ? Math.abs(periodStats.balance - 50).toFixed(1) : '—'}%</span>
              </div>
              <div class="hero-stat-info">
                <span class="hero-stat-label">
                  Asymmetry
                  <InfoTip
                    text="Deviation from 50/50 balance. Under 2.5% is pro level, above 5% may cause issues."
                    position="bottom"
                  />
                </span>
                <span class="hero-stat-detail">{periodStats && periodStats.balance !== 50 ? (periodStats.balance > 50 ? 'L dominant' : 'R dominant') : 'Balanced'}</span>
              </div>
            </div>

            <div class="hero-stat balance-visual">
              <div class="balance-display">
                <div class="balance-side left">
                  <span class="balance-pct">{periodStats ? periodStats.balance.toFixed(0) : 50}</span>
                  <span class="balance-leg">L</span>
                </div>
                <div class="balance-bar-wrap">
                  <div class="balance-bar">
                    <div class="balance-fill left" style="width: {periodStats ? Math.min(50, periodStats.balance) : 50}%"></div>
                    <div class="balance-fill right" style="width: {periodStats ? Math.min(50, 100 - periodStats.balance) : 50}%"></div>
                  </div>
                  <div class="balance-center-mark"></div>
                </div>
                <div class="balance-side right">
                  <span class="balance-pct">{periodStats ? (100 - periodStats.balance).toFixed(0) : 50}</span>
                  <span class="balance-leg">R</span>
                </div>
              </div>
            </div>

            <div class="hero-stat zones">
              <div class="zone-mini-bars">
                <div class="zone-mini optimal" style="width: {periodStats?.zoneOptimal || 0}%"></div>
                <div class="zone-mini attention" style="width: {periodStats?.zoneAttention || 0}%"></div>
                <div class="zone-mini problem" style="width: {periodStats?.zoneProblem || 0}%"></div>
              </div>
              <div class="zone-mini-values">
                <span class="zone-mini-val optimal">{periodStats?.zoneOptimal?.toFixed(0) || 0}%</span>
                <span class="zone-mini-val attention">{periodStats?.zoneAttention?.toFixed(0) || 0}%</span>
                <span class="zone-mini-val problem">{periodStats?.zoneProblem?.toFixed(0) || 0}%</span>
              </div>
              <span class="hero-stat-label">
                Time in Zone
                <InfoTip
                  text="Time spent in each zone. Green is optimal, yellow needs attention, red is a problem."
                  position="bottom"
                />
              </span>
            </div>

            <div class="hero-stat summary">
              <div class="summary-grid">
                <div class="summary-metric">
                  <span class="summary-num">{periodStats?.rides || 0}</span>
                  <span class="summary-unit">rides</span>
                </div>
                <div class="summary-metric">
                  <span class="summary-num">{periodStats ? (periodStats.duration / 3600000).toFixed(1) : 0}</span>
                  <span class="summary-unit">hours</span>
                </div>
                <div class="summary-metric">
                  <span class="summary-num">{periodStats?.totalDistance?.toFixed(0) || 0}</span>
                  <span class="summary-unit">km</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Main Grid -->
          <div class="main-grid animate-in">
            <!-- Left Column: Activity + Training -->
            <div class="grid-card">
              <div class="card-header">
                <span class="card-title">Weekly Activity <InfoTip text="Number of rides per day. Aim for 3-5 rides per week for steady progress." position="bottom" /></span>
                <span class="card-subtitle">{ridesPerDay.reduce((a, b) => a + b, 0)} rides</span>
              </div>
              <div class="bar-chart-enhanced">
                <div class="bar-chart-area">
                  {#each weekDays as day, i}
                    <div class="bar-col">
                      <div class="bar-wrapper">
                        {#if ridesPerDay[i] > 0}
                          <span class="bar-count">{ridesPerDay[i]}</span>
                        {/if}
                        <div class="bar" style="height: {ridesPerDay[i] ? (ridesPerDay[i] / maxRidesPerDay) * 100 : 0}%"></div>
                      </div>
                      <span class="bar-label">{day.slice(0, 1)}</span>
                    </div>
                  {/each}
                </div>
              </div>

              {#if weeklyComparison}
                <div class="card-section">
                  <span class="section-label">vs Last Week</span>
                  <div class="comparison-grid">
                    <div class="comp-item">
                      <span class="comp-label">Rides <InfoTip text="Number of rides compared to last week." position="bottom" size="sm" /></span>
                      <span class="comp-value">{weeklyComparison.thisWeek.rides_count}</span>
                      <span class="comp-delta {weeklyComparison.changes.rides_count >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.rides_count >= 0 ? '+' : ''}{weeklyComparison.changes.rides_count}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">Hours <InfoTip text="Total saddle time. More hours equals more training adaptation." position="bottom" size="sm" /></span>
                      <span class="comp-value">{(weeklyComparison.thisWeek.total_duration_ms / 3600000).toFixed(1)}</span>
                      <span class="comp-delta {weeklyComparison.changes.duration >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.duration >= 0 ? '+' : ''}{(weeklyComparison.changes.duration / 3600000).toFixed(1)}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">Distance <InfoTip text="Total kilometers ridden. Useful for tracking volume alongside hours." position="bottom" size="sm" /></span>
                      <span class="comp-value">{weeklyComparison.thisWeek.total_distance_km.toFixed(0)}km</span>
                      <span class="comp-delta {weeklyComparison.changes.distance >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.distance >= 0 ? '+' : ''}{weeklyComparison.changes.distance.toFixed(0)}
                      </span>
                    </div>
                    {#if weeklyComparison.thisWeek.total_elevation > 0}
                      <div class="comp-item">
                        <span class="comp-label">Climb <InfoTip text="Total elevation gained. Over 2000m per week builds climbing strength." position="bottom" size="sm" /></span>
                        <span class="comp-value">{Math.round(weeklyComparison.thisWeek.total_elevation)}m</span>
                        <span class="comp-delta {weeklyComparison.changes.elevation >= 0 ? 'up' : 'down'}">
                          {weeklyComparison.changes.elevation >= 0 ? '+' : ''}{Math.round(weeklyComparison.changes.elevation)}
                        </span>
                      </div>
                    {/if}
                  </div>
                </div>
                <!-- Weekly Performance Indicators -->
                <div class="card-section">
                  <span class="section-label">Performance</span>
                  <div class="performance-mini-grid">
                    <div class="perf-mini-item">
                      <span class="perf-mini-label">Score <InfoTip text="Overall technique score from 0 to 100. Higher means better form." position="top" /></span>
                      <span class="perf-mini-value">{weeklyComparison.thisWeek.avg_score.toFixed(0)}</span>
                      <span class="perf-mini-delta {weeklyComparison.changes.score >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.score >= 0 ? '+' : ''}{weeklyComparison.changes.score.toFixed(0)}
                      </span>
                    </div>
                    <div class="perf-mini-item">
                      <span class="perf-mini-label">Optimal <InfoTip text="Time with all metrics in the green zone. Higher is better." position="top" /></span>
                      <span class="perf-mini-value">{weeklyComparison.thisWeek.avg_zone_optimal.toFixed(0)}%</span>
                      <span class="perf-mini-delta {weeklyComparison.changes.zone_optimal >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.zone_optimal >= 0 ? '+' : ''}{weeklyComparison.changes.zone_optimal.toFixed(0)}
                      </span>
                    </div>
                    {#if weeklyComparison.thisWeek.avg_power > 0}
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">Power <InfoTip text="Average power output in watts. Higher at same HR means better fitness." position="top" /></span>
                        <span class="perf-mini-value">{Math.round(weeklyComparison.thisWeek.avg_power)}W</span>
                        <span class="perf-mini-delta {weeklyComparison.changes.power >= 0 ? 'up' : 'down'}">
                          {weeklyComparison.changes.power >= 0 ? '+' : ''}{Math.round(weeklyComparison.changes.power)}
                        </span>
                      </div>
                    {/if}
                    {#if weeklyComparison.thisWeek.total_energy_kj > 0}
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">Energy <InfoTip text="Total energy in kilojoules. Roughly equals calories burned." position="top" /></span>
                        <span class="perf-mini-value">{Math.round(weeklyComparison.thisWeek.total_energy_kj)}kJ</span>
                        <span class="perf-mini-delta {weeklyComparison.changes.energy >= 0 ? 'up' : 'down'}">
                          {weeklyComparison.changes.energy >= 0 ? '+' : ''}{Math.round(weeklyComparison.changes.energy)}
                        </span>
                      </div>
                    {/if}
                  </div>
                </div>
              {/if}
            </div>

            <!-- Right Column: Technique -->
            <div class="grid-card">
              <div class="card-header">
                <span class="card-title">Technique <InfoTip text="Pedaling efficiency metrics. TE is power transfer, PS is stroke smoothness." position="bottom" /></span>
                <span class="card-subtitle">{selectedPeriod}d avg</span>
              </div>

              <div class="technique-enhanced">
                <div class="technique-metric">
                  <div class="technique-metric-header">
                    <span class="technique-metric-label">
                      Torque Effectiveness
                      <InfoTip
                        text="Power going into forward motion. Optimal range is 70-80%."
                        position="top"
                      />
                    </span>
                    <span class="technique-metric-value">{periodStats?.te?.toFixed(0) || ((stats.avg_te_left + stats.avg_te_right) / 2).toFixed(0)}%</span>
                  </div>
                  <div class="technique-metric-bar-wrap">
                    <div class="technique-metric-bar">
                      <div class="technique-metric-fill te" style="width: {Math.min(100, (periodStats?.te || ((stats.avg_te_left + stats.avg_te_right) / 2)))}%"></div>
                      <div class="technique-optimal-zone te"></div>
                    </div>
                  </div>
                  <div class="technique-metric-sides">
                    <span>L {periodStats?.te ? ((periodStats.te * 2 - stats.avg_te_right) || stats.avg_te_left).toFixed(0) : stats.avg_te_left.toFixed(0)}%</span>
                    <span>R {stats.avg_te_right.toFixed(0)}%</span>
                  </div>
                </div>

                <div class="technique-metric">
                  <div class="technique-metric-header">
                    <span class="technique-metric-label">
                      Pedal Smoothness
                      <InfoTip
                        text="Smoothness of your pedal stroke. Target 20% or higher, elite is 25%+."
                        position="top"
                      />
                    </span>
                    <span class="technique-metric-value">{periodStats?.ps?.toFixed(0) || ((stats.avg_ps_left + stats.avg_ps_right) / 2).toFixed(0)}%</span>
                  </div>
                  <div class="technique-metric-bar-wrap">
                    <div class="technique-metric-bar">
                      <div class="technique-metric-fill ps" style="width: {Math.min(100, (periodStats?.ps || ((stats.avg_ps_left + stats.avg_ps_right) / 2)) / 40 * 100)}%"></div>
                      <div class="technique-optimal-zone ps"></div>
                    </div>
                  </div>
                  <div class="technique-metric-sides">
                    <span>L {stats.avg_ps_left.toFixed(0)}%</span>
                    <span>R {stats.avg_ps_right.toFixed(0)}%</span>
                  </div>
                </div>
              </div>

              {#if weeklyComparison}
                <div class="card-section">
                  <span class="section-label">vs Last Week</span>
                  <div class="comparison-grid">
                    <div class="comp-item">
                      <span class="comp-label">TE <InfoTip text="Torque Effectiveness vs last week. Positive change is improvement." position="bottom" size="sm" /></span>
                      <span class="comp-value">{weeklyComparison.thisWeek.avg_te.toFixed(0)}%</span>
                      <span class="comp-delta {weeklyComparison.changes.te >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.te >= 0 ? '+' : ''}{weeklyComparison.changes.te.toFixed(1)}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">PS <InfoTip text="Pedal Smoothness vs last week. Improves slowly, small gains matter." position="bottom" size="sm" /></span>
                      <span class="comp-value">{weeklyComparison.thisWeek.avg_ps.toFixed(0)}%</span>
                      <span class="comp-delta {weeklyComparison.changes.ps >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.ps >= 0 ? '+' : ''}{weeklyComparison.changes.ps.toFixed(1)}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">Balance <InfoTip text="Power asymmetry vs last week. Lower is better, negative is improvement." position="bottom" size="sm" /></span>
                      <span class="comp-value">{Math.abs(weeklyComparison.thisWeek.avg_balance_left - 50).toFixed(1)}%</span>
                      <span class="comp-delta {weeklyComparison.changes.balance <= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.balance <= 0 ? '' : '+'}{weeklyComparison.changes.balance.toFixed(1)}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">Optimal <InfoTip text="Time in optimal zone vs last week. Higher is better technique." position="bottom" size="sm" /></span>
                      <span class="comp-value">{weeklyComparison.thisWeek.avg_zone_optimal.toFixed(0)}%</span>
                      <span class="comp-delta {weeklyComparison.changes.zone_optimal >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.zone_optimal >= 0 ? '+' : ''}{weeklyComparison.changes.zone_optimal.toFixed(0)}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Fatigue Analysis -->
                {#if fatigueData && fatigueData.hasData}
                  <div class="card-section">
                    <span class="section-label">
                      Fatigue (Last Ride)
                      <InfoTip
                        text="Technique at ride start vs end. Dropping values indicate fatigue."
                        position="top"
                      />
                    </span>
                    <div class="performance-mini-grid">
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">Balance <InfoTip text="Left vs right power split. Changes indicate fatigue or compensation." position="top" size="sm" /></span>
                        <span class="perf-mini-value">{fatigueData.firstThird.balance.toFixed(0)}→{fatigueData.lastThird.balance.toFixed(0)}</span>
                        <span class="perf-mini-delta {fatigueData.degradation.balance > 0.5 ? 'down' : fatigueData.degradation.balance < -0.5 ? 'up' : ''}">
                          {fatigueData.degradation.balance > 0.5 ? '↓' : fatigueData.degradation.balance < -0.5 ? '↑' : '—'}
                        </span>
                      </div>
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">TE <InfoTip text="TE change during ride. Drop indicates muscular fatigue." position="top" size="sm" /></span>
                        <span class="perf-mini-value">{fatigueData.firstThird.te.toFixed(0)}→{fatigueData.lastThird.te.toFixed(0)}</span>
                        <span class="perf-mini-delta {fatigueData.degradation.te > 2 ? 'down' : fatigueData.degradation.te < -2 ? 'up' : ''}">
                          {fatigueData.degradation.te > 2 ? '↓' : fatigueData.degradation.te < -2 ? '↑' : '—'}
                        </span>
                      </div>
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">PS <InfoTip text="PS change during ride. Usually first metric to drop when tired." position="top" size="sm" /></span>
                        <span class="perf-mini-value">{fatigueData.firstThird.ps.toFixed(0)}→{fatigueData.lastThird.ps.toFixed(0)}</span>
                        <span class="perf-mini-delta {fatigueData.degradation.ps > 1 ? 'down' : fatigueData.degradation.ps < -1 ? 'up' : ''}">
                          {fatigueData.degradation.ps > 1 ? '↓' : fatigueData.degradation.ps < -1 ? '↑' : '—'}
                        </span>
                      </div>
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">Trend <InfoTip text="Overall fatigue trend. Stable is good pacing, dropped is overexertion." position="top" size="sm" /></span>
                        <span class="perf-mini-value fatigue-summary {fatigueData.degradation.te > 2 || fatigueData.degradation.ps > 1 ? 'problem' : fatigueData.degradation.te > 0 || fatigueData.degradation.ps > 0 ? 'attention' : 'optimal'}">
                          {fatigueData.degradation.te > 2 || fatigueData.degradation.ps > 1 ? 'Dropped' : fatigueData.degradation.te > 0 || fatigueData.degradation.ps > 0 ? 'Slight' : 'Stable'}
                        </span>
                      </div>
                    </div>
                  </div>
                {/if}
              {/if}
            </div>
          </div>

          <!-- Power & Cycling Metrics (if available) -->
          {#if periodStats && (periodStats.avgPower > 0 || periodStats.totalElevationGain > 0)}
            <div class="metrics-compact animate-in">
              {#if periodStats.avgPower > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{Math.round(periodStats.avgPower)}</span>
                  <span class="metric-chip-unit">W avg <InfoTip text="Average power in watts. Track over time to measure fitness gains." position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.maxPower > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{Math.round(periodStats.maxPower)}</span>
                  <span class="metric-chip-unit">W max <InfoTip text="Peak power achieved. Useful for sprints and FTP estimation." position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.avgNormalizedPower > 0}
                <div class="metric-chip accent">
                  <span class="metric-chip-val">{Math.round(periodStats.avgNormalizedPower)}</span>
                  <span class="metric-chip-unit">
                    NP
                    <InfoTip
                      text="Normalized Power adjusts for surges. Close to average means steady effort."
                      position="bottom"
                    />
                  </span>
                </div>
              {/if}
              {#if periodStats.avgCadence > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{Math.round(periodStats.avgCadence)}</span>
                  <span class="metric-chip-unit">rpm <InfoTip text="Pedal revolutions per minute. Optimal is 80-95 rpm for most riders." position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.avgHr > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{Math.round(periodStats.avgHr)}</span>
                  <span class="metric-chip-unit">bpm avg <InfoTip text="Average heart rate. Lower at same power means better fitness." position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.maxHr > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{Math.round(periodStats.maxHr)}</span>
                  <span class="metric-chip-unit">bpm max <InfoTip text="Peak heart rate reached. Near max often hurts technique." position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.avgSpeed > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{periodStats.avgSpeed.toFixed(1)}</span>
                  <span class="metric-chip-unit">km/h <InfoTip text="Average speed. Less reliable than power due to terrain and wind." position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.totalElevationGain > 0}
                <div class="metric-chip elevation">
                  <span class="metric-chip-val">{Math.round(periodStats.totalElevationGain)}</span>
                  <span class="metric-chip-unit">m ↑ <InfoTip text="Total elevation gained. Over 1000m is a serious climbing workout." position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.totalElevationLoss > 0}
                <div class="metric-chip elevation">
                  <span class="metric-chip-val">{Math.round(periodStats.totalElevationLoss)}</span>
                  <span class="metric-chip-unit">m ↓ <InfoTip text="Total elevation lost. Should match gain on loop rides." position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.maxGrade > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{periodStats.maxGrade.toFixed(1)}</span>
                  <span class="metric-chip-unit">% grade <InfoTip text="Maximum gradient. Above 10% is steep, above 15% is very steep." position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.totalEnergy > 0}
                <div class="metric-chip energy">
                  <span class="metric-chip-val">{Math.round(periodStats.totalEnergy)}</span>
                  <span class="metric-chip-unit">kJ <InfoTip text="Total energy output. Plan nutrition at 250-300 kcal per hour." position="bottom" /></span>
                </div>
              {/if}
            </div>
          {/if}

          <!-- Trends Row: Balance + Technique side by side -->
          {#if balanceTrend.path || trendData.length >= 2}
            <div class="trends-row animate-in">
              <!-- Balance Trend -->
              <div class="trend-card">
                <div class="trend-card-header">
                  <span class="trend-card-title">Balance Trend <InfoTip text="Left/right balance trend over time. Consistent values near 50% indicate good symmetry." position="bottom" /></span>
                  <span class="trend-card-period">{filteredRides.length} rides</span>
                </div>
                {#if balanceTrend.path}
                  <div class="trend-chart">
                    <svg viewBox="0 0 200 60" class="trend-svg">
                      <defs>
                        <linearGradient id="balanceGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" stop-color="var(--color-optimal)" stop-opacity="0.3"/>
                          <stop offset="100%" stop-color="var(--color-optimal)" stop-opacity="0"/>
                        </linearGradient>
                      </defs>
                      <!-- Horizontal grid lines -->
                      <line x1="8" y1="15" x2="192" y2="15" stroke="var(--border-subtle)" stroke-width="0.5" opacity="0.5"/>
                      <line x1="8" y1="30" x2="192" y2="30" stroke="var(--color-optimal)" stroke-width="1" stroke-dasharray="4,3" opacity="0.5"/>
                      <line x1="8" y1="45" x2="192" y2="45" stroke="var(--border-subtle)" stroke-width="0.5" opacity="0.5"/>
                      <!-- Y-axis labels -->
                      <text x="4" y="17" font-size="6" fill="var(--text-muted)" text-anchor="start">60</text>
                      <text x="4" y="32" font-size="6" fill="var(--text-muted)" text-anchor="start">50</text>
                      <text x="4" y="47" font-size="6" fill="var(--text-muted)" text-anchor="start">40</text>
                      <!-- Area fill -->
                      <path d={balanceTrend.areaPath} fill="url(#balanceGradient)"/>
                      <!-- Trend line -->
                      <path d={balanceTrend.path} fill="none" stroke="var(--color-optimal)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <!-- Data points -->
                      {#each balanceTrend.points as point, i}
                        {#if i === 0 || i === balanceTrend.points.length - 1 || balanceTrend.points.length <= 8}
                          <circle cx={point.x} cy={point.y} r="3" fill="var(--bg-surface)" stroke="var(--color-optimal)" stroke-width="1.5"/>
                        {/if}
                      {/each}
                    </svg>
                  </div>
                  <div class="trend-stats">
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">Current <InfoTip text="Value from your most recent ride in this period." position="bottom" size="sm" /></span>
                      <span class="trend-stat-value {getBalanceStatus(balanceTrend.points[balanceTrend.points.length - 1]?.balance || 50)}">{(balanceTrend.points[balanceTrend.points.length - 1]?.balance || 50).toFixed(1)}%</span>
                    </div>
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">Average <InfoTip text="Average value across all rides in the selected period." position="bottom" size="sm" /></span>
                      <span class="trend-stat-value">{((balanceTrend.minBalance + balanceTrend.maxBalance) / 2).toFixed(1)}%</span>
                    </div>
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">Spread <InfoTip text="Difference between best and worst. Lower spread means more consistent form." position="bottom" size="sm" /></span>
                      <span class="trend-stat-value {(balanceTrend.maxBalance - balanceTrend.minBalance) > 5 ? 'problem' : (balanceTrend.maxBalance - balanceTrend.minBalance) > 2.5 ? 'attention' : 'optimal'}">{(balanceTrend.maxBalance - balanceTrend.minBalance).toFixed(1)}%</span>
                    </div>
                  </div>
                {:else}
                  <div class="trend-empty">Need more rides</div>
                {/if}
              </div>

              <!-- Technique Trend -->
              <div class="trend-card">
                <div class="trend-card-header">
                  <span class="trend-card-title">Technique <InfoTip text="Technique metrics trend over time. Look for steady improvement patterns." position="bottom" /></span>
                  <div class="trend-metric-switcher">
                    <button class:active={activeTrendMetric === 'asymmetry'} on:click={() => activeTrendMetric = 'asymmetry'}>Asym</button>
                    <button class:active={activeTrendMetric === 'te'} on:click={() => activeTrendMetric = 'te'}>TE</button>
                    <button class:active={activeTrendMetric === 'ps'} on:click={() => activeTrendMetric = 'ps'}>PS</button>
                  </div>
                </div>
                {#if techniqueTrend.path}
                  <div class="trend-chart">
                    <svg viewBox="0 0 200 60" class="trend-svg">
                      <defs>
                        <linearGradient id="techniqueGradientAsym" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" stop-color="var(--color-accent)" stop-opacity="0.25"/>
                          <stop offset="100%" stop-color="var(--color-accent)" stop-opacity="0"/>
                        </linearGradient>
                        <linearGradient id="techniqueGradientTE" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" stop-color="var(--color-optimal)" stop-opacity="0.25"/>
                          <stop offset="100%" stop-color="var(--color-optimal)" stop-opacity="0"/>
                        </linearGradient>
                        <linearGradient id="techniqueGradientPS" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" stop-color="var(--color-attention)" stop-opacity="0.25"/>
                          <stop offset="100%" stop-color="var(--color-attention)" stop-opacity="0"/>
                        </linearGradient>
                      </defs>
                      <!-- Horizontal grid lines -->
                      <line x1="8" y1="15" x2="192" y2="15" stroke="var(--border-subtle)" stroke-width="0.5" opacity="0.4"/>
                      <line x1="8" y1="30" x2="192" y2="30" stroke="var(--border-subtle)" stroke-width="0.5" opacity="0.4"/>
                      <line x1="8" y1="45" x2="192" y2="45" stroke="var(--border-subtle)" stroke-width="0.5" opacity="0.4"/>
                      <!-- Y-axis min/max labels -->
                      <text x="196" y="12" font-size="6" fill="var(--text-muted)" text-anchor="end">{techniqueTrend.max.toFixed(0)}%</text>
                      <text x="196" y="56" font-size="6" fill="var(--text-muted)" text-anchor="end">{techniqueTrend.min.toFixed(0)}%</text>
                      <!-- Area fill -->
                      <path d={techniqueTrend.areaPath} fill={activeTrendMetric === 'asymmetry' ? 'url(#techniqueGradientAsym)' : activeTrendMetric === 'te' ? 'url(#techniqueGradientTE)' : 'url(#techniqueGradientPS)'}/>
                      <!-- Trend line -->
                      <path d={techniqueTrend.path} fill="none" stroke={getTrendColor(activeTrendMetric)} stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <!-- Data points -->
                      {#each techniqueTrend.points as point, i}
                        {#if i === 0 || i === techniqueTrend.points.length - 1 || techniqueTrend.points.length <= 8}
                          <circle cx={point.x} cy={point.y} r="3" fill="var(--bg-surface)" stroke={getTrendColor(activeTrendMetric)} stroke-width="1.5"/>
                        {/if}
                      {/each}
                    </svg>
                  </div>
                  <div class="trend-stats">
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">Latest <InfoTip text="Value from your most recent ride for this metric." position="bottom" size="sm" /></span>
                      <span class="trend-stat-value {activeTrendMetric === 'asymmetry' ? getAsymmetryClass(techniqueTrend.points[techniqueTrend.points.length - 1]?.value || 0) : ''}">{(techniqueTrend.points[techniqueTrend.points.length - 1]?.value || 0).toFixed(1)}%</span>
                    </div>
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">Average <InfoTip text="Average value across all rides in the selected period." position="bottom" size="sm" /></span>
                      <span class="trend-stat-value">{(techniqueTrend.points.reduce((s, p) => s + p.value, 0) / techniqueTrend.points.length).toFixed(1)}%</span>
                    </div>
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">Best <InfoTip text="Best value in the period. For asymmetry lower is better, for TE/PS higher is better." position="bottom" size="sm" /></span>
                      <span class="trend-stat-value optimal">{(activeTrendMetric === 'asymmetry' ? Math.min(...techniqueTrend.points.map(p => p.value)) : Math.max(...techniqueTrend.points.map(p => p.value))).toFixed(1)}%</span>
                    </div>
                  </div>
                {:else}
                  <div class="trend-empty">Need more data</div>
                {/if}
              </div>
            </div>
          {/if}

          <!-- Progress Row (if we have comparison data) -->
          {#if prevPeriodStats && periodStats}
            <div class="progress-row-card animate-in">
              <div class="progress-header">
                <span class="progress-title">Progress vs Previous {selectedPeriod} Days</span>
              </div>
              <div class="progress-items">
                {#if scoreProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">Score</span>
                    <span class="progress-item-now">{periodStats.score.toFixed(0)}</span>
                    <span class="progress-item-change {scoreProgress.direction}">{scoreProgress.direction === 'up' ? '↑' : scoreProgress.direction === 'down' ? '↓' : ''}{scoreProgress.value.toFixed(0)}</span>
                  </div>
                {/if}
                {#if optimalProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">Optimal Zone</span>
                    <span class="progress-item-now">{periodStats.zoneOptimal.toFixed(0)}%</span>
                    <span class="progress-item-change {optimalProgress.direction}">{optimalProgress.direction === 'up' ? '↑' : optimalProgress.direction === 'down' ? '↓' : ''}{optimalProgress.value.toFixed(0)}%</span>
                  </div>
                {/if}
                {#if teProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">TE</span>
                    <span class="progress-item-now">{periodStats.te.toFixed(0)}%</span>
                    <span class="progress-item-change {teProgress.direction}">{teProgress.direction === 'up' ? '↑' : teProgress.direction === 'down' ? '↓' : ''}{teProgress.value.toFixed(0)}</span>
                  </div>
                {/if}
                {#if psProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">PS</span>
                    <span class="progress-item-now">{periodStats.ps.toFixed(0)}%</span>
                    <span class="progress-item-change {psProgress.direction}">{psProgress.direction === 'up' ? '↑' : psProgress.direction === 'down' ? '↓' : ''}{psProgress.value.toFixed(0)}</span>
                  </div>
                {/if}
                {#if durationProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">Time</span>
                    <span class="progress-item-now">{(periodStats.duration / 3600000).toFixed(1)}h</span>
                    <span class="progress-item-change {durationProgress.direction}">{durationProgress.direction === 'up' ? '↑' : durationProgress.direction === 'down' ? '↓' : ''}{durationProgress.value.toFixed(1)}h</span>
                  </div>
                {/if}
                {#if distanceProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">Distance</span>
                    <span class="progress-item-now">{periodStats.totalDistance.toFixed(0)}km</span>
                    <span class="progress-item-change {distanceProgress.direction}">{distanceProgress.direction === 'up' ? '↑' : distanceProgress.direction === 'down' ? '↓' : ''}{distanceProgress.value.toFixed(0)}</span>
                  </div>
                {/if}
              </div>
            </div>
          {/if}

          <!-- Recent Rides -->
          {#if recentRides.length > 0}
            <div class="recent-rides animate-in">
              <div class="card-header">
                <span class="card-title">Recent Rides <InfoTip text="Recent rides with technique metrics. Click any row to see full details." position="bottom" /></span>
                <a href="/rides" class="view-all">View all →</a>
              </div>
              <div class="rides-table-wrap">
                <table class="rides-table">
                  <thead>
                    <tr>
                      <th class="col-date">Date</th>
                      <th class="col-duration">Duration</th>
                      <th class="col-asymmetry">Asym <InfoTip text="Power asymmetry percentage. Lower means more balanced power output." position="bottom" size="sm" /></th>
                      <th class="col-balance">L / R</th>
                      <th class="col-te">TE <InfoTip text="Torque Effectiveness for left and right legs separately." position="bottom" size="sm" /></th>
                      <th class="col-ps">PS <InfoTip text="Pedal Smoothness for left and right legs separately." position="bottom" size="sm" /></th>
                      <th class="col-zones">Zones <InfoTip text="Time distribution across optimal, attention, and problem zones." position="bottom" size="sm" /></th>
                      {#if recentRides.some(r => r.power_avg > 0)}<th class="col-power">Power</th>{/if}
                    </tr>
                  </thead>
                  <tbody>
                    {#each recentRides.slice(0, 5) as ride}
                      <tr on:click={() => window.location.href = `/rides/${ride.id}`}>
                        <td class="col-date">
                          <span class="date-primary">{formatDate(ride.timestamp)}</span>
                          <span class="date-secondary">{formatRelativeTime(ride.timestamp)}</span>
                        </td>
                        <td class="col-duration">{formatDuration(ride.duration_ms)}</td>
                        <td class="col-asymmetry">
                          <span class="asymmetry-value {getBalanceStatus(ride.balance_left)}">{Math.abs(ride.balance_left - 50).toFixed(1)}%</span>
                          <span class="dominance">{ride.balance_left > 50 ? 'L' : ride.balance_left < 50 ? 'R' : ''}</span>
                        </td>
                        <td class="col-balance">{ride.balance_left.toFixed(0)} / {(100 - ride.balance_left).toFixed(0)}</td>
                        <td class="col-te">{ride.te_left.toFixed(0)}/{ride.te_right.toFixed(0)}</td>
                        <td class="col-ps">{ride.ps_left.toFixed(0)}/{ride.ps_right.toFixed(0)}</td>
                        <td class="col-zones">
                          <div class="zone-bar-mini">
                            <div class="zone-segment optimal" style="width: {ride.zone_optimal}%"></div>
                            <div class="zone-segment attention" style="width: {ride.zone_attention}%"></div>
                            <div class="zone-segment problem" style="width: {ride.zone_problem}%"></div>
                          </div>
                        </td>
                        {#if recentRides.some(r => r.power_avg > 0)}
                          <td class="col-power">{ride.power_avg > 0 ? `${Math.round(ride.power_avg)}W` : '—'}</td>
                        {/if}
                      </tr>
                    {/each}
                  </tbody>
                </table>
              </div>
            </div>
          {/if}
        {:else}
          <div class="empty-state animate-in">
            <div class="empty-icon"><svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg></div>
            <h2>No rides yet</h2>
            <p>Start recording rides with KPedal on your Karoo to see your analytics here.</p>
            <div class="setup-card">
              <h3>Quick Setup</h3>
              <div class="setup-steps">
                <div class="setup-step"><span class="step-number">1</span><span class="step-text">Install KPedal on your Karoo device</span></div>
                <div class="setup-step"><span class="step-number">2</span><span class="step-text">Add KPedal data fields to your ride profile</span></div>
                <div class="setup-step"><span class="step-number">3</span><span class="step-text">Start riding — data syncs automatically</span></div>
              </div>
            </div>
          </div>
        {/if}
      {/if}
    </div>
  </div>
{/if}

<style>
  /* ============================================
     QUIET LUXURY - Apple-inspired Design System
     Refined, spacious, sophisticated
     ============================================ */

  /* Base */
  .landing {
    min-height: 100vh;
    padding: 32px;
    position: relative;
  }

  .site-logo {
    position: fixed;
    top: 32px;
    left: 32px;
    display: flex;
    align-items: center;
    gap: 8px;
    text-decoration: none;
    z-index: 100;
  }
  .site-logo-dot {
    width: 8px;
    height: 8px;
    background: var(--color-optimal);
    border-radius: 50%;
  }
  .site-logo-text {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.5px;
  }

  .theme-toggle {
    position: fixed;
    top: 32px;
    right: 32px;
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-muted);
    cursor: pointer;
    transition: all 0.3s ease;
    z-index: 100;
    backdrop-filter: blur(12px);
  }
  .theme-toggle:hover {
    background: var(--bg-hover);
    color: var(--text-secondary);
    transform: scale(1.05);
  }

  .landing-container {
    max-width: 680px;
    margin: 0 auto;
    padding: 64px 0 96px;
  }

  /* Hero - Clean, impactful */
  .hero {
    text-align: center;
    padding: 48px 0 80px;
  }

  .hero-eyebrow {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 1.5px;
    margin-bottom: 24px;
  }

  .hero-title {
    font-size: 52px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -2px;
    line-height: 1.1;
    margin-bottom: 24px;
  }
  .hero-title-line {
    display: block;
  }

  .hero-subtitle {
    font-size: 17px;
    color: var(--text-tertiary);
    max-width: 480px;
    margin: 0 auto 40px;
    line-height: 1.6;
  }

  .hero-cta {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: 54px;
    padding: 0 28px;
    background: var(--text-primary);
    color: var(--bg-base);
    font-size: 15px;
    font-weight: 500;
    border-radius: 100px;
    border: 1px solid transparent;
    text-decoration: none;
    transition: all 0.2s ease;
  }
  .hero-cta:hover {
    opacity: 0.85;
    transform: translateY(-1px);
  }
  .hero-cta svg {
    transition: transform 0.2s ease;
  }
  .hero-cta:hover svg {
    transform: translateX(3px);
  }

  .hero-version {
    display: inline-block;
    margin-left: 8px;
    padding: 3px 8px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-default);
    border-radius: 6px;
    font-size: 11px;
    font-weight: 500;
    color: var(--text-tertiary);
    letter-spacing: 0;
    text-transform: none;
  }

  .hero-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
  }

  .hero-cta-secondary {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: 54px;
    padding: 0 28px;
    background: transparent;
    color: var(--text-primary);
    font-size: 15px;
    font-weight: 500;
    border-radius: 100px;
    border: 1px solid var(--border-default);
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .hero-cta-secondary:hover {
    background: var(--bg-hover);
    border-color: var(--border-strong);
  }

  .hero-note {
    font-size: 13px;
    color: var(--text-muted);
    margin-top: 24px;
  }

  /* Logo dot for footer */
  .logo-dot {
    width: 8px;
    height: 8px;
    background: var(--color-optimal);
    border-radius: 50%;
  }
  .logo-dot.small { width: 6px; height: 6px; }

  /* Buttons - Refined, subtle */
  .cta-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 24px;
    border-radius: 100px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.25s ease;
    text-decoration: none;
    border: none;
    letter-spacing: -0.1px;
  }
  .cta-btn svg { opacity: 0.8; }

  .cta-btn.primary {
    background: var(--text-primary);
    color: var(--bg-base);
  }
  .cta-btn.primary:hover {
    opacity: 0.85;
    transform: translateY(-1px);
  }

  .cta-btn.secondary {
    background: transparent;
    border: 1px solid var(--border-default);
    color: var(--text-primary);
  }
  .cta-btn.secondary:hover {
    background: var(--bg-hover);
    border-color: var(--border-strong);
  }

  .cta-btn.large {
    padding: 14px 32px;
    font-size: 15px;
  }

  /* Sections - Generous spacing */
  .section {
    margin-bottom: 96px;
  }
  .section-title {
    font-size: 26px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 10px;
    letter-spacing: -0.5px;
    text-transform: none;
  }
  .section-subtitle {
    font-size: 15px;
    color: var(--text-tertiary);
    margin-bottom: 40px;
    line-height: 1.6;
    letter-spacing: -0.1px;
  }
  /* Metrics Deep - Refined cards */
  .metrics-deep {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .metric-deep-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 18px;
    padding: 24px 28px;
    transition: all 0.3s ease;
  }
  .metric-deep-card:hover {
    border-color: var(--border-default);
  }
  .metric-deep-card.featured {
    border-color: rgba(94, 232, 156, 0.15);
  }
  .metric-deep-header {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 14px;
  }
  .metric-deep-icon {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    opacity: 0.8;
  }
  .metric-deep-icon.balance { background: var(--color-attention); }
  .metric-deep-icon.te { background: var(--color-optimal); }
  .metric-deep-icon.ps { background: var(--color-problem); }
  .metric-deep-header h3 {
    font-size: 17px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
    letter-spacing: -0.2px;
  }
  .metric-deep-range {
    font-size: 12px;
    color: var(--text-muted);
    font-weight: 500;
    margin-left: auto;
  }
  .metric-deep-desc {
    font-size: 14px;
    color: var(--text-tertiary);
    line-height: 1.7;
    margin-bottom: 18px;
  }
  .metric-zones {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
  .zone {
    font-size: 11px;
    padding: 6px 12px;
    border-radius: 100px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 500;
  }
  .zone span { font-weight: 600; }
  .zone.optimal { background: var(--color-optimal-soft); color: var(--color-optimal-text); }
  .zone.attention { background: var(--color-attention-soft); color: var(--color-attention-text); }
  .zone.problem { background: var(--color-problem-soft); color: var(--color-problem-text); }

  /* Data Fields Showcase - Premium feel */
  .datafields-section {
    text-align: center;
  }
  .datafields-showcase {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    overflow: hidden;
  }
  .datafield-tabs {
    display: flex;
    border-bottom: 1px solid var(--border-subtle);
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    padding: 0;
  }
  .datafield-tabs::-webkit-scrollbar { display: none; }
  .datafield-tab {
    flex: 1;
    min-width: 80px;
    padding: 16px 14px;
    background: none;
    border: none;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: var(--text-muted);
    transition: all 0.3s ease;
    border-bottom: 2px solid transparent;
    margin-bottom: -1px;
    position: relative;
  }
  .datafield-tab:hover {
    color: var(--text-secondary);
    background: var(--bg-hover);
  }
  .datafield-tab.active {
    color: var(--text-primary);
    border-bottom-color: var(--color-accent);
  }
  .datafield-preview {
    display: flex;
    gap: 40px;
    padding: 40px;
    align-items: center;
  }

  /* Device Frame - Premium look */
  .preview-device {
    flex-shrink: 0;
  }
  .device-frame {
    background: linear-gradient(160deg, #2a2a2e 0%, #1a1a1c 50%, #0c0c0e 100%);
    border-radius: 32px;
    padding: 10px;
    box-shadow:
      0 30px 80px rgba(0, 0, 0, 0.5),
      0 0 0 1px rgba(255, 255, 255, 0.05),
      inset 0 1px 0 rgba(255, 255, 255, 0.08);
    position: relative;
  }
  .device-notch {
    position: absolute;
    top: 4px;
    left: 50%;
    transform: translateX(-50%);
    width: 24px;
    height: 4px;
    background: #0c0c0e;
    border-radius: 2px;
  }
  .preview-screen.karoo {
    width: 160px;
    height: 260px;
    background: #000000;
    border-radius: 24px;
    overflow: hidden;
    font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  }
  .preview-info {
    flex: 1;
    text-align: left;
  }
  .preview-info h4 {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 14px;
    letter-spacing: -0.4px;
  }
  .preview-info p {
    font-size: 15px;
    color: var(--text-tertiary);
    line-height: 1.7;
    max-width: 320px;
  }

  /* Karoo Layout Base Styles */
  .karoo-layout {
    width: 100%;
    height: 100%;
    padding: 16px 14px;
    display: flex;
    flex-direction: column;
    background: #000000;
  }
  .karoo-label {
    font-size: 9px;
    color: #6e6e73;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: 500;
  }
  .karoo-divider {
    height: 1px;
    background: rgba(255, 255, 255, 0.08);
    margin: 8px 0;
  }
  .karoo-layout .optimal {
    color: #30d158 !important;
  }

  /* Balance Bar (reused across layouts) */
  .balance-bar {
    width: 100%;
    height: 6px;
    background: #3a3a3c;
    border-radius: 3px;
    overflow: hidden;
    position: relative;
  }
  .balance-bar.lg { height: 8px; border-radius: 4px; }
  .bar-fill {
    position: absolute;
    left: 0;
    top: 0;
    height: 100%;
    background: linear-gradient(90deg, #8e8e93 0%, #a2a2a7 100%);
    border-radius: inherit;
  }
  .balance-values {
    display: flex;
    justify-content: space-between;
    width: 100%;
    padding: 0 8px;
  }
  .balance-num {
    font-size: 28px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
  }
  .balance-labels {
    display: flex;
    justify-content: space-between;
    font-size: 9px;
    color: #6e6e73;
    margin-top: 4px;
    padding: 0 4px;
    font-weight: 500;
  }

  /* Quick Glance */
  .quick-glance { justify-content: space-between; }
  .qg-status-section {
    flex: 0.4;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }
  .qg-status-icon {
    width: 48px;
    height: 48px;
    color: #30d158;
  }
  .qg-status-text {
    font-size: 10px;
    color: #6e6e73;
    font-weight: 500;
  }
  .qg-balance-section {
    flex: 0.6;
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding-top: 8px;
  }

  /* Power Balance */
  .power-balance {
    justify-content: space-between;
    gap: 12px;
  }
  .pb-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .pb-status {
    font-size: 9px;
    font-weight: 600;
    padding: 3px 8px;
    border-radius: 4px;
    background: rgba(48, 209, 88, 0.15);
    color: #30d158;
  }
  .pb-main {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
  }
  .pb-side {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
  }
  .pb-value {
    font-size: 42px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  .pb-label {
    font-size: 10px;
    color: #6e6e73;
    font-weight: 500;
  }
  .pb-divider {
    width: 1px;
    height: 48px;
    background: rgba(255, 255, 255, 0.1);
  }

  /* Efficiency */
  .efficiency { justify-content: space-between; }
  .eff-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 6px 0;
  }
  .eff-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .eff-avg {
    font-size: 10px;
    font-weight: 600;
  }
  .eff-values {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
  }
  .eff-side {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
    gap: 2px;
  }
  .eff-num {
    font-size: 32px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  .eff-side-label {
    font-size: 9px;
    color: #6e6e73;
    font-weight: 500;
  }
  .eff-divider {
    width: 1px;
    height: 36px;
    background: rgba(255, 255, 255, 0.1);
  }

  /* Full Overview */
  .full-overview {
    justify-content: space-between;
    gap: 0;
  }
  .fo-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    padding: 4px 0;
  }
  .fo-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .fo-status {
    font-size: 9px;
    font-weight: 600;
  }
  .fo-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
  }
  .fo-num {
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
  }
  .fo-num.sm { font-size: 20px; }
  .fo-sep {
    font-size: 16px;
    color: #48484a;
    font-weight: 400;
  }

  /* Balance Trend */
  .balance-trend {
    justify-content: space-between;
    gap: 4px;
  }
  .bt-section {
    display: flex;
    flex-direction: column;
    flex: 1;
  }
  .bt-section.main { flex: 1.4; }
  .bt-values {
    display: flex;
    justify-content: center;
    align-items: center;
    flex: 1;
    gap: 6px;
  }
  .bt-values span {
    font-size: 26px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
  }
  .bt-values.lg span { font-size: 36px; }
  .bt-sep {
    font-size: 20px;
    color: #48484a;
    font-weight: 400;
  }
  .bt-section:not(.main) .bt-values span {
    color: #8e8e93;
    font-weight: 400;
    font-size: 22px;
  }
  .bt-section:not(.main) .bt-sep {
    font-size: 16px;
  }

  /* Single Balance */
  .single-balance {
    justify-content: space-between;
    gap: 12px;
  }
  .sb-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .sb-status {
    font-size: 9px;
    font-weight: 600;
    padding: 3px 8px;
    border-radius: 4px;
    background: rgba(48, 209, 88, 0.15);
    color: #30d158;
  }
  .sb-main {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
  }
  .sb-side {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
  }
  .sb-num {
    font-size: 52px;
    font-weight: 600;
    color: #ffffff;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  .sb-label {
    font-size: 11px;
    color: #6e6e73;
    font-weight: 500;
  }
  .sb-divider {
    width: 1px;
    height: 56px;
    background: rgba(255, 255, 255, 0.1);
  }

  /* Drills - Apple-style clean */
  .drills-section {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    padding: 36px;
    margin-bottom: 96px;
  }
  .drills-tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 32px;
    background: var(--bg-base);
    padding: 4px;
    border-radius: 12px;
    width: fit-content;
  }
  .drill-tab {
    padding: 10px 24px;
    background: transparent;
    border: none;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-tertiary);
    cursor: pointer;
    transition: all 0.25s ease;
  }
  .drill-tab:hover {
    color: var(--text-secondary);
  }
  .drill-tab.active {
    background: var(--bg-surface);
    color: var(--text-primary);
    box-shadow: var(--shadow-sm);
  }
  .drills-list {
    display: flex;
    flex-direction: column;
    gap: 0;
    margin-bottom: 32px;
  }
  .landing .drill-item {
    background: transparent;
    border: none;
    border-bottom: 1px solid var(--border-subtle);
    border-radius: 0;
    padding: 20px 16px;
    margin: 0 -16px;
    cursor: pointer;
    transition: background 0.2s ease;
    text-align: left;
    width: calc(100% + 32px);
  }
  .landing .drill-item:first-child {
    border-top: 1px solid var(--border-subtle);
  }
  /* Hover only on desktop with mouse */
  @media (hover: hover) and (pointer: fine) {
    .landing .drill-item:not(.expanded):hover {
      background: var(--bg-hover);
    }
  }
  .landing .drill-item.expanded {
    background: var(--bg-hover);
    border-radius: 12px;
    border-color: transparent;
    margin-top: 8px;
    margin-bottom: 8px;
  }
  .landing .drill-item.expanded + .drill-item {
    border-top-color: transparent;
  }
  .landing .drill-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .landing .drill-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .landing .drill-name {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    letter-spacing: -0.2px;
  }
  .landing .drill-meta {
    display: flex;
    gap: 8px;
    align-items: center;
  }
  .landing .drill-duration {
    font-size: 13px;
    color: var(--text-muted);
  }
  .landing .drill-dot {
    width: 3px;
    height: 3px;
    background: var(--text-faint);
    border-radius: 50%;
  }
  .landing .drill-level {
    font-size: 12px;
    color: var(--text-muted);
  }
  .landing .drill-level.beginner { color: var(--color-optimal-text); }
  .landing .drill-level.intermediate { color: var(--color-attention-text); }
  .landing .drill-level.advanced { color: var(--color-problem-text); }
  .landing .drill-chevron {
    color: var(--text-faint);
    transition: all 0.25s ease;
    width: 16px;
    height: 16px;
  }
  .landing .drill-item.expanded .drill-chevron {
    transform: rotate(180deg);
    color: var(--text-muted);
  }
  .landing .drill-details {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid var(--border-subtle);
  }
  .landing .drill-details p {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.6;
    margin-bottom: 12px;
  }
  .landing .drill-target {
    font-size: 13px;
    color: var(--text-muted);
    font-variant-numeric: tabular-nums;
  }
  .drills-highlight {
    padding: 24px 28px;
    background: var(--bg-base);
    border-radius: 16px;
    border: 1px solid var(--border-subtle);
  }
  .highlight-label {
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--color-accent);
    font-weight: 600;
    display: block;
    margin-bottom: 10px;
  }
  .drills-highlight h4 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
    letter-spacing: -0.2px;
  }
  .drills-highlight p {
    font-size: 14px;
    color: var(--text-tertiary);
    line-height: 1.6;
  }

  /* Alerts - Apple-style clean */
  .alerts-section {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    padding: 36px;
  }
  .alerts-demo {
    display: flex;
    flex-direction: column;
    gap: 36px;
  }
  .alert-banner-demo {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px 28px;
    background: var(--bg-base);
    border-radius: 16px;
    max-width: 320px;
    margin: 0 auto;
  }
  .alert-indicator {
    width: 8px;
    height: 8px;
    background: var(--color-attention);
    border-radius: 50%;
    box-shadow: 0 0 8px var(--color-attention);
    animation: pulse-glow 2s ease-in-out infinite;
  }
  @keyframes pulse-glow {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }
  .alert-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .alert-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.1px;
  }
  .alert-detail {
    font-size: 13px;
    color: var(--text-muted);
    font-variant-numeric: tabular-nums;
  }
  .alerts-features {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0;
    background: var(--bg-base);
    border-radius: 16px;
    overflow: hidden;
  }
  .alert-feature {
    padding: 24px 28px;
    border-bottom: 1px solid var(--border-subtle);
    border-right: 1px solid var(--border-subtle);
  }
  .alert-feature:nth-child(2n) {
    border-right: none;
  }
  .alert-feature:nth-last-child(-n+2) {
    border-bottom: none;
  }
  .alert-feature h4 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 6px;
    letter-spacing: -0.1px;
  }
  .alert-feature p {
    font-size: 13px;
    color: var(--text-muted);
    line-height: 1.55;
  }

  /* Dashboard Preview - Refined browser */
  .dashboard-preview {
    display: flex;
    flex-direction: column;
    gap: 32px;
  }
  .dash-browser {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    overflow: hidden;
  }
  .browser-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 14px 20px;
    background: var(--bg-elevated);
    border-bottom: 1px solid var(--border-subtle);
  }
  .browser-dots {
    display: flex;
    gap: 8px;
  }
  .browser-dots span {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    opacity: 0.7;
  }
  .browser-dots span:first-child { background: #ff5f56; }
  .browser-dots span:nth-child(2) { background: #ffbd2e; }
  .browser-dots span:last-child { background: #27c93f; }
  .browser-url {
    flex: 1;
    text-align: center;
    font-size: 12px;
    color: var(--text-muted);
    letter-spacing: -0.1px;
  }
  .browser-content {
    padding: 28px;
  }
  .dash-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  .dash-greeting {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.2px;
  }
  .dash-period {
    font-size: 11px;
    color: var(--text-muted);
    padding: 6px 12px;
    background: var(--bg-base);
    border-radius: 100px;
  }
  .dash-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 16px;
  }
  .dash-stat-card {
    background: var(--bg-base);
    border-radius: 14px;
    padding: 16px;
    text-align: center;
  }
  .dash-stat-value {
    display: block;
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.3px;
  }
  .dash-stat-value.score { color: var(--color-optimal-text); }
  .dash-stat-label {
    font-size: 10px;
    color: var(--text-muted);
    margin-top: 4px;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .dash-chart {
    background: var(--bg-base);
    border-radius: 14px;
    padding: 20px;
  }
  .chart-label {
    font-size: 11px;
    color: var(--text-muted);
    margin-bottom: 12px;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .trend-line {
    width: 100%;
    height: 60px;
  }
  .dash-features {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .dash-feature {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px 20px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 14px;
    font-size: 13px;
    color: var(--text-tertiary);
    letter-spacing: -0.1px;
  }
  .dash-feature svg {
    color: var(--text-muted);
    flex-shrink: 0;
    width: 18px;
    height: 18px;
  }

  /* Motivation - Premium centered layout */
  .motivation-section {
    text-align: center;
  }
  .motivation-content {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    padding: 40px;
  }
  .motivation-stats {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 48px;
    margin-bottom: 36px;
  }
  .motivation-stat {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .stat-number {
    font-size: 44px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -1.5px;
    line-height: 1;
  }
  .stat-desc {
    font-size: 14px;
    color: var(--text-muted);
  }
  .motivation-divider {
    width: 1px;
    height: 56px;
    background: var(--border-subtle);
  }
  .motivation-examples {
    display: flex;
    justify-content: center;
    gap: 48px;
    padding-top: 28px;
    border-top: 1px solid var(--border-subtle);
  }
  .example-group {
    text-align: left;
  }
  .example-label {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    display: block;
    margin-bottom: 14px;
  }
  .example-items {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .example-items span {
    font-size: 14px;
    color: var(--text-tertiary);
  }

  /* Works Everywhere - Subtle, elegant */
  .works-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }
  .works-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 18px;
    padding: 28px 24px;
    text-align: center;
    transition: all 0.3s ease;
  }
  .works-card:hover {
    border-color: var(--border-default);
  }
  .works-icon {
    width: 48px;
    height: 48px;
    background: var(--bg-elevated);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-muted);
    margin: 0 auto 16px;
  }
  .works-icon svg {
    width: 26px;
    height: 26px;
  }
  .works-card h4 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 12px;
    letter-spacing: -0.2px;
  }
  .works-card p {
    font-size: 13px;
    color: var(--text-tertiary);
    line-height: 1.7;
  }

  /* Requirements / What You Need - Premium */
  .requirements-section {
    padding-bottom: 24px;
  }
  .requirements-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
    margin-bottom: 32px;
  }
  .req-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 18px;
    padding: 28px 24px;
    text-align: center;
    transition: all 0.3s ease;
  }
  .req-card:hover {
    border-color: var(--border-default);
  }
  .req-icon {
    width: 44px;
    height: 44px;
    background: var(--bg-elevated);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-muted);
    margin: 0 auto 14px;
  }
  .req-icon svg {
    width: 24px;
    height: 24px;
  }
  .req-card h4 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 10px;
    letter-spacing: -0.2px;
  }
  .req-card p {
    font-size: 13px;
    color: var(--text-tertiary);
    line-height: 1.65;
  }
  .compatible-pedals {
    padding: 32px 0;
    text-align: center;
    border-top: 1px solid var(--border-subtle);
    margin-top: 40px;
  }
  .pedals-label {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    display: block;
    margin-bottom: 16px;
  }
  .pedals-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
    align-items: center;
    margin-bottom: 14px;
  }
  .pedals-list span {
    font-size: 14px;
    color: var(--text-secondary);
  }
  .pedals-sep {
    color: var(--text-faint);
  }
  .pedals-partial {
    color: var(--text-muted);
  }
  .pedals-note {
    font-size: 12px;
    color: var(--text-muted);
  }

  /* CTA - Refined, spacious */
  .cta-section {
    text-align: center;
    padding: 80px 0 64px;
  }
  .cta-headline {
    font-size: 30px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 16px;
    letter-spacing: -0.5px;
  }
  .cta-subtext {
    font-size: 16px;
    color: var(--text-tertiary);
    margin-bottom: 40px;
    letter-spacing: -0.1px;
  }
  .cta-actions {
    display: flex;
    gap: 16px;
    justify-content: center;
    flex-wrap: wrap;
  }
  .cta-note {
    margin-top: 24px;
    font-size: 13px;
    color: var(--text-muted);
  }

  /* Before/After - Clean comparison */
  .comparison-table {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    overflow: hidden;
  }
  .comparison-header {
    display: grid;
    grid-template-columns: 1fr 1fr;
    background: var(--bg-elevated);
  }
  .comparison-col {
    padding: 16px 24px;
    font-size: 11px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .comparison-col.highlight { background: var(--color-optimal-soft); color: var(--color-optimal-text); }
  .comparison-row { display: grid; grid-template-columns: 1fr 1fr; border-top: 1px solid var(--border-subtle); }
  .comparison-item { display: flex; align-items: center; gap: 12px; padding: 14px 18px; font-size: 12px; }
  .comparison-item.before { color: var(--text-muted); }
  .comparison-item.before svg { color: var(--color-problem); width: 14px; height: 14px; }
  .comparison-item.after { background: var(--color-optimal-soft); color: var(--text-primary); }
  .comparison-item.after svg { color: var(--color-optimal); width: 14px; height: 14px; }

  /* FAQ - Elegant, refined */
  .faq-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .faq-item {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
    overflow: hidden;
    transition: all 0.3s ease;
  }
  .faq-item:hover {
    border-color: var(--border-default);
  }
  .faq-item summary {
    padding: 18px 22px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
    cursor: pointer;
    list-style: none;
    display: flex;
    justify-content: space-between;
    align-items: center;
    letter-spacing: -0.1px;
    transition: all 0.3s ease;
  }
  .faq-item summary::-webkit-details-marker { display: none; }
  .faq-item summary::after {
    content: '+';
    font-size: 18px;
    color: var(--text-muted);
    transition: all 0.3s ease;
    font-weight: 300;
  }
  .faq-item[open] summary::after { content: '−'; }
  .faq-item[open] summary {
    border-bottom: 1px solid var(--border-subtle);
    color: var(--text-primary);
  }
  .faq-item p {
    padding: 0 22px 18px;
    font-size: 14px;
    color: var(--text-tertiary);
    line-height: 1.7;
    margin: 0;
  }

  /* Footer - WattRamp style */
  .landing-footer {
    padding: 60px 0 40px;
    border-top: 1px solid var(--border-subtle);
  }
  .footer-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 48px;
    margin-bottom: 40px;
  }
  .footer-brand {
    max-width: 200px;
  }
  .footer-logo {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
  }
  .footer-brand-name {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.3px;
  }
  .footer-tagline {
    font-size: 13px;
    color: var(--text-muted);
    line-height: 1.5;
  }
  .footer-nav {
    display: flex;
    gap: 56px;
  }
  .footer-col {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .footer-col h4 {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 4px;
  }
  .footer-col a,
  .footer-link-btn {
    font-size: 14px;
    color: var(--text-secondary);
    text-decoration: none;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
    text-align: left;
    font-family: inherit;
    transition: color 0.15s ease;
  }
  .footer-col a:hover,
  .footer-link-btn:hover {
    color: var(--text-primary);
  }
  .footer-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 24px;
    border-top: 1px solid var(--border-subtle);
  }
  .footer-bottom span {
    font-size: 13px;
    color: var(--text-muted);
  }
  .footer-github {
    color: var(--text-muted);
    transition: color 0.15s ease;
  }
  .footer-github:hover {
    color: var(--text-primary);
  }

  /* Dashboard (authenticated) - Compact Design */
  .dashboard { padding: 24px 0 48px; }
  .loading-state, .error-state { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 60vh; gap: 16px; color: var(--text-secondary); }

  /* Dash Header */
  .dash-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
  .dash-greeting h1 { font-size: 22px; font-weight: 600; color: var(--text-primary); letter-spacing: -0.3px; }
  .dash-controls { display: flex; gap: 8px; }

  /* Hero Stats Row */
  .hero-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 16px;
  }
  .hero-stat {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
  }
  .hero-stat.asymmetry { display: flex; align-items: center; gap: 12px; }
  .hero-stat-main { flex-shrink: 0; }
  .hero-stat-value { font-size: 26px; font-weight: 700; color: var(--text-primary); }
  .hero-stat-value.optimal { color: var(--color-optimal); }
  .hero-stat-value.attention { color: var(--color-attention); }
  .hero-stat-value.problem { color: var(--color-problem); }
  .hero-stat-info { display: flex; flex-direction: column; gap: 1px; }
  .hero-stat-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; }
  .hero-stat-detail { font-size: 11px; color: var(--text-tertiary); }

  /* Balance Display */
  .hero-stat.balance-visual { display: flex; align-items: center; justify-content: center; }
  .balance-display { display: flex; align-items: center; gap: 10px; width: 100%; }
  .balance-side { display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 32px; }
  .balance-pct { font-size: 18px; font-weight: 700; color: var(--text-primary); line-height: 1; }
  .balance-leg { font-size: 10px; font-weight: 600; color: var(--text-muted); }
  .balance-bar-wrap { flex: 1; position: relative; }
  .balance-bar { display: flex; height: 8px; background: var(--bg-base); border-radius: 4px; overflow: hidden; }
  .balance-fill { height: 100%; transition: width 0.3s; }
  .balance-fill.left { background: var(--color-optimal); margin-left: auto; border-radius: 4px 0 0 4px; }
  .balance-fill.right { background: var(--color-optimal); border-radius: 0 4px 4px 0; }
  .balance-center-mark { position: absolute; left: 50%; top: -3px; bottom: -3px; width: 2px; background: var(--text-primary); transform: translateX(-50%); border-radius: 1px; }

  /* Zone Mini */
  .hero-stat.zones { display: flex; flex-direction: column; gap: 6px; }
  .zone-mini-bars { display: flex; height: 8px; border-radius: 4px; overflow: hidden; background: var(--bg-base); }
  .zone-mini { height: 100%; transition: width 0.3s; }
  .zone-mini.optimal { background: var(--color-optimal); }
  .zone-mini.attention { background: var(--color-attention); }
  .zone-mini.problem { background: var(--color-problem); }
  .zone-mini-values { display: flex; justify-content: space-between; }
  .zone-mini-val { font-size: 13px; font-weight: 600; }
  .zone-mini-val.optimal { color: var(--color-optimal-text); }
  .zone-mini-val.attention { color: var(--color-attention-text); }
  .zone-mini-val.problem { color: var(--color-problem-text); }

  /* Summary Grid */
  .hero-stat.summary { display: flex; align-items: center; justify-content: center; }
  .summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; width: 100%; }
  .summary-metric { display: flex; flex-direction: column; align-items: center; gap: 2px; }
  .summary-num { font-size: 20px; font-weight: 700; color: var(--text-primary); line-height: 1; }
  .summary-unit { font-size: 10px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; }

  /* Main Grid */
  .main-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; align-items: stretch; }
  .grid-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    display: flex;
    flex-direction: column;
    min-width: 0;
    overflow: hidden;
  }
  .grid-card.wide { grid-column: span 2; }
  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
  .card-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .card-subtitle { font-size: 11px; color: var(--text-muted); }
  .card-section { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--border-subtle); }
  .section-label { display: block; font-size: 10px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; margin-bottom: 8px; }

  /* Comparison Grid */
  .comparison-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
  .comp-item { text-align: center; padding: 8px 4px; background: var(--bg-base); border-radius: 6px; }
  .comp-label { display: block; font-size: 9px; color: var(--text-muted); margin-bottom: 2px; }
  .comp-value { display: block; font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .comp-delta { display: block; font-size: 10px; font-weight: 500; margin-top: 2px; }
  .comp-delta.up { color: var(--color-optimal-text); }
  .comp-delta.down { color: var(--color-problem-text); }

  /* Technique Enhanced */
  .technique-enhanced {
    background: var(--bg-base);
    border-radius: 8px;
    padding: 12px;
    margin-bottom: 12px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    flex: 1;
    justify-content: center;
  }
  .technique-metric {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .technique-metric-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
  }
  .technique-metric-label {
    font-size: 11px;
    color: var(--text-muted);
    font-weight: 500;
  }
  .technique-metric-value {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }
  .technique-metric-bar-wrap {
    position: relative;
  }
  .technique-metric-bar {
    height: 10px;
    background: var(--bg-elevated);
    border-radius: 5px;
    overflow: hidden;
    position: relative;
  }
  .technique-metric-fill {
    height: 100%;
    border-radius: 5px;
    transition: width 0.3s ease;
  }
  .technique-metric-fill.te {
    background: linear-gradient(90deg, var(--color-optimal), var(--color-optimal-light, var(--color-optimal)));
  }
  .technique-metric-fill.ps {
    background: linear-gradient(90deg, var(--color-attention), var(--color-attention-light, var(--color-attention)));
  }
  .technique-optimal-zone {
    position: absolute;
    top: 0;
    height: 100%;
    border-left: 2px dashed rgba(255,255,255,0.3);
    border-right: 2px dashed rgba(255,255,255,0.3);
    pointer-events: none;
  }
  .technique-optimal-zone.te {
    left: 70%;
    width: 10%;
  }
  .technique-optimal-zone.ps {
    left: 50%;
    width: 12.5%;
  }
  .technique-metric-sides {
    display: flex;
    justify-content: space-between;
    font-size: 10px;
    color: var(--text-muted);
  }

  /* Legacy Technique Grid */
  .technique-grid { display: flex; flex-direction: column; gap: 12px; }
  .technique-item { }
  .technique-label { display: block; font-size: 11px; color: var(--text-muted); margin-bottom: 4px; }
  .technique-values { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 4px; }
  .technique-side { font-size: 11px; color: var(--text-tertiary); }
  .technique-main { font-size: 20px; font-weight: 700; color: var(--text-primary); }
  .technique-bar { height: 4px; background: var(--bg-base); border-radius: 2px; overflow: hidden; }
  .technique-fill { height: 100%; background: var(--color-optimal); border-radius: 2px; }
  .technique-fill.ps { background: var(--color-attention); }

  /* Technique Comparison */
  .technique-comparison { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
  .tech-comp-item { text-align: center; padding: 6px 4px; background: var(--bg-base); border-radius: 6px; }
  .tech-comp-label { display: block; font-size: 9px; color: var(--text-muted); margin-bottom: 2px; }
  .tech-comp-change { font-size: 12px; font-weight: 600; }
  .tech-comp-change.up { color: var(--color-optimal-text); }
  .tech-comp-change.down { color: var(--color-problem-text); }

  /* Fatigue Summary */
  .fatigue-summary { font-size: 12px !important; }
  .fatigue-summary.optimal { color: var(--color-optimal-text); }
  .fatigue-summary.attention { color: var(--color-attention-text); }
  .fatigue-summary.problem { color: var(--color-problem-text); }

  /* Legacy Fatigue Mini */
  .fatigue-mini { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
  .fatigue-mini-item { text-align: center; padding: 6px 4px; background: var(--bg-base); border-radius: 6px; }
  .fatigue-mini-label { display: block; font-size: 9px; color: var(--text-muted); margin-bottom: 2px; }
  .fatigue-mini-val { display: block; font-size: 11px; font-weight: 500; color: var(--text-secondary); }
  .fatigue-mini-delta { display: block; font-size: 11px; font-weight: 600; margin-top: 2px; color: var(--text-muted); }
  .fatigue-mini-delta.up { color: var(--color-optimal-text); }
  .fatigue-mini-delta.down { color: var(--color-problem-text); }

  /* Metrics Compact (chips) */
  .metrics-compact {
    display: flex;
    flex-wrap: nowrap;
    justify-content: flex-start;
    gap: 10px;
    margin-bottom: 16px;
    padding: 16px 20px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }
  .metrics-compact::-webkit-scrollbar { display: none; }
  .metric-chip {
    display: flex;
    align-items: baseline;
    gap: 5px;
    padding: 8px 14px;
    background: var(--bg-base);
    border-radius: 8px;
    flex-shrink: 0;
    white-space: nowrap;
  }
  .metric-chip-val { font-size: 16px; font-weight: 600; color: var(--text-primary); }
  .metric-chip-unit { display: inline-flex; align-items: center; font-size: 12px; color: var(--text-muted); }
  .metric-chip.accent { background: var(--color-accent-soft, rgba(94, 232, 156, 0.1)); }
  .metric-chip.accent .metric-chip-val { color: var(--color-accent, #5ee89c); }
  .metric-chip.elevation .metric-chip-val { color: var(--color-accent); }
  .metric-chip.energy .metric-chip-val { color: var(--color-optimal-text); }

  /* Trends Row (Balance + Technique side by side) */
  .trends-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 16px;
  }
  .trend-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    min-width: 0;
    overflow: hidden;
  }
  .trend-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }
  .trend-card-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .trend-card-period {
    font-size: 11px;
    color: var(--text-muted);
  }
  .trend-chart {
    height: 70px;
    margin-bottom: 12px;
    background: var(--bg-base);
    border-radius: 8px;
    padding: 4px;
  }
  .trend-svg {
    width: 100%;
    height: 100%;
  }
  .trend-stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }
  .trend-stat-item {
    text-align: center;
    padding: 8px 4px;
    background: var(--bg-base);
    border-radius: 6px;
  }
  .trend-stat-label {
    display: block;
    font-size: 10px;
    color: var(--text-muted);
    margin-bottom: 2px;
  }
  .trend-stat-value {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .trend-stat-value.optimal { color: var(--color-optimal-text); }
  .trend-stat-value.attention { color: var(--color-attention-text); }
  .trend-stat-value.problem { color: var(--color-problem-text); }
  .trend-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100px;
    font-size: 12px;
    color: var(--text-muted);
  }
  .trend-metric-switcher {
    display: flex;
    gap: 2px;
    background: var(--bg-base);
    border-radius: 6px;
    padding: 2px;
  }
  .trend-metric-switcher button {
    padding: 4px 8px;
    font-size: 10px;
    font-weight: 500;
    color: var(--text-muted);
    background: transparent;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.15s;
  }
  .trend-metric-switcher button:hover { color: var(--text-secondary); }
  .trend-metric-switcher button.active {
    background: var(--color-accent);
    color: var(--color-accent-text, #000);
    font-weight: 600;
  }

  /* Progress Row Card */
  .progress-row-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 16px;
  }
  .progress-header {
    margin-bottom: 12px;
  }
  .progress-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-secondary);
  }
  .progress-items {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
    gap: 8px;
  }
  .progress-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 10px 8px;
    background: var(--bg-base);
    border-radius: 8px;
    text-align: center;
  }
  .progress-item-label {
    font-size: 10px;
    color: var(--text-muted);
    margin-bottom: 4px;
  }
  .progress-item-now {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    line-height: 1.2;
  }
  .progress-item-change {
    font-size: 12px;
    font-weight: 600;
    margin-top: 2px;
  }
  .progress-item-change.up { color: var(--color-optimal-text); }
  .progress-item-change.down { color: var(--color-problem-text); }
  .progress-item-change.same { color: var(--text-muted); }

  /* Recent Rides */
  .recent-rides {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    overflow: hidden;
  }
  .view-all { font-size: 12px; color: var(--text-tertiary); }
  .view-all:hover { color: var(--color-accent); }
  .rides-table-wrap { margin: 0 -14px -14px; overflow-x: auto; -webkit-overflow-scrolling: touch; scrollbar-width: none; }
  .rides-table-wrap::-webkit-scrollbar { display: none; }
  .rides-table { width: 100%; border-collapse: collapse; font-size: 12px; }
  .rides-table th { text-align: left; padding: 10px 12px; font-size: 10px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; background: var(--bg-elevated); border-top: 1px solid var(--border-subtle); white-space: nowrap; }
  .rides-table td { padding: 10px 12px; border-top: 1px solid var(--border-subtle); vertical-align: middle; }
  .rides-table tbody tr { cursor: pointer; transition: background 0.15s; }
  .rides-table tbody tr:hover { background: var(--bg-hover); }
  .rides-table tbody tr:last-child td { border-bottom: none; }

  .col-date { white-space: nowrap; }
  .date-primary { font-weight: 500; color: var(--text-primary); display: block; }
  .date-secondary { font-size: 10px; color: var(--text-muted); }
  .col-duration { font-weight: 500; color: var(--text-secondary); white-space: nowrap; }
  .col-asymmetry { white-space: nowrap; }
  .asymmetry-value { font-weight: 600; }
  .asymmetry-value.optimal { color: var(--color-optimal-text); }
  .asymmetry-value.attention { color: var(--color-attention-text); }
  .asymmetry-value.problem { color: var(--color-problem-text); }
  .dominance { margin-left: 4px; font-size: 10px; color: var(--text-muted); font-weight: 500; }
  .col-balance { color: var(--text-secondary); font-variant-numeric: tabular-nums; white-space: nowrap; }
  .col-te, .col-ps { color: var(--text-secondary); font-variant-numeric: tabular-nums; white-space: nowrap; }
  .col-zones { width: 80px; min-width: 60px; }
  .zone-bar-mini { height: 6px; display: flex; border-radius: 3px; overflow: hidden; background: var(--bg-elevated); }
  .zone-segment { height: 100%; }
  .zone-segment.optimal { background: var(--color-optimal); }
  .zone-segment.attention { background: var(--color-attention); }
  .zone-segment.problem { background: var(--color-problem); }
  .col-power { color: var(--text-secondary); font-variant-numeric: tabular-nums; white-space: nowrap; }

  /* Recent Rides Compact Table */
  .rides-table-wrap {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    overflow: hidden;
  }
  .rides-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
  }
  .rides-table th {
    text-align: left;
    padding: 10px 12px;
    font-size: 10px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    background: var(--bg-elevated);
    border-bottom: 1px solid var(--border-subtle);
  }
  .rides-table td {
    padding: 10px 12px;
    border-bottom: 1px solid var(--border-subtle);
    vertical-align: middle;
  }
  .rides-table tr {
    cursor: pointer;
    transition: background 0.15s;
  }
  .rides-table tr:hover { background: var(--bg-hover); }
  .rides-table tr:last-child td { border-bottom: none; }

  .col-date { white-space: nowrap; }
  .ride-date-primary { font-weight: 500; color: var(--text-primary); display: block; }
  .ride-date-secondary { font-size: 10px; color: var(--text-muted); }
  .col-duration { color: var(--text-secondary); font-weight: 500; }
  .col-asymmetry { white-space: nowrap; }
  .asymmetry-val { font-weight: 600; }
  .asymmetry-val.optimal { color: var(--color-optimal-text); }
  .asymmetry-val.attention { color: var(--color-attention-text); }
  .asymmetry-val.problem { color: var(--color-problem-text); }
  .asymmetry-side { margin-left: 4px; font-size: 10px; color: var(--text-muted); font-weight: 500; }
  .col-te, .col-ps { color: var(--text-secondary); font-variant-numeric: tabular-nums; }
  .col-optimal { }
  .optimal-val { color: var(--color-optimal-text); font-weight: 600; }
  .col-power { color: var(--text-secondary); font-variant-numeric: tabular-nums; }

  /* Recent Rides Grid (legacy) */
  .rides-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 12px;
  }
  .ride-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    transition: border-color 0.2s;
  }
  .ride-card:hover { border-color: var(--border-hover); }
  .ride-card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }
  .ride-card-date { display: flex; flex-direction: column; gap: 2px; }
  .ride-day { font-size: 14px; font-weight: 500; color: var(--text-primary); }
  .ride-duration { font-size: 12px; color: var(--text-muted); }
  .ride-card-score {
    font-size: 16px; font-weight: 700;
    min-width: 36px; height: 36px;
    display: flex; align-items: center; justify-content: center;
    border-radius: 8px;
  }
  .ride-card-score.score-optimal { background: var(--color-optimal-soft); color: var(--color-optimal-text); }
  .ride-card-score.score-attention { background: var(--color-attention-soft); color: var(--color-attention-text); }
  .ride-card-score.score-problem { background: var(--color-problem-soft); color: var(--color-problem-text); }
  .ride-card-metrics {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
  .ride-metric { min-width: 50px; }
  .metric-val { display: block; font-size: 14px; font-weight: 500; color: var(--text-primary); }
  .metric-lbl { font-size: 10px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; }

  .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 24px; }
  .stat-card { background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 12px; padding: 16px; text-align: center; }
  .stat-value { display: block; font-size: 24px; font-weight: 700; color: var(--text-primary); line-height: 1.2; font-variant-numeric: tabular-nums; }
  .stat-value.score-optimal, .stat-value.balance-optimal { color: var(--color-optimal-text); }
  .stat-value.score-attention, .stat-value.balance-attention { color: var(--color-attention-text); }
  .stat-value.score-problem, .stat-value.balance-problem { color: var(--color-problem-text); }
  .stat-label { display: block; font-size: 12px; color: var(--text-muted); margin-top: 4px; }

  /* Period Selector */
  .period-selector {
    display: flex;
    gap: 8px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    padding: 4px;
  }
  .period-btn {
    padding: 8px 16px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    background: transparent;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .period-btn:hover { color: var(--text-primary); background: var(--bg-hover); }
  .period-btn.active {
    background: var(--color-accent);
    color: var(--color-accent-text);
    font-weight: 600;
  }

  /* Charts Row */
  .charts-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 32px;
  }
  .chart-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
  }
  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;
  }
  .chart-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .chart-subtitle {
    font-size: 12px;
    color: var(--text-muted);
  }

  /* Bar Chart Enhanced (Weekly Activity) */
  .bar-chart-enhanced {
    margin-bottom: 12px;
  }
  .bar-chart-area {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    height: 90px;
    gap: 6px;
    padding: 8px 4px 0;
    background: var(--bg-base);
    border-radius: 8px;
  }
  .bar-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
  }
  .bar-wrapper {
    width: 100%;
    height: 65px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end;
    position: relative;
  }
  .bar-count {
    font-size: 9px;
    font-weight: 600;
    color: var(--text-secondary);
    margin-bottom: 2px;
  }
  .bar {
    width: 85%;
    max-width: 24px;
    background: linear-gradient(to top, var(--color-accent), var(--color-accent-light, var(--color-accent)));
    border-radius: 4px 4px 0 0;
    min-height: 4px;
    transition: height 0.3s ease;
  }
  .bar-label {
    font-size: 10px;
    color: var(--text-muted);
    text-transform: uppercase;
    font-weight: 500;
  }
  /* Legacy bar-chart class */
  .bar-chart {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    height: 80px;
    gap: 4px;
    padding-top: 8px;
  }

  /* Performance Mini Grid */
  .performance-mini-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }
  .perf-mini-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 8px;
    background: var(--bg-base);
    border-radius: 6px;
  }
  .perf-mini-label {
    font-size: 10px;
    color: var(--text-muted);
    flex-shrink: 0;
  }
  .perf-mini-value {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
    margin-left: auto;
  }
  .perf-mini-delta {
    font-size: 10px;
    font-weight: 600;
    min-width: 28px;
    text-align: right;
  }
  .perf-mini-delta.up { color: var(--color-optimal-text); }
  .perf-mini-delta.down { color: var(--color-problem-text); }

  /* Line Chart (Balance Trend) */
  .line-chart {
    height: 80px;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }
  .trend-svg {
    width: 100%;
    height: 60px;
  }
  .trend-labels {
    display: flex;
    justify-content: center;
    margin-top: 8px;
  }
  .chart-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 60px;
    font-size: 13px;
    color: var(--text-muted);
  }

  /* Zone Bars */
  .zone-bars {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding-top: 4px;
  }
  .zone-bar-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .zone-bar-label {
    width: 60px;
    font-size: 12px;
    color: var(--text-secondary);
    flex-shrink: 0;
  }
  .zone-bar-track {
    flex: 1;
    height: 8px;
    background: var(--bg-elevated);
    border-radius: 4px;
    overflow: hidden;
  }
  .zone-bar-fill {
    height: 100%;
    border-radius: 4px;
    transition: width 0.3s ease;
  }
  .zone-bar-fill.optimal { background: var(--color-optimal); }
  .zone-bar-fill.attention { background: var(--color-attention); }
  .zone-bar-fill.problem { background: var(--color-problem); }
  .zone-bar-value {
    width: 36px;
    font-size: 13px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    text-align: right;
    flex-shrink: 0;
  }
  .zone-bar-value.optimal { color: var(--color-optimal-text); }
  .zone-bar-value.attention { color: var(--color-attention-text); }
  .zone-bar-value.problem { color: var(--color-problem-text); }

  /* Analytics Row */
  .analytics-row {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 16px;
    margin-bottom: 32px;
  }
  .chart-card.wide {
    grid-column: span 1;
  }
  .line-chart.wide {
    height: 100px;
  }
  .trend-svg.wide {
    width: 100%;
    height: 80px;
  }

  /* Balance Trend Card */
  .balance-trend-card {
    display: flex;
    flex-direction: column;
  }
  .balance-trend-stats {
    display: flex;
    gap: 24px;
    margin-bottom: 12px;
  }
  .trend-stat {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .trend-stat-label {
    font-size: 11px;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .trend-stat-value {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    font-variant-numeric: tabular-nums;
  }
  .trend-stat-value.optimal { color: var(--color-optimal-text); }
  .trend-stat-value.attention { color: var(--color-attention-text); }
  .trend-stat-value.problem { color: var(--color-problem-text); }

  .balance-trend-chart {
    height: 100px;
  }
  .balance-trend-legend {
    display: flex;
    gap: 16px;
    margin-top: 8px;
    justify-content: center;
  }
  .trend-legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    color: var(--text-muted);
  }
  .trend-legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
  .trend-legend-line {
    width: 12px;
    height: 0;
    border-top: 1px dashed var(--color-optimal);
  }

  /* Progress Card Enhanced */
  .progress-card-enhanced {
    display: flex;
    flex-direction: column;
  }
  .progress-grid {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .progress-section { }
  .progress-section-label {
    display: block;
    font-size: 10px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 8px;
  }
  .progress-section-items {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .progress-section-items.horizontal {
    flex-direction: row;
    gap: 12px;
  }
  .progress-item-enhanced {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 10px;
    background: var(--bg-base);
    border-radius: 8px;
  }
  .progress-item-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 1px;
  }
  .progress-item-label {
    font-size: 11px;
    color: var(--text-muted);
  }
  .progress-item-value {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .progress-item-delta {
    font-size: 13px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
  .progress-item-delta.up { color: var(--color-optimal-text); }
  .progress-item-delta.down { color: var(--color-problem-text); }
  .progress-item-delta.same { color: var(--text-muted); }

  .progress-stat {
    flex: 1;
    text-align: center;
    padding: 8px;
    background: var(--bg-base);
    border-radius: 8px;
  }
  .progress-stat-value {
    display: block;
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .progress-stat-delta {
    display: block;
    font-size: 11px;
    font-weight: 500;
    margin-top: 2px;
  }
  .progress-stat-delta.up { color: var(--color-optimal-text); }
  .progress-stat-delta.down { color: var(--color-problem-text); }
  .progress-stat-delta.same { color: var(--text-muted); }

  .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
  .view-all-link { font-size: 13px; color: var(--text-tertiary); }
  .view-all-link:hover { color: var(--color-accent); }

  .dashboard-metrics-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
  .dashboard-metric-card { background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 12px; padding: 16px; }
  .dashboard-metric-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
  .dashboard-metric-title { font-size: 13px; color: var(--text-secondary); }
  .dashboard-metric-badge { font-size: 10px; font-weight: 600; padding: 3px 6px; border-radius: 4px; background: var(--bg-elevated); color: var(--text-tertiary); }
  .dashboard-metric-badge.optimal { background: var(--color-optimal-soft); color: var(--color-optimal-text); }
  .dashboard-metric-values { display: flex; align-items: center; gap: 12px; }
  .dashboard-metric-side { flex: 1; text-align: center; }
  .side-label { display: block; font-size: 11px; color: var(--text-muted); margin-bottom: 4px; }
  .side-value { font-size: 20px; font-weight: 600; color: var(--text-primary); }
  .dashboard-metric-divider { width: 1px; height: 32px; background: var(--border-subtle); }
  .dashboard-metric-single { text-align: center; }
  .single-value { display: block; font-size: 28px; font-weight: 600; margin-bottom: 4px; }
  .single-label { font-size: 11px; color: var(--text-muted); }

  .rides-list { background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 12px; overflow: hidden; }
  .ride-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid var(--border-subtle); }
  .ride-item:last-child { border-bottom: none; }
  .ride-date { display: flex; flex-direction: column; gap: 2px; }
  .date-day { font-size: 14px; font-weight: 500; color: var(--text-primary); }
  .date-duration { font-size: 12px; color: var(--text-tertiary); }
  .ride-metrics { display: flex; align-items: center; gap: 12px; }
  .ride-balance, .ride-score { font-size: 13px; font-weight: 500; padding: 4px 8px; border-radius: 6px; }
  .ride-balance.balance-optimal { background: var(--color-optimal-soft); color: var(--color-optimal-text); }
  .ride-balance.balance-attention { background: var(--color-attention-soft); color: var(--color-attention-text); }
  .ride-balance.balance-problem { background: var(--color-problem-soft); color: var(--color-problem-text); }
  .ride-score.score-optimal { background: var(--color-optimal-soft); color: var(--color-optimal-text); }
  .ride-score.score-attention { background: var(--color-attention-soft); color: var(--color-attention-text); }
  .ride-score.score-problem { background: var(--color-problem-soft); color: var(--color-problem-text); }

  .empty-state { text-align: center; padding: 80px 24px; }
  .empty-icon { margin-bottom: 24px; color: var(--text-muted); }
  .empty-state h2 { font-size: 24px; font-weight: 600; margin-bottom: 8px; color: var(--text-primary); }
  .empty-state > p { color: var(--text-secondary); margin-bottom: 32px; }
  .setup-card { background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 12px; padding: 20px; text-align: left; max-width: 360px; margin: 0 auto; }
  .setup-card h3 { font-size: 14px; font-weight: 600; margin-bottom: 16px; color: var(--text-primary); }
  .setup-steps { display: flex; flex-direction: column; gap: 12px; }
  .setup-step { display: flex; align-items: center; gap: 12px; }
  .step-number { width: 24px; height: 24px; border-radius: 50%; background: var(--color-accent); color: var(--color-accent-text); font-size: 12px; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
  .step-text { font-size: 13px; color: var(--text-secondary); text-align: left; }

  /* Responsive - Refined for all screens */
  /* ============================================
     RESPONSIVE: 1024px - Small desktops / Large tablets
     ============================================ */
  @media (max-width: 1024px) {
    .metrics-deep { gap: 14px; }
    .metric-deep-card { padding: 20px 24px; }
    .works-grid { grid-template-columns: 1fr; gap: 12px; }
    .requirements-grid { grid-template-columns: 1fr; gap: 12px; }
    .dash-features { grid-template-columns: 1fr 1fr; }
  }

  /* ============================================
     RESPONSIVE: 768px - Tablets
     ============================================ */
  @media (max-width: 768px) {
    /* Base layout */
    .landing { padding: 20px; }
    .landing-container { padding: 48px 0 64px; }

    /* Fixed elements - larger touch targets */
    .site-logo { top: 16px; left: 16px; }
    .theme-toggle {
      top: 16px;
      right: 16px;
      width: 44px;
      height: 44px;
    }

    /* Hero */
    .hero { padding: 36px 0 56px; }
    .hero-eyebrow { font-size: 12px; margin-bottom: 18px; }
    .hero-title { font-size: 40px; letter-spacing: -1.5px; }
    .hero-subtitle { font-size: 16px; max-width: 380px; margin-bottom: 36px; }
    .hero-actions { gap: 12px; }
    .hero-cta, .hero-cta-secondary {
      padding: 14px 28px;
      font-size: 15px;
      min-height: 48px; /* Touch target */
    }
    .hero-note { font-size: 13px; margin-top: 20px; }

    /* Sections */
    .section { margin-bottom: 64px; }
    .section-title { font-size: 26px; letter-spacing: -0.5px; }
    .section-subtitle { font-size: 15px; margin-bottom: 28px; }

    /* Data Fields */
    .datafield-tabs {
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      scrollbar-width: none;
      flex-wrap: nowrap;
      padding: 0 12px;
    }
    .datafield-tabs::-webkit-scrollbar { display: none; }
    .datafield-tab {
      flex-shrink: 0;
      padding: 12px 16px;
      font-size: 13px;
      white-space: nowrap;
    }
    .datafield-preview {
      flex-direction: column;
      gap: 24px;
    }
    .preview-device { margin: 0 auto; }
    .device-frame { border-radius: 28px; }
    .preview-info { text-align: center; padding: 0 16px; }
    .preview-info h4 { font-size: 17px; }
    .preview-info p { font-size: 14px; }

    /* Dashboard - Compact Mobile */
    .dash-header { margin-bottom: 16px; }
    .dash-greeting h1 { font-size: 18px; }

    /* Hero Stats */
    .hero-stats { grid-template-columns: 1fr 1fr; gap: 8px; }
    .hero-stat { padding: 10px; }
    .hero-stat-value { font-size: 22px; }
    .hero-stat.asymmetry { flex-direction: column; align-items: flex-start; gap: 6px; }

    /* Balance Display Mobile */
    .balance-display { gap: 8px; }
    .balance-side { min-width: 28px; }
    .balance-pct { font-size: 15px; }
    .balance-leg { font-size: 9px; }
    .balance-bar { height: 6px; }
    .balance-center-mark { top: -2px; bottom: -2px; }

    /* Zones Mobile */
    .zone-mini-bars { height: 6px; }
    .zone-mini-val { font-size: 11px; }

    /* Summary Mobile */
    .summary-num { font-size: 16px; }
    .summary-unit { font-size: 9px; }

    /* Main Grid */
    .main-grid { grid-template-columns: 1fr; gap: 12px; }
    .grid-card { padding: 12px; }
    .grid-card.wide { grid-column: span 1; }
    .card-header { margin-bottom: 10px; }
    .card-title { font-size: 13px; }

    /* Comparison Grid */
    .comparison-grid { grid-template-columns: repeat(2, 1fr); }
    .comp-value { font-size: 13px; }

    /* Technique Grid */
    .technique-main { font-size: 18px; }
    .technique-side { font-size: 10px; }
    .technique-comparison { grid-template-columns: repeat(2, 1fr); }
    .fatigue-mini { grid-template-columns: repeat(3, 1fr); }

    /* Metrics Compact */
    .metrics-compact { padding: 12px 16px; gap: 8px; }
    .metric-chip { padding: 6px 12px; }
    .metric-chip-val { font-size: 14px; }
    .metric-chip-unit { font-size: 11px; }

    /* Trends Row */
    .trends-row { grid-template-columns: 1fr; gap: 12px; }
    .trend-card { padding: 12px; }
    .trend-chart { height: 50px; margin-bottom: 10px; }
    .trend-stats { gap: 6px; }
    .trend-stat-item { padding: 6px 4px; }
    .trend-stat-value { font-size: 13px; }
    .trend-metric-switcher button { padding: 3px 6px; font-size: 9px; }

    /* Progress Row */
    .progress-row-card { padding: 12px; }
    .progress-items { grid-template-columns: repeat(3, 1fr); gap: 6px; }
    .progress-item { padding: 8px 4px; }
    .progress-item-now { font-size: 15px; }
    .progress-item-change { font-size: 11px; }

    /* Recent Rides */
    .recent-rides { padding: 12px; }
    .rides-table-wrap { margin: 0 -12px -12px; }
    .rides-table { min-width: 450px; font-size: 11px; }
    .rides-table th { padding: 6px 8px; font-size: 9px; }
    .rides-table td { padding: 8px 6px; }
    .rides-table .col-balance,
    .rides-table .col-te,
    .rides-table .col-ps { display: none; }
    .rides-table .col-zones { width: 70px; }
    .zone-bar-mini { min-width: 50px; }

    /* Comparison */
    .comparison-row { grid-template-columns: 1fr; gap: 0; }
    .comparison-item { padding: 14px 16px; }
    .comparison-item.after {
      border-top: 1px solid var(--border-subtle);
      border-left: none;
    }

    /* Metrics */
    .metric-deep-card { padding: 20px; border-radius: 14px; }
    .metric-deep-header { gap: 12px; margin-bottom: 12px; }
    .metric-deep-header h3 { font-size: 17px; }
    .metric-deep-range { font-size: 12px; }
    .metric-deep-desc { font-size: 14px; line-height: 1.55; }
    .metric-zones { gap: 8px; margin-top: 14px; }
    .zone { padding: 8px 12px; font-size: 12px; }

    /* Drills */
    .drills-section { padding: 24px; border-radius: 16px; }
    .drills-tabs { gap: 6px; }
    .drill-tab {
      padding: 10px 18px;
      font-size: 14px;
      min-height: 44px;
    }
    .drill-item { padding: 14px 16px; }
    .drill-name { font-size: 15px; }
    .drill-meta { font-size: 12px; }
    .drills-highlight { padding: 20px; }

    /* Alerts */
    .alerts-section { padding: 24px; border-radius: 16px; }
    .alerts-features { grid-template-columns: 1fr 1fr; gap: 0; }
    .alert-feature { padding: 18px 16px; }
    .alert-feature h4 { font-size: 14px; }
    .alert-feature p { font-size: 13px; }

    /* Dashboard preview */
    .dashboard-preview { flex-direction: column; gap: 24px; }
    .dash-browser { max-width: 100%; }
    .browser-content { padding: 16px; }
    .dash-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
    .dash-stat-card { padding: 14px; }
    .dash-stat-value { font-size: 22px; }
    .dash-stat-label { font-size: 11px; }
    .dash-features { grid-template-columns: 1fr; gap: 10px; }
    .dash-feature { padding: 12px 14px; font-size: 13px; }

    /* Motivation */
    .motivation-content { padding: 28px; }
    .motivation-examples { flex-direction: column; gap: 24px; }
    .example-items { font-size: 13px; }

    /* Works grid */
    .works-card { padding: 20px; border-radius: 14px; }
    .works-card h4 { font-size: 16px; }
    .works-card p { font-size: 13px; }

    /* Requirements */
    .req-card { padding: 20px; border-radius: 14px; }
    .req-card h4 { font-size: 16px; }
    .req-card p { font-size: 13px; }
    .compatible-pedals { padding: 20px; }
    .pedals-list {
      flex-wrap: wrap;
      justify-content: center;
      gap: 6px 0;
    }
    .pedals-list span { font-size: 13px; }

    /* FAQ */
    .faq-item summary {
      padding: 16px 18px;
      font-size: 14px;
      min-height: 48px;
    }
    .faq-item p { padding: 0 18px 16px; font-size: 14px; }

    /* CTA */
    .cta-section { padding: 56px 0 40px; }
    .cta-headline { font-size: 28px; }
    .cta-subtext { font-size: 15px; }
    .cta-btn.large {
      padding: 14px 28px;
      min-height: 48px;
    }

    /* Footer */
    .landing-footer { padding: 48px 20px 32px; }
    .footer-top { flex-direction: column; gap: 32px; align-items: center; }
    .footer-brand { max-width: 100%; text-align: center; display: flex; flex-direction: column; align-items: center; }
    .footer-logo { justify-content: center; }
    .footer-nav { display: flex; justify-content: center; gap: 48px; }
    .footer-col {
      align-items: center;
      text-align: center;
      gap: 10px;
    }
    .footer-col h4 { font-size: 11px; }
    .footer-col a, .footer-link-btn {
      font-size: 14px;
      min-height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .footer-bottom { justify-content: center; gap: 20px; }
  }

  /* ============================================
     RESPONSIVE: 480px - Mobile phones
     ============================================ */
  @media (max-width: 480px) {
    /* Base layout - tighter padding */
    .landing { padding: 16px; }
    .landing-container { padding: 32px 0 48px; }

    /* Fixed elements */
    .site-logo { top: 14px; left: 14px; gap: 6px; }
    .site-logo-dot { width: 7px; height: 7px; }
    .site-logo-text { font-size: 14px; }
    .theme-toggle { top: 14px; right: 14px; }

    /* Hero - compact but readable */
    .hero { padding: 24px 0 40px; }
    .hero-eyebrow {
      font-size: 11px;
      margin-bottom: 16px;
      letter-spacing: 1px;
    }
    .hero-version {
      margin-left: 6px;
      padding: 2px 6px;
      font-size: 10px;
      border-radius: 4px;
    }
    .hero-title {
      font-size: 32px;
      letter-spacing: -1px;
      line-height: 1.15;
    }
    .hero-title-line { display: block; }
    .hero-subtitle {
      font-size: 14px;
      max-width: 300px;
      margin-bottom: 28px;
      line-height: 1.55;
    }
    .hero-actions {
      flex-direction: column;
      gap: 10px;
      width: 100%;
      max-width: 280px;
      margin: 0 auto;
    }
    .hero-cta, .hero-cta-secondary {
      width: 100%;
      padding: 14px 24px;
      font-size: 15px;
      justify-content: center;
    }
    .hero-note { font-size: 12px; margin-top: 18px; }

    /* Sections */
    .section { margin-bottom: 48px; }
    .section-title { font-size: 22px; letter-spacing: -0.3px; }
    .section-subtitle { font-size: 14px; margin-bottom: 24px; }

    /* Data Fields */
    .datafield-tabs {
      padding: 0 10px;
    }
    .datafield-tab {
      padding: 10px 14px;
      font-size: 12px;
    }
    .datafield-preview {
      padding: 20px 16px;
      border-radius: 14px;
      gap: 20px;
    }
    .device-frame {
      border-radius: 24px;
      padding: 8px;
      transform: scale(0.95);
    }
    .preview-info { padding: 0 8px; }
    .preview-info h4 { font-size: 16px; margin-bottom: 6px; }
    .preview-info p { font-size: 13px; line-height: 1.5; }

    /* Dashboard */
    .dashboard { padding: 24px 0 48px; }
    .dash-greeting h1 { font-size: 24px; }
    .period-selector { width: 100%; justify-content: center; }
    .period-btn { flex: 1; text-align: center; }
    .stats-grid { gap: 12px; margin-bottom: 24px; }
    .stat-card { padding: 14px; }
    .stat-icon { width: 32px; height: 32px; }
    .stat-value { font-size: 20px; }
    .stat-label { font-size: 11px; }
    .charts-row { gap: 12px; margin-bottom: 24px; }
    .chart-card { padding: 14px; }
    .chart-title { font-size: 13px; }
    .chart-subtitle { font-size: 11px; }
    .bar-chart { height: 70px; }
    .bar-wrapper { height: 50px; }
    .bar-label { font-size: 9px; }
    .line-chart { height: 70px; }
    .trend-svg { height: 50px; }
    .dashboard-metrics-grid { grid-template-columns: 1fr; gap: 12px; }
    .dashboard-metric-card { padding: 14px; }
    .rides-list { border-radius: 10px; }
    .ride-item { padding: 12px 14px; }
    .date-day { font-size: 13px; }
    .date-duration { font-size: 11px; }
    .ride-balance, .ride-score { font-size: 12px; padding: 3px 6px; }
    .analytics-row { gap: 12px; margin-bottom: 24px; }
    .zone-bar-label { width: 50px; font-size: 11px; }
    .zone-bar-value { width: 32px; font-size: 12px; }
    .line-chart.wide { height: 80px; }
    .trend-svg.wide { height: 60px; }
    .progress-items { gap: 8px; }
    .progress-metric { font-size: 12px; }
    .progress-change { font-size: 14px; }

    /* Recent Rides - extra compact */
    .rides-table { min-width: 320px; font-size: 10px; }
    .rides-table th { padding: 5px 4px; font-size: 8px; }
    .rides-table td { padding: 6px 4px; }
    .rides-table .col-power { display: none; }
    .rides-table .col-zones { width: 50px; }
    .zone-bar-mini { min-width: 40px; height: 5px; }
    .date-secondary { display: none; }

    /* Comparison - stacked with labels */
    .comparison-table { border-radius: 14px; }
    .comparison-header { display: none; }
    .comparison-row { gap: 0; }
    .comparison-item {
      padding: 14px 16px;
      font-size: 13px;
      flex-direction: column;
      align-items: flex-start;
      gap: 6px;
    }
    .comparison-item svg { flex-shrink: 0; }
    .comparison-item.before::before {
      content: 'Without KPedal';
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--text-muted);
      display: block;
      margin-bottom: 2px;
    }
    .comparison-item.after::before {
      content: 'With KPedal';
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--color-optimal-text);
      display: block;
      margin-bottom: 2px;
    }

    /* Metrics */
    .metric-deep-card {
      padding: 18px 16px;
      border-radius: 12px;
    }
    .metric-deep-header { gap: 10px; margin-bottom: 10px; }
    .metric-deep-header h3 { font-size: 16px; }
    .metric-deep-range { font-size: 11px; }
    .metric-deep-desc { font-size: 13px; line-height: 1.5; }
    .metric-zones {
      flex-direction: column;
      gap: 6px;
      margin-top: 12px;
    }
    .zone {
      padding: 8px 12px;
      font-size: 12px;
      border-radius: 6px;
      justify-content: space-between;
    }
    .zone span { font-weight: 600; }

    /* Drills */
    .drills-section {
      padding: 18px;
      border-radius: 14px;
    }
    .drills-tabs {
      width: 100%;
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 6px;
    }
    .drill-tab {
      padding: 10px 8px;
      font-size: 13px;
      justify-content: center;
    }
    .drills-list { gap: 6px; }
    .drill-item {
      padding: 14px;
      border-radius: 10px;
    }
    .drill-header { gap: 10px; }
    .drill-name { font-size: 14px; }
    .drill-meta { font-size: 11px; gap: 6px; }
    .drill-details { padding-top: 10px; }
    .drill-details p { font-size: 13px; }
    .drill-target { font-size: 11px; padding: 4px 8px; }
    .drills-highlight {
      padding: 16px;
      border-radius: 10px;
      margin-top: 14px;
    }
    .drills-highlight h4 { font-size: 15px; }
    .drills-highlight p { font-size: 12px; }

    /* Metrics Compact - Phone */
    .metrics-compact { padding: 10px 12px; gap: 8px; }
    .metric-chip { padding: 6px 10px; }
    .metric-chip-val { font-size: 13px; }
    .metric-chip-unit { font-size: 10px; }

    /* Alerts */
    .alerts-section {
      padding: 18px;
      border-radius: 14px;
    }
    .alert-banner-demo {
      padding: 14px 16px;
      border-radius: 10px;
    }
    .alert-title { font-size: 13px; }
    .alert-detail { font-size: 12px; }
    .alerts-features {
      grid-template-columns: 1fr;
      gap: 0;
      margin-top: 16px;
    }
    .alert-feature {
      padding: 14px 16px;
      border-right: none !important;
      border-bottom: 1px solid var(--border-subtle);
    }
    .alert-feature:last-child { border-bottom: none; }
    .alert-feature h4 { font-size: 14px; }
    .alert-feature p { font-size: 12px; }

    /* Dashboard preview */
    .dash-browser { border-radius: 12px; }
    .browser-bar { padding: 10px 14px; }
    .browser-url { font-size: 11px; }
    .browser-content { padding: 14px; }
    .dash-header { margin-bottom: 14px; }
    .dash-greeting { font-size: 13px; }
    .dash-period { font-size: 10px; padding: 3px 8px; }
    .dash-grid {
      grid-template-columns: repeat(2, 1fr);
      gap: 8px;
    }
    .dash-stat-card {
      padding: 12px 10px;
      border-radius: 8px;
    }
    .dash-stat-value { font-size: 20px; }
    .dash-stat-label { font-size: 10px; }
    .dash-chart {
      padding: 12px;
      border-radius: 8px;
      margin-top: 10px;
    }
    .chart-label { font-size: 10px; }
    .dash-features { gap: 8px; }
    .dash-feature {
      padding: 12px;
      font-size: 12px;
      border-radius: 8px;
    }
    .dash-feature svg { width: 16px; height: 16px; }

    /* Motivation */
    .motivation-content {
      padding: 20px 16px;
      border-radius: 14px;
    }
    .motivation-stats {
      flex-direction: column;
      gap: 16px;
      padding-bottom: 16px;
    }
    .motivation-divider { width: 48px; height: 1px; }
    .stat-number { font-size: 32px; }
    .stat-desc { font-size: 13px; }
    .motivation-examples { gap: 20px; padding-top: 16px; }
    .example-label { font-size: 11px; }
    .example-items { font-size: 12px; gap: 6px; }

    /* Works grid */
    .works-card {
      padding: 18px 16px;
      border-radius: 12px;
    }
    .works-icon svg { width: 24px; height: 24px; }
    .works-card h4 { font-size: 15px; margin: 10px 0 6px; }
    .works-card p { font-size: 12px; line-height: 1.5; }

    /* Requirements */
    .req-card {
      padding: 18px 16px;
      border-radius: 12px;
    }
    .req-icon svg { width: 20px; height: 20px; }
    .req-card h4 { font-size: 15px; }
    .req-card p { font-size: 12px; line-height: 1.5; }
    .compatible-pedals {
      padding: 16px;
      margin-top: 14px;
      border-radius: 10px;
    }
    .pedals-label { font-size: 10px; margin-bottom: 10px; }
    .pedals-list { gap: 4px 0; }
    .pedals-list span { font-size: 12px; }
    .pedals-sep { margin: 0 6px; }
    .pedals-note { font-size: 11px; margin-top: 10px; }

    /* FAQ */
    .faq-section { margin-bottom: 40px; }
    .faq-list { gap: 8px; }
    .faq-item { border-radius: 10px; }
    .faq-item summary {
      padding: 14px 16px;
      font-size: 13px;
      min-height: 48px;
    }
    .faq-item summary::after {
      width: 18px;
      height: 18px;
      flex-shrink: 0;
    }
    .faq-item p {
      padding: 0 16px 14px;
      font-size: 13px;
      line-height: 1.55;
    }

    /* CTA */
    .cta-section { padding: 40px 0 32px; }
    .cta-headline { font-size: 22px; letter-spacing: -0.3px; }
    .cta-subtext {
      font-size: 13px;
      margin-bottom: 24px;
      max-width: 280px;
    }
    .cta-btn {
      width: 100%;
      max-width: 260px;
    }
    .cta-btn.large {
      padding: 14px 24px;
      font-size: 15px;
    }

    /* Footer - simplified single column */
    .landing-footer { padding: 32px 20px 24px; }
    .footer-top { gap: 28px; align-items: center; }
    .footer-brand { text-align: center; display: flex; flex-direction: column; align-items: center; }
    .footer-logo { justify-content: center; }
    .footer-brand-name { font-size: 17px; }
    .footer-tagline { font-size: 13px; }
    .footer-nav {
      display: flex;
      flex-direction: row;
      flex-wrap: wrap;
      justify-content: center;
      gap: 24px 32px;
    }
    .footer-col {
      display: none; /* Hide column headers on mobile */
    }
    .footer-col h4 { display: none; }
    .footer-col a, .footer-link-btn {
      display: inline-flex;
      font-size: 14px;
      padding: 8px 4px;
      min-height: 44px;
      align-items: center;
      justify-content: center;
    }
    /* Show links directly without columns */
    .footer-nav {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 8px 24px;
    }
    .footer-col {
      display: contents;
    }
    .footer-bottom {
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding-top: 24px;
      font-size: 13px;
    }
    .footer-bottom span { text-align: center; }
    .footer-github {
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .footer-github svg { width: 20px; height: 20px; }
  }

  /* ============================================
     RESPONSIVE: 360px - Small phones
     ============================================ */
  @media (max-width: 360px) {
    .landing { padding: 14px; }

    .site-logo { top: 12px; left: 12px; }
    .site-logo-text { font-size: 13px; }
    .theme-toggle { top: 12px; right: 12px; width: 40px; height: 40px; }

    .hero { padding: 20px 0 32px; }
    .hero-eyebrow { font-size: 10px; }
    .hero-title { font-size: 28px; }
    .hero-subtitle { font-size: 13px; max-width: 260px; }
    .hero-actions { max-width: 240px; }
    .hero-cta, .hero-cta-secondary {
      padding: 12px 20px;
      font-size: 14px;
    }

    .section-title { font-size: 20px; }
    .section-subtitle { font-size: 13px; }

    .datafield-tabs { padding: 0 8px; }
    .datafield-tab { padding: 8px 12px; font-size: 11px; }
    .datafield-preview { padding: 16px 12px; }
    .device-frame { transform: scale(0.9); }

    .metric-deep-card { padding: 16px 14px; }
    .metric-deep-header h3 { font-size: 15px; }
    .metric-deep-desc { font-size: 12px; }

    .drills-section { padding: 16px 14px; }
    .drill-tab { padding: 8px 6px; font-size: 12px; }
    .drill-name { font-size: 13px; }

    .alerts-section { padding: 16px 14px; }

    .dash-stat-value { font-size: 18px; }
    .dash-stat-label { font-size: 9px; }

    .stat-number { font-size: 28px; }

    .works-card { padding: 16px 14px; }
    .req-card { padding: 16px 14px; }

    .pedals-list span { font-size: 11px; }

    .faq-item summary { font-size: 12px; padding: 12px 14px; }
    .faq-item p { font-size: 12px; padding: 0 14px 12px; }

    .cta-headline { font-size: 20px; }
    .cta-subtext { font-size: 12px; }
    .cta-btn.large { padding: 12px 20px; font-size: 14px; }

    /* Footer */
    .landing-footer { padding: 28px 16px 20px; }
    .footer-nav { gap: 6px 20px; }
    .footer-col a, .footer-link-btn { font-size: 13px; }
    .footer-bottom { font-size: 12px; gap: 12px; }
  }

  /* ============================================
     Touch device optimizations
     ============================================ */
  @media (hover: none) and (pointer: coarse) {
    /* Disable hover effects on touch */
    .hero-cta:hover,
    .hero-cta-secondary:hover,
    .datafield-tab:hover,
    .drill-tab:hover,
    .drill-item:hover,
    .metric-deep-card:hover,
    .works-card:hover,
    .req-card:hover,
    .footer-col a:hover,
    .footer-link-btn:hover {
      transform: none;
    }

    /* Active states for touch feedback */
    .hero-cta:active { opacity: 0.8; }
    .hero-cta-secondary:active { background: var(--bg-active); }
    .datafield-tab:active,
    .drill-tab:active { background: var(--bg-active); }
    .drill-item:active { background: var(--bg-hover); }
    .faq-item summary:active { background: var(--bg-hover); }
  }

  /* ============================================
     Landscape phone orientation
     ============================================ */
  @media (max-width: 768px) and (orientation: landscape) {
    .hero { padding: 24px 0 40px; }
    .hero-title { font-size: 32px; }
    .hero-actions { flex-direction: row; max-width: none; }
    .hero-cta, .hero-cta-secondary { width: auto; }

    .device-frame { transform: scale(0.8); }

    .motivation-stats { flex-direction: row; gap: 32px; }
    .motivation-divider { width: 1px; height: 40px; }
  }

  /* ============================================
     Reduce motion for accessibility
     ============================================ */
  @media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
      animation-duration: 0.01ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: 0.01ms !important;
    }
  }
</style>
