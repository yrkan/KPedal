package io.github.kpedal.drill

import io.github.kpedal.R
import io.github.kpedal.drill.model.*

/**
 * Catalog of predefined drills.
 * Contains 10 drills: 4 timed focus, 3 target-based, 3 guided workouts.
 * Uses @StringRes for localization.
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
        nameRes = R.string.drill_left_leg_focus,
        descriptionRes = R.string.drill_left_leg_focus_desc,
        type = DrillType.TIMED_FOCUS,
        metric = DrillMetric.BALANCE,
        difficulty = DrillDifficulty.BEGINNER,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_warmup,
                descriptionRes = R.string.phase_desc_easy_spinning,
                durationMs = 10_000,
                instructionRes = R.string.instr_spin_easy_prepare
            ),
            DrillPhase(
                nameRes = R.string.phase_left_focus,
                descriptionRes = R.string.phase_desc_emphasize_left,
                durationMs = 30_000,
                target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    maxValue = 48f  // Left leg stronger = balance < 50
                ),
                instructionRes = R.string.instr_focus_left_leg
            ),
            DrillPhase(
                nameRes = R.string.phase_recovery,
                descriptionRes = R.string.phase_desc_return_normal,
                durationMs = 10_000,
                instructionRes = R.string.instr_relax_spin_normally
            )
        ),
        tipResIds = listOf(
            R.string.tip_visualize_left,
            R.string.tip_dont_neglect_right,
            R.string.tip_maintain_cadence
        )
    )

    private val rightLegFocus = Drill(
        id = "right_leg_focus",
        nameRes = R.string.drill_right_leg_focus,
        descriptionRes = R.string.drill_right_leg_focus_desc,
        type = DrillType.TIMED_FOCUS,
        metric = DrillMetric.BALANCE,
        difficulty = DrillDifficulty.BEGINNER,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_warmup,
                descriptionRes = R.string.phase_desc_easy_spinning,
                durationMs = 10_000,
                instructionRes = R.string.instr_spin_easy_prepare
            ),
            DrillPhase(
                nameRes = R.string.phase_right_focus,
                descriptionRes = R.string.phase_desc_emphasize_right,
                durationMs = 30_000,
                target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    minValue = 52f  // Right leg stronger = balance > 50
                ),
                instructionRes = R.string.instr_focus_right_leg
            ),
            DrillPhase(
                nameRes = R.string.phase_recovery,
                descriptionRes = R.string.phase_desc_return_normal,
                durationMs = 10_000,
                instructionRes = R.string.instr_relax_spin_normally
            )
        ),
        tipResIds = listOf(
            R.string.tip_visualize_right,
            R.string.tip_dont_neglect_left,
            R.string.tip_maintain_cadence
        )
    )

    private val smoothCircles = Drill(
        id = "smooth_circles",
        nameRes = R.string.drill_smooth_circles,
        descriptionRes = R.string.drill_smooth_circles_desc,
        type = DrillType.TIMED_FOCUS,
        metric = DrillMetric.PEDAL_SMOOTHNESS,
        difficulty = DrillDifficulty.INTERMEDIATE,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_prepare,
                descriptionRes = R.string.phase_desc_find_rhythm,
                durationMs = 15_000,
                instructionRes = R.string.instr_settle_cadence
            ),
            DrillPhase(
                nameRes = R.string.phase_smooth_circles,
                descriptionRes = R.string.phase_desc_max_smoothness,
                durationMs = 45_000,
                target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 22f  // Target good smoothness
                ),
                instructionRes = R.string.instr_smooth_circles
            ),
            DrillPhase(
                nameRes = R.string.phase_cooldown,
                descriptionRes = R.string.phase_desc_easy_spinning,
                durationMs = 15_000,
                instructionRes = R.string.instr_maintain_feeling
            )
        ),
        tipResIds = listOf(
            R.string.tip_think_360,
            R.string.tip_engage_hamstrings,
            R.string.tip_pull_up,
            R.string.tip_core_stability
        )
    )

    private val powerTransfer = Drill(
        id = "power_transfer",
        nameRes = R.string.drill_power_transfer,
        descriptionRes = R.string.drill_power_transfer_desc,
        type = DrillType.TIMED_FOCUS,
        metric = DrillMetric.TORQUE_EFFECTIVENESS,
        difficulty = DrillDifficulty.INTERMEDIATE,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_build,
                descriptionRes = R.string.phase_desc_increase_focus,
                durationMs = 15_000,
                instructionRes = R.string.instr_feel_power_transfer
            ),
            DrillPhase(
                nameRes = R.string.phase_max_transfer,
                descriptionRes = R.string.phase_desc_optimize_te,
                durationMs = 60_000,
                target = DrillTarget(
                    metric = DrillMetric.TORQUE_EFFECTIVENESS,
                    minValue = 70f,
                    maxValue = 80f  // Optimal range
                ),
                instructionRes = R.string.instr_apply_power
            ),
            DrillPhase(
                nameRes = R.string.phase_recover,
                descriptionRes = R.string.phase_desc_easy_spin,
                durationMs = 15_000,
                instructionRes = R.string.instr_spin_easy_recover
            )
        ),
        tipResIds = listOf(
            R.string.tip_te_above_80,
            R.string.tip_focus_70_80,
            R.string.tip_dont_sacrifice
        )
    )

    // ========== TARGET-BASED DRILLS ==========

    private val balanceChallenge = Drill(
        id = "balance_challenge",
        nameRes = R.string.drill_balance_challenge,
        descriptionRes = R.string.drill_balance_challenge_desc,
        type = DrillType.TARGET_BASED,
        metric = DrillMetric.BALANCE,
        difficulty = DrillDifficulty.INTERMEDIATE,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_find_center,
                descriptionRes = R.string.phase_desc_center_power,
                durationMs = 20_000,
                instructionRes = R.string.instr_find_balance_point
            ),
            DrillPhase(
                nameRes = R.string.phase_hold_balance,
                descriptionRes = R.string.phase_desc_hold_5050,
                durationMs = 30_000,
                target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f  // 48-52% is success
                ),
                holdTimeMs = 15_000,  // Need 15 seconds in target
                instructionRes = R.string.instr_hold_5050
            ),
            DrillPhase(
                nameRes = R.string.phase_relax,
                descriptionRes = R.string.phase_desc_release_focus,
                durationMs = 10_000,
                instructionRes = R.string.instr_good_work_relax
            )
        ),
        tipResIds = listOf(
            R.string.tip_equal_pressure,
            R.string.tip_minor_imbalance,
            R.string.tip_steady_cadence
        )
    )

    private val smoothnessTarget = Drill(
        id = "smoothness_target",
        nameRes = R.string.drill_smoothness_target,
        descriptionRes = R.string.drill_smoothness_target_desc,
        type = DrillType.TARGET_BASED,
        metric = DrillMetric.PEDAL_SMOOTHNESS,
        difficulty = DrillDifficulty.ADVANCED,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_warmup,
                descriptionRes = R.string.phase_desc_loosen_up,
                durationMs = 20_000,
                instructionRes = R.string.instr_easy_spin_form
            ),
            DrillPhase(
                nameRes = R.string.phase_target_zone,
                descriptionRes = R.string.phase_desc_hold_ps_25,
                durationMs = 40_000,
                target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 25f
                ),
                holdTimeMs = 20_000,  // Need 20 seconds in target
                instructionRes = R.string.instr_keep_ps_25
            ),
            DrillPhase(
                nameRes = R.string.phase_cooldown,
                descriptionRes = R.string.phase_desc_recover,
                durationMs = 15_000,
                instructionRes = R.string.instr_great_effort
            )
        ),
        tipResIds = listOf(
            R.string.tip_elite_smoothness,
            R.string.tip_engage_all_muscles,
            R.string.tip_moderate_cadence
        )
    )

    private val highCadenceSmoothness = Drill(
        id = "high_cadence_smoothness",
        nameRes = R.string.drill_high_cadence_smooth,
        descriptionRes = R.string.drill_high_cadence_smooth_desc,
        type = DrillType.TARGET_BASED,
        metric = DrillMetric.PEDAL_SMOOTHNESS,
        difficulty = DrillDifficulty.ADVANCED,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_normal_cadence,
                descriptionRes = R.string.phase_desc_establish_baseline,
                durationMs = 15_000,
                instructionRes = R.string.instr_start_normal_cadence
            ),
            DrillPhase(
                nameRes = R.string.phase_build_cadence,
                descriptionRes = R.string.phase_desc_increase_100rpm,
                durationMs = 15_000,
                instructionRes = R.string.instr_increase_cadence_100
            ),
            DrillPhase(
                nameRes = R.string.phase_hold_smooth,
                descriptionRes = R.string.phase_desc_smooth_high_cadence,
                durationMs = 30_000,
                target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 20f
                ),
                holdTimeMs = 15_000,
                instructionRes = R.string.instr_keep_ps_high_cadence
            ),
            DrillPhase(
                nameRes = R.string.phase_wind_down,
                descriptionRes = R.string.phase_desc_return_to_normal,
                durationMs = 15_000,
                instructionRes = R.string.instr_reduce_cadence_smooth
            )
        ),
        tipResIds = listOf(
            R.string.tip_smoothness_drops,
            R.string.tip_relax_hips,
            R.string.tip_dont_bounce,
            R.string.tip_core_crucial
        )
    )

    // ========== GUIDED WORKOUTS ==========

    private val balanceRecovery = Drill(
        id = "balance_recovery",
        nameRes = R.string.drill_balance_recovery,
        descriptionRes = R.string.drill_balance_recovery_desc,
        type = DrillType.GUIDED_WORKOUT,
        metric = DrillMetric.BALANCE,
        difficulty = DrillDifficulty.BEGINNER,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_baseline,
                descriptionRes = R.string.phase_desc_find_natural_balance,
                durationMs = 30_000,
                instructionRes = R.string.instr_observe_balance
            ),
            DrillPhase(
                nameRes = R.string.phase_left_emphasis,
                descriptionRes = R.string.phase_desc_focus_left,
                durationMs = 45_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, maxValue = 48f),
                instructionRes = R.string.instr_emphasize_left
            ),
            DrillPhase(
                nameRes = R.string.phase_center,
                descriptionRes = R.string.phase_desc_find_5050,
                durationMs = 30_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, targetValue = 50f, tolerance = 3f),
                instructionRes = R.string.instr_return_center
            ),
            DrillPhase(
                nameRes = R.string.phase_right_emphasis,
                descriptionRes = R.string.phase_desc_focus_right,
                durationMs = 45_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, minValue = 52f),
                instructionRes = R.string.instr_emphasize_right
            ),
            DrillPhase(
                nameRes = R.string.phase_center_again,
                descriptionRes = R.string.phase_desc_find_5050,
                durationMs = 30_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, targetValue = 50f, tolerance = 3f),
                instructionRes = R.string.instr_feel_symmetry
            ),
            DrillPhase(
                nameRes = R.string.phase_natural_finish,
                descriptionRes = R.string.phase_desc_spin_naturally,
                durationMs = 30_000,
                instructionRes = R.string.instr_notice_improvement
            )
        ),
        tipResIds = listOf(
            R.string.tip_identify_weaker,
            R.string.tip_neural_pathways,
            R.string.tip_dont_force
        )
    )

    private val efficiencyBuilder = Drill(
        id = "efficiency_builder",
        nameRes = R.string.drill_efficiency_builder,
        descriptionRes = R.string.drill_efficiency_builder_desc,
        type = DrillType.GUIDED_WORKOUT,
        metric = DrillMetric.COMBINED,
        difficulty = DrillDifficulty.INTERMEDIATE,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_warmup,
                descriptionRes = R.string.phase_desc_easy_spinning,
                durationMs = 60_000,
                instructionRes = R.string.instr_warmup_easy
            ),
            DrillPhase(
                nameRes = R.string.phase_balance_focus,
                descriptionRes = R.string.phase_desc_center_power,
                durationMs = 90_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, targetValue = 50f, tolerance = 3f),
                instructionRes = R.string.instr_focus_even_balance
            ),
            DrillPhase(
                nameRes = R.string.phase_smoothness_block,
                descriptionRes = R.string.phase_desc_smooth_circles,
                durationMs = 120_000,
                target = DrillTarget(metric = DrillMetric.PEDAL_SMOOTHNESS, minValue = 22f),
                instructionRes = R.string.instr_smooth_circular
            ),
            DrillPhase(
                nameRes = R.string.phase_recovery,
                descriptionRes = R.string.phase_desc_easy_spin,
                durationMs = 45_000,
                instructionRes = R.string.instr_shake_out
            ),
            DrillPhase(
                nameRes = R.string.phase_te_focus,
                descriptionRes = R.string.phase_desc_power_transfer,
                durationMs = 120_000,
                target = DrillTarget(metric = DrillMetric.TORQUE_EFFECTIVENESS, minValue = 70f, maxValue = 80f),
                instructionRes = R.string.instr_optimize_te
            ),
            DrillPhase(
                nameRes = R.string.phase_combined,
                descriptionRes = R.string.phase_desc_all_metrics,
                durationMs = 90_000,
                instructionRes = R.string.instr_combine_all
            ),
            DrillPhase(
                nameRes = R.string.phase_cooldown,
                descriptionRes = R.string.phase_desc_wind_down,
                durationMs = 60_000,
                instructionRes = R.string.instr_appreciate_effort
            )
        ),
        tipResIds = listOf(
            R.string.tip_no_perfection,
            R.string.tip_one_at_time,
            R.string.tip_regular_practice,
            R.string.tip_notice_hardest
        )
    )

    private val pedalingMastery = Drill(
        id = "pedaling_mastery",
        nameRes = R.string.drill_pedaling_mastery,
        descriptionRes = R.string.drill_pedaling_mastery_desc,
        type = DrillType.GUIDED_WORKOUT,
        metric = DrillMetric.COMBINED,
        difficulty = DrillDifficulty.ADVANCED,
        phases = listOf(
            DrillPhase(
                nameRes = R.string.phase_activation,
                descriptionRes = R.string.phase_desc_wake_legs,
                durationMs = 60_000,
                instructionRes = R.string.instr_prepare_mentally
            ),
            DrillPhase(
                nameRes = R.string.phase_left_isolation,
                descriptionRes = R.string.phase_desc_left_focus,
                durationMs = 60_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, maxValue = 47f),
                instructionRes = R.string.instr_strong_left
            ),
            DrillPhase(
                nameRes = R.string.phase_right_isolation,
                descriptionRes = R.string.phase_desc_right_focus,
                durationMs = 60_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, minValue = 53f),
                instructionRes = R.string.instr_strong_right
            ),
            DrillPhase(
                nameRes = R.string.phase_perfect_balance,
                descriptionRes = R.string.phase_desc_5050_hold,
                durationMs = 60_000,
                target = DrillTarget(metric = DrillMetric.BALANCE, targetValue = 50f, tolerance = 2f),
                instructionRes = R.string.instr_perfect_5050
            ),
            DrillPhase(
                nameRes = R.string.phase_smoothness_focus,
                descriptionRes = R.string.phase_desc_circular_motion,
                durationMs = 90_000,
                target = DrillTarget(metric = DrillMetric.PEDAL_SMOOTHNESS, minValue = 24f),
                instructionRes = R.string.instr_max_smoothness
            ),
            DrillPhase(
                nameRes = R.string.phase_recovery,
                descriptionRes = R.string.phase_desc_easy_spin,
                durationMs = 60_000,
                instructionRes = R.string.instr_easy_recover
            ),
            DrillPhase(
                nameRes = R.string.phase_te_optimization,
                descriptionRes = R.string.phase_desc_power_transfer,
                durationMs = 90_000,
                target = DrillTarget(metric = DrillMetric.TORQUE_EFFECTIVENESS, minValue = 72f, maxValue = 78f),
                instructionRes = R.string.instr_dial_te
            ),
            DrillPhase(
                nameRes = R.string.phase_high_cadence_test,
                descriptionRes = R.string.phase_desc_speed_form,
                durationMs = 60_000,
                target = DrillTarget(metric = DrillMetric.PEDAL_SMOOTHNESS, minValue = 18f),
                instructionRes = R.string.instr_increase_cadence_form
            ),
            DrillPhase(
                nameRes = R.string.phase_integration,
                descriptionRes = R.string.phase_desc_everything_together,
                durationMs = 120_000,
                instructionRes = R.string.instr_combine_skills
            ),
            DrillPhase(
                nameRes = R.string.phase_cooldown,
                descriptionRes = R.string.phase_desc_wind_down,
                durationMs = 90_000,
                instructionRes = R.string.instr_appreciate_mastery
            )
        ),
        tipResIds = listOf(
            R.string.tip_demanding_session,
            R.string.tip_phases_build,
            R.string.tip_quality_over_perfection,
            R.string.tip_track_progress,
            R.string.tip_once_twice_week
        )
    )
}
