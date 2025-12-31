<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { isAuthenticated, authFetch } from '$lib/auth';
  import { t, locale } from '$lib/i18n';
  import type { RideDetail } from '$lib/types/ride-detail';
  import { formatDate, formatTime, formatDuration, calculateFatigue, calculateTechniqueStats, calculatePowerZones } from '$lib/types/ride-detail';
  import { HeroStats, PerformanceStrip, TechniqueCard, FatigueCard, PowerZonesCard, TimelineChart, MinuteByMinuteTable } from '$lib/components/ride-detail';

  let ride: RideDetail | null = null;
  let loading = true;
  let error = false;

  $: rideId = $page.params.id;
  $: hasPerformanceData = ride && (ride.power_avg > 0 || ride.hr_avg > 0 || ride.cadence_avg > 0 || ride.elevation_gain > 0 || ride.elevation_loss > 0);
  $: fatigueData = ride && ride.snapshots && ride.snapshots.length >= 6 ? calculateFatigue(ride.snapshots) : null;
  $: techniqueStats = ride ? calculateTechniqueStats(ride) : null;
  $: powerZoneBreakdown = ride && ride.snapshots && ride.snapshots.length > 0 && ride.snapshots.some(s => s.power_avg > 0)
    ? calculatePowerZones(ride.snapshots)
    : null;

  onMount(async () => {
    if (!$isAuthenticated) {
      goto('/login');
      return;
    }
    await loadRide();
  });

  async function loadRide() {
    loading = true;
    error = false;
    try {
      const res = await authFetch(`/rides/${rideId}`);
      if (res.ok) {
        const data = await res.json();
        if (data.success) ride = data.data;
        else error = true;
      } else {
        error = true;
      }
    } catch {
      error = true;
    } finally {
      loading = false;
    }
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
        <p>{$t('errors.failedToLoad')}</p>
        <a href="/rides" class="back-btn-link">← {$t('rides.backToRides')}</a>
      </div>
    {:else if ride}
      <header class="page-header animate-in">
        <a href="/rides" class="back-link" aria-label={$t('common.back')}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        </a>
        <div class="header-info">
          <h1>{formatDate(ride.timestamp, $locale)}</h1>
          <div class="header-meta">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12,6 12,12 16,14"/></svg>
              {formatTime(ride.timestamp, $locale)}
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

      <HeroStats {ride} />

      {#if hasPerformanceData}
        <PerformanceStrip {ride} />
      {/if}

      <div class="main-grid animate-in">
        <TechniqueCard {ride} {techniqueStats} />
      </div>

      {#if fatigueData || (powerZoneBreakdown && powerZoneBreakdown.length > 0)}
        <div class="analysis-row animate-in">
          {#if fatigueData}
            <FatigueCard {fatigueData} />
          {/if}
          {#if powerZoneBreakdown && powerZoneBreakdown.length > 0}
            <PowerZonesCard powerZones={powerZoneBreakdown} />
          {/if}
        </div>
      {/if}

      {#if ride.snapshots?.length > 0}
        <TimelineChart snapshots={ride.snapshots} durationMs={ride.duration_ms} />
      {/if}

      {#if ride.snapshots?.length > 0}
        <MinuteByMinuteTable snapshots={ride.snapshots} />
      {/if}
    {/if}
  </div>
</div>

<style>
  .ride-page .page-header { align-items: flex-start; margin-bottom: 12px; }
  .ride-page .back-link { margin-top: 2px; }
  .header-info { flex: 1; }
  .page-header h1 { font-size: 20px; font-weight: 600; color: var(--text-primary); line-height: 1.2; }
  .header-meta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 6px; }
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

  .main-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    margin-bottom: 12px;
  }

  .analysis-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    margin-bottom: 12px;
  }

  @media (max-width: 768px) {
    .main-grid { grid-template-columns: 1fr; }
    .analysis-row { grid-template-columns: 1fr; gap: 10px; }
    .header-meta { gap: 8px; }
    .meta-item { padding: 4px 8px; font-size: 12px; }
  }

  @media (max-width: 480px) {
    .page-header h1 { font-size: 18px; }
  }
</style>
