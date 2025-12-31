<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { PeriodStats } from '$lib/types/rides';
  import { formatTotalDuration, getAsymmetryClass } from '$lib/types/rides';

  export let stats: PeriodStats;
</script>

{#if stats.rides > 0}
  <div class="stats-strip animate-in">
    <div class="stat-item">
      <span class="stat-value">{stats.rides}</span>
      <span class="stat-label">{$t('rides.list.ridesLabel')} <InfoTip text={$t('rides.list.ridesTip')} position="bottom" size="sm" /></span>
    </div>
    <div class="stat-item">
      <span class="stat-value">{formatTotalDuration(stats.totalDuration)}</span>
    </div>
    {#if stats.totalDistance > 0}
      <div class="stat-item">
        <span class="stat-value">{stats.totalDistance.toFixed(0)}</span>
        <span class="stat-label">{$t('rides.list.kmLabel')} <InfoTip text={$t('rides.list.kmTip')} position="bottom" size="sm" /></span>
      </div>
    {/if}
    {#if stats.totalElevation > 0}
      <div class="stat-item">
        <span class="stat-value">{Math.round(stats.totalElevation)}</span>
        <span class="stat-label">{$t('rides.list.elevLabel')} <InfoTip text={$t('rides.list.elevTip')} position="bottom" size="sm" /></span>
      </div>
    {/if}
    <div class="stat-item">
      <span class="stat-value {getAsymmetryClass(stats.avgAsymmetry)}">{stats.avgAsymmetry.toFixed(1)}%</span>
      <span class="stat-label">{$t('rides.list.asymLabel')} <InfoTip text={$t('rides.list.asymTip')} position="bottom" size="sm" /></span>
    </div>
    <div class="stat-item">
      <span class="stat-value">{stats.avgTe.toFixed(0)}%</span>
      <span class="stat-label">{$t('rides.list.teLabel')} <InfoTip text={$t('rides.list.teTip')} position="bottom" size="sm" /></span>
    </div>
    <div class="stat-item">
      <span class="stat-value">{stats.avgPs.toFixed(0)}%</span>
      <span class="stat-label">{$t('rides.list.psLabel')} <InfoTip text={$t('rides.list.psTip')} position="bottom" size="sm" /></span>
    </div>
    <div class="stat-item highlight">
      <span class="stat-value">{stats.avgOptimal.toFixed(0)}%</span>
      <span class="stat-label">{$t('rides.list.optimalLabel')} <InfoTip text={$t('rides.list.optimalTip')} position="bottom" size="sm" /></span>
    </div>
    {#if stats.avgPower > 0}
      <div class="stat-item">
        <span class="stat-value">{Math.round(stats.avgPower)}</span>
        <span class="stat-label">{$t('rides.list.wAvgLabel')} <InfoTip text={$t('rides.list.wAvgTip')} position="bottom" size="sm" /></span>
      </div>
      {#if stats.avgNP > 0}
        <div class="stat-item highlight">
          <span class="stat-value">{Math.round(stats.avgNP)}</span>
          <span class="stat-label">{$t('rides.list.npLabel')} <InfoTip text={$t('rides.list.npTip')} position="bottom" size="sm" /></span>
        </div>
      {/if}
      {#if stats.totalEnergy > 0}
        <div class="stat-item">
          <span class="stat-value">{Math.round(stats.totalEnergy)}</span>
          <span class="stat-label">{$t('rides.list.kjLabel')} <InfoTip text={$t('rides.list.kjTip')} position="bottom" size="sm" /></span>
        </div>
      {/if}
    {/if}
  </div>
{/if}

<style>
  .stats-strip {
    display: flex;
    flex-wrap: nowrap;
    gap: 12px;
    padding: 14px 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }

  .stats-strip::-webkit-scrollbar {
    display: none;
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

  @media (max-width: 640px) {
    .stats-strip {
      padding: 12px;
      gap: 8px;
    }

    .stat-item {
      padding: 6px 10px;
    }

    .stat-value {
      font-size: 14px;
    }

    .stat-label {
      font-size: 11px;
    }
  }
</style>
