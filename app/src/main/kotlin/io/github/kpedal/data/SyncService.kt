package io.github.kpedal.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.github.kpedal.api.ApiClient
import io.github.kpedal.api.CloudSettings
import io.github.kpedal.api.RefreshRequest
import io.github.kpedal.api.SyncAchievementRequest
import io.github.kpedal.api.SyncAchievementsRequest
import io.github.kpedal.api.SyncDrillRequest
import io.github.kpedal.api.SyncDrillsRequest
import io.github.kpedal.api.SyncResponse
import io.github.kpedal.api.SyncRideRequest
import io.github.kpedal.api.SyncRideWithSnapshotsRequest
import io.github.kpedal.api.SyncSnapshotRequest
import io.github.kpedal.data.database.AchievementDao
import io.github.kpedal.data.database.AchievementEntity
import io.github.kpedal.data.database.DrillResultDao
import io.github.kpedal.data.database.DrillResultEntity
import io.github.kpedal.data.database.RideDao
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.engine.RideSnapshot
import io.github.kpedal.engine.StatusCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Service for syncing ride data to cloud.
 *
 * Sync strategy:
 * - App settings changes → auto-upload to cloud (debounced)
 * - Sync button → pull from cloud only
 * - Ride start → pull from cloud (get latest web changes)
 */
