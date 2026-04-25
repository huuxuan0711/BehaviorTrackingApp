package com.xmobile.project2digitalwellbeing.data.usage.source.system

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.usage.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppMetadataDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppMetadataDataSource {

    private val packageManager: PackageManager
        get() = context.packageManager

    override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
        return packageNames.takeUnless { it.isEmpty() }
            ?.associateWith(::buildAppMetadata)
            ?: emptyMap()
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
