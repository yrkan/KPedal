package io.github.kpedal.engine

import io.github.kpedal.KPedalExtension
import io.github.kpedal.data.models.PedalInfo
import io.github.kpedal.data.models.SensorInfo
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.SavedDevices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * Monitors pedal connection status and signal quality.
 * Tracks update frequency to determine signal quality.
 * Also monitors SavedDevices to show sensor capabilities.
 */
class PedalMonitor(
    private val extension: KPedalExtension
) {
    companion object {
        private const val TAG = "CustomAnt"
        private const val UPDATE_INTERVAL_MS = 1000L // Check every second
        private const val STALE_THRESHOLD_MS = 5000L // Consider stale after 5 seconds

        // Known data type IDs for cycling dynamics
        private val POWER_BALANCE_TYPES = setOf(
            DataType.Type.PEDAL_POWER_BALANCE,
            "PEDAL_POWER_BALANCE",
            "pedal_power_balance"
        )
        private val TORQUE_EFFECTIVENESS_TYPES = setOf(
            DataType.Type.TORQUE_EFFECTIVENESS,
            "TORQUE_EFFECTIVENESS",
            "torque_effectiveness"
        )
        private val PEDAL_SMOOTHNESS_TYPES = setOf(
            DataType.Type.PEDAL_SMOOTHNESS,
            "PEDAL_SMOOTHNESS",
            "pedal_smoothness"
        )
        private val POWER_TYPES = setOf(
            DataType.Type.POWER,
            "POWER",
            "power"
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var monitoringJob: kotlinx.coroutines.Job? = null
    private val savedDevicesConsumerId = AtomicReference<String?>(null)

    private val _pedalInfo = MutableStateFlow(PedalInfo())
    val pedalInfo: StateFlow<PedalInfo> = _pedalInfo.asStateFlow()

    // Track update times for frequency calculation
    private var lastUpdateCount = 0
    private var updateCount = 0
    private var lastCheckTime = System.currentTimeMillis()
    private var lastDataTime = 0L

    // Cached sensor state from SDK (thread-safe via volatile)
    @Volatile
    private var cachedSensorState: SensorStreamState = SensorStreamState.Idle

    // Cached received data types for identifying active sensor
    @Volatile
    private var cachedReceivedDataTypes: Set<ReceivedDataType> = emptySet()

    // Cached sensor info from SavedDevices (thread-safe with lock)
    private val sensorLock = Any()
    private var cachedSensors: List<SensorInfo> = emptyList()
    private var cachedHasPowerMeter = false
    private var cachedHasCyclingDynamics = false
    private var cachedPowerMeterName = "Power Meter Pedals"

    /**
     * Start monitoring pedal connection.
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        // Subscribe to SavedDevices to get sensor info
        subscribeSavedDevices()

        // Ensure PedalingEngine is streaming so we get real sensor states
        // (safe to call even if already streaming - it's idempotent)
        extension.pedalingEngine.startStreaming()

        monitoringJob = scope.launch {
            // Observe SDK sensorState for connection status
            launch {
                extension.pedalingEngine.sensorState.collect { state ->
                    android.util.Log.i(TAG, "KPedal: sensorState → $state")
                    cachedSensorState = state
                    // Trigger immediate update on state change
                    updatePedalInfo()
                }
            }

            // Observe received data types to identify active sensor
            launch {
                extension.pedalingEngine.receivedDataTypes.collect { dataTypes ->
                    if (cachedReceivedDataTypes != dataTypes) {
                        cachedReceivedDataTypes = dataTypes
                        android.util.Log.i(TAG, "KPedal: receivedTypes → $dataTypes")
                        updatePedalInfo()
                    }
                }
            }

            // Observe metrics updates to track data freshness
            launch {
                extension.pedalingEngine.metrics.collect { metrics ->
                    // Only count as update if we have any pedal data
                    if (metrics.balance != 50f || metrics.torqueEffLeft > 0 || metrics.pedalSmoothLeft > 0) {
                        updateCount++
                        lastDataTime = System.currentTimeMillis()
                    }
                }
            }

            // Periodically calculate update frequency and emit status
            while (isActive) {
                delay(UPDATE_INTERVAL_MS)
                updatePedalInfo()
            }
        }
        android.util.Log.i(TAG, "KPedal: Started monitoring")
    }

    /**
     * Subscribe to SavedDevices to monitor connected sensors.
     */
    private fun subscribeSavedDevices() {
        if (!extension.karooSystem.connected) {
            android.util.Log.w(TAG, "KPedal: KarooSystem not connected")
            return
        }

        try {
            val consumerId = extension.karooSystem.addConsumer { event: SavedDevices ->
                processSavedDevices(event.devices)
            }
            savedDevicesConsumerId.set(consumerId)
            android.util.Log.i(TAG, "KPedal: Subscribed to SavedDevices")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "KPedal: SavedDevices subscribe failed: ${e.message}")
        }
    }

    /**
     * Process saved devices and extract sensor information.
     */
    private fun processSavedDevices(devices: List<SavedDevices.SavedDevice>) {
        android.util.Log.i(TAG, "KPedal: Processing ${devices.size} saved devices")

        val sensors = mutableListOf<SensorInfo>()
        var foundPowerMeter = false
        var foundCyclingDynamics = false
        var powerMeterName = "Power Meter Pedals"

        for (device in devices) {
            if (!device.enabled) continue

            val supportedTypes = device.supportedDataTypes
            android.util.Log.d(TAG, "KPedal: Device ${device.name} (${device.connectionType}): " +
                    "enabled=${device.enabled}, types=${supportedTypes.take(5)}...")

            val hasPowerBalance = supportedTypes.any { it in POWER_BALANCE_TYPES }
            val hasTorqueEffectiveness = supportedTypes.any { it in TORQUE_EFFECTIVENESS_TYPES }
            val hasPedalSmoothness = supportedTypes.any { it in PEDAL_SMOOTHNESS_TYPES }
            val hasPower = supportedTypes.any { it in POWER_TYPES }

            if (hasPower || hasPowerBalance) {
                foundPowerMeter = true
                powerMeterName = device.name

                if (hasTorqueEffectiveness || hasPedalSmoothness) {
                    foundCyclingDynamics = true
                    android.util.Log.i(TAG, "KPedal: ✓ Cycling Dynamics: ${device.name}")
                } else {
                    android.util.Log.i(TAG, "KPedal: ✓ Power Meter (no CD): ${device.name}")
                }

                sensors.add(
                    SensorInfo(
                        id = device.id,
                        name = device.name,
                        connectionType = device.connectionType,
                        enabled = device.enabled,
                        supportedDataTypes = supportedTypes,
                        hasPowerBalance = hasPowerBalance,
                        hasTorqueEffectiveness = hasTorqueEffectiveness,
                        hasPedalSmoothness = hasPedalSmoothness
                    )
                )
            }
        }

        // Log warnings if no suitable sensors found
        if (!foundPowerMeter) {
            android.util.Log.w(TAG, "KPedal: ⚠ No power meter found!")
        } else if (!foundCyclingDynamics) {
            android.util.Log.w(TAG, "KPedal: ⚠ No Cycling Dynamics - enable Assioma IAV Dynamic mode")
        }

        // Cache the results (thread-safe)
        synchronized(sensorLock) {
            cachedSensors = sensors.toList() // Defensive copy
            cachedHasPowerMeter = foundPowerMeter
            cachedHasCyclingDynamics = foundCyclingDynamics
            cachedPowerMeterName = powerMeterName
        }

        // Trigger an immediate update
        updatePedalInfo()
    }

    /**
     * Stop monitoring.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null

        savedDevicesConsumerId.getAndSet(null)?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "KPedal: Failed to remove consumer: ${e.message}")
            }
        }

        android.util.Log.i(TAG, "KPedal: Stopped monitoring")
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        stopMonitoring()
        scope.cancel()
    }

    private fun updatePedalInfo() {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastCheckTime) / 1000f

        // Calculate updates per second
        val newUpdates = updateCount - lastUpdateCount
        val updateFrequency = if (elapsedSeconds > 0) newUpdates / elapsedSeconds else 0f

        // Determine connection status from SDK sensorState (authoritative source)
        val sensorState = cachedSensorState
        val isConnected = sensorState is SensorStreamState.Streaming

        // Determine signal quality based on update frequency
        val signalQuality = when {
            !isConnected -> PedalInfo.SignalQuality.UNKNOWN
            updateFrequency >= 1f -> PedalInfo.SignalQuality.GOOD
            updateFrequency >= 0.5f -> PedalInfo.SignalQuality.FAIR
            else -> PedalInfo.SignalQuality.POOR
        }

        // Read cached sensor info (thread-safe)
        val (sensors, hasPowerMeter, hasCyclingDynamics, defaultPowerMeterName) = synchronized(sensorLock) {
            SensorCacheSnapshot(cachedSensors, cachedHasPowerMeter, cachedHasCyclingDynamics, cachedPowerMeterName)
        }

        // Identify active sensor by matching received data types to sensor capabilities
        // Priority: Cycling Dynamics (TE/PS) > Power Balance > Power only
        val receivedTypes = cachedReceivedDataTypes
        val activeSensor = if (isConnected && sensors.isNotEmpty()) {
            when {
                // If receiving TE or PS, the active sensor must support Cycling Dynamics
                ReceivedDataType.TORQUE_EFFECTIVENESS in receivedTypes ||
                ReceivedDataType.PEDAL_SMOOTHNESS in receivedTypes -> {
                    sensors.find { it.hasTorqueEffectiveness || it.hasPedalSmoothness }
                }
                // If receiving Balance, the active sensor must support power balance
                ReceivedDataType.PEDAL_POWER_BALANCE in receivedTypes -> {
                    sensors.find { it.hasPowerBalance }
                }
                // If only receiving Power, could be any power sensor
                ReceivedDataType.POWER in receivedTypes -> {
                    // Prefer sensor with Cycling Dynamics, otherwise first one
                    sensors.find { it.hasCyclingDynamics } ?: sensors.firstOrNull()
                }
                else -> null
            }
        } else null
        val activeDeviceId = activeSensor?.id
        val activeSensorName = activeSensor?.name ?: defaultPowerMeterName

        val newPedalInfo = PedalInfo(
            isConnected = isConnected,
            sensorState = sensorState,
            signalQuality = signalQuality,
            lastDataReceivedMs = lastDataTime,
            updateFrequency = updateFrequency,
            modelName = activeSensorName,
            sensors = sensors,
            hasPowerMeter = hasPowerMeter,
            hasCyclingDynamics = hasCyclingDynamics,
            activeDeviceId = activeDeviceId
        )
        android.util.Log.d(TAG, "KPedal: PedalInfo → connected=$isConnected, state=$sensorState, active=$activeSensorName")
        _pedalInfo.value = newPedalInfo

        // Reset counters for next interval
        lastUpdateCount = updateCount
        lastCheckTime = now
    }

    private data class SensorCacheSnapshot(
        val sensors: List<SensorInfo>,
        val hasPowerMeter: Boolean,
        val hasCyclingDynamics: Boolean,
        val powerMeterName: String
    )
}
