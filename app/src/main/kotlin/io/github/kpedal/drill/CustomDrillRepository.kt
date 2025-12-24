package io.github.kpedal.drill

import android.content.Context
import io.github.kpedal.data.database.CustomDrillEntity
import io.github.kpedal.data.database.KPedalDatabase
import io.github.kpedal.drill.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for custom drills.
 * Handles conversion between database entities and drill models.
 */
class CustomDrillRepository(context: Context) {

    private val dao = KPedalDatabase.getInstance(context).customDrillDao()

    /**
     * All custom drills as Drill models.
     */
    val drillsFlow: Flow<List<Drill>> = dao.getAllDrills().map { entities ->
        entities.map { it.toDrill() }
    }

    /**
     * All custom drill entities (for editing).
     */
    val entitiesFlow: Flow<List<CustomDrillEntity>> = dao.getAllDrills()

    /**
     * Save a new custom drill or update existing.
     */
    suspend fun save(entity: CustomDrillEntity): Long {
        return dao.insert(entity)
    }

    /**
     * Get a drill entity by ID.
     */
    suspend fun getEntity(id: Long): CustomDrillEntity? {
        return dao.getDrill(id)
    }

    /**
     * Delete a custom drill.
     */
    suspend fun delete(id: Long) {
        dao.delete(id)
    }

    /**
     * Get count of custom drills.
     */
    suspend fun getCount(): Int {
        return dao.getCount()
    }

    /**
     * Convert entity to Drill model for execution.
     */
    private fun CustomDrillEntity.toDrill(): Drill {
        val drillMetric = when (metric) {
            "BALANCE" -> DrillMetric.BALANCE
            "TE" -> DrillMetric.TORQUE_EFFECTIVENESS
            "PS" -> DrillMetric.PEDAL_SMOOTHNESS
            else -> DrillMetric.BALANCE
        }

        val target = createTarget(drillMetric)

        val phase = DrillPhase(
            name = "Focus",
            description = description,
            durationMs = durationSeconds * 1000L,
            target = target,
            instruction = "Maintain target ${drillMetric.name.lowercase().replace("_", " ")}"
        )

        return Drill(
            id = "custom_$id",
            name = name,
            description = description,
            type = DrillType.TARGET_BASED,
            metric = drillMetric,
            difficulty = DrillDifficulty.INTERMEDIATE,
            phases = listOf(
                DrillPhase(
                    name = "Prepare",
                    description = "Get ready",
                    durationMs = 5_000L,
                    instruction = "Prepare for the drill"
                ),
                phase,
                DrillPhase(
                    name = "Recovery",
                    description = "Cool down",
                    durationMs = 5_000L,
                    instruction = "Good work! Spin easy"
                )
            ),
            tips = listOf("Custom drill: $name")
        )
    }

    private fun CustomDrillEntity.createTarget(metric: DrillMetric): DrillTarget {
        // Default values based on metric if null
        val defaultMin = when (metric) {
            DrillMetric.BALANCE -> 48f
            DrillMetric.TORQUE_EFFECTIVENESS -> 70f
            DrillMetric.PEDAL_SMOOTHNESS -> 20f
            DrillMetric.COMBINED -> 50f
        }
        val defaultMax = when (metric) {
            DrillMetric.BALANCE -> 52f
            DrillMetric.TORQUE_EFFECTIVENESS -> 80f
            DrillMetric.PEDAL_SMOOTHNESS -> 30f
            DrillMetric.COMBINED -> 50f
        }

        return when (targetType) {
            "MIN" -> DrillTarget(
                metric = metric,
                minValue = targetMin ?: defaultMin
            )
            "MAX" -> DrillTarget(
                metric = metric,
                maxValue = targetMax ?: defaultMax
            )
            "RANGE" -> DrillTarget(
                metric = metric,
                minValue = targetMin ?: defaultMin,
                maxValue = targetMax ?: defaultMax
            )
            "EXACT" -> DrillTarget(
                metric = metric,
                targetValue = targetValue ?: ((defaultMin + defaultMax) / 2),
                tolerance = tolerance
            )
            else -> DrillTarget(
                metric = metric,
                minValue = targetMin ?: defaultMin,
                maxValue = targetMax ?: defaultMax
            )
        }
    }
}
