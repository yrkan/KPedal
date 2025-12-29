package io.github.kpedal.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Retrofit API client for KPedal cloud sync.
 */
object ApiClient {
    private const val TAG = "ApiClient"
    private const val BASE_URL = "https://api.kpedal.com/"

    /**
     * Logs requests and responses for debugging network issues.
     */
    private val loggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        Log.d(TAG, "→ ${request.method} ${request.url}")

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "✗ ${request.url} failed after ${duration}ms: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }

        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "← ${response.code} ${request.url} (${duration}ms)")

        response
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
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
