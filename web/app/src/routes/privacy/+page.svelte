<script lang="ts">
  import { isAuthenticated } from '$lib/auth';
  import { t } from '$lib/i18n';
  import Footer from '$lib/components/Footer.svelte';
  import GuestLogo from '$lib/components/GuestLogo.svelte';
  import GuestControls from '$lib/components/GuestControls.svelte';
  import {
    DataTable, SummaryGrid, FlowDiagram, DontCollectGrid,
    SecurityList, RightsGrid, ThirdPartyList, TOC, ContactInfo,
    type SummaryItem, type FlowStep, type SecurityItem, type RightCard, type ThirdParty, type TocItem
  } from '$lib/components/privacy';

  const summaryItems: SummaryItem[] = [
    { icon: 'shield', titleKey: 'privacy.summary.encrypted', descKey: 'privacy.summary.encryptedDesc' },
    { icon: 'no-location', titleKey: 'privacy.summary.noLocation', descKey: 'privacy.summary.noLocationDesc' },
    { icon: 'no-money', titleKey: 'privacy.summary.neverSold', descKey: 'privacy.summary.neverSoldDesc' },
    { icon: 'delete', titleKey: 'privacy.summary.yourControl', descKey: 'privacy.summary.yourControlDesc' }
  ];

  const tocItems: TocItem[] = [
    { id: 'what-we-collect', labelKey: 'privacy.toc.whatWeCollect' },
    { id: 'what-we-dont-collect', labelKey: 'privacy.toc.whatWeDontCollect' },
    { id: 'how-data-flows', labelKey: 'privacy.toc.howDataFlows' },
    { id: 'storage-security', labelKey: 'privacy.toc.storageSecurity' },
    { id: 'your-rights', labelKey: 'privacy.toc.yourRights' },
    { id: 'third-parties', labelKey: 'privacy.toc.thirdParties' },
    { id: 'contact', labelKey: 'privacy.toc.contact' }
  ];

  const flowSteps: FlowStep[] = [
    { number: 1, titleKey: 'privacy.content.dataFlow.step1.title', descKey: 'privacy.content.dataFlow.step1.desc' },
    { number: 2, titleKey: 'privacy.content.dataFlow.step2.title', descKey: 'privacy.content.dataFlow.step2.desc' },
    { number: 3, titleKey: 'privacy.content.dataFlow.step3.title', descKey: 'privacy.content.dataFlow.step3.desc' },
    { number: 4, titleKey: 'privacy.content.dataFlow.step4.title', descKey: 'privacy.content.dataFlow.step4.desc' }
  ];

  const securityItems: SecurityItem[] = [
    { titleKey: 'privacy.content.security.https', descKey: 'privacy.content.security.httpsDesc' },
    { titleKey: 'privacy.content.security.jwt', descKey: 'privacy.content.security.jwtDesc' },
    { titleKey: 'privacy.content.security.rateLimit', descKey: 'privacy.content.security.rateLimitDesc' },
    { titleKey: 'privacy.content.security.validation', descKey: 'privacy.content.security.validationDesc' },
    { titleKey: 'privacy.content.security.csrf', descKey: 'privacy.content.security.csrfDesc' }
  ];

  const rightsItems: RightCard[] = [
    { titleKey: 'privacy.rights.access', descKey: 'privacy.rights.accessDesc' },
    { titleKey: 'privacy.rights.delete', descKey: 'privacy.rights.deleteDesc' },
    { titleKey: 'privacy.rights.export', descKey: 'privacy.rights.exportDesc' },
    { titleKey: 'privacy.rights.optOut', descKey: 'privacy.rights.optOutDesc' }
  ];

  const thirdParties: ThirdParty[] = [
    { titleKey: 'privacy.content.thirdParties.google.title', descKey: 'privacy.content.thirdParties.google.desc', linkText: 'privacy.content.thirdParties.google.link', linkUrl: 'https://policies.google.com/privacy' },
    { titleKey: 'privacy.content.thirdParties.cloudflare.title', descKey: 'privacy.content.thirdParties.cloudflare.desc', linkText: 'privacy.content.thirdParties.cloudflare.link', linkUrl: 'https://www.cloudflare.com/privacypolicy/' }
  ];

  const dontCollectItems = [
    'privacy.content.dontCollect.items.gps',
    'privacy.content.dontCollect.items.route',
    'privacy.content.dontCollect.items.temperature',
    'privacy.content.dontCollect.items.strava',
    'privacy.content.dontCollect.items.rideTitles',
    'privacy.content.dontCollect.items.photos'
  ];

  // Data table row helpers
  $: accountDataRows = [
    { field: $t('privacy.content.accountData.fields.email'), purpose: $t('privacy.content.accountData.fields.emailPurpose') },
    { field: $t('privacy.content.accountData.fields.name'), purpose: $t('privacy.content.accountData.fields.namePurpose') },
    { field: $t('privacy.content.accountData.fields.profilePicture'), purpose: $t('privacy.content.accountData.fields.profilePicturePurpose') },
    { field: $t('privacy.content.accountData.fields.googleId'), purpose: $t('privacy.content.accountData.fields.googleIdPurpose') }
  ];

  $: pedalingMetricsRows = [
    { field: $t('privacy.content.pedalingMetrics.fields.timestamp'), purpose: $t('privacy.content.pedalingMetrics.fields.timestampPurpose') },
    { field: $t('privacy.content.pedalingMetrics.fields.duration'), purpose: $t('privacy.content.pedalingMetrics.fields.durationPurpose') },
    { field: $t('privacy.content.pedalingMetrics.fields.balance'), purpose: $t('privacy.content.pedalingMetrics.fields.balancePurpose') },
    { field: $t('privacy.content.pedalingMetrics.fields.te'), purpose: $t('privacy.content.pedalingMetrics.fields.tePurpose') },
    { field: $t('privacy.content.pedalingMetrics.fields.ps'), purpose: $t('privacy.content.pedalingMetrics.fields.psPurpose') },
    { field: $t('privacy.content.pedalingMetrics.fields.zones'), purpose: $t('privacy.content.pedalingMetrics.fields.zonesPurpose') },
    { field: $t('privacy.content.pedalingMetrics.fields.score'), purpose: $t('privacy.content.pedalingMetrics.fields.scorePurpose') }
  ];

  $: performanceMetricsRows = [
    { field: $t('privacy.content.performanceMetrics.fields.power'), purpose: $t('privacy.content.performanceMetrics.fields.powerPurpose') },
    { field: $t('privacy.content.performanceMetrics.fields.cadence'), purpose: $t('privacy.content.performanceMetrics.fields.cadencePurpose') },
    { field: $t('privacy.content.performanceMetrics.fields.hr'), purpose: $t('privacy.content.performanceMetrics.fields.hrPurpose') },
    { field: $t('privacy.content.performanceMetrics.fields.speed'), purpose: $t('privacy.content.performanceMetrics.fields.speedPurpose') },
    { field: $t('privacy.content.performanceMetrics.fields.distance'), purpose: $t('privacy.content.performanceMetrics.fields.distancePurpose') },
    { field: $t('privacy.content.performanceMetrics.fields.elevation'), purpose: $t('privacy.content.performanceMetrics.fields.elevationPurpose') },
    { field: $t('privacy.content.performanceMetrics.fields.grade'), purpose: $t('privacy.content.performanceMetrics.fields.gradePurpose') },
    { field: $t('privacy.content.performanceMetrics.fields.np'), purpose: $t('privacy.content.performanceMetrics.fields.npPurpose') },
    { field: $t('privacy.content.performanceMetrics.fields.energy'), purpose: $t('privacy.content.performanceMetrics.fields.energyPurpose') }
  ];

  $: snapshotsRows = [
    { field: $t('privacy.content.snapshots.fields.minuteIndex'), purpose: $t('privacy.content.snapshots.fields.minuteIndexPurpose') },
    { field: $t('privacy.content.snapshots.fields.metricsAvg'), purpose: $t('privacy.content.snapshots.fields.metricsAvgPurpose') },
    { field: $t('privacy.content.snapshots.fields.perfAvg'), purpose: $t('privacy.content.snapshots.fields.perfAvgPurpose') },
    { field: $t('privacy.content.snapshots.fields.zoneStatus'), purpose: $t('privacy.content.snapshots.fields.zoneStatusPurpose') }
  ];

  $: deviceDataRows = [
    { field: $t('privacy.content.deviceData.fields.deviceId'), purpose: $t('privacy.content.deviceData.fields.deviceIdPurpose') },
    { field: $t('privacy.content.deviceData.fields.deviceName'), purpose: $t('privacy.content.deviceData.fields.deviceNamePurpose') },
    { field: $t('privacy.content.deviceData.fields.deviceType'), purpose: $t('privacy.content.deviceData.fields.deviceTypePurpose') },
    { field: $t('privacy.content.deviceData.fields.lastSync'), purpose: $t('privacy.content.deviceData.fields.lastSyncPurpose') }
  ];

  $: userSettingsRows = [
    { field: $t('privacy.content.userSettings.fields.thresholds'), purpose: $t('privacy.content.userSettings.fields.thresholdsPurpose') },
    { field: $t('privacy.content.userSettings.fields.alertPrefs'), purpose: $t('privacy.content.userSettings.fields.alertPrefsPurpose') },
    { field: $t('privacy.content.userSettings.fields.cooldowns'), purpose: $t('privacy.content.userSettings.fields.cooldownsPurpose') },
    { field: $t('privacy.content.userSettings.fields.screenWake'), purpose: $t('privacy.content.userSettings.fields.screenWakePurpose') },
    { field: $t('privacy.content.userSettings.fields.bgMode'), purpose: $t('privacy.content.userSettings.fields.bgModePurpose') },
    { field: $t('privacy.content.userSettings.fields.autoSync'), purpose: $t('privacy.content.userSettings.fields.autoSyncPurpose') }
  ];

  $: drillResultsRows = [
    { field: $t('privacy.content.drillResults.fields.drillId'), purpose: $t('privacy.content.drillResults.fields.drillIdPurpose') },
    { field: $t('privacy.content.drillResults.fields.drillTimestamp'), purpose: $t('privacy.content.drillResults.fields.drillTimestampPurpose') },
    { field: $t('privacy.content.drillResults.fields.drillScore'), purpose: $t('privacy.content.drillResults.fields.drillScorePurpose') },
    { field: $t('privacy.content.drillResults.fields.timeInTarget'), purpose: $t('privacy.content.drillResults.fields.timeInTargetPurpose') },
    { field: $t('privacy.content.drillResults.fields.phaseScores'), purpose: $t('privacy.content.drillResults.fields.phaseScoresPurpose') }
  ];

  $: achievementsRows = [
    { field: $t('privacy.content.achievements.fields.achievementId'), purpose: $t('privacy.content.achievements.fields.achievementIdPurpose') },
    { field: $t('privacy.content.achievements.fields.unlockedAt'), purpose: $t('privacy.content.achievements.fields.unlockedAtPurpose') }
  ];

  $: localOnlyRows = [
    { field: $t('privacy.content.localOnly.fields.ratings'), purpose: $t('privacy.content.localOnly.fields.ratingsPurpose') },
    { field: $t('privacy.content.localOnly.fields.notes'), purpose: $t('privacy.content.localOnly.fields.notesPurpose') },
    { field: $t('privacy.content.localOnly.fields.customDrills'), purpose: $t('privacy.content.localOnly.fields.customDrillsPurpose') },
    { field: $t('privacy.content.localOnly.fields.onboarding'), purpose: $t('privacy.content.localOnly.fields.onboardingPurpose') },
    { field: $t('privacy.content.localOnly.fields.checkpoint'), purpose: $t('privacy.content.localOnly.fields.checkpointPurpose') }
  ];

  $: usedNotStoredRows = [
    { field: $t('privacy.content.usedNotStored.fields.smoothed'), purpose: $t('privacy.content.usedNotStored.fields.smoothedPurpose') },
    { field: $t('privacy.content.usedNotStored.fields.rawStream'), purpose: $t('privacy.content.usedNotStored.fields.rawStreamPurpose') }
  ];

  $: permissionsRows = [
    { field: $t('privacy.content.permissions.fields.foreground'), purpose: $t('privacy.content.permissions.fields.foregroundPurpose') },
    { field: $t('privacy.content.permissions.fields.notifications'), purpose: $t('privacy.content.permissions.fields.notificationsPurpose') },
    { field: $t('privacy.content.permissions.fields.boot'), purpose: $t('privacy.content.permissions.fields.bootPurpose') }
  ];

  $: infraRows = [
    { field: $t('privacy.content.infrastructure.workers'), purpose: $t('privacy.content.infrastructure.workersUse') },
    { field: $t('privacy.content.infrastructure.d1'), purpose: $t('privacy.content.infrastructure.d1Use') },
    { field: $t('privacy.content.infrastructure.kv'), purpose: $t('privacy.content.infrastructure.kvUse') },
    { field: $t('privacy.content.infrastructure.pages'), purpose: $t('privacy.content.infrastructure.pagesUse') }
  ];

  $: cloudRetentionRows = [
    { field: $t('privacy.content.retention.cloud.rideData'), purpose: $t('privacy.content.retention.cloud.rideDataPeriod') },
    { field: $t('privacy.content.retention.cloud.drillResults'), purpose: $t('privacy.content.retention.cloud.drillResultsPeriod') },
    { field: $t('privacy.content.retention.cloud.achievements'), purpose: $t('privacy.content.retention.cloud.achievementsPeriod') },
    { field: $t('privacy.content.retention.cloud.userSettings'), purpose: $t('privacy.content.retention.cloud.userSettingsPeriod') },
    { field: $t('privacy.content.retention.cloud.accountData'), purpose: $t('privacy.content.retention.cloud.accountDataPeriod') },
    { field: $t('privacy.content.retention.cloud.accessTokens'), purpose: $t('privacy.content.retention.cloud.accessTokensPeriod') },
    { field: $t('privacy.content.retention.cloud.refreshTokens'), purpose: $t('privacy.content.retention.cloud.refreshTokensPeriod') },
    { field: $t('privacy.content.retention.cloud.oauthState'), purpose: $t('privacy.content.retention.cloud.oauthStatePeriod') }
  ];

  $: localRetentionRows = [
    { field: $t('privacy.content.retention.local.rideHistory'), purpose: $t('privacy.content.retention.local.rideHistoryPeriod') },
    { field: $t('privacy.content.retention.local.drillResults'), purpose: $t('privacy.content.retention.local.drillResultsPeriod') },
    { field: $t('privacy.content.retention.local.achievements'), purpose: $t('privacy.content.retention.local.achievementsPeriod') },
    { field: $t('privacy.content.retention.local.customDrills'), purpose: $t('privacy.content.retention.local.customDrillsPeriod') },
    { field: $t('privacy.content.retention.local.settings'), purpose: $t('privacy.content.retention.local.settingsPeriod') },
    { field: $t('privacy.content.retention.local.checkpoints'), purpose: $t('privacy.content.retention.local.checkpointsPeriod') }
  ];

  $: browserStorageRows = [
    { field: `<code>${$t('privacy.content.thirdParties.browserStorage.access')}</code>`, purpose: $t('privacy.content.thirdParties.browserStorage.accessPurpose') },
    { field: `<code>${$t('privacy.content.thirdParties.browserStorage.refresh')}</code>`, purpose: $t('privacy.content.thirdParties.browserStorage.refreshPurpose') },
    { field: `<code>${$t('privacy.content.thirdParties.browserStorage.theme')}</code>`, purpose: $t('privacy.content.thirdParties.browserStorage.themePurpose') }
  ];
