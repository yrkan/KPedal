package io.github.kpedal.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.kpedal.KPedalExtension
import io.github.kpedal.R
import io.github.kpedal.data.AlertSettings
import io.github.kpedal.data.AlertTriggerLevel
import io.github.kpedal.data.MetricAlertConfig
import io.github.kpedal.data.MetricType
import io.github.kpedal.data.PreferencesRepository
import io.github.kpedal.data.SensorDisconnectAction
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.TurnScreenOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages real-time alerts for pedaling metrics.
 * Thread-safe implementation using atomic types.
 */
class AlertManager(
    private val extension: KPedalExtension,
    private val pedalingEngine: PedalingEngine,
    private val preferencesRepository: PreferencesRepository
) {

    companion object {
        private const val TAG = "AlertManager"

        // Alert IDs for InRideAlert
        private const val ALERT_BALANCE = "kpedal_balance"
        private const val ALERT_TE = "kpedal_te"
        private const val ALERT_PS = "kpedal_ps"
        private const val ALERT_SENSOR_DISCONNECT = "kpedal_sensor_disconnect"

        // Disconnect alert cooldown (don't spam if sensor keeps reconnecting/disconnecting)
        private const val DISCONNECT_ALERT_COOLDOWN_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Per-metric cooldown tracking (timestamp of last alert)
    private val balanceLastAlert = AtomicLong(0)
    private val teLastAlert = AtomicLong(0)
    private val psLastAlert = AtomicLong(0)
    private val disconnectLastAlert = AtomicLong(0)

    // Previous status tracking for transition detection
    private val balancePrevStatus = AtomicReference(StatusCalculator.Status.OPTIMAL)
    private val tePrevStatus = AtomicReference(StatusCalculator.Status.OPTIMAL)
    private val psPrevStatus = AtomicReference(StatusCalculator.Status.OPTIMAL)

    // Current alert settings (cached)
    private val currentSettings = AtomicReference(AlertSettings())
    private val currentThresholds = AtomicReference(PreferencesRepository.Settings())

    // Vibrator for haptic feedback
    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = extension.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                extension.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to get vibrator: ${e.message}")
            null
        }
    }

    /**
     * Start monitoring metrics and dispatching alerts.
     * Uses combine() for efficient single-collector approach.
     */
    fun startMonitoring() {
        android.util.Log.i(TAG, "Starting alert monitoring")

        // Single combined collector for all inputs
        scope.launch {
            combine(
                pedalingEngine.metrics,
                preferencesRepository.alertSettingsFlow,
                preferencesRepository.settingsFlow
            ) { metrics, alertSettings, thresholds ->
                Triple(metrics, alertSettings, thresholds)
            }.collect { (metrics, alertSettings, thresholds) ->
                if (!metrics.hasData) return@collect
                if (!alertSettings.globalEnabled) return@collect

                checkAlerts(metrics, alertSettings, thresholds)
            }
        }

        // Monitor sensor state for disconnect alerts (only if SHOW_ALERT is enabled)
        scope.launch {
            combine(
                pedalingEngine.sensorState,
                preferencesRepository.alertSettingsFlow
            ) { state, settings ->
                state to settings.sensorDisconnectAction
            }.collect { (state, action) ->
                if (state is SensorStreamState.Disconnected && action == SensorDisconnectAction.SHOW_ALERT) {
                    handleSensorDisconnect()
                }
            }
        }
    }

    /**
     * Handle sensor disconnect by alerting the user.
     */
    private fun handleSensorDisconnect() {
        val now = System.currentTimeMillis()
        val lastAlert = disconnectLastAlert.get()

        // Check cooldown
        if (now - lastAlert < DISCONNECT_ALERT_COOLDOWN_MS) {
            android.util.Log.d(TAG, "Sensor disconnect alert skipped (cooldown)")
            return
        }

        // Update last alert time with CAS
        if (!disconnectLastAlert.compareAndSet(lastAlert, now)) return

        android.util.Log.w(TAG, "Dispatching sensor disconnect alert")

        try {
            // Wake screen
            extension.karooSystem.dispatch(TurnScreenOn)

            // Visual alert
            extension.karooSystem.dispatch(
                InRideAlert(
                    id = ALERT_SENSOR_DISCONNECT,
                    icon = R.drawable.ic_kpedal,
                    title = extension.getString(R.string.alert_sensor_lost_title),
                    detail = extension.getString(R.string.alert_sensor_lost_detail),
                    autoDismissMs = 8000L,
                    backgroundColor = R.color.alert_bg,
                    textColor = R.color.alert_text
                )
            )

            // Sound alert (urgent pattern)
            extension.karooSystem.dispatch(
                PlayBeepPattern(
                    listOf(
                        PlayBeepPattern.Tone(frequency = 1000, durationMs = 200),
                        PlayBeepPattern.Tone(frequency = null, durationMs = 100),
                        PlayBeepPattern.Tone(frequency = 800, durationMs = 200),
                        PlayBeepPattern.Tone(frequency = null, durationMs = 100),
                        PlayBeepPattern.Tone(frequency = 600, durationMs = 200)
                    )
                )
            )

            // Vibration
            playVibration(StatusCalculator.Status.PROBLEM)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to dispatch disconnect alert: ${e.message}")
        }
    }

    /**
     * Stop monitoring and cleanup.
     */
    fun stopMonitoring() {
        android.util.Log.i(TAG, "Stopping alert monitoring")
        scope.cancel()
    }

    /**
     * Check all metrics and trigger alerts if needed.
     */
    private fun checkAlerts(
        metrics: PedalingMetrics,
        settings: AlertSettings,
        thresholds: PreferencesRepository.Settings
    ) {
        val now = System.currentTimeMillis()

        // Balance alert
        if (settings.balanceConfig.enabled && settings.balanceConfig.triggerLevel != AlertTriggerLevel.DISABLED) {
            val status = StatusCalculator.balanceStatus(metrics.balance, thresholds.balanceThreshold)
            checkMetricAlert(
                metricType = MetricType.BALANCE,
                currentStatus = status,
                previousStatus = balancePrevStatus,
                lastAlertTime = balanceLastAlert,
                config = settings.balanceConfig,
                now = now,
                screenWake = settings.screenWakeOnAlert,
                alertId = ALERT_BALANCE,
                alertTitle = "BALANCE",
                alertDetail = formatBalanceDetail(metrics)
            )
        }

        // TE alert
        if (settings.teConfig.enabled && settings.teConfig.triggerLevel != AlertTriggerLevel.DISABLED) {
            val status = StatusCalculator.teStatus(
                metrics.torqueEffAvg,
                thresholds.teOptimalMin,
                thresholds.teOptimalMax
            )
            checkMetricAlert(
                metricType = MetricType.TORQUE_EFFECTIVENESS,
                currentStatus = status,
                previousStatus = tePrevStatus,
                lastAlertTime = teLastAlert,
                config = settings.teConfig,
                now = now,
                screenWake = settings.screenWakeOnAlert,
                alertId = ALERT_TE,
                alertTitle = "TORQUE EFF.",
                alertDetail = formatTeDetail(metrics, thresholds)
            )
        }

        // PS alert
        if (settings.psConfig.enabled && settings.psConfig.triggerLevel != AlertTriggerLevel.DISABLED) {
            val status = StatusCalculator.psStatus(metrics.pedalSmoothAvg, thresholds.psMinimum)
            checkMetricAlert(
                metricType = MetricType.PEDAL_SMOOTHNESS,
                currentStatus = status,
                previousStatus = psPrevStatus,
                lastAlertTime = psLastAlert,
                config = settings.psConfig,
                now = now,
                screenWake = settings.screenWakeOnAlert,
                alertId = ALERT_PS,
                alertTitle = "SMOOTHNESS",
                alertDetail = formatPsDetail(metrics, thresholds)
            )
        }
    }

    /**
     * Check a single metric and trigger alert if conditions are met.
     */
    private fun checkMetricAlert(
        @Suppress("UNUSED_PARAMETER") metricType: MetricType,
        currentStatus: StatusCalculator.Status,
        previousStatus: AtomicReference<StatusCalculator.Status>,
        lastAlertTime: AtomicLong,
        config: MetricAlertConfig,
        now: Long,
        screenWake: Boolean,
        alertId: String,
        alertTitle: String,
        alertDetail: String
    ) {
        // Retry loop for atomic status update
        var prevStatus: StatusCalculator.Status
        do {
            prevStatus = previousStatus.get()
            if (prevStatus == currentStatus) {
                // No status change, nothing to do
                return
            }
        } while (!previousStatus.compareAndSet(prevStatus, currentStatus))

        // Check if status transition should trigger alert
        val shouldAlert = when (config.triggerLevel) {
            AlertTriggerLevel.PROBLEM_ONLY -> {
                // Only alert when transitioning TO PROBLEM status
                currentStatus == StatusCalculator.Status.PROBLEM &&
                    prevStatus != StatusCalculator.Status.PROBLEM
            }
            AlertTriggerLevel.ATTENTION_AND_UP -> {
                // Alert on ATTENTION or PROBLEM, but only on worsening transitions
                (currentStatus == StatusCalculator.Status.PROBLEM ||
                    currentStatus == StatusCalculator.Status.ATTENTION) &&
                    currentStatus != prevStatus &&
                    isWorsening(prevStatus, currentStatus)
            }
            AlertTriggerLevel.DISABLED -> false
        }

        if (!shouldAlert) return

        // Check cooldown
        val lastAlert = lastAlertTime.get()
        val cooldownMs = config.cooldownSeconds * 1000L
        if (now - lastAlert < cooldownMs) return

        // Update last alert time with CAS
        if (!lastAlertTime.compareAndSet(lastAlert, now)) return

        // Dispatch alert
        dispatchAlert(
            alertId = alertId,
            title = alertTitle,
            detail = alertDetail,
            visualAlert = config.visualAlert,
            soundAlert = config.soundAlert,
            vibrationAlert = config.vibrationAlert,
            screenWake = screenWake,
            status = currentStatus
        )
    }

    /**
     * Check if status is worsening (OPTIMAL -> ATTENTION -> PROBLEM).
     */
    private fun isWorsening(
        previous: StatusCalculator.Status,
        current: StatusCalculator.Status
    ): Boolean {
        val prevLevel = when (previous) {
            StatusCalculator.Status.OPTIMAL -> 0
            StatusCalculator.Status.ATTENTION -> 1
            StatusCalculator.Status.PROBLEM -> 2
        }
        val currLevel = when (current) {
            StatusCalculator.Status.OPTIMAL -> 0
            StatusCalculator.Status.ATTENTION -> 1
            StatusCalculator.Status.PROBLEM -> 2
        }
        return currLevel > prevLevel
    }

    /**
     * Dispatch alert through Karoo SDK and Android vibrator.
     */
    private fun dispatchAlert(
        alertId: String,
        title: String,
        detail: String,
        visualAlert: Boolean,
        soundAlert: Boolean,
        vibrationAlert: Boolean,
        screenWake: Boolean,
        status: StatusCalculator.Status
    ) {
        try {
            android.util.Log.i(TAG, "Dispatching alert: $alertId - $title ($status)")

            // Wake screen if enabled
            if (screenWake) {
                val screenResult = extension.karooSystem.dispatch(TurnScreenOn)
                if (!screenResult) {
                    android.util.Log.w(TAG, "Failed to turn screen on")
                }
            }

            // Visual alert (InRideAlert)
            if (visualAlert) {
                val alertResult = extension.karooSystem.dispatch(
                    InRideAlert(
                        id = alertId,
                        icon = R.drawable.ic_kpedal,
                        title = title,
                        detail = detail,
                        autoDismissMs = 5000L,
                        backgroundColor = R.color.alert_bg,
                        textColor = R.color.alert_text
                    )
                )
                if (!alertResult) {
                    android.util.Log.w(TAG, "Failed to dispatch visual alert: $alertId")
                }
            }

            // Sound alert (PlayBeepPattern)
            if (soundAlert) {
                playAlertSound(status)
            }

            // Vibration alert (Android Vibrator)
            if (vibrationAlert) {
                playVibration(status)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to dispatch alert: ${e.message}")
        }
    }

    /**
     * Play alert sound using Karoo's beeper.
     */
    private fun playAlertSound(status: StatusCalculator.Status) {
        try {
            val tones = when (status) {
                StatusCalculator.Status.PROBLEM -> listOf(
                    PlayBeepPattern.Tone(frequency = 800, durationMs = 150),
                    PlayBeepPattern.Tone(frequency = null, durationMs = 50),
                    PlayBeepPattern.Tone(frequency = 800, durationMs = 150),
                    PlayBeepPattern.Tone(frequency = null, durationMs = 50),
                    PlayBeepPattern.Tone(frequency = 800, durationMs = 150)
                )
                StatusCalculator.Status.ATTENTION -> listOf(
                    PlayBeepPattern.Tone(frequency = 600, durationMs = 150),
                    PlayBeepPattern.Tone(frequency = null, durationMs = 50),
                    PlayBeepPattern.Tone(frequency = 600, durationMs = 150)
                )
                else -> listOf(
                    PlayBeepPattern.Tone(frequency = 500, durationMs = 100)
                )
            }
            val beepResult = extension.karooSystem.dispatch(PlayBeepPattern(tones))
            if (!beepResult) {
                android.util.Log.w(TAG, "Failed to play beep sound")
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to play alert sound: ${e.message}")
        }
    }

    /**
     * Play vibration feedback.
     */
    private fun playVibration(status: StatusCalculator.Status) {
        try {
            val v = vibrator ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (status) {
                    StatusCalculator.Status.PROBLEM -> {
                        // Triple vibration for problem
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 100, 50, 100, 50, 100),
                            -1
                        )
                    }
                    StatusCalculator.Status.ATTENTION -> {
                        // Double vibration for attention
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 80, 50, 80),
                            -1
                        )
                    }
                    else -> {
                        // Single short vibration
                        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                }
                v.vibrate(effect)
            } else {
                // Legacy vibration
                @Suppress("DEPRECATION")
                when (status) {
                    StatusCalculator.Status.PROBLEM -> v.vibrate(longArrayOf(0, 100, 50, 100, 50, 100), -1)
                    StatusCalculator.Status.ATTENTION -> v.vibrate(longArrayOf(0, 80, 50, 80), -1)
                    else -> v.vibrate(50)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to vibrate: ${e.message}")
        }
    }

    // Format helpers

    private fun formatBalanceDetail(metrics: PedalingMetrics): String {
        val left = (100f - metrics.balance).toInt()
        val right = metrics.balance.toInt()
        val deviation = kotlin.math.abs(50 - right)
        return "L$left / R$right (${deviation}% off)"
    }

    private fun formatTeDetail(metrics: PedalingMetrics, thresholds: PreferencesRepository.Settings): String {
        val avg = metrics.torqueEffAvg.toInt()
        return "TE ${avg}% (target ${thresholds.teOptimalMin}-${thresholds.teOptimalMax}%)"
    }

    private fun formatPsDetail(metrics: PedalingMetrics, thresholds: PreferencesRepository.Settings): String {
        val avg = metrics.pedalSmoothAvg.toInt()
        return "PS ${avg}% (min ${thresholds.psMinimum}%)"
    }

    /**
     * Reset all alert state.
     */
    fun reset() {
        balanceLastAlert.set(0)
        teLastAlert.set(0)
        psLastAlert.set(0)
        disconnectLastAlert.set(0)
        balancePrevStatus.set(StatusCalculator.Status.OPTIMAL)
        tePrevStatus.set(StatusCalculator.Status.OPTIMAL)
        psPrevStatus.set(StatusCalculator.Status.OPTIMAL)
    }

    /**
     * Cleanup resources.
     */
    fun destroy() {
        stopMonitoring()
    }
}
