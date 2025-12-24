package io.github.kpedal.api

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Sync API Models")
class SyncApiModelsTest {

    @Nested
    @DisplayName("SyncRideRequest")
    inner class SyncRideRequestTests {

        @Test
        fun `can be created with all fields`() {
            val request = SyncRideRequest(
                timestamp = 1700000000000L,
                duration = 3600000L,
                balance_left_avg = 48,
                balance_right_avg = 52,
                te_left_avg = 75,
                te_right_avg = 73,
                ps_left_avg = 22,
                ps_right_avg = 24,
                optimal_pct = 60,
                attention_pct = 30,
                problem_pct = 10
            )

            assertThat(request.timestamp).isEqualTo(1700000000000L)
            assertThat(request.duration).isEqualTo(3600000L)
            assertThat(request.balance_left_avg).isEqualTo(48)
            assertThat(request.balance_right_avg).isEqualTo(52)
            assertThat(request.te_left_avg).isEqualTo(75)
            assertThat(request.te_right_avg).isEqualTo(73)
            assertThat(request.ps_left_avg).isEqualTo(22)
            assertThat(request.ps_right_avg).isEqualTo(24)
            assertThat(request.optimal_pct).isEqualTo(60)
            assertThat(request.attention_pct).isEqualTo(30)
            assertThat(request.problem_pct).isEqualTo(10)
        }

        @Test
        fun `zone percentages should sum to 100`() {
            val request = SyncRideRequest(
                timestamp = 0,
                duration = 0,
                balance_left_avg = 50,
                balance_right_avg = 50,
                te_left_avg = 70,
                te_right_avg = 70,
                ps_left_avg = 20,
                ps_right_avg = 20,
                optimal_pct = 40,
                attention_pct = 35,
                problem_pct = 25
            )

            val sum = request.optimal_pct + request.attention_pct + request.problem_pct
            assertThat(sum).isEqualTo(100)
        }

        @Test
        fun `balance left and right should sum to 100`() {
            val request = SyncRideRequest(
                timestamp = 0,
                duration = 0,
                balance_left_avg = 48,
                balance_right_avg = 52,
                te_left_avg = 70,
                te_right_avg = 70,
                ps_left_avg = 20,
                ps_right_avg = 20,
                optimal_pct = 100,
                attention_pct = 0,
                problem_pct = 0
            )

            val sum = request.balance_left_avg + request.balance_right_avg
            assertThat(sum).isEqualTo(100)
        }

        @Test
        fun `handles zero values`() {
            val request = SyncRideRequest(
                timestamp = 0,
                duration = 0,
                balance_left_avg = 0,
                balance_right_avg = 0,
                te_left_avg = 0,
                te_right_avg = 0,
                ps_left_avg = 0,
                ps_right_avg = 0,
                optimal_pct = 0,
                attention_pct = 0,
                problem_pct = 0
            )

            assertThat(request.timestamp).isEqualTo(0L)
            assertThat(request.duration).isEqualTo(0L)
        }

        @Test
        fun `handles max timestamp`() {
            val request = SyncRideRequest(
                timestamp = Long.MAX_VALUE,
                duration = Long.MAX_VALUE,
                balance_left_avg = 50,
                balance_right_avg = 50,
                te_left_avg = 75,
                te_right_avg = 75,
                ps_left_avg = 25,
                ps_right_avg = 25,
                optimal_pct = 100,
                attention_pct = 0,
                problem_pct = 0
            )

            assertThat(request.timestamp).isEqualTo(Long.MAX_VALUE)
        }

        @Test
        fun `equals and hashCode work correctly`() {
            val request1 = SyncRideRequest(
                timestamp = 1000L,
                duration = 3600000L,
                balance_left_avg = 48,
                balance_right_avg = 52,
                te_left_avg = 75,
                te_right_avg = 73,
                ps_left_avg = 22,
                ps_right_avg = 24,
                optimal_pct = 60,
                attention_pct = 30,
                problem_pct = 10
            )

            val request2 = SyncRideRequest(
                timestamp = 1000L,
                duration = 3600000L,
                balance_left_avg = 48,
                balance_right_avg = 52,
                te_left_avg = 75,
                te_right_avg = 73,
                ps_left_avg = 22,
                ps_right_avg = 24,
                optimal_pct = 60,
                attention_pct = 30,
                problem_pct = 10
            )

            assertThat(request1).isEqualTo(request2)
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
        }
    }

