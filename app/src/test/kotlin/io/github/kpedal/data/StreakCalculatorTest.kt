package io.github.kpedal.data

import com.google.common.truth.Truth.assertThat
import io.github.kpedal.data.database.RideEntity
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.Calendar

@DisplayName("StreakCalculator")
class StreakCalculatorTest {

    private fun createRide(
        daysAgo: Int = 0,
        zoneOptimal: Int = 50,
        hoursOffset: Int = 12
    ): RideEntity {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, hoursOffset)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return RideEntity(
            id = 0,
            timestamp = calendar.timeInMillis,
            durationMs = 3600000, // 1 hour
            durationFormatted = "1:00:00",
            balanceLeft = 50,
            balanceRight = 50,
            teLeft = 75,
            teRight = 75,
            psLeft = 22,
            psRight = 22,
            zoneOptimal = zoneOptimal,
            zoneAttention = 100 - zoneOptimal,
            zoneProblem = 0
        )
    }

    @Nested
    @DisplayName("Empty list")
    inner class EmptyListTests {

        @Test
        fun `empty list returns 0`() {
            assertThat(StreakCalculator.calculateStreak(emptyList())).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Single ride")
    inner class SingleRideTests {

        @Test
        fun `single ride today with good zone returns 1`() {
            val rides = listOf(createRide(daysAgo = 0, zoneOptimal = 50))
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(1)
        }

        @Test
        fun `single ride yesterday with good zone returns 1`() {
            val rides = listOf(createRide(daysAgo = 1, zoneOptimal = 50))
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(1)
        }

        @Test
        fun `single ride 2 days ago returns 0 (streak broken)`() {
            val rides = listOf(createRide(daysAgo = 2, zoneOptimal = 75))
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(0)
        }

        @Test
        fun `single ride 7 days ago returns 0`() {
            val rides = listOf(createRide(daysAgo = 7, zoneOptimal = 80))
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Zone optimal threshold")
    inner class ZoneOptimalThresholdTests {

        @Test
        fun `ride with zoneOptimal exactly 50 counts`() {
            val rides = listOf(createRide(daysAgo = 0, zoneOptimal = 50))
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(1)
        }

        @Test
        fun `ride with zoneOptimal 49 does not count`() {
            val rides = listOf(createRide(daysAgo = 0, zoneOptimal = 49))
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(0)
        }

        @Test
        fun `ride with zoneOptimal 0 does not count`() {
            val rides = listOf(createRide(daysAgo = 0, zoneOptimal = 0))
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(0)
        }

        @ParameterizedTest(name = "zoneOptimal {0}% counts as good ride")
        @ValueSource(ints = [50, 51, 60, 75, 90, 100])
        fun `ride with zoneOptimal above threshold counts`(zoneOptimal: Int) {
            val rides = listOf(createRide(daysAgo = 0, zoneOptimal = zoneOptimal))
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(1)
        }

        @ParameterizedTest(name = "zoneOptimal {0}% does not count")
        @ValueSource(ints = [0, 10, 25, 40, 49])
        fun `ride with zoneOptimal below threshold does not count`(zoneOptimal: Int) {
            val rides = listOf(createRide(daysAgo = 0, zoneOptimal = zoneOptimal))
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Consecutive days")
    inner class ConsecutiveDaysTests {

        @Test
        fun `two consecutive days returns 2`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 60),
                createRide(daysAgo = 1, zoneOptimal = 55)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(2)
        }

        @Test
        fun `three consecutive days returns 3`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 60),
                createRide(daysAgo = 1, zoneOptimal = 55),
                createRide(daysAgo = 2, zoneOptimal = 50)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(3)
        }

        @Test
        fun `seven consecutive days returns 7`() {
            val rides = (0..6).map { createRide(daysAgo = it, zoneOptimal = 50 + it) }
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(7)
        }

        @Test
        fun `30 consecutive days returns 30`() {
            val rides = (0..29).map { createRide(daysAgo = it, zoneOptimal = 50) }
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(30)
        }

        @Test
        fun `consecutive from yesterday returns correct count`() {
            val rides = listOf(
                createRide(daysAgo = 1, zoneOptimal = 60),
                createRide(daysAgo = 2, zoneOptimal = 55),
                createRide(daysAgo = 3, zoneOptimal = 50)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("Gap breaks streak")
    inner class GapBreaksStreakTests {

        @Test
        fun `gap of 1 day breaks streak`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 60),
                createRide(daysAgo = 2, zoneOptimal = 55)  // Missing day 1
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(1)
        }

        @Test
        fun `gap in middle of streak stops count`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 60),
                createRide(daysAgo = 1, zoneOptimal = 55),
                createRide(daysAgo = 3, zoneOptimal = 50),  // Missing day 2
                createRide(daysAgo = 4, zoneOptimal = 50)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(2)
        }

        @Test
        fun `bad ride day creates gap`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 60),
                createRide(daysAgo = 1, zoneOptimal = 30),  // Bad ride - doesn't count
                createRide(daysAgo = 2, zoneOptimal = 55)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(1)
        }

        @Test
        fun `gap from today returns 0`() {
            val rides = listOf(
                createRide(daysAgo = 2, zoneOptimal = 60),
                createRide(daysAgo = 3, zoneOptimal = 55),
                createRide(daysAgo = 4, zoneOptimal = 50)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Multiple rides same day")
    inner class MultipleRidesSameDayTests {

        @Test
        fun `multiple good rides same day counts as 1 day`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 60, hoursOffset = 8),
                createRide(daysAgo = 0, zoneOptimal = 55, hoursOffset = 12),
                createRide(daysAgo = 0, zoneOptimal = 70, hoursOffset = 18)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(1)
        }

        @Test
        fun `one good ride among bad rides on same day counts`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 30, hoursOffset = 8),
                createRide(daysAgo = 0, zoneOptimal = 60, hoursOffset = 12),  // Good one
                createRide(daysAgo = 0, zoneOptimal = 40, hoursOffset = 18)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(1)
        }

        @Test
        fun `all bad rides same day returns 0`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 30, hoursOffset = 8),
                createRide(daysAgo = 0, zoneOptimal = 40, hoursOffset = 12),
                createRide(daysAgo = 0, zoneOptimal = 45, hoursOffset = 18)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(0)
        }

        @Test
        fun `multiple rides across consecutive days`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 60, hoursOffset = 8),
                createRide(daysAgo = 0, zoneOptimal = 55, hoursOffset = 18),
                createRide(daysAgo = 1, zoneOptimal = 70, hoursOffset = 10),
                createRide(daysAgo = 1, zoneOptimal = 65, hoursOffset = 16),
                createRide(daysAgo = 2, zoneOptimal = 50, hoursOffset = 12)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCaseTests {

        @Test
        fun `rides not in order still work`() {
            val rides = listOf(
                createRide(daysAgo = 2, zoneOptimal = 50),
                createRide(daysAgo = 0, zoneOptimal = 60),
                createRide(daysAgo = 1, zoneOptimal = 55)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(3)
        }

        @Test
        fun `exactly at boundary zoneOptimal 50 counts`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 50),
                createRide(daysAgo = 1, zoneOptimal = 50)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(2)
        }

        @Test
        fun `mix of good and exact boundary rides`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 100),
                createRide(daysAgo = 1, zoneOptimal = 50),
                createRide(daysAgo = 2, zoneOptimal = 75),
                createRide(daysAgo = 3, zoneOptimal = 50)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(4)
        }

        @Test
        fun `ride at midnight edge`() {
            val rides = listOf(
                createRide(daysAgo = 0, zoneOptimal = 60, hoursOffset = 0),
                createRide(daysAgo = 1, zoneOptimal = 55, hoursOffset = 23)
            )
            assertThat(StreakCalculator.calculateStreak(rides)).isEqualTo(2)
        }
    }
}
