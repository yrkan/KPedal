# kpedal

Real-time pedaling efficiency extension for Hammerhead Karoo 2/3. Displays Balance, Torque Effectiveness, and Pedal Smoothness metrics from ANT+ Cycling Dynamics power meter pedals.

## Overview

kpedal provides cyclists with real-time feedback on pedaling technique during rides. The extension displays data fields on Karoo ride screens and includes a companion app for training drills, ride history, analytics, and achievement tracking.

**Key Features:**
- **Background mode** - Collects pedaling data for ALL rides, even without KPedal data fields on screen
- 6 customizable data field layouts for ride screens
- Real-time alerts for pedaling technique issues
- 10 built-in training drills with guided phases
- Custom drill creation
- Automatic ride history with analysis (up to 100 rides)
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

6 layouts available for Karoo ride screens:

| Layout | Type ID | Best For | Description |
|--------|---------|----------|-------------|
| **Quick Glance** | `quick-glance` | Quick status check | Status indicator + balance bar |
| **Power + Balance** | `power-balance` | Balance monitoring | Large L/R percentage display |
| **Efficiency** | `efficiency` | TE/PS focus | TE + PS with L/R values |
| **Full Overview** | `full-overview` | All metrics | Balance + TE + PS in one view |
| **Balance Trend** | `balance-trend` | Trend analysis | Current, 3s avg, 10s avg balance |
| **Single Balance** | `single-balance` | Full-screen | L/R balance large display |

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

**Target Types:**
- **MIN**: Value must be above threshold (e.g., PS ≥ 25%)
- **MAX**: Value must be below threshold (e.g., Balance ≤ 48%)
- **RANGE**: Value must be between min and max (e.g., TE 70-80%)
- **EXACT**: Value must be within tolerance of target (e.g., Balance 50% ± 2%)

### Drill Scoring

- **Score** = % of time spent in target zone (0-100%)
- Only phases WITH targets count toward score
- Warm-up/recovery phases (no target) are tracked but don't affect score
- Per-phase scores recorded for detailed feedback

**Score Ratings:**
| Score | Rating |
|-------|--------|
| 90%+ | Excellent |
| 75-89% | Good |
| 60-74% | Fair |
| 40-59% | Needs Work |
| <40% | Keep Practicing |

## Achievements (16)

### Ride Count (4)

| Achievement | Requirement |
|-------------|-------------|
| First Ride | Complete your first ride |
| Getting Started | Complete 10 rides |
| Dedicated | Complete 50 rides |
| Century | Complete 100 rides |

### Balance Mastery (3)

| Achievement | Requirement |
|-------------|-------------|
| Balanced | 1 minute at optimal balance |
| Well Balanced | 5 minutes at optimal balance |
| Master of Balance | 10 minutes at optimal balance |

### Efficiency (2)

| Achievement | Requirement |
|-------------|-------------|
| Efficient Rider | TE + PS optimal for 5 minutes |
| Smooth Operator | PS ≥25% for 10 minutes |

### Streaks (4)

| Achievement | Requirement |
|-------------|-------------|
| Getting Consistent | 3 day riding streak |
| Weekly Warrior | 7 day riding streak |
| Two Week Hero | 14 day riding streak |
| Monthly Master | 30 day riding streak |

### Drills (3)

| Achievement | Requirement |
|-------------|-------------|
| Practice Makes Perfect | Complete your first drill |
| Drill Enthusiast | Complete 10 drills |
| Perfect Form | Score 90%+ on any drill |

## Weekly Challenges

7 rotating challenges (based on week number):

| Challenge | Target | Type |
|-----------|--------|------|
| Active Week | Complete 3 rides | Rides |
| Balanced Rider | Avg balance 48-52% in 3 rides | Balance |
| Zone Master | 60%+ time in optimal zone | Zone time |
| Technique Focus | Complete 2 drills | Drills |
| Consistency | Ride 4 days in a row | Streak |
| Efficient Pedaling | Avg TE above 70% in 3 rides | TE |
| Smooth Circles | Avg PS above 20% in 3 rides | PS |

## Settings

### Configurable Thresholds

| Setting | Range | Default | Description |
|---------|-------|---------|-------------|
| Balance Threshold | ±1-10% | ±5% | Deviation from 50% to trigger attention/problem |
| TE Optimal Min | 50-90% | 70% | Lower bound of optimal TE range |
| TE Optimal Max | 55-100% | 80% | Upper bound of optimal TE range |
| PS Minimum | 10-30% | 20% | Minimum smoothness for optimal status |

### Status Thresholds Logic

