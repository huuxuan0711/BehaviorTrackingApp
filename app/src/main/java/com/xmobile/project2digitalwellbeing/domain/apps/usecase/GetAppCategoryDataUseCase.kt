package com.xmobile.project2digitalwellbeing.domain.apps.usecase

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppFocusGroup
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class GetAppCategoryDataUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val aggregator: UsageAggregator
) {
    suspend operator fun invoke(): Map<AppFocusGroup, List<AppUsageStat>> {
        val nowMillis = System.currentTimeMillis()
        val startOfDayMillis = nowMillis - 24L * 60 * 60 * 1000

        val sessions = repository.getSessions(startOfDayMillis, nowMillis)
        val allAppMetadataList = repository.getAllAppMetadata()
        val allAppMetadataMap = allAppMetadataList.associateBy { it.packageName }

        val usageStats = aggregator.buildAppUsageStats(sessions, allAppMetadataMap)
        val usageStatsByPackage = usageStats.associateBy { it.packageName }

        val allAppsWithUsage = allAppMetadataList.map { metadata ->
            usageStatsByPackage[metadata.packageName] ?: AppUsageStat(
                packageName = metadata.packageName,
                appName = metadata.appName,
                category = metadata.reportingCategory,
                totalTimeMillis = 0L,
                launchCount = 0
            )
        }

        val knownPackages = allAppMetadataList.map { it.packageName }.toSet()
        val missingFromMetadata = usageStats.filter { it.packageName !in knownPackages }

        val finalStats = (allAppsWithUsage + missingFromMetadata)
            .sortedWith(
                compareByDescending<AppUsageStat> { it.totalTimeMillis }
                    .thenBy { it.appName ?: it.packageName }
            )

        return finalStats.groupBy { it.category.toFocusGroup() }
            .toSortedMap(compareBy { it.name })
    }

    private fun AppCategory.toFocusGroup(): AppFocusGroup {
        return when (this) {
            AppCategory.PRODUCTIVITY,
            AppCategory.EDUCATION,
            AppCategory.TOOLS -> AppFocusGroup.PRODUCTIVE

            AppCategory.SOCIAL,
            AppCategory.VIDEO,
            AppCategory.GAME,
            AppCategory.MUSIC -> AppFocusGroup.DISTRACTING

            AppCategory.COMMUNICATION,
            AppCategory.BROWSER,
            AppCategory.SYSTEM,
            AppCategory.OTHER,
            AppCategory.UNKNOWN -> AppFocusGroup.NEUTRAL
        }
    }
}
