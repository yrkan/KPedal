package io.github.kpedal.engine

import io.github.kpedal.engine.checkpoint.AccumulatorState
import io.github.kpedal.ui.screens.LiveRideData
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
import kotlin.math.roundToInt

/**
 * Collects and aggregates pedaling metrics during a ride for the live screen.
 */
class LiveDataCollector {

    companion object {
        private const val TAG = "LiveDataCollector"
        private const val UPDATE_INTERVAL_MS = 1000L
        private const val INITIAL_SAMPLES = 30  // ~30 samples for trend baseline (quick feedback)
        private const val MAX_ELAPSED_MS = 2000L  // Cap elapsed time to avoid pause inflation
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _liveData = MutableStateFlow(LiveRideData())
    val liveData: StateFlow<LiveRideData> = _liveData.asStateFlow()

    // Lock for thread-safe accumulator access
    private val lock = Any()

    // Accumulator values (guarded by lock)
    private var startTimeMs: Long = 0L
    private var sampleCount: Int = 0
    private var balanceLeftSum: Float = 0f
    private var balanceRightSum: Float = 0f
    private var teLeftSum: Float = 0f
    private var teRightSum: Float = 0f
    private var psLeftSum: Float = 0f
    private var psRightSum: Float = 0f

    // Extended metrics accumulators
    private var powerSum: Long = 0L
    private var powerMax: Int = 0
    private var powerSampleCount: Int = 0
    private var cadenceSum: Long = 0L
    private var cadenceSampleCount: Int = 0
    private var heartRateSum: Long = 0L
    private var heartRateMax: Int = 0
    private var heartRateSampleCount: Int = 0
    private var speedSum: Float = 0f
    private var speedSampleCount: Int = 0
    private var lastDistance: Float = 0f

    // Pro cyclist metrics accumulators
    private var lastElevationGain: Float = 0f
    private var lastElevationLoss: Float = 0f
    private var gradeSum: Float = 0f
    private var gradeMax: Float = 0f
    private var gradeSampleCount: Int = 0
    private var lastNormalizedPower: Int = 0
    private var lastEnergy: Float = 0f

    // Time in zone tracking (in milliseconds for accuracy)
    private var timeOptimalMs: Long = 0L
    private var timeAttentionMs: Long = 0L
    private var timeProblemMs: Long = 0L

    // Per-minute snapshot collection (cloud-only)
    private val snapshots = mutableListOf<RideSnapshot>()
    private var lastSnapshotMinute: Int = -1
    // Per-minute accumulators (reset each minute)
    private var minuteBalanceLeftSum: Float = 0f
    private var minuteBalanceRightSum: Float = 0f
    private var minuteTeLeftSum: Float = 0f
    private var minuteTeRightSum: Float = 0f
    private var minutePsLeftSum: Float = 0f
    private var minutePsRightSum: Float = 0f
    private var minutePowerSum: Long = 0L
    private var minuteCadenceSum: Long = 0L
    private var minuteHeartRateSum: Long = 0L
    private var minuteTimeOptimalMs: Long = 0L
    private var minuteTimeAttentionMs: Long = 0L
    private var minuteTimeProblemMs: Long = 0L
    private var minuteSampleCount: Int = 0

    // Initial values for trend calculation (first 30 seconds average)
    private var initialBalanceSum: Float = 0f
    private var initialTeSum: Float = 0f
    private var initialPsSum: Float = 0f
    private var initialSampleCount: Int = 0
    private var initialPeriodComplete: Boolean = false

    @Volatile
    private var isCollecting = false
    private var lastMetrics: PedalingMetrics? = null
    private var lastUpdateTimeMs: Long = 0L
    private var emitJob: kotlinx.coroutines.Job? = null

    /**
     * Start collecting live data.
     * Call this when a ride starts.
     */
    fun startCollecting() {
        if (isCollecting) return

        isCollecting = true
        startTimeMs = System.currentTimeMillis()
        lastUpdateTimeMs = startTimeMs
        resetAccumulators()
        // Reset live data to defaults (hasData = false) to prevent stale data from previous ride
        _liveData.value = LiveRideData()

        // Start periodic emission (every 1 second instead of every metrics update)
        emitJob = scope.launch {
            while (isActive && isCollecting) {
                delay(UPDATE_INTERVAL_MS)
                emitLiveData()
            }
        }

        android.util.Log.i(TAG, "Started collecting live data")
    }

    /**
     * Stop collecting live data.
     * Call this when a ride ends.
     */
    fun stopCollecting() {
        synchronized(lock) {
            isCollecting = false
            // Create final snapshot for any remaining minute data
            if (minuteSampleCount > 0) {
                val finalSnapshot = createSnapshotFromMinuteData(
                    lastSnapshotMinute + 1,
                    System.currentTimeMillis()
                )
                snapshots.add(finalSnapshot)
                android.util.Log.d(TAG, "Created final snapshot for minute ${finalSnapshot.minuteIndex}")
            }
        }
        emitJob?.cancel()
        emitJob = null
        // Emit final data
        emitLiveData()
        android.util.Log.i(TAG, "Stopped collecting live data with ${snapshots.size} snapshots")
    }

    /**
     * Reset all accumulated data.
     */
    fun reset() {
        resetAccumulators()
        _liveData.value = LiveRideData()
        android.util.Log.i(TAG, "Reset live data")
    }

    private fun resetAccumulators() {
        synchronized(lock) {
            sampleCount = 0
            balanceLeftSum = 0f
            balanceRightSum = 0f
            teLeftSum = 0f
            teRightSum = 0f
            psLeftSum = 0f
            psRightSum = 0f
            // Extended metrics
            powerSum = 0L
            powerMax = 0
            powerSampleCount = 0
            cadenceSum = 0L
            cadenceSampleCount = 0
            heartRateSum = 0L
            heartRateMax = 0
            heartRateSampleCount = 0
            speedSum = 0f
            speedSampleCount = 0
            lastDistance = 0f
            // Pro cyclist metrics
            lastElevationGain = 0f
            lastElevationLoss = 0f
            gradeSum = 0f
            gradeMax = 0f
            gradeSampleCount = 0
            lastNormalizedPower = 0
            lastEnergy = 0f
            // Zone tracking
            timeOptimalMs = 0L
            timeAttentionMs = 0L
            timeProblemMs = 0L
            initialBalanceSum = 0f
            initialTeSum = 0f
            initialPsSum = 0f
            initialSampleCount = 0
            initialPeriodComplete = false
            lastUpdateTimeMs = System.currentTimeMillis()
            // Snapshot tracking
            snapshots.clear()
            lastSnapshotMinute = -1
            resetMinuteAccumulators()
        }
    }

    private fun resetMinuteAccumulators() {
        minuteBalanceLeftSum = 0f
        minuteBalanceRightSum = 0f
        minuteTeLeftSum = 0f
        minuteTeRightSum = 0f
        minutePsLeftSum = 0f
        minutePsRightSum = 0f
        minutePowerSum = 0L
        minuteCadenceSum = 0L
        minuteHeartRateSum = 0L
        minuteTimeOptimalMs = 0L
        minuteTimeAttentionMs = 0L
        minuteTimeProblemMs = 0L
        minuteSampleCount = 0
    }

    /**
     * Update with new metrics.
     * Call this every time new metrics arrive.
     */
    fun updateMetrics(metrics: PedalingMetrics) {
        if (!metrics.hasData) return

        synchronized(lock) {
            // Check isCollecting inside lock to prevent race with stopCollecting
            if (!isCollecting) return
            val currentTimeMs = System.currentTimeMillis()
            val elapsedMs = currentTimeMs - lastUpdateTimeMs
            lastUpdateTimeMs = currentTimeMs
            lastMetrics = metrics

            // Accumulate values
            sampleCount++
            balanceLeftSum += metrics.balanceLeft
            balanceRightSum += metrics.balance
            teLeftSum += metrics.torqueEffLeft
            teRightSum += metrics.torqueEffRight
            psLeftSum += metrics.pedalSmoothLeft
            psRightSum += metrics.pedalSmoothRight

            // Accumulate extended metrics (only when valid data present)
            if (metrics.power > 0) {
                powerSum += metrics.power
                powerSampleCount++
                if (metrics.power > powerMax) {
                    powerMax = metrics.power
                }
            }
            if (metrics.cadence > 0) {
                cadenceSum += metrics.cadence
                cadenceSampleCount++
            }
            if (metrics.heartRate > 0) {
                heartRateSum += metrics.heartRate
                heartRateSampleCount++
                if (metrics.heartRate > heartRateMax) {
                    heartRateMax = metrics.heartRate
                }
            }
            if (metrics.speed > 0) {
                speedSum += metrics.speedKmh
                speedSampleCount++
            }
            // Track distance (use latest value, not sum)
            if (metrics.distance > 0) {
                lastDistance = metrics.distanceKm
            }

            // Pro cyclist metrics
            // Elevation gain/loss - use latest cumulative values from Karoo
            if (metrics.elevationGain > 0) {
                lastElevationGain = metrics.elevationGain
            }
            if (metrics.elevationLoss > 0) {
                lastElevationLoss = metrics.elevationLoss
            }
            // Grade - track average and max
            if (metrics.grade != 0f) {
                gradeSum += kotlin.math.abs(metrics.grade)
                gradeSampleCount++
                val absGrade = kotlin.math.abs(metrics.grade)
                if (absGrade > gradeMax) {
                    gradeMax = absGrade
                }
            }
            // Normalized Power - use latest value from Karoo
            if (metrics.normalizedPower > 0) {
                lastNormalizedPower = metrics.normalizedPower
            }
            // Energy - use latest cumulative value from Karoo
            if (metrics.energyKj > 0) {
                lastEnergy = metrics.energyKj
            }

            // Track initial period for trend comparison (quick baseline for live feedback)
            if (!initialPeriodComplete) {
                initialBalanceSum += kotlin.math.abs(50f - metrics.balance)
                initialTeSum += metrics.torqueEffAvg
                initialPsSum += metrics.pedalSmoothAvg
                initialSampleCount++
                if (initialSampleCount >= INITIAL_SAMPLES) {
                    initialPeriodComplete = true
                }
            }

            // Update time in zone based on balance status
            // Cap elapsed time to avoid pause inflation (e.g., after ride pause)
            val cappedElapsedMs = elapsedMs.coerceAtMost(MAX_ELAPSED_MS)
            val balanceStatus = StatusCalculator.balanceStatus(metrics.balance)
            when (balanceStatus) {
                StatusCalculator.Status.OPTIMAL -> timeOptimalMs += cappedElapsedMs
                StatusCalculator.Status.ATTENTION -> timeAttentionMs += cappedElapsedMs
                StatusCalculator.Status.PROBLEM -> timeProblemMs += cappedElapsedMs
            }

            // Per-minute snapshot collection
            accumulateMinuteMetrics(metrics, cappedElapsedMs, balanceStatus)
            checkAndCreateSnapshot(currentTimeMs)
        }
        // Note: Live data is emitted periodically by emitJob, not on every update
    }

    private fun accumulateMinuteMetrics(
        metrics: PedalingMetrics,
        elapsedMs: Long,
        balanceStatus: StatusCalculator.Status
    ) {
        minuteSampleCount++
        minuteBalanceLeftSum += metrics.balanceLeft
        minuteBalanceRightSum += metrics.balance
        minuteTeLeftSum += metrics.torqueEffLeft
        minuteTeRightSum += metrics.torqueEffRight
        minutePsLeftSum += metrics.pedalSmoothLeft
        minutePsRightSum += metrics.pedalSmoothRight

        if (metrics.power > 0) {
            minutePowerSum += metrics.power
        }
        if (metrics.cadence > 0) {
            minuteCadenceSum += metrics.cadence
        }
        if (metrics.heartRate > 0) {
            minuteHeartRateSum += metrics.heartRate
        }

        when (balanceStatus) {
            StatusCalculator.Status.OPTIMAL -> minuteTimeOptimalMs += elapsedMs
            StatusCalculator.Status.ATTENTION -> minuteTimeAttentionMs += elapsedMs
            StatusCalculator.Status.PROBLEM -> minuteTimeProblemMs += elapsedMs
        }
    }

    private fun checkAndCreateSnapshot(currentTimeMs: Long) {
        val elapsedFromStartMs = currentTimeMs - startTimeMs
        val currentMinute = (elapsedFromStartMs / 60000).toInt()

        // Create snapshot when we cross into a new minute
        if (currentMinute > lastSnapshotMinute && minuteSampleCount > 0) {
            val snapshot = createSnapshotFromMinuteData(lastSnapshotMinute + 1, currentTimeMs)
            snapshots.add(snapshot)
            lastSnapshotMinute = currentMinute
            resetMinuteAccumulators()
            android.util.Log.d(TAG, "Created snapshot for minute ${snapshot.minuteIndex}")
        }
    }

    private fun createSnapshotFromMinuteData(minuteIndex: Int, timestamp: Long): RideSnapshot {
        val count = minuteSampleCount.coerceAtLeast(1)

        // Determine dominant zone for this minute
        val dominantZone = when {
            minuteTimeOptimalMs >= minuteTimeAttentionMs && minuteTimeOptimalMs >= minuteTimeProblemMs -> "OPTIMAL"
            minuteTimeAttentionMs >= minuteTimeProblemMs -> "ATTENTION"
            else -> "PROBLEM"
        }

        return RideSnapshot(
            minuteIndex = minuteIndex,
            timestamp = timestamp,
            balanceLeft = (minuteBalanceLeftSum / count).roundToInt(),
            balanceRight = (minuteBalanceRightSum / count).roundToInt(),
            teLeft = (minuteTeLeftSum / count).roundToInt(),
            teRight = (minuteTeRightSum / count).roundToInt(),
            psLeft = (minutePsLeftSum / count).roundToInt(),
            psRight = (minutePsRightSum / count).roundToInt(),
            powerAvg = if (count > 0) (minutePowerSum / count).toInt() else 0,
            cadenceAvg = if (count > 0) (minuteCadenceSum / count).toInt() else 0,
            heartRateAvg = if (count > 0) (minuteHeartRateSum / count).toInt() else 0,
            zoneStatus = dominantZone
        )
    }

    private fun emitLiveData() {
        // Capture snapshot under lock
        val snapshot = synchronized(lock) {
            // Early return if no data - prevents invalid zero-zone display
            if (sampleCount == 0) return

            val count = sampleCount
            LiveDataSnapshot(
                elapsedMs = System.currentTimeMillis() - startTimeMs,
                avgBalanceLeft = (balanceLeftSum / count).roundToInt(),
                avgBalanceRight = (balanceRightSum / count).roundToInt(),
                avgTeLeft = (teLeftSum / count).roundToInt(),
                avgTeRight = (teRightSum / count).roundToInt(),
                avgPsLeft = (psLeftSum / count).roundToInt(),
                avgPsRight = (psRightSum / count).roundToInt(),
                timeOptimalMs = timeOptimalMs,
                timeAttentionMs = timeAttentionMs,
                timeProblemMs = timeProblemMs,
                // Current values for trend calculation
                currentBalanceDeviation = if (count > 0) kotlin.math.abs(50f - balanceRightSum / count) else 0f,
                currentTeAvg = if (count > 0) (teLeftSum + teRightSum) / (count * 2) else 0f,
                currentPsAvg = if (count > 0) (psLeftSum + psRightSum) / (count * 2) else 0f,
                // Initial period values
                initialBalanceDeviation = if (initialSampleCount > 0) initialBalanceSum / initialSampleCount else 0f,
                initialTeAvg = if (initialSampleCount > 0) initialTeSum / initialSampleCount else 0f,
                initialPsAvg = if (initialSampleCount > 0) initialPsSum / initialSampleCount else 0f,
                hasTrendData = initialPeriodComplete && count > initialSampleCount,
                // Extended metrics
                powerAvg = if (powerSampleCount > 0) (powerSum / powerSampleCount).toInt() else 0,
                powerMax = powerMax,
                cadenceAvg = if (cadenceSampleCount > 0) (cadenceSum / cadenceSampleCount).toInt() else 0,
                heartRateAvg = if (heartRateSampleCount > 0) (heartRateSum / heartRateSampleCount).toInt() else 0,
                heartRateMax = heartRateMax,
                speedAvgKmh = if (speedSampleCount > 0) speedSum / speedSampleCount else 0f,
                distanceKm = lastDistance,
                // Pro cyclist metrics
                elevationGain = lastElevationGain.roundToInt(),
                elevationLoss = lastElevationLoss.roundToInt(),
                gradeAvg = if (gradeSampleCount > 0) gradeSum / gradeSampleCount else 0f,
                gradeMax = gradeMax,
                normalizedPower = lastNormalizedPower,
                energyKj = lastEnergy.roundToInt()
            )
        }

        val durationStr = formatDuration(snapshot.elapsedMs)
        val totalTimeMs = snapshot.timeOptimalMs + snapshot.timeAttentionMs + snapshot.timeProblemMs

        // Calculate percentages using largest remainder method for fair rounding
        val (zoneOptimalPct, zoneAttentionPct, zoneProblemPct) = if (totalTimeMs > 0) {
            val optimalExact = snapshot.timeOptimalMs * 100.0 / totalTimeMs
            val attentionExact = snapshot.timeAttentionMs * 100.0 / totalTimeMs
            val problemExact = snapshot.timeProblemMs * 100.0 / totalTimeMs

            // Floor all values first
            var optimalInt = optimalExact.toInt()
            var attentionInt = attentionExact.toInt()
            var problemInt = problemExact.toInt()

            // Distribute remainder to values with largest fractional parts
            val remainder = 100 - (optimalInt + attentionInt + problemInt)
            val fractions = listOf(
                0 to (optimalExact - optimalInt),
                1 to (attentionExact - attentionInt),
                2 to (problemExact - problemInt)
            ).sortedByDescending { it.second }

            for (i in 0 until remainder) {
                when (fractions[i].first) {
                    0 -> optimalInt++
                    1 -> attentionInt++
                    2 -> problemInt++
                }
            }

            Triple(optimalInt, attentionInt, problemInt)
        } else {
            Triple(0, 0, 0)
        }

        // Calculate zone times in minutes
        val zoneOptimalMin = (snapshot.timeOptimalMs / 60000).toInt()
        val zoneAttentionMin = (snapshot.timeAttentionMs / 60000).toInt()
        val zoneProblemMin = (snapshot.timeProblemMs / 60000).toInt()

        // Calculate trends (only if we have enough data)
        val (balanceTrend, teTrend, psTrend) = if (snapshot.hasTrendData) {
            Triple(
                // Balance: lower deviation = better, so if current < initial, trend is positive
                when {
                    snapshot.currentBalanceDeviation < snapshot.initialBalanceDeviation - 1f -> 1
                    snapshot.currentBalanceDeviation > snapshot.initialBalanceDeviation + 1f -> -1
                    else -> 0
                },
                // TE: higher is better (within optimal range)
                when {
                    snapshot.currentTeAvg > snapshot.initialTeAvg + 2f -> 1
                    snapshot.currentTeAvg < snapshot.initialTeAvg - 2f -> -1
                    else -> 0
                },
                // PS: higher is better
                when {
                    snapshot.currentPsAvg > snapshot.initialPsAvg + 2f -> 1
                    snapshot.currentPsAvg < snapshot.initialPsAvg - 2f -> -1
                    else -> 0
                }
            )
        } else {
            Triple(0, 0, 0)
        }

        // Calculate overall score using unified formula from StatusCalculator
        // Formula: 40% balance + 35% efficiency (TE/PS) + 25% consistency (zones)
        val overallScore = StatusCalculator.calculateOverallScore(
            balanceRight = snapshot.avgBalanceRight,
            teLeft = snapshot.avgTeLeft,
            teRight = snapshot.avgTeRight,
            psLeft = snapshot.avgPsLeft,
            psRight = snapshot.avgPsRight,
            zoneOptimal = zoneOptimalPct
        )

        _liveData.value = LiveRideData(
            duration = durationStr,
            balanceLeft = snapshot.avgBalanceLeft,
            balanceRight = snapshot.avgBalanceRight,
            teLeft = snapshot.avgTeLeft,
            teRight = snapshot.avgTeRight,
            psLeft = snapshot.avgPsLeft,
            psRight = snapshot.avgPsRight,
            zoneOptimal = zoneOptimalPct,
            zoneAttention = zoneAttentionPct,
            zoneProblem = zoneProblemPct,
            zoneOptimalMin = zoneOptimalMin,
            zoneAttentionMin = zoneAttentionMin,
            zoneProblemMin = zoneProblemMin,
            balanceTrend = balanceTrend,
            teTrend = teTrend,
            psTrend = psTrend,
            score = overallScore,
            // Extended metrics
            powerAvg = snapshot.powerAvg,
            powerMax = snapshot.powerMax,
            cadenceAvg = snapshot.cadenceAvg,
            heartRateAvg = snapshot.heartRateAvg,
            heartRateMax = snapshot.heartRateMax,
            speedAvgKmh = snapshot.speedAvgKmh,
            distanceKm = snapshot.distanceKm,
            // Pro cyclist metrics
            elevationGain = snapshot.elevationGain,
            elevationLoss = snapshot.elevationLoss,
            gradeAvg = snapshot.gradeAvg,
            gradeMax = snapshot.gradeMax,
            normalizedPower = snapshot.normalizedPower,
            energyKj = snapshot.energyKj,
            hasData = true
        )
    }

    private data class LiveDataSnapshot(
        val elapsedMs: Long,
        val avgBalanceLeft: Int,
        val avgBalanceRight: Int,
        val avgTeLeft: Int,
        val avgTeRight: Int,
        val avgPsLeft: Int,
        val avgPsRight: Int,
        val timeOptimalMs: Long,
        val timeAttentionMs: Long,
        val timeProblemMs: Long,
        val currentBalanceDeviation: Float,
        val currentTeAvg: Float,
        val currentPsAvg: Float,
        val initialBalanceDeviation: Float,
        val initialTeAvg: Float,
        val initialPsAvg: Float,
        val hasTrendData: Boolean,
        // Extended metrics
        val powerAvg: Int,
        val powerMax: Int,
        val cadenceAvg: Int,
        val heartRateAvg: Int,
        val heartRateMax: Int,
        val speedAvgKmh: Float,
        val distanceKm: Float,
        // Pro cyclist metrics
        val elevationGain: Int,
        val elevationLoss: Int,
        val gradeAvg: Float,
        val gradeMax: Float,
        val normalizedPower: Int,
        val energyKj: Int
    )

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    /**
     * Get the current ride duration in milliseconds.
     * Used for accurate storage in the database.
     */
    fun getDurationMs(): Long {
        return if (startTimeMs > 0) {
            System.currentTimeMillis() - startTimeMs
        } else {
            0L
        }
    }

    /**
     * Get collected per-minute snapshots.
     * Used for syncing to cloud for time-series charts.
     * Returns a defensive copy.
     */
    fun getSnapshots(): List<RideSnapshot> {
        synchronized(lock) {
            return snapshots.toList()
        }
    }

    // ========== Checkpoint Support ==========

    /**
     * Get current sample count for checkpoint decision.
     */
    fun getSampleCount(): Int {
        synchronized(lock) {
            return sampleCount
        }
    }

    /**
     * Create checkpoint state from all accumulators.
     * Used for periodic checkpoint saving.
     */
    fun createCheckpointState(): AccumulatorState {
        synchronized(lock) {
            return AccumulatorState(
                // Core metrics
                sampleCount = sampleCount,
                balanceLeftSum = balanceLeftSum,
                balanceRightSum = balanceRightSum,
                teLeftSum = teLeftSum,
                teRightSum = teRightSum,
                psLeftSum = psLeftSum,
                psRightSum = psRightSum,
                // Extended metrics
                powerSum = powerSum,
                powerMax = powerMax,
                powerSampleCount = powerSampleCount,
                cadenceSum = cadenceSum,
                cadenceSampleCount = cadenceSampleCount,
                heartRateSum = heartRateSum,
                heartRateMax = heartRateMax,
                heartRateSampleCount = heartRateSampleCount,
                speedSum = speedSum,
                speedSampleCount = speedSampleCount,
                lastDistance = lastDistance,
                // Pro cyclist metrics
                lastElevationGain = lastElevationGain,
                lastElevationLoss = lastElevationLoss,
                gradeSum = gradeSum,
                gradeMax = gradeMax,
                gradeSampleCount = gradeSampleCount,
                lastNormalizedPower = lastNormalizedPower,
                lastEnergy = lastEnergy,
                // Zone tracking
                timeOptimalMs = timeOptimalMs,
                timeAttentionMs = timeAttentionMs,
                timeProblemMs = timeProblemMs,
                // Initial period
                initialBalanceSum = initialBalanceSum,
                initialTeSum = initialTeSum,
                initialPsSum = initialPsSum,
                initialSampleCount = initialSampleCount,
                initialPeriodComplete = initialPeriodComplete,
                // Timing
                startTimeMs = startTimeMs,
                lastUpdateTimeMs = lastUpdateTimeMs,
                // Minute accumulators
                minuteBalanceLeftSum = minuteBalanceLeftSum,
                minuteBalanceRightSum = minuteBalanceRightSum,
                minuteTeLeftSum = minuteTeLeftSum,
                minuteTeRightSum = minuteTeRightSum,
                minutePsLeftSum = minutePsLeftSum,
                minutePsRightSum = minutePsRightSum,
                minutePowerSum = minutePowerSum,
                minuteCadenceSum = minuteCadenceSum,
                minuteHeartRateSum = minuteHeartRateSum,
                minuteTimeOptimalMs = minuteTimeOptimalMs,
                minuteTimeAttentionMs = minuteTimeAttentionMs,
                minuteTimeProblemMs = minuteTimeProblemMs,
                minuteSampleCount = minuteSampleCount,
                lastSnapshotMinute = lastSnapshotMinute
            )
        }
    }

    /**
     * Restore all accumulators from checkpoint state.
     * Called on app launch to recover from crash.
     */
    fun restoreFromCheckpoint(state: AccumulatorState, restoredSnapshots: List<RideSnapshot>) {
        synchronized(lock) {
            // Core metrics
            sampleCount = state.sampleCount
            balanceLeftSum = state.balanceLeftSum
            balanceRightSum = state.balanceRightSum
            teLeftSum = state.teLeftSum
            teRightSum = state.teRightSum
            psLeftSum = state.psLeftSum
            psRightSum = state.psRightSum
            // Extended metrics
            powerSum = state.powerSum
            powerMax = state.powerMax
            powerSampleCount = state.powerSampleCount
            cadenceSum = state.cadenceSum
            cadenceSampleCount = state.cadenceSampleCount
            heartRateSum = state.heartRateSum
            heartRateMax = state.heartRateMax
            heartRateSampleCount = state.heartRateSampleCount
            speedSum = state.speedSum
            speedSampleCount = state.speedSampleCount
            lastDistance = state.lastDistance
            // Pro cyclist metrics
            lastElevationGain = state.lastElevationGain
            lastElevationLoss = state.lastElevationLoss
            gradeSum = state.gradeSum
            gradeMax = state.gradeMax
            gradeSampleCount = state.gradeSampleCount
            lastNormalizedPower = state.lastNormalizedPower
            lastEnergy = state.lastEnergy
            // Zone tracking
            timeOptimalMs = state.timeOptimalMs
            timeAttentionMs = state.timeAttentionMs
            timeProblemMs = state.timeProblemMs
            // Initial period
            initialBalanceSum = state.initialBalanceSum
            initialTeSum = state.initialTeSum
            initialPsSum = state.initialPsSum
            initialSampleCount = state.initialSampleCount
            initialPeriodComplete = state.initialPeriodComplete
            // Timing
            startTimeMs = state.startTimeMs
            lastUpdateTimeMs = state.lastUpdateTimeMs
            // Minute accumulators
            minuteBalanceLeftSum = state.minuteBalanceLeftSum
            minuteBalanceRightSum = state.minuteBalanceRightSum
            minuteTeLeftSum = state.minuteTeLeftSum
            minuteTeRightSum = state.minuteTeRightSum
            minutePsLeftSum = state.minutePsLeftSum
            minutePsRightSum = state.minutePsRightSum
            minutePowerSum = state.minutePowerSum
            minuteCadenceSum = state.minuteCadenceSum
            minuteHeartRateSum = state.minuteHeartRateSum
            minuteTimeOptimalMs = state.minuteTimeOptimalMs
            minuteTimeAttentionMs = state.minuteTimeAttentionMs
            minuteTimeProblemMs = state.minuteTimeProblemMs
            minuteSampleCount = state.minuteSampleCount
            lastSnapshotMinute = state.lastSnapshotMinute
            // Restore snapshots
            snapshots.clear()
            snapshots.addAll(restoredSnapshots)
        }

        android.util.Log.i(TAG, "Restored from checkpoint: samples=$sampleCount, snapshots=${restoredSnapshots.size}")

        // Emit restored state to update UI
        emitLiveData()
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        stopCollecting()
        scope.cancel()
    }
}
