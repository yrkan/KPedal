<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section class="section alerts-section" aria-labelledby="alerts-title">
  <h2 id="alerts-title" class="section-title">{$t('landing.alerts.title')}</h2>
  <p class="section-subtitle">{$t('landing.alerts.subtitle')}</p>

  <div class="alerts-demo">
    <div class="alert-banner-demo">
      <div class="alert-indicator"></div>
      <div class="alert-content">
        <span class="alert-title">{$t('landing.alerts.demo.title')}</span>
        <span class="alert-detail">{$t('landing.alerts.demo.detail')}</span>
      </div>
    </div>

    <div class="alerts-features">
      <div class="alert-feature">
        <h4>{$t('landing.alerts.features.vibration.title')}</h4>
        <p>{$t('landing.alerts.features.vibration.desc')}</p>
      </div>
      <div class="alert-feature">
        <h4>{$t('landing.alerts.features.screenWake.title')}</h4>
        <p>{$t('landing.alerts.features.screenWake.desc')}</p>
      </div>
      <div class="alert-feature">
        <h4>{$t('landing.alerts.features.cooldown.title')}</h4>
        <p>{$t('landing.alerts.features.cooldown.desc')}</p>
      </div>
      <div class="alert-feature">
        <h4>{$t('landing.alerts.features.thresholds.title')}</h4>
        <p>{$t('landing.alerts.features.thresholds.desc')}</p>
      </div>
    </div>
  </div>
</section>

<style>
  .alerts-section {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    padding: 36px;
  }
  .alerts-demo {
    display: flex;
    flex-direction: column;
    gap: 36px;
  }
  .alert-banner-demo {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px 28px;
    background: var(--bg-base);
    border-radius: 16px;
    max-width: 320px;
    margin: 0 auto;
  }
  .alert-indicator {
    width: 8px;
    height: 8px;
    background: var(--color-attention);
    border-radius: 50%;
    box-shadow: 0 0 8px var(--color-attention);
    animation: pulse-glow 2s ease-in-out infinite;
  }
  @keyframes pulse-glow {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }
  .alert-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .alert-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.1px;
  }
  .alert-detail {
    font-size: 13px;
    color: var(--text-muted);
    font-variant-numeric: tabular-nums;
  }
  .alerts-features {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0;
    background: var(--bg-base);
    border-radius: 16px;
    overflow: hidden;
  }
  .alert-feature {
    padding: 24px 28px;
    border-bottom: 1px solid var(--border-subtle);
    border-right: 1px solid var(--border-subtle);
  }
  .alert-feature:nth-child(2n) {
    border-right: none;
  }
  .alert-feature:nth-last-child(-n+2) {
    border-bottom: none;
  }
  .alert-feature h4 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 6px;
    letter-spacing: -0.1px;
  }
  .alert-feature p {
    font-size: 13px;
    color: var(--text-muted);
    line-height: 1.55;
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .alerts-section { padding: 24px; border-radius: 16px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .alerts-section {
      padding: 20px 16px;
    }
    .alert-banner-demo {
      padding: 16px 20px;
    }
    .alert-title { font-size: 13px; }
  }

  /* Even smaller */
  @media (max-width: 320px) {
    .alerts-section { padding: 16px 14px; }
  }
</style>
