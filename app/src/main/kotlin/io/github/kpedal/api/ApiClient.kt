package io.github.kpedal.api

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit API client for KPedal cloud sync.
 */
object ApiClient {
    private const val TAG = "ApiClient"
    private const val BASE_URL = "https://api.kpedal.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Shorter keep-alive to avoid stale connections on Karoo
            .connectionPool(ConnectionPool(2, 1, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val syncService: SyncApiService by lazy {
        retrofit.create(SyncApiService::class.java)
    }
}
