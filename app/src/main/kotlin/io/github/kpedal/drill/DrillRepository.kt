package io.github.kpedal.drill

import android.content.Context
import io.github.kpedal.data.database.DrillResultEntity
import io.github.kpedal.data.database.KPedalDatabase
import io.github.kpedal.drill.model.DrillResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

/**
 * Repository for drill results.
 */
class DrillRepository(context: Context) {

    private val dao = KPedalDatabase.getInstance(context).drillResultDao()

    /**
     * Save a drill result.
     */
    suspend fun saveResult(result: DrillResult): Long {
        val entity = result.toEntity()
        val id = dao.insert(entity)
        // Trim old results to prevent database bloat
        dao.trimResultsForDrill(result.drillId)
        return id
    }

    /**
     * Get all results (reactive).
     */
    val resultsFlow: Flow<List<DrillResult>> = dao.getAllResults().map { entities ->
        entities.map { it.toModel() }
    }

    /**
     * Get results for a specific drill (reactive).
     */
    fun getResultsForDrill(drillId: String): Flow<List<DrillResult>> {
        return dao.getResultsForDrill(drillId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Get a single result by ID.
     */
    suspend fun getResult(id: Long): DrillResult? {
        return dao.getResult(id)?.toModel()
    }

    /**
     * Get best score for a drill.
     */
    suspend fun getBestScore(drillId: String): Float? {
        return dao.getBestScore(drillId)
    }

    /**
     * Get count of completed drills for a specific drill type.
     */
    suspend fun getCompletedCount(drillId: String): Int {
        return dao.getCompletedCount(drillId)
    }

    /**
     * Get total count of all completed drills (across all drill types).
     */
    suspend fun getAllCompletedCount(): Int {
        return dao.getAllCompletedCount()
    }

    /**
     * Delete a result.
     */
    suspend fun deleteResult(id: Long) {
        dao.delete(id)
    }

    /**
     * Delete all results.
     */
    suspend fun deleteAllResults() {
        dao.deleteAll()
    }

    // Conversion functions

    private fun DrillResult.toEntity(): DrillResultEntity {
        val scoresJson = JSONArray().apply {
            phaseScores.forEach { score ->
                // Sanitize invalid float values before JSON serialization
                val sanitized = when {
                    score.isNaN() -> 0.0
                    score.isInfinite() -> if (score > 0) 100.0 else 0.0
                    else -> score.toDouble().coerceIn(0.0, 100.0)
                }
                put(sanitized)
            }
        }.toString()

        return DrillResultEntity(
            drillId = drillId,
            drillName = drillName,
            timestamp = timestamp,
            durationMs = durationMs,
            score = score,
            timeInTargetMs = timeInTargetMs,
            timeInTargetPercent = timeInTargetPercent,
            completed = completed,
            phaseScoresJson = scoresJson
        )
    }

    private fun DrillResultEntity.toModel(): DrillResult {
        val scores = try {
            val jsonArray = JSONArray(phaseScoresJson)
            (0 until jsonArray.length()).map { jsonArray.getDouble(it).toFloat() }
        } catch (e: Exception) {
            android.util.Log.w("DrillRepository", "Failed to parse phase scores JSON for drill $drillId: ${e.message}")
            emptyList()
        }

        return DrillResult(
            id = id,
            drillId = drillId,
            drillName = drillName,
            timestamp = timestamp,
            durationMs = durationMs,
            score = score,
            timeInTargetMs = timeInTargetMs,
            timeInTargetPercent = timeInTargetPercent,
            completed = completed,
            phaseScores = scores
        )
    }
}
