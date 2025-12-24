# kpedal — Application Specification

**Version:** 1.0  
**Platform:** Hammerhead Karoo 3  
**Type:** Data Field Extension (karoo-ext SDK)  
**Date:** December 2024

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Research & Metrics](#2-research--metrics)
3. [Design System](#3-design-system)
4. [Screens & Layouts](#4-screens--layouts)
5. [Status Logic](#5-status-logic)
6. [Technical Requirements](#6-technical-requirements)
7. [Project Structure](#7-project-structure)
8. [UI Components](#8-ui-components)
9. [User Interaction](#9-user-interaction)
10. [Alerts & Notifications](#10-alerts--notifications)
11. [Data Storage](#11-data-storage)

---

## 1. Product Overview

### 1.1 What It Is

**kpedal** is an extension for the Karoo 3 bike computer that displays pedaling efficiency metrics in real-time. The app visualizes data from power meter pedals (Garmin Rally, Favero Assioma, etc.) in a clear and readable format.

### 1.2 The Problem

Standard Karoo data fields show only numbers without context:
- Unclear if the value is good or bad
- Need to memorize optimal ranges
- Difficult to track multiple metrics simultaneously
- Distracts from riding

### 1.3 The Solution

kpedal uses **color as status**:
- **White** = normal, everything is fine
- **Colored** = needs attention

One glance — and you know if something needs adjusting.

### 1.4 Target Audience

- Cyclists with power meter pedals
- Triathletes working on technique
- Coaches analyzing athlete technique
- Enthusiasts looking to improve efficiency

### 1.5 Naming

**kpedal** — follows Karoo extension naming convention:
- Ki2 (Shimano Di2)
- KRemote (remote control)
- KPower (power metrics)
- **kpedal** (pedaling metrics)

---

## 2. Research & Metrics

### 2.1 Displayed Metrics

| Metric | Description | Data Source |
|--------|-------------|-------------|
| **Balance** | Left/right power balance | ANT+ Power Balance |
| **TE** | Torque Effectiveness — torque efficiency | ANT+ Cycling Dynamics |
| **PS** | Pedal Smoothness — pedaling smoothness | ANT+ Cycling Dynamics |

### 2.2 Balance (L/R Balance)

**What it measures:** Power distribution between left and right leg as a percentage.

**Research findings:**
- Professional cyclists: 48-52% (±2%)
- Amateurs: 45-55% (±5%) — normal
- 2-5% asymmetry — natural and changes with fatigue
- Balance usually evens out at higher intensities

**Optimal values:**
```
48-52%   → Excellent (green)
45-55%   → Acceptable (yellow)
<45/>55% → Problem (red)
```

**Sources:**
- TrainingPeaks: "Understanding Left/Right Balance"
- Wattbike research on pedaling asymmetry

### 2.3 Torque Effectiveness (TE)

**What it measures:** What percentage of applied force actually pushes the pedal forward (vs. pushing down or pulling back inefficiently).

**Key finding:** TE above 80% is NOT better!
- At TE > 80%, power on the main (downstroke) phase decreases
- Optimal: 70-80% according to Wattbike research
- Too high TE = "stealing" power from the strong phase

**Optimal values:**
```
70-80%   → Optimal (green)
65-85%   → Acceptable (yellow)
<65/>85% → Needs attention (red)
```

**Sources:**
- Wattbike Pedal Effectiveness Score (PES)
- "Cyclists' Improvement of Pedaling Efficacy" — research paper

### 2.4 Pedal Smoothness (PS)

**What it measures:** How evenly force is distributed throughout the entire pedal revolution. 100% = perfectly circular motion (unrealistic).

**Real-world values:**
- Good cyclists: 20-30%
- Elite: 25-35%
- Mountain bikers: higher (need traction)

**Optimal values:**
```
≥20%    → Good (green)
15-19%  → Acceptable (yellow)
<15%    → Rough, needs work (red)
```

### 2.5 Why These Metrics Matter

| Metric | Practical Benefit |
|--------|-------------------|
| Balance | Identifies fit issues, injuries, muscle imbalances |
| TE | Shows how efficiently force is being used |
| PS | Indicator of fatigue and technique quality |

### 2.6 When NOT to Worry

- **Small asymmetry** (2-5%) — normal
- **Changes with fatigue** — expected
- **Different L/R values for TE/PS** — everyone has this
- **Low PS during sprints** — normal (power matters more)

---

## 3. Design System

### 3.1 Design Principles

```
1. ONE GLANCE = UNDERSTANDING
   No need to read numbers to understand status

2. COLOR = STATUS
   Green = normal, Yellow = attention, Red = problem

3. MINIMAL INFORMATION
   Only what's needed for decision making

4. KAROO NATIVE STYLE
   White text, dark background, no decorations
```

### 3.2 Color Palette

```javascript
const Colors = {
  // Background
  bg: '#000000',      // Black background
  
  // Text
  text: '#FFFFFF',    // Primary (white)
  dim: '#666666',     // Labels
  muted: '#333333',   // Dividers
  
  // Status
  ok: '#4CAF50',      // Green — optimal
  warn: '#FFC107',    // Yellow — attention
  bad: '#F44336',     // Red — problem
}
```

### 3.3 Color Application

| Element | Color | Condition |
|---------|-------|-----------|
| Number (metric) | `#FFFFFF` | Within normal range |
| Number (metric) | `ok/warn/bad` | Outside normal range |
| Header | `#666666` | Always |
| Average value | Status color | Based on status |
| Dividers | `#333333` | Always |
| Background | `#000000` | Always |

### 3.4 Typography

```
Numbers (primary):  text-5xl / text-4xl / text-3xl / text-2xl
                    font-bold, tabular-nums
                    
Headers:            text-xs, font-medium, uppercase
                    color: dim (#666)

Labels (L/R):       text-xs
                    color: dim (#666)
```

**Important:** `tabular-nums` — so numbers don't jump when changing.

### 3.5 Layout

```
┌─────────────────────────┐
│ LABEL              AVG% │  ← Header: dim, xs
├─────────────────────────┤
│                         │
│   52           48       │  ← Numbers: bold, large
│   L             R       │  ← Labels: dim, xs
│                         │
└─────────────────────────┘
```

### 3.6 Dividers

- Horizontal: `height: 1px`, `background: #222`
- Vertical: `width: 1px`, `background: #333`
- No shadows, no gradients

---

## 4. Screens & Layouts

### 4.1 Screen Overview

| Screen | Purpose | When Used |
|--------|---------|-----------|
| **Layouts** | 6 data field variants | During ride |
| **Settings** | Threshold and alert settings | Before ride |
| **Summary** | Post-ride statistics | After ride |

---

### 4.2 Layout 1: Quick Glance

**Purpose:** Quick assessment — all OK or issues exist.

```
┌─────────────────────────┐
│                         │
│           ✓             │  40%
│       ALL GOOD          │
│                         │
├─────────────────────────┤
│ BALANCE                 │
│                         │
│   52           48       │  60%
│   ████████│████████     │
│   L             R       │
└─────────────────────────┘
```

**Components:**
- `StatusIndicator` — shows ✓ or ! with issue count
- `Balance` — primary metric

**StatusIndicator logic:**
```javascript
const allOk = teOk && psOk && balanceOk;
// If all OK: green ✓, "ALL GOOD"
// If issues exist: yellow/red !, "X ISSUES"
```

---

### 4.3 Layout 2: Balance Focus

**Purpose:** Working on pedaling symmetry.

```
┌─────────────────────────┐
│ BALANCE                 │
│                         │
│   52           48       │  50%
│   ████████│████████     │
│   L             R       │
├─────────────────────────┤
│  TE L   │   TE R        │
│   74    │    77         │  50%
│         │               │
└─────────────────────────┘
```

**Components:**
- `Balance` — large, main focus
- `Compact` × 2 — TE for left and right leg

---

### 4.4 Layout 3: Efficiency

**Purpose:** Pedaling technique training.

```
┌─────────────────────────┐
│ TE                  76% │
│                         │
│   74           77       │  50%
│   L             R       │
├─────────────────────────┤
│ PS                  24% │
│                         │
│   23           25       │  50%
│   L             R       │
└─────────────────────────┘
```

**Components:**
- `TorqueEff` — torque effectiveness
- `PedalSmooth` — pedal smoothness

---

### 4.5 Layout 4: Full Overview

**Purpose:** All metrics on one screen.

```
┌─────────────────────────┐
│ BALANCE                 │
│   52           48       │  33%
│   ████████│████████     │
├─────────────────────────┤
│ TE                  76% │
│   74    │    77         │  33%
│   L     │     R         │
├─────────────────────────┤
│ PS                  24% │
│   23    │    25         │  33%
│   L     │     R         │
└─────────────────────────┘
```

**Components:**
- `Balance` (compact)
- `TorqueEff` (compact)
- `PedalSmooth` (compact)

**Note:** Uses `compact` mode with reduced font sizes.

---

### 4.6 Layout 5: Compact Grid

**Purpose:** Maximum data in minimum space.

```
┌────────────┬────────────┐
│   TE L     │   TE R     │
│    74      │    77      │  33%
├────────────┼────────────┤
│   PS L     │   PS R     │
│    23      │    25      │  33%
├────────────┴────────────┤
│   52    │    48         │
│   ████████│████████     │  33%
└─────────────────────────┘
```

**Components:**
- `Compact` × 4 — TE and PS for each leg
- `Balance` (compact) — bottom, full width

---

### 4.7 Layout 6: Single Field

**Purpose:** Adding to other Karoo pages.

```
┌─────────────────────────┐
│ BALANCE                 │
│                         │
│                         │
│   52           48       │  100%
│   ████████│████████     │
│   L             R       │
│                         │
│                         │
└─────────────────────────┘
```

**Components:**
- `Balance` — full screen mode

---

### 4.8 Settings Screen

**Purpose:** Configure thresholds and notifications.

```
┌─────────────────────────┐
│ Settings                │
├─────────────────────────┤
│ THRESHOLDS              │
│ Balance alert      ±5%  │
│ TE optimal      70-80%  │
│ PS minimum        ≥20%  │
├─────────────────────────┤
│ ALERTS                  │
│ Vibration          [ON] │
│ Sound             [OFF] │
├─────────────────────────┤
│ kpedal 1.0              │
└─────────────────────────┘
```

**Settings:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| Balance alert | ±5% | Warning threshold |
| TE optimal | 70-80% | Optimal range |
| PS minimum | ≥20% | Minimum value |
| Vibration | ON | Vibration when out of zone |
| Sound | OFF | Audio alert |

---

### 4.9 Summary Screen

**Purpose:** Post-ride statistics.

```
┌─────────────────────────┐
│ Summary         1:42:35 │
├─────────────────────────┤
│ BALANCE                 │
│ 49 ━━━━━━━│━━━━━━━ 51   │
├─────────────────────────┤
│ TE              │ PS    │
│ 74    76        │ 22 24 │
│ L      R        │ L   R │
├─────────────────────────┤
│ TIME IN ZONE            │
│ ██████████░░░░          │
│ 72%    20%    8%        │
└─────────────────────────┘
```

**Data:**
- Ride duration
- Average L/R balance
- Average TE and PS for each leg
- Time in each zone (optimal/fair/low)

---

## 5. Status Logic

### 5.1 Balance

```javascript
const balanceStatus = (bal) => {
  const deviation = Math.abs(50 - bal);
  
  if (deviation <= 2) {
    return { color: '#4CAF50', status: 'optimal' };
  }
  if (deviation <= 5) {
    return { color: '#FFC107', status: 'attention' };
  }
  return { color: '#F44336', status: 'problem' };
};
```

| Deviation | Status | Color | Action |
|-----------|--------|-------|--------|
| ≤2% | Optimal | Green | Continue |
| ≤5% | Attention | Yellow | Monitor |
| >5% | Problem | Red | Correct |

### 5.2 Torque Effectiveness

```javascript
const teStatus = (te) => {
  if (te >= 70 && te <= 80) {
    return { color: '#4CAF50', status: 'optimal' };
  }
  if (te >= 65 && te <= 85) {
    return { color: '#FFC107', status: 'acceptable' };
  }
  return { color: '#F44336', status: 'needs_work' };
};
```

| Value | Status | Color | Comment |
|-------|--------|-------|---------|
| 70-80% | Optimal | Green | Ideal balance |
| 65-69% | Low | Yellow | Room for improvement |
| 81-85% | High | Yellow | Too much circular motion |
| <65% / >85% | Problem | Red | Needs attention |

### 5.3 Pedal Smoothness

```javascript
const psStatus = (ps) => {
  if (ps >= 20) {
    return { color: '#4CAF50', status: 'smooth' };
  }
  if (ps >= 15) {
    return { color: '#FFC107', status: 'fair' };
  }
  return { color: '#F44336', status: 'rough' };
};
```

| Value | Status | Color |
|-------|--------|-------|
| ≥20% | Smooth | Green |
| 15-19% | Fair | Yellow |
| <15% | Rough | Red |

### 5.4 Color Display Logic

**Rule:** Number displays white if within range. Colored if needs attention.

```javascript
// For Balance: color applies to the side with greater deviation
const leftColor = left > 52 ? status.color : '#FFFFFF';
const rightColor = right > 52 ? status.color : '#FFFFFF';

// For TE/PS: each leg is evaluated independently
const leftColor = teStatus(teL).color;
const rightColor = teStatus(teR).color;
```

---

## 6. Technical Requirements

### 6.1 Platform

| Parameter | Value |
|-----------|-------|
| Device | Hammerhead Karoo 3 |
| Display | 480×800px, 3.2", 292 PPI |
| SDK | karoo-ext (Kotlin) |
| Minimum Version | Karoo OS 1.524+ |

### 6.2 Data Sources

```kotlin
// ANT+ Cycling Dynamics
DataType.Type.TORQUE_EFFECTIVENESS_LEFT    // Left leg TE
DataType.Type.TORQUE_EFFECTIVENESS_RIGHT   // Right leg TE
DataType.Type.PEDAL_SMOOTHNESS_LEFT        // Left leg PS
DataType.Type.PEDAL_SMOOTHNESS_RIGHT       // Right leg PS

// ANT+ Power
DataType.Type.POWER_BALANCE                // L/R Balance
```

### 6.3 Compatible Pedals

| Manufacturer | Model | Balance | TE/PS |
|--------------|-------|---------|-------|
| Garmin | Rally (RS/RK) | ✓ | ✓ |
| Favero | Assioma DUO | ✓ | ✓ |
| Wahoo | POWRLINK Zero | ✓ | ✗ |
| SRM | X-Power | ✓ | ✓ |
| Rotor | 2INpower | ✓ | ✓ |

### 6.4 Data Update Rate

```
Update frequency: 1 Hz (every second)
Smoothing: 3-second rolling average
Display latency: <100ms
```

### 6.5 Data Field Sizes

Karoo supports different field sizes:

| Size | Proportion | Usage |
|------|------------|-------|
| 1×1 | Small square | Score, single number |
| 2×1 | Wide | Not used |
| 2×2 | Medium square | Balance, TE, PS |
| Full | Entire screen | Full Overview |

---

## 7. Project Structure

### 7.1 File Structure

```
kpedal/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/kpedal/
│   │   │   ├── KPedalExtension.kt      # Entry point
│   │   │   ├── KPedalDataField.kt      # Data field renderer
│   │   │   ├── KPedalSettings.kt       # Settings
│   │   │   ├── data/
│   │   │   │   ├── PedalingMetrics.kt  # Data models
│   │   │   │   └── StatusCalculator.kt # Status logic
│   │   │   ├── ui/
│   │   │   │   ├── BalanceView.kt      # Balance component
│   │   │   │   ├── TorqueEffView.kt    # TE component
│   │   │   │   ├── PedalSmoothView.kt  # PS component
│   │   │   │   ├── CompactView.kt      # Compact number
│   │   │   │   ├── StatusView.kt       # Status indicator
│   │   │   │   └── Colors.kt           # Color palette
│   │   │   └── layouts/
│   │   │       ├── QuickGlance.kt
│   │   │       ├── BalanceFocus.kt
│   │   │       ├── Efficiency.kt
│   │   │       ├── FullOverview.kt
│   │   │       ├── CompactGrid.kt
│   │   │       └── SingleField.kt
│   │   └── res/
│   │       ├── values/
│   │       │   ├── strings.xml
│   │       │   └── colors.xml
│   │       └── drawable/
│   └── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### 7.2 Core Classes

#### KPedalExtension.kt
```kotlin
class KPedalExtension : KarooExtension() {
    override fun onCreate() {
        registerDataFields()
        registerSettings()
    }
    
    private fun registerDataFields() {
        // Register 6 layouts
    }
}
```

#### PedalingMetrics.kt
```kotlin
data class PedalingMetrics(
    val balance: Float,           // 0-100 (right side %)
    val torqueEffLeft: Float,     // 0-100%
    val torqueEffRight: Float,    // 0-100%
    val pedalSmoothLeft: Float,   // 0-100%
    val pedalSmoothRight: Float,  // 0-100%
)
```

#### StatusCalculator.kt
```kotlin
object StatusCalculator {
    
    enum class Status { OPTIMAL, ATTENTION, PROBLEM }
    
    fun balanceStatus(balance: Float): Status {
        val deviation = abs(50f - balance)
        return when {
            deviation <= 2f -> Status.OPTIMAL
            deviation <= 5f -> Status.ATTENTION
            else -> Status.PROBLEM
        }
    }
    
    fun teStatus(te: Float): Status {
        return when {
            te in 70f..80f -> Status.OPTIMAL
            te in 65f..85f -> Status.ATTENTION
            else -> Status.PROBLEM
        }
    }
    
    fun psStatus(ps: Float): Status {
        return when {
            ps >= 20f -> Status.OPTIMAL
            ps >= 15f -> Status.ATTENTION
            else -> Status.PROBLEM
        }
    }
}
```

#### Colors.kt
```kotlin
object KPedalColors {
    val background = Color(0xFF000000)
    val text = Color(0xFFFFFFFF)
    val dim = Color(0xFF666666)
    val muted = Color(0xFF333333)
    
    val optimal = Color(0xFF4CAF50)
    val attention = Color(0xFFFFC107)
    val problem = Color(0xFFF44336)
    
    fun forStatus(status: Status): Color = when(status) {
        Status.OPTIMAL -> optimal
        Status.ATTENTION -> attention
        Status.PROBLEM -> problem
    }
}
```

### 7.3 Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.hammerhead:karoo-ext:1.0.0")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material3:material3:1.1.0")
}
```

---

## 8. UI Components

### 8.1 Balance Component

Primary component for displaying L/R balance.

```
┌─────────────────────────┐
│ BALANCE                 │  ← Header
│                         │
│   52           48       │  ← Large L/R numbers
│   ████████│████████     │  ← Visual bar
│   L             R       │  ← Labels
└─────────────────────────┘
```

**Number color logic:**
- If deviation ≤2% from 50 → both white
- If deviation >2% → dominant side colored

**Bar color logic:**
- Side with higher % is colored with status color
- Other side — gray (#666)
- Center line — white

### 8.2 TorqueEff / PedalSmooth Components

Identical structure for both metrics.

```
┌─────────────────────────┐
│ TE                  76% │  ← Header + average
│                         │
│   74    │    77         │  ← Numbers L | R
│   L     │     R         │  ← Labels
└─────────────────────────┘
```

**Features:**
- Average value in header (color by status)
- Vertical divider between L and R
- Each number evaluated independently

### 8.3 Compact Component

Minimal component for grid layouts.

```
┌─────────────┐
│   TE L      │  ← Label
│    74       │  ← Number (colored)
└─────────────┘
```

### 8.4 StatusIndicator Component

Overall status of all metrics.

```
┌─────────────────────────┐
│                         │
│           ✓             │  ← Symbol ✓ or !
│       ALL GOOD          │  ← Status text
│                         │
└─────────────────────────┘
```

**Logic:**
- All metrics OK → green ✓, "ALL GOOD"
- 1 issue → yellow !, "1 ISSUE"
- 2+ issues → red !, "X ISSUES"

---

## 9. User Interaction

### 9.1 Navigation

```
Karoo Home
    └── Data Fields
            └── kpedal
                    ├── Quick Glance
                    ├── Balance Focus
                    ├── Efficiency
                    ├── Full Overview
                    ├── Compact Grid
                    └── Single Field
    └── Apps
            └── kpedal
                    ├── Settings
                    └── Summary (post-ride)
```

### 9.2 Adding a Field to Screen

1. Open profile editor
2. Select data page
3. Tap on empty cell
4. Select "kpedal" from list
5. Choose desired layout

### 9.3 Configuring Thresholds

1. Open Apps → kpedal → Settings
2. Modify thresholds as needed
3. Settings save automatically

### 9.4 Viewing Summary

1. Complete ride
2. Open Apps → kpedal → Summary
3. Review statistics

---

## 10. Alerts & Notifications

### 10.1 Alert Types

| Alert | Condition | Action |
|-------|-----------|--------|
| Balance | Deviation > threshold | Vibration |
| TE Low | TE < 65% | Vibration |
| TE High | TE > 85% | Vibration |
| PS Low | PS < 15% | Vibration |

### 10.2 Alert Settings

- **Vibration:** ON/OFF
- **Sound:** ON/OFF
- **Cooldown:** 30 seconds between repeat alerts

### 10.3 Implementation

```kotlin
class AlertManager {
    private var lastAlertTime = 0L
    private val cooldown = 30_000L // 30 sec
    
    fun checkAndAlert(metrics: PedalingMetrics) {
        if (System.currentTimeMillis() - lastAlertTime < cooldown) return
        
        val issues = mutableListOf<String>()
        
        if (balanceStatus(metrics.balance) == Status.PROBLEM) {
            issues.add("Balance")
        }
        // ... check other metrics
        
        if (issues.isNotEmpty()) {
            triggerAlert(issues)
            lastAlertTime = System.currentTimeMillis()
        }
    }
}
```

---

## 11. Data Storage

### 11.1 Settings

Stored in SharedPreferences:

```kotlin
object Preferences {
    const val KEY_BALANCE_THRESHOLD = "balance_threshold"
    const val KEY_TE_MIN = "te_min"
    const val KEY_TE_MAX = "te_max"
    const val KEY_PS_MIN = "ps_min"
    const val KEY_VIBRATION = "vibration_enabled"
    const val KEY_SOUND = "sound_enabled"
}
```

### 11.2 Ride Data

Recorded to FIT file via Karoo API:

```kotlin
// Karoo automatically records all data
// kpedal only displays, does not record
```

### 11.3 Summary Data

Calculated from recorded ride:

```kotlin
data class RideSummary(
    val duration: Duration,
    val avgBalance: Pair<Float, Float>,
    val avgTE: Pair<Float, Float>,
    val avgPS: Pair<Float, Float>,
    val timeInZone: Map<Status, Duration>,
)
```

---

## Appendices

### A. Glossary

| Term | Description |
|------|-------------|
| **Balance** | Power distribution between legs (L/R) |
| **TE** | Torque Effectiveness — torque efficiency percentage |
| **PS** | Pedal Smoothness — pedaling smoothness percentage |
| **Data Field** | Data display field in Karoo interface |
| **Cycling Dynamics** | Extended pedaling metrics (ANT+ profile) |
| **karoo-ext** | SDK for developing Karoo extensions |

### B. References

- [Karoo SDK Documentation](https://developer.hammerhead.io/)
- [ANT+ Cycling Power Profile](https://www.thisisant.com/developer/)
- [Wattbike Pedal Effectiveness Research](https://wattbike.com/)
- [TrainingPeaks: Understanding Cycling Dynamics](https://trainingpeaks.com/)

### C. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Dec 2024 | Initial release |

---

## Contact

**Project:** kpedal  
**Platform:** Karoo 3  
**Status:** In Development
