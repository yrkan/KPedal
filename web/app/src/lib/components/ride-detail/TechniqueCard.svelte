<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { RideDetail, TechniqueStats } from '$lib/types/ride-detail';
  import { getTeStatus, getPsStatus } from '$lib/types/ride-detail';

  export let ride: RideDetail;
  export let techniqueStats: TechniqueStats | null;
</script>

<div class="grid-card technique-card">
  <div class="card-header">
    <span class="card-title">{$t('rides.detail.technique')} <InfoTip text={$t('rides.detail.techniqueTip')} position="right" size="sm" /></span>
  </div>

  <div class="tech-metrics">
    <!-- TE -->
    <div class="tech-metric-row">
      <div class="tech-metric-main">
        <span class="tech-metric-name">{$t('rides.detail.te')} <InfoTip text={$t('rides.detail.teTip')} position="right" size="sm" /></span>
        <span class="tech-metric-value {getTeStatus((ride.te_left + ride.te_right) / 2)}">{((ride.te_left + ride.te_right) / 2).toFixed(0)}%</span>
      </div>
      <div class="tech-metric-bar">
        <div class="tech-bar-track">
          <div class="tech-bar-optimal te"></div>
          <div class="tech-bar-fill te" style="width: {Math.min(100, (ride.te_left + ride.te_right) / 2)}%"></div>
          <div class="tech-bar-marker" style="left: 70%" title="70%"></div>
          <div class="tech-bar-marker" style="left: 80%" title="80%"></div>
        </div>
        <div class="tech-bar-labels te">
          <span>0</span>
          <span class="optimal-label">70-80</span>
          <span>100</span>
        </div>
      </div>
      <div class="tech-metric-lr">
        <span>L {ride.te_left.toFixed(0)}%</span>
        <span>R {ride.te_right.toFixed(0)}%</span>
      </div>
    </div>

    <!-- PS -->
    <div class="tech-metric-row">
      <div class="tech-metric-main">
        <span class="tech-metric-name">{$t('rides.detail.ps')} <InfoTip text={$t('rides.detail.psTip')} position="right" size="sm" /></span>
        <span class="tech-metric-value {getPsStatus((ride.ps_left + ride.ps_right) / 2)}">{((ride.ps_left + ride.ps_right) / 2).toFixed(0)}%</span>
      </div>
      <div class="tech-metric-bar">
        <div class="tech-bar-track">
          <div class="tech-bar-optimal ps"></div>
          <div class="tech-bar-fill ps" style="width: {Math.min(100, ((ride.ps_left + ride.ps_right) / 2) / 40 * 100)}%"></div>
          <div class="tech-bar-marker" style="left: 50%" title="20%"></div>
        </div>
        <div class="tech-bar-labels ps">
          <span>0</span>
          <span class="optimal-label">≥20</span>
          <span>40</span>
        </div>
      </div>
      <div class="tech-metric-lr">
        <span>L {ride.ps_left.toFixed(0)}%</span>
        <span>R {ride.ps_right.toFixed(0)}%</span>
      </div>
    </div>
  </div>

  {#if techniqueStats && ride.snapshots?.length >= 3}
    <div class="technique-stats">
      <div class="tech-stat">
        <span class="tech-stat-value">{techniqueStats.teOptimalTime.toFixed(0)}%</span>
        <span class="tech-stat-label">{$t('rides.detail.teInZone')} <InfoTip text={$t('rides.detail.teInZoneTip')} position="top" size="sm" /></span>
      </div>
      <div class="tech-stat">
        <span class="tech-stat-value">{techniqueStats.psOptimalTime.toFixed(0)}%</span>
        <span class="tech-stat-label">{$t('rides.detail.psInZone')} <InfoTip text={$t('rides.detail.psInZoneTip')} position="top" size="sm" /></span>
      </div>
      <div class="tech-stat">
        <span class="tech-stat-value">{techniqueStats.teStability.toFixed(0)}%</span>
        <span class="tech-stat-label">{$t('rides.detail.teStab')} <InfoTip text={$t('rides.detail.teStabTip')} position="top" size="sm" /></span>
      </div>
      <div class="tech-stat">
        <span class="tech-stat-value">{techniqueStats.psStability.toFixed(0)}%</span>
        <span class="tech-stat-label">{$t('rides.detail.psStab')} <InfoTip text={$t('rides.detail.psStabTip')} position="top" size="sm" /></span>
      </div>
    </div>
  {/if}
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

  .technique-card { grid-column: span 2; }

  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
  .card-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }

  .tech-metrics { display: flex; flex-direction: column; gap: 12px; }
  .tech-metric-row { display: flex; align-items: center; gap: 12px; }
  .tech-metric-main { display: flex; align-items: baseline; gap: 6px; min-width: 120px; }
  .tech-metric-name { font-size: 12px; font-weight: 600; color: var(--text-muted); min-width: 24px; }
  .tech-metric-value { font-size: 20px; font-weight: 700; }
  .tech-metric-value.optimal { color: var(--color-optimal-text); }
  .tech-metric-value.attention { color: var(--color-attention-text); }
  .tech-metric-value.problem { color: var(--color-problem-text); }
  .tech-metric-bar { flex: 1; position: relative; }
  .tech-bar-track { height: 6px; background: var(--bg-elevated); border-radius: 3px; overflow: visible; position: relative; }
  .tech-bar-optimal { position: absolute; top: 0; bottom: 0; background: var(--color-optimal); opacity: 0.15; border-radius: 3px; }
  .tech-bar-optimal.te { left: 70%; right: 20%; }
  .tech-bar-optimal.ps { left: 50%; right: 0; }
  .tech-bar-fill { position: absolute; top: 0; left: 0; height: 100%; border-radius: 3px; }
  .tech-bar-fill.te { background: var(--color-optimal); }
  .tech-bar-fill.ps { background: var(--color-attention); }
  .tech-bar-marker { position: absolute; top: -4px; bottom: -4px; width: 2px; background: var(--color-optimal); opacity: 0.7; border-radius: 1px; }
  .tech-bar-labels { display: flex; justify-content: space-between; font-size: 9px; color: var(--text-muted); margin-top: 2px; position: relative; }
  .tech-bar-labels .optimal-label { position: absolute; transform: translateX(-50%); color: var(--color-optimal-text); font-weight: 600; }
  .tech-bar-labels.te .optimal-label { left: 75%; }
  .tech-bar-labels.ps .optimal-label { left: 75%; }
  .tech-metric-lr { display: flex; gap: 8px; font-size: 11px; color: var(--text-muted); min-width: 90px; justify-content: flex-end; }

  .technique-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--border-subtle); }
  .tech-stat { text-align: center; padding: 5px; background: var(--bg-elevated); border-radius: 5px; }
  .tech-stat-value { display: block; font-size: 13px; font-weight: 700; color: var(--text-primary); }
  .tech-stat-label { display: block; font-size: 9px; color: var(--text-muted); margin-top: 1px; }

  @media (max-width: 768px) {
    .technique-card { grid-column: span 1; }
    .tech-metric-row { gap: 8px; }
    .tech-metric-main { min-width: 100px; gap: 4px; }
    .tech-metric-value { font-size: 18px; }
    .tech-metric-lr { min-width: 70px; font-size: 10px; gap: 6px; }
    .technique-stats { grid-template-columns: repeat(2, 1fr); }
  }

  @media (max-width: 480px) {
    .tech-metric-main { min-width: 90px; }
    .tech-metric-value { font-size: 16px; }
    .tech-metric-lr { display: none; }
    .tech-metric-bar { flex: 1; }
  }
</style>
