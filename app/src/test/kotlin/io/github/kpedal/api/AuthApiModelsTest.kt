package io.github.kpedal.api

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Auth API Models")
class AuthApiModelsTest {

    @Nested
    @DisplayName("DeviceCodeRequest")
    inner class DeviceCodeRequestTests {

        @Test
        fun `can be created with required fields`() {
            val request = DeviceCodeRequest(device_id = "device_123")

            assertThat(request.device_id).isEqualTo("device_123")
            assertThat(request.device_name).isEqualTo("Karoo") // default
        }

        @Test
        fun `can override default device name`() {
            val request = DeviceCodeRequest(
                device_id = "device",
                device_name = "Karoo 3"
            )

            assertThat(request.device_name).isEqualTo("Karoo 3")
        }

        @Test
        fun `equals and hashCode work correctly`() {
            val request1 = DeviceCodeRequest("device")
            val request2 = DeviceCodeRequest("device")
            val request3 = DeviceCodeRequest("other")

            assertThat(request1).isEqualTo(request2)
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
            assertThat(request1).isNotEqualTo(request3)
        }

        @Test
        fun `copy works correctly`() {
            val original = DeviceCodeRequest("device")
            val copied = original.copy(device_name = "Custom Device")

            assertThat(copied.device_id).isEqualTo("device")
            assertThat(copied.device_name).isEqualTo("Custom Device")
        }
    }

    @Nested
    @DisplayName("DeviceTokenRequest")
    inner class DeviceTokenRequestTests {

        @Test
        fun `can be created with required fields`() {
            val request = DeviceTokenRequest(
                device_code = "abc123",
                device_id = "device_456"
            )

            assertThat(request.device_code).isEqualTo("abc123")
            assertThat(request.device_id).isEqualTo("device_456")
        }

        @Test
        fun `equals works correctly`() {
            val request1 = DeviceTokenRequest("code", "device")
            val request2 = DeviceTokenRequest("code", "device")
            val request3 = DeviceTokenRequest("other", "device")

            assertThat(request1).isEqualTo(request2)
            assertThat(request1).isNotEqualTo(request3)
        }
    }

    @Nested
    @DisplayName("RefreshRequest")
    inner class RefreshRequestTests {

        @Test
        fun `can be created with refresh token`() {
            val request = RefreshRequest("refresh_token_value")
            assertThat(request.refresh_token).isEqualTo("refresh_token_value")
        }

        @Test
        fun `handles long tokens`() {
            val longToken = "a".repeat(500)
            val request = RefreshRequest(longToken)
            assertThat(request.refresh_token).hasLength(500)
        }
    }

    @Nested
    @DisplayName("LogoutRequest")
    inner class LogoutRequestTests {

        @Test
        fun `can be created with refresh token`() {
            val request = LogoutRequest("refresh_token")
            assertThat(request.refresh_token).isEqualTo("refresh_token")
        }
    }

    @Nested
    @DisplayName("ApiResponse")
    inner class ApiResponseTests {

        @Test
        fun `successful response with message`() {
            val response = ApiResponse(success = true, message = "OK")

            assertThat(response.success).isTrue()
            assertThat(response.message).isEqualTo("OK")
            assertThat(response.error).isNull()
        }

        @Test
        fun `failed response with error`() {
            val response = ApiResponse(success = false, error = "Invalid token")

            assertThat(response.success).isFalse()
            assertThat(response.error).isEqualTo("Invalid token")
            assertThat(response.message).isNull()
        }

        @Test
        fun `default values are null`() {
            val response = ApiResponse(success = true)

            assertThat(response.message).isNull()
            assertThat(response.error).isNull()
        }
    }

    @Nested
    @DisplayName("DeviceCodeResponse")
    inner class DeviceCodeResponseTests {

        @Test
        fun `successful response with data`() {
            val data = DeviceCodeData(
                device_code = "device_code_123",
                user_code = "ABCD-1234",
                verification_uri = "link.kpedal.com",
                expires_in = 600,
                interval = 5
            )
            val response = DeviceCodeResponse(success = true, data = data)

            assertThat(response.success).isTrue()
            assertThat(response.data).isNotNull()
            assertThat(response.data?.device_code).isEqualTo("device_code_123")
            assertThat(response.data?.user_code).isEqualTo("ABCD-1234")
            assertThat(response.error).isNull()
        }

        @Test
        fun `failed response with error`() {
            val response = DeviceCodeResponse(success = false, error = "Rate limited")

            assertThat(response.success).isFalse()
            assertThat(response.data).isNull()
            assertThat(response.error).isEqualTo("Rate limited")
        }
    }

