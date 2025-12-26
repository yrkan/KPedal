package io.github.kpedal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.github.kpedal.data.AchievementRepository
import io.github.kpedal.data.AuthRepository
import io.github.kpedal.data.PreferencesRepository
import io.github.kpedal.data.RideRepository
import io.github.kpedal.data.SyncService
import io.github.kpedal.data.database.KPedalDatabase
import io.github.kpedal.datatypes.*
import io.github.kpedal.drill.DrillRepository
import io.github.kpedal.engine.AchievementChecker
import io.github.kpedal.engine.AlertManager
import io.github.kpedal.engine.PedalingEngine
import io.github.kpedal.engine.PedalMonitor
import io.github.kpedal.engine.RideStateMonitor
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KPedalExtension : KarooExtension("kpedal", BuildConfig.VERSION_NAME) {

    companion object {
        private const val TAG = "KPedalExtension"
        private const val NOTIFICATION_CHANNEL_ID = "kpedal_background"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var instance: KPedalExtension? = null
            private set
    }

    lateinit var karooSystem: KarooSystemService
        private set

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Initialize eagerly to avoid lazy initialization issues
    private var _pedalingEngine: PedalingEngine? = null
    val pedalingEngine: PedalingEngine
        get() = _pedalingEngine ?: throw IllegalStateException("PedalingEngine not initialized")

    // Preferences
    private var _preferencesRepository: PreferencesRepository? = null
    val preferencesRepository: PreferencesRepository
        get() = _preferencesRepository ?: throw IllegalStateException("PreferencesRepository not initialized")

    // Ride history persistence
    private var _rideRepository: RideRepository? = null
    val rideRepository: RideRepository
        get() = _rideRepository ?: throw IllegalStateException("RideRepository not initialized")

    // Drill repository
    private var _drillRepository: DrillRepository? = null
    val drillRepository: DrillRepository
        get() = _drillRepository ?: throw IllegalStateException("DrillRepository not initialized")

    // Achievement repository
    private var _achievementRepository: AchievementRepository? = null
    val achievementRepository: AchievementRepository
        get() = _achievementRepository ?: throw IllegalStateException("AchievementRepository not initialized")

    // Achievement checker
    private var _achievementChecker: AchievementChecker? = null
    val achievementChecker: AchievementChecker
        get() = _achievementChecker ?: throw IllegalStateException("AchievementChecker not initialized")

    private var _rideStateMonitor: RideStateMonitor? = null
    val rideStateMonitor: RideStateMonitor
        get() = _rideStateMonitor ?: throw IllegalStateException("RideStateMonitor not initialized")

    // Alert management
    private var _alertManager: AlertManager? = null
    val alertManager: AlertManager
        get() = _alertManager ?: throw IllegalStateException("AlertManager not initialized")

    // Pedal monitoring
    private var _pedalMonitor: PedalMonitor? = null
    val pedalMonitor: PedalMonitor
        get() = _pedalMonitor ?: throw IllegalStateException("PedalMonitor not initialized")

    // Auth repository
    private var _authRepository: AuthRepository? = null
    val authRepository: AuthRepository
        get() = _authRepository ?: throw IllegalStateException("AuthRepository not initialized")

    // Sync service for cloud sync
    private var _syncService: SyncService? = null
    val syncService: SyncService
        get() = _syncService ?: throw IllegalStateException("SyncService not initialized")

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Start as foreground service to ensure background data collection
        startForegroundService()

        karooSystem = KarooSystemService(this)
        _preferencesRepository = PreferencesRepository(this)
        _pedalingEngine = PedalingEngine(this)
        _rideRepository = RideRepository(this)
        _drillRepository = DrillRepository(this)
        _achievementRepository = AchievementRepository(this)
        _authRepository = AuthRepository(this)
        val database = KPedalDatabase.getInstance(this)
        _syncService = SyncService(
            context = this,
            authRepository = authRepository,
            rideDao = database.rideDao(),
            drillResultDao = database.drillResultDao(),
            achievementDao = database.achievementDao(),
            preferencesRepository = preferencesRepository
        )
        _achievementChecker = AchievementChecker(
            achievementRepository = achievementRepository,
            rideRepository = rideRepository,
            drillRepository = drillRepository,
            syncService = syncService
        )
        _rideStateMonitor = RideStateMonitor(
            extension = this,
            rideRepository = rideRepository,
            liveDataCollector = pedalingEngine.liveDataCollector,
            achievementChecker = achievementChecker,
            syncService = syncService
        )
        _alertManager = AlertManager(
            extension = this,
            pedalingEngine = pedalingEngine,
            preferencesRepository = preferencesRepository
        )
        _pedalMonitor = PedalMonitor(pedalingEngine)

        karooSystem.connect { connected ->
            _isConnected.value = connected
            if (connected) {
                android.util.Log.i(TAG, "KarooSystemService connected")
                // Only start RideStateMonitor - it will start/stop other components
                // based on ride state to minimize resource usage
                _rideStateMonitor?.startMonitoring()

                // Send heartbeat to update device status on server
                serviceScope.launch(Dispatchers.IO) {
                    sendHeartbeat()
                }
            } else {
                android.util.Log.w(TAG, "KarooSystemService disconnected")
                _pedalingEngine?.stopStreaming()
                _rideStateMonitor?.stopMonitoring()
                _alertManager?.stopMonitoring()
                _pedalMonitor?.stopMonitoring()
            }
        }
    }

    /**
     * Send heartbeat to server to update device status.
     * Uses the check-request endpoint which also updates last_sync.
     */
    private suspend fun sendHeartbeat() {
        try {
            val sync = _syncService ?: return
            val triggered = sync.checkAndHandleSyncRequest()
            android.util.Log.i(TAG, "Heartbeat sent, sync triggered: $triggered")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to send heartbeat: ${e.message}")
        }
    }

    /**
     * Start the service in foreground mode with a persistent notification.
     * This ensures the service keeps running to collect data for all rides.
     */
    private fun startForegroundService() {
        try {
            createNotificationChannel()
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            android.util.Log.i(TAG, "Started foreground service for background data collection")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to start foreground service: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_kpedal)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        android.util.Log.i(TAG, "onDestroy called")

        // Clear instance first to prevent new observers
        instance = null
        _isConnected.value = false

        // Cleanup PedalMonitor
        _pedalMonitor?.destroy()
        _pedalMonitor = null

        // Cleanup AlertManager
        _alertManager?.destroy()
        _alertManager = null

        // Cleanup RideStateMonitor
        _rideStateMonitor?.destroy()
        _rideStateMonitor = null

        // Cleanup PedalingEngine
        _pedalingEngine?.destroy()
        _pedalingEngine = null

        // Cleanup SyncService
        _syncService?.destroy()
        _syncService = null
        _authRepository = null
        _achievementChecker = null
        _achievementRepository = null
        _drillRepository = null
        _rideRepository = null
        _preferencesRepository = null

        // Cancel service scope
        serviceScope.cancel()

        // Disconnect from Karoo
        try {
            karooSystem.disconnect()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Error disconnecting KarooSystem: ${e.message}")
        }

        super.onDestroy()
    }

    override val types by lazy {
        listOf(
            QuickGlanceDataType(this),
            PowerBalanceDataType(this),
            EfficiencyDataType(this),
            FullOverviewDataType(this),
            BalanceTrendDataType(this),
            SingleBalanceDataType(this),
            LiveDataType(this)
        )
    }
}
