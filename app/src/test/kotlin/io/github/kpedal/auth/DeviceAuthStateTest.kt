package io.github.kpedal.auth

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DeviceAuthService.DeviceAuthState")
class DeviceAuthStateTest {

    @Nested
    @DisplayName("Idle state")
    inner class IdleStateTests {

        @Test
        fun `Idle is a singleton object`() {
            val idle1 = DeviceAuthService.DeviceAuthState.Idle
            val idle2 = DeviceAuthService.DeviceAuthState.Idle

            assertThat(idle1).isSameInstanceAs(idle2)
        }

        @Test
        fun `Idle is a DeviceAuthState`() {
            val idle = DeviceAuthService.DeviceAuthState.Idle

            assertThat(idle).isInstanceOf(DeviceAuthService.DeviceAuthState::class.java)
        }
    }

    @Nested
    @DisplayName("RequestingCode state")
    inner class RequestingCodeStateTests {

        @Test
        fun `RequestingCode is a singleton object`() {
            val state1 = DeviceAuthService.DeviceAuthState.RequestingCode
            val state2 = DeviceAuthService.DeviceAuthState.RequestingCode

            assertThat(state1).isSameInstanceAs(state2)
        }
    }

    @Nested
    @DisplayName("WaitingForUser state")
    inner class WaitingForUserStateTests {

        @Test
        fun `can be created with all fields`() {
            val state = DeviceAuthService.DeviceAuthState.WaitingForUser(
                userCode = "ABCD-1234",
                verificationUri = "link.kpedal.com",
                expiresIn = 600
            )

            assertThat(state.userCode).isEqualTo("ABCD-1234")
            assertThat(state.verificationUri).isEqualTo("link.kpedal.com")
            assertThat(state.expiresIn).isEqualTo(600)
        }

        @Test
        fun `equals works correctly`() {
            val state1 = DeviceAuthService.DeviceAuthState.WaitingForUser("CODE", "uri", 300)
            val state2 = DeviceAuthService.DeviceAuthState.WaitingForUser("CODE", "uri", 300)
            val state3 = DeviceAuthService.DeviceAuthState.WaitingForUser("OTHER", "uri", 300)

            assertThat(state1).isEqualTo(state2)
            assertThat(state1).isNotEqualTo(state3)
        }

        @Test
        fun `copy works correctly`() {
            val original = DeviceAuthService.DeviceAuthState.WaitingForUser("CODE", "uri", 600)
            val copied = original.copy(expiresIn = 300)

            assertThat(copied.userCode).isEqualTo("CODE")
            assertThat(copied.verificationUri).isEqualTo("uri")
            assertThat(copied.expiresIn).isEqualTo(300)
        }
    }

    @Nested
    @DisplayName("Polling state")
    inner class PollingStateTests {

        @Test
        fun `can be created with all fields`() {
            val state = DeviceAuthService.DeviceAuthState.Polling(
                userCode = "WXYZ-5678",
                verificationUri = "link.kpedal.com",
                attemptsRemaining = 100
            )

            assertThat(state.userCode).isEqualTo("WXYZ-5678")
            assertThat(state.verificationUri).isEqualTo("link.kpedal.com")
            assertThat(state.attemptsRemaining).isEqualTo(100)
        }

        @Test
        fun `attemptsRemaining can decrease`() {
            val state1 = DeviceAuthService.DeviceAuthState.Polling("CODE", "uri", 100)
            val state2 = state1.copy(attemptsRemaining = 99)

            assertThat(state2.attemptsRemaining).isEqualTo(99)
        }
    }

    @Nested
    @DisplayName("Success state")
    inner class SuccessStateTests {

        @Test
        fun `can be created with user info`() {
            val state = DeviceAuthService.DeviceAuthState.Success(
                email = "user@example.com",
                name = "Test User"
            )

            assertThat(state.email).isEqualTo("user@example.com")
            assertThat(state.name).isEqualTo("Test User")
        }

        @Test
        fun `handles unicode in name`() {
            val state = DeviceAuthService.DeviceAuthState.Success(
                email = "user@example.com",
                name = "Тест Юзер 日本語"
            )

            assertThat(state.name).isEqualTo("Тест Юзер 日本語")
        }

        @Test
        fun `equals works correctly`() {
            val state1 = DeviceAuthService.DeviceAuthState.Success("email@test.com", "Name")
            val state2 = DeviceAuthService.DeviceAuthState.Success("email@test.com", "Name")
            val state3 = DeviceAuthService.DeviceAuthState.Success("other@test.com", "Name")

            assertThat(state1).isEqualTo(state2)
            assertThat(state1).isNotEqualTo(state3)
        }
    }

