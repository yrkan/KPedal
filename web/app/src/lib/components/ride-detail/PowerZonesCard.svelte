<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { PowerZoneStats } from '$lib/types/ride-detail';
  import { getBalanceStatus, getTeStatus, getPsStatus } from '$lib/types/ride-detail';

  export let powerZones: PowerZoneStats[];

  $: totalMinutes = powerZones.reduce((a, z) => a + z.minutes, 0);
</script>

<div class="grid-card power-zones-card">
  <div class="card-header">
    <span class="card-title">{$t('rides.detail.byPowerZone')} <InfoTip text={$t('rides.detail.byPowerZoneTip')} position="bottom" size="sm" /></span>
  </div>
  <div class="pz-distribution">
    {#each powerZones as zone}
      <div class="pz-dist-segment" style="width: {(zone.minutes / totalMinutes) * 100}%; background: {zone.color}" title="{zone.label}: {zone.minutes}m"></div>
    {/each}
  </div>
  <div class="pz-list">
    {#each powerZones as zone}
      <div class="pz-item">
        <div class="pz-item-zone">
          <span class="pz-zone-dot" style="background: {zone.color}"></span>
          <span class="pz-zone-name">{zone.label}</span>
          <span class="pz-zone-time">{zone.minutes}m</span>
        </div>
        <div class="pz-item-metrics">
          <span class="pz-val {getBalanceStatus(50 - zone.avgBalance)}">{zone.avgBalance.toFixed(1)}%</span>
          <span class="pz-val {getTeStatus(zone.avgTe)}">{zone.avgTe.toFixed(0)}%</span>
          <span class="pz-val {getPsStatus(zone.avgPs)}">{zone.avgPs.toFixed(0)}%</span>
        </div>
      </div>
    {/each}
  </div>
  <div class="pz-legend">
    <span>{$t('rides.detail.asym')}</span>
    <span>{$t('rides.detail.te')}</span>
    <span>{$t('rides.detail.ps')}</span>
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

  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
  .card-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }

  .pz-distribution {
    display: flex;
    height: 6px;
    border-radius: 3px;
    overflow: hidden;
    margin-bottom: 10px;
  }
  .pz-dist-segment {
    height: 100%;
    min-width: 2px;
    transition: opacity 0.2s;
  }
  .pz-dist-segment:hover { opacity: 0.8; }

  .pz-list {
    display: flex;
    flex-direction: column;
  }

  .pz-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px solid var(--border-subtle);
  }
  .pz-item:last-child { border-bottom: none; }

  .pz-item-zone {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
  }

  .pz-zone-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .pz-zone-name {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .pz-zone-time {
    font-size: 10px;
    color: var(--text-muted);
    margin-left: 4px;
  }

  .pz-item-metrics {
    display: flex;
    gap: 16px;
  }

  .pz-val {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-secondary);
    font-variant-numeric: tabular-nums;
    min-width: 40px;
    text-align: right;
  }
  .pz-val.optimal { color: var(--color-optimal-text); }
  .pz-val.attention { color: var(--color-attention-text); }
  .pz-val.problem { color: var(--color-problem-text); }

  .pz-legend {
    display: flex;
    justify-content: flex-end;
    gap: 16px;
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid var(--border-subtle);
  }
  .pz-legend span {
    font-size: 9px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    min-width: 40px;
    text-align: right;
  }

  @media (max-width: 768px) {
    .pz-item-metrics { gap: 12px; }
    .pz-val { font-size: 11px; min-width: 35px; }
    .pz-legend { gap: 12px; }
    .pz-legend span { min-width: 35px; }
  }

  @media (max-width: 480px) {
    .pz-zone-name { font-size: 10px; }
    .pz-zone-time { font-size: 9px; }
    .pz-item { padding: 6px 0; }
    .pz-item-metrics { gap: 10px; }
    .pz-val { font-size: 10px; min-width: 32px; }
    .pz-legend { gap: 10px; }
    .pz-legend span { font-size: 8px; min-width: 32px; }
    .pz-zone-dot { width: 6px; height: 6px; }
    .pz-distribution { height: 5px; }
  }
</style>
