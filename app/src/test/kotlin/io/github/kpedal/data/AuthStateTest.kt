package io.github.kpedal.data

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AuthRepository.AuthState")
class AuthStateTest {

    @Nested
    @DisplayName("Default state")
    inner class DefaultStateTests {

        @Test
        fun `default state is logged out`() {
            val state = AuthRepository.AuthState()

            assertThat(state.isLoggedIn).isFalse()
        }

        @Test
        fun `default state has null user info`() {
            val state = AuthRepository.AuthState()

            assertThat(state.userEmail).isNull()
            assertThat(state.userName).isNull()
            assertThat(state.userPicture).isNull()
        }

        @Test
        fun `default device id is empty string`() {
            val state = AuthRepository.AuthState()

            assertThat(state.deviceId).isEmpty()
        }
    }

    @Nested
    @DisplayName("Logged in state")
    inner class LoggedInStateTests {

        @Test
        fun `can create logged in state with user info`() {
            val state = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                userName = "Test User",
                userPicture = "https://example.com/avatar.png",
                deviceId = "device_123"
            )

            assertThat(state.isLoggedIn).isTrue()
            assertThat(state.userEmail).isEqualTo("test@example.com")
            assertThat(state.userName).isEqualTo("Test User")
            assertThat(state.userPicture).isEqualTo("https://example.com/avatar.png")
            assertThat(state.deviceId).isEqualTo("device_123")
        }

        @Test
        fun `logged in state can have null picture`() {
            val state = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                userName = "Test User",
                userPicture = null,
                deviceId = "device_123"
            )

            assertThat(state.isLoggedIn).isTrue()
            assertThat(state.userPicture).isNull()
        }
    }

    @Nested
    @DisplayName("State transitions")
    inner class StateTransitionTests {

        @Test
        fun `transition from logged out to logged in`() {
            val loggedOut = AuthRepository.AuthState(
                isLoggedIn = false,
                deviceId = "device_123"
            )

            val loggedIn = loggedOut.copy(
                isLoggedIn = true,
                userEmail = "user@example.com",
                userName = "User Name"
            )

            assertThat(loggedIn.isLoggedIn).isTrue()
            assertThat(loggedIn.userEmail).isEqualTo("user@example.com")
            assertThat(loggedIn.deviceId).isEqualTo("device_123") // preserved
        }

        @Test
        fun `transition from logged in to logged out`() {
            val loggedIn = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "user@example.com",
                userName = "User",
                deviceId = "device_123"
            )

            val loggedOut = AuthRepository.AuthState(
                isLoggedIn = false,
                deviceId = loggedIn.deviceId
            )

            assertThat(loggedOut.isLoggedIn).isFalse()
            assertThat(loggedOut.userEmail).isNull()
            assertThat(loggedOut.userName).isNull()
            assertThat(loggedOut.deviceId).isEqualTo("device_123") // preserved
        }

        @Test
        fun `can update user info while logged in`() {
            val original = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "old@example.com",
                userName = "Old Name",
                deviceId = "device"
            )

            val updated = original.copy(
                userEmail = "new@example.com",
                userName = "New Name"
            )

            assertThat(updated.isLoggedIn).isTrue()
            assertThat(updated.userEmail).isEqualTo("new@example.com")
            assertThat(updated.userName).isEqualTo("New Name")
        }
    }

    @Nested
    @DisplayName("Data class behavior")
    inner class DataClassBehaviorTests {

        @Test
        fun `equals compares all fields`() {
            val state1 = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                userName = "Test",
                userPicture = "pic.png",
                deviceId = "device"
            )

            val state2 = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                userName = "Test",
                userPicture = "pic.png",
                deviceId = "device"
            )

            val state3 = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                userName = "Different",
                userPicture = "pic.png",
                deviceId = "device"
            )

            assertThat(state1).isEqualTo(state2)
            assertThat(state1).isNotEqualTo(state3)
        }

        @Test
        fun `hashCode is consistent`() {
            val state1 = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                deviceId = "device"
            )

            val state2 = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                deviceId = "device"
            )

            assertThat(state1.hashCode()).isEqualTo(state2.hashCode())
        }

        @Test
        fun `copy preserves unchanged fields`() {
            val original = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                userName = "Test User",
                userPicture = "https://example.com/pic.jpg",
                deviceId = "device_abc"
            )

            val copied = original.copy(userPicture = "https://new.example.com/pic.jpg")

            assertThat(copied.isLoggedIn).isEqualTo(original.isLoggedIn)
            assertThat(copied.userEmail).isEqualTo(original.userEmail)
            assertThat(copied.userName).isEqualTo(original.userName)
            assertThat(copied.deviceId).isEqualTo(original.deviceId)
            assertThat(copied.userPicture).isEqualTo("https://new.example.com/pic.jpg")
        }

        @Test
        fun `toString is readable`() {
            val state = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com"
            )

            val string = state.toString()

            assertThat(string).contains("isLoggedIn=true")
            assertThat(string).contains("userEmail=test@example.com")
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCaseTests {

        @Test
        fun `handles empty email`() {
            val state = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "",
                userName = "User",
                deviceId = "device"
            )

            assertThat(state.userEmail).isEmpty()
        }

        @Test
        fun `handles empty name`() {
            val state = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "test@example.com",
                userName = "",
                deviceId = "device"
            )

            assertThat(state.userName).isEmpty()
        }

        @Test
        fun `handles unicode in name`() {
            val state = AuthRepository.AuthState(
                isLoggedIn = true,
                userName = "Тест Юзер 日本語 🚴",
                deviceId = "device"
            )

            assertThat(state.userName).isEqualTo("Тест Юзер 日本語 🚴")
        }

        @Test
        fun `handles very long email`() {
            val longEmail = "a".repeat(100) + "@" + "b".repeat(100) + ".com"
            val state = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = longEmail,
                deviceId = "device"
            )

            assertThat(state.userEmail).isEqualTo(longEmail)
        }

        @Test
        fun `handles UUID-style device id`() {
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
            val state = AuthRepository.AuthState(deviceId = uuid)

            assertThat(state.deviceId).isEqualTo(uuid)
        }
    }

    @Nested
    @DisplayName("Destructuring")
    inner class DestructuringTests {

        @Test
        fun `can destructure all components`() {
            val state = AuthRepository.AuthState(
                isLoggedIn = true,
                userEmail = "email@test.com",
                userName = "Name",
                userPicture = "pic.png",
                deviceId = "device_123"
            )

            val (isLoggedIn, userEmail, userName, userPicture, deviceId) = state

            assertThat(isLoggedIn).isTrue()
            assertThat(userEmail).isEqualTo("email@test.com")
            assertThat(userName).isEqualTo("Name")
            assertThat(userPicture).isEqualTo("pic.png")
            assertThat(deviceId).isEqualTo("device_123")
        }
    }
}
