<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section class="section works-everywhere-section" aria-labelledby="works-title">
  <h2 id="works-title" class="section-title">{$t('landing.works.title')}</h2>
  <p class="section-subtitle">{$t('landing.works.subtitle')}</p>

  <div class="works-grid">
    <div class="works-card">
      <div class="works-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/>
        </svg>
      </div>
      <h4>{$t('landing.works.background.title')}</h4>
      <p>{$t('landing.works.background.desc')}</p>
    </div>
    <div class="works-card">
      <div class="works-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
        </svg>
      </div>
      <h4>{$t('landing.works.sync.title')}</h4>
      <p>{$t('landing.works.sync.desc')}</p>
    </div>
    <div class="works-card">
      <div class="works-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
        </svg>
      </div>
      <h4>{$t('landing.works.multiDevice.title')}</h4>
      <p>{$t('landing.works.multiDevice.desc')}</p>
    </div>
  </div>
</section>

<style>
  .works-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }
  .works-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 18px;
    padding: 28px 24px;
    text-align: center;
    transition: all 0.3s ease;
  }
  .works-card:hover {
    border-color: var(--border-default);
  }
  .works-icon {
    width: 48px;
    height: 48px;
    background: var(--bg-elevated);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-muted);
    margin: 0 auto 16px;
  }
  .works-icon svg {
    width: 26px;
    height: 26px;
  }
  .works-card h4 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 12px;
    letter-spacing: -0.2px;
  }
  .works-card p {
    font-size: 13px;
    color: var(--text-tertiary);
    line-height: 1.7;
  }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .works-grid { grid-template-columns: 1fr; gap: 12px; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .works-card { padding: 20px; border-radius: 14px; }
    .works-card h4 { font-size: 16px; }
    .works-card p { font-size: 13px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .works-card {
      padding: 16px;
    }
    .works-icon svg { width: 24px; height: 24px; }
    .works-card h4 { font-size: 15px; margin: 10px 0 6px; }
    .works-card p { font-size: 12px; line-height: 1.5; }
  }

  /* Responsive - 320px */
  @media (max-width: 320px) {
    .works-card { padding: 16px 14px; }
  }

  /* Touch devices */
  @media (hover: none) and (pointer: coarse) {
    .works-card:hover {
      transform: none;
    }
  }
</style>
