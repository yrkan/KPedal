<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { isAuthenticated, authFetch } from '$lib/auth';

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
  }

  interface Stats {
    total_rides: number;
    total_duration_ms: number;
    avg_score: number;
    avg_balance_left: number;
    avg_zone_optimal: number;
  }

  let rides: Ride[] = [];
  let stats: Stats | null = null;
  let total = 0;
  let offset = 0;
  let limit = 25;
  let loading = true;
  let error: string | null = null;
  let deletingId: number | null = null;
  let sortBy: 'date' | 'score' | 'optimal' = 'date';

  onMount(async () => {
    if (!$isAuthenticated) {
      goto('/login');
      return;
    }
    await Promise.all([loadRides(), loadStats()]);
  });

  async function loadStats() {
    try {
      const res = await authFetch('/api/rides/stats/summary');
      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          stats = data.data;
        }
      }
    } catch (err) {
      // ignore
    }
  }

  async function loadRides() {
    loading = true;
    error = null;

    try {
      const res = await authFetch(`/api/rides?limit=${limit}&offset=${offset}&sort=${sortBy}`);
      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          rides = data.data.rides || [];
          total = data.data.total || 0;
        }
      } else {
        error = 'Failed to load rides';
      }
    } catch (err) {
      error = 'Failed to load rides';
    } finally {
      loading = false;
    }
  }

  function changeSort(newSort: 'date' | 'score' | 'optimal') {
    if (sortBy !== newSort) {
      sortBy = newSort;
      offset = 0;
      loadRides();
    }
  }

  async function deleteRide(id: number) {
    if (!confirm('Delete this ride?')) return;

    deletingId = id;
    try {
      const res = await authFetch(`/api/rides/${id}`, { method: 'DELETE' });
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

  function formatDuration(ms: number): string {
    const hours = Math.floor(ms / 3600000);
    const minutes = Math.floor((ms % 3600000) / 60000);
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes}m`;
  }

  function formatDate(timestamp: number): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diffDays = Math.floor((now.getTime() - date.getTime()) / 86400000);

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return date.toLocaleDateString('en-US', { weekday: 'long' });

    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  function formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
  }

  function getBalanceStatus(left: number): string {
    const diff = Math.abs(left - 50);
    if (diff <= 2.5) return 'optimal';
    if (diff <= 5) return 'attention';
    return 'problem';
  }

  function getTeStatus(te: number): string {
    if (te >= 70 && te <= 80) return 'optimal';
    if (te >= 60) return 'attention';
    return 'problem';
  }

  function getPsStatus(ps: number): string {
    if (ps >= 20) return 'optimal';
    if (ps >= 15) return 'attention';
    return 'problem';
  }

  function getScoreStatus(score: number): string {
    if (score >= 80) return 'optimal';
    if (score >= 60) return 'attention';
    return 'problem';
  }

  function getTeAvg(ride: Ride): number {
    return Math.round((ride.te_left + ride.te_right) / 2);
  }

  function getPsAvg(ride: Ride): number {
    return Math.round((ride.ps_left + ride.ps_right) / 2);
  }
</script>

<svelte:head>
  <title>Rides - KPedal</title>
</svelte:head>

<div class="rides-page">
  <div class="container container-md">
    <header class="page-header animate-in">
      <a href="/" class="back-link" aria-label="Back to dashboard">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </a>
      <h1>Rides</h1>
      {#if total > 0}
        <span class="header-count">{total}</span>
      {/if}
    </header>

    {#if loading && !stats}
      <div class="loading-state animate-in">
        <div class="spinner"></div>
      </div>
    {:else if error}
      <div class="error-state animate-in">
        <p>{error}</p>
        <button class="retry-btn" on:click={loadRides}>Retry</button>
      </div>
    {:else if rides.length === 0 && !loading}
      <div class="empty-state animate-in">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
        </svg>
        <p>No rides yet</p>
        <a href="/">← Dashboard</a>
      </div>
    {:else}
      <!-- Summary Stats -->
      {#if stats}
        <div class="rides-summary animate-in">
          <div class="summary-stat">
            <span class="summary-value">{stats.total_rides}</span>
            <span class="summary-label">Total Rides</span>
          </div>
          <div class="summary-stat">
            <span class="summary-value {getScoreStatus(stats.avg_score)}">{Math.round(stats.avg_score)}</span>
            <span class="summary-label">Avg Score</span>
          </div>
          <div class="summary-stat">
            <span class="summary-value {getBalanceStatus(stats.avg_balance_left)}">{stats.avg_balance_left.toFixed(1)}%</span>
            <span class="summary-label">Avg Balance L</span>
          </div>
          <div class="summary-stat">
            <span class="summary-value">{formatDuration(stats.total_duration_ms)}</span>
            <span class="summary-label">Total Time</span>
          </div>
        </div>
      {/if}

      <!-- Sort Options -->
      <div class="sort-options animate-in">
        <span class="sort-label">Sort by</span>
        <button class="sort-btn" class:active={sortBy === 'date'} on:click={() => changeSort('date')}>Date</button>
        <button class="sort-btn" class:active={sortBy === 'score'} on:click={() => changeSort('score')}>Score</button>
        <button class="sort-btn" class:active={sortBy === 'optimal'} on:click={() => changeSort('optimal')}>Optimal</button>
      </div>

      {#if loading}
        <div class="loading-inline"><div class="spinner-sm"></div></div>
      {/if}

      <div class="rides-list animate-in">
        {#each rides as ride}
          <article class="ride-card {getScoreStatus(ride.score)}">
            <div class="ride-score">
              <span class="score-value">{ride.score}</span>
              <span class="score-label">score</span>
            </div>

            <div class="ride-content">
              <header class="ride-header">
                <div class="ride-when">
                  <span class="ride-date">{formatDate(ride.timestamp)}</span>
                  <span class="ride-time">{formatTime(ride.timestamp)}</span>
                </div>
                <div class="ride-header-right">
                  <span class="ride-duration">{formatDuration(ride.duration_ms)}</span>
                  <button
                    class="delete-btn"
                    on:click={() => deleteRide(ride.id)}
                    disabled={deletingId === ride.id}
                    aria-label="Delete ride"
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
              </header>

              <div class="ride-metrics">
                <div class="metric">
                  <span class="metric-name">Balance</span>
                  <span class="metric-value {getBalanceStatus(ride.balance_left)}">
                    {ride.balance_left.toFixed(0)}<span class="metric-unit">L</span>
                    <span class="metric-sep">/</span>
                    {ride.balance_right.toFixed(0)}<span class="metric-unit">R</span>
                  </span>
                </div>
                <div class="metric">
                  <span class="metric-name">TE</span>
                  <span class="metric-value {getTeStatus(getTeAvg(ride))}">
                    {ride.te_left.toFixed(0)}<span class="metric-sep">/</span>{ride.te_right.toFixed(0)}
                    <span class="metric-avg">{getTeAvg(ride)}%</span>
                  </span>
                </div>
                <div class="metric">
                  <span class="metric-name">PS</span>
                  <span class="metric-value {getPsStatus(getPsAvg(ride))}">
                    {ride.ps_left.toFixed(0)}<span class="metric-sep">/</span>{ride.ps_right.toFixed(0)}
                    <span class="metric-avg">{getPsAvg(ride)}%</span>
                  </span>
                </div>
              </div>

              <div class="ride-zones">
                <div class="zone-bar">
                  <div class="zone-segment optimal" style="width: {ride.zone_optimal}%"></div>
                  <div class="zone-segment attention" style="width: {ride.zone_attention}%"></div>
                  <div class="zone-segment problem" style="width: {ride.zone_problem}%"></div>
                </div>
                <div class="zone-stats">
                  <span class="zone-stat"><i class="zone-dot optimal"></i>{ride.zone_optimal}%</span>
                  <span class="zone-stat"><i class="zone-dot attention"></i>{ride.zone_attention}%</span>
                  <span class="zone-stat"><i class="zone-dot problem"></i>{ride.zone_problem}%</span>
                </div>
              </div>
            </div>
          </article>
        {/each}
      </div>

      {#if total > limit}
        <div class="pagination animate-in">
          <button class="page-btn" on:click={prevPage} disabled={offset === 0} aria-label="Previous page">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15,18 9,12 15,6"/>
            </svg>
          </button>
          <span class="page-info">{Math.floor(offset / limit) + 1} / {Math.ceil(total / limit)}</span>
          <button class="page-btn" on:click={nextPage} disabled={offset + limit >= total} aria-label="Next page">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9,6 15,12 9,18"/>
            </svg>
          </button>
        </div>
      {/if}
    {/if}
  </div>
</div>

<style>
  .rides-page {
    padding: 32px 0 64px;
  }

  .page-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;
  }

  .back-link {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    color: var(--text-secondary);
    transition: all 0.15s ease;
  }

  .back-link:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .page-header h1 {
    font-size: 24px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .header-count {
    margin-left: auto;
    font-size: 13px;
    font-weight: 500;
    padding: 4px 10px;
    border-radius: 12px;
    background: var(--bg-elevated);
    color: var(--text-tertiary);
  }

  .loading-state, .error-state, .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 24px;
    color: var(--text-tertiary);
    gap: 12px;
  }

  .retry-btn {
    padding: 8px 16px;
    background: var(--color-accent);
    color: var(--color-accent-text);
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    transition: all 0.15s ease;
  }

  .retry-btn:hover {
    background: var(--color-accent-hover);
  }

  .empty-state a {
    font-size: 13px;
    color: var(--text-tertiary);
  }

  /* Summary Stats */
  .rides-summary {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 20px;
  }
  .summary-stat {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    padding: 14px;
    text-align: center;
  }
  .summary-value {
    display: block;
    font-size: 20px;
    font-weight: 700;
    color: var(--text-primary);
    font-variant-numeric: tabular-nums;
  }
  .summary-value.optimal { color: var(--color-optimal-text); }
  .summary-value.attention { color: var(--color-attention-text); }
  .summary-value.problem { color: var(--color-problem-text); }
  .summary-label {
    display: block;
    font-size: 11px;
    color: var(--text-muted);
    margin-top: 4px;
  }

  /* Sort Options */
  .sort-options {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
  }
  .sort-label {
    font-size: 12px;
    color: var(--text-muted);
    margin-right: 4px;
  }
  .sort-btn {
    padding: 6px 12px;
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .sort-btn:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }
  .sort-btn.active {
    background: var(--color-accent);
    color: var(--color-accent-text);
    border-color: var(--color-accent);
  }

  /* Loading Inline */
  .loading-inline {
    display: flex;
    justify-content: center;
    padding: 20px;
  }
  .spinner-sm {
    width: 24px;
    height: 24px;
    border: 2px solid var(--border-default);
    border-top-color: var(--color-accent);
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  /* Rides List */
  .rides-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  /* Ride Card */
  .ride-card {
    display: flex;
    gap: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
    transition: border-color 0.15s ease;
  }

  .ride-card:hover {
    border-color: var(--border-default);
  }

  /* Score Badge */
  .ride-score {
    flex-shrink: 0;
    width: 56px;
    height: 56px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    background: var(--bg-elevated);
  }

  .ride-card.optimal .ride-score {
    background: var(--color-optimal-soft);
  }
  .ride-card.attention .ride-score {
    background: var(--color-attention-soft);
  }
  .ride-card.problem .ride-score {
    background: var(--color-problem-soft);
  }

  .score-value {
    font-size: 20px;
    font-weight: 700;
    line-height: 1;
  }

  .ride-card.optimal .score-value { color: var(--color-optimal-text); }
  .ride-card.attention .score-value { color: var(--color-attention-text); }
  .ride-card.problem .score-value { color: var(--color-problem-text); }

  .score-label {
    font-size: 9px;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-muted);
    margin-top: 2px;
  }

  /* Content */
  .ride-content {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  /* Header */
  .ride-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .ride-when {
    display: flex;
    align-items: baseline;
    gap: 8px;
  }

  .ride-date {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .ride-time {
    font-size: 12px;
    color: var(--text-tertiary);
  }

  .ride-header-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .ride-duration {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    padding: 3px 8px;
    background: var(--bg-elevated);
    border-radius: 6px;
  }

  /* Metrics */
  .ride-metrics {
    display: flex;
    gap: 20px;
  }

  .metric {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .metric-name {
    font-size: 10px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    color: var(--text-muted);
  }

  .metric-value {
    font-size: 14px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--text-primary);
  }

  .metric-value.optimal { color: var(--color-optimal-text); }
  .metric-value.attention { color: var(--color-attention-text); }
  .metric-value.problem { color: var(--color-problem-text); }

  .metric-unit {
    font-size: 10px;
    font-weight: 500;
    color: var(--text-muted);
    margin-left: 1px;
  }

  .metric-sep {
    color: var(--text-muted);
    margin: 0 2px;
    font-weight: 400;
  }

  .metric-avg {
    font-size: 11px;
    font-weight: 500;
    margin-left: 4px;
    opacity: 0.8;
  }

  /* Zones */
  .ride-zones {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .zone-bar {
    flex: 1;
    height: 5px;
    display: flex;
    border-radius: 3px;
    overflow: hidden;
    background: var(--bg-elevated);
  }

  .zone-segment {
    height: 100%;
  }

  .zone-segment.optimal { background: var(--color-optimal); }
  .zone-segment.attention { background: var(--color-attention); }
  .zone-segment.problem { background: var(--color-problem); }

  .zone-stats {
    display: flex;
    gap: 10px;
    flex-shrink: 0;
  }

  .zone-stat {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    font-weight: 500;
    font-variant-numeric: tabular-nums;
    color: var(--text-secondary);
  }

  .zone-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .zone-dot.optimal { background: var(--color-optimal); }
  .zone-dot.attention { background: var(--color-attention); }
  .zone-dot.problem { background: var(--color-problem); }

  /* Delete Button */
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
    flex-shrink: 0;
  }

  .ride-card:hover .delete-btn {
    opacity: 1;
  }

  .delete-btn:hover:not(:disabled) {
    background: var(--color-problem-soft);
    color: var(--color-problem);
  }

  .delete-btn:disabled {
    opacity: 0.5;
  }

  .spinner-tiny {
    width: 12px;
    height: 12px;
    border: 2px solid var(--border-default);
    border-top-color: var(--text-muted);
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  /* Pagination */
  .pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
    margin-top: 20px;
  }

  .page-btn {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .page-btn:hover:not(:disabled) {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .page-btn:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }

  .page-info {
    font-size: 13px;
    color: var(--text-tertiary);
    min-width: 60px;
    text-align: center;
  }

  /* Mobile */
  @media (max-width: 640px) {
    .rides-page {
      padding: 24px 0 48px;
    }

    .rides-summary {
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;
    }
    .summary-stat {
      padding: 12px;
    }
    .summary-value {
      font-size: 18px;
    }
    .summary-label {
      font-size: 10px;
    }

    .sort-options {
      flex-wrap: wrap;
    }
    .sort-label {
      width: 100%;
      margin-bottom: 4px;
    }

    .ride-card {
      flex-direction: column;
      gap: 12px;
      padding: 14px;
    }

    .ride-score {
      width: 100%;
      height: auto;
      flex-direction: row;
      justify-content: flex-start;
      gap: 8px;
      padding: 10px 12px;
      border-radius: 8px;
    }

    .score-value {
      font-size: 18px;
    }

    .score-label {
      margin-top: 0;
      margin-left: 2px;
    }

    .ride-metrics {
      flex-wrap: wrap;
      gap: 12px 16px;
    }

    .metric {
      flex: 0 0 auto;
    }

    .ride-zones {
      flex-direction: column;
      align-items: stretch;
      gap: 8px;
    }

    .zone-stats {
      justify-content: space-between;
    }

    .delete-btn {
      opacity: 1;
    }
  }
</style>
