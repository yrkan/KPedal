<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { FatigueData } from '$lib/types/ride-detail';
  import { getFatigueStatus } from '$lib/types/ride-detail';

  export let fatigueData: FatigueData;

  $: balStatus = getFatigueStatus('balance', fatigueData.balance.delta);
  $: teStatus = getFatigueStatus('te', fatigueData.te.delta);
  $: psStatus = getFatigueStatus('ps', fatigueData.ps.delta);
  $: degradedCount = [balStatus, teStatus, psStatus].filter(s => s === 'degraded').length;
</script>

<div class="grid-card fatigue-card">
  <div class="card-header">
    <span class="card-title">{$t('rides.detail.fatigue')} <InfoTip text={$t('rides.detail.fatigueTip')} position="bottom" size="sm" /></span>
    <span class="fatigue-badge {degradedCount === 0 ? 'good' : degradedCount <= 1 ? 'moderate' : 'poor'}">
      {degradedCount === 0 ? $t('rides.detail.fatigueStrong') : degradedCount <= 1 ? $t('rides.detail.fatigueModerate') : $t('rides.detail.fatigueFatigued')}
    </span>
  </div>

  <div class="fatigue-list">
    <div class="fatigue-row">
      <span class="fatigue-metric-name">{$t('rides.detail.fatigueAsymmetry')}</span>
      <span class="fatigue-vals">
        <span>{fatigueData.balance.first.toFixed(1)}%</span>
        <span class="fatigue-arrow">→</span>
        <span class="{balStatus}">{fatigueData.balance.last.toFixed(1)}%</span>
      </span>
      <span class="fatigue-change {balStatus}">
        {fatigueData.balance.delta > 0 ? '+' : ''}{fatigueData.balance.delta.toFixed(1)}%
      </span>
    </div>
    <div class="fatigue-row">
      <span class="fatigue-metric-name">TE</span>
      <span class="fatigue-vals">
        <span>{fatigueData.te.first.toFixed(0)}%</span>
        <span class="fatigue-arrow">→</span>
        <span class="{teStatus}">{fatigueData.te.last.toFixed(0)}%</span>
      </span>
      <span class="fatigue-change {teStatus}">
        {fatigueData.te.delta > 0 ? '+' : ''}{fatigueData.te.delta.toFixed(0)}%
      </span>
    </div>
    <div class="fatigue-row">
      <span class="fatigue-metric-name">PS</span>
      <span class="fatigue-vals">
        <span>{fatigueData.ps.first.toFixed(0)}%</span>
        <span class="fatigue-arrow">→</span>
        <span class="{psStatus}">{fatigueData.ps.last.toFixed(0)}%</span>
      </span>
      <span class="fatigue-change {psStatus}">
        {fatigueData.ps.delta > 0 ? '+' : ''}{fatigueData.ps.delta.toFixed(0)}%
      </span>
    </div>
  </div>

  <div class="fatigue-insights">
    {#if balStatus === 'degraded'}
      <div class="fatigue-insight">
        <span class="insight-icon problem">!</span>
        <span>{$t('rides.detail.balanceDegradedInsight', { values: { value: Math.abs(fatigueData.balance.delta).toFixed(1) } })}</span>
      </div>
    {/if}
    {#if teStatus === 'degraded'}
      <div class="fatigue-insight">
        <span class="insight-icon problem">!</span>
        <span>{$t('rides.detail.teDroppedInsight', { values: { value: Math.abs(fatigueData.te.delta).toFixed(0) } })}</span>
      </div>
    {/if}
    {#if psStatus === 'degraded'}
      <div class="fatigue-insight">
        <span class="insight-icon problem">!</span>
        <span>{$t('rides.detail.psFellInsight', { values: { value: Math.abs(fatigueData.ps.delta).toFixed(0) } })}</span>
      </div>
    {/if}
    {#if degradedCount === 0}
      <div class="fatigue-insight">
        <span class="insight-icon good">✓</span>
        <span>{$t('rides.detail.excellentFatigueInsight')}</span>
      </div>
    {/if}
  </div>

  <div class="fatigue-legend">
    <span>{$t('rides.detail.fatigueStartEnd')}</span>
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

  .fatigue-card {
    display: flex;
    flex-direction: column;
  }

  .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
  .card-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }

  .fatigue-badge {
    font-size: 9px;
    font-weight: 600;
    padding: 3px 6px;
    border-radius: 4px;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .fatigue-badge.good { background: var(--color-optimal-soft); color: var(--color-optimal-text); }
  .fatigue-badge.moderate { background: var(--color-attention-soft); color: var(--color-attention-text); }
  .fatigue-badge.poor { background: var(--color-problem-soft); color: var(--color-problem-text); }

  .fatigue-list {
    display: flex;
    flex-direction: column;
  }

  .fatigue-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px solid var(--border-subtle);
  }
  .fatigue-row:last-child { border-bottom: none; }

  .fatigue-metric-name {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-primary);
    min-width: 75px;
  }

  .fatigue-vals {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-secondary);
    font-variant-numeric: tabular-nums;
  }
  .fatigue-vals .improved { color: var(--color-optimal-text); }
  .fatigue-vals .stable { color: var(--text-muted); }
  .fatigue-vals .degraded { color: var(--color-problem-text); }

  .fatigue-arrow {
    color: var(--text-muted);
    font-size: 11px;
  }

  .fatigue-change {
    font-size: 11px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    min-width: 45px;
    text-align: right;
  }
  .fatigue-change.improved { color: var(--color-optimal-text); }
  .fatigue-change.stable { color: var(--text-muted); }
  .fatigue-change.degraded { color: var(--color-problem-text); }

  .fatigue-insights {
    margin-top: auto;
    padding-top: 10px;
    border-top: 1px solid var(--border-subtle);
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .fatigue-insight {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    font-size: 11px;
    line-height: 1.4;
    color: var(--text-secondary);
  }

  .insight-icon {
    flex-shrink: 0;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 700;
  }
  .insight-icon.problem { background: var(--color-problem-soft); color: var(--color-problem-text); }
  .insight-icon.good { background: var(--color-optimal-soft); color: var(--color-optimal-text); }

  .fatigue-legend {
    display: flex;
    justify-content: center;
    margin-top: 10px;
    padding-top: 8px;
    border-top: 1px solid var(--border-subtle);
  }
  .fatigue-legend span {
    font-size: 9px;
    color: var(--text-muted);
  }

  @media (max-width: 768px) {
    .fatigue-metric-name { font-size: 10px; min-width: 65px; }
    .fatigue-vals { font-size: 11px; gap: 5px; }
  }

  @media (max-width: 480px) {
    .fatigue-row { padding: 6px 0; }
    .fatigue-metric-name { font-size: 9px; min-width: 55px; }
    .fatigue-vals { font-size: 10px; gap: 4px; }
    .fatigue-arrow { font-size: 9px; }
    .fatigue-legend { margin-top: 6px; padding-top: 6px; }
    .fatigue-legend span { font-size: 8px; }
  }
</style>