    @Nested
    @DisplayName("Error state")
    inner class ErrorStateTests {

        @Test
        fun `can be created with message`() {
            val state = DeviceAuthService.DeviceAuthState.Error("Network error")

            assertThat(state.message).isEqualTo("Network error")
        }

        @Test
        fun `handles various error messages`() {
            val errors = listOf(
                "Network error",
                "Invalid device code",
                "Rate limited",
                "Server error: 500",
                ""
            )

            errors.forEach { msg ->
                val state = DeviceAuthService.DeviceAuthState.Error(msg)
                assertThat(state.message).isEqualTo(msg)
            }
        }
    }

    @Nested
    @DisplayName("Expired state")
    inner class ExpiredStateTests {

        @Test
        fun `Expired is a singleton object`() {
            val state1 = DeviceAuthService.DeviceAuthState.Expired
            val state2 = DeviceAuthService.DeviceAuthState.Expired

            assertThat(state1).isSameInstanceAs(state2)
        }
    }

    @Nested
    @DisplayName("AccessDenied state")
    inner class AccessDeniedStateTests {

        @Test
        fun `AccessDenied is a singleton object`() {
            val state1 = DeviceAuthService.DeviceAuthState.AccessDenied
            val state2 = DeviceAuthService.DeviceAuthState.AccessDenied

            assertThat(state1).isSameInstanceAs(state2)
        }
    }

    @Nested
    @DisplayName("State transitions")
    inner class StateTransitionTests {

        @Test
        fun `all states are DeviceAuthState subtypes`() {
            val states: List<DeviceAuthService.DeviceAuthState> = listOf(
                DeviceAuthService.DeviceAuthState.Idle,
                DeviceAuthService.DeviceAuthState.RequestingCode,
                DeviceAuthService.DeviceAuthState.WaitingForUser("CODE", "uri", 600),
                DeviceAuthService.DeviceAuthState.Polling("CODE", "uri", 100),
                DeviceAuthService.DeviceAuthState.Success("email", "name"),
                DeviceAuthService.DeviceAuthState.Error("error"),
                DeviceAuthService.DeviceAuthState.Expired,
                DeviceAuthService.DeviceAuthState.AccessDenied
            )

            states.forEach { state ->
                assertThat(state).isInstanceOf(DeviceAuthService.DeviceAuthState::class.java)
            }
        }

        @Test
        fun `can use when expression exhaustively`() {
            val states = listOf(
                DeviceAuthService.DeviceAuthState.Idle,
                DeviceAuthService.DeviceAuthState.RequestingCode,
                DeviceAuthService.DeviceAuthState.WaitingForUser("CODE", "uri", 600),
                DeviceAuthService.DeviceAuthState.Polling("CODE", "uri", 100),
                DeviceAuthService.DeviceAuthState.Success("email", "name"),
                DeviceAuthService.DeviceAuthState.Error("error"),
                DeviceAuthService.DeviceAuthState.Expired,
                DeviceAuthService.DeviceAuthState.AccessDenied
            )

            states.forEach { state ->
                val description = when (state) {
                    is DeviceAuthService.DeviceAuthState.Idle -> "idle"
                    is DeviceAuthService.DeviceAuthState.RequestingCode -> "requesting"
                    is DeviceAuthService.DeviceAuthState.WaitingForUser -> "waiting"
                    is DeviceAuthService.DeviceAuthState.Polling -> "polling"
                    is DeviceAuthService.DeviceAuthState.Success -> "success"
                    is DeviceAuthService.DeviceAuthState.Error -> "error"
                    is DeviceAuthService.DeviceAuthState.Expired -> "expired"
                    is DeviceAuthService.DeviceAuthState.AccessDenied -> "denied"
                }
                assertThat(description).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("User code format")
    inner class UserCodeFormatTests {

        @Test
        fun `typical user code format XXXX-XXXX`() {
            val state = DeviceAuthService.DeviceAuthState.WaitingForUser(
                userCode = "ABCD-1234",
                verificationUri = "link.kpedal.com",
                expiresIn = 600
            )

            assertThat(state.userCode).matches("[A-Z]{4}-[0-9]{4}")
        }

        @Test
        fun `user code can have different formats`() {
            // The code format can vary
            val codes = listOf("ABCD-1234", "WXYZ-9876", "HHJK-0001")

            codes.forEach { code ->
                val state = DeviceAuthService.DeviceAuthState.WaitingForUser(code, "uri", 600)
                assertThat(state.userCode).isEqualTo(code)
            }
        }
    }
}
