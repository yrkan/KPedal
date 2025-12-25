package io.github.kpedal.auth

import com.google.gson.Gson
import io.github.kpedal.api.ApiClient
import io.github.kpedal.api.DeviceCodeData
import io.github.kpedal.api.DeviceCodeRequest
import io.github.kpedal.api.DeviceTokenRequest
import io.github.kpedal.api.DeviceTokenData
import io.github.kpedal.api.DeviceTokenResponse
import io.github.kpedal.data.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for handling Device Code Flow authentication.
 *
 * Flow:
 * 1. App calls requestDeviceCode() to get a user_code
 * 2. User enters code at kpedal.com/link on their phone/computer
 * 3. App polls pollForToken() until user completes auth
 * 4. On success, tokens are saved to AuthRepository
 */
class DeviceAuthService(
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "DeviceAuthService"
        private const val INITIAL_POLL_INTERVAL_MS = 5000L // Poll every 5 sec (D1 has immediate consistency)
        private const val MAX_POLL_INTERVAL_MS = 8000L // Max 8 sec with backoff
        private const val BACKOFF_AFTER_ATTEMPTS = 20 // Start backing off after 20 attempts (~1.5 min)
        private const val MAX_POLL_ATTEMPTS = 120 // 10 min with current settings
        private val gson = Gson()
    }

    /**
     * Current state of the device auth flow.
     */
    sealed class DeviceAuthState {
        /** Not started */
        data object Idle : DeviceAuthState()

        /** Requesting device code from server */
        data object RequestingCode : DeviceAuthState()

        /** Device code received, waiting for user to authenticate */
        data class WaitingForUser(
            val userCode: String,
            val verificationUri: String,
            val expiresIn: Int
        ) : DeviceAuthState()

        /** Polling for token */
        data class Polling(
            val userCode: String,
            val verificationUri: String,
            val attemptsRemaining: Int
        ) : DeviceAuthState()

        /** Auth completed successfully */
        data class Success(
            val email: String,
            val name: String
        ) : DeviceAuthState()

        /** Auth failed */
        data class Error(val message: String) : DeviceAuthState()

        /** Device code expired */
        data object Expired : DeviceAuthState()

        /** User denied access */
        data object AccessDenied : DeviceAuthState()
    }

    private val _state = MutableStateFlow<DeviceAuthState>(DeviceAuthState.Idle)
    val state: StateFlow<DeviceAuthState> = _state.asStateFlow()

    private var currentDeviceCode: String? = null
    private var pollIntervalMs: Long = INITIAL_POLL_INTERVAL_MS
    private var isPolling = false

    /**
     * Start the device code flow.
     * Requests a device code from the server.
     */
    suspend fun startAuthFlow(): DeviceCodeData? {
        _state.value = DeviceAuthState.RequestingCode
        pollIntervalMs = INITIAL_POLL_INTERVAL_MS // Reset interval for new flow

        return try {
            val deviceId = authRepository.getOrCreateDeviceId()
            val response = ApiClient.authService.requestDeviceCode(
                DeviceCodeRequest(
                    device_id = deviceId,
                    device_name = "Karoo"
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    currentDeviceCode = data.device_code
                    pollIntervalMs = data.interval * 1000L

                    _state.value = DeviceAuthState.WaitingForUser(
                        userCode = data.user_code,
                        verificationUri = data.verification_uri,
                        expiresIn = data.expires_in
                    )

                    android.util.Log.i(TAG, "Device code received: ${data.user_code}")
                    data
                } else {
                    _state.value = DeviceAuthState.Error("No data in response")
                    null
                }
            } else {
                val error = response.body()?.error ?: "Unknown error"
                _state.value = DeviceAuthState.Error(error)
                android.util.Log.e(TAG, "Failed to get device code: $error")
                null
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val error = e.message ?: "Network error"
            _state.value = DeviceAuthState.Error(error)
            android.util.Log.e(TAG, "Error requesting device code: $error")
            null
        }
    }

    /**
     * Start polling for token.
     * Call this after startAuthFlow() returns successfully.
     * Will poll until user authenticates, code expires, or cancelled.
     */
    suspend fun startPolling(): DeviceTokenData? {
        val deviceCode = currentDeviceCode
        if (deviceCode == null) {
            _state.value = DeviceAuthState.Error("No device code")
            return null
        }

        isPolling = true
        var attempts = 0
        val deviceId = authRepository.getOrCreateDeviceId()

        while (isPolling && attempts < MAX_POLL_ATTEMPTS) {
            attempts++

            val currentState = _state.value
            if (currentState is DeviceAuthState.WaitingForUser) {
                _state.value = DeviceAuthState.Polling(
                    userCode = currentState.userCode,
                    verificationUri = currentState.verificationUri,
                    attemptsRemaining = MAX_POLL_ATTEMPTS - attempts
                )
            }

            try {
                val response = ApiClient.authService.pollForToken(
                    DeviceTokenRequest(
                        device_code = deviceCode,
                        device_id = deviceId
                    )
                )

                // Parse response body - for error responses we need to parse errorBody
                val body: DeviceTokenResponse? = if (response.isSuccessful) {
                    response.body()
                } else {
                    // Parse error body for 4xx responses
                    try {
                        response.errorBody()?.string()?.let { errorJson ->
                            gson.fromJson(errorJson, DeviceTokenResponse::class.java)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Failed to parse error body: ${e.message}")
                        null
                    }
                }

                when {
                    body?.success == true && body.data != null -> {
                        // Success! Save tokens
                        val data = body.data
                        authRepository.saveTokens(data.access_token, data.refresh_token)
                        authRepository.saveUserInfo(
                            email = data.user.email,
                            name = data.user.name,
                            picture = data.user.picture
                        )

                        _state.value = DeviceAuthState.Success(
                            email = data.user.email,
                            name = data.user.name
                        )

                        android.util.Log.i(TAG, "Auth successful: ${data.user.email}")
                        isPolling = false
                        currentDeviceCode = null
                        return data
                    }

                    body?.status == "authorization_pending" -> {
                        // Still waiting, continue polling
                        android.util.Log.d(TAG, "Still waiting for user auth (attempt $attempts)")
                    }

                    body?.status == "slow_down" -> {
                        // Polling too fast, increase interval
                        pollIntervalMs = (pollIntervalMs * 1.5).toLong()
                        android.util.Log.d(TAG, "Slowing down, new interval: ${pollIntervalMs}ms")
                    }

                    body?.status == "expired" -> {
                        _state.value = DeviceAuthState.Expired
                        android.util.Log.w(TAG, "Device code expired")
                        isPolling = false
                        currentDeviceCode = null
                        return null
                    }

                    body?.status == "access_denied" -> {
                        _state.value = DeviceAuthState.AccessDenied
                        android.util.Log.w(TAG, "Access denied by user")
                        isPolling = false
                        currentDeviceCode = null
                        return null
                    }

                    body == null && !response.isSuccessful -> {
                        // Network error or server error without body
                        android.util.Log.w(TAG, "Poll request failed: ${response.code()}")
                        // Continue polling on server errors
                    }

                    else -> {
                        val error = body?.error ?: "Unknown error"
                        _state.value = DeviceAuthState.Error(error)
                        android.util.Log.e(TAG, "Poll error: $error")
                        isPolling = false
                        currentDeviceCode = null
                        return null
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    isPolling = false
                    throw e
                }
                android.util.Log.w(TAG, "Poll network error: ${e.message}")
                // Continue polling on network errors
            }

            // Wait before next poll with progressive backoff
            delay(pollIntervalMs)

            // Increase interval after initial attempts (5 sec -> 6 sec -> 7 sec -> 8 sec)
            if (attempts >= BACKOFF_AFTER_ATTEMPTS && pollIntervalMs < MAX_POLL_INTERVAL_MS) {
                pollIntervalMs = minOf(pollIntervalMs + 1000L, MAX_POLL_INTERVAL_MS)
                android.util.Log.d(TAG, "Backing off, new interval: ${pollIntervalMs}ms")
            }
        }

        if (attempts >= MAX_POLL_ATTEMPTS) {
            _state.value = DeviceAuthState.Expired
            android.util.Log.w(TAG, "Polling timed out after $attempts attempts")
        }

        isPolling = false
        currentDeviceCode = null
        return null
    }

    /**
     * Cancel the current auth flow.
     */
    fun cancel() {
        isPolling = false
        currentDeviceCode = null
        _state.value = DeviceAuthState.Idle
        android.util.Log.i(TAG, "Auth flow cancelled")
    }

    /**
     * Reset state to idle.
     */
    fun reset() {
        isPolling = false
        currentDeviceCode = null
        _state.value = DeviceAuthState.Idle
    }
}
