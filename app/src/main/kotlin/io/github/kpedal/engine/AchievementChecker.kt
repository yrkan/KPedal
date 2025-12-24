package io.github.kpedal.engine

import io.github.kpedal.data.AchievementRepository
import io.github.kpedal.data.RideRepository
import io.github.kpedal.data.StreakCalculator
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.data.models.Achievement
import io.github.kpedal.drill.DrillRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Checks and unlocks achievements based on user progress.
 */
class AchievementChecker(
    private val achievementRepository: AchievementRepository,
    private val rideRepository: RideRepository,
    private val drillRepository: DrillRepository
) {
    /**
     * Check all achievements after a ride is saved.
     * Returns list of newly unlocked achievements.
     */
    suspend fun checkAfterRide(ride: RideEntity): List<Achievement> = withContext(Dispatchers.IO) {
        val newlyUnlocked = mutableListOf<Achievement>()

        // Helper to check and unlock - unlock() returns true only if newly unlocked (INSERT OR IGNORE)
        suspend fun tryUnlock(achievement: Achievement, condition: Boolean) {
            if (condition && achievementRepository.unlock(achievement)) {
                newlyUnlocked.add(achievement)
            }
        }

        // Get all rides for counting
        val allRides = rideRepository.getAllRides()
        val rideCount = allRides.size

        // Check ride count achievements
        tryUnlock(Achievement.FirstRide, rideCount >= 1)
        tryUnlock(Achievement.TenRides, rideCount >= 10)
        tryUnlock(Achievement.FiftyRides, rideCount >= 50)
        tryUnlock(Achievement.HundredRides, rideCount >= 100)

        // Check balance achievements (based on zone optimal time)
        val optimalMinutes = (ride.durationMs / 60000.0) * (ride.zoneOptimal / 100.0)
        tryUnlock(Achievement.PerfectBalance1m, optimalMinutes >= 1)
        tryUnlock(Achievement.PerfectBalance5m, optimalMinutes >= 5)
        tryUnlock(Achievement.PerfectBalance10m, optimalMinutes >= 10)

        // Check efficiency (TE and PS both good)
        val teAvg = (ride.teLeft + ride.teRight) / 2
        val psAvg = (ride.psLeft + ride.psRight) / 2
        tryUnlock(Achievement.EfficiencyMaster, teAvg in 70..80 && psAvg >= 20 && optimalMinutes >= 5)

        // Smooth operator - PS >= 25 for 10 minutes
        tryUnlock(Achievement.SmoothOperator, psAvg >= 25 && (ride.durationMs / 60000.0) >= 10)

        // Check streak achievements
        val streak = StreakCalculator.calculateStreak(allRides)
        tryUnlock(Achievement.ThreeDayStreak, streak >= 3)
        tryUnlock(Achievement.SevenDayStreak, streak >= 7)
        tryUnlock(Achievement.FourteenDayStreak, streak >= 14)
        tryUnlock(Achievement.ThirtyDayStreak, streak >= 30)

        newlyUnlocked
    }

    /**
     * Check drill-related achievements after a drill is completed.
     * Returns list of newly unlocked achievements.
     */
    suspend fun checkAfterDrill(score: Float): List<Achievement> = withContext(Dispatchers.IO) {
        val newlyUnlocked = mutableListOf<Achievement>()

        // Helper to check and unlock
        suspend fun tryUnlock(achievement: Achievement, condition: Boolean) {
            if (condition && achievementRepository.unlock(achievement)) {
                newlyUnlocked.add(achievement)
            }
        }

        // Get drill count
        val completedCount = drillRepository.getAllCompletedCount()

        tryUnlock(Achievement.FirstDrill, completedCount >= 1)
        tryUnlock(Achievement.TenDrills, completedCount >= 10)
        tryUnlock(Achievement.PerfectDrill, score >= 90f)

        newlyUnlocked
    }
}
