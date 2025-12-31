<script lang="ts">
  import { t, locale } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { DrillResult } from '$lib/types/drills';
  import { formatDrillDuration, formatDrillFullDate, formatDrillTime, getTargetStatus } from '$lib/types/drills';

  export let drills: DrillResult[];
</script>

<div class="data-table-wrapper animate-in">
  <table class="data-table drills-table">
    <thead>
      <tr>
        <th>{$t('drills.date')}</th>
        <th>{$t('drills.drill')}</th>
        <th class="col-duration">{$t('drills.duration')}</th>
        <th>{$t('drills.timeInTarget')} <InfoTip text={$t('drills.tips.timeInTarget')} position="bottom" size="sm" /></th>
        <th class="col-status-header">{$t('drills.status')} <InfoTip text={$t('drills.tips.status')} position="bottom" size="sm" /></th>
      </tr>
    </thead>
    <tbody>
      {#each drills as drill}
        <tr class="drill-row">
          <td class="col-date">
            <span class="date-primary">{formatDrillFullDate(drill.timestamp, $locale, $t('common.today'), $t('common.yesterday'))}</span>
            <span class="date-secondary">{formatDrillTime(drill.timestamp, $locale)}</span>
          </td>
          <td class="col-name">{drill.drill_name}</td>
          <td class="col-duration">{formatDrillDuration(drill.duration_ms)}</td>
          <td class="col-target">
            <div class="target-cell">
              <span class="target-value {getTargetStatus(drill.time_in_target_percent)}">{drill.time_in_target_percent.toFixed(0)}%</span>
              <div class="target-bar">
                <div class="target-fill {getTargetStatus(drill.time_in_target_percent)}" style="width: {drill.time_in_target_percent}%"></div>
              </div>
              <span class="target-time">{formatDrillDuration(drill.time_in_target_ms)}</span>
            </div>
          </td>
          <td class="col-status col-status-header">
            {#if drill.completed}
              <span class="status-badge completed">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <polyline points="20,6 9,17 4,12"/>
                </svg>
                {$t('drills.completed')}
              </span>
            {:else}
              <span class="status-badge incomplete">{$t('drills.incomplete')}</span>
            {/if}
          </td>
        </tr>
      {/each}
    </tbody>
  </table>
</div>

<style>
  .data-table-wrapper {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 14px;
    overflow-x: auto;
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
  }

  .data-table th {
    padding: 12px 16px;
    text-align: left;
    font-weight: 500;
    color: var(--text-muted);
    font-size: 12px;
    border-bottom: 1px solid var(--border-subtle);
    white-space: nowrap;
  }

  .data-table td {
    padding: 12px 16px;
    border-bottom: 1px solid var(--border-subtle);
    color: var(--text-secondary);
  }

  .drill-row:last-child td {
    border-bottom: none;
  }

  .col-date { white-space: nowrap; }
  .date-primary { font-weight: 500; color: var(--text-primary); }
  .date-secondary { font-size: 11px; color: var(--text-muted); margin-left: 8px; }

  .col-name { font-weight: 500; color: var(--text-primary); }
  .col-duration { color: var(--text-secondary); font-variant-numeric: tabular-nums; }

  .col-target { min-width: 180px; }
  .target-cell {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .target-value {
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    min-width: 40px;
  }
  .target-value.optimal { color: var(--color-optimal-text); }
  .target-value.attention { color: var(--color-attention-text); }
  .target-value.problem { color: var(--color-problem-text); }

  .target-bar {
    flex: 1;
    height: 6px;
    background: var(--bg-elevated);
    border-radius: 3px;
    overflow: hidden;
    min-width: 60px;
  }

  .target-fill {
    height: 100%;
    border-radius: 3px;
    transition: width 0.3s ease;
  }
  .target-fill.optimal { background: var(--color-optimal); }
  .target-fill.attention { background: var(--color-attention); }
  .target-fill.problem { background: var(--color-problem); }

  .target-time {
    font-size: 11px;
    color: var(--text-muted);
    min-width: 50px;
    text-align: right;
  }

  .status-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
  }

  .status-badge.completed {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }

  .status-badge.incomplete {
    background: var(--bg-elevated);
    color: var(--text-muted);
  }

  @media (max-width: 768px) {
    .data-table th,
    .data-table td {
      padding: 10px 12px;
    }

    /* Hide duration column (both header and data) */
    .col-duration {
      display: none;
    }

    .target-time {
      display: none;
    }

    .col-target {
      min-width: 120px;
    }

    .target-bar {
      min-width: 40px;
    }

    /* Compact status badge */
    .status-badge {
      padding: 3px 8px;
      font-size: 11px;
    }
  }

  @media (max-width: 480px) {
    .data-table th,
    .data-table td {
      padding: 8px 10px;
    }

    .col-date {
      min-width: 70px;
    }

    .date-secondary {
      display: block;
      margin-left: 0;
      margin-top: 2px;
    }

    .col-target {
      min-width: 90px;
    }

    .target-bar {
      display: none;
    }

    /* Hide status column on very small screens */
    .col-status,
    .col-status-header {
      display: none;
    }
  }
</style>