</script>

<svelte:head>
  <title>{$t('privacy.title')} — KPedal</title>
  <meta name="description" content="KPedal Privacy Policy. Learn how we collect, use, and protect your cycling data. No location tracking, encrypted storage, your data is never sold.">
  <link rel="canonical" href="https://kpedal.com/privacy">
  <meta property="og:type" content="website">
  <meta property="og:url" content="https://kpedal.com/privacy">
  <meta property="og:title" content="{$t('privacy.title')} — KPedal">
  <meta property="og:description" content="Learn how KPedal protects your cycling data. No location tracking, encrypted storage, full control over your data.">
  <meta name="twitter:card" content="summary">
  <meta name="twitter:title" content="{$t('privacy.title')} — KPedal">
  <meta name="twitter:description" content="Learn how KPedal protects your cycling data. No location tracking, encrypted storage, full control.">
</svelte:head>

<div class="privacy-page">
  {#if !$isAuthenticated}
    <GuestLogo />
    <div class="top-controls"><GuestControls /></div>
  {/if}

  <div class="privacy-container">
    <header class="privacy-header">
      {#if !$isAuthenticated}
        <a href="/" class="back-link">← {$t('privacy.backToKpedal')}</a>
      {/if}
      <h1>{$t('privacy.title')}</h1>
      <p class="last-updated">{$t('privacy.lastUpdated', { values: { date: 'January 3, 2026' } })}</p>
    </header>

    <SummaryGrid items={summaryItems} />

    <div class="privacy-content">
      <TOC items={tocItems} />

      <!-- Section 1: What We Collect -->
      <section id="what-we-collect" class="privacy-section">
        <h2>{$t('privacy.sections.whatWeCollect')}</h2>

        <h3>{$t('privacy.content.accountData.title')}</h3>
        <p>{$t('privacy.content.accountData.intro')}</p>
        <DataTable rows={accountDataRows} />
        <p class="note">{$t('privacy.content.accountData.note')}</p>

        <h3>{$t('privacy.content.pedalingMetrics.title')}</h3>
        <p>{$t('privacy.content.pedalingMetrics.intro')}</p>
        <DataTable rows={pedalingMetricsRows} />

        <h3>{$t('privacy.content.performanceMetrics.title')}</h3>
        <p>{$t('privacy.content.performanceMetrics.intro')}</p>
        <DataTable rows={performanceMetricsRows} />
        <p class="note">{$t('privacy.content.performanceMetrics.note')}</p>

        <h3>{$t('privacy.content.snapshots.title')}</h3>
        <p>{$t('privacy.content.snapshots.intro')}</p>
        <DataTable rows={snapshotsRows} />
        <p class="note">{$t('privacy.content.snapshots.note')}</p>

        <h3>{$t('privacy.content.deviceData.title')}</h3>
        <DataTable rows={deviceDataRows} />

        <h3>{$t('privacy.content.userSettings.title')}</h3>
        <p>{$t('privacy.content.userSettings.intro')}</p>
        <DataTable rows={userSettingsRows} />
        <p class="note">{$t('privacy.content.userSettings.note')}</p>

        <h3>{$t('privacy.content.drillResults.title')}</h3>
        <p>{$t('privacy.content.drillResults.intro')}</p>
        <DataTable rows={drillResultsRows} />

        <h3>{$t('privacy.content.achievements.title')}</h3>
        <p>{$t('privacy.content.achievements.intro')}</p>
        <DataTable rows={achievementsRows} />

        <h3>{$t('privacy.content.localOnly.title')}</h3>
        <p>{@html $t('privacy.content.localOnly.intro')}</p>
        <DataTable rows={localOnlyRows} />
        <p class="note">{$t('privacy.content.localOnly.note')}</p>
      </section>

      <!-- Section 2: What We Don't Collect -->
      <section id="what-we-dont-collect" class="privacy-section">
        <h2>{$t('privacy.sections.whatWeDontCollect')}</h2>
        <p>{$t('privacy.content.dontCollect.intro')}</p>
        <DontCollectGrid items={dontCollectItems} />
        <p class="note">{$t('privacy.content.dontCollect.note')}</p>

        <h3>{$t('privacy.content.usedNotStored.title')}</h3>
        <p>{@html $t('privacy.content.usedNotStored.intro')}</p>
        <DataTable rows={usedNotStoredRows} />

        <h3>{$t('privacy.content.permissions.title')}</h3>
        <p>{$t('privacy.content.permissions.intro')}</p>
        <DataTable rows={permissionsRows} />
        <p class="note">{$t('privacy.content.permissions.note')}</p>
      </section>

      <!-- Section 3: How Data Flows -->
      <section id="how-data-flows" class="privacy-section">
        <h2>{$t('privacy.sections.howDataFlows')}</h2>
        <FlowDiagram steps={flowSteps} />
        <h3>{$t('privacy.content.dataFlow.offlineTitle')}</h3>
        <p>{$t('privacy.content.dataFlow.offlineDesc')}</p>
      </section>

      <!-- Section 4: Storage & Security -->
      <section id="storage-security" class="privacy-section">
        <h2>{$t('privacy.sections.storageSecurity')}</h2>

        <h3>{$t('privacy.content.infrastructure.title')}</h3>
        <DataTable rows={infraRows} variant="infra" />

        <h3>{$t('privacy.content.security.title')}</h3>
        <SecurityList items={securityItems} />

        <h3>{$t('privacy.content.retention.title')}</h3>
        <h4>{$t('privacy.content.retention.cloudTitle')}</h4>
        <DataTable rows={cloudRetentionRows} variant="retention" />

        <h4>{$t('privacy.content.retention.localTitle')}</h4>
        <DataTable rows={localRetentionRows} variant="retention" />
      </section>

      <!-- Section 5: Your Rights -->
      <section id="your-rights" class="privacy-section">
        <h2>{$t('privacy.sections.yourRights')}</h2>
        <RightsGrid items={rightsItems} />
      </section>

      <!-- Section 6: Third Parties -->
      <section id="third-parties" class="privacy-section">
        <h2>{$t('privacy.sections.thirdParties')}</h2>
        <ThirdPartyList items={thirdParties} />

        <h3>{$t('privacy.content.thirdParties.notUsedTitle')}</h3>
        <p>{$t('privacy.content.thirdParties.notUsedIntro')}</p>
        <ul>
          {#each $t('privacy.content.thirdParties.notUsed') as item}
            <li>{item}</li>
          {/each}
        </ul>

        <h3>{$t('privacy.content.thirdParties.browserStorageTitle')}</h3>
        <p>{$t('privacy.content.thirdParties.browserStorageIntro')}</p>
        <DataTable rows={browserStorageRows} />
        <p class="note">{$t('privacy.content.thirdParties.browserStorageNote')}</p>
      </section>

      <!-- Section 7: Contact -->
      <section id="contact" class="privacy-section">
        <h2>{$t('privacy.sections.contact')}</h2>
        <p>{$t('privacy.contact.text')}</p>
        <ContactInfo />
      </section>

      <!-- Policy Changes -->
      <section class="privacy-section">
        <h2>{$t('privacy.sections.policyChanges')}</h2>
        <p>{$t('privacy.policyChanges')}</p>
      </section>
    </div>
  </div>

  <Footer />
</div>

<style>
  .privacy-page {
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    background: var(--bg-base);
    position: relative;
    overflow-x: hidden;
    width: 100%;
    max-width: 100vw;
  }

  .privacy-container {
    flex: 1;
    max-width: 720px;
    width: 100%;
    margin: 0 auto;
    padding: 24px;
    position: relative;
    overflow-wrap: break-word;
    word-wrap: break-word;
    box-sizing: border-box;
  }

  .top-controls {
    position: fixed;
    top: 24px;
    right: 24px;
    z-index: 100;
  }

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
  }

  .back-link:hover { text-decoration: underline; }

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

  .privacy-section li { margin-bottom: 6px; }

  .privacy-section a { color: var(--color-accent); }
  .privacy-section a:hover { text-decoration: underline; }

  .note {
    font-size: 13px;
    color: var(--text-tertiary);
    font-style: italic;
  }

  .privacy-page *, .privacy-page *::before, .privacy-page *::after {
    box-sizing: border-box;
  }

  @media (max-width: 640px) {
    .privacy-container {
      padding: 16px;
      padding-top: 60px;
    }

    .top-controls {
      top: 16px;
      right: 16px;
    }

    .privacy-header {
      padding: 24px 0 24px;
    }

    .privacy-header h1 { font-size: 26px; }
    .privacy-section h2 { font-size: 20px; }
    .privacy-section h3 { font-size: 15px; }
    .privacy-section p { font-size: 14px; }
  }
</style>
