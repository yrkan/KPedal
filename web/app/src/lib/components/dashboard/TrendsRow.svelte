<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { Ride, TrendData, BalancePoint, BalanceTrendResult, TechniquePoint, TechniqueTrendResult } from '$lib/types/dashboard';
  import { CHART_HEIGHT, CHART_PADDING, getBalanceStatus, getTrendColor, getAsymmetryClass, getBalanceTrendData, getTechniqueTrendData } from '$lib/utils/dashboard';
  import { locale } from '$lib/i18n';
  import { goto } from '$app/navigation';

  export let filteredRides: Ride[];
  export let trendData: TrendData[];
  export let selectedPeriod: '7' | '14' | '30' | '60';

  // Internal state
  let activeTrendMetric: 'asymmetry' | 'te' | 'ps' = 'asymmetry';
  let hoveredBalancePoint: BalancePoint | null = null;
  let hoveredTechniquePoint: TechniquePoint | null = null;
  let tooltipX = 0;
  let tooltipY = 0;
  let balanceChartWidth = 300;
  let techniqueChartWidth = 300;

  // Computed
  $: balanceTrendMaxPoints = selectedPeriod === '60' ? 60 : selectedPeriod === '30' ? 30 : selectedPeriod === '14' ? 14 : 7;
  $: balanceTrend = getBalanceTrendData(filteredRides, balanceTrendMaxPoints, balanceChartWidth, $locale);
  $: techniqueTrend = getTechniqueTrendData(trendData, activeTrendMetric, techniqueChartWidth, $locale);

  function handleBalancePointHover(point: BalancePoint | null, event?: MouseEvent) {
    hoveredBalancePoint = point;
    if (point && event) {
      const rect = (event.currentTarget as SVGElement).closest('.trend-chart')?.getBoundingClientRect();
      if (rect) {
        tooltipX = event.clientX - rect.left;
        tooltipY = event.clientY - rect.top - 40;
      }
    }
  }

  function handleTechniquePointHover(point: TechniquePoint | null, event?: MouseEvent) {
    hoveredTechniquePoint = point;
    if (point && event) {
      const rect = (event.currentTarget as SVGElement).closest('.trend-chart')?.getBoundingClientRect();
      if (rect) {
        tooltipX = event.clientX - rect.left;
        tooltipY = event.clientY - rect.top - 40;
      }
    }
  }

  function navigateToRide(rideId: number | string) {
    goto(`/rides/${rideId}`);
  }
</script>

