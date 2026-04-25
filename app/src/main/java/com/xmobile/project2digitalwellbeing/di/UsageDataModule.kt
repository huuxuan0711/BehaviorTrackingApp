package com.xmobile.project2digitalwellbeing.di

import android.content.Context
import androidx.room.Room
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.UsageDatabase
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.dao.AppMetadataDao
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.dao.InsightDao
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.dao.RawUsageEventDao
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.dao.SessionDao
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.dao.SyncStateDao
import com.xmobile.project2digitalwellbeing.data.usage.source.system.AppMetadataDataSource
import com.xmobile.project2digitalwellbeing.data.usage.source.system.AppMetadataDataSourceImpl
import com.xmobile.project2digitalwellbeing.data.usage.source.system.UsageStatsDataSource
import com.xmobile.project2digitalwellbeing.data.usage.source.system.UsageStatsDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UsageDatabaseModule {
    @Provides
    @Singleton
    fun provideUsageDatabase(@ApplicationContext context: Context): UsageDatabase {
        return Room.databaseBuilder(
            context,
            UsageDatabase::class.java,
            "usage_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideAppMetadataDao(database: UsageDatabase): AppMetadataDao = database.appMetadataDao()

    @Provides
    fun provideSessionDao(database: UsageDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideInsightDao(database: UsageDatabase): InsightDao = database.insightDao()

    @Provides
    fun provideSyncStateDao(database: UsageDatabase): SyncStateDao = database.syncStateDao()

    @Provides
    fun provideRawUsageEventDao(database: UsageDatabase): RawUsageEventDao = database.rawUsageEventDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UsageSourceModule {
    @Binds
    abstract fun bindUsageStatsDataSource(
        implementation: UsageStatsDataSourceImpl
    ): UsageStatsDataSource

    @Binds
    abstract fun bindAppMetadataDataSource(
        implementation: AppMetadataDataSourceImpl
    ): AppMetadataDataSource
}
