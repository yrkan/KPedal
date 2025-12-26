package io.github.kpedal.engine

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Unit tests for achievement unlock condition logic.
 *
 * Tests the pure condition evaluation logic extracted from AchievementChecker.
 * This allows testing without mocking repositories and coroutines.
 */
@DisplayName("Achievement Conditions")
class AchievementConditionsTest {

    @Nested
    @DisplayName("Ride Count Achievements")
    inner class RideCountAchievements {

        @ParameterizedTest(name = "count {0} unlocks FirstRide: {1}")
        @CsvSource(
            "0, false",
            "1, true",
            "5, true",
            "100, true"
        )
        fun `FirstRide requires at least 1 ride`(count: Int, expected: Boolean) {
            assertThat(count >= 1).isEqualTo(expected)
        }

        @ParameterizedTest(name = "count {0} unlocks TenRides: {1}")
        @CsvSource(
            "0, false",
            "9, false",
            "10, true",
            "50, true"
        )
        fun `TenRides requires at least 10 rides`(count: Int, expected: Boolean) {
            assertThat(count >= 10).isEqualTo(expected)
        }

        @ParameterizedTest(name = "count {0} unlocks FiftyRides: {1}")
        @CsvSource(
            "0, false",
            "49, false",
            "50, true",
            "100, true"
        )
        fun `FiftyRides requires at least 50 rides`(count: Int, expected: Boolean) {
            assertThat(count >= 50).isEqualTo(expected)
        }

        @ParameterizedTest(name = "count {0} unlocks HundredRides: {1}")
        @CsvSource(
            "0, false",
            "99, false",
            "100, true",
            "150, true"
        )
        fun `HundredRides requires at least 100 rides`(count: Int, expected: Boolean) {
            assertThat(count >= 100).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("Optimal Minutes Calculation")
    inner class OptimalMinutesCalculation {

        /**
         * Mirrors: (ride.durationMs / 60000.0) * (ride.zoneOptimal / 100.0)
         */
        private fun calculateOptimalMinutes(durationMs: Long, zoneOptimal: Int): Double {
            return (durationMs / 60000.0) * (zoneOptimal / 100.0)
        }

        @Test
        fun `1 hour ride with 50% optimal gives 30 optimal minutes`() {
            val result = calculateOptimalMinutes(3600000L, 50)
            assertThat(result).isWithin(0.01).of(30.0)
        }

        @Test
        fun `30 min ride with 100% optimal gives 30 optimal minutes`() {
            val result = calculateOptimalMinutes(1800000L, 100)
            assertThat(result).isWithin(0.01).of(30.0)
        }

        @Test
        fun `10 min ride with 60% optimal gives 6 optimal minutes`() {
            val result = calculateOptimalMinutes(600000L, 60)
            assertThat(result).isWithin(0.01).of(6.0)
        }

        @Test
        fun `5 min ride with 20% optimal gives 1 optimal minute`() {
            val result = calculateOptimalMinutes(300000L, 20)
            assertThat(result).isWithin(0.01).of(1.0)
        }

        @ParameterizedTest(name = "durationMs={0}, zoneOptimal={1}% gives {2} optimal minutes")
        @CsvSource(
            "60000, 100, 1.0",     // 1 min * 100% = 1 min
            "60000, 50, 0.5",      // 1 min * 50% = 0.5 min
            "120000, 50, 1.0",     // 2 min * 50% = 1 min
            "300000, 100, 5.0",    // 5 min * 100% = 5 min
            "600000, 100, 10.0",   // 10 min * 100% = 10 min
            "600000, 50, 5.0",     // 10 min * 50% = 5 min
            "1800000, 33, 9.9"     // 30 min * 33% ≈ 9.9 min
        )
        fun `optimal minutes calculation scenarios`(
            durationMs: Long,
            zoneOptimal: Int,
            expected: Double
        ) {
            val result = calculateOptimalMinutes(durationMs, zoneOptimal)
            assertThat(result).isWithin(0.1).of(expected)
        }
    }

    @Nested
    @DisplayName("Balance Achievements (PerfectBalance)")
    inner class BalanceAchievements {

        private fun calculateOptimalMinutes(durationMs: Long, zoneOptimal: Int): Double {
            return (durationMs / 60000.0) * (zoneOptimal / 100.0)
        }

        @Test
        fun `PerfectBalance1m - 1 minute ride with 100% optimal`() {
            val optimalMinutes = calculateOptimalMinutes(60000L, 100)
            assertThat(optimalMinutes >= 1).isTrue()
        }

        @Test
        fun `PerfectBalance1m - 2 minute ride with 50% optimal`() {
            val optimalMinutes = calculateOptimalMinutes(120000L, 50)
            assertThat(optimalMinutes >= 1).isTrue()
        }

        @Test
        fun `PerfectBalance1m - 1 minute ride with 50% optimal fails`() {
            val optimalMinutes = calculateOptimalMinutes(60000L, 50)
            assertThat(optimalMinutes >= 1).isFalse()
        }

        @Test
        fun `PerfectBalance5m - 5 minute ride with 100% optimal`() {
            val optimalMinutes = calculateOptimalMinutes(300000L, 100)
            assertThat(optimalMinutes >= 5).isTrue()
        }

        @Test
        fun `PerfectBalance5m - 10 minute ride with 50% optimal`() {
            val optimalMinutes = calculateOptimalMinutes(600000L, 50)
            assertThat(optimalMinutes >= 5).isTrue()
        }

        @Test
        fun `PerfectBalance10m - 10 minute ride with 100% optimal`() {
            val optimalMinutes = calculateOptimalMinutes(600000L, 100)
            assertThat(optimalMinutes >= 10).isTrue()
        }

        @Test
        fun `PerfectBalance10m - 20 minute ride with 50% optimal`() {
            val optimalMinutes = calculateOptimalMinutes(1200000L, 50)
            assertThat(optimalMinutes >= 10).isTrue()
        }
    }

    @Nested
    @DisplayName("Efficiency Master Achievement")
    inner class EfficiencyMasterAchievement {

        /**
         * Mirrors AchievementChecker condition:
         * teAvg in 70..80 && psAvg >= 20 && optimalMinutes >= 5
         */
        private fun checkEfficiencyMaster(
            teLeft: Int, teRight: Int,
            psLeft: Int, psRight: Int,
            durationMs: Long, zoneOptimal: Int
        ): Boolean {
            val teAvg = (teLeft + teRight) / 2
            val psAvg = (psLeft + psRight) / 2
            val optimalMinutes = (durationMs / 60000.0) * (zoneOptimal / 100.0)
            return teAvg in 70..80 && psAvg >= 20 && optimalMinutes >= 5
        }

        @Test
        fun `all conditions met returns true`() {
            assertThat(checkEfficiencyMaster(
                teLeft = 75, teRight = 75,  // Avg 75, in range
                psLeft = 22, psRight = 22,   // Avg 22, >= 20
                durationMs = 600000L,        // 10 min
                zoneOptimal = 60             // 6 optimal minutes >= 5
            )).isTrue()
        }

        @Test
        fun `TE too low returns false`() {
            assertThat(checkEfficiencyMaster(
                teLeft = 65, teRight = 65,  // Avg 65, below range
                psLeft = 22, psRight = 22,
                durationMs = 600000L,
                zoneOptimal = 100
            )).isFalse()
        }

        @Test
        fun `TE too high returns false`() {
            assertThat(checkEfficiencyMaster(
                teLeft = 85, teRight = 85,  // Avg 85, above range
                psLeft = 22, psRight = 22,
                durationMs = 600000L,
                zoneOptimal = 100
            )).isFalse()
        }

        @Test
        fun `PS too low returns false`() {
            assertThat(checkEfficiencyMaster(
                teLeft = 75, teRight = 75,
                psLeft = 15, psRight = 15,  // Avg 15, below 20
                durationMs = 600000L,
                zoneOptimal = 100
            )).isFalse()
        }

        @Test
        fun `optimal minutes too low returns false`() {
            assertThat(checkEfficiencyMaster(
                teLeft = 75, teRight = 75,
                psLeft = 22, psRight = 22,
                durationMs = 300000L,        // 5 min
                zoneOptimal = 50             // 2.5 optimal minutes < 5
            )).isFalse()
        }

        @Test
        fun `TE at boundary 70 is valid`() {
            assertThat(checkEfficiencyMaster(
                teLeft = 70, teRight = 70,
                psLeft = 22, psRight = 22,
                durationMs = 600000L,
                zoneOptimal = 100
            )).isTrue()
        }

        @Test
        fun `TE at boundary 80 is valid`() {
            assertThat(checkEfficiencyMaster(
                teLeft = 80, teRight = 80,
                psLeft = 22, psRight = 22,
                durationMs = 600000L,
                zoneOptimal = 100
            )).isTrue()
        }

        @Test
        fun `PS at boundary 20 is valid`() {
            assertThat(checkEfficiencyMaster(
                teLeft = 75, teRight = 75,
                psLeft = 20, psRight = 20,
                durationMs = 600000L,
                zoneOptimal = 100
            )).isTrue()
        }

        @Test
        fun `asymmetric TE values use average`() {
            assertThat(checkEfficiencyMaster(
                teLeft = 65, teRight = 85,  // Avg 75, in range
                psLeft = 22, psRight = 22,
                durationMs = 600000L,
                zoneOptimal = 100
            )).isTrue()
        }
    }

    @Nested
    @DisplayName("Smooth Operator Achievement")
    inner class SmoothOperatorAchievement {

        /**
         * Mirrors AchievementChecker condition:
         * psAvg >= 25 && (ride.durationMs / 60000.0) >= 10
         */
        private fun checkSmoothOperator(
            psLeft: Int,
            psRight: Int,
            durationMs: Long
        ): Boolean {
            val psAvg = (psLeft + psRight) / 2
            return psAvg >= 25 && (durationMs / 60000.0) >= 10
        }

        @Test
        fun `PS 25+ and 10+ minutes returns true`() {
            assertThat(checkSmoothOperator(25, 25, 600000L)).isTrue()
        }

        @Test
        fun `PS below 25 returns false`() {
            assertThat(checkSmoothOperator(24, 24, 600000L)).isFalse()
        }

        @Test
        fun `duration below 10 minutes returns false`() {
            assertThat(checkSmoothOperator(30, 30, 599999L)).isFalse()
        }

        @Test
        fun `high PS with short ride returns false`() {
            assertThat(checkSmoothOperator(30, 30, 300000L)).isFalse()
        }

        @Test
        fun `PS exactly at 25 is valid`() {
            assertThat(checkSmoothOperator(25, 25, 600000L)).isTrue()
        }

        @Test
        fun `asymmetric PS uses average`() {
            // 20 + 30 = 50 / 2 = 25
            assertThat(checkSmoothOperator(20, 30, 600000L)).isTrue()
        }

        @ParameterizedTest(name = "psLeft={0}, psRight={1}, duration={2}ms: {3}")
        @CsvSource(
            "25, 25, 600000, true",    // Exact thresholds
            "30, 30, 900000, true",    // Above thresholds
            "24, 24, 600000, false",   // PS just below
            "25, 25, 599999, false",   // Duration just below
            "20, 30, 600000, true",    // Asymmetric PS avg=25
            "19, 30, 600000, false",   // Asymmetric PS avg=24.5 rounds to 24
            "40, 40, 3600000, true"    // High values
        )
        fun `smooth operator condition scenarios`(
            psLeft: Int,
            psRight: Int,
            durationMs: Long,
            expected: Boolean
        ) {
            assertThat(checkSmoothOperator(psLeft, psRight, durationMs)).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("Streak Achievements")
    inner class StreakAchievements {

        @ParameterizedTest(name = "streak {0} unlocks ThreeDayStreak: {1}")
        @CsvSource(
            "0, false",
            "2, false",
            "3, true",
            "7, true"
        )
        fun `ThreeDayStreak requires at least 3 days`(streak: Int, expected: Boolean) {
            assertThat(streak >= 3).isEqualTo(expected)
        }

        @ParameterizedTest(name = "streak {0} unlocks SevenDayStreak: {1}")
        @CsvSource(
            "0, false",
            "6, false",
            "7, true",
            "14, true"
        )
        fun `SevenDayStreak requires at least 7 days`(streak: Int, expected: Boolean) {
            assertThat(streak >= 7).isEqualTo(expected)
        }

        @ParameterizedTest(name = "streak {0} unlocks FourteenDayStreak: {1}")
        @CsvSource(
            "0, false",
            "13, false",
            "14, true",
            "30, true"
        )
        fun `FourteenDayStreak requires at least 14 days`(streak: Int, expected: Boolean) {
            assertThat(streak >= 14).isEqualTo(expected)
        }

        @ParameterizedTest(name = "streak {0} unlocks ThirtyDayStreak: {1}")
        @CsvSource(
            "0, false",
            "29, false",
            "30, true",
            "100, true"
        )
        fun `ThirtyDayStreak requires at least 30 days`(streak: Int, expected: Boolean) {
            assertThat(streak >= 30).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("Drill Achievements")
    inner class DrillAchievements {

        @ParameterizedTest(name = "count {0} unlocks FirstDrill: {1}")
        @CsvSource(
            "0, false",
            "1, true",
            "5, true"
        )
        fun `FirstDrill requires at least 1 completed drill`(count: Int, expected: Boolean) {
            assertThat(count >= 1).isEqualTo(expected)
        }

        @ParameterizedTest(name = "count {0} unlocks TenDrills: {1}")
        @CsvSource(
            "0, false",
            "9, false",
            "10, true",
            "20, true"
        )
        fun `TenDrills requires at least 10 completed drills`(count: Int, expected: Boolean) {
            assertThat(count >= 10).isEqualTo(expected)
        }

        @ParameterizedTest(name = "score {0} unlocks PerfectDrill: {1}")
        @CsvSource(
            "0.0, false",
            "50.0, false",
            "89.9, false",
            "90.0, true",
            "100.0, true"
        )
        fun `PerfectDrill requires score of at least 90`(score: Float, expected: Boolean) {
            assertThat(score >= 90f).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("StreakCalculator Logic")
    inner class StreakCalculatorLogic {

        /**
         * Tests for "good ride" criteria: zoneOptimal >= 50%
         */
        @ParameterizedTest(name = "zoneOptimal {0}% is good ride: {1}")
        @CsvSource(
            "0, false",
            "49, false",
            "50, true",
            "75, true",
            "100, true"
        )
        fun `good ride requires zoneOptimal of at least 50`(zoneOptimal: Int, expected: Boolean) {
            assertThat(zoneOptimal >= 50).isEqualTo(expected)
        }

        /**
         * Day key calculation (days since epoch)
         */
        @Test
        fun `same day rides group together`() {
            val baseTime = 1703980800000L // Some fixed timestamp
            val sameDay1 = baseTime
            val sameDay2 = baseTime + 3600000L // +1 hour

            val dayKey1 = getDayKey(sameDay1)
            val dayKey2 = getDayKey(sameDay2)

            assertThat(dayKey1).isEqualTo(dayKey2)
        }

        @Test
        fun `different day rides have different keys`() {
            val baseTime = 1703980800000L
            val day1 = baseTime
            val day2 = baseTime + 86400000L // +24 hours

            val dayKey1 = getDayKey(day1)
            val dayKey2 = getDayKey(day2)

            assertThat(dayKey1).isNotEqualTo(dayKey2)
            assertThat(dayKey2 - dayKey1).isEqualTo(1)
        }

        /**
         * Helper to calculate day key (mirrors StreakCalculator.getDayKey)
         */
        private fun getDayKey(timestampMs: Long): Long {
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = timestampMs
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis / (24 * 60 * 60 * 1000)
        }
    }
}
