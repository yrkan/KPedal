<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section class="section cta-section" aria-labelledby="cta-title">
  <h2 id="cta-title" class="cta-headline">{$t('landing.cta.headline')}</h2>
  <p class="cta-subtext">{$t('landing.cta.subtext')}</p>
  <div class="cta-actions" role="group" aria-label={$t('aria.getStartedOptions')}>
    <a href="https://github.com/yrkan/kpedal/releases" class="cta-btn primary large" target="_blank" rel="noopener noreferrer" aria-label={$t('aria.downloadKaroo')}>
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
        <polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
      </svg>
      {$t('landing.cta.downloadForKaroo')}
    </a>
    <a href="https://app.kpedal.com/login" class="cta-btn secondary" aria-label={$t('aria.tryDemo')}>
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <circle cx="12" cy="12" r="10"/>
        <polygon points="10 8 16 12 10 16 10 8" fill="currentColor" stroke="none"/>
      </svg>
      {$t('landing.cta.tryDemo')}
    </a>
  </div>
</section>

<style>
  .cta-section {
    text-align: center;
    padding: 80px 0 64px;
  }
  .cta-headline {
    font-size: 30px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 16px;
    letter-spacing: -0.5px;
  }
  .cta-subtext {
    font-size: 16px;
    color: var(--text-tertiary);
    margin-bottom: 40px;
    letter-spacing: -0.1px;
  }
  .cta-actions {
    display: flex;
    gap: 16px;
    justify-content: center;
    flex-wrap: wrap;
  }

  /* Buttons */
  .cta-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 24px;
    border-radius: 100px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.25s ease;
    text-decoration: none;
    border: none;
    letter-spacing: -0.1px;
  }
  .cta-btn svg { opacity: 0.8; }

  .cta-btn.primary {
    background: var(--text-primary);
    color: var(--bg-base);
  }
  .cta-btn.primary:hover {
    opacity: 0.85;
    transform: translateY(-1px);
  }

  .cta-btn.secondary {
    background: var(--color-optimal);
    border: none;
    color: white;
    box-shadow: 0 2px 8px var(--glow-optimal, rgba(34, 197, 94, 0.3));
  }
  .cta-btn.secondary:hover {
    filter: brightness(1.1);
    transform: translateY(-1px);
    box-shadow: 0 4px 14px var(--glow-optimal, rgba(34, 197, 94, 0.4));
  }
  :global([data-theme="dark"]) .cta-btn.secondary {
    background: #16a34a;
    box-shadow: 0 2px 8px rgba(22, 163, 74, 0.25);
  }
  :global([data-theme="dark"]) .cta-btn.secondary:hover {
    background: #15803d;
    box-shadow: 0 4px 14px rgba(22, 163, 74, 0.35);
  }

  .cta-btn.large {
    padding: 14px 32px;
    font-size: 15px;
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .cta-section { padding: 56px 0 40px; }
    .cta-headline { font-size: 28px; }
    .cta-subtext { font-size: 15px; }
    .cta-btn.large {
      width: 100%;
      justify-content: center;
    }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .cta-section { padding: 40px 0 32px; }
    .cta-headline { font-size: 22px; letter-spacing: -0.3px; }
    .cta-subtext {
      font-size: 13px;
      margin-bottom: 28px;
      padding: 0 8px;
    }
    .cta-btn {
      padding: 12px 20px;
      font-size: 14px;
    }
    .cta-btn.large {
      padding: 14px 24px;
      font-size: 14px;
    }
  }

  /* Responsive - 320px */
  @media (max-width: 320px) {
    .cta-headline { font-size: 20px; }
    .cta-subtext { font-size: 12px; }
    .cta-btn.large { padding: 12px 20px; font-size: 14px; }
  }
</style>
