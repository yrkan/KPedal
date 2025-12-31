<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { browser } from '$app/environment';
  import { isAuthenticated, user, authFetch, isDemo } from '$lib/auth';
  import { startDashboardTour, isTourCompleted } from '$lib/tour';
  import { getDemoDashboard } from '$lib/demoData';
  import Footer from '$lib/components/Footer.svelte';
  import { t, locale } from '$lib/i18n';

  // Landing page components
  import {
    LandingHeader,
    Hero,
    DataFieldsShowcase,
    ComparisonTable,
    MetricsDeepDive,
    DrillsSection,
    AlertsSection,
    DashboardPreview,
    MotivationSection,
    WorksEverywhereSection,
    RequirementsSection,
    FAQSection,
    CTASection
  } from '$lib/components/landing';

  // Dashboard components
  import {
    DashboardHeader,
    LoadingState,
    ErrorState,
    HeroStats,
    ActivityCard,
    TechniqueCard,
    TrendsRow,
    ProgressRow,
    RecentRides,
    MetricsStrip,
    EmptyState
  } from '$lib/components/dashboard';

  // Types
  import type {
    Stats,
    Ride,
    WeeklyComparison,
    TrendData,
    FatigueAnalysis,
    PeriodStats,
    PrevPeriodStats,
    InsightTranslations
  } from '$lib/types/dashboard';

  // Utilities
  import {
    getFilteredRides,
    getWeekDays,
    getRidesPerDay,
    getPeriodStats,
    getPreviousPeriodStats,
    getProgress,
    getBalanceProgress,
    generateInsights
  } from '$lib/utils/dashboard';

  import { calculateFatigueAnalysis } from '$lib/utils/fatigue';

  // Use $page.url.hostname - available immediately without waiting for onMount
  // This eliminates the "pageReady" delay for client-side navigation
  $: isLandingDomain = browser ? window.location.hostname === 'kpedal.com' : $page.url.hostname === 'kpedal.com';

  // Show landing ONLY on kpedal.com (landing domain)
  // On app.kpedal.com, guests are redirected to /login, authenticated users see dashboard
  $: showLanding = isLandingDomain;

  // State
  let stats: Stats | null = null;
  let recentRides: Ride[] = [];
  let weeklyRides: Ride[] = [];
  let weeklyComparison: WeeklyComparison | null = null;
  let fatigueData: FatigueAnalysis | null = null;
  let trendData: TrendData[] = [];
  let loading = true;
  let error = false;
  let selectedPeriod: '7' | '14' | '30' | '60' = '7';

  async function loadDashboardData() {
    loading = true;
    try {
      // Demo mode: use static data (0ms, no API call)
      if ($isDemo) {
        const data = getDemoDashboard();
        stats = data.stats;
        recentRides = data.recentRides;
        weeklyRides = recentRides;
        weeklyComparison = data.weeklyComparison;
        trendData = data.trends;
        if (data.lastRideSnapshots?.length >= 6) {
          fatigueData = calculateFatigueAnalysis(data.lastRideSnapshots);
        }
        loading = false;
        return;
      }

      // Regular users: API call
      const res = await authFetch('/rides/dashboard?include=snapshots');

      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          stats = data.data.stats;
          recentRides = data.data.recentRides || [];
          weeklyRides = recentRides;
          weeklyComparison = data.data.weeklyComparison;
          trendData = data.data.trends || [];

          if (data.data.lastRideSnapshots?.length >= 6) {
            fatigueData = calculateFatigueAnalysis(data.data.lastRideSnapshots);
          }
        }
      } else {
        error = true;
      }
    } catch (err) {
      error = true;
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    // Check hostname directly to avoid SSR/hydration race condition
    const isLanding = window.location.hostname === 'kpedal.com';

    // Redirect guests on app.kpedal.com to login
    if (!$isAuthenticated && !isLanding) {
      goto('/login');
      return;
    }

    // On landing domain, components handle their own state
    if (isLanding) {
      loading = false;
      return;
    }

    // On app.kpedal.com with authenticated user - load dashboard data
    loadDashboardData().then(() => {
      if ($isDemo && !isTourCompleted()) {
        setTimeout(() => startDashboardTour(), 800);
      }
    });
  });

  // Reactive declarations using imported utilities
  $: periodStats = getPeriodStats(weeklyRides, parseInt(selectedPeriod));
  $: prevPeriodStats = getPreviousPeriodStats(weeklyRides, parseInt(selectedPeriod));
  $: scoreProgress = periodStats && prevPeriodStats ? getProgress(periodStats.score, prevPeriodStats.score) : null;
  $: optimalProgress = periodStats && prevPeriodStats ? getProgress(periodStats.zoneOptimal, prevPeriodStats.zoneOptimal) : null;
  $: teProgress = periodStats && prevPeriodStats ? getProgress(periodStats.te, prevPeriodStats.te) : null;
  $: psProgress = periodStats && prevPeriodStats ? getProgress(periodStats.ps, prevPeriodStats.ps) : null;
  $: balanceProgress = periodStats && prevPeriodStats ? getBalanceProgress(periodStats.balance, prevPeriodStats.balance) : null;
  $: powerProgress = periodStats && prevPeriodStats && periodStats.avgPower > 0 && prevPeriodStats.avgPower > 0
    ? getProgress(periodStats.avgPower, prevPeriodStats.avgPower, 2) : null;
  $: durationProgress = periodStats && prevPeriodStats
    ? getProgress(periodStats.duration / 3600000, prevPeriodStats.duration / 3600000, 0.2) : null;
  $: distanceProgress = periodStats && prevPeriodStats
    ? getProgress(periodStats.totalDistance, prevPeriodStats.distance, 5) : null;
  $: filteredRides = getFilteredRides(weeklyRides, parseInt(selectedPeriod));

  // Calculate rides count for each period (for disabling period buttons)
  $: ridesIn7Days = getFilteredRides(weeklyRides, 7).length;
  $: ridesIn14Days = getFilteredRides(weeklyRides, 14).length;
  $: ridesIn30Days = getFilteredRides(weeklyRides, 30).length;
  $: ridesIn60Days = getFilteredRides(weeklyRides, 60).length;
  $: weekDays = getWeekDays($locale);
  $: ridesPerDay = getRidesPerDay(weeklyRides);
  $: maxRidesPerDay = Math.max(...ridesPerDay, 1);
  $: insightTranslations = {
    fatigueAction: $t('dashboard.insights.fatigueAction'),
    fatigueLink: $t('dashboard.insights.fatigueLink'),
    balanceAction: $t('dashboard.insights.balanceAction'),
    balanceLink: $t('dashboard.insights.balanceLink'),
    leftLeg: $t('dashboard.insights.leftLeg'),
    rightLeg: $t('dashboard.insights.rightLeg'),
    leftDominant: $t('dashboard.insights.leftDominant'),
    rightDominant: $t('dashboard.insights.rightDominant'),
    lowTeAction: $t('dashboard.insights.lowTeAction'),
    lowTeLink: $t('dashboard.insights.lowTeLink'),
    lowPsAction: $t('dashboard.insights.lowPsAction'),
    lowPsLink: $t('dashboard.insights.lowPsLink'),
    greatBalance: $t('dashboard.insights.greatBalance'),
    teOptimal: $t('dashboard.insights.teOptimal'),
    excellentScore: $t('dashboard.insights.excellentScore'),
    goodConsistency: $t('dashboard.insights.goodConsistency'),
    improvement: $t('dashboard.insights.improvement'),
    encourageRides: $t('dashboard.insights.encourageRides'),
    ride: $t('dashboard.insights.ride'),
    ridesPl: $t('dashboard.insights.ridesPl'),
    nearOptimalTe: $t('dashboard.insights.nearOptimalTe')
  } as InsightTranslations;
  $: insights = generateInsights(periodStats, prevPeriodStats, weeklyComparison, fatigueData, insightTranslations);
