<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { WeeklyComparison, Insight } from '$lib/types/dashboard';

  export let weekDays: string[];
  export let ridesPerDay: number[];
  export let maxRidesPerDay: number;
  export let weeklyComparison: WeeklyComparison | null;
  export let insights: Insight[];
</script>

<div class="grid-card">
  <div class="card-header">
    <span class="card-title">{$t('dashboard.weeklyActivity')} <InfoTip text={$t('infotips.weeklyActivity')} position="bottom" /></span>
    <span class="card-subtitle">{ridesPerDay.reduce((a, b) => a + b, 0)} {$t('dashboard.summary.rides')}</span>
  </div>
  <div class="bar-chart-enhanced">
    <div class="bar-chart-area">
      {#each weekDays as day, i}
        <div class="bar-col">
          <div class="bar-wrapper">
            {#if ridesPerDay[i] > 0}
              <span class="bar-count">{ridesPerDay[i]}</span>
            {/if}
            <div class="bar" style="height: {ridesPerDay[i] ? (ridesPerDay[i] / maxRidesPerDay) * 100 : 0}%"></div>
          </div>
          <span class="bar-label">{day.slice(0, 1)}</span>
        </div>
      {/each}
    </div>
  </div>

  {#if weeklyComparison}
    <div class="card-section">
      <span class="section-label">{$t('dashboard.vsLastWeek')}</span>
      <div class="comparison-grid">
        <div class="comp-item">
          <span class="comp-label">{$t('dashboard.comparison.rides')} <InfoTip text={$t('infotips.compRides')} position="bottom" size="sm" /></span>
          <span class="comp-value">{weeklyComparison.thisWeek.rides_count}</span>
          <span class="comp-delta {weeklyComparison.changes.rides_count >= 0 ? 'up' : 'down'}">
            {weeklyComparison.changes.rides_count >= 0 ? '+' : ''}{weeklyComparison.changes.rides_count}
          </span>
        </div>
        <div class="comp-item">
          <span class="comp-label">{$t('dashboard.comparison.hours')} <InfoTip text={$t('infotips.compHours')} position="bottom" size="sm" /></span>
          <span class="comp-value">{(weeklyComparison.thisWeek.total_duration_ms / 3600000).toFixed(1)}</span>
          <span class="comp-delta {weeklyComparison.changes.duration >= 0 ? 'up' : 'down'}">
            {weeklyComparison.changes.duration >= 0 ? '+' : ''}{(weeklyComparison.changes.duration / 3600000).toFixed(1)}
          </span>
        </div>
        <div class="comp-item">
          <span class="comp-label">{$t('dashboard.comparison.distance')} <InfoTip text={$t('infotips.compDistance')} position="bottom" size="sm" /></span>
          <span class="comp-value">{weeklyComparison.thisWeek.total_distance_km.toFixed(0)}km</span>
          <span class="comp-delta {weeklyComparison.changes.distance >= 0 ? 'up' : 'down'}">
            {weeklyComparison.changes.distance >= 0 ? '+' : ''}{weeklyComparison.changes.distance.toFixed(0)}
          </span>
        </div>
        {#if weeklyComparison.thisWeek.total_elevation > 0}
          <div class="comp-item">
            <span class="comp-label">{$t('dashboard.comparison.climb')} <InfoTip text={$t('infotips.compClimb')} position="bottom" size="sm" /></span>
            <span class="comp-value">{Math.round(weeklyComparison.thisWeek.total_elevation)}m</span>
            <span class="comp-delta {weeklyComparison.changes.elevation >= 0 ? 'up' : 'down'}">
              {weeklyComparison.changes.elevation >= 0 ? '+' : ''}{Math.round(weeklyComparison.changes.elevation)}
            </span>
          </div>
        {/if}
      </div>
    </div>
    <!-- Weekly Performance Indicators -->
    <div class="card-section">
      <span class="section-label">{$t('dashboard.sections.performance')}</span>
      <div class="performance-mini-grid">
        <div class="perf-mini-item">
          <span class="perf-mini-label">{$t('dashboard.perfStats.score')} <InfoTip text={$t('infotips.perfScore')} position="top" /></span>
          <span class="perf-mini-value">{weeklyComparison.thisWeek.avg_score.toFixed(0)}</span>
          <span class="perf-mini-delta {weeklyComparison.changes.score >= 0 ? 'up' : 'down'}">
            {weeklyComparison.changes.score >= 0 ? '+' : ''}{weeklyComparison.changes.score.toFixed(0)}
          </span>
        </div>
        <div class="perf-mini-item">
          <span class="perf-mini-label">{$t('dashboard.perfStats.optimal')} <InfoTip text={$t('infotips.perfOptimal')} position="top" /></span>
          <span class="perf-mini-value">{weeklyComparison.thisWeek.avg_zone_optimal.toFixed(0)}%</span>
          <span class="perf-mini-delta {weeklyComparison.changes.zone_optimal >= 0 ? 'up' : 'down'}">
            {weeklyComparison.changes.zone_optimal >= 0 ? '+' : ''}{weeklyComparison.changes.zone_optimal.toFixed(0)}
          </span>
        </div>
        {#if weeklyComparison.thisWeek.avg_power > 0}
          <div class="perf-mini-item">
            <span class="perf-mini-label">{$t('dashboard.perfStats.power')} <InfoTip text={$t('infotips.perfPower')} position="top" /></span>
            <span class="perf-mini-value">{Math.round(weeklyComparison.thisWeek.avg_power)}W</span>
            <span class="perf-mini-delta {weeklyComparison.changes.power >= 0 ? 'up' : 'down'}">
              {weeklyComparison.changes.power >= 0 ? '+' : ''}{Math.round(weeklyComparison.changes.power)}
            </span>
          </div>
        {/if}
        {#if weeklyComparison.thisWeek.total_energy_kj > 0}
          <div class="perf-mini-item">
            <span class="perf-mini-label">{$t('dashboard.perfStats.energy')} <InfoTip text={$t('infotips.perfEnergy')} position="top" /></span>
            <span class="perf-mini-value">{Math.round(weeklyComparison.thisWeek.total_energy_kj)}kJ</span>
            <span class="perf-mini-delta {weeklyComparison.changes.energy >= 0 ? 'up' : 'down'}">
              {weeklyComparison.changes.energy >= 0 ? '+' : ''}{Math.round(weeklyComparison.changes.energy)}
            </span>
          </div>
        {/if}
      </div>
    </div>
  {/if}

  <!-- Insights Section -->
  {#if insights.length > 0}
    <div class="card-section insights-section">
      <span class="section-label">{$t('dashboard.sections.insights')}</span>
      <div class="insights-list">
        {#each insights as insight}
          <div class="insight-item">
            <span class="insight-icon">{insight.icon}</span>
            <span class="insight-text">{insight.text}{#if insight.link}&nbsp;<a href={insight.link.href} class="insight-link">{insight.link.label}</a>{/if}</span>
          </div>
        {/each}
      </div>
    </div>
  {/if}
</div>

<style>
  .grid-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    display: flex;
    flex-direction: column;
    min-width: 0;
    overflow: hidden;
  }
  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
  .card-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .card-subtitle { font-size: 11px; color: var(--text-muted); }
  .card-section { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--border-subtle); }
  .section-label { display: block; font-size: 10px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; margin-bottom: 8px; }

  /* Bar Chart Enhanced (Weekly Activity) */
  .bar-chart-enhanced { margin-bottom: 12px; }
  .bar-chart-area {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    height: 90px;
    gap: 6px;
    padding: 8px 4px 0;
    background: var(--bg-base);
    border-radius: 8px;
  }
  .bar-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
  }
  .bar-wrapper {
    width: 100%;
    height: 65px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end;
    position: relative;
  }
  .bar-count {
    font-size: 9px;
    font-weight: 600;
    color: var(--text-secondary);
    margin-bottom: 2px;
  }
  .bar {
    width: 85%;
    max-width: 24px;
    background: linear-gradient(to top, var(--color-accent), var(--color-accent-light, var(--color-accent)));
    border-radius: 4px 4px 0 0;
    min-height: 4px;
    transition: height 0.3s ease;
  }
  .bar-label {
    font-size: 10px;
    color: var(--text-muted);
    text-transform: uppercase;
    font-weight: 500;
  }

  /* Comparison Grid */
  .comparison-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
  .comp-item { text-align: center; padding: 8px 4px; background: var(--bg-base); border-radius: 6px; }
  .comp-label { display: block; font-size: 9px; color: var(--text-muted); margin-bottom: 2px; }
  .comp-value { display: block; font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .comp-delta { display: block; font-size: 10px; font-weight: 500; margin-top: 2px; }
  .comp-delta.up { color: var(--color-optimal-text); }
  .comp-delta.down { color: var(--color-problem-text); }

  /* Performance Mini Grid */
  .performance-mini-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }
  .perf-mini-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 8px;
    background: var(--bg-base);
    border-radius: 6px;
  }
  .perf-mini-label { font-size: 10px; color: var(--text-muted); flex-shrink: 0; }
  .perf-mini-value { font-size: 13px; font-weight: 600; color: var(--text-primary); margin-left: auto; }
  .perf-mini-delta { font-size: 10px; font-weight: 600; min-width: 28px; text-align: right; }
  .perf-mini-delta.up { color: var(--color-optimal-text); }
  .perf-mini-delta.down { color: var(--color-problem-text); }

  /* Insights */
  .insights-list { display: flex; flex-direction: column; }
  .insight-item {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    padding: 6px 0;
    font-size: 11px;
    line-height: 1.4;
  }
  .insight-item:not(:last-child) {
    border-bottom: 1px solid var(--border-subtle);
    padding-bottom: 8px;
  }
  .insight-icon { font-size: 13px; flex-shrink: 0; line-height: 1.4; }
  .insight-text { flex: 1; color: var(--text-secondary); line-height: 1.4; }
  .insight-link { color: var(--color-accent); text-decoration: none; font-weight: 500; }
  .insight-link:hover { text-decoration: underline; }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .grid-card { padding: 12px; }
    .comparison-grid { grid-template-columns: repeat(2, 1fr); }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .card-title { font-size: 13px; }
    .bar-chart-area { height: 75px; }
    .bar-wrapper { height: 55px; }
    .comp-value { font-size: 13px; }
    .perf-mini-item { padding: 5px 6px; }
    .perf-mini-label { font-size: 9px; }
    .perf-mini-value { font-size: 12px; }
    .perf-mini-delta { font-size: 9px; min-width: 24px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .grid-card { padding: 10px; border-radius: 10px; }
    .card-header { margin-bottom: 10px; }
    .card-title { font-size: 12px; }
    .card-subtitle { font-size: 10px; }
    .card-section { margin-top: 10px; padding-top: 10px; }
    .section-label { font-size: 9px; margin-bottom: 6px; }
    .bar-chart-area { height: 70px; padding: 6px 3px 0; }
    .bar-wrapper { height: 50px; }
    .bar { max-width: 20px; }
    .bar-label { font-size: 9px; }
    .bar-count { font-size: 8px; }
    .comparison-grid { gap: 4px; }
    .comp-item { padding: 6px 3px; }
    .comp-label { font-size: 8px; }
    .comp-value { font-size: 12px; }
    .comp-delta { font-size: 9px; }
    .performance-mini-grid { gap: 6px; }
    .insight-item { font-size: 10px; }
    .insight-icon { font-size: 12px; }
  }
</style>