    @Nested
    @DisplayName("SyncResponse")
    inner class SyncResponseTests {

        @Test
        fun `successful sync with data`() {
            val data = SyncData(ride_id = "ride_123", synced_at = "2024-01-15T10:30:00Z")
            val response = SyncResponse(success = true, data = data)

            assertThat(response.success).isTrue()
            assertThat(response.data).isNotNull()
            assertThat(response.data?.ride_id).isEqualTo("ride_123")
            assertThat(response.data?.synced_at).isEqualTo("2024-01-15T10:30:00Z")
            assertThat(response.error).isNull()
        }

        @Test
        fun `failed sync with error`() {
            val response = SyncResponse(success = false, error = "Server error")

            assertThat(response.success).isFalse()
            assertThat(response.data).isNull()
            assertThat(response.error).isEqualTo("Server error")
        }

        @Test
        fun `default values for optional fields`() {
            val response = SyncResponse(success = true)

            assertThat(response.data).isNull()
            assertThat(response.error).isNull()
        }
    }

    @Nested
    @DisplayName("SyncData")
    inner class SyncDataTests {

        @Test
        fun `contains ride id and timestamp`() {
            val data = SyncData(
                ride_id = "abc123",
                synced_at = "2024-01-15T12:00:00Z"
            )

            assertThat(data.ride_id).isEqualTo("abc123")
            assertThat(data.synced_at).isEqualTo("2024-01-15T12:00:00Z")
        }

        @Test
        fun `handles UUID-style ride id`() {
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
            val data = SyncData(ride_id = uuid, synced_at = "2024-01-15T12:00:00Z")

            assertThat(data.ride_id).isEqualTo(uuid)
        }
    }

    @Nested
    @DisplayName("SyncStatusResponse")
    inner class SyncStatusResponseTests {

        @Test
        fun `successful status response`() {
            val data = SyncStatusData(
                device_id = "device_123",
                last_sync = "2024-01-15T10:00:00Z",
                ride_count = 42
            )
            val response = SyncStatusResponse(success = true, data = data)

            assertThat(response.success).isTrue()
            assertThat(response.data?.device_id).isEqualTo("device_123")
            assertThat(response.data?.last_sync).isEqualTo("2024-01-15T10:00:00Z")
            assertThat(response.data?.ride_count).isEqualTo(42)
        }

        @Test
        fun `failed status response`() {
            val response = SyncStatusResponse(success = false, error = "Unauthorized")

            assertThat(response.success).isFalse()
            assertThat(response.error).isEqualTo("Unauthorized")
        }
    }

    @Nested
    @DisplayName("SyncStatusData")
    inner class SyncStatusDataTests {

        @Test
        fun `contains device info`() {
            val data = SyncStatusData(
                device_id = "karoo_001",
                last_sync = "2024-01-15T08:30:00Z",
                ride_count = 100
            )

            assertThat(data.device_id).isEqualTo("karoo_001")
            assertThat(data.last_sync).isEqualTo("2024-01-15T08:30:00Z")
            assertThat(data.ride_count).isEqualTo(100)
        }

        @Test
        fun `last_sync can be null for new devices`() {
            val data = SyncStatusData(
                device_id = "new_device",
                last_sync = null,
                ride_count = 0
            )

            assertThat(data.last_sync).isNull()
            assertThat(data.ride_count).isEqualTo(0)
        }

        @Test
        fun `handles high ride count`() {
            val data = SyncStatusData(
                device_id = "device",
                last_sync = "2024-01-15T00:00:00Z",
                ride_count = 10000
            )

            assertThat(data.ride_count).isEqualTo(10000)
        }
    }
}
