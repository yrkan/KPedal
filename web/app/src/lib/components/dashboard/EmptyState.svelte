<script lang="ts">
  import { t } from '$lib/i18n';
  import { getFirstName } from '$lib/utils/dashboard';

  export let userName: string | undefined;
</script>

<div class="empty-state-new animate-in">
  <!-- Welcome Section -->
  <div class="welcome-section">
    <div class="welcome-icon">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
      </svg>
    </div>
    <h2>{$t('dashboard.welcome', { values: { name: getFirstName(userName) } })}</h2>
    <p>{$t('dashboard.onboarding.dashboardWillShow')}</p>
  </div>

  <!-- Preview of what they'll see -->
  <div class="preview-section">
    <h3>{$t('dashboard.onboarding.whatYouWillSee')}</h3>
    <div class="preview-cards">
      <div class="preview-card">
        <div class="preview-icon balance-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/></svg>
        </div>
        <div class="preview-info">
          <span class="preview-label">{$t('dashboard.onboarding.powerBalance')}</span>
          <span class="preview-example">{$t('dashboard.onboarding.exampleBalance')}</span>
        </div>
        <span class="preview-target">{$t('dashboard.onboarding.proTarget')}</span>
      </div>
      <div class="preview-card">
        <div class="preview-icon te-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
        </div>
        <div class="preview-info">
          <span class="preview-label">{$t('dashboard.onboarding.torqueEffectiveness')}</span>
          <span class="preview-example">{$t('dashboard.onboarding.exampleTe')}</span>
        </div>
        <span class="preview-target">{$t('dashboard.onboarding.teOptimal')}</span>
      </div>
      <div class="preview-card">
        <div class="preview-icon ps-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v8M8 12h8"/></svg>
        </div>
        <div class="preview-info">
          <span class="preview-label">{$t('dashboard.onboarding.pedalSmoothness')}</span>
          <span class="preview-example">{$t('dashboard.onboarding.examplePs')}</span>
        </div>
        <span class="preview-target">{$t('dashboard.onboarding.psOptimal')}</span>
      </div>
    </div>
  </div>

  <!-- Setup Steps -->
  <div class="setup-section">
    <h3>{$t('dashboard.onboarding.getStarted')}</h3>
    <div class="setup-steps-new">
      <div class="setup-step-new">
        <div class="step-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        </div>
        <div class="step-content">
          <span class="step-title">{$t('dashboard.onboarding.step1Title')}</span>
          <span class="step-desc">{$t('dashboard.onboarding.step1Desc')}</span>
        </div>
        <a href="https://github.com/kpedal/kpedal/releases/latest" target="_blank" rel="noopener" class="step-action">
          {$t('dashboard.actions.downloadApk')}
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
        </a>
      </div>
      <div class="setup-step-new">
        <div class="step-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>
        </div>
        <div class="step-content">
          <span class="step-title">{$t('dashboard.onboarding.step2Title')}</span>
          <span class="step-desc">{$t('dashboard.onboarding.step2Desc')}</span>
        </div>
      </div>
      <div class="setup-step-new">
        <div class="step-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 0 1-9 9m9-9a9 9 0 0 0-9-9m9 9H3m9 9a9 9 0 0 1-9-9m9 9c-1.657 0-3-4.03-3-9s1.343-9 3-9m0 18c1.657 0 3-4.03 3-9s-1.343-9-3-9m-9 9a9 9 0 0 1 9-9"/></svg>
        </div>
        <div class="step-content">
          <span class="step-title">{$t('dashboard.onboarding.step3Title')}</span>
          <span class="step-desc">{$t('dashboard.onboarding.step3Desc')}</span>
        </div>
        <a href="/link" class="step-action">
          {$t('dashboard.actions.linkDevice')}
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
        </a>
      </div>
    </div>
  </div>

  <!-- Already have device linked? -->
  <div class="sync-hint">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
    <span>{$t('dashboard.onboarding.alreadyInstalled')}</span>
  </div>
