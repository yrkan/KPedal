package io.github.kpedal.drill

import com.google.common.truth.Truth.assertThat
import io.github.kpedal.drill.model.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Unit tests for drill-related logic.
 *
 * Tests pure functions and data class behavior that don't require
 * coroutines or Android dependencies.
 */
@DisplayName("Drill Logic")
class DrillLogicTest {

    @Nested
    @DisplayName("DrillTarget.isMet()")
    inner class TargetIsMet {

        @Test
        fun `target value with tolerance - value in range returns true`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 2f
            )

            assertThat(target.isMet(50f)).isTrue()
            assertThat(target.isMet(48f)).isTrue()
            assertThat(target.isMet(52f)).isTrue()
        }

        @Test
        fun `target value with tolerance - value outside range returns false`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 2f
            )

            assertThat(target.isMet(47.9f)).isFalse()
            assertThat(target.isMet(52.1f)).isFalse()
        }

        @Test
        fun `target value without tolerance - exact match required`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 0f
            )

            assertThat(target.isMet(50f)).isTrue()
            assertThat(target.isMet(50.1f)).isFalse()
            assertThat(target.isMet(49.9f)).isFalse()
        }

        @Test
        fun `min value only - value above returns true`() {
            val target = DrillTarget(
                metric = DrillMetric.PEDAL_SMOOTHNESS,
                minValue = 20f
            )

            assertThat(target.isMet(20f)).isTrue()
            assertThat(target.isMet(25f)).isTrue()
            assertThat(target.isMet(19.9f)).isFalse()
        }

        @Test
        fun `max value only - value below returns true`() {
            val target = DrillTarget(
                metric = DrillMetric.TORQUE_EFFECTIVENESS,
                maxValue = 80f
            )

            assertThat(target.isMet(80f)).isTrue()
            assertThat(target.isMet(75f)).isTrue()
            assertThat(target.isMet(80.1f)).isFalse()
        }

        @Test
        fun `min and max range - value in range returns true`() {
            val target = DrillTarget(
                metric = DrillMetric.TORQUE_EFFECTIVENESS,
                minValue = 70f,
                maxValue = 80f
            )

            assertThat(target.isMet(70f)).isTrue()
            assertThat(target.isMet(75f)).isTrue()
            assertThat(target.isMet(80f)).isTrue()
            assertThat(target.isMet(69.9f)).isFalse()
            assertThat(target.isMet(80.1f)).isFalse()
        }

        @Test
        fun `no constraints - always returns true`() {
            val target = DrillTarget(metric = DrillMetric.BALANCE)

            assertThat(target.isMet(0f)).isTrue()
            assertThat(target.isMet(50f)).isTrue()
            assertThat(target.isMet(100f)).isTrue()
        }

        @ParameterizedTest(name = "balance {0}% with target 50±2 returns {1}")
        @CsvSource(
            "50.0, true",   // Center
            "48.0, true",   // Lower bound
            "52.0, true",   // Upper bound
            "49.5, true",   // Within tolerance
            "47.9, false",  // Just outside lower
            "52.1, false",  // Just outside upper
            "45.0, false",  // Far below
            "55.0, false"   // Far above
        )
        fun `balance target with tolerance boundaries`(value: Float, expected: Boolean) {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 2f
            )

            assertThat(target.isMet(value)).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("DrillTarget.calculateProximity()")
    inner class TargetProximity {

        @Test
        fun `in target returns 0`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 2f
            )

            assertThat(target.calculateProximity(50f)).isEqualTo(0f)
            assertThat(target.calculateProximity(49f)).isEqualTo(0f)
            assertThat(target.calculateProximity(51f)).isEqualTo(0f)
        }

        @Test
        fun `below target returns negative value`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 2f
            )

            val proximity = target.calculateProximity(43f) // 5 below lower bound (48)
            assertThat(proximity).isLessThan(0f)
        }

        @Test
        fun `above target returns positive value`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 2f
            )

            val proximity = target.calculateProximity(57f) // 5 above upper bound (52)
            assertThat(proximity).isGreaterThan(0f)
        }

        @Test
        fun `proximity is clamped to -1 and 1`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 2f
            )

            assertThat(target.calculateProximity(0f)).isEqualTo(-1f)
            assertThat(target.calculateProximity(100f)).isEqualTo(1f)
        }

        @Test
        fun `min only target - below returns negative`() {
            val target = DrillTarget(
                metric = DrillMetric.PEDAL_SMOOTHNESS,
                minValue = 20f
            )

            assertThat(target.calculateProximity(20f)).isEqualTo(0f)
            assertThat(target.calculateProximity(25f)).isEqualTo(0f)
            assertThat(target.calculateProximity(15f)).isLessThan(0f)
        }

        @Test
        fun `max only target - above returns positive`() {
            val target = DrillTarget(
                metric = DrillMetric.TORQUE_EFFECTIVENESS,
                maxValue = 80f
            )

            assertThat(target.calculateProximity(80f)).isEqualTo(0f)
            assertThat(target.calculateProximity(75f)).isEqualTo(0f)
            assertThat(target.calculateProximity(85f)).isGreaterThan(0f)
        }

        @Test
        fun `no constraints returns null`() {
            val target = DrillTarget(metric = DrillMetric.BALANCE)

            assertThat(target.calculateProximity(50f)).isNull()
        }
    }

    @Nested
    @DisplayName("DrillTarget.description()")
    inner class TargetDescription {

        @Test
        fun `balance target with tolerance shows L R range`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 2f
            )

            assertThat(target.description()).isEqualTo("L:48-52 R:48-52")
        }

        @Test
        fun `balance target exact shows L R values`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 0f
            )

            assertThat(target.description()).isEqualTo("L:50 R:50")
        }

        @Test
        fun `left leg focus shows L percentage`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                maxValue = 48f  // Right < 48 means Left > 52
            )

            assertThat(target.description()).isEqualTo("L≥52%")
        }

        @Test
        fun `right leg focus shows R percentage`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                minValue = 52f  // Right > 52
            )

            assertThat(target.description()).isEqualTo("R≥52%")
        }

        @Test
        fun `min value for non-balance shows threshold`() {
            val target = DrillTarget(
                metric = DrillMetric.PEDAL_SMOOTHNESS,
                minValue = 20f
            )

            assertThat(target.description()).isEqualTo("≥20%")
        }

        @Test
        fun `max value for non-balance shows threshold`() {
            val target = DrillTarget(
                metric = DrillMetric.TORQUE_EFFECTIVENESS,
                maxValue = 80f
            )

            assertThat(target.description()).isEqualTo("≤80%")
        }

        @Test
        fun `range for non-balance shows range`() {
            val target = DrillTarget(
                metric = DrillMetric.TORQUE_EFFECTIVENESS,
                minValue = 70f,
                maxValue = 80f
            )

            assertThat(target.description()).isEqualTo("70-80%")
        }
    }

    @Nested
    @DisplayName("DrillResult.rating")
    inner class ResultRating {

        @ParameterizedTest(name = "score {0} returns {1}")
        @CsvSource(
            "100.0, Excellent",
            "95.0, Excellent",
            "90.0, Excellent",
            "89.9, Good",
            "75.0, Good",
            "74.9, Fair",
            "60.0, Fair",
            "59.9, Needs Work",
            "40.0, Needs Work",
            "39.9, Keep Practicing",
            "0.0, Keep Practicing"
        )
        fun `rating based on score thresholds`(score: Float, expected: String) {
            val result = DrillResult(
                drillId = "test",
                drillName = "Test Drill",
                timestamp = 0L,
                durationMs = 60000L,
                score = score,
                timeInTargetMs = 30000L,
                timeInTargetPercent = score,
                completed = true
            )

            assertThat(result.rating).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("DrillResult.durationFormatted")
    inner class ResultDuration {

        @Test
        fun `formats seconds correctly`() {
            val result = createResult(durationMs = 45000L)
            assertThat(result.durationFormatted).isEqualTo("0:45")
        }

        @Test
        fun `formats minutes and seconds correctly`() {
            val result = createResult(durationMs = 125000L) // 2:05
            assertThat(result.durationFormatted).isEqualTo("2:05")
        }

        @Test
        fun `formats with leading zero for seconds`() {
            val result = createResult(durationMs = 65000L) // 1:05
            assertThat(result.durationFormatted).isEqualTo("1:05")
        }

        private fun createResult(durationMs: Long) = DrillResult(
            drillId = "test",
            drillName = "Test",
            timestamp = 0L,
            durationMs = durationMs,
            score = 50f,
            timeInTargetMs = 0L,
            timeInTargetPercent = 0f,
            completed = true
        )
    }

    @Nested
    @DisplayName("Drill.totalDurationMs")
    inner class DrillDuration {

        @Test
        fun `sums all phase durations`() {
            val drill = Drill(
                id = "test",
                name = "Test Drill",
                description = "Test",
                type = DrillType.GUIDED_WORKOUT,
                metric = DrillMetric.BALANCE,
                difficulty = DrillDifficulty.BEGINNER,
                phases = listOf(
                    DrillPhase("P1", "", 30000L),
                    DrillPhase("P2", "", 60000L),
                    DrillPhase("P3", "", 30000L)
                )
            )

            assertThat(drill.totalDurationMs).isEqualTo(120000L)
        }

        @Test
        fun `empty phases returns 0`() {
            val drill = Drill(
                id = "test",
                name = "Test",
                description = "Test",
                type = DrillType.TIMED_FOCUS,
                metric = DrillMetric.BALANCE,
                difficulty = DrillDifficulty.BEGINNER,
                phases = emptyList()
            )

            assertThat(drill.totalDurationMs).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("Drill.durationFormatted")
    inner class DrillDurationFormatted {

        @Test
        fun `formats seconds only`() {
            val drill = createDrill(30000L) // 30 seconds
            assertThat(drill.durationFormatted).isEqualTo("30s")
        }

        @Test
        fun `formats minutes and seconds`() {
            val drill = createDrill(90000L) // 1m 30s
            assertThat(drill.durationFormatted).isEqualTo("1m 30s")
        }

        @Test
        fun `formats exact minutes`() {
            val drill = createDrill(120000L) // 2m 0s
            assertThat(drill.durationFormatted).isEqualTo("2m 0s")
        }

        @Test
        fun `formats hours minutes`() {
            val drill = createDrill(3720000L) // 1h 2m
            assertThat(drill.durationFormatted).isEqualTo("1h 2m")
        }

        private fun createDrill(durationMs: Long) = Drill(
            id = "test",
            name = "Test",
            description = "Test",
            type = DrillType.TIMED_FOCUS,
            metric = DrillMetric.BALANCE,
            difficulty = DrillDifficulty.BEGINNER,
            phases = listOf(DrillPhase("P1", "", durationMs))
        )
    }

    @Nested
    @DisplayName("DrillExecutionState computed properties")
    inner class ExecutionStateProperties {

        private val testDrill = Drill(
            id = "test",
            name = "Test Drill",
            description = "Test",
            type = DrillType.GUIDED_WORKOUT,
            metric = DrillMetric.BALANCE,
            difficulty = DrillDifficulty.BEGINNER,
            phases = listOf(
                DrillPhase("Phase 1", "", 30000L),
                DrillPhase("Phase 2", "", 60000L),
                DrillPhase("Phase 3", "", 30000L)
            )
        )

        @Test
        fun `currentPhase returns correct phase`() {
            val state = DrillExecutionState(
                drill = testDrill,
                currentPhaseIndex = 1
            )

            assertThat(state.currentPhase?.name).isEqualTo("Phase 2")
        }

        @Test
        fun `nextPhase returns next phase`() {
            val state = DrillExecutionState(
                drill = testDrill,
                currentPhaseIndex = 0
            )

            assertThat(state.nextPhase?.name).isEqualTo("Phase 2")
        }

        @Test
        fun `nextPhase returns null on last phase`() {
            val state = DrillExecutionState(
                drill = testDrill,
                currentPhaseIndex = 2
            )

            assertThat(state.nextPhase).isNull()
        }

        @Test
        fun `phaseRemainingMs calculates correctly`() {
            val state = DrillExecutionState(
                drill = testDrill,
                currentPhaseIndex = 0,
                phaseElapsedMs = 10000L
            )

            // Phase 1 is 30000ms, elapsed is 10000ms
            assertThat(state.phaseRemainingMs).isEqualTo(20000L)
        }

        @Test
        fun `phaseRemainingMs does not go negative`() {
            val state = DrillExecutionState(
                drill = testDrill,
                currentPhaseIndex = 0,
                phaseElapsedMs = 50000L // More than phase duration
            )

            assertThat(state.phaseRemainingMs).isEqualTo(0L)
        }

        @Test
        fun `totalRemainingMs calculates correctly`() {
            val state = DrillExecutionState(
                drill = testDrill,
                elapsedMs = 40000L
            )

            // Total is 120000ms (30+60+30), elapsed is 40000ms
            assertThat(state.totalRemainingMs).isEqualTo(80000L)
        }

        @Test
        fun `progressPercent calculates correctly`() {
            val state = DrillExecutionState(
                drill = testDrill,
                elapsedMs = 60000L // Half of 120000ms
            )

            assertThat(state.progressPercent).isEqualTo(50f)
        }

        @Test
        fun `phaseProgressPercent calculates correctly`() {
            val state = DrillExecutionState(
                drill = testDrill,
                currentPhaseIndex = 0,
                phaseElapsedMs = 15000L // Half of 30000ms
            )

            assertThat(state.phaseProgressPercent).isEqualTo(50f)
        }

        @Test
        fun `formatTimeRemaining returns minutes and seconds format`() {
            val state = DrillExecutionState(
                drill = testDrill,
                elapsedMs = 60000L // 60s remaining is 1:00
            )

            assertThat(state.formatTimeRemaining()).isEqualTo("1:00")
        }

        @Test
        fun `formatPhaseTimeRemaining returns seconds`() {
            val state = DrillExecutionState(
                drill = testDrill,
                currentPhaseIndex = 0,
                phaseElapsedMs = 15000L // 15s remaining
            )

            assertThat(state.formatPhaseTimeRemaining()).isEqualTo("15s")
        }
    }

    @Nested
    @DisplayName("Score Calculation Logic")
    inner class ScoreCalculation {

        /**
         * Mirrors the score calculation in DrillEngine.calculateScore()
         */
        private fun calculateScore(totalTimeInTarget: Long, totalTimeWithTarget: Long): Float {
            if (totalTimeWithTarget == 0L) return 0f
            return (totalTimeInTarget.toFloat() / totalTimeWithTarget * 100).coerceIn(0f, 100f)
        }

        @Test
        fun `100% time in target returns 100`() {
            assertThat(calculateScore(60000L, 60000L)).isEqualTo(100f)
        }

        @Test
        fun `50% time in target returns 50`() {
            assertThat(calculateScore(30000L, 60000L)).isEqualTo(50f)
        }

        @Test
        fun `0% time in target returns 0`() {
            assertThat(calculateScore(0L, 60000L)).isEqualTo(0f)
        }

        @Test
        fun `no target time returns 0`() {
            assertThat(calculateScore(0L, 0L)).isEqualTo(0f)
        }

        @Test
        fun `score is clamped to 100`() {
            // Edge case: shouldn't happen, but defensive
            assertThat(calculateScore(70000L, 60000L)).isEqualTo(100f)
        }

        @ParameterizedTest(name = "timeInTarget={0}ms, totalTime={1}ms returns {2}%")
        @CsvSource(
            "30000, 60000, 50.0",
            "45000, 60000, 75.0",
            "54000, 60000, 90.0",
            "15000, 60000, 25.0",
            "60000, 120000, 50.0",
            "100000, 100000, 100.0"
        )
        fun `score calculation scenarios`(
            timeInTarget: Long,
            totalTime: Long,
            expected: Float
        ) {
            assertThat(calculateScore(timeInTarget, totalTime)).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("Phase Score Recording Logic")
    inner class PhaseScoreLogic {

        private val noTargetScore = -1f

        /**
         * Mirrors the phase score calculation in DrillEngine.recordPhaseScore()
         */
        private fun calculatePhaseScore(
            hasTarget: Boolean,
            phaseTimeInTarget: Long,
            phaseTimeWithTarget: Long
        ): Float {
            return if (hasTarget && phaseTimeWithTarget > 0) {
                (phaseTimeInTarget.toFloat() / phaseTimeWithTarget * 100).coerceIn(0f, 100f)
            } else if (hasTarget) {
                0f
            } else {
                noTargetScore
            }
        }

        @Test
        fun `phase without target returns negative one marker`() {
            assertThat(calculatePhaseScore(false, 0L, 0L)).isEqualTo(noTargetScore)
        }

        @Test
        fun `phase with target and time calculates score`() {
            assertThat(calculatePhaseScore(true, 30000L, 60000L)).isEqualTo(50f)
        }

        @Test
        fun `phase with target but no time returns 0`() {
            assertThat(calculatePhaseScore(true, 0L, 0L)).isEqualTo(0f)
        }

        @Test
        fun `phase with 100% in target returns 100`() {
            assertThat(calculatePhaseScore(true, 60000L, 60000L)).isEqualTo(100f)
        }
    }
}
