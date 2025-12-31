<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { RideDetail } from '$lib/types/ride-detail';

  export let ride: RideDetail;
</script>

<div class="perf-strip animate-in">
  {#if ride.power_avg > 0}
    <div class="perf-chip">
      <span class="perf-val">{Math.round(ride.power_avg)}</span>
      <span class="perf-unit">W <InfoTip text={$t('rides.perf.wattsTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.power_max > 0}
    <div class="perf-chip">
      <span class="perf-val">{Math.round(ride.power_max)}</span>
      <span class="perf-unit">W max <InfoTip text={$t('rides.perf.maxWattsTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.normalized_power > 0}
    <div class="perf-chip">
      <span class="perf-val">{Math.round(ride.normalized_power)}</span>
      <span class="perf-unit">NP <InfoTip text={$t('rides.perf.npTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.hr_avg > 0}
    <div class="perf-chip">
      <span class="perf-val">{Math.round(ride.hr_avg)}</span>
      <span class="perf-unit">bpm <InfoTip text={$t('rides.perf.bpmTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.hr_max > 0}
    <div class="perf-chip">
      <span class="perf-val">{Math.round(ride.hr_max)}</span>
      <span class="perf-unit">max <InfoTip text={$t('rides.perf.maxBpmTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.cadence_avg > 0}
    <div class="perf-chip">
      <span class="perf-val">{Math.round(ride.cadence_avg)}</span>
      <span class="perf-unit">rpm <InfoTip text={$t('rides.perf.rpmTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.speed_avg > 0}
    <div class="perf-chip">
      <span class="perf-val">{ride.speed_avg.toFixed(1)}</span>
      <span class="perf-unit">km/h <InfoTip text={$t('rides.perf.speedTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.energy_kj > 0}
    <div class="perf-chip">
      <span class="perf-val">{Math.round(ride.energy_kj)}</span>
      <span class="perf-unit">kJ <InfoTip text={$t('rides.perf.kjTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.elevation_gain > 0}
    <div class="perf-chip">
      <span class="perf-val">+{Math.round(ride.elevation_gain)}</span>
      <span class="perf-unit">m <InfoTip text={$t('rides.perf.elevGainTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.elevation_loss > 0}
    <div class="perf-chip">
      <span class="perf-val">-{Math.round(ride.elevation_loss)}</span>
      <span class="perf-unit">m <InfoTip text={$t('rides.perf.elevLossTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
  {#if ride.grade_max > 0}
    <div class="perf-chip">
      <span class="perf-val">{ride.grade_max.toFixed(0)}%</span>
      <span class="perf-unit">grade <InfoTip text={$t('rides.perf.gradeTip')} position="bottom" size="sm" /></span>
    </div>
  {/if}
</div>

<style>
  .perf-strip {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 12px;
    padding: 10px 12px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
  }

  .perf-chip {
    display: flex;
    align-items: baseline;
    gap: 4px;
    padding: 6px 10px;
    background: var(--bg-base);
    border-radius: 6px;
  }

  .perf-val { font-size: 15px; font-weight: 600; color: var(--text-primary); }
  .perf-unit { font-size: 11px; color: var(--text-muted); }

  @media (max-width: 768px) {
    .perf-strip { padding: 10px 12px; gap: 6px; }
    .perf-chip { padding: 4px 8px; }
    .perf-val { font-size: 14px; }
    .perf-unit { font-size: 10px; }
  }
</style>
