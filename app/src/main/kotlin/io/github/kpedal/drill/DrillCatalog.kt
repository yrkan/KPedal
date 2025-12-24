package io.github.kpedal.drill

import io.github.kpedal.drill.model.*

/**
 * Catalog of predefined drills.
 * Contains 10 drills: 4 timed focus, 3 target-based, 3 guided workouts.
 */
object DrillCatalog {

    /**
     * All available drills.
     */
    val all: List<Drill> by lazy {
        listOf(
            // Timed Focus Drills (4)
            leftLegFocus,
            rightLegFocus,
            smoothCircles,
            powerTransfer,

            // Target-Based Drills (3)
            balanceChallenge,
            smoothnessTarget,
            highCadenceSmoothness,

            // Guided Workouts (3)
            balanceRecovery,
            efficiencyBuilder,
            pedalingMastery
        )
    }

    /**
     * Get drill by ID.
     */
    fun getDrill(id: String): Drill? = all.find { it.id == id }

    /**
     * Get drills by type.
     */
    fun getByType(type: DrillType): List<Drill> = all.filter { it.type == type }

    /**
     * Get drills by difficulty.
     */
    fun getByDifficulty(difficulty: DrillDifficulty): List<Drill> =
        all.filter { it.difficulty == difficulty }

    // ========== TIMED FOCUS DRILLS ==========

