<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section class="section before-after-section" aria-labelledby="difference-title">
  <h2 id="difference-title" class="section-title">{$t('landing.comparison.title')}</h2>
  <p class="section-subtitle">{$t('landing.comparison.subtitle')}</p>

  <div class="comparison-table">
    <div class="comparison-header">
      <div class="comparison-col">{$t('landing.comparison.without')}</div>
      <div class="comparison-col highlight">{$t('landing.comparison.with')}</div>
    </div>
    <div class="comparison-row">
      <div class="comparison-item before">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <span>{$t('landing.comparison.items.before1')}</span>
      </div>
      <div class="comparison-item after">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span>{$t('landing.comparison.items.after1')}</span>
      </div>
    </div>
    <div class="comparison-row">
      <div class="comparison-item before">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <span>{$t('landing.comparison.items.before2')}</span>
      </div>
      <div class="comparison-item after">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span>{$t('landing.comparison.items.after2')}</span>
      </div>
    </div>
    <div class="comparison-row">
      <div class="comparison-item before">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <span>{$t('landing.comparison.items.before3')}</span>
      </div>
      <div class="comparison-item after">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span>{$t('landing.comparison.items.after3')}</span>
      </div>
    </div>
    <div class="comparison-row">
      <div class="comparison-item before">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <span>{$t('landing.comparison.items.before4')}</span>
      </div>
      <div class="comparison-item after">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span>{$t('landing.comparison.items.after4')}</span>
      </div>
    </div>
  </div>
</section>

<style>
  .comparison-table {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    overflow: hidden;
  }
  .comparison-header {
    display: grid;
    grid-template-columns: 1fr 1fr;
    background: var(--bg-elevated);
  }
  .comparison-col {
    padding: 16px 24px;
    font-size: 11px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .comparison-col.highlight {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }
  .comparison-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    border-top: 1px solid var(--border-subtle);
  }
  .comparison-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    font-size: 12px;
  }
  .comparison-item.before {
    color: var(--text-muted);
  }
  .comparison-item.before svg {
    color: var(--color-problem);
    width: 14px;
    height: 14px;
  }
  .comparison-item.after {
    background: var(--color-optimal-soft);
    color: var(--text-primary);
  }
  .comparison-item.after svg {
    color: var(--color-optimal);
    width: 14px;
    height: 14px;
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .comparison-row {
      grid-template-columns: 1fr;
      gap: 0;
    }
    .comparison-item {
      padding: 14px 16px;
    }
    .comparison-item.after {
      border-top: 1px solid var(--border-subtle);
      border-left: none;
    }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .comparison-table { border-radius: 14px; }
    .comparison-header { display: none; }
    .comparison-row { gap: 0; }
    .comparison-item {
      padding: 14px 16px;
      font-size: 13px;
      flex-direction: column;
      align-items: flex-start;
      gap: 6px;
    }
    .comparison-item svg { flex-shrink: 0; }
    .comparison-item.before::before {
      content: 'Without KPedal';
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--text-muted);
      display: block;
      margin-bottom: 2px;
    }
    .comparison-item.after::before {
      content: 'With KPedal';
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--color-optimal-text);
      display: block;
      margin-bottom: 2px;
    }
  }
</style>
