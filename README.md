# KPedal

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Platform: Karoo](https://img.shields.io/badge/Platform-Karoo%202%2F3-blue.svg)](https://www.hammerhead.io/pages/karoo)
[![API: Cloudflare Workers](https://img.shields.io/badge/API-Cloudflare%20Workers-orange.svg)](https://workers.cloudflare.com/)
[![Frontend: SvelteKit](https://img.shields.io/badge/Frontend-SvelteKit%205-red.svg)](https://kit.svelte.dev/)

Real-time pedaling efficiency extension for Hammerhead Karoo 2/3. Displays Balance, Torque Effectiveness, and Pedal Smoothness metrics from ANT+ Cycling Dynamics power meter pedals.

<p align="center">
  <a href="https://kpedal.com">Website</a> •
  <a href="https://app.kpedal.com">Web App</a> •
  <a href="#installation">Installation</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#documentation">Documentation</a>
</p>

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

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         KPedal Ecosystem                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐    ANT+    ┌───────────────┐                  │
│  │ Power Meter  │ ─────────▶ │   Karoo 2/3   │                  │
│  │   Pedals     │  Cycling   │  KPedal App   │                  │
│  └──────────────┘  Dynamics  └───────┬───────┘                  │
│                                      │                          │
│                                      │ HTTPS                    │
│                                      ▼                          │
│                         ┌────────────────────┐                  │
│                         │ api.kpedal.com     │                  │
│                         │ Cloudflare Workers │                  │
│                         └─────────┬──────────┘                  │
│                                   │                             │
│                    ┌──────────────┼──────────────┐              │
│                    ▼              ▼              ▼              │
│              ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│              │    D1    │  │    KV    │  │   OAuth  │           │
│              │ Database │  │ Sessions │  │  Google  │           │
│              └──────────┘  └──────────┘  └──────────┘           │
│                                   │                             │
│                                   ▼                             │
│                         ┌────────────────────┐                  │
│                         │ app.kpedal.com     │                  │
│                         │ Cloudflare Pages   │                  │
│                         │    SvelteKit 5     │                  │
│                         └────────────────────┘                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Quick Start

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
2. Note the 6-character code (e.g., "ABC123")
3. Visit [link.kpedal.com](https://link.kpedal.com) on any device
4. Enter code and sign in with Google
5. Done! Rides will sync automatically

---

## Screenshots

### Karoo Data Fields

| Quick Glance | Power Balance | Full Overview | Live |
|:---:|:---:|:---:|:---:|
| Status + Balance bar | Large L/R display | All metrics compact | All + Time In Zone |
| Best for racing | Balance training | General use | Long rides |

### Web Dashboard

| Dashboard | Ride Detail | Achievements | Settings |
|:---:|:---:|:---:|:---:|
| Score ring, charts | Timeline, gauges | Badge grid | Devices, theme |

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

## Data Field Layouts

7 layouts available for Karoo ride screens. Each layout **automatically adapts** to the grid size:

| Size | Grid Rows | Description |
|------|:---------:|-------------|
| **Small** | < 20 | Compact essential metrics |
| **Medium** | 20-40 | Balanced layout with bars |
| **Large** | > 40 | Full details with zones and averages |

### Layout Catalog

| Layout | Type ID | Best For | Features |
|--------|---------|----------|----------|
| **Quick Glance** | `quick-glance` | Racing, minimal distraction | Status text ("Bal, TE" when issues) + balance bar |
| **Power + Balance** | `power-balance` | Balance training | Large L/R percentage, balance bar, asymmetry indicator |
| **Efficiency** | `efficiency` | TE/PS focus | L/R TE and PS values, averages on large size |
| **Full Overview** | `full-overview` | All-around monitoring | Balance + TE + PS in compact grid |
| **Balance Trend** | `balance-trend` | Trend analysis | Current, 3s avg, 10s avg with trend arrows |
| **Balance** | `single-balance` | Full-screen balance | Large L/R with visual bar |
| **Live** | `live` | Ride analysis | All metrics + Time In Zone percentages |

### Layout Selection Guide

| Use Case | Recommended | Why |
|----------|-------------|-----|
| Racing / criteriums | Quick Glance | Minimal distraction, only alerts when needed |
| Balance training | Power + Balance, Balance Trend | Focus on L/R with trend information |
| Efficiency work | Efficiency | See TE and PS values clearly |
| General training | Full Overview | All metrics at a glance |
| Long rides | Live | See cumulative zones and averages |
| Rehab / injury prevention | Balance Trend | Track asymmetry over time |

---

## Training Drills

### Built-in Drills (10)

#### Timed Focus Drills

| Drill | Duration | Target | Description |
|-------|:--------:|--------|-------------|
| **Left Leg Focus** | 50s | Balance >55% | Warm-up → Left emphasis 30s → Recovery |
| **Right Leg Focus** | 50s | Balance <45% | Warm-up → Right emphasis 30s → Recovery |
| **Smooth Circles** | 75s | PS ≥25% | Warm-up → Smoothness focus 45s → Recovery |
| **Power Transfer** | 90s | TE 70-80% | Warm-up → Optimal TE zone 60s → Recovery |

#### Target-Based Drills

| Drill | Duration | Target | Hold Time |
|-------|:--------:|--------|:---------:|
| **Balance Challenge** | 60s | Balance 49-51% | 15s cumulative |
| **Smoothness Target** | 75s | PS ≥25% | 20s cumulative |
| **High Cadence Smoothness** | 75s | PS ≥20% @ 100+ rpm | 15s cumulative |

#### Guided Workouts

| Drill | Duration | Phases | Description |
|-------|:--------:|:------:|-------------|
| **Balance Recovery** | 3.5 min | 6 | Alternate leg focus with centering phases |
| **Efficiency Builder** | 10 min | 7 | Progressive: balance → TE → PS → combined |
| **Pedaling Mastery** | 15 min | 10 | Comprehensive technique session |

### Custom Drills

Create personalized drills with configurable parameters:

| Parameter | Options | Description |
|-----------|---------|-------------|
| **Metric** | Balance, TE, PS | Which metric to target |
| **Duration** | 10s - 10 min | Total drill duration |
| **Target Type** | MIN, MAX, RANGE, EXACT | How to evaluate success |
| **Target Value(s)** | 0-100% | Target percentage(s) |

### Drill Scoring

| Score | Rating | Badge |
|:-----:|--------|:-----:|
| 90-100% | Excellent | ⭐⭐⭐ |
| 75-89% | Good | ⭐⭐ |
| 60-74% | Fair | ⭐ |
| 40-59% | Needs Work | - |
| 0-39% | Keep Practicing | - |

---

## Achievements

### Categories

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

---

## Weekly Challenges

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

---

## Settings

### Thresholds

| Setting | Range | Default | Description |
|---------|:-----:|:-------:|-------------|
| **Balance Threshold** | ±1-10% | ±5% | Deviation from 50% for attention/problem |
| **TE Optimal Min** | 50-90% | 70% | Lower bound of optimal TE |
| **TE Optimal Max** | 55-100% | 80% | Upper bound of optimal TE |
| **PS Minimum** | 10-30% | 20% | Minimum PS for optimal status |

### Alerts

**Global:**
- Alerts Enabled (master switch)
- Screen Wake on alert

**Per-Metric (Balance, TE, PS):**
- Enabled / Disabled
- Trigger Level: Problem Only, Attention+
- Visual / Sound / Vibration
- Cooldown: 10-120 seconds

### Background Mode

| Setting | Default | Description |
|---------|:-------:|-------------|
| **Background Mode** | On | Collect data for all rides |
| **Auto-Sync** | On | Sync rides after completion |

---

## Ride Analysis

### Scoring Algorithm

**Overall Score** (0-100) = weighted components:

| Component | Weight | Calculation |
|-----------|:------:|-------------|
| Balance | 40% | Closeness to 50/50 |
| Efficiency | 35% | (TE score + PS score) / 2 |
| Consistency | 25% | % time in optimal zone |

### Score to Stars

| Score | Stars |
|:-----:|:-----:|
| 85-100 | ⭐⭐⭐⭐⭐ |
| 70-84 | ⭐⭐⭐⭐ |
| 55-69 | ⭐⭐⭐ |
| 40-54 | ⭐⭐ |
| 0-39 | ⭐ |

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

## Web Portal

### Domains

| Domain | Purpose |
|--------|---------|
| [kpedal.com](https://kpedal.com) | Landing page (guests), Dashboard (authenticated) |
| [app.kpedal.com](https://app.kpedal.com) | Web application (redirects to login if guest) |
| [link.kpedal.com](https://link.kpedal.com) | Device Code verification |
| [api.kpedal.com](https://api.kpedal.com) | REST API |

### Features

| Page | Features |
|------|----------|
| **Dashboard** | Score ring, balance display, weekly chart, zone distribution, recent rides |
| **Rides** | Full history, filters, search |
| **Ride Detail** | Performance strip, technique gauges, L/R comparison, timeline chart |
| **Drills** | Drill history, scores, phase breakdown |
| **Achievements** | Unlocked/locked grid, progress tracking |
| **Settings** | Account, connected devices, theme (light/dark/system) |

### Cloud Sync

**Link your Karoo:**
1. Settings → Link Account → Get 6-char code
2. Visit [link.kpedal.com](https://link.kpedal.com)
3. Enter code, sign in with Google
4. Done!

**What syncs:**
- ✅ Ride summaries + per-minute snapshots
- ✅ Drill results with phase scores
- ✅ Achievements with timestamps
- ✅ Settings (thresholds, alerts)
- ❌ GPS/routes (not collected)
- ❌ Custom drills (local only)
- ❌ Ride notes/ratings (local only)

### Device Management

From web Settings:
- View all linked Karoo devices
- See last sync time
- Request sync from device
- **Revoke device access** → immediate logout on device

---

## Web Development

### Tech Stack

| Component | Technology |
|-----------|------------|
| API | Cloudflare Workers + Hono |
| Database | Cloudflare D1 (SQLite) |
| Sessions | Cloudflare KV |
| Frontend | SvelteKit 5 + Cloudflare Pages |
| Auth | Google OAuth 2.0 |

### Project Structure

```
web/
├── api/                    # Cloudflare Worker
│   ├── src/
│   │   ├── index.ts        # Main app, routes
│   │   ├── auth/           # OAuth, JWT, Device Code Flow
│   │   ├── api/            # rides, sync, devices, settings, drills, achievements
│   │   ├── middleware/     # auth, rateLimit, validate
│   │   └── db/schema.sql   # D1 schema
│   └── wrangler.toml
│
└── app/                    # SvelteKit 5
    ├── src/
    │   ├── routes/         # Pages
    │   ├── lib/            # auth.ts, theme.ts, config.ts
    │   └── app.css         # Global styles
    └── svelte.config.js
```

### Build Commands

```bash
# ===== API =====
cd web/api
npm run dev              # Local server :8787
npm run deploy           # Deploy to Workers
npx vitest run           # Run tests

# ===== Frontend =====
cd web/app
npm run dev              # Local server :5173
npm run build            # Production build
npx wrangler pages deploy .svelte-kit/cloudflare --project-name=kpedal-web
```

### API Routes

| Route | Method | Description |
|-------|--------|-------------|
| `/auth/login` | GET | → Google OAuth |
| `/auth/device/code` | POST | Start Device Code Flow |
| `/auth/device/token` | POST | Poll for authorization |
| `/me` | GET | Current user profile |
| `/rides` | GET | List rides (paginated) |
| `/rides/:id` | GET/DELETE | Single ride |
| `/rides/dashboard` | GET | Combined: stats + rides + weekly + trends |
| `/sync/ride` | POST | Sync ride from Karoo |
| `/settings` | GET/PUT | User settings |
| `/devices` | GET | Linked devices |
| `/devices/:id` | DELETE | Revoke device |

---

## Requirements

| Requirement | Specification |
|-------------|---------------|
| **Device** | Hammerhead Karoo 2 or Karoo 3 |
| **Display** | 480×800 pixels |
| **Android** | API 23+ (Android 6.0+) |
| **Pedals** | ANT+ Cycling Dynamics (dual-sided) |

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

---

## License

MIT

---

## Links

| | |
|---|---|
| **Website** | [kpedal.com](https://kpedal.com) |
| **Web App** | [app.kpedal.com](https://app.kpedal.com) |
| **Device Link** | [link.kpedal.com](https://link.kpedal.com) |
| **GitHub** | [github.com/yrkan/kpedal](https://github.com/yrkan/kpedal) |
| **Issues** | [github.com/yrkan/kpedal/issues](https://github.com/yrkan/kpedal/issues) |

---

<p align="center">
  <b>KPedal</b> — Real-time pedaling efficiency for Karoo
</p>
