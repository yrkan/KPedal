<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { isAuthenticated, authFetch } from '$lib/auth';
  import InfoTip from '$lib/components/InfoTip.svelte';

  interface Snapshot {
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

  let ride: Ride | null = null;
  let loading = true;
  let error: string | null = null;
  let activeChart: 'balance' | 'te' | 'ps' | 'power' | 'hr' = 'balance';

  $: rideId = $page.params.id;
  $: chartData = ride?.snapshots ? getChartData(ride.snapshots, activeChart) : { points: '', areaPath: '', min: 0, max: 100 };
  $: hasPerformanceData = ride && (ride.power_avg > 0 || ride.hr_avg > 0 || ride.cadence_avg > 0);
  $: hasElevationData = ride && (ride.elevation_gain > 0 || ride.elevation_loss > 0);
  $: fatigueData = ride?.snapshots?.length >= 6 ? calculateFatigue(ride.snapshots) : null;
  $: techniqueStats = ride ? calculateTechniqueStats(ride) : null;

  onMount(async () => {
    if (!$isAuthenticated) {
      goto('/login');
      return;
    }
    await loadRide();
  });

  async function loadRide() {
    loading = true;
    error = null;
    try {
      const res = await authFetch(`/rides/${rideId}`);
      if (res.ok) {
        const data = await res.json();
        if (data.success) ride = data.data;
        else error = data.error || 'Failed to load ride';
      } else if (res.status === 404) {
        error = 'Ride not found';
      } else {
        error = 'Failed to load ride';
      }
    } catch {
      error = 'Failed to load ride';
    } finally {
      loading = false;
    }
  }

  function calculateFatigue(snapshots: Snapshot[]) {
    const third = Math.floor(snapshots.length / 3);
    const first = snapshots.slice(0, third);
    const last = snapshots.slice(-third);
    const avg = (arr: Snapshot[], fn: (s: Snapshot) => number) => arr.reduce((a, s) => a + fn(s), 0) / arr.length;

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

  function calculateTechniqueStats(ride: Ride) {
    // L-R symmetry (difference between legs)
    const teSymmetry = Math.abs(ride.te_left - ride.te_right);
    const psSymmetry = Math.abs(ride.ps_left - ride.ps_right);

    // Stability from snapshots (coefficient of variation)
    let teStability = 0;
    let psStability = 0;

    if (ride.snapshots?.length >= 3) {
      const teValues = ride.snapshots.map(s => (s.te_left + s.te_right) / 2);
      const psValues = ride.snapshots.map(s => (s.ps_left + s.ps_right) / 2);

      const teAvg = teValues.reduce((a, b) => a + b, 0) / teValues.length;
      const psAvg = psValues.reduce((a, b) => a + b, 0) / psValues.length;

      const teStdDev = Math.sqrt(teValues.reduce((sum, v) => sum + (v - teAvg) ** 2, 0) / teValues.length);
      const psStdDev = Math.sqrt(psValues.reduce((sum, v) => sum + (v - psAvg) ** 2, 0) / psValues.length);

      // Convert to stability score (100 - CV%). Lower variation = higher stability
      teStability = Math.max(0, 100 - (teStdDev / teAvg) * 100);
      psStability = Math.max(0, 100 - (psStdDev / psAvg) * 100);
    }

    // Time in optimal zone for TE (70-80%) and PS (>=20%)
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

    return {
      teSymmetry,
      psSymmetry,
      teStability,
      psStability,
      teOptimalTime,
      psOptimalTime
    };
  }

  function formatDuration(ms: number): string {
    const h = Math.floor(ms / 3600000);
    const m = Math.floor((ms % 3600000) / 60000);
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
  }

  function formatDate(ts: number): string {
    return new Date(ts).toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
  }

  function formatTime(ts: number): string {
    return new Date(ts).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
  }

  function getAsymmetry(bal: number): number { return Math.abs(bal - 50); }
  function getDominance(bal: number): string {
    if (Math.abs(bal - 50) < 0.5) return 'Balanced';
    return bal > 50 ? 'L dominant' : 'R dominant';
  }

  function getBalanceStatus(bal: number): string {
    const a = Math.abs(bal - 50);
    if (a <= 2.5) return 'optimal';
    if (a <= 5) return 'attention';
    return 'problem';
  }

  function getTeStatus(te: number): string {
    if (te >= 70 && te <= 80) return 'optimal';
    if (te >= 60 && te <= 85) return 'attention';
    return 'problem';
  }

  function getPsStatus(ps: number): string {
    if (ps >= 20) return 'optimal';
    if (ps >= 15) return 'attention';
    return 'problem';
  }

  function getScoreStatus(s: number): string {
    if (s >= 85) return 'optimal';
    if (s >= 70) return 'attention';
    return 'problem';
  }

  function getChartData(snapshots: Snapshot[], metric: string) {
    if (!snapshots?.length) return { points: '', areaPath: '', min: 0, max: 100 };
    const values = snapshots.map(s => {
      if (metric === 'balance') return Math.abs(s.balance_left - 50);
      if (metric === 'te') return (s.te_left + s.te_right) / 2;
      if (metric === 'ps') return (s.ps_left + s.ps_right) / 2;
      if (metric === 'power') return s.power_avg || 0;
      return s.hr_avg || 0;
    });
    const min = Math.min(...values), max = Math.max(...values);
    const range = max - min || 1, pad = range * 0.1;
    const cMin = Math.max(0, min - pad), cMax = max + pad, cRange = cMax - cMin;
    const w = 200, h = 50;
    const coords = values.map((v, i) => ({ x: 4 + (i / (values.length - 1)) * (w - 8), y: h - 4 - ((v - cMin) / cRange) * (h - 8) }));
    const points = coords.map(c => `${c.x},${c.y}`).join(' ');
    const areaPath = `M${coords[0].x},${h} L${coords.map(c => `${c.x},${c.y}`).join(' L')} L${coords[coords.length - 1].x},${h} Z`;
    return { points, areaPath, min: cMin, max: cMax };
  }

  function getChartColor(m: string): string {
    if (m === 'te') return 'var(--color-optimal)';
    if (m === 'ps') return 'var(--color-attention)';
    if (m === 'power') return '#8B5CF6';
    if (m === 'hr') return '#EF4444';
    return 'var(--color-accent)';
  }
</script>

<svelte:head>
  <title>{ride ? formatDate(ride.timestamp) : 'Ride'} - KPedal</title>
</svelte:head>

<div class="page ride-page">
  <div class="container">
    {#if loading}
      <div class="loading-state animate-in"><div class="spinner"></div></div>
    {:else if error}
      <div class="error-state animate-in">
        <p>{error}</p>
        <a href="/rides" class="back-btn-link">← Back to Rides</a>
      </div>
    {:else if ride}
      <header class="page-header animate-in">
        <a href="/rides" class="back-link" aria-label="Back">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        </a>
        <div class="header-info">
          <h1>{formatDate(ride.timestamp)}</h1>
          <div class="header-meta">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12,6 12,12 16,14"/></svg>
              {formatTime(ride.timestamp)}
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
              {formatDuration(ride.duration_ms)}
            </span>
            {#if ride.distance_km > 0}
              <span class="meta-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>
                {ride.distance_km.toFixed(1)} km
              </span>
            {/if}
          </div>
        </div>
      </header>

      <!-- Hero Stats -->
      <div class="hero-stats animate-in">
        {#if ride.score > 0}
          <div class="hero-stat score-stat">
            <span class="hero-stat-value {getScoreStatus(ride.score)}">{ride.score}</span>
            <div class="hero-stat-info">
              <span class="hero-stat-label">Score <InfoTip text="Overall technique score from 0 to 100. Combines balance, TE, and PS." position="bottom" size="sm" /></span>
            </div>
          </div>
        {/if}

        <div class="hero-stat asymmetry-stat">
          <span class="hero-stat-value {getBalanceStatus(ride.balance_left)}">{getAsymmetry(ride.balance_left).toFixed(1)}%</span>
          <div class="hero-stat-info">
            <span class="hero-stat-label">Asymmetry <InfoTip text="Deviation from 50/50 balance. Under 2.5% is pro level." position="bottom" size="sm" /></span>
            <span class="hero-stat-detail">{getDominance(ride.balance_left)}</span>
          </div>
        </div>

        <div class="hero-stat balance-stat">
          <div class="balance-display">
            <div class="balance-side">
              <span class="balance-pct">{ride.balance_left.toFixed(0)}</span>
              <span class="balance-leg">L</span>
            </div>
            <div class="balance-bar-wrap">
              <div class="balance-bar">
                <div class="balance-fill left" style="width: {Math.min(50, ride.balance_left)}%"></div>
                <div class="balance-fill right" style="width: {Math.min(50, ride.balance_right)}%"></div>
              </div>
              <div class="balance-center-mark"></div>
            </div>
            <div class="balance-side">
              <span class="balance-pct">{ride.balance_right.toFixed(0)}</span>
              <span class="balance-leg">R</span>
            </div>
          </div>
        </div>

        <div class="hero-stat zones-stat">
          <div class="zone-mini-bars">
            <div class="zone-mini optimal" style="width: {ride.zone_optimal}%"></div>
            <div class="zone-mini attention" style="width: {ride.zone_attention}%"></div>
            <div class="zone-mini problem" style="width: {ride.zone_problem}%"></div>
          </div>
          <div class="zone-mini-values">
            <span class="zone-val optimal">{ride.zone_optimal}%</span>
            <span class="zone-val attention">{ride.zone_attention}%</span>
            <span class="zone-val problem">{ride.zone_problem}%</span>
          </div>
          <span class="hero-stat-label">Time in Zone <InfoTip text="Time in optimal, attention, and problem zones. Higher green is better." position="bottom" size="sm" /></span>
        </div>
      </div>

      <!-- Performance Strip -->
      {#if hasPerformanceData || hasElevationData}
        <div class="perf-strip animate-in">
          {#if ride.power_avg > 0}
            <div class="perf-chip">
              <span class="perf-val">{Math.round(ride.power_avg)}</span>
              <span class="perf-unit">W <InfoTip text="Average power output in watts." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.power_max > 0}
            <div class="perf-chip">
              <span class="perf-val">{Math.round(ride.power_max)}</span>
              <span class="perf-unit">W max <InfoTip text="Peak power achieved during the ride." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.normalized_power > 0}
            <div class="perf-chip">
              <span class="perf-val">{Math.round(ride.normalized_power)}</span>
              <span class="perf-unit">NP <InfoTip text="Normalized Power adjusts for variability in effort." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.hr_avg > 0}
            <div class="perf-chip">
              <span class="perf-val">{Math.round(ride.hr_avg)}</span>
              <span class="perf-unit">bpm <InfoTip text="Average heart rate during the ride." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.hr_max > 0}
            <div class="perf-chip">
              <span class="perf-val">{Math.round(ride.hr_max)}</span>
              <span class="perf-unit">max <InfoTip text="Peak heart rate reached. Near max often hurts technique." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.cadence_avg > 0}
            <div class="perf-chip">
              <span class="perf-val">{Math.round(ride.cadence_avg)}</span>
              <span class="perf-unit">rpm <InfoTip text="Average cadence. Optimal is 80-95 rpm for most riders." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.speed_avg > 0}
            <div class="perf-chip">
              <span class="perf-val">{ride.speed_avg.toFixed(1)}</span>
              <span class="perf-unit">km/h <InfoTip text="Average speed. Less reliable than power due to terrain." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.energy_kj > 0}
            <div class="perf-chip">
              <span class="perf-val">{Math.round(ride.energy_kj)}</span>
              <span class="perf-unit">kJ <InfoTip text="Total energy output. Roughly equals calories burned." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.elevation_gain > 0}
            <div class="perf-chip">
              <span class="perf-val">+{Math.round(ride.elevation_gain)}</span>
              <span class="perf-unit">m <InfoTip text="Total elevation gained during the ride." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.elevation_loss > 0}
            <div class="perf-chip">
              <span class="perf-val">-{Math.round(ride.elevation_loss)}</span>
              <span class="perf-unit">m <InfoTip text="Total elevation lost during the ride." position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if ride.grade_max > 0}
            <div class="perf-chip">
              <span class="perf-val">{ride.grade_max.toFixed(0)}%</span>
              <span class="perf-unit">grade <InfoTip text="Maximum gradient. Above 10% is steep." position="bottom" size="sm" /></span>
            </div>
          {/if}
        </div>
      {/if}

      <!-- Main Grid -->
      <div class="main-grid animate-in">
        <!-- Technique Card -->
        <div class="grid-card technique-card">
          <div class="card-header">
            <span class="card-title">Technique <InfoTip text="Pedaling efficiency metrics. TE is power transfer, PS is stroke smoothness." position="right" size="sm" /></span>
          </div>

          <!-- Compact Metrics -->
          <div class="tech-metrics">
            <!-- TE -->
            <div class="tech-metric-row">
              <div class="tech-metric-main">
                <span class="tech-metric-name">TE <InfoTip text="Torque Effectiveness. How much power goes into forward motion." position="right" size="sm" /></span>
                <span class="tech-metric-value {getTeStatus((ride.te_left + ride.te_right) / 2)}">{((ride.te_left + ride.te_right) / 2).toFixed(0)}%</span>
                <span class="tech-metric-range">70-80%</span>
              </div>
              <div class="tech-metric-bar">
                <div class="tech-bar-track">
                  <div class="tech-bar-fill te" style="width: {Math.min(100, (ride.te_left + ride.te_right) / 2)}%"></div>
                  <div class="tech-bar-zone te"></div>
                </div>
              </div>
              <div class="tech-metric-lr">
                <span>L {ride.te_left.toFixed(0)}%</span>
                <span>R {ride.te_right.toFixed(0)}%</span>
              </div>
            </div>

            <!-- PS -->
            <div class="tech-metric-row">
              <div class="tech-metric-main">
                <span class="tech-metric-name">PS <InfoTip text="Pedal Smoothness. How evenly power is applied through the stroke." position="right" size="sm" /></span>
                <span class="tech-metric-value {getPsStatus((ride.ps_left + ride.ps_right) / 2)}">{((ride.ps_left + ride.ps_right) / 2).toFixed(0)}%</span>
                <span class="tech-metric-range">≥20%</span>
              </div>
              <div class="tech-metric-bar">
                <div class="tech-bar-track">
                  <div class="tech-bar-fill ps" style="width: {Math.min(100, ((ride.ps_left + ride.ps_right) / 2) / 40 * 100)}%"></div>
                  <div class="tech-bar-zone ps"></div>
                </div>
              </div>
              <div class="tech-metric-lr">
                <span>L {ride.ps_left.toFixed(0)}%</span>
                <span>R {ride.ps_right.toFixed(0)}%</span>
              </div>
            </div>
          </div>

          <!-- Stats Row -->
          {#if techniqueStats && ride.snapshots?.length >= 3}
            <div class="technique-stats">
              <div class="tech-stat">
                <span class="tech-stat-value">{techniqueStats.teOptimalTime.toFixed(0)}%</span>
                <span class="tech-stat-label">TE in zone <InfoTip text="Time with TE in optimal 70-80% range." position="top" size="sm" /></span>
              </div>
              <div class="tech-stat">
                <span class="tech-stat-value">{techniqueStats.psOptimalTime.toFixed(0)}%</span>
                <span class="tech-stat-label">PS in zone <InfoTip text="Time with PS above 20% threshold." position="top" size="sm" /></span>
              </div>
              <div class="tech-stat">
                <span class="tech-stat-value">{techniqueStats.teStability.toFixed(0)}%</span>
                <span class="tech-stat-label">TE stab <InfoTip text="How consistent TE was. Higher means steadier technique." position="top" size="sm" /></span>
              </div>
              <div class="tech-stat">
                <span class="tech-stat-value">{techniqueStats.psStability.toFixed(0)}%</span>
                <span class="tech-stat-label">PS stab <InfoTip text="How consistent PS was. Higher means steadier technique." position="top" size="sm" /></span>
              </div>
            </div>
          {/if}
        </div>
      </div>

      <!-- Timeline Chart -->
      {#if ride.snapshots?.length > 0}
        <div class="grid-card wide animate-in">
          <div class="card-header">
            <span class="card-title">Timeline <InfoTip text="Metrics over time. Look for drops indicating fatigue." position="right" size="sm" /></span>
            <div class="chart-tabs">
              <button class="chart-tab" class:active={activeChart === 'balance'} on:click={() => activeChart = 'balance'}>Asymmetry</button>
              <button class="chart-tab" class:active={activeChart === 'te'} on:click={() => activeChart = 'te'}>TE</button>
              <button class="chart-tab" class:active={activeChart === 'ps'} on:click={() => activeChart = 'ps'}>PS</button>
              {#if ride.snapshots.some(s => s.power_avg > 0)}
                <button class="chart-tab" class:active={activeChart === 'power'} on:click={() => activeChart = 'power'}>Power</button>
              {/if}
              {#if ride.snapshots.some(s => s.hr_avg > 0)}
                <button class="chart-tab" class:active={activeChart === 'hr'} on:click={() => activeChart = 'hr'}>HR</button>
              {/if}
            </div>
          </div>
          <div class="chart-container">
            <svg viewBox="0 0 200 50" class="chart-svg">
              <defs>
                <linearGradient id="chartGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color={getChartColor(activeChart)} stop-opacity="0.25"/>
                  <stop offset="100%" stop-color={getChartColor(activeChart)} stop-opacity="0"/>
                </linearGradient>
              </defs>
              <path d={chartData.areaPath} fill="url(#chartGrad)"/>
              <polyline points={chartData.points} fill="none" stroke={getChartColor(activeChart)} stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <div class="chart-labels">
              <span>{chartData.max.toFixed(0)}</span>
              <span>{chartData.min.toFixed(0)}</span>
            </div>
          </div>
          <div class="chart-x-labels">
            <span>Start</span>
            <span>{formatDuration(ride.duration_ms)}</span>
          </div>
        </div>
      {/if}

      <!-- Minute by Minute -->
      {#if ride.snapshots?.length > 0}
        <div class="grid-card wide animate-in">
          <div class="card-header">
            <span class="card-title">Minute-by-Minute <InfoTip text="Detailed breakdown by minute. Useful for analyzing specific segments." position="right" size="sm" /></span>
            <span class="card-subtitle">{ride.snapshots.length} points</span>
          </div>
          <div class="table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Min</th>
                  <th>Balance <InfoTip text="Left/Right power split for this minute." position="bottom" size="sm" /></th>
                  <th>TE <InfoTip text="Torque Effectiveness L/R for this minute." position="bottom" size="sm" /></th>
                  <th>PS <InfoTip text="Pedal Smoothness L/R for this minute." position="bottom" size="sm" /></th>
                  {#if ride.snapshots.some(s => s.power_avg > 0)}<th>Power</th>{/if}
                  {#if ride.snapshots.some(s => s.hr_avg > 0)}<th>HR</th>{/if}
                  <th>Zone <InfoTip text="Overall zone status for this minute." position="bottom" size="sm" /></th>
                </tr>
              </thead>
              <tbody>
                {#each ride.snapshots as s}
                  <tr class="zone-{s.zone_status.toLowerCase()}">
                    <td class="col-min">{s.minute_index + 1}</td>
                    <td><span class="{getBalanceStatus(s.balance_left)}">{s.balance_left}/{s.balance_right}</span></td>
                    <td><span class="{getTeStatus((s.te_left + s.te_right) / 2)}">{s.te_left}/{s.te_right}</span></td>
                    <td><span class="{getPsStatus((s.ps_left + s.ps_right) / 2)}">{s.ps_left}/{s.ps_right}</span></td>
                    {#if ride.snapshots.some(ss => ss.power_avg > 0)}<td>{s.power_avg > 0 ? `${s.power_avg}W` : '—'}</td>{/if}
                    {#if ride.snapshots.some(ss => ss.hr_avg > 0)}<td>{s.hr_avg > 0 ? s.hr_avg : '—'}</td>{/if}
                    <td><span class="zone-badge {s.zone_status.toLowerCase()}">{s.zone_status}</span></td>
                  </tr>
                {/each}
              </tbody>
            </table>
          </div>
        </div>
      {/if}
    {/if}
  </div>
</div>

<style>
  /* Page-specific header styles */
  .ride-page .page-header { align-items: flex-start; margin-bottom: 16px; }
  .ride-page .back-link { margin-top: 2px; }
  .header-info { flex: 1; }
  .page-header h1 { font-size: 22px; font-weight: 600; color: var(--text-primary); line-height: 1.2; }
  .header-meta { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 8px; }
  .meta-item {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    background: var(--bg-surface);
    padding: 5px 10px;
    border-radius: 6px;
    border: 1px solid var(--border-subtle);
  }
  .meta-item svg { width: 14px; height: 14px; opacity: 0.7; }

  /* Hero Stats */
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

  .hero-stat.score-stat, .hero-stat.asymmetry-stat { display: flex; align-items: center; gap: 12px; }
  .hero-stat-value { font-size: 26px; font-weight: 700; color: var(--text-primary); }
  .hero-stat-value.optimal { color: var(--color-optimal); }
  .hero-stat-value.attention { color: var(--color-attention); }
  .hero-stat-value.problem { color: var(--color-problem); }
  .hero-stat-info { display: flex; flex-direction: column; gap: 1px; }
  .hero-stat-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; }
  .hero-stat-detail { font-size: 11px; color: var(--text-tertiary); }

  /* Balance Display */
  .hero-stat.balance-stat { display: flex; align-items: center; justify-content: center; }
  .balance-display { display: flex; align-items: center; gap: 10px; width: 100%; }
  .balance-side { display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 32px; }
  .balance-pct { font-size: 18px; font-weight: 700; color: var(--text-primary); line-height: 1; }
  .balance-leg { font-size: 10px; font-weight: 600; color: var(--text-muted); }
  .balance-bar-wrap { flex: 1; position: relative; }
  .balance-bar { display: flex; height: 8px; background: var(--bg-base); border-radius: 4px; overflow: hidden; }
  .balance-fill.left { background: var(--color-accent); height: 100%; }
  .balance-fill.right { background: var(--color-accent); opacity: 0.4; height: 100%; }
  .balance-center-mark {
    position: absolute;
    left: 50%;
    top: -2px;
    bottom: -2px;
    width: 2px;
    background: var(--text-muted);
    transform: translateX(-50%);
    border-radius: 1px;
  }

  /* Zones */
  .hero-stat.zones-stat { display: flex; flex-direction: column; gap: 6px; }
  .zone-mini-bars { display: flex; height: 8px; border-radius: 4px; overflow: hidden; background: var(--bg-base); }
  .zone-mini { height: 100%; }
  .zone-mini.optimal { background: var(--color-optimal); }
  .zone-mini.attention { background: var(--color-attention); }
  .zone-mini.problem { background: var(--color-problem); }
  .zone-mini-values { display: flex; justify-content: space-between; }
  .zone-val { font-size: 11px; font-weight: 600; }
  .zone-val.optimal { color: var(--color-optimal-text); }
  .zone-val.attention { color: var(--color-attention-text); }
  .zone-val.problem { color: var(--color-problem-text); }

  /* Main Grid */
  .main-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 16px;
  }

  .grid-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    min-width: 0;
    overflow: hidden;
  }

  .grid-card.wide { grid-column: span 2; }

  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
  .card-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .card-subtitle { font-size: 11px; color: var(--text-muted); }
  .card-section { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--border-subtle); }
  .section-label { display: block; font-size: 10px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; margin-bottom: 8px; }

  /* Technique Card */
  .technique-card { grid-column: span 2; }

  /* Compact Technique Metrics */
  .tech-metrics { display: flex; flex-direction: column; gap: 12px; }
  .tech-metric-row { display: flex; align-items: center; gap: 12px; }
  .tech-metric-main { display: flex; align-items: baseline; gap: 6px; min-width: 120px; }
  .tech-metric-name { font-size: 12px; font-weight: 600; color: var(--text-muted); min-width: 24px; }
  .tech-metric-value { font-size: 20px; font-weight: 700; }
  .tech-metric-value.optimal { color: var(--color-optimal-text); }
  .tech-metric-value.attention { color: var(--color-attention-text); }
  .tech-metric-value.problem { color: var(--color-problem-text); }
  .tech-metric-range { font-size: 10px; color: var(--text-muted); }
  .tech-metric-bar { flex: 1; position: relative; }
  .tech-bar-track { height: 6px; background: var(--bg-elevated); border-radius: 3px; overflow: visible; position: relative; }
  .tech-bar-fill { height: 100%; border-radius: 3px; }
  .tech-bar-fill.te { background: var(--color-optimal); }
  .tech-bar-fill.ps { background: var(--color-attention); }
  .tech-bar-zone { position: absolute; top: -1px; bottom: -1px; border: 1.5px solid var(--text-muted); border-radius: 3px; opacity: 0.25; }
  .tech-bar-zone.te { left: 70%; right: 20%; }
  .tech-bar-zone.ps { left: 50%; right: 0; }
  .tech-metric-lr { display: flex; gap: 8px; font-size: 11px; color: var(--text-muted); min-width: 90px; justify-content: flex-end; }

  /* Technique Stats */
  .technique-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border-subtle); }
  .tech-stat { text-align: center; padding: 6px; background: var(--bg-elevated); border-radius: 6px; }
  .tech-stat-value { display: block; font-size: 14px; font-weight: 700; color: var(--text-primary); }
  .tech-stat-label { display: block; font-size: 9px; color: var(--text-muted); margin-top: 1px; }

  /* Performance Strip */
  .perf-strip {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 16px;
    padding: 14px 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
  }
  .perf-chip {
    display: flex;
    align-items: baseline;
    gap: 4px;
    padding: 6px 10px;
    background: var(--bg-base);
    border-radius: 6px;
  }
  .perf-val { font-size: 15px; font-weight: 600; color: var(--text-primary); }
  .perf-unit { font-size: 11px; color: var(--text-muted); }

  /* Chart */
  .chart-tabs { display: flex; gap: 4px; background: var(--bg-elevated); border-radius: 6px; padding: 3px; }
  .chart-tab {
    padding: 4px 10px;
    font-size: 11px;
    font-weight: 500;
    color: var(--text-secondary);
    background: transparent;
    border-radius: 4px;
    cursor: pointer;
  }
  .chart-tab:hover { color: var(--text-primary); }
  .chart-tab.active { background: var(--bg-surface); color: var(--text-primary); }

  .chart-container { position: relative; background: var(--bg-elevated); border-radius: 8px; overflow: hidden; }
  .chart-svg { display: block; width: 100%; height: 60px; }
  .chart-labels {
    position: absolute;
    top: 4px;
    bottom: 4px;
    left: 6px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    font-size: 9px;
    color: var(--text-muted);
  }
  .chart-x-labels { display: flex; justify-content: space-between; font-size: 10px; color: var(--text-muted); margin-top: 6px; }

  /* Table */
  .table-wrapper { overflow-x: auto; max-height: 350px; overflow-y: auto; scrollbar-width: none; }
  .table-wrapper::-webkit-scrollbar { display: none; }

  .data-table { width: 100%; border-collapse: collapse; font-size: 12px; min-width: 400px; }
  .data-table th {
    text-align: left;
    padding: 8px 8px;
    font-size: 10px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.3px;
    background: var(--bg-elevated);
    border-bottom: 1px solid var(--border-subtle);
    position: sticky;
    top: 0;
    z-index: 1;
  }
  .data-table td { padding: 6px 8px; border-bottom: 1px solid var(--border-subtle); color: var(--text-secondary); font-variant-numeric: tabular-nums; }
  .data-table tr:last-child td { border-bottom: none; }
  .data-table tr.zone-optimal { border-left: 3px solid var(--color-optimal); }
  .data-table tr.zone-attention { border-left: 3px solid var(--color-attention); }
  .data-table tr.zone-problem { border-left: 3px solid var(--color-problem); }
  .col-min { font-weight: 600; color: var(--text-primary); width: 40px; }

  .data-table .optimal { color: var(--color-optimal-text); }
  .data-table .attention { color: var(--color-attention-text); }
  .data-table .problem { color: var(--color-problem-text); }

  .zone-badge {
    display: inline-block;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 9px;
    font-weight: 600;
    text-transform: uppercase;
  }
  .zone-badge.optimal { background: var(--color-optimal-soft); color: var(--color-optimal-text); }
  .zone-badge.attention { background: var(--color-attention-soft); color: var(--color-attention-text); }
  .zone-badge.problem { background: var(--color-problem-soft); color: var(--color-problem-text); }

  @media (max-width: 768px) {
    .hero-stats { grid-template-columns: 1fr 1fr; gap: 8px; }
    .hero-stat { padding: 10px; }
    .hero-stat-value { font-size: 22px; }
    .hero-stat.score-stat, .hero-stat.asymmetry-stat { flex-direction: column; align-items: flex-start; gap: 4px; }
    .balance-pct { font-size: 15px; }
    .main-grid { grid-template-columns: 1fr; }
    .grid-card.wide, .technique-card { grid-column: span 1; }
    .perf-strip { padding: 12px; gap: 6px; }
    .perf-chip { padding: 5px 8px; }
    .perf-val { font-size: 14px; }
    .perf-unit { font-size: 10px; }
    .tech-metric-row { gap: 8px; }
    .tech-metric-main { min-width: 100px; gap: 4px; }
    .tech-metric-value { font-size: 18px; }
    .tech-metric-lr { min-width: 70px; font-size: 10px; gap: 6px; }
    .technique-stats { grid-template-columns: repeat(2, 1fr); }
    .card-header { flex-wrap: wrap; gap: 8px; }
    .chart-tabs { flex-wrap: wrap; }
    .header-meta { gap: 8px; }
    .meta-item { padding: 4px 8px; font-size: 12px; }
  }

  @media (max-width: 480px) {
    .page-header h1 { font-size: 18px; }
    .tech-metric-main { min-width: 90px; }
    .tech-metric-value { font-size: 16px; }
    .tech-metric-lr { display: none; }
    .tech-metric-bar { flex: 1; }
  }
</style>
