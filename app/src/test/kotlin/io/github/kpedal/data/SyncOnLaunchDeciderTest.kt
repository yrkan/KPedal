package io.github.kpedal.data

import com.google.common.truth.Truth.assertThat
import io.github.kpedal.data.SyncOnLaunchDecider.Conditions
import io.github.kpedal.data.SyncOnLaunchDecider.Decision
import io.github.kpedal.data.SyncOnLaunchDecider.decide
import io.github.kpedal.data.SyncOnLaunchDecider.shouldSync
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SyncOnLaunchDecider")
class SyncOnLaunchDeciderTest {

    companion object {
        private const val COOLDOWN_MS = 300_000L // 5 minutes
        private const val CURRENT_TIME = 1_000_000_000L
    }

    /**
     * Helper to create base conditions with sensible defaults.
     */
    private fun baseConditions(
        isLoggedIn: Boolean = true,
        isNetworkAvailable: Boolean = true,
        isRecording: Boolean = false,
        lastSyncTimeMs: Long = 0L,
        currentTimeMs: Long = CURRENT_TIME,
        cooldownMs: Long = COOLDOWN_MS,
        pendingRidesCount: Int = 0,
        pendingDrillsCount: Int = 0,
        pendingAchievementsCount: Int = 0
    ) = Conditions(
        isLoggedIn = isLoggedIn,
        isNetworkAvailable = isNetworkAvailable,
        isRecording = isRecording,
        lastSyncTimeMs = lastSyncTimeMs,
        currentTimeMs = currentTimeMs,
        cooldownMs = cooldownMs,
        pendingRidesCount = pendingRidesCount,
        pendingDrillsCount = pendingDrillsCount,
        pendingAchievementsCount = pendingAchievementsCount
    )

    @Nested
    @DisplayName("Skip conditions")
    inner class SkipConditionsTests {

        @Test
        fun `returns NotLoggedIn when user is not logged in`() {
            val conditions = baseConditions(isLoggedIn = false, pendingRidesCount = 5)

            val decision = decide(conditions)

            assertThat(decision).isEqualTo(Decision.NotLoggedIn)
            assertThat(decision.shouldSync()).isFalse()
        }

        @Test
        fun `returns Offline when network is not available`() {
            val conditions = baseConditions(isNetworkAvailable = false, pendingRidesCount = 5)

            val decision = decide(conditions)

            assertThat(decision).isEqualTo(Decision.Offline)
            assertThat(decision.shouldSync()).isFalse()
        }

        @Test
        fun `returns Recording when ride is being recorded`() {
            val conditions = baseConditions(isRecording = true, pendingRidesCount = 5)

            val decision = decide(conditions)

            assertThat(decision).isEqualTo(Decision.Recording)
            assertThat(decision.shouldSync()).isFalse()
        }

        @Test
        fun `returns Cooldown when cooldown period has not elapsed`() {
            val conditions = baseConditions(
                lastSyncTimeMs = CURRENT_TIME - 60_000, // 1 minute ago
                pendingRidesCount = 5
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Cooldown::class.java)
            val cooldown = decision as Decision.Cooldown
            assertThat(cooldown.remainingSeconds).isEqualTo(240) // 4 minutes remaining
            assertThat(decision.shouldSync()).isFalse()
        }

        @Test
        fun `returns NothingPending when no pending items`() {
            val conditions = baseConditions(
                pendingRidesCount = 0,
                pendingDrillsCount = 0,
                pendingAchievementsCount = 0
            )

            val decision = decide(conditions)

            assertThat(decision).isEqualTo(Decision.NothingPending)
            assertThat(decision.shouldSync()).isFalse()
        }
    }

    @Nested
    @DisplayName("Sync decision")
    inner class SyncDecisionTests {

        @Test
        fun `returns Sync when all conditions are met with pending rides`() {
            val conditions = baseConditions(pendingRidesCount = 3)

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Sync::class.java)
            val sync = decision as Decision.Sync
            assertThat(sync.pendingRides).isEqualTo(3)
            assertThat(sync.pendingDrills).isEqualTo(0)
            assertThat(sync.pendingAchievements).isEqualTo(0)
            assertThat(decision.shouldSync()).isTrue()
        }