</script>

<svelte:head>
  <title>{$isAuthenticated ? 'Dashboard | KPedal' : 'KPedal — Real-Time Pedaling Analytics for Hammerhead Karoo'}</title>
  <meta name="description" content="Free Karoo extension for cyclists. See power balance, torque effectiveness, pedal smoothness in real-time. 10 guided drills, automatic cloud sync, research-backed thresholds. Works with Garmin Rally, Favero Assioma, SRM pedals.">

  <!-- Keywords and additional meta -->
  <meta name="keywords" content="KPedal, Karoo extension, pedaling efficiency, power balance, torque effectiveness, pedal smoothness, cycling analytics, Hammerhead Karoo, Karoo 2, Karoo 3, cycling drills, power meter pedals, Garmin Rally, Favero Assioma, ANT+ Cycling Dynamics">
  <meta name="application-name" content="KPedal">

  <!-- Canonical URL -->
  <link rel="canonical" href="https://kpedal.com/">

  <!-- Open Graph / Facebook -->
  <meta property="og:type" content="website">
  <meta property="og:url" content="https://kpedal.com/">
  <meta property="og:title" content="KPedal — Real-Time Pedaling Analytics for Hammerhead Karoo">
  <meta property="og:description" content="Free Karoo extension. See power balance, torque effectiveness, pedal smoothness in real-time. 10 guided drills with scoring. Cloud sync to web dashboard.">
  <meta property="og:site_name" content="KPedal">
  <meta property="og:locale" content="en_US">

  <!-- Twitter -->
  <meta name="twitter:card" content="summary">
  <meta name="twitter:url" content="https://kpedal.com/">
  <meta name="twitter:title" content="KPedal — Real-Time Pedaling Analytics for Hammerhead Karoo">
  <meta name="twitter:description" content="Free Karoo extension. See power balance, torque effectiveness, pedal smoothness in real-time. 10 guided drills with scoring.">

  <!-- JSON-LD Structured Data -->
  {@html `<script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    "name": "KPedal",
    "operatingSystem": "Android (Hammerhead Karoo)",
    "applicationCategory": "SportsApplication",
    "applicationSubCategory": "Cycling Analytics",
    "offers": {
      "@type": "Offer",
      "price": "0",
      "priceCurrency": "USD"
    },
    "description": "Real-time pedaling efficiency analytics for Hammerhead Karoo bike computers. Displays power balance, torque effectiveness, and pedal smoothness with research-backed thresholds and guided training drills.",
    "url": "https://kpedal.com",
    "downloadUrl": "https://github.com/yrkan/kpedal/releases",
    "softwareVersion": "1.4.0",
    "featureList": [
      "Real-time power balance monitoring",
      "Torque effectiveness analysis",
      "Pedal smoothness tracking",
      "10 guided training drills",
      "Customizable alerts with vibration",
      "Cloud sync to web dashboard",
      "Background data collection",
      "Multi-device support"
    ],
    "screenshot": "https://kpedal.com/screenshot.png",
    "author": {
      "@type": "Organization",
      "name": "KPedal",
      "url": "https://kpedal.com"
    },
    "aggregateRating": {
      "@type": "AggregateRating",
      "ratingValue": "5",
      "ratingCount": "1",
      "bestRating": "5",
      "worstRating": "1"
    }
  }
  </script>`}

  <!-- FAQ Schema -->
  {@html `<script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    "mainEntity": [
      {
        "@type": "Question",
        "name": "Does KPedal work offline?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "Yes! All core features work without internet. Cloud sync is optional — your data is always stored locally on the Karoo first."
        }
      },
      {
        "@type": "Question",
        "name": "Will KPedal drain my Karoo battery?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "Minimal impact. KPedal only runs during active rides and uses efficient data collection. Background mode uses approximately 1-2% extra battery per hour."
        }
      },
      {
        "@type": "Question",
        "name": "What power pedals are compatible with KPedal?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "Full support (Balance + TE + PS): Garmin Rally RS/RK/XC, Garmin Vector 3, Favero Assioma DUO/DUO-Shi/Pro RS/Pro MX, SRM X-Power, Rotor 2INpower, Look Keo Blade Power, IQ2 Power Pedals. Balance only: Wahoo POWRLINK Zero, Power2Max, Quarq DZero, SRAM AXS PM, Stages LR, 4iiii Precision Dual, Sigeyi AXO, FSA Powerbox. Not compatible: single-sided power meters, trainer power, Garmin Vector 1/2."
        }
      },
      {
        "@type": "Question",
        "name": "Can I use KPedal with Garmin or Wahoo computers?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "No. KPedal is built specifically for Hammerhead Karoo using their SDK. It uses native Karoo features not available on other platforms."
        }
      },
      {
        "@type": "Question",
        "name": "What is the optimal torque effectiveness range?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "70-80% is optimal based on Wattbike research. Higher isn't better — above 80% can actually reduce total power output. Below 60% indicates significant technique issues."
        }
      },
      {
        "@type": "Question",
        "name": "How do I install KPedal on Karoo?",
        "acceptedAnswer": {
          "@type": "Answer",
          "text": "Download the APK from GitHub Releases. Transfer to Karoo via USB or use Karoo's built-in file browser with a direct download link. Open the APK to install."
        }
      }
    ]
  }
  </script>`}

  <!-- Organization Schema -->
  {@html `<script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "Organization",
    "name": "KPedal",
    "url": "https://kpedal.com",
    "logo": "https://kpedal.com/favicon.svg",
    "sameAs": [
      "https://github.com/yrkan/kpedal"
    ],
    "contactPoint": {
      "@type": "ContactPoint",
      "contactType": "technical support",
      "url": "https://github.com/yrkan/kpedal/issues"
    }
  }
  </script>`}

  <!-- WebSite Schema for search box -->
  {@html `<script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "WebSite",
    "name": "KPedal",
    "url": "https://kpedal.com",
    "description": "Real-time pedaling efficiency analytics for Hammerhead Karoo",
    "publisher": {
      "@type": "Organization",
      "name": "KPedal"
    }
  }
  </script>`}
