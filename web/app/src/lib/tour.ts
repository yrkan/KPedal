/**
 * KPedal Comprehensive Product Tour
 * Super detailed guided onboarding for demo users using driver.js
 * Covers: Dashboard, Rides, Ride Details, Drills, Achievements, Settings
 *
 * Performance: driver.js is lazy-loaded only when tour is actually started
 */

import { browser } from '$app/environment';

// Types imported separately (tree-shaken, no runtime cost)
import type { DriveStep, Driver } from 'driver.js';

// Lazy load driver.js and CSS only when needed
let driverModule: typeof import('driver.js') | null = null;
let cssLoaded = false;

async function loadDriver(): Promise<typeof import('driver.js')> {
  if (!driverModule) {
    // Load CSS dynamically (only once)
    if (!cssLoaded && browser) {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = '/driver.css';
      document.head.appendChild(link);
      cssLoaded = true;
    }
    // Dynamic import of driver.js
    driverModule = await import('driver.js');
  }
  return driverModule;
}
import { goto } from '$app/navigation';
import { get } from 'svelte/store';
import { t } from '$lib/i18n';

const TOUR_STORAGE_KEY = 'kpedal_tour_completed';
const TOUR_PAGE_KEY = 'kpedal_tour_page';

// Helper to get translation
const _ = () => get(t);

/**
 * Check if user has completed the tour
 */
export function isTourCompleted(): boolean {
  if (!browser) return true;
  return localStorage.getItem(TOUR_STORAGE_KEY) === 'true';
}

/**
 * Mark tour as completed
 */
export function markTourCompleted(): void {
  if (!browser) return;
  localStorage.setItem(TOUR_STORAGE_KEY, 'true');
  localStorage.removeItem(TOUR_PAGE_KEY);
}

/**
 * Reset tour (for restarting)
 */
export function resetTour(): void {
  if (!browser) return;
  localStorage.removeItem(TOUR_STORAGE_KEY);
  localStorage.removeItem(TOUR_PAGE_KEY);
}

/**
 * Get next tour page to visit
 */
export function getNextTourPage(): string | null {
  if (!browser) return null;
  return localStorage.getItem(TOUR_PAGE_KEY);
}

/**
 * Set next tour page
 */
export function setNextTourPage(page: string): void {
  if (!browser) return;
  localStorage.setItem(TOUR_PAGE_KEY, page);
}

/**
 * Clear next tour page
 */
export function clearNextTourPage(): void {
  if (!browser) return;
  localStorage.removeItem(TOUR_PAGE_KEY);
}

// ============================================
// DASHBOARD TOUR STEPS
// ============================================

