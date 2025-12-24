# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

kpedal is a Karoo 3 extension that displays pedaling efficiency metrics (Balance, Torque Effectiveness, Pedal Smoothness) in real-time using data from power meter pedals via ANT+ Cycling Dynamics.

## Build Commands

```bash
# Build debug APK
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug

# Build release APK
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease

# Compile only (faster for checking errors)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew compileDebugKotlin

# Run unit tests (JUnit 5)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew test

# Run Android lint
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lint

# Clean build
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean
```

**Prerequisites:**
- GitHub Personal Access Token with `read:packages` scope in `~/.gradle/gradle.properties`:
  ```properties
  gpr.user=YOUR_GITHUB_USERNAME
  gpr.key=YOUR_GITHUB_TOKEN
  ```

Output APK: `app/build/outputs/apk/release/kpedal-x.x.x.apk`

## Architecture

### Core Components

- **KPedalExtension** (`KPedalExtension.kt`): Main entry point extending `KarooExtension`. Runs as Android Foreground Service to collect data for ALL rides (not just when DataTypes are on screen). Manages KarooSystemService connection and registers 6 DataType implementations. Singleton accessed via `KPedalExtension.instance`. Initializes all engine components on connect.

- **BootReceiver** (`BootReceiver.kt`): Starts KPedalExtension on device boot via BOOT_COMPLETED broadcast.

- **PedalingEngine** (`engine/PedalingEngine.kt`): Central data pipeline that subscribes to Karoo SDK streams and emits combined `PedalingMetrics` via StateFlow. Uses 50ms debouncing to prevent UI overload. Manages consumer IDs with AtomicReference for thread-safe cleanup.

- **StatusCalculator** (`engine/StatusCalculator.kt`): Stateless object that evaluates metrics against research-based thresholds:
  - Balance: ±2.5% optimal, ±5% attention, >5% problem
  - TE: 70-80% optimal (NOT higher - per Wattbike research)
  - PS: ≥20% optimal

- **AlertManager** (`engine/AlertManager.kt`): Manages real-time in-ride alerts via `InRideAlert`. Uses `combine()` for efficient single-collector approach. Supports vibration, beeps, and screen wake.

- **LiveDataCollector** (`engine/LiveDataCollector.kt`): Aggregates ride statistics (averages, time-in-zone) for live display. Emits every 1 second (not on every metrics update). Uses thread-safe accumulators with synchronized lock.

- **DrillEngine** (`drill/DrillEngine.kt`): Executes guided training drills with phases, targets, and scoring. Tracks time-in-target for each phase.

- **RideStateMonitor** (`engine/RideStateMonitor.kt`): Monitors ride state (recording/stopped) via Karoo SDK. Auto-saves rides on stop, triggers achievement checks.

- **AchievementChecker** (`engine/AchievementChecker.kt`): Checks and unlocks achievements after ride saves and drill completions.

- **PedalMonitor** (`engine/PedalMonitor.kt`): Tracks pedal connection status and signal quality based on data freshness.

### Data Flow

```
Karoo SDK Streams → PedalingEngine → StateFlow<PedalingMetrics> → DataType views
                         ↓                                      ↘
                  LiveDataCollector                         AlertManager
                  (ride statistics)                         (in-ride alerts)
```

### Two UI Systems

**1. DataTypes (RemoteViews)** - For Karoo data fields during rides:
- Located in `datatypes/` folder
- Uses Android RemoteViews with XML layouts in `res/layout/datatype_*.xml`
- Extends `BaseDataType` which handles lifecycle (scope, StateFlow collection)
- Subclasses only implement `getLayoutResId()` and `updateViews()`

**2. App Screens (Jetpack Compose)** - For settings and drills:
- Located in `ui/screens/` folder
- Pure Compose UI with Theme colors
- Navigation via NavHost in `MainActivity.kt`

### DataTypes (6 layouts)

- QuickGlanceDataType - Status indicator (shows "Bal, TE" when issues) + balance bar
- PowerBalanceDataType - Large L/R balance focus
- EfficiencyDataType - TE + PS with averages
- FullOverviewDataType - All metrics compact
- BalanceTrendDataType - Compact grid with trends
- SingleBalanceDataType - Full screen balance

### Drills System

**DrillCatalog** (`drill/DrillCatalog.kt`): Contains all drill definitions with phases, targets, and instructions.

**DrillEngine** lifecycle:
1. `startDrill(drill)` → sets up phases and callbacks
2. Countdown (3-2-1) → COUNTDOWN status
3. Phase execution → RUNNING status, tracks `isInTarget`, `targetHoldMs`
4. Phase transitions → `onPhaseChange` callback
5. Completion → `onComplete` callback with `DrillResult`

**Drill screens** (`ui/screens/drills/`):
- DrillsListScreen → DrillDetailScreen → DrillExecutionScreen → DrillResultScreen
- DrillHistoryScreen for past results

### Navigation Routes

