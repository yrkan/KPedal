<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section class="hero" aria-labelledby="hero-title">
  <p class="hero-eyebrow">
    <span itemprop="applicationSubCategory">{$t('landing.hero.eyebrow')}</span>
    <a href="https://github.com/yrkan/KPedal/releases/latest" target="_blank" rel="noopener" class="hero-version" itemprop="softwareVersion">{$t('landing.hero.version')}</a>
  </p>
  <h1 id="hero-title" class="hero-title" itemprop="headline">
    <span class="hero-title-line">{$t('landing.hero.titleLine1')}</span>
    <span class="hero-title-line">{$t('landing.hero.titleLine2')}</span>
  </h1>
  <p class="hero-subtitle" itemprop="description">
    {$t('landing.hero.subtitle')}
  </p>

  <div class="hero-actions" role="group" aria-label={$t('aria.downloadOptions')}>
    <a href="https://github.com/yrkan/kpedal/releases" class="hero-cta" target="_blank" rel="noopener noreferrer" itemprop="downloadUrl" aria-label={$t('aria.downloadKPedal')}>
      {$t('landing.hero.downloadFree')}
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
        <path d="M5 12h14M12 5l7 7-7 7"/>
      </svg>
    </a>
    <a href="https://app.kpedal.com/login" class="hero-cta-secondary" aria-label={$t('aria.tryDemo')}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <circle cx="12" cy="12" r="10"/>
        <polygon points="10 8 16 12 10 16 10 8" fill="currentColor" stroke="none"/>
      </svg>
      {$t('landing.hero.tryDemo')}
    </a>
  </div>

  <p class="hero-note">
    <span>{$t('landing.hero.free')}</span> & <a href="https://github.com/yrkan/kpedal" target="_blank" rel="noopener noreferrer">{$t('landing.hero.openSource')}</a> · {$t('landing.hero.noAccountRequired')}
  </p>
</section>

<style>
  .hero {
    text-align: center;
    padding: 48px 0 80px;
  }

  .hero-eyebrow {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 1.5px;
    margin-bottom: 24px;
  }

  .hero-title {
    font-size: 52px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -2px;
    line-height: 1.1;
    margin-bottom: 24px;
  }
  .hero-title-line {
    display: block;
  }

  .hero-subtitle {
    font-size: 17px;
    color: var(--text-tertiary);
    max-width: 480px;
    margin: 0 auto 40px;
    line-height: 1.6;
  }

  .hero-cta {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: 54px;
    padding: 0 28px;
    background: var(--text-primary);
    color: var(--bg-base);
    font-size: 15px;
    font-weight: 500;
    border-radius: 100px;
    border: 1px solid transparent;
    text-decoration: none;
    transition: all 0.2s ease;
  }
  .hero-cta:hover {
    opacity: 0.85;
    transform: translateY(-1px);
  }
  .hero-cta svg {
    transition: transform 0.2s ease;
  }
  .hero-cta:hover svg {
    transform: translateX(3px);
  }

  .hero-version {
    display: inline-block;
    margin-left: 8px;
    padding: 3px 8px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-default);
    border-radius: 6px;
    font-size: 11px;
    font-weight: 500;
    color: var(--text-tertiary);
    letter-spacing: 0;
    text-transform: none;
    text-decoration: none;
    transition: border-color 0.15s, color 0.15s;
  }
  .hero-version:hover {
    border-color: var(--color-optimal);
    color: var(--color-optimal);
  }

  .hero-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
  }

  .hero-cta-secondary {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: 54px;
    padding: 0 28px;
    background: var(--color-optimal);
    color: white;
    font-size: 15px;
    font-weight: 500;
    border-radius: 100px;
    border: none;
    cursor: pointer;
    transition: all 0.2s ease;
    text-decoration: none;
    box-shadow: 0 2px 8px var(--glow-optimal, rgba(34, 197, 94, 0.3));
  }
  .hero-cta-secondary:hover {
    filter: brightness(1.1);
    transform: translateY(-1px);
    box-shadow: 0 4px 14px var(--glow-optimal, rgba(34, 197, 94, 0.4));
  }

  :global([data-theme="dark"]) .hero-cta-secondary {
    background: #16a34a;
    box-shadow: 0 2px 8px rgba(22, 163, 74, 0.25);
  }
  :global([data-theme="dark"]) .hero-cta-secondary:hover {
    background: #15803d;
    box-shadow: 0 4px 14px rgba(22, 163, 74, 0.35);
  }

  .hero-note {
    font-size: 13px;
    color: var(--text-muted);
    margin-top: 24px;
  }
  .hero-note a {
    color: var(--color-optimal);
    text-decoration: none;
  }
  .hero-note a:hover {
    text-decoration: underline;
  }

  /* Responsive */
  @media (max-width: 640px) {
    .hero { padding: 36px 0 56px; }
    .hero-eyebrow { font-size: 12px; margin-bottom: 18px; }
    .hero-title { font-size: 40px; letter-spacing: -1.5px; }
    .hero-subtitle { font-size: 16px; max-width: 380px; margin-bottom: 36px; }
    .hero-actions { gap: 12px; }
    .hero-cta, .hero-cta-secondary {
      height: 50px;
      padding: 0 24px;
      font-size: 14px;
    }
    .hero-note { font-size: 13px; margin-top: 20px; }
  }
</style>