</svelte:head>

{#if showLanding}
  <div class="landing" role="main" itemscope itemtype="https://schema.org/WebPage">
    <LandingHeader />

    <article class="landing-container" itemscope itemtype="https://schema.org/SoftwareApplication">
      <meta itemprop="name" content="KPedal" />
      <meta itemprop="operatingSystem" content="Android (Hammerhead Karoo)" />
      <meta itemprop="applicationCategory" content="SportsApplication" />
      <link itemprop="downloadUrl" href="https://github.com/yrkan/kpedal/releases" />

      <Hero />

      <DataFieldsShowcase />

      <ComparisonTable />

      <MetricsDeepDive />

      <DrillsSection />

      <AlertsSection />

      <DashboardPreview />

      <MotivationSection />

      <WorksEverywhereSection />

      <RequirementsSection />

      <FAQSection />

      <CTASection />

      <Footer />
    </article>
  </div>
{:else}
  <!-- Dashboard (authenticated) -->
  <div class="dashboard">
    <div class="container">
      {#if loading}
        <LoadingState />
      {:else if error}
        <ErrorState onRetry={() => location.reload()} />
      {:else}
        {#if stats && stats.total_rides > 0}
          <DashboardHeader
            {selectedPeriod}
            {ridesIn7Days}
            {ridesIn14Days}
            {ridesIn30Days}
            {ridesIn60Days}
            on:periodChange={(e) => selectedPeriod = e.detail}
          />
          <!-- Hero Stats Row -->
          <HeroStats {periodStats} />

          <!-- Main Grid -->
          <div class="main-grid animate-in">
            <!-- Left Column: Activity + Training -->
            <ActivityCard {weekDays} {ridesPerDay} {maxRidesPerDay} {weeklyComparison} {insights} />

            <!-- Right Column: Technique -->
            <TechniqueCard {stats} {periodStats} {selectedPeriod} {weeklyComparison} {fatigueData} />
          </div>

          <!-- Power & Cycling Metrics (if available) -->
          {#if periodStats && (periodStats.avgPower > 0 || periodStats.totalElevationGain > 0)}
            <MetricsStrip {periodStats} />
          {/if}

          <!-- Trends Row: Balance + Technique side by side -->
          <TrendsRow {filteredRides} {trendData} {selectedPeriod} />

          <!-- Progress Row (if we have comparison data) -->
          {#if prevPeriodStats && periodStats}
            <ProgressRow {periodStats} {selectedPeriod} {scoreProgress} {optimalProgress} {teProgress} {psProgress} {durationProgress} {distanceProgress} />
          {/if}

          <!-- Recent Rides -->
          {#if recentRides.length > 0}
            <RecentRides {recentRides} />
          {/if}
        {:else}
          <EmptyState userName={$user?.name} />
        {/if}
      {/if}
    </div>
  </div>
{/if}

<style>
  /* ============================================
     Landing Page Styles
     ============================================ */
  .landing {
    min-height: 100vh;
    padding: 32px;
    position: relative;
  }

  .landing-container {
    max-width: 680px;
    margin: 0 auto;
    padding: 64px 0 96px;
  }

  /* Global section styles for landing child components */
  :global(.section) {
    margin-bottom: 96px;
  }
  :global(.section-title) {
    font-size: 26px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 10px;
    letter-spacing: -0.5px;
    text-transform: none;
  }
  :global(.section-subtitle) {
    font-size: 15px;
    color: var(--text-tertiary);
    margin-bottom: 40px;
    line-height: 1.6;
    letter-spacing: -0.1px;
  }

  /* ============================================
     Dashboard Styles
     ============================================ */
  .dashboard {
    padding: 24px 0 48px;
  }

  .main-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 16px;
    align-items: stretch;
  }

  /* ============================================
     Responsive: 768px - Tablets
     ============================================ */
  @media (max-width: 768px) {
    .landing { padding: 20px; }
    .landing-container { padding: 48px 0 64px; }

    :global(.section) { margin-bottom: 64px; }
    :global(.section-title) { font-size: 26px; letter-spacing: -0.5px; }
    :global(.section-subtitle) { font-size: 15px; margin-bottom: 28px; }

    .main-grid { grid-template-columns: 1fr; gap: 12px; }
  }

  /* ============================================
     Responsive: 480px - Mobile phones
     ============================================ */
  @media (max-width: 480px) {
    .landing {
      padding: 20px;
      padding-bottom: 40px;
    }
    .landing-container {
      padding: 40px 0 60px;
    }

    :global(.section) { margin-bottom: 48px; }
    :global(.section-title) { font-size: 22px; letter-spacing: -0.3px; }
    :global(.section-subtitle) { font-size: 14px; margin-bottom: 24px; }

    .dashboard { padding: 24px 0 48px; }
  }

  /* ============================================
     Responsive: 360px - Small phones
     ============================================ */
  @media (max-width: 360px) {
    .landing { padding: 14px; }

    :global(.section-title) { font-size: 20px; }
    :global(.section-subtitle) { font-size: 13px; }
  }

  /* ============================================
     Accessibility: Reduced motion
     ============================================ */
  @media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
      animation-duration: 0.01ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: 0.01ms !important;
    }
  }
</style>