    private val leftLegFocus = Drill(
        id = "left_leg_focus",
        name = "Left Leg Focus",
        description = "Focus on your left leg for 30 seconds. Mentally emphasize the left pedal stroke while maintaining normal cadence.",
        type = DrillType.TIMED_FOCUS,
        metric = DrillMetric.BALANCE,
        difficulty = DrillDifficulty.BEGINNER,
        phases = listOf(
            DrillPhase(
                name = "Warm Up",
                description = "Easy spinning",
                durationMs = 10_000,
                instruction = "Spin easy, prepare to focus"
            ),
            DrillPhase(
                name = "Left Focus",
                description = "Emphasize left leg",
                durationMs = 30_000,
                target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    maxValue = 48f  // Left leg stronger = balance < 50
                ),
                instruction = "Focus on pushing through with your LEFT leg"
            ),
            DrillPhase(
                name = "Recovery",
                description = "Return to normal",
                durationMs = 10_000,
                instruction = "Relax and spin normally"
            )
        ),
        tips = listOf(
            "Visualize power coming from your left leg",
            "Don't completely neglect the right leg",
            "Maintain smooth cadence throughout"
        )
    )

    private val rightLegFocus = Drill(
        id = "right_leg_focus",
        name = "Right Leg Focus",
        description = "Focus on your right leg for 30 seconds. Mentally emphasize the right pedal stroke while maintaining normal cadence.",
        type = DrillType.TIMED_FOCUS,
        metric = DrillMetric.BALANCE,
        difficulty = DrillDifficulty.BEGINNER,
        phases = listOf(
            DrillPhase(
                name = "Warm Up",
                description = "Easy spinning",
                durationMs = 10_000,
                instruction = "Spin easy, prepare to focus"
            ),
            DrillPhase(
                name = "Right Focus",
                description = "Emphasize right leg",
                durationMs = 30_000,
                target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    minValue = 52f  // Right leg stronger = balance > 50
                ),
                instruction = "Focus on pushing through with your RIGHT leg"
            ),
            DrillPhase(
                name = "Recovery",
                description = "Return to normal",
                durationMs = 10_000,
                instruction = "Relax and spin normally"
            )
        ),
        tips = listOf(
            "Visualize power coming from your right leg",
            "Don't completely neglect the left leg",
            "Maintain smooth cadence throughout"
        )
    )

    private val smoothCircles = Drill(
        id = "smooth_circles",
        name = "Smooth Circles",
        description = "Focus on making perfect circles with your pedal stroke for 45 seconds. Eliminate dead spots.",
        type = DrillType.TIMED_FOCUS,
        metric = DrillMetric.PEDAL_SMOOTHNESS,
        difficulty = DrillDifficulty.INTERMEDIATE,
        phases = listOf(
            DrillPhase(
                name = "Prepare",
                description = "Find your rhythm",
                durationMs = 15_000,
                instruction = "Settle into a comfortable cadence"
            ),
            DrillPhase(
                name = "Smooth Circles",
                description = "Maximize smoothness",
                durationMs = 45_000,
                target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 22f  // Target good smoothness
                ),
                instruction = "Imagine drawing smooth circles. Scrape mud off your shoe at the bottom."
            ),
            DrillPhase(
                name = "Cool Down",
                description = "Easy spinning",
                durationMs = 15_000,
                instruction = "Relax and maintain the feeling"
            )
        ),
        tips = listOf(
            "Think about the entire 360° of the pedal stroke",
            "Engage hamstrings at the bottom",
            "Pull up slightly through the back stroke",
            "Keep core engaged for stability"
        )
    )

    private val powerTransfer = Drill(
        id = "power_transfer",
        name = "Power Transfer",
        description = "Focus on efficient power transfer for 60 seconds. Maximize torque effectiveness without sacrificing power.",
        type = DrillType.TIMED_FOCUS,
        metric = DrillMetric.TORQUE_EFFECTIVENESS,
        difficulty = DrillDifficulty.INTERMEDIATE,
        phases = listOf(
            DrillPhase(
                name = "Build Up",
                description = "Gradually increase focus",
                durationMs = 15_000,
                instruction = "Start easy, focus on feeling the power transfer"
            ),
            DrillPhase(
                name = "Max Transfer",
                description = "Optimize TE",
                durationMs = 60_000,
                target = DrillTarget(
                    metric = DrillMetric.TORQUE_EFFECTIVENESS,
                    minValue = 70f,
                    maxValue = 80f  // Optimal range
                ),
                instruction = "Apply power through the entire stroke. Avoid mashing."
            ),
            DrillPhase(
                name = "Recover",
                description = "Easy spin",
                durationMs = 15_000,
                instruction = "Spin easy, let legs recover"
            )
        ),
        tips = listOf(
            "TE above 80% can actually reduce total power",
            "Focus on the 70-80% optimal zone",
            "Don't sacrifice main power phase for recovery phase"
        )
    )

    // ========== TARGET-BASED DRILLS ==========

    private val balanceChallenge = Drill(
        id = "balance_challenge",
        name = "Balance Challenge",
        description = "Hold perfect 50/50 balance for 15 seconds. Test your control and symmetry.",
        type = DrillType.TARGET_BASED,
        metric = DrillMetric.BALANCE,
        difficulty = DrillDifficulty.INTERMEDIATE,
        phases = listOf(
            DrillPhase(
                name = "Find Center",
                description = "Center your power",
                durationMs = 20_000,
                instruction = "Find your natural balance point"
            ),
            DrillPhase(
                name = "Hold Balance",
                description = "Hold 50/50",
                durationMs = 30_000,
                target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f  // 48-52% is success
                ),
                holdTimeMs = 15_000,  // Need 15 seconds in target
                instruction = "Hold 50/50 balance. Small adjustments only!"
            ),
            DrillPhase(
                name = "Relax",
                description = "Release focus",
                durationMs = 10_000,
                instruction = "Good work! Relax and spin easy"
            )
        ),
        tips = listOf(
            "Focus on equal pressure from both legs",
            "Minor imbalance is normal, aim for the zone",
            "Keep cadence steady - don't chase the numbers"
        )
    )

    private val smoothnessTarget = Drill(
        id = "smoothness_target",
        name = "Smoothness Target",
        description = "Hold pedal smoothness above 25% for 20 seconds. Challenge your technique.",
        type = DrillType.TARGET_BASED,
        metric = DrillMetric.PEDAL_SMOOTHNESS,
        difficulty = DrillDifficulty.ADVANCED,
        phases = listOf(
            DrillPhase(
                name = "Warm Up",
                description = "Loosen up",
                durationMs = 20_000,
                instruction = "Easy spinning, focus on form"
            ),
            DrillPhase(
                name = "Target Zone",
                description = "Hold PS ≥25%",
                durationMs = 40_000,
                target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 25f
                ),
                holdTimeMs = 20_000,  // Need 20 seconds in target
                instruction = "Keep smoothness above 25%. Full circles!"
            ),
            DrillPhase(
                name = "Cool Down",
                description = "Recover",
                durationMs = 15_000,
                instruction = "Great effort! Spin easy"
            )
        ),
        tips = listOf(
            "Elite cyclists maintain 25-35% smoothness",
            "Engage all muscle groups through the stroke",
            "Maintain moderate cadence (85-95 rpm works well)"
        )
    )

    private val highCadenceSmoothness = Drill(
        id = "high_cadence_smoothness",
        name = "High Cadence Smoothness",
        description = "Maintain smooth pedaling at higher cadence. The ultimate coordination test.",
        type = DrillType.TARGET_BASED,
        metric = DrillMetric.PEDAL_SMOOTHNESS,
        difficulty = DrillDifficulty.ADVANCED,
        phases = listOf(
            DrillPhase(
                name = "Normal Cadence",
                description = "Establish baseline",
                durationMs = 15_000,
                instruction = "Start at your normal cadence"
            ),
            DrillPhase(
                name = "Build Cadence",
                description = "Increase to 100+ rpm",
                durationMs = 15_000,
                instruction = "Gradually increase cadence to 100+ rpm"
            ),
            DrillPhase(
                name = "Hold Smooth",
                description = "Smooth at high cadence",
                durationMs = 30_000,
                target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 20f
                ),
                holdTimeMs = 15_000,
                instruction = "Keep smoothness above 20% at high cadence!"
            ),
            DrillPhase(
                name = "Wind Down",
                description = "Return to normal",
                durationMs = 15_000,
                instruction = "Gradually reduce cadence, stay smooth"
            )
        ),
        tips = listOf(
            "Smoothness naturally drops at very high cadence",
            "Focus on relaxing your hips",
            "Don't bounce in the saddle",
            "Core stability is crucial"
        )
    )

    // ========== GUIDED WORKOUTS ==========

    private val balanceRecovery = Drill(
        id = "balance_recovery",
        name = "Balance Recovery",
        description = "5-minute workout to correct balance imbalances. Alternate leg focus with centering.",
        type = DrillType.GUIDED_WORKOUT,
        metric = DrillMetric.BALANCE,
        difficulty = DrillDifficulty.BEGINNER,
        phases = listOf(
            DrillPhase(
                name = "Baseline",
                description = "Find natural balance",
                durationMs = 30_000,
                instruction = "Spin naturally, observe your balance"
            ),
            DrillPhase(
                name = "Left Emphasis",
                description = "Focus left",
                durationMs = 45_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, maxValue = 48f),
                instruction = "Emphasize your LEFT leg"
            ),
            DrillPhase(
                name = "Center",
                description = "Find 50/50",
                durationMs = 30_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, targetValue = 50f, tolerance = 3f),
                instruction = "Return to center, 50/50 balance"
            ),
            DrillPhase(
                name = "Right Emphasis",
                description = "Focus right",
                durationMs = 45_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, minValue = 52f),
                instruction = "Emphasize your RIGHT leg"
            ),
            DrillPhase(
                name = "Center Again",
                description = "Find 50/50",
                durationMs = 30_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, targetValue = 50f, tolerance = 3f),
                instruction = "Return to center, feel the symmetry"
            ),
            DrillPhase(
                name = "Natural Finish",
                description = "Spin naturally",
                durationMs = 30_000,
                instruction = "Spin naturally, notice any improvement"
            )
        ),
        tips = listOf(
            "This workout helps identify your weaker leg",
            "Regular practice improves neural pathways",
            "Don't force extreme imbalance"
        )
    )

    private val efficiencyBuilder = Drill(
        id = "efficiency_builder",
        name = "Efficiency Builder",
        description = "10-minute structured workout targeting all efficiency metrics. Build complete pedaling technique.",
        type = DrillType.GUIDED_WORKOUT,
        metric = DrillMetric.COMBINED,
        difficulty = DrillDifficulty.INTERMEDIATE,
        phases = listOf(
            DrillPhase(
                name = "Warm Up",
                description = "Easy spinning",
                durationMs = 60_000,
                instruction = "Warm up with easy spinning"
            ),
            DrillPhase(
                name = "Balance Focus",
                description = "Center your power",
                durationMs = 90_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, targetValue = 50f, tolerance = 3f),
                instruction = "Focus on even balance, 50/50 power"
            ),
            DrillPhase(
                name = "Smoothness Block",
                description = "Smooth circles",
                durationMs = 120_000,
                target = DrillTarget(metric = DrillMetric.PEDAL_SMOOTHNESS, minValue = 22f),
                instruction = "Focus on smooth, circular pedal stroke"
            ),
            DrillPhase(
                name = "Recovery",
                description = "Easy spin",
                durationMs = 45_000,
                instruction = "Easy spinning, shake out legs"
            ),
            DrillPhase(
                name = "TE Focus",
                description = "Power transfer",
                durationMs = 120_000,
                target = DrillTarget(metric = DrillMetric.TORQUE_EFFECTIVENESS, minValue = 70f, maxValue = 80f),
                instruction = "Optimize torque effectiveness 70-80%"
            ),
            DrillPhase(
                name = "Combined",
                description = "All metrics",
                durationMs = 90_000,
                instruction = "Now combine: Balance + Smooth + Efficient"
            ),
            DrillPhase(
                name = "Cool Down",
                description = "Wind down",
                durationMs = 60_000,
                instruction = "Easy spinning, appreciate your effort"
            )
        ),
        tips = listOf(
            "Don't expect perfection on all metrics at once",
            "Focus on one aspect at a time initially",
            "Progress comes with regular practice",
            "Notice which metric is hardest for you"
        )
    )

    private val pedalingMastery = Drill(
        id = "pedaling_mastery",
        name = "Pedaling Mastery",
        description = "15-minute comprehensive workout. The ultimate pedaling technique session with progressive challenges.",
        type = DrillType.GUIDED_WORKOUT,
        metric = DrillMetric.COMBINED,
        difficulty = DrillDifficulty.ADVANCED,
        phases = listOf(
            DrillPhase(
                name = "Activation",
                description = "Wake up the legs",
                durationMs = 60_000,
                instruction = "Light spinning, prepare mentally"
            ),
            DrillPhase(
                name = "Left Leg Isolation",
                description = "Left focus",
                durationMs = 60_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, maxValue = 47f),
                instruction = "Strong left leg emphasis"
            ),
            DrillPhase(
                name = "Right Leg Isolation",
                description = "Right focus",
                durationMs = 60_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, minValue = 53f),
                instruction = "Strong right leg emphasis"
            ),
            DrillPhase(
                name = "Perfect Balance",
                description = "50/50 hold",
                durationMs = 60_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, targetValue = 50f, tolerance = 2f),
                instruction = "Perfect balance, 50/50"
            ),
            DrillPhase(
                name = "Smoothness Focus",
                description = "Circular motion",
                durationMs = 90_000,
                target = DrillTarget(metric = DrillMetric.PEDAL_SMOOTHNESS, minValue = 24f),
                instruction = "Maximum smoothness, eliminate dead spots"
            ),
            DrillPhase(
                name = "Recovery",
                description = "Easy spin",
                durationMs = 60_000,
                instruction = "Easy spinning, recover"
            ),
            DrillPhase(
                name = "TE Optimization",
                description = "Power transfer",
                durationMs = 90_000,
                target = DrillTarget(metric = DrillMetric.TORQUE_EFFECTIVENESS, minValue = 72f, maxValue = 78f),
                instruction = "Dial in TE to 72-78% zone"
            ),
            DrillPhase(
                name = "High Cadence Test",
                description = "Speed + form",
                durationMs = 60_000,
                target = DrillTarget(metric = DrillMetric.PEDAL_SMOOTHNESS, minValue = 18f),
                instruction = "Increase cadence, maintain form"
            ),
            DrillPhase(
                name = "Integration",
                description = "Everything together",
                durationMs = 120_000,
                instruction = "Combine all skills: Balance + Smooth + Efficient"
            ),
            DrillPhase(
                name = "Cool Down",
                description = "Wind down",
                durationMs = 90_000,
                instruction = "Easy spinning, appreciate your mastery work"
            )
        ),
        tips = listOf(
            "This is a demanding session - be well rested",
            "Each phase builds on previous skills",
            "Focus on quality over perfection",
            "Track your progress over multiple sessions",
            "Consider doing this 1-2x per week"
        )
    )
}
