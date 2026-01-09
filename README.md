# KPedal

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/yrkan/KPedal/total)](https://github.com/yrkan/KPedal/releases)
[![Platform: Karoo](https://img.shields.io/badge/Platform-Karoo%202%2F3-blue.svg)](https://www.hammerhead.io/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF.svg)](https://kotlinlang.org/)
[![AGP](https://img.shields.io/badge/AGP-8.13.2-3DDC84.svg)](https://developer.android.com/build)
[![API: Cloudflare Workers](https://img.shields.io/badge/API-Cloudflare%20Workers-F38020.svg)](https://workers.cloudflare.com/)
[![Frontend: SvelteKit](https://img.shields.io/badge/Frontend-SvelteKit%205-FF3E00.svg)](https://kit.svelte.dev/)

Real-time pedaling efficiency extension for Hammerhead Karoo 2/3. Displays Balance, Torque Effectiveness, and Pedal Smoothness metrics from ANT+ Cycling Dynamics power meter pedals.

<p align="center">
  <a href="https://kpedal.com">Website</a> •
  <a href="https://start.kpedal.com">Get Started</a> •
  <a href="https://kpedal.com/login">Try Demo</a> •
  <a href="https://app.kpedal.com">Web App</a> •
  <a href="#installation">Installation</a> •
  <a href="https://kpedal.com/privacy">Privacy</a>
</p>

---

## What's New in v1.8

| Category | Change |
|----------|--------|
| **+12 DataTypes** | Pedaling Score, Fatigue Indicator, Cadence Balance, Delta Average, Left/Right Leg, Climbing Mode, Symmetry Index, HR Efficiency, Power Focus, Sprint Mode, Compact Multi |
| **Glance Migration** | All DataTypes rebuilt with Jetpack Glance — eliminates crash after 10+ min rides |
| **Sync Reliability** | Failed items (syncStatus=2) now auto-retry on next trigger |
| **Stability** | Fixed RemoteViews action accumulation causing TransactionTooLargeException |

---

## Table of Contents

- [What's New in v1.8](#whats-new-in-v18)
- [Overview](#overview)
- [Quick Start](#quick-start)
- [Pedaling Metrics](#pedaling-metrics)
- [Karoo App Features](#karoo-app-features)
- [Web Portal](#web-portal)
  - [Demo Account](#demo-account)
  - [Privacy & Security](#privacy--security)
- [Compatible Pedals](#compatible-pedals)
- [Installation](#installation)
- [Development](#development)
  - [Tech Stack](#tech-stack)
  - [Testing](#testing)
  - [API Performance](#api-performance)
- [Troubleshooting](#troubleshooting)
- [Requirements](#requirements)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

KPedal provides cyclists with real-time feedback on pedaling technique during rides. The extension displays data fields on Karoo ride screens and includes a companion app for training drills, ride history, analytics, and achievement tracking.

### Key Features

| Feature | Description |
|---------|-------------|
| **Background Mode** | Collects pedaling data for ALL rides, even without KPedal data fields on screen |
| **Cloud Sync** | Sync rides, drills, achievements and settings to [app.kpedal.com](https://app.kpedal.com) |
| **19 Data Layouts** | Customizable data fields with adaptive sizing (6 size categories) |
| **Real-time Alerts** | Vibration, sound, and visual feedback when technique needs attention |
| **10 Training Drills** | Built-in drills with guided phases and scoring |
| **Custom Drills** | Create personalized drills with configurable targets |
| **Ride History** | Automatic recording with per-minute snapshots |
| **Analytics** | 7-day and 30-day trend charts with progress tracking |
| **16 Achievements** | Unlock badges across 5 categories |
| **7 Weekly Challenges** | Rotating goals based on ISO week number |
| **Web Dashboard** | Full-featured portal with ride details, charts, device management |
| **Demo Mode** | Try the web portal without sign-up — explore with sample data |
| **Guided Tour** | Interactive 39-step walkthrough of all web portal features |
| **Multi-language** | 17 languages: EN, ES, DE, FR, IT, PT, NL, JA, ZH, UK, RU, HE, AR, PL, KO, DA, SV |

### System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            KPedal Ecosystem                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐                                                       │
│  │   Power Meter    │                                                       │
│  │   Pedals (ANT+)  │                                                       │
│  └────────┬─────────┘                                                       │
│           │ ANT+ Cycling Dynamics                                           │
│           ▼                                                                 │
│  ┌──────────────────────────────────────────────────────────────────┐       │
│  │                    Hammerhead Karoo 2/3                          │       │
│  │  ┌────────────────────────────────────────────────────────────┐  │       │
│  │  │                   KPedal Android App                       │  │       │
│  │  │                                                            │  │       │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │  │       │
│  │  │  │ PedalEngine │→ │AlertManager │→ │ 19 Glance DataTypes │ │  │       │
│  │  │  └─────────────┘  └─────────────┘  └─────────────────────┘ │  │       │
│  │  │         │                                                  │  │       │
│  │  │         ▼                                                  │  │       │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │  │       │
│  │  │  │LiveCollector│  │ DrillEngine │  │   Room Database     │ │  │       │
│  │  │  └─────────────┘  └─────────────┘  └─────────────────────┘ │  │       │
│  │  │         │                │                    │            │  │       │
│  │  │         └────────────────┴────────────────────┘            │  │       │
│  │  │                          │                                 │  │       │
│  │  │                   ┌──────┴───────┐                         │  │       │
│  │  │                   │  SyncService │                         │  │       │
│  │  │                   └──────┬───────┘                         │  │       │
│  │  └────────────────────────────────────────────────────────────┘  │       │
│  └──────────────────────────────┬───────────────────────────────────┘       │
│                                 │ HTTPS                                     │
│                                 ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐       │
│  │                    api.kpedal.com                                │       │
│  │                  Cloudflare Workers                              │       │
│  │  ┌────────────────────────────────────────────────────────────┐  │       │
│  │  │  Hono Framework │ JWT Auth │ Rate Limiting │ Validation    │  │       │
│  │  └────────────────────────────────────────────────────────────┘  │       │
│  │                                 │                                │       │
│  │         ┌───────────────────────┼───────────────────────┐        │       │
│  │         ▼                       ▼                       ▼        │       │
│  │  ┌────────────┐          ┌────────────┐          ┌────────────┐  │       │
│  │  │ D1 (SQLite)│          │  KV Store  │          │   Google   │  │       │
│  │  │  Database  │          │  Sessions  │          │   OAuth    │  │       │
│  │  └────────────┘          └────────────┘          └────────────┘  │       │
│  └──────────────────────────────────────────────────────────────────┘       │
│                                 │                                           │
│                                 ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐       │
│  │                    app.kpedal.com                                │       │
│  │                   Cloudflare Pages                               │       │
│  │  ┌────────────────────────────────────────────────────────────┐  │       │
│  │  │           SvelteKit 5 │ Responsive UI │ Dark Mode          │  │       │
│  │  └────────────────────────────────────────────────────────────┘  │       │
│  └──────────────────────────────────────────────────────────────────┘       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Start

### Try the Demo First

Want to see KPedal in action before installing?

1. Visit [kpedal.com/login](https://kpedal.com/login)
2. Click **Try Demo**
3. Follow the **interactive guided tour** (~2 minutes)

The tour walks you through all 6 pages with 39 steps covering every feature — from dashboard metrics to ride analysis to settings. No sign-up required, just click and explore!

### 1. Install on Karoo

```bash
# Via ADB
adb install kpedal-x.x.x.apk

# Or install directly via Hammerhead Companion
```

### 2. Add Data Fields

1. Open **Profiles** on Karoo
2. Edit your profile → Add data page
3. Select **More Data** → **kpedal**
4. Choose a layout (Quick Glance recommended for racing)

### 3. Link to Cloud (Optional)

1. In app **Settings** → **Link Account**
2. Note the 8-character code (e.g., `ABCD-1234`)
3. Visit [link.kpedal.com](https://link.kpedal.com) on any device
4. Enter code and sign in with Google
5. Done! Rides will sync automatically

---

## Pedaling Metrics

### Core Metrics

| Metric | Description | Optimal Range | Research Basis |
|--------|-------------|---------------|----------------|
| **Balance** | Left/Right power distribution | 48-52% | Pro cyclists maintain ±2% |
| **TE** | Torque Effectiveness - ratio of positive to total torque | 70-80% | Wattbike: >80% reduces total power |
| **PS** | Pedal Smoothness - evenness of power delivery | ≥20% | Elite cyclists: 25-35% |

### Status Colors

| Color | Status | Balance | TE | PS |
|:-----:|--------|:-------:|:--:|:--:|
| ⚪ White | Normal | 45-55% | 60-90% | 15-19% |
| 🟢 Green | Optimal | 48-52% | 70-80% | ≥20% |
| 🟡 Yellow | Attention | 45-47% or 53-55% | 60-69% or 81-90% | 15-19% |
| 🔴 Red | Problem | <45% or >55% | <60% or >90% | <15% |

### Research Notes

<details>
<summary><b>Balance</b> - Click to expand</summary>

- Pro cyclists: 48-52% (±2%)
- Amateur cyclists: 45-55% is normal
- 2-5% asymmetry is natural and changes with fatigue
- Complete symmetry (50/50) is not required
- Asymmetry often increases with fatigue - useful for pacing

</details>

<details>
<summary><b>Torque Effectiveness</b> - Click to expand</summary>

- **80%+ is NOT better** - this is a common misconception
- Above 80%, power on the main downstroke phase decreases
- Optimal zone: 70-80% (based on Wattbike research)
- Very high TE means "stealing" power from your strongest phase
- Focus on being in the 70-80% range, not maximizing

</details>

<details>
<summary><b>Pedal Smoothness</b> - Click to expand</summary>

- Good cyclists: 20-30%
- Elite cyclists: 25-35%
- 100% would be perfectly circular motion (unrealistic and inefficient)
- Lower values during sprints and high power is normal
- Increases naturally with lower power and higher cadence

</details>

---

## Karoo App Features

### Data Field Layouts

19 layouts available for Karoo ride screens, built with **Jetpack Glance** for smooth 1Hz updates and crash-free operation. Each layout **automatically adapts** to 6 size categories based on grid dimensions:

| Size | Grid Type | Height | Description |
|------|-----------|:------:|-------------|
| **LARGE** | Full width | 250px+ | Full details with progress bars, zones, averages |
| **MEDIUM_WIDE** | Full width | 160-250px | Compact wide with essential metrics |
| **SMALL_WIDE** | Full width | <160px | Minimal wide, single row of metrics |
| **MEDIUM** | Half width | 200px+ | Balanced layout with L/R values |
| **SMALL** | Half width | <200px | Compact essential metrics |
| **NARROW** | Half width tall | 600px+ | Tall narrow, optimized for 2-column layouts |

#### Layout Catalog

**Core Layouts (7):**

| Layout | Type ID | Best For | Features |
|--------|---------|----------|----------|
| **Quick Glance** | `quick-glance` | Racing, minimal distraction | Status text ("Bal, TE" when issues) + balance bar |
| **Power + Balance** | `power-balance` | Balance training | Large L/R percentage, balance bar, asymmetry indicator |
| **Efficiency** | `efficiency` | TE/PS focus | L/R TE and PS values, averages on large size |
| **Full Overview** | `full-overview` | All-around monitoring | Balance + TE + PS in compact grid |
| **Balance Trend** | `balance-trend` | Trend analysis | Current, 3s avg, 10s avg with trend arrows |
| **Balance** | `single-balance` | Full-screen balance | Large L/R with visual bar |
| **Live** | `live` | Ride analysis | All metrics + Time In Zone percentages |

**New Layouts (12) — v1.8:**

| Layout | Type ID | Best For | Features |
|--------|---------|----------|----------|
| **Pedaling Score** | `pedaling-score` | Overall technique | Real-time ride score (0-100) with star rating |
| **Fatigue Indicator** | `fatigue-indicator` | Endurance rides | Tracks technique degradation, balance/efficiency deltas |
| **Cadence Balance** | `cadence-balance` | Cadence drills | Balance at different cadences, correlation analysis |
| **Delta Average** | `delta-average` | Progress tracking | Current vs ride average difference for all metrics |
| **Left Leg** | `left-leg` | Leg-specific training | Left leg power %, TE, PS in focused layout |
| **Right Leg** | `right-leg` | Leg-specific training | Right leg power %, TE, PS in focused layout |
| **Climbing Mode** | `climbing-mode` | Hill climbs | Optimized for low cadence, high torque efforts |
| **Symmetry Index** | `symmetry-index` | Balance precision | 0-100% symmetry score with visual indicator |
| **HR Efficiency** | `hr-efficiency` | Training efficiency | Heart rate + TE/PS for cardiovascular analysis |
| **Power Focus** | `power-focus` | Power training | Power + balance + efficiency combined |
| **Sprint Mode** | `sprint-mode` | Sprints, attacks | High-contrast layout for maximum output efforts |
| **Compact Multi** | `compact-multi` | Dense information | All key metrics in ultra-compact grid format |

#### Layout Selection Guide

| Use Case | Recommended | Why |
|----------|-------------|-----|
| Racing / criteriums | Quick Glance | Minimal distraction, only alerts when needed |
| Balance training | Power + Balance, Balance Trend | Focus on L/R with trend information |
| Efficiency work | Efficiency | See TE and PS values clearly |
| General training | Full Overview | All metrics at a glance |
| Long rides | Live, Fatigue Indicator | See cumulative zones and technique degradation |
| Rehab / injury prevention | Left Leg, Right Leg, Symmetry Index | Focus on specific leg or precise balance score |
| Climbing | Climbing Mode | Optimized for high torque, low cadence efforts |
| Sprints / attacks | Sprint Mode | High-contrast display for max efforts |
| Progress tracking | Pedaling Score, Delta Average | Real-time technique score and comparison to average |
| Small data fields | Compact Multi | Maximum info in minimal space |

### Training Drills

#### Built-in Drills (10)

**Timed Focus Drills:**

| Drill | Duration | Target | Description |
|-------|:--------:|--------|-------------|
| **Left Leg Focus** | 50s | Balance >55% | Warm-up → Left emphasis 30s → Recovery |
| **Right Leg Focus** | 50s | Balance <45% | Warm-up → Right emphasis 30s → Recovery |
| **Smooth Circles** | 75s | PS ≥25% | Warm-up → Smoothness focus 45s → Recovery |
| **Power Transfer** | 90s | TE 70-80% | Warm-up → Optimal TE zone 60s → Recovery |

**Target-Based Drills:**

| Drill | Duration | Target | Hold Time |
|-------|:--------:|--------|:---------:|
| **Balance Challenge** | 60s | Balance 49-51% | 15s cumulative |
| **Smoothness Target** | 75s | PS ≥25% | 20s cumulative |
| **High Cadence Smoothness** | 75s | PS ≥20% @ 100+ rpm | 15s cumulative |

**Guided Workouts:**

| Drill | Duration | Phases | Description |
|-------|:--------:|:------:|-------------|
| **Balance Recovery** | 3.5 min | 6 | Alternate leg focus with centering phases |
| **Efficiency Builder** | 10 min | 7 | Progressive: balance → TE → PS → combined |
| **Pedaling Mastery** | 15 min | 10 | Comprehensive technique session |

#### Custom Drills

Create personalized drills with configurable parameters:

| Parameter | Options | Description |
|-----------|---------|-------------|
| **Metric** | Balance, TE, PS | Which metric to target |
| **Duration** | 10s - 10 min | Total drill duration |
| **Target Type** | MIN, MAX, RANGE, EXACT | How to evaluate success |
| **Target Value(s)** | 0-100% | Target percentage(s) |

#### Drill Scoring

| Score | Rating | Badge |
|:-----:|--------|:-----:|
| 90-100% | Excellent | ⭐⭐⭐ |
| 75-89% | Good | ⭐⭐ |
| 60-74% | Fair | ⭐ |
| 40-59% | Needs Work | - |
| 0-39% | Keep Practicing | - |

### Achievements

<details>
<summary><b>Ride Count (4)</b></summary>

| Achievement | Requirement |
|-------------|-------------|
| **First Ride** | 1 ride |
| **Getting Started** | 10 rides |
| **Dedicated** | 50 rides |
| **Century** | 100 rides |

</details>

<details>
<summary><b>Balance (3)</b></summary>

| Achievement | Requirement |
|-------------|-------------|
| **Balanced** | 1 min in optimal zone |
| **Well Balanced** | 5 min in optimal zone |
| **Master of Balance** | 10 min in optimal zone |

</details>

<details>
<summary><b>Efficiency (2)</b></summary>

| Achievement | Requirement |
|-------------|-------------|
| **Efficient Rider** | 5 min with TE + PS optimal |
| **Smooth Operator** | 10 min with PS ≥ 25% |

</details>

<details>
<summary><b>Streaks (4)</b></summary>

| Achievement | Requirement |
|-------------|-------------|
| **3-Day Streak** | 3 consecutive days |
| **Weekly Warrior** | 7 consecutive days |
| **Two Week Streak** | 14 consecutive days |
| **Monthly Master** | 30 consecutive days |

</details>

<details>
<summary><b>Drills (3)</b></summary>

| Achievement | Requirement |
|-------------|-------------|
| **First Drill** | Complete 1 drill |
| **Drill Enthusiast** | Complete 10 drills |
| **Perfect Form** | Score 90%+ on any drill |

</details>

### Weekly Challenges

7 rotating challenges based on ISO week number:

| Week Mod | Challenge | Target |
|:--------:|-----------|--------|
| 1 | **Active Week** | Complete 3 rides |
| 2 | **Balanced Rider** | Avg 48-52% in 3 rides |
| 3 | **Zone Master** | 60%+ time in optimal zone |
| 4 | **Technique Focus** | Complete 2 drills |
| 5 | **Consistency** | Ride 4 consecutive days |
| 6 | **Efficient Pedaling** | Avg TE >70% in 3 rides |
| 7 | **Smooth Circles** | Avg PS >20% in 3 rides |

### Settings

#### Thresholds

| Setting | Range | Default | Description |
|---------|:-----:|:-------:|-------------|
| **Balance Threshold** | ±1-10% | ±5% | Deviation from 50% for attention/problem |
| **TE Optimal Min** | 50-90% | 70% | Lower bound of optimal TE |
| **TE Optimal Max** | 55-100% | 80% | Upper bound of optimal TE |
| **PS Minimum** | 10-30% | 20% | Minimum PS for optimal status |

#### Alerts

**Global:**
- Alerts Enabled (master switch)
- Screen Wake on alert

**Per-Metric (Balance, TE, PS):**
- Enabled / Disabled
- Trigger Level: Problem Only, Attention+
- Visual / Sound / Vibration
- Cooldown: 10-120 seconds

**Sensor Disconnect:**

| Action | Description |
|--------|-------------|
| **Show --** | Display `--` in data fields when sensor lost (default) |
| **Alert** | Show banner alert with sound/vibration |
| **Nothing** | Continue showing last known values |

The sensor disconnect alert includes:
- Screen wake
- Visual banner (8 seconds)
- Descending tone pattern (1000Hz → 800Hz → 600Hz)
- Triple vibration
- 30-second cooldown between alerts

#### Background Mode

| Setting | Default | Description |
|---------|:-------:|-------------|
| **Background Mode** | On | Collect data for all rides |
| **Auto-Sync** | On | Sync rides after completion |

#### Crash Recovery

If the app crashes or Karoo restarts during a ride, KPedal automatically recovers your data:

| Feature | Description |
|---------|-------------|
| **Checkpoint Saving** | Saves ride state every 1 minute to local database |
| **Auto-Restore** | On restart, silently restores if checkpoint < 24 hours old |
| **Seamless** | No user action required, ride continues from last checkpoint |

**What gets recovered:**
- Total ride duration
- Balance, TE, PS averages
- Time in each zone (optimal/attention/problem)
- Extended metrics (power, HR, cadence, etc.)
- Per-minute snapshots

### Ride Analysis

#### Scoring Algorithm

**Overall Score** (0-100) = weighted components:

| Component | Weight | Calculation |
|-----------|:------:|-------------|
| Balance | 40% | Closeness to 50/50 |
| Efficiency | 35% | (TE score + PS score) / 2 |
| Consistency | 25% | % time in optimal zone |

<details>
<summary><b>Score Formulas</b> - Click to expand</summary>

**Balance Score** (40% weight):
```
deviation = abs(balanceRight - 50)
balanceScore = max(0, 100 - deviation * 10)
```
- 50/50 = 100 points
- 48/52 or 52/48 = 80 points
- 45/55 = 50 points

**Efficiency Score** (35% weight):
```
teScore = if TE in [70-80] → 100, else → 100 - abs(75 - TE) * 2
psScore = if PS >= 20 → 100, else → PS * 5
efficiencyScore = (teScore + psScore) / 2
```

**Consistency Score** (25% weight):
```
consistencyScore = zoneOptimal  // % time in optimal zone
```

**Final Score**:
```
score = balanceScore * 0.4 + efficiencyScore * 0.35 + consistencyScore * 0.25
```

Score is calculated on Android (`StatusCalculator.kt`) and sent to cloud. Backend has fallback for legacy rides without score.

</details>

#### Score to Stars

| Score | Stars |
|:-----:|:-----:|
| 85-100 | ⭐⭐⭐⭐⭐ |
| 70-84 | ⭐⭐⭐⭐ |
| 55-69 | ⭐⭐⭐ |
| 40-54 | ⭐⭐ |
| 0-39 | ⭐ |

---

## Web Portal

### Domains

| Domain | Purpose |
|--------|---------|
| [kpedal.com](https://kpedal.com) | Landing page (guests), Dashboard (authenticated) |
| [start.kpedal.com](https://start.kpedal.com) | Interactive setup guide |
| [app.kpedal.com](https://app.kpedal.com) | Web application (redirects to login if guest) |
| [link.kpedal.com](https://link.kpedal.com) | Device Code verification |
| [api.kpedal.com](https://api.kpedal.com) | REST API |

### Demo Account

Try the full web portal without signing up:

1. Visit [kpedal.com/login](https://kpedal.com/login)
2. Click **Try Demo**
3. Follow the **guided tour** through all features
4. Explore the dashboard with 27 sample rides

**Demo features:**
- 🎯 **Guided Product Tour** — Interactive walkthrough of all features (~2 min)
- Full dashboard with score ring, charts, and analytics
- Ride history with detailed per-ride analysis
- Timeline charts with per-minute snapshots
- Achievements and drill results
- Light/Dark theme toggle

**Guided Tour:**

The demo includes a comprehensive guided tour that walks you through every feature:

| Page | Steps | What You'll Learn |
|------|:-----:|-------------------|
| **Dashboard** | 13 | Period selector, asymmetry metrics, zones, technique gauges, trends |
| **Rides** | 5 | Stats strip, view toggle, ride list navigation |
| **Ride Detail** | 7 | Score, performance strip, technique breakdown, fatigue analysis, timeline |
| **Drills** | 4 | Drill history, scoring, time-in-target metrics |
| **Achievements** | 4 | Categories, progress tracking, milestones |
| **Settings** | 6 | Devices, thresholds, alerts, theme |

Click the **Tour** button in the demo banner anytime to restart the tour.

**Demo performance:**
- **Zero latency** — Static data bundled in frontend (0ms API calls)
- **Dynamic timestamps** — Demo rides auto-adjust so newest appears as "today"
- **Instant login** — Hardcoded demo user, no database query
- **Smart caching** — HTML revalidation + 1-hour edge cache for regular users

**Demo limitations:**
- Read-only (can't delete rides or modify settings)
- Sample data only (not connected to real device)

### Dashboard

The main dashboard provides a comprehensive overview of your pedaling performance:

| Section | Features |
|---------|----------|
| **Hero Stats** | Score ring, asymmetry badge, balance visual, time in zones, ride summary |
| **Weekly Activity** | Bar chart by day, vs. last week comparison |
| **Technique Card** | TE/PS averages with L/R breakdown, fatigue analysis insights |
| **Trend Charts** | Balance and technique trends over 7/30 days |
| **Progress Card** | Improvement vs. previous period |
| **Recent Rides** | Quick access table with scores and key metrics |

### Ride Detail Page

Detailed analysis for each synced ride:

| Section | Description |
|---------|-------------|
| **Performance Strip** | Duration, distance, avg power, cadence, heart rate, elevation |
| **Score Ring** | Overall score (0-100) with star rating |
| **Technique Gauges** | SVG arc charts for TE and PS with optimal zone highlighting |
| **L/R Comparison** | Side-by-side balance, TE, PS with visual bars |
| **Fatigue Analysis** | First ⅓ vs last ⅓ comparison with actionable insights |
| **Power Zone Breakdown** | Technique metrics by power zone with distribution chart |
| **Timeline Chart** | Per-minute snapshots with multiple metric overlays |

#### Fatigue Analysis

Compares ride start (first third) to ride end (last third):

| Status | Meaning | Action |
|--------|---------|--------|
| ✓ **Strong** | No degradation in any metric | Great endurance, technique held |
| ! **Moderate** | 1 metric degraded | Some fatigue impact |
| ⚠ **Fatigued** | 2-3 metrics degraded | Focus on pacing and technique when tired |

#### Power Zone Breakdown

Shows technique quality across power zones:

- Zone distribution bar (% time in each zone)
- Per-zone metrics: Asymmetry, TE, PS
- Helps identify power levels where technique breaks down

### Drills Page

| Feature | Description |
|---------|-------------|
| **Drill History** | List of completed drills with scores |
| **Stats Summary** | Total drills, best score, average score |
| **Phase Breakdown** | Detailed scores for each drill phase |
| **Filtering** | By drill type, date range |

### Achievements Page

| Feature | Description |
|---------|-------------|
| **Badge Grid** | Visual grid of all 16 achievements |
| **Progress Tracking** | Unlocked count, latest unlock |
| **Categories** | Grouped by type (Rides, Balance, Efficiency, Streaks, Drills) |
| **Timestamps** | When each achievement was unlocked |

### Settings Page

| Section | Features |
|---------|----------|
| **Account** | Email, sign out |
| **Linked Devices** | Karoo device list, last sync time, request sync, revoke access |
| **Theme** | Light / Dark / System |

### Responsive Design

The web portal is fully responsive:

| Breakpoint | Layout |
|------------|--------|
| **Desktop** (>768px) | Top navbar, 2-column grid, full tables |
| **Tablet** (481-768px) | Condensed navbar, adaptive grid |
| **Mobile** (≤480px) | Bottom navigation bar, single column, simplified tables |

### Theme Support

| Theme | Description |
|-------|-------------|
| **Auto** | Switches automatically at 7 AM (light) and 7 PM (dark) — default |
| **Light** | Clean white background with darker status colors for readability |
| **Dark** | Dark background with muted status colors (easier on eyes) |

Theme persists in localStorage and syncs with Karoo app settings.

### Cloud Sync

**Link your Karoo:**
1. Settings → Link Account → Get 8-character code (e.g., `ABCD-1234`)
2. Visit [link.kpedal.com](https://link.kpedal.com)
3. Enter code, sign in with Google
4. Done!

**Data Flow:**

| Data Type | Direction | Description |
|-----------|:---------:|-------------|
| Rides | Android → Cloud | One-way upload after ride completion |
| Snapshots | Android → Cloud | Per-minute metrics with rides |
| Drills | Android → Cloud | Drill results with phase scores |
| Achievements | Android → Cloud | Unlock timestamps |
| Settings | ↔ Bidirectional | Sync on app start, auto-upload on change |

**What syncs:**
- ✅ Ride summaries + per-minute snapshots
- ✅ Extended metrics (power, HR, cadence, distance, elevation)
- ✅ Drill results with phase scores
- ✅ Achievements with timestamps
- ✅ Settings (thresholds, alerts)
- ❌ GPS/routes (not collected)
- ❌ Custom drills (local only)
- ❌ Ride notes/ratings (local only)
- ❌ Deletions (not synchronized)

#### Ride Sync Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Ride Sync Architecture                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────────────────┐ │
│  │  Karoo Ride  │     │RideState     │     │   LiveDataCollector      │ │
│  │   Recording  │────▶│  Monitor     │────▶│  (1s interval samples)   │ │
│  └──────────────┘     └──────────────┘     └──────────────────────────┘ │
│                              │                         │                │
│                              │ Ride ends               │ Snapshots      │
│                              ▼                         ▼                │
│                       ┌──────────────┐     ┌──────────────────────────┐ │
│                       │    Room DB   │◀────│   RideRepository.save()  │ │
│                       │  (local)     │     │   syncStatus = PENDING   │ │
│                       └──────────────┘     └──────────────────────────┘ │
│                              │                                          │
│                              ▼                                          │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                        SyncService                                │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────────┐  │  │
│  │  │ Auto-sync after │  │ NetworkCallback │  │  Manual sync      │  │  │
│  │  │ ride completion │  │ (offline→online)│  │  button press     │  │  │
│  │  └────────┬────────┘  └────────┬────────┘  └─────────┬─────────┘  │  │
│  │           └───────────────────┬┴─────────────────────┘            │  │
│  │                               ▼                                   │  │
│  │                    ┌──────────────────┐                           │  │
│  │                    │ syncPendingRides │                           │  │
│  │                    │   + snapshots    │                           │  │
│  │                    └────────┬─────────┘                           │  │
│  └─────────────────────────────┼─────────────────────────────────────┘  │
│                                │ HTTPS POST /sync/ride                  │
│                                ▼                                        │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    api.kpedal.com                                │   │
│  │         Cloudflare Worker → D1 Database (SQLite)                 │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                │                                        │
│                                ▼                                        │
│                       ┌──────────────┐                                  │
│                       │  Room DB     │  syncStatus = SYNCED             │
│                       │  (update)    │  cloudId = response.id           │
│                       └──────────────┘                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### Sync Triggers

| Trigger | When | Conditions | Cooldown |
|---------|------|------------|:--------:|
| **Ride completion** | `RideState.Idle` after recording | Online, logged in | None |
| **Network restored** | WiFi/cellular connected | Not recording, logged in | 60s |
| **App launch** | User opens KPedal app | Online, not recording, pending data | 5 min |
| **Manual button** | User taps Sync | Logged in | None |
| **Settings change** | Any threshold/alert change | Logged in | 2s debounce |

#### Sync Decision Logic

When the app launches or network is restored, `SyncOnLaunchDecider` evaluates conditions in priority order:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Sync Decision Flow                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────┐                                               │
│  │   Trigger Event  │  (app launch, network restore)                │
│  └────────┬─────────┘                                               │
│           │                                                         │
│           ▼                                                         │
│  ┌──────────────────┐     No      ┌─────────────────────────────┐   │
│  │  User logged in? │────────────▶│ Skip: NotLoggedIn           │   │
│  └────────┬─────────┘             └─────────────────────────────┘   │
│           │ Yes                                                     │
│           ▼                                                         │
│  ┌──────────────────┐     No      ┌─────────────────────────────┐   │
│  │ Network online?  │────────────▶│ Skip: Offline               │   │
│  └────────┬─────────┘             └─────────────────────────────┘   │
│           │ Yes                                                     │
│           ▼                                                         │
│  ┌──────────────────┐     Yes     ┌─────────────────────────────┐   │
│  │ Ride recording?  │────────────▶│ Skip: Recording             │   │
│  └────────┬─────────┘             └─────────────────────────────┘   │
│           │ No                                                      │
│           ▼                                                         │
│  ┌──────────────────┐     No      ┌─────────────────────────────┐   │
│  │ Cooldown passed? │────────────▶│ Skip: Cooldown (N sec left) │   │
│  └────────┬─────────┘             └─────────────────────────────┘   │
│           │ Yes                                                     │
│           ▼                                                         │
│  ┌──────────────────┐     No      ┌─────────────────────────────┐   │
│  │ Pending data?    │────────────▶│ Skip: NothingPending        │   │
│  └────────┬─────────┘             └─────────────────────────────┘   │
│           │ Yes                                                     │
│           ▼                                                         │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ ✓ Sync: Upload pending rides, drills, achievements           │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### Cooldown Periods

| Trigger | Cooldown | Purpose |
|---------|:--------:|---------|
| **App launch sync** | 5 minutes | Prevent excessive syncs when user switches apps |
| **Network restore sync** | 60 seconds | Avoid sync storms on flaky connections |
| **Settings upload** | 2 seconds | Debounce rapid threshold adjustments |
| **Alert cooldown** | 10-120s (configurable) | Prevent alert fatigue during ride |

#### Pending Data Detection

Sync checks for pending AND failed items across three tables:

| Data Type | Sync Condition | Sync Endpoint |
|-----------|----------------|---------------|
| **Rides** | `syncStatus IN (PENDING, FAILED)` | `POST /sync/ride` |
| **Drills** | `syncStatus IN (PENDING, FAILED)` | `POST /drills` |
| **Achievements** | `syncStatus IN (PENDING, FAILED)` | `POST /achievements` |

**v1.8 improvement:** Failed items are now automatically retried on next sync trigger, not just new items.

#### Offline Ride Scenario

```
Timeline: Ride without network → Return home → Auto-sync

1. 🚴 Start ride (no network)
   └─ RideStateMonitor: notifyRideStateChanged(recording=true)
   └─ SyncService: isRecording = true (blocks network sync)

2. 🚴 During ride
   └─ LiveDataCollector: samples every 1 second
   └─ Creates RideSnapshot every minute

3. 🏁 End ride
   └─ RideStateMonitor: notifyRideStateChanged(recording=false)
   └─ SyncService: isRecording = false
   └─ RideRepository.saveRide(syncStatus=PENDING)
   └─ SyncService.syncPendingRides() → FAILS (no network)
   └─ Ride stays in Room DB with syncStatus=PENDING

4. 🏠 Arrive home (WiFi connects)
   └─ NetworkCallback.onAvailable() triggered
   └─ SyncService.onNetworkBecameAvailable()
       ├─ Check: isRecording? No ✓
       ├─ Check: cooldown passed? Yes ✓
       ├─ Check: logged in? Yes ✓
       ├─ Check: pending rides? Yes ✓
       └─ syncPendingRides() → SUCCESS
   └─ Room DB: syncStatus=SYNCED, cloudId=123

5. 💻 Open web portal
   └─ Ride visible with all snapshots and metrics
```

#### Sync Status States

| Status | Value | Meaning | Next Action |
|--------|:-----:|---------|-------------|
| `PENDING` | 0 | Saved locally, not yet uploaded | Will sync on next trigger |
| `SYNCED` | 1 | Successfully uploaded to cloud | No action needed |
| `FAILED` | 2 | Upload failed (network error, timeout) | **Auto-retry on next trigger (v1.8)** |

**v1.8 reliability fix:** Room DAO queries now use `syncStatus IN (0, 2)` to include both pending and failed items, ensuring no data is lost due to transient network failures.

#### Error Handling & Retry

| Error Type | Behavior |
|------------|----------|
| **No network** | Mark as PENDING, retry on NetworkCallback |
| **HTTP 5xx** | Retry up to 3 times with exponential backoff |
| **HTTP 401** | Refresh token, then retry |
| **HTTP 403 (revoked)** | Mark device as revoked, stop syncing |
| **Timeout** | Retry on next trigger |

#### Sync Behavior Summary

| Action | Result |
|--------|--------|
| Ride ends (online) | Immediate sync ride + snapshots to cloud |
| Ride ends (offline) | Save as PENDING, auto-sync when network restores |
| Network restored | Auto-sync pending (60s cooldown, skips if riding) |
| Change setting in app | Auto-upload to cloud (2s debounce) |
| Press Sync button | Pull settings + sync all pending rides |
| Start ride | Pull settings from cloud |
| Change on web | Saved to cloud, app gets it on next sync |
| Delete ride on web | Deleted from cloud only, stays on device |

### Device Management

From web Settings:
- View all linked Karoo devices
- See last sync time
- Request sync from device
- **Revoke device access** → immediate logout on device

### Privacy & Security

KPedal is privacy-focused:
- **No location/GPS data** — we only collect pedaling metrics
- **No cookies** — uses localStorage/sessionStorage only
- **No analytics** — no Google Analytics, no trackers
- **Your data, your control** — delete rides or account anytime
- **Open source** — audit the code yourself

See full [Privacy Policy](https://kpedal.com/privacy).

#### Security Implementation

| Feature | Implementation |
|---------|----------------|
| **Authentication** | Google OAuth 2.0, no password storage |
| **Tokens** | JWT with short expiry (15min access, 7d refresh) |
| **Token Storage** | EncryptedSharedPreferences (Android), httpOnly cookies (web) |
| **Rate Limiting** | Per-IP limits: 60/min auth, 100/min API, 30/min sync |
| **Input Validation** | Zod schemas for all API inputs |
| **CORS** | Strict origin whitelist (kpedal.com, app.kpedal.com) |
| **Headers** | HSTS, X-Frame-Options, CSP, no-sniff |
| **Device Revocation** | Immediate token invalidation from web settings |

---

## Compatible Pedals

Requires **dual-sided** power meter with **ANT+ Cycling Dynamics**:

### Full Support (Balance + TE + PS)

| Pedals | Type |
|--------|------|
| Garmin Rally RS/RK/XC | Road/MTB |
| Garmin Vector 3 | Road |
| Favero Assioma DUO | Road |
| Favero Assioma DUO-Shi | Road SPD-SL |
| SRM X-Power | Road/MTB |
| Rotor 2INpower | Crank |

### Balance Only

| Pedals | Notes |
|--------|-------|
| Wahoo POWRLINK Zero | No TE/PS |
| Power2Max NG/NGeco | Spider |
| Quarq DZero | Spider |
| SRAM RED/Force AXS PM | Crank |

### Not Compatible

- Single-sided power meters
- Trainer power
- Garmin Vector 1/2 (legacy protocol)

---

## Installation

### From APK

```bash
# Option 1: ADB
adb install kpedal-x.x.x.apk

# Option 2: Web Sideload
# 1. Enable Developer Mode (Settings → About → Tap Build 7x)
# 2. Go to http://KAROO_IP:4567
# 3. Upload APK

# Option 3 (Recommended): via Hammerhead Companion
# 1. Open GitHub Releases page on your phone browser
# 2. Long-press on the .apk file link
# 3. Select Share → Hammerhead Companion
# 4. Tap Install on your Karoo when prompted

```

### Build from Source

**Prerequisites:**
- Android Studio Arctic Fox+
- JDK 17+ (bundled with Android Studio)
- GitHub PAT with `read:packages` scope

**Setup:**

```bash
# Add to ~/.gradle/gradle.properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=ghp_YOUR_GITHUB_TOKEN
```

**Build:**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleRelease    # Release APK
./gradlew assembleDebug      # Debug APK
./gradlew test               # Unit tests
./gradlew lint               # Android lint
```

**Output:** `app/build/outputs/apk/release/kpedal-<version>.apk`

---

## Development

### Android App Architecture

```
app/src/main/kotlin/io/github/kpedal/
├── KPedalExtension.kt          # Main entry point (Foreground Service)
├── BootReceiver.kt             # Auto-start on device boot
│
├── engine/                     # Core data processing
│   ├── PedalingEngine.kt       # Data pipeline (Karoo SDK → StateFlow)
│   ├── StatusCalculator.kt     # Metric threshold evaluation
│   ├── AlertManager.kt         # Real-time in-ride alerts
│   ├── LiveDataCollector.kt    # Ride statistics aggregator
│   ├── RideStateMonitor.kt     # Ride start/stop detection
│   ├── AchievementChecker.kt   # Achievement unlock logic
│   ├── PedalMonitor.kt         # Pedal connection tracking
│   └── checkpoint/             # Crash recovery
│       ├── RideCheckpointManager.kt
│       ├── CheckpointDecider.kt
│       └── AccumulatorState.kt
│
├── datatypes/                  # Karoo data field layouts (Jetpack Glance)
│   ├── BaseDataType.kt         # Layout size utilities
│   ├── GlanceDataType.kt       # Base class for Glance DataTypes
│   └── glance/                 # 19 Glance-based DataTypes
│       ├── GlanceColors.kt     # Color definitions
│       ├── GlanceComponents.kt # Shared composables
│       ├── QuickGlanceGlance.kt
│       ├── PowerBalanceGlance.kt
│       ├── EfficiencyGlance.kt
│       ├── FullOverviewGlance.kt
│       ├── BalanceTrendGlance.kt
│       ├── SingleBalanceGlance.kt
│       ├── LiveGlance.kt
│       ├── PedalingScoreGlance.kt      # NEW v1.8
│       ├── FatigueIndicatorGlance.kt   # NEW v1.8
│       ├── CadenceBalanceGlance.kt     # NEW v1.8
│       ├── DeltaAverageGlance.kt       # NEW v1.8
│       ├── LeftLegGlance.kt            # NEW v1.8
│       ├── RightLegGlance.kt           # NEW v1.8
│       ├── ClimbingModeGlance.kt       # NEW v1.8
│       ├── SymmetryIndexGlance.kt      # NEW v1.8
│       ├── HREfficiencyGlance.kt       # NEW v1.8
│       ├── PowerFocusGlance.kt         # NEW v1.8
│       ├── SprintModeGlance.kt         # NEW v1.8
│       └── CompactMultiGlance.kt       # NEW v1.8
│
├── drill/                      # Training drills system
│   ├── DrillCatalog.kt         # Drill definitions
│   ├── DrillEngine.kt          # Drill execution
│   ├── DrillRepository.kt      # Results storage
│   └── CustomDrillRepository.kt
│
├── data/                       # Persistence layer
│   ├── PreferencesRepository.kt    # DataStore settings
│   ├── RideRepository.kt           # Room database (rides)
│   ├── AchievementRepository.kt    # Achievements storage
│   ├── AnalyticsRepository.kt      # Trend calculations
│   ├── AuthRepository.kt           # Encrypted tokens
│   └── SyncService.kt              # Cloud sync with retry logic
│
├── api/
│   └── ApiClient.kt            # Retrofit client
│
└── ui/
    ├── MainActivity.kt         # Compose NavHost
    ├── screens/                # Jetpack Compose screens
    └── theme/                  # Colors, typography
```

#### Glance Architecture (v1.8)

DataTypes are built with **Jetpack Glance** — a modern approach that solves critical stability issues:

| Aspect | Old (RemoteViews) | New (Glance) |
|--------|-------------------|--------------|
| **View creation** | Single instance, accumulates actions | Fresh RemoteViews on each `compose()` |
| **Action limit** | Could exceed 65K after 10+ min | Max ~25 actions per update |
| **Memory** | Unbounded growth | Constant, no leaks |
| **Crash risk** | High after prolonged use | None |
| **Code style** | Imperative XML layouts | Declarative Compose-like |

```kotlin
// GlanceDataType base class
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class GlanceDataType(...) : DataTypeImpl(...) {
    private val glance = GlanceRemoteViews()

    // Fresh RemoteViews created on each update - no accumulation!
    val result = glance.compose(context, DpSize.Unspecified) {
        Content(metrics, config, sensorDisconnected)
    }
    emitter.updateView(result.remoteViews)
}
```

### Tech Stack

#### Android App

| Component | Version | Description |
|-----------|:-------:|-------------|
| **Kotlin** | 2.2.20 | Programming language |
| **Android Gradle Plugin** | 8.13.2 | Build system |
| **KSP** | 2.2.20-2.0.4 | Kotlin Symbol Processing |
| **Jetpack Compose** | BOM 2025.01.00 | Modern UI toolkit |
| **Jetpack Glance** | 1.1.1 | RemoteViews with Compose API (DataTypes) |
| **Room** | 2.7.2 | SQLite database abstraction |
| **Coroutines** | 1.10.2 | Async programming |
| **Kotlinx Serialization** | 1.8.0 | JSON serialization |
| **karoo-ext SDK** | 1.1.7 | Hammerhead Karoo integration |
| **Retrofit** | 2.11.0 | HTTP client |
| **OkHttp** | 4.12.0 | HTTP client engine |
| **DataStore** | 1.1.2 | Preferences storage |
| **Navigation Compose** | 2.8.5 | Navigation framework |
| **Lifecycle** | 2.8.7 | Lifecycle-aware components |
| **Activity Compose** | 1.9.3 | Compose Activity integration |
| **Security Crypto** | 1.1.0-alpha06 | Encrypted storage |
| **Target SDK** | 35 | Android 15 |
| **Min SDK** | 23 | Android 6.0 |

#### Web API

| Component | Version | Description |
|-----------|:-------:|-------------|
| **Hono** | 4.11.3 | Web framework for Workers |
| **jose** | 6.1.0 | JWT implementation |
| **TypeScript** | 5.7.0 | Type-safe JavaScript |
| **Vitest** | 4.0.0 | Test framework |
| **Wrangler** | 4.56.0 | Cloudflare CLI |
| **Workers Types** | 4.20260101.0 | Cloudflare Workers TypeScript types |

#### Web Frontend

| Component | Version | Description |
|-----------|:-------:|-------------|
| **SvelteKit** | 2.x | Full-stack framework |
| **Svelte** | 5.x | Reactive UI (runes: $state, $derived) |
| **Vite** | 7.3.0 | Build tool |
| **driver.js** | 1.4.0 | Product tour library |
| **svelte-i18n** | 4.0.1 | Internationalization |
| **svelte-check** | 4.x | Type checking |
| **Cloudflare Pages** | - | Static hosting + edge functions |

#### Infrastructure

| Component | Technology |
|-----------|------------|
| **API** | Cloudflare Workers + Hono |
| **Database** | Cloudflare D1 (SQLite) |
| **Sessions** | Cloudflare KV |
| **Edge Cache** | Cache API (demo data) |
| **Frontend** | SvelteKit 5 + Cloudflare Pages |
| **Auth** | Google OAuth 2.0 + JWT |
| **DNS/CDN** | Cloudflare |

### Web Project Structure

```
web/
├── api/                        # Cloudflare Worker
│   ├── src/
│   │   ├── index.ts            # Main app, routes, middleware
│   │   ├── auth/               # OAuth, JWT, Device Code Flow
│   │   │   ├── google.ts       # Google OAuth handlers
│   │   │   ├── jwt.ts          # Token generation/validation
│   │   │   └── device-code.ts  # Device Code Flow
│   │   ├── api/                # Route handlers
│   │   │   ├── rides.ts        # Rides CRUD + stats
│   │   │   ├── sync.ts         # Sync from Karoo
│   │   │   ├── devices.ts      # Device management
│   │   │   ├── settings.ts     # User settings
│   │   │   ├── drills.ts       # Drill results
│   │   │   └── achievements.ts # Achievements
│   │   ├── middleware/         # Request processing
│   │   │   ├── auth.ts         # JWT validation
│   │   │   ├── rateLimit.ts    # Rate limiting
│   │   │   ├── validate.ts     # Input validation
│   │   │   └── demoProtection.ts  # Demo account write protection
│   │   ├── utils/
│   │   │   └── demoData.ts     # Demo caching + timestamp offset
│   │   └── db/
│   │       ├── schema.sql          # D1 schema
│   │       ├── demo-data.sql       # Demo user + 27 rides
│   │       ├── demo-snapshots.sql  # Per-minute ride snapshots
│   │       └── demo-achievements.sql  # Demo achievements + drills
│   ├── wrangler.toml           # Worker config
│   └── vitest.config.ts        # Test config
│
└── app/                        # SvelteKit 5
    ├── src/
    │   ├── routes/             # Pages
    │   │   ├── +page.svelte        # Landing / Dashboard
    │   │   ├── +layout.svelte      # App shell
    │   │   ├── start/              # Setup guide (start.kpedal.com)
    │   │   ├── login/              # Google sign-in
    │   │   ├── link/               # Device Code entry
    │   │   ├── rides/              # Ride list
    │   │   ├── rides/[id]/         # Ride detail
    │   │   ├── drills/             # Drill history
    │   │   ├── achievements/       # Achievement grid
    │   │   ├── settings/           # Account settings
    │   │   └── privacy/            # Privacy policy
    │   ├── lib/
    │   │   ├── auth.ts             # Auth store, tokens
    │   │   ├── demoData.ts         # Static demo data (27 rides, drills, achievements)
    │   │   ├── theme.ts            # Theme store (light/dark/auto)
    │   │   ├── tour.ts             # Product tour (driver.js)
    │   │   ├── i18n.ts             # Internationalization (17 languages)
    │   │   ├── config.ts           # API_URL constant
    │   │   └── components/         # Shared components (Footer, InfoTip)
    │   ├── locales/                # Translation files (17 languages)
    │   │   ├── en.json             # English (1533 keys)
    │   │   └── *.json              # DE, FR, IT, PT, NL, JA, ZH, UK, RU, HE, AR, PL, KO, DA, SV
    │   └── app.css                 # Global styles, CSS vars, themes
    └── svelte.config.js
```

### Build Commands

```bash
# ===== Android =====
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleRelease        # Release APK
./gradlew assembleDebug          # Debug APK
./gradlew test                   # Unit tests
./gradlew test --tests "*.StatusCalculatorTest"  # Single test class
./gradlew lint                   # Android lint
./gradlew compileDebugKotlin     # Fast compile check

# ===== API =====
cd web/api
npm run dev                      # Local server :8787
npm run deploy                   # Deploy to Workers
npx vitest run                   # Run all tests
npx vitest run src/auth          # Run auth tests only
npm run typecheck                # TypeScript check
npm run lint                     # ESLint

# ===== Frontend =====
cd web/app
npm run dev                      # Local server :5173
npm run build                    # Production build
npm run check                    # Svelte check
npx wrangler pages deploy .svelte-kit/cloudflare --project-name=kpedal-web --commit-dirty=true
```

### API Endpoints

| Route | Method | Description |
|-------|--------|-------------|
| `/` | GET | Health check |
| `/auth/login` | GET | → Google OAuth |
| `/auth/callback` | GET | OAuth callback, sets tokens |
| `/auth/demo` | POST | Login as demo user (read-only) |
| `/auth/device/code` | POST | Start Device Code Flow |
| `/auth/device/token` | POST | Poll for authorization |
| `/auth/refresh` | POST | Refresh access token |
| `/auth/logout` | POST | Invalidate session |
| `/me` | GET | Current user profile |
| `/rides` | GET | List rides (paginated) |
| `/rides/:id` | GET | Single ride with snapshots |
| `/rides/:id` | DELETE | Delete ride |
| `/rides/dashboard` | GET | Combined: stats + rides + weekly + trends |
| `/rides/stats/summary` | GET | Aggregate statistics |
| `/rides/stats/weekly` | GET | This week vs last week |
| `/rides/stats/trends` | GET | Daily aggregates for charts |
| `/drills` | GET | List drill results |
| `/drills` | POST | Sync drills from device |
| `/drills/dashboard` | GET | Combined: drills + stats |
| `/achievements` | GET | List achievements |
| `/achievements` | POST | Sync achievements from device |
| `/achievements/dashboard` | GET | Combined: achievements + stats |
| `/sync/ride` | POST | Sync single ride |
| `/sync/rides` | POST | Batch sync rides |
| `/sync/check-request` | GET | Check for sync requests |
| `/settings` | GET | Get user settings |
| `/settings` | PUT | Update settings (partial) |
| `/devices` | GET | List linked devices |
| `/devices/:id` | DELETE | Revoke device access |
| `/devices/:id/request-sync` | POST | Request sync from web |

### Rate Limits

| Route Type | Limit | Notes |
|------------|-------|-------|
| Auth routes | 60/min | Higher to support Device Code polling (5s interval) |
| API routes | 100/min | Standard API calls |
| Sync routes | 30/min | Device uploads |

### Database Schema

```sql
-- Users (from Google OAuth)
users (id, google_id, email, name, picture, created_at, updated_at)

-- Linked devices
devices (id, user_id, name, type, last_sync, created_at)

-- Synced rides
rides (
    id, user_id, device_id, timestamp, duration_ms,
    -- Core metrics
    balance_left, balance_right, te_left, te_right, ps_left, ps_right,
    zone_optimal, zone_attention, zone_problem, score,
    -- Extended metrics
    power_avg, power_max, cadence_avg, hr_avg, hr_max,
    speed_avg, distance_km,
    -- Pro metrics
    elevation_gain, elevation_loss, grade_avg, grade_max,
    normalized_power, energy_kj,
    -- Meta
    notes, rating, created_at
)

-- Per-minute snapshots (for timeline charts)
ride_snapshots (
    id, ride_id, minute_index, timestamp,
    balance_left, balance_right, te_left, te_right, ps_left, ps_right,
    power_avg, cadence_avg, hr_avg, zone_status
)

-- Drill results
drill_results (
    id, user_id, device_id, drill_id, drill_name, timestamp,
    duration_ms, score, time_in_target_ms, time_in_target_percent,
    completed, phase_scores_json, created_at
)

-- Achievements
achievements (id, user_id, achievement_id, unlocked_at, created_at)

-- Device Code Flow
device_codes (device_code, user_code, device_id, device_name, user_id, status, expires_at)

-- User settings (synced)
user_settings (
    user_id,
    balance_threshold, te_optimal_min, te_optimal_max, ps_minimum,
    alerts_enabled, screen_wake_on_alert,
    balance_alert_*, te_alert_*, ps_alert_*,
    background_mode_enabled, auto_sync_enabled, updated_at
)
```

### Testing

#### Android (JUnit 5)

```bash
# All tests
./gradlew test

# Single test class
./gradlew test --tests "io.github.kpedal.engine.StatusCalculatorTest"

# Single test method
./gradlew test --tests "*.StatusCalculatorTest.testBalanceOptimal"
```

Test coverage:
- `StatusCalculatorTest` — Metric threshold evaluation
- `LiveDataCollectorLogicTest` — Ride statistics aggregation
- `DrillLogicTest` — Drill scoring and phase transitions
- `AchievementConditionsTest` — Achievement unlock logic

**Testing libraries:**
| Library | Version | Purpose |
|---------|:-------:|---------|
| JUnit 5 | 5.11.4 | Test framework |
| Truth | 1.4.4 | Assertions |
| MockK | 1.13.16 | Mocking |
| Coroutines Test | 1.10.2 | Coroutine testing |

#### Web API (Vitest)

```bash
cd web/api

# All tests
npx vitest run

# Specific directory
npx vitest run src/auth

# Single file
npx vitest run src/auth/device-code.test.ts

# Watch mode
npx vitest
```

Test coverage:
- `auth/*.test.ts` — OAuth, JWT, Device Code Flow
- `middleware/*.test.ts` — Auth, rate limiting, validation
- `api/*.test.ts` — Rides, devices, sync endpoints

### API Performance

The API is optimized for low latency and efficient resource usage:

#### Smart Placement

```toml
# wrangler.toml
[placement]
mode = "smart"
```

Worker runs near D1 database (EEUR region), reducing D1 latency from **~250ms to ~10ms** per query.

#### Optimization Techniques

| Technique | Benefit |
|-----------|---------|
| **D1 Batch Queries** | 6 SQL queries in single roundtrip (`DB.batch([...])`) |
| **Combined Endpoints** | `/rides/dashboard` returns stats + rides + weekly + trends in ONE call |
| **Static Demo Data** | Demo mode uses bundled JSON, zero API calls |
| **Demo Login** | Hardcoded demo user, no DB query, fire-and-forget KV write |
| **Cache Headers** | HTML `must-revalidate`, immutable assets 1-year cache |
| **Edge Cache** | 1-hour TTL for demo API responses (regular users) |

#### Response Headers

```
x-cache: HIT              # Edge cache status
cf-placement: local-TLV   # Worker location
x-ratelimit-remaining: 96 # Rate limit status
```

#### Latency Targets

| Endpoint | Target | Actual |
|----------|:------:|:------:|
| Health check (`/`) | <50ms | ~15ms |
| Auth (`/me`) | <100ms | ~30ms |
| Dashboard | <200ms | ~80ms |
| Demo login | <200ms | ~40ms (warm), ~150ms (cold) |
| Demo pages | 0ms | 0ms (static data) |

---

## Troubleshooting

<details>
<summary><b>App crashes after 10-15 minutes (v1.7 and earlier)</b></summary>

**Fixed in v1.8.0** — If you experience crashes during long rides, update to v1.8.0.

**Root cause:** RemoteViews action accumulation. Each DataType update added ~25 actions to a single RemoteViews instance. After 10+ minutes (600+ updates), the accumulated actions exceeded Android's limits, causing crashes.

**Solution:** v1.8 migrated to Jetpack Glance, which creates fresh RemoteViews on each update. This eliminates action accumulation entirely.

</details>

<details>
<summary><b>No data showing</b></summary>

- Pair pedals in Karoo Sensors menu first
- Use KPedal data fields, not Karoo's built-in
- Check pedal compatibility (needs Cycling Dynamics)
- Enable Background Mode in Settings

</details>

<details>
<summary><b>Alerts not working</b></summary>

- Check Settings → Alerts Enabled (master switch)
- Check individual metric alert settings
- Wait for cooldown period to pass
- Check Karoo system volume

</details>

<details>
<summary><b>Sync issues</b></summary>

- Link account: Settings → Link Account
- Token expired: Press Sync to auto-refresh
- Device revoked: Re-link account
- Check WiFi connection
- **v1.8+:** Failed syncs are automatically retried on next trigger

</details>

<details>
<summary><b>Rides not syncing (v1.7 and earlier)</b></summary>

**Fixed in v1.8.0** — If rides fail to sync and never retry, update to v1.8.0.

**Root cause:** Failed sync items (syncStatus=2) were not included in retry queries. Only new pending items (syncStatus=0) were uploaded.

**Solution:** v1.8 Room DAO queries now use `syncStatus IN (0, 2)` to include both pending and failed items.

</details>

<details>
<summary><b>Web portal issues</b></summary>

- Can't sign in: Clear cookies, try incognito
- No rides: Press Sync on Karoo app
- Charts not loading: Refresh page
- Demo not loading: Check network connection, try refreshing

</details>

<details>
<summary><b>Device Code not working</b></summary>

- Code expires after 10 minutes
- Make sure you're on [link.kpedal.com](https://link.kpedal.com)
- Generate a new code if expired
- Check internet connection on both devices

</details>

---

## Requirements

| Requirement | Specification |
|-------------|---------------|
| **Device** | Hammerhead Karoo 2 or Karoo 3 |
| **Display** | 480×800 pixels |
| **Android** | API 23-35 (Android 6.0 - 15) |
| **Pedals** | ANT+ Cycling Dynamics (dual-sided) |

### Build Requirements

| Tool | Version |
|------|---------|
| **JDK** | 17+ (bundled with Android Studio) |
| **Android Studio** | Arctic Fox+ |
| **Node.js** | 18+ (for web development) |
| **npm** | 9+ |

---

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push: `git push origin feature/amazing-feature`
5. Open Pull Request

### Development Setup

```bash
git clone https://github.com/yrkan/KPedal.git
cd kpedal

# Android
./gradlew assembleDebug

# API
cd web/api && npm install && npm run dev

# Frontend
cd web/app && npm install && npm run dev
```

### Code Style

- **Android**: Kotlin coding conventions, ktlint
- **API**: TypeScript strict mode, ESLint
- **Frontend**: Svelte 5 runes ($state, $derived, $props), CSS variables

---

## License

MIT

---

## Links

| | |
|---|---|
| **Website** | [kpedal.com](https://kpedal.com) |
| **Get Started** | [start.kpedal.com](https://start.kpedal.com) |
| **Try Demo** | [kpedal.com/login](https://kpedal.com/login) |
| **Web App** | [app.kpedal.com](https://app.kpedal.com) |
| **Device Link** | [link.kpedal.com](https://link.kpedal.com) |
| **Privacy Policy** | [kpedal.com/privacy](https://kpedal.com/privacy) |
| **GitHub** | [github.com/yrkan/KPedal](https://github.com/yrkan/KPedal) |
| **Issues** | [github.com/yrkan/KPedal/issues](https://github.com/yrkan/KPedal/issues) |

---

<p align="center">
  <b>KPedal</b> — Real-time pedaling efficiency for Karoo
</p>
