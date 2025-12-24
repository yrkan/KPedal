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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * Engine for collecting and processing pedaling metrics from Karoo streams.
 *
 * Subscribes to:
 * - PEDAL_POWER_BALANCE (L/R balance)
 * - TORQUE_EFFECTIVENESS (L/R)
 * - PEDAL_SMOOTHNESS (L/R)
 * - POWER (current watts)
 * - CADENCE (current RPM)
 * - SMOOTHED_3S_AVERAGE_PEDAL_POWER_BALANCE
 * - SMOOTHED_10S_AVERAGE_PEDAL_POWER_BALANCE
 */
@OptIn(FlowPreview::class)
class PedalingEngine(private val extension: KPedalExtension) {

    companion object {
        private const val TAG = "PedalingEngine"
        private const val DEBOUNCE_MS = 50L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _metrics = MutableStateFlow(PedalingMetrics())
    val metrics: StateFlow<PedalingMetrics> = _metrics.asStateFlow()

    // Live data collector for ride statistics
    val liveDataCollector = LiveDataCollector()

    // Trigger for debounced updates
    private val updateTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Consumer IDs for cleanup
    private val balanceConsumerId = AtomicReference<String?>(null)
    private val teConsumerId = AtomicReference<String?>(null)
    private val psConsumerId = AtomicReference<String?>(null)
    private val powerConsumerId = AtomicReference<String?>(null)
    private val cadenceConsumerId = AtomicReference<String?>(null)
    private val balance3sConsumerId = AtomicReference<String?>(null)
    private val balance10sConsumerId = AtomicReference<String?>(null)

    // Current values (volatile for thread safety)
    @Volatile private var currentBalanceLeft: Float = 50f
    @Volatile private var currentTeLeft: Float = 0f
    @Volatile private var currentTeRight: Float = 0f
    @Volatile private var currentPsLeft: Float = 0f
    @Volatile private var currentPsRight: Float = 0f
    @Volatile private var currentPower: Int = 0
    @Volatile private var currentCadence: Int = 0
    @Volatile private var currentBalance3sLeft: Float = 50f
    @Volatile private var currentBalance10sLeft: Float = 50f

    init {
        // Debounced metrics updates to prevent UI overload
        scope.launch {
            updateTrigger
                .debounce(DEBOUNCE_MS)
                .collect {
                    emitMetrics()
                }
        }
    }

    /**
     * Start streaming pedaling data from Karoo sensors.
     */
    fun startStreaming() {
        if (!extension.karooSystem.connected) {
            android.util.Log.w(TAG, "KarooSystem not connected")
            return
        }

        // Track successfully added consumers for cleanup on error
        val addedConsumers = mutableListOf<AtomicReference<String?>>()

        try {
            // Subscribe to Power Balance
            // Returns: Field.PEDAL_POWER_BALANCE_LEFT (left side %)
            balanceConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.PEDAL_POWER_BALANCE)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.PEDAL_POWER_BALANCE_LEFT]?.toFloat()?.let { left ->
                            currentBalanceLeft = left
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
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
                        values[DataType.Field.TORQUE_EFFECTIVENESS_LEFT]?.toFloat()?.let { left ->
                            currentTeLeft = left
                        }
                        values[DataType.Field.TORQUE_EFFECTIVENESS_RIGHT]?.toFloat()?.let { right ->
                            currentTeRight = right
                        }
                        triggerUpdate()
                    }
                    else -> { /* Ignore */ }
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
                        values[DataType.Field.PEDAL_SMOOTHNESS_LEFT]?.toFloat()?.let { left ->
                            currentPsLeft = left
                        }
                        values[DataType.Field.PEDAL_SMOOTHNESS_RIGHT]?.toFloat()?.let { right ->
                            currentPsRight = right
                        }
                        triggerUpdate()
                    }
                    else -> { /* Ignore */ }
                }
            })
            addedConsumers.add(psConsumerId)

            // Subscribe to Power
            // Returns: Field.POWER (current watts)
            powerConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.POWER)
            ) { event: OnStreamState ->
                when (val state = event.state) {
                    is StreamState.Streaming -> {
                        val values = state.dataPoint.values
                        values[DataType.Field.POWER]?.toInt()?.let { power ->
                            currentPower = power
                            triggerUpdate()
                        }
                    }
                    else -> { /* Ignore */ }
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

            android.util.Log.i(TAG, "Started streaming pedaling data")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error starting streams: ${e.message}", e)
            // Cleanup already added consumers on error
            addedConsumers.forEach { ref ->
                ref.getAndSet(null)?.let { id ->
                    safeRemoveConsumer(id)
                }
            }
        }
    }

    private fun triggerUpdate() {
        updateTrigger.tryEmit(Unit)
    }

    /**
     * Stop streaming and cleanup consumers.
     * Note: LiveDataCollector is managed by RideStateMonitor, not here.
     */
    fun stopStreaming() {
        balanceConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        teConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        psConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        powerConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        cadenceConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        balance3sConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }
        balance10sConsumerId.getAndSet(null)?.let { safeRemoveConsumer(it) }

        android.util.Log.i(TAG, "Stopped streaming")
    }

    private fun safeRemoveConsumer(id: String) {
        try {
            extension.karooSystem.removeConsumer(id)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to remove consumer $id: ${e.message}")
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
        val balance3sLeft = currentBalance3sLeft
        val balance10sLeft = currentBalance10sLeft

        // Balance: SDK returns left side %, right = 100 - left
        // PedalingMetrics.balance stores RIGHT side value for display
        val newMetrics = PedalingMetrics(
            balance = 100f - balanceLeft,  // Right side %
            torqueEffLeft = teLeft,
            torqueEffRight = teRight,
            pedalSmoothLeft = psLeft,
            pedalSmoothRight = psRight,
            power = power,
            cadence = cadence,
            balance3s = 100f - balance3sLeft,  // Right side %
            balance10s = 100f - balance10sLeft,  // Right side %
            timestamp = System.currentTimeMillis()
        )

        _metrics.value = newMetrics

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
