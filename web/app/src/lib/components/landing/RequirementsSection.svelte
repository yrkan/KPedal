<script lang="ts">
  import { t } from '$lib/i18n';
</script>

<section class="section requirements-section" aria-labelledby="requirements-title" itemprop="softwareRequirements">
  <h2 id="requirements-title" class="section-title">{$t('landing.requirements.title')}</h2>
  <p class="section-subtitle">{$t('landing.requirements.subtitle')}</p>

  <div class="requirements-grid">
    <div class="req-card">
      <div class="req-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/>
        </svg>
      </div>
      <h4>{$t('landing.requirements.karoo.title')}</h4>
      <p>{$t('landing.requirements.karoo.desc')}</p>
    </div>
    <div class="req-card">
      <div class="req-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/>
        </svg>
      </div>
      <h4>{$t('landing.requirements.pedals.title')}</h4>
      <p>{$t('landing.requirements.pedals.desc')}</p>
    </div>
  </div>

  <div class="pedals-compatibility">
    <div class="pedals-grid">
      <div class="pedals-card full">
        <div class="pedals-card-header">
          <span class="pedals-icon">✓</span>
          <span class="pedals-title">{$t('landing.requirements.fullSupport.title')}</span>
        </div>
        <p class="pedals-desc">{$t('landing.requirements.fullSupport.desc')}</p>
        <div class="pedals-chips">
          <span class="pedal-chip">Garmin Rally</span>
          <span class="pedal-chip">Garmin Vector 3</span>
          <span class="pedal-chip">Favero Assioma DUO</span>
          <span class="pedal-chip">Favero Assioma DUO-Shi</span>
          <span class="pedal-chip">Favero Assioma Pro RS</span>
          <span class="pedal-chip">Favero Assioma Pro MX</span>
          <span class="pedal-chip">SRM X-Power</span>
          <span class="pedal-chip">Rotor 2INpower</span>
          <span class="pedal-chip">Look Keo Blade</span>
          <span class="pedal-chip">IQ2</span>
        </div>
      </div>
      <div class="pedals-card balance">
        <div class="pedals-card-header">
          <span class="pedals-icon">◐</span>
          <span class="pedals-title">{$t('landing.requirements.balanceOnly.title')}</span>
        </div>
        <p class="pedals-desc">{$t('landing.requirements.balanceOnly.desc')}</p>
        <div class="pedals-chips">
          <span class="pedal-chip">Wahoo POWRLINK</span>
          <span class="pedal-chip">Power2Max</span>
          <span class="pedal-chip">Quarq DZero</span>
          <span class="pedal-chip">SRAM AXS PM</span>
          <span class="pedal-chip">Stages LR</span>
          <span class="pedal-chip">4iiii Dual</span>
          <span class="pedal-chip">Sigeyi AXO</span>
          <span class="pedal-chip">FSA Powerbox</span>
        </div>
      </div>
      <div class="pedals-card incompatible">
        <div class="pedals-card-header">
          <span class="pedals-icon">✗</span>
          <span class="pedals-title">{$t('landing.requirements.notCompatible.title')}</span>
        </div>
        <p class="pedals-desc">{$t('landing.requirements.notCompatible.desc')}</p>
        <div class="pedals-chips">
          <span class="pedal-chip">{$t('landing.requirements.pedalsList.singleSided')}</span>
          <span class="pedal-chip">{$t('landing.requirements.pedalsList.trainerPower')}</span>
          <span class="pedal-chip">Vector 1/2</span>
        </div>
      </div>
    </div>
  </div>
</section>

<style>
  .requirements-section {
    padding-bottom: 24px;
  }
  .requirements-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
    margin-bottom: 32px;
  }
  .req-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 18px;
    padding: 28px 24px;
    text-align: center;
    transition: all 0.3s ease;
  }
  .req-card:hover {
    border-color: var(--border-default);
  }
  .req-icon {
    width: 44px;
    height: 44px;
    background: var(--bg-elevated);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-muted);
    margin: 0 auto 14px;
  }
  .req-icon svg {
    width: 24px;
    height: 24px;
  }
  .req-card h4 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 10px;
    letter-spacing: -0.2px;
  }
  .req-card p {
    font-size: 13px;
    color: var(--text-tertiary);
    line-height: 1.65;
  }
  .pedals-compatibility {
    margin-top: 40px;
    padding-top: 32px;
    border-top: 1px solid var(--border-subtle);
  }
  .pedals-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
  .pedals-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 20px;
    text-align: left;
  }
  .pedals-card.full {
    border-color: var(--color-optimal);
    border-width: 2px;
  }
  .pedals-card.balance {
    border-color: var(--color-attention);
  }
  .pedals-card.incompatible {
    opacity: 0.7;
  }
  .pedals-card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }
  .pedals-icon {
    font-size: 16px;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
  }
  .pedals-card.full .pedals-icon {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }
  .pedals-card.balance .pedals-icon {
    background: var(--color-attention-soft);
    color: var(--color-attention);
  }
  .pedals-card.incompatible .pedals-icon {
    background: var(--bg-elevated);
    color: var(--text-muted);
  }
  .pedals-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .pedals-desc {
    font-size: 12px;
    color: var(--text-tertiary);
    margin: 0 0 12px 0;
  }
  .pedals-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  .pedal-chip {
    font-size: 12px;
    padding: 4px 10px;
    background: var(--bg-elevated);
    border-radius: 12px;
    color: var(--text-secondary);
    white-space: nowrap;
  }
  .pedals-card.full .pedal-chip {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }
  .pedals-card.balance .pedal-chip {
    background: var(--color-attention-soft);
    color: var(--color-attention-text);
  }

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .requirements-grid { grid-template-columns: 1fr; gap: 12px; }
    .pedals-grid { grid-template-columns: 1fr; gap: 12px; }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .req-card { padding: 20px; border-radius: 14px; }
    .req-card h4 { font-size: 16px; }
    .req-card p { font-size: 13px; }
    .pedals-card { padding: 16px; }
    .pedals-title { font-size: 14px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .req-card {
      padding: 16px;
    }
    .req-icon svg { width: 20px; height: 20px; }
    .req-card h4 { font-size: 15px; }
    .req-card p { font-size: 12px; line-height: 1.5; }
    .pedals-compatibility { margin-top: 24px; padding-top: 20px; }
    .pedals-grid { gap: 10px; }
    .pedals-card { padding: 14px; border-radius: 10px; }
    .pedals-title { font-size: 13px; }
    .pedals-desc { font-size: 11px; margin-bottom: 10px; }
  }

  /* Responsive - 320px */
  @media (max-width: 320px) {
    .req-card { padding: 16px 14px; }
  }

  /* Touch devices */
  @media (hover: none) and (pointer: coarse) {
    .req-card:hover {
      transform: none;
    }
  }
</style>
