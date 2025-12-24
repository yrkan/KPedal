package io.github.kpedal.data.database

import com.google.common.truth.Truth.assertThat
import io.github.kpedal.api.SyncRideRequest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RideEntity")
class RideEntityTest {

    private fun createTestRide(
        id: Long = 0,
        timestamp: Long = System.currentTimeMillis(),
        durationMs: Long = 3600000L,
        balanceLeft: Int = 48,
        balanceRight: Int = 52,
        teLeft: Int = 75,
        teRight: Int = 73,
        psLeft: Int = 22,
        psRight: Int = 24,
        zoneOptimal: Int = 60,
        zoneAttention: Int = 30,
        zoneProblem: Int = 10,
        syncStatus: Int = RideEntity.SYNC_STATUS_PENDING
    ) = RideEntity(
        id = id,
        timestamp = timestamp,
        durationMs = durationMs,
        durationFormatted = "1:00:00",
        balanceLeft = balanceLeft,
        balanceRight = balanceRight,
        teLeft = teLeft,
        teRight = teRight,
        psLeft = psLeft,
        psRight = psRight,
        zoneOptimal = zoneOptimal,
        zoneAttention = zoneAttention,
        zoneProblem = zoneProblem,
        syncStatus = syncStatus
    )

    @Nested
    @DisplayName("Sync status constants")
    inner class SyncStatusConstantsTests {

        @Test
        fun `SYNC_STATUS_PENDING is 0`() {
            assertThat(RideEntity.SYNC_STATUS_PENDING).isEqualTo(0)
        }

        @Test
        fun `SYNC_STATUS_SYNCED is 1`() {
            assertThat(RideEntity.SYNC_STATUS_SYNCED).isEqualTo(1)
        }

        @Test
        fun `SYNC_STATUS_FAILED is 2`() {
            assertThat(RideEntity.SYNC_STATUS_FAILED).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("Default values")
    inner class DefaultValuesTests {

        @Test
        fun `id defaults to 0`() {
            val ride = createTestRide()
            assertThat(ride.id).isEqualTo(0)
        }

        @Test
        fun `savedManually defaults to false`() {
            val ride = createTestRide()
            assertThat(ride.savedManually).isFalse()
        }

        @Test
        fun `rating defaults to 0 (unrated)`() {
            val ride = createTestRide()
            assertThat(ride.rating).isEqualTo(0)
        }

        @Test
        fun `notes defaults to empty string`() {
            val ride = createTestRide()
            assertThat(ride.notes).isEmpty()
        }

        @Test
        fun `syncStatus defaults to PENDING`() {
            val ride = createTestRide()
            assertThat(ride.syncStatus).isEqualTo(RideEntity.SYNC_STATUS_PENDING)
        }

        @Test
        fun `lastSyncAttempt defaults to 0`() {
            val ride = createTestRide()
            assertThat(ride.lastSyncAttempt).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Balance validation")
    inner class BalanceValidationTests {

        @Test
        fun `balance left and right should sum to 100`() {
            val ride = createTestRide(balanceLeft = 48, balanceRight = 52)
            assertThat(ride.balanceLeft + ride.balanceRight).isEqualTo(100)
        }

        @Test
        fun `perfect balance is 50-50`() {
            val ride = createTestRide(balanceLeft = 50, balanceRight = 50)
            assertThat(ride.balanceLeft).isEqualTo(50)
            assertThat(ride.balanceRight).isEqualTo(50)
        }

        @Test
        fun `extreme left bias`() {
            val ride = createTestRide(balanceLeft = 70, balanceRight = 30)
            assertThat(ride.balanceLeft).isGreaterThan(ride.balanceRight)
        }

        @Test
        fun `extreme right bias`() {
            val ride = createTestRide(balanceLeft = 30, balanceRight = 70)
            assertThat(ride.balanceRight).isGreaterThan(ride.balanceLeft)
        }
    }

    @Nested
    @DisplayName("Zone percentages")
    inner class ZonePercentagesTests {

        @Test
        fun `zones should sum to 100`() {
            val ride = createTestRide(zoneOptimal = 40, zoneAttention = 35, zoneProblem = 25)
            val sum = ride.zoneOptimal + ride.zoneAttention + ride.zoneProblem
            assertThat(sum).isEqualTo(100)
        }

        @Test
        fun `all optimal ride`() {
            val ride = createTestRide(zoneOptimal = 100, zoneAttention = 0, zoneProblem = 0)
            assertThat(ride.zoneOptimal).isEqualTo(100)
        }

        @Test
        fun `all problem ride`() {
            val ride = createTestRide(zoneOptimal = 0, zoneAttention = 0, zoneProblem = 100)
            assertThat(ride.zoneProblem).isEqualTo(100)
        }
    }

    @Nested
    @DisplayName("Rating")
    inner class RatingTests {

        @Test
        fun `unrated is 0`() {
            val ride = createTestRide()
            assertThat(ride.rating).isEqualTo(0)
        }

        @Test
        fun `can be rated 1-5`() {
            for (stars in 1..5) {
                val ride = createTestRide().copy(rating = stars)
                assertThat(ride.rating).isEqualTo(stars)
            }
        }
    }

    @Nested
    @DisplayName("Sync status transitions")
    inner class SyncStatusTransitionsTests {

        @Test
        fun `new ride is pending`() {
            val ride = createTestRide()
            assertThat(ride.syncStatus).isEqualTo(RideEntity.SYNC_STATUS_PENDING)
        }

        @Test
        fun `can transition to synced`() {
            val ride = createTestRide().copy(syncStatus = RideEntity.SYNC_STATUS_SYNCED)
            assertThat(ride.syncStatus).isEqualTo(RideEntity.SYNC_STATUS_SYNCED)
        }

        @Test
        fun `can transition to failed`() {
            val ride = createTestRide().copy(syncStatus = RideEntity.SYNC_STATUS_FAILED)
            assertThat(ride.syncStatus).isEqualTo(RideEntity.SYNC_STATUS_FAILED)
        }

        @Test
        fun `lastSyncAttempt is updated on sync`() {
            val now = System.currentTimeMillis()
            val ride = createTestRide().copy(lastSyncAttempt = now)
            assertThat(ride.lastSyncAttempt).isEqualTo(now)
        }
    }

    @Nested
    @DisplayName("Data class behavior")
    inner class DataClassBehaviorTests {

        @Test
        fun `equals compares all fields`() {
            val ride1 = createTestRide(id = 1, timestamp = 1000L)
            val ride2 = createTestRide(id = 1, timestamp = 1000L)
            val ride3 = createTestRide(id = 2, timestamp = 1000L)

            assertThat(ride1).isEqualTo(ride2)
            assertThat(ride1).isNotEqualTo(ride3)
        }

        @Test
        fun `copy preserves unchanged fields`() {
            val original = createTestRide(id = 5, timestamp = 1234567890L)
            val copied = original.copy(rating = 5)

            assertThat(copied.id).isEqualTo(5)
            assertThat(copied.timestamp).isEqualTo(1234567890L)
            assertThat(copied.rating).isEqualTo(5)
        }
    }

    @Nested
    @DisplayName("Conversion to SyncRideRequest")
    inner class ConversionToSyncRequestTests {

        private fun RideEntity.toSyncRequest() = SyncRideRequest(
            timestamp = this.timestamp,
            duration = this.durationMs,
            balance_left_avg = this.balanceLeft,
            balance_right_avg = this.balanceRight,
            te_left_avg = this.teLeft,
            te_right_avg = this.teRight,
            ps_left_avg = this.psLeft,
            ps_right_avg = this.psRight,
            optimal_pct = this.zoneOptimal,
            attention_pct = this.zoneAttention,
            problem_pct = this.zoneProblem
        )

        @Test
        fun `converts timestamp correctly`() {
            val timestamp = 1700000000000L
            val ride = createTestRide(timestamp = timestamp)
            val request = ride.toSyncRequest()

            assertThat(request.timestamp).isEqualTo(timestamp)
        }

        @Test
        fun `converts duration correctly`() {
            val durationMs = 7200000L // 2 hours
            val ride = createTestRide(durationMs = durationMs)
            val request = ride.toSyncRequest()

            assertThat(request.duration).isEqualTo(durationMs)
        }

        @Test
        fun `converts balance correctly`() {
            val ride = createTestRide(balanceLeft = 47, balanceRight = 53)
            val request = ride.toSyncRequest()

            assertThat(request.balance_left_avg).isEqualTo(47)
            assertThat(request.balance_right_avg).isEqualTo(53)
        }

        @Test
        fun `converts TE correctly`() {
            val ride = createTestRide(teLeft = 78, teRight = 76)
            val request = ride.toSyncRequest()

            assertThat(request.te_left_avg).isEqualTo(78)
            assertThat(request.te_right_avg).isEqualTo(76)
        }

        @Test
        fun `converts PS correctly`() {
            val ride = createTestRide(psLeft = 25, psRight = 23)
            val request = ride.toSyncRequest()

            assertThat(request.ps_left_avg).isEqualTo(25)
            assertThat(request.ps_right_avg).isEqualTo(23)
        }

        @Test
        fun `converts zones correctly`() {
            val ride = createTestRide(zoneOptimal = 70, zoneAttention = 20, zoneProblem = 10)
            val request = ride.toSyncRequest()

            assertThat(request.optimal_pct).isEqualTo(70)
            assertThat(request.attention_pct).isEqualTo(20)
            assertThat(request.problem_pct).isEqualTo(10)
        }

        @Test
        fun `full ride converts correctly`() {
            val ride = RideEntity(
                id = 42,
                timestamp = 1700000000000L,
                durationMs = 5400000L, // 1.5 hours
                durationFormatted = "1:30:00",
                balanceLeft = 49,
                balanceRight = 51,
                teLeft = 74,
                teRight = 72,
                psLeft = 21,
                psRight = 23,
                zoneOptimal = 55,
                zoneAttention = 35,
                zoneProblem = 10,
                savedManually = true,
                rating = 4,
                notes = "Good ride",
                syncStatus = RideEntity.SYNC_STATUS_PENDING,
                lastSyncAttempt = 0
            )

            val request = ride.toSyncRequest()

            assertThat(request.timestamp).isEqualTo(1700000000000L)
            assertThat(request.duration).isEqualTo(5400000L)
            assertThat(request.balance_left_avg).isEqualTo(49)
            assertThat(request.balance_right_avg).isEqualTo(51)
            assertThat(request.te_left_avg).isEqualTo(74)
            assertThat(request.te_right_avg).isEqualTo(72)
            assertThat(request.ps_left_avg).isEqualTo(21)
            assertThat(request.ps_right_avg).isEqualTo(23)
            assertThat(request.optimal_pct).isEqualTo(55)
            assertThat(request.attention_pct).isEqualTo(35)
            assertThat(request.problem_pct).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCaseTests {

        @Test
        fun `handles zero duration`() {
            val ride = createTestRide(durationMs = 0)
            assertThat(ride.durationMs).isEqualTo(0)
        }

        @Test
        fun `handles max duration`() {
            val ride = createTestRide(durationMs = Long.MAX_VALUE)
            assertThat(ride.durationMs).isEqualTo(Long.MAX_VALUE)
        }

        @Test
        fun `handles unicode in notes`() {
            val ride = createTestRide().copy(notes = "Отличная тренировка! 🚴‍♂️")
            assertThat(ride.notes).isEqualTo("Отличная тренировка! 🚴‍♂️")
        }

        @Test
        fun `handles long notes`() {
            val longNotes = "a".repeat(1000)
            val ride = createTestRide().copy(notes = longNotes)
            assertThat(ride.notes).hasLength(1000)
        }
    }
}
