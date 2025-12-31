<script lang="ts">
  import { t } from '$lib/i18n';
  import type { PeriodStats, Progress } from '$lib/types/dashboard';

  export let periodStats: PeriodStats;
  export let selectedPeriod: '7' | '14' | '30' | '60';
  export let scoreProgress: Progress | null;
  export let optimalProgress: Progress | null;
  export let teProgress: Progress | null;
  export let psProgress: Progress | null;
  export let durationProgress: Progress | null;
  export let distanceProgress: Progress | null;
</script>

<div class="progress-row-card animate-in">
  <div class="progress-header">
    <span class="progress-title">{$t('dashboard.progress.title', { values: { period: selectedPeriod } })}</span>
  </div>
  <div class="progress-items">
    {#if scoreProgress}
      <div class="progress-item">
        <span class="progress-item-label">{$t('dashboard.progress.score')}</span>
        <span class="progress-item-now">{periodStats.score.toFixed(0)}</span>
        <span class="progress-item-change {scoreProgress.direction}">{scoreProgress.direction === 'up' ? '↑' : scoreProgress.direction === 'down' ? '↓' : ''}{scoreProgress.value.toFixed(0)}</span>
      </div>
    {/if}
    {#if optimalProgress}
      <div class="progress-item">
        <span class="progress-item-label">{$t('dashboard.progress.optimalZone')}</span>
        <span class="progress-item-now">{periodStats.zoneOptimal.toFixed(0)}%</span>
        <span class="progress-item-change {optimalProgress.direction}">{optimalProgress.direction === 'up' ? '↑' : optimalProgress.direction === 'down' ? '↓' : ''}{optimalProgress.value.toFixed(0)}%</span>
      </div>
    {/if}
    {#if teProgress}
      <div class="progress-item">
        <span class="progress-item-label">{$t('metrics.teShort')}</span>
        <span class="progress-item-now">{periodStats.te.toFixed(0)}%</span>
        <span class="progress-item-change {teProgress.direction}">{teProgress.direction === 'up' ? '↑' : teProgress.direction === 'down' ? '↓' : ''}{teProgress.value.toFixed(0)}</span>
      </div>
    {/if}
    {#if psProgress}
      <div class="progress-item">
        <span class="progress-item-label">{$t('metrics.psShort')}</span>
        <span class="progress-item-now">{periodStats.ps.toFixed(0)}%</span>
        <span class="progress-item-change {psProgress.direction}">{psProgress.direction === 'up' ? '↑' : psProgress.direction === 'down' ? '↓' : ''}{psProgress.value.toFixed(0)}</span>
      </div>
    {/if}
    {#if durationProgress}
      <div class="progress-item">
        <span class="progress-item-label">{$t('dashboard.progress.time')}</span>
        <span class="progress-item-now">{(periodStats.duration / 3600000).toFixed(1)}h</span>
        <span class="progress-item-change {durationProgress.direction}">{durationProgress.direction === 'up' ? '↑' : durationProgress.direction === 'down' ? '↓' : ''}{durationProgress.value.toFixed(1)}h</span>
      </div>
    {/if}
    {#if distanceProgress}
      <div class="progress-item">
        <span class="progress-item-label">{$t('dashboard.progress.distance')}</span>
        <span class="progress-item-now">{periodStats.totalDistance.toFixed(0)}km</span>
        <span class="progress-item-change {distanceProgress.direction}">{distanceProgress.direction === 'up' ? '↑' : distanceProgress.direction === 'down' ? '↓' : ''}{distanceProgress.value.toFixed(0)}</span>
      </div>
    {/if}
  </div>
</div>

<style>
  .progress-row-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 16px;
  }
  .progress-header {
    margin-bottom: 12px;
  }
  .progress-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-secondary);
  }
  .progress-items {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
    gap: 8px;
  }
  .progress-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 10px 8px;
    background: var(--bg-base);
    border-radius: 8px;
    text-align: center;
  }
  .progress-item-label {
    font-size: 10px;
    color: var(--text-muted);
    margin-bottom: 4px;
  }
  .progress-item-now {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    line-height: 1.2;
  }
  .progress-item-change {
    font-size: 12px;
    font-weight: 600;
    margin-top: 2px;
  }
  .progress-item-change.up { color: var(--color-optimal-text); }
  .progress-item-change.down { color: var(--color-problem-text); }
  .progress-item-change.same { color: var(--text-muted); }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .progress-row-card { padding: 12px; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .progress-row-card { padding: 10px; }
    .progress-item { padding: 8px 6px; }
    .progress-item-now { font-size: 16px; }
    .progress-item-change { font-size: 11px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .progress-row-card { padding: 8px; border-radius: 10px; margin-bottom: 12px; }
    .progress-header { margin-bottom: 10px; }
    .progress-title { font-size: 12px; }
    .progress-items { gap: 6px; }
    .progress-item { padding: 6px 4px; border-radius: 6px; }
    .progress-item-label { font-size: 9px; margin-bottom: 2px; }
    .progress-item-now { font-size: 14px; }
    .progress-item-change { font-size: 10px; }
  }
</style>
