<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { Snapshot } from '$lib/types/ride-detail';
  import { getBalanceStatus, getTeStatus, getPsStatus } from '$lib/types/ride-detail';

  export let snapshots: Snapshot[];

  $: hasPower = snapshots.some(s => s.power_avg > 0);
  $: hasHr = snapshots.some(s => s.hr_avg > 0);
</script>

<div class="grid-card wide animate-in">
  <div class="card-header">
    <span class="card-title">{$t('rides.minuteByMinute')} <InfoTip text={$t('rides.detail.minuteByMinuteTip')} position="right" size="sm" /></span>
    <span class="card-subtitle">{snapshots.length} {$t('rides.detail.points')}</span>
  </div>
  <div class="table-wrapper">
    <table class="data-table">
      <thead>
        <tr>
          <th>{$t('rides.detail.min')}</th>
          <th>{$t('rides.detail.balance')} <InfoTip text={$t('rides.detail.balanceTip')} position="bottom" size="sm" /></th>
          <th class="col-te">{$t('rides.detail.te')} <InfoTip text={$t('rides.detail.teTip2')} position="bottom" size="sm" /></th>
          <th class="col-ps">{$t('rides.detail.ps')} <InfoTip text={$t('rides.detail.psTip2')} position="bottom" size="sm" /></th>
          {#if hasPower}<th class="col-power">{$t('rides.detail.power')}</th>{/if}
          {#if hasHr}<th class="col-hr">{$t('rides.detail.hr')}</th>{/if}
          <th>{$t('rides.detail.zone')} <InfoTip text={$t('rides.detail.zoneTip')} position="bottom" size="sm" /></th>
        </tr>
      </thead>
      <tbody>
        {#each snapshots as s}
          <tr class="zone-{s.zone_status.toLowerCase()}">
            <td class="col-min">{s.minute_index + 1}</td>
            <td><span class="{getBalanceStatus(s.balance_left)}">{s.balance_left}/{s.balance_right}</span></td>
            <td class="col-te"><span class="{getTeStatus((s.te_left + s.te_right) / 2)}">{s.te_left}/{s.te_right}</span></td>
            <td class="col-ps"><span class="{getPsStatus((s.ps_left + s.ps_right) / 2)}">{s.ps_left}/{s.ps_right}</span></td>
            {#if hasPower}<td class="col-power">{s.power_avg > 0 ? `${s.power_avg}W` : '—'}</td>{/if}
            {#if hasHr}<td class="col-hr">{s.hr_avg > 0 ? s.hr_avg : '—'}</td>{/if}
            <td><span class="zone-badge {s.zone_status.toLowerCase()}">{$t(`zones.${s.zone_status.toLowerCase()}`)}</span></td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
</div>

<style>
  .grid-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    padding: 12px;
    min-width: 0;
    overflow: hidden;
  }

  .grid-card.wide { grid-column: span 2; }

  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
  .card-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }
  .card-subtitle { font-size: 11px; color: var(--text-muted); }

  .table-wrapper {
    border-radius: 10px;
    max-height: 400px;
    overflow: auto;
    scrollbar-width: none;
    border: 1px solid var(--border-subtle);
  }
  .table-wrapper::-webkit-scrollbar { display: none; }

  .data-table { width: 100%; border-collapse: collapse; font-size: 12px; min-width: 380px; background: var(--bg-surface); }
  .data-table th {
    text-align: left;
    padding: 10px 10px;
    font-size: 10px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.4px;
    background: var(--bg-elevated);
    border-bottom: 1px solid var(--border-subtle);
    position: sticky;
    top: 0;
    z-index: 1;
    white-space: nowrap;
  }
  .data-table td {
    padding: 8px 10px;
    border-bottom: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    font-variant-numeric: tabular-nums;
    vertical-align: middle;
    background: var(--bg-surface);
  }
  .data-table tbody tr {
    transition: background 0.15s;
  }
  .data-table tbody tr:hover td { background: var(--bg-hover); }
  .data-table tr:last-child td { border-bottom: none; }
  .col-min {
    font-weight: 600;
    color: var(--text-primary);
    width: 36px;
  }

  .data-table :global(.optimal) { color: var(--color-optimal-text); font-weight: 500; }
  .data-table :global(.attention) { color: var(--color-attention-text); font-weight: 500; }
  .data-table :global(.problem) { color: var(--color-problem-text); font-weight: 500; }

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
    .grid-card.wide { grid-column: span 1; }
  }

  @media (max-width: 480px) {
    /* Hide TE and PS columns on small screens to fit essential data */
    .col-te,
    .col-ps {
      display: none;
    }

    .data-table {
      min-width: 280px;
    }

    .data-table th,
    .data-table td {
      padding: 6px 8px;
    }
  }
</style>
