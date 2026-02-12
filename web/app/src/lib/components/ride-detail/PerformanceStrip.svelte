<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { RideDetail } from '$lib/types/ride-detail';

  export let ride: RideDetail;

  $: hasPower = ride.power_avg > 0 || ride.power_max > 0 || ride.normalized_power > 0;
  $: hasHr = ride.hr_avg > 0 || ride.hr_max > 0;
  $: hasElevation = ride.elevation_gain > 0 || ride.elevation_loss > 0 || ride.grade_avg > 0 || ride.grade_max > 0;
  $: hasCadence = ride.cadence_avg > 0;
  $: hasSpeed = ride.distance_km > 0 || ride.speed_avg > 0;
  $: hasEnergy = ride.energy_kj > 0;
  $: kjPerHour = ride.energy_kj > 0 && ride.duration_ms > 0
    ? Math.round(ride.energy_kj / (ride.duration_ms / 3600000))
    : 0;
</script>

<div class="perf-strip animate-in">
  {#if hasPower}
    <div class="perf-row">
      <svg class="row-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"/></svg>
      <div class="perf-chips">
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
      </div>
    </div>
  {/if}

  {#if hasHr}
    <div class="perf-row">
      <svg class="row-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      <div class="perf-chips">
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
      </div>
    </div>
  {/if}

  {#if hasElevation}
    <div class="perf-row">
      <svg class="row-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m8 3 4 8 5-5 5 15H2L8 3z"/></svg>
      <div class="perf-chips">
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
        {#if ride.grade_avg > 0}
          <div class="perf-chip">
            <span class="perf-val">{ride.grade_avg.toFixed(1)}%</span>
            <span class="perf-unit">avg <InfoTip text={$t('rides.perf.gradeAvgTip')} position="bottom" size="sm" /></span>
          </div>
        {/if}
        {#if ride.grade_max > 0}
          <div class="perf-chip">
            <span class="perf-val">{ride.grade_max.toFixed(0)}%</span>
            <span class="perf-unit">max <InfoTip text={$t('rides.perf.gradeTip')} position="bottom" size="sm" /></span>
          </div>
        {/if}
      </div>
    </div>
  {/if}

  {#if hasCadence}
    <div class="perf-row">
      <svg class="row-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/></svg>
      <div class="perf-chips">
        <div class="perf-chip">
          <span class="perf-val">{Math.round(ride.cadence_avg)}</span>
          <span class="perf-unit">rpm <InfoTip text={$t('rides.perf.rpmTip')} position="bottom" size="sm" /></span>
        </div>
      </div>
    </div>
  {/if}

  {#if hasSpeed}
    <div class="perf-row">
      <svg class="row-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 8 12 12 14 14"/></svg>
      <div class="perf-chips">
        {#if ride.distance_km > 0}
          <div class="perf-chip">
            <span class="perf-val">{ride.distance_km.toFixed(1)}</span>
            <span class="perf-unit">km</span>
          </div>
        {/if}
        {#if ride.speed_avg > 0}
          <div class="perf-chip">
            <span class="perf-val">{ride.speed_avg.toFixed(1)}</span>
            <span class="perf-unit">km/h <InfoTip text={$t('rides.perf.speedTip')} position="bottom" size="sm" /></span>
          </div>
        {/if}
      </div>
    </div>
  {/if}

  {#if hasEnergy}
    <div class="perf-row">
      <svg class="row-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="4"/></svg>
      <div class="perf-chips">
        <div class="perf-chip">
          <span class="perf-val">{Math.round(ride.energy_kj)}</span>
          <span class="perf-unit">kJ <InfoTip text={$t('rides.perf.kjTip')} position="bottom" size="sm" /></span>
        </div>
        {#if kjPerHour > 0}
          <div class="perf-chip">
            <span class="perf-val">{kjPerHour}</span>
            <span class="perf-unit">kJ/h <InfoTip text={$t('rides.perf.kjhTip')} position="bottom" size="sm" /></span>
          </div>
        {/if}
      </div>
    </div>
  {/if}
</div>

<style>
  .perf-strip {
    display: flex;
    flex-direction: column;
    gap: 2px;
    margin-bottom: 12px;
    padding: 10px 12px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
  }

  .perf-row {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .row-icon {
    width: 16px;
    height: 16px;
    color: var(--text-muted);
    flex-shrink: 0;
  }

  .perf-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
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
    .perf-strip { padding: 10px 12px; gap: 2px; }
    .perf-chip { padding: 4px 8px; }
    .perf-val { font-size: 14px; }
    .perf-unit { font-size: 10px; }
  }
</style>