**Balance** (based on threshold setting, default ±5%):
- Optimal: ≤ threshold/2 deviation (±2.5%)
- Attention: ≤ threshold deviation (±5%)
- Problem: > threshold deviation (>5%)

**Torque Effectiveness**:
- Optimal: within configured range (70-80%)
- Attention: within ±5% of range (65-85%)
- Problem: outside attention zone

**Pedal Smoothness**:
- Optimal: ≥ minimum threshold (20%)
- Attention: ≥ minimum - 5% (15%)
- Problem: < attention threshold

## Alerts

Configure real-time alerts during rides:

### Per-Metric Settings

| Setting | Options |
|---------|---------|
| Trigger Level | Problem Only, Attention+, Disabled |
| Visual Alert | InRideAlert overlay |
| Sound Alert | Beep pattern |
| Vibration | Device vibration |
| Cooldown | 10-120 seconds (per metric) |

### Global Settings

| Setting | Default | Description |
|---------|---------|-------------|
| Global Enable | On | Master on/off for all alerts |
| Screen Wake | On | Wake screen when alert triggers |

## Ride Analysis

### Automatic Scoring

**Overall Score** (0-100) calculated from:
- Balance Score: 40% weight
- Efficiency Score: 35% weight (TE 50% + PS 50%)
- Consistency Score: 25% weight (time in optimal zone)

### Star Rating

| Score | Rating |
|-------|--------|
| 85+ | 5 stars |
| 70-84 | 4 stars |
| 55-69 | 3 stars |
| 40-54 | 2 stars |
| <40 | 1 star |

### Auto-generated Insights

- Up to 3 strengths (e.g., "Excellent L/R balance", "Optimal torque effectiveness")
- Up to 3 improvement suggestions (e.g., "Focus on smoother pedal stroke")
- Summary based on overall score

## Live Summary

Real-time ride statistics accessible during a ride:

- **Duration**: Current ride time
- **Balance**: Current L/R % with trend indicator
- **TE/PS**: Left and right values with trends
- **Time in Zone**: Optimal/Attention/Problem percentages
- **Overall Score**: Real-time ride quality score (0-100)
- **Manual Save**: Save ride before auto-save on stop

## Analytics

- **7-day trends**: Recent performance overview
- **30-day trends**: Monthly progress tracking
- **Sparkline charts**: Visual trend indicators
- **Per-metric tracking**: Balance, TE, PS progress over time

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
| Power2Max NG/NGeco | Yes | No |

**Note:** Single-sided power meters do not provide these metrics.

## Installation

### From APK

```bash
adb install kpedal-1.0.0.apk
```

Or use Karoo's sideload feature.

### Build from Source

**Prerequisites:**

1. Android Studio with JDK 17
2. GitHub Personal Access Token with `read:packages` scope

**Setup:**

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

# Quick compile check
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew compileDebugKotlin

# Run unit tests
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test

