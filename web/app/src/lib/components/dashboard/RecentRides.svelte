<script lang="ts">
  import { t, locale } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { Ride } from '$lib/types/dashboard';
  import { formatDate, formatRelativeTime, formatDuration, getBalanceStatus } from '$lib/utils/dashboard';

  export let recentRides: Ride[];
</script>

<div class="recent-rides animate-in">
  <div class="card-header">
    <span class="card-title">{$t('dashboard.recentRides')} <InfoTip text={$t('infotips.recentRides')} position="bottom" /></span>
    <a href="/rides" class="view-all">{$t('dashboard.viewAll')}</a>
  </div>
  <div class="rides-table-wrap">
    <table class="rides-table">
      <thead>
        <tr>
          <th class="col-date">{$t('dashboard.table.date')}</th>
          <th class="col-duration">{$t('dashboard.table.duration')}</th>
          <th class="col-asymmetry">{$t('dashboard.table.asym')} <InfoTip text={$t('infotips.tableAsym')} position="bottom" size="sm" /></th>
          <th class="col-balance">{$t('metrics.leftRight')}</th>
          <th class="col-te">{$t('metrics.teShort')} <InfoTip text={$t('infotips.te')} position="bottom" size="sm" /></th>
          <th class="col-ps">{$t('metrics.psShort')} <InfoTip text={$t('infotips.ps')} position="bottom" size="sm" /></th>
          <th class="col-zones">{$t('dashboard.table.zones')} <InfoTip text={$t('infotips.tableZones')} position="bottom" size="sm" /></th>
          {#if recentRides.some(r => r.power_avg > 0)}<th class="col-power">{$t('dashboard.table.power')}</th>{/if}
        </tr>
      </thead>
      <tbody>
        {#each recentRides.slice(0, 5) as ride}
          <tr on:click={() => window.location.href = `/rides/${ride.id}`}>
            <td class="col-date">
              <span class="date-primary">{formatDate(ride.timestamp)}</span>
              <span class="date-secondary">{formatRelativeTime(ride.timestamp, { today: $t('common.today'), yesterday: $t('common.yesterday'), daysAgo: $t('common.daysAgo') }, $locale)}</span>
            </td>
            <td class="col-duration">{formatDuration(ride.duration_ms)}</td>
            <td class="col-asymmetry">
              <span class="asymmetry-value {getBalanceStatus(ride.balance_left)}">{Math.abs(ride.balance_left - 50).toFixed(1)}%</span>
              <span class="dominance">{ride.balance_left > 50 ? $t('metrics.left') : ride.balance_left < 50 ? $t('metrics.right') : ''}</span>
            </td>
            <td class="col-balance">{ride.balance_left.toFixed(0)} / {(100 - ride.balance_left).toFixed(0)}</td>
            <td class="col-te">{ride.te_left.toFixed(0)}/{ride.te_right.toFixed(0)}</td>
            <td class="col-ps">{ride.ps_left.toFixed(0)}/{ride.ps_right.toFixed(0)}</td>
            <td class="col-zones">
              <div class="zone-bar-mini">
                <div class="zone-segment optimal" style="width: {ride.zone_optimal}%"></div>
                <div class="zone-segment attention" style="width: {ride.zone_attention}%"></div>
                <div class="zone-segment problem" style="width: {ride.zone_problem}%"></div>
              </div>
            </td>
            {#if recentRides.some(r => r.power_avg > 0)}
              <td class="col-power">{ride.power_avg > 0 ? `${Math.round(ride.power_avg)}W` : '—'}</td>
            {/if}
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
</div>

<style>
  .recent-rides {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    overflow: hidden;
  }
  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
  .card-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .view-all { font-size: 12px; color: var(--text-tertiary); text-decoration: none; }
  .view-all:hover { color: var(--color-accent); }

  .rides-table-wrap { margin: 0 -14px -14px; overflow-x: auto; -webkit-overflow-scrolling: touch; scrollbar-width: none; }
  .rides-table-wrap::-webkit-scrollbar { display: none; }
  .rides-table { width: 100%; border-collapse: collapse; font-size: 12px; }
  .rides-table th { text-align: left; padding: 10px 12px; font-size: 10px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; background: var(--bg-elevated); border-top: 1px solid var(--border-subtle); white-space: nowrap; }
  .rides-table td { padding: 10px 12px; border-top: 1px solid var(--border-subtle); vertical-align: middle; }
  .rides-table tbody tr { cursor: pointer; transition: background 0.15s; }
  .rides-table tbody tr:hover { background: var(--bg-hover); }
  .rides-table tbody tr:last-child td { border-bottom: none; }

  .col-date { white-space: nowrap; }
  .date-primary { font-weight: 500; color: var(--text-primary); display: block; }
  .date-secondary { font-size: 10px; color: var(--text-muted); }
  .col-duration { font-weight: 500; color: var(--text-secondary); white-space: nowrap; }
  .col-asymmetry { white-space: nowrap; }
  .asymmetry-value { font-weight: 600; }
  .asymmetry-value.optimal { color: var(--color-optimal-text); }
  .asymmetry-value.attention { color: var(--color-attention-text); }
  .asymmetry-value.problem { color: var(--color-problem-text); }
  .dominance { margin-left: 4px; font-size: 10px; color: var(--text-muted); font-weight: 500; }
  .col-balance { color: var(--text-secondary); font-variant-numeric: tabular-nums; white-space: nowrap; }
  .col-te, .col-ps { color: var(--text-secondary); font-variant-numeric: tabular-nums; white-space: nowrap; }
  .col-zones { width: 80px; min-width: 60px; }
  .zone-bar-mini { height: 6px; display: flex; border-radius: 3px; overflow: hidden; background: var(--bg-elevated); }
  .zone-segment { height: 100%; }
  .zone-segment.optimal { background: var(--color-optimal); }
  .zone-segment.attention { background: var(--color-attention); }
  .zone-segment.problem { background: var(--color-problem); }
  .col-power { color: var(--text-secondary); font-variant-numeric: tabular-nums; white-space: nowrap; }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .recent-rides { padding: 12px; }
    .rides-table-wrap { margin: 0 -12px -12px; }
    .rides-table { min-width: 450px; font-size: 11px; }
    .rides-table th { padding: 6px 8px; font-size: 9px; }
    .rides-table td { padding: 8px 6px; }
    .rides-table .col-balance,
    .rides-table .col-te,
    .rides-table .col-ps { display: none; }
    .rides-table .col-zones { width: 70px; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .card-title { font-size: 13px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .recent-rides { padding: 10px; border-radius: 10px; }
    .card-header { margin-bottom: 10px; }
    .card-title { font-size: 12px; }
    .view-all { font-size: 11px; }
    .rides-table-wrap { margin: 0 -10px -10px; }
    .rides-table { min-width: 320px; font-size: 10px; }
    .rides-table th { padding: 5px 4px; font-size: 8px; }
    .rides-table td { padding: 6px 4px; }
    .rides-table .col-power { display: none; }
    .rides-table .col-zones { width: 50px; }
  }
</style>
