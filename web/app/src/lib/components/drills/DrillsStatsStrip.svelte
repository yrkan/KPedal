<script lang="ts">
  import { t } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { DrillDashboardStats } from '$lib/types/drills';
  import { formatTotalDuration, getTargetStatus } from '$lib/types/drills';

  export let stats: DrillDashboardStats;
  export let avgTimeInTarget: number;
  export let bestTimeInTarget: number;
  export let completionRate: number;
</script>

<div class="stats-strip animate-in">
  <div class="stat-chip highlight">
    <span class="stat-chip-val {getTargetStatus(avgTimeInTarget)}">{avgTimeInTarget}%</span>
    <span class="stat-chip-label">{$t('drills.avgTarget').toLowerCase()} <InfoTip text={$t('drills.tips.avgTarget')} position="bottom" size="sm" /></span>
  </div>
  <div class="stat-chip">
    <span class="stat-chip-val">{stats.summary.total_drills}</span>
    <span class="stat-chip-label">{$t('drills.sessions').toLowerCase()} <InfoTip text={$t('drills.tips.sessions')} position="bottom" size="sm" /></span>
  </div>
  <div class="stat-chip">
    <span class="stat-chip-val">{formatTotalDuration(stats.summary.total_duration_ms)}</span>
  </div>
  <div class="stat-chip">
    <span class="stat-chip-val optimal">{bestTimeInTarget}%</span>
    <span class="stat-chip-label">{$t('drills.best').toLowerCase()} <InfoTip text={$t('drills.tips.best')} position="bottom" size="sm" /></span>
  </div>
  <div class="stat-chip">
    <span class="stat-chip-val">{completionRate}%</span>
    <span class="stat-chip-label">{$t('drills.completed').toLowerCase()} <InfoTip text={$t('drills.tips.completed')} position="bottom" size="sm" /></span>
  </div>
  <div class="stat-chip">
    <span class="stat-chip-val">{stats.summary.drill_types_tried}</span>
    <span class="stat-chip-label">{$t('drills.drillTypes').toLowerCase()} <InfoTip text={$t('drills.tips.types')} position="bottom" size="sm" /></span>
  </div>
</div>

<style>
  .stats-strip {
    display: flex;
    flex-wrap: nowrap;
    gap: 12px;
    padding: 14px 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }

  .stats-strip::-webkit-scrollbar {
    display: none;
  }

  .stat-chip {
    display: flex;
    align-items: baseline;
    gap: 5px;
    padding: 8px 14px;
    background: var(--bg-base);
    border-radius: 8px;
    flex-shrink: 0;
    white-space: nowrap;
  }

  .stat-chip.highlight {
    background: var(--color-optimal-soft, rgba(94, 232, 156, 0.1));
  }

  .stat-chip-val {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .stat-chip-val.optimal { color: var(--color-optimal-text); }
  .stat-chip-val.attention { color: var(--color-attention-text); }
  .stat-chip-val.problem { color: var(--color-problem-text); }

  .stat-chip-label {
    font-size: 12px;
    color: var(--text-muted);
  }

  @media (max-width: 640px) {
    .stats-strip {
      padding: 12px;
      gap: 8px;
    }

    .stat-chip {
      padding: 6px 10px;
    }

    .stat-chip-val {
      font-size: 14px;
    }

    .stat-chip-label {
      font-size: 11px;
    }
  }
</style>
