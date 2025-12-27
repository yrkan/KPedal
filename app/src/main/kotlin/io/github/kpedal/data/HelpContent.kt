package io.github.kpedal.data

import androidx.annotation.StringRes
import io.github.kpedal.R

/**
 * Static help content for metric explanations and tips.
 * Uses @StringRes for localization.
 */
object HelpContent {

    data class MetricInfo(
        val id: String,
        @StringRes val nameRes: Int,
        @StringRes val shortDescriptionRes: Int,
        @StringRes val explanationRes: Int,
        @StringRes val optimalRangeRes: Int,
        val tipResIds: List<Int>,
        @StringRes val researchRes: Int? = null
    )

    val metrics = listOf(
        MetricInfo(
            id = "balance",
            nameRes = R.string.metric_balance_name,
            shortDescriptionRes = R.string.metric_balance_short,
            explanationRes = R.string.metric_balance_explanation,
            optimalRangeRes = R.string.metric_balance_optimal,
            tipResIds = listOf(
                R.string.metric_balance_tip1,
                R.string.metric_balance_tip2,
                R.string.metric_balance_tip3,
                R.string.metric_balance_tip4,
                R.string.metric_balance_tip5
            ),
            researchRes = R.string.metric_balance_research
        ),
        MetricInfo(
            id = "te",
            nameRes = R.string.metric_te_name,
            shortDescriptionRes = R.string.metric_te_short,
            explanationRes = R.string.metric_te_explanation,
            optimalRangeRes = R.string.metric_te_optimal,
            tipResIds = listOf(
                R.string.metric_te_tip1,
                R.string.metric_te_tip2,
                R.string.metric_te_tip3,
                R.string.metric_te_tip4,
                R.string.metric_te_tip5
            ),
            researchRes = R.string.metric_te_research
        ),
        MetricInfo(
            id = "ps",
            nameRes = R.string.metric_ps_name,
            shortDescriptionRes = R.string.metric_ps_short,
            explanationRes = R.string.metric_ps_explanation,
            optimalRangeRes = R.string.metric_ps_optimal,
            tipResIds = listOf(
                R.string.metric_ps_tip1,
                R.string.metric_ps_tip2,
                R.string.metric_ps_tip3,
                R.string.metric_ps_tip4,
                R.string.metric_ps_tip5
            ),
            researchRes = R.string.metric_ps_research
        )
    )

    val onboardingPages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_welcome_title,
            descriptionRes = R.string.onboarding_welcome_desc,
            icon = "bike"
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_data_fields_title,
            descriptionRes = R.string.onboarding_data_fields_desc,
            icon = "layout"
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_drills_title,
            descriptionRes = R.string.onboarding_drills_desc,
            icon = "drill"
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_alerts_title,
            descriptionRes = R.string.onboarding_alerts_desc,
            icon = "alert"
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_background_title,
            descriptionRes = R.string.onboarding_background_desc,
            icon = "background"
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_cloud_title,
            descriptionRes = R.string.onboarding_cloud_desc,
            icon = "cloud"
        )
    )

    data class OnboardingPage(
        @StringRes val titleRes: Int,
        @StringRes val descriptionRes: Int,
        val icon: String
    )

    fun getMetric(id: String): MetricInfo? = metrics.find { it.id == id }
}