        @Test
        fun `returns Sync when all conditions are met with pending drills`() {
            val conditions = baseConditions(pendingDrillsCount = 2)

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Sync::class.java)
            val sync = decision as Decision.Sync
            assertThat(sync.pendingRides).isEqualTo(0)
            assertThat(sync.pendingDrills).isEqualTo(2)
            assertThat(sync.pendingAchievements).isEqualTo(0)
        }

        @Test
        fun `returns Sync when all conditions are met with pending achievements`() {
            val conditions = baseConditions(pendingAchievementsCount = 5)

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Sync::class.java)
            val sync = decision as Decision.Sync
            assertThat(sync.pendingAchievements).isEqualTo(5)
        }

        @Test
        fun `returns Sync with all pending counts`() {
            val conditions = baseConditions(
                pendingRidesCount = 2,
                pendingDrillsCount = 3,
                pendingAchievementsCount = 4
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Sync::class.java)
            val sync = decision as Decision.Sync
            assertThat(sync.pendingRides).isEqualTo(2)
            assertThat(sync.pendingDrills).isEqualTo(3)
            assertThat(sync.pendingAchievements).isEqualTo(4)
        }
    }

    @Nested
    @DisplayName("Cooldown calculations")
    inner class CooldownTests {

        @Test
        fun `allows sync when cooldown has fully elapsed`() {
            val conditions = baseConditions(
                lastSyncTimeMs = CURRENT_TIME - COOLDOWN_MS - 1000, // cooldown + 1 second
                pendingRidesCount = 1
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Sync::class.java)
        }

        @Test
        fun `allows sync when cooldown exactly elapsed`() {
            val conditions = baseConditions(
                lastSyncTimeMs = CURRENT_TIME - COOLDOWN_MS,
                pendingRidesCount = 1
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Sync::class.java)
        }

        @Test
        fun `blocks sync when 1 second before cooldown expires`() {
            val conditions = baseConditions(
                lastSyncTimeMs = CURRENT_TIME - COOLDOWN_MS + 1000,
                pendingRidesCount = 1
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Cooldown::class.java)
            val cooldown = decision as Decision.Cooldown
            assertThat(cooldown.remainingSeconds).isEqualTo(1)
        }

        @Test
        fun `calculates remaining seconds correctly at halfway`() {
            val conditions = baseConditions(
                lastSyncTimeMs = CURRENT_TIME - (COOLDOWN_MS / 2),
                pendingRidesCount = 1
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Cooldown::class.java)
            val cooldown = decision as Decision.Cooldown
            assertThat(cooldown.remainingSeconds).isEqualTo(150) // 2.5 minutes = 150 seconds
        }

        @Test
        fun `allows sync on first launch when lastSyncTimeMs is 0`() {
            val conditions = baseConditions(
                lastSyncTimeMs = 0L,
                pendingRidesCount = 1
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Sync::class.java)
        }
    }

    @Nested
    @DisplayName("Priority of conditions")
    inner class PriorityTests {

        @Test
        fun `NotLoggedIn takes priority over Offline`() {
            val conditions = baseConditions(
                isLoggedIn = false,
                isNetworkAvailable = false,
                pendingRidesCount = 5
            )

            val decision = decide(conditions)

            assertThat(decision).isEqualTo(Decision.NotLoggedIn)
        }

        @Test
        fun `Offline takes priority over Recording`() {
            val conditions = baseConditions(
                isNetworkAvailable = false,
                isRecording = true,
                pendingRidesCount = 5
            )

            val decision = decide(conditions)

            assertThat(decision).isEqualTo(Decision.Offline)
        }

        @Test
        fun `Recording takes priority over Cooldown`() {
            val conditions = baseConditions(
                isRecording = true,
                lastSyncTimeMs = CURRENT_TIME - 1000, // within cooldown
                pendingRidesCount = 5
            )

            val decision = decide(conditions)

            assertThat(decision).isEqualTo(Decision.Recording)
        }

        @Test
        fun `Cooldown takes priority over NothingPending`() {
            val conditions = baseConditions(
                lastSyncTimeMs = CURRENT_TIME - 1000, // within cooldown
                pendingRidesCount = 0
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Cooldown::class.java)
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCaseTests {

        @Test
        fun `handles large pending counts`() {
            val conditions = baseConditions(
                pendingRidesCount = Int.MAX_VALUE,
                pendingDrillsCount = Int.MAX_VALUE,
                pendingAchievementsCount = Int.MAX_VALUE
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Sync::class.java)
            val sync = decision as Decision.Sync
            assertThat(sync.pendingRides).isEqualTo(Int.MAX_VALUE)
        }

        @Test
        fun `handles zero cooldown`() {
            val conditions = baseConditions(
                cooldownMs = 0L,
                lastSyncTimeMs = CURRENT_TIME,
                pendingRidesCount = 1
            )

            val decision = decide(conditions)

            assertThat(decision).isInstanceOf(Decision.Sync::class.java)
        }

        @Test
        fun `handles negative time difference gracefully`() {
            // This shouldn't happen, but let's ensure it doesn't crash
            val conditions = baseConditions(
                lastSyncTimeMs = CURRENT_TIME + 1000, // future time (clock skew)
                pendingRidesCount = 1
            )

            val decision = decide(conditions)

            // Should be blocked by cooldown since time difference is negative
            assertThat(decision).isInstanceOf(Decision.Cooldown::class.java)
        }
    }

    @Nested
    @DisplayName("Conditions data class")
    inner class ConditionsDataClassTests {

        @Test
        fun `equals works correctly`() {
            val c1 = baseConditions(pendingRidesCount = 5)
            val c2 = baseConditions(pendingRidesCount = 5)
            val c3 = baseConditions(pendingRidesCount = 3)

            assertThat(c1).isEqualTo(c2)
            assertThat(c1).isNotEqualTo(c3)
        }

        @Test
        fun `copy preserves unchanged values`() {
            val original = baseConditions(pendingRidesCount = 5)
            val copied = original.copy(isRecording = true)

            assertThat(copied.pendingRidesCount).isEqualTo(5)
            assertThat(copied.isRecording).isTrue()
        }
    }

    @Nested
    @DisplayName("Decision sealed class")
    inner class DecisionSealedClassTests {

        @Test
        fun `all decision types are distinct`() {
            val decisions = listOf(
                Decision.NotLoggedIn,
                Decision.Offline,
                Decision.Recording,
                Decision.Cooldown(60),
                Decision.NothingPending,
                Decision.Sync(1, 2, 3)
            )

            assertThat(decisions.distinct().size).isEqualTo(6)
        }

        @Test
        fun `shouldSync returns true only for Sync decision`() {
            assertThat(Decision.NotLoggedIn.shouldSync()).isFalse()
            assertThat(Decision.Offline.shouldSync()).isFalse()
            assertThat(Decision.Recording.shouldSync()).isFalse()
            assertThat(Decision.Cooldown(60).shouldSync()).isFalse()
            assertThat(Decision.NothingPending.shouldSync()).isFalse()
            assertThat(Decision.Sync(1, 0, 0).shouldSync()).isTrue()
        }

        @Test
        fun `Cooldown with same remainingSeconds are equal`() {
            val c1 = Decision.Cooldown(120)
            val c2 = Decision.Cooldown(120)
            val c3 = Decision.Cooldown(60)

            assertThat(c1).isEqualTo(c2)
            assertThat(c1).isNotEqualTo(c3)
        }

        @Test
        fun `Sync with same counts are equal`() {
            val s1 = Decision.Sync(1, 2, 3)
            val s2 = Decision.Sync(1, 2, 3)
            val s3 = Decision.Sync(1, 2, 4)

            assertThat(s1).isEqualTo(s2)
            assertThat(s1).isNotEqualTo(s3)
        }
    }
}
