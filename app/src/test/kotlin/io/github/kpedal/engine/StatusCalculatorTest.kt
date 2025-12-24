package io.github.kpedal.engine

import com.google.common.truth.Truth.assertThat
import io.github.kpedal.engine.StatusCalculator.Status
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("StatusCalculator")
class StatusCalculatorTest {

    @Nested
    @DisplayName("balanceStatus()")
    inner class BalanceStatusTests {

        @Test
        fun `perfect 50-50 balance returns OPTIMAL`() {
            assertThat(StatusCalculator.balanceStatus(50f)).isEqualTo(Status.OPTIMAL)
        }

        @ParameterizedTest(name = "balance {0}% with threshold {1} returns {2}")
        @CsvSource(
            // Default threshold 5: optimal ≤2.5%, attention ≤5%, problem >5%
            "50.0, 5, OPTIMAL",      // Perfect center
            "47.5, 5, OPTIMAL",      // At optimal boundary (left)
            "52.5, 5, OPTIMAL",      // At optimal boundary (right)
            "47.4, 5, ATTENTION",    // Just past optimal (left)
            "52.6, 5, ATTENTION",    // Just past optimal (right)
            "45.0, 5, ATTENTION",    // At attention boundary (left)
            "55.0, 5, ATTENTION",    // At attention boundary (right)
            "44.9, 5, PROBLEM",      // Just past attention (left)
            "55.1, 5, PROBLEM",      // Just past attention (right)
            "40.0, 5, PROBLEM",      // Severe imbalance (left)
            "60.0, 5, PROBLEM",      // Severe imbalance (right)
            // Custom threshold 10: optimal ≤5%, attention ≤10%, problem >10%
            "45.0, 10, OPTIMAL",     // 5% deviation with threshold 10
            "55.0, 10, OPTIMAL",     // 5% deviation with threshold 10
            "44.9, 10, ATTENTION",   // Just past optimal with threshold 10
            "40.0, 10, ATTENTION",   // At attention boundary with threshold 10
            "39.9, 10, PROBLEM",     // Just past attention with threshold 10
            // Custom threshold 2: optimal ≤1%, attention ≤2%, problem >2%
            "49.0, 2, OPTIMAL",      // 1% deviation with threshold 2
            "51.0, 2, OPTIMAL",      // 1% deviation with threshold 2
            "48.9, 2, ATTENTION",    // Just past optimal with threshold 2
            "47.9, 2, PROBLEM"       // Just past attention with threshold 2
        )
        fun `returns correct status for balance deviation and threshold`(
            balance: Float,
            threshold: Int,
            expected: String
        ) {
            assertThat(StatusCalculator.balanceStatus(balance, threshold))
                .isEqualTo(Status.valueOf(expected))
        }

        @Test
        fun `extreme left balance (0%) returns PROBLEM`() {
            assertThat(StatusCalculator.balanceStatus(0f)).isEqualTo(Status.PROBLEM)
        }

        @Test
        fun `extreme right balance (100%) returns PROBLEM`() {
            assertThat(StatusCalculator.balanceStatus(100f)).isEqualTo(Status.PROBLEM)
        }

        @Test
        fun `threshold 1 has minimum optimal threshold of 1`() {
            // With threshold 1, optimalThreshold = max(0.5, 1) = 1, attentionThreshold = 1
            // So deviation > 1 goes straight to PROBLEM (no ATTENTION zone)
            assertThat(StatusCalculator.balanceStatus(49f, 1)).isEqualTo(Status.OPTIMAL)
            assertThat(StatusCalculator.balanceStatus(51f, 1)).isEqualTo(Status.OPTIMAL)
            // deviation = 1.1 > attentionThreshold (1) -> PROBLEM
            assertThat(StatusCalculator.balanceStatus(48.9f, 1)).isEqualTo(Status.PROBLEM)
        }
    }

    @Nested
    @DisplayName("teStatus()")
    inner class TeStatusTests {

        @ParameterizedTest(name = "TE {0}% with range {1}-{2} returns {3}")
        @CsvSource(
            // Default range 70-80: optimal in range, attention ±5, problem outside
            "75.0, 70, 80, OPTIMAL",     // Middle of optimal
            "70.0, 70, 80, OPTIMAL",     // At lower optimal bound
            "80.0, 70, 80, OPTIMAL",     // At upper optimal bound
            "69.9, 70, 80, ATTENTION",   // Just below optimal
            "80.1, 70, 80, ATTENTION",   // Just above optimal
            "65.0, 70, 80, ATTENTION",   // At lower attention bound
            "85.0, 70, 80, ATTENTION",   // At upper attention bound
            "64.9, 70, 80, PROBLEM",     // Just below attention
            "85.1, 70, 80, PROBLEM",     // Just above attention
            "50.0, 70, 80, PROBLEM",     // Very low TE
            "95.0, 70, 80, PROBLEM",     // Very high TE (still not optimal!)
            // Custom range 60-70
            "65.0, 60, 70, OPTIMAL",     // Middle of custom range
            "60.0, 60, 70, OPTIMAL",     // At lower bound
            "70.0, 60, 70, OPTIMAL",     // At upper bound
            "55.0, 60, 70, ATTENTION",   // At attention margin
            "54.9, 60, 70, PROBLEM"      // Below attention margin
        )
        fun `returns correct status for TE and range`(
            te: Float,
            optimalMin: Int,
            optimalMax: Int,
            expected: String
        ) {
            assertThat(StatusCalculator.teStatus(te, optimalMin, optimalMax))
                .isEqualTo(Status.valueOf(expected))
        }

        @Test
        fun `TE above 80 is NOT optimal (research-based)`() {
            // Important: High TE above 80% is not better per Wattbike research
            assertThat(StatusCalculator.teStatus(81f)).isEqualTo(Status.ATTENTION)
            assertThat(StatusCalculator.teStatus(90f)).isEqualTo(Status.PROBLEM)
            assertThat(StatusCalculator.teStatus(100f)).isEqualTo(Status.PROBLEM)
        }

        @Test
        fun `TE at 0 returns PROBLEM`() {
            assertThat(StatusCalculator.teStatus(0f)).isEqualTo(Status.PROBLEM)
        }
    }

