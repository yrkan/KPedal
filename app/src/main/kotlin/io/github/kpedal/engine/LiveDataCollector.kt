package io.github.kpedal.engine

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

    // Time in zone tracking (in milliseconds for accuracy)
    private var timeOptimalMs: Long = 0L
    private var timeAttentionMs: Long = 0L
    private var timeProblemMs: Long = 0L

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
        isCollecting = false
        emitJob?.cancel()
        emitJob = null
        // Emit final data
        emitLiveData()
        android.util.Log.i(TAG, "Stopped collecting live data")
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
            timeOptimalMs = 0L
            timeAttentionMs = 0L
            timeProblemMs = 0L
            initialBalanceSum = 0f
            initialTeSum = 0f
            initialPsSum = 0f
            initialSampleCount = 0
            initialPeriodComplete = false
            lastUpdateTimeMs = System.currentTimeMillis()
        }
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
        }
        // Note: Live data is emitted periodically by emitJob, not on every update
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
                hasTrendData = initialPeriodComplete && count > initialSampleCount
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

        // Calculate overall score (0-100)
        // Based on: 50% time in optimal zone + 25% balance quality + 25% TE/PS quality
        val zoneScore = zoneOptimalPct  // 0-100 already
        val balanceScore = (100 - (snapshot.currentBalanceDeviation * 10)).coerceIn(0f, 100f)
        val avgTe = (snapshot.avgTeLeft + snapshot.avgTeRight) / 2f
        val avgPs = (snapshot.avgPsLeft + snapshot.avgPsRight) / 2f
        val teScore = when {
            avgTe >= 70 && avgTe <= 80 -> 100f
            avgTe >= 60 -> 80f
            avgTe >= 50 -> 60f
            else -> 40f
        }
        val psScore = when {
            avgPs >= 20 -> 100f
            avgPs >= 15 -> 80f
            avgPs >= 10 -> 60f
            else -> 40f
        }
        val overallScore = (zoneScore * 0.5f + balanceScore * 0.25f + (teScore + psScore) / 2 * 0.25f).roundToInt().coerceIn(0, 100)

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
        val hasTrendData: Boolean
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
     * Clean up resources.
     */
    fun destroy() {
        stopCollecting()
        scope.cancel()
    }
}
