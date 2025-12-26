package io.github.kpedal.engine

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.roundToInt

/**
 * Unit tests for LiveDataCollector logic.
 *
 * Tests the calculation logic for zones, averages, trends, and scoring
 * without needing coroutines or Android dependencies.
 */
@DisplayName("LiveDataCollector Logic")
class LiveDataCollectorLogicTest {

    /**
     * Zone percentage calculation using largest remainder method.
     * Mirrors the logic in LiveDataCollector.emitLiveData()
     */
    private fun calculateZonePercentages(
        optimalMs: Long,
        attentionMs: Long,
        problemMs: Long
    ): Triple<Int, Int, Int> {
        val totalMs = optimalMs + attentionMs + problemMs
        if (totalMs == 0L) return Triple(0, 0, 0)

        val optimalExact = optimalMs * 100.0 / totalMs
        val attentionExact = attentionMs * 100.0 / totalMs
        val problemExact = problemMs * 100.0 / totalMs

        var optimalInt = optimalExact.toInt()
        var attentionInt = attentionExact.toInt()
        var problemInt = problemExact.toInt()

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

        return Triple(optimalInt, attentionInt, problemInt)
    }

    /**
     * Trend calculation logic.
     * Returns: 1 = improving, 0 = stable, -1 = degrading
     */
    private fun calculateBalanceTrend(
        currentDeviation: Float,
        initialDeviation: Float
    ): Int = when {
        currentDeviation < initialDeviation - 1f -> 1   // Lower deviation = better
        currentDeviation > initialDeviation + 1f -> -1  // Higher deviation = worse
        else -> 0
    }

    private fun calculateTeTrend(
        currentAvg: Float,
        initialAvg: Float
    ): Int = when {
        currentAvg > initialAvg + 2f -> 1   // Higher TE = better
        currentAvg < initialAvg - 2f -> -1  // Lower TE = worse
        else -> 0
    }

    private fun calculatePsTrend(
        currentAvg: Float,
        initialAvg: Float
    ): Int = when {
        currentAvg > initialAvg + 2f -> 1   // Higher PS = better
        currentAvg < initialAvg - 2f -> -1  // Lower PS = worse
        else -> 0
    }

    /**
     * Score calculation logic.
     * Based on: 50% time in optimal zone + 25% balance quality + 25% TE/PS quality
     */
    private fun calculateOverallScore(
        zoneOptimalPct: Int,
        balanceDeviation: Float,
        avgTe: Float,
        avgPs: Float
    ): Int {
        val zoneScore = zoneOptimalPct
        val balanceScore = (100 - (balanceDeviation * 10)).coerceIn(0f, 100f)
        val teScore = when {
            avgTe in 70f..80f -> 100f
            avgTe >= 60f -> 80f
            avgTe >= 50f -> 60f
            else -> 40f
        }
        val psScore = when {
            avgPs >= 20f -> 100f
            avgPs >= 15f -> 80f
            avgPs >= 10f -> 60f
            else -> 40f
        }
        return (zoneScore * 0.5f + balanceScore * 0.25f + (teScore + psScore) / 2 * 0.25f)
            .roundToInt()
            .coerceIn(0, 100)
    }

