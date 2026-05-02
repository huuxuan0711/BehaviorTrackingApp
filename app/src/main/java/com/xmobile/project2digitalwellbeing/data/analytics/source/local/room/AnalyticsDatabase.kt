package com.xmobile.project2digitalwellbeing.data.analytics.source.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xmobile.project2digitalwellbeing.data.apps.source.local.room.dao.AppMetadataDao
import com.xmobile.project2digitalwellbeing.data.insights.source.local.room.dao.InsightDao
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao.RawUsageEventDao
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao.SessionDao
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao.SyncStateDao
import com.xmobile.project2digitalwellbeing.data.apps.source.local.room.entity.AppMetadataEntity
import com.xmobile.project2digitalwellbeing.data.insights.source.local.room.entity.InsightEntity
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.entity.RawUsageEventEntity
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.entity.SessionEntity
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.entity.SyncStateEntity

@Database(
    entities = [
        AppMetadataEntity::class,
        SessionEntity::class,
        InsightEntity::class,
        SyncStateEntity::class,
        RawUsageEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AnalyticsDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun sessionDao(): SessionDao
    abstract fun insightDao(): InsightDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun rawUsageEventDao(): RawUsageEventDao
}
