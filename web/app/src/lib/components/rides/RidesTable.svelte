<script lang="ts">
  import { t, locale } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { Ride } from '$lib/types/rides';
  import { formatDate, formatTime, formatDuration, getAsymmetry, getAsymmetryClass, getDominance } from '$lib/types/rides';

  export let rides: Ride[];
  export let deletingId: number | null = null;
  export let onRideClick: (id: number) => void;
  export let onDeleteRide: (id: number, event: Event) => void;

  $: hasPower = rides.some(r => r.power_avg > 0);
</script>

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
        {#if hasPower}
          <th class="col-power">{$t('rides.list.powerCol')} <InfoTip text={$t('rides.list.powerColTip')} position="bottom" size="sm" /></th>
        {/if}
        <th class="col-actions"></th>
      </tr>
    </thead>
    <tbody>
      {#each rides as ride}
        <tr on:click={() => onRideClick(ride.id)} class="ride-row" data-ride-id={ride.id}>
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
          {#if hasPower}
            <td class="col-power">{ride.power_avg > 0 ? `${Math.round(ride.power_avg)}W` : '—'}</td>
          {/if}
          <td class="col-actions">
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
          </td>
        </tr>
      {/each}
    </tbody>
  </table>
</div>

<style>
  .data-table-wrapper {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 14px;
    overflow-x: auto;
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
  }

  .data-table th {
    padding: 12px 16px;
    text-align: left;
    font-weight: 500;
    color: var(--text-muted);
    font-size: 12px;
    border-bottom: 1px solid var(--border-subtle);
    white-space: nowrap;
  }

  .data-table td {
    padding: 12px 16px;
    border-bottom: 1px solid var(--border-subtle);
    color: var(--text-secondary);
  }

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
    transition: all 0.15s;
    background: transparent;
    border: none;
  }

  .ride-row:hover .delete-btn { opacity: 1; }
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

  @media (max-width: 768px) {
    .data-table th,
    .data-table td {
      padding: 10px 12px;
    }

    .col-balance,
    .col-te,
    .col-ps {
      display: none;
    }
  }
</style>
