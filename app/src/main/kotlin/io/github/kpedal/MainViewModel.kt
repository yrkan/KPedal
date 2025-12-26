package io.github.kpedal

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kpedal.api.ApiClient
import io.github.kpedal.api.LogoutRequest
import io.github.kpedal.auth.DeviceAuthService
import io.github.kpedal.data.AchievementRepository
import io.github.kpedal.data.AlertSettings
import io.github.kpedal.data.AnalyticsRepository
import io.github.kpedal.data.AuthRepository
import io.github.kpedal.data.MetricAlertConfig
import io.github.kpedal.data.PreferencesRepository
import io.github.kpedal.data.RideRepository
import io.github.kpedal.data.StreakCalculator
import io.github.kpedal.data.SyncService
import io.github.kpedal.data.database.KPedalDatabase
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.data.models.DashboardData
import io.github.kpedal.data.models.PedalInfo
import io.github.kpedal.data.models.TrendData
import io.github.kpedal.data.models.UnlockedAchievement
import io.github.kpedal.data.models.ChallengeProgress
import io.github.kpedal.data.models.WeeklyChallenges
import io.github.kpedal.engine.AchievementChecker
import io.github.kpedal.data.database.CustomDrillEntity
import io.github.kpedal.drill.CustomDrillRepository
import io.github.kpedal.drill.DrillCatalog
import io.github.kpedal.drill.DrillEngine
import io.github.kpedal.drill.DrillRepository
import io.github.kpedal.drill.model.Drill
import io.github.kpedal.drill.model.DrillExecutionState
import io.github.kpedal.drill.model.DrillExecutionStatus
import io.github.kpedal.drill.model.DrillPhase
import io.github.kpedal.drill.model.DrillResult
import io.github.kpedal.engine.PedalingMetrics
import io.github.kpedal.ui.screens.LiveRideData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for MainActivity.
 * Manages UI state and settings.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
        private const val EXTENSION_RETRY_DELAY_MS = 500L
        private const val EXTENSION_TIMEOUT_MS = 30_000L // 30 seconds timeout
    }

    private val preferencesRepository = PreferencesRepository(application)
    private val rideRepository = RideRepository(application)
    private val drillRepository = DrillRepository(application)
    private val customDrillRepository = CustomDrillRepository(application)
    private val analyticsRepository = AnalyticsRepository(application)
    private val achievementRepository = AchievementRepository(application)

    // Use AuthRepository from Extension when available, fallback to local
    private val authRepository: AuthRepository
        get() = KPedalExtension.instance?.authRepository ?: _localAuthRepository
    private val _localAuthRepository = AuthRepository(application)

    // Use SyncService from Extension (singleton) - avoids duplicate observers
    private val syncService: SyncService?
        get() = KPedalExtension.instance?.syncService

    private val achievementChecker = AchievementChecker(
        achievementRepository = achievementRepository,
        rideRepository = rideRepository,
        drillRepository = drillRepository
    )
    private val deviceAuthService by lazy { DeviceAuthService(authRepository) }

    // Vibrator for drill haptic feedback
    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to get vibrator: ${e.message}")
            null
        }
    }

    /**
     * Current settings (lazy collection - only when subscribed).
     */
    val settings: StateFlow<PreferencesRepository.Settings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PreferencesRepository.Settings()
        )

    /**
     * Alert settings (lazy collection - only when subscribed).
     */
    val alertSettings: StateFlow<AlertSettings> = preferencesRepository.alertSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlertSettings()
        )

    // ========== Auth & Sync ==========

    /**
     * Authentication state (reactive).
     * Uses currentState as initialValue to prevent flash of "Link Account" on open.
     */
    val authState: StateFlow<AuthRepository.AuthState> = authRepository.authStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentState
        )

    /**
     * Sync state (reactive).
     * Observes Extension's SyncService when available.
     */
    private val _syncState = MutableStateFlow(SyncService.SyncState())
    val syncState: StateFlow<SyncService.SyncState> = _syncState.asStateFlow()

    /**
     * Device auth flow state (reactive).
     */
    val deviceAuthState: StateFlow<DeviceAuthService.DeviceAuthState> = deviceAuthService.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeviceAuthService.DeviceAuthState.Idle
        )

    /**
     * Background mode enabled (reactive).
     */
    val backgroundModeEnabled: StateFlow<Boolean> = preferencesRepository.backgroundModeEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    /**
     * Auto-sync enabled (reactive).
     */
    val autoSyncEnabled: StateFlow<Boolean> = preferencesRepository.autoSyncEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    /**
     * Current pedaling metrics (from extension if connected).
     */
    private val _metrics = MutableStateFlow(PedalingMetrics())
    val metrics: StateFlow<PedalingMetrics> = _metrics.asStateFlow()

    // ========== Drill Engine ==========

    private val drillEngine: DrillEngine by lazy {
        DrillEngine(metrics)
    }

    /**
     * Current drill execution state (null if no drill running).
     */
    private val _drillState = MutableStateFlow<DrillExecutionState?>(null)
    val drillState: StateFlow<DrillExecutionState?> = _drillState.asStateFlow()

    /**
     * Current drill result (after completion).
     */
    private val _drillResult = MutableStateFlow<DrillResult?>(null)
    val drillResult: StateFlow<DrillResult?> = _drillResult.asStateFlow()

    /**
     * Extension connection state.
     */
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /**
     * Live ride data (collected from real ride data).
     */
    private val _liveData = MutableStateFlow(LiveRideData())
    val liveData: StateFlow<LiveRideData> = _liveData.asStateFlow()

    init {
        // Single coroutine to observe all extension data
        viewModelScope.launch {
            observeExtension()
        }
    }

    /**
     * Wait for KPedalExtension and observe all its data streams.
     * Consolidates all observations into a single coroutine for efficiency.
     */
    private suspend fun CoroutineScope.observeExtension() {
        // Wait for extension to become available with timeout
        var extension: KPedalExtension? = null
        var elapsedTime = 0L
        while (extension == null && elapsedTime < EXTENSION_TIMEOUT_MS) {
            extension = KPedalExtension.instance
            if (extension == null) {
                delay(EXTENSION_RETRY_DELAY_MS)
                elapsedTime += EXTENSION_RETRY_DELAY_MS
            }
        }

        if (extension == null) {
            android.util.Log.w(TAG, "Extension not available after ${EXTENSION_TIMEOUT_MS}ms timeout")
            return
        }

        // Capture references before launching coroutines to avoid race conditions
        val isConnectedFlow = extension.isConnected
        val metricsFlow = extension.pedalingEngine.metrics
        val liveDataFlow = extension.pedalingEngine.liveDataCollector.liveData
        val pedalInfoFlow = extension.pedalMonitor.pedalInfo

        // Launch parallel collectors for each data stream
        // Connection state
        launch {
            try {
                isConnectedFlow.collect { connected ->
                    _isConnected.value = connected
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Connection observation cancelled: ${e.message}")
            }
        }

        // Metrics
        launch {
            try {
                metricsFlow.collect { m ->
                    _metrics.value = m
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Metrics observation cancelled: ${e.message}")
            }
        }

        // Live data
        launch {
            try {
                liveDataFlow.collect { data ->
                    _liveData.value = data
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Live data observation cancelled: ${e.message}")
            }
        }

        // Pedal Info
        launch {
            try {
                pedalInfoFlow.collect { info ->
                    _pedalInfo.value = info
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Pedal info observation cancelled: ${e.message}")
            }
        }

        // Sync state (from Extension's SyncService)
        launch {
            try {
                extension.syncService.syncStateFlow.collect { state ->
                    _syncState.value = state
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Sync state observation cancelled: ${e.message}")
            }
        }
    }

    // Settings update methods

    fun updateBalanceThreshold(threshold: Int) {
        viewModelScope.launch {
            preferencesRepository.updateBalanceThreshold(threshold)
        }
    }

    fun updateTeOptimalRange(min: Int, max: Int) {
        viewModelScope.launch {
            preferencesRepository.updateTeOptimalRange(min, max)
        }
    }

    fun updatePsMinimum(minimum: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePsMinimum(minimum)
        }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateVibrationEnabled(enabled)
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateSoundEnabled(enabled)
        }
    }

    // ========== Alert Settings ==========

    fun updateGlobalAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateGlobalAlertsEnabled(enabled)
        }
    }

    fun updateScreenWakeOnAlert(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateScreenWakeOnAlert(enabled)
        }
    }

    fun updateBalanceAlertConfig(config: MetricAlertConfig) {
        viewModelScope.launch {
            preferencesRepository.updateBalanceAlertConfig(config)
        }
    }

    fun updateTeAlertConfig(config: MetricAlertConfig) {
        viewModelScope.launch {
            preferencesRepository.updateTeAlertConfig(config)
        }
    }

    fun updatePsAlertConfig(config: MetricAlertConfig) {
        viewModelScope.launch {
            preferencesRepository.updatePsAlertConfig(config)
        }
    }

    // ========== Drills ==========

    /**
     * Custom drills (reactive).
     */
    val customDrills: StateFlow<List<Drill>> = customDrillRepository.drillsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Get a drill by ID (checks both custom and catalog).
     */
    fun getDrill(drillId: String): Drill? {
        // Check custom drills first
        if (drillId.startsWith("custom_")) {
            return customDrills.value.find { it.id == drillId }
        }
        return DrillCatalog.getDrill(drillId)
    }

    /**
     * Save a custom drill.
     */
    fun saveCustomDrill(entity: CustomDrillEntity) {
        viewModelScope.launch {
            customDrillRepository.save(entity)
        }
    }

    /**
     * Delete a custom drill.
     */
    fun deleteCustomDrill(drillId: String) {
        if (!drillId.startsWith("custom_")) return
        val id = drillId.removePrefix("custom_").toLongOrNull() ?: return
        viewModelScope.launch {
            customDrillRepository.delete(id)
        }
    }

    /**
     * Get custom drill entity for editing.
     */
    suspend fun getCustomDrillEntity(id: Long): CustomDrillEntity? {
        return customDrillRepository.getEntity(id)
    }

    /**
     * Get best score for a drill.
     */
    fun getBestScore(drillId: String): Flow<Float?> = flow {
        emit(drillRepository.getBestScore(drillId))
    }

    /**
     * Get completed count for a drill.
     */
    fun getCompletedCount(drillId: String): Flow<Int> = flow {
        emit(drillRepository.getCompletedCount(drillId))
    }

    /**
     * All drill results (reactive) for history screen.
     */
    val drillResults: StateFlow<List<DrillResult>> = drillRepository.resultsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Delete a single drill result.
     */
    fun deleteDrillResult(id: Long) {
        viewModelScope.launch {
            drillRepository.deleteResult(id)
        }
    }

    /**
     * Delete all drill results.
     */
    fun deleteAllDrillResults() {
        viewModelScope.launch {
            drillRepository.deleteAllResults()
        }
    }

    /**
     * Get last N scores for a drill (for trend chart).
     */
    fun getRecentScores(drillId: String, count: Int = 5): Flow<List<Float>> {
        return drillRepository.getResultsForDrill(drillId).map { results ->
            results
                .filter { it.completed }
                .take(count)
                .map { it.score }
                .reversed() // Oldest to newest for chart
        }
    }

    /**
     * Get previous score for a drill (for comparison).
     * Returns the most recent completed score.
     */
    fun getPreviousScore(drillId: String): Flow<Float?> {
        return drillRepository.getResultsForDrill(drillId).map { results ->
            results.firstOrNull { it.completed }?.score
        }
    }

    // Track if we're already observing drill engine
    private var drillObserverJob: kotlinx.coroutines.Job? = null

    /**
     * Start a drill.
     */
    fun startDrill(drill: Drill) {
        // Reset previous state
        _drillResult.value = null

        // Set initial state immediately (before navigation happens)
        _drillState.value = DrillExecutionState(
            drill = drill,
            status = DrillExecutionStatus.COUNTDOWN,
            countdownSeconds = 3
        )

        // Setup callbacks with haptic feedback
        drillEngine.onCountdownTick = { _ ->
            // Short vibration for countdown
            vibrateShort()
        }

        drillEngine.onPhaseChange = { _, _ ->
            // Double vibration for phase change
            vibratePhaseChange()
        }

        drillEngine.onTargetEnter = {
            // Quick success vibration
            vibrateTargetEnter()
        }

        drillEngine.onTargetExit = {
            // Warning vibration
            vibrateTargetExit()
        }

        drillEngine.onPhaseEndingWarning = { _ ->
            // Quick tick for phase ending countdown
            vibratePhaseEndingWarning()
        }

        drillEngine.onComplete = { result ->
            // Completion vibration pattern
            vibrateComplete()
            viewModelScope.launch {
                val drillId = drillRepository.saveResult(result)
                _drillResult.value = result

                // Trigger sync for this drill
                KPedalExtension.instance?.syncService?.onDrillCompleted(drillId)

                // Check for drill achievements
                if (result.completed) {
                    checkDrillAchievements(result.score)
                }
            }
        }

        // Cancel previous observer if exists
        drillObserverJob?.cancel()

        // Observe engine state
        drillObserverJob = viewModelScope.launch {
            drillEngine.state.collect { state ->
                if (state != null) {
                    _drillState.value = state
                }
            }
        }

        // Start the drill
        drillEngine.start(drill)
    }

    // ========== Haptic Feedback for Drills ==========

    private fun vibrateShort() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(50)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun vibratePhaseChange() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Double pulse for phase change
                    val timings = longArrayOf(0, 80, 100, 80)
                    val amplitudes = intArrayOf(0, 200, 0, 200)
                    v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 80, 100, 80), -1)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun vibrateTargetEnter() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(30, 100))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(30)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun vibrateTargetExit() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(100, 150))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(100)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun vibratePhaseEndingWarning() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Short tick for phase countdown
                    v.vibrate(VibrationEffect.createOneShot(40, 120))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(40)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun vibrateComplete() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Success pattern: three quick pulses
                    val timings = longArrayOf(0, 100, 80, 100, 80, 150)
                    val amplitudes = intArrayOf(0, 180, 0, 180, 0, 255)
                    v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 100, 80, 100, 80, 150), -1)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    /**
     * Pause current drill.
     */
    fun pauseDrill() {
        drillEngine.pause()
    }

    /**
     * Resume current drill.
     */
    fun resumeDrill() {
        drillEngine.resume()
    }

    /**
     * Stop current drill.
     */
    fun stopDrill() {
        // Get current state before stopping for result creation
        val currentState = _drillState.value
        drillEngine.stop()

        // If no result was set by onComplete, create one from current state
        if (_drillResult.value == null && currentState != null) {
            _drillResult.value = io.github.kpedal.drill.model.DrillResult(
                drillId = currentState.drill.id,
                drillName = currentState.drill.name,
                timestamp = System.currentTimeMillis(),
                durationMs = currentState.elapsedMs,
                score = currentState.score,
                timeInTargetMs = currentState.targetHoldMs,
                timeInTargetPercent = currentState.score,
                completed = false,
                phaseScores = emptyList()
            )
        }
    }

    /**
     * Clear drill result (after viewing).
     */
    fun clearDrillResult() {
        _drillResult.value = null
        _drillState.value = null
    }

    /**
     * Check for drill-related achievements after completing a drill.
     */
    private suspend fun checkDrillAchievements(score: Float) {
        try {
            val newAchievements = achievementChecker.checkAfterDrill(score)
            if (newAchievements.isNotEmpty()) {
                android.util.Log.i(TAG, "Unlocked ${newAchievements.size} drill achievements: ${newAchievements.map { it.name }}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to check drill achievements: ${e.message}")
        }
    }

    // ========== Ride History ==========

    /**
     * All saved rides (reactive).
     */
    val rides: StateFlow<List<RideEntity>> = rideRepository.ridesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Dashboard data for HomeScreen (reactive).
     * Combines total rides, average balance, last ride, and streak.
     */
    val dashboardData: StateFlow<DashboardData> = combine(
        rideRepository.totalRideCountFlow,
        rideRepository.averageBalanceFlow,
        rideRepository.lastRideFlow,
        rideRepository.ridesFlow
    ) { totalRides, avgBalance, lastRide, allRides ->
        DashboardData(
            totalRides = totalRides,
            avgBalance = avgBalance ?: 50f,
            currentStreak = StreakCalculator.calculateStreak(allRides),
            lastRide = lastRide
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardData()
    )

    // ========== Analytics ==========

    private val _trendData = MutableStateFlow(TrendData())
    val trendData: StateFlow<TrendData> = _trendData.asStateFlow()

    /**
     * Load trend data for a period.
     */
    fun loadTrendData(period: TrendData.Period) {
        viewModelScope.launch {
            val data = analyticsRepository.getTrendData(period)
            _trendData.value = data
        }
    }

    // ========== Onboarding ==========

    /**
     * Whether user has seen onboarding (reactive).
     */
    val hasSeenOnboarding: StateFlow<Boolean> = preferencesRepository.hasSeenOnboardingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // Default to true to avoid flash
        )

    /**
     * Mark onboarding as complete.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.markOnboardingSeen()
        }
    }

    // ========== Auth & Sync Methods ==========

    // Track active device auth job
    private var deviceAuthJob: kotlinx.coroutines.Job? = null

    /**
     * Start device code authentication flow.
     * Requests device code and starts polling for authorization.
     * After successful auth, syncs any pending rides.
     */
    fun startDeviceAuth() {
        deviceAuthJob?.cancel()
        deviceAuthJob = viewModelScope.launch {
            try {
                // Request device code
                val codeData = deviceAuthService.startAuthFlow()
                if (codeData != null) {
                    // Start polling for token
                    val tokenData = deviceAuthService.startPolling()

                    // If auth was successful, sync pending rides immediately
                    if (tokenData != null) {
                        android.util.Log.i(TAG, "Auth successful, syncing pending rides...")
                        val synced = syncService?.syncPendingRides() ?: 0
                        android.util.Log.i(TAG, "Post-auth sync: $synced rides synced")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e(TAG, "Device auth error: ${e.message}")
            }
        }
    }

    /**
     * Cancel the current device auth flow.
     */
    fun cancelDeviceAuth() {
        deviceAuthJob?.cancel()
        deviceAuthJob = null
        deviceAuthService.cancel()
    }

    /**
     * Sign out.
     */
    fun signOut() {
        // Cancel any ongoing device auth flow
        cancelDeviceAuth()

        viewModelScope.launch {
            try {
                val refreshToken = authRepository.getRefreshToken()
                if (refreshToken != null) {
                    // Try to revoke on server (best effort)
                    try {
                        ApiClient.authService.logout(LogoutRequest(refreshToken))
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Server logout failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Logout error: ${e.message}")
            } finally {
                authRepository.clearAuth()
                android.util.Log.i(TAG, "Signed out")
            }
        }
    }

    /**
     * Trigger manual sync of all pending rides.
     */
    fun triggerManualSync() {
        viewModelScope.launch {
            val synced = syncService?.syncPendingRides() ?: 0
            android.util.Log.i(TAG, "Manual sync completed: $synced rides synced")
        }
    }

    /**
     * Trigger full sync: upload local settings, sync rides, fetch cloud settings.
     */
    fun triggerFullSync() {
        viewModelScope.launch {
            syncService?.fullSync()
            android.util.Log.i(TAG, "Full sync completed")
        }
    }

    /**
     * Check for pending sync request from web dashboard.
     * Call this when Settings is opened.
     */
    fun checkForSyncRequest() {
        viewModelScope.launch {
            val triggered = syncService?.checkAndHandleSyncRequest() ?: false
            if (triggered) {
                android.util.Log.i(TAG, "Sync triggered by web request")
            }
        }
    }

    /**
     * Clear the device revoked flag after user acknowledges.
     */
    fun clearDeviceRevokedFlag() {
        syncService?.clearDeviceRevokedFlag()
    }

    /**
     * Update background mode enabled.
     */
    fun updateBackgroundModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateBackgroundModeEnabled(enabled)
        }
    }

    /**
     * Update auto-sync enabled.
     */
    fun updateAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoSyncEnabled(enabled)
        }
    }

    // ========== Achievements ==========

    /**
     * All unlocked achievements (reactive).
     */
    val unlockedAchievements: StateFlow<List<UnlockedAchievement>> = achievementRepository.unlockedAchievementsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Count of unlocked achievements (reactive).
     */
    val unlockedAchievementCount: StateFlow<Int> = achievementRepository.unlockedCountFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // ========== Weekly Challenges ==========

    /**
     * Current challenge progress (reactive).
     * Calculated from rides and drills this week.
     */
    val challengeProgress: StateFlow<ChallengeProgress?> = combine(
        rideRepository.ridesFlow,
        drillRepository.resultsFlow,
        preferencesRepository.settingsFlow
    ) { rides, drills, currentSettings ->
        calculateChallengeProgress(rides, drills, currentSettings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private fun calculateChallengeProgress(
        rides: List<RideEntity>,
        drills: List<DrillResult>,
        settings: PreferencesRepository.Settings
    ): ChallengeProgress {
        val challenge = WeeklyChallenges.getCurrentChallenge()
        val weekStart = WeeklyChallenges.getWeekStartTimestamp()

        // Filter to this week only
        val weekRides = rides.filter { it.timestamp >= weekStart }
        val weekDrills = drills.filter { it.timestamp >= weekStart }

        // Calculate optimal ranges from settings
        val balanceMin = 50 - settings.balanceThreshold
        val balanceMax = 50 + settings.balanceThreshold
        val teMin = settings.teOptimalMin
        val teMax = settings.teOptimalMax
        val psMin = settings.psMinimum

        val progress = when (challenge.type) {
            WeeklyChallenges.ChallengeType.RIDES -> weekRides.size
            WeeklyChallenges.ChallengeType.DRILLS -> weekDrills.size
            WeeklyChallenges.ChallengeType.BALANCE -> {
                // Count rides with good balance (using settings)
                weekRides.count { ride ->
                    ride.balanceRight in balanceMin..balanceMax
                }
            }
            WeeklyChallenges.ChallengeType.ZONE -> {
                // Average zone optimal across rides
                if (weekRides.isNotEmpty()) {
                    weekRides.map { it.zoneOptimal }.average().toInt()
                } else 0
            }
            WeeklyChallenges.ChallengeType.STREAK -> {
                StreakCalculator.calculateStreak(rides)
            }
            WeeklyChallenges.ChallengeType.TE -> {
                // Count rides with good TE (using settings)
                weekRides.count { ride ->
                    val te = (ride.teLeft + ride.teRight) / 2
                    te in teMin..teMax
                }
            }
            WeeklyChallenges.ChallengeType.PS -> {
                // Count rides with good PS (using settings)
                weekRides.count { ride ->
                    val ps = (ride.psLeft + ride.psRight) / 2
                    ps >= psMin
                }
            }
        }

        return ChallengeProgress(
            challenge = challenge,
            currentProgress = progress,
            target = challenge.target,
            weekStart = weekStart
        )
    }

    // ========== Pedal Info ==========

    /**
     * Current pedal connection info (reactive).
     */
    private val _pedalInfo = MutableStateFlow(PedalInfo())
    val pedalInfo: StateFlow<PedalInfo> = _pedalInfo.asStateFlow()

    /**
     * Get a single ride by ID (for detail screen).
     */
    fun getRideFlow(id: Long): Flow<RideEntity?> = flow {
        emit(rideRepository.getRide(id))
    }

    /**
     * Delete a ride.
     */
    fun deleteRide(id: Long) {
        viewModelScope.launch {
            rideRepository.deleteRide(id)
        }
    }

    /**
     * Update ride rating.
     */
    fun updateRideRating(id: Long, rating: Int) {
        viewModelScope.launch {
            rideRepository.updateRating(id, rating)
        }
    }

    /**
     * Manually save current ride data.
     * @return true if save was initiated, false if no data
     */
    fun manualSaveRide(): Boolean {
        return KPedalExtension.instance?.rideStateMonitor?.manualSave() ?: false
    }
}