    @Nested
    @DisplayName("psStatus()")
    inner class PsStatusTests {

        @ParameterizedTest(name = "PS {0}% with minimum {1} returns {2}")
        @CsvSource(
            // Default minimum 20: optimal ≥20, attention ≥15, problem <15
            "25.0, 20, OPTIMAL",      // Above minimum
            "20.0, 20, OPTIMAL",      // At minimum
            "19.9, 20, ATTENTION",    // Just below minimum
            "15.0, 20, ATTENTION",    // At attention threshold (20-5=15)
            "14.9, 20, PROBLEM",      // Just below attention
            "10.0, 20, PROBLEM",      // Low smoothness
            "0.0, 20, PROBLEM",       // Zero smoothness
            "100.0, 20, OPTIMAL",     // Maximum smoothness (unrealistic but valid)
            // Custom minimum 25 (elite target)
            "30.0, 25, OPTIMAL",      // Above elite minimum
            "25.0, 25, OPTIMAL",      // At elite minimum
            "24.9, 25, ATTENTION",    // Just below elite minimum
            "20.0, 25, ATTENTION",    // At attention (25-5=20)
            "19.9, 25, PROBLEM",      // Below attention
            // Custom minimum 10 (low threshold)
            "10.0, 10, OPTIMAL",      // At low minimum
            "5.0, 10, ATTENTION",     // At attention (capped at 5)
            "4.9, 10, PROBLEM"        // Below attention
        )
        fun `returns correct status for PS and minimum`(
            ps: Float,
            minimum: Int,
            expected: String
        ) {
            assertThat(StatusCalculator.psStatus(ps, minimum))
                .isEqualTo(Status.valueOf(expected))
        }

        @Test
        fun `attention threshold is capped at minimum 5`() {
            // With minimum 8, attention should be max(8-5, 5) = 5
            assertThat(StatusCalculator.psStatus(5f, 8)).isEqualTo(Status.ATTENTION)
            assertThat(StatusCalculator.psStatus(4.9f, 8)).isEqualTo(Status.PROBLEM)
        }
    }

    @Nested
    @DisplayName("statusColor()")
    inner class StatusColorTests {

        @Test
        fun `OPTIMAL returns green color`() {
            assertThat(StatusCalculator.statusColor(Status.OPTIMAL))
                .isEqualTo(StatusCalculator.COLOR_OPTIMAL)
        }

        @Test
        fun `ATTENTION returns yellow color`() {
            assertThat(StatusCalculator.statusColor(Status.ATTENTION))
                .isEqualTo(StatusCalculator.COLOR_ATTENTION)
        }

        @Test
        fun `PROBLEM returns red color`() {
            assertThat(StatusCalculator.statusColor(Status.PROBLEM))
                .isEqualTo(StatusCalculator.COLOR_PROBLEM)
        }
    }

    @Nested
    @DisplayName("textColor()")
    inner class TextColorTests {

        @Test
        fun `OPTIMAL returns white color`() {
            assertThat(StatusCalculator.textColor(Status.OPTIMAL))
                .isEqualTo(StatusCalculator.COLOR_WHITE)
        }

        @Test
        fun `ATTENTION returns yellow color`() {
            assertThat(StatusCalculator.textColor(Status.ATTENTION))
                .isEqualTo(StatusCalculator.COLOR_ATTENTION)
        }

        @Test
        fun `PROBLEM returns red color`() {
            assertThat(StatusCalculator.textColor(Status.PROBLEM))
                .isEqualTo(StatusCalculator.COLOR_PROBLEM)
        }
    }

