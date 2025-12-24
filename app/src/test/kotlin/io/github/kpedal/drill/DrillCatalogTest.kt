package io.github.kpedal.drill

import com.google.common.truth.Truth.assertThat
import io.github.kpedal.drill.model.DrillDifficulty
import io.github.kpedal.drill.model.DrillType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DrillCatalog")
class DrillCatalogTest {

    @Nested
    @DisplayName("all drills")
    inner class AllDrillsTests {

        @Test
        fun `contains exactly 10 drills`() {
            assertThat(DrillCatalog.all).hasSize(10)
        }

        @Test
        fun `all drills have unique IDs`() {
            val ids = DrillCatalog.all.map { it.id }
            assertThat(ids).containsNoDuplicates()
        }

        @Test
        fun `all drills have non-empty names`() {
            DrillCatalog.all.forEach { drill ->
                assertThat(drill.name).isNotEmpty()
            }
        }

        @Test
        fun `all drills have at least one phase`() {
            DrillCatalog.all.forEach { drill ->
                assertThat(drill.phases).isNotEmpty()
            }
        }

        @Test
        fun `all drills have positive total duration`() {
            DrillCatalog.all.forEach { drill ->
                assertThat(drill.totalDurationMs).isGreaterThan(0L)
            }
        }
    }

    @Nested
    @DisplayName("getDrill()")
    inner class GetDrillTests {

        @Test
        fun `returns drill for existing ID`() {
            val drill = DrillCatalog.getDrill("left_leg_focus")
            assertThat(drill).isNotNull()
            assertThat(drill!!.name).isEqualTo("Left Leg Focus")
        }

        @Test
        fun `returns null for non-existing ID`() {
            val drill = DrillCatalog.getDrill("non_existing_drill")
            assertThat(drill).isNull()
        }

        @Test
        fun `returns null for empty string ID`() {
            val drill = DrillCatalog.getDrill("")
            assertThat(drill).isNull()
        }

        @Test
        fun `can find all drills by ID`() {
            val expectedIds = listOf(
                "left_leg_focus",
                "right_leg_focus",
                "smooth_circles",
                "power_transfer",
                "balance_challenge",
                "smoothness_target",
                "high_cadence_smoothness",
                "balance_recovery",
                "efficiency_builder",
                "pedaling_mastery"
            )
            expectedIds.forEach { id ->
                assertThat(DrillCatalog.getDrill(id)).isNotNull()
            }
        }
    }

    @Nested
    @DisplayName("getByType()")
    inner class GetByTypeTests {

        @Test
        fun `returns 4 TIMED_FOCUS drills`() {
            val drills = DrillCatalog.getByType(DrillType.TIMED_FOCUS)
            assertThat(drills).hasSize(4)
            drills.forEach { drill ->
                assertThat(drill.type).isEqualTo(DrillType.TIMED_FOCUS)
            }
        }

        @Test
        fun `returns 3 TARGET_BASED drills`() {
            val drills = DrillCatalog.getByType(DrillType.TARGET_BASED)
            assertThat(drills).hasSize(3)
            drills.forEach { drill ->
                assertThat(drill.type).isEqualTo(DrillType.TARGET_BASED)
            }
        }

        @Test
        fun `returns 3 GUIDED_WORKOUT drills`() {
            val drills = DrillCatalog.getByType(DrillType.GUIDED_WORKOUT)
            assertThat(drills).hasSize(3)
            drills.forEach { drill ->
                assertThat(drill.type).isEqualTo(DrillType.GUIDED_WORKOUT)
            }
        }

        @Test
        fun `all types sum to total drills`() {
            val timed = DrillCatalog.getByType(DrillType.TIMED_FOCUS).size
            val target = DrillCatalog.getByType(DrillType.TARGET_BASED).size
            val guided = DrillCatalog.getByType(DrillType.GUIDED_WORKOUT).size
            assertThat(timed + target + guided).isEqualTo(DrillCatalog.all.size)
        }
    }

    @Nested
    @DisplayName("getByDifficulty()")
    inner class GetByDifficultyTests {

        @Test
        fun `returns BEGINNER drills`() {
            val drills = DrillCatalog.getByDifficulty(DrillDifficulty.BEGINNER)
            assertThat(drills).isNotEmpty()
            drills.forEach { drill ->
                assertThat(drill.difficulty).isEqualTo(DrillDifficulty.BEGINNER)
            }
        }

        @Test
        fun `returns INTERMEDIATE drills`() {
            val drills = DrillCatalog.getByDifficulty(DrillDifficulty.INTERMEDIATE)
            assertThat(drills).isNotEmpty()
            drills.forEach { drill ->
                assertThat(drill.difficulty).isEqualTo(DrillDifficulty.INTERMEDIATE)
            }
        }

        @Test
        fun `returns ADVANCED drills`() {
            val drills = DrillCatalog.getByDifficulty(DrillDifficulty.ADVANCED)
            assertThat(drills).isNotEmpty()
            drills.forEach { drill ->
                assertThat(drill.difficulty).isEqualTo(DrillDifficulty.ADVANCED)
            }
        }

        @Test
        fun `all difficulties sum to total drills`() {
            val beginner = DrillCatalog.getByDifficulty(DrillDifficulty.BEGINNER).size
            val intermediate = DrillCatalog.getByDifficulty(DrillDifficulty.INTERMEDIATE).size
            val advanced = DrillCatalog.getByDifficulty(DrillDifficulty.ADVANCED).size
            assertThat(beginner + intermediate + advanced).isEqualTo(DrillCatalog.all.size)
        }
    }

    @Nested
    @DisplayName("Drill structure validation")
    inner class DrillStructureTests {

        @Test
        fun `all phases have positive duration`() {
            DrillCatalog.all.forEach { drill ->
                drill.phases.forEach { phase ->
                    assertThat(phase.durationMs).isGreaterThan(0L)
                }
            }
        }

        @Test
        fun `all phases have non-empty names`() {
            DrillCatalog.all.forEach { drill ->
                drill.phases.forEach { phase ->
                    assertThat(phase.name).isNotEmpty()
                }
            }
        }

        @Test
        fun `target-based drills have at least one phase with target`() {
            DrillCatalog.getByType(DrillType.TARGET_BASED).forEach { drill ->
                val hasTarget = drill.phases.any { it.target != null }
                assertThat(hasTarget).isTrue()
            }
        }

        @Test
        fun `guided workouts have multiple phases`() {
            DrillCatalog.getByType(DrillType.GUIDED_WORKOUT).forEach { drill ->
                assertThat(drill.phases.size).isAtLeast(3)
            }
        }
    }
}
