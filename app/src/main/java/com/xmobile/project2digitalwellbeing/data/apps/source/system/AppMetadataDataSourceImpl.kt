package com.xmobile.project2digitalwellbeing.data.apps.source.system

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerError
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerSource
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.apps.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.apps.model.SourceAppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppMetadataDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppMetadataDataSource {

    private val packageManager: PackageManager
        get() = context.packageManager

    override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
        if (packageNames.isEmpty()) {
            return emptyMap()
        }

        try {
            return packageNames.associateWith(::buildAppMetadata)
        } catch (exception: SecurityException) {
            throw UsageDataLayerException(
                UsageDataLayerError.PermissionDenied(
                    source = UsageDataLayerSource.APP_METADATA,
                    cause = exception
                )
            )
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.SystemReadFailed(
                    source = UsageDataLayerSource.APP_METADATA,
                    cause = exception
                )
            )
        }
    }

    private fun buildAppMetadata(packageName: String): AppMetadata {
        val applicationInfo = getApplicationInfoOrNull(packageName)
        val appName = applicationInfo?.let { packageManager.getApplicationLabel(it).toString() }
        val resolution = resolveCategory(
            packageName = packageName,
            appName = appName,
            applicationInfo = applicationInfo
        )

        return AppMetadata(
            packageName = packageName,
            appName = appName,
            sourceCategory = resolution.sourceCategory,
            reportingCategory = resolution.reportingCategory,
            classificationSource = resolution.classificationSource,
            confidence = resolution.confidence
        )
    }

    private fun getApplicationInfoOrNull(packageName: String): ApplicationInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun resolveCategory(
        packageName: String,
        appName: String?,
        applicationInfo: ApplicationInfo?
    ): CategoryResolution {
        return AppCategoryClassifier.resolve(
            packageName = packageName,
            appName = appName,
            systemCategory = applicationInfo?.toSystemBackedCategory()
        )
    }
}
