package com.xmobile.project2digitalwellbeing.data.apps.repository

import android.content.Context
import com.xmobile.project2digitalwellbeing.data.apps.mapper.toDomain
import com.xmobile.project2digitalwellbeing.data.apps.mapper.toEntity
import com.xmobile.project2digitalwellbeing.data.apps.source.local.room.dao.AppMetadataDao
import com.xmobile.project2digitalwellbeing.data.apps.source.system.AppMetadataDataSource
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerError
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerSource
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.apps.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.apps.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val appMetadataDataSource: AppMetadataDataSource,
    private val appMetadataDao: AppMetadataDao,
    @ApplicationContext private val context: Context
) : AppRepository {

    override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
        if (packageNames.isEmpty()) {
            return emptyMap()
        }

        return try {
            val cachedMetadata = appMetadataDao.getByPackageNames(packageNames.toList())
                .associateBy { it.packageName }

            val missingPackageNames = packageNames - cachedMetadata.keys
            val resolvedMetadata = if (missingPackageNames.isEmpty()) {
                emptyMap()
            } else {
                appMetadataDataSource.getAppMetadata(missingPackageNames).also { freshMetadata ->
                    if (freshMetadata.isNotEmpty()) {
                        val updatedAtMillis = System.currentTimeMillis()
                        appMetadataDao.upsertAll(
                            freshMetadata.values.map { metadata ->
                                metadata.toEntity(updatedAtMillis = updatedAtMillis)
                            }
                        )
                    }
                }
            }

            buildMap {
                cachedMetadata.values.forEach { entity ->
                    put(entity.packageName, entity.toDomain())
                }
                putAll(resolvedMetadata)
            }
        } catch (exception: UsageDataLayerException) {
            throw exception
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheReadFailed(
                    source = UsageDataLayerSource.METADATA_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun getAllAppMetadata(): List<AppMetadata> {
        return try {
            val cachedMetadata = appMetadataDao.getAll()
            val cachedMap = cachedMetadata.associateBy { it.packageName }

            appMetadataDataSource.getAllInstalledAppsMetadata().also { freshMetadata ->
                if (freshMetadata.isNotEmpty()) {
                    val updatedAtMillis = System.currentTimeMillis()
                    val toUpdate = freshMetadata.map { metadata ->
                        val existing = cachedMap[metadata.packageName]
                        if (existing != null && existing.classificationSource == ClassificationSource.MANUAL_OVERRIDE.name) {
                            metadata.toEntity(updatedAtMillis).copy(
                                reportingCategory = existing.reportingCategory,
                                classificationSource = existing.classificationSource,
                                confidence = existing.confidence
                            )
                        } else {
                            metadata.toEntity(updatedAtMillis)
                        }
                    }
                    appMetadataDao.upsertAll(toUpdate)
                }
            }

            appMetadataDao.getAll().map { it.toDomain() }
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.SystemReadFailed(
                    source = UsageDataLayerSource.APP_METADATA,
                    cause = exception
                )
            )
        }
    }

    override suspend fun updateAppCategory(packageName: String, category: AppCategory) {
        try {
            val existing = appMetadataDao.getByPackageNames(listOf(packageName)).firstOrNull()
            if (existing != null) {
                appMetadataDao.upsertAll(
                    listOf(
                        existing.copy(
                            reportingCategory = category.name,
                            classificationSource = ClassificationSource.MANUAL_OVERRIDE.name,
                            confidence = 1.0f,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    )
                )
            }
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheWriteFailed(
                    source = UsageDataLayerSource.METADATA_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun resolveAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}
