package com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.entity.RawUsageEventEntity

@Dao
interface RawUsageEventDao {
    @Query(
        """
        SELECT * FROM raw_usage_events
        WHERE timestampMillis BETWEEN :startTimeMillis AND :endTimeMillis
        ORDER BY timestampMillis ASC
        """
    )
    suspend fun getEventsInRange(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<RawUsageEventEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<RawUsageEventEntity>)

    @Query(
        """
        DELETE FROM raw_usage_events
        WHERE timestampMillis BETWEEN :startTimeMillis AND :endTimeMillis
        """
    )
    suspend fun deleteEventsInRange(startTimeMillis: Long, endTimeMillis: Long)
}
