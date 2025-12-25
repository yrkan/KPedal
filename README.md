# KPedal

Real-time pedaling efficiency extension for Hammerhead Karoo 2/3. Displays Balance, Torque Effectiveness, and Pedal Smoothness metrics from ANT+ Cycling Dynamics power meter pedals.

## Overview

KPedal provides cyclists with real-time feedback on pedaling technique during rides. The extension displays data fields on Karoo ride screens and includes a companion app for training drills, ride history, analytics, and achievement tracking.

**Key Features:**
- **Background mode** - Collects pedaling data for ALL rides, even without KPedal data fields on screen
- **Cloud sync** - Sync rides and settings to kpedal.com across devices
- 7 customizable data field layouts for ride screens
- Real-time alerts for pedaling technique issues
- 10 built-in training drills with guided phases
- Custom drill creation
- Automatic ride history with analysis
- 7-day and 30-day trend analytics
- 16 achievements to unlock
- 7 rotating weekly challenges

## Pedaling Metrics

| Metric | Description | Optimal Range | Research Basis |
|--------|-------------|---------------|----------------|
| **Balance** | Left/Right power distribution | 48-52% | Pro cyclists maintain ±2% |
| **TE** | Torque Effectiveness | 70-80% | Wattbike: >80% reduces total power |
| **PS** | Pedal Smoothness | ≥20% | Elite cyclists: 25-35% |

### Status Colors

| Color | Status | Meaning |
|-------|--------|---------|
| White | Normal | Within acceptable range |
| Green | Optimal | Ideal performance zone |
| Yellow | Attention | Monitor and adjust |
| Red | Problem | Needs immediate correction |

### Research Notes

**Balance:**
- Pro cyclists: 48-52% (±2%)
- Amateur cyclists: 45-55% is normal
- 2-5% asymmetry is natural and changes with fatigue
- Complete symmetry (50/50) is not required

**Torque Effectiveness:**
- **80%+ is NOT better** - this is a common misconception
- Above 80%, power on the main downstroke phase decreases
- Optimal zone: 70-80% (based on Wattbike research)
- Very high TE means "stealing" power from your strongest phase

**Pedal Smoothness:**
- Good cyclists: 20-30%
- Elite cyclists: 25-35%
- 100% would be perfectly circular motion (unrealistic)
- Lower values during sprints and high power is normal

## Data Field Layouts

7 layouts available for Karoo ride screens. Each layout automatically adapts to the grid size:
- **Small** (< 20 rows) - Compact view with essential metrics
- **Medium** (20-40 rows) - Balanced layout
- **Large** (> 40 rows) - Full details with bars and zones

| Layout | Type ID | Best For | Description |
|--------|---------|----------|-------------|
| **Quick Glance** | `quick-glance` | Quick status check | Status indicator + balance bar |
| **Power + Balance** | `power-balance` | Balance monitoring | Large L/R percentage display |
| **Efficiency** | `efficiency` | TE/PS focus | TE + PS with L/R values |
| **Full Overview** | `full-overview` | All metrics | Balance + TE + PS in one view |
| **Balance Trend** | `balance-trend` | Trend analysis | Current, 3s avg, 10s avg balance |
| **Balance** | `single-balance` | Full-screen | L/R balance large display |
| **Live** | `live` | Ride summary | All metrics with Time In Zone stats |

## Training Drills

### Built-in Drills (10)

#### Timed Focus Drills (4)

| Drill | Duration | Metric | Difficulty | Description |
|-------|----------|--------|------------|-------------|
| **Left Leg Focus** | 50s | Balance | Beginner | Emphasize left leg power for 30s |
| **Right Leg Focus** | 50s | Balance | Beginner | Emphasize right leg power for 30s |
| **Smooth Circles** | 75s | PS | Intermediate | Maximize smoothness for 45s |
| **Power Transfer** | 90s | TE | Intermediate | Optimize TE in 70-80% zone for 60s |

#### Target-Based Drills (3)

| Drill | Duration | Metric | Difficulty | Description |
|-------|----------|--------|------------|-------------|
| **Balance Challenge** | 60s | Balance | Intermediate | Hold 50/50 balance for 15s |
| **Smoothness Target** | 75s | PS | Advanced | Hold PS ≥25% for 20s |
| **High Cadence Smoothness** | 75s | PS | Advanced | Maintain PS ≥20% at 100+ rpm for 15s |

#### Guided Workouts (3)

