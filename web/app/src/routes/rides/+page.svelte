<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { isAuthenticated, isDemo, authFetch } from '$lib/auth';
  import { getDemoRides } from '$lib/demoData';
  import { t } from '$lib/i18n';
  import type { Ride } from '$lib/types/rides';
  import { calculatePeriodStats } from '$lib/types/rides';
  import {
    RidesStatsStrip,
    RidesTable,
    RideCard,
    RidesEmptyState,
    RidesPagination
  } from '$lib/components/rides';

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
      <RidesEmptyState />
    {:else}
      <RidesStatsStrip stats={periodStats} />

      {#if loading}
        <div class="loading-inline"><div class="spinner-sm"></div></div>
      {/if}

      {#if viewMode === 'table'}
        <RidesTable
          rides={sortedRides}
          {deletingId}
          onRideClick={goToRide}
          onDeleteRide={deleteRide}
        />
      {:else}
        <div class="rides-grid animate-in">
          {#each sortedRides as ride}
            <RideCard
              {ride}
              {deletingId}
              onRideClick={goToRide}
              onDeleteRide={deleteRide}
            />
          {/each}
        </div>
      {/if}

      <RidesPagination
        {offset}
        {limit}
        {total}
        onPrevPage={prevPage}
        onNextPage={nextPage}
      />
    {/if}
  </div>
</div>

<style>
  .rides-page {
    padding: 24px 16px 100px;
    min-height: 100vh;
    background: var(--bg-base);
  }

  .container-lg {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .page-header h1 {
    flex: 1;
  }

  .loading-state {
    display: flex;
    justify-content: center;
    padding: 60px;
  }

  .spinner {
    width: 32px;
    height: 32px;
    border: 3px solid var(--border-default);
    border-top-color: var(--color-accent);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .error-state {
    text-align: center;
    padding: 60px 24px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
  }

  .error-state p {
    color: var(--text-muted);
    margin-bottom: 16px;
  }

  .retry-btn {
    padding: 10px 20px;
    background: var(--color-accent);
    color: white;
    border: none;
    border-radius: 8px;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.15s;
  }

  .retry-btn:hover {
    background: var(--color-accent-hover);
  }

  .loading-inline {
    display: flex;
    justify-content: center;
    padding: 12px;
  }

  .spinner-sm {
    width: 20px;
    height: 20px;
    border: 2px solid var(--border-default);
    border-top-color: var(--color-accent);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  .rides-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
  }

  @media (max-width: 640px) {
    .rides-page {
      padding: 16px 16px 80px;
    }

    .rides-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
