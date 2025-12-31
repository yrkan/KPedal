<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { RideDetail } from '$lib/types/ride-detail';
  import { getAsymmetry, getBalanceStatus, getScoreStatus } from '$lib/types/ride-detail';

  export let ride: RideDetail;

  function getDominance(bal: number): string {
    if (Math.abs(bal - 50) < 0.5) return $t('rides.detail.balanced');
    return bal > 50 ? $t('rides.detail.leftDominant') : $t('rides.detail.rightDominant');
  }
</script>

<div class="hero-stats animate-in">
  {#if ride.score > 0}
    <div class="hero-stat score-stat">
      <span class="hero-stat-value {getScoreStatus(ride.score)}">{ride.score}</span>
      <div class="hero-stat-info">
        <span class="hero-stat-label">{$t('rides.detail.score')} <InfoTip text={$t('rides.detail.scoreTip')} position="bottom" size="sm" /></span>
      </div>
    </div>
  {/if}

  <div class="hero-stat asymmetry-stat">
    <span class="hero-stat-value {getBalanceStatus(ride.balance_left)}">{getAsymmetry(ride.balance_left).toFixed(1)}%</span>
    <div class="hero-stat-info">
      <span class="hero-stat-label">{$t('rides.detail.asymmetry')} <InfoTip text={$t('rides.detail.asymmetryTip')} position="bottom" size="sm" /></span>
      <span class="hero-stat-detail">{getDominance(ride.balance_left)}</span>
    </div>
  </div>

  <div class="hero-stat balance-stat">
    <div class="balance-display">
      <div class="balance-side">
        <span class="balance-pct">{ride.balance_left.toFixed(0)}</span>
        <span class="balance-leg">L</span>
      </div>
      <div class="balance-bar-wrap">
        <div class="balance-bar">
          <div class="balance-fill left" style="width: {Math.min(50, ride.balance_left)}%"></div>
          <div class="balance-fill right" style="width: {Math.min(50, ride.balance_right)}%"></div>
        </div>
        <div class="balance-center-mark"></div>
      </div>
      <div class="balance-side">
        <span class="balance-pct">{ride.balance_right.toFixed(0)}</span>
        <span class="balance-leg">R</span>
      </div>
    </div>
  </div>

  <div class="hero-stat zones-stat">
    <div class="zone-mini-bars">
      <div class="zone-mini optimal" style="width: {ride.zone_optimal}%"></div>
      <div class="zone-mini attention" style="width: {ride.zone_attention}%"></div>
      <div class="zone-mini problem" style="width: {ride.zone_problem}%"></div>
    </div>
    <div class="zone-mini-values">
      <span class="zone-val optimal">{ride.zone_optimal}%</span>
      <span class="zone-val attention">{ride.zone_attention}%</span>
      <span class="zone-val problem">{ride.zone_problem}%</span>
    </div>
    <span class="hero-stat-label">{$t('rides.detail.timeInZone')} <InfoTip text={$t('rides.detail.timeInZoneTip')} position="bottom" size="sm" /></span>
  </div>
</div>

<style>
  .hero-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
    margin-bottom: 12px;
  }

  .hero-stat {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    padding: 12px;
  }

  .hero-stat.score-stat, .hero-stat.asymmetry-stat { display: flex; align-items: center; gap: 12px; }
  .hero-stat-value { font-size: 26px; font-weight: 700; color: var(--text-primary); }
  .hero-stat-value.optimal { color: var(--color-optimal); }
  .hero-stat-value.attention { color: var(--color-attention); }
  .hero-stat-value.problem { color: var(--color-problem); }
  .hero-stat-info { display: flex; flex-direction: column; gap: 1px; }
  .hero-stat-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; }
  .hero-stat-detail { font-size: 11px; color: var(--text-tertiary); }

  .hero-stat.balance-stat { display: flex; align-items: center; justify-content: center; }
  .balance-display { display: flex; align-items: center; gap: 10px; width: 100%; }
  .balance-side { display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 32px; }
  .balance-pct { font-size: 18px; font-weight: 700; color: var(--text-primary); line-height: 1; }
  .balance-leg { font-size: 10px; font-weight: 600; color: var(--text-muted); }
  .balance-bar-wrap { flex: 1; position: relative; }
  .balance-bar { display: flex; height: 8px; background: var(--bg-base); border-radius: 4px; overflow: hidden; }
  .balance-fill.left { background: var(--color-accent); height: 100%; }
  .balance-fill.right { background: var(--color-accent); opacity: 0.4; height: 100%; }
  .balance-center-mark {
    position: absolute;
    left: 50%;
    top: -2px;
    bottom: -2px;
    width: 2px;
    background: var(--text-muted);
    transform: translateX(-50%);
    border-radius: 1px;
  }

  .hero-stat.zones-stat { display: flex; flex-direction: column; gap: 6px; }
  .zone-mini-bars { display: flex; height: 8px; border-radius: 4px; overflow: hidden; background: var(--bg-base); }
  .zone-mini { height: 100%; }
  .zone-mini.optimal { background: var(--color-optimal); }
  .zone-mini.attention { background: var(--color-attention); }
  .zone-mini.problem { background: var(--color-problem); }
  .zone-mini-values { display: flex; justify-content: space-between; }
  .zone-val { font-size: 11px; font-weight: 600; }
  .zone-val.optimal { color: var(--color-optimal-text); }
  .zone-val.attention { color: var(--color-attention-text); }
  .zone-val.problem { color: var(--color-problem-text); }

  @media (max-width: 768px) {
    .hero-stats { grid-template-columns: 1fr 1fr; gap: 8px; }
    .hero-stat { padding: 10px; }
    .hero-stat-value { font-size: 22px; }
    .hero-stat.score-stat, .hero-stat.asymmetry-stat { flex-direction: column; align-items: flex-start; gap: 4px; }
    .balance-pct { font-size: 15px; }
  }
</style>
