<script lang="ts">
  import { isAuthenticated } from '$lib/auth';
  import { theme, resolvedTheme } from '$lib/theme';
  import { t, locale, locales, localeNames, setLocale, type Locale } from '$lib/i18n';
</script>

<svelte:head>
  <title>{$t('privacy.title')} — KPedal</title>
  <meta name="description" content="KPedal Privacy Policy. Learn how we collect, use, and protect your cycling data. No location tracking, encrypted storage, your data is never sold.">
  <link rel="canonical" href="https://kpedal.com/privacy">

  <!-- Open Graph -->
  <meta property="og:type" content="website">
  <meta property="og:url" content="https://kpedal.com/privacy">
  <meta property="og:title" content="{$t('privacy.title')} — KPedal">
  <meta property="og:description" content="Learn how KPedal protects your cycling data. No location tracking, encrypted storage, full control over your data.">

  <!-- Twitter -->
  <meta name="twitter:card" content="summary">
  <meta name="twitter:title" content="{$t('privacy.title')} — KPedal">
  <meta name="twitter:description" content="Learn how KPedal protects your cycling data. No location tracking, encrypted storage, full control.">
</svelte:head>

<div class="privacy-page">
  <div class="privacy-container">
    {#if !$isAuthenticated}
      <div class="top-controls">
        <select
          class="lang-select"
          value={$locale}
          on:change={(e) => setLocale(e.currentTarget.value as Locale)}
          aria-label={$t('aria.languageSelector')}
        >
          {#each locales as loc}
            <option value={loc}>{localeNames[loc]}</option>
          {/each}
        </select>
        <button class="theme-toggle" on:click={() => theme.toggle()} aria-label={$t('common.toggleTheme')}>
          {#if $resolvedTheme === 'light'}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="5"/>
              <line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
              <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
              <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
              <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
            </svg>
          {:else}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          {/if}
          {#if $theme === 'auto'}
            <span class="auto-badge">A</span>
          {/if}
        </button>
      </div>
    {/if}

    <header class="privacy-header">
      {#if !$isAuthenticated}
        <a href="/" class="back-link">← {$t('privacy.backToKpedal')}</a>
      {/if}
      <h1>{$t('privacy.title')}</h1>
      <p class="last-updated">{$t('privacy.lastUpdated', { values: { date: 'December 27, 2025' } })}</p>
    </header>

    <!-- Quick Summary -->
    <section class="quick-summary">
      <div class="summary-grid">
        <div class="summary-item">
          <div class="summary-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
          <div class="summary-text">
            <strong>{$t('privacy.summary.encrypted')}</strong>
            <span>{$t('privacy.summary.encryptedDesc')}</span>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
            </svg>
          </div>
          <div class="summary-text">
            <strong>{$t('privacy.summary.noLocation')}</strong>
            <span>{$t('privacy.summary.noLocationDesc')}</span>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="1" x2="12" y2="23"/>
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
            </svg>
          </div>
          <div class="summary-text">
            <strong>{$t('privacy.summary.neverSold')}</strong>
            <span>{$t('privacy.summary.neverSoldDesc')}</span>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3,6 5,6 21,6"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
          </div>
          <div class="summary-text">
            <strong>{$t('privacy.summary.yourControl')}</strong>
            <span>{$t('privacy.summary.yourControlDesc')}</span>
          </div>
        </div>
      </div>
    </section>

    <div class="privacy-content">
      <!-- Table of Contents -->
      <nav class="toc">
        <h2>{$t('privacy.toc.title')}</h2>
        <ol>
          <li><a href="#what-we-collect">{$t('privacy.toc.whatWeCollect')}</a></li>
          <li><a href="#what-we-dont-collect">{$t('privacy.toc.whatWeDontCollect')}</a></li>
          <li><a href="#how-data-flows">{$t('privacy.toc.howDataFlows')}</a></li>
          <li><a href="#storage-security">{$t('privacy.toc.storageSecurity')}</a></li>
          <li><a href="#your-rights">{$t('privacy.toc.yourRights')}</a></li>
          <li><a href="#third-parties">{$t('privacy.toc.thirdParties')}</a></li>
          <li><a href="#contact">{$t('privacy.toc.contact')}</a></li>
        </ol>
      </nav>

      <!-- Section 1: What We Collect -->
      <section id="what-we-collect" class="privacy-section">
        <h2>{$t('privacy.sections.whatWeCollect')}</h2>

        <h3>{$t('privacy.content.accountData.title')}</h3>
        <p>{$t('privacy.content.accountData.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.accountData.fields.email')}</span>
            <span class="data-purpose">{$t('privacy.content.accountData.fields.emailPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.accountData.fields.name')}</span>
            <span class="data-purpose">{$t('privacy.content.accountData.fields.namePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.accountData.fields.profilePicture')}</span>
            <span class="data-purpose">{$t('privacy.content.accountData.fields.profilePicturePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.accountData.fields.googleId')}</span>
            <span class="data-purpose">{$t('privacy.content.accountData.fields.googleIdPurpose')}</span>
          </div>
        </div>
        <p class="note">{$t('privacy.content.accountData.note')}</p>

        <h3>{$t('privacy.content.pedalingMetrics.title')}</h3>
        <p>{$t('privacy.content.pedalingMetrics.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.pedalingMetrics.fields.timestamp')}</span>
            <span class="data-purpose">{$t('privacy.content.pedalingMetrics.fields.timestampPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.pedalingMetrics.fields.duration')}</span>
            <span class="data-purpose">{$t('privacy.content.pedalingMetrics.fields.durationPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.pedalingMetrics.fields.balance')}</span>
            <span class="data-purpose">{$t('privacy.content.pedalingMetrics.fields.balancePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.pedalingMetrics.fields.te')}</span>
            <span class="data-purpose">{$t('privacy.content.pedalingMetrics.fields.tePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.pedalingMetrics.fields.ps')}</span>
            <span class="data-purpose">{$t('privacy.content.pedalingMetrics.fields.psPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.pedalingMetrics.fields.zones')}</span>
            <span class="data-purpose">{$t('privacy.content.pedalingMetrics.fields.zonesPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.pedalingMetrics.fields.score')}</span>
            <span class="data-purpose">{$t('privacy.content.pedalingMetrics.fields.scorePurpose')}</span>
          </div>
        </div>

        <h3>{$t('privacy.content.performanceMetrics.title')}</h3>
        <p>{$t('privacy.content.performanceMetrics.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.performanceMetrics.fields.power')}</span>
            <span class="data-purpose">{$t('privacy.content.performanceMetrics.fields.powerPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.performanceMetrics.fields.cadence')}</span>
            <span class="data-purpose">{$t('privacy.content.performanceMetrics.fields.cadencePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.performanceMetrics.fields.hr')}</span>
            <span class="data-purpose">{$t('privacy.content.performanceMetrics.fields.hrPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.performanceMetrics.fields.speed')}</span>
            <span class="data-purpose">{$t('privacy.content.performanceMetrics.fields.speedPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.performanceMetrics.fields.distance')}</span>
            <span class="data-purpose">{$t('privacy.content.performanceMetrics.fields.distancePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.performanceMetrics.fields.elevation')}</span>
            <span class="data-purpose">{$t('privacy.content.performanceMetrics.fields.elevationPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.performanceMetrics.fields.grade')}</span>
            <span class="data-purpose">{$t('privacy.content.performanceMetrics.fields.gradePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.performanceMetrics.fields.np')}</span>
            <span class="data-purpose">{$t('privacy.content.performanceMetrics.fields.npPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.performanceMetrics.fields.energy')}</span>
            <span class="data-purpose">{$t('privacy.content.performanceMetrics.fields.energyPurpose')}</span>
          </div>
        </div>
        <p class="note">{$t('privacy.content.performanceMetrics.note')}</p>

        <h3>{$t('privacy.content.snapshots.title')}</h3>
        <p>{$t('privacy.content.snapshots.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.snapshots.fields.minuteIndex')}</span>
            <span class="data-purpose">{$t('privacy.content.snapshots.fields.minuteIndexPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.snapshots.fields.metricsAvg')}</span>
            <span class="data-purpose">{$t('privacy.content.snapshots.fields.metricsAvgPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.snapshots.fields.perfAvg')}</span>
            <span class="data-purpose">{$t('privacy.content.snapshots.fields.perfAvgPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.snapshots.fields.zoneStatus')}</span>
            <span class="data-purpose">{$t('privacy.content.snapshots.fields.zoneStatusPurpose')}</span>
          </div>
        </div>
        <p class="note">{$t('privacy.content.snapshots.note')}</p>

        <h3>{$t('privacy.content.deviceData.title')}</h3>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.deviceData.fields.deviceId')}</span>
            <span class="data-purpose">{$t('privacy.content.deviceData.fields.deviceIdPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.deviceData.fields.deviceName')}</span>
            <span class="data-purpose">{$t('privacy.content.deviceData.fields.deviceNamePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.deviceData.fields.deviceType')}</span>
            <span class="data-purpose">{$t('privacy.content.deviceData.fields.deviceTypePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.deviceData.fields.lastSync')}</span>
            <span class="data-purpose">{$t('privacy.content.deviceData.fields.lastSyncPurpose')}</span>
          </div>
        </div>

        <h3>{$t('privacy.content.userSettings.title')}</h3>
        <p>{$t('privacy.content.userSettings.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.userSettings.fields.thresholds')}</span>
            <span class="data-purpose">{$t('privacy.content.userSettings.fields.thresholdsPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.userSettings.fields.alertPrefs')}</span>
            <span class="data-purpose">{$t('privacy.content.userSettings.fields.alertPrefsPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.userSettings.fields.cooldowns')}</span>
            <span class="data-purpose">{$t('privacy.content.userSettings.fields.cooldownsPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.userSettings.fields.screenWake')}</span>
            <span class="data-purpose">{$t('privacy.content.userSettings.fields.screenWakePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.userSettings.fields.bgMode')}</span>
            <span class="data-purpose">{$t('privacy.content.userSettings.fields.bgModePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.userSettings.fields.autoSync')}</span>
            <span class="data-purpose">{$t('privacy.content.userSettings.fields.autoSyncPurpose')}</span>
          </div>
        </div>
        <p class="note">{$t('privacy.content.userSettings.note')}</p>

        <h3>{$t('privacy.content.drillResults.title')}</h3>
        <p>{$t('privacy.content.drillResults.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.drillResults.fields.drillId')}</span>
            <span class="data-purpose">{$t('privacy.content.drillResults.fields.drillIdPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.drillResults.fields.drillTimestamp')}</span>
            <span class="data-purpose">{$t('privacy.content.drillResults.fields.drillTimestampPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.drillResults.fields.drillScore')}</span>
            <span class="data-purpose">{$t('privacy.content.drillResults.fields.drillScorePurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.drillResults.fields.timeInTarget')}</span>
            <span class="data-purpose">{$t('privacy.content.drillResults.fields.timeInTargetPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.drillResults.fields.phaseScores')}</span>
            <span class="data-purpose">{$t('privacy.content.drillResults.fields.phaseScoresPurpose')}</span>
          </div>
        </div>

        <h3>{$t('privacy.content.achievements.title')}</h3>
        <p>{$t('privacy.content.achievements.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.achievements.fields.achievementId')}</span>
            <span class="data-purpose">{$t('privacy.content.achievements.fields.achievementIdPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.achievements.fields.unlockedAt')}</span>
            <span class="data-purpose">{$t('privacy.content.achievements.fields.unlockedAtPurpose')}</span>
          </div>
        </div>

        <h3>{$t('privacy.content.localOnly.title')}</h3>
        <p>{@html $t('privacy.content.localOnly.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.localOnly.fields.ratings')}</span>
            <span class="data-purpose">{$t('privacy.content.localOnly.fields.ratingsPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.localOnly.fields.notes')}</span>
            <span class="data-purpose">{$t('privacy.content.localOnly.fields.notesPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.localOnly.fields.customDrills')}</span>
            <span class="data-purpose">{$t('privacy.content.localOnly.fields.customDrillsPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.localOnly.fields.onboarding')}</span>
            <span class="data-purpose">{$t('privacy.content.localOnly.fields.onboardingPurpose')}</span>
          </div>
        </div>
        <p class="note">{$t('privacy.content.localOnly.note')}</p>
      </section>

      <!-- Section 2: What We Don't Collect -->
      <section id="what-we-dont-collect" class="privacy-section">
        <h2>{$t('privacy.sections.whatWeDontCollect')}</h2>
        <p>{$t('privacy.content.dontCollect.intro')}</p>

        <div class="dont-collect-grid">
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>{$t('privacy.content.dontCollect.items.gps')}</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>{$t('privacy.content.dontCollect.items.route')}</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>{$t('privacy.content.dontCollect.items.temperature')}</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>{$t('privacy.content.dontCollect.items.strava')}</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>{$t('privacy.content.dontCollect.items.rideTitles')}</span>
          </div>
          <div class="dont-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>{$t('privacy.content.dontCollect.items.photos')}</span>
          </div>
        </div>
        <p class="note">{$t('privacy.content.dontCollect.note')}</p>

        <h3>{$t('privacy.content.usedNotStored.title')}</h3>
        <p>{@html $t('privacy.content.usedNotStored.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.usedNotStored.fields.smoothed')}</span>
            <span class="data-purpose">{$t('privacy.content.usedNotStored.fields.smoothedPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.usedNotStored.fields.rawStream')}</span>
            <span class="data-purpose">{$t('privacy.content.usedNotStored.fields.rawStreamPurpose')}</span>
          </div>
        </div>

        <h3>{$t('privacy.content.permissions.title')}</h3>
        <p>{$t('privacy.content.permissions.intro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.permissions.fields.foreground')}</span>
            <span class="data-purpose">{$t('privacy.content.permissions.fields.foregroundPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.permissions.fields.notifications')}</span>
            <span class="data-purpose">{$t('privacy.content.permissions.fields.notificationsPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field">{$t('privacy.content.permissions.fields.boot')}</span>
            <span class="data-purpose">{$t('privacy.content.permissions.fields.bootPurpose')}</span>
          </div>
        </div>
        <p class="note">{$t('privacy.content.permissions.note')}</p>
      </section>

      <!-- Section 3: How Data Flows -->
      <section id="how-data-flows" class="privacy-section">
        <h2>{$t('privacy.sections.howDataFlows')}</h2>

        <div class="flow-diagram">
          <div class="flow-step">
            <div class="flow-number">1</div>
            <div class="flow-content">
              <h4>{$t('privacy.content.dataFlow.step1.title')}</h4>
              <p>{$t('privacy.content.dataFlow.step1.desc')}</p>
            </div>
          </div>
          <div class="flow-arrow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <polyline points="19,12 12,19 5,12"/>
            </svg>
          </div>
          <div class="flow-step">
            <div class="flow-number">2</div>
            <div class="flow-content">
              <h4>{$t('privacy.content.dataFlow.step2.title')}</h4>
              <p>{$t('privacy.content.dataFlow.step2.desc')}</p>
            </div>
          </div>
          <div class="flow-arrow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <polyline points="19,12 12,19 5,12"/>
            </svg>
          </div>
          <div class="flow-step">
            <div class="flow-number">3</div>
            <div class="flow-content">
              <h4>{$t('privacy.content.dataFlow.step3.title')}</h4>
              <p>{$t('privacy.content.dataFlow.step3.desc')}</p>
            </div>
          </div>
          <div class="flow-arrow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <polyline points="19,12 12,19 5,12"/>
            </svg>
          </div>
          <div class="flow-step">
            <div class="flow-number">4</div>
            <div class="flow-content">
              <h4>{$t('privacy.content.dataFlow.step4.title')}</h4>
              <p>{$t('privacy.content.dataFlow.step4.desc')}</p>
            </div>
          </div>
        </div>

        <h3>{$t('privacy.content.dataFlow.offlineTitle')}</h3>
        <p>{$t('privacy.content.dataFlow.offlineDesc')}</p>
      </section>

      <!-- Section 4: Storage & Security -->
      <section id="storage-security" class="privacy-section">
        <h2>{$t('privacy.sections.storageSecurity')}</h2>

        <h3>{$t('privacy.content.infrastructure.title')}</h3>
        <div class="infra-table">
          <div class="infra-row">
            <span class="infra-service">{$t('privacy.content.infrastructure.workers')}</span>
            <span class="infra-use">{$t('privacy.content.infrastructure.workersUse')}</span>
          </div>
          <div class="infra-row">
            <span class="infra-service">{$t('privacy.content.infrastructure.d1')}</span>
            <span class="infra-use">{$t('privacy.content.infrastructure.d1Use')}</span>
          </div>
          <div class="infra-row">
            <span class="infra-service">{$t('privacy.content.infrastructure.kv')}</span>
            <span class="infra-use">{$t('privacy.content.infrastructure.kvUse')}</span>
          </div>
          <div class="infra-row">
            <span class="infra-service">{$t('privacy.content.infrastructure.pages')}</span>
            <span class="infra-use">{$t('privacy.content.infrastructure.pagesUse')}</span>
          </div>
        </div>

        <h3>{$t('privacy.content.security.title')}</h3>
        <div class="security-list">
          <div class="security-item">
            <strong>{$t('privacy.content.security.https')}</strong>
            <p>{$t('privacy.content.security.httpsDesc')}</p>
          </div>
          <div class="security-item">
            <strong>{$t('privacy.content.security.jwt')}</strong>
            <p>{$t('privacy.content.security.jwtDesc')}</p>
          </div>
          <div class="security-item">
            <strong>{$t('privacy.content.security.rateLimit')}</strong>
            <p>{$t('privacy.content.security.rateLimitDesc')}</p>
          </div>
          <div class="security-item">
            <strong>{$t('privacy.content.security.validation')}</strong>
            <p>{$t('privacy.content.security.validationDesc')}</p>
          </div>
          <div class="security-item">
            <strong>{$t('privacy.content.security.csrf')}</strong>
            <p>{$t('privacy.content.security.csrfDesc')}</p>
          </div>
        </div>

        <h3>{$t('privacy.content.retention.title')}</h3>
        <h4>{$t('privacy.content.retention.cloudTitle')}</h4>
        <div class="retention-table">
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.cloud.rideData')}</span>
            <span class="retention-period">{$t('privacy.content.retention.cloud.rideDataPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.cloud.drillResults')}</span>
            <span class="retention-period">{$t('privacy.content.retention.cloud.drillResultsPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.cloud.achievements')}</span>
            <span class="retention-period">{$t('privacy.content.retention.cloud.achievementsPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.cloud.userSettings')}</span>
            <span class="retention-period">{$t('privacy.content.retention.cloud.userSettingsPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.cloud.accountData')}</span>
            <span class="retention-period">{$t('privacy.content.retention.cloud.accountDataPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.cloud.accessTokens')}</span>
            <span class="retention-period">{$t('privacy.content.retention.cloud.accessTokensPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.cloud.refreshTokens')}</span>
            <span class="retention-period">{$t('privacy.content.retention.cloud.refreshTokensPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.cloud.oauthState')}</span>
            <span class="retention-period">{$t('privacy.content.retention.cloud.oauthStatePeriod')}</span>
          </div>
        </div>

        <h4>{$t('privacy.content.retention.localTitle')}</h4>
        <div class="retention-table">
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.local.rideHistory')}</span>
            <span class="retention-period">{$t('privacy.content.retention.local.rideHistoryPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.local.drillResults')}</span>
            <span class="retention-period">{$t('privacy.content.retention.local.drillResultsPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.local.achievements')}</span>
            <span class="retention-period">{$t('privacy.content.retention.local.achievementsPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.local.customDrills')}</span>
            <span class="retention-period">{$t('privacy.content.retention.local.customDrillsPeriod')}</span>
          </div>
          <div class="retention-row">
            <span class="retention-data">{$t('privacy.content.retention.local.settings')}</span>
            <span class="retention-period">{$t('privacy.content.retention.local.settingsPeriod')}</span>
          </div>
        </div>
      </section>

      <!-- Section 5: Your Rights -->
      <section id="your-rights" class="privacy-section">
        <h2>{$t('privacy.sections.yourRights')}</h2>

        <div class="rights-grid">
          <div class="right-card">
            <h4>{$t('privacy.rights.access')}</h4>
            <p>{$t('privacy.rights.accessDesc')}</p>
          </div>
          <div class="right-card">
            <h4>{$t('privacy.rights.delete')}</h4>
            <p>{$t('privacy.rights.deleteDesc')}</p>
          </div>
          <div class="right-card">
            <h4>{$t('privacy.rights.export')}</h4>
            <p>{$t('privacy.rights.exportDesc')}</p>
          </div>
          <div class="right-card">
            <h4>{$t('privacy.rights.optOut')}</h4>
            <p>{$t('privacy.rights.optOutDesc')}</p>
          </div>
        </div>
      </section>

      <!-- Section 6: Third Parties -->
      <section id="third-parties" class="privacy-section">
        <h2>{$t('privacy.sections.thirdParties')}</h2>

        <div class="third-party-list">
          <div class="third-party-item">
            <h4>{$t('privacy.content.thirdParties.google.title')}</h4>
            <p>{$t('privacy.content.thirdParties.google.desc')}</p>
            <a href="https://policies.google.com/privacy" target="_blank" rel="noopener noreferrer">{$t('privacy.content.thirdParties.google.link')}</a>
          </div>
          <div class="third-party-item">
            <h4>{$t('privacy.content.thirdParties.cloudflare.title')}</h4>
            <p>{$t('privacy.content.thirdParties.cloudflare.desc')}</p>
            <a href="https://www.cloudflare.com/privacypolicy/" target="_blank" rel="noopener noreferrer">{$t('privacy.content.thirdParties.cloudflare.link')}</a>
          </div>
        </div>

        <h3>{$t('privacy.content.thirdParties.notUsedTitle')}</h3>
        <p>{$t('privacy.content.thirdParties.notUsedIntro')}</p>
        <ul>
          {#each $t('privacy.content.thirdParties.notUsed') as item}
            <li>{item}</li>
          {/each}
        </ul>

        <h3>{$t('privacy.content.thirdParties.browserStorageTitle')}</h3>
        <p>{$t('privacy.content.thirdParties.browserStorageIntro')}</p>
        <div class="data-table">
          <div class="data-row">
            <span class="data-field"><code>{$t('privacy.content.thirdParties.browserStorage.access')}</code></span>
            <span class="data-purpose">{$t('privacy.content.thirdParties.browserStorage.accessPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field"><code>{$t('privacy.content.thirdParties.browserStorage.refresh')}</code></span>
            <span class="data-purpose">{$t('privacy.content.thirdParties.browserStorage.refreshPurpose')}</span>
          </div>
          <div class="data-row">
            <span class="data-field"><code>{$t('privacy.content.thirdParties.browserStorage.theme')}</code></span>
            <span class="data-purpose">{$t('privacy.content.thirdParties.browserStorage.themePurpose')}</span>
          </div>
        </div>
        <p class="note">{$t('privacy.content.thirdParties.browserStorageNote')}</p>
      </section>

      <!-- Section 7: Contact -->
      <section id="contact" class="privacy-section">
        <h2>{$t('privacy.sections.contact')}</h2>
        <p>{$t('privacy.contact.text')}</p>
        <div class="contact-info">
          <a href="mailto:privacy@kpedal.com" class="contact-link">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
              <polyline points="22,6 12,13 2,6"/>
            </svg>
            {$t('privacy.contact.email')}
          </a>
          <a href="https://github.com/yrkan/kpedal/issues" target="_blank" rel="noopener noreferrer" class="contact-link">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/>
            </svg>
            {$t('privacy.contact.github')}
          </a>
        </div>
      </section>

      <!-- Policy Changes -->
      <section class="privacy-section">
        <h2>{$t('privacy.sections.policyChanges')}</h2>
        <p>{$t('privacy.policyChanges')}</p>
      </section>
    </div>

    <footer class="privacy-footer">
      <a href="/" class="footer-logo">
        <span class="logo-dot"></span>
        <span>KPedal</span>
      </a>
      <p>{$t('privacy.footer')}</p>
    </footer>
  </div>
</div>

<style>
  .privacy-page {
    min-height: 100vh;
    padding: 24px;
    background: var(--bg-base);
  }

  .privacy-container {
    max-width: 720px;
    margin: 0 auto;
    position: relative;
  }

  .top-controls {
    position: fixed;
    top: 24px;
    right: 24px;
    display: flex;
    align-items: center;
    gap: 8px;
    z-index: 100;
  }

  .lang-select {
    height: 36px;
    padding: 0 10px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.15s ease;
    -webkit-appearance: none;
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpolyline points='6,9 12,15 18,9'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 8px center;
    padding-right: 26px;
  }

  .lang-select:hover {
    background-color: var(--bg-hover);
    color: var(--text-primary);
  }

  .lang-select:focus {
    outline: none;
    border-color: var(--border-default);
  }

  .lang-select option {
    background: var(--bg-surface);
    color: var(--text-primary);
  }

  .theme-toggle {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s ease;
    position: relative;
  }

  .auto-badge {
    position: absolute;
    bottom: -4px;
    right: -4px;
    width: 14px;
    height: 14px;
    background: #22c55e;
    color: #fff;
    font-size: 9px;
    font-weight: 900;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    box-shadow: 0 1px 3px rgba(0,0,0,0.4);
    border: 1.5px solid var(--bg-base);
  }

  .theme-toggle:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  /* Header */
  .privacy-header {
    padding: 40px 0 32px;
    border-bottom: 1px solid var(--border-subtle);
    margin-bottom: 32px;
  }

  .back-link {
    font-size: 13px;
    color: var(--text-muted);
    text-decoration: none;
    white-space: nowrap;
    display: block;
    margin-bottom: 20px;
    background: transparent;
    -webkit-tap-highlight-color: transparent;
  }

  .back-link:hover,
  .back-link:focus,
  .back-link:active {
    text-decoration: underline;
    background: transparent;
    outline: none;
  }

  .privacy-header h1 {
    font-size: 36px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 8px;
    letter-spacing: -0.5px;
  }

  .last-updated {
    font-size: 14px;
    color: var(--text-tertiary);
  }

  /* Quick Summary */
  .quick-summary {
    margin-bottom: 40px;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .summary-item {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
  }

  .summary-icon {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 10px;
    color: var(--text-secondary);
    flex-shrink: 0;
  }

  .summary-text {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .summary-text strong {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .summary-text span {
    font-size: 13px;
    color: var(--text-tertiary);
  }

  /* Table of Contents */
  .toc {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
    padding: 20px 24px;
    margin-bottom: 40px;
  }

  .toc h2 {
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-tertiary);
    margin-bottom: 12px;
  }

  .toc ol {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px 24px;
    list-style: none;
    padding: 0;
    margin: 0;
    counter-reset: toc;
  }

  .toc li {
    counter-increment: toc;
  }

  .toc a {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: var(--text-secondary);
    transition: color 0.15s ease;
  }

  .toc a::before {
    content: counter(toc) ".";
    color: var(--text-muted);
    font-size: 13px;
  }

  .toc a:hover {
    color: var(--text-primary);
  }

  /* Sections */
  .privacy-section {
    margin-bottom: 48px;
  }

  .privacy-section h2 {
    font-size: 22px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--border-subtle);
  }

  .privacy-section h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 24px 0 12px;
  }

  .privacy-section h4 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  .privacy-section p {
    color: var(--text-secondary);
    line-height: 1.7;
    margin-bottom: 12px;
  }

  .privacy-section ul {
    color: var(--text-secondary);
    line-height: 1.7;
    margin: 12px 0;
    padding-left: 20px;
  }

  .privacy-section li {
    margin-bottom: 6px;
  }

  .privacy-section a {
    color: var(--color-accent);
  }

  .privacy-section a:hover {
    text-decoration: underline;
  }

  .note {
    font-size: 13px;
    color: var(--text-tertiary);
    font-style: italic;
  }

  /* Data Tables */
  .data-table, .infra-table, .retention-table {
    display: flex;
    flex-direction: column;
    gap: 1px;
    background: var(--border-subtle);
    border-radius: 8px;
    overflow: hidden;
    margin: 12px 0;
  }

  .data-row, .infra-row, .retention-row {
    display: grid;
    grid-template-columns: 180px 1fr;
    background: var(--bg-surface);
    padding: 12px 16px;
  }

  .data-field, .infra-service, .retention-data {
    font-weight: 500;
    color: var(--text-primary);
    font-size: 14px;
  }

  .data-purpose, .infra-use, .retention-period {
    color: var(--text-tertiary);
    font-size: 14px;
  }

  /* Don't Collect Grid */
  .dont-collect-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin-top: 16px;
  }

  .dont-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    font-size: 14px;
    color: var(--text-secondary);
  }

  .dont-item svg {
    color: var(--text-muted);
    flex-shrink: 0;
  }

  /* Flow Diagram */
  .flow-diagram {
    display: flex;
    flex-direction: column;
    gap: 0;
    margin: 20px 0;
  }

  .flow-step {
    display: flex;
    gap: 16px;
    padding: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
  }

  .flow-number {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 50%;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-secondary);
    flex-shrink: 0;
  }

  .flow-content h4 {
    margin-bottom: 4px;
  }

  .flow-content p {
    margin: 0;
    font-size: 14px;
  }

  .flow-arrow {
    display: flex;
    justify-content: center;
    padding: 8px 0;
    color: var(--text-muted);
  }

  /* Security List */
  .security-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin: 12px 0;
  }

  .security-item {
    padding: 14px 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
  }

  .security-item strong {
    display: block;
    font-size: 14px;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .security-item p {
    margin: 0;
    font-size: 13px;
    color: var(--text-tertiary);
  }

  /* Rights Grid */
  .rights-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .right-card {
    padding: 20px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 12px;
  }

  .right-card h4 {
    margin-bottom: 8px;
  }

  .right-card p {
    margin: 0;
    font-size: 14px;
  }

  /* Third Party */
  .third-party-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin: 12px 0;
  }

  .third-party-item {
    padding: 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
  }

  .third-party-item h4 {
    margin-bottom: 6px;
  }

  .third-party-item p {
    margin-bottom: 8px;
    font-size: 14px;
  }

  .third-party-item a {
    font-size: 13px;
  }

  /* Contact */
  .contact-info {
    display: flex;
    gap: 16px;
    margin-top: 16px;
  }

  .contact-link {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 12px 20px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    font-size: 14px;
    color: var(--text-primary);
    transition: all 0.15s ease;
  }

  .contact-link:hover {
    background: var(--bg-hover);
    border-color: var(--border-default);
    text-decoration: none;
  }

  .contact-link svg {
    color: var(--text-tertiary);
  }

  /* Code */
  code {
    background: var(--bg-elevated);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'SF Mono', Monaco, monospace;
  }

  /* Footer */
  .privacy-footer {
    text-align: center;
    padding-top: 40px;
    border-top: 1px solid var(--border-subtle);
    margin-top: 40px;
  }

  .footer-logo {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .logo-dot {
    width: 8px;
    height: 8px;
    background: var(--color-optimal);
    border-radius: 50%;
  }

  .privacy-footer p {
    font-size: 13px;
    color: var(--text-muted);
  }

  /* Responsive */
  @media (max-width: 640px) {
    .privacy-page {
      padding: 16px;
    }

    .top-controls {
      top: 16px;
      right: 16px;
      gap: 6px;
    }

    .lang-select {
      height: 34px;
      font-size: 12px;
      padding: 0 8px;
      padding-right: 24px;
    }

    .theme-toggle {
      width: 34px;
      height: 34px;
    }

    .privacy-header {
      padding: 24px 0 24px;
    }

    .back-link svg {
      width: 16px;
      height: 16px;
    }

    .summary-grid {
      grid-template-columns: 1fr;
    }

    .toc ol {
      grid-template-columns: 1fr;
    }

    .dont-collect-grid {
      grid-template-columns: 1fr;
    }

    .rights-grid {
      grid-template-columns: 1fr;
    }

    .data-row, .infra-row, .retention-row {
      grid-template-columns: 1fr;
      gap: 4px;
    }

    .contact-info {
      flex-direction: column;
    }

    .privacy-header h1 {
      font-size: 28px;
    }
  }
</style>
