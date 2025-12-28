<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { browser } from '$app/environment';
  import { isAuthenticated, user, authFetch, isDemo } from '$lib/auth';
  import { startDashboardTour, isTourCompleted, resetTour } from '$lib/tour';
  import { theme, resolvedTheme } from '$lib/theme';
  import { API_URL } from '$lib/config';
  import { getDemoDashboard } from '$lib/demoData';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import { t, locale, locales, localeNames, setLocale, type Locale } from '$lib/i18n';

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

  // Use $page.url.hostname - available immediately without waiting for onMount
  // This eliminates the "pageReady" delay for client-side navigation
  $: isLandingDomain = browser ? window.location.hostname === 'kpedal.com' : $page.url.hostname === 'kpedal.com';

  // Show landing ONLY on kpedal.com (landing domain)
  // On app.kpedal.com, guests are redirected to /login, authenticated users see dashboard
  $: showLanding = isLandingDomain;

  // State
  let stats: Stats | null = null;
  let recentRides: Ride[] = [];
  let weeklyRides: Ride[] = [];
  let weeklyComparison: WeeklyComparison | null = null;
  let fatigueData: FatigueAnalysis | null = null;
  let trendData: TrendData[] = [];
  let loading = true;
  let error = false;
  let selectedPeriod: '7' | '14' | '30' | '60' = '7';
  let activeTrendMetric: 'asymmetry' | 'te' | 'ps' = 'asymmetry';

  // Chart hover states
  let hoveredBalancePoint: BalancePoint | null = null;
  let hoveredTechniquePoint: TechniquePoint | null = null;
  let tooltipX = 0;
  let tooltipY = 0;

  // Chart dimensions - width is dynamic, height is fixed
  let balanceChartWidth = 300;
  let techniqueChartWidth = 300;
  const CHART_HEIGHT = 70;
  const CHART_PADDING = 6;

  function handleBalancePointHover(point: BalancePoint | null, event?: MouseEvent) {
    hoveredBalancePoint = point;
    if (point && event) {
      const rect = (event.currentTarget as SVGElement).closest('.trend-chart')?.getBoundingClientRect();
      if (rect) {
        tooltipX = event.clientX - rect.left;
        tooltipY = event.clientY - rect.top - 40;
      }
    }
  }

  function handleTechniquePointHover(point: TechniquePoint | null, event?: MouseEvent) {
    hoveredTechniquePoint = point;
    if (point && event) {
      const rect = (event.currentTarget as SVGElement).closest('.trend-chart')?.getBoundingClientRect();
      if (rect) {
        tooltipX = event.clientX - rect.left;
        tooltipY = event.clientY - rect.top - 40;
      }
    }
  }

  function navigateToRide(rideId: number | string) {
    goto(`/rides/${rideId}`);
  }

  // Fatigue analysis data structure
  interface FatigueAnalysis {
    hasData: boolean;
    firstThird: { balance: number; te: number; ps: number; power: number };
    lastThird: { balance: number; te: number; ps: number; power: number };
    degradation: { balance: number; te: number; ps: number };
  }

  // Interactive state for landing
  let activeDataField = 0;
  let activeDrillCategory: 'focus' | 'challenge' | 'workout' = 'focus';
  let expandedDrill: string | null = null;

  $: dataFields = [
    { id: 'quick', name: $t('landing.dataFields.quickGlance.name'), desc: $t('landing.dataFields.quickGlance.desc') },
    { id: 'balance', name: $t('landing.dataFields.powerBalance.name'), desc: $t('landing.dataFields.powerBalance.desc') },
    { id: 'efficiency', name: $t('landing.dataFields.efficiency.name'), desc: $t('landing.dataFields.efficiency.desc') },
    { id: 'full', name: $t('landing.dataFields.fullOverview.name'), desc: $t('landing.dataFields.fullOverview.desc') },
    { id: 'trend', name: $t('landing.dataFields.balanceTrend.name'), desc: $t('landing.dataFields.balanceTrend.desc') }
  ];

  $: drills = {
    focus: [
      { id: 'left', name: $t('landing.drills.items.leftFocus.name'), duration: $t('landing.drills.items.leftFocus.duration'), level: $t('landing.drills.levels.beginner'), target: $t('landing.drills.items.leftFocus.target'), desc: $t('landing.drills.items.leftFocus.desc') },
      { id: 'right', name: $t('landing.drills.items.rightFocus.name'), duration: $t('landing.drills.items.rightFocus.duration'), level: $t('landing.drills.levels.beginner'), target: $t('landing.drills.items.rightFocus.target'), desc: $t('landing.drills.items.rightFocus.desc') },
      { id: 'smooth', name: $t('landing.drills.items.smoothCircles.name'), duration: $t('landing.drills.items.smoothCircles.duration'), level: $t('landing.drills.levels.intermediate'), target: $t('landing.drills.items.smoothCircles.target'), desc: $t('landing.drills.items.smoothCircles.desc') },
      { id: 'power', name: $t('landing.drills.items.powerTransfer.name'), duration: $t('landing.drills.items.powerTransfer.duration'), level: $t('landing.drills.levels.intermediate'), target: $t('landing.drills.items.powerTransfer.target'), desc: $t('landing.drills.items.powerTransfer.desc') }
    ],
    challenge: [
      { id: 'balance-c', name: $t('landing.drills.items.balanceChallenge.name'), duration: $t('landing.drills.items.balanceChallenge.duration'), level: $t('landing.drills.levels.intermediate'), target: $t('landing.drills.items.balanceChallenge.target'), desc: $t('landing.drills.items.balanceChallenge.desc') },
      { id: 'smooth-t', name: $t('landing.drills.items.smoothnessTarget.name'), duration: $t('landing.drills.items.smoothnessTarget.duration'), level: $t('landing.drills.levels.advanced'), target: $t('landing.drills.items.smoothnessTarget.target'), desc: $t('landing.drills.items.smoothnessTarget.desc') },
      { id: 'cadence', name: $t('landing.drills.items.highCadenceSmooth.name'), duration: $t('landing.drills.items.highCadenceSmooth.duration'), level: $t('landing.drills.levels.advanced'), target: $t('landing.drills.items.highCadenceSmooth.target'), desc: $t('landing.drills.items.highCadenceSmooth.desc') }
    ],
    workout: [
      { id: 'recovery', name: $t('landing.drills.items.balanceRecovery.name'), duration: $t('landing.drills.items.balanceRecovery.duration'), level: $t('landing.drills.levels.beginner'), target: $t('landing.drills.items.balanceRecovery.target'), desc: $t('landing.drills.items.balanceRecovery.desc') },
      { id: 'builder', name: $t('landing.drills.items.efficiencyBuilder.name'), duration: $t('landing.drills.items.efficiencyBuilder.duration'), level: $t('landing.drills.levels.intermediate'), target: $t('landing.drills.items.efficiencyBuilder.target'), desc: $t('landing.drills.items.efficiencyBuilder.desc') },
      { id: 'mastery', name: $t('landing.drills.items.pedalingMastery.name'), duration: $t('landing.drills.items.pedalingMastery.duration'), level: $t('landing.drills.levels.advanced'), target: $t('landing.drills.items.pedalingMastery.target'), desc: $t('landing.drills.items.pedalingMastery.desc') }
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
      // Demo mode: use static data (0ms, no API call)
      if ($isDemo) {
        const data = getDemoDashboard();
        stats = data.stats;
        recentRides = data.recentRides;
        weeklyRides = recentRides;
        weeklyComparison = data.weeklyComparison;
        trendData = data.trends;
        if (data.lastRideSnapshots?.length >= 6) {
          fatigueData = calculateFatigueAnalysis(data.lastRideSnapshots);
        }
        loading = false;
        return;
      }

      // Regular users: API call
      const res = await authFetch('/rides/dashboard?include=snapshots');

      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          stats = data.data.stats;
          recentRides = data.data.recentRides || [];
          weeklyRides = recentRides;
          weeklyComparison = data.data.weeklyComparison;
          trendData = data.data.trends || [];

          if (data.data.lastRideSnapshots?.length >= 6) {
            fatigueData = calculateFatigueAnalysis(data.data.lastRideSnapshots);
          }
        }
      } else {
        error = true;
      }
    } catch (err) {
      error = true;
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

  onMount(() => {
    // Redirect guests on app.kpedal.com to login
    if (!$isAuthenticated && !isLandingDomain) {
      goto('/login');
      return;
    }

    // On landing domain, start the data field rotation
    if (isLandingDomain) {
      loading = false;
      const interval = setInterval(() => {
        activeDataField = (activeDataField + 1) % dataFields.length;
      }, 4000);
      return () => clearInterval(interval);
    }

    // On app.kpedal.com with authenticated user - load dashboard data
    loadDashboardData().then(() => {
      if ($isDemo && !isTourCompleted()) {
        setTimeout(() => startDashboardTour(), 800);
      }
    });
  });

  function handleLogin() {
    window.location.href = `${API_URL}/auth/login`;
  }

  function formatDuration(ms: number): string {
    const hours = Math.floor(ms / 3600000);
    const minutes = Math.floor((ms % 3600000) / 60000);
    return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
  }

  function getLocaleString(currentLocale: string | null | undefined): string {
    const localeMap: Record<string, string> = { en: 'en-US', es: 'es-ES' };
    return currentLocale ? localeMap[currentLocale] || currentLocale : 'en-US';
  }

  function formatDate(timestamp: number, currentLocale?: string | null): string {
    return new Date(timestamp).toLocaleDateString(getLocaleString(currentLocale), { month: 'short', day: 'numeric' });
  }

  function formatRelativeTime(timestamp: number, translations: { today: string; yesterday: string; daysAgo: string }, currentLocale?: string | null): string {
    const now = Date.now();
    const diff = now - timestamp;
    const days = Math.floor(diff / (24 * 60 * 60 * 1000));
    if (days === 0) return translations.today;
    if (days === 1) return translations.yesterday;
    if (days < 7) return translations.daysAgo.replace('{days}', String(days));
    return formatDate(timestamp, currentLocale);
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

  function getWeekDays(currentLocale: string | null | undefined): string[] {
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

  interface BalancePoint {
    x: number;
    y: number;
    balance: number;
    date: string;
    rideId: number;
    status: 'optimal' | 'attention' | 'problem';
  }

  interface BalanceTrendResult {
    path: string;
    areaPath: string;
    movingAvgPath: string;
    avgLine: number;
    minBalance: number;
    maxBalance: number;
    points: BalancePoint[];
    optimalZone: { y1: number; y2: number };
  }

  function getBalanceTrendData(rides: Ride[], maxPoints: number = 20, chartWidth: number = 300, currentLocale?: string | null): BalanceTrendResult {
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

  // Technique Trends chart helper
  interface TechniquePoint {
    x: number;
    y: number;
    value: number;
    date: string;
    status: 'optimal' | 'attention' | 'problem';
  }

  interface TechniqueTrendResult {
    path: string;
    areaPath: string;
    movingAvgPath: string;
    min: number;
    max: number;
    points: TechniquePoint[];
    optimalZone: { y1: number; y2: number } | null;
  }

  function getTechniqueTrendData(trends: TrendData[], metric: 'asymmetry' | 'te' | 'ps', chartWidth: number = 300, currentLocale?: string | null): TechniqueTrendResult {
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

  // Insights generation — coach-like feedback
  interface Insight {
    type: 'win' | 'action' | 'tip';
    icon: string;
    text: string;
    link?: { label: string; href: string };
  }

  interface InsightTranslations {
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

  function generateInsights(
    stats: typeof periodStats,
    prevStats: typeof prevPeriodStats,
    weekly: typeof weeklyComparison,
    fatigue: typeof fatigueData,
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
  $: balanceTrendMaxPoints = selectedPeriod === '60' ? 60 : selectedPeriod === '30' ? 30 : selectedPeriod === '14' ? 14 : 7;
  $: balanceTrend = getBalanceTrendData(filteredRides, balanceTrendMaxPoints, balanceChartWidth, $locale);

  // Calculate rides count for each period (for disabling period buttons)
  $: ridesIn7Days = getFilteredRides(weeklyRides, 7).length;
  $: ridesIn14Days = getFilteredRides(weeklyRides, 14).length;
  $: ridesIn30Days = getFilteredRides(weeklyRides, 30).length;
  $: ridesIn60Days = getFilteredRides(weeklyRides, 60).length;
  $: weekDays = getWeekDays($locale);
  $: ridesPerDay = getRidesPerDay(weeklyRides);
  $: maxRidesPerDay = Math.max(...ridesPerDay, 1);
  $: techniqueTrend = getTechniqueTrendData(trendData, activeTrendMetric, techniqueChartWidth, $locale);
  $: insightTranslations = {
    fatigueAction: $t('dashboard.insights.fatigueAction'),
    fatigueLink: $t('dashboard.insights.fatigueLink'),
    balanceAction: $t('dashboard.insights.balanceAction'),
    balanceLink: $t('dashboard.insights.balanceLink'),
    leftLeg: $t('dashboard.insights.leftLeg'),
    rightLeg: $t('dashboard.insights.rightLeg'),
    leftDominant: $t('dashboard.insights.leftDominant'),
    rightDominant: $t('dashboard.insights.rightDominant'),
    lowTeAction: $t('dashboard.insights.lowTeAction'),
    lowTeLink: $t('dashboard.insights.lowTeLink'),
    lowPsAction: $t('dashboard.insights.lowPsAction'),
    lowPsLink: $t('dashboard.insights.lowPsLink'),
    greatBalance: $t('dashboard.insights.greatBalance'),
    teOptimal: $t('dashboard.insights.teOptimal'),
    excellentScore: $t('dashboard.insights.excellentScore'),
    goodConsistency: $t('dashboard.insights.goodConsistency'),
    improvement: $t('dashboard.insights.improvement'),
    encourageRides: $t('dashboard.insights.encourageRides'),
    ride: $t('dashboard.insights.ride'),
    ridesPl: $t('dashboard.insights.ridesPl'),
    nearOptimalTe: $t('dashboard.insights.nearOptimalTe')
  } as InsightTranslations;
  $: insights = generateInsights(periodStats, prevPeriodStats, weeklyComparison, fatigueData, insightTranslations);
</script>

<svelte:head>
  <title>{$isAuthenticated ? 'Dashboard | KPedal' : 'KPedal — Real-Time Pedaling Analytics for Hammerhead Karoo'}</title>
  <meta name="description" content="Free Karoo extension for cyclists. See power balance, torque effectiveness, pedal smoothness in real-time. 10 guided drills, automatic cloud sync, research-backed thresholds. Works with Garmin Rally, Favero Assioma, SRM pedals.">

  <!-- Keywords and additional meta -->
  <meta name="keywords" content="KPedal, Karoo extension, pedaling efficiency, power balance, torque effectiveness, pedal smoothness, cycling analytics, Hammerhead Karoo, Karoo 2, Karoo 3, cycling drills, power meter pedals, Garmin Rally, Favero Assioma, ANT+ Cycling Dynamics">
  <meta name="application-name" content="KPedal">

  <!-- Canonical URL -->
  <link rel="canonical" href="https://kpedal.com/">

  <!-- Open Graph / Facebook -->
  <meta property="og:type" content="website">
  <meta property="og:url" content="https://kpedal.com/">
  <meta property="og:title" content="KPedal — Real-Time Pedaling Analytics for Hammerhead Karoo">
  <meta property="og:description" content="Free Karoo extension. See power balance, torque effectiveness, pedal smoothness in real-time. 10 guided drills with scoring. Cloud sync to web dashboard.">
  <meta property="og:site_name" content="KPedal">
  <meta property="og:locale" content="en_US">

  <!-- Twitter -->
  <meta name="twitter:card" content="summary">
  <meta name="twitter:url" content="https://kpedal.com/">
  <meta name="twitter:title" content="KPedal — Real-Time Pedaling Analytics for Hammerhead Karoo">
  <meta name="twitter:description" content="Free Karoo extension. See power balance, torque effectiveness, pedal smoothness in real-time. 10 guided drills with scoring.">

  <!-- JSON-LD Structured Data -->
  {@html `<script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    "name": "KPedal",
    "operatingSystem": "Android (Hammerhead Karoo)",
    "applicationCategory": "SportsApplication",
    "applicationSubCategory": "Cycling Analytics",
    "offers": {
      "@type": "Offer",
      "price": "0",
      "priceCurrency": "USD"
    },
    "description": "Real-time pedaling efficiency analytics for Hammerhead Karoo bike computers. Displays power balance, torque effectiveness, and pedal smoothness with research-backed thresholds and guided training drills.",
    "url": "https://kpedal.com",
    "downloadUrl": "https://github.com/yrkan/kpedal/releases",
    "softwareVersion": "1.3.0",
    "featureList": [
      "Real-time power balance monitoring",
      "Torque effectiveness analysis",
      "Pedal smoothness tracking",
      "10 guided training drills",
      "Customizable alerts with vibration",
      "Cloud sync to web dashboard",
      "Background data collection",
      "Multi-device support"
    ],
    "screenshot": "https://kpedal.com/screenshot.png",
    "author": {
      "@type": "Organization",
      "name": "KPedal",
      "url": "https://kpedal.com"
    },
    "aggregateRating": {
      "@type": "AggregateRating",
      "ratingValue": "5",
      "ratingCount": "1",
      "bestRating": "5",
      "worstRating": "1"
    }
  }
  </script>`}

  <!-- FAQ Schema -->
  {@html `<script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    "mainEntity": [
      {
        "@type": "Question",
        "name": "Does KPedal work offline?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "Yes! All core features work without internet. Cloud sync is optional — your data is always stored locally on the Karoo first."
        }
      },
      {
        "@type": "Question",
        "name": "Will KPedal drain my Karoo battery?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "Minimal impact. KPedal only runs during active rides and uses efficient data collection. Background mode uses approximately 1-2% extra battery per hour."
        }
      },
      {
        "@type": "Question",
        "name": "What power pedals are compatible with KPedal?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "Full support (Balance + TE + PS): Garmin Rally RS/RK/XC, Garmin Vector 3, Favero Assioma DUO/DUO-Shi/Pro RS/Pro MX, SRM X-Power, Rotor 2INpower, Look Keo Blade Power, IQ2 Power Pedals. Balance only: Wahoo POWRLINK Zero, Power2Max, Quarq DZero, SRAM AXS PM, Stages LR, 4iiii Precision Dual, Sigeyi AXO, FSA Powerbox. Not compatible: single-sided power meters, trainer power, Garmin Vector 1/2."
        }
      },
      {
        "@type": "Question",
        "name": "Can I use KPedal with Garmin or Wahoo computers?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "No. KPedal is built specifically for Hammerhead Karoo using their SDK. It uses native Karoo features not available on other platforms."
        }
      },
      {
        "@type": "Question",
        "name": "What is the optimal torque effectiveness range?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "70-80% is optimal based on Wattbike research. Higher isn't better — above 80% can actually reduce total power output. Below 60% indicates significant technique issues."
        }
      },
      {
        "@type": "Question",
        "name": "How do I install KPedal on Karoo?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "Download the APK from GitHub Releases. Transfer to Karoo via USB or use Karoo's built-in file browser with a direct download link. Open the APK to install."
        }
      }
    ]
  }
  </script>`}

  <!-- Organization Schema -->
  {@html `<script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "Organization",
    "name": "KPedal",
    "url": "https://kpedal.com",
    "logo": "https://kpedal.com/favicon.svg",
    "sameAs": [
      "https://github.com/yrkan/kpedal"
    ],
    "contactPoint": {
      "@type": "ContactPoint",
      "contactType": "technical support",
      "url": "https://github.com/yrkan/kpedal/issues"
    }
  }
  </script>`}

  <!-- WebSite Schema for search box -->
  {@html `<script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "WebSite",
    "name": "KPedal",
    "url": "https://kpedal.com",
    "description": "Real-time pedaling efficiency analytics for Hammerhead Karoo",
    "publisher": {
      "@type": "Organization",
      "name": "KPedal"
    }
  }
  </script>`}
</svelte:head>

{#if showLanding}
  <div class="landing" role="main" itemscope itemtype="https://schema.org/WebPage">
    <!-- Header with Logo and Theme Toggle -->
    <header class="landing-header">
      <a href="/" class="site-logo" aria-label={$t('aria.home')}>
        <span class="site-logo-dot" aria-hidden="true"></span>
        <span class="site-logo-text">KPedal</span>
      </a>

      <div class="header-actions">
        <select
          class="lang-select"
          value={$locale}
          on:change={(e) => setLocale(e.currentTarget.value as Locale)}
          aria-label={$t('aria.languageSelector')}
        >
          {#each locales as loc}
            <option value={loc}>{localeNames[loc]}</option>
          {/each}
        </select>

        <button class="theme-toggle" on:click={() => theme.toggle()} aria-label={$t('common.toggleTheme')}>
          {#if $resolvedTheme === 'light'}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="5"/>
              <line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
              <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
              <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
              <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
            </svg>
          {:else}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          {/if}
          {#if $theme === 'auto'}
            <span class="auto-badge">A</span>
          {/if}
        </button>
      </div>
    </header>

    <article class="landing-container" itemscope itemtype="https://schema.org/SoftwareApplication">
      <meta itemprop="name" content="KPedal" />
      <meta itemprop="operatingSystem" content="Android (Hammerhead Karoo)" />
      <meta itemprop="applicationCategory" content="SportsApplication" />
      <link itemprop="downloadUrl" href="https://github.com/yrkan/kpedal/releases" />

      <!-- Hero -->
      <section class="hero" aria-labelledby="hero-title">
        <p class="hero-eyebrow">
          <span itemprop="applicationSubCategory">{$t('landing.hero.eyebrow')}</span>
          <a href="https://github.com/yrkan/KPedal/releases/latest" target="_blank" rel="noopener" class="hero-version" itemprop="softwareVersion">{$t('landing.hero.version')}</a>
        </p>
        <h1 id="hero-title" class="hero-title" itemprop="headline">
          <span class="hero-title-line">{$t('landing.hero.titleLine1')}</span>
          <span class="hero-title-line">{$t('landing.hero.titleLine2')}</span>
        </h1>
        <p class="hero-subtitle" itemprop="description">
          {$t('landing.hero.subtitle')}
        </p>

        <div class="hero-actions" role="group" aria-label={$t('aria.downloadOptions')}>
          <a href="https://github.com/yrkan/kpedal/releases" class="hero-cta" target="_blank" rel="noopener noreferrer" itemprop="downloadUrl" aria-label={$t('aria.downloadKPedal')}>
            {$t('landing.hero.downloadFree')}
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </a>
          <a href="https://app.kpedal.com/login" class="hero-cta-secondary" aria-label={$t('aria.tryDemo')}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="12" cy="12" r="10"/>
              <polygon points="10 8 16 12 10 16 10 8" fill="currentColor" stroke="none"/>
            </svg>
            {$t('landing.hero.tryDemo')}
          </a>
        </div>

        <p class="hero-note">
          <span>{$t('landing.hero.free')}</span> & <a href="https://github.com/yrkan/kpedal" target="_blank" rel="noopener noreferrer">{$t('landing.hero.openSource')}</a> · {$t('landing.hero.noAccountRequired')}
        </p>
      </section>

      <!-- Data Fields Interactive -->
      <section id="datafields" class="section datafields-section" aria-labelledby="datafields-title">
        <h2 id="datafields-title" class="section-title">{$t('landing.dataFields.title')}</h2>
        <p class="section-subtitle">{$t('landing.dataFields.subtitle')}</p>

        <div class="datafields-showcase">
          <div class="datafield-tabs">
            <button class="datafield-tab" class:active={activeDataField === 0} on:click={() => activeDataField = 0}>
              {$t('landing.dataFields.tabs.quickGlance')}
            </button>
            <button class="datafield-tab" class:active={activeDataField === 1} on:click={() => activeDataField = 1}>
              {$t('landing.dataFields.tabs.balance')}
            </button>
            <button class="datafield-tab" class:active={activeDataField === 2} on:click={() => activeDataField = 2}>
              {$t('landing.dataFields.tabs.efficiency')}
            </button>
            <button class="datafield-tab" class:active={activeDataField === 3} on:click={() => activeDataField = 3}>
              {$t('landing.dataFields.tabs.fullView')}
            </button>
            <button class="datafield-tab" class:active={activeDataField === 4} on:click={() => activeDataField = 4}>
              {$t('landing.dataFields.tabs.trend')}
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
                        <div class="qg-status-text">{$t('landing.dataFields.karoo.optimal')}</div>
                      </div>
                      <div class="karoo-divider"></div>
                      <div class="qg-balance-section">
                        <div class="karoo-label">{$t('landing.dataFields.karoo.balance')}</div>
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
                        <span class="karoo-label">{$t('landing.dataFields.karoo.balance')}</span>
                        <span class="pb-status optimal">{$t('landing.dataFields.karoo.optimal')}</span>
                      </div>
                      <div class="pb-main">
                        <div class="pb-side">
                          <span class="pb-value">48</span>
                          <span class="pb-label">{$t('landing.dataFields.karoo.left')}</span>
                        </div>
                        <div class="pb-divider"></div>
                        <div class="pb-side">
                          <span class="pb-value">52</span>
                          <span class="pb-label">{$t('landing.dataFields.karoo.right')}</span>
                        </div>
                      </div>
                      <div class="balance-bar"><div class="bar-fill" style="width: 48%"></div></div>
                    </div>
                  {:else if activeDataField === 2}
                    <!-- Efficiency -->
                    <div class="karoo-layout efficiency">
                      <div class="eff-section">
                        <div class="eff-header">
                          <span class="karoo-label">{$t('landing.dataFields.karoo.torqueEff')}</span>
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
                          <span class="karoo-label">{$t('landing.dataFields.karoo.smoothness')}</span>
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
                        <div class="fo-header"><span class="karoo-label">{$t('landing.dataFields.karoo.balance')}</span><span class="fo-status optimal">{$t('landing.dataFields.karoo.ok')}</span></div>
                        <div class="fo-row"><span class="fo-num">48</span><span class="fo-sep">/</span><span class="fo-num">52</span></div>
                      </div>
                      <div class="karoo-divider"></div>
                      <div class="fo-section">
                        <div class="fo-header"><span class="karoo-label">{$t('landing.dataFields.karoo.te')}</span><span class="fo-status optimal">76%</span></div>
                        <div class="fo-row"><span class="fo-num sm">74</span><span class="fo-sep">/</span><span class="fo-num sm">77</span></div>
                      </div>
                      <div class="karoo-divider"></div>
                      <div class="fo-section">
                        <div class="fo-header"><span class="karoo-label">{$t('landing.dataFields.karoo.ps')}</span><span class="fo-status optimal">24%</span></div>
                        <div class="fo-row"><span class="fo-num sm">23</span><span class="fo-sep">/</span><span class="fo-num sm">25</span></div>
                      </div>
                    </div>
                  {:else if activeDataField === 4}
                    <!-- Balance Trend -->
                    <div class="karoo-layout balance-trend">
                      <div class="bt-section main">
                        <div class="karoo-label">{$t('landing.dataFields.karoo.now')}</div>
                        <div class="bt-values lg"><span>48</span><span class="bt-sep">:</span><span>52</span></div>
                      </div>
                      <div class="karoo-divider"></div>
                      <div class="bt-section">
                        <div class="karoo-label">{$t('landing.dataFields.karoo.avg3s')}</div>
                        <div class="bt-values"><span>49</span><span class="bt-sep">:</span><span>51</span></div>
                      </div>
                      <div class="bt-section">
                        <div class="karoo-label">{$t('landing.dataFields.karoo.avg10s')}</div>
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
      <section class="section before-after-section" aria-labelledby="difference-title">
        <h2 id="difference-title" class="section-title">{$t('landing.comparison.title')}</h2>
        <p class="section-subtitle">{$t('landing.comparison.subtitle')}</p>

        <div class="comparison-table">
          <div class="comparison-header">
            <div class="comparison-col">{$t('landing.comparison.without')}</div>
            <div class="comparison-col highlight">{$t('landing.comparison.with')}</div>
          </div>
          <div class="comparison-row">
            <div class="comparison-item before">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              <span>{$t('landing.comparison.items.before1')}</span>
            </div>
            <div class="comparison-item after">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <span>{$t('landing.comparison.items.after1')}</span>
            </div>
          </div>
          <div class="comparison-row">
            <div class="comparison-item before">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              <span>{$t('landing.comparison.items.before2')}</span>
            </div>
            <div class="comparison-item after">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <span>{$t('landing.comparison.items.after2')}</span>
            </div>
          </div>
          <div class="comparison-row">
            <div class="comparison-item before">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              <span>{$t('landing.comparison.items.before3')}</span>
            </div>
            <div class="comparison-item after">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <span>{$t('landing.comparison.items.after3')}</span>
            </div>
          </div>
          <div class="comparison-row">
            <div class="comparison-item before">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              <span>{$t('landing.comparison.items.before4')}</span>
            </div>
            <div class="comparison-item after">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <span>{$t('landing.comparison.items.after4')}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Metrics Deep Dive -->
      <section id="features" class="section" aria-labelledby="features-title" itemprop="featureList">
        <h2 id="features-title" class="section-title">{$t('landing.metrics.title')}</h2>
        <p class="section-subtitle">{$t('landing.metrics.subtitle')}</p>

        <div class="metrics-deep">
          <div class="metric-deep-card">
            <div class="metric-deep-header">
              <div class="metric-deep-icon balance"></div>
              <div>
                <h3>{$t('landing.metrics.balance.title')}</h3>
                <span class="metric-deep-range">{$t('landing.metrics.balance.range')}</span>
              </div>
            </div>
            <p class="metric-deep-desc">{$t('landing.metrics.balance.desc')}</p>
            <div class="metric-zones">
              <div class="zone optimal"><span>48-52%</span> {$t('landing.metrics.zones.optimal')}</div>
              <div class="zone attention"><span>45-48% / 52-55%</span> {$t('landing.metrics.zones.attention')}</div>
              <div class="zone problem"><span>&lt;45% / &gt;55%</span> {$t('landing.metrics.zones.problem')}</div>
            </div>
          </div>

          <div class="metric-deep-card featured">
            <div class="metric-deep-header">
              <div class="metric-deep-icon te"></div>
              <div>
                <h3>{$t('landing.metrics.te.title')}</h3>
                <span class="metric-deep-range">{$t('landing.metrics.te.range')}</span>
              </div>
            </div>
            <p class="metric-deep-desc">{@html $t('landing.metrics.te.desc')}</p>
            <div class="metric-zones">
              <div class="zone optimal"><span>70-80%</span> {$t('landing.metrics.zones.optimal')}</div>
              <div class="zone attention"><span>60-70% / 80-85%</span> {$t('landing.metrics.zones.attention')}</div>
              <div class="zone problem"><span>&lt;60% / &gt;85%</span> {$t('landing.metrics.zones.problem')}</div>
            </div>
          </div>

          <div class="metric-deep-card">
            <div class="metric-deep-header">
              <div class="metric-deep-icon ps"></div>
              <div>
                <h3>{$t('landing.metrics.ps.title')}</h3>
                <span class="metric-deep-range">{$t('landing.metrics.ps.range')}</span>
              </div>
            </div>
            <p class="metric-deep-desc">{$t('landing.metrics.ps.desc')}</p>
            <div class="metric-zones">
              <div class="zone optimal"><span>≥20%</span> {$t('landing.metrics.zones.optimal')}</div>
              <div class="zone attention"><span>15-20%</span> {$t('landing.metrics.zones.attention')}</div>
              <div class="zone problem"><span>&lt;15%</span> {$t('landing.metrics.zones.problem')}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- Drills Section -->
      <section id="drills" class="section drills-section" aria-labelledby="drills-title">
        <h2 id="drills-title" class="section-title">{$t('landing.drills.title')}</h2>
        <p class="section-subtitle">{$t('landing.drills.subtitle')}</p>

        <div class="drills-tabs">
          <button class="drill-tab" class:active={activeDrillCategory === 'focus'} on:click={() => activeDrillCategory = 'focus'}>
            {$t('landing.drills.tabs.focus')}
          </button>
          <button class="drill-tab" class:active={activeDrillCategory === 'challenge'} on:click={() => activeDrillCategory = 'challenge'}>
            {$t('landing.drills.tabs.challenge')}
          </button>
          <button class="drill-tab" class:active={activeDrillCategory === 'workout'} on:click={() => activeDrillCategory = 'workout'}>
            {$t('landing.drills.tabs.workout')}
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
                    <span class="drill-level" class:beginner={drill.level === $t('landing.drills.levels.beginner')} class:intermediate={drill.level === $t('landing.drills.levels.intermediate')} class:advanced={drill.level === $t('landing.drills.levels.advanced')}>{drill.level}</span>
                  </span>
                </div>
                <svg class="drill-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="6 9 12 15 18 9"/>
                </svg>
              </div>
              {#if expandedDrill === drill.id}
                <div class="drill-details">
                  <p>{drill.desc}</p>
                  <div class="drill-target">{$t('landing.drills.target')}: {drill.target}</div>
                </div>
              {/if}
            </button>
          {/each}
        </div>

        <div class="drills-highlight">
          <span class="highlight-label">{$t('landing.drills.featured')}</span>
          <h4>{$t('landing.drills.featuredTitle')}</h4>
          <p>{$t('landing.drills.featuredDesc')}</p>
        </div>
      </section>

      <!-- Alerts Section -->
      <section class="section alerts-section" aria-labelledby="alerts-title">
        <h2 id="alerts-title" class="section-title">{$t('landing.alerts.title')}</h2>
        <p class="section-subtitle">{$t('landing.alerts.subtitle')}</p>

        <div class="alerts-demo">
          <div class="alert-banner-demo">
            <div class="alert-indicator"></div>
            <div class="alert-content">
              <span class="alert-title">{$t('landing.alerts.demo.title')}</span>
              <span class="alert-detail">{$t('landing.alerts.demo.detail')}</span>
            </div>
          </div>

          <div class="alerts-features">
            <div class="alert-feature">
              <h4>{$t('landing.alerts.features.vibration.title')}</h4>
              <p>{$t('landing.alerts.features.vibration.desc')}</p>
            </div>
            <div class="alert-feature">
              <h4>{$t('landing.alerts.features.screenWake.title')}</h4>
              <p>{$t('landing.alerts.features.screenWake.desc')}</p>
            </div>
            <div class="alert-feature">
              <h4>{$t('landing.alerts.features.cooldown.title')}</h4>
              <p>{$t('landing.alerts.features.cooldown.desc')}</p>
            </div>
            <div class="alert-feature">
              <h4>{$t('landing.alerts.features.thresholds.title')}</h4>
              <p>{$t('landing.alerts.features.thresholds.desc')}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Analytics & Dashboard -->
      <section class="section" aria-labelledby="analytics-title">
        <h2 id="analytics-title" class="section-title">{$t('landing.analytics.title')}</h2>
        <p class="section-subtitle">{$t('landing.analytics.subtitle')}</p>

        <div class="dashboard-preview">
          <div class="dash-browser">
            <div class="browser-bar">
              <div class="browser-dots"><span></span><span></span><span></span></div>
              <div class="browser-url">kpedal.com</div>
            </div>
            <div class="browser-content">
              <div class="dash-header-preview">
                <div class="dash-greeting-preview">{$t('landing.analytics.preview.greeting')}</div>
                <div class="period-selector-preview">
                  <span class="period-btn-preview active">7d</span>
                  <span class="period-btn-preview">14d</span>
                  <span class="period-btn-preview">30d</span>
                </div>
              </div>

              <!-- Hero Stats Row -->
              <div class="hero-stats-preview">
                <div class="hero-stat-preview asymmetry-preview">
                  <span class="hero-value optimal">0.8%</span>
                  <span class="hero-label">{$t('metrics.asymmetry')}</span>
                  <span class="hero-detail">{$t('dashboard.balanced')}</span>
                </div>
                <div class="hero-stat-preview balance-preview">
                  <div class="balance-bar-preview">
                    <span class="balance-side-preview">49</span>
                    <div class="balance-visual-preview">
                      <div class="balance-fill-left" style="width: 49%"></div>
                      <div class="balance-fill-right" style="width: 51%"></div>
                    </div>
                    <span class="balance-side-preview">51</span>
                  </div>
                </div>
                <div class="hero-stat-preview zones-preview">
                  <div class="zone-bars-preview">
                    <div class="zone-bar optimal" style="width: 72%"></div>
                    <div class="zone-bar attention" style="width: 20%"></div>
                    <div class="zone-bar problem" style="width: 8%"></div>
                  </div>
                  <span class="hero-label">{$t('zones.timeInZone')}</span>
                </div>
                <div class="hero-stat-preview summary-preview">
                  <div class="summary-row">
                    <span class="summary-num">12</span>
                    <span class="summary-unit">{$t('dashboard.weeklyChart.rides')}</span>
                  </div>
                  <div class="summary-row">
                    <span class="summary-num">8.4</span>
                    <span class="summary-unit">{$t('dashboard.weeklyChart.hours')}</span>
                  </div>
                </div>
              </div>

              <!-- Main Grid -->
              <div class="main-grid-preview">
                <div class="grid-card-preview rides-card">
                  <div class="card-header-preview">
                    <span>{$t('landing.analytics.preview.recentRides')}</span>
                  </div>
                  <div class="recent-rides-preview">
                    <div class="ride-row-preview">
                      <div class="ride-info">
                        <span class="ride-title">{$t('landing.analytics.preview.morningRide')}</span>
                        <span class="ride-meta">{$t('common.today')} · 1h 23m</span>
                      </div>
                      <div class="ride-score optimal">87</div>
                    </div>
                    <div class="ride-row-preview">
                      <div class="ride-info">
                        <span class="ride-title">{$t('landing.analytics.preview.eveningSpin')}</span>
                        <span class="ride-meta">{$t('common.yesterday')} · 45m</span>
                      </div>
                      <div class="ride-score optimal">92</div>
                    </div>
                    <div class="ride-row-preview">
                      <div class="ride-info">
                        <span class="ride-title">{$t('landing.analytics.preview.longRide')}</span>
                        <span class="ride-meta">2 {$t('landing.analytics.preview.daysAgo')} · 2h 15m</span>
                      </div>
                      <div class="ride-score attention">71</div>
                    </div>
                  </div>
                </div>
                <div class="grid-card-preview technique-card">
                  <div class="card-header-preview">
                    <span>{$t('landing.analytics.preview.technique')}</span>
                    <span class="card-subtitle-preview">7d {$t('landing.analytics.preview.avgSuffix')}</span>
                  </div>
                  <div class="technique-preview">
                    <div class="technique-metric-lr">
                      <span class="metric-name">{$t('metrics.te')}</span>
                      <div class="lr-comparison">
                        <div class="leg left">
                          <span class="leg-label">L</span>
                          <div class="leg-bar-wrap">
                            <div class="leg-bar te" style="width: 72%"></div>
                          </div>
                          <span class="leg-value">72%</span>
                        </div>
                        <div class="leg right">
                          <span class="leg-label">R</span>
                          <div class="leg-bar-wrap">
                            <div class="leg-bar te" style="width: 76%"></div>
                          </div>
                          <span class="leg-value">76%</span>
                        </div>
                      </div>
                    </div>
                    <div class="technique-metric-lr">
                      <span class="metric-name">{$t('metrics.ps')}</span>
                      <div class="lr-comparison">
                        <div class="leg left">
                          <span class="leg-label">L</span>
                          <div class="leg-bar-wrap">
                            <div class="leg-bar ps" style="width: 52%"></div>
                          </div>
                          <span class="leg-value">21%</span>
                        </div>
                        <div class="leg right">
                          <span class="leg-label">R</span>
                          <div class="leg-bar-wrap">
                            <div class="leg-bar ps" style="width: 58%"></div>
                          </div>
                          <span class="leg-value">23%</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="dash-features">
            <div class="dash-feature">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              </svg>
              <span>{$t('landing.analytics.features.periods')}</span>
            </div>
            <div class="dash-feature">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              <span>{$t('landing.analytics.features.history')}</span>
            </div>
            <div class="dash-feature">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
              <span>{$t('landing.analytics.features.multiDevice')}</span>
            </div>
            <div class="dash-feature">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/>
              </svg>
              <span>{$t('landing.analytics.features.openSource')}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Stay Motivated -->
      <section class="section motivation-section" aria-labelledby="motivation-title">
        <h2 id="motivation-title" class="section-title">{$t('landing.motivation.title')}</h2>
        <p class="section-subtitle">{$t('landing.motivation.subtitle')}</p>

        <div class="motivation-content">
          <div class="motivation-stats">
            <div class="motivation-stat">
              <span class="stat-number">16</span>
              <span class="stat-desc">{$t('landing.motivation.achievements')}</span>
            </div>
            <div class="motivation-divider"></div>
            <div class="motivation-stat">
              <span class="stat-number">7</span>
              <span class="stat-desc">{$t('landing.motivation.challenges')}</span>
            </div>
          </div>

          <div class="motivation-examples">
            <div class="example-group">
              <span class="example-label">{$t('landing.motivation.achievementsLabel')}</span>
              <div class="example-items">
                <span>{$t('landing.motivation.examples.firstRide')}</span>
                <span>{$t('landing.motivation.examples.hundredRides')}</span>
                <span>{$t('landing.motivation.examples.perfectBalance')}</span>
                <span>{$t('landing.motivation.examples.thirtyDayStreak')}</span>
              </div>
            </div>
            <div class="example-group">
              <span class="example-label">{$t('landing.motivation.challengesLabel')}</span>
              <div class="example-items">
                <span>{$t('landing.motivation.examples.balancedRider')}</span>
                <span>{$t('landing.motivation.examples.zoneMaster')}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Works Everywhere - Combined Background + Sync -->
      <section class="section works-everywhere-section" aria-labelledby="works-title">
        <h2 id="works-title" class="section-title">{$t('landing.works.title')}</h2>
        <p class="section-subtitle">{$t('landing.works.subtitle')}</p>

        <div class="works-grid">
          <div class="works-card">
            <div class="works-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/>
              </svg>
            </div>
            <h4>{$t('landing.works.background.title')}</h4>
            <p>{$t('landing.works.background.desc')}</p>
          </div>
          <div class="works-card">
            <div class="works-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/>
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
              </svg>
            </div>
            <h4>{$t('landing.works.sync.title')}</h4>
            <p>{$t('landing.works.sync.desc')}</p>
          </div>
          <div class="works-card">
            <div class="works-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>
            <h4>{$t('landing.works.multiDevice.title')}</h4>
            <p>{$t('landing.works.multiDevice.desc')}</p>
          </div>
        </div>
      </section>

      <!-- What You Need -->
      <section class="section requirements-section" aria-labelledby="requirements-title" itemprop="softwareRequirements">
        <h2 id="requirements-title" class="section-title">{$t('landing.requirements.title')}</h2>
        <p class="section-subtitle">{$t('landing.requirements.subtitle')}</p>

        <div class="requirements-grid">
          <div class="req-card">
            <div class="req-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/>
              </svg>
            </div>
            <h4>{$t('landing.requirements.karoo.title')}</h4>
            <p>{$t('landing.requirements.karoo.desc')}</p>
          </div>
          <div class="req-card">
            <div class="req-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/>
              </svg>
            </div>
            <h4>{$t('landing.requirements.pedals.title')}</h4>
            <p>{$t('landing.requirements.pedals.desc')}</p>
          </div>
        </div>

        <div class="pedals-compatibility">
          <div class="pedals-grid">
            <div class="pedals-card full">
              <div class="pedals-card-header">
                <span class="pedals-icon">✓</span>
                <span class="pedals-title">{$t('landing.requirements.fullSupport.title')}</span>
              </div>
              <p class="pedals-desc">{$t('landing.requirements.fullSupport.desc')}</p>
              <div class="pedals-chips">
                <span class="pedal-chip">Garmin Rally</span>
                <span class="pedal-chip">Garmin Vector 3</span>
                <span class="pedal-chip">Favero Assioma DUO</span>
                <span class="pedal-chip">Favero Assioma DUO-Shi</span>
                <span class="pedal-chip">Favero Assioma Pro RS</span>
                <span class="pedal-chip">Favero Assioma Pro MX</span>
                <span class="pedal-chip">SRM X-Power</span>
                <span class="pedal-chip">Rotor 2INpower</span>
                <span class="pedal-chip">Look Keo Blade</span>
                <span class="pedal-chip">IQ2</span>
              </div>
            </div>
            <div class="pedals-card balance">
              <div class="pedals-card-header">
                <span class="pedals-icon">◐</span>
                <span class="pedals-title">{$t('landing.requirements.balanceOnly.title')}</span>
              </div>
              <p class="pedals-desc">{$t('landing.requirements.balanceOnly.desc')}</p>
              <div class="pedals-chips">
                <span class="pedal-chip">Wahoo POWRLINK</span>
                <span class="pedal-chip">Power2Max</span>
                <span class="pedal-chip">Quarq DZero</span>
                <span class="pedal-chip">SRAM AXS PM</span>
                <span class="pedal-chip">Stages LR</span>
                <span class="pedal-chip">4iiii Dual</span>
                <span class="pedal-chip">Sigeyi AXO</span>
                <span class="pedal-chip">FSA Powerbox</span>
              </div>
            </div>
            <div class="pedals-card incompatible">
              <div class="pedals-card-header">
                <span class="pedals-icon">✗</span>
                <span class="pedals-title">{$t('landing.requirements.notCompatible.title')}</span>
              </div>
              <p class="pedals-desc">{$t('landing.requirements.notCompatible.desc')}</p>
              <div class="pedals-chips">
                <span class="pedal-chip">{$t('landing.requirements.pedalsList.singleSided')}</span>
                <span class="pedal-chip">{$t('landing.requirements.pedalsList.trainerPower')}</span>
                <span class="pedal-chip">Vector 1/2</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- FAQ -->
      <section class="section faq-section" aria-labelledby="faq-title" itemscope itemtype="https://schema.org/FAQPage">
        <h2 id="faq-title" class="section-title">{$t('landing.faq.title')}</h2>

        <div class="faq-list" role="list">
          <details class="faq-item">
            <summary>{$t('landing.faq.q1.q')}</summary>
            <p>{@html $t('landing.faq.q1.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q2.q')}</summary>
            <p>{@html $t('landing.faq.q2.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q3.q')}</summary>
            <p>{@html $t('landing.faq.q3.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q4.q')}</summary>
            <p>{@html $t('landing.faq.q4.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q5.q')}</summary>
            <p>{@html $t('landing.faq.q5.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q6.q')}</summary>
            <p>{@html $t('landing.faq.q6.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q7.q')}</summary>
            <p>{@html $t('landing.faq.q7.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q8.q')}</summary>
            <p>{@html $t('landing.faq.q8.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q9.q')}</summary>
            <p>{@html $t('landing.faq.q9.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q10.q')}</summary>
            <p>{@html $t('landing.faq.q10.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q11.q')}</summary>
            <p>{@html $t('landing.faq.q11.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q12.q')}</summary>
            <p>{@html $t('landing.faq.q12.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q13.q')}</summary>
            <p>{@html $t('landing.faq.q13.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q14.q')}</summary>
            <p>{@html $t('landing.faq.q14.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q15.q')}</summary>
            <p>{@html $t('landing.faq.q15.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q16.q')}</summary>
            <p>{@html $t('landing.faq.q16.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q17.q')}</summary>
            <p>{@html $t('landing.faq.q17.a')}</p>
          </details>

          <details class="faq-item">
            <summary>{$t('landing.faq.q18.q')}</summary>
            <p>{@html $t('landing.faq.q18.a')}</p>
          </details>
        </div>
      </section>

      <!-- Final CTA -->
      <section class="section cta-section" aria-labelledby="cta-title">
        <h2 id="cta-title" class="cta-headline">{$t('landing.cta.headline')}</h2>
        <p class="cta-subtext">{$t('landing.cta.subtext')}</p>
        <div class="cta-actions" role="group" aria-label={$t('aria.getStartedOptions')}>
          <a href="https://github.com/yrkan/kpedal/releases" class="cta-btn primary large" target="_blank" rel="noopener noreferrer" aria-label={$t('aria.downloadKaroo')}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            {$t('landing.cta.downloadForKaroo')}
          </a>
          <a href="https://app.kpedal.com/login" class="cta-btn secondary" aria-label={$t('aria.tryDemo')}>
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="12" cy="12" r="10"/>
              <polygon points="10 8 16 12 10 16 10 8" fill="currentColor" stroke="none"/>
            </svg>
            {$t('landing.cta.tryDemo')}
          </a>
        </div>
      </section>

      <!-- Footer -->
      <footer class="landing-footer" role="contentinfo">
        <div class="footer-top">
          <div class="footer-brand">
            <div class="footer-logo" aria-hidden="true">
              <span class="logo-dot small"></span>
              <span class="footer-brand-name">{$t('app.name')}</span>
            </div>
            <p class="footer-tagline">{$t('landing.footer.tagline')}</p>
          </div>

          <nav class="footer-nav" aria-label={$t('aria.footerNav')}>
            <div class="footer-col">
              <h4>{$t('landing.footer.product')}</h4>
              <a href="#features">{$t('landing.footer.features')}</a>
              <a href="#datafields">{$t('landing.footer.dataFields')}</a>
              <a href="#drills">{$t('landing.footer.drills')}</a>
            </div>
            <div class="footer-col">
              <h4>{$t('landing.footer.resources')}</h4>
              <a href="https://github.com/yrkan/kpedal" target="_blank" rel="noopener noreferrer">{$t('landing.footer.github')}</a>
              <a href="https://github.com/yrkan/kpedal/releases" target="_blank" rel="noopener noreferrer">{$t('landing.footer.releases')}</a>
              <a href="https://github.com/yrkan/kpedal/issues" target="_blank" rel="noopener noreferrer">{$t('landing.footer.reportIssue')}</a>
            </div>
            <div class="footer-col">
              <h4>{$t('landing.footer.legal')}</h4>
              <a href="/privacy">{$t('landing.footer.privacyPolicy')}</a>
              <button class="footer-link-btn" on:click={handleLogin}>{$t('landing.footer.signIn')}</button>
            </div>
          </nav>
        </div>

        <div class="footer-bottom">
          <p>{$t('landing.footer.madeWith')}</p>
          <a href="https://github.com/yrkan/kpedal" target="_blank" rel="noopener noreferrer" class="footer-github" aria-label={$t('aria.viewGitHub')}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>
          </a>
        </div>
      </footer>
    </article>
  </div>
{:else}
  <!-- Dashboard (authenticated) -->
  <div class="dashboard">
    <div class="container">
      {#if loading}
        <div class="loading-state"><div class="spinner"></div></div>
      {:else if error}
        <div class="error-state">
          <p>{$t('common.loadError')}</p>
          <button class="btn btn-primary" on:click={() => location.reload()}>{$t('common.retry')}</button>
        </div>
      {:else}
        {#if stats && stats.total_rides > 0}
          <header class="dash-header animate-in">
            <div class="dash-greeting">
              <h1>{$t('dashboard.hello', { values: { name: getFirstName($user?.name) } })}</h1>
            </div>
            <div class="dash-controls">
              <div class="period-selector">
                <button class="period-btn" class:active={selectedPeriod === '7'} on:click={() => selectedPeriod = '7'}>7d</button>
                <button class="period-btn" class:active={selectedPeriod === '14'} on:click={() => selectedPeriod = '14'} disabled={ridesIn14Days === ridesIn7Days}>14d</button>
                <button class="period-btn" class:active={selectedPeriod === '30'} on:click={() => selectedPeriod = '30'} disabled={ridesIn30Days === ridesIn14Days}>30d</button>
                <button class="period-btn" class:active={selectedPeriod === '60'} on:click={() => selectedPeriod = '60'} disabled={ridesIn60Days === ridesIn30Days}>60d</button>
              </div>
            </div>
          </header>
          <!-- Hero Stats Row -->
          <div class="hero-stats animate-in">
            <div class="hero-stat asymmetry">
              <div class="hero-stat-main">
                <span class="hero-stat-value {getBalanceStatus(periodStats?.balance || 50)}">{periodStats ? Math.abs(periodStats.balance - 50).toFixed(1) : '—'}%</span>
              </div>
              <div class="hero-stat-info">
                <span class="hero-stat-label">
                  {$t('metrics.asymmetry')}
                  <InfoTip
                    text={$t('infotips.asymmetry')}
                    position="bottom"
                  />
                </span>
                <span class="hero-stat-detail">{periodStats && periodStats.balance !== 50 ? (periodStats.balance > 50 ? $t('dashboard.leftDominant') : $t('dashboard.rightDominant')) : $t('dashboard.balanced')}</span>
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
                {$t('zones.timeInZone')}
                <InfoTip
                  text={$t('infotips.timeInZone')}
                  position="bottom"
                />
              </span>
            </div>

            <div class="hero-stat summary">
              <div class="summary-grid">
                <div class="summary-metric">
                  <span class="summary-num">{periodStats?.rides || 0}</span>
                  <span class="summary-unit">{$t('dashboard.summary.rides')}</span>
                </div>
                <div class="summary-metric">
                  <span class="summary-num">{periodStats ? (periodStats.duration / 3600000).toFixed(1) : 0}</span>
                  <span class="summary-unit">{$t('dashboard.summary.hours')}</span>
                </div>
                <div class="summary-metric">
                  <span class="summary-num">{periodStats?.totalDistance?.toFixed(0) || 0}</span>
                  <span class="summary-unit">{$t('dashboard.summary.km')}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Main Grid -->
          <div class="main-grid animate-in">
            <!-- Left Column: Activity + Training -->
            <div class="grid-card">
              <div class="card-header">
                <span class="card-title">{$t('dashboard.weeklyActivity')} <InfoTip text={$t('infotips.weeklyActivity')} position="bottom" /></span>
                <span class="card-subtitle">{ridesPerDay.reduce((a, b) => a + b, 0)} {$t('dashboard.summary.rides')}</span>
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
                  <span class="section-label">{$t('dashboard.vsLastWeek')}</span>
                  <div class="comparison-grid">
                    <div class="comp-item">
                      <span class="comp-label">{$t('dashboard.comparison.rides')} <InfoTip text={$t('infotips.compRides')} position="bottom" size="sm" /></span>
                      <span class="comp-value">{weeklyComparison.thisWeek.rides_count}</span>
                      <span class="comp-delta {weeklyComparison.changes.rides_count >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.rides_count >= 0 ? '+' : ''}{weeklyComparison.changes.rides_count}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">{$t('dashboard.comparison.hours')} <InfoTip text={$t('infotips.compHours')} position="bottom" size="sm" /></span>
                      <span class="comp-value">{(weeklyComparison.thisWeek.total_duration_ms / 3600000).toFixed(1)}</span>
                      <span class="comp-delta {weeklyComparison.changes.duration >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.duration >= 0 ? '+' : ''}{(weeklyComparison.changes.duration / 3600000).toFixed(1)}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">{$t('dashboard.comparison.distance')} <InfoTip text={$t('infotips.compDistance')} position="bottom" size="sm" /></span>
                      <span class="comp-value">{weeklyComparison.thisWeek.total_distance_km.toFixed(0)}km</span>
                      <span class="comp-delta {weeklyComparison.changes.distance >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.distance >= 0 ? '+' : ''}{weeklyComparison.changes.distance.toFixed(0)}
                      </span>
                    </div>
                    {#if weeklyComparison.thisWeek.total_elevation > 0}
                      <div class="comp-item">
                        <span class="comp-label">{$t('dashboard.comparison.climb')} <InfoTip text={$t('infotips.compClimb')} position="bottom" size="sm" /></span>
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
                  <span class="section-label">{$t('dashboard.sections.performance')}</span>
                  <div class="performance-mini-grid">
                    <div class="perf-mini-item">
                      <span class="perf-mini-label">{$t('dashboard.perfStats.score')} <InfoTip text={$t('infotips.perfScore')} position="top" /></span>
                      <span class="perf-mini-value">{weeklyComparison.thisWeek.avg_score.toFixed(0)}</span>
                      <span class="perf-mini-delta {weeklyComparison.changes.score >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.score >= 0 ? '+' : ''}{weeklyComparison.changes.score.toFixed(0)}
                      </span>
                    </div>
                    <div class="perf-mini-item">
                      <span class="perf-mini-label">{$t('dashboard.perfStats.optimal')} <InfoTip text={$t('infotips.perfOptimal')} position="top" /></span>
                      <span class="perf-mini-value">{weeklyComparison.thisWeek.avg_zone_optimal.toFixed(0)}%</span>
                      <span class="perf-mini-delta {weeklyComparison.changes.zone_optimal >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.zone_optimal >= 0 ? '+' : ''}{weeklyComparison.changes.zone_optimal.toFixed(0)}
                      </span>
                    </div>
                    {#if weeklyComparison.thisWeek.avg_power > 0}
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">{$t('dashboard.perfStats.power')} <InfoTip text={$t('infotips.perfPower')} position="top" /></span>
                        <span class="perf-mini-value">{Math.round(weeklyComparison.thisWeek.avg_power)}W</span>
                        <span class="perf-mini-delta {weeklyComparison.changes.power >= 0 ? 'up' : 'down'}">
                          {weeklyComparison.changes.power >= 0 ? '+' : ''}{Math.round(weeklyComparison.changes.power)}
                        </span>
                      </div>
                    {/if}
                    {#if weeklyComparison.thisWeek.total_energy_kj > 0}
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">{$t('dashboard.perfStats.energy')} <InfoTip text={$t('infotips.perfEnergy')} position="top" /></span>
                        <span class="perf-mini-value">{Math.round(weeklyComparison.thisWeek.total_energy_kj)}kJ</span>
                        <span class="perf-mini-delta {weeklyComparison.changes.energy >= 0 ? 'up' : 'down'}">
                          {weeklyComparison.changes.energy >= 0 ? '+' : ''}{Math.round(weeklyComparison.changes.energy)}
                        </span>
                      </div>
                    {/if}
                  </div>
                </div>
              {/if}

              <!-- Insights Section -->
              {#if insights.length > 0}
                <div class="card-section insights-section">
                  <span class="section-label">{$t('dashboard.sections.insights')}</span>
                  <div class="insights-list">
                    {#each insights as insight}
                      <div class="insight-item">
                        <span class="insight-icon">{insight.icon}</span>
                        <span class="insight-text">{insight.text}{#if insight.link}&nbsp;<a href={insight.link.href} class="insight-link">{insight.link.label}</a>{/if}</span>
                      </div>
                    {/each}
                  </div>
                </div>
              {/if}
            </div>

            <!-- Right Column: Technique -->
            <div class="grid-card">
              <div class="card-header">
                <span class="card-title">{$t('dashboard.technique')} <InfoTip text={$t('infotips.technique')} position="bottom" /></span>
                <span class="card-subtitle">{selectedPeriod}d {$t('metrics.avg').toLowerCase()}</span>
              </div>

              <div class="technique-enhanced">
                <div class="technique-metric">
                  <div class="technique-metric-header">
                    <span class="technique-metric-label">
                      {$t('metrics.te')}
                      <InfoTip
                        text={$t('infotips.te')}
                        position="top"
                      />
                    </span>
                    <span class="technique-metric-value">{periodStats?.te?.toFixed(0) || ((stats.avg_te_left + stats.avg_te_right) / 2).toFixed(0)}%</span>
                  </div>
                  <div class="technique-metric-bar-wrap">
                    <div class="technique-metric-bar">
                      <div class="technique-optimal-zone te"></div>
                      <div class="technique-metric-fill te" style="width: {Math.min(100, (periodStats?.te || ((stats.avg_te_left + stats.avg_te_right) / 2)))}%"></div>
                      <div class="technique-bar-marker" style="left: 70%"></div>
                      <div class="technique-bar-marker" style="left: 80%"></div>
                    </div>
                    <div class="technique-bar-labels te">
                      <span>0</span>
                      <span class="optimal-label">70-80</span>
                      <span>100</span>
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
                      {$t('metrics.ps')}
                      <InfoTip
                        text={$t('infotips.ps')}
                        position="top"
                      />
                    </span>
                    <span class="technique-metric-value">{periodStats?.ps?.toFixed(0) || ((stats.avg_ps_left + stats.avg_ps_right) / 2).toFixed(0)}%</span>
                  </div>
                  <div class="technique-metric-bar-wrap">
                    <div class="technique-metric-bar">
                      <div class="technique-optimal-zone ps"></div>
                      <div class="technique-metric-fill ps" style="width: {Math.min(100, (periodStats?.ps || ((stats.avg_ps_left + stats.avg_ps_right) / 2)) / 40 * 100)}%"></div>
                      <div class="technique-bar-marker" style="left: 50%"></div>
                    </div>
                    <div class="technique-bar-labels ps">
                      <span>0</span>
                      <span class="optimal-label">≥20</span>
                      <span>40</span>
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
                  <span class="section-label">{$t('dashboard.vsLastWeek')}</span>
                  <div class="comparison-grid">
                    <div class="comp-item">
                      <span class="comp-label">{$t('metrics.teShort')} <InfoTip text={$t('infotips.compTe')} position="bottom" size="sm" /></span>
                      <span class="comp-value">{weeklyComparison.thisWeek.avg_te.toFixed(0)}%</span>
                      <span class="comp-delta {weeklyComparison.changes.te >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.te >= 0 ? '+' : ''}{weeklyComparison.changes.te.toFixed(1)}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">{$t('metrics.psShort')} <InfoTip text={$t('infotips.compPs')} position="bottom" size="sm" /></span>
                      <span class="comp-value">{weeklyComparison.thisWeek.avg_ps.toFixed(0)}%</span>
                      <span class="comp-delta {weeklyComparison.changes.ps >= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.ps >= 0 ? '+' : ''}{weeklyComparison.changes.ps.toFixed(1)}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">{$t('dashboard.comparison.balance')} <InfoTip text={$t('infotips.compBalance')} position="bottom" size="sm" /></span>
                      <span class="comp-value">{Math.abs(weeklyComparison.thisWeek.avg_balance_left - 50).toFixed(1)}%</span>
                      <span class="comp-delta {weeklyComparison.changes.balance <= 0 ? 'up' : 'down'}">
                        {weeklyComparison.changes.balance <= 0 ? '' : '+'}{weeklyComparison.changes.balance.toFixed(1)}
                      </span>
                    </div>
                    <div class="comp-item">
                      <span class="comp-label">{$t('dashboard.comparison.optimal')} <InfoTip text={$t('infotips.compOptimal')} position="bottom" size="sm" /></span>
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
                      {$t('dashboard.fatigue.title')}
                      <InfoTip
                        text={$t('infotips.fatigue')}
                        position="top"
                      />
                    </span>
                    <div class="performance-mini-grid">
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">{$t('metrics.balance')} <InfoTip text={$t('infotips.balanceFatigue')} position="top" size="sm" /></span>
                        <span class="perf-mini-value">{fatigueData.firstThird.balance.toFixed(0)}→{fatigueData.lastThird.balance.toFixed(0)}</span>
                        <span class="perf-mini-delta {fatigueData.degradation.balance > 0.5 ? 'down' : fatigueData.degradation.balance < -0.5 ? 'up' : ''}">
                          {fatigueData.degradation.balance > 0.5 ? '↓' : fatigueData.degradation.balance < -0.5 ? '↑' : '—'}
                        </span>
                      </div>
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">{$t('metrics.teShort')} <InfoTip text={$t('infotips.teFatigue')} position="top" size="sm" /></span>
                        <span class="perf-mini-value">{fatigueData.firstThird.te.toFixed(0)}→{fatigueData.lastThird.te.toFixed(0)}</span>
                        <span class="perf-mini-delta {fatigueData.degradation.te > 2 ? 'down' : fatigueData.degradation.te < -2 ? 'up' : ''}">
                          {fatigueData.degradation.te > 2 ? '↓' : fatigueData.degradation.te < -2 ? '↑' : '—'}
                        </span>
                      </div>
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">{$t('metrics.psShort')} <InfoTip text={$t('infotips.psFatigue')} position="top" size="sm" /></span>
                        <span class="perf-mini-value">{fatigueData.firstThird.ps.toFixed(0)}→{fatigueData.lastThird.ps.toFixed(0)}</span>
                        <span class="perf-mini-delta {fatigueData.degradation.ps > 1 ? 'down' : fatigueData.degradation.ps < -1 ? 'up' : ''}">
                          {fatigueData.degradation.ps > 1 ? '↓' : fatigueData.degradation.ps < -1 ? '↑' : '—'}
                        </span>
                      </div>
                      <div class="perf-mini-item">
                        <span class="perf-mini-label">{$t('dashboard.fatigue.trend')} <InfoTip text={$t('infotips.fatigueTrend')} position="top" size="sm" /></span>
                        <span class="perf-mini-value fatigue-summary {fatigueData.degradation.te > 2 || fatigueData.degradation.ps > 1 ? 'problem' : fatigueData.degradation.te > 0 || fatigueData.degradation.ps > 0 ? 'attention' : 'optimal'}">
                          {fatigueData.degradation.te > 2 || fatigueData.degradation.ps > 1 ? $t('dashboard.fatigue.dropped') : fatigueData.degradation.te > 0 || fatigueData.degradation.ps > 0 ? $t('dashboard.fatigue.slight') : $t('dashboard.fatigue.stable')}
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
                  <span class="metric-chip-unit">{$t('dashboard.units.wAvg')} <InfoTip text={$t('infotips.powerAvg')} position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.maxPower > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{Math.round(periodStats.maxPower)}</span>
                  <span class="metric-chip-unit">{$t('dashboard.units.wMax')} <InfoTip text={$t('infotips.powerMax')} position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.avgNormalizedPower > 0}
                <div class="metric-chip accent">
                  <span class="metric-chip-val">{Math.round(periodStats.avgNormalizedPower)}</span>
                  <span class="metric-chip-unit">
                    {$t('metrics.npShort')}
                    <InfoTip
                      text={$t('infotips.np')}
                      position="bottom"
                    />
                  </span>
                </div>
              {/if}
              {#if periodStats.avgCadence > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{Math.round(periodStats.avgCadence)}</span>
                  <span class="metric-chip-unit">{$t('dashboard.units.rpm')} <InfoTip text={$t('infotips.cadence')} position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.avgHr > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{Math.round(periodStats.avgHr)}</span>
                  <span class="metric-chip-unit">{$t('dashboard.units.bpmAvg')} <InfoTip text={$t('infotips.hrAvg')} position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.maxHr > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{Math.round(periodStats.maxHr)}</span>
                  <span class="metric-chip-unit">{$t('dashboard.units.bpmMax')} <InfoTip text={$t('infotips.hrMax')} position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.avgSpeed > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{periodStats.avgSpeed.toFixed(1)}</span>
                  <span class="metric-chip-unit">{$t('dashboard.units.kmh')} <InfoTip text={$t('infotips.speed')} position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.totalElevationGain > 0}
                <div class="metric-chip elevation">
                  <span class="metric-chip-val">{Math.round(periodStats.totalElevationGain)}</span>
                  <span class="metric-chip-unit">{$t('dashboard.units.mUp')} <InfoTip text={$t('infotips.elevationGain')} position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.totalElevationLoss > 0}
                <div class="metric-chip elevation">
                  <span class="metric-chip-val">{Math.round(periodStats.totalElevationLoss)}</span>
                  <span class="metric-chip-unit">{$t('dashboard.units.mDown')} <InfoTip text={$t('infotips.elevationLoss')} position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.maxGrade > 0}
                <div class="metric-chip">
                  <span class="metric-chip-val">{periodStats.maxGrade.toFixed(1)}</span>
                  <span class="metric-chip-unit">{$t('dashboard.units.grade')} <InfoTip text={$t('infotips.grade')} position="bottom" /></span>
                </div>
              {/if}
              {#if periodStats.totalEnergy > 0}
                <div class="metric-chip energy">
                  <span class="metric-chip-val">{Math.round(periodStats.totalEnergy)}</span>
                  <span class="metric-chip-unit">{$t('dashboard.units.kJ')} <InfoTip text={$t('infotips.energy')} position="bottom" /></span>
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
                  <span class="trend-card-title">{$t('dashboard.trendCards.balanceTrend')} <InfoTip text={$t('infotips.balanceTrend')} position="bottom" /></span>
                  <span class="trend-card-period">{filteredRides.length} {$t('dashboard.units.rides')}</span>
                </div>
                {#if balanceTrend.path}
                  <div class="trend-chart" role="img" aria-label={$t('aria.balanceTrendChart')} bind:clientWidth={balanceChartWidth}>
                    <!-- Tooltip -->
                    {#if hoveredBalancePoint}
                      <div class="chart-tooltip" style="left: {tooltipX}px; top: {tooltipY}px;">
                        <span class="tooltip-date">{hoveredBalancePoint.date}</span>
                        <span class="tooltip-value {hoveredBalancePoint.status}">{hoveredBalancePoint.balance.toFixed(1)}% L</span>
                        <span class="tooltip-hint">{$t('dashboard.table.clickToView')}</span>
                      </div>
                    {/if}
                    <svg viewBox="0 0 {balanceChartWidth} {CHART_HEIGHT}" class="trend-svg">
                      <defs>
                        <linearGradient id="balanceGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" stop-color="var(--color-optimal)" stop-opacity="0.15"/>
                          <stop offset="100%" stop-color="var(--color-optimal)" stop-opacity="0"/>
                        </linearGradient>
                      </defs>
                      <!-- Center line (50%) -->
                      <line x1={CHART_PADDING} y1={CHART_HEIGHT / 2} x2={balanceChartWidth - CHART_PADDING} y2={CHART_HEIGHT / 2} stroke="var(--border-subtle)" stroke-width="1" stroke-dasharray="3,3" opacity="0.5"/>
                      <!-- Area fill -->
                      <path d={balanceTrend.areaPath} fill="url(#balanceGradient)"/>
                      <!-- Trend line -->
                      <path d={balanceTrend.path} fill="none" stroke="var(--color-optimal)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <!-- Data points -->
                      {#each balanceTrend.points as point}
                        <circle
                          cx={point.x}
                          cy={point.y}
                          r={hoveredBalancePoint === point ? 5 : 4}
                          fill={point.status === 'optimal' ? 'var(--color-optimal)' : point.status === 'attention' ? 'var(--color-attention)' : 'var(--color-problem)'}
                          stroke="var(--bg-surface)"
                          stroke-width="2"
                          class="chart-point"
                          role="button"
                          tabindex="0"
                          on:mouseenter={(e) => handleBalancePointHover(point, e)}
                          on:mouseleave={() => handleBalancePointHover(null)}
                          on:click={() => navigateToRide(point.rideId)}
                          on:keypress={(e) => e.key === 'Enter' && navigateToRide(point.rideId)}
                        />
                      {/each}
                    </svg>
                  </div>
                  <div class="trend-stats">
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">{$t('dashboard.trendStats.current')} <InfoTip text={$t('infotips.trendCurrent')} position="bottom" size="sm" /></span>
                      <span class="trend-stat-value {getBalanceStatus(balanceTrend.points[balanceTrend.points.length - 1]?.balance || 50)}">{(balanceTrend.points[balanceTrend.points.length - 1]?.balance || 50).toFixed(1)}%</span>
                    </div>
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">{$t('dashboard.trendStats.average')} <InfoTip text={$t('infotips.trendAverage')} position="bottom" size="sm" /></span>
                      <span class="trend-stat-value">{((balanceTrend.minBalance + balanceTrend.maxBalance) / 2).toFixed(1)}%</span>
                    </div>
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">{$t('dashboard.trendStats.spread')} <InfoTip text={$t('infotips.trendSpread')} position="bottom" size="sm" /></span>
                      <span class="trend-stat-value {(balanceTrend.maxBalance - balanceTrend.minBalance) > 5 ? 'problem' : (balanceTrend.maxBalance - balanceTrend.minBalance) > 2.5 ? 'attention' : 'optimal'}">{(balanceTrend.maxBalance - balanceTrend.minBalance).toFixed(1)}%</span>
                    </div>
                  </div>
                {:else}
                  <div class="trend-empty">{$t('dashboard.emptyStates.needMoreRides')}</div>
                {/if}
              </div>

              <!-- Technique Trend -->
              <div class="trend-card">
                <div class="trend-card-header">
                  <span class="trend-card-title">{$t('dashboard.trendCards.technique')} <InfoTip text={$t('infotips.techniqueTrend')} position="bottom" /></span>
                  <div class="trend-metric-switcher">
                    <button class:active={activeTrendMetric === 'asymmetry'} on:click={() => activeTrendMetric = 'asymmetry'}>{$t('dashboard.trendCards.asym')}</button>
                    <button class:active={activeTrendMetric === 'te'} on:click={() => activeTrendMetric = 'te'}>{$t('metrics.teShort')}</button>
                    <button class:active={activeTrendMetric === 'ps'} on:click={() => activeTrendMetric = 'ps'}>{$t('metrics.psShort')}</button>
                  </div>
                </div>
                {#if techniqueTrend.path}
                  <div class="trend-chart" role="img" aria-label={$t('aria.techniqueTrendChart')} bind:clientWidth={techniqueChartWidth}>
                    <!-- Tooltip -->
                    {#if hoveredTechniquePoint}
                      <div class="chart-tooltip" style="left: {tooltipX}px; top: {tooltipY}px;">
                        <span class="tooltip-date">{hoveredTechniquePoint.date}</span>
                        <span class="tooltip-value {hoveredTechniquePoint.status}">{hoveredTechniquePoint.value.toFixed(1)}%</span>
                      </div>
                    {/if}
                    <svg viewBox="0 0 {techniqueChartWidth} {CHART_HEIGHT}" class="trend-svg">
                      <defs>
                        <linearGradient id="techniqueGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" stop-color={activeTrendMetric === 'asymmetry' ? 'var(--color-accent)' : activeTrendMetric === 'te' ? 'var(--color-optimal)' : 'var(--color-attention)'} stop-opacity="0.15"/>
                          <stop offset="100%" stop-color={activeTrendMetric === 'asymmetry' ? 'var(--color-accent)' : activeTrendMetric === 'te' ? 'var(--color-optimal)' : 'var(--color-attention)'} stop-opacity="0"/>
                        </linearGradient>
                      </defs>
                      <!-- Center line -->
                      <line x1={CHART_PADDING} y1={CHART_HEIGHT / 2} x2={techniqueChartWidth - CHART_PADDING} y2={CHART_HEIGHT / 2} stroke="var(--border-subtle)" stroke-width="1" stroke-dasharray="3,3" opacity="0.5"/>
                      <!-- Area fill -->
                      <path d={techniqueTrend.areaPath} fill="url(#techniqueGradient)"/>
                      <!-- Trend line -->
                      <path d={techniqueTrend.path} fill="none" stroke={getTrendColor(activeTrendMetric)} stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <!-- Data points -->
                      {#each techniqueTrend.points as point}
                        <circle
                          cx={point.x}
                          cy={point.y}
                          r={hoveredTechniquePoint === point ? 5 : 4}
                          fill={point.status === 'optimal' ? 'var(--color-optimal)' : point.status === 'attention' ? 'var(--color-attention)' : 'var(--color-problem)'}
                          stroke="var(--bg-surface)"
                          stroke-width="2"
                          class="chart-point"
                          role="button"
                          tabindex="0"
                          on:mouseenter={(e) => handleTechniquePointHover(point, e)}
                          on:mouseleave={() => handleTechniquePointHover(null)}
                        />
                      {/each}
                    </svg>
                  </div>
                  <div class="trend-stats">
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">{$t('dashboard.trendStats.latest')} <InfoTip text={$t('infotips.trendLatest')} position="bottom" size="sm" /></span>
                      <span class="trend-stat-value {activeTrendMetric === 'asymmetry' ? getAsymmetryClass(techniqueTrend.points[techniqueTrend.points.length - 1]?.value || 0) : ''}">{(techniqueTrend.points[techniqueTrend.points.length - 1]?.value || 0).toFixed(1)}%</span>
                    </div>
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">{$t('dashboard.trendStats.average')} <InfoTip text={$t('infotips.trendAverage')} position="bottom" size="sm" /></span>
                      <span class="trend-stat-value">{(techniqueTrend.points.reduce((s, p) => s + p.value, 0) / techniqueTrend.points.length).toFixed(1)}%</span>
                    </div>
                    <div class="trend-stat-item">
                      <span class="trend-stat-label">{$t('dashboard.trendStats.best')} <InfoTip text={$t('infotips.trendBest')} position="bottom" size="sm" /></span>
                      <span class="trend-stat-value optimal">{(activeTrendMetric === 'asymmetry' ? Math.min(...techniqueTrend.points.map(p => p.value)) : Math.max(...techniqueTrend.points.map(p => p.value))).toFixed(1)}%</span>
                    </div>
                  </div>
                {:else}
                  <div class="trend-empty">{$t('dashboard.emptyStates.needMoreData')}</div>
                {/if}
              </div>
            </div>
          {/if}

          <!-- Progress Row (if we have comparison data) -->
          {#if prevPeriodStats && periodStats}
            <div class="progress-row-card animate-in">
              <div class="progress-header">
                <span class="progress-title">{$t('dashboard.progress.title', { values: { period: selectedPeriod } })}</span>
              </div>
              <div class="progress-items">
                {#if scoreProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">{$t('dashboard.progress.score')}</span>
                    <span class="progress-item-now">{periodStats.score.toFixed(0)}</span>
                    <span class="progress-item-change {scoreProgress.direction}">{scoreProgress.direction === 'up' ? '↑' : scoreProgress.direction === 'down' ? '↓' : ''}{scoreProgress.value.toFixed(0)}</span>
                  </div>
                {/if}
                {#if optimalProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">{$t('dashboard.progress.optimalZone')}</span>
                    <span class="progress-item-now">{periodStats.zoneOptimal.toFixed(0)}%</span>
                    <span class="progress-item-change {optimalProgress.direction}">{optimalProgress.direction === 'up' ? '↑' : optimalProgress.direction === 'down' ? '↓' : ''}{optimalProgress.value.toFixed(0)}%</span>
                  </div>
                {/if}
                {#if teProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">{$t('metrics.teShort')}</span>
                    <span class="progress-item-now">{periodStats.te.toFixed(0)}%</span>
                    <span class="progress-item-change {teProgress.direction}">{teProgress.direction === 'up' ? '↑' : teProgress.direction === 'down' ? '↓' : ''}{teProgress.value.toFixed(0)}</span>
                  </div>
                {/if}
                {#if psProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">{$t('metrics.psShort')}</span>
                    <span class="progress-item-now">{periodStats.ps.toFixed(0)}%</span>
                    <span class="progress-item-change {psProgress.direction}">{psProgress.direction === 'up' ? '↑' : psProgress.direction === 'down' ? '↓' : ''}{psProgress.value.toFixed(0)}</span>
                  </div>
                {/if}
                {#if durationProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">{$t('dashboard.progress.time')}</span>
                    <span class="progress-item-now">{(periodStats.duration / 3600000).toFixed(1)}h</span>
                    <span class="progress-item-change {durationProgress.direction}">{durationProgress.direction === 'up' ? '↑' : durationProgress.direction === 'down' ? '↓' : ''}{durationProgress.value.toFixed(1)}h</span>
                  </div>
                {/if}
                {#if distanceProgress}
                  <div class="progress-item">
                    <span class="progress-item-label">{$t('dashboard.progress.distance')}</span>
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
                <span class="card-title">{$t('dashboard.recentRides')} <InfoTip text={$t('infotips.recentRides')} position="bottom" /></span>
                <a href="/rides" class="view-all">{$t('dashboard.viewAll')}</a>
              </div>
              <div class="rides-table-wrap">
                <table class="rides-table">
                  <thead>
                    <tr>
                      <th class="col-date">{$t('dashboard.table.date')}</th>
                      <th class="col-duration">{$t('dashboard.table.duration')}</th>
                      <th class="col-asymmetry">{$t('dashboard.table.asym')} <InfoTip text={$t('infotips.tableAsym')} position="bottom" size="sm" /></th>
                      <th class="col-balance">{$t('metrics.leftRight')}</th>
                      <th class="col-te">{$t('metrics.teShort')} <InfoTip text={$t('infotips.te')} position="bottom" size="sm" /></th>
                      <th class="col-ps">{$t('metrics.psShort')} <InfoTip text={$t('infotips.ps')} position="bottom" size="sm" /></th>
                      <th class="col-zones">{$t('dashboard.table.zones')} <InfoTip text={$t('infotips.tableZones')} position="bottom" size="sm" /></th>
                      {#if recentRides.some(r => r.power_avg > 0)}<th class="col-power">{$t('dashboard.table.power')}</th>{/if}
                    </tr>
                  </thead>
                  <tbody>
                    {#each recentRides.slice(0, 5) as ride}
                      <tr on:click={() => window.location.href = `/rides/${ride.id}`}>
                        <td class="col-date">
                          <span class="date-primary">{formatDate(ride.timestamp)}</span>
                          <span class="date-secondary">{formatRelativeTime(ride.timestamp, { today: $t('common.today'), yesterday: $t('common.yesterday'), daysAgo: $t('common.daysAgo') }, $locale)}</span>
                        </td>
                        <td class="col-duration">{formatDuration(ride.duration_ms)}</td>
                        <td class="col-asymmetry">
                          <span class="asymmetry-value {getBalanceStatus(ride.balance_left)}">{Math.abs(ride.balance_left - 50).toFixed(1)}%</span>
                          <span class="dominance">{ride.balance_left > 50 ? $t('metrics.left') : ride.balance_left < 50 ? $t('metrics.right') : ''}</span>
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
          <div class="empty-state-new animate-in">
            <!-- Welcome Section -->
            <div class="welcome-section">
              <div class="welcome-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
                </svg>
              </div>
              <h2>{$t('dashboard.welcome', { values: { name: getFirstName($user?.name) } })}</h2>
              <p>{$t('dashboard.onboarding.dashboardWillShow')}</p>
            </div>

            <!-- Preview of what they'll see -->
            <div class="preview-section">
              <h3>{$t('dashboard.onboarding.whatYouWillSee')}</h3>
              <div class="preview-cards">
                <div class="preview-card">
                  <div class="preview-icon balance-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/></svg>
                  </div>
                  <div class="preview-info">
                    <span class="preview-label">{$t('dashboard.onboarding.powerBalance')}</span>
                    <span class="preview-example">{$t('dashboard.onboarding.exampleBalance')}</span>
                  </div>
                  <span class="preview-target">{$t('dashboard.onboarding.proTarget')}</span>
                </div>
                <div class="preview-card">
                  <div class="preview-icon te-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
                  </div>
                  <div class="preview-info">
                    <span class="preview-label">{$t('dashboard.onboarding.torqueEffectiveness')}</span>
                    <span class="preview-example">{$t('dashboard.onboarding.exampleTe')}</span>
                  </div>
                  <span class="preview-target">{$t('dashboard.onboarding.teOptimal')}</span>
                </div>
                <div class="preview-card">
                  <div class="preview-icon ps-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v8M8 12h8"/></svg>
                  </div>
                  <div class="preview-info">
                    <span class="preview-label">{$t('dashboard.onboarding.pedalSmoothness')}</span>
                    <span class="preview-example">{$t('dashboard.onboarding.examplePs')}</span>
                  </div>
                  <span class="preview-target">{$t('dashboard.onboarding.psOptimal')}</span>
                </div>
              </div>
            </div>

            <!-- Setup Steps -->
            <div class="setup-section">
              <h3>{$t('dashboard.onboarding.getStarted')}</h3>
              <div class="setup-steps-new">
                <div class="setup-step-new">
                  <div class="step-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                  </div>
                  <div class="step-content">
                    <span class="step-title">{$t('dashboard.onboarding.step1Title')}</span>
                    <span class="step-desc">{$t('dashboard.onboarding.step1Desc')}</span>
                  </div>
                  <a href="https://github.com/kpedal/kpedal/releases/latest" target="_blank" rel="noopener" class="step-action">
                    {$t('dashboard.actions.downloadApk')}
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                  </a>
                </div>
                <div class="setup-step-new">
                  <div class="step-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>
                  </div>
                  <div class="step-content">
                    <span class="step-title">{$t('dashboard.onboarding.step2Title')}</span>
                    <span class="step-desc">{$t('dashboard.onboarding.step2Desc')}</span>
                  </div>
                </div>
                <div class="setup-step-new">
                  <div class="step-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 0 1-9 9m9-9a9 9 0 0 0-9-9m9 9H3m9 9a9 9 0 0 1-9-9m9 9c-1.657 0-3-4.03-3-9s1.343-9 3-9m0 18c1.657 0 3-4.03 3-9s-1.343-9-3-9m-9 9a9 9 0 0 1 9-9"/></svg>
                  </div>
                  <div class="step-content">
                    <span class="step-title">{$t('dashboard.onboarding.step3Title')}</span>
                    <span class="step-desc">{$t('dashboard.onboarding.step3Desc')}</span>
                  </div>
                  <a href="/link" class="step-action">
                    {$t('dashboard.actions.linkDevice')}
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                  </a>
                </div>
              </div>
            </div>

            <!-- Already have device linked? -->
            <div class="sync-hint">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
              <span>{$t('dashboard.onboarding.alreadyInstalled')}</span>
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

  .landing-header {
    /* Semantic wrapper for fixed header elements */
    display: contents;
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

  .header-actions {
    position: fixed;
    top: 32px;
    right: 32px;
    display: flex;
    align-items: center;
    gap: 12px;
    z-index: 100;
  }

  .landing .lang-select {
    appearance: none;
    height: 36px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    padding: 0 28px 0 12px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s ease;
    backdrop-filter: blur(12px);
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpolyline points='6,9 12,15 18,9'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 8px center;
  }

  .landing .lang-select:hover {
    border-color: var(--border-default);
    color: var(--text-primary);
  }

  .landing .lang-select:focus {
    outline: none;
    border-color: var(--color-accent);
  }

  .landing .lang-select option {
    background: var(--bg-surface);
    color: var(--text-primary);
  }

  .theme-toggle {
    position: relative;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s ease;
    backdrop-filter: blur(12px);
  }
  .theme-toggle:hover {
    border-color: var(--border-default);
    color: var(--text-secondary);
  }
  .auto-badge {
    position: absolute;
    bottom: -4px;
    right: -4px;
    width: 14px;
    height: 14px;
    background: #22c55e;
    color: #fff;
    font-size: 9px;
    font-weight: 900;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    box-shadow: 0 1px 3px rgba(0,0,0,0.4);
    border: 1.5px solid var(--bg-base);
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
    text-decoration: none;
    transition: border-color 0.15s, color 0.15s;
  }
  .hero-version:hover {
    border-color: var(--color-optimal);
    color: var(--color-optimal);
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
    background: var(--color-optimal);
    color: white;
    font-size: 15px;
    font-weight: 500;
    border-radius: 100px;
    border: none;
    cursor: pointer;
    transition: all 0.2s ease;
    text-decoration: none;
    box-shadow: 0 2px 8px var(--glow-optimal, rgba(34, 197, 94, 0.3));
  }
  .hero-cta-secondary:hover {
    filter: brightness(1.1);
    transform: translateY(-1px);
    box-shadow: 0 4px 14px var(--glow-optimal, rgba(34, 197, 94, 0.4));
  }

  :global([data-theme="dark"]) .hero-cta-secondary {
    background: #16a34a;
    box-shadow: 0 2px 8px rgba(22, 163, 74, 0.25);
  }
  :global([data-theme="dark"]) .hero-cta-secondary:hover {
    background: #15803d;
    box-shadow: 0 4px 14px rgba(22, 163, 74, 0.35);
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
    background: var(--color-optimal);
    border: none;
    color: white;
    box-shadow: 0 2px 8px var(--glow-optimal, rgba(34, 197, 94, 0.3));
  }
  .cta-btn.secondary:hover {
    filter: brightness(1.1);
    transform: translateY(-1px);
    box-shadow: 0 4px 14px var(--glow-optimal, rgba(34, 197, 94, 0.4));
  }
  :global([data-theme="dark"]) .cta-btn.secondary {
    background: #16a34a;
    box-shadow: 0 2px 8px rgba(22, 163, 74, 0.25);
  }
  :global([data-theme="dark"]) .cta-btn.secondary:hover {
    background: #15803d;
    box-shadow: 0 4px 14px rgba(22, 163, 74, 0.35);
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
    padding: 20px;
  }
  .dash-header-preview {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  .dash-greeting-preview {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .period-selector-preview {
    display: flex;
    gap: 4px;
  }
  .period-btn-preview {
    font-size: 10px;
    padding: 4px 8px;
    background: var(--bg-base);
    border-radius: 6px;
    color: var(--text-muted);
  }
  .period-btn-preview.active {
    background: var(--text-primary);
    color: var(--bg-base);
  }
  .hero-stats-preview {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
    margin-bottom: 14px;
  }
  .hero-stat-preview {
    background: var(--bg-base);
    border-radius: 10px;
    padding: 12px;
  }
  .asymmetry-preview {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .hero-value {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .hero-value.optimal { color: var(--color-optimal); }
  .hero-label {
    font-size: 9px;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .hero-detail {
    font-size: 10px;
    color: var(--text-tertiary);
  }
  .balance-bar-preview {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 100%;
  }
  .balance-side-preview {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .balance-visual-preview {
    flex: 1;
    height: 8px;
    display: flex;
    border-radius: 4px;
    overflow: hidden;
  }
  .balance-fill-left {
    background: var(--color-optimal);
    height: 100%;
  }
  .balance-fill-right {
    background: var(--color-optimal);
    opacity: 0.5;
    height: 100%;
  }
  .zones-preview {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 6px;
  }
  .zone-bars-preview {
    display: flex;
    height: 8px;
    border-radius: 4px;
    overflow: hidden;
  }
  .zone-bar.optimal { background: var(--color-optimal); }
  .zone-bar.attention { background: var(--color-attention); }
  .zone-bar.problem { background: var(--color-problem); }
  .summary-preview {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
  }
  .summary-row {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }
  .summary-row .summary-num {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .summary-row .summary-unit {
    font-size: 10px;
    color: var(--text-muted);
  }
  .main-grid-preview {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    align-items: stretch;
  }
  .main-grid-preview .grid-card-preview {
    display: flex;
    flex-direction: column;
  }
  .grid-card-preview {
    background: var(--bg-base);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
  }
  .card-header-preview {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-primary);
  }
  /* Recent Rides Card */
  .recent-rides-preview {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .ride-row-preview {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 10px;
    background: var(--bg-elevated);
    border-radius: 8px;
  }
  .ride-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .ride-title {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .ride-meta {
    font-size: 9px;
    color: var(--text-muted);
  }
  .ride-score {
    font-size: 14px;
    font-weight: 700;
    padding: 4px 8px;
    border-radius: 6px;
  }
  .ride-score.optimal {
    color: var(--color-optimal);
    background: var(--color-optimal-soft);
  }
  .ride-score.attention {
    color: var(--color-attention);
    background: var(--color-attention-soft);
  }
  /* Progress Card */
  .progress-card {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }
  .progress-content {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 14px;
  }
  .progress-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  .progress-stats {
    display: flex;
    flex-direction: column;
    gap: 1px;
  }
  .progress-value {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-optimal);
    line-height: 1.1;
  }
  .progress-label {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .progress-period {
    font-size: 9px;
    color: var(--text-muted);
  }
  .progress-detail {
    display: flex;
    gap: 16px;
    padding-top: 12px;
    border-top: 1px solid var(--border-subtle);
  }
  .progress-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .progress-item-label {
    font-size: 9px;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .progress-item-value {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .progress-item-value .delta {
    font-size: 10px;
    font-weight: 500;
  }
  /* Technique Card - L/R Comparison */
  .technique-card .technique-preview {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 14px;
  }
  .technique-metric-lr {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .metric-name {
    font-size: 10px;
    font-weight: 600;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .lr-comparison {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .leg {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .leg-label {
    font-size: 10px;
    font-weight: 600;
    color: var(--text-muted);
    width: 12px;
  }
  .leg-bar-wrap {
    flex: 1;
    height: 6px;
    background: var(--bg-elevated);
    border-radius: 3px;
    overflow: hidden;
  }
  .leg-bar {
    height: 100%;
    border-radius: 3px;
    transition: width 0.3s ease;
  }
  .leg-bar.te {
    background: linear-gradient(90deg, var(--color-optimal) 0%, #4ade80 100%);
  }
  .leg-bar.ps {
    background: linear-gradient(90deg, #60a5fa 0%, #3b82f6 100%);
  }
  .leg-value {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-primary);
    width: 28px;
    text-align: right;
  }
  .card-subtitle-preview {
    font-weight: 500;
    color: var(--text-tertiary);
    font-size: 10px;
  }
  .technique-bar-preview {
    height: 8px;
    background: var(--bg-elevated);
    border-radius: 4px;
    position: relative;
    overflow: hidden;
  }
  .technique-bar-preview .optimal-zone {
    position: absolute;
    height: 100%;
    opacity: 0.15;
    border-left: 1px dashed var(--color-optimal);
    border-right: 1px dashed var(--color-optimal);
  }
  .technique-bar-preview .optimal-zone.te {
    left: 70%;
    width: 10%;
    background: var(--color-optimal);
  }
  .technique-bar-preview .optimal-zone.ps {
    left: 50%;
    width: 50%;
    background: var(--color-optimal);
  }
  .technique-bar-preview .fill {
    height: 100%;
    border-radius: 4px;
    transition: width 0.3s ease;
  }
  .technique-bar-preview .fill.te { background: linear-gradient(90deg, var(--color-optimal) 0%, var(--color-optimal) 100%); }
  .technique-bar-preview .fill.ps { background: linear-gradient(90deg, var(--color-optimal) 0%, var(--color-optimal) 100%); }
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
  .pedals-compatibility {
    margin-top: 40px;
    padding-top: 32px;
    border-top: 1px solid var(--border-subtle);
  }
  .pedals-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
  .pedals-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 20px;
    text-align: left;
  }
  .pedals-card.full {
    border-color: var(--color-optimal);
    border-width: 2px;
  }
  .pedals-card.balance {
    border-color: var(--color-attention);
  }
  .pedals-card.incompatible {
    opacity: 0.7;
  }
  .pedals-card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }
  .pedals-icon {
    font-size: 16px;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
  }
  .pedals-card.full .pedals-icon {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }
  .pedals-card.balance .pedals-icon {
    background: var(--color-attention-soft);
    color: var(--color-attention);
  }
  .pedals-card.incompatible .pedals-icon {
    background: var(--bg-elevated);
    color: var(--text-muted);
  }
  .pedals-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .pedals-desc {
    font-size: 12px;
    color: var(--text-tertiary);
    margin: 0 0 12px 0;
  }
  .pedals-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  .pedal-chip {
    font-size: 12px;
    padding: 4px 10px;
    background: var(--bg-elevated);
    border-radius: 12px;
    color: var(--text-secondary);
    white-space: nowrap;
  }
  .pedals-card.full .pedal-chip {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }
  .pedals-card.balance .pedal-chip {
    background: var(--color-attention-soft);
    color: var(--color-attention-text);
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
    max-width: 800px;
    margin: 0 auto;
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
    padding: 14px 22px 18px;
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
  .footer-bottom p {
    font-size: 13px;
    color: var(--text-muted);
    margin: 0;
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
    overflow: visible;
    position: relative;
  }
  .technique-metric-fill {
    position: absolute;
    top: 0;
    left: 0;
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
    background: var(--color-optimal);
    opacity: 0.15;
    border-radius: 5px;
    pointer-events: none;
  }
  .technique-optimal-zone.te {
    left: 70%;
    right: 20%;
  }
  .technique-optimal-zone.ps {
    left: 50%;
    right: 0;
  }
  .technique-bar-marker {
    position: absolute;
    top: -3px;
    bottom: -3px;
    width: 2px;
    background: var(--color-optimal);
    opacity: 0.7;
    border-radius: 1px;
  }
  .technique-bar-labels {
    display: flex;
    justify-content: space-between;
    font-size: 9px;
    color: var(--text-muted);
    margin-top: 3px;
    position: relative;
  }
  .technique-bar-labels .optimal-label {
    position: absolute;
    transform: translateX(-50%);
    color: var(--color-optimal-text);
    font-weight: 600;
  }
  .technique-bar-labels.te .optimal-label { left: 75%; }
  .technique-bar-labels.ps .optimal-label { left: 75%; }
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
    min-height: 28px;
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
    position: relative;
    overflow: visible;
  }
  .trend-svg {
    width: 100%;
    height: 100%;
    display: block;
  }
  .chart-point {
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .chart-point:hover {
    filter: drop-shadow(0 0 4px currentColor);
  }
  .chart-tooltip {
    position: absolute;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    padding: 10px 14px;
    pointer-events: none;
    z-index: 100;
    display: flex;
    flex-direction: column;
    gap: 4px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
    transform: translateX(-50%);
    white-space: nowrap;
    backdrop-filter: blur(8px);
  }
  .tooltip-date {
    font-size: 11px;
    font-weight: 500;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .tooltip-value {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }
  .tooltip-value.optimal { color: var(--color-optimal-text); }
  .tooltip-value.attention { color: var(--color-attention-text); }
  .tooltip-value.problem { color: var(--color-problem-text); }
  .tooltip-hint {
    font-size: 10px;
    color: var(--text-muted);
    opacity: 0.8;
    margin-top: 2px;
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
  .period-btn:hover:not(:disabled) { color: var(--text-primary); background: var(--bg-hover); }
  .period-btn.active {
    background: var(--color-accent);
    color: var(--color-accent-text);
    font-weight: 600;
  }
  .period-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
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

  /* ============================================
     NEW EMPTY STATE - Enhanced onboarding
     ============================================ */
  .empty-state-new {
    max-width: 720px;
    margin: 0 auto;
    padding: 40px 24px 60px;
  }

  .welcome-section {
    text-align: center;
    margin-bottom: 48px;
  }
  .welcome-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    color: var(--color-accent);
  }
  .welcome-section h2 {
    font-size: 26px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
    letter-spacing: -0.3px;
  }
  .welcome-section p {
    font-size: 15px;
    color: var(--text-secondary);
    max-width: 400px;
    margin: 0 auto;
    line-height: 1.5;
  }

  .preview-section {
    margin-bottom: 40px;
  }
  .preview-section h3,
  .setup-section h3 {
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-muted);
    margin-bottom: 16px;
    text-align: center;
  }
  .preview-cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
  .preview-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .preview-icon {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .preview-icon svg {
    width: 20px;
    height: 20px;
  }
  .preview-icon.balance-icon {
    background: var(--color-accent-soft);
    color: var(--color-accent);
  }
  .preview-icon.te-icon {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }
  .preview-icon.ps-icon {
    background: var(--color-attention-soft);
    color: var(--color-attention-text);
  }
  .preview-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .preview-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .preview-example {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
    font-variant-numeric: tabular-nums;
  }
  .preview-target {
    font-size: 11px;
    color: var(--text-tertiary);
    padding-top: 8px;
    border-top: 1px solid var(--border-subtle);
  }

  .setup-section {
    margin-bottom: 32px;
  }
  .setup-steps-new {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .setup-step-new {
    display: flex;
    align-items: center;
    gap: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px 20px;
    transition: border-color 0.15s;
  }
  .setup-step-new:hover {
    border-color: var(--border-default);
  }
  .step-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    background: var(--bg-elevated);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-secondary);
    flex-shrink: 0;
  }
  .step-icon svg {
    width: 22px;
    height: 22px;
  }
  .step-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .step-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .step-desc {
    font-size: 13px;
    color: var(--text-tertiary);
  }
  .step-action {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    background: var(--color-accent);
    color: var(--color-accent-text);
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    text-decoration: none;
    transition: all 0.15s;
    flex-shrink: 0;
  }
  .step-action:hover {
    background: var(--color-accent-hover);
    transform: translateY(-1px);
  }

  .sync-hint {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 14px 20px;
    background: var(--bg-elevated);
    border-radius: 10px;
    font-size: 13px;
    color: var(--text-secondary);
  }
  .sync-hint svg {
    flex-shrink: 0;
    color: var(--text-muted);
  }

  @media (max-width: 640px) {
    .empty-state-new {
      padding: 24px 16px 40px;
    }
    .welcome-section {
      margin-bottom: 32px;
    }
    .welcome-section h2 {
      font-size: 22px;
    }
    .preview-cards {
      grid-template-columns: 1fr;
      gap: 10px;
    }
    .preview-card {
      flex-direction: row;
      align-items: center;
      padding: 14px;
    }
    .preview-info {
      flex: 1;
    }
    .preview-example {
      font-size: 16px;
    }
    .preview-target {
      border-top: none;
      border-left: 1px solid var(--border-subtle);
      padding: 0 0 0 12px;
      margin-left: auto;
    }
    .setup-step-new {
      flex-wrap: wrap;
      padding: 14px;
      gap: 12px;
    }
    .step-action {
      width: 100%;
      justify-content: center;
      padding: 10px 14px;
    }
    .sync-hint {
      flex-direction: column;
      text-align: center;
      gap: 6px;
    }
  }

  /* ============================================
     INSIGHTS - Inside Weekly Activity Card
     ============================================ */
  .insights-section {
    margin-top: auto;
    padding-top: 12px;
  }
  .insights-list {
    display: flex;
    flex-direction: column;
  }
  .insight-item {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    padding: 6px 0;
    font-size: 11px;
    line-height: 1.4;
  }
  .insight-item:not(:last-child) {
    border-bottom: 1px solid var(--border-subtle);
    padding-bottom: 8px;
  }

  .insight-icon {
    font-size: 13px;
    flex-shrink: 0;
    line-height: 1.4;
  }
  .insight-text {
    flex: 1;
    color: var(--text-secondary);
    line-height: 1.4;
  }
  .insight-link {
    color: var(--color-accent);
    text-decoration: none;
    font-weight: 500;
  }
  .insight-link:hover {
    text-decoration: underline;
  }

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
    .header-actions {
      top: 16px;
      right: 16px;
      gap: 8px;
    }
    .landing .lang-select {
      height: 34px;
      padding: 0 26px 0 10px;
      font-size: 12px;
    }
    .theme-toggle {
      width: 34px;
      height: 34px;
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
    .trends-row { grid-template-columns: 1fr 1fr; gap: 10px; }
    .trend-card { padding: 10px; }
    .trend-card-header { flex-wrap: nowrap; min-height: 28px; }
    .trend-card-title { font-size: 13px; white-space: nowrap; }
    .trend-card-title :global(.info-tip) { display: none; }
    .trend-chart { margin-bottom: 8px; }
    .chart-tooltip { padding: 6px 10px; border-radius: 8px; }
    .tooltip-value { font-size: 14px; }
    .tooltip-hint { display: none; }
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
    .hero-stats-preview { grid-template-columns: 1fr 1fr; gap: 10px; }
    .main-grid-preview { grid-template-columns: 1fr; gap: 12px; }
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
    .pedals-grid { grid-template-columns: 1fr; gap: 12px; }
    .pedals-card { padding: 16px; }
    .pedals-title { font-size: 14px; }
    .pedal-chip { font-size: 11px; padding: 3px 8px; }

    /* FAQ */
    .faq-item summary {
      padding: 16px 18px;
      font-size: 14px;
      min-height: 48px;
    }
    .faq-item p { padding: 12px 18px 16px; font-size: 14px; }

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
     RESPONSIVE: 480px - Mobile phones (Apple-style)
     ============================================ */
  @media (max-width: 480px) {
    /* Base layout - Apple-style generous spacing */
    .landing {
      padding: 20px;
      padding-bottom: 40px;
    }
    .landing-container {
      padding: 40px 0 60px;
    }

    /* Fixed elements - refined */
    .site-logo {
      top: 16px;
      left: 20px;
      gap: 8px;
    }
    .site-logo-dot {
      width: 8px;
      height: 8px;
      box-shadow: 0 0 8px var(--color-optimal);
    }
    .site-logo-text {
      font-size: 15px;
      font-weight: 600;
      letter-spacing: -0.3px;
    }
    .header-actions {
      top: 16px;
      right: 20px;
      gap: 6px;
    }
    .landing .lang-select {
      height: 32px;
      padding: 0 24px 0 10px;
      font-size: 12px;
    }
    .theme-toggle {
      width: 32px;
      height: 32px;
    }
    .theme-toggle svg {
      width: 16px;
      height: 16px;
    }

    /* Hero - Apple-style bold typography */
    .hero {
      padding: 32px 0 48px;
      text-align: center;
    }
    .hero-eyebrow {
      font-size: 12px;
      margin-bottom: 20px;
      letter-spacing: 1.2px;
      opacity: 0.7;
    }
    .hero-version {
      margin-left: 8px;
      padding: 3px 8px;
      font-size: 11px;
      border-radius: 6px;
      background: var(--bg-elevated);
      text-decoration: none;
    }
    .hero-title {
      font-size: 36px;
      letter-spacing: -1.5px;
      line-height: 1.1;
      font-weight: 600;
    }
    .hero-title-line {
      display: block;
    }
    .hero-subtitle {
      font-size: 15px;
      max-width: 320px;
      margin: 0 auto 32px;
      line-height: 1.6;
      color: var(--text-secondary);
    }
    .hero-actions {
      flex-direction: column;
      gap: 12px;
      width: 100%;
      max-width: 300px;
      margin: 0 auto;
    }
    .hero-cta, .hero-cta-secondary {
      width: 100%;
      padding: 16px 24px;
      font-size: 16px;
      font-weight: 500;
      justify-content: center;
      border-radius: 14px;
      min-height: 54px;
    }
    .hero-cta {
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
    }
    .hero-cta-secondary {
      box-shadow: 0 4px 14px var(--glow-optimal, rgba(34, 197, 94, 0.25));
    }
    .hero-note {
      font-size: 13px;
      margin-top: 24px;
      opacity: 0.7;
    }

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
    .pedals-compatibility { margin-top: 24px; padding-top: 20px; }
    .pedals-grid { gap: 10px; }
    .pedals-card { padding: 14px; border-radius: 10px; }
    .pedals-title { font-size: 13px; }
    .pedals-desc { font-size: 11px; margin-bottom: 10px; }
    .pedal-chip { font-size: 10px; padding: 3px 7px; }

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
      padding: 10px 16px 14px;
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
    .footer-bottom p { text-align: center; }
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
    .theme-toggle { top: 12px; right: 12px; width: 32px; height: 32px; }

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

    .pedal-chip { font-size: 10px; }

    .faq-item summary { font-size: 12px; padding: 12px 14px; }
    .faq-item p { font-size: 12px; padding: 10px 14px 12px; }

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
