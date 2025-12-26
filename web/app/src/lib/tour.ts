/**
 * KPedal Comprehensive Product Tour
 * Super detailed guided onboarding for demo users using driver.js
 * Covers: Dashboard, Rides, Ride Details, Drills, Achievements, Settings
 */

import { driver, type DriveStep, type Driver } from 'driver.js';
import 'driver.js/dist/driver.css';
import { browser } from '$app/environment';
import { goto } from '$app/navigation';

const TOUR_STORAGE_KEY = 'kpedal_tour_completed';
const TOUR_PAGE_KEY = 'kpedal_tour_page';

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

const dashboardSteps: DriveStep[] = [
  // Welcome modal
  {
    popover: {
      title: 'Welcome to KPedal! 👋',
      description: `
        <p>This is <strong>demo data from a real cyclist</strong> showing ~4 weeks of rides.</p>
        <p style="margin-top: 8px;">Let me give you a complete tour of everything KPedal can do. It'll take about 2 minutes.</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
  // Period selector
  {
    element: '.period-selector',
    popover: {
      title: 'Time Period Selector',
      description: `
        <p>Switch between <strong>7, 14, 30, or 60 days</strong> to analyze your trends over different timeframes.</p>
        <p style="margin-top: 8px; color: var(--text-secondary);">Shorter periods show recent form. Longer periods reveal training patterns.</p>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // Power Asymmetry stat
  {
    element: '.hero-stat.asymmetry',
    popover: {
      title: 'Power Asymmetry',
      description: `
        <p>Your deviation from perfect 50/50 L/R balance.</p>
        <ul style="margin: 8px 0; padding-left: 16px; line-height: 1.6;">
          <li><strong style="color: var(--color-optimal)">Under 2.5%</strong> — Pro level</li>
          <li><strong style="color: var(--color-attention)">2.5-5%</strong> — Good, room to improve</li>
          <li><strong style="color: var(--color-problem)">Over 5%</strong> — Needs attention</li>
        </ul>
        <p style="color: var(--text-secondary);">This demo cyclist averages 1-2% — excellent symmetry!</p>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // InfoTip highlight
  {
    element: '.hero-stat.asymmetry .info-tip-trigger',
    popover: {
      title: 'Info Tips Everywhere',
      description: `
        <p>See this <strong>ⓘ icon</strong>? Hover or tap it anytime for detailed explanations.</p>
        <p style="margin-top: 8px; color: var(--text-secondary);">Every metric in KPedal has an info tip explaining what it means and what to aim for.</p>
      `,
      side: 'right',
      align: 'center',
    },
  },
  // Balance visual
  {
    element: '.hero-stat.balance-visual',
    popover: {
      title: 'L/R Power Balance',
      description: `
        <p>Visual breakdown of your left vs right leg power distribution.</p>
        <p style="margin-top: 8px;"><strong>Green zone</strong> = balanced. The bar shows exactly how your power splits between legs.</p>
        <p style="margin-top: 8px; color: var(--text-secondary);">Most cyclists are slightly dominant on one side. Track this to improve symmetry over time.</p>
      `,
      side: 'bottom',
      align: 'center',
    },
  },
  // Time in zones
  {
    element: '.hero-stat.zones',
    popover: {
      title: 'Time in Zone Distribution',
      description: `
        <p>How your ride time breaks down by technique quality:</p>
        <ul style="margin: 8px 0; padding-left: 16px; line-height: 1.6;">
          <li><strong style="color: var(--color-optimal)">Green (Optimal)</strong> — All metrics in ideal range</li>
          <li><strong style="color: var(--color-attention)">Yellow (Attention)</strong> — One or more metrics drifting</li>
          <li><strong style="color: var(--color-problem)">Red (Problem)</strong> — Significant form breakdown</li>
        </ul>
        <p style="color: var(--text-secondary);">Goal: Maximize green time, especially in long rides!</p>
      `,
      side: 'bottom',
      align: 'center',
    },
  },
  // Summary stats
  {
    element: '.hero-stat.summary',
    popover: {
      title: 'Activity Summary',
      description: `
        <p>Quick glance at your training volume for the selected period:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Total rides</strong> completed</li>
          <li><strong>Hours</strong> in the saddle</li>
          <li><strong>Kilometers</strong> covered</li>
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
      title: 'Weekly Activity Chart',
      description: `
        <p>See your ride frequency per day of the week.</p>
        <p style="margin-top: 8px;">Below the chart, compare <strong>this week vs last week</strong>:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li>Number of rides</li>
          <li>Total hours</li>
          <li>Distance and climbing</li>
        </ul>
        <p style="color: var(--text-secondary);">Consistency is key — aim for regular training, not sporadic big efforts.</p>
      `,
      side: 'right',
      align: 'start',
    },
  },
  // Technique card - TE/PS gauges
  {
    element: '.main-grid .grid-card:last-child',
    popover: {
      title: 'Technique Metrics',
      description: `
        <p>The two core efficiency metrics from your power meter:</p>
        <p style="margin-top: 8px;"><strong>Torque Effectiveness (TE)</strong><br/>
        Percentage of power going forward. Aim for <strong>70-80%</strong> — higher isn't always better per Wattbike research.</p>
        <p style="margin-top: 8px;"><strong>Pedal Smoothness (PS)</strong><br/>
        How even your power is through the pedal stroke. Aim for <strong>≥20%</strong>. Elite cyclists hit 25%+.</p>
      `,
      side: 'left',
      align: 'start',
    },
  },
  // Trend charts
  {
    element: '.trends-row',
    popover: {
      title: 'Trends Over Time',
      description: `
        <p>Track your balance and technique improvements ride by ride.</p>
        <p style="margin-top: 8px;"><strong>Left chart:</strong> Balance trend across rides</p>
        <p><strong>Right chart:</strong> Switch between TE, PS, and Asymmetry</p>
        <p style="margin-top: 8px; color: var(--text-secondary);">Click any point to jump to that ride's detailed analysis.</p>
      `,
      side: 'top',
      align: 'center',
    },
  },
  // Recent rides table
  {
    element: '.recent-rides',
    popover: {
      title: 'Recent Rides Table',
      description: `
        <p>All your synced rides with key metrics at a glance:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li>Date, duration, and score</li>
          <li>Asymmetry percentage</li>
          <li>L/R breakdown for TE and PS</li>
          <li>Zone distribution bars</li>
        </ul>
        <p style="margin-top: 8px; color: var(--text-secondary);">Click any row to see detailed minute-by-minute analysis!</p>
      `,
      side: 'top',
      align: 'center',
    },
  },
  // Navigation hint
  {
    element: 'nav',
    popover: {
      title: 'Navigation',
      description: `
        <p>Explore other sections:</p>
        <ul style="margin: 8px 0; padding-left: 16px; line-height: 1.6;">
          <li><strong>Rides</strong> — Full history with filters and search</li>
          <li><strong>Drills</strong> — Guided training exercises</li>
          <li><strong>Achievements</strong> — Milestones and badges</li>
          <li><strong>Settings</strong> — Devices, thresholds, and alerts</li>
        </ul>
      `,
      side: 'bottom',
      align: 'center',
    },
  },
  // End of dashboard, prompt for Rides page
  {
    popover: {
      title: 'Dashboard Complete! 🎉',
      description: `
        <p>You've seen the dashboard overview. Ready to explore more?</p>
        <p style="margin-top: 12px;">Click <strong>Next</strong> to continue the tour on the <strong>Rides</strong> page, where you'll see your full ride history.</p>
        <p style="margin-top: 8px; color: var(--text-secondary);">Or click Done to end the tour here.</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
];

// ============================================
// RIDES PAGE TOUR STEPS
// ============================================

const ridesPageSteps: DriveStep[] = [
  // Welcome to rides
  {
    popover: {
      title: 'Rides History 🚴',
      description: `
        <p>This page shows your <strong>complete ride history</strong> with filtering and sorting.</p>
        <p style="margin-top: 8px;">Let me show you the key features.</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
  // Stats strip
  {
    element: '.stats-strip',
    popover: {
      title: 'Period Statistics',
      description: `
        <p>Quick stats for the selected period:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li>Total rides and duration</li>
          <li>Average and best scores</li>
          <li>Power and distance totals</li>
        </ul>
        <p style="color: var(--text-secondary);">These update based on your filters.</p>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // View toggle buttons
  {
    element: '.view-toggle',
    popover: {
      title: 'View Toggle 📋↔️🃏',
      description: `
        <p>Switch between two view modes:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Table View</strong> — Compact list with all metrics</li>
          <li><strong>Card View</strong> — Visual cards with zone bars</li>
        </ul>
        <p style="color: var(--text-secondary);">Choose what works best for you!</p>
      `,
      side: 'bottom',
      align: 'end',
    },
  },
  // Ride row/card
  {
    element: '.ride-row[data-ride-id], .ride-card[data-ride-id]',
    popover: {
      title: 'Click Any Ride',
      description: `
        <p>Each ride shows:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li>Date, duration, and technique score</li>
          <li>Power asymmetry</li>
          <li>L/R metrics for TE and PS</li>
          <li>Zone distribution</li>
        </ul>
        <p style="margin-top: 8px;"><strong>Click any ride</strong> to see the full detail page with minute-by-minute charts!</p>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // End of rides, prompt for ride detail
  {
    popover: {
      title: 'Rides Page Complete!',
      description: `
        <p>You can browse all your rides here.</p>
        <p style="margin-top: 12px;">Click <strong>Next</strong> to see a <strong>ride detail page</strong> with in-depth analysis.</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
];

// ============================================
// RIDE DETAIL PAGE TOUR STEPS
// ============================================

const rideDetailSteps: DriveStep[] = [
  // Welcome
  {
    popover: {
      title: 'Ride Detail Analysis 📊',
      description: `
        <p>This is where the <strong>deep analysis</strong> happens.</p>
        <p style="margin-top: 8px;">Every ride gets a complete breakdown of your pedaling technique minute by minute.</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
  // Hero stats section
  {
    element: '.hero-stats',
    popover: {
      title: 'Key Metrics at a Glance',
      description: `
        <p>The top row shows your most important ride metrics:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Score</strong> — Overall technique rating (0-100)</li>
          <li><strong>Asymmetry</strong> — L/R power imbalance</li>
          <li><strong>Balance Bar</strong> — Visual L/R split</li>
          <li><strong>Time in Zone</strong> — Quality distribution</li>
        </ul>
      `,
      side: 'bottom',
      align: 'center',
    },
  },
  // Performance strip
  {
    element: '.perf-strip',
    popover: {
      title: 'Ride Performance Data',
      description: `
        <p>Detailed ride metrics:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Power</strong> — Avg, max, and normalized</li>
          <li><strong>Heart Rate</strong> — Avg and max</li>
          <li><strong>Cadence</strong> — Pedaling speed in RPM</li>
          <li><strong>Elevation</strong> — Climbing and descending</li>
        </ul>
        <p style="color: var(--text-secondary);">Tap any info icon for detailed explanations!</p>
      `,
      side: 'bottom',
      align: 'center',
    },
  },
  // Technique card
  {
    element: '.technique-card',
    popover: {
      title: 'Technique Breakdown',
      description: `
        <p>Your pedaling efficiency metrics:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>TE (Torque Effectiveness)</strong> — Power going forward. Aim for 70-80%</li>
          <li><strong>PS (Pedal Smoothness)</strong> — Stroke evenness. Aim for ≥20%</li>
          <li><strong>L/R breakdown</strong> — See each leg's contribution</li>
        </ul>
        <p style="color: var(--text-secondary);">Large L/R differences indicate muscle imbalance or bike fit issues.</p>
      `,
      side: 'right',
      align: 'start',
    },
  },
  // Fatigue card
  {
    element: '.fatigue-card',
    popover: {
      title: 'Fatigue Analysis',
      description: `
        <p>How your technique changed from start to end:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Start → End</strong> values for each metric</li>
          <li><strong>Green</strong> = improved, <strong>Red</strong> = degraded</li>
          <li><strong>Badge</strong> — STRONG (no drops), MODERATE, or WEAK</li>
        </ul>
        <p style="color: var(--text-secondary);">Consistent technique under fatigue is key to long-ride performance!</p>
      `,
      side: 'left',
      align: 'start',
    },
  },
  // Timeline chart
  {
    element: '.timeline-card',
    popover: {
      title: 'Minute-by-Minute Timeline',
      description: `
        <p>Track how metrics changed throughout your ride:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Tabs</strong> — Switch between Balance, TE, PS, Power, HR</li>
          <li><strong>Points</strong> — Each dot is one minute</li>
          <li><strong>Hover</strong> — See exact values at any point</li>
        </ul>
        <p style="color: var(--text-secondary);">Watch for patterns — does technique drop after 30 min? That's fatigue!</p>
      `,
      side: 'top',
      align: 'center',
    },
  },
  // End of ride detail
  {
    popover: {
      title: 'Ride Detail Complete!',
      description: `
        <p>You now know how to analyze individual rides in depth.</p>
        <p style="margin-top: 12px;">Click <strong>Next</strong> to explore the <strong>Drills</strong> page — guided exercises to improve your technique.</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
];

// ============================================
// DRILLS PAGE TOUR STEPS
// ============================================

const drillsPageSteps: DriveStep[] = [
  // Welcome
  {
    popover: {
      title: 'Training Drills 🏋️',
      description: `
        <p><strong>Drills</strong> are guided training exercises you do on your Karoo to improve specific skills.</p>
        <p style="margin-top: 8px;">Results sync here so you can track your progress!</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
  // Stats strip
  {
    element: '.stats-strip',
    popover: {
      title: 'Drill Statistics',
      description: `
        <p>Your overall drill performance:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Avg Target</strong> — How well you hit target zones</li>
          <li><strong>Sessions</strong> — Total drills completed</li>
          <li><strong>Best</strong> — Your highest time-in-target score</li>
          <li><strong>Completion rate</strong> — % finished vs abandoned</li>
        </ul>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // Drill row/card
  {
    element: '.drill-row, .drill-card',
    popover: {
      title: 'Drill Results',
      description: `
        <p>Each drill session shows:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Drill name</strong> — What type of exercise</li>
          <li><strong>Time in Target</strong> — % of time you hit the goal</li>
          <li><strong>Duration</strong> — How long you practiced</li>
          <li><strong>Status</strong> — Completed or stopped early</li>
        </ul>
        <p style="color: var(--text-secondary);">Aim for 80%+ time in target as you improve!</p>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // End of drills
  {
    popover: {
      title: 'Drills Page Complete!',
      description: `
        <p>Drills help you deliberately practice specific skills.</p>
        <p style="margin-top: 12px;">Click <strong>Next</strong> to see <strong>Achievements</strong> — milestones you can unlock!</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
];

// ============================================
// ACHIEVEMENTS PAGE TOUR STEPS
// ============================================

const achievementsPageSteps: DriveStep[] = [
  // Welcome
  {
    popover: {
      title: 'Achievements & Milestones 🏆',
      description: `
        <p>Unlock achievements as you train and improve!</p>
        <p style="margin-top: 8px;">These track your progress across volume, technique, and consistency.</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
  // Stats strip
  {
    element: '.stats-strip',
    popover: {
      title: 'Progress Overview',
      description: `
        <p>Your achievement progress:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Achieved</strong> — Unlocked milestones</li>
          <li><strong>Remaining</strong> — Still to unlock</li>
          <li><strong>Progress %</strong> — Overall completion</li>
          <li><strong>Last</strong> — Most recent unlock date</li>
        </ul>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // Achievement row/card
  {
    element: '.milestone-card',
    popover: {
      title: 'Achievement Categories',
      description: `
        <p>Achievements span multiple categories:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Volume</strong> — Ride counts (10, 50, 100+)</li>
          <li><strong>Balance</strong> — L/R symmetry goals</li>
          <li><strong>Technique</strong> — Score milestones (80+, 90+)</li>
          <li><strong>Consistency</strong> — Training streaks</li>
          <li><strong>Training</strong> — Drill completions</li>
        </ul>
        <p style="color: var(--text-secondary);">Achieved milestones show with a green checkmark!</p>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // End of achievements
  {
    popover: {
      title: 'Achievements Complete!',
      description: `
        <p>Keep riding and training to unlock more milestones!</p>
        <p style="margin-top: 12px;">Click <strong>Next</strong> for the final stop: <strong>Settings</strong>.</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
];

// ============================================
// SETTINGS PAGE TOUR STEPS
// ============================================

const settingsPageSteps: DriveStep[] = [
  // Welcome
  {
    popover: {
      title: 'Settings & Preferences ⚙️',
      description: `
        <p>Configure KPedal to work exactly how you want.</p>
        <p style="margin-top: 8px;">Settings sync between web and your Karoo device!</p>
      `,
      side: 'over',
      align: 'center',
    },
  },
  // Connected devices
  {
    element: '.settings-section:has(.devices-list), .settings-section:nth-child(2)',
    popover: {
      title: 'Connected Devices',
      description: `
        <p>See all your linked Karoo devices:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li>Device name and status</li>
          <li>Last sync time</li>
          <li><strong>Request Sync</strong> to pull data from device</li>
          <li><strong>Remove</strong> to unlink a device</li>
        </ul>
        <p style="color: var(--text-secondary);">You can link multiple Karoos to one account.</p>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // Pedaling metrics thresholds
  {
    element: '.metrics-card:first-child',
    popover: {
      title: 'Metric Thresholds',
      description: `
        <p>Customize what "optimal" means for you:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Balance Threshold</strong> — How much L/R imbalance triggers yellow/red</li>
          <li><strong>TE Optimal Range</strong> — Your target torque effectiveness zone</li>
          <li><strong>PS Minimum</strong> — Pedal smoothness floor</li>
        </ul>
        <p style="color: var(--text-secondary);">Defaults are based on cycling research, but you can adjust to your level.</p>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // In-ride alerts
  {
    element: '.metrics-card:nth-child(2)',
    popover: {
      title: 'In-Ride Alerts',
      description: `
        <p>Get notified during rides when metrics need attention:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li>Choose which metrics to monitor</li>
          <li>Set sensitivity (critical only or sensitive)</li>
          <li>Pick notification type (vibrate, sound, banner)</li>
          <li>Set cooldown between alerts</li>
        </ul>
        <p style="color: var(--text-secondary);">Alerts are shown directly on your Karoo during the ride!</p>
      `,
      side: 'bottom',
      align: 'start',
    },
  },
  // Theme
  {
    element: '.theme-selector',
    popover: {
      title: 'Appearance',
      description: `
        <p>Choose your preferred color scheme:</p>
        <ul style="margin: 8px 0; padding-left: 16px;">
          <li><strong>Light</strong> — Clean white background</li>
          <li><strong>Dark</strong> — Easy on the eyes</li>
          <li><strong>System</strong> — Follows your OS setting</li>
        </ul>
      `,
      side: 'bottom',
      align: 'center',
    },
  },
  // Tour complete!
  {
    popover: {
      title: "You're All Set! 🚴‍♂️",
      description: `
        <p>You've completed the full KPedal tour!</p>
        <p style="margin-top: 12px;"><strong>To see your own data:</strong></p>
        <ol style="margin: 8px 0; padding-left: 20px; line-height: 1.6;">
          <li>Download KPedal on your Karoo</li>
          <li>Add it to your ride screen</li>
          <li>Go for a ride!</li>
          <li>Data syncs automatically</li>
        </ol>
        <p style="margin-top: 12px;"><a href="https://github.com/yrkan/kpedal/releases" target="_blank" style="color: var(--color-accent); text-decoration: underline;">Download KPedal →</a></p>
      `,
      side: 'over',
      align: 'center',
    },
  },
];

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
 * Returns true if found, false if timeout
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
 * Wait for loading state to finish (no spinner visible)
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
function createDriver(
  steps: DriveStep[],
  nextPage: string | null,
  isFinalTour: boolean = false
): Driver {
  return driver({
    showProgress: true,
    progressText: '{{current}} / {{total}}',
    showButtons: ['next', 'previous', 'close'],
    nextBtnText: 'Next',
    prevBtnText: 'Back',
    doneBtnText: nextPage ? 'Continue →' : 'Done',
    animate: true,
    overlayColor: 'rgba(0, 0, 0, 0.75)',
    stagePadding: 8,
    stageRadius: 8,
    popoverClass: 'kpedal-tour-popover',
    steps,
    onDestroyStarted: () => {
      if (nextPage) {
        setNextTourPage(nextPage);
        // Navigate to next page
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
 * Start the dashboard tour
 */
export function startDashboardTour(): void {
  if (!browser) return;

  const availableSteps = filterAvailableSteps(dashboardSteps);
  driverInstance = createDriver(availableSteps, '/rides');
  driverInstance.drive();
}

/**
 * Start the rides page tour
 */
export async function startRidesPageTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  // Wait for data to load (ride rows or cards to appear)
  await waitForLoading(5000);
  await waitForElement(['.ride-row[data-ride-id]', '.ride-card[data-ride-id]', '.empty-state-enhanced'], 5000);

  const availableSteps = filterAvailableSteps(ridesPageSteps);

  // Find first ride ID for detail tour
  const firstRideRow = document.querySelector('.ride-row[data-ride-id], .ride-card[data-ride-id]') as HTMLElement;
  const rideId = firstRideRow?.dataset.rideId;
  const nextPage = rideId ? `/rides/${rideId}` : '/drills';

  driverInstance = createDriver(availableSteps, nextPage);
  driverInstance.drive();
}

/**
 * Start the ride detail page tour
 */
export async function startRideDetailTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  // Wait for ride data to load (hero stats to appear)
  await waitForLoading(5000);
  await waitForElement(['.hero-stats', '.error-state'], 5000);

  const availableSteps = filterAvailableSteps(rideDetailSteps);
  driverInstance = createDriver(availableSteps, '/drills');
  driverInstance.drive();
}

/**
 * Start the drills page tour
 */
export async function startDrillsPageTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  // Wait for drills data to load
  await waitForLoading(5000);
  await waitForElement(['.drill-row', '.drill-card', '.empty-state-enhanced'], 5000);

  const availableSteps = filterAvailableSteps(drillsPageSteps);
  driverInstance = createDriver(availableSteps, '/achievements');
  driverInstance.drive();
}

/**
 * Start the achievements page tour
 */
export async function startAchievementsPageTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  // Wait for achievements data to load
  await waitForLoading(5000);
  await waitForElement(['.milestone-card', '.empty-state-enhanced'], 5000);

  const availableSteps = filterAvailableSteps(achievementsPageSteps);
  driverInstance = createDriver(availableSteps, '/settings');
  driverInstance.drive();
}

/**
 * Start the settings page tour
 */
export async function startSettingsPageTour(): Promise<void> {
  if (!browser) return;
  clearNextTourPage();

  // Wait for settings to load
  await waitForLoading(5000);
  await waitForElement(['.settings-section', '.theme-selector'], 3000);

  const availableSteps = filterAvailableSteps(settingsPageSteps);
  driverInstance = createDriver(availableSteps, null, true); // Final tour
  driverInstance.drive();
}

/**
 * Continue tour on current page (called from page components)
 */
export async function continueTourOnPage(pathname: string): Promise<void> {
  if (!browser) return;

  const nextPage = getNextTourPage();
  if (!nextPage) return;

  // Check if we're on the expected page
  if (!pathname.startsWith(nextPage.replace(/\/\[.*\]/, ''))) {
    // Navigate to expected page
    goto(nextPage);
    return;
  }

  // Small delay for initial render, then each tour function handles its own data loading wait
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
