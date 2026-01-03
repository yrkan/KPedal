package io.github.kpedal.engine.checkpoint

import kotlinx.serialization.Serializable

/**
 * Serializable snapshot of all LiveDataCollector accumulators.
 * Used for checkpoint persistence to recover ride data after crash.
 */
@Serializable
data class AccumulatorState(
    // Core metrics
    val sampleCount: Int,
    val balanceLeftSum: Float,
    val balanceRightSum: Float,
    val teLeftSum: Float,
    val teRightSum: Float,
    val psLeftSum: Float,
    val psRightSum: Float,

    // Extended metrics
    val powerSum: Long,
    val powerMax: Int,
    val powerSampleCount: Int,
    val cadenceSum: Long,
    val cadenceSampleCount: Int,
    val heartRateSum: Long,
    val heartRateMax: Int,
    val heartRateSampleCount: Int,
    val speedSum: Float,
    val speedSampleCount: Int,
    val lastDistance: Float,

    // Pro cyclist metrics
    val lastElevationGain: Float,
    val lastElevationLoss: Float,
    val gradeSum: Float,
    val gradeMax: Float,
    val gradeSampleCount: Int,
    val lastNormalizedPower: Int,
    val lastEnergy: Float,

    // Zone tracking (milliseconds)
    val timeOptimalMs: Long,
    val timeAttentionMs: Long,
    val timeProblemMs: Long,

    // Initial period tracking (for trend calculation)
    val initialBalanceSum: Float,
    val initialTeSum: Float,
    val initialPsSum: Float,
    val initialSampleCount: Int,
    val initialPeriodComplete: Boolean,

    // Timing
    val startTimeMs: Long,
    val lastUpdateTimeMs: Long,

    // Per-minute accumulators (current incomplete minute)
    val minuteBalanceLeftSum: Float,
    val minuteBalanceRightSum: Float,
    val minuteTeLeftSum: Float,
    val minuteTeRightSum: Float,
    val minutePsLeftSum: Float,
    val minutePsRightSum: Float,
    val minutePowerSum: Long,
    val minuteCadenceSum: Long,
    val minuteHeartRateSum: Long,
    val minuteTimeOptimalMs: Long,
    val minuteTimeAttentionMs: Long,
    val minuteTimeProblemMs: Long,
    val minuteSampleCount: Int,
    val lastSnapshotMinute: Int
)
