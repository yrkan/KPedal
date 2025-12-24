package io.github.kpedal.engine

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PedalingMetrics")
class PedalingMetricsTest {

    @Nested
    @DisplayName("balanceLeft")
    inner class BalanceLeftTests {

        @Test
        fun `balanceLeft is 100 minus balance`() {
            val metrics = PedalingMetrics(balance = 55f)
            assertThat(metrics.balanceLeft).isEqualTo(45f)
        }

        @Test
        fun `balanced 50-50 returns 50`() {
            val metrics = PedalingMetrics(balance = 50f)
            assertThat(metrics.balanceLeft).isEqualTo(50f)
        }

        @Test
        fun `extreme right (balance=100) returns 0`() {
            val metrics = PedalingMetrics(balance = 100f)
            assertThat(metrics.balanceLeft).isEqualTo(0f)
        }

        @Test
        fun `extreme left (balance=0) returns 100`() {
            val metrics = PedalingMetrics(balance = 0f)
            assertThat(metrics.balanceLeft).isEqualTo(100f)
        }
    }

    @Nested
    @DisplayName("balance3sLeft")
    inner class Balance3sLeftTests {

        @Test
        fun `balance3sLeft is 100 minus balance3s`() {
            val metrics = PedalingMetrics(balance3s = 52f)
            assertThat(metrics.balance3sLeft).isEqualTo(48f)
        }

        @Test
        fun `default balance3s 50 returns 50`() {
            val metrics = PedalingMetrics()
            assertThat(metrics.balance3sLeft).isEqualTo(50f)
        }
    }

    @Nested
    @DisplayName("balance10sLeft")
    inner class Balance10sLeftTests {

        @Test
        fun `balance10sLeft is 100 minus balance10s`() {
            val metrics = PedalingMetrics(balance10s = 48f)
            assertThat(metrics.balance10sLeft).isEqualTo(52f)
        }

        @Test
        fun `default balance10s 50 returns 50`() {
            val metrics = PedalingMetrics()
            assertThat(metrics.balance10sLeft).isEqualTo(50f)
        }
    }

    @Nested
    @DisplayName("torqueEffAvg")
    inner class TorqueEffAvgTests {

        @Test
        fun `average of equal values`() {
            val metrics = PedalingMetrics(
                torqueEffLeft = 75f,
                torqueEffRight = 75f
            )
            assertThat(metrics.torqueEffAvg).isEqualTo(75f)
        }

        @Test
        fun `average of different values`() {
            val metrics = PedalingMetrics(
                torqueEffLeft = 70f,
                torqueEffRight = 80f
            )
            assertThat(metrics.torqueEffAvg).isEqualTo(75f)
        }

        @Test
        fun `average with zeros`() {
            val metrics = PedalingMetrics(
                torqueEffLeft = 0f,
                torqueEffRight = 0f
            )
            assertThat(metrics.torqueEffAvg).isEqualTo(0f)
        }

        @Test
        fun `average with one zero`() {
            val metrics = PedalingMetrics(
                torqueEffLeft = 80f,
                torqueEffRight = 0f
            )
            assertThat(metrics.torqueEffAvg).isEqualTo(40f)
        }
    }

    @Nested
    @DisplayName("pedalSmoothAvg")
    inner class PedalSmoothAvgTests {

        @Test
        fun `average of equal values`() {
            val metrics = PedalingMetrics(
                pedalSmoothLeft = 22f,
                pedalSmoothRight = 22f
            )
            assertThat(metrics.pedalSmoothAvg).isEqualTo(22f)
        }

        @Test
        fun `average of different values`() {
            val metrics = PedalingMetrics(
                pedalSmoothLeft = 20f,
                pedalSmoothRight = 30f
            )
            assertThat(metrics.pedalSmoothAvg).isEqualTo(25f)
        }

        @Test
        fun `average with zeros`() {
            val metrics = PedalingMetrics(
                pedalSmoothLeft = 0f,
                pedalSmoothRight = 0f
            )
            assertThat(metrics.pedalSmoothAvg).isEqualTo(0f)
        }
    }

    @Nested
    @DisplayName("hasData")
    inner class HasDataTests {

        @Test
        fun `hasData is false when timestamp is 0`() {
            val metrics = PedalingMetrics(timestamp = 0L)
            assertThat(metrics.hasData).isFalse()
        }

        @Test
        fun `hasData is true when timestamp is positive`() {
            val metrics = PedalingMetrics(timestamp = System.currentTimeMillis())
            assertThat(metrics.hasData).isTrue()
        }

        @Test
        fun `hasData is true with any positive timestamp`() {
            val metrics = PedalingMetrics(timestamp = 1L)
            assertThat(metrics.hasData).isTrue()
        }

        @Test
        fun `default timestamp returns no data`() {
            val metrics = PedalingMetrics()
            assertThat(metrics.hasData).isFalse()
        }
    }

    @Nested
    @DisplayName("Default values")
    inner class DefaultValuesTests {

        @Test
        fun `default balance is 50`() {
            val metrics = PedalingMetrics()
            assertThat(metrics.balance).isEqualTo(50f)
        }

        @Test
        fun `default torqueEff values are 0`() {
            val metrics = PedalingMetrics()
            assertThat(metrics.torqueEffLeft).isEqualTo(0f)
            assertThat(metrics.torqueEffRight).isEqualTo(0f)
        }

        @Test
        fun `default pedalSmooth values are 0`() {
            val metrics = PedalingMetrics()
            assertThat(metrics.pedalSmoothLeft).isEqualTo(0f)
            assertThat(metrics.pedalSmoothRight).isEqualTo(0f)
        }

        @Test
        fun `default power and cadence are 0`() {
            val metrics = PedalingMetrics()
            assertThat(metrics.power).isEqualTo(0)
            assertThat(metrics.cadence).isEqualTo(0)
        }

        @Test
        fun `default smoothed balance values are 50`() {
            val metrics = PedalingMetrics()
            assertThat(metrics.balance3s).isEqualTo(50f)
            assertThat(metrics.balance10s).isEqualTo(50f)
        }
    }
}
