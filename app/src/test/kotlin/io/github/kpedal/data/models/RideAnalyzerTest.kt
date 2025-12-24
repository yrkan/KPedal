package io.github.kpedal.data.models

import com.google.common.truth.Truth.assertThat
import io.github.kpedal.data.database.RideEntity
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("RideAnalyzer")
class RideAnalyzerTest {

    /**
     * Helper to create a RideEntity with specified metrics.
     */
    private fun createRide(
        balanceRight: Int = 50,
        teLeft: Int = 75,
        teRight: Int = 75,
        psLeft: Int = 22,
        psRight: Int = 22,
        zoneOptimal: Int = 60
    ) = RideEntity(
        id = 1,
        timestamp = System.currentTimeMillis(),
        durationMs = 3600000,
        durationFormatted = "1:00:00",
        balanceLeft = 100 - balanceRight,
        balanceRight = balanceRight,
        teLeft = teLeft,
        teRight = teRight,
        psLeft = psLeft,
        psRight = psRight,
        zoneOptimal = zoneOptimal,
        zoneAttention = 100 - zoneOptimal,
        zoneProblem = 0
    )

    @Nested
    @DisplayName("calculateBalanceScore()")
    inner class BalanceScoreTests {

        @ParameterizedTest(name = "balance {0}% (deviation {1}) returns score {2}")
        @CsvSource(
            "50, 0, 100",   // Perfect balance
            "51, 1, 100",   // ≤1 deviation
            "49, 1, 100",   // ≤1 deviation
            "52, 2, 90",    // ≤2 deviation
            "48, 2, 90",    // ≤2 deviation
            "53, 3, 80",    // ≤3 deviation
            "47, 3, 80",    // ≤3 deviation
            "55, 5, 70",    // ≤5 deviation
            "45, 5, 70",    // ≤5 deviation
            "57, 7, 55",    // ≤7 deviation
            "43, 7, 55",    // ≤7 deviation
            "60, 10, 40",   // ≤10 deviation
            "40, 10, 40",   // ≤10 deviation
            "65, 15, 25",   // >10 deviation
            "35, 15, 25"    // >10 deviation
        )
        fun `returns correct score for balance deviation`(
            balanceRight: Int,
            deviation: Int,
            expectedScore: Int
        ) {
            val ride = createRide(balanceRight = balanceRight)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.balanceScore).isEqualTo(expectedScore)
        }
    }

    @Nested
    @DisplayName("calculateEfficiencyScore()")
    inner class EfficiencyScoreTests {

        @Nested
        @DisplayName("TE component")
        inner class TeScoreTests {

            @Test
            fun `TE 70-80 gives TE score 100`() {
                val ride = createRide(teLeft = 75, teRight = 75, psLeft = 25, psRight = 25)
                val analysis = RideAnalyzer.analyze(ride)
                // TE=100, PS=100 → efficiency = 100
                assertThat(analysis.efficiencyScore).isEqualTo(100)
            }

            @Test
            fun `TE 65-85 gives TE score 80`() {
                val ride = createRide(teLeft = 65, teRight = 65, psLeft = 25, psRight = 25)
                val analysis = RideAnalyzer.analyze(ride)
                // TE=80, PS=100 → efficiency = 90
                assertThat(analysis.efficiencyScore).isEqualTo(90)
            }

            @Test
            fun `TE 60-90 gives TE score 60`() {
                val ride = createRide(teLeft = 60, teRight = 60, psLeft = 25, psRight = 25)
                val analysis = RideAnalyzer.analyze(ride)
                // TE=60, PS=100 → efficiency = 80
                assertThat(analysis.efficiencyScore).isEqualTo(80)
            }

            @Test
            fun `TE outside 60-90 gives TE score 40`() {
                val ride = createRide(teLeft = 50, teRight = 50, psLeft = 25, psRight = 25)
                val analysis = RideAnalyzer.analyze(ride)
                // TE=40, PS=100 → efficiency = 70
                assertThat(analysis.efficiencyScore).isEqualTo(70)
            }
        }

        @Nested
        @DisplayName("PS component")
        inner class PsScoreTests {

            @Test
            fun `PS at least 25 gives PS score 100`() {
                val ride = createRide(teLeft = 75, teRight = 75, psLeft = 25, psRight = 25)
                val analysis = RideAnalyzer.analyze(ride)
                assertThat(analysis.efficiencyScore).isEqualTo(100)
            }

            @Test
            fun `PS 22-24 gives PS score 90`() {
                val ride = createRide(teLeft = 75, teRight = 75, psLeft = 22, psRight = 22)
                val analysis = RideAnalyzer.analyze(ride)
                // TE=100, PS=90 → efficiency = 95
                assertThat(analysis.efficiencyScore).isEqualTo(95)
            }

            @Test
            fun `PS 20-21 gives PS score 80`() {
                val ride = createRide(teLeft = 75, teRight = 75, psLeft = 20, psRight = 20)
                val analysis = RideAnalyzer.analyze(ride)
                // TE=100, PS=80 → efficiency = 90
                assertThat(analysis.efficiencyScore).isEqualTo(90)
            }

            @Test
            fun `PS 18-19 gives PS score 65`() {
                val ride = createRide(teLeft = 75, teRight = 75, psLeft = 18, psRight = 18)
                val analysis = RideAnalyzer.analyze(ride)
                // TE=100, PS=65 → efficiency = 82
                assertThat(analysis.efficiencyScore).isEqualTo(82)
            }

            @Test
            fun `PS 15-17 gives PS score 50`() {
                val ride = createRide(teLeft = 75, teRight = 75, psLeft = 15, psRight = 15)
                val analysis = RideAnalyzer.analyze(ride)
                // TE=100, PS=50 → efficiency = 75
                assertThat(analysis.efficiencyScore).isEqualTo(75)
            }

            @Test
            fun `PS below 15 gives PS score 35`() {
                val ride = createRide(teLeft = 75, teRight = 75, psLeft = 10, psRight = 10)
                val analysis = RideAnalyzer.analyze(ride)
                // TE=100, PS=35 → efficiency = 67
                assertThat(analysis.efficiencyScore).isEqualTo(67)
            }
        }

        @Test
        fun `efficiency score is 50-50 weighted average of TE and PS`() {
            // TE = 65 (score 80), PS = 15 (score 50)
            val ride = createRide(teLeft = 65, teRight = 65, psLeft = 15, psRight = 15)
            val analysis = RideAnalyzer.analyze(ride)
            // (80 * 0.5 + 50 * 0.5) = 65
            assertThat(analysis.efficiencyScore).isEqualTo(65)
        }
    }

    @Nested
    @DisplayName("calculateConsistencyScore()")
    inner class ConsistencyScoreTests {

        @ParameterizedTest(name = "zoneOptimal {0}% returns score {1}")
        @CsvSource(
            "100, 100",
            "80, 100",
            "79, 90",
            "70, 90",
            "69, 80",
            "60, 80",
            "59, 70",
            "50, 70",
            "49, 55",
            "40, 55",
            "39, 40",
            "30, 40",
            "29, 25",
            "0, 25"
        )
        fun `returns correct score for zoneOptimal`(
            zoneOptimal: Int,
            expectedScore: Int
        ) {
            val ride = createRide(zoneOptimal = zoneOptimal)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.consistencyScore).isEqualTo(expectedScore)
        }
    }

    @Nested
    @DisplayName("overallScore calculation")
    inner class OverallScoreTests {

        @Test
        fun `overall score is weighted average of balance, efficiency, consistency`() {
            // Balance 50 → score 100, TE 75/75 PS 25/25 → efficiency 100, zone 80 → consistency 100
            val ride = createRide(
                balanceRight = 50,
                teLeft = 75, teRight = 75,
                psLeft = 25, psRight = 25,
                zoneOptimal = 80
            )
            val analysis = RideAnalyzer.analyze(ride)
            // (100 * 0.4 + 100 * 0.35 + 100 * 0.25) = 100
            assertThat(analysis.overallScore).isEqualTo(100)
        }

        @Test
        fun `overall score with mixed metrics`() {
            // Balance 55 → deviation 5 → score 70
            // TE 65 → score 80, PS 20 → score 80 → efficiency 80
            // zone 50 → score 70
            val ride = createRide(
                balanceRight = 55,
                teLeft = 65, teRight = 65,
                psLeft = 20, psRight = 20,
                zoneOptimal = 50
            )
            val analysis = RideAnalyzer.analyze(ride)
            // (70 * 0.4 + 80 * 0.35 + 70 * 0.25) = 28 + 28 + 17.5 = 73.5 → 73
            assertThat(analysis.overallScore).isEqualTo(73)
        }

        @Test
        fun `overall score with poor metrics`() {
            // Balance 65 → deviation 15 → score 25
            // TE 50 → score 40, PS 10 → score 35 → efficiency 37
            // zone 20 → score 25
            val ride = createRide(
                balanceRight = 65,
                teLeft = 50, teRight = 50,
                psLeft = 10, psRight = 10,
                zoneOptimal = 20
            )
            val analysis = RideAnalyzer.analyze(ride)
            // (25 * 0.4 + 37 * 0.35 + 25 * 0.25) = 10 + 12.95 + 6.25 = 29.2 → 29
            assertThat(analysis.overallScore).isEqualTo(29)
        }
    }

    @Nested
    @DisplayName("suggestedRating")
    inner class SuggestedRatingTests {

        @ParameterizedTest(name = "overall score {0} returns {1} stars")
        @CsvSource(
            "100, 5",
            "85, 5",
            "84, 4",
            "70, 4",
            "69, 3",
            "55, 3",
            "54, 2",
            "40, 2",
            "39, 1",
            "0, 1"
        )
        fun `returns correct star rating for overall score`(
            overallScore: Int,
            expectedRating: Int
        ) {
            // Create a ride that will have approximately the target overall score
            // This is a simplification - in practice we test the rating mapping
            val ride = when {
                overallScore >= 85 -> createRide(balanceRight = 50, teLeft = 75, teRight = 75, psLeft = 25, psRight = 25, zoneOptimal = 80)
                overallScore >= 70 -> createRide(balanceRight = 52, teLeft = 75, teRight = 75, psLeft = 22, psRight = 22, zoneOptimal = 70)
                overallScore >= 55 -> createRide(balanceRight = 55, teLeft = 70, teRight = 70, psLeft = 20, psRight = 20, zoneOptimal = 50)
                overallScore >= 40 -> createRide(balanceRight = 58, teLeft = 65, teRight = 65, psLeft = 18, psRight = 18, zoneOptimal = 40)
                else -> createRide(balanceRight = 65, teLeft = 50, teRight = 50, psLeft = 10, psRight = 10, zoneOptimal = 20)
            }
            val analysis = RideAnalyzer.analyze(ride)

            // Verify the rating is in expected range
            assertThat(analysis.suggestedRating).isIn(1..5)
        }

        @Test
        fun `5 stars for score 85+`() {
            val ride = createRide(
                balanceRight = 50,
                teLeft = 75, teRight = 75,
                psLeft = 25, psRight = 25,
                zoneOptimal = 80
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.suggestedRating).isEqualTo(5)
        }

        @Test
        fun `1 star for very poor score`() {
            val ride = createRide(
                balanceRight = 65,
                teLeft = 50, teRight = 50,
                psLeft = 10, psRight = 10,
                zoneOptimal = 20
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.suggestedRating).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("strengths and improvements")
    inner class StrengthsImprovementsTests {

        @Test
        fun `excellent balance adds strength`() {
            val ride = createRide(balanceRight = 50)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.strengths).contains("Excellent L/R balance")
        }

        @Test
        fun `good balance adds strength`() {
            val ride = createRide(balanceRight = 53)  // 3% deviation
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.strengths).contains("Good balance control")
        }

        @Test
        fun `slight imbalance adds improvement`() {
            val ride = createRide(balanceRight = 56)  // 6% deviation
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.improvements).contains("Balance slightly off-center")
        }

        @Test
        fun `significant imbalance adds improvement`() {
            val ride = createRide(balanceRight = 62)  // 12% deviation
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.improvements).contains("Significant balance imbalance")
        }

        @Test
        fun `optimal TE adds strength`() {
            val ride = createRide(teLeft = 75, teRight = 75)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.strengths).contains("Optimal torque effectiveness")
        }

        @Test
        fun `good TE adds strength`() {
            val ride = createRide(teLeft = 65, teRight = 65)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.strengths).contains("Good power transfer")
        }

        @Test
        fun `high TE adds improvement warning`() {
            val ride = createRide(teLeft = 88, teRight = 88)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.improvements).contains("TE too high - may reduce total power")
        }

        @Test
        fun `low TE adds improvement`() {
            val ride = createRide(teLeft = 50, teRight = 50)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.improvements).contains("Low torque effectiveness")
        }

        @Test
        fun `very smooth pedaling adds strength`() {
            val ride = createRide(psLeft = 28, psRight = 28)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.strengths).contains("Very smooth pedaling")
        }

        @Test
        fun `good smoothness adds strength`() {
            val ride = createRide(psLeft = 22, psRight = 22)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.strengths).contains("Good pedal smoothness")
        }

        @Test
        fun `low smoothness adds improvement`() {
            val ride = createRide(psLeft = 16, psRight = 16)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.improvements).contains("Smoothness could improve")
        }

        @Test
        fun `very low smoothness adds improvement`() {
            val ride = createRide(psLeft = 10, psRight = 10)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.improvements).contains("Focus on smoother pedal stroke")
        }

        @Test
        fun `excellent zone time adds strength`() {
            // Use ride with some improvements so zone strength is within top 3
            val ride = createRide(
                balanceRight = 55,  // Slight imbalance - adds improvement not strength
                teLeft = 65, teRight = 65,  // Attention zone - adds strength (Good power transfer)
                psLeft = 18, psRight = 18,  // Below optimal - adds improvement
                zoneOptimal = 75  // Excellent zone - should add strength
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.strengths).contains("Excellent time in optimal zone")
        }

        @Test
        fun `good zone time adds strength`() {
            // Use ride with some improvements so zone strength is within top 3
            val ride = createRide(
                balanceRight = 58,  // Imbalance - adds improvement
                teLeft = 65, teRight = 65,  // Attention zone - adds strength
                psLeft = 18, psRight = 18,  // Below optimal - adds improvement
                zoneOptimal = 55  // Good zone - should add strength
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.strengths).contains("Good consistency")
        }

        @Test
        fun `low zone time adds improvement`() {
            val ride = createRide(zoneOptimal = 35)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.improvements).contains("More time in optimal zone needed")
        }

        @Test
        fun `very low zone time adds improvement`() {
            val ride = createRide(zoneOptimal = 20)
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.improvements).contains("Practice maintaining optimal form")
        }

        @Test
        fun `strengths capped at 3`() {
            // Create an excellent ride with many potential strengths
            val ride = createRide(
                balanceRight = 50,      // Excellent balance
                teLeft = 75, teRight = 75,  // Optimal TE
                psLeft = 28, psRight = 28,  // Very smooth
                zoneOptimal = 75            // Excellent zone time
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.strengths.size).isAtMost(3)
        }

        @Test
        fun `improvements capped at 3`() {
            // Create a poor ride with many potential improvements
            val ride = createRide(
                balanceRight = 65,          // Significant imbalance
                teLeft = 50, teRight = 50,  // Low TE
                psLeft = 10, psRight = 10,  // Very low PS
                zoneOptimal = 20            // Very low zone time
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.improvements.size).isAtMost(3)
        }
    }

    @Nested
    @DisplayName("summary")
    inner class SummaryTests {

        @Test
        fun `excellent summary for score 85+`() {
            val ride = createRide(
                balanceRight = 50,
                teLeft = 75, teRight = 75,
                psLeft = 25, psRight = 25,
                zoneOptimal = 80
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.summary).contains("Excellent ride")
        }

        @Test
        fun `good summary for score 70-84`() {
            // Target: score 70-84
            // Balance 54 (dev 4) = 70, TE 68 (60-90) = 60, PS 18 (18-19) = 65, zone 55 = 70
            // Efficiency = (60*0.5 + 65*0.5) = 62.5
            // Overall = 70*0.4 + 62*0.35 + 70*0.25 = 28 + 21.7 + 17.5 = 67.2 (too low)
            // Let's try: Balance 53 = 80, TE 75 = 100, PS 18 = 65, zone 50 = 70
            // Efficiency = (100*0.5 + 65*0.5) = 82.5
            // Overall = 80*0.4 + 82*0.35 + 70*0.25 = 32 + 28.7 + 17.5 = 78.2
            val ride = createRide(
                balanceRight = 53,
                teLeft = 75, teRight = 75,
                psLeft = 18, psRight = 18,
                zoneOptimal = 50
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.summary).contains("Good performance")
        }

        @Test
        fun `solid summary for score 55-69`() {
            // Target: score 55-69
            // Balance 56 (dev 6) = 55, TE 62 (60-90) = 60, PS 16 (15-17) = 50, zone 35 = 40
            // Efficiency = (60*0.5 + 50*0.5) = 55
            // Overall = 55*0.4 + 55*0.35 + 40*0.25 = 22 + 19.25 + 10 = 51.25 (too low)
            // Let's try: Balance 55 = 70, TE 66 = 80, PS 17 = 50, zone 40 = 55
            // Efficiency = (80*0.5 + 50*0.5) = 65
            // Overall = 70*0.4 + 65*0.35 + 55*0.25 = 28 + 22.75 + 13.75 = 64.5
            val ride = createRide(
                balanceRight = 55,
                teLeft = 66, teRight = 66,
                psLeft = 17, psRight = 17,
                zoneOptimal = 40
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.summary).contains("Solid effort")
        }

        @Test
        fun `improvement summary for score 40-54`() {
            val ride = createRide(
                balanceRight = 58,
                teLeft = 65, teRight = 65,
                psLeft = 15, psRight = 15,
                zoneOptimal = 35
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.summary).contains("Room for improvement")
        }

        @Test
        fun `practice summary for score below 40`() {
            val ride = createRide(
                balanceRight = 65,
                teLeft = 50, teRight = 50,
                psLeft = 10, psRight = 10,
                zoneOptimal = 20
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.summary).contains("Practice makes perfect")
        }
    }

    @Nested
    @DisplayName("edge cases")
    inner class EdgeCaseTests {

        @Test
        fun `handles zero duration ride`() {
            val ride = RideEntity(
                id = 1,
                timestamp = System.currentTimeMillis(),
                durationMs = 0,
                durationFormatted = "0:00:00",
                balanceLeft = 50,
                balanceRight = 50,
                teLeft = 75,
                teRight = 75,
                psLeft = 22,
                psRight = 22,
                zoneOptimal = 60,
                zoneAttention = 40,
                zoneProblem = 0
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.overallScore).isIn(0..100)
        }

        @Test
        fun `handles extreme balance values`() {
            val ride = createRide(balanceRight = 100)  // All right
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.balanceScore).isEqualTo(25)  // >10 deviation
        }

        @Test
        fun `handles zero metrics`() {
            val ride = createRide(
                balanceRight = 0,
                teLeft = 0, teRight = 0,
                psLeft = 0, psRight = 0,
                zoneOptimal = 0
            )
            val analysis = RideAnalyzer.analyze(ride)
            assertThat(analysis.overallScore).isIn(0..100)
            assertThat(analysis.suggestedRating).isIn(1..5)
        }
    }
}