```
home → drills → drill-detail/{drillId} → drill-execution/{drillId} → drill-result/{drillId}
     → drill-history
     → settings → alert-settings
     → live
     → history → ride-detail/{rideId}
     → analytics
     → achievements
     → challenges
     → help
     → pedal-info
     → onboarding
     → quick-glance, power-balance, efficiency, full-overview, balance-trend, single-balance
```

### Key Patterns

- **Balance convention**: `PedalingMetrics.balance` stores RIGHT side percentage; left = 100 - balance
- **Color semantics**: White = normal, Green = optimal, Yellow = attention, Red = problem
- **Thread safety**: AtomicReference for consumer IDs, AtomicLong for cooldown timestamps, @Volatile for simple values

### UI/UX Patterns (Compose Screens)

- **Headers**: Row with `←` back button (no ripple: `indication = null`) + title + optional right action
- **Lists**: No card backgrounds, use `Divider()` between items
- **Buttons**: `RoundedCornerShape(8.dp)`, `padding(vertical = 16.dp)`, `fontSize = 15.sp`
- **Dialogs**: Simple text links (Cancel/Delete), not buttons with backgrounds
- **Colors**: Use `Theme.colors.*` (optimal, attention, problem, text, dim, muted, divider)

### Persistence Layer

- **PreferencesRepository** (`data/PreferencesRepository.kt`): DataStore-based settings. Uses `distinctUntilChanged()` to prevent duplicate emissions.
- **RideRepository** (`data/RideRepository.kt`): Room database for ride history
- **DrillRepository** (`drill/DrillRepository.kt`): Stores drill results with scores
- **AchievementRepository** (`data/AchievementRepository.kt`): Room database for unlocked achievements
- **AnalyticsRepository** (`data/AnalyticsRepository.kt`): Calculates trend data for 7/30 day periods
- **CustomDrillRepository** (`drill/CustomDrillRepository.kt`): Stores user-created custom drills

### Database (Room)

Current version: 5

Tables:
- `rides` - Ride history with metrics averages and zones
- `drill_results` - Drill execution results with scores
- `achievements` - Unlocked achievements with timestamps
- `custom_drills` - User-created drills

Migration strategy: Fallback to destructive migration

### Background Mode

KPedal runs as a Foreground Service to collect pedaling metrics for ALL rides, even when KPedal DataTypes are not on the ride screen.

**How it works:**
1. **BootReceiver** starts KPedalExtension on device boot
2. **KPedalExtension** runs as Foreground Service with persistent notification
3. **RideStateMonitor** listens for RideState changes (Recording/Paused/Idle)
4. When any ride starts (RideState.Recording):
   - PedalingEngine.startStreaming() - subscribes to data streams
   - AlertManager.startMonitoring() - enables alerts
   - PedalMonitor.startMonitoring() - tracks connection
   - LiveDataCollector.startCollecting() - begins aggregating
5. When ride ends (RideState.Idle):
   - All components stop to save resources
   - Ride is auto-saved to database

**Resource optimization:** Components only run during active rides. Between rides, only RideStateMonitor is active (minimal overhead - just listening for state changes).

**Manifest permissions:**
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` - for background operation
- `RECEIVE_BOOT_COMPLETED` - to start on device boot
- `POST_NOTIFICATIONS` - for foreground service notification

**Important:** RideStateMonitor is the orchestrator - it starts/stops PedalingEngine, AlertManager, PedalMonitor and LiveDataCollector based on ride state.

### Performance Notes

- **LiveDataCollector**: Emits every 1 second (not on every metrics update ~20Hz) to reduce UI load
- **AlertManager**: Uses `combine()` for single collector instead of 3 separate collectors
- **PreferencesRepository**: Uses `distinctUntilChanged()` to prevent duplicate emissions
- **PedalingEngine**: Uses 50ms debouncing to prevent RemoteViews overload

## SDK Reference

Uses karoo-ext SDK v1.1.7. Key stream types:
- `DataType.Type.PEDAL_POWER_BALANCE` → `Field.PEDAL_POWER_BALANCE_LEFT`
- `DataType.Type.TORQUE_EFFECTIVENESS` → `Field.TORQUE_EFFECTIVENESS_LEFT/RIGHT`
- `DataType.Type.PEDAL_SMOOTHNESS` → `Field.PEDAL_SMOOTHNESS_LEFT/RIGHT`
- `DataType.Type.SMOOTHED_3S_AVERAGE_PEDAL_POWER_BALANCE`
- `DataType.Type.SMOOTHED_10S_AVERAGE_PEDAL_POWER_BALANCE`

## Target Device

Hammerhead Karoo 2/3 (480×800px display), minSdk 23, targetSdk 34

## Known Issues

- Edge gestures can conflict with scrolling (Karoo gesture navigation)
- Long text in drill instructions needs `textAlign = TextAlign.Center` for proper centering
- Karoo 3 display is 480×800px - keep UI compact, avoid large padding
- Use `lineHeight` for multi-line Text to prevent excessive spacing on wrap
