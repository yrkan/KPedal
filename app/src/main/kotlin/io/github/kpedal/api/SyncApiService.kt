package io.github.kpedal.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Sync API service for uploading ride data to cloud.
 */
interface SyncApiService {

    /**
     * Sync a single ride to cloud.
     */
    @POST("api/sync/ride")
    suspend fun syncRide(
        @Header("Authorization") token: String,
        @Header("X-Device-ID") deviceId: String,
        @Body ride: SyncRideRequest
    ): Response<SyncResponse>

    /**
     * Get sync status for this device.
     */
    @GET("api/sync/status")
    suspend fun getSyncStatus(
        @Header("Authorization") token: String,
        @Header("X-Device-ID") deviceId: String
    ): Response<SyncStatusResponse>
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
    val problem_pct: Int
)

// Response models

data class SyncResponse(
    val success: Boolean,
    val data: SyncData? = null,
    val error: String? = null
)

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
