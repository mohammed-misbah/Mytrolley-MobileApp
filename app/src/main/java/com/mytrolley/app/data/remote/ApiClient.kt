package com.mytrolley.app.data.remote

import com.mytrolley.app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Defaults to the production catalogue; overridable at build time with
    // -PmytrolleyApiUrl=... (e.g. http://10.0.2.2:8000/api/v1/ for local Django).
    // The value always includes the /api/v1/ prefix, so service paths are relative.
    val baseUrl: String get() = BuildConfig.API_BASE_URL

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val service: MyTrolleyApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MyTrolleyApiService::class.java)
}