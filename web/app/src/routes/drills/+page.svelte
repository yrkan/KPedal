<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { isAuthenticated, authFetch } from '$lib/auth';
  import InfoTip from '$lib/components/InfoTip.svelte';

  interface DrillResult {
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

  interface DrillStats {
    total_drills: number;
    completed_drills: number;
    avg_score: number;
    best_score: number;
    total_duration_ms: number;
    drill_types_tried: number;
  }

  interface DrillTypeStats {
    drill_id: string;
    drill_name: string;
    attempts: number;
    completions: number;
    avg_score: number;
    best_score: number;
    last_attempt: number;
  }

  let drills: DrillResult[] = [];
  let stats: { summary: DrillStats; by_drill: DrillTypeStats[] } | null = null;
  let total = 0;
  let offset = 0;
  let limit = 50;
  let loading = true;
  let error: string | null = null;
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
    error = null;

    try {
      // Single API call replaces 2 separate requests
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
        error = 'Failed to load drills';
      }
    } catch (err) {
      error = 'Failed to load drills';
    } finally {
      loading = false;
    }
  }

  function filterByDrill(drillId: string | null) {
    filterDrillId = drillId;
    offset = 0;
    loadDashboard();
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

  function formatDuration(ms: number): string {
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    if (minutes > 0) return `${minutes}m ${seconds}s`;
    return `${seconds}s`;
  }

  function formatTotalDuration(ms: number): string {
    const hours = Math.floor(ms / 3600000);
    const minutes = Math.floor((ms % 3600000) / 60000);
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes}m`;
  }

  function formatDate(timestamp: number): string {
    return new Date(timestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  function formatFullDate(timestamp: number): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diffDays = Math.floor((now.getTime() - date.getTime()) / 86400000);

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return date.toLocaleDateString('en-US', { weekday: 'short' });

    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  function formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
  }

  function getTargetStatus(percent: number): string {
    if (percent >= 80) return 'optimal';
    if (percent >= 60) return 'attention';
    return 'problem';
  }

  function getProgressTrend(drillStats: DrillTypeStats): 'improving' | 'stable' | 'declining' | 'unknown' {
    // Would need historical data to calculate - for now show based on completion rate
    const completionRate = drillStats.completions / drillStats.attempts;
    if (completionRate >= 0.8) return 'improving';
    if (completionRate >= 0.5) return 'stable';
    return 'declining';
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
  <title>Drills - KPedal</title>
</svelte:head>

<div class="page drills-page">
  <div class="container container-lg">
    <header class="page-header animate-in">
      <a href="/" class="back-link" aria-label="Back to dashboard">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </a>
      <h1>Drills</h1>
      <div class="view-toggle">
        <button class="view-btn" class:active={viewMode === 'table'} on:click={() => viewMode = 'table'} title="Table view">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
          </svg>
        </button>
        <button class="view-btn" class:active={viewMode === 'cards'} on:click={() => viewMode = 'cards'} title="Card view">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
          </svg>
        </button>
      </div>
    </header>

    {#if loading && !stats}
      <div class="loading-state animate-in">
        <div class="spinner"></div>
      </div>
    {:else if error}
      <div class="error-state animate-in">
        <p>{error}</p>
        <button class="retry-btn" on:click={loadDashboard}>Retry</button>
      </div>
    {:else if drills.length === 0 && !loading && !filterDrillId}
      <div class="empty-state-enhanced animate-in">
        <div class="empty-icon-wrap">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 6v6l4 2"/>
          </svg>
        </div>
        <h3>No drills completed yet</h3>
        <p>KPedal includes guided drills to improve your pedaling technique. Complete them on your Karoo to track progress here.</p>

        <div class="drill-categories">
          <span class="drill-cat-label">Available drill types</span>
          <div class="drill-cat-grid">
            <div class="drill-cat">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/></svg>
              <span>Balance Focus</span>
            </div>
            <div class="drill-cat">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
              <span>Power Transfer</span>
            </div>
            <div class="drill-cat">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg>
              <span>Smooth Circles</span>
            </div>
            <div class="drill-cat">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
              <span>Challenges</span>
            </div>
          </div>
        </div>

        <div class="empty-actions">
          <a href="/" class="empty-action-btn">← Dashboard</a>
        </div>
      </div>
    {:else}
      <!-- Stats Overview -->
      {#if stats}
        <div class="stats-strip animate-in">
          <div class="stat-chip highlight">
            <span class="stat-chip-val {getTargetStatus(avgTimeInTarget)}">{avgTimeInTarget}%</span>
            <span class="stat-chip-label">avg target <InfoTip text="Average time spent hitting target metrics. Higher is better." position="bottom" size="sm" /></span>
          </div>
          <div class="stat-chip">
            <span class="stat-chip-val">{stats.summary.total_drills}</span>
            <span class="stat-chip-label">sessions <InfoTip text="Total number of drill sessions completed." position="bottom" size="sm" /></span>
          </div>
          <div class="stat-chip">
            <span class="stat-chip-val">{formatTotalDuration(stats.summary.total_duration_ms)}</span>
          </div>
          <div class="stat-chip">
            <span class="stat-chip-val optimal">{bestTimeInTarget}%</span>
            <span class="stat-chip-label">best <InfoTip text="Best time-in-target percentage achieved." position="bottom" size="sm" /></span>
          </div>
          <div class="stat-chip">
            <span class="stat-chip-val">{completionRate}%</span>
            <span class="stat-chip-label">completed <InfoTip text="Percentage of drills finished to completion." position="bottom" size="sm" /></span>
          </div>
          <div class="stat-chip">
            <span class="stat-chip-val">{stats.summary.drill_types_tried}</span>
            <span class="stat-chip-label">types <InfoTip text="Number of different drill types you've tried." position="bottom" size="sm" /></span>
          </div>
        </div>
      {/if}

      {#if loading}
        <div class="loading-inline"><div class="spinner-sm"></div></div>
      {/if}

      {#if viewMode === 'table'}
        <!-- Table View -->
        <div class="data-table-wrapper animate-in">
          <table class="data-table drills-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Drill</th>
                <th>Duration</th>
                <th>Time in Target <InfoTip text="Percentage of drill time spent in the target zone." position="bottom" size="sm" /></th>
                <th>Status <InfoTip text="Whether the drill was completed or abandoned early." position="bottom" size="sm" /></th>
              </tr>
            </thead>
            <tbody>
              {#each sortedDrills as drill}
                <tr class="drill-row">
                  <td class="col-date">
                    <span class="date-primary">{formatFullDate(drill.timestamp)}</span>
                    <span class="date-secondary">{formatTime(drill.timestamp)}</span>
                  </td>
                  <td class="col-name">{drill.drill_name}</td>
                  <td class="col-duration">{formatDuration(drill.duration_ms)}</td>
                  <td class="col-target">
                    <div class="target-cell">
                      <span class="target-value {getTargetStatus(drill.time_in_target_percent)}">{drill.time_in_target_percent.toFixed(0)}%</span>
                      <div class="target-bar">
                        <div class="target-fill {getTargetStatus(drill.time_in_target_percent)}" style="width: {drill.time_in_target_percent}%"></div>
                      </div>
                      <span class="target-time">{formatDuration(drill.time_in_target_ms)}</span>
                    </div>
                  </td>
                  <td class="col-status">
                    {#if drill.completed}
                      <span class="status-badge completed">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                          <polyline points="20,6 9,17 4,12"/>
                        </svg>
                        Completed
                      </span>
                    {:else}
                      <span class="status-badge incomplete">Incomplete</span>
                    {/if}
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {:else}
        <!-- Card View -->
        <div class="cards-grid drills-grid animate-in">
          {#each sortedDrills as drill}
            <article class="drill-card">
              <div class="drill-card-header">
                <div class="drill-info">
                  <span class="drill-name">{drill.drill_name}</span>
                  <div class="drill-when">
                    <span class="drill-date">{formatFullDate(drill.timestamp)}</span>
                    <span class="drill-time">{formatTime(drill.timestamp)}</span>
                  </div>
                </div>
                <div class="drill-badges">
                  {#if drill.completed}
                    <span class="badge completed">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                        <polyline points="20,6 9,17 4,12"/>
                      </svg>
                    </span>
                  {/if}
                  <span class="drill-duration">{formatDuration(drill.duration_ms)}</span>
                </div>
              </div>

              <div class="drill-target-section">
                <div class="target-display">
                  <span class="target-number {getTargetStatus(drill.time_in_target_percent)}">{drill.time_in_target_percent.toFixed(0)}%</span>
                  <span class="target-label">Time in Target</span>
                </div>
                <div class="target-details">
                  <div class="target-progress-bar">
                    <div class="target-progress-fill {getTargetStatus(drill.time_in_target_percent)}" style="width: {drill.time_in_target_percent}%"></div>
                  </div>
                  <span class="target-duration">{formatDuration(drill.time_in_target_ms)} of {formatDuration(drill.duration_ms)}</span>
                </div>
              </div>
            </article>
          {/each}
        </div>
      {/if}

      {#if total > limit}
        <div class="pagination animate-in">
          <button class="page-btn" on:click={prevPage} disabled={offset === 0} aria-label="Previous page">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15,18 9,12 15,6"/>
            </svg>
          </button>
          <span class="page-info">Page {Math.floor(offset / limit) + 1} of {Math.ceil(total / limit)}</span>
          <button class="page-btn" on:click={nextPage} disabled={offset + limit >= total} aria-label="Next page">
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
  /* Page-specific overrides */
  .drills-page :global(.spinner-sm) {
    width: 28px;
    height: 28px;
    border: 2px solid var(--border-default);
    border-top-color: var(--color-accent);
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  @keyframes spin { to { transform: rotate(360deg); } }

  /* Empty Filter */
  .empty-filter {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    padding: 60px;
    color: var(--text-tertiary);
  }

  .clear-filter-btn {
    padding: 10px 20px;
    font-size: 14px;
    color: var(--text-secondary);
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    cursor: pointer;
  }

  .clear-filter-btn:hover { background: var(--bg-hover); color: var(--text-primary); }

  /* Table-specific styles */
  .drills-table th { white-space: nowrap; }
  .drill-row:last-child td { border-bottom: none; }

  .col-date { white-space: nowrap; }
  .date-primary { font-weight: 500; color: var(--text-primary); display: block; }
  .date-secondary { font-size: 11px; color: var(--text-muted); }

  .col-name { font-weight: 500; color: var(--text-primary); }
  .col-duration { color: var(--text-secondary); }

  .target-cell { display: flex; align-items: center; gap: 10px; }
  .target-value {
    font-weight: 700;
    min-width: 40px;
  }
  .target-value.optimal { color: var(--color-optimal-text); }
  .target-value.attention { color: var(--color-attention-text); }
  .target-value.problem { color: var(--color-problem-text); }

  .target-bar {
    width: 80px;
    height: 6px;
    background: var(--bg-elevated);
    border-radius: 3px;
    overflow: hidden;
  }

  .target-fill {
    height: 100%;
    border-radius: 3px;
  }
  .target-fill.optimal { background: var(--color-optimal); }
  .target-fill.attention { background: var(--color-attention); }
  .target-fill.problem { background: var(--color-problem); }

  .target-time { font-size: 11px; color: var(--text-muted); }

  .status-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    font-size: 12px;
    font-weight: 500;
    border-radius: 6px;
  }
  .status-badge.completed {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }
  .status-badge.incomplete {
    background: var(--bg-elevated);
    color: var(--text-muted);
  }

  /* Card View */
  .drill-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 14px;
    padding: 18px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .drill-card:hover { border-color: var(--border-default); }

  .drill-card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .drill-info { display: flex; flex-direction: column; gap: 4px; }
  .drill-name { font-size: 15px; font-weight: 600; color: var(--text-primary); }
  .drill-when { display: flex; align-items: baseline; gap: 8px; }
  .drill-date { font-size: 13px; color: var(--text-secondary); }
  .drill-time { font-size: 12px; color: var(--text-tertiary); }

  .drill-badges { display: flex; align-items: center; gap: 8px; }
  .badge {
    width: 24px; height: 24px;
    display: flex; align-items: center; justify-content: center;
    border-radius: 6px;
  }
  .badge.completed {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }
  .drill-duration {
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
    padding: 4px 10px;
    background: var(--bg-elevated);
    border-radius: 6px;
  }

  .drill-target-section {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .target-display { text-align: center; flex-shrink: 0; }
  .target-number {
    font-size: 28px;
    font-weight: 700;
    line-height: 1;
    display: block;
  }
  .target-number.optimal { color: var(--color-optimal-text); }
  .target-number.attention { color: var(--color-attention-text); }
  .target-number.problem { color: var(--color-problem-text); }
  .target-label {
    font-size: 10px;
    color: var(--text-muted);
    text-transform: uppercase;
    margin-top: 4px;
    display: block;
  }

  .target-details { flex: 1; display: flex; flex-direction: column; gap: 6px; }
  .target-progress-bar {
    height: 8px;
    background: var(--bg-elevated);
    border-radius: 4px;
    overflow: hidden;
  }
  .target-progress-fill {
    height: 100%;
    border-radius: 4px;
  }
  .target-progress-fill.optimal { background: var(--color-optimal); }
  .target-progress-fill.attention { background: var(--color-attention); }
  .target-progress-fill.problem { background: var(--color-problem); }
  .target-duration { font-size: 12px; color: var(--text-secondary); }

  /* Enhanced Empty State */
  .empty-state-enhanced {
    text-align: center;
    padding: 60px 24px;
    max-width: 480px;
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
  .drill-categories {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 20px;
  }
  .drill-cat-label {
    display: block;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    color: var(--text-muted);
    margin-bottom: 12px;
  }
  .drill-cat-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .drill-cat {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    background: var(--bg-base);
    border-radius: 8px;
    font-size: 13px;
    color: var(--text-secondary);
  }
  .drill-cat svg {
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

  /* Mobile */
  @media (max-width: 768px) {
    /* Table */
    .data-table-wrapper { overflow-x: auto; }
    .drills-table { min-width: 420px; font-size: 12px; }
    .drills-table th { padding: 8px 10px; font-size: 10px; }
    .drills-table td { padding: 10px 8px; }
    .target-bar { width: 60px; }
    .target-time { display: none; }
    .col-status { white-space: nowrap; }
    .status-badge { font-size: 11px; padding: 3px 8px; }
    /* Cards */
    .drills-grid { grid-template-columns: 1fr; }
    .drill-card { padding: 14px; }
    .target-number { font-size: 24px; }
  }

  @media (max-width: 480px) {
    /* Table */
    .drills-table { min-width: 340px; font-size: 11px; }
    .drills-table th { padding: 6px 6px; font-size: 9px; }
    .drills-table td { padding: 8px 6px; }
    .date-secondary { display: none; }
    .col-name { max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .col-duration { white-space: nowrap; font-size: 10px; }
    .target-cell { gap: 4px; }
    .target-value { min-width: 28px; font-size: 11px; }
    .target-bar { width: 36px; height: 5px; }
    .status-badge { font-size: 10px; padding: 2px 6px; gap: 3px; }
    .status-badge svg { width: 10px; height: 10px; }
    /* Cards */
    .drill-card { padding: 12px; gap: 12px; }
    .drill-name { font-size: 14px; }
    .drill-target-section { gap: 12px; }
    .target-number { font-size: 22px; }
    .target-label { font-size: 9px; }
    .target-duration { font-size: 11px; }
  }
</style>
