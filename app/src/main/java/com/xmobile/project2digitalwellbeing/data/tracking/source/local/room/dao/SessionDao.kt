package com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.entity.SessionEntity

@Dao
interface SessionDao {
    @Query(
        """
        SELECT * FROM sessions
        WHERE startTimeMillis < :endTimeMillis
        AND endTimeMillis > :startTimeMillis
        ORDER BY startTimeMillis ASC
        """
    )
    suspend fun getSessionsInRange(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Query(
        """
        DELETE FROM sessions
        WHERE startTimeMillis < :endTimeMillis
        AND endTimeMillis > :startTimeMillis
        """
    )
    suspend fun deleteSessionsInRange(startTimeMillis: Long, endTimeMillis: Long)
}
