package com.xmobile.project2digitalwellbeing.di

import com.xmobile.project2digitalwellbeing.data.ai.repository.GeminiCloudInsightRepositoryImpl
import com.xmobile.project2digitalwellbeing.data.ai.local.EncryptedCloudSecretRepository
import com.xmobile.project2digitalwellbeing.data.apps.repository.AppRepositoryImpl
import com.xmobile.project2digitalwellbeing.data.network.AndroidNetworkStatusProvider
import com.xmobile.project2digitalwellbeing.data.tracking.repository.UsageRepositoryImpl
import com.xmobile.project2digitalwellbeing.data.preferences.repository.UsagePreferencesRepositoryImpl
import com.xmobile.project2digitalwellbeing.domain.apps.repository.AppRepository
import com.xmobile.project2digitalwellbeing.domain.network.NetworkStatusProvider
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudInsightRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudSecretRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppAnalyticsRepositoryModule {
    @Binds
    abstract fun bindUsageRepository(
        implementation: UsageRepositoryImpl
    ): UsageRepository

    @Binds
    abstract fun bindAppRepository(
        implementation: AppRepositoryImpl
    ): AppRepository

    @Binds
    abstract fun bindUsagePreferencesRepository(
        implementation: UsagePreferencesRepositoryImpl
    ): UsagePreferencesRepository

    @Binds
    abstract fun bindCloudInsightRepository(
        implementation: GeminiCloudInsightRepositoryImpl
    ): CloudInsightRepository

    @Binds
    abstract fun bindCloudSecretRepository(
        implementation: EncryptedCloudSecretRepository
    ): CloudSecretRepository

    @Binds
    abstract fun bindNetworkStatusProvider(
        implementation: AndroidNetworkStatusProvider
    ): NetworkStatusProvider
}
