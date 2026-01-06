package io.github.kpedal.engine

import io.github.kpedal.KPedalExtension
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * State of sensor data streaming.
 */
sealed class SensorStreamState {
    /** Initial state, not yet started */
    object Idle : SensorStreamState()
    /** Searching for sensor */
    object Searching : SensorStreamState()
    /** Actively receiving data */
    object Streaming : SensorStreamState()
    /** Sensor not available (not connected or doesn't support data type) */
    data class NotAvailable(val reason: String) : SensorStreamState()
    /** Connection lost (was streaming, now stale) */
    object Disconnected : SensorStreamState()
}

/**
 * Types of data we can receive from sensors.
 * Used to identify which sensor is active based on its capabilities.
 */
enum class ReceivedDataType {
    POWER,              // Basic power (watts)
    PEDAL_POWER_BALANCE, // L/R balance (requires dual-sided power meter)
    TORQUE_EFFECTIVENESS, // TE (requires Cycling Dynamics)
    PEDAL_SMOOTHNESS     // PS (requires Cycling Dynamics)
}

/**
 * Engine for collecting and processing pedaling metrics from Karoo streams.
 *
 * Subscribes to:
 * - PEDAL_POWER_BALANCE (L/R balance)
 * - TORQUE_EFFECTIVENESS (L/R)
 * - PEDAL_SMOOTHNESS (L/R)
 * - POWER (current watts)
 * - CADENCE (current RPM)
 * - HEART_RATE (BPM)
 * - SPEED (m/s)
 * - DISTANCE (meters)
 * - ALTITUDE (meters)
 * - SMOOTHED_3S_AVERAGE_PEDAL_POWER_BALANCE
 * - SMOOTHED_10S_AVERAGE_PEDAL_POWER_BALANCE
 */
@OptIn(FlowPreview::class)
class PedalingEngine(private val extension: KPedalExtension) {

