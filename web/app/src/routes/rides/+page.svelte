<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { isAuthenticated, isDemo, authFetch } from '$lib/auth';
  import { getDemoRides } from '$lib/demoData';
  import { t, locale } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';

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

  interface PeriodStats {
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

  let rides: Ride[] = [];
  let total = 0;
  let offset = 0;
  let limit = 100;
  let loading = true;
  let error = false;
  let deletingId: number | null = null;
  let viewMode: 'table' | 'cards' = 'table';
  let viewModeInitialized = false;

  // Computed stats (all rides, sorted by date)
  $: sortedRides = [...rides].sort((a, b) => b.timestamp - a.timestamp);
  $: periodStats = calculatePeriodStats(sortedRides);

  // Save viewMode to localStorage when it changes (only after initialization)
  $: if (viewModeInitialized && typeof localStorage !== 'undefined') {
    localStorage.setItem('rides-view-mode', viewMode);
  }

  onMount(async () => {
    // Restore viewMode from localStorage
    const savedViewMode = localStorage.getItem('rides-view-mode');
    if (savedViewMode === 'table' || savedViewMode === 'cards') {
      viewMode = savedViewMode;
    }
    viewModeInitialized = true;

    if (!$isAuthenticated) {
      goto('/login');
      return;
    }
    await loadRides();
  });

  async function loadRides() {
    loading = true;
    error = false;

    try {
      // Demo mode: use static data (0ms, no API call)
      if ($isDemo) {
        const data = getDemoRides();
        rides = data.rides;
        total = data.total;
        loading = false;
        return;
      }

      // Regular users: API call
      const res = await authFetch(`/rides?limit=${limit}&offset=${offset}`);
      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          rides = data.data.rides || [];
          total = data.data.total || 0;
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

  function calculatePeriodStats(ridesData: Ride[]): PeriodStats {
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
    // Pro cyclist metrics
    const totalElevation = ridesData.reduce((sum, r) => sum + (r.elevation_gain || 0), 0);
    const ridesWithNP = ridesData.filter(r => r.normalized_power > 0);
    const avgNP = ridesWithNP.length > 0 ? ridesWithNP.reduce((sum, r) => sum + r.normalized_power, 0) / ridesWithNP.length : 0;
    const totalEnergy = ridesData.reduce((sum, r) => sum + (r.energy_kj || 0), 0);

    return { rides: ridesData.length, totalDuration, avgAsymmetry, avgTe, avgPs, avgOptimal, avgPower, totalDistance, totalElevation, avgNP, totalEnergy };
  }

  async function deleteRide(id: number, event: Event) {
    event.stopPropagation();
    if (!confirm($t('rides.deleteRide'))) return;

    deletingId = id;
    try {
      const res = await authFetch(`/rides/${id}`, { method: 'DELETE' });
      if (res.ok) {
        rides = rides.filter(r => r.id !== id);
        total--;
      }
    } catch (err) {
      // ignore
    } finally {
      deletingId = null;
    }
  }

  function goToRide(id: number) {
    goto(`/rides/${id}`);
  }

  function getLocaleString(currentLocale: string | null | undefined): string {
    const localeMap: Record<string, string> = { en: 'en-US', es: 'es-ES' };
    return currentLocale ? localeMap[currentLocale] || currentLocale : 'en-US';
  }

  function formatDuration(ms: number): string {
    const hours = Math.floor(ms / 3600000);
    const minutes = Math.floor((ms % 3600000) / 60000);
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes}m`;
  }

  function formatTotalDuration(ms: number): string {
    const hours = Math.floor(ms / 3600000);
    const minutes = Math.floor((ms % 3600000) / 60000);
    if (hours >= 100) return `${hours}h`;
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes}m`;
  }

  function formatDate(timestamp: number, currentLocale?: string | null): string {
    const date = new Date(timestamp);
    return date.toLocaleDateString(getLocaleString(currentLocale), { month: 'short', day: 'numeric' });
  }

  function formatFullDate(timestamp: number, currentLocale?: string | null): string {
    const date = new Date(timestamp);
    return date.toLocaleDateString(getLocaleString(currentLocale), { weekday: 'short', month: 'short', day: 'numeric' });
  }

  function formatTime(timestamp: number, currentLocale?: string | null): string {
    return new Date(timestamp).toLocaleTimeString(getLocaleString(currentLocale), { hour: 'numeric', minute: '2-digit' });
  }

  function getAsymmetry(ride: Ride): number {
    return Math.abs(ride.balance_left - 50);
  }

  function getDominance(ride: Ride, leftLabel: string, rightLabel: string): string {
    if (Math.abs(ride.balance_left - 50) < 0.5) return '—';
    return ride.balance_left > 50 ? leftLabel : rightLabel;
  }

  function getAsymmetryClass(asymmetry: number): string {
    if (asymmetry <= 2.5) return 'optimal';
    if (asymmetry <= 5) return 'attention';
    return 'problem';
  }

  function getTeClass(te: number): string {
    if (te >= 70 && te <= 80) return 'optimal';
    if (te >= 60 && te <= 85) return 'attention';
    return 'problem';
  }

  function getPsClass(ps: number): string {
    if (ps >= 20) return 'optimal';
    if (ps >= 15) return 'attention';
    return 'problem';
  }

  function nextPage() {
    if (offset + limit < total) {
      offset += limit;
      loadRides();
    }
  }

  function prevPage() {
    if (offset > 0) {
      offset = Math.max(0, offset - limit);
      loadRides();
    }
  }
</script>

<svelte:head>
  <title>{$t('rides.title')} - KPedal</title>
</svelte:head>

<div class="page rides-page">
  <div class="container container-lg">
    <header class="page-header animate-in">
      <a href="/" class="back-link" aria-label={$t('rides.backToDashboard')}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </a>
      <h1>{$t('rides.title')}</h1>
      {#if rides.length > 0}
        <div class="view-toggle">
          <button class="view-btn" class:active={viewMode === 'table'} on:click={() => viewMode = 'table'} title={$t('rides.viewTable')}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
            </svg>
          </button>
          <button class="view-btn" class:active={viewMode === 'cards'} on:click={() => viewMode = 'cards'} title={$t('rides.viewCards')}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
            </svg>
          </button>
        </div>
      {/if}
    </header>

    {#if loading && rides.length === 0}
      <div class="loading-state animate-in">
        <div class="spinner"></div>
      </div>
    {:else if error}
      <div class="error-state animate-in">
        <p>{error}</p>
        <button class="retry-btn" on:click={loadRides}>{$t('common.retry')}</button>
      </div>
    {:else if rides.length === 0}
      <div class="empty-state-enhanced animate-in">
        <div class="empty-icon-wrap">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </div>
        <h3>{$t('rides.noRides')}</h3>
        <p>{$t('rides.noRidesHint')}</p>

        <div class="ride-metrics-preview">
          <span class="preview-label">{$t('rides.eachRideShows')}</span>
          <div class="preview-grid">
            <div class="preview-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/></svg>
              <span>{$t('metrics.leftRight')} {$t('metrics.balance')}</span>
            </div>
            <div class="preview-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
              <span>{$t('metrics.teShort')}</span>
            </div>
            <div class="preview-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg>
              <span>{$t('metrics.psShort')}</span>
            </div>
            <div class="preview-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20V10M18 20V4M6 20v-4"/></svg>
              <span>{$t('zones.title')}</span>
            </div>
          </div>
        </div>

        <div class="empty-actions">
          <a href="/settings" class="empty-action-btn secondary">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            {$t('settings.linkDevice')}
          </a>
          <a href="/" class="empty-action-btn">← {$t('nav.dashboard')}</a>
        </div>
      </div>
    {:else}
      <!-- Summary Stats -->
      {#if periodStats.rides > 0}
        <div class="stats-strip animate-in">
          <div class="stat-item">
            <span class="stat-value">{periodStats.rides}</span>
            <span class="stat-label">{$t('rides.list.ridesLabel')} <InfoTip text={$t('rides.list.ridesTip')} position="bottom" size="sm" /></span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{formatTotalDuration(periodStats.totalDuration)}</span>
          </div>
          {#if periodStats.totalDistance > 0}
            <div class="stat-item">
              <span class="stat-value">{periodStats.totalDistance.toFixed(0)}</span>
              <span class="stat-label">{$t('rides.list.kmLabel')} <InfoTip text={$t('rides.list.kmTip')} position="bottom" size="sm" /></span>
            </div>
          {/if}
          {#if periodStats.totalElevation > 0}
            <div class="stat-item">
              <span class="stat-value">{Math.round(periodStats.totalElevation)}</span>
              <span class="stat-label">{$t('rides.list.elevLabel')} <InfoTip text={$t('rides.list.elevTip')} position="bottom" size="sm" /></span>
            </div>
          {/if}
          <div class="stat-item">
            <span class="stat-value {getAsymmetryClass(periodStats.avgAsymmetry)}">{periodStats.avgAsymmetry.toFixed(1)}%</span>
            <span class="stat-label">{$t('rides.list.asymLabel')} <InfoTip text={$t('rides.list.asymTip')} position="bottom" size="sm" /></span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{periodStats.avgTe.toFixed(0)}%</span>
            <span class="stat-label">{$t('rides.list.teLabel')} <InfoTip text={$t('rides.list.teTip')} position="bottom" size="sm" /></span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{periodStats.avgPs.toFixed(0)}%</span>
            <span class="stat-label">{$t('rides.list.psLabel')} <InfoTip text={$t('rides.list.psTip')} position="bottom" size="sm" /></span>
          </div>
          <div class="stat-item highlight">
            <span class="stat-value">{periodStats.avgOptimal.toFixed(0)}%</span>
            <span class="stat-label">{$t('rides.list.optimalLabel')} <InfoTip text={$t('rides.list.optimalTip')} position="bottom" size="sm" /></span>
          </div>
          {#if periodStats.avgPower > 0}
            <div class="stat-item">
              <span class="stat-value">{Math.round(periodStats.avgPower)}</span>
              <span class="stat-label">{$t('rides.list.wAvgLabel')} <InfoTip text={$t('rides.list.wAvgTip')} position="bottom" size="sm" /></span>
            </div>
            {#if periodStats.avgNP > 0}
              <div class="stat-item highlight">
                <span class="stat-value">{Math.round(periodStats.avgNP)}</span>
                <span class="stat-label">{$t('rides.list.npLabel')} <InfoTip text={$t('rides.list.npTip')} position="bottom" size="sm" /></span>
              </div>
            {/if}
            {#if periodStats.totalEnergy > 0}
              <div class="stat-item">
                <span class="stat-value">{Math.round(periodStats.totalEnergy)}</span>
                <span class="stat-label">{$t('rides.list.kjLabel')} <InfoTip text={$t('rides.list.kjTip')} position="bottom" size="sm" /></span>
              </div>
            {/if}
          {/if}
        </div>
      {/if}

      {#if loading}
        <div class="loading-inline"><div class="spinner-sm"></div></div>
      {/if}

      <!-- Table View -->
      {#if viewMode === 'table'}
        <div class="data-table-wrapper rides-table-wrapper animate-in">
          <table class="data-table rides-table">
            <thead>
              <tr>
                <th class="col-date">{$t('rides.list.dateCol')}</th>
                <th class="col-duration">{$t('rides.list.durationCol')}</th>
                <th class="col-asymmetry">{$t('rides.list.asymCol')} <InfoTip text={$t('rides.list.asymColTip')} position="bottom" size="sm" /></th>
                <th class="col-balance">{$t('rides.list.lrCol')} <InfoTip text={$t('rides.list.lrColTip')} position="bottom" size="sm" /></th>
                <th class="col-te">{$t('rides.list.teCol')} <InfoTip text={$t('rides.list.teColTip')} position="bottom" size="sm" /></th>
                <th class="col-ps">{$t('rides.list.psCol')} <InfoTip text={$t('rides.list.psColTip')} position="bottom" size="sm" /></th>
                <th class="col-zones">{$t('rides.list.zonesCol')} <InfoTip text={$t('rides.list.zonesColTip')} position="bottom" size="sm" /></th>
                {#if sortedRides.some(r => r.power_avg > 0)}
                  <th class="col-power">{$t('rides.list.powerCol')} <InfoTip text={$t('rides.list.powerColTip')} position="bottom" size="sm" /></th>
                {/if}
                <th class="col-actions"></th>
              </tr>
            </thead>
            <tbody>
              {#each sortedRides as ride}
                <tr on:click={() => goToRide(ride.id)} class="ride-row" data-ride-id={ride.id}>
                  <td class="col-date">
                    <span class="date-primary">{formatDate(ride.timestamp, $locale)}</span>
                    <span class="date-secondary">{formatTime(ride.timestamp, $locale)}</span>
                  </td>
                  <td class="col-duration">{formatDuration(ride.duration_ms)}</td>
                  <td class="col-asymmetry">
                    <span class="asymmetry-value {getAsymmetryClass(getAsymmetry(ride))}">{getAsymmetry(ride).toFixed(1)}%</span>
                    <span class="dominance">{getDominance(ride, $t('metrics.left'), $t('metrics.right'))}</span>
                  </td>
                  <td class="col-balance">{ride.balance_left.toFixed(0)} / {ride.balance_right.toFixed(0)}</td>
                  <td class="col-te">{ride.te_left.toFixed(0)}/{ride.te_right.toFixed(0)}</td>
                  <td class="col-ps">{ride.ps_left.toFixed(0)}/{ride.ps_right.toFixed(0)}</td>
                  <td class="col-zones">
                    <div class="zone-bar-mini">
                      <div class="zone-segment optimal" style="width: {ride.zone_optimal}%"></div>
                      <div class="zone-segment attention" style="width: {ride.zone_attention}%"></div>
                      <div class="zone-segment problem" style="width: {ride.zone_problem}%"></div>
                    </div>
                  </td>
                  {#if sortedRides.some(r => r.power_avg > 0)}
                    <td class="col-power">{ride.power_avg > 0 ? `${Math.round(ride.power_avg)}W` : '—'}</td>
                  {/if}
                  <td class="col-actions">
                    <button
                      class="delete-btn"
                      on:click={(e) => deleteRide(ride.id, e)}
                      disabled={deletingId === ride.id}
                      aria-label={$t('rides.list.deleteAriaLabel')}
                    >
                      {#if deletingId === ride.id}
                        <span class="spinner-tiny"></span>
                      {:else}
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                        </svg>
                      {/if}
                    </button>
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {:else}
        <!-- Card View -->
        <div class="rides-grid animate-in">
          {#each sortedRides as ride}
            <article class="ride-card" on:click={() => goToRide(ride.id)} data-ride-id={ride.id}>
              <div class="ride-card-header">
                <div class="ride-date-info">
                  <span class="ride-date">{formatFullDate(ride.timestamp, $locale)}</span>
                  <span class="ride-time">{formatTime(ride.timestamp, $locale)}</span>
                </div>
                <div class="ride-header-right">
                  <span class="ride-duration">{formatDuration(ride.duration_ms)}</span>
                  <button
                    class="delete-btn"
                    on:click={(e) => deleteRide(ride.id, e)}
                    disabled={deletingId === ride.id}
                    aria-label={$t('rides.list.deleteAriaLabel')}
                  >
                    {#if deletingId === ride.id}
                      <span class="spinner-tiny"></span>
                    {:else}
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                      </svg>
                    {/if}
                  </button>
                </div>
              </div>

              <div class="ride-main-stats">
                <div class="asymmetry-display">
                  <span class="asymmetry-num {getAsymmetryClass(getAsymmetry(ride))}">{getAsymmetry(ride).toFixed(1)}%</span>
                  <span class="asymmetry-label">{$t('rides.list.asymmetryLabel')} {getDominance(ride, $t('metrics.left'), $t('metrics.right')) !== '—' ? `(${getDominance(ride, $t('metrics.left'), $t('metrics.right'))} ${$t('rides.list.domLabel')})` : ''}</span>
                </div>
                <div class="zone-display">
                  <div class="zone-bar-card">
                    <div class="zone-segment optimal" style="width: {ride.zone_optimal}%"></div>
                    <div class="zone-segment attention" style="width: {ride.zone_attention}%"></div>
                    <div class="zone-segment problem" style="width: {ride.zone_problem}%"></div>
                  </div>
                  <span class="zone-label">{$t('rides.list.optimalPercent', { values: { percent: ride.zone_optimal } })}</span>
                </div>
              </div>

              <div class="ride-metrics-grid">
                <div class="metric-box">
                  <span class="metric-name">{$t('rides.list.balanceLR')}</span>
                  <span class="metric-value">{ride.balance_left.toFixed(0)} / {ride.balance_right.toFixed(0)}</span>
                </div>
                <div class="metric-box">
                  <span class="metric-name">{$t('rides.list.teLR')}</span>
                  <span class="metric-value {getTeClass((ride.te_left + ride.te_right) / 2)}">{ride.te_left.toFixed(0)} / {ride.te_right.toFixed(0)}</span>
                </div>
                <div class="metric-box">
                  <span class="metric-name">{$t('rides.list.psLR')}</span>
                  <span class="metric-value {getPsClass((ride.ps_left + ride.ps_right) / 2)}">{ride.ps_left.toFixed(0)} / {ride.ps_right.toFixed(0)}</span>
                </div>
                {#if ride.power_avg > 0}
                  <div class="metric-box">
                    <span class="metric-name">{$t('rides.list.powerLabel')}</span>
                    <span class="metric-value">{Math.round(ride.power_avg)}W</span>
                  </div>
                {/if}
              </div>
            </article>
          {/each}
        </div>
      {/if}

      {#if total > limit}
        <div class="pagination animate-in">
          <button class="page-btn" on:click={prevPage} disabled={offset === 0} aria-label={$t('rides.list.prevPage')}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15,18 9,12 15,6"/>
            </svg>
          </button>
          <span class="page-info">{$t('rides.list.pageInfo', { values: { page: Math.floor(offset / limit) + 1, total: Math.ceil(total / limit) } })}</span>
          <button class="page-btn" on:click={nextPage} disabled={offset + limit >= total} aria-label={$t('rides.list.nextPage')}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9,6 15,12 9,18"/>
            </svg>
          </button>
        </div>
      {/if}
    {/if}
  </div>
</div>

<style>
  /* Stats Strip - page-specific overrides */
  .rides-page .stats-strip {
    gap: 10px;
  }

  .stat-item {
    display: flex;
    align-items: baseline;
    gap: 5px;
    padding: 8px 14px;
    background: var(--bg-base);
    border-radius: 8px;
    flex-shrink: 0;
    white-space: nowrap;
  }

  .stat-value {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .stat-value.optimal { color: var(--color-optimal-text); }
  .stat-value.attention { color: var(--color-attention-text); }
  .stat-value.problem { color: var(--color-problem-text); }
  .stat-value.accent { color: var(--color-accent); }

  .stat-label {
    font-size: 12px;
    color: var(--text-muted);
  }

  .stat-item.highlight {
    background: var(--color-optimal-soft, rgba(94, 232, 156, 0.1));
  }
  .stat-item.highlight .stat-value {
    color: var(--color-optimal-text);
  }

  /* Table View - page-specific */
  .rides-table-wrapper {
    border-radius: 14px;
  }

  .rides-table {
    font-size: 13px;
  }

  .rides-table th { white-space: nowrap; }

  .ride-row {
    cursor: pointer;
    transition: background 0.15s;
  }

  .ride-row:hover { background: var(--bg-hover); }
  .ride-row:last-child td { border-bottom: none; }

  .col-date { white-space: nowrap; }
  .date-primary { font-weight: 500; color: var(--text-primary); }
  .date-secondary { font-size: 11px; color: var(--text-muted); margin-left: 8px; }

  .col-duration { font-weight: 500; color: var(--text-secondary); }

  .col-asymmetry { white-space: nowrap; }
  .asymmetry-value {
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
  .asymmetry-value.optimal { color: var(--color-optimal-text); }
  .asymmetry-value.attention { color: var(--color-attention-text); }
  .asymmetry-value.problem { color: var(--color-problem-text); }
  .dominance {
    margin-left: 6px;
    font-size: 11px;
    color: var(--text-muted);
    font-weight: 500;
  }

  .balance-lr { color: var(--text-secondary); font-variant-numeric: tabular-nums; }

  .te-value, .ps-value {
    font-variant-numeric: tabular-nums;
    color: var(--text-secondary);
  }
  .te-value.optimal, .ps-value.optimal { color: var(--color-optimal-text); }
  .te-value.attention, .ps-value.attention { color: var(--color-attention-text); }
  .te-value.problem, .ps-value.problem { color: var(--color-problem-text); }

  .col-zones { width: 120px; }
  .zone-bar-mini {
    height: 8px;
    display: flex;
    border-radius: 4px;
    overflow: hidden;
    background: var(--bg-elevated);
  }
  .zone-segment { height: 100%; transition: width 0.3s ease; }
  .zone-segment.optimal { background: linear-gradient(90deg, var(--color-optimal), var(--color-optimal-light, var(--color-optimal))); }
  .zone-segment.attention { background: var(--color-attention); }
  .zone-segment.problem { background: var(--color-problem); }

  .col-power { font-variant-numeric: tabular-nums; color: var(--text-secondary); }
  .col-actions { width: 40px; text-align: center; }

  .delete-btn {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    color: var(--text-muted);
    cursor: pointer;
    opacity: 0;
    transition: all 0.15s ease;
    background: transparent;
  }

  .ride-row:hover .delete-btn, .ride-card:hover .delete-btn { opacity: 1; }
  .delete-btn:hover:not(:disabled) { background: var(--color-problem-soft); color: var(--color-problem); }
  .delete-btn:disabled { opacity: 0.5; }

  .spinner-tiny {
    width: 12px; height: 12px;
    border: 2px solid var(--border-default);
    border-top-color: var(--text-muted);
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  /* Card View */
  .rides-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
    gap: 16px;
  }

  .ride-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    cursor: pointer;
    transition: all 0.15s ease;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .ride-card:hover {
    border-color: var(--border-default);
    transform: translateY(-1px);
  }

  .ride-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .ride-date-info { display: flex; align-items: baseline; gap: 8px; }
  .ride-date { font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .ride-time { font-size: 11px; color: var(--text-tertiary); }

  .ride-header-right { display: flex; align-items: center; gap: 8px; }
  .ride-duration {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-secondary);
    padding: 4px 10px;
    background: var(--bg-base);
    border-radius: 6px;
  }

  .ride-main-stats {
    display: flex;
    gap: 16px;
    align-items: stretch;
    background: var(--bg-base);
    border-radius: 8px;
    padding: 12px;
  }

  .asymmetry-display {
    flex: 0 0 auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding-right: 16px;
    border-right: 1px solid var(--border-subtle);
  }
  .asymmetry-num {
    font-size: 26px;
    font-weight: 700;
    line-height: 1;
  }
  .asymmetry-num.optimal { color: var(--color-optimal-text); }
  .asymmetry-num.attention { color: var(--color-attention-text); }
  .asymmetry-num.problem { color: var(--color-problem-text); }
  .asymmetry-label {
    display: block;
    font-size: 10px;
    color: var(--text-muted);
    margin-top: 4px;
    text-align: center;
  }

  .zone-display {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }
  .zone-bar-card {
    height: 10px;
    display: flex;
    border-radius: 5px;
    overflow: hidden;
    background: var(--bg-elevated);
    margin-bottom: 6px;
  }
  .zone-label { font-size: 11px; color: var(--text-muted); }

  .ride-metrics-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }

  .metric-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    padding: 8px 4px;
    background: var(--bg-base);
    border-radius: 6px;
  }
  .metric-name {
    font-size: 9px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    color: var(--text-muted);
  }
  .metric-value {
    font-size: 13px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--text-primary);
  }
  .metric-value.optimal { color: var(--color-optimal-text); }
  .metric-value.attention { color: var(--color-attention-text); }
  .metric-value.problem { color: var(--color-problem-text); }

  /* Enhanced Empty State */
  .empty-state-enhanced {
    text-align: center;
    padding: 60px 24px;
    max-width: 420px;
    margin: 0 auto;
  }
  .empty-icon-wrap {
    width: 80px;
    height: 80px;
    margin: 0 auto 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    color: var(--text-muted);
  }
  .empty-state-enhanced h3 {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }
  .empty-state-enhanced > p {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.5;
    margin-bottom: 24px;
  }
  .ride-metrics-preview {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 20px;
  }
  .preview-label {
    display: block;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    color: var(--text-muted);
    margin-bottom: 12px;
  }
  .preview-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .preview-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    background: var(--bg-base);
    border-radius: 8px;
    font-size: 13px;
    color: var(--text-secondary);
  }
  .preview-item svg {
    color: var(--text-muted);
    flex-shrink: 0;
  }
  .empty-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
  }
  .empty-action-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 10px 18px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    text-decoration: none;
    transition: all 0.15s;
    background: var(--bg-surface);
    color: var(--text-secondary);
    border: 1px solid var(--border-subtle);
  }
  .empty-action-btn:hover {
    border-color: var(--border-default);
    color: var(--text-primary);
  }
  .empty-action-btn.secondary {
    background: var(--color-accent);
    color: var(--color-accent-text);
    border-color: var(--color-accent);
  }
  .empty-action-btn.secondary:hover {
    background: var(--color-accent-hover);
    border-color: var(--color-accent-hover);
  }

  /* Mobile */
  @media (max-width: 768px) {
    .stat-item { padding: 6px 12px; }
    .stat-value { font-size: 14px; }
    .stat-label { font-size: 11px; }
    .rides-table-wrapper { overflow-x: auto; }
    .rides-table { min-width: 450px; font-size: 11px; }
    .rides-table th { padding: 6px 8px; font-size: 9px; }
    .rides-table td { padding: 8px 6px; }
    .col-balance, .col-te, .col-ps { display: none; }
    .col-zones { width: 70px; }
    .zone-bar-mini { min-width: 50px; }
    .rides-grid { grid-template-columns: 1fr; }
    .ride-metrics-grid { grid-template-columns: repeat(4, 1fr); }
    .delete-btn { opacity: 1; }
  }

  @media (max-width: 480px) {
    .stat-item { padding: 6px 10px; }
    .stat-value { font-size: 13px; }
    .stat-label { font-size: 10px; }
    .rides-table { min-width: 320px; font-size: 10px; }
    .rides-table th { padding: 5px 4px; font-size: 8px; }
    .rides-table td { padding: 6px 4px; }
    .col-power { display: none; }
    .col-zones { width: 50px; }
    .zone-bar-mini { min-width: 40px; height: 6px; }
    .date-secondary { display: none; }
    .ride-metrics-grid { grid-template-columns: repeat(2, 1fr); }
    .asymmetry-num { font-size: 22px; }
  }
</style>
