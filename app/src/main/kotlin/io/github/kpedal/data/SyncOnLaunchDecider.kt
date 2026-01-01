package io.github.kpedal.data

/**
 * Pure logic for deciding whether to sync on app launch.
 * Extracted for testability - no Android dependencies.
 */
object SyncOnLaunchDecider {

    /**
     * Input conditions for sync decision.
     */
    data class Conditions(
        val isLoggedIn: Boolean,
        val isNetworkAvailable: Boolean,
        val isRecording: Boolean,
        val lastSyncTimeMs: Long,
        val currentTimeMs: Long,
        val cooldownMs: Long,
        val pendingRidesCount: Int,
        val pendingDrillsCount: Int,
        val pendingAchievementsCount: Int
    )

    /**
     * Result of sync decision.
     */
    sealed class Decision {
        /** Skip sync - user not logged in */
        data object NotLoggedIn : Decision()

        /** Skip sync - no network available */
        data object Offline : Decision()

        /** Skip sync - ride is being recorded */
        data object Recording : Decision()

        /** Skip sync - cooldown period not elapsed */
        data class Cooldown(val remainingSeconds: Long) : Decision()

        /** Skip sync - nothing pending to sync */
        data object NothingPending : Decision()

        /** Proceed with sync */
        data class Sync(
            val pendingRides: Int,
            val pendingDrills: Int,
            val pendingAchievements: Int
        ) : Decision()
    }

    /**
     * Determine whether to sync based on conditions.
     *
     * @param conditions Current state conditions
     * @return Decision on whether and why to sync or skip
     */
    fun decide(conditions: Conditions): Decision {
        // Check login status
        if (!conditions.isLoggedIn) {
            return Decision.NotLoggedIn
        }

        // Check network
        if (!conditions.isNetworkAvailable) {
            return Decision.Offline
        }

        // Check if recording
        if (conditions.isRecording) {
            return Decision.Recording
        }

        // Check cooldown
        val timeSinceLastSync = conditions.currentTimeMs - conditions.lastSyncTimeMs
        if (timeSinceLastSync < conditions.cooldownMs) {
            val remainingSeconds = (conditions.cooldownMs - timeSinceLastSync) / 1000
            return Decision.Cooldown(remainingSeconds)
        }

        // Check pending count
        val totalPending = conditions.pendingRidesCount +
                conditions.pendingDrillsCount +
                conditions.pendingAchievementsCount

        if (totalPending == 0) {
            return Decision.NothingPending
        }

        // All checks passed - sync
        return Decision.Sync(
            pendingRides = conditions.pendingRidesCount,
            pendingDrills = conditions.pendingDrillsCount,
            pendingAchievements = conditions.pendingAchievementsCount
        )
    }

    /**
     * Check if decision allows sync to proceed.
     */
    fun Decision.shouldSync(): Boolean = this is Decision.Sync
}
