<script lang="ts">
  import { t, locale } from '$lib/i18n';
  import type { Ride } from '$lib/types/rides';
  import { formatFullDate, formatTime, formatDuration, getAsymmetry, getAsymmetryClass, getTeClass, getPsClass, getDominance } from '$lib/types/rides';

  export let ride: Ride;
  export let deletingId: number | null = null;
  export let onRideClick: (id: number) => void;
  export let onDeleteRide: (id: number, event: Event) => void;
</script>

<article class="ride-card" on:click={() => onRideClick(ride.id)} data-ride-id={ride.id}>
  <div class="ride-card-header">
    <div class="ride-date-info">
      <span class="ride-date">{formatFullDate(ride.timestamp, $locale)}</span>
      <span class="ride-time">{formatTime(ride.timestamp, $locale)}</span>
    </div>
    <div class="ride-header-right">
      <span class="ride-duration">{formatDuration(ride.duration_ms)}</span>
      <button
        class="delete-btn"
        on:click={(e) => onDeleteRide(ride.id, e)}
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

<style>
  .ride-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 14px;
    padding: 16px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .ride-card:hover {
    border-color: var(--border-default);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  .ride-card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;
  }

  .ride-date-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .ride-date {
    font-weight: 600;
    color: var(--text-primary);
    font-size: 14px;
  }

  .ride-time {
    font-size: 12px;
    color: var(--text-muted);
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
    background: var(--bg-elevated);
    padding: 4px 10px;
    border-radius: 6px;
  }

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
    transition: all 0.15s;
    background: transparent;
    border: none;
  }

  .ride-card:hover .delete-btn { opacity: 1; }
  .delete-btn:hover { background: var(--color-problem-soft); color: var(--color-problem); }
  .delete-btn:disabled { opacity: 0.5; cursor: not-allowed; }

  .spinner-tiny {
    width: 12px;
    height: 12px;
    border: 2px solid var(--border-default);
    border-top-color: var(--text-secondary);
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .ride-main-stats {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--border-subtle);
  }

  .asymmetry-display {
    display: flex;
    align-items: baseline;
    gap: 8px;
  }

  .asymmetry-num {
    font-size: 28px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }

  .asymmetry-num.optimal { color: var(--color-optimal-text); }
  .asymmetry-num.attention { color: var(--color-attention-text); }
  .asymmetry-num.problem { color: var(--color-problem-text); }

  .asymmetry-label {
    font-size: 13px;
    color: var(--text-muted);
  }

  .zone-display {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .zone-bar-card {
    height: 10px;
    display: flex;
    border-radius: 5px;
    overflow: hidden;
    background: var(--bg-elevated);
  }

  .zone-segment { height: 100%; transition: width 0.3s ease; }
  .zone-segment.optimal { background: linear-gradient(90deg, var(--color-optimal), var(--color-optimal-light, var(--color-optimal))); }
  .zone-segment.attention { background: var(--color-attention); }
  .zone-segment.problem { background: var(--color-problem); }

  .zone-label {
    font-size: 12px;
    color: var(--text-muted);
  }

  .ride-metrics-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .metric-box {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 10px 12px;
    background: var(--bg-base);
    border-radius: 8px;
  }

  .metric-name {
    font-size: 11px;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }

  .metric-value {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    font-variant-numeric: tabular-nums;
  }

  .metric-value.optimal { color: var(--color-optimal-text); }
  .metric-value.attention { color: var(--color-attention-text); }
  .metric-value.problem { color: var(--color-problem-text); }
</style>
