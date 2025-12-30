package io.github.kpedal.data.models

import io.github.kpedal.data.database.RideEntity
import io.github.kpedal.engine.StatusCalculator
import kotlin.math.abs

/**
 * Automatic ride quality analysis.
 */
data class RideAnalysis(
    val overallScore: Int,           // 0-100
    val balanceScore: Int,           // 0-100
    val efficiencyScore: Int,        // 0-100
    val consistencyScore: Int,       // 0-100
    val suggestedRating: Int,        // 1-5 stars
    val strengths: List<String>,
    val improvements: List<String>,
    val summary: String
)

/**
 * Analyzes a ride and provides insights.
 * Uses unified score calculation from StatusCalculator.
 */
object RideAnalyzer {

    /**
     * Analyze a ride and return insights.
     */
    fun analyze(ride: RideEntity): RideAnalysis {
        // Use unified score calculation from StatusCalculator
        val balanceScore = StatusCalculator.calculateBalanceScore(ride.balanceRight)
        val efficiencyScore = StatusCalculator.calculateEfficiencyScore(
            ride.teLeft, ride.teRight, ride.psLeft, ride.psRight
        )
        val consistencyScore = StatusCalculator.calculateConsistencyScore(ride.zoneOptimal)
        val overallScore = StatusCalculator.calculateOverallScore(balanceScore, efficiencyScore, consistencyScore)

        val suggestedRating = when {
            overallScore >= 85 -> 5
            overallScore >= 70 -> 4
            overallScore >= 55 -> 3
            overallScore >= 40 -> 2
            else -> 1
        }

        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        // Balance analysis
        val balanceDeviation = abs(ride.balanceRight - 50)
        when {
            balanceDeviation <= 2 -> strengths.add("Excellent L/R balance")
            balanceDeviation <= 5 -> strengths.add("Good balance control")
            balanceDeviation <= 8 -> improvements.add("Balance slightly off-center")
            else -> improvements.add("Significant balance imbalance")
        }

        // TE analysis
        val teAvg = (ride.teLeft + ride.teRight) / 2
        when {
            teAvg in 70..80 -> strengths.add("Optimal torque effectiveness")
            teAvg in 65..85 -> strengths.add("Good power transfer")
            teAvg > 85 -> improvements.add("TE too high - may reduce total power")
            else -> improvements.add("Low torque effectiveness")
        }

        // PS analysis
        val psAvg = (ride.psLeft + ride.psRight) / 2
        when {
            psAvg >= 25 -> strengths.add("Very smooth pedaling")
            psAvg >= 20 -> strengths.add("Good pedal smoothness")
            psAvg >= 15 -> improvements.add("Smoothness could improve")
            else -> improvements.add("Focus on smoother pedal stroke")
        }

        // Zone analysis
        when {
            ride.zoneOptimal >= 70 -> strengths.add("Excellent time in optimal zone")
            ride.zoneOptimal >= 50 -> strengths.add("Good consistency")
            ride.zoneOptimal >= 30 -> improvements.add("More time in optimal zone needed")
            else -> improvements.add("Practice maintaining optimal form")
        }

        val summary = generateSummary(overallScore, balanceDeviation, teAvg, psAvg, ride.zoneOptimal)

        return RideAnalysis(
            overallScore = overallScore,
            balanceScore = balanceScore,
            efficiencyScore = efficiencyScore,
            consistencyScore = consistencyScore,
            suggestedRating = suggestedRating,
            strengths = strengths.take(3),
            improvements = improvements.take(3),
            summary = summary
        )
    }

    // Score calculation functions moved to StatusCalculator for unified usage

    private fun generateSummary(
        overallScore: Int,
        balanceDeviation: Int,
        teAvg: Int,
        psAvg: Int,
        zoneOptimal: Int
    ): String {
        return when {
            overallScore >= 85 -> "Excellent ride! Your pedaling technique was highly efficient."
            overallScore >= 70 -> "Good performance. Keep working on ${if (balanceDeviation > 3) "balance" else "smoothness"}."
            overallScore >= 55 -> "Solid effort. Focus on maintaining optimal form throughout."
            overallScore >= 40 -> "Room for improvement. Try the technique drills."
            else -> "Practice makes perfect. Keep training!"
        }
    }
}