| Drill | Duration | Phases | Difficulty | Description |
|-------|----------|--------|------------|-------------|
| **Balance Recovery** | 3.5 min | 6 | Beginner | Alternate leg focus with centering |
| **Efficiency Builder** | 10 min | 7 | Intermediate | All efficiency metrics workout |
| **Pedaling Mastery** | 15 min | 10 | Advanced | Comprehensive technique session |

### Custom Drills

Create your own drills with configurable parameters:

| Parameter | Options |
|-----------|---------|
| **Name** | Custom drill name |
| **Metric** | Balance, Torque Effectiveness, Pedal Smoothness |
| **Duration** | 10s - 10 min |
| **Target Type** | MIN (above), MAX (below), RANGE, EXACT |

### Drill Scoring

- **Score** = % of time spent in target zone (0-100%)
- Only phases WITH targets count toward score
- Warm-up/recovery phases (no target) are tracked but don't affect score

| Score | Rating |
|-------|--------|
| 90%+ | Excellent |
| 75-89% | Good |
| 60-74% | Fair |
| 40-59% | Needs Work |
| <40% | Keep Practicing |

## Achievements (16)

| Category | Achievements |
|----------|-------------|
| **Ride Count** | First Ride, Getting Started (10), Dedicated (50), Century (100) |
| **Balance** | Balanced (1 min), Well Balanced (5 min), Master of Balance (10 min) |
| **Efficiency** | Efficient Rider (5 min TE+PS), Smooth Operator (10 min PS≥25%) |
| **Streaks** | 3-day, 7-day, 14-day, 30-day riding streaks |
| **Drills** | First drill, 10 drills, Perfect Form (90%+ score) |

## Weekly Challenges

7 rotating challenges (based on week number):

| Challenge | Target |
|-----------|--------|
| Active Week | Complete 3 rides |
| Balanced Rider | Avg balance 48-52% in 3 rides |
| Zone Master | 60%+ time in optimal zone |
| Technique Focus | Complete 2 drills |
| Consistency | Ride 4 days in a row |
| Efficient Pedaling | Avg TE above 70% in 3 rides |
| Smooth Circles | Avg PS above 20% in 3 rides |

## Settings

### Configurable Thresholds

| Setting | Range | Default | Description |
|---------|-------|---------|-------------|
| Balance Threshold | ±1-10% | ±5% | Deviation from 50% to trigger attention/problem |
| TE Optimal Min | 50-90% | 70% | Lower bound of optimal TE range |
| TE Optimal Max | 55-100% | 80% | Upper bound of optimal TE range |
| PS Minimum | 10-30% | 20% | Minimum smoothness for optimal status |

### Alerts

Configure real-time alerts during rides:

| Setting | Options |
|---------|---------|
| Trigger Level | Problem Only, Attention+, Disabled |
| Visual Alert | InRideAlert overlay |
| Sound Alert | Beep pattern |
| Vibration | Device vibration |
| Cooldown | 10-120 seconds per metric |
| Screen Wake | Wake screen when alert triggers |

## Ride Analysis

### Automatic Scoring

**Overall Score** (0-100) calculated from:
- Balance Score: 40% weight
- Efficiency Score: 35% weight (TE 50% + PS 50%)
- Consistency Score: 25% weight (time in optimal zone)

| Score | Rating |
|-------|--------|
| 85+ | 5 stars |
| 70-84 | 4 stars |
| 55-69 | 3 stars |
| 40-54 | 2 stars |
| <40 | 1 star |

## Compatible Pedals

Requires dual-sided power meter with ANT+ Cycling Dynamics support:

| Pedals | Balance | TE/PS |
|--------|---------|-------|
| Garmin Rally RS/RK | Yes | Yes |
| Garmin Vector 3 | Yes | Yes |
| Favero Assioma DUO | Yes | Yes |
| Wahoo POWRLINK Zero | Yes | No |
| SRM X-Power | Yes | Yes |
| Rotor 2INpower | Yes | Yes |

**Note:** Single-sided power meters do not provide these metrics.

## Installation

### From APK

```bash
adb install kpedal-x.x.x.apk
```

Or use Karoo's sideload feature.

### Build from Source

**Prerequisites:**
1. Android Studio with JDK 17
2. GitHub Personal Access Token with `read:packages` scope

Add to `~/.gradle/gradle.properties`:
```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

**Build Commands:**

```bash
# Release APK (minified)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease

# Debug APK
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug

