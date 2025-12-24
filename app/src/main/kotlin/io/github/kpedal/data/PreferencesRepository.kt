package io.github.kpedal.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kpedal_settings")

/**
 * Repository for kpedal settings using DataStore.
 */
class PreferencesRepository(private val context: Context) {

    /**
     * Settings data class with defaults.
     */
    @Immutable
    data class Settings(
        val balanceThreshold: Int = 5,      // ±5% default
        val teOptimalMin: Int = 70,         // TE optimal range 70-80%
        val teOptimalMax: Int = 80,
        val psMinimum: Int = 20,            // PS minimum 20%
        val vibrationEnabled: Boolean = true,
        val soundEnabled: Boolean = false,
        val alertCooldown: Int = 30         // 30 seconds between alerts
    )

    companion object {
        // Preference keys - Thresholds
        private val KEY_BALANCE_THRESHOLD = intPreferencesKey("balance_threshold")
        private val KEY_TE_OPTIMAL_MIN = intPreferencesKey("te_optimal_min")
        private val KEY_TE_OPTIMAL_MAX = intPreferencesKey("te_optimal_max")
        private val KEY_PS_MINIMUM = intPreferencesKey("ps_minimum")
        private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_ALERT_COOLDOWN = intPreferencesKey("alert_cooldown")

        // Alert keys - Global
        private val KEY_ALERTS_GLOBAL_ENABLED = booleanPreferencesKey("alerts_global_enabled")
        private val KEY_SCREEN_WAKE_ON_ALERT = booleanPreferencesKey("screen_wake_on_alert")

        // Alert keys - Balance
        private val KEY_BALANCE_ALERT_ENABLED = booleanPreferencesKey("balance_alert_enabled")
        private val KEY_BALANCE_ALERT_TRIGGER = stringPreferencesKey("balance_alert_trigger")
        private val KEY_BALANCE_ALERT_VISUAL = booleanPreferencesKey("balance_alert_visual")
        private val KEY_BALANCE_ALERT_SOUND = booleanPreferencesKey("balance_alert_sound")
        private val KEY_BALANCE_ALERT_VIBRATION = booleanPreferencesKey("balance_alert_vibration")
        private val KEY_BALANCE_ALERT_COOLDOWN = intPreferencesKey("balance_alert_cooldown")

        // Alert keys - TE
        private val KEY_TE_ALERT_ENABLED = booleanPreferencesKey("te_alert_enabled")
        private val KEY_TE_ALERT_TRIGGER = stringPreferencesKey("te_alert_trigger")
        private val KEY_TE_ALERT_VISUAL = booleanPreferencesKey("te_alert_visual")
        private val KEY_TE_ALERT_SOUND = booleanPreferencesKey("te_alert_sound")
        private val KEY_TE_ALERT_VIBRATION = booleanPreferencesKey("te_alert_vibration")
        private val KEY_TE_ALERT_COOLDOWN = intPreferencesKey("te_alert_cooldown")

        // Alert keys - PS
        private val KEY_PS_ALERT_ENABLED = booleanPreferencesKey("ps_alert_enabled")
        private val KEY_PS_ALERT_TRIGGER = stringPreferencesKey("ps_alert_trigger")
        private val KEY_PS_ALERT_VISUAL = booleanPreferencesKey("ps_alert_visual")
        private val KEY_PS_ALERT_SOUND = booleanPreferencesKey("ps_alert_sound")
        private val KEY_PS_ALERT_VIBRATION = booleanPreferencesKey("ps_alert_vibration")
        private val KEY_PS_ALERT_COOLDOWN = intPreferencesKey("ps_alert_cooldown")

        // Onboarding
        private val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")

        // Background Mode & Sync
        private val KEY_BACKGROUND_MODE_ENABLED = booleanPreferencesKey("background_mode_enabled")
        private val KEY_AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        private val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    /**
     * Flow of current settings.
     */
    val settingsFlow: Flow<Settings> = context.dataStore.data
        .map { prefs ->
            Settings(
                balanceThreshold = prefs[KEY_BALANCE_THRESHOLD] ?: 5,
                teOptimalMin = prefs[KEY_TE_OPTIMAL_MIN] ?: 70,
                teOptimalMax = prefs[KEY_TE_OPTIMAL_MAX] ?: 80,
                psMinimum = prefs[KEY_PS_MINIMUM] ?: 20,
                vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
                soundEnabled = prefs[KEY_SOUND_ENABLED] ?: false,
                alertCooldown = prefs[KEY_ALERT_COOLDOWN] ?: 30
            )
        }
        .distinctUntilChanged()

    /**
     * Update balance threshold.
     */
    suspend fun updateBalanceThreshold(threshold: Int) {
        val validThreshold = threshold.coerceIn(1, 10)
        context.dataStore.edit { prefs ->
            prefs[KEY_BALANCE_THRESHOLD] = validThreshold
        }
    }

    /**
     * Update TE optimal range.
     */
    suspend fun updateTeOptimalRange(min: Int, max: Int) {
        val validMin = min.coerceIn(50, 90)
        val validMax = max.coerceIn(validMin + 5, 100)
        context.dataStore.edit { prefs ->
            prefs[KEY_TE_OPTIMAL_MIN] = validMin
            prefs[KEY_TE_OPTIMAL_MAX] = validMax
        }
    }

    /**
     * Update PS minimum.
     */
    suspend fun updatePsMinimum(minimum: Int) {
        val validMinimum = minimum.coerceIn(10, 30)
        context.dataStore.edit { prefs ->
            prefs[KEY_PS_MINIMUM] = validMinimum
        }
    }

    /**
     * Update vibration enabled.
     */
    suspend fun updateVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIBRATION_ENABLED] = enabled
        }
    }

    /**
     * Update sound enabled.
     */
    suspend fun updateSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUND_ENABLED] = enabled
        }
    }

    /**
     * Update alert cooldown.
     */
    suspend fun updateAlertCooldown(seconds: Int) {
        val validCooldown = seconds.coerceIn(10, 120)
        context.dataStore.edit { prefs ->
            prefs[KEY_ALERT_COOLDOWN] = validCooldown
        }
    }

    // ========== Alert Settings ==========

    /**
     * Flow of alert settings.
     */
    val alertSettingsFlow: Flow<AlertSettings> = context.dataStore.data
        .map { prefs ->
            AlertSettings(
                globalEnabled = prefs[KEY_ALERTS_GLOBAL_ENABLED] ?: true,
                screenWakeOnAlert = prefs[KEY_SCREEN_WAKE_ON_ALERT] ?: true,
                balanceConfig = MetricAlertConfig(
                    enabled = prefs[KEY_BALANCE_ALERT_ENABLED] ?: true,
                    triggerLevel = try {
                        AlertTriggerLevel.valueOf(prefs[KEY_BALANCE_ALERT_TRIGGER] ?: "PROBLEM_ONLY")
                    } catch (e: Exception) {
                        AlertTriggerLevel.PROBLEM_ONLY
                    },
                    visualAlert = prefs[KEY_BALANCE_ALERT_VISUAL] ?: true,
                    soundAlert = prefs[KEY_BALANCE_ALERT_SOUND] ?: false,
                    vibrationAlert = prefs[KEY_BALANCE_ALERT_VIBRATION] ?: true,
                    cooldownSeconds = prefs[KEY_BALANCE_ALERT_COOLDOWN] ?: 30
                ),
                teConfig = MetricAlertConfig(
                    enabled = prefs[KEY_TE_ALERT_ENABLED] ?: true,
                    triggerLevel = try {
                        AlertTriggerLevel.valueOf(prefs[KEY_TE_ALERT_TRIGGER] ?: "PROBLEM_ONLY")
                    } catch (e: Exception) {
                        AlertTriggerLevel.PROBLEM_ONLY
                    },
                    visualAlert = prefs[KEY_TE_ALERT_VISUAL] ?: true,
                    soundAlert = prefs[KEY_TE_ALERT_SOUND] ?: false,
                    vibrationAlert = prefs[KEY_TE_ALERT_VIBRATION] ?: true,
                    cooldownSeconds = prefs[KEY_TE_ALERT_COOLDOWN] ?: 30
                ),
                psConfig = MetricAlertConfig(
                    enabled = prefs[KEY_PS_ALERT_ENABLED] ?: true,
                    triggerLevel = try {
                        AlertTriggerLevel.valueOf(prefs[KEY_PS_ALERT_TRIGGER] ?: "PROBLEM_ONLY")
                    } catch (e: Exception) {
                        AlertTriggerLevel.PROBLEM_ONLY
                    },
                    visualAlert = prefs[KEY_PS_ALERT_VISUAL] ?: true,
                    soundAlert = prefs[KEY_PS_ALERT_SOUND] ?: false,
                    vibrationAlert = prefs[KEY_PS_ALERT_VIBRATION] ?: true,
                    cooldownSeconds = prefs[KEY_PS_ALERT_COOLDOWN] ?: 30
                )
            )
        }
        .distinctUntilChanged()

    suspend fun updateGlobalAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALERTS_GLOBAL_ENABLED] = enabled
        }
    }

    suspend fun updateScreenWakeOnAlert(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCREEN_WAKE_ON_ALERT] = enabled
        }
    }

    suspend fun updateBalanceAlertConfig(config: MetricAlertConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BALANCE_ALERT_ENABLED] = config.enabled
            prefs[KEY_BALANCE_ALERT_TRIGGER] = config.triggerLevel.name
            prefs[KEY_BALANCE_ALERT_VISUAL] = config.visualAlert
            prefs[KEY_BALANCE_ALERT_SOUND] = config.soundAlert
            prefs[KEY_BALANCE_ALERT_VIBRATION] = config.vibrationAlert
            prefs[KEY_BALANCE_ALERT_COOLDOWN] = config.cooldownSeconds.coerceIn(10, 120)
        }
    }

    suspend fun updateTeAlertConfig(config: MetricAlertConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TE_ALERT_ENABLED] = config.enabled
            prefs[KEY_TE_ALERT_TRIGGER] = config.triggerLevel.name
            prefs[KEY_TE_ALERT_VISUAL] = config.visualAlert
            prefs[KEY_TE_ALERT_SOUND] = config.soundAlert
            prefs[KEY_TE_ALERT_VIBRATION] = config.vibrationAlert
            prefs[KEY_TE_ALERT_COOLDOWN] = config.cooldownSeconds.coerceIn(10, 120)
        }
    }

    suspend fun updatePsAlertConfig(config: MetricAlertConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PS_ALERT_ENABLED] = config.enabled
            prefs[KEY_PS_ALERT_TRIGGER] = config.triggerLevel.name
            prefs[KEY_PS_ALERT_VISUAL] = config.visualAlert
            prefs[KEY_PS_ALERT_SOUND] = config.soundAlert
            prefs[KEY_PS_ALERT_VIBRATION] = config.vibrationAlert
            prefs[KEY_PS_ALERT_COOLDOWN] = config.cooldownSeconds.coerceIn(10, 120)
        }
    }

    // ========== Onboarding ==========

    /**
     * Flow indicating whether user has seen onboarding.
     */
    val hasSeenOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_ONBOARDING] ?: false
    }

    /**
     * Mark onboarding as seen.
     */
    suspend fun markOnboardingSeen() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_SEEN_ONBOARDING] = true
        }
    }

    // ========== Background Mode & Sync ==========

    /**
     * Flow indicating whether background mode is enabled.
     * When enabled, KPedal collects data for all rides even when DataTypes are not visible.
     * Default: true
     */
    val backgroundModeEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_BACKGROUND_MODE_ENABLED] ?: true }
        .distinctUntilChanged()

    /**
     * Flow indicating whether auto-sync is enabled.
     * When enabled, rides are automatically synced to cloud after completion.
     * Default: true
     */
    val autoSyncEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_AUTO_SYNC_ENABLED] ?: true }
        .distinctUntilChanged()

    /**
     * Flow of last successful sync timestamp.
     */
    val lastSyncTimestampFlow: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[KEY_LAST_SYNC_TIMESTAMP] ?: 0L }
        .distinctUntilChanged()

    /**
     * Update background mode enabled state.
     */
    suspend fun updateBackgroundModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKGROUND_MODE_ENABLED] = enabled
        }
    }

    /**
     * Update auto-sync enabled state.
     */
    suspend fun updateAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SYNC_ENABLED] = enabled
        }
    }

    /**
     * Update last sync timestamp.
     */
    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }
}
