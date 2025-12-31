<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { isAuthenticated, authFetch, isDemo } from '$lib/auth';
  import { getDemoDrills } from '$lib/demoData';
  import { t } from '$lib/i18n';
  import type { DrillResult, DrillDashboardStats } from '$lib/types/drills';
  import { DrillsStatsStrip, DrillsTable, DrillCard, DrillsEmptyState, DrillsPagination } from '$lib/components/drills';

  let drills: DrillResult[] = [];
  let stats: DrillDashboardStats | null = null;
  let total = 0;
  let offset = 0;
  let limit = 50;
  let loading = true;
  let error = false;
  let filterDrillId: string | null = null;
  let viewMode: 'table' | 'cards' = 'table';
  let viewModeInitialized = false;
  let sortBy: 'date' | 'target' | 'duration' = 'date';

  // Save viewMode to localStorage when it changes (only after initialization)
  $: if (viewModeInitialized && typeof localStorage !== 'undefined') {
    localStorage.setItem('drills-view-mode', viewMode);
  }

  onMount(async () => {
    // Restore viewMode from localStorage
    const savedViewMode = localStorage.getItem('drills-view-mode');
    if (savedViewMode === 'table' || savedViewMode === 'cards') {
      viewMode = savedViewMode;
    }
    viewModeInitialized = true;

    if (!$isAuthenticated) {
      goto('/login');
      return;
    }
    await loadDashboard();
  });

  async function loadDashboard() {
    loading = true;
    error = false;

    try {
      // Demo mode: use static data (0ms, no API call)
      if ($isDemo) {
        const data = getDemoDrills();
        drills = data.drills;
        total = data.total;
        stats = data.stats;
        loading = false;
        return;
      }

      // Regular users: API call
      let url = `/drills/dashboard?limit=${limit}&offset=${offset}`;
      if (filterDrillId) {
        url += `&drill_id=${filterDrillId}`;
      }
      const res = await authFetch(url);
      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          drills = data.data.drills || [];
          total = data.data.total || 0;
          stats = data.data.stats;
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

  function nextPage() {
    if (offset + limit < total) {
      offset += limit;
      loadDashboard();
    }
  }

  function prevPage() {
    if (offset > 0) {
      offset = Math.max(0, offset - limit);
      loadDashboard();
    }
  }

  $: sortedDrills = [...drills].sort((a, b) => {
    if ((sortBy as string) === 'target') return b.time_in_target_percent - a.time_in_target_percent;
    if ((sortBy as string) === 'duration') return b.duration_ms - a.duration_ms;
    return b.timestamp - a.timestamp;
  });

  $: avgTimeInTarget = drills.length > 0
    ? Math.round(drills.reduce((sum, d) => sum + d.time_in_target_percent, 0) / drills.length)
    : 0;

  $: bestTimeInTarget = drills.length > 0
    ? Math.round(Math.max(...drills.map(d => d.time_in_target_percent)))
    : 0;

  $: completionRate = stats?.summary
    ? Math.round(stats.summary.completed_drills / stats.summary.total_drills * 100) || 0
    : 0;
</script>

<svelte:head>
  <title>{$t('drills.title')} - KPedal</title>
</svelte:head>

<div class="page drills-page">
  <div class="container container-lg">
    <header class="page-header animate-in">
      <a href="/" class="back-link" aria-label={$t('rides.backToDashboard')}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </a>
      <h1>{$t('drills.title')}</h1>
      {#if drills.length > 0}
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

    {#if loading && !stats}
      <div class="loading-state animate-in">
        <div class="spinner"></div>
      </div>
    {:else if error}
      <div class="error-state animate-in">
        <p>{$t('errors.failedToLoadDrills')}</p>
        <button class="retry-btn" on:click={loadDashboard}>{$t('common.retry')}</button>
      </div>
    {:else if drills.length === 0 && !loading && !filterDrillId}
      <DrillsEmptyState />
    {:else}
      {#if stats}
        <DrillsStatsStrip {stats} {avgTimeInTarget} {bestTimeInTarget} {completionRate} />
      {/if}

      {#if loading}
        <div class="loading-inline"><div class="spinner-sm"></div></div>
      {/if}

      {#if viewMode === 'table'}
        <DrillsTable drills={sortedDrills} />
      {:else}
        <div class="cards-grid drills-grid animate-in">
          {#each sortedDrills as drill}
            <DrillCard {drill} />
          {/each}
        </div>
      {/if}

      <DrillsPagination {offset} {limit} {total} onPrevPage={prevPage} onNextPage={nextPage} />
    {/if}
  </div>
</div>

<style>
  /* Page-specific spinner */
  .drills-page :global(.spinner-sm) {
    width: 28px;
    height: 28px;
    border: 2px solid var(--border-default);
    border-top-color: var(--color-accent);
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  @keyframes spin { to { transform: rotate(360deg); } }

  /* Cards grid */
  .drills-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
  }

  /* Mobile */
  @media (max-width: 768px) {
    .drills-grid { grid-template-columns: 1fr; }
  }
</style>
