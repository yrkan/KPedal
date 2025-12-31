package io.github.kpedal.data.models

import androidx.annotation.StringRes
import io.github.kpedal.R
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
    @StringRes val strengthResIds: List<Int>,
    @StringRes val improvementResIds: List<Int>,
    @StringRes val summaryResId: Int
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

        val strengths = mutableListOf<Int>()
        val improvements = mutableListOf<Int>()

        // Balance analysis
        val balanceDeviation = abs(ride.balanceRight - 50)
        when {
            balanceDeviation <= 2 -> strengths.add(R.string.analysis_excellent_balance)
            balanceDeviation <= 5 -> strengths.add(R.string.analysis_good_balance)
            balanceDeviation <= 8 -> improvements.add(R.string.analysis_balance_off)
            else -> improvements.add(R.string.analysis_balance_imbalance)
        }

        // TE analysis
        val teAvg = (ride.teLeft + ride.teRight) / 2
        when {
            teAvg in 70..80 -> strengths.add(R.string.analysis_optimal_te)
            teAvg in 65..85 -> strengths.add(R.string.analysis_good_te)
            teAvg > 85 -> improvements.add(R.string.analysis_te_too_high)
            else -> improvements.add(R.string.analysis_te_low)
        }

        // PS analysis
        val psAvg = (ride.psLeft + ride.psRight) / 2
        when {
            psAvg >= 25 -> strengths.add(R.string.analysis_very_smooth)
            psAvg >= 20 -> strengths.add(R.string.analysis_good_smooth)
            psAvg >= 15 -> improvements.add(R.string.analysis_smooth_improve)
            else -> improvements.add(R.string.analysis_smooth_focus)
        }

        // Zone analysis
        when {
            ride.zoneOptimal >= 70 -> strengths.add(R.string.analysis_excellent_zone)
            ride.zoneOptimal >= 50 -> strengths.add(R.string.analysis_good_consistency)
            ride.zoneOptimal >= 30 -> improvements.add(R.string.analysis_zone_more)
            else -> improvements.add(R.string.analysis_zone_practice)
        }

        val summaryResId = generateSummaryResId(overallScore, balanceDeviation)

        return RideAnalysis(
            overallScore = overallScore,
            balanceScore = balanceScore,
            efficiencyScore = efficiencyScore,
            consistencyScore = consistencyScore,
            suggestedRating = suggestedRating,
            strengthResIds = strengths.take(3),
            improvementResIds = improvements.take(3),
            summaryResId = summaryResId
        )
    }

    @StringRes
    private fun generateSummaryResId(overallScore: Int, balanceDeviation: Int): Int {
        return when {
            overallScore >= 85 -> R.string.analysis_summary_excellent
            overallScore >= 70 -> if (balanceDeviation > 3) R.string.analysis_summary_good_balance else R.string.analysis_summary_good_smooth
            overallScore >= 55 -> R.string.analysis_summary_solid
            overallScore >= 40 -> R.string.analysis_summary_improve
            else -> R.string.analysis_summary_practice
        }
    }
}
