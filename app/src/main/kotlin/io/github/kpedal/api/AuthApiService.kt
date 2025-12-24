package io.github.kpedal.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Auth API service for Device Code Flow and token refresh.
 */
interface AuthApiService {

    /**
     * Start device code flow.
     * Returns a device_code and user_code to display.
     */
    @POST("auth/device/code")
    suspend fun requestDeviceCode(@Body request: DeviceCodeRequest): Response<DeviceCodeResponse>

    /**
     * Poll for token.
     * Device polls this endpoint until user authorizes.
     */
    @POST("auth/device/token")
    suspend fun pollForToken(@Body request: DeviceTokenRequest): Response<DeviceTokenResponse>

    /**
     * Refresh access token.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<RefreshResponse>

    /**
     * Logout and revoke refresh token.
     */
    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<ApiResponse>
}

// Request models

data class DeviceCodeRequest(
    val device_id: String,
    val device_name: String = "Karoo"
)

data class DeviceTokenRequest(
    val device_code: String,
    val device_id: String
)

data class RefreshRequest(
    val refresh_token: String
)

data class LogoutRequest(
    val refresh_token: String
)

// Response models

data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

data class DeviceCodeResponse(
    val success: Boolean,
    val data: DeviceCodeData? = null,
    val error: String? = null
)

data class DeviceCodeData(
    val device_code: String,
    val user_code: String,
    val verification_uri: String,
    val expires_in: Int,
    val interval: Int
)

data class DeviceTokenResponse(
    val success: Boolean,
    val data: DeviceTokenData? = null,
    val error: String? = null,
    val status: String? = null // "authorization_pending", "expired", "access_denied"
)

data class DeviceTokenData(
    val access_token: String,
    val refresh_token: String,
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val picture: String?
)

data class RefreshResponse(
    val success: Boolean,
    val data: RefreshData? = null,
    val error: String? = null
)

data class RefreshData(
    val access_token: String
)
