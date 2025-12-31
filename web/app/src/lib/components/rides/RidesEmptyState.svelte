<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<div class="empty-state-enhanced animate-in">
  <div class="empty-icon-wrap">
    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
    </svg>
  </div>
  <h3>{$t('rides.noRides')}</h3>
  <p>{$t('rides.noRidesHint')}</p>

  <div class="ride-metrics-preview">
    <span class="preview-label">{$t('rides.eachRideShows')}</span>
    <div class="preview-grid">
      <div class="preview-item">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/></svg>
        <span>{$t('metrics.leftRight')} {$t('metrics.balance')}</span>
      </div>
      <div class="preview-item">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
        <span>{$t('metrics.teShort')}</span>
      </div>
      <div class="preview-item">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg>
        <span>{$t('metrics.psShort')}</span>
      </div>
      <div class="preview-item">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20V10M18 20V4M6 20v-4"/></svg>
        <span>{$t('zones.title')}</span>
      </div>
    </div>
  </div>

  <div class="empty-actions">
    <a href="/settings" class="empty-action-btn secondary">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      {$t('settings.linkDevice')}
    </a>
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

  .ride-metrics-preview {
    background: var(--bg-base);
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 24px;
  }

  .preview-label {
    display: block;
    font-size: 12px;
    color: var(--text-muted);
    margin-bottom: 16px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .preview-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .preview-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    background: var(--bg-surface);
    border-radius: 8px;
    font-size: 13px;
    color: var(--text-secondary);
  }

  .preview-item svg {
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

  .empty-action-btn.secondary {
    background: var(--color-accent-soft);
    border-color: var(--color-accent);
    color: var(--color-accent);
  }

  .empty-action-btn.secondary:hover {
    background: var(--color-accent);
    color: white;
  }

  @media (max-width: 640px) {
    .empty-state-enhanced {
      padding: 40px 16px;
    }

    .preview-grid {
      grid-template-columns: 1fr;
    }

    .empty-actions {
      flex-direction: column;
    }

    .empty-action-btn {
      justify-content: center;
    }
  }
</style>
