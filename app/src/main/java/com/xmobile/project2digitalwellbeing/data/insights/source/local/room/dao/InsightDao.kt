package com.xmobile.project2digitalwellbeing.data.insights.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xmobile.project2digitalwellbeing.data.insights.source.local.room.entity.InsightEntity

@Dao
interface InsightDao {
    @Query(
        """
        SELECT * FROM insights
        WHERE windowStartMillis >= :startTimeMillis
        AND windowEndMillis <= :endTimeMillis
        ORDER BY score DESC
        """
    )
    suspend fun getInsightsInRange(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<InsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<InsightEntity>)

    @Query(
        """
        DELETE FROM insights
        WHERE windowStartMillis >= :startTimeMillis
        AND windowEndMillis <= :endTimeMillis
        """
    )
    suspend fun deleteInsightsInRange(startTimeMillis: Long, endTimeMillis: Long)
}
