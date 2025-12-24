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

  let rides: Ride[] = [];
  let total = 0;
  let offset = 0;
  let limit = 20;
  let loading = true;
  let error: string | null = null;

  onMount(async () => {
    if (!$isAuthenticated) {
      goto('/login');
      return;
    }
    await loadRides();
  });

  async function loadRides() {
    loading = true;
    error = null;

    try {
      const res = await authFetch(`/api/rides?limit=${limit}&offset=${offset}`);
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

  async function deleteRide(id: number) {
    if (!confirm('Delete this ride?')) return;

    try {
      const res = await authFetch(`/api/rides/${id}`, { method: 'DELETE' });
      if (res.ok) {
        rides = rides.filter(r => r.id !== id);
        total--;
      }
    } catch (err) {
      alert('Failed to delete ride');
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
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  }

  function formatDate(timestamp: number): string {
    return new Date(timestamp).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  function formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  function getBalanceStatus(left: number): string {
    const diff = Math.abs(left - 50);
    if (diff <= 2.5) return 'optimal';
    if (diff <= 5) return 'attention';
    return 'problem';
  }

  function getScoreStatus(score: number): string {
    if (score >= 80) return 'optimal';
    if (score >= 60) return 'attention';
    return 'problem';
  }
</script>

<svelte:head>
  <title>Rides - KPedal</title>
</svelte:head>

<div class="rides-page">
  <div class="container">
    <header class="page-header animate-in">
      <div class="header-content">
        <h1>Ride History</h1>
        <span class="ride-count">{total} rides</span>
      </div>
    </header>

    {#if loading}
      <div class="loading-state animate-in">
        <div class="spinner"></div>
        <p>Loading rides...</p>
      </div>
    {:else if error}
      <div class="error-state animate-in">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 8v4"/>
          <circle cx="12" cy="16" r="0.5" fill="currentColor"/>
        </svg>
        <p>{error}</p>
        <button class="btn btn-primary" on:click={loadRides}>Try Again</button>
      </div>
    {:else if rides.length === 0}
      <div class="empty-state animate-in">
        <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
        </svg>
        <h3>No rides yet</h3>
        <p>Your ride history will appear here once you sync data from your Karoo device.</p>
      </div>
    {:else}
      <!-- Desktop Table View -->
      <div class="rides-table animate-in">
        <div class="table-header">
          <span class="col-date">Date</span>
          <span class="col-duration">Duration</span>
          <span class="col-balance">Balance</span>
          <span class="col-te">TE</span>
          <span class="col-ps">PS</span>
          <span class="col-zones">Zones</span>
          <span class="col-score">Score</span>
          <span class="col-actions"></span>
        </div>

        {#each rides as ride}
          <div class="table-row">
            <span class="col-date">
              <span class="date-primary">{formatDate(ride.timestamp)}</span>
              <span class="date-secondary">{formatTime(ride.timestamp)}</span>
            </span>
            <span class="col-duration">{formatDuration(ride.duration_ms)}</span>
            <span class="col-balance">
              <span class="balance-badge {getBalanceStatus(ride.balance_left)}">
                {ride.balance_left.toFixed(1)}% / {ride.balance_right.toFixed(1)}%
              </span>
            </span>
            <span class="col-te">
              <span class="metric-value">{ride.te_left.toFixed(0)}%</span>
              <span class="metric-divider">/</span>
              <span class="metric-value">{ride.te_right.toFixed(0)}%</span>
            </span>
            <span class="col-ps">
              <span class="metric-value">{ride.ps_left.toFixed(0)}%</span>
              <span class="metric-divider">/</span>
              <span class="metric-value">{ride.ps_right.toFixed(0)}%</span>
            </span>
            <span class="col-zones">
              <span class="zone-badge optimal">{ride.zone_optimal.toFixed(0)}%</span>
              <span class="zone-badge attention">{ride.zone_attention.toFixed(0)}%</span>
              <span class="zone-badge problem">{ride.zone_problem.toFixed(0)}%</span>
            </span>
            <span class="col-score">
              <span class="score-badge {getScoreStatus(ride.score)}">{ride.score}</span>
            </span>
            <span class="col-actions">
              <button class="btn-delete" on:click={() => deleteRide(ride.id)} title="Delete ride" aria-label="Delete ride">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3,6 5,6 21,6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  <line x1="10" y1="11" x2="10" y2="17"/>
                  <line x1="14" y1="11" x2="14" y2="17"/>
                </svg>
              </button>
            </span>
          </div>
        {/each}
      </div>

      <!-- Mobile Card View -->
      <div class="rides-cards animate-in">
        {#each rides as ride}
          <div class="ride-card">
            <div class="ride-header">
              <div class="ride-date">
                <span class="date-primary">{formatDate(ride.timestamp)}</span>
                <span class="date-secondary">{formatTime(ride.timestamp)}</span>
              </div>
              <div class="ride-score {getScoreStatus(ride.score)}">{ride.score}</div>
            </div>
            <div class="ride-metrics">
              <div class="ride-metric">
                <span class="metric-label">Duration</span>
                <span class="metric-value">{formatDuration(ride.duration_ms)}</span>
              </div>
              <div class="ride-metric">
                <span class="metric-label">Balance</span>
                <span class="metric-value {getBalanceStatus(ride.balance_left)}">{ride.balance_left.toFixed(1)}%</span>
              </div>
              <div class="ride-metric">
                <span class="metric-label">TE</span>
                <span class="metric-value">{((ride.te_left + ride.te_right) / 2).toFixed(0)}%</span>
              </div>
              <div class="ride-metric">
                <span class="metric-label">PS</span>
                <span class="metric-value">{((ride.ps_left + ride.ps_right) / 2).toFixed(0)}%</span>
              </div>
            </div>
            <div class="ride-zones">
              <div class="zone-bar">
                <div class="zone-segment optimal" style="width: {ride.zone_optimal}%"></div>
                <div class="zone-segment attention" style="width: {ride.zone_attention}%"></div>
                <div class="zone-segment problem" style="width: {ride.zone_problem}%"></div>
              </div>
            </div>
            <button class="ride-delete" on:click={() => deleteRide(ride.id)} aria-label="Delete ride">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3,6 5,6 21,6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                <line x1="10" y1="11" x2="10" y2="17"/>
                <line x1="14" y1="11" x2="14" y2="17"/>
              </svg>
            </button>
          </div>
        {/each}
      </div>

      <!-- Pagination -->
      {#if total > limit}
        <div class="pagination animate-in">
          <button class="btn btn-secondary btn-sm" on:click={prevPage} disabled={offset === 0} aria-label="Previous page">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15,18 9,12 15,6"/>
            </svg>
            Previous
          </button>
          <span class="page-info">
            {offset + 1}-{Math.min(offset + limit, total)} of {total}
          </span>
          <button class="btn btn-secondary btn-sm" on:click={nextPage} disabled={offset + limit >= total} aria-label="Next page">
            Next
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
    margin-bottom: 24px;
  }

  .header-content {
    display: flex;
    align-items: baseline;
    gap: 12px;
  }

  .page-header h1 {
    font-size: 28px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.5px;
  }

  .ride-count {
    font-size: 14px;
    color: var(--text-tertiary);
  }

  /* Loading / Error / Empty States */
  .loading-state, .error-state, .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 24px;
    text-align: center;
    color: var(--text-tertiary);
  }

  .loading-state p, .error-state p, .empty-state p {
    margin-top: 16px;
    color: var(--text-secondary);
  }

  .empty-state h3 {
    margin-top: 20px;
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .empty-state p {
    max-width: 320px;
  }

  .error-state svg, .empty-state svg {
    color: var(--text-muted);
  }

  /* Desktop Table */
  .rides-table {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
    overflow: hidden;
  }

  .table-header, .table-row {
    display: grid;
    grid-template-columns: 1.8fr 1fr 1.5fr 1fr 1fr 2fr 0.8fr 0.5fr;
    padding: 16px 20px;
    align-items: center;
    gap: 12px;
  }

  .table-header {
    background: var(--bg-elevated);
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: 600;
    color: var(--text-tertiary);
    border-bottom: 1px solid var(--border-subtle);
  }

  .table-row {
    border-bottom: 1px solid var(--border-subtle);
    font-size: 14px;
    transition: background 0.15s ease;
  }

  .table-row:last-child {
    border-bottom: none;
  }

  .table-row:hover {
    background: var(--bg-hover);
  }

  .col-date {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .date-primary {
    font-weight: 500;
    color: var(--text-primary);
  }

  .date-secondary {
    font-size: 12px;
    color: var(--text-tertiary);
  }

  .col-duration {
    color: var(--text-secondary);
    font-weight: 500;
  }

  .balance-badge {
    padding: 4px 8px;
    border-radius: 6px;
    font-size: 13px;
    font-weight: 500;
  }

  .balance-badge.optimal {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }

  .balance-badge.attention {
    background: var(--color-attention-soft);
    color: var(--color-attention);
  }

  .balance-badge.problem {
    background: var(--color-problem-soft);
    color: var(--color-problem);
  }

  .col-te, .col-ps {
    display: flex;
    align-items: center;
    gap: 4px;
    color: var(--text-secondary);
  }

  .metric-divider {
    color: var(--text-muted);
  }

  .col-zones {
    display: flex;
    gap: 6px;
  }

  .zone-badge {
    padding: 3px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
  }

  .zone-badge.optimal {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }

  .zone-badge.attention {
    background: var(--color-attention-soft);
    color: var(--color-attention);
  }

  .zone-badge.problem {
    background: var(--color-problem-soft);
    color: var(--color-problem);
  }

  .score-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 600;
  }

  .score-badge.optimal {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }

  .score-badge.attention {
    background: var(--color-attention-soft);
    color: var(--color-attention);
  }

  .score-badge.problem {
    background: var(--color-problem-soft);
    color: var(--color-problem);
  }

  .btn-delete {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: transparent;
    color: var(--text-muted);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .btn-delete:hover {
    background: var(--color-problem-soft);
    color: var(--color-problem);
  }

  /* Mobile Cards */
  .rides-cards {
    display: none;
    flex-direction: column;
    gap: 12px;
  }

  .ride-card {
    position: relative;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
    padding: 16px;
  }

  .ride-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;
  }

  .ride-date {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .ride-score {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: 700;
  }

  .ride-score.optimal {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }

  .ride-score.attention {
    background: var(--color-attention-soft);
    color: var(--color-attention);
  }

  .ride-score.problem {
    background: var(--color-problem-soft);
    color: var(--color-problem);
  }

  .ride-metrics {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 12px;
  }

  .ride-metric {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .ride-metric .metric-label {
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-tertiary);
  }

  .ride-metric .metric-value {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .ride-metric .metric-value.optimal { color: var(--color-optimal); }
  .ride-metric .metric-value.attention { color: var(--color-attention); }
  .ride-metric .metric-value.problem { color: var(--color-problem); }

  .ride-zones {
    margin-top: 4px;
  }

  .zone-bar {
    display: flex;
    height: 6px;
    border-radius: 3px;
    overflow: hidden;
    background: var(--bg-elevated);
  }

  .zone-segment.optimal { background: var(--color-optimal); }
  .zone-segment.attention { background: var(--color-attention); }
  .zone-segment.problem { background: var(--color-problem); }

  .ride-delete {
    position: absolute;
    top: 16px;
    right: 60px;
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    background: transparent;
    color: var(--text-muted);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .ride-delete:hover {
    background: var(--color-problem-soft);
    color: var(--color-problem);
  }

  /* Pagination */
  .pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16px;
    margin-top: 24px;
  }

  .page-info {
    font-size: 14px;
    color: var(--text-secondary);
  }

  .pagination .btn {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .pagination .btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  /* Responsive */
  @media (max-width: 1024px) {
    .table-header, .table-row {
      grid-template-columns: 1.5fr 0.8fr 1.3fr 1.5fr 0.7fr 0.5fr;
    }
    .col-te, .col-ps {
      display: none;
    }
  }

  @media (max-width: 768px) {
    .rides-table {
      display: none;
    }
    .rides-cards {
      display: flex;
    }
  }
</style>
