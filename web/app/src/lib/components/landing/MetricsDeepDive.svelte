<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section id="features" class="section" aria-labelledby="features-title" itemprop="featureList">
  <h2 id="features-title" class="section-title">{$t('landing.metrics.title')}</h2>
  <p class="section-subtitle">{$t('landing.metrics.subtitle')}</p>

  <div class="metrics-deep">
    <div class="metric-deep-card">
      <div class="metric-deep-header">
        <div class="metric-deep-icon balance"></div>
        <div>
          <h3>{$t('landing.metrics.balance.title')}</h3>
          <span class="metric-deep-range">{$t('landing.metrics.balance.range')}</span>
        </div>
      </div>
      <p class="metric-deep-desc">{$t('landing.metrics.balance.desc')}</p>
      <div class="metric-zones">
        <div class="zone optimal"><span>48-52%</span> {$t('landing.metrics.zones.optimal')}</div>
        <div class="zone attention"><span>45-48% / 52-55%</span> {$t('landing.metrics.zones.attention')}</div>
        <div class="zone problem"><span>&lt;45% / &gt;55%</span> {$t('landing.metrics.zones.problem')}</div>
      </div>
    </div>

    <div class="metric-deep-card featured">
      <div class="metric-deep-header">
        <div class="metric-deep-icon te"></div>
        <div>
          <h3>{$t('landing.metrics.te.title')}</h3>
          <span class="metric-deep-range">{$t('landing.metrics.te.range')}</span>
        </div>
      </div>
      <p class="metric-deep-desc">{@html $t('landing.metrics.te.desc')}</p>
      <div class="metric-zones">
        <div class="zone optimal"><span>70-80%</span> {$t('landing.metrics.zones.optimal')}</div>
        <div class="zone attention"><span>60-70% / 80-85%</span> {$t('landing.metrics.zones.attention')}</div>
        <div class="zone problem"><span>&lt;60% / &gt;85%</span> {$t('landing.metrics.zones.problem')}</div>
      </div>
    </div>

    <div class="metric-deep-card">
      <div class="metric-deep-header">
        <div class="metric-deep-icon ps"></div>
        <div>
          <h3>{$t('landing.metrics.ps.title')}</h3>
          <span class="metric-deep-range">{$t('landing.metrics.ps.range')}</span>
        </div>
      </div>
      <p class="metric-deep-desc">{$t('landing.metrics.ps.desc')}</p>
      <div class="metric-zones">
        <div class="zone optimal"><span>≥20%</span> {$t('landing.metrics.zones.optimal')}</div>
        <div class="zone attention"><span>15-20%</span> {$t('landing.metrics.zones.attention')}</div>
        <div class="zone problem"><span>&lt;15%</span> {$t('landing.metrics.zones.problem')}</div>
      </div>
    </div>
  </div>
</section>

<style>
  .metrics-deep {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .metric-deep-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 18px;
    padding: 24px 28px;
    transition: all 0.3s ease;
  }
  .metric-deep-card:hover {
    border-color: var(--border-default);
  }
  .metric-deep-card.featured {
    border-color: rgba(94, 232, 156, 0.15);
  }
  .metric-deep-header {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 14px;
  }
  .metric-deep-icon {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    opacity: 0.8;
  }
  .metric-deep-icon.balance { background: var(--color-attention); }
  .metric-deep-icon.te { background: var(--color-optimal); }
  .metric-deep-icon.ps { background: var(--color-problem); }
  .metric-deep-header h3 {
    font-size: 17px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
    letter-spacing: -0.2px;
  }
  .metric-deep-range {
    font-size: 12px;
    color: var(--text-muted);
    font-weight: 500;
    margin-left: auto;
  }
  .metric-deep-desc {
    font-size: 14px;
    color: var(--text-tertiary);
    line-height: 1.7;
    margin-bottom: 18px;
  }
  .metric-zones {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
  .zone {
    font-size: 11px;
    padding: 6px 12px;
    border-radius: 100px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 500;
  }
  .zone span { font-weight: 600; }
  .zone.optimal { background: var(--color-optimal-soft); color: var(--color-optimal-text); }
  .zone.attention { background: var(--color-attention-soft); color: var(--color-attention-text); }
  .zone.problem { background: var(--color-problem-soft); color: var(--color-problem-text); }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .metrics-deep { gap: 14px; }
    .metric-deep-card { padding: 20px 24px; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .metric-deep-card { padding: 20px; border-radius: 14px; }
    .metric-deep-header { gap: 12px; margin-bottom: 12px; }
    .metric-deep-header h3 { font-size: 17px; }
    .metric-deep-range { font-size: 12px; }
    .metric-deep-desc { font-size: 14px; line-height: 1.55; }
    .metric-zones { gap: 8px; margin-top: 14px; }
    .zone { padding: 8px 12px; font-size: 12px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .metric-deep-card {
      padding: 16px 14px;
    }
    .metric-deep-header { gap: 10px; margin-bottom: 10px; }
    .metric-deep-header h3 { font-size: 16px; }
    .metric-deep-range { font-size: 11px; }
    .metric-deep-desc { font-size: 13px; line-height: 1.5; }
    .metric-zones {
      flex-direction: column;
      gap: 6px;
    }
    .zone {
      font-size: 11px;
      padding: 8px 14px;
      justify-content: flex-start;
    }
    .zone span { font-weight: 600; }
  }

  /* Touch devices */
  @media (hover: none) and (pointer: coarse) {
    .metric-deep-card:hover {
      transform: none;
    }
  }
</style>
