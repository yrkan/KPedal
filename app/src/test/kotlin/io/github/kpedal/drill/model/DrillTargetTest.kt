package io.github.kpedal.drill.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("DrillTarget")
class DrillTargetTest {

    @Nested
    @DisplayName("isMet()")
    inner class IsMetTests {

        @Nested
        @DisplayName("with targetValue and tolerance")
        inner class TargetValueTests {

            @Test
            fun `value at center returns true`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f
                )
                assertThat(target.isMet(50f)).isTrue()
            }

            @Test
            fun `value at lower bound returns true`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f
                )
                assertThat(target.isMet(48f)).isTrue()
            }

            @Test
            fun `value at upper bound returns true`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f
                )
                assertThat(target.isMet(52f)).isTrue()
            }

            @Test
            fun `value just below lower bound returns false`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f
                )
                assertThat(target.isMet(47.9f)).isFalse()
            }

            @Test
            fun `value just above upper bound returns false`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f
                )
                assertThat(target.isMet(52.1f)).isFalse()
            }

            @Test
            fun `zero tolerance requires exact match`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 0f
                )
                assertThat(target.isMet(50f)).isTrue()
                assertThat(target.isMet(50.1f)).isFalse()
                assertThat(target.isMet(49.9f)).isFalse()
            }

            @Test
            fun `large tolerance accepts wide range`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 10f
                )
                assertThat(target.isMet(40f)).isTrue()
                assertThat(target.isMet(60f)).isTrue()
                assertThat(target.isMet(39.9f)).isFalse()
            }
        }

        @Nested
        @DisplayName("with range (minValue and maxValue)")
        inner class RangeTests {

            @ParameterizedTest(name = "value {2} in range {0}-{1} returns {3}")
            @CsvSource(
                "70, 80, 75, true",    // Middle of range
                "70, 80, 70, true",    // At min
                "70, 80, 80, true",    // At max
                "70, 80, 69.9, false", // Just below min
                "70, 80, 80.1, false", // Just above max
                "70, 80, 50, false",   // Well below
                "70, 80, 90, false"    // Well above
            )
            fun `range check returns correct result`(
                min: Float,
                max: Float,
                value: Float,
                expected: Boolean
            ) {
                val target = DrillTarget(
                    metric = DrillMetric.TORQUE_EFFECTIVENESS,
                    minValue = min,
                    maxValue = max
                )
                assertThat(target.isMet(value)).isEqualTo(expected)
            }
        }

        @Nested
        @DisplayName("with minValue only")
        inner class MinOnlyTests {

            @Test
            fun `value above minimum returns true`() {
                val target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 20f
                )
                assertThat(target.isMet(25f)).isTrue()
            }

            @Test
            fun `value at minimum returns true`() {
                val target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 20f
                )
                assertThat(target.isMet(20f)).isTrue()
            }

            @Test
            fun `value below minimum returns false`() {
                val target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 20f
                )
                assertThat(target.isMet(19.9f)).isFalse()
            }

            @Test
            fun `very high value returns true`() {
                val target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 20f
                )
                assertThat(target.isMet(100f)).isTrue()
            }
        }

        @Nested
        @DisplayName("with maxValue only")
        inner class MaxOnlyTests {

            @Test
            fun `value below maximum returns true`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    maxValue = 48f  // Left leg should dominate
                )
                assertThat(target.isMet(45f)).isTrue()
            }

            @Test
            fun `value at maximum returns true`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    maxValue = 48f
                )
                assertThat(target.isMet(48f)).isTrue()
            }

            @Test
            fun `value above maximum returns false`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    maxValue = 48f
                )
                assertThat(target.isMet(48.1f)).isFalse()
            }

            @Test
            fun `very low value returns true`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    maxValue = 48f
                )
                assertThat(target.isMet(0f)).isTrue()
            }
        }

        @Nested
        @DisplayName("with no constraints")
        inner class NoConstraintsTests {

            @Test
            fun `any value returns true when no constraints`() {
                val target = DrillTarget(metric = DrillMetric.BALANCE)
                assertThat(target.isMet(0f)).isTrue()
                assertThat(target.isMet(50f)).isTrue()
                assertThat(target.isMet(100f)).isTrue()
            }
        }
    }

    @Nested
    @DisplayName("description()")
    inner class DescriptionTests {

        @Test
        fun `targetValue with tolerance for non-balance metric`() {
            val target = DrillTarget(
                metric = DrillMetric.TORQUE_EFFECTIVENESS,
                targetValue = 75f,
                tolerance = 5f
            )
            assertThat(target.description()).isEqualTo("75% ± 5")
        }

        @Test
        fun `targetValue without tolerance for non-balance metric`() {
            val target = DrillTarget(
                metric = DrillMetric.TORQUE_EFFECTIVENESS,
                targetValue = 75f,
                tolerance = 0f
            )
            assertThat(target.description()).isEqualTo("75%")
        }

        @Test
        fun `range for non-balance metric`() {
            val target = DrillTarget(
                metric = DrillMetric.TORQUE_EFFECTIVENESS,
                minValue = 70f,
                maxValue = 80f
            )
            assertThat(target.description()).isEqualTo("70-80%")
        }

        @Test
        fun `minOnly for non-balance metric`() {
            val target = DrillTarget(
                metric = DrillMetric.PEDAL_SMOOTHNESS,
                minValue = 20f
            )
            assertThat(target.description()).isEqualTo("≥20%")
        }

        @Test
        fun `maxOnly for non-balance metric`() {
            val target = DrillTarget(
                metric = DrillMetric.TORQUE_EFFECTIVENESS,
                maxValue = 80f
            )
            assertThat(target.description()).isEqualTo("≤80%")
        }

        @Test
        fun `no constraints returns any`() {
            val target = DrillTarget(metric = DrillMetric.TORQUE_EFFECTIVENESS)
            assertThat(target.description()).isEqualTo("any")
        }
    }

    @Nested
    @DisplayName("balanceDescription()")
    inner class BalanceDescriptionTests {

        @Test
        fun `exact target with tolerance shows L-R ranges`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 2f
            )
            // 50 ± 2 → R: 48-52, L: 48-52
            assertThat(target.description()).isEqualTo("L:48-52 R:48-52")
        }

        @Test
        fun `exact target without tolerance shows L-R values`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                targetValue = 50f,
                tolerance = 0f
            )
            assertThat(target.description()).isEqualTo("L:50 R:50")
        }

        @Test
        fun `maxValue less than 50 shows left leg focus`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                maxValue = 48f  // Right ≤48% means Left ≥52%
            )
            assertThat(target.description()).isEqualTo("L≥52%")
        }

        @Test
        fun `minValue greater than 50 shows right leg focus`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                minValue = 52f  // Right ≥52%
            )
            assertThat(target.description()).isEqualTo("R≥52%")
        }

        @Test
        fun `range shows both L and R ranges`() {
            val target = DrillTarget(
                metric = DrillMetric.BALANCE,
                minValue = 48f,
                maxValue = 52f
            )
            // R: 48-52, L: 48-52
            assertThat(target.description()).isEqualTo("L:48-52 R:48-52")
        }

        @Test
        fun `no constraints shows 50-50`() {
            val target = DrillTarget(metric = DrillMetric.BALANCE)
            assertThat(target.description()).isEqualTo("50/50")
        }
    }

    @Nested
    @DisplayName("formatCurrentValue()")
    inner class FormatCurrentValueTests {

        @Test
        fun `balance shows L and R values`() {
            val target = DrillTarget(metric = DrillMetric.BALANCE)
            assertThat(target.formatCurrentValue(55f)).isEqualTo("L:45 R:55")
        }

        @Test
        fun `balance 50-50 shows equal`() {
            val target = DrillTarget(metric = DrillMetric.BALANCE)
            assertThat(target.formatCurrentValue(50f)).isEqualTo("L:50 R:50")
        }

        @Test
        fun `non-balance shows percentage`() {
            val target = DrillTarget(metric = DrillMetric.TORQUE_EFFECTIVENESS)
            assertThat(target.formatCurrentValue(75.5f)).isEqualTo("75%")
        }

        @Test
        fun `pedal smoothness shows percentage`() {
            val target = DrillTarget(metric = DrillMetric.PEDAL_SMOOTHNESS)
            assertThat(target.formatCurrentValue(22.8f)).isEqualTo("22%")
        }
    }

    @Nested
    @DisplayName("calculateProximity()")
    inner class CalculateProximityTests {

        @Nested
        @DisplayName("with targetValue")
        inner class TargetProximityTests {

            @Test
            fun `value in target returns 0`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f
                )
                assertThat(target.calculateProximity(50f)).isEqualTo(0f)
                assertThat(target.calculateProximity(48f)).isEqualTo(0f)
                assertThat(target.calculateProximity(52f)).isEqualTo(0f)
            }

            @Test
            fun `value below target returns negative`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f
                )
                val proximity = target.calculateProximity(43f)  // 5 below lower bound (48)
                assertThat(proximity).isLessThan(0f)
            }

            @Test
            fun `value above target returns positive`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f
                )
                val proximity = target.calculateProximity(57f)  // 5 above upper bound (52)
                assertThat(proximity).isGreaterThan(0f)
            }

            @Test
            fun `proximity is clamped to -1 to 1`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    targetValue = 50f,
                    tolerance = 2f
                )
                assertThat(target.calculateProximity(0f)).isAtLeast(-1f)
                assertThat(target.calculateProximity(100f)).isAtMost(1f)
            }
        }

        @Nested
        @DisplayName("with range")
        inner class RangeProximityTests {

            @Test
            fun `value in range returns 0`() {
                val target = DrillTarget(
                    metric = DrillMetric.TORQUE_EFFECTIVENESS,
                    minValue = 70f,
                    maxValue = 80f
                )
                assertThat(target.calculateProximity(75f)).isEqualTo(0f)
            }

            @Test
            fun `value below range returns negative`() {
                val target = DrillTarget(
                    metric = DrillMetric.TORQUE_EFFECTIVENESS,
                    minValue = 70f,
                    maxValue = 80f
                )
                val proximity = target.calculateProximity(65f)
                assertThat(proximity).isLessThan(0f)
            }

            @Test
            fun `value above range returns positive`() {
                val target = DrillTarget(
                    metric = DrillMetric.TORQUE_EFFECTIVENESS,
                    minValue = 70f,
                    maxValue = 80f
                )
                val proximity = target.calculateProximity(85f)
                assertThat(proximity).isGreaterThan(0f)
            }
        }

        @Nested
        @DisplayName("with minValue only")
        inner class MinProximityTests {

            @Test
            fun `value at or above min returns 0`() {
                val target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 20f
                )
                assertThat(target.calculateProximity(20f)).isEqualTo(0f)
                assertThat(target.calculateProximity(30f)).isEqualTo(0f)
            }

            @Test
            fun `value below min returns negative`() {
                val target = DrillTarget(
                    metric = DrillMetric.PEDAL_SMOOTHNESS,
                    minValue = 20f
                )
                val proximity = target.calculateProximity(15f)
                assertThat(proximity).isLessThan(0f)
            }
        }

        @Nested
        @DisplayName("with maxValue only")
        inner class MaxProximityTests {

            @Test
            fun `value at or below max returns 0`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    maxValue = 48f
                )
                assertThat(target.calculateProximity(48f)).isEqualTo(0f)
                assertThat(target.calculateProximity(40f)).isEqualTo(0f)
            }

            @Test
            fun `value above max returns positive`() {
                val target = DrillTarget(
                    metric = DrillMetric.BALANCE,
                    maxValue = 48f
                )
                val proximity = target.calculateProximity(53f)
                assertThat(proximity).isGreaterThan(0f)
            }
        }

        @Test
        fun `no constraints returns null`() {
            val target = DrillTarget(metric = DrillMetric.BALANCE)
            assertThat(target.calculateProximity(50f)).isNull()
        }
    }

    @Nested
    @DisplayName("DrillMetric enum")
    inner class DrillMetricTests {

        @Test
        fun `all metrics are available`() {
            assertThat(DrillMetric.values()).hasLength(4)
            assertThat(DrillMetric.values()).asList().containsExactly(
                DrillMetric.BALANCE,
                DrillMetric.TORQUE_EFFECTIVENESS,
                DrillMetric.PEDAL_SMOOTHNESS,
                DrillMetric.COMBINED
            )
        }
    }
}
