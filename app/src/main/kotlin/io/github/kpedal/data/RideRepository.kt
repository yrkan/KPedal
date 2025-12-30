package io.github.kpedal.data

import android.content.Context
import io.github.kpedal.data.database.KPedalDatabase
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.engine.StatusCalculator
import io.github.kpedal.ui.screens.LiveRideData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for ride history persistence.
 * Follows the existing PreferencesRepository pattern.
 */
class RideRepository(context: Context) {

    companion object {
        private const val MAX_RIDES = 100
    }

    private val database = KPedalDatabase.getInstance(context)
    private val rideDao = database.rideDao()

    /**
     * Flow of all rides for reactive UI.
     */
    val ridesFlow: Flow<List<RideEntity>> = rideDao.getAllRidesFlow()

    /**
     * Save ride data.
     * Automatically enforces 100 ride limit.
     *
     * @param liveData The live ride data to save
     * @param durationMs Duration in milliseconds for accurate storage
     * @param savedManually True if saved via manual button
     * @return The saved RideEntity with ID
     */
    suspend fun saveRide(
        liveData: LiveRideData,
        durationMs: Long,
        savedManually: Boolean = false
    ): RideEntity = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()

        // Calculate unified score using StatusCalculator
        val score = StatusCalculator.calculateOverallScore(
            balanceRight = liveData.balanceRight,
            teLeft = liveData.teLeft,
            teRight = liveData.teRight,
            psLeft = liveData.psLeft,
            psRight = liveData.psRight,
            zoneOptimal = liveData.zoneOptimal
        )

        val entity = RideEntity(
            timestamp = timestamp,
            durationMs = durationMs,
            durationFormatted = liveData.duration,
            balanceLeft = liveData.balanceLeft,
            balanceRight = liveData.balanceRight,
            teLeft = liveData.teLeft,
            teRight = liveData.teRight,
            psLeft = liveData.psLeft,
            psRight = liveData.psRight,
            zoneOptimal = liveData.zoneOptimal,
            zoneAttention = liveData.zoneAttention,
            zoneProblem = liveData.zoneProblem,
            savedManually = savedManually,
            // Extended metrics
            powerAvg = liveData.powerAvg,
            powerMax = liveData.powerMax,
            cadenceAvg = liveData.cadenceAvg,
            heartRateAvg = liveData.heartRateAvg,
            heartRateMax = liveData.heartRateMax,
            speedAvgKmh = liveData.speedAvgKmh,
            distanceKm = liveData.distanceKm,
            // Pro cyclist metrics
            elevationGain = liveData.elevationGain,
            elevationLoss = liveData.elevationLoss,
            gradeAvg = liveData.gradeAvg,
            gradeMax = liveData.gradeMax,
            normalizedPower = liveData.normalizedPower,
            energyKj = liveData.energyKj,
            // Unified score
            score = score
        )

        val id = rideDao.insertRide(entity)

        // Enforce 100 ride limit
        enforceRideLimit()

        // Return entity with assigned ID
        entity.copy(id = id)
    }

    /**
     * Delete a ride by ID.
     */
    suspend fun deleteRide(id: Long) = withContext(Dispatchers.IO) {
        rideDao.deleteRideById(id)
    }

    /**
     * Get a single ride by ID.
     */
    suspend fun getRide(id: Long): RideEntity? = withContext(Dispatchers.IO) {
        rideDao.getRideById(id)
    }

    /**
     * Get all rides (one-time read).
     */
    suspend fun getAllRides(): List<RideEntity> = withContext(Dispatchers.IO) {
        rideDao.getAllRides()
    }

    /**
     * Get current ride count.
     */
    suspend fun getRideCount(): Int = withContext(Dispatchers.IO) {
        rideDao.getRideCount()
    }

    /**
     * Enforce the 100 ride limit by deleting oldest rides.
     */
    private suspend fun enforceRideLimit() {
        rideDao.deleteOldestBeyondLimit(MAX_RIDES)
    }

    // ========== Dashboard Data ==========

    /**
     * Flow of the most recent ride.
     */
    val lastRideFlow: Flow<RideEntity?> = rideDao.getLastRideFlow()

    /**
     * Flow of total ride count.
     */
    val totalRideCountFlow: Flow<Int> = rideDao.getTotalRideCountFlow()

    /**
     * Flow of average balance across all rides.
     */
    val averageBalanceFlow: Flow<Float?> = rideDao.getAverageBalanceFlow()

    /**
     * Get rides since a timestamp (for streak calculation).
     */
    suspend fun getRidesSince(sinceMs: Long): List<RideEntity> = withContext(Dispatchers.IO) {
        rideDao.getRidesSince(sinceMs)
    }

    /**
     * Get rides in a date range (for analytics).
     */
    suspend fun getRidesInRange(startMs: Long, endMs: Long): List<RideEntity> = withContext(Dispatchers.IO) {
        rideDao.getRidesInRange(startMs, endMs)
    }

    // ========== Rating ==========

    /**
     * Update a ride's rating.
     */
    suspend fun updateRating(id: Long, rating: Int) = withContext(Dispatchers.IO) {
        rideDao.updateRating(id, rating)
    }

    /**
     * Update a ride's notes.
     */
    suspend fun updateNotes(id: Long, notes: String) = withContext(Dispatchers.IO) {
        rideDao.updateNotes(id, notes)
    }
}