    @Nested
    @DisplayName("DeviceCodeData")
    inner class DeviceCodeDataTests {

        @Test
        fun `contains all required fields`() {
            val data = DeviceCodeData(
                device_code = "code_abc",
                user_code = "WXYZ-9876",
                verification_uri = "https://link.kpedal.com",
                expires_in = 300,
                interval = 10
            )

            assertThat(data.device_code).isEqualTo("code_abc")
            assertThat(data.user_code).isEqualTo("WXYZ-9876")
            assertThat(data.verification_uri).isEqualTo("https://link.kpedal.com")
            assertThat(data.expires_in).isEqualTo(300)
            assertThat(data.interval).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("DeviceTokenResponse")
    inner class DeviceTokenResponseTests {

        @Test
        fun `successful token response with data`() {
            val user = UserInfo(
                id = "user_123",
                email = "test@example.com",
                name = "Test User",
                picture = "https://example.com/pic.jpg"
            )
            val data = DeviceTokenData(
                access_token = "access_token",
                refresh_token = "refresh_token",
                user = user
            )
            val response = DeviceTokenResponse(success = true, data = data)

            assertThat(response.success).isTrue()
            assertThat(response.data).isNotNull()
            assertThat(response.data?.access_token).isEqualTo("access_token")
            assertThat(response.data?.refresh_token).isEqualTo("refresh_token")
            assertThat(response.data?.user?.email).isEqualTo("test@example.com")
            assertThat(response.error).isNull()
        }

        @Test
        fun `authorization pending status`() {
            val response = DeviceTokenResponse(
                success = false,
                status = "authorization_pending"
            )

            assertThat(response.success).isFalse()
            assertThat(response.status).isEqualTo("authorization_pending")
            assertThat(response.data).isNull()
        }

        @Test
        fun `expired status`() {
            val response = DeviceTokenResponse(
                success = false,
                status = "expired"
            )

            assertThat(response.status).isEqualTo("expired")
        }

        @Test
        fun `access denied status`() {
            val response = DeviceTokenResponse(
                success = false,
                status = "access_denied"
            )

            assertThat(response.status).isEqualTo("access_denied")
        }
    }

    @Nested
    @DisplayName("DeviceTokenData")
    inner class DeviceTokenDataTests {

        @Test
        fun `contains all required fields`() {
            val user = UserInfo("id", "email@test.com", "Name", null)
            val data = DeviceTokenData(
                access_token = "access",
                refresh_token = "refresh",
                user = user
            )

            assertThat(data.access_token).isEqualTo("access")
            assertThat(data.refresh_token).isEqualTo("refresh")
            assertThat(data.user).isEqualTo(user)
        }
    }

    @Nested
    @DisplayName("UserInfo")
    inner class UserInfoTests {

        @Test
        fun `can be created with all fields`() {
            val user = UserInfo(
                id = "user_123",
                email = "test@example.com",
                name = "Test User",
                picture = "https://example.com/avatar.png"
            )

            assertThat(user.id).isEqualTo("user_123")
            assertThat(user.email).isEqualTo("test@example.com")
            assertThat(user.name).isEqualTo("Test User")
            assertThat(user.picture).isEqualTo("https://example.com/avatar.png")
        }

        @Test
        fun `picture can be null`() {
            val user = UserInfo(
                id = "user_123",
                email = "test@example.com",
                name = "Test User",
                picture = null
            )

            assertThat(user.picture).isNull()
        }

        @Test
        fun `handles unicode in name`() {
            val user = UserInfo(
                id = "user_123",
                email = "test@example.com",
                name = "Тест Юзер 日本語",
                picture = null
            )

            assertThat(user.name).isEqualTo("Тест Юзер 日本語")
        }

        @Test
        fun `equals works correctly`() {
            val user1 = UserInfo("id", "email@test.com", "Name", null)
            val user2 = UserInfo("id", "email@test.com", "Name", null)
            val user3 = UserInfo("id2", "email@test.com", "Name", null)

            assertThat(user1).isEqualTo(user2)
            assertThat(user1).isNotEqualTo(user3)
        }
    }

    @Nested
    @DisplayName("RefreshResponse")
    inner class RefreshResponseTests {

        @Test
        fun `successful refresh with new token`() {
            val data = RefreshData(access_token = "new_access_token")
            val response = RefreshResponse(success = true, data = data)

            assertThat(response.success).isTrue()
            assertThat(response.data?.access_token).isEqualTo("new_access_token")
            assertThat(response.error).isNull()
            assertThat(response.code).isNull()
        }

        @Test
        fun `failed refresh with error`() {
            val response = RefreshResponse(success = false, error = "Token expired")

            assertThat(response.success).isFalse()
            assertThat(response.data).isNull()
            assertThat(response.error).isEqualTo("Token expired")
        }

        @Test
        fun `DEVICE_REVOKED code constant is correct`() {
            assertThat(RefreshResponse.CODE_DEVICE_REVOKED).isEqualTo("DEVICE_REVOKED")
        }

        @Test
        fun `device revoked refresh response`() {
            val response = RefreshResponse(
                success = false,
                error = "Device not found or access revoked",
                code = RefreshResponse.CODE_DEVICE_REVOKED
            )

            assertThat(response.success).isFalse()
            assertThat(response.code).isEqualTo("DEVICE_REVOKED")
            assertThat(response.error).contains("revoked")
        }

        @Test
        fun `can check if device was revoked`() {
            val tokenExpired = RefreshResponse(success = false, error = "Token expired")
            val deviceRevoked = RefreshResponse(
                success = false,
                error = "Device revoked",
                code = RefreshResponse.CODE_DEVICE_REVOKED
            )

            // Token expired - normal failure
            assertThat(tokenExpired.code).isNull()

            // Device revoked - needs special handling (logout)
            assertThat(deviceRevoked.code).isEqualTo(RefreshResponse.CODE_DEVICE_REVOKED)

            // Application logic
            val shouldLogout = deviceRevoked.code == RefreshResponse.CODE_DEVICE_REVOKED
            assertThat(shouldLogout).isTrue()
        }
    }

    @Nested
    @DisplayName("RefreshData")
    inner class RefreshDataTests {

        @Test
        fun `contains access token`() {
            val data = RefreshData(access_token = "new_token")
            assertThat(data.access_token).isEqualTo("new_token")
        }

        @Test
        fun `handles JWT-like tokens`() {
            val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
            val data = RefreshData(access_token = jwtToken)
            assertThat(data.access_token).isEqualTo(jwtToken)
        }
    }
}
