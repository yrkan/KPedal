package io.github.kpedal.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.github.kpedal.api.ApiClient
import io.github.kpedal.api.RefreshRequest
import io.github.kpedal.api.SyncRideRequest
import io.github.kpedal.data.database.RideDao
import io.github.kpedal.data.database.RideEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service for syncing ride data to cloud.
 */
class SyncService(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val rideDao: RideDao,
    private val preferencesRepository: PreferencesRepository
) {
    enum class SyncStatus {
        IDLE,
        SYNCING,
        SUCCESS,
        FAILED,
        OFFLINE
    }

    data class SyncState(
        val status: SyncStatus = SyncStatus.IDLE,
        val pendingCount: Int = 0,
        val lastSyncTimestamp: Long = 0,
        val errorMessage: String? = null
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncState = MutableStateFlow(SyncState())

    /**
     * Flow of current sync state.
     */
    val syncStateFlow: Flow<SyncState> = _syncState.asStateFlow()

    /**
     * Current sync state.
     */
    val currentState: SyncState get() = _syncState.value

    init {
        // Collect pending count and update state
        scope.launch {
            rideDao.getPendingSyncCountFlow().collect { count ->
                _syncState.value = _syncState.value.copy(pendingCount = count)
            }
        }
        // Collect last sync timestamp
        scope.launch {
            preferencesRepository.lastSyncTimestampFlow.collect { timestamp ->
                _syncState.value = _syncState.value.copy(lastSyncTimestamp = timestamp)
            }
        }
    }

    /**
     * Sync all pending rides to cloud.
     * @return Number of rides successfully synced
     */
    suspend fun syncPendingRides(): Int {
        if (!authRepository.isLoggedIn) {
            return 0
        }

        if (!isNetworkAvailable()) {
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.OFFLINE,
                errorMessage = "No network connection"
            )
            return 0
        }

        _syncState.value = _syncState.value.copy(
            status = SyncStatus.SYNCING,
            errorMessage = null
        )

        val pendingRides = rideDao.getPendingRides()
        if (pendingRides.isEmpty()) {
            _syncState.value = _syncState.value.copy(status = SyncStatus.IDLE)
            return 0
        }

        var successCount = 0
        for (ride in pendingRides) {
            if (syncRide(ride)) {
                successCount++
            }
        }

        val finalStatus = if (successCount == pendingRides.size) {
            SyncStatus.SUCCESS
        } else if (successCount > 0) {
            SyncStatus.SUCCESS // Partial success
        } else {
            SyncStatus.FAILED
        }

        if (successCount > 0) {
            preferencesRepository.updateLastSyncTimestamp(System.currentTimeMillis())
        }

        _syncState.value = _syncState.value.copy(status = finalStatus)

        return successCount
    }

    /**
     * Sync a single ride to cloud.
     * @return true if sync was successful
     */
    suspend fun syncRide(ride: RideEntity): Boolean {
        if (!authRepository.isLoggedIn) {
            return false
        }

        val accessToken = authRepository.getAccessToken() ?: run {
            // Try to refresh token
            if (!refreshToken()) {
                return false
            }
            authRepository.getAccessToken() ?: return false
        }

        val deviceId = authRepository.getOrCreateDeviceId()

        val request = SyncRideRequest(
            timestamp = ride.timestamp,
            duration = ride.durationMs,
            balance_left_avg = ride.balanceLeft,
            balance_right_avg = ride.balanceRight,
            te_left_avg = ride.teLeft,
            te_right_avg = ride.teRight,
            ps_left_avg = ride.psLeft,
            ps_right_avg = ride.psRight,
            optimal_pct = ride.zoneOptimal,
            attention_pct = ride.zoneAttention,
            problem_pct = ride.zoneProblem
        )

        return try {
            val response = ApiClient.syncService.syncRide(
                token = "Bearer $accessToken",
                deviceId = deviceId,
                ride = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                rideDao.markAsSynced(ride.id)
                true
            } else if (response.code() == 401) {
                // Token expired, try refresh
                if (refreshToken()) {
                    // Retry with new token
                    val newToken = authRepository.getAccessToken() ?: return false
                    val retryResponse = ApiClient.syncService.syncRide(
                        token = "Bearer $newToken",
                        deviceId = deviceId,
                        ride = request
                    )
                    if (retryResponse.isSuccessful && retryResponse.body()?.success == true) {
                        rideDao.markAsSynced(ride.id)
                        true
                    } else {
                        rideDao.markAsSyncFailed(ride.id)
                        false
                    }
                } else {
                    rideDao.markAsSyncFailed(ride.id)
                    false
                }
            } else {
                rideDao.markAsSyncFailed(ride.id)
                _syncState.value = _syncState.value.copy(
                    errorMessage = response.body()?.error ?: "Sync failed"
                )
                false
            }
        } catch (e: Exception) {
            rideDao.markAsSyncFailed(ride.id)
            _syncState.value = _syncState.value.copy(errorMessage = e.message)
            false
        }
    }

    /**
     * Called after a ride is saved. Triggers sync if auto-sync is enabled.
     */
    fun onRideSaved(rideId: Long) {
        scope.launch {
            preferencesRepository.autoSyncEnabledFlow.collect { autoSync ->
                if (autoSync && authRepository.isLoggedIn) {
                    val ride = rideDao.getRideById(rideId)
                    if (ride != null && ride.syncStatus == RideEntity.SYNC_STATUS_PENDING) {
                        syncRide(ride)
                    }
                }
                return@collect
            }
        }
    }

    /**
     * Refresh access token using refresh token.
     * @return true if refresh was successful
     */
    private suspend fun refreshToken(): Boolean {
        val refreshToken = authRepository.getRefreshToken() ?: return false

        return try {
            val response = ApiClient.authService.refreshToken(RefreshRequest(refreshToken))
            if (response.isSuccessful && response.body()?.success == true) {
                val newAccessToken = response.body()?.data?.access_token
                if (newAccessToken != null) {
                    authRepository.updateAccessToken(newAccessToken)
                    true
                } else {
                    false
                }
            } else {
                // Refresh token invalid, user needs to re-login
                authRepository.clearAuth()
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