</div>

<style>
  .empty-state-new {
    max-width: 720px;
    margin: 0 auto;
    padding: 40px 24px 60px;
  }

  .welcome-section {
    text-align: center;
    margin-bottom: 48px;
  }
  .welcome-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    color: var(--color-accent);
  }
  .welcome-section h2 {
    font-size: 26px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
    letter-spacing: -0.3px;
  }
  .welcome-section p {
    font-size: 15px;
    color: var(--text-secondary);
    max-width: 400px;
    margin: 0 auto;
    line-height: 1.5;
  }

  .preview-section {
    margin-bottom: 40px;
  }
  .preview-section h3,
  .setup-section h3 {
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-muted);
    margin-bottom: 16px;
    text-align: center;
  }
  .preview-cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
  .preview-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .preview-icon {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .preview-icon svg {
    width: 20px;
    height: 20px;
  }
  .preview-icon.balance-icon {
    background: var(--color-accent-soft);
    color: var(--color-accent);
  }
  .preview-icon.te-icon {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }
  .preview-icon.ps-icon {
    background: var(--color-attention-soft);
    color: var(--color-attention-text);
  }
  .preview-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .preview-example {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
    font-variant-numeric: tabular-nums;
  }
  .preview-target {
    font-size: 11px;
    color: var(--text-tertiary);
    padding-top: 8px;
    border-top: 1px solid var(--border-subtle);
  }

  .setup-section {
    margin-bottom: 32px;
  }
  .setup-steps-new {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .setup-step-new {
    display: flex;
    align-items: center;
    gap: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 16px 20px;
    transition: border-color 0.15s;
  }
  .setup-step-new:hover {
    border-color: var(--border-default);
  }
  .step-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    background: var(--bg-elevated);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-secondary);
    flex-shrink: 0;
  }
  .step-icon svg {
    width: 22px;
    height: 22px;
  }
  .step-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .step-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .step-desc {
    font-size: 13px;
    color: var(--text-tertiary);
  }
  .step-action {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    background: var(--color-accent);
    color: var(--color-accent-text);
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    text-decoration: none;
    transition: all 0.15s;
    flex-shrink: 0;
  }
  .step-action:hover {
    background: var(--color-accent-hover);
    transform: translateY(-1px);
  }

  .sync-hint {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 14px 20px;
    background: var(--bg-elevated);
    border-radius: 10px;
    font-size: 13px;
    color: var(--text-secondary);
  }
  .sync-hint svg {
    flex-shrink: 0;
    color: var(--text-muted);
  }

  /* Responsive - 640px */
  @media (max-width: 640px) {
    .empty-state-new {
      padding: 24px 16px 40px;
    }
    .welcome-section {
      margin-bottom: 32px;
    }
    .welcome-section h2 {
      font-size: 22px;
    }
    .preview-cards {
      grid-template-columns: 1fr;
      gap: 10px;
    }
    .preview-card {
      flex-direction: row;
      align-items: center;
      padding: 12px;
      gap: 10px;
    }
    .preview-example {
      font-size: 15px;
    }
    .preview-target {
      border-top: none;
      border-left: 1px solid var(--border-subtle);
      padding: 0 0 0 10px;
      margin-left: auto;
      font-size: 10px;
      max-width: 65px;
      text-align: right;
      flex-shrink: 0;
    }
    .preview-icon {
      width: 32px;
      height: 32px;
    }
    .preview-icon svg {
      width: 18px;
      height: 18px;
    }
    .preview-label {
      font-size: 12px;
    }
    .setup-step-new {
      flex-wrap: wrap;
      padding: 14px;
      gap: 12px;
    }
    .step-action {
      width: 100%;
      justify-content: center;
      padding: 10px 14px;
    }
    .sync-hint {
      flex-direction: column;
      text-align: center;
      gap: 6px;
    }
  }
</style>
