<script lang="ts">
  import { t } from '$lib/i18n';

  export let title: string;
  export let description: string;
  export let iconPath: string = 'M22 12h-4l-3 9L9 3l-3 9H2'; // default: activity icon
  export let maxWidth: string = '420px';
</script>

<div class="empty-state-enhanced animate-in" style="max-width: {maxWidth};">
  <div class="empty-icon-wrap">
    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <path d={iconPath}/>
    </svg>
  </div>
  <h3>{title}</h3>
  <p>{description}</p>

  <!-- Preview content slot -->
  <slot name="preview" />

  <!-- Actions slot -->
  <div class="empty-actions">
    <slot name="actions">
      <a href="/" class="empty-action-btn">← {$t('nav.dashboard')}</a>
    </slot>
  </div>
</div>

<style>
  .empty-state-enhanced {
    text-align: center;
    padding: 60px 24px;
    margin: 0 auto;
  }

  .empty-icon-wrap {
    width: 80px;
    height: 80px;
    margin: 0 auto 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    color: var(--text-muted);
  }

  h3 {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.5;
    margin-bottom: 24px;
  }

  .empty-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
  }

  :global(.empty-action-btn) {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 10px 18px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    text-decoration: none;
    transition: all 0.15s;
    background: var(--bg-surface);
    color: var(--text-secondary);
    border: 1px solid var(--border-subtle);
  }

  :global(.empty-action-btn:hover) {
    border-color: var(--border-default);
    color: var(--text-primary);
  }

  :global(.empty-action-btn.primary) {
    background: var(--color-accent);
    color: var(--color-accent-text);
    border-color: var(--color-accent);
  }

  :global(.empty-action-btn.primary:hover) {
    background: var(--color-accent-hover);
    border-color: var(--color-accent-hover);
  }

  /* Mobile */
  @media (max-width: 480px) {
    .empty-state-enhanced {
      padding: 40px 20px;
    }

    .empty-icon-wrap {
      width: 64px;
      height: 64px;
      border-radius: 16px;
    }

    .empty-icon-wrap :global(svg) {
      width: 36px;
      height: 36px;
    }

    h3 {
      font-size: 18px;
    }

    p {
      font-size: 13px;
    }

    .empty-actions {
      flex-direction: column;
    }

    :global(.empty-action-btn) {
      width: 100%;
      justify-content: center;
    }
  }
</style>
