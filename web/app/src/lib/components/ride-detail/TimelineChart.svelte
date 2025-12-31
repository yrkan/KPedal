<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { Snapshot, ChartData } from '$lib/types/ride-detail';
  import { getChartData, getChartColor, formatDuration } from '$lib/types/ride-detail';

  export let snapshots: Snapshot[];
  export let durationMs: number;

  const CHART_HEIGHT = 80;
  const CHART_PADDING = 8;

  let activeChart: 'balance' | 'te' | 'ps' | 'power' | 'hr' = 'balance';
  let hoveredPoint: { index: number; x: number; y: number; value: number; minute: number } | null = null;
  let chartWidth = 300;

  $: chartData = getChartData(snapshots, activeChart, chartWidth, CHART_HEIGHT, CHART_PADDING);
  $: hasPower = snapshots.some(s => s.power_avg > 0);
  $: hasHr = snapshots.some(s => s.hr_avg > 0);
</script>

<div class="grid-card wide timeline-card animate-in">
  <div class="card-header">
    <span class="card-title">{$t('rides.timeline')} <InfoTip text={$t('rides.detail.timelineTip')} position="right" size="sm" /></span>
    <div class="chart-tabs">
      <button class="chart-tab" class:active={activeChart === 'balance'} on:click={() => activeChart = 'balance'}>
        <span class="tab-icon" style="background: var(--color-accent)"></span>
        {$t('rides.detail.asym')}
      </button>
      <button class="chart-tab" class:active={activeChart === 'te'} on:click={() => activeChart = 'te'}>
        <span class="tab-icon" style="background: var(--color-optimal)"></span>
        {$t('rides.detail.te')}
      </button>
      <button class="chart-tab" class:active={activeChart === 'ps'} on:click={() => activeChart = 'ps'}>
        <span class="tab-icon" style="background: var(--color-attention)"></span>
        {$t('rides.detail.ps')}
      </button>
      {#if hasPower}
        <button class="chart-tab" class:active={activeChart === 'power'} on:click={() => activeChart = 'power'}>
          <span class="tab-icon" style="background: #8B5CF6"></span>
          {$t('rides.detail.power')}
        </button>
      {/if}
      {#if hasHr}
        <button class="chart-tab" class:active={activeChart === 'hr'} on:click={() => activeChart = 'hr'}>
          <span class="tab-icon" style="background: #EF4444"></span>
          {$t('rides.detail.hr')}
        </button>
      {/if}
    </div>
  </div>
  <div class="timeline-chart-area" bind:clientWidth={chartWidth}>
    {#if hoveredPoint}
      <div class="timeline-tooltip" style="left: {hoveredPoint.x}px; top: {hoveredPoint.y - 10}px;">
        <span class="tooltip-time">{$t('rides.detail.tooltipMin', { values: { minute: hoveredPoint.minute + 1 } })}</span>
        <span class="tooltip-val">{hoveredPoint.value.toFixed(activeChart === 'balance' ? 1 : 0)}{activeChart === 'power' ? 'W' : activeChart === 'hr' ? 'bpm' : '%'}</span>
      </div>
    {/if}
    <svg viewBox="0 0 {chartWidth} {CHART_HEIGHT}" class="timeline-svg" preserveAspectRatio="none">
      <defs>
        <linearGradient id="timelineGrad-{activeChart}" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stop-color={getChartColor(activeChart)} stop-opacity="0.2"/>
          <stop offset="100%" stop-color={getChartColor(activeChart)} stop-opacity="0"/>
        </linearGradient>
      </defs>
      <!-- Center reference line -->
      <line x1={CHART_PADDING} y1={CHART_HEIGHT / 2} x2={chartWidth - CHART_PADDING} y2={CHART_HEIGHT / 2} stroke="var(--border-subtle)" stroke-width="1" stroke-dasharray="3,3" opacity="0.4"/>
      <!-- Area fill -->
      <path d={chartData.areaPath} fill="url(#timelineGrad-{activeChart})"/>
      <!-- Line -->
      <path d={chartData.path} fill="none" stroke={getChartColor(activeChart)} stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <!-- Data points -->
      {#each chartData.points as point, i}
        <circle
          cx={point.x}
          cy={point.y}
          r={hoveredPoint?.index === i ? 5 : 3}
          fill={getChartColor(activeChart)}
          stroke="var(--bg-surface)"
          stroke-width="1.5"
          class="timeline-point"
          on:mouseenter={() => hoveredPoint = { index: i, x: point.x, y: point.y, value: point.value, minute: point.minute }}
          on:mouseleave={() => hoveredPoint = null}
        />
      {/each}
    </svg>
  </div>
  <div class="timeline-x-labels">
    <span>0:00</span>
    <span>{formatDuration(durationMs / 2)}</span>
    <span>{formatDuration(durationMs)}</span>
  </div>
  <div class="timeline-stats">
    <div class="timeline-stat">
      <span class="timeline-stat-label">{$t('rides.detail.min')}</span>
      <span class="timeline-stat-value">{chartData.min.toFixed(activeChart === 'balance' ? 1 : 0)}{activeChart === 'power' ? 'W' : activeChart === 'hr' ? '' : '%'}</span>
    </div>
    <div class="timeline-stat">
      <span class="timeline-stat-label">{$t('rides.detail.avg')}</span>
      <span class="timeline-stat-value">{chartData.avg.toFixed(activeChart === 'balance' ? 1 : 0)}{activeChart === 'power' ? 'W' : activeChart === 'hr' ? '' : '%'}</span>
    </div>
    <div class="timeline-stat">
      <span class="timeline-stat-label">{$t('rides.detail.max')}</span>
      <span class="timeline-stat-value">{chartData.max.toFixed(activeChart === 'balance' ? 1 : 0)}{activeChart === 'power' ? 'W' : activeChart === 'hr' ? '' : '%'}</span>
    </div>
    <div class="timeline-stat">
      <span class="timeline-stat-label">{$t('rides.detail.spread')}</span>
      <span class="timeline-stat-value {(chartData.max - chartData.min) > (activeChart === 'balance' ? 3 : activeChart === 'power' ? 100 : 10) ? 'problem' : ''}">{(chartData.max - chartData.min).toFixed(activeChart === 'balance' ? 1 : 0)}</span>
    </div>
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

  .timeline-card { padding: 14px; }
  .timeline-card .card-header { margin-bottom: 12px; }

  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
  .card-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }

  .chart-tabs {
    display: flex;
    gap: 2px;
    background: var(--bg-elevated);
    border-radius: 8px;
    padding: 3px;
  }
  .chart-tab {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 5px 10px;
    font-size: 11px;
    font-weight: 500;
    color: var(--text-secondary);
    background: transparent;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .chart-tab:hover { color: var(--text-primary); background: var(--bg-base); }
  .chart-tab.active { background: var(--bg-surface); color: var(--text-primary); box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
  .tab-icon {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .timeline-chart-area {
    position: relative;
    height: 80px;
    background: var(--bg-base);
    border-radius: 10px;
    overflow: visible;
  }
  .timeline-svg {
    width: 100%;
    height: 100%;
    display: block;
  }
  .timeline-point {
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .timeline-point:hover {
    filter: drop-shadow(0 0 4px currentColor);
  }
  .timeline-tooltip {
    position: absolute;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    padding: 6px 10px;
    pointer-events: none;
    z-index: 100;
    display: flex;
    flex-direction: column;
    gap: 2px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    transform: translate(-50%, -100%);
    white-space: nowrap;
  }
  .tooltip-time {
    font-size: 10px;
    font-weight: 500;
    color: var(--text-muted);
    text-transform: uppercase;
  }
  .tooltip-val {
    font-size: 15px;
    font-weight: 700;
    color: var(--text-primary);
  }
  .timeline-x-labels {
    display: flex;
    justify-content: space-between;
    font-size: 10px;
    color: var(--text-muted);
    margin-top: 6px;
    padding: 0 8px;
  }
  .timeline-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 6px;
    margin-top: 10px;
  }
  .timeline-stat {
    text-align: center;
    padding: 6px 4px;
    background: var(--bg-base);
    border-radius: 6px;
  }
  .timeline-stat-label {
    display: block;
    font-size: 9px;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.3px;
    margin-bottom: 2px;
  }
  .timeline-stat-value {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .timeline-stat-value.problem { color: var(--color-problem-text); }

  @media (max-width: 768px) {
    .grid-card.wide { grid-column: span 1; }
    .card-header { flex-wrap: wrap; gap: 8px; }
    .chart-tabs { flex-wrap: wrap; gap: 2px; }
    .chart-tab { padding: 4px 8px; font-size: 10px; }
    .tab-icon { width: 6px; height: 6px; }
    .timeline-chart-area { height: 70px; }
    .timeline-stats { grid-template-columns: repeat(4, 1fr); gap: 4px; }
    .timeline-stat { padding: 5px 3px; }
    .timeline-stat-value { font-size: 12px; }
  }

  @media (max-width: 480px) {
    .timeline-card { padding: 10px; }
    .timeline-chart-area { height: 60px; }
    .timeline-stats { grid-template-columns: repeat(2, 1fr); }
    .timeline-x-labels { font-size: 9px; }
  }
</style>