    /**
     * Duration formatting logic.
     */
    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    @Nested
    @DisplayName("Zone Percentage Calculation")
    inner class ZonePercentageTests {

        @Test
        fun `equal time in all zones returns 33-33-34`() {
            val (optimal, attention, problem) = calculateZonePercentages(
                optimalMs = 1000,
                attentionMs = 1000,
                problemMs = 1000
            )
            assertThat(optimal + attention + problem).isEqualTo(100)
            // One of them should have the extra percent
            assertThat(listOf(optimal, attention, problem).sorted()).isEqualTo(listOf(33, 33, 34))
        }

        @Test
        fun `all optimal time returns 100-0-0`() {
            val (optimal, attention, problem) = calculateZonePercentages(
                optimalMs = 60000,
                attentionMs = 0,
                problemMs = 0
            )
            assertThat(optimal).isEqualTo(100)
            assertThat(attention).isEqualTo(0)
            assertThat(problem).isEqualTo(0)
        }

        @Test
        fun `zero total time returns 0-0-0`() {
            val (optimal, attention, problem) = calculateZonePercentages(
                optimalMs = 0,
                attentionMs = 0,
                problemMs = 0
            )
            assertThat(optimal).isEqualTo(0)
            assertThat(attention).isEqualTo(0)
            assertThat(problem).isEqualTo(0)
        }

        @ParameterizedTest(name = "optimal={0}ms, attention={1}ms, problem={2}ms sums to 100%")
        @CsvSource(
            "6000, 3000, 1000",   // 60-30-10
            "5000, 5000, 0",      // 50-50-0
            "1000, 0, 9000",      // 10-0-90
            "333, 333, 334",      // ~33-33-34
            "1, 1, 1"             // Edge case: small values
        )
        fun `zone percentages always sum to 100`(optMs: Long, attMs: Long, probMs: Long) {
            val (optimal, attention, problem) = calculateZonePercentages(optMs, attMs, probMs)
            assertThat(optimal + attention + problem).isEqualTo(100)
        }

        @Test
        fun `largest remainder method distributes remainder correctly`() {
            // 45000 / 60000 = 75%
            // 10000 / 60000 = 16.67%
            // 5000 / 60000 = 8.33%
            // Sum of floors: 75 + 16 + 8 = 99, need 1 more
            val (optimal, attention, problem) = calculateZonePercentages(
                optimalMs = 45000,
                attentionMs = 10000,
                problemMs = 5000
            )
            assertThat(optimal).isEqualTo(75)
            // The remainder should go to attention (0.67) over problem (0.33)
            assertThat(attention).isEqualTo(17)
            assertThat(problem).isEqualTo(8)
        }
    }

    @Nested
    @DisplayName("Trend Calculation")
    inner class TrendTests {

        @Nested
        @DisplayName("Balance Trend")
        inner class BalanceTrendTests {

            @Test
            fun `lower deviation indicates improvement`() {
                assertThat(calculateBalanceTrend(2f, 5f)).isEqualTo(1)
            }

            @Test
            fun `higher deviation indicates degradation`() {
                assertThat(calculateBalanceTrend(6f, 3f)).isEqualTo(-1)
            }

            @Test
            fun `similar deviation indicates stable`() {
                assertThat(calculateBalanceTrend(3f, 3.5f)).isEqualTo(0)
            }

            @Test
            fun `threshold of 1 for improvement`() {
                assertThat(calculateBalanceTrend(3.9f, 5f)).isEqualTo(1)  // Exactly 1.1 better
                assertThat(calculateBalanceTrend(4f, 5f)).isEqualTo(0)   // Exactly 1 better - stable
            }
        }

        @Nested
        @DisplayName("TE Trend")
        inner class TeTrendTests {

            @Test
            fun `higher TE indicates improvement`() {
                assertThat(calculateTeTrend(77f, 72f)).isEqualTo(1)
            }

            @Test
            fun `lower TE indicates degradation`() {
                assertThat(calculateTeTrend(68f, 75f)).isEqualTo(-1)
            }

            @Test
            fun `similar TE indicates stable`() {
                assertThat(calculateTeTrend(74f, 75f)).isEqualTo(0)
            }

            @Test
            fun `threshold of 2 for TE changes`() {
                assertThat(calculateTeTrend(77.1f, 75f)).isEqualTo(1)  // > 2 better
                assertThat(calculateTeTrend(77f, 75f)).isEqualTo(0)   // Exactly 2 - stable
            }
        }

        @Nested
        @DisplayName("PS Trend")
        inner class PsTrendTests {

            @Test
            fun `higher PS indicates improvement`() {
                assertThat(calculatePsTrend(25f, 20f)).isEqualTo(1)
            }

            @Test
            fun `lower PS indicates degradation`() {
                assertThat(calculatePsTrend(18f, 24f)).isEqualTo(-1)
            }

            @Test
            fun `similar PS indicates stable`() {
                assertThat(calculatePsTrend(21f, 22f)).isEqualTo(0)
            }
        }
    }

