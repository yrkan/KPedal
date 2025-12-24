package io.github.kpedal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Repository for authentication state and secure token storage.
 * Uses EncryptedSharedPreferences for secure JWT token storage.
 */
class AuthRepository(private val context: Context) {

    /**
     * Authentication state.
     */
    data class AuthState(
        val isLoggedIn: Boolean = false,
        val userEmail: String? = null,
        val userName: String? = null,
        val userPicture: String? = null,
        val deviceId: String = ""
    )

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _authState = MutableStateFlow(loadAuthState())

    /**
     * Flow of current authentication state.
     */
    val authStateFlow: Flow<AuthState> = _authState.asStateFlow()

    /**
     * Current authentication state.
     */
    val currentState: AuthState get() = _authState.value

    /**
     * Whether user is currently logged in.
     */
    val isLoggedIn: Boolean get() = _authState.value.isLoggedIn

    /**
     * Get access token for API calls.
     */
    fun getAccessToken(): String? = securePrefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * Get refresh token for token refresh.
     */
    fun getRefreshToken(): String? = securePrefs.getString(KEY_REFRESH_TOKEN, null)

    /**
     * Get or create device ID.
     * Device ID is a UUID that identifies this Karoo device.
     */
    fun getOrCreateDeviceId(): String {
        var deviceId = securePrefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            securePrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    /**
     * Save tokens after successful login.
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    /**
     * Update access token (after refresh).
     */
    fun updateAccessToken(accessToken: String) {
        securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
    }

    /**
     * Save user info after successful login.
     */
    fun saveUserInfo(email: String, name: String, picture: String?) {
        securePrefs.edit()
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_PICTURE, picture)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()

        _authState.value = AuthState(
            isLoggedIn = true,
            userEmail = email,
            userName = name,
            userPicture = picture,
            deviceId = getOrCreateDeviceId()
        )
    }

    /**
     * Clear all auth data (logout).
     */
    fun clearAuth() {
        securePrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PICTURE)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()

        _authState.value = AuthState(
            isLoggedIn = false,
            deviceId = getOrCreateDeviceId()
        )
    }

    private fun loadAuthState(): AuthState {
        return AuthState(
            isLoggedIn = securePrefs.getBoolean(KEY_IS_LOGGED_IN, false),
            userEmail = securePrefs.getString(KEY_USER_EMAIL, null),
            userName = securePrefs.getString(KEY_USER_NAME, null),
            userPicture = securePrefs.getString(KEY_USER_PICTURE, null),
            deviceId = getOrCreateDeviceId()
        )
    }

    companion object {
        private const val PREFS_NAME = "kpedal_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PICTURE = "user_picture"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
