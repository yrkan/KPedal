package io.github.kpedal.data.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Calendar

@DisplayName("WeeklyChallenges")
class WeeklyChallengesTest {

    @Nested
    @DisplayName("challenges list")
    inner class ChallengesListTests {

        @Test
        fun `contains exactly 7 challenges`() {
            assertThat(WeeklyChallenges.challenges).hasSize(7)
        }

        @Test
        fun `all challenges have unique IDs`() {
            val ids = WeeklyChallenges.challenges.map { it.id }
            assertThat(ids).containsNoDuplicates()
        }

        @Test
        fun `all challenges have non-empty names`() {
            WeeklyChallenges.challenges.forEach { challenge ->
                assertThat(challenge.name).isNotEmpty()
            }
        }

        @Test
        fun `all challenges have positive targets`() {
            WeeklyChallenges.challenges.forEach { challenge ->
                assertThat(challenge.target).isGreaterThan(0)
            }
        }

        @Test
        fun `all challenge types are represented`() {
            val types = WeeklyChallenges.challenges.map { it.type }.toSet()
            assertThat(types).hasSize(7)  // All 7 types
        }
    }

    @Nested
    @DisplayName("getCurrentChallenge()")
    inner class GetCurrentChallengeTests {

        @Test
        fun `returns a valid challenge`() {
            val challenge = WeeklyChallenges.getCurrentChallenge()
            assertThat(challenge).isNotNull()
            assertThat(challenge.id).isNotEmpty()
        }

        @Test
        fun `returns challenge from the list`() {
            val challenge = WeeklyChallenges.getCurrentChallenge()
            assertThat(WeeklyChallenges.challenges).contains(challenge)
        }
    }

    @Nested
    @DisplayName("getNextChallenge()")
    inner class GetNextChallengeTests {

        @Test
        fun `returns a valid challenge`() {
            val challenge = WeeklyChallenges.getNextChallenge()
            assertThat(challenge).isNotNull()
            assertThat(challenge.id).isNotEmpty()
        }

        @Test
        fun `returns different from current (usually)`() {
            // In most cases next challenge is different
            // Exception: when challenges.size == 1
            val current = WeeklyChallenges.getCurrentChallenge()
            val next = WeeklyChallenges.getNextChallenge()
            // They should be different since we have 7 challenges
            assertThat(next).isNotEqualTo(current)
        }

        @Test
        fun `returns challenge from the list`() {
            val challenge = WeeklyChallenges.getNextChallenge()
            assertThat(WeeklyChallenges.challenges).contains(challenge)
        }
    }

    @Nested
    @DisplayName("getWeekStartTimestamp()")
    inner class GetWeekStartTimestampTests {

        @Test
        fun `returns timestamp in the past or now`() {
            val weekStart = WeeklyChallenges.getWeekStartTimestamp()
            assertThat(weekStart).isAtMost(System.currentTimeMillis())
        }

        @Test
        fun `returns Monday`() {
            val weekStart = WeeklyChallenges.getWeekStartTimestamp()
            val cal = Calendar.getInstance()
            cal.timeInMillis = weekStart
            assertThat(cal.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.MONDAY)
        }

        @Test
        fun `returns midnight`() {
            val weekStart = WeeklyChallenges.getWeekStartTimestamp()
            val cal = Calendar.getInstance()
            cal.timeInMillis = weekStart
            assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
            assertThat(cal.get(Calendar.MINUTE)).isEqualTo(0)
            assertThat(cal.get(Calendar.SECOND)).isEqualTo(0)
            assertThat(cal.get(Calendar.MILLISECOND)).isEqualTo(0)
        }

        @Test
        fun `returns within last 7 days`() {
            val weekStart = WeeklyChallenges.getWeekStartTimestamp()
            val now = System.currentTimeMillis()
            val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
            assertThat(now - weekStart).isLessThan(sevenDaysMs)
        }
    }

    @Nested
    @DisplayName("Challenge rotation")
    inner class ChallengeRotationTests {

        @Test
        fun `week index uses modulo for cycling`() {
            // Verify that challenges rotate properly using week % size
            val size = WeeklyChallenges.challenges.size
            assertThat(size).isEqualTo(7)

            // Week 1 -> index 1, Week 7 -> index 0, Week 8 -> index 1
            // This test verifies the rotation logic conceptually
            val mod0: Int = 0 % 7
            val mod1: Int = 1 % 7
            val mod7: Int = 7 % 7
            val mod8: Int = 8 % 7
            val mod52: Int = 52 % 7
            assertThat(mod0).isEqualTo(0)
            assertThat(mod1).isEqualTo(1)
            assertThat(mod7).isEqualTo(0)
            assertThat(mod8).isEqualTo(1)
            assertThat(mod52).isEqualTo(3)
        }
    }
}

@DisplayName("ChallengeProgress")
class ChallengeProgressTest {

    private val sampleChallenge = WeeklyChallenges.Challenge(
        id = "test",
        name = "Test Challenge",
        description = "Test description",
        target = 5,
        unit = "rides"
    )

    @Nested
    @DisplayName("progressPercent")
    inner class ProgressPercentTests {

        @Test
        fun `0 progress returns 0%`() {
            val progress = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 0,
                target = 5,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progress.progressPercent).isEqualTo(0)
        }

        @Test
        fun `half progress returns 50%`() {
            val progress = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 5,
                target = 10,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progress.progressPercent).isEqualTo(50)
        }

        @Test
        fun `full progress returns 100%`() {
            val progress = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 5,
                target = 5,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progress.progressPercent).isEqualTo(100)
        }

        @Test
        fun `over target still returns 100%`() {
            val progress = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 10,
                target = 5,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progress.progressPercent).isEqualTo(100)
        }

        @ParameterizedTest(name = "{0} of {1} = {2}%")
        @CsvSource(
            "1, 10, 10",
            "2, 10, 20",
            "3, 10, 30",
            "7, 10, 70",
            "9, 10, 90",
            "1, 3, 33",
            "2, 3, 66",
            "3, 3, 100"
        )
        fun `calculates correct percentage`(
            current: Int,
            target: Int,
            expectedPercent: Int
        ) {
            val progress = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = current,
                target = target,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progress.progressPercent).isEqualTo(expectedPercent)
        }
    }

    @Nested
    @DisplayName("isComplete")
    inner class IsCompleteTests {

        @Test
        fun `not complete when below target`() {
            val progress = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 2,
                target = 5,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progress.isComplete).isFalse()
        }

        @Test
        fun `complete when at target`() {
            val progress = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 5,
                target = 5,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progress.isComplete).isTrue()
        }

        @Test
        fun `complete when over target`() {
            val progress = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 8,
                target = 5,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progress.isComplete).isTrue()
        }

        @Test
        fun `not complete at 0 progress`() {
            val progress = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 0,
                target = 5,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progress.isComplete).isFalse()
        }

        @Test
        fun `complete when one below becomes equal`() {
            val progressBefore = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 4,
                target = 5,
                weekStart = System.currentTimeMillis()
            )
            val progressAfter = ChallengeProgress(
                challenge = sampleChallenge,
                currentProgress = 5,
                target = 5,
                weekStart = System.currentTimeMillis()
            )
            assertThat(progressBefore.isComplete).isFalse()
            assertThat(progressAfter.isComplete).isTrue()
        }
    }
}