# Run unit tests
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test
```

**Output:** `app/build/outputs/apk/release/kpedal-x.x.x.apk`

## Usage

### Adding Data Fields to Ride Screen

1. **Profiles** → Select your profile → **Edit**
2. Add or edit a data page
3. Tap on an empty slot → **More Data** → **kpedal**
4. Select one of the 7 layouts
5. Save and start riding

### App Navigation

| Section | Description |
|---------|-------------|
| **Home** | Dashboard with stats, quick navigation |
| **Live** | Real-time ride metrics and statistics |
| **Drills** | Training drills and custom drill creation |
| **History** | Saved rides with detailed analysis |
| **Analytics** | 7/30 day trends and progress charts |
| **Achievements** | Unlocked and locked badges |
| **Challenges** | Current weekly challenge with progress |
| **Settings** | Thresholds, alerts, cloud sync |

### Background Mode

KPedal runs as a background service to collect pedaling metrics for **all rides**, even when KPedal data fields are not on your ride screen.

- Service starts automatically when Karoo boots
- All rides are analyzed and saved to history
- A persistent notification appears (required by Android)

## Requirements

- **Device**: Hammerhead Karoo 2 or Karoo 3
- **Display**: 480×800 pixels
- **Pedals**: ANT+ Cycling Dynamics compatible (dual-sided)

---

## Web Portal

KPedal includes a web portal at **kpedal.com** for viewing ride data across devices.

### Features

| Feature | Description |
|---------|-------------|
| **Dashboard** | Score ring, weekly chart, insights, recent rides |
| **Ride History** | Browse all synced rides with detailed metrics |
| **Settings Sync** | Thresholds and alert settings sync with Karoo |
| **Device Management** | View linked devices, request sync, revoke access |
| **Dark/Light Mode** | WCAG AA compliant themes |

### Cloud Sync

**How to link your Karoo:**
1. In app Settings → Link Account
2. App displays a 6-character code
3. Go to **kpedal.com/link** on any browser
4. Enter the code and sign in with Google
5. App automatically links

**Sync behavior:**
- Rides sync after completion (if Auto-sync enabled)
- Settings changes auto-upload (2s debounce)
- Press Sync to pull latest from cloud
- Changes on web apply on next Sync or ride start

### Privacy

**Data synced:** Ride summaries, settings, device info

**NOT synced:** GPS/routes, raw power data, drill results, achievements

See full policy at **kpedal.com/privacy**

---

## Web Development

### Architecture

```
web/
├── api/          # Cloudflare Worker (Hono)
│   └── src/
│       ├── auth/     # OAuth, JWT, Device Code Flow
│       ├── api/      # rides, sync, settings endpoints
│       └── db/       # D1 schema
└── app/          # SvelteKit 5 (Cloudflare Pages)
    └── src/
        ├── routes/   # /, /login, /link, /rides, /settings
        └── lib/      # auth, theme, config
```

### Build Commands

```bash
# API
cd web/api
npm run dev              # Local dev (port 8787)
npm run deploy           # Deploy to Workers
npx vitest run           # Run tests

# Frontend
cd web/app
npm run dev              # Local dev (port 5173)
npm run build            # Production build
npx wrangler pages deploy .svelte-kit/cloudflare
```

### Infrastructure

| Component | Service |
|-----------|---------|
| API | Cloudflare Workers (api.kpedal.com) |
| Frontend | Cloudflare Pages (kpedal.com) |
| Database | Cloudflare D1 (SQLite) |
| Sessions | Cloudflare KV |

---

## Technical Details

### SDK & Libraries

| Library | Version |
|---------|---------|
| Karoo Extension SDK | 1.1.7 |
| Kotlin | 1.9.20 |
| Compose BOM | 2023.10.01 |
| Room Database | 2.6.1 |

### Architecture

```
io.github.kpedal/
├── KPedalExtension.kt        # Foreground Service entry point
├── MainActivity.kt           # Compose navigation
├── datatypes/                # 7 RemoteViews DataType layouts
├── drill/                    # Drill system (catalog, engine, repository)
├── engine/                   # Core (PedalingEngine, AlertManager, etc.)
├── data/                     # Persistence (Room, DataStore)
├── api/                      # Cloud sync (Retrofit)
└── ui/                       # Compose screens
```

### Performance

| Component | Optimization |
|-----------|-------------|
| PedalingEngine | 50ms debouncing |
| LiveDataCollector | 1 second emissions |
| AlertManager | Single collector with `combine()` |
| Settings sync | 2 second debounce for uploads |

---

## License

MIT

---

**KPedal** — Real-time pedaling efficiency for Karoo
