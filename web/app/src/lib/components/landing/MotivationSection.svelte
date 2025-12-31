<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section class="section motivation-section" aria-labelledby="motivation-title">
  <h2 id="motivation-title" class="section-title">{$t('landing.motivation.title')}</h2>
  <p class="section-subtitle">{$t('landing.motivation.subtitle')}</p>

  <div class="motivation-content">
    <div class="motivation-stats">
      <div class="motivation-stat">
        <span class="stat-number">16</span>
        <span class="stat-desc">{$t('landing.motivation.achievements')}</span>
      </div>
      <div class="motivation-divider"></div>
      <div class="motivation-stat">
        <span class="stat-number">7</span>
        <span class="stat-desc">{$t('landing.motivation.challenges')}</span>
      </div>
    </div>

    <div class="motivation-examples">
      <div class="example-group">
        <span class="example-label">{$t('landing.motivation.achievementsLabel')}</span>
        <div class="example-items">
          <span>{$t('landing.motivation.examples.firstRide')}</span>
          <span>{$t('landing.motivation.examples.hundredRides')}</span>
          <span>{$t('landing.motivation.examples.perfectBalance')}</span>
          <span>{$t('landing.motivation.examples.thirtyDayStreak')}</span>
        </div>
      </div>
      <div class="example-group">
        <span class="example-label">{$t('landing.motivation.challengesLabel')}</span>
        <div class="example-items">
          <span>{$t('landing.motivation.examples.balancedRider')}</span>
          <span>{$t('landing.motivation.examples.zoneMaster')}</span>
        </div>
      </div>
    </div>
  </div>
</section>

<style>
  .motivation-section {
    text-align: center;
  }
  .motivation-content {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    padding: 40px;
  }
  .motivation-stats {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 48px;
    margin-bottom: 36px;
  }
  .motivation-stat {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .stat-number {
    font-size: 44px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -1.5px;
    line-height: 1;
  }
  .stat-desc {
    font-size: 14px;
    color: var(--text-muted);
  }
  .motivation-divider {
    width: 1px;
    height: 56px;
    background: var(--border-subtle);
  }
  .motivation-examples {
    display: flex;
    justify-content: center;
    gap: 48px;
    padding-top: 28px;
    border-top: 1px solid var(--border-subtle);
  }
  .example-group {
    text-align: left;
  }
  .example-label {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    display: block;
    margin-bottom: 14px;
  }
  .example-items {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .example-items span {
    font-size: 14px;
    color: var(--text-tertiary);
  }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .motivation-content { padding: 28px; }
    .motivation-examples { flex-direction: column; gap: 24px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .motivation-content {
      padding: 20px 16px;
    }
    .motivation-stats {
      flex-direction: column;
      gap: 20px;
    }
    .motivation-divider { width: 48px; height: 1px; }
    .stat-number { font-size: 36px; }
    .motivation-examples { gap: 20px; padding-top: 16px; }
  }

  /* Responsive - 320px */
  @media (max-width: 320px) {
    .motivation-stats { flex-direction: row; gap: 32px; }
    .motivation-divider { width: 1px; height: 40px; }
  }
</style>
