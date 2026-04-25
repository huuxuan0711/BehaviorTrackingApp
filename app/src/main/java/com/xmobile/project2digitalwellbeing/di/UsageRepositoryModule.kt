package com.xmobile.project2digitalwellbeing.di

import com.xmobile.project2digitalwellbeing.data.usage.repository.UsageRepositoryImpl
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UsageRepositoryModule {
    @Binds
    abstract fun bindUsageRepository(
        implementation: UsageRepositoryImpl
    ): UsageRepository
}
