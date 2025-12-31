<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { Stats, PeriodStats, WeeklyComparison, FatigueAnalysis } from '$lib/types/dashboard';

  export let stats: Stats;
  export let periodStats: PeriodStats | null;
  export let selectedPeriod: '7' | '14' | '30' | '60';
  export let weeklyComparison: WeeklyComparison | null;
  export let fatigueData: FatigueAnalysis | null;
</script>

<div class="grid-card">
  <div class="card-header">
    <span class="card-title">{$t('dashboard.technique')} <InfoTip text={$t('infotips.technique')} position="bottom" /></span>
    <span class="card-subtitle">{selectedPeriod}d {$t('metrics.avg').toLowerCase()}</span>
  </div>

  <div class="technique-enhanced">
    <div class="technique-metric">
      <div class="technique-metric-header">
        <span class="technique-metric-label">
          {$t('metrics.te')}
          <InfoTip text={$t('infotips.te')} position="top" />
        </span>
        <span class="technique-metric-value">{periodStats?.te?.toFixed(0) || ((stats.avg_te_left + stats.avg_te_right) / 2).toFixed(0)}%</span>
      </div>
      <div class="technique-metric-bar-wrap">
        <div class="technique-metric-bar">
          <div class="technique-optimal-zone te"></div>
          <div class="technique-metric-fill te" style="width: {Math.min(100, (periodStats?.te || ((stats.avg_te_left + stats.avg_te_right) / 2)))}%"></div>
          <div class="technique-bar-marker" style="left: 70%"></div>
          <div class="technique-bar-marker" style="left: 80%"></div>
        </div>
        <div class="technique-bar-labels te">
          <span>0</span>
          <span class="optimal-label">70-80</span>
          <span>100</span>
        </div>
      </div>
      <div class="technique-metric-sides">
        <span>L {periodStats?.te ? ((periodStats.te * 2 - stats.avg_te_right) || stats.avg_te_left).toFixed(0) : stats.avg_te_left.toFixed(0)}%</span>
        <span>R {stats.avg_te_right.toFixed(0)}%</span>
      </div>
    </div>

    <div class="technique-metric">
      <div class="technique-metric-header">
        <span class="technique-metric-label">
          {$t('metrics.ps')}
          <InfoTip text={$t('infotips.ps')} position="top" />
        </span>
        <span class="technique-metric-value">{periodStats?.ps?.toFixed(0) || ((stats.avg_ps_left + stats.avg_ps_right) / 2).toFixed(0)}%</span>
      </div>
      <div class="technique-metric-bar-wrap">
        <div class="technique-metric-bar">
          <div class="technique-optimal-zone ps"></div>
          <div class="technique-metric-fill ps" style="width: {Math.min(100, (periodStats?.ps || ((stats.avg_ps_left + stats.avg_ps_right) / 2)) / 40 * 100)}%"></div>
          <div class="technique-bar-marker" style="left: 50%"></div>
        </div>
        <div class="technique-bar-labels ps">
          <span>0</span>
          <span class="optimal-label">≥20</span>
          <span>40</span>
        </div>
      </div>
      <div class="technique-metric-sides">
        <span>L {stats.avg_ps_left.toFixed(0)}%</span>
        <span>R {stats.avg_ps_right.toFixed(0)}%</span>
      </div>
    </div>
  </div>

  {#if weeklyComparison}
    <div class="card-section">
      <span class="section-label">{$t('dashboard.vsLastWeek')}</span>
      <div class="comparison-grid">
        <div class="comp-item">
          <span class="comp-label">{$t('metrics.teShort')} <InfoTip text={$t('infotips.compTe')} position="bottom" size="sm" /></span>
          <span class="comp-value">{weeklyComparison.thisWeek.avg_te.toFixed(0)}%</span>
          <span class="comp-delta {weeklyComparison.changes.te >= 0 ? 'up' : 'down'}">
            {weeklyComparison.changes.te >= 0 ? '+' : ''}{weeklyComparison.changes.te.toFixed(1)}
          </span>
        </div>
        <div class="comp-item">
          <span class="comp-label">{$t('metrics.psShort')} <InfoTip text={$t('infotips.compPs')} position="bottom" size="sm" /></span>
          <span class="comp-value">{weeklyComparison.thisWeek.avg_ps.toFixed(0)}%</span>
          <span class="comp-delta {weeklyComparison.changes.ps >= 0 ? 'up' : 'down'}">
            {weeklyComparison.changes.ps >= 0 ? '+' : ''}{weeklyComparison.changes.ps.toFixed(1)}
          </span>
        </div>
        <div class="comp-item">
          <span class="comp-label">{$t('dashboard.comparison.balance')} <InfoTip text={$t('infotips.compBalance')} position="bottom" size="sm" /></span>
          <span class="comp-value">{Math.abs(weeklyComparison.thisWeek.avg_balance_left - 50).toFixed(1)}%</span>
          <span class="comp-delta {weeklyComparison.changes.balance <= 0 ? 'up' : 'down'}">
            {weeklyComparison.changes.balance <= 0 ? '' : '+'}{weeklyComparison.changes.balance.toFixed(1)}
          </span>
        </div>
        <div class="comp-item">
          <span class="comp-label">{$t('dashboard.comparison.optimal')} <InfoTip text={$t('infotips.compOptimal')} position="bottom" size="sm" /></span>
          <span class="comp-value">{weeklyComparison.thisWeek.avg_zone_optimal.toFixed(0)}%</span>
          <span class="comp-delta {weeklyComparison.changes.zone_optimal >= 0 ? 'up' : 'down'}">
            {weeklyComparison.changes.zone_optimal >= 0 ? '+' : ''}{weeklyComparison.changes.zone_optimal.toFixed(0)}
          </span>
        </div>
      </div>
    </div>

    <!-- Fatigue Analysis -->
    {#if fatigueData && fatigueData.hasData}
      <div class="card-section">
        <span class="section-label">
          {$t('dashboard.fatigue.title')}
          <InfoTip text={$t('infotips.fatigue')} position="top" />
        </span>
        <div class="performance-mini-grid">
          <div class="perf-mini-item">
            <span class="perf-mini-label">{$t('metrics.balance')} <InfoTip text={$t('infotips.balanceFatigue')} position="top" size="sm" /></span>
            <span class="perf-mini-value">{fatigueData.firstThird.balance.toFixed(0)}→{fatigueData.lastThird.balance.toFixed(0)}</span>
            <span class="perf-mini-delta {fatigueData.degradation.balance > 0.5 ? 'down' : fatigueData.degradation.balance < -0.5 ? 'up' : ''}">
              {fatigueData.degradation.balance > 0.5 ? '↓' : fatigueData.degradation.balance < -0.5 ? '↑' : '—'}
            </span>
          </div>
          <div class="perf-mini-item">
            <span class="perf-mini-label">{$t('metrics.teShort')} <InfoTip text={$t('infotips.teFatigue')} position="top" size="sm" /></span>
            <span class="perf-mini-value">{fatigueData.firstThird.te.toFixed(0)}→{fatigueData.lastThird.te.toFixed(0)}</span>
            <span class="perf-mini-delta {fatigueData.degradation.te > 2 ? 'down' : fatigueData.degradation.te < -2 ? 'up' : ''}">
              {fatigueData.degradation.te > 2 ? '↓' : fatigueData.degradation.te < -2 ? '↑' : '—'}
            </span>
          </div>
          <div class="perf-mini-item">
            <span class="perf-mini-label">{$t('metrics.psShort')} <InfoTip text={$t('infotips.psFatigue')} position="top" size="sm" /></span>
            <span class="perf-mini-value">{fatigueData.firstThird.ps.toFixed(0)}→{fatigueData.lastThird.ps.toFixed(0)}</span>
            <span class="perf-mini-delta {fatigueData.degradation.ps > 1 ? 'down' : fatigueData.degradation.ps < -1 ? 'up' : ''}">
              {fatigueData.degradation.ps > 1 ? '↓' : fatigueData.degradation.ps < -1 ? '↑' : '—'}
            </span>
          </div>
          <div class="perf-mini-item">
            <span class="perf-mini-label">{$t('dashboard.fatigue.trend')} <InfoTip text={$t('infotips.fatigueTrend')} position="top" size="sm" /></span>
            <span class="perf-mini-value fatigue-summary {fatigueData.degradation.te > 2 || fatigueData.degradation.ps > 1 ? 'problem' : fatigueData.degradation.te > 0 || fatigueData.degradation.ps > 0 ? 'attention' : 'optimal'}">
              {fatigueData.degradation.te > 2 || fatigueData.degradation.ps > 1 ? $t('dashboard.fatigue.dropped') : fatigueData.degradation.te > 0 || fatigueData.degradation.ps > 0 ? $t('dashboard.fatigue.slight') : $t('dashboard.fatigue.stable')}
            </span>
          </div>
        </div>
      </div>
    {/if}
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

  /* Technique Enhanced */
  .technique-enhanced {
    background: var(--bg-base);
    border-radius: 8px;
    padding: 12px;
    margin-bottom: 12px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    flex: 1;
    justify-content: center;
  }
  .technique-metric { display: flex; flex-direction: column; gap: 6px; }
  .technique-metric-header { display: flex; justify-content: space-between; align-items: baseline; }
  .technique-metric-label { font-size: 11px; color: var(--text-muted); font-weight: 500; }
  .technique-metric-value { font-size: 18px; font-weight: 700; color: var(--text-primary); }
  .technique-metric-bar-wrap { position: relative; }
  .technique-metric-bar {
    height: 10px;
    background: var(--bg-elevated);
    border-radius: 5px;
    overflow: visible;
    position: relative;
  }
  .technique-metric-fill {
    position: absolute;
    top: 0;
    left: 0;
    height: 100%;
    border-radius: 5px;
    transition: width 0.3s ease;
  }
  .technique-metric-fill.te {
    background: linear-gradient(90deg, var(--color-optimal), var(--color-optimal-light, var(--color-optimal)));
  }
  .technique-metric-fill.ps {
    background: linear-gradient(90deg, var(--color-attention), var(--color-attention-light, var(--color-attention)));
  }
  .technique-optimal-zone {
    position: absolute;
    top: 0;
    height: 100%;
    background: var(--color-optimal);
    opacity: 0.15;
    border-radius: 5px;
    pointer-events: none;
  }
  .technique-optimal-zone.te { left: 70%; right: 20%; }
  .technique-optimal-zone.ps { left: 50%; right: 0; }
  .technique-bar-marker {
    position: absolute;
    top: -3px;
    bottom: -3px;
    width: 2px;
    background: var(--color-optimal);
    opacity: 0.7;
    border-radius: 1px;
  }
  .technique-bar-labels {
    display: flex;
    justify-content: space-between;
    font-size: 9px;
    color: var(--text-muted);
    margin-top: 3px;
    position: relative;
  }
  .technique-bar-labels .optimal-label {
    position: absolute;
    transform: translateX(-50%);
    color: var(--color-optimal-text);
    font-weight: 600;
  }
  .technique-bar-labels.te .optimal-label { left: 75%; }
  .technique-bar-labels.ps .optimal-label { left: 75%; }
  .technique-metric-sides {
    display: flex;
    justify-content: space-between;
    font-size: 10px;
    color: var(--text-muted);
  }

  /* Comparison Grid */
  .comparison-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
  .comp-item { text-align: center; padding: 8px 4px; background: var(--bg-base); border-radius: 6px; }
  .comp-label { display: block; font-size: 9px; color: var(--text-muted); margin-bottom: 2px; }
  .comp-value { display: block; font-size: 14px; font-weight: 600; color: var(--text-primary); }
  .comp-delta { display: block; font-size: 10px; font-weight: 500; margin-top: 2px; }
  .comp-delta.up { color: var(--color-optimal-text); }
  .comp-delta.down { color: var(--color-problem-text); }

  /* Performance Mini Grid (for Fatigue) */
  .performance-mini-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
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

  /* Fatigue Summary */
  .fatigue-summary { font-size: 12px !important; }
  .fatigue-summary.optimal { color: var(--color-optimal-text); }
  .fatigue-summary.attention { color: var(--color-attention-text); }
  .fatigue-summary.problem { color: var(--color-problem-text); }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .grid-card { padding: 12px; }
    .comparison-grid { grid-template-columns: repeat(2, 1fr); }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .card-title { font-size: 13px; }
    .technique-enhanced { padding: 10px; gap: 12px; }
    .technique-metric-value { font-size: 16px; }
    .technique-metric-bar { height: 8px; }
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
    .technique-enhanced { padding: 8px; gap: 10px; margin-bottom: 8px; }
    .technique-metric { gap: 4px; }
    .technique-metric-label { font-size: 10px; }
    .technique-metric-value { font-size: 15px; }
    .technique-metric-bar { height: 8px; }
    .technique-bar-labels { font-size: 8px; }
    .technique-metric-sides { font-size: 9px; }
    .comparison-grid { gap: 4px; }
    .comp-item { padding: 6px 3px; }
    .comp-label { font-size: 8px; }
    .comp-value { font-size: 12px; }
    .comp-delta { font-size: 9px; }
    .performance-mini-grid { gap: 6px; }
  }
</style>
