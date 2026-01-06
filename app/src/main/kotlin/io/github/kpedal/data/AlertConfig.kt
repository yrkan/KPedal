package io.github.kpedal.data

import androidx.compose.runtime.Immutable

/**
 * Enum for the three metric types that can trigger alerts.
 */
enum class MetricType {
    BALANCE,
    TORQUE_EFFECTIVENESS,
    PEDAL_SMOOTHNESS
}

/**
 * Alert trigger levels - what status triggers an alert.
 */
enum class AlertTriggerLevel {
    PROBLEM_ONLY,      // Only alert on PROBLEM status
    ATTENTION_AND_UP,  // Alert on ATTENTION or PROBLEM
    DISABLED           // No alerts for this metric
}

/**
 * How to notify user when sensor disconnects during ride.
 */
enum class SensorDisconnectAction {
    SHOW_DASHES,  // Show "---" in data fields (less intrusive)
    SHOW_ALERT,   // Show InRideAlert + vibration (more noticeable)
    DISABLED      // No notification
}

/**
 * Configuration for a single metric's alerts.
 */
@Immutable
data class MetricAlertConfig(
    val enabled: Boolean = true,
    val triggerLevel: AlertTriggerLevel = AlertTriggerLevel.PROBLEM_ONLY,
    val visualAlert: Boolean = true,     // InRideAlert overlay
    val soundAlert: Boolean = false,     // PlayBeepPattern
    val vibrationAlert: Boolean = true,  // Android Vibrator
    val cooldownSeconds: Int = 30        // Per-metric cooldown (10-120)
)

/**
 * Complete alert settings for all metrics.
 */
@Immutable
data class AlertSettings(
    val globalEnabled: Boolean = true,
    val balanceConfig: MetricAlertConfig = MetricAlertConfig(),
    val teConfig: MetricAlertConfig = MetricAlertConfig(),
    val psConfig: MetricAlertConfig = MetricAlertConfig(),
    val screenWakeOnAlert: Boolean = true,
    val sensorDisconnectAction: SensorDisconnectAction = SensorDisconnectAction.SHOW_DASHES
)