    @Nested
    @DisplayName("countIssues()")
    inner class CountIssuesTests {

        @Test
        fun `all optimal metrics returns 0 issues`() {
            val metrics = PedalingMetrics(
                balance = 50f,
                torqueEffLeft = 75f,
                torqueEffRight = 75f,
                pedalSmoothLeft = 22f,
                pedalSmoothRight = 22f
            )
            assertThat(StatusCalculator.countIssues(metrics)).isEqualTo(0)
        }

        @Test
        fun `one bad metric returns 1 issue`() {
            val metrics = PedalingMetrics(
                balance = 60f,  // Problem
                torqueEffLeft = 75f,
                torqueEffRight = 75f,
                pedalSmoothLeft = 22f,
                pedalSmoothRight = 22f
            )
            assertThat(StatusCalculator.countIssues(metrics)).isEqualTo(1)
        }

        @Test
        fun `two bad metrics returns 2 issues`() {
            val metrics = PedalingMetrics(
                balance = 60f,  // Problem
                torqueEffLeft = 50f,  // Problem (avg 50)
                torqueEffRight = 50f,
                pedalSmoothLeft = 22f,
                pedalSmoothRight = 22f
            )
            assertThat(StatusCalculator.countIssues(metrics)).isEqualTo(2)
        }

        @Test
        fun `all bad metrics returns 3 issues`() {
            val metrics = PedalingMetrics(
                balance = 60f,  // Problem
                torqueEffLeft = 50f,  // Problem
                torqueEffRight = 50f,
                pedalSmoothLeft = 5f,  // Problem
                pedalSmoothRight = 5f
            )
            assertThat(StatusCalculator.countIssues(metrics)).isEqualTo(3)
        }

        @Test
        fun `attention status counts as issue`() {
            val metrics = PedalingMetrics(
                balance = 53f,  // Attention (3% deviation)
                torqueEffLeft = 75f,
                torqueEffRight = 75f,
                pedalSmoothLeft = 22f,
                pedalSmoothRight = 22f
            )
            assertThat(StatusCalculator.countIssues(metrics)).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("allOptimal()")
    inner class AllOptimalTests {

        @Test
        fun `returns true when all metrics are optimal`() {
            val metrics = PedalingMetrics(
                balance = 50f,
                torqueEffLeft = 75f,
                torqueEffRight = 75f,
                pedalSmoothLeft = 22f,
                pedalSmoothRight = 22f
            )
            assertThat(StatusCalculator.allOptimal(metrics)).isTrue()
        }

        @Test
        fun `returns false when balance is not optimal`() {
            val metrics = PedalingMetrics(
                balance = 53f,  // Attention
                torqueEffLeft = 75f,
                torqueEffRight = 75f,
                pedalSmoothLeft = 22f,
                pedalSmoothRight = 22f
            )
            assertThat(StatusCalculator.allOptimal(metrics)).isFalse()
        }

        @Test
        fun `returns false when TE is not optimal`() {
            val metrics = PedalingMetrics(
                balance = 50f,
                torqueEffLeft = 65f,  // Attention
                torqueEffRight = 65f,
                pedalSmoothLeft = 22f,
                pedalSmoothRight = 22f
            )
            assertThat(StatusCalculator.allOptimal(metrics)).isFalse()
        }

        @Test
        fun `returns false when PS is not optimal`() {
            val metrics = PedalingMetrics(
                balance = 50f,
                torqueEffLeft = 75f,
                torqueEffRight = 75f,
                pedalSmoothLeft = 18f,  // Attention
                pedalSmoothRight = 18f
            )
            assertThat(StatusCalculator.allOptimal(metrics)).isFalse()
        }

        @Test
        fun `respects custom thresholds`() {
            val metrics = PedalingMetrics(
                balance = 55f,  // Would be attention with default, but optimal with threshold 10
                torqueEffLeft = 65f,  // Would be attention with default, but optimal with 60-70
                torqueEffRight = 65f,
                pedalSmoothLeft = 18f,  // Would be attention with default, but optimal with minimum 15
                pedalSmoothRight = 18f
            )
            assertThat(StatusCalculator.allOptimal(
                metrics,
                balanceThreshold = 10,
                teMin = 60,
                teMax = 70,
                psMinimum = 15
            )).isTrue()
        }
    }

    @Nested
    @DisplayName("Color constants")
    inner class ColorConstantsTests {

        @Test
        fun `COLOR_WHITE is white`() {
            assertThat(StatusCalculator.COLOR_WHITE).isEqualTo(0xFFFFFFFF.toInt())
        }

        @Test
        fun `COLOR_OPTIMAL is green`() {
            assertThat(StatusCalculator.COLOR_OPTIMAL).isEqualTo(0xFF4CAF50.toInt())
        }

        @Test
        fun `COLOR_ATTENTION is yellow-amber`() {
            assertThat(StatusCalculator.COLOR_ATTENTION).isEqualTo(0xFFFFC107.toInt())
        }

        @Test
        fun `COLOR_PROBLEM is red`() {
            assertThat(StatusCalculator.COLOR_PROBLEM).isEqualTo(0xFFF44336.toInt())
        }

        @Test
        fun `COLOR_DIM is gray`() {
            assertThat(StatusCalculator.COLOR_DIM).isEqualTo(0xFF666666.toInt())
        }
    }
}
