package com.xmobile.project2digitalwellbeing.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.xmobile.project2digitalwellbeing.BuildConfig
import com.xmobile.project2digitalwellbeing.data.ai.remote.GeminiApiKeyInterceptor
import com.xmobile.project2digitalwellbeing.data.ai.remote.api.GeminiApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppAiNetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Named("geminiBaseUrl")
    fun provideGeminiBaseUrl(): String = BuildConfig.GEMINI_API_BASE_URL

    @Provides
    @Named("geminiModelName")
    fun provideGeminiModelName(): String = BuildConfig.GEMINI_MODEL_NAME

    @Provides
    @Singleton
    fun provideGeminiLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideGeminiOkHttpClient(
        apiKeyInterceptor: GeminiApiKeyInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiRetrofit(
        gson: Gson,
        okHttpClient: OkHttpClient,
        @Named("geminiBaseUrl") baseUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(retrofit: Retrofit): GeminiApiService {
        return retrofit.create(GeminiApiService::class.java)
    }
}
