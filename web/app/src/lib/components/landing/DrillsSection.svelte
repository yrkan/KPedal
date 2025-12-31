<script lang="ts">
  import { t } from '$lib/i18n';

  let activeDrillCategory: 'focus' | 'challenge' | 'workout' = 'focus';
  let expandedDrill: string | null = null;

  $: drills = {
    focus: [
      { id: 'left', name: $t('landing.drills.items.leftFocus.name'), duration: $t('landing.drills.items.leftFocus.duration'), level: $t('landing.drills.levels.beginner'), target: $t('landing.drills.items.leftFocus.target'), desc: $t('landing.drills.items.leftFocus.desc') },
      { id: 'right', name: $t('landing.drills.items.rightFocus.name'), duration: $t('landing.drills.items.rightFocus.duration'), level: $t('landing.drills.levels.beginner'), target: $t('landing.drills.items.rightFocus.target'), desc: $t('landing.drills.items.rightFocus.desc') },
      { id: 'smooth', name: $t('landing.drills.items.smoothCircles.name'), duration: $t('landing.drills.items.smoothCircles.duration'), level: $t('landing.drills.levels.intermediate'), target: $t('landing.drills.items.smoothCircles.target'), desc: $t('landing.drills.items.smoothCircles.desc') },
      { id: 'power', name: $t('landing.drills.items.powerTransfer.name'), duration: $t('landing.drills.items.powerTransfer.duration'), level: $t('landing.drills.levels.intermediate'), target: $t('landing.drills.items.powerTransfer.target'), desc: $t('landing.drills.items.powerTransfer.desc') }
    ],
    challenge: [
      { id: 'balance-c', name: $t('landing.drills.items.balanceChallenge.name'), duration: $t('landing.drills.items.balanceChallenge.duration'), level: $t('landing.drills.levels.intermediate'), target: $t('landing.drills.items.balanceChallenge.target'), desc: $t('landing.drills.items.balanceChallenge.desc') },
      { id: 'smooth-t', name: $t('landing.drills.items.smoothnessTarget.name'), duration: $t('landing.drills.items.smoothnessTarget.duration'), level: $t('landing.drills.levels.advanced'), target: $t('landing.drills.items.smoothnessTarget.target'), desc: $t('landing.drills.items.smoothnessTarget.desc') },
      { id: 'cadence', name: $t('landing.drills.items.highCadenceSmooth.name'), duration: $t('landing.drills.items.highCadenceSmooth.duration'), level: $t('landing.drills.levels.advanced'), target: $t('landing.drills.items.highCadenceSmooth.target'), desc: $t('landing.drills.items.highCadenceSmooth.desc') }
    ],
    workout: [
      { id: 'recovery', name: $t('landing.drills.items.balanceRecovery.name'), duration: $t('landing.drills.items.balanceRecovery.duration'), level: $t('landing.drills.levels.beginner'), target: $t('landing.drills.items.balanceRecovery.target'), desc: $t('landing.drills.items.balanceRecovery.desc') },
      { id: 'builder', name: $t('landing.drills.items.efficiencyBuilder.name'), duration: $t('landing.drills.items.efficiencyBuilder.duration'), level: $t('landing.drills.levels.intermediate'), target: $t('landing.drills.items.efficiencyBuilder.target'), desc: $t('landing.drills.items.efficiencyBuilder.desc') },
      { id: 'mastery', name: $t('landing.drills.items.pedalingMastery.name'), duration: $t('landing.drills.items.pedalingMastery.duration'), level: $t('landing.drills.levels.advanced'), target: $t('landing.drills.items.pedalingMastery.target'), desc: $t('landing.drills.items.pedalingMastery.desc') }
    ]
  };

  function toggleDrill(id: string) {
    expandedDrill = expandedDrill === id ? null : id;
  }
</script>

