<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { PeriodStats } from '$lib/types/dashboard';
  import { getBalanceStatus } from '$lib/utils/dashboard';

  export let periodStats: PeriodStats | null;
</script>

<div class="hero-stats animate-in">
  <div class="hero-stat asymmetry">
    <div class="hero-stat-main">
      <span class="hero-stat-value {getBalanceStatus(periodStats?.balance || 50)}">{periodStats ? Math.abs(periodStats.balance - 50).toFixed(1) : '—'}%</span>
    </div>
    <div class="hero-stat-info">
      <span class="hero-stat-label">
        {$t('metrics.asymmetry')}
        <InfoTip text={$t('infotips.asymmetry')} position="bottom" />
      </span>
      <span class="hero-stat-detail">{periodStats && periodStats.balance !== 50 ? (periodStats.balance > 50 ? $t('dashboard.leftDominant') : $t('dashboard.rightDominant')) : $t('dashboard.balanced')}</span>
    </div>
  </div>

  <div class="hero-stat balance-visual">
    <div class="balance-display">
      <div class="balance-side left">
        <span class="balance-pct">{periodStats ? periodStats.balance.toFixed(0) : 50}</span>
        <span class="balance-leg">L</span>
      </div>
      <div class="balance-bar-wrap">
        <div class="balance-bar">
          <div class="balance-fill left" style="width: {periodStats ? Math.min(50, periodStats.balance) : 50}%"></div>
          <div class="balance-fill right" style="width: {periodStats ? Math.min(50, 100 - periodStats.balance) : 50}%"></div>
        </div>
        <div class="balance-center-mark"></div>
      </div>
      <div class="balance-side right">
        <span class="balance-pct">{periodStats ? (100 - periodStats.balance).toFixed(0) : 50}</span>
        <span class="balance-leg">R</span>
      </div>
    </div>
  </div>

  <div class="hero-stat zones">
    <div class="zone-mini-bars">
      <div class="zone-mini optimal" style="width: {periodStats?.zoneOptimal || 0}%"></div>
      <div class="zone-mini attention" style="width: {periodStats?.zoneAttention || 0}%"></div>
      <div class="zone-mini problem" style="width: {periodStats?.zoneProblem || 0}%"></div>
    </div>
    <div class="zone-mini-values">
      <span class="zone-mini-val optimal">{periodStats?.zoneOptimal?.toFixed(0) || 0}%</span>
      <span class="zone-mini-val attention">{periodStats?.zoneAttention?.toFixed(0) || 0}%</span>
      <span class="zone-mini-val problem">{periodStats?.zoneProblem?.toFixed(0) || 0}%</span>
    </div>
    <span class="hero-stat-label">
      {$t('zones.timeInZone')}
      <InfoTip text={$t('infotips.timeInZone')} position="bottom" />
    </span>
  </div>

  <div class="hero-stat summary">
    <div class="summary-grid">
      <div class="summary-metric">
        <span class="summary-num">{periodStats?.rides || 0}</span>
        <span class="summary-unit">{$t('dashboard.summary.rides')}</span>
      </div>
      <div class="summary-metric">
        <span class="summary-num">{periodStats ? (periodStats.duration / 3600000).toFixed(1) : 0}</span>
        <span class="summary-unit">{$t('dashboard.summary.hours')}</span>
      </div>
      <div class="summary-metric">
        <span class="summary-num">{periodStats?.totalDistance?.toFixed(0) || 0}</span>
        <span class="summary-unit">{$t('dashboard.summary.km')}</span>
      </div>
    </div>
  </div>
</div>

<style>
  .hero-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 16px;
  }
  .hero-stat {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 14px;
  }
  .hero-stat.asymmetry { display: flex; align-items: center; gap: 12px; }
  .hero-stat-main { flex-shrink: 0; }
  .hero-stat-value { font-size: 26px; font-weight: 700; color: var(--text-primary); }
  .hero-stat-value.optimal { color: var(--color-optimal); }
  .hero-stat-value.attention { color: var(--color-attention); }
  .hero-stat-value.problem { color: var(--color-problem); }
  .hero-stat-info { display: flex; flex-direction: column; gap: 1px; }
  .hero-stat-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; }
  .hero-stat-detail { font-size: 11px; color: var(--text-tertiary); }

  /* Balance Display */
  .hero-stat.balance-visual { display: flex; align-items: center; justify-content: center; }
  .balance-display { display: flex; align-items: center; gap: 10px; width: 100%; }
  .balance-side { display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 32px; }
  .balance-pct { font-size: 18px; font-weight: 700; color: var(--text-primary); line-height: 1; }
  .balance-leg { font-size: 10px; font-weight: 600; color: var(--text-muted); }
  .balance-bar-wrap { flex: 1; position: relative; }
  .balance-bar { display: flex; height: 8px; background: var(--bg-base); border-radius: 4px; overflow: hidden; }
  .balance-fill { height: 100%; transition: width 0.3s; }
  .balance-fill.left { background: var(--color-optimal); margin-left: auto; border-radius: 4px 0 0 4px; }
  .balance-fill.right { background: var(--color-optimal); border-radius: 0 4px 4px 0; }
  .balance-center-mark { position: absolute; left: 50%; top: -3px; bottom: -3px; width: 2px; background: var(--text-primary); transform: translateX(-50%); border-radius: 1px; }

  /* Zone Mini */
  .hero-stat.zones { display: flex; flex-direction: column; gap: 6px; }
  .zone-mini-bars { display: flex; height: 8px; border-radius: 4px; overflow: hidden; background: var(--bg-base); }
  .zone-mini { height: 100%; transition: width 0.3s; }
  .zone-mini.optimal { background: var(--color-optimal); }
  .zone-mini.attention { background: var(--color-attention); }
  .zone-mini.problem { background: var(--color-problem); }
  .zone-mini-values { display: flex; justify-content: space-between; }
  .zone-mini-val { font-size: 13px; font-weight: 600; }
  .zone-mini-val.optimal { color: var(--color-optimal-text); }
  .zone-mini-val.attention { color: var(--color-attention-text); }
  .zone-mini-val.problem { color: var(--color-problem-text); }

  /* Summary Grid */
  .hero-stat.summary { display: flex; align-items: center; justify-content: center; }
  .summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; width: 100%; }
  .summary-metric { display: flex; flex-direction: column; align-items: center; gap: 2px; }
  .summary-num { font-size: 20px; font-weight: 700; color: var(--text-primary); line-height: 1; }
  .summary-unit { font-size: 10px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .hero-stats { grid-template-columns: 1fr 1fr; gap: 8px; }
    .hero-stat { padding: 10px; }
    .hero-stat-value { font-size: 22px; }
    .hero-stat.asymmetry { flex-direction: column; align-items: flex-start; gap: 6px; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .balance-display { gap: 8px; }
    .balance-side { min-width: 28px; }
    .balance-pct { font-size: 16px; }
    .balance-bar { height: 6px; }
    .summary-num { font-size: 18px; }
    .summary-unit { font-size: 9px; }
    .zone-mini-bars { height: 6px; }
    .zone-mini-val { font-size: 11px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .hero-stats { gap: 6px; }
    .hero-stat { padding: 8px; border-radius: 10px; }
    .hero-stat-value { font-size: 20px; }
    .hero-stat-label { font-size: 10px; }
    .hero-stat-detail { font-size: 10px; }
    .balance-pct { font-size: 15px; }
    .balance-leg { font-size: 9px; }
    .summary-num { font-size: 16px; }
    .zone-mini-val { font-size: 10px; }
  }
</style>
