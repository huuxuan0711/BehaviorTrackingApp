package com.xmobile.project2digitalwellbeing.di

import android.content.Context
import androidx.room.Room
import com.xmobile.project2digitalwellbeing.data.analytics.source.local.room.AnalyticsDatabase
import com.xmobile.project2digitalwellbeing.data.preferences.local.AppPreferencesDataStore
import com.xmobile.project2digitalwellbeing.data.apps.source.local.room.dao.AppMetadataDao
import com.xmobile.project2digitalwellbeing.data.insights.source.local.room.dao.InsightDao
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao.RawUsageEventDao
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao.SessionDao
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao.SyncStateDao
import com.xmobile.project2digitalwellbeing.data.apps.source.system.AppMetadataDataSource
import com.xmobile.project2digitalwellbeing.data.apps.source.system.AppMetadataDataSourceImpl
import com.xmobile.project2digitalwellbeing.data.tracking.source.system.UsageStatsDataSource
import com.xmobile.project2digitalwellbeing.data.tracking.source.system.UsageStatsDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrackingDatabaseModule {
    @Provides
    @Singleton
    fun provideAppPreferencesDataStore(@ApplicationContext context: Context): AppPreferencesDataStore {
        return AppPreferencesDataStore(context)
    }

    @Provides
    @Singleton
    fun provideAnalyticsDatabase(@ApplicationContext context: Context): AnalyticsDatabase {
        return Room.databaseBuilder(
            context,
            AnalyticsDatabase::class.java,
            "usage_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideAppMetadataDao(database: AnalyticsDatabase): AppMetadataDao = database.appMetadataDao()

    @Provides
    fun provideSessionDao(database: AnalyticsDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideInsightDao(database: AnalyticsDatabase): InsightDao = database.insightDao()

    @Provides
    fun provideSyncStateDao(database: AnalyticsDatabase): SyncStateDao = database.syncStateDao()

    @Provides
    fun provideRawUsageEventDao(database: AnalyticsDatabase): RawUsageEventDao = database.rawUsageEventDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingSourceModule {
    @Binds
    abstract fun bindUsageStatsDataSource(
        implementation: UsageStatsDataSourceImpl
    ): UsageStatsDataSource

    @Binds
    abstract fun bindAppMetadataDataSource(
        implementation: AppMetadataDataSourceImpl
    ): AppMetadataDataSource
}
