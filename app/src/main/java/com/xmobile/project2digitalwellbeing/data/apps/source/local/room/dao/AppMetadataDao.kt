package com.xmobile.project2digitalwellbeing.data.apps.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xmobile.project2digitalwellbeing.data.apps.source.local.room.entity.AppMetadataEntity

@Dao
interface AppMetadataDao {
    @Query("SELECT * FROM app_metadata WHERE packageName IN (:packageNames)")
    suspend fun getByPackageNames(packageNames: List<String>): List<AppMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(metadata: List<AppMetadataEntity>)
}
