<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<div class="empty-state-enhanced animate-in">
  <div class="empty-icon-wrap">
    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <circle cx="12" cy="12" r="10"/>
      <path d="M12 6v6l4 2"/>
    </svg>
  </div>
  <h3>{$t('drills.noDrills')}</h3>
  <p>{$t('drills.noDrillsHint')}</p>

  <div class="drill-categories">
    <span class="drill-cat-label">{$t('drills.availableTypes')}</span>
    <div class="drill-cat-grid">
      <div class="drill-cat">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/></svg>
        <span>{$t('drills.types.balanceFocus')}</span>
      </div>
      <div class="drill-cat">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
        <span>{$t('drills.types.powerTransfer')}</span>
      </div>
      <div class="drill-cat">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg>
        <span>{$t('drills.types.smoothCircles')}</span>
      </div>
      <div class="drill-cat">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
        <span>{$t('drills.types.challenges')}</span>
      </div>
    </div>
  </div>

  <div class="empty-actions">
    <a href="/" class="empty-action-btn">← {$t('nav.dashboard')}</a>
  </div>
</div>

<style>
  .empty-state-enhanced {
    text-align: center;
    padding: 60px 24px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
  }

  .empty-icon-wrap {
    width: 80px;
    height: 80px;
    margin: 0 auto 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 50%;
  }

  .empty-icon-wrap svg {
    color: var(--text-muted);
  }

  .empty-state-enhanced h3 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 8px;
  }

  .empty-state-enhanced > p {
    font-size: 14px;
    color: var(--text-muted);
    margin: 0 0 32px;
  }

  .drill-categories {
    background: var(--bg-base);
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 24px;
  }

  .drill-cat-label {
    display: block;
    font-size: 12px;
    color: var(--text-muted);
    margin-bottom: 16px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .drill-cat-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .drill-cat {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    background: var(--bg-surface);
    border-radius: 8px;
    font-size: 13px;
    color: var(--text-secondary);
  }

  .drill-cat svg {
    color: var(--color-accent);
    flex-shrink: 0;
  }

  .empty-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    flex-wrap: wrap;
  }

  .empty-action-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 12px 20px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    color: var(--text-secondary);
    font-size: 14px;
    font-weight: 500;
    text-decoration: none;
    transition: all 0.2s;
  }

  .empty-action-btn:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  @media (max-width: 640px) {
    .empty-state-enhanced {
      padding: 40px 16px;
    }

    .drill-cat-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
