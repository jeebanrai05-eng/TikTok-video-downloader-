package com.example.data.api

import com.example.data.model.TikWmResponse
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TikTokApiService {

    @GET("api/")
    suspend fun extractWithGet(
        @Query("url") url: String,
        @Query("hd") hd: Int = 1
    ): TikWmResponse

    @FormUrlEncoded
    @POST("api/")
    suspend fun extractWithPost(
        @Field("url") url: String,
        @Field("hd") hd: Int = 1
    ): TikWmResponse

    companion object {
        private const val BASE_URL = "https://www.tikwm.com/"

        fun create(): TikTokApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = TikTokNetworkClient.fastApiClient.newBuilder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", TikTokNetworkClient.MOBILE_USER_AGENT)
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(TikTokApiService::class.java)
        }
    }
}
