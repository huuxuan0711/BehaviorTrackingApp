package com.xmobile.project2digitalwellbeing.di

import com.xmobile.project2digitalwellbeing.data.tracking.repository.UsageRepositoryImpl
import com.xmobile.project2digitalwellbeing.data.preferences.repository.UsagePreferencesRepositoryImpl
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
    abstract fun bindUsagePreferencesRepository(
        implementation: UsagePreferencesRepositoryImpl
    ): UsagePreferencesRepository
}