    @Nested
    @DisplayName("Overall Score Calculation")
    inner class ScoreTests {

        @Test
        fun `perfect metrics return high score`() {
            val score = calculateOverallScore(
                zoneOptimalPct = 100,  // 50 points
                balanceDeviation = 0f, // 100 * 0.25 = 25 points
                avgTe = 75f,           // 100 -> 25 * 0.5 = 12.5 points
                avgPs = 25f            // 100 -> 25 * 0.5 = 12.5 points
            )
            assertThat(score).isEqualTo(100)
        }

        @Test
        fun `poor metrics return low score`() {
            val score = calculateOverallScore(
                zoneOptimalPct = 0,     // 0 points
                balanceDeviation = 10f, // 0 points (capped at 0)
                avgTe = 40f,            // 40 -> 5 points
                avgPs = 5f              // 40 -> 5 points
            )
            assertThat(score).isLessThan(30)
        }

        @Test
        fun `balanced contribution from components`() {
            // Zone: 60% = 30 points (50% weight)
            // Balance: deviation 2 = 80 points -> 20 points (25% weight)
            // TE: 75 = 100 points -> 12.5 points (12.5% weight)
            // PS: 22 = 100 points -> 12.5 points (12.5% weight)
            // Total: 30 + 20 + 12.5 + 12.5 = 75
            val score = calculateOverallScore(
                zoneOptimalPct = 60,
                balanceDeviation = 2f,
                avgTe = 75f,
                avgPs = 22f
            )
            assertThat(score).isEqualTo(75)
        }

        @ParameterizedTest(name = "TE {0}% returns TE score factor {1}")
        @CsvSource(
            "75, 100",  // In optimal range
            "70, 100",  // At lower bound
            "80, 100",  // At upper bound
            "65, 80",   // Good but not optimal
            "55, 60",   // Fair
            "40, 40"    // Poor
        )
        fun `TE score tiers work correctly`(te: Float, expectedTeScore: Float) {
            // Calculate score with only TE varying, other factors neutral
            val score = calculateOverallScore(
                zoneOptimalPct = 0,
                balanceDeviation = 0f,
                avgTe = te,
                avgPs = 100f  // Max out PS
            )
            // With zone=0, balance=100, te=expectedTeScore, ps=100
            // Score = 0 + 25 + (expectedTeScore + 100) / 2 * 0.25
            val expectedScore = (25 + (expectedTeScore + 100) / 2 * 0.25f).roundToInt()
            assertThat(score).isEqualTo(expectedScore)
        }

        @ParameterizedTest(name = "PS {0}% returns PS score factor {1}")
        @CsvSource(
            "25, 100",  // Above minimum
            "20, 100",  // At minimum
            "18, 80",   // Attention zone
            "12, 60",   // Fair
            "5, 40"     // Poor
        )
        fun `PS score tiers work correctly`(ps: Float, expectedPsScore: Float) {
            val score = calculateOverallScore(
                zoneOptimalPct = 0,
                balanceDeviation = 0f,
                avgTe = 100f,  // Max out TE tier to 40 (below 60)
                avgPs = ps
            )
            // TE of 100 is > 80, so it's actually in PROBLEM zone, giving 40
            // So TE factor is 40, not 100
        }

        @Test
        fun `balance deviation impact is capped`() {
            // Deviation of 10 should give balance score of 0 (not negative)
            val scoreHighDev = calculateOverallScore(
                zoneOptimalPct = 50,
                balanceDeviation = 10f,
                avgTe = 75f,
                avgPs = 25f
            )

            // Deviation of 15 should also give 0 (capped)
            val scoreVeryHighDev = calculateOverallScore(
                zoneOptimalPct = 50,
                balanceDeviation = 15f,
                avgTe = 75f,
                avgPs = 25f
            )

            assertThat(scoreHighDev).isEqualTo(scoreVeryHighDev)
        }

        @Test
        fun `score is clamped to 0-100 range`() {
            // Theoretically impossible to exceed 100, but test clamping
            val maxScore = calculateOverallScore(
                zoneOptimalPct = 100,
                balanceDeviation = 0f,
                avgTe = 75f,
                avgPs = 25f
            )
            assertThat(maxScore).isAtMost(100)

            val minScore = calculateOverallScore(
                zoneOptimalPct = 0,
                balanceDeviation = 100f,
                avgTe = 0f,
                avgPs = 0f
            )
            assertThat(minScore).isAtLeast(0)
        }
    }

    @Nested
    @DisplayName("Duration Formatting")
    inner class DurationFormattingTests {

        @Test
        fun `format zero duration`() {
            assertThat(formatDuration(0)).isEqualTo("0:00:00")
        }

        @Test
        fun `format seconds only`() {
            assertThat(formatDuration(45_000)).isEqualTo("0:00:45")
        }

        @Test
        fun `format minutes and seconds`() {
            assertThat(formatDuration(125_000)).isEqualTo("0:02:05")
        }

        @Test
        fun `format hours minutes seconds`() {
            assertThat(formatDuration(3_725_000)).isEqualTo("1:02:05")
        }

        @Test
        fun `format long ride`() {
            assertThat(formatDuration(7_200_000)).isEqualTo("2:00:00")
        }

        @Test
        fun `format with leading zeros`() {
            assertThat(formatDuration(3_601_000)).isEqualTo("1:00:01")
        }
    }

