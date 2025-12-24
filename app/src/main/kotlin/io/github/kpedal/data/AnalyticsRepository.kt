package io.github.kpedal.data

import android.content.Context
import io.github.kpedal.data.database.KPedalDatabase
import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.data.models.TrendData
import io.github.kpedal.ui.components.charts.TrendPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for analytics and trend calculations.
 */
class AnalyticsRepository(context: Context) {

    private val database = KPedalDatabase.getInstance(context)
    private val rideDao = database.rideDao()

    /**
     * Calculate trend data for a given period.
     */
    suspend fun getTrendData(period: TrendData.Period): TrendData = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val startMs = now - (period.days * 24 * 60 * 60 * 1000L)
        val rides = rideDao.getRidesInRange(startMs, now)

        if (rides.isEmpty()) {
            return@withContext TrendData(period = period)
        }

        // Group rides by day
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val ridesByDay = rides.groupBy { ride ->
            getDayStart(ride.timestamp)
        }.toSortedMap()

        // Calculate daily averages for trends
        val balanceTrend = mutableListOf<TrendPoint>()
        val teTrend = mutableListOf<TrendPoint>()
        val psTrend = mutableListOf<TrendPoint>()

        ridesByDay.forEach { (dayMs, dayRides) ->
            val label = dateFormat.format(Date(dayMs))
            val avgBalance = dayRides.map { it.balanceRight.toFloat() }.average().toFloat()
            val avgTE = dayRides.map { (it.teLeft + it.teRight) / 2f }.average().toFloat()
            val avgPS = dayRides.map { (it.psLeft + it.psRight) / 2f }.average().toFloat()

            balanceTrend.add(TrendPoint(avgBalance, label))
            teTrend.add(TrendPoint(avgTE, label))
            psTrend.add(TrendPoint(avgPS, label))
        }

        // Overall averages
        val avgBalance = rides.map { it.balanceRight.toFloat() }.average().toFloat()
        val avgTE = rides.map { (it.teLeft + it.teRight) / 2f }.average().toFloat()
        val avgPS = rides.map { (it.psLeft + it.psRight) / 2f }.average().toFloat()

        // Calculate progress score
        val progressScore = calculateProgressScore(rides, period)

        TrendData(
            period = period,
            balanceTrend = balanceTrend,
            teTrend = teTrend,
            psTrend = psTrend,
            avgBalance = avgBalance,
            avgTE = avgTE,
            avgPS = avgPS,
            rideCount = rides.size,
            progressScore = progressScore
        )
    }

    /**
     * Calculate progress score by comparing first half to second half of the period.
     * Returns value from -100 to +100.
     */
    private fun calculateProgressScore(rides: List<RideEntity>, period: TrendData.Period): Int {
        if (rides.size < 4) return 0

        val sorted = rides.sortedBy { it.timestamp }
        val midpoint = sorted.size / 2
        val firstHalf = sorted.take(midpoint)
        val secondHalf = sorted.drop(midpoint)

        // Score based on zone optimal improvement
        val firstHalfAvg = firstHalf.map { it.zoneOptimal }.average()
        val secondHalfAvg = secondHalf.map { it.zoneOptimal }.average()

        val improvement = secondHalfAvg - firstHalfAvg
        return improvement.toInt().coerceIn(-100, 100)
    }

    /**
     * Get start of day for a timestamp.
     */
    private fun getDayStart(timestampMs: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