@OptIn(FlowPreview::class)
class SyncService(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val rideDao: RideDao,
    private val drillResultDao: DrillResultDao,
    private val achievementDao: AchievementDao,
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
        val errorMessage: String? = null,
        val deviceRevoked: Boolean = false
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

    companion object {
        private const val TAG = "SyncService"
        private const val SETTINGS_UPLOAD_DEBOUNCE_MS = 2000L // 2 seconds debounce
        private const val NETWORK_SYNC_COOLDOWN_MS = 60_000L // 1 minute cooldown between network-triggered syncs
    }

    // Network callback for automatic sync when network becomes available
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Track ride state to avoid syncing during active rides
    @Volatile
    private var isRecording = false

    // Debounce network-triggered syncs
    @Volatile
    private var lastNetworkSyncAttemptTime = 0L

    // Flag to prevent upload loop when applying cloud settings
    @Volatile
    private var isApplyingCloudSettings = false

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
        // Auto-upload settings when they change (debounced)
        scope.launch {
            combine(
                preferencesRepository.settingsFlow,
                preferencesRepository.alertSettingsFlow,
                preferencesRepository.backgroundModeEnabledFlow,
                preferencesRepository.autoSyncEnabledFlow
            ) { settings, alertSettings, backgroundMode, autoSync ->
                // Combine into a single object for change detection
                SettingsSnapshot(settings, alertSettings, backgroundMode, autoSync)
            }
                .distinctUntilChanged()
                .drop(1) // Skip initial emission (don't upload on app start)
                .debounce(SETTINGS_UPLOAD_DEBOUNCE_MS)
                .collect {
                    // Skip upload if we're applying cloud settings (prevents loop)
                    if (isApplyingCloudSettings) {
                        android.util.Log.d(TAG, "Skipping upload - applying cloud settings")
                        return@collect
                    }
                    if (authRepository.isLoggedIn && isNetworkAvailable()) {
                        val uploaded = uploadSettings()
                        android.util.Log.i(TAG, "Auto-uploaded settings on change: $uploaded")
                    }
                }
        }

        // Register network callback for automatic sync when network is restored
        registerNetworkCallback()
    }

    /**
     * Register a network callback to automatically sync pending data when network becomes available.
     * This handles the case where a ride was recorded offline and the device later connects to WiFi.
     */
    private fun registerNetworkCallback() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            android.util.Log.w(TAG, "ConnectivityManager not available")
            return
        }

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                android.util.Log.i(TAG, "Network became available")
                scope.launch {
                    onNetworkBecameAvailable()
                }
            }

            override fun onLost(network: Network) {
                android.util.Log.d(TAG, "Network lost")
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
            android.util.Log.i(TAG, "Registered network callback for auto-sync on network restore")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to register network callback: ${e.message}")
            networkCallback = null
        }
    }

    /**
     * Called when network becomes available. Syncs all pending data if conditions are met.
     *
     * Conditions:
     * - User is logged in
     * - Not currently recording a ride (don't interrupt active ride)
     * - Cooldown period has passed (prevent rapid-fire syncs on network flapping)
     * - There is pending data to sync
     */
    private suspend fun onNetworkBecameAvailable() {
        // Skip if not logged in
        if (!authRepository.isLoggedIn) {
            android.util.Log.d(TAG, "Network available but not logged in, skipping sync")
            return
        }

        // Skip if currently recording a ride - don't interfere with active ride
        if (isRecording) {
            android.util.Log.d(TAG, "Network available but ride in progress, skipping sync")
            return
        }

        // Debounce - don't sync too frequently (handles network flapping)
        val now = System.currentTimeMillis()
        if (now - lastNetworkSyncAttemptTime < NETWORK_SYNC_COOLDOWN_MS) {
            android.util.Log.d(TAG, "Network available but cooldown active (${(NETWORK_SYNC_COOLDOWN_MS - (now - lastNetworkSyncAttemptTime)) / 1000}s remaining), skipping sync")
            return
        }

        // Check if there's anything to sync
        val pendingRides = rideDao.getPendingRides()
        val pendingDrills = drillResultDao.getPendingSync()
        val pendingAchievements = achievementDao.getPendingSync()

        if (pendingRides.isEmpty() && pendingDrills.isEmpty() && pendingAchievements.isEmpty()) {
            android.util.Log.d(TAG, "Network available but nothing pending to sync")
            return
        }

        android.util.Log.i(TAG, "Network restored - syncing ${pendingRides.size} rides, ${pendingDrills.size} drills, ${pendingAchievements.size} achievements")
        lastNetworkSyncAttemptTime = now

        _syncState.value = _syncState.value.copy(
            status = SyncStatus.SYNCING,
            errorMessage = null
        )

        try {
            // Sync all pending data
            val syncedRides = syncPendingRidesInternal()
            val syncedDrills = syncPendingDrills()
            val syncedAchievements = syncPendingAchievements()

            android.util.Log.i(TAG, "Network sync complete: $syncedRides rides, $syncedDrills drills, $syncedAchievements achievements")

            if (syncedRides > 0 || syncedDrills > 0 || syncedAchievements > 0) {
                preferencesRepository.updateLastSyncTimestamp(System.currentTimeMillis())
                _syncState.value = _syncState.value.copy(
                    status = SyncStatus.SUCCESS,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            } else {
                // Nothing synced (maybe all failed)
                val remaining = rideDao.getPendingRides().size + drillResultDao.getPendingSync().size + achievementDao.getPendingSync().size
                if (remaining > 0) {
                    _syncState.value = _syncState.value.copy(status = SyncStatus.FAILED)
                } else {
                    _syncState.value = _syncState.value.copy(status = SyncStatus.IDLE)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Network sync failed: ${e.message}")
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.FAILED,
                errorMessage = e.message
            )
        }
    }

    /**
     * Notify SyncService of ride state changes.
     * Called by RideStateMonitor when ride starts/ends.
     *
     * @param recording true if a ride is currently being recorded
     */
    fun notifyRideStateChanged(recording: Boolean) {
        val wasRecording = isRecording
        isRecording = recording
        android.util.Log.d(TAG, "Ride state changed: recording=$recording")

        // When ride ends, try to sync if we have network
        if (wasRecording && !recording) {
            scope.launch {
                // Small delay to allow ride save to complete
                delay(2000)
                if (isNetworkAvailable() && authRepository.isLoggedIn) {
                    android.util.Log.i(TAG, "Ride ended, checking for pending syncs")
                    // This will respect the cooldown
                    onNetworkBecameAvailable()
                }
            }
        }
    }

    /**
     * Snapshot of all settings for change detection.
     */
    private data class SettingsSnapshot(
        val settings: PreferencesRepository.Settings,
        val alertSettings: AlertSettings,
        val backgroundMode: Boolean,
        val autoSync: Boolean
    )

    /**
     * Full sync: pull settings from cloud + sync pending rides.
     * Use this for manual "Sync" button.
     *
     * Note: Local settings are auto-uploaded on change, so this only pulls.
     */
    suspend fun fullSync() {
        if (!authRepository.isLoggedIn) {
            return
        }

        if (!isNetworkAvailable()) {
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.OFFLINE,
                errorMessage = "No network connection"
            )
            return
        }

        _syncState.value = _syncState.value.copy(
            status = SyncStatus.SYNCING,
            errorMessage = null
        )

        try {
            // 1. Pull latest settings from cloud (web is source of truth on manual sync)
            val fetched = fetchSettings()
            android.util.Log.i(TAG, "Fetched settings from cloud: $fetched")

            // 2. Sync pending rides to cloud
            val syncedRides = syncPendingRidesInternal()
            android.util.Log.i(TAG, "Synced $syncedRides rides")

            // 3. Sync pending drills to cloud
            val syncedDrills = syncPendingDrills()
            android.util.Log.i(TAG, "Synced $syncedDrills drills")

            // 4. Sync pending achievements to cloud
            val syncedAchievements = syncPendingAchievements()
            android.util.Log.i(TAG, "Synced $syncedAchievements achievements")

            _syncState.value = _syncState.value.copy(
                status = SyncStatus.SUCCESS,
                lastSyncTimestamp = System.currentTimeMillis()
            )

            preferencesRepository.updateLastSyncTimestamp(System.currentTimeMillis())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Full sync failed: ${e.message}")
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.FAILED,
                errorMessage = e.message
            )
        }
    }

    /**
     * Sync all pending rides to cloud.
     * Updates sync state during operation.
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

        val successCount = syncPendingRidesInternal()

        val finalStatus = if (successCount > 0) {
            SyncStatus.SUCCESS
        } else if (rideDao.getPendingRides().isEmpty()) {
            SyncStatus.IDLE
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
     * Internal method for syncing pending rides without state updates.
     * @return Number of rides successfully synced
     */
    private suspend fun syncPendingRidesInternal(): Int {
        val pendingRides = rideDao.getPendingRides()
        if (pendingRides.isEmpty()) {
            return 0
        }

        var successCount = 0
        for (ride in pendingRides) {
            if (syncRide(ride)) {
                successCount++
            }
        }

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

        val request = createSyncRideRequest(ride)

        return try {
            val response = ApiClient.syncService.syncRide(
                token = "Bearer $accessToken",
                deviceId = deviceId,
                ride = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                rideDao.markAsSynced(ride.id)
                true
            } else if (response.code() == 403 && response.body()?.code == SyncResponse.CODE_DEVICE_REVOKED) {
                // Device was revoked from web - logout
                handleDeviceRevoked()
                false
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
                    } else if (retryResponse.code() == 403 && retryResponse.body()?.code == SyncResponse.CODE_DEVICE_REVOKED) {
                        handleDeviceRevoked()
                        false
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
     * Sync a single ride with per-minute snapshots to cloud.
     * Used when ride is saved with collected snapshots.
     * @return true if sync was successful
     */
    suspend fun syncRideWithSnapshots(ride: RideEntity, snapshots: List<RideSnapshot>): Boolean {
        if (!authRepository.isLoggedIn) {
            return false
        }

        val accessToken = authRepository.getAccessToken() ?: run {
            if (!refreshToken()) {
                return false
            }
            authRepository.getAccessToken() ?: return false
        }

        val deviceId = authRepository.getOrCreateDeviceId()

        val rideRequest = createSyncRideRequest(ride)

        val snapshotRequests = snapshots.map { snapshot ->
            SyncSnapshotRequest(
                minute_index = snapshot.minuteIndex,
                timestamp = snapshot.timestamp,
                balance_left = snapshot.balanceLeft,
                balance_right = snapshot.balanceRight,
                te_left = snapshot.teLeft,
                te_right = snapshot.teRight,
                ps_left = snapshot.psLeft,
                ps_right = snapshot.psRight,
                power_avg = snapshot.powerAvg,
                cadence_avg = snapshot.cadenceAvg,
                hr_avg = snapshot.heartRateAvg,
                zone_status = snapshot.zoneStatus
            )
        }

        val request = SyncRideWithSnapshotsRequest(
            ride = rideRequest,
            snapshots = snapshotRequests
        )

        return try {
            val response = ApiClient.syncService.syncRideWithSnapshots(
                token = "Bearer $accessToken",
                deviceId = deviceId,
                request = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                rideDao.markAsSynced(ride.id)
                android.util.Log.i(TAG, "Synced ride with ${snapshots.size} snapshots")
                true
            } else if (response.code() == 403 && response.body()?.code == SyncResponse.CODE_DEVICE_REVOKED) {
                handleDeviceRevoked()
                false
            } else if (response.code() == 401) {
                if (refreshToken()) {
                    val newToken = authRepository.getAccessToken() ?: return false
                    val retryResponse = ApiClient.syncService.syncRideWithSnapshots(
                        token = "Bearer $newToken",
                        deviceId = deviceId,
                        request = request
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
            android.util.Log.e(TAG, "Error syncing ride with snapshots: ${e.message}")
            false
        }
    }

    // ========================================
    // Drill Sync Methods
    // ========================================

    /**
     * Sync a single drill result to cloud.
     * @return true if sync was successful
     */
    suspend fun syncDrill(drill: DrillResultEntity): Boolean {
        if (!authRepository.isLoggedIn) {
            return false
        }

        val accessToken = authRepository.getAccessToken() ?: run {
            if (!refreshToken()) return false
            authRepository.getAccessToken() ?: return false
        }

        val deviceId = authRepository.getOrCreateDeviceId()

        val request = SyncDrillRequest(
            drill_id = drill.drillId,
            drill_name = drill.drillName,
            timestamp = drill.timestamp,
            duration_ms = drill.durationMs,
            score = drill.score,
            time_in_target_ms = drill.timeInTargetMs,
            time_in_target_percent = drill.timeInTargetPercent,
            completed = drill.completed,
            phase_scores_json = drill.phaseScoresJson
        )

        return try {
            val response = ApiClient.syncService.syncDrill(
                token = "Bearer $accessToken",
                deviceId = deviceId,
                drill = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                drillResultDao.markAsSynced(drill.id)
                android.util.Log.i(TAG, "Synced drill: ${drill.drillName}")
                true
            } else if (response.code() == 403 && response.body()?.code == SyncResponse.CODE_DEVICE_REVOKED) {
                handleDeviceRevoked()
                false
            } else if (response.code() == 401) {
                if (refreshToken()) {
                    val newToken = authRepository.getAccessToken() ?: return false
                    val retryResponse = ApiClient.syncService.syncDrill(
                        token = "Bearer $newToken",
                        deviceId = deviceId,
                        drill = request
                    )
                    if (retryResponse.isSuccessful && retryResponse.body()?.success == true) {
                        drillResultDao.markAsSynced(drill.id)
                        true
                    } else {
                        drillResultDao.markAsSyncFailed(drill.id)
                        false
                    }
                } else {
                    drillResultDao.markAsSyncFailed(drill.id)
                    false
                }
            } else {
                drillResultDao.markAsSyncFailed(drill.id)
                false
            }
        } catch (e: Exception) {
            drillResultDao.markAsSyncFailed(drill.id)
            android.util.Log.e(TAG, "Error syncing drill: ${e.message}")
            false
        }
    }

    /**
     * Sync all pending drill results to cloud.
     * @return Number of drills successfully synced
     */
    suspend fun syncPendingDrills(): Int {
        if (!authRepository.isLoggedIn || !isNetworkAvailable()) {
            return 0
        }

        val pendingDrills = drillResultDao.getPendingSync()
        if (pendingDrills.isEmpty()) {
            return 0
        }

        var successCount = 0
        for (drill in pendingDrills) {
            if (syncDrill(drill)) {
                successCount++
            }
        }

        android.util.Log.i(TAG, "Synced $successCount/${pendingDrills.size} pending drills")
        return successCount
    }

    // ========================================
    // Achievement Sync Methods
    // ========================================

    /**
     * Sync all pending achievements to cloud in a batch.
     * @return Number of achievements successfully synced
     */
    suspend fun syncPendingAchievements(): Int {
        if (!authRepository.isLoggedIn || !isNetworkAvailable()) {
            return 0
        }

        val pendingAchievements = achievementDao.getPendingSync()
        if (pendingAchievements.isEmpty()) {
            return 0
        }

        val accessToken = authRepository.getAccessToken() ?: run {
            if (!refreshToken()) return 0
            authRepository.getAccessToken() ?: return 0
        }

        val deviceId = authRepository.getOrCreateDeviceId()

        val requests = pendingAchievements.map { achievement ->
            SyncAchievementRequest(
                achievement_id = achievement.id,
                unlocked_at = achievement.unlockedAt
            )
        }

        return try {
            val response = ApiClient.syncService.syncAchievements(
                token = "Bearer $accessToken",
                deviceId = deviceId,
                request = SyncAchievementsRequest(requests)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                // Mark all as synced
                pendingAchievements.forEach { achievement ->
                    achievementDao.markAsSynced(achievement.id)
                }
                android.util.Log.i(TAG, "Synced ${pendingAchievements.size} achievements")
                pendingAchievements.size
            } else if (response.code() == 403 && response.body()?.code == SyncResponse.CODE_DEVICE_REVOKED) {
                handleDeviceRevoked()
                0
            } else if (response.code() == 401) {
                if (refreshToken()) {
                    val newToken = authRepository.getAccessToken() ?: return 0
                    val retryResponse = ApiClient.syncService.syncAchievements(
                        token = "Bearer $newToken",
                        deviceId = deviceId,
                        request = SyncAchievementsRequest(requests)
                    )
                    if (retryResponse.isSuccessful && retryResponse.body()?.success == true) {
                        pendingAchievements.forEach { achievement ->
                            achievementDao.markAsSynced(achievement.id)
                        }
                        pendingAchievements.size
                    } else {
                        pendingAchievements.forEach { achievement ->
                            achievementDao.markAsSyncFailed(achievement.id)
                        }
                        0
                    }
                } else {
                    pendingAchievements.forEach { achievement ->
                        achievementDao.markAsSyncFailed(achievement.id)
                    }
                    0
                }
            } else {
                pendingAchievements.forEach { achievement ->
                    achievementDao.markAsSyncFailed(achievement.id)
                }
                0
            }
        } catch (e: Exception) {
            pendingAchievements.forEach { achievement ->
                achievementDao.markAsSyncFailed(achievement.id)
            }
            android.util.Log.e(TAG, "Error syncing achievements: ${e.message}")
            0
        }
    }

    /**
     * Sync a single achievement to cloud immediately.
     * Call this when an achievement is unlocked.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onAchievementUnlocked(achievementId: String) {
        scope.launch {
            if (authRepository.isLoggedIn && preferencesRepository.autoSyncEnabledFlow.first()) {
                syncPendingAchievements()
            }
        }
    }

    /**
     * Sync a drill result after it's saved.
     * Call this when a drill is completed.
     */
    fun onDrillCompleted(drillId: Long) {
        scope.launch {
            if (authRepository.isLoggedIn && preferencesRepository.autoSyncEnabledFlow.first()) {
                val drill = drillResultDao.getResult(drillId)
                if (drill != null && drill.syncStatus == DrillResultEntity.SYNC_STATUS_PENDING) {
                    syncDrill(drill)
                }
            }
        }
    }

    /**
     * Called after a ride is saved. Triggers sync if auto-sync is enabled.
     * NOTE: This syncs WITHOUT snapshots. Use onRideSavedWithSnapshots for full sync.
     */
    fun onRideSaved(rideId: Long) {
        scope.launch {
            val autoSync = preferencesRepository.autoSyncEnabledFlow.first()
            if (autoSync && authRepository.isLoggedIn) {
                val ride = rideDao.getRideById(rideId)
                if (ride != null && ride.syncStatus == RideEntity.SYNC_STATUS_PENDING) {
                    syncRide(ride)
                }
            }
        }
    }

    /**
     * Called after a ride is saved with snapshots collected during the ride.
     * Triggers full sync with per-minute data if auto-sync is enabled.
     */
    fun onRideSavedWithSnapshots(rideId: Long, snapshots: List<RideSnapshot>) {
        scope.launch {
            val autoSync = preferencesRepository.autoSyncEnabledFlow.first()
            if (autoSync && authRepository.isLoggedIn) {
                val ride = rideDao.getRideById(rideId)
                if (ride != null && ride.syncStatus == RideEntity.SYNC_STATUS_PENDING) {
                    if (snapshots.isNotEmpty()) {
                        syncRideWithSnapshots(ride, snapshots)
                    } else {
                        // Fallback to simple sync if no snapshots
                        syncRide(ride)
                    }
                }
            }
        }
    }

    /**
     * Refresh access token using refresh token.
     * Sends device_id to verify device is still authorized.
     * @return true if refresh was successful
     */
    private suspend fun refreshToken(): Boolean {
        val refreshToken = authRepository.getRefreshToken() ?: return false
        val deviceId = authRepository.getOrCreateDeviceId()

        return try {
            val response = ApiClient.authService.refreshToken(
                deviceId = deviceId,
                request = RefreshRequest(refreshToken)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val newAccessToken = response.body()?.data?.access_token
                if (newAccessToken != null) {
                    authRepository.updateAccessToken(newAccessToken)
                    true
                } else {
                    false
                }
            } else if (response.code() == 403 && response.body()?.code == io.github.kpedal.api.RefreshResponse.CODE_DEVICE_REVOKED) {
                // Device was revoked from web - handle revocation
                handleDeviceRevoked()
                false
            } else {
                // Refresh token invalid, user needs to re-login
                authRepository.clearAuth()
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a sync was requested from the web dashboard.
     * If so, triggers a sync immediately.
     * Also fetches latest settings from cloud.
     * @return true if sync was triggered
     */
    suspend fun checkAndHandleSyncRequest(): Boolean {
        if (!authRepository.isLoggedIn) {
            return false
        }

        if (!isNetworkAvailable()) {
            return false
        }

        val accessToken = authRepository.getAccessToken() ?: run {
            if (!refreshToken()) return false
            authRepository.getAccessToken() ?: return false
        }

        val deviceId = authRepository.getOrCreateDeviceId()

        // Fetch and apply cloud settings on each heartbeat
        fetchAndApplySettings(accessToken)

        return try {
            val response = ApiClient.syncService.checkSyncRequest(
                token = "Bearer $accessToken",
                deviceId = deviceId
            )

            if (response.isSuccessful && response.body()?.data?.syncRequested == true) {
                // Sync was requested! Trigger sync immediately
                syncPendingRides()
                true
            } else if (response.code() == 403 && response.body()?.code == SyncResponse.CODE_DEVICE_REVOKED) {
                // Device was revoked from web - logout
                handleDeviceRevoked()
                false
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetch settings from cloud and apply them locally.
     * Public method for explicit settings sync (e.g., on ride start).
     * @return true if settings were fetched and applied successfully
     */
    suspend fun fetchSettings(): Boolean {
        if (!authRepository.isLoggedIn) {
            return false
        }

        if (!isNetworkAvailable()) {
            return false
        }

        val accessToken = authRepository.getAccessToken() ?: run {
            if (!refreshToken()) return false
            authRepository.getAccessToken() ?: return false
        }

        return fetchAndApplySettings(accessToken)
    }

    /**
     * Fetch settings from cloud and apply them locally.
     * This is called on heartbeat to get settings changed on web.
     * @return true if settings were fetched successfully
     */
    private suspend fun fetchAndApplySettings(accessToken: String): Boolean {
        return try {
            val response = ApiClient.syncService.getSettings("Bearer $accessToken")
            if (response.isSuccessful && response.body()?.success == true) {
                val cloudSettings = response.body()?.data?.settings
                if (cloudSettings != null) {
                    applyCloudSettings(cloudSettings)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncService", "Failed to fetch settings: ${e.message}")
            false
        }
    }

    /**
     * Apply cloud settings to local preferences.
     */
    private suspend fun applyCloudSettings(settings: CloudSettings) {
        // Set flag to prevent auto-upload loop
        isApplyingCloudSettings = true

        try {
            // Thresholds
            preferencesRepository.updateBalanceThreshold(settings.balance_threshold)
            preferencesRepository.updateTeOptimalRange(settings.te_optimal_min, settings.te_optimal_max)
            preferencesRepository.updatePsMinimum(settings.ps_minimum)

            // Global alerts
            preferencesRepository.updateGlobalAlertsEnabled(settings.alerts_enabled)
            preferencesRepository.updateScreenWakeOnAlert(settings.screen_wake_on_alert)

            // Balance alerts
            preferencesRepository.updateBalanceAlertConfig(
                MetricAlertConfig(
                    enabled = settings.balance_alert_enabled,
                    triggerLevel = try {
                        AlertTriggerLevel.valueOf(settings.balance_alert_trigger)
                    } catch (e: Exception) {
                        AlertTriggerLevel.PROBLEM_ONLY
                    },
                    visualAlert = settings.balance_alert_visual,
                    soundAlert = settings.balance_alert_sound,
                    vibrationAlert = settings.balance_alert_vibration,
                    cooldownSeconds = settings.balance_alert_cooldown
                )
            )

            // TE alerts
            preferencesRepository.updateTeAlertConfig(
                MetricAlertConfig(
                    enabled = settings.te_alert_enabled,
                    triggerLevel = try {
                        AlertTriggerLevel.valueOf(settings.te_alert_trigger)
                    } catch (e: Exception) {
                        AlertTriggerLevel.PROBLEM_ONLY
                    },
                    visualAlert = settings.te_alert_visual,
                    soundAlert = settings.te_alert_sound,
                    vibrationAlert = settings.te_alert_vibration,
                    cooldownSeconds = settings.te_alert_cooldown
                )
            )

            // PS alerts
            preferencesRepository.updatePsAlertConfig(
                MetricAlertConfig(
                    enabled = settings.ps_alert_enabled,
                    triggerLevel = try {
                        AlertTriggerLevel.valueOf(settings.ps_alert_trigger)
                    } catch (e: Exception) {
                        AlertTriggerLevel.PROBLEM_ONLY
                    },
                    visualAlert = settings.ps_alert_visual,
                    soundAlert = settings.ps_alert_sound,
                    vibrationAlert = settings.ps_alert_vibration,
                    cooldownSeconds = settings.ps_alert_cooldown
                )
            )

            // Sync settings
            preferencesRepository.updateBackgroundModeEnabled(settings.background_mode_enabled)
            preferencesRepository.updateAutoSyncEnabled(settings.auto_sync_enabled)

            android.util.Log.i(TAG, "Applied cloud settings")
        } finally {
            // Clear flag after a delay to allow debounce to skip
            delay(SETTINGS_UPLOAD_DEBOUNCE_MS + 500)
            isApplyingCloudSettings = false
        }
    }

    /**
     * Upload current local settings to cloud.
     * Call this after settings are changed locally.
     */
    suspend fun uploadSettings(): Boolean {
        if (!authRepository.isLoggedIn) {
            return false
        }

        if (!isNetworkAvailable()) {
            return false
        }

        val accessToken = authRepository.getAccessToken() ?: run {
            if (!refreshToken()) return false
            authRepository.getAccessToken() ?: return false
        }

        return try {
            // Collect current settings
            val settings = collectLocalSettings()

            val response = ApiClient.syncService.updateSettings(
                token = "Bearer $accessToken",
                settings = settings
            )

            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            android.util.Log.w("SyncService", "Failed to upload settings: ${e.message}")
            false
        }
    }

    /**
     * Collect current local settings into CloudSettings object.
     */
    private suspend fun collectLocalSettings(): CloudSettings {
        // Get current settings from preferences using .first() to get single value
        val settings = preferencesRepository.settingsFlow.first()
        val alertSettings = preferencesRepository.alertSettingsFlow.first()
        val backgroundMode = preferencesRepository.backgroundModeEnabledFlow.first()
        val autoSync = preferencesRepository.autoSyncEnabledFlow.first()

        return CloudSettings(
            balance_threshold = settings.balanceThreshold,
            te_optimal_min = settings.teOptimalMin,
            te_optimal_max = settings.teOptimalMax,
            ps_minimum = settings.psMinimum,
            alerts_enabled = alertSettings.globalEnabled,
            screen_wake_on_alert = alertSettings.screenWakeOnAlert,
            balance_alert_enabled = alertSettings.balanceConfig.enabled,
            balance_alert_trigger = alertSettings.balanceConfig.triggerLevel.name,
            balance_alert_visual = alertSettings.balanceConfig.visualAlert,
            balance_alert_sound = alertSettings.balanceConfig.soundAlert,
            balance_alert_vibration = alertSettings.balanceConfig.vibrationAlert,
            balance_alert_cooldown = alertSettings.balanceConfig.cooldownSeconds,
            te_alert_enabled = alertSettings.teConfig.enabled,
            te_alert_trigger = alertSettings.teConfig.triggerLevel.name,
            te_alert_visual = alertSettings.teConfig.visualAlert,
            te_alert_sound = alertSettings.teConfig.soundAlert,
            te_alert_vibration = alertSettings.teConfig.vibrationAlert,
            te_alert_cooldown = alertSettings.teConfig.cooldownSeconds,
            ps_alert_enabled = alertSettings.psConfig.enabled,
            ps_alert_trigger = alertSettings.psConfig.triggerLevel.name,
            ps_alert_visual = alertSettings.psConfig.visualAlert,
            ps_alert_sound = alertSettings.psConfig.soundAlert,
            ps_alert_vibration = alertSettings.psConfig.vibrationAlert,
            ps_alert_cooldown = alertSettings.psConfig.cooldownSeconds,
            background_mode_enabled = backgroundMode,
            auto_sync_enabled = autoSync
        )
    }

    /**
     * Handle device revocation - clear auth and notify UI.
     */
    private fun handleDeviceRevoked() {
        android.util.Log.w("SyncService", "Device was revoked from web, logging out")
        authRepository.clearAuth()
        _syncState.value = _syncState.value.copy(
            status = SyncStatus.FAILED,
            deviceRevoked = true,
            errorMessage = "Device access revoked"
        )
    }

    /**
     * Clear the device revoked flag (after UI has shown message).
     */
    fun clearDeviceRevokedFlag() {
        _syncState.value = _syncState.value.copy(deviceRevoked = false)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Create SyncRideRequest from RideEntity with all fields.
     * Recalculates score for legacy rides (score = 0) to ensure consistent scoring.
     */
    private fun createSyncRideRequest(ride: RideEntity): SyncRideRequest {
        // For legacy rides (before score was added), recalculate using unified formula
        val score = if (ride.score == 0) {
            StatusCalculator.calculateOverallScore(
                balanceRight = ride.balanceRight,
                teLeft = ride.teLeft,
                teRight = ride.teRight,
                psLeft = ride.psLeft,
                psRight = ride.psRight,
                zoneOptimal = ride.zoneOptimal
            )
        } else {
            ride.score
        }

        return SyncRideRequest(
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
            problem_pct = ride.zoneProblem,
            // Unified score (recalculated for legacy rides)
            score = score,
            // Extended metrics
            power_avg = ride.powerAvg,
            power_max = ride.powerMax,
            cadence_avg = ride.cadenceAvg,
            hr_avg = ride.heartRateAvg,
            hr_max = ride.heartRateMax,
            speed_avg = ride.speedAvgKmh,
            distance_km = ride.distanceKm,
            // Pro cyclist metrics
            elevation_gain = ride.elevationGain,
            elevation_loss = ride.elevationLoss,
            grade_avg = ride.gradeAvg,
            grade_max = ride.gradeMax,
            normalized_power = ride.normalizedPower,
            energy_kj = ride.energyKj
        )
    }

    /**
     * Clean up resources. Call when service is no longer needed.
     */
    fun destroy() {
        // Unregister network callback
        networkCallback?.let { callback ->
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                connectivityManager?.unregisterNetworkCallback(callback)
                android.util.Log.i(TAG, "Unregistered network callback")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to unregister network callback: ${e.message}")
            }
        }
        networkCallback = null

        scope.cancel()
        android.util.Log.i(TAG, "SyncService destroyed")
    }
}