<section id="drills" class="section drills-section" aria-labelledby="drills-title">
  <h2 id="drills-title" class="section-title">{$t('landing.drills.title')}</h2>
  <p class="section-subtitle">{$t('landing.drills.subtitle')}</p>

  <div class="drills-tabs">
    <button class="drill-tab" class:active={activeDrillCategory === 'focus'} on:click={() => activeDrillCategory = 'focus'}>
      {$t('landing.drills.tabs.focus')}
    </button>
    <button class="drill-tab" class:active={activeDrillCategory === 'challenge'} on:click={() => activeDrillCategory = 'challenge'}>
      {$t('landing.drills.tabs.challenge')}
    </button>
    <button class="drill-tab" class:active={activeDrillCategory === 'workout'} on:click={() => activeDrillCategory = 'workout'}>
      {$t('landing.drills.tabs.workout')}
    </button>
  </div>

  <div class="drills-list">
    {#each drills[activeDrillCategory] as drill}
      <button class="drill-item" class:expanded={expandedDrill === drill.id} on:click={() => toggleDrill(drill.id)}>
        <div class="drill-header">
          <div class="drill-info">
            <span class="drill-name">{drill.name}</span>
            <span class="drill-meta">
              <span class="drill-duration">{drill.duration}</span>
              <span class="drill-dot"></span>
              <span class="drill-level" class:beginner={drill.level === $t('landing.drills.levels.beginner')} class:intermediate={drill.level === $t('landing.drills.levels.intermediate')} class:advanced={drill.level === $t('landing.drills.levels.advanced')}>{drill.level}</span>
            </span>
          </div>
          <svg class="drill-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="6 9 12 15 18 9"/>
          </svg>
        </div>
        {#if expandedDrill === drill.id}
          <div class="drill-details">
            <p>{drill.desc}</p>
            <div class="drill-target">{$t('landing.drills.target')}: {drill.target}</div>
          </div>
        {/if}
      </button>
    {/each}
  </div>

  <div class="drills-highlight">
    <span class="highlight-label">{$t('landing.drills.featured')}</span>
    <h4>{$t('landing.drills.featuredTitle')}</h4>
    <p>{$t('landing.drills.featuredDesc')}</p>
  </div>
</section>

<style>
  .drills-section {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 20px;
    padding: 36px;
    margin-bottom: 96px;
  }
  .drills-tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 32px;
    background: var(--bg-base);
    padding: 4px;
    border-radius: 12px;
    width: fit-content;
  }
  .drill-tab {
    padding: 10px 24px;
    background: transparent;
    border: none;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-tertiary);
    cursor: pointer;
    transition: all 0.25s ease;
  }
  .drill-tab:hover {
    color: var(--text-secondary);
  }
  .drill-tab.active {
    background: var(--bg-surface);
    color: var(--text-primary);
    box-shadow: var(--shadow-sm);
  }
  .drills-list {
    display: flex;
    flex-direction: column;
    gap: 0;
    margin-bottom: 32px;
  }
  .drill-item {
    background: transparent;
    border: none;
    border-bottom: 1px solid var(--border-subtle);
    border-radius: 0;
    padding: 20px 16px;
    margin: 0 -16px;
    cursor: pointer;
    transition: background 0.2s ease;
    text-align: left;
    width: calc(100% + 32px);
  }
  .drill-item:first-child {
    border-top: 1px solid var(--border-subtle);
  }
  /* Hover only on desktop with mouse */
  @media (hover: hover) and (pointer: fine) {
    .drill-item:not(.expanded):hover {
      background: var(--bg-hover);
    }
  }
  .drill-item.expanded {
    background: var(--bg-hover);
    border-radius: 12px;
    border-color: transparent;
    margin-top: 8px;
    margin-bottom: 8px;
  }
  .drill-item.expanded + .drill-item {
    border-top-color: transparent;
  }
  .drill-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .drill-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .drill-name {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    letter-spacing: -0.2px;
  }
  .drill-meta {
    display: flex;
    gap: 8px;
    align-items: center;
  }
  .drill-duration {
    font-size: 13px;
    color: var(--text-muted);
  }
  .drill-dot {
    width: 3px;
    height: 3px;
    background: var(--text-faint);
    border-radius: 50%;
  }
  .drill-level {
    font-size: 12px;
    color: var(--text-muted);
  }
  .drill-level.beginner { color: var(--color-optimal-text); }
  .drill-level.intermediate { color: var(--color-attention-text); }
  .drill-level.advanced { color: var(--color-problem-text); }
  .drill-chevron {
    color: var(--text-faint);
    transition: all 0.25s ease;
    width: 16px;
    height: 16px;
  }
  .drill-item.expanded .drill-chevron {
    transform: rotate(180deg);
    color: var(--text-muted);
  }
  .drill-details {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid var(--border-subtle);
  }
  .drill-details p {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.6;
    margin-bottom: 12px;
  }
  .drill-target {
    font-size: 13px;
    color: var(--text-muted);
    font-variant-numeric: tabular-nums;
  }
  .drills-highlight {
    padding: 24px 28px;
    background: var(--bg-base);
    border-radius: 16px;
    border: 1px solid var(--border-subtle);
  }
  .highlight-label {
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--color-accent);
    font-weight: 600;
    display: block;
    margin-bottom: 10px;
  }
  .drills-highlight h4 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
    letter-spacing: -0.2px;
  }
  .drills-highlight p {
    font-size: 14px;
    color: var(--text-tertiary);
    line-height: 1.6;
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .drills-section { padding: 24px; border-radius: 16px; }
    .drills-tabs { gap: 6px; }
    .drill-tab {
      padding: 10px 18px;
      font-size: 14px;
      min-height: 44px;
    }
    .drill-item { padding: 14px 16px; }
    .drill-name { font-size: 15px; }
    .drill-meta { font-size: 12px; }
    .drills-highlight { padding: 20px; }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .drills-section {
      padding: 20px 16px;
    }
    .drills-tabs {
      flex-wrap: wrap;
      width: 100%;
    }
    .drill-tab {
      flex: 1;
      text-align: center;
      padding: 10px 12px;
      font-size: 13px;
    }
    .drills-list { gap: 6px; }
    .drill-item {
      padding: 16px 14px;
    }
    .drill-header { gap: 10px; }
    .drill-name { font-size: 14px; }
    .drill-meta { font-size: 11px; gap: 6px; }
    .drills-highlight {
      padding: 18px 16px;
    }
    .drills-highlight h4 { font-size: 15px; }
    .drills-highlight p { font-size: 12px; }
  }

  /* Even smaller */
  @media (max-width: 320px) {
    .drills-section { padding: 16px 14px; }
    .drill-tab { padding: 8px 6px; font-size: 12px; }
    .drill-name { font-size: 13px; }
  }

  /* Touch devices */
  @media (hover: none) and (pointer: coarse) {
    .drill-tab:hover,
    .drill-item:hover {
      transform: none;
    }
    .drill-tab:active { background: var(--bg-active); }
    .drill-item:active { background: var(--bg-hover); }
  }
</style>
