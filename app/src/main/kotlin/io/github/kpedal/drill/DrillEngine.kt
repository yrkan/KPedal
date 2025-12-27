package io.github.kpedal.drill

import io.github.kpedal.drill.model.*
import io.github.kpedal.engine.PedalingMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Engine for executing drills.
 * Handles timing, scoring, phase transitions, and target evaluation.
 *
 * Scoring logic:
 * - Only phases WITH targets count toward the score
 * - Recovery/warm-up phases (no target) are tracked but don't affect score
 * - Phase scores are recorded as time-in-target percentage for target phases
 * - Phases without targets get -1 score (displayed as "N/A" in UI)
 */
class DrillEngine(
    private val metricsFlow: StateFlow<PedalingMetrics>
) {
    companion object {
        private const val TAG = "DrillEngine"
        private const val TICK_INTERVAL_MS = 100L
        private const val COUNTDOWN_SECONDS = 3
        const val NO_TARGET_SCORE = -1f  // Indicates phase had no target
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Current execution state
    private val _state = MutableStateFlow<DrillExecutionState?>(null)
    val state: StateFlow<DrillExecutionState?> = _state.asStateFlow()

    // Completed drill result
    private val _result = MutableStateFlow<DrillResult?>(null)
    val result: StateFlow<DrillResult?> = _result.asStateFlow()

    // Control flags
    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    // Current drill job for cancellation
    private var drillJob: kotlinx.coroutines.Job? = null

    // Tracking for scoring (only phases with targets count)
    private var totalTimeInTarget = 0L
    private var totalTimeWithTarget = 0L
    private val phaseScores = mutableListOf<Float>()
    private val phaseHasTarget = mutableListOf<Boolean>()
    private var phaseTimeInTarget = 0L
    private var phaseTimeWithTarget = 0L
    private var currentPhaseHasTarget = false

    // Callbacks
    var onPhaseChange: ((Int, DrillPhase) -> Unit)? = null
    var onTargetEnter: (() -> Unit)? = null
    var onTargetExit: (() -> Unit)? = null
    var onCountdownTick: ((Int) -> Unit)? = null
    var onPhaseEndingWarning: ((Int) -> Unit)? = null  // Called at 3, 2, 1 seconds before phase ends
    var onComplete: ((DrillResult) -> Unit)? = null

    // Phase ending warning tracking
    private var lastPhaseWarningSecond = -1

    // Resolved drill name (localized at start time)
    private var resolvedDrillName: String = ""

    /**
     * Start a drill with countdown.
     * @param drill The drill to execute
     * @param drillName The localized drill name (resolved via stringResource in Composable)
     */
    fun start(drill: Drill, drillName: String) {
        resolvedDrillName = drillName
        // Cancel any previous drill job to prevent overlapping
        drillJob?.cancel()
        drillJob = null

        if (isRunning.get()) {
            android.util.Log.w(TAG, "Stopping previous drill before starting new one")
            isRunning.set(false)
        }

        android.util.Log.i(TAG, "Starting drill: ${drill.id}")

        // Reset state
        resetTracking()
        _result.value = null

        // Initialize state with countdown
        _state.value = DrillExecutionState(
            drill = drill,
            status = DrillExecutionStatus.COUNTDOWN,
            countdownSeconds = COUNTDOWN_SECONDS
        )

        isRunning.set(true)
        isPaused.set(false)

        // Start execution loop and store job reference
        drillJob = scope.launch {
            runCountdown()
            if (isRunning.get()) {
                runDrill()
            }
        }
    }

    /**
     * Pause the drill.
     */
    fun pause() {
        if (!isRunning.get() || isPaused.get()) return
        isPaused.set(true)
        _state.update { it?.copy(status = DrillExecutionStatus.PAUSED) }
        android.util.Log.i(TAG, "Drill paused")
    }

    /**
     * Resume from pause.
     */
    fun resume() {
        if (!isRunning.get() || !isPaused.get()) return
        isPaused.set(false)
        _state.update { it?.copy(status = DrillExecutionStatus.RUNNING) }
        android.util.Log.i(TAG, "Drill resumed")
    }

    /**
     * Stop/cancel the drill.
     */
    fun stop() {
        if (!isRunning.get()) return

        android.util.Log.i(TAG, "Drill stopped")
        isRunning.set(false)
        isPaused.set(false)

        // Cancel the drill job
        drillJob?.cancel()
        drillJob = null

        val currentState = _state.value
        if (currentState != null && currentState.status == DrillExecutionStatus.RUNNING) {
            // Create incomplete result
            val result = createResult(currentState, completed = false)
            _result.value = result
            onComplete?.invoke(result)
        }

        _state.update { it?.copy(status = DrillExecutionStatus.CANCELLED) }
    }

    /**
     * Cleanup resources.
     */
    fun destroy() {
        stop()
        scope.cancel()
    }

    private fun resetTracking() {
        totalTimeInTarget = 0L
        totalTimeWithTarget = 0L
        phaseScores.clear()
        phaseHasTarget.clear()
        phaseTimeInTarget = 0L
        phaseTimeWithTarget = 0L
        currentPhaseHasTarget = false
        lastPhaseWarningSecond = -1
    }

    private suspend fun runCountdown() {
        for (i in COUNTDOWN_SECONDS downTo 1) {
            if (!isRunning.get()) return
            _state.update { it?.copy(countdownSeconds = i) }
            onCountdownTick?.invoke(i)
            delay(1000)
        }
    }

    private suspend fun runDrill() {
        val startTime = System.currentTimeMillis()
        var lastTick = startTime
        var wasInTarget = false

        // Start in RUNNING state
        _state.update { it?.copy(status = DrillExecutionStatus.RUNNING) }

        // Initialize first phase tracking
        _state.value?.currentPhase?.let { phase ->
            currentPhaseHasTarget = phase.target != null
            onPhaseChange?.invoke(0, phase)
        }

        while (isRunning.get()) {
            // Handle pause
            if (isPaused.get()) {
                delay(TICK_INTERVAL_MS)
                lastTick = System.currentTimeMillis()
                continue
            }

            val now = System.currentTimeMillis()
            val deltaMs = now - lastTick
            lastTick = now

            val currentState = _state.value ?: break

            // Calculate new elapsed times
            val newElapsedMs = currentState.elapsedMs + deltaMs
            var newPhaseElapsedMs = currentState.phaseElapsedMs + deltaMs
            var newPhaseIndex = currentState.currentPhaseIndex
            var newTargetHoldMs = currentState.targetHoldMs

            // Check for phase transition
            val currentPhase = currentState.currentPhase
            if (currentPhase != null && newPhaseElapsedMs >= currentPhase.durationMs) {
                // Record phase score before moving to next phase
                recordPhaseScore()

                // Move to next phase
                newPhaseIndex++
                if (newPhaseIndex >= currentState.drill.phases.size) {
                    // Drill complete
                    completeDrill(currentState.copy(elapsedMs = newElapsedMs))
                    return
                }

                // Get new phase and update tracking
                val newPhase = currentState.drill.phases[newPhaseIndex]
                currentPhaseHasTarget = newPhase.target != null

                // Reset phase tracking
                newPhaseElapsedMs = 0L
                newTargetHoldMs = 0L
                phaseTimeInTarget = 0L
                phaseTimeWithTarget = 0L
                wasInTarget = false
                lastPhaseWarningSecond = -1  // Reset warning tracker for new phase

                // Notify phase change
                onPhaseChange?.invoke(newPhaseIndex, newPhase)
            }

            // Evaluate target (only for phases with targets)
            val metrics = metricsFlow.value
            val phaseForEval = currentState.drill.phases.getOrNull(newPhaseIndex)
            val target = phaseForEval?.target
            val isInTarget = if (target != null) evaluateTarget(target, metrics) else false

            // Track target time (only for phases WITH targets)
            if (target != null) {
                phaseTimeWithTarget += deltaMs
                totalTimeWithTarget += deltaMs
                if (isInTarget) {
                    newTargetHoldMs += deltaMs
                    phaseTimeInTarget += deltaMs
                    totalTimeInTarget += deltaMs
                }

                // Notify target state changes (only when there's a target)
                if (isInTarget && !wasInTarget) {
                    onTargetEnter?.invoke()
                } else if (!isInTarget && wasInTarget) {
                    onTargetExit?.invoke()
                }
                wasInTarget = isInTarget
            } else {
                // No target - reset wasInTarget state
                wasInTarget = false
            }

            // Calculate score (only from phases with targets)
            val score = calculateScore()

            // Calculate target proximity
            val proximity = if (target != null) {
                val value = when (target.metric) {
                    DrillMetric.BALANCE -> metrics.balance
                    DrillMetric.TORQUE_EFFECTIVENESS -> metrics.torqueEffAvg
                    DrillMetric.PEDAL_SMOOTHNESS -> metrics.pedalSmoothAvg
                    DrillMetric.COMBINED -> 0f
                }
                target.calculateProximity(value) ?: 0f
            } else {
                0f
            }

            // Calculate phase ending countdown (3, 2, 1 before phase ends)
            val phaseForCountdown = currentState.drill.phases.getOrNull(newPhaseIndex)
            val phaseRemainingSec = if (phaseForCountdown != null) {
                ((phaseForCountdown.durationMs - newPhaseElapsedMs) / 1000).toInt()
            } else -1

            val phaseEndingCountdown = when {
                phaseRemainingSec in 1..3 -> phaseRemainingSec
                else -> -1
            }

            // Trigger phase ending warning callback
            if (phaseRemainingSec in 1..3 && phaseRemainingSec != lastPhaseWarningSecond) {
                lastPhaseWarningSecond = phaseRemainingSec
                onPhaseEndingWarning?.invoke(phaseRemainingSec)
            }

            // Update state
            _state.value = currentState.copy(
                currentPhaseIndex = newPhaseIndex,
                elapsedMs = newElapsedMs,
                phaseElapsedMs = newPhaseElapsedMs,
                targetHoldMs = newTargetHoldMs,
                isInTarget = isInTarget && target != null,
                score = score,
                hasTarget = target != null,
                targetProximity = proximity,
                phaseEndingCountdown = phaseEndingCountdown
            )

            delay(TICK_INTERVAL_MS)
        }
    }

    private fun evaluateTarget(target: DrillTarget?, metrics: PedalingMetrics): Boolean {
        if (target == null) return true
        if (!metrics.hasData) return false

        val value = when (target.metric) {
            DrillMetric.BALANCE -> metrics.balance
            DrillMetric.TORQUE_EFFECTIVENESS -> metrics.torqueEffAvg
            DrillMetric.PEDAL_SMOOTHNESS -> metrics.pedalSmoothAvg
            DrillMetric.COMBINED -> return true // Combined metrics don't have single target
        }

        return target.isMet(value)
    }

    private fun calculateScore(): Float {
        if (totalTimeWithTarget == 0L) return 0f
        return (totalTimeInTarget.toFloat() / totalTimeWithTarget * 100).coerceIn(0f, 100f)
    }

    private fun recordPhaseScore() {
        // Track whether this phase had a target
        phaseHasTarget.add(currentPhaseHasTarget)

        val score = if (currentPhaseHasTarget && phaseTimeWithTarget > 0) {
            // Phase with target: calculate actual score
            (phaseTimeInTarget.toFloat() / phaseTimeWithTarget * 100).coerceIn(0f, 100f)
        } else if (currentPhaseHasTarget) {
            // Phase with target but no time tracked (shouldn't happen)
            0f
        } else {
            // Phase without target: use special marker
            NO_TARGET_SCORE
        }
        phaseScores.add(score)
    }

    private fun completeDrill(finalState: DrillExecutionState) {
        android.util.Log.i(TAG, "Drill completed: ${finalState.drill.id}")

        isRunning.set(false)
        isPaused.set(false)

        // Record final phase score
        recordPhaseScore()

        // Create result
        val result = createResult(finalState, completed = true)
        _result.value = result
        _state.update { it?.copy(status = DrillExecutionStatus.COMPLETED, score = result.score) }

        onComplete?.invoke(result)
    }

    private fun createResult(state: DrillExecutionState, completed: Boolean): DrillResult {
        val timeInTargetPercent = if (totalTimeWithTarget > 0) {
            (totalTimeInTarget.toFloat() / totalTimeWithTarget * 100).coerceIn(0f, 100f)
        } else {
            100f
        }

        return DrillResult(
            drillId = state.drill.id,
            drillName = resolvedDrillName,
            timestamp = System.currentTimeMillis(),
            durationMs = state.elapsedMs,
            score = timeInTargetPercent,
            timeInTargetMs = totalTimeInTarget,
            timeInTargetPercent = timeInTargetPercent,
            completed = completed,
            phaseScores = phaseScores.toList()
        )
    }
}