# Run lint
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lint
```

**Output:** `app/build/outputs/apk/release/kpedal-1.0.0.apk`

## Usage

### Adding Data Fields to Ride Screen

1. **Profiles** → Select your profile → **Edit**
2. Add or edit a data page
3. Tap on an empty slot → **More Data** → **kpedal**
4. Select one of the 6 layouts
5. Save and start riding

### App Navigation

| Section | Description |
|---------|-------------|
| **Home** | Dashboard with stats, quick navigation |
| **Live** | Real-time ride metrics and statistics |
| **Drills** | Training drills list and custom drill creation |
| **History** | Saved rides with detailed analysis |
| **Analytics** | 7/30 day trends and progress charts |
| **Achievements** | Unlocked and locked badges |
| **Challenges** | Current weekly challenge with progress |
| **Data Fields** | Preview all 6 layout designs |
| **Pedal Status** | Connection status and signal quality |
| **Settings** | Thresholds and alert configuration |
| **Help** | Quick reference and onboarding |

### Pedal Status Indicators

| Status | Meaning |
|--------|---------|
| Connected | Receiving data normally |
| Stale | No data for >5 seconds |
| Disconnected | No data for >30 seconds |

### First Launch (Onboarding)

4-page onboarding for new users:
1. **Welcome** - Introduction to KPedal
2. **Data Fields** - How to add fields to Karoo screens
3. **Drills** - Overview of training drills
4. **Alerts** - Alert configuration

### Dashboard

Home screen shows:
- **Total rides** - Lifetime ride count
- **Avg balance** - All-time average L/R balance
- **Streak** - Current consecutive days riding
- **Last ride** - Date, balance, optimal zone %

### Background Mode

KPedal runs as a background service to collect pedaling metrics for **all rides**, even when KPedal data fields are not on your ride screen.

**How it works:**
1. Service starts automatically when Karoo boots
2. Monitors ride state (recording/paused/stopped)
3. When any ride starts, begins collecting Balance, TE, PS metrics
4. When ride ends, automatically saves analysis to history

**Benefits:**
- Never miss ride data - all rides are analyzed
- No need to configure data fields to get ride history
- Retroactive analysis even for rides without data fields

**Note:** A persistent notification "KPedal - Monitoring pedaling metrics" appears in the notification shade. This is required by Android for background services.

## Requirements

- **Device**: Hammerhead Karoo 2 or Karoo 3
- **Display**: 480×800 pixels
- **OS**: Karoo OS 1.524+
- **Pedals**: ANT+ Cycling Dynamics compatible (dual-sided)
- **Android**: minSdk 23, targetSdk 34

## Technical Details

### SDK & Libraries

| Library | Version |
|---------|---------|
| Karoo Extension SDK | 1.1.7 |
| Kotlin | 1.9.20 |
| Compose BOM | 2023.10.01 |
| Room Database | 2.6.1 |
| Navigation Compose | 2.7.5 |
| Kotlin Coroutines | 1.7.3 |
| JUnit 5 | 5.10.1 |

### Data Streams

```
DataType.Type.PEDAL_POWER_BALANCE → Field.PEDAL_POWER_BALANCE_LEFT
DataType.Type.TORQUE_EFFECTIVENESS → Field.TORQUE_EFFECTIVENESS_LEFT/RIGHT
DataType.Type.PEDAL_SMOOTHNESS → Field.PEDAL_SMOOTHNESS_LEFT/RIGHT
DataType.Type.SMOOTHED_3S_AVERAGE_PEDAL_POWER_BALANCE
DataType.Type.SMOOTHED_10S_AVERAGE_PEDAL_POWER_BALANCE
```

### Architecture

```
io.github.kpedal/
├── KPedalExtension.kt        # KarooExtension entry point
├── MainActivity.kt           # Compose navigation
├── MainViewModel.kt          # State management
├── datatypes/                # 6 RemoteViews DataType layouts
│   ├── BaseDataType.kt       # Common lifecycle handling
│   ├── QuickGlanceDataType.kt
│   ├── PowerBalanceDataType.kt
│   ├── EfficiencyDataType.kt
│   ├── FullOverviewDataType.kt
│   ├── BalanceTrendDataType.kt
│   └── SingleBalanceDataType.kt
├── drill/                    # Drill system
│   ├── DrillCatalog.kt       # 10 built-in drills
│   ├── DrillEngine.kt        # Execution engine
│   ├── DrillRepository.kt    # Results storage
│   ├── CustomDrillRepository.kt
│   └── model/                # Drill data models
├── engine/                   # Core engine
│   ├── PedalingEngine.kt     # Central data pipeline
│   ├── PedalingMetrics.kt    # Data model
│   ├── StatusCalculator.kt   # Threshold evaluation
│   ├── AlertManager.kt       # In-ride alerts
│   ├── LiveDataCollector.kt  # Ride statistics
│   ├── RideStateMonitor.kt   # Ride state tracking
│   ├── AchievementChecker.kt # Achievement logic
│   └── PedalMonitor.kt       # Connection status
├── data/                     # Persistence
│   ├── PreferencesRepository.kt  # Settings (DataStore)
│   ├── RideRepository.kt         # Ride history
│   ├── AchievementRepository.kt  # Achievements
│   ├── AnalyticsRepository.kt    # Trend calculations
│   ├── database/                 # Room entities & DAOs
│   └── models/                   # Data models
└── ui/                       # Compose UI
    ├── theme/                # Colors, typography
    ├── components/           # Reusable components
    └── screens/              # App screens
```

### Database

- **Engine**: Room Database v5
- **File**: `kpedal_rides.db`
- **Tables**: rides, drill_results, achievements, custom_drills
- **Migration**: Fallback to destructive

### Performance Optimizations

| Component | Optimization |
|-----------|-------------|
| PedalingEngine | 50ms debouncing to prevent RemoteViews overload |
| LiveDataCollector | 1 second emission intervals |
| AlertManager | Single collector pattern with `combine()` |
| PreferencesRepository | `distinctUntilChanged()` filtering |

### Thread Safety

- `AtomicReference` for consumer IDs
- `AtomicLong` for cooldown timestamps
- `@Volatile` for simple boolean flags
- `synchronized` lock in SummaryCollector

## License

MIT

---

**kpedal** — Real-time pedaling efficiency for Karoo
