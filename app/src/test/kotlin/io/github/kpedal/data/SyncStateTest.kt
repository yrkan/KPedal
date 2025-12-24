package io.github.kpedal.data

import com.google.common.truth.Truth.assertThat
import io.github.kpedal.data.SyncService.SyncState
import io.github.kpedal.data.SyncService.SyncStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("SyncService")
class SyncStateTest {

    @Nested
    @DisplayName("SyncStatus enum")
    inner class SyncStatusTests {

        @Test
        fun `has all expected values`() {
            val values = SyncStatus.values()
            assertThat(values).hasLength(5)
            assertThat(values).asList().containsExactly(
                SyncStatus.IDLE,
                SyncStatus.SYNCING,
                SyncStatus.SUCCESS,
                SyncStatus.FAILED,
                SyncStatus.OFFLINE
            )
        }

        @ParameterizedTest(name = "SyncStatus.{0} can be converted to and from string")
        @EnumSource(SyncStatus::class)
        fun `can be serialized and deserialized`(status: SyncStatus) {
            val serialized = status.name
            val deserialized = SyncStatus.valueOf(serialized)
            assertThat(deserialized).isEqualTo(status)
        }

        @Test
        fun `IDLE is default state`() {
            assertThat(SyncStatus.IDLE.ordinal).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("SyncState data class")
    inner class SyncStateDataTests {

        @Test
        fun `default values are correct`() {
            val state = SyncState()

            assertThat(state.status).isEqualTo(SyncStatus.IDLE)
            assertThat(state.pendingCount).isEqualTo(0)
            assertThat(state.lastSyncTimestamp).isEqualTo(0L)
            assertThat(state.errorMessage).isNull()
        }

        @Test
        fun `can be created with custom values`() {
            val state = SyncState(
                status = SyncStatus.SYNCING,
                pendingCount = 5,
                lastSyncTimestamp = 1234567890L,
                errorMessage = "Test error"
            )

            assertThat(state.status).isEqualTo(SyncStatus.SYNCING)
            assertThat(state.pendingCount).isEqualTo(5)
            assertThat(state.lastSyncTimestamp).isEqualTo(1234567890L)
            assertThat(state.errorMessage).isEqualTo("Test error")
        }

        @Test
        fun `copy preserves unchanged values`() {
            val original = SyncState(
                status = SyncStatus.IDLE,
                pendingCount = 3,
                lastSyncTimestamp = 1000L,
                errorMessage = null
            )

            val copied = original.copy(status = SyncStatus.SYNCING)

            assertThat(copied.status).isEqualTo(SyncStatus.SYNCING)
            assertThat(copied.pendingCount).isEqualTo(3)
            assertThat(copied.lastSyncTimestamp).isEqualTo(1000L)
            assertThat(copied.errorMessage).isNull()
        }

        @Test
        fun `copy can update multiple values`() {
            val original = SyncState()

            val updated = original.copy(
                status = SyncStatus.FAILED,
                errorMessage = "Network error"
            )

            assertThat(updated.status).isEqualTo(SyncStatus.FAILED)
            assertThat(updated.errorMessage).isEqualTo("Network error")
            assertThat(updated.pendingCount).isEqualTo(0)
        }

        @Test
        fun `equals works correctly`() {
            val state1 = SyncState(status = SyncStatus.IDLE, pendingCount = 5)
            val state2 = SyncState(status = SyncStatus.IDLE, pendingCount = 5)
            val state3 = SyncState(status = SyncStatus.SYNCING, pendingCount = 5)

            assertThat(state1).isEqualTo(state2)
            assertThat(state1).isNotEqualTo(state3)
        }

        @Test
        fun `hashCode is consistent`() {
            val state1 = SyncState(status = SyncStatus.SUCCESS, pendingCount = 10)
            val state2 = SyncState(status = SyncStatus.SUCCESS, pendingCount = 10)

            assertThat(state1.hashCode()).isEqualTo(state2.hashCode())
        }
    }

    @Nested
    @DisplayName("SyncState transitions")
    inner class SyncStateTransitionTests {

        @Test
        fun `IDLE to SYNCING transition`() {
            val idle = SyncState(status = SyncStatus.IDLE, pendingCount = 3)
            val syncing = idle.copy(status = SyncStatus.SYNCING, errorMessage = null)

            assertThat(syncing.status).isEqualTo(SyncStatus.SYNCING)
            assertThat(syncing.pendingCount).isEqualTo(3)
        }

        @Test
        fun `SYNCING to SUCCESS transition with timestamp update`() {
            val syncing = SyncState(status = SyncStatus.SYNCING, pendingCount = 3)
            val timestamp = System.currentTimeMillis()
            val success = syncing.copy(
                status = SyncStatus.SUCCESS,
                pendingCount = 0,
                lastSyncTimestamp = timestamp
            )

            assertThat(success.status).isEqualTo(SyncStatus.SUCCESS)
            assertThat(success.pendingCount).isEqualTo(0)
            assertThat(success.lastSyncTimestamp).isEqualTo(timestamp)
        }

        @Test
        fun `SYNCING to FAILED transition with error message`() {
            val syncing = SyncState(status = SyncStatus.SYNCING, pendingCount = 3)
            val failed = syncing.copy(
                status = SyncStatus.FAILED,
                errorMessage = "Connection timeout"
            )

            assertThat(failed.status).isEqualTo(SyncStatus.FAILED)
            assertThat(failed.pendingCount).isEqualTo(3)
            assertThat(failed.errorMessage).isEqualTo("Connection timeout")
        }

        @Test
        fun `IDLE to OFFLINE transition when no network`() {
            val idle = SyncState(status = SyncStatus.IDLE, pendingCount = 5)
            val offline = idle.copy(
                status = SyncStatus.OFFLINE,
                errorMessage = "No network connection"
            )

            assertThat(offline.status).isEqualTo(SyncStatus.OFFLINE)
            assertThat(offline.errorMessage).isEqualTo("No network connection")
        }

        @Test
        fun `pendingCount updates independently of status`() {
            val state = SyncState(pendingCount = 0)

            val withPending = state.copy(pendingCount = 5)
            assertThat(withPending.pendingCount).isEqualTo(5)
            assertThat(withPending.status).isEqualTo(SyncStatus.IDLE)

            val moreePending = withPending.copy(pendingCount = 10)
            assertThat(moreePending.pendingCount).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("SyncState edge cases")
    inner class SyncStateEdgeCaseTests {

        @Test
        fun `handles large pending count`() {
            val state = SyncState(pendingCount = Int.MAX_VALUE)
            assertThat(state.pendingCount).isEqualTo(Int.MAX_VALUE)
        }

        @Test
        fun `handles max timestamp`() {
            val state = SyncState(lastSyncTimestamp = Long.MAX_VALUE)
            assertThat(state.lastSyncTimestamp).isEqualTo(Long.MAX_VALUE)
        }

        @Test
        fun `handles empty error message`() {
            val state = SyncState(errorMessage = "")
            assertThat(state.errorMessage).isEmpty()
        }

        @Test
        fun `handles long error message`() {
            val longMessage = "E".repeat(1000)
            val state = SyncState(errorMessage = longMessage)
            assertThat(state.errorMessage).hasLength(1000)
        }
    }
}