{#if balanceTrend.path || trendData.length >= 2}
  <div class="trends-row animate-in">
    <!-- Balance Trend -->
    <div class="trend-card">
      <div class="trend-card-header">
        <span class="trend-card-title">{$t('dashboard.trendCards.balanceTrend')} <InfoTip text={$t('infotips.balanceTrend')} position="bottom" /></span>
        <span class="trend-card-period">{filteredRides.length} {$t('dashboard.units.rides')}</span>
      </div>
      {#if balanceTrend.path}
        <div class="trend-chart" role="img" aria-label={$t('aria.balanceTrendChart')} bind:clientWidth={balanceChartWidth}>
          {#if hoveredBalancePoint}
            <div class="chart-tooltip" style="left: {tooltipX}px; top: {tooltipY}px;">
              <span class="tooltip-date">{hoveredBalancePoint.date}</span>
              <span class="tooltip-value {hoveredBalancePoint.status}">{hoveredBalancePoint.balance.toFixed(1)}% L</span>
              <span class="tooltip-hint">{$t('dashboard.table.clickToView')}</span>
            </div>
          {/if}
          <svg viewBox="0 0 {balanceChartWidth} {CHART_HEIGHT}" class="trend-svg">
            <defs>
              <linearGradient id="balanceGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="var(--color-optimal)" stop-opacity="0.15"/>
                <stop offset="100%" stop-color="var(--color-optimal)" stop-opacity="0"/>
              </linearGradient>
            </defs>
            <line x1={CHART_PADDING} y1={CHART_HEIGHT / 2} x2={balanceChartWidth - CHART_PADDING} y2={CHART_HEIGHT / 2} stroke="var(--border-subtle)" stroke-width="1" stroke-dasharray="3,3" opacity="0.5"/>
            <path d={balanceTrend.areaPath} fill="url(#balanceGradient)"/>
            <path d={balanceTrend.path} fill="none" stroke="var(--color-optimal)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            {#each balanceTrend.points as point}
              <circle
                cx={point.x}
                cy={point.y}
                r={hoveredBalancePoint === point ? 5 : 4}
                fill={point.status === 'optimal' ? 'var(--color-optimal)' : point.status === 'attention' ? 'var(--color-attention)' : 'var(--color-problem)'}
                stroke="var(--bg-surface)"
                stroke-width="2"
                class="chart-point"
                role="button"
                tabindex="0"
                on:mouseenter={(e) => handleBalancePointHover(point, e)}
                on:mouseleave={() => handleBalancePointHover(null)}
                on:click={() => navigateToRide(point.rideId)}
                on:keypress={(e) => e.key === 'Enter' && navigateToRide(point.rideId)}
              />
            {/each}
          </svg>
        </div>
        <div class="trend-stats">
          <div class="trend-stat-item">
            <span class="trend-stat-label">{$t('dashboard.trendStats.current')} <InfoTip text={$t('infotips.trendCurrent')} position="bottom" size="sm" /></span>
            <span class="trend-stat-value {getBalanceStatus(balanceTrend.points[balanceTrend.points.length - 1]?.balance || 50)}">{(balanceTrend.points[balanceTrend.points.length - 1]?.balance || 50).toFixed(1)}%</span>
          </div>
          <div class="trend-stat-item">
            <span class="trend-stat-label">{$t('dashboard.trendStats.average')} <InfoTip text={$t('infotips.trendAverage')} position="bottom" size="sm" /></span>
            <span class="trend-stat-value">{((balanceTrend.minBalance + balanceTrend.maxBalance) / 2).toFixed(1)}%</span>
          </div>
          <div class="trend-stat-item">
            <span class="trend-stat-label">{$t('dashboard.trendStats.spread')} <InfoTip text={$t('infotips.trendSpread')} position="bottom" size="sm" /></span>
            <span class="trend-stat-value {(balanceTrend.maxBalance - balanceTrend.minBalance) > 5 ? 'problem' : (balanceTrend.maxBalance - balanceTrend.minBalance) > 2.5 ? 'attention' : 'optimal'}">{(balanceTrend.maxBalance - balanceTrend.minBalance).toFixed(1)}%</span>
          </div>
        </div>
      {:else}
        <div class="trend-empty">{$t('dashboard.emptyStates.needMoreRides')}</div>
      {/if}
    </div>

    <!-- Technique Trend -->
    <div class="trend-card">
      <div class="trend-card-header">
        <span class="trend-card-title">{$t('dashboard.trendCards.technique')} <InfoTip text={$t('infotips.techniqueTrend')} position="bottom" /></span>
        <div class="trend-metric-switcher">
          <button class:active={activeTrendMetric === 'asymmetry'} on:click={() => activeTrendMetric = 'asymmetry'}>{$t('dashboard.trendCards.asym')}</button>
          <button class:active={activeTrendMetric === 'te'} on:click={() => activeTrendMetric = 'te'}>{$t('metrics.teShort')}</button>
          <button class:active={activeTrendMetric === 'ps'} on:click={() => activeTrendMetric = 'ps'}>{$t('metrics.psShort')}</button>
        </div>
      </div>
      {#if techniqueTrend.path}
        <div class="trend-chart" role="img" aria-label={$t('aria.techniqueTrendChart')} bind:clientWidth={techniqueChartWidth}>
          {#if hoveredTechniquePoint}
            <div class="chart-tooltip" style="left: {tooltipX}px; top: {tooltipY}px;">
              <span class="tooltip-date">{hoveredTechniquePoint.date}</span>
              <span class="tooltip-value {hoveredTechniquePoint.status}">{hoveredTechniquePoint.value.toFixed(1)}%</span>
            </div>
          {/if}
          <svg viewBox="0 0 {techniqueChartWidth} {CHART_HEIGHT}" class="trend-svg">
            <defs>
              <linearGradient id="techniqueGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color={activeTrendMetric === 'asymmetry' ? 'var(--color-accent)' : activeTrendMetric === 'te' ? 'var(--color-optimal)' : 'var(--color-attention)'} stop-opacity="0.15"/>
                <stop offset="100%" stop-color={activeTrendMetric === 'asymmetry' ? 'var(--color-accent)' : activeTrendMetric === 'te' ? 'var(--color-optimal)' : 'var(--color-attention)'} stop-opacity="0"/>
              </linearGradient>
            </defs>
            <line x1={CHART_PADDING} y1={CHART_HEIGHT / 2} x2={techniqueChartWidth - CHART_PADDING} y2={CHART_HEIGHT / 2} stroke="var(--border-subtle)" stroke-width="1" stroke-dasharray="3,3" opacity="0.5"/>
            <path d={techniqueTrend.areaPath} fill="url(#techniqueGradient)"/>
            <path d={techniqueTrend.path} fill="none" stroke={getTrendColor(activeTrendMetric)} stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            {#each techniqueTrend.points as point}
              <circle
                cx={point.x}
                cy={point.y}
                r={hoveredTechniquePoint === point ? 5 : 4}
                fill={point.status === 'optimal' ? 'var(--color-optimal)' : point.status === 'attention' ? 'var(--color-attention)' : 'var(--color-problem)'}
                stroke="var(--bg-surface)"
                stroke-width="2"
                class="chart-point"
                on:mouseenter={(e) => handleTechniquePointHover(point, e)}
                on:mouseleave={() => handleTechniquePointHover(null)}
              />
            {/each}
          </svg>
        </div>
        <div class="trend-stats">
          <div class="trend-stat-item">
            <span class="trend-stat-label">{$t('dashboard.trendStats.latest')} <InfoTip text={$t('infotips.trendLatest')} position="bottom" size="sm" /></span>
            <span class="trend-stat-value {activeTrendMetric === 'asymmetry' ? getAsymmetryClass(techniqueTrend.points[techniqueTrend.points.length - 1]?.value || 0) : ''}">{(techniqueTrend.points[techniqueTrend.points.length - 1]?.value || 0).toFixed(1)}%</span>
          </div>
          <div class="trend-stat-item">
            <span class="trend-stat-label">{$t('dashboard.trendStats.average')} <InfoTip text={$t('infotips.trendAverage')} position="bottom" size="sm" /></span>
            <span class="trend-stat-value">{(techniqueTrend.points.reduce((s, p) => s + p.value, 0) / techniqueTrend.points.length).toFixed(1)}%</span>
          </div>
          <div class="trend-stat-item">
            <span class="trend-stat-label">{$t('dashboard.trendStats.best')} <InfoTip text={$t('infotips.trendBest')} position="bottom" size="sm" /></span>
            <span class="trend-stat-value optimal">{(activeTrendMetric === 'asymmetry' ? Math.min(...techniqueTrend.points.map(p => p.value)) : Math.max(...techniqueTrend.points.map(p => p.value))).toFixed(1)}%</span>
          </div>
        </div>
      {:else}
        <div class="trend-empty">{$t('dashboard.emptyStates.needMoreData')}</div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .trends-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 16px;
  }
  .trend-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
    min-width: 0;
    overflow: hidden;
  }
  .trend-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    min-height: 28px;
  }
  .trend-card-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .trend-card-period { font-size: 11px; color: var(--text-muted); }
  .trend-chart {
    height: 70px;
    margin-bottom: 12px;
    background: var(--bg-base);
    border-radius: 8px;
    position: relative;
    overflow: visible;
  }
  .trend-svg { width: 100%; height: 100%; display: block; }
  .chart-point { cursor: pointer; transition: all 0.15s ease; }
  .chart-point:hover { filter: drop-shadow(0 0 4px currentColor); }
  .chart-tooltip {
    position: absolute;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    padding: 10px 14px;
    pointer-events: none;
    z-index: 100;
    display: flex;
    flex-direction: column;
    gap: 4px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
    transform: translateX(-50%);
    white-space: nowrap;
    backdrop-filter: blur(8px);
  }
  .tooltip-date { font-size: 11px; font-weight: 500; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }
  .tooltip-value { font-size: 18px; font-weight: 700; color: var(--text-primary); }
  .tooltip-value.optimal { color: var(--color-optimal-text); }
  .tooltip-value.attention { color: var(--color-attention-text); }
  .tooltip-value.problem { color: var(--color-problem-text); }
  .tooltip-hint { font-size: 10px; color: var(--text-muted); opacity: 0.8; margin-top: 2px; }
  .trend-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .trend-stat-item { text-align: center; padding: 8px 4px; background: var(--bg-base); border-radius: 6px; }
  .trend-stat-label { display: block; font-size: 10px; color: var(--text-muted); margin-bottom: 2px; }
  .trend-stat-value { font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .trend-stat-value.optimal { color: var(--color-optimal-text); }
  .trend-stat-value.attention { color: var(--color-attention-text); }
  .trend-stat-value.problem { color: var(--color-problem-text); }
  .trend-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100px;
    font-size: 12px;
    color: var(--text-muted);
  }
  .trend-metric-switcher {
    display: flex;
    gap: 2px;
    background: var(--bg-base);
    border-radius: 6px;
    padding: 2px;
  }
  .trend-metric-switcher button {
    padding: 4px 8px;
    font-size: 10px;
    font-weight: 500;
    color: var(--text-muted);
    background: transparent;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.15s;
    border: none;
  }
  .trend-metric-switcher button:hover { color: var(--text-secondary); }
  .trend-metric-switcher button.active {
    background: var(--color-accent);
    color: var(--color-accent-text, #000);
    font-weight: 600;
  }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .trends-row { grid-template-columns: 1fr; gap: 12px; }
    .trend-card { padding: 12px; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .trend-card { padding: 10px; }
    .trend-card-header { flex-wrap: nowrap; min-height: 28px; }
    .trend-card-title { font-size: 13px; white-space: nowrap; }
    .trend-card-title :global(.info-tip) { display: none; }
    .trend-chart { margin-bottom: 8px; }
    .chart-tooltip { padding: 6px 10px; border-radius: 8px; }
    .tooltip-value { font-size: 16px; }
    .trend-stats { gap: 6px; }
    .trend-stat-item { padding: 6px 4px; }
    .trend-stat-value { font-size: 13px; }
    .trend-metric-switcher button { padding: 3px 6px; font-size: 9px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .trends-row { gap: 10px; margin-bottom: 12px; }
    .trend-card { padding: 10px; border-radius: 10px; }
    .trend-card-header { margin-bottom: 8px; }
    .trend-card-title { font-size: 12px; }
    .trend-card-period { font-size: 10px; }
    .trend-chart { height: 60px; margin-bottom: 8px; }
    .chart-tooltip { padding: 5px 8px; }
    .tooltip-date { font-size: 9px; }
    .tooltip-value { font-size: 14px; }
    .tooltip-hint { font-size: 9px; }
    .trend-stats { gap: 4px; }
    .trend-stat-item { padding: 5px 3px; }
    .trend-stat-label { font-size: 9px; }
    .trend-stat-value { font-size: 12px; }
    .trend-metric-switcher { padding: 1px; }
    .trend-metric-switcher button { padding: 2px 5px; font-size: 8px; }
  }
</style>
