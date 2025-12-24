package io.github.kpedal.data

import android.content.Context
import io.github.kpedal.data.database.AchievementEntity
import io.github.kpedal.data.database.KPedalDatabase
import io.github.kpedal.data.models.Achievement
import io.github.kpedal.data.models.UnlockedAchievement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for achievements.
 */
class AchievementRepository(context: Context) {

    private val database = KPedalDatabase.getInstance(context)
    private val achievementDao = database.achievementDao()

    /**
     * Flow of all unlocked achievements.
     */
    val unlockedAchievementsFlow: Flow<List<UnlockedAchievement>> = achievementDao.getAllAchievementsFlow()
        .map { entities ->
            entities.mapNotNull { entity ->
                Achievement.getById(entity.id)?.let { achievement ->
                    UnlockedAchievement(
                        achievement = achievement,
                        unlockedAt = entity.unlockedAt,
                        progress = entity.progress
                    )
                }
            }
        }

    /**
     * Flow of unlocked achievement count.
     */
    val unlockedCountFlow: Flow<Int> = achievementDao.getUnlockedCountFlow()

    /**
     * Check if an achievement is unlocked.
     */
    suspend fun isUnlocked(achievementId: String): Boolean = withContext(Dispatchers.IO) {
        achievementDao.getAchievement(achievementId) != null
    }

    /**
     * Unlock an achievement.
     * Returns true if newly unlocked, false if already unlocked.
     * Thread-safe: uses INSERT OR IGNORE and checks return value.
     */
    suspend fun unlock(achievement: Achievement): Boolean = withContext(Dispatchers.IO) {
        val rowId = achievementDao.insert(
            AchievementEntity(
                id = achievement.id,
                unlockedAt = System.currentTimeMillis()
            )
        )
        // rowId > 0 means inserted, -1 means already exists (IGNORE)
        rowId > 0
    }

    /**
     * Get all unlocked achievement IDs.
     */
    suspend fun getUnlockedIds(): Set<String> = withContext(Dispatchers.IO) {
        achievementDao.getAllAchievements().map { it.id }.toSet()
    }
}
