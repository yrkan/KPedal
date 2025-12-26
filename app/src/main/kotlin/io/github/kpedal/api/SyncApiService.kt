package io.github.kpedal.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Sync API service for uploading ride data to cloud.
 */
interface SyncApiService {

    /**
     * Sync a single ride to cloud.
     */
    @POST("sync/ride")
    suspend fun syncRide(
        @Header("Authorization") token: String,
        @Header("X-Device-ID") deviceId: String,
        @Body ride: SyncRideRequest
    ): Response<SyncResponse>

    /**
     * Sync a ride with per-minute snapshots to cloud.
     */
    @POST("sync/ride-full")
    suspend fun syncRideWithSnapshots(
        @Header("Authorization") token: String,
        @Header("X-Device-ID") deviceId: String,
        @Body request: SyncRideWithSnapshotsRequest
    ): Response<SyncResponse>

    /**
     * Sync a single drill result to cloud.
     */
    @POST("sync/drill")
    suspend fun syncDrill(
        @Header("Authorization") token: String,
        @Header("X-Device-ID") deviceId: String,
        @Body drill: SyncDrillRequest
    ): Response<SyncResponse>

    /**
     * Sync multiple drill results to cloud.
     */
    @POST("sync/drills")
    suspend fun syncDrills(
        @Header("Authorization") token: String,
        @Header("X-Device-ID") deviceId: String,
        @Body request: SyncDrillsRequest
    ): Response<SyncResponse>

    /**
     * Sync achievements to cloud.
     */
    @POST("sync/achievements")
    suspend fun syncAchievements(
        @Header("Authorization") token: String,
        @Header("X-Device-ID") deviceId: String,
        @Body request: SyncAchievementsRequest
    ): Response<SyncResponse>

    /**
     * Get sync status for this device.
     */
    @GET("sync/status")
    suspend fun getSyncStatus(
        @Header("Authorization") token: String,
        @Header("X-Device-ID") deviceId: String
    ): Response<SyncStatusResponse>

    /**
     * Check if a sync was requested from the web.
     */
    @GET("sync/check-request")
    suspend fun checkSyncRequest(
        @Header("Authorization") token: String,
        @Header("X-Device-ID") deviceId: String
    ): Response<CheckSyncRequestResponse>

    /**
     * Get user settings from cloud.
     */
    @GET("settings")
    suspend fun getSettings(
        @Header("Authorization") token: String
    ): Response<SettingsResponse>

    /**
     * Update user settings on cloud.
     */
    @PUT("settings")
    suspend fun updateSettings(
        @Header("Authorization") token: String,
        @Body settings: CloudSettings
    ): Response<SettingsResponse>
}

// Request models

data class SyncRideRequest(
    val timestamp: Long,
    val duration: Long,
    val balance_left_avg: Int,
    val balance_right_avg: Int,
    val te_left_avg: Int,
    val te_right_avg: Int,
    val ps_left_avg: Int,
    val ps_right_avg: Int,
    val optimal_pct: Int,
    val attention_pct: Int,
    val problem_pct: Int,
    // Extended metrics
    val power_avg: Int = 0,
    val power_max: Int = 0,
    val cadence_avg: Int = 0,
    val hr_avg: Int = 0,
    val speed_avg: Float = 0f,
    val distance_km: Float = 0f
)

/**
 * Request model for syncing ride with per-minute snapshots.
 */
data class SyncRideWithSnapshotsRequest(
    val ride: SyncRideRequest,
    val snapshots: List<SyncSnapshotRequest>
)

/**
 * Per-minute snapshot for cloud sync.
 */
data class SyncSnapshotRequest(
    val minute_index: Int,
    val timestamp: Long,
    val balance_left: Int,
    val balance_right: Int,
    val te_left: Int,
    val te_right: Int,
    val ps_left: Int,
    val ps_right: Int,
    val power_avg: Int,
    val cadence_avg: Int,
    val hr_avg: Int,
    val zone_status: String
)

/**
 * Request model for syncing a single drill result.
 */
data class SyncDrillRequest(
    val drill_id: String,
    val drill_name: String,
    val timestamp: Long,
    val duration_ms: Long,
    val score: Float,
    val time_in_target_ms: Long,
    val time_in_target_percent: Float,
    val completed: Boolean,
    val phase_scores_json: String?
)

/**
 * Request model for batch syncing drill results.
 */
data class SyncDrillsRequest(
    val drills: List<SyncDrillRequest>
)

/**
 * Request model for syncing a single achievement.
 */
data class SyncAchievementRequest(
    val achievement_id: String,
    val unlocked_at: Long
)

/**
 * Request model for batch syncing achievements.
 */
data class SyncAchievementsRequest(
    val achievements: List<SyncAchievementRequest>
)

// Response models

data class SyncResponse(
    val success: Boolean,
    val data: SyncData? = null,
    val error: String? = null,
    val code: String? = null
) {
    companion object {
        const val CODE_DEVICE_REVOKED = "DEVICE_REVOKED"
    }
}

data class SyncData(
    val ride_id: String,
    val synced_at: String
)

data class SyncStatusResponse(
    val success: Boolean,
    val data: SyncStatusData? = null,
    val error: String? = null
)

data class SyncStatusData(
    val device_id: String,
    val last_sync: String?,
    val ride_count: Int
)

data class CheckSyncRequestResponse(
    val success: Boolean,
    val data: CheckSyncRequestData? = null,
    val error: String? = null,
    val code: String? = null
)

data class CheckSyncRequestData(
    val syncRequested: Boolean,
    val requestedAt: Long? = null
)

// Settings models

data class SettingsResponse(
    val success: Boolean,
    val data: SettingsData? = null,
    val error: String? = null
)

data class SettingsData(
    val settings: CloudSettings
)

/**
 * Cloud-synced user settings.
 * Uses snake_case for JSON serialization.
 */
data class CloudSettings(
    // Thresholds
    val balance_threshold: Int = 5,
    val te_optimal_min: Int = 70,
    val te_optimal_max: Int = 80,
    val ps_minimum: Int = 20,

    // Global alerts
    val alerts_enabled: Boolean = true,
    val screen_wake_on_alert: Boolean = true,

    // Balance alerts
    val balance_alert_enabled: Boolean = true,
    val balance_alert_trigger: String = "PROBLEM_ONLY",
    val balance_alert_visual: Boolean = true,
    val balance_alert_sound: Boolean = false,
    val balance_alert_vibration: Boolean = true,
    val balance_alert_cooldown: Int = 30,

    // TE alerts
    val te_alert_enabled: Boolean = true,
    val te_alert_trigger: String = "PROBLEM_ONLY",
    val te_alert_visual: Boolean = true,
    val te_alert_sound: Boolean = false,
    val te_alert_vibration: Boolean = true,
    val te_alert_cooldown: Int = 30,

    // PS alerts
    val ps_alert_enabled: Boolean = true,
    val ps_alert_trigger: String = "PROBLEM_ONLY",
    val ps_alert_visual: Boolean = true,
    val ps_alert_sound: Boolean = false,
    val ps_alert_vibration: Boolean = true,
    val ps_alert_cooldown: Int = 30,

    // Sync settings
    val background_mode_enabled: Boolean = true,
    val auto_sync_enabled: Boolean = true,

    // Metadata
    val updated_at: String? = null
)
