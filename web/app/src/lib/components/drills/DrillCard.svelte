<script lang="ts">
  import { t, locale } from '$lib/i18n';
  import type { DrillResult } from '$lib/types/drills';
  import { formatDrillDuration, formatDrillFullDate, formatDrillTime, getTargetStatus } from '$lib/types/drills';

  export let drill: DrillResult;
</script>

<article class="drill-card">
  <div class="drill-card-header">
    <div class="drill-info">
      <span class="drill-name">{drill.drill_name}</span>
      <div class="drill-when">
        <span class="drill-date">{formatDrillFullDate(drill.timestamp, $locale, $t('common.today'), $t('common.yesterday'))}</span>
        <span class="drill-time">{formatDrillTime(drill.timestamp, $locale)}</span>
      </div>
    </div>
    <div class="drill-badges">
      {#if drill.completed}
        <span class="badge completed">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <polyline points="20,6 9,17 4,12"/>
          </svg>
        </span>
      {/if}
      <span class="drill-duration">{formatDrillDuration(drill.duration_ms)}</span>
    </div>
  </div>

  <div class="drill-target-section">
    <div class="target-display">
      <span class="target-number {getTargetStatus(drill.time_in_target_percent)}">{drill.time_in_target_percent.toFixed(0)}%</span>
      <span class="target-label">{$t('drills.timeInTarget')}</span>
    </div>
    <div class="target-details">
      <div class="target-progress-bar">
        <div class="target-progress-fill {getTargetStatus(drill.time_in_target_percent)}" style="width: {drill.time_in_target_percent}%"></div>
      </div>
      <span class="target-duration">{formatDrillDuration(drill.time_in_target_ms)} {$t('drills.of')} {formatDrillDuration(drill.duration_ms)}</span>
    </div>
  </div>
</article>

<style>
  .drill-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 14px;
    padding: 16px;
    transition: all 0.2s;
  }

  .drill-card:hover {
    border-color: var(--border-default);
    box-shadow: var(--shadow-sm);
  }

  .drill-card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;
  }

  .drill-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .drill-name {
    font-weight: 600;
    color: var(--text-primary);
    font-size: 14px;
  }

  .drill-when {
    display: flex;
    gap: 8px;
    font-size: 12px;
  }

  .drill-date {
    color: var(--text-secondary);
  }

  .drill-time {
    color: var(--text-muted);
  }

  .drill-badges {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .badge.completed {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
    border-radius: 50%;
  }

  .drill-duration {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    background: var(--bg-elevated);
    padding: 4px 10px;
    border-radius: 6px;
  }

  .drill-target-section {
    display: flex;
    gap: 16px;
    align-items: center;
    padding-top: 16px;
    border-top: 1px solid var(--border-subtle);
  }

  .target-display {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 70px;
  }

  .target-number {
    font-size: 28px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }

  .target-number.optimal { color: var(--color-optimal-text); }
  .target-number.attention { color: var(--color-attention-text); }
  .target-number.problem { color: var(--color-problem-text); }

  .target-label {
    font-size: 11px;
    color: var(--text-muted);
    margin-top: 4px;
  }

  .target-details {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .target-progress-bar {
    height: 10px;
    background: var(--bg-elevated);
    border-radius: 5px;
    overflow: hidden;
  }

  .target-progress-fill {
    height: 100%;
    border-radius: 5px;
    transition: width 0.3s ease;
  }
  .target-progress-fill.optimal { background: var(--color-optimal); }
  .target-progress-fill.attention { background: var(--color-attention); }
  .target-progress-fill.problem { background: var(--color-problem); }

  .target-duration {
    font-size: 12px;
    color: var(--text-muted);
  }
</style>