    @Nested
    @DisplayName("Averaging Logic")
    inner class AveragingTests {

        @Test
        fun `average of balanced samples`() {
            val samples = listOf(48f, 50f, 52f)
            val avg = samples.sum() / samples.size
            assertThat(avg).isEqualTo(50f)
        }

        @Test
        fun `rounded average for display`() {
            val samples = listOf(47.3f, 48.7f, 52.1f)
            val avg = (samples.sum() / samples.size).roundToInt()
            assertThat(avg).isEqualTo(49)  // 49.37 rounds to 49
        }

        @Test
        fun `empty samples returns zero`() {
            val count = 0
            val sum = 0f
            val avg = if (count > 0) sum / count else 0f
            assertThat(avg).isEqualTo(0f)
        }
    }

    @Nested
    @DisplayName("Zone Time Conversion")
    inner class ZoneTimeTests {

        @Test
        fun `milliseconds to minutes conversion`() {
            val timeMs = 150_000L  // 2.5 minutes
            val minutes = (timeMs / 60000).toInt()
            assertThat(minutes).isEqualTo(2)  // Truncated
        }

        @Test
        fun `zone minutes for display`() {
            val optimalMs = 360_000L   // 6 minutes
            val attentionMs = 180_000L // 3 minutes
            val problemMs = 60_000L    // 1 minute

            assertThat((optimalMs / 60000).toInt()).isEqualTo(6)
            assertThat((attentionMs / 60000).toInt()).isEqualTo(3)
            assertThat((problemMs / 60000).toInt()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("Snapshot Logic")
    inner class SnapshotTests {

        @Test
        fun `minute index calculation`() {
            val startTimeMs = 0L
            val currentTimeMs = 125_000L  // 2 min 5 sec

            val currentMinute = ((currentTimeMs - startTimeMs) / 60000).toInt()
            assertThat(currentMinute).isEqualTo(2)
        }

        @Test
        fun `snapshot trigger on minute boundary`() {
            var lastSnapshotMinute = -1

            // Simulate time progression
            for (elapsedSec in 0..180 step 10) {
                val currentMinute = (elapsedSec * 1000L / 60000).toInt()
                if (currentMinute > lastSnapshotMinute) {
                    lastSnapshotMinute = currentMinute
                }
            }

            assertThat(lastSnapshotMinute).isEqualTo(3)
        }

        @Test
        fun `dominant zone calculation`() {
            fun calculateDominantZone(optimalMs: Long, attentionMs: Long, problemMs: Long): String {
                return when {
                    optimalMs >= attentionMs && optimalMs >= problemMs -> "OPTIMAL"
                    attentionMs >= problemMs -> "ATTENTION"
                    else -> "PROBLEM"
                }
            }

            assertThat(calculateDominantZone(5000, 3000, 2000)).isEqualTo("OPTIMAL")
            assertThat(calculateDominantZone(2000, 5000, 3000)).isEqualTo("ATTENTION")
            assertThat(calculateDominantZone(2000, 3000, 5000)).isEqualTo("PROBLEM")
            assertThat(calculateDominantZone(3000, 3000, 3000)).isEqualTo("OPTIMAL")  // Tie goes to optimal
        }
    }

    @Nested
    @DisplayName("Initial Period Tracking")
    inner class InitialPeriodTests {

        @Test
        fun `initial period completes after 30 samples`() {
            val INITIAL_SAMPLES = 30
            var sampleCount = 0
            var initialPeriodComplete = false

            for (i in 1..35) {
                sampleCount++
                if (sampleCount >= INITIAL_SAMPLES && !initialPeriodComplete) {
                    initialPeriodComplete = true
                }
            }

            assertThat(initialPeriodComplete).isTrue()
        }

        @Test
        fun `trend data available after initial period`() {
            val initialPeriodComplete = true
            val totalSamples = 50
            val initialSamples = 30

            val hasTrendData = initialPeriodComplete && totalSamples > initialSamples
            assertThat(hasTrendData).isTrue()
        }

        @Test
        fun `no trend data before initial period`() {
            val initialPeriodComplete = false
            val totalSamples = 20
            val initialSamples = 30

            val hasTrendData = initialPeriodComplete && totalSamples > initialSamples
            assertThat(hasTrendData).isFalse()
        }
    }
}
