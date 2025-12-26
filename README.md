# KPedal

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Platform: Karoo](https://img.shields.io/badge/Platform-Karoo%202%2F3-blue.svg)](https://www.hammerhead.io/pages/karoo)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF.svg)](https://kotlinlang.org/)
[![AGP](https://img.shields.io/badge/AGP-8.13.2-3DDC84.svg)](https://developer.android.com/build)
[![API: Cloudflare Workers](https://img.shields.io/badge/API-Cloudflare%20Workers-F38020.svg)](https://workers.cloudflare.com/)
[![Frontend: SvelteKit](https://img.shields.io/badge/Frontend-SvelteKit%205-FF3E00.svg)](https://kit.svelte.dev/)

Real-time pedaling efficiency extension for Hammerhead Karoo 2/3. Displays Balance, Torque Effectiveness, and Pedal Smoothness metrics from ANT+ Cycling Dynamics power meter pedals.

<p align="center">
  <a href="https://kpedal.com">Website</a> •
  <a href="https://kpedal.com/login">Try Demo</a> •
  <a href="https://app.kpedal.com">Web App</a> •
  <a href="#installation">Installation</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="https://kpedal.com/privacy">Privacy</a>
</p>

---

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Pedaling Metrics](#pedaling-metrics)
- [Karoo App Features](#karoo-app-features)
- [Web Portal](#web-portal)
  - [Demo Account](#demo-account)
  - [Privacy & Security](#privacy--data)
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
- [Changelog](#changelog)

---

## Overview

KPedal provides cyclists with real-time feedback on pedaling technique during rides. The extension displays data fields on Karoo ride screens and includes a companion app for training drills, ride history, analytics, and achievement tracking.

### Key Features

| Feature | Description |
|---------|-------------|
| **Background Mode** | Collects pedaling data for ALL rides, even without KPedal data fields on screen |
| **Cloud Sync** | Sync rides, drills, achievements and settings to [app.kpedal.com](https://app.kpedal.com) |
| **7 Data Layouts** | Customizable data fields with adaptive sizing (small/medium/large) |
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

### System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            KPedal Ecosystem                                  │
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
│  │  │  │ PedalEngine │→ │AlertManager │→ │ 7 DataType Layouts  │ │  │       │
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
│  │  │  Hono Framework │ JWT Auth │ Rate Limiting │ Validation   │  │       │
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

# Or use web sideload at http://KAROO_IP:4567
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

7 layouts available for Karoo ride screens. Each layout **automatically adapts** to the grid size:

| Size | Grid Rows | Description |
|------|:---------:|-------------|
| **Small** | < 20 | Compact essential metrics |
| **Medium** | 20-40 | Balanced layout with bars |
| **Large** | > 40 | Full details with zones and averages |

#### Layout Catalog

| Layout | Type ID | Best For | Features |
|--------|---------|----------|----------|
| **Quick Glance** | `quick-glance` | Racing, minimal distraction | Status text ("Bal, TE" when issues) + balance bar |
| **Power + Balance** | `power-balance` | Balance training | Large L/R percentage, balance bar, asymmetry indicator |
| **Efficiency** | `efficiency` | TE/PS focus | L/R TE and PS values, averages on large size |
| **Full Overview** | `full-overview` | All-around monitoring | Balance + TE + PS in compact grid |
| **Balance Trend** | `balance-trend` | Trend analysis | Current, 3s avg, 10s avg with trend arrows |
| **Balance** | `single-balance` | Full-screen balance | Large L/R with visual bar |
| **Live** | `live` | Ride analysis | All metrics + Time In Zone percentages |

#### Layout Selection Guide

| Use Case | Recommended | Why |
|----------|-------------|-----|
| Racing / criteriums | Quick Glance | Minimal distraction, only alerts when needed |
| Balance training | Power + Balance, Balance Trend | Focus on L/R with trend information |
| Efficiency work | Efficiency | See TE and PS values clearly |
| General training | Full Overview | All metrics at a glance |
| Long rides | Live | See cumulative zones and averages |
| Rehab / injury prevention | Balance Trend | Track asymmetry over time |

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

#### Background Mode

| Setting | Default | Description |
|---------|:-------:|-------------|
| **Background Mode** | On | Collect data for all rides |
| **Auto-Sync** | On | Sync rides after completion |

### Ride Analysis

#### Scoring Algorithm

**Overall Score** (0-100) = weighted components:

| Component | Weight | Calculation |
|-----------|:------:|-------------|
| Balance | 40% | Closeness to 50/50 |
| Efficiency | 35% | (TE score + PS score) / 2 |
| Consistency | 25% | % time in optimal zone |

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
- **Instant loading** — 3-layer caching (browser → edge → origin)
- **Browser cache** — sessionStorage for immediate response
- **Edge cache** — Cache API at Cloudflare edge (no origin roundtrip)
- **Smart timestamps** — Demo rides auto-adjust so newest appears as "today"

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
| **Light** | Clean white background with darker status colors for readability |
| **Dark** | Dark background with muted status colors (easier on eyes) |
| **System** | Follows OS preference |

Theme persists in localStorage and syncs with Karoo app settings.

### Cloud Sync

**Link your Karoo:**
1. Settings → Link Account → Get 8-character code (e.g., `ABCD-1234`)
2. Visit [link.kpedal.com](https://link.kpedal.com)
3. Enter code, sign in with Google
4. Done!

**What syncs:**
- ✅ Ride summaries + per-minute snapshots
- ✅ Extended metrics (power, HR, cadence, distance, elevation)
- ✅ Drill results with phase scores
- ✅ Achievements with timestamps
- ✅ Settings (thresholds, alerts)
- ❌ GPS/routes (not collected)
- ❌ Custom drills (local only)
- ❌ Ride notes/ratings (local only)

**Sync Behavior:**

| Action | Result |
|--------|--------|
| Change setting in app | Auto-upload to cloud (2s debounce) |
| Press Sync button | Pull from cloud + sync pending rides |
| Start ride | Pull settings from cloud |
| Change on web | Saved to cloud, app gets it on next sync |

### Device Management

From web Settings:
- View all linked Karoo devices
- See last sync time
- Request sync from device
- **Revoke device access** → immediate logout on device

### Privacy & Data

KPedal is privacy-focused:
- **No location/GPS data** — we only collect pedaling metrics
- **No cookies** — uses localStorage/sessionStorage only
- **No analytics** — no Google Analytics, no trackers
- **Your data, your control** — delete rides or account anytime
- **Open source** — audit the code yourself

See full [Privacy Policy](https://kpedal.com/privacy).

### Security

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

**Output:** `app/build/outputs/apk/release/kpedal-x.x.x.apk`

---

## Development

### Android App Architecture

```
app/src/main/java/io/github/kpedal/
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
│   └── PedalMonitor.kt         # Pedal connection tracking
│
├── datatypes/                  # Karoo data field layouts (RemoteViews)
│   ├── BaseDataType.kt         # Common lifecycle handling
│   ├── QuickGlanceDataType.kt
│   ├── PowerBalanceDataType.kt
│   ├── EfficiencyDataType.kt
│   ├── FullOverviewDataType.kt
│   ├── BalanceTrendDataType.kt
│   ├── SingleBalanceDataType.kt
│   └── LiveDataType.kt
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
│   └── SyncService.kt              # Cloud sync
│
├── api/
│   └── ApiClient.kt            # Retrofit client
│
└── ui/
    ├── MainActivity.kt         # Compose NavHost
    ├── screens/                # Jetpack Compose screens
    └── theme/                  # Colors, typography
```

### Tech Stack

#### Android App

| Component | Version | Description |
|-----------|:-------:|-------------|
| **Kotlin** | 2.2.20 | Programming language |
| **Android Gradle Plugin** | 8.13.2 | Build system |
| **Jetpack Compose** | BOM 2025.01.00 | Modern UI toolkit |
| **Room** | 2.7.2 | SQLite database abstraction |
| **Coroutines** | 1.10.2 | Async programming |
| **karoo-ext SDK** | 1.1.7 | Hammerhead Karoo integration |
| **Retrofit** | 2.11.0 | HTTP client |
| **DataStore** | 1.1.2 | Preferences storage |
| **Navigation Compose** | 2.8.5 | Navigation framework |
| **Target SDK** | 35 | Android 15 |
| **Min SDK** | 23 | Android 6.0 |

#### Web API

| Component | Version | Description |
|-----------|:-------:|-------------|
| **Hono** | 4.11.0 | Web framework for Workers |
| **jose** | 6.1.0 | JWT implementation |
| **TypeScript** | 5.7.0 | Type-safe JavaScript |
| **Vitest** | 4.0.0 | Test framework |
| **Wrangler** | 4.56.0 | Cloudflare CLI |

#### Web Frontend

| Component | Version | Description |
|-----------|:-------:|-------------|
| **SvelteKit** | 5.x | Full-stack framework |
| **Svelte** | 5.x | Reactive UI (runes: $state, $derived) |
| **driver.js** | 1.x | Product tour library |
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
    │   │   ├── +page.svelte        # Dashboard
    │   │   ├── +layout.svelte      # App shell
    │   │   ├── login/              # Google sign-in
    │   │   ├── link/               # Device Code entry
    │   │   ├── rides/              # Ride list
    │   │   ├── rides/[id]/         # Ride detail
    │   │   ├── drills/             # Drill history
    │   │   ├── achievements/       # Achievement grid
    │   │   ├── settings/           # Account settings
    │   │   └── privacy/            # Privacy policy
    │   ├── lib/
    │   │   ├── auth.ts             # Auth store, tokens, demo prefetch
    │   │   ├── theme.ts            # Theme store (light/dark/system)
    │   │   ├── tour.ts             # Product tour (driver.js)
    │   │   ├── config.ts           # API_URL constant
    │   │   └── components/         # Shared components (InfoTip, etc.)
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
npx wrangler pages deploy .svelte-kit/cloudflare --project-name=kpedal-web
```

### API Endpoints

| Route | Method | Description |
|-------|--------|-------------|
| `/auth/login` | GET | → Google OAuth |
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
| **Demo Edge Cache** | Cache API at edge, 1-hour TTL, no origin roundtrip |
| **Browser Prefetch** | Demo data prefetched on login, stored in sessionStorage |
| **Hardcoded Timestamps** | Demo offset calculated without DB query |

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
| Demo (cached) | <50ms | ~20ms |

---

## Troubleshooting

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

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push: `git push origin feature/amazing-feature`
5. Open Pull Request

### Development Setup

```bash
git clone https://github.com/yrkan/kpedal.git
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

#### Web API (Vitest)

```bash
cd web/api

# All tests (372 tests)
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

## License

MIT

---

## Links

| | |
|---|---|
| **Website** | [kpedal.com](https://kpedal.com) |
| **Try Demo** | [kpedal.com/login](https://kpedal.com/login) |
| **Web App** | [app.kpedal.com](https://app.kpedal.com) |
| **Device Link** | [link.kpedal.com](https://link.kpedal.com) |
| **Privacy Policy** | [kpedal.com/privacy](https://kpedal.com/privacy) |
| **GitHub** | [github.com/yrkan/kpedal](https://github.com/yrkan/kpedal) |
| **Issues** | [github.com/yrkan/kpedal/issues](https://github.com/yrkan/kpedal/issues) |

---

## Changelog

### Latest Updates (December 2025)

- **Kotlin 2.2.20** — Updated from 2.0.21, latest stable with AGP 8.x
- **Android Gradle Plugin 8.13.2** — Latest stable build tools
- **Target SDK 35** — Android 15 support
- **Room 2.7.2** — Latest database abstraction
- **Compose BOM 2025.01.00** — Latest Jetpack Compose
- **Hono 4.11.0** — Updated API framework
- **jose 6.1.0** — JWT library major update
- **Vitest 4.0.0** — Test framework major update
- **Demo instant loading** — 3-layer caching for instant demo experience
- **Product tour** — Interactive 39-step walkthrough with driver.js

---

<p align="center">
  <b>KPedal</b> — Real-time pedaling efficiency for Karoo
</p>
