<script lang="ts">
  import { t } from '$lib/i18n';
  import type { SummaryItem } from './types';

  export let items: SummaryItem[];

  const icons = {
    shield: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`,
    'no-location': `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>`,
    'no-money': `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>`,
    delete: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3,6 5,6 21,6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>`
  };
</script>

<section class="quick-summary">
  <div class="summary-grid">
    {#each items as item}
      <div class="summary-item">
        <div class="summary-icon">{@html icons[item.icon]}</div>
        <div class="summary-text">
          <strong>{$t(item.titleKey)}</strong>
          <span>{$t(item.descKey)}</span>
        </div>
      </div>
    {/each}
  </div>
</section>

<style>
  .quick-summary {
    margin-bottom: 40px;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .summary-item {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
  }

  .summary-icon {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 10px;
    color: var(--text-secondary);
    flex-shrink: 0;
  }

  .summary-text {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .summary-text strong {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .summary-text span {
    font-size: 13px;
    color: var(--text-tertiary);
  }

  @media (max-width: 640px) {
    .summary-grid {
      grid-template-columns: 1fr;
    }

    .summary-item {
      padding: 14px;
      gap: 12px;
    }

    .summary-icon {
      width: 36px;
      height: 36px;
    }

    .summary-text strong {
      font-size: 13px;
    }

    .summary-text span {
      font-size: 12px;
    }
  }
</style>