    companion object {
        private const val TAG = "CustomAnt"
        private const val DEBOUNCE_MS = 50L
        private const val DISCONNECT_CHECK_INTERVAL_MS = 3000L
        private const val DISCONNECT_THRESHOLD_MS = 15000L // Consider disconnected after 15s of no data
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _metrics = MutableStateFlow(PedalingMetrics())
    val metrics: StateFlow<PedalingMetrics> = _metrics.asStateFlow()

    // Sensor streaming state for UI feedback
    private val _sensorState = MutableStateFlow<SensorStreamState>(SensorStreamState.Idle)
    val sensorState: StateFlow<SensorStreamState> = _sensorState.asStateFlow()

    // Track last data receive time for disconnect detection
    @Volatile
    private var lastDataReceivedMs: Long = 0L

    // Track stream states for different data types
    @Volatile
    private var powerStreamState: StreamState = StreamState.Idle
    @Volatile
    private var balanceStreamState: StreamState = StreamState.Idle

    // Track which data types we're actually receiving (for identifying active sensor)
    private val _receivedDataTypes = MutableStateFlow<Set<ReceivedDataType>>(emptySet())
    val receivedDataTypes: StateFlow<Set<ReceivedDataType>> = _receivedDataTypes.asStateFlow()

    private fun markDataTypeReceived(type: ReceivedDataType) {
        val current = _receivedDataTypes.value
        if (type !in current) {
            _receivedDataTypes.value = current + type
            android.util.Log.i(TAG, "KPedal: Now receiving → $type (all: ${_receivedDataTypes.value})")
        }
    }

    private fun clearReceivedDataTypes() {
        if (_receivedDataTypes.value.isNotEmpty()) {
            _receivedDataTypes.value = emptySet()
            android.util.Log.i(TAG, "KPedal: Cleared received data types")
        }
    }

    // Live data collector for ride statistics
    val liveDataCollector = LiveDataCollector()

    // Trigger for debounced updates
    private val updateTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Streaming state to prevent duplicate consumers
    @Volatile
    private var isStreaming = false

    // Consumer IDs for cleanup
    private val balanceConsumerId = AtomicReference<String?>(null)
    private val teConsumerId = AtomicReference<String?>(null)
    private val psConsumerId = AtomicReference<String?>(null)
    private val powerConsumerId = AtomicReference<String?>(null)
    private val cadenceConsumerId = AtomicReference<String?>(null)
    private val heartRateConsumerId = AtomicReference<String?>(null)
    private val speedConsumerId = AtomicReference<String?>(null)
    private val distanceConsumerId = AtomicReference<String?>(null)
    private val balance3sConsumerId = AtomicReference<String?>(null)
    private val balance10sConsumerId = AtomicReference<String?>(null)
    // Pro cyclist metrics consumers
    private val elevationGainConsumerId = AtomicReference<String?>(null)
    private val elevationLossConsumerId = AtomicReference<String?>(null)
    private val gradeConsumerId = AtomicReference<String?>(null)
    private val normalizedPowerConsumerId = AtomicReference<String?>(null)
    private val energyConsumerId = AtomicReference<String?>(null)

    // Current values (volatile for thread safety)
    @Volatile private var currentBalanceLeft: Float = 50f
    @Volatile private var currentTeLeft: Float = 0f
    @Volatile private var currentTeRight: Float = 0f
    @Volatile private var currentPsLeft: Float = 0f
    @Volatile private var currentPsRight: Float = 0f
    @Volatile private var currentPower: Int = 0
    @Volatile private var currentCadence: Int = 0
    @Volatile private var currentHeartRate: Int = 0
    @Volatile private var currentSpeed: Float = 0f
    @Volatile private var currentDistance: Float = 0f
    @Volatile private var currentBalance3sLeft: Float = 50f
    @Volatile private var currentBalance10sLeft: Float = 50f

    // Track if we've received real pedaling data from sensors
    // (balance alone is not enough - need TE or PS to confirm pedal data)
    @Volatile private var hasReceivedPedalData: Boolean = false
    // Pro cyclist current values
    @Volatile private var currentElevationGain: Float = 0f
    @Volatile private var currentElevationLoss: Float = 0f
    @Volatile private var currentGrade: Float = 0f
    @Volatile private var currentNormalizedPower: Int = 0
    @Volatile private var currentEnergy: Float = 0f

    init {
        // Debounced metrics updates to prevent UI overload
        scope.launch {
            updateTrigger
                .debounce(DEBOUNCE_MS)
                .collect {
                    emitMetrics()
                }
        }

        // Periodic disconnect detection
        scope.launch {
            while (isActive) {
                delay(DISCONNECT_CHECK_INTERVAL_MS)
                checkForDisconnect()
            }
        }
    }

    /**
     * Update combined sensor state based on POWER and PEDAL_POWER_BALANCE streams.
     * Priority: Streaming > Searching > NotAvailable > Idle
     */
    private fun updateCombinedSensorState() {
        val power = powerStreamState
        val balance = balanceStreamState

        val newState = when {
            // If either is streaming, we're connected
            balance is StreamState.Streaming -> SensorStreamState.Streaming
            power is StreamState.Streaming -> SensorStreamState.Streaming
            // If either is searching, we're searching
            balance is StreamState.Searching -> SensorStreamState.Searching
            power is StreamState.Searching -> SensorStreamState.Searching
            // If balance is not available but power is idle, power meter may not support cycling dynamics
            balance is StreamState.NotAvailable && power is StreamState.Idle ->
                SensorStreamState.NotAvailable("Power meter may not support Cycling Dynamics")
            // If both are not available
            balance is StreamState.NotAvailable || power is StreamState.NotAvailable ->
                SensorStreamState.NotAvailable("Power meter not connected")
            // Default to Idle
            else -> SensorStreamState.Idle
        }

        if (_sensorState.value != newState) {
            _sensorState.value = newState
            android.util.Log.i(TAG, "KPedal: State → $newState (power=$power, balance=$balance)")
        }
    }

    /**
     * Check if sensor data has stopped coming and update state accordingly.
     */
    private fun checkForDisconnect() {
        if (!isStreaming) return

        val currentState = _sensorState.value
        val now = System.currentTimeMillis()
        val timeSinceLastData = now - lastDataReceivedMs

        // Only transition to Disconnected if we were previously Streaming
        if (currentState is SensorStreamState.Streaming &&
            lastDataReceivedMs > 0 &&
            timeSinceLastData > DISCONNECT_THRESHOLD_MS
        ) {
            _sensorState.value = SensorStreamState.Disconnected
            android.util.Log.w(TAG, "KPedal: ⚠ Disconnected - no data for ${timeSinceLastData}ms")
        }
    }

    /**
     * Start streaming pedaling data from Karoo sensors.
     */
    fun startStreaming() {
        if (isStreaming) {
            android.util.Log.d(TAG, "KPedal: Already streaming, skip")
            return
        }

        if (!extension.karooSystem.connected) {
            android.util.Log.w(TAG, "KPedal: KarooSystem not connected")
            return
        }

        isStreaming = true
        hasReceivedPedalData = false  // Reset on new streaming session

        // Track successfully added consumers for cleanup on error
        val addedConsumers = mutableListOf<AtomicReference<String?>>()

        android.util.Log.i(TAG, "KPedal: Starting sensor streams... karooConnected=${extension.karooSystem.connected}")

        try {
            // Subscribe to Power Balance
            // Returns: Field.PEDAL_POWER_BALANCE_LEFT (left side %)
            balanceConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.PEDAL_POWER_BALANCE)
            ) { event: OnStreamState ->
                val state = event.state
                balanceStreamState = state
                updateCombinedSensorState()

                when (state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.PEDAL_POWER_BALANCE_LEFT]?.toFloat()?.let { left ->
                            currentBalanceLeft = left
                            markDataTypeReceived(ReceivedDataType.PEDAL_POWER_BALANCE)
                            // Balance from SDK = power meter connected (even without TE/PS)
                            hasReceivedPedalData = true
                            lastDataReceivedMs = System.currentTimeMillis()
                            android.util.Log.d(TAG, "KPedal BALANCE: L=${left.toInt()}% R=${(100-left).toInt()}%")
                            triggerUpdate()
                        }
                    }
                    is StreamState.Searching -> {
                        android.util.Log.i(TAG, "KPedal BALANCE: 🔍 Searching...")
                    }
                    is StreamState.NotAvailable -> {
                        android.util.Log.w(TAG, "KPedal BALANCE: ❌ Not available")
                    }
                    is StreamState.Idle -> {
                        android.util.Log.d(TAG, "KPedal BALANCE: ⏸ Idle")
                    }
                }
            })
            addedConsumers.add(balanceConsumerId)

            // Subscribe to Torque Effectiveness
            // Returns: Field.TORQUE_EFFECTIVENESS_LEFT, Field.TORQUE_EFFECTIVENESS_RIGHT
            teConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.TORQUE_EFFECTIVENESS)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        val left = values[DataType.Field.TORQUE_EFFECTIVENESS_LEFT]?.toFloat()
                        val right = values[DataType.Field.TORQUE_EFFECTIVENESS_RIGHT]?.toFloat()
                        left?.let {
                            currentTeLeft = it
                            if (it > 0) {
                                hasReceivedPedalData = true
                                markDataTypeReceived(ReceivedDataType.TORQUE_EFFECTIVENESS)
                            }
                        }
                        right?.let {
                            currentTeRight = it
                            if (it > 0) {
                                hasReceivedPedalData = true
                                markDataTypeReceived(ReceivedDataType.TORQUE_EFFECTIVENESS)
                            }
                        }
                        android.util.Log.d(TAG, "KPedal TE: L=${left?.toInt()}% R=${right?.toInt()}%")
                        triggerUpdate()
                    }
                    is StreamState.Searching -> {
                        android.util.Log.i(TAG, "KPedal TE: 🔍 Searching...")
                    }
                    is StreamState.NotAvailable -> {
                        android.util.Log.w(TAG, "KPedal TE: ❌ Not available")
                    }
                    is StreamState.Idle -> {
                        android.util.Log.d(TAG, "KPedal TE: ⏸ Idle")
                    }
                }
            })
            addedConsumers.add(teConsumerId)

            // Subscribe to Pedal Smoothness
            // Returns: Field.PEDAL_SMOOTHNESS_LEFT, Field.PEDAL_SMOOTHNESS_RIGHT
            psConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.PEDAL_SMOOTHNESS)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        val left = values[DataType.Field.PEDAL_SMOOTHNESS_LEFT]?.toFloat()
                        val right = values[DataType.Field.PEDAL_SMOOTHNESS_RIGHT]?.toFloat()
                        left?.let {
                            currentPsLeft = it
                            if (it > 0) {
                                hasReceivedPedalData = true
                                markDataTypeReceived(ReceivedDataType.PEDAL_SMOOTHNESS)
                            }
                        }
                        right?.let {
                            currentPsRight = it
                            if (it > 0) {
                                hasReceivedPedalData = true
                                markDataTypeReceived(ReceivedDataType.PEDAL_SMOOTHNESS)
                            }
                        }
                        android.util.Log.d(TAG, "KPedal PS: L=${left?.toInt()}% R=${right?.toInt()}%")
                        triggerUpdate()
                    }
                    is StreamState.Searching -> {
                        android.util.Log.i(TAG, "KPedal PS: 🔍 Searching...")
                    }
                    is StreamState.NotAvailable -> {
                        android.util.Log.w(TAG, "KPedal PS: ❌ Not available")
                    }
                    is StreamState.Idle -> {
                        android.util.Log.d(TAG, "KPedal PS: ⏸ Idle")
                    }
                }
            })
            addedConsumers.add(psConsumerId)

            // Subscribe to Power
            // Returns: Field.POWER (current watts)
            powerConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.POWER)
            ) { event: OnStreamState ->
                val state = event.state
                powerStreamState = state
                updateCombinedSensorState()

                when (state) {
                    is StreamState.Streaming -> {
                        val dataPoint = state.dataPoint
                        val values = dataPoint.values
                        values[DataType.Field.POWER]?.toInt()?.let { power ->
                            currentPower = power
                            markDataTypeReceived(ReceivedDataType.POWER)
                            // Power data = sensor connected, update timestamp
                            lastDataReceivedMs = System.currentTimeMillis()
                            android.util.Log.d(TAG, "KPedal POWER: ${power}W")
                            triggerUpdate()
                        }
                    }
                    is StreamState.Searching -> {
                        android.util.Log.i(TAG, "KPedal POWER: 🔍 Searching...")
                    }
                    is StreamState.NotAvailable -> {
                        android.util.Log.w(TAG, "KPedal POWER: ❌ Not available")
                    }
                    is StreamState.Idle -> {
                        android.util.Log.d(TAG, "KPedal POWER: ⏸ Idle")
                    }
                }
            })
            addedConsumers.add(powerConsumerId)

            // Subscribe to Cadence
            // Returns: Field.CADENCE (current RPM)
            cadenceConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.CADENCE)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.CADENCE]?.toInt()?.let { cadence ->
                            currentCadence = cadence
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(cadenceConsumerId)

            // Subscribe to Heart Rate
            // Returns: Field.HEART_RATE (current BPM)
            heartRateConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.HEART_RATE)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.HEART_RATE]?.toInt()?.let { hr ->
                            currentHeartRate = hr
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(heartRateConsumerId)

            // Subscribe to Speed
            // Returns: Field.SPEED (m/s)
            speedConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.SPEED)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.SPEED]?.toFloat()?.let { speed ->
                            currentSpeed = speed
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(speedConsumerId)

            // Subscribe to Distance
            // Returns: Field.DISTANCE (meters)
            distanceConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.DISTANCE)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.DISTANCE]?.toFloat()?.let { dist ->
                            currentDistance = dist
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(distanceConsumerId)

            // Subscribe to Elevation Gain
            // Returns: Field.ELEVATION_GAIN (meters)
            elevationGainConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.ELEVATION_GAIN)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.ELEVATION_GAIN]?.toFloat()?.let { gain ->
                            currentElevationGain = gain
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(elevationGainConsumerId)

            // Subscribe to Elevation Loss
            // Returns: Field.ELEVATION_LOSS (meters)
            elevationLossConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.ELEVATION_LOSS)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.ELEVATION_LOSS]?.toFloat()?.let { loss ->
                            currentElevationLoss = loss
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(elevationLossConsumerId)

            // Subscribe to Grade/Gradient
            // Returns: Field.ELEVATION_GRADE (percentage)
            gradeConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.ELEVATION_GRADE)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.ELEVATION_GRADE]?.toFloat()?.let { grade ->
                            currentGrade = grade
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(gradeConsumerId)

            // Subscribe to Normalized Power
            // Returns: Field.NORMALIZED_POWER (watts)
            normalizedPowerConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.NORMALIZED_POWER)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.NORMALIZED_POWER]?.toInt()?.let { np ->
                            currentNormalizedPower = np
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(normalizedPowerConsumerId)

            // Subscribe to Energy Output
            // Returns: Field.ENERGY_OUTPUT (kilojoules)
            energyConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.ENERGY_OUTPUT)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.ENERGY_OUTPUT]?.toFloat()?.let { energy ->
                            currentEnergy = energy
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(energyConsumerId)

            // Subscribe to 3s Smoothed Balance
            // Returns: Field.PEDAL_POWER_BALANCE_LEFT
            balance3sConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.SMOOTHED_3S_AVERAGE_PEDAL_POWER_BALANCE)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.PEDAL_POWER_BALANCE_LEFT]?.toFloat()?.let { left ->
                            currentBalance3sLeft = left
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(balance3sConsumerId)

            // Subscribe to 10s Smoothed Balance
            // Returns: Field.PEDAL_POWER_BALANCE_LEFT
            balance10sConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.SMOOTHED_10S_AVERAGE_PEDAL_POWER_BALANCE)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.PEDAL_POWER_BALANCE_LEFT]?.toFloat()?.let { left ->
                            currentBalance10sLeft = left
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(balance10sConsumerId)

            // Note: LiveDataCollector is started/stopped by RideStateMonitor
            // based on RideState changes (Recording/Idle), not here.

            android.util.Log.i(TAG, "KPedal: ✓ Streaming started")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "KPedal: ❌ Error starting streams: ${e.message}", e)
            isStreaming = false
            // Cleanup already added consumers on error
            addedConsumers.forEach { ref ->
                ref.getAndSet(null)?.let { id ->
                    safeRemoveConsumer(id)
                }
            }
        }
    }

    private fun triggerUpdate() {
        val emitted = updateTrigger.tryEmit(Unit)
        android.util.Log.d(TAG, "KPedal: triggerUpdate() → emitted=$emitted, hasReceivedPedalData=$hasReceivedPedalData")
    }

    /**
     * Stop streaming and cleanup consumers.
     * Note: LiveDataCollector is managed by RideStateMonitor, not here.
     */
    fun stopStreaming() {
        if (!isStreaming) {
            android.util.Log.d(TAG, "KPedal: Not streaming, skip stop")
            return
        }

        isStreaming = false
        _sensorState.value = SensorStreamState.Idle
        lastDataReceivedMs = 0L
        powerStreamState = StreamState.Idle
        balanceStreamState = StreamState.Idle

        balanceConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        teConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        psConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        powerConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        cadenceConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        heartRateConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        speedConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        distanceConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        balance3sConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        balance10sConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        // Pro cyclist metrics
        elevationGainConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        elevationLossConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        gradeConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        normalizedPowerConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        energyConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }

        // Reset pedal data flag and received data types for next ride
        hasReceivedPedalData = false
        clearReceivedDataTypes()

        android.util.Log.i(TAG, "KPedal: Streaming stopped")
    }

    private fun safeRemoveConsumer(id: String) {
        try {
            extension.karooSystem.removeConsumer(id)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "KPedal: Failed to remove consumer: ${e.message}")
        }
    }

    /**
     * Emit current metrics to StateFlow.
     * Called from debounced flow to prevent UI overload.
     *
     * Note: Each metric is captured from independent SDK callbacks, so perfect
     * synchronization across all values is not guaranteed. The debounce ensures
     * UI updates are reasonably coherent.
     */
    private fun emitMetrics() {
        // Capture current volatile values (individually atomic reads)
        val balanceLeft = currentBalanceLeft
        val teLeft = currentTeLeft
        val teRight = currentTeRight
        val psLeft = currentPsLeft
        val psRight = currentPsRight
        val power = currentPower
        val cadence = currentCadence
        val heartRate = currentHeartRate
        val speed = currentSpeed
        val distance = currentDistance
        val balance3sLeft = currentBalance3sLeft
        val balance10sLeft = currentBalance10sLeft
        // Pro cyclist metrics
        val elevationGain = currentElevationGain
        val elevationLoss = currentElevationLoss
        val grade = currentGrade
        val normalizedPower = currentNormalizedPower
        val energy = currentEnergy

        // Balance: SDK returns left side %, right = 100 - left
        // PedalingMetrics.balance stores RIGHT side value for display
        // Only set timestamp if we have real pedal data (TE or PS > 0)
        // This ensures hasData = false until real data arrives
        val newMetrics = PedalingMetrics(
            balance = 100f - balanceLeft,  // Right side %
            torqueEffLeft = teLeft,
            torqueEffRight = teRight,
            pedalSmoothLeft = psLeft,
            pedalSmoothRight = psRight,
            power = power,
            cadence = cadence,
            heartRate = heartRate,
            speed = speed,
            distance = distance,
            balance3s = 100f - balance3sLeft,  // Right side %
            balance10s = 100f - balance10sLeft,  // Right side %
            // Pro cyclist metrics
            elevationGain = elevationGain,
            elevationLoss = elevationLoss,
            grade = grade,
            normalizedPower = normalizedPower,
            energyKj = energy,
            timestamp = if (hasReceivedPedalData) System.currentTimeMillis() else 0L
        )

        _metrics.value = newMetrics

        // Always log emit for debugging
        android.util.Log.d(TAG, "KPedal EMIT: hasData=${newMetrics.hasData} hasPedalData=$hasReceivedPedalData bal=${(100f - balanceLeft).toInt()}% te=${teLeft.toInt()}/${teRight.toInt()} ps=${psLeft.toInt()}/${psRight.toInt()} pwr=${power}W")

        // Update live data collector
        liveDataCollector.updateMetrics(newMetrics)
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        stopStreaming()
        liveDataCollector.destroy()
        scope.cancel()
    }
}
