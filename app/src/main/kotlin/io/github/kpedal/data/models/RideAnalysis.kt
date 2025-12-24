package io.github.kpedal.data.models

import io.github.kpedal.data.database.RideEntity
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
 */
object RideAnalyzer {

    /**
     * Analyze a ride and return insights.
     */
    fun analyze(ride: RideEntity): RideAnalysis {
        val balanceScore = calculateBalanceScore(ride)
        val efficiencyScore = calculateEfficiencyScore(ride)
        val consistencyScore = calculateConsistencyScore(ride)

        val overallScore = (balanceScore * 0.4 + efficiencyScore * 0.35 + consistencyScore * 0.25).toInt()

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

    private fun calculateBalanceScore(ride: RideEntity): Int {
        val deviation = abs(ride.balanceRight - 50)
        return when {
            deviation <= 1 -> 100
            deviation <= 2 -> 90
            deviation <= 3 -> 80
            deviation <= 5 -> 70
            deviation <= 7 -> 55
            deviation <= 10 -> 40
            else -> 25
        }
    }

    private fun calculateEfficiencyScore(ride: RideEntity): Int {
        val teAvg = (ride.teLeft + ride.teRight) / 2
        val psAvg = (ride.psLeft + ride.psRight) / 2

        // TE score (optimal is 70-80)
        val teScore = when {
            teAvg in 70..80 -> 100
            teAvg in 65..85 -> 80
            teAvg in 60..90 -> 60
            else -> 40
        }

        // PS score (optimal is >= 20)
        val psScore = when {
            psAvg >= 25 -> 100
            psAvg >= 22 -> 90
            psAvg >= 20 -> 80
            psAvg >= 18 -> 65
            psAvg >= 15 -> 50
            else -> 35
        }

        return (teScore * 0.5 + psScore * 0.5).toInt()
    }

    private fun calculateConsistencyScore(ride: RideEntity): Int {
        // Based on time in optimal zone
        return when {
            ride.zoneOptimal >= 80 -> 100
            ride.zoneOptimal >= 70 -> 90
            ride.zoneOptimal >= 60 -> 80
            ride.zoneOptimal >= 50 -> 70
            ride.zoneOptimal >= 40 -> 55
            ride.zoneOptimal >= 30 -> 40
            else -> 25
        }
    }

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