function getDashboardSteps(): DriveStep[] {
  const i = _();
  return [
    // Welcome modal
    {
      popover: {
        title: i('tour.dashboard.welcome.title'),
        description: `
          <p>${i('tour.dashboard.welcome.desc')}</p>
          <p style="margin-top: 8px;">${i('tour.dashboard.welcome.hint')}</p>
          <p style="margin-top: 14px; text-align: center;"><button onclick="window.skipKPedalTour()" class="skip-tour-link">${i('tour.buttons.skip')}</button></p>
        `,
        side: 'over',
        align: 'center',
      },
    },
    // Period selector
    {
      element: '.period-selector',
      popover: {
        title: i('tour.dashboard.period.title'),
        description: `
          <p>${i('tour.dashboard.period.desc')}</p>
          <p style="margin-top: 8px; color: var(--text-secondary);">${i('tour.dashboard.period.hint')}</p>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    // Power Asymmetry stat
    {
      element: '.hero-stat.asymmetry',
      popover: {
        title: i('tour.dashboard.asymmetry.title'),
        description: `
          <p>${i('tour.dashboard.asymmetry.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px; line-height: 1.6;">
            <li><strong style="color: var(--color-optimal)">${i('tour.dashboard.asymmetry.under25')}</strong> — ${i('tour.dashboard.asymmetry.proLevel')}</li>
            <li><strong style="color: var(--color-attention)">${i('tour.dashboard.asymmetry.range25to5')}</strong> — ${i('tour.dashboard.asymmetry.goodRoom')}</li>
            <li><strong style="color: var(--color-problem)">${i('tour.dashboard.asymmetry.over5')}</strong> — ${i('tour.dashboard.asymmetry.needsAttention')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.dashboard.asymmetry.hint')}</p>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    // InfoTip highlight
    {
      element: '.hero-stat.asymmetry .info-tip-trigger',
      popover: {
        title: i('tour.dashboard.infoTip.title'),
        description: `
          <p>${i('tour.dashboard.infoTip.desc')}</p>
          <p style="margin-top: 8px; color: var(--text-secondary);">${i('tour.dashboard.infoTip.hint')}</p>
        `,
        side: 'right',
        align: 'center',
      },
    },
    // Balance visual
    {
      element: '.hero-stat.balance-visual',
      popover: {
        title: i('tour.dashboard.balance.title'),
        description: `
          <p>${i('tour.dashboard.balance.desc')}</p>
          <p style="margin-top: 8px;">${i('tour.dashboard.balance.greenZone')}</p>
          <p style="margin-top: 8px; color: var(--text-secondary);">${i('tour.dashboard.balance.hint')}</p>
        `,
        side: 'bottom',
        align: 'center',
      },
    },
    // Time in zones
    {
      element: '.hero-stat.zones',
      popover: {
        title: i('tour.dashboard.zones.title'),
        description: `
          <p>${i('tour.dashboard.zones.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px; line-height: 1.6;">
            <li><strong style="color: var(--color-optimal)">${i('tour.dashboard.zones.optimal')}</strong> — ${i('tour.dashboard.zones.optimalDesc')}</li>
            <li><strong style="color: var(--color-attention)">${i('tour.dashboard.zones.attention')}</strong> — ${i('tour.dashboard.zones.attentionDesc')}</li>
            <li><strong style="color: var(--color-problem)">${i('tour.dashboard.zones.problem')}</strong> — ${i('tour.dashboard.zones.problemDesc')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.dashboard.zones.hint')}</p>
        `,
        side: 'bottom',
        align: 'center',
      },
    },
    // Summary stats
    {
      element: '.hero-stat.summary',
      popover: {
        title: i('tour.dashboard.summary.title'),
        description: `
          <p>${i('tour.dashboard.summary.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li><strong>${i('tour.dashboard.summary.rides')}</strong></li>
            <li><strong>${i('tour.dashboard.summary.hours')}</strong></li>
            <li><strong>${i('tour.dashboard.summary.km')}</strong></li>
          </ul>
        `,
        side: 'bottom',
        align: 'end',
      },
    },
    // Weekly activity card
    {
      element: '.main-grid .grid-card:first-child',
      popover: {
        title: i('tour.dashboard.weekly.title'),
        description: `
          <p>${i('tour.dashboard.weekly.desc')}</p>
          <p style="margin-top: 8px;">${i('tour.dashboard.weekly.compareHint')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.dashboard.weekly.numRides')}</li>
            <li>${i('tour.dashboard.weekly.totalHours')}</li>
            <li>${i('tour.dashboard.weekly.distanceClimb')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.dashboard.weekly.hint')}</p>
        `,
        side: 'right',
        align: 'start',
      },
    },
    // Technique card - TE/PS gauges
    {
      element: '.main-grid .grid-card:last-child',
      popover: {
        title: i('tour.dashboard.technique.title'),
        description: `
          <p>${i('tour.dashboard.technique.desc')}</p>
          <p style="margin-top: 8px;"><strong>${i('tour.dashboard.technique.teTitle')}</strong><br/>
          ${i('tour.dashboard.technique.teDesc')}</p>
          <p style="margin-top: 8px;"><strong>${i('tour.dashboard.technique.psTitle')}</strong><br/>
          ${i('tour.dashboard.technique.psDesc')}</p>
        `,
        side: 'left',
        align: 'start',
      },
    },
    // Trend charts
    {
      element: '.trends-row',
      popover: {
        title: i('tour.dashboard.trends.title'),
        description: `
          <p>${i('tour.dashboard.trends.desc')}</p>
          <p style="margin-top: 8px;">${i('tour.dashboard.trends.left')}</p>
          <p>${i('tour.dashboard.trends.right')}</p>
          <p style="margin-top: 8px; color: var(--text-secondary);">${i('tour.dashboard.trends.hint')}</p>
        `,
        side: 'top',
        align: 'center',
      },
    },
    // Recent rides table
    {
      element: '.recent-rides',
      popover: {
        title: i('tour.dashboard.recentRides.title'),
        description: `
          <p>${i('tour.dashboard.recentRides.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.dashboard.recentRides.date')}</li>
            <li>${i('tour.dashboard.recentRides.asymmetry')}</li>
            <li>${i('tour.dashboard.recentRides.lrBreakdown')}</li>
            <li>${i('tour.dashboard.recentRides.zoneBars')}</li>
          </ul>
          <p style="margin-top: 8px; color: var(--text-secondary);">${i('tour.dashboard.recentRides.hint')}</p>
        `,
        side: 'top',
        align: 'center',
      },
    },
    // Navigation hint
    {
      element: 'nav',
      popover: {
        title: i('tour.dashboard.nav.title'),
        description: `
          <p>${i('tour.dashboard.nav.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px; line-height: 1.6;">
            <li>${i('tour.dashboard.nav.rides')}</li>
            <li>${i('tour.dashboard.nav.drills')}</li>
            <li>${i('tour.dashboard.nav.achievements')}</li>
            <li>${i('tour.dashboard.nav.settings')}</li>
          </ul>
        `,
        side: 'bottom',
        align: 'center',
      },
    },
    // End of dashboard
    {
      popover: {
        title: i('tour.dashboard.complete.title'),
        description: `
          <p>${i('tour.dashboard.complete.desc')}</p>
          <p style="margin-top: 12px;">${i('tour.dashboard.complete.next')}</p>
          <p style="margin-top: 8px; color: var(--text-secondary);">${i('tour.dashboard.complete.done')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
  ];
}

// ============================================
// RIDES PAGE TOUR STEPS
// ============================================

function getRidesPageSteps(): DriveStep[] {
  const i = _();
  return [
    {
      popover: {
        title: i('tour.rides.welcome.title'),
        description: `
          <p>${i('tour.rides.welcome.desc')}</p>
          <p style="margin-top: 8px;">${i('tour.rides.welcome.hint')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '.stats-strip',
      popover: {
        title: i('tour.rides.stats.title'),
        description: `
          <p>${i('tour.rides.stats.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.rides.stats.ridesAndDuration')}</li>
            <li>${i('tour.rides.stats.scores')}</li>
            <li>${i('tour.rides.stats.powerDistance')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.rides.stats.hint')}</p>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '.view-toggle',
      popover: {
        title: i('tour.rides.viewToggle.title'),
        description: `
          <p>${i('tour.rides.viewToggle.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.rides.viewToggle.table')}</li>
            <li>${i('tour.rides.viewToggle.card')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.rides.viewToggle.hint')}</p>
        `,
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '.ride-row[data-ride-id], .ride-card[data-ride-id]',
      popover: {
        title: i('tour.rides.rideRow.title'),
        description: `
          <p>${i('tour.rides.rideRow.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.rides.rideRow.date')}</li>
            <li>${i('tour.rides.rideRow.asymmetry')}</li>
            <li>${i('tour.rides.rideRow.lrMetrics')}</li>
            <li>${i('tour.rides.rideRow.zones')}</li>
          </ul>
          <p style="margin-top: 8px;">${i('tour.rides.rideRow.hint')}</p>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: i('tour.rides.complete.title'),
        description: `
          <p>${i('tour.rides.complete.desc')}</p>
          <p style="margin-top: 12px;">${i('tour.rides.complete.next')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
  ];
}

// ============================================
// RIDE DETAIL PAGE TOUR STEPS
// ============================================

function getRideDetailSteps(): DriveStep[] {
  const i = _();
  return [
    {
      popover: {
        title: i('tour.rideDetail.welcome.title'),
        description: `
          <p>${i('tour.rideDetail.welcome.desc')}</p>
          <p style="margin-top: 8px;">${i('tour.rideDetail.welcome.hint')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '.hero-stats',
      popover: {
        title: i('tour.rideDetail.heroStats.title'),
        description: `
          <p>${i('tour.rideDetail.heroStats.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.rideDetail.heroStats.score')}</li>
            <li>${i('tour.rideDetail.heroStats.asymmetry')}</li>
            <li>${i('tour.rideDetail.heroStats.balance')}</li>
            <li>${i('tour.rideDetail.heroStats.zones')}</li>
          </ul>
        `,
        side: 'bottom',
        align: 'center',
      },
    },
    {
      element: '.perf-strip',
      popover: {
        title: i('tour.rideDetail.perfStrip.title'),
        description: `
          <p>${i('tour.rideDetail.perfStrip.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.rideDetail.perfStrip.power')}</li>
            <li>${i('tour.rideDetail.perfStrip.hr')}</li>
            <li>${i('tour.rideDetail.perfStrip.cadence')}</li>
            <li>${i('tour.rideDetail.perfStrip.elevation')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.rideDetail.perfStrip.hint')}</p>
        `,
        side: 'bottom',
        align: 'center',
      },
    },
    {
      element: '.technique-card',
      popover: {
        title: i('tour.rideDetail.techniqueCard.title'),
        description: `
          <p>${i('tour.rideDetail.techniqueCard.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.rideDetail.techniqueCard.te')}</li>
            <li>${i('tour.rideDetail.techniqueCard.ps')}</li>
            <li>${i('tour.rideDetail.techniqueCard.lr')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.rideDetail.techniqueCard.hint')}</p>
        `,
        side: 'right',
        align: 'start',
      },
    },
    {
      element: '.fatigue-card',
      popover: {
        title: i('tour.rideDetail.fatigueCard.title'),
        description: `
          <p>${i('tour.rideDetail.fatigueCard.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.rideDetail.fatigueCard.startEnd')}</li>
            <li>${i('tour.rideDetail.fatigueCard.colors')}</li>
            <li>${i('tour.rideDetail.fatigueCard.badge')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.rideDetail.fatigueCard.hint')}</p>
        `,
        side: 'left',
        align: 'start',
      },
    },
    {
      element: '.timeline-card',
      popover: {
        title: i('tour.rideDetail.timeline.title'),
        description: `
          <p>${i('tour.rideDetail.timeline.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.rideDetail.timeline.tabs')}</li>
            <li>${i('tour.rideDetail.timeline.points')}</li>
            <li>${i('tour.rideDetail.timeline.hover')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.rideDetail.timeline.hint')}</p>
        `,
        side: 'top',
        align: 'center',
      },
    },
    {
      popover: {
        title: i('tour.rideDetail.complete.title'),
        description: `
          <p>${i('tour.rideDetail.complete.desc')}</p>
          <p style="margin-top: 12px;">${i('tour.rideDetail.complete.next')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
  ];
}

// ============================================
// DRILLS PAGE TOUR STEPS
// ============================================

function getDrillsPageSteps(): DriveStep[] {
  const i = _();
  return [
    {
      popover: {
        title: i('tour.drills.welcome.title'),
        description: `
          <p>${i('tour.drills.welcome.desc')}</p>
          <p style="margin-top: 8px;">${i('tour.drills.welcome.hint')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '.stats-strip',
      popover: {
        title: i('tour.drills.stats.title'),
        description: `
          <p>${i('tour.drills.stats.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.drills.stats.avgTarget')}</li>
            <li>${i('tour.drills.stats.sessions')}</li>
            <li>${i('tour.drills.stats.best')}</li>
            <li>${i('tour.drills.stats.completion')}</li>
          </ul>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '.drill-row, .drill-card',
      popover: {
        title: i('tour.drills.drillRow.title'),
        description: `
          <p>${i('tour.drills.drillRow.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.drills.drillRow.name')}</li>
            <li>${i('tour.drills.drillRow.timeInTarget')}</li>
            <li>${i('tour.drills.drillRow.duration')}</li>
            <li>${i('tour.drills.drillRow.status')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.drills.drillRow.hint')}</p>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: i('tour.drills.complete.title'),
        description: `
          <p>${i('tour.drills.complete.desc')}</p>
          <p style="margin-top: 12px;">${i('tour.drills.complete.next')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
  ];
}

// ============================================
// ACHIEVEMENTS PAGE TOUR STEPS
// ============================================

function getAchievementsPageSteps(): DriveStep[] {
  const i = _();
  return [
    {
      popover: {
        title: i('tour.achievements.welcome.title'),
        description: `
          <p>${i('tour.achievements.welcome.desc')}</p>
          <p style="margin-top: 8px;">${i('tour.achievements.welcome.hint')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '.stats-strip',
      popover: {
        title: i('tour.achievements.stats.title'),
        description: `
          <p>${i('tour.achievements.stats.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.achievements.stats.achieved')}</li>
            <li>${i('tour.achievements.stats.remaining')}</li>
            <li>${i('tour.achievements.stats.progress')}</li>
            <li>${i('tour.achievements.stats.last')}</li>
          </ul>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '.milestone-card',
      popover: {
        title: i('tour.achievements.categories.title'),
        description: `
          <p>${i('tour.achievements.categories.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.achievements.categories.volume')}</li>
            <li>${i('tour.achievements.categories.balance')}</li>
            <li>${i('tour.achievements.categories.technique')}</li>
            <li>${i('tour.achievements.categories.consistency')}</li>
            <li>${i('tour.achievements.categories.training')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.achievements.categories.hint')}</p>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: i('tour.achievements.complete.title'),
        description: `
          <p>${i('tour.achievements.complete.desc')}</p>
          <p style="margin-top: 12px;">${i('tour.achievements.complete.next')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
  ];
}

// ============================================
// SETTINGS PAGE TOUR STEPS
// ============================================

function getSettingsPageSteps(): DriveStep[] {
  const i = _();
  return [
    {
      popover: {
        title: i('tour.settings.welcome.title'),
        description: `
          <p>${i('tour.settings.welcome.desc')}</p>
          <p style="margin-top: 8px;">${i('tour.settings.welcome.hint')}</p>
        `,
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '.settings-section:has(.devices-list), .settings-section:nth-child(2)',
      popover: {
        title: i('tour.settings.devices.title'),
        description: `
          <p>${i('tour.settings.devices.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.settings.devices.name')}</li>
            <li>${i('tour.settings.devices.lastSync')}</li>
            <li>${i('tour.settings.devices.requestSync')}</li>
            <li>${i('tour.settings.devices.remove')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.settings.devices.hint')}</p>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '.metrics-card:first-child',
      popover: {
        title: i('tour.settings.thresholds.title'),
        description: `
          <p>${i('tour.settings.thresholds.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.settings.thresholds.balance')}</li>
            <li>${i('tour.settings.thresholds.teRange')}</li>
            <li>${i('tour.settings.thresholds.psMin')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.settings.thresholds.hint')}</p>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '.metrics-card:nth-child(2)',
      popover: {
        title: i('tour.settings.alerts.title'),
        description: `
          <p>${i('tour.settings.alerts.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.settings.alerts.metrics')}</li>
            <li>${i('tour.settings.alerts.sensitivity')}</li>
            <li>${i('tour.settings.alerts.type')}</li>
            <li>${i('tour.settings.alerts.cooldown')}</li>
          </ul>
          <p style="color: var(--text-secondary);">${i('tour.settings.alerts.hint')}</p>
        `,
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '.theme-selector',
      popover: {
        title: i('tour.settings.theme.title'),
        description: `
          <p>${i('tour.settings.theme.desc')}</p>
          <ul style="margin: 8px 0; padding-left: 16px;">
            <li>${i('tour.settings.theme.auto')}</li>
            <li>${i('tour.settings.theme.light')}</li>
            <li>${i('tour.settings.theme.dark')}</li>
          </ul>
        `,
        side: 'bottom',
        align: 'center',
      },
    },
    {
      popover: {
        title: i('tour.settings.complete.title'),
        description: `
          <p>${i('tour.settings.complete.desc')}</p>
          <p style="margin-top: 12px;"><strong>${i('tour.settings.complete.toSeeData')}</strong></p>
          <ol style="margin: 8px 0; padding-left: 20px; line-height: 1.6;">
            <li>${i('tour.settings.complete.step1')}</li>
            <li>${i('tour.settings.complete.step2')}</li>
            <li>${i('tour.settings.complete.step3')}</li>
            <li>${i('tour.settings.complete.step4')}</li>
          </ol>
          <p style="margin-top: 12px;"><a href="https://github.com/yrkan/kpedal/releases" target="_blank" style="color: var(--color-accent); text-decoration: underline;">${i('tour.settings.complete.download')}</a></p>
        `,
        side: 'over',
        align: 'center',
      },
    },
  ];
}

// ============================================
// TOUR DRIVER INSTANCES
// ============================================

let driverInstance: Driver | null = null;

/**
 * Filter steps to only include elements that exist
 */
function filterAvailableSteps(steps: DriveStep[]): DriveStep[] {
  return steps.filter((step) => {
    if (!step.element) return true; // Modal steps always included
    const selectors = (step.element as string).split(', ');
    return selectors.some(selector => document.querySelector(selector) !== null);
  });
}

/**
 * Wait for any of the specified selectors to appear in DOM
 */
async function waitForElement(selectors: string[], timeoutMs: number = 5000): Promise<boolean> {
  const startTime = Date.now();
  const checkInterval = 100;

  while (Date.now() - startTime < timeoutMs) {
    for (const selector of selectors) {
      if (document.querySelector(selector)) {
        return true;
      }
    }
    await new Promise(resolve => setTimeout(resolve, checkInterval));
  }
  return false;
}

/**
 * Wait for loading state to finish
 */
async function waitForLoading(timeoutMs: number = 5000): Promise<boolean> {
  const startTime = Date.now();
  const checkInterval = 100;

  while (Date.now() - startTime < timeoutMs) {
    const spinner = document.querySelector('.spinner, .loading-state');
    if (!spinner) {
      return true;
    }
    await new Promise(resolve => setTimeout(resolve, checkInterval));
  }
  return false;
}

/**
 * Create a driver instance with common config
 */
async function createDriver(
  steps: DriveStep[],
  nextPage: string | null,
  isFinalTour: boolean = false
): Promise<Driver> {
  const i = _();
  const { driver } = await loadDriver();
  return driver({
    showProgress: true,
    progressText: '{{current}} / {{total}}',
    showButtons: ['next', 'previous', 'close'],
    nextBtnText: i('tour.buttons.next'),
    prevBtnText: i('tour.buttons.prev'),
    doneBtnText: nextPage ? i('tour.buttons.continue') : i('tour.buttons.done'),
    animate: true,
    overlayColor: 'rgba(0, 0, 0, 0.75)',
    stagePadding: 8,
    stageRadius: 8,
    popoverClass: 'kpedal-tour-popover',
    steps,
    onDestroyStarted: () => {
      if (nextPage) {
        setNextTourPage(nextPage);
        goto(nextPage);
      } else if (isFinalTour) {
        markTourCompleted();
      }
      driverInstance?.destroy();
    },
    onDestroyed: () => {
      driverInstance = null;
    },
  });
}

// ============================================
// PUBLIC TOUR FUNCTIONS
// ============================================

/**
 * Skip tour completely
 */
function skipTour(): void {
  if (driverInstance) {
    driverInstance.destroy();
  }
  markTourCompleted();
}

// Register global function for inline onclick
if (browser) {
  (window as unknown as { skipKPedalTour: () => void }).skipKPedalTour = skipTour;
}

/**
 * Start the dashboard tour
 */
export async function startDashboardTour(): Promise<void> {
  if (!browser) return;

  const availableSteps = filterAvailableSteps(getDashboardSteps());
  driverInstance = await createDriver(availableSteps, '/rides');
  driverInstance.drive();
}

/**
 * Start the rides page tour
 */
export async function startRidesPageTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  await waitForLoading(5000);
  await waitForElement(['.ride-row[data-ride-id]', '.ride-card[data-ride-id]', '.empty-state-enhanced'], 5000);

  const availableSteps = filterAvailableSteps(getRidesPageSteps());

  const firstRideRow = document.querySelector('.ride-row[data-ride-id], .ride-card[data-ride-id]') as HTMLElement;
  const rideId = firstRideRow?.dataset.rideId;
  const nextPage = rideId ? `/rides/${rideId}` : '/drills';

  driverInstance = await createDriver(availableSteps, nextPage);
  driverInstance.drive();
}

/**
 * Start the ride detail page tour
 */
export async function startRideDetailTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  await waitForLoading(5000);
  await waitForElement(['.hero-stats', '.error-state'], 5000);

  const availableSteps = filterAvailableSteps(getRideDetailSteps());
  driverInstance = await createDriver(availableSteps, '/drills');
  driverInstance.drive();
}

/**
 * Start the drills page tour
 */
export async function startDrillsPageTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  await waitForLoading(5000);
  await waitForElement(['.drill-row', '.drill-card', '.empty-state-enhanced'], 5000);

  const availableSteps = filterAvailableSteps(getDrillsPageSteps());
  driverInstance = await createDriver(availableSteps, '/achievements');
  driverInstance.drive();
}

/**
 * Start the achievements page tour
 */
export async function startAchievementsPageTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  await waitForLoading(5000);
  await waitForElement(['.milestone-card', '.empty-state-enhanced'], 5000);

  const availableSteps = filterAvailableSteps(getAchievementsPageSteps());
  driverInstance = await createDriver(availableSteps, '/settings');
  driverInstance.drive();
}

/**
 * Start the settings page tour
 */
export async function startSettingsPageTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  await waitForLoading(5000);
  await waitForElement(['.settings-section', '.theme-selector'], 3000);

  const availableSteps = filterAvailableSteps(getSettingsPageSteps());
  driverInstance = await createDriver(availableSteps, null, true);
  driverInstance.drive();
}

/**
 * Continue tour on current page
 */
export async function continueTourOnPage(pathname: string): Promise<void> {
  if (!browser) return;

  const nextPage = getNextTourPage();
  if (!nextPage) return;

  if (!pathname.startsWith(nextPage.replace(/\/\[.*\]/, ''))) {
    goto(nextPage);
    return;
  }

  await new Promise(resolve => setTimeout(resolve, 100));

  if (pathname === '/rides') {
    await startRidesPageTour();
  } else if (pathname.startsWith('/rides/')) {
    await startRideDetailTour();
  } else if (pathname === '/drills') {
    await startDrillsPageTour();
  } else if (pathname === '/achievements') {
    await startAchievementsPageTour();
  } else if (pathname === '/settings') {
    await startSettingsPageTour();
  }
}

/**
 * Stop the tour if running
 */
export function stopTour(): void {
  if (driverInstance) {
    driverInstance.destroy();
    driverInstance = null;
  }
}

/**
 * Check if tour is currently active
 */
export function isTourActive(): boolean {
  return driverInstance !== null && driverInstance.isActive();
}

/**
 * Check if there's a pending tour continuation
 */
export function hasPendingTour(): boolean {
  return getNextTourPage() !== null;
}
