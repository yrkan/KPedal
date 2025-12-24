package io.github.kpedal.data

/**
 * Static help content for metric explanations and tips.
 */
object HelpContent {

    data class MetricInfo(
        val id: String,
        val name: String,
        val shortDescription: String,
        val explanation: String,
        val optimalRange: String,
        val tips: List<String>,
        val research: String? = null
    )

    val metrics = listOf(
        MetricInfo(
            id = "balance",
            name = "Power Balance",
            shortDescription = "L/R power distribution",
            explanation = "Power Balance shows how evenly you distribute power between your left and right legs. A perfect balance would be 50/50, but minor asymmetry (48-52%) is normal and not concerning.",
            optimalRange = "48-52% (optimal), 45-55% (acceptable)",
            tips = listOf(
                "Focus on even pressure through the entire pedal stroke",
                "Try single-leg drills to strengthen your weaker side",
                "Check your bike fit - saddle height can affect balance",
                "Minor imbalances are normal and often correct themselves",
                "Don't obsess over small fluctuations during a ride"
            ),
            research = "Studies show most cyclists have 1-5% natural imbalance. Pro cyclists average 48-52%."
        ),
        MetricInfo(
            id = "te",
            name = "Torque Effectiveness",
            shortDescription = "Pedal stroke efficiency",
            explanation = "Torque Effectiveness (TE) measures how much of your pedaling motion contributes to forward propulsion. A score of 100% would mean every part of your stroke adds power, but this is neither realistic nor optimal.",
            optimalRange = "70-80% (optimal, NOT higher)",
            tips = listOf(
                "Focus on pulling through the bottom of the stroke",
                "Think of 'scraping mud' off your shoe at the bottom",
                "Higher TE (>80%) often REDUCES main power output",
                "Smooth, circular pedaling is more efficient than forcing high TE",
                "TE naturally varies with cadence and power"
            ),
            research = "Wattbike research shows TE above 80% actually decreases power output. Elite cyclists aim for 70-80%."
        ),
        MetricInfo(
            id = "ps",
            name = "Pedal Smoothness",
            shortDescription = "Power delivery consistency",
            explanation = "Pedal Smoothness (PS) shows how evenly power is applied throughout the pedal stroke. Higher values mean more consistent power delivery with fewer spikes and dips.",
            optimalRange = "20%+ (good), 25-35% (elite)",
            tips = listOf(
                "Practice high-cadence spinning drills",
                "Focus on eliminating dead spots in your stroke",
                "Keep consistent pressure throughout the rotation",
                "Lower gears and higher cadence often improve smoothness",
                "PS naturally decreases at very high power outputs"
            ),
            research = "Professional cyclists typically achieve 25-35% pedal smoothness."
        )
    )

    val onboardingPages = listOf(
        OnboardingPage(
            title = "Welcome to KPedal",
            description = "Real-time pedaling metrics from your power meter pedals. Track balance, efficiency, and smoothness to improve your technique.",
            icon = "bike"
        ),
        OnboardingPage(
            title = "Data Fields",
            description = "Add KPedal data fields to your Karoo screens to see metrics during your ride. Choose from 6 different layouts.",
            icon = "layout"
        ),
        OnboardingPage(
            title = "Drills",
            description = "Practice with guided drills to improve your pedaling technique. Each drill targets specific aspects of your stroke.",
            icon = "drill"
        ),
        OnboardingPage(
            title = "Alerts",
            description = "Get notified when your metrics go outside optimal ranges. Configure alerts in Settings.",
            icon = "alert"
        )
    )

    data class OnboardingPage(
        val title: String,
        val description: String,
        val icon: String
    )

    fun getMetric(id: String): MetricInfo? = metrics.find { it.id == id }
}
