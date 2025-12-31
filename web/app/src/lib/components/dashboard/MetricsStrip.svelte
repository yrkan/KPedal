<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { PeriodStats } from '$lib/types/dashboard';

  export let periodStats: PeriodStats;
</script>

<div class="metrics-compact animate-in">
  {#if periodStats.avgPower > 0}
    <div class="metric-chip">
      <span class="metric-chip-val">{Math.round(periodStats.avgPower)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.wAvg')} <InfoTip text={$t('infotips.powerAvg')} position="bottom" /></span>
    </div>
  {/if}
  {#if periodStats.maxPower > 0}
    <div class="metric-chip">
      <span class="metric-chip-val">{Math.round(periodStats.maxPower)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.wMax')} <InfoTip text={$t('infotips.powerMax')} position="bottom" /></span>
    </div>
  {/if}
  {#if periodStats.avgNormalizedPower > 0}
    <div class="metric-chip accent">
      <span class="metric-chip-val">{Math.round(periodStats.avgNormalizedPower)}</span>
      <span class="metric-chip-unit">
        {$t('metrics.npShort')}
        <InfoTip text={$t('infotips.np')} position="bottom" />
      </span>
    </div>
  {/if}
  {#if periodStats.avgCadence > 0}
    <div class="metric-chip">
      <span class="metric-chip-val">{Math.round(periodStats.avgCadence)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.rpm')} <InfoTip text={$t('infotips.cadence')} position="bottom" /></span>
    </div>
  {/if}
  {#if periodStats.avgHr > 0}
    <div class="metric-chip">
      <span class="metric-chip-val">{Math.round(periodStats.avgHr)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.bpmAvg')} <InfoTip text={$t('infotips.hrAvg')} position="bottom" /></span>
    </div>
  {/if}
  {#if periodStats.maxHr > 0}
    <div class="metric-chip">
      <span class="metric-chip-val">{Math.round(periodStats.maxHr)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.bpmMax')} <InfoTip text={$t('infotips.hrMax')} position="bottom" /></span>
    </div>
  {/if}
  {#if periodStats.avgSpeed > 0}
    <div class="metric-chip">
      <span class="metric-chip-val">{periodStats.avgSpeed.toFixed(1)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.kmh')} <InfoTip text={$t('infotips.speed')} position="bottom" /></span>
    </div>
  {/if}
  {#if periodStats.totalElevationGain > 0}
    <div class="metric-chip elevation">
      <span class="metric-chip-val">{Math.round(periodStats.totalElevationGain)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.mUp')} <InfoTip text={$t('infotips.elevationGain')} position="bottom" /></span>
    </div>
  {/if}
  {#if periodStats.totalElevationLoss > 0}
    <div class="metric-chip elevation">
      <span class="metric-chip-val">{Math.round(periodStats.totalElevationLoss)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.mDown')} <InfoTip text={$t('infotips.elevationLoss')} position="bottom" /></span>
    </div>
  {/if}
  {#if periodStats.maxGrade > 0}
    <div class="metric-chip">
      <span class="metric-chip-val">{periodStats.maxGrade.toFixed(1)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.grade')} <InfoTip text={$t('infotips.grade')} position="bottom" /></span>
    </div>
  {/if}
  {#if periodStats.totalEnergy > 0}
    <div class="metric-chip energy">
      <span class="metric-chip-val">{Math.round(periodStats.totalEnergy)}</span>
      <span class="metric-chip-unit">{$t('dashboard.units.kJ')} <InfoTip text={$t('infotips.energy')} position="bottom" /></span>
    </div>
  {/if}
</div>

<style>
  .metrics-compact {
    display: flex;
    flex-wrap: nowrap;
    justify-content: flex-start;
    gap: 10px;
    margin-bottom: 16px;
    padding: 16px 20px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }
  .metrics-compact::-webkit-scrollbar { display: none; }

  .metric-chip {
    display: flex;
    align-items: baseline;
    gap: 5px;
    padding: 8px 14px;
    background: var(--bg-base);
    border-radius: 8px;
    flex-shrink: 0;
    white-space: nowrap;
  }
  .metric-chip-val { font-size: 16px; font-weight: 600; color: var(--text-primary); }
  .metric-chip-unit { display: inline-flex; align-items: center; font-size: 12px; color: var(--text-muted); }
  .metric-chip.accent { background: var(--color-accent-soft, rgba(94, 232, 156, 0.1)); }
  .metric-chip.accent .metric-chip-val { color: var(--color-accent, #5ee89c); }
  .metric-chip.elevation .metric-chip-val { color: var(--color-accent); }
  .metric-chip.energy .metric-chip-val { color: var(--color-optimal-text); }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .metrics-compact { padding: 12px 16px; gap: 8px; }
    .metric-chip { padding: 6px 12px; }
    .metric-chip-val { font-size: 14px; }
    .metric-chip-unit { font-size: 11px; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .metrics-compact { padding: 10px 12px; gap: 8px; }
    .metric-chip { padding: 6px 10px; }
    .metric-chip-val { font-size: 13px; }
    .metric-chip-unit { font-size: 10px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .metrics-compact { padding: 8px 10px; gap: 6px; border-radius: 10px; margin-bottom: 12px; }
    .metric-chip { padding: 5px 8px; border-radius: 6px; }
    .metric-chip-val { font-size: 12px; }
    .metric-chip-unit { font-size: 9px; }
  }
</style>
