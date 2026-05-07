package com.xmobile.project2digitalwellbeing.domain.apps.usecase

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppFocusGroup
import com.xmobile.project2digitalwellbeing.domain.apps.model.toFocusGroup
import com.xmobile.project2digitalwellbeing.domain.apps.repository.AppRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import javax.inject.Inject

class GetAppCategoryDataUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val appRepository: AppRepository,
    private val aggregator: UsageAggregator
) {
    suspend operator fun invoke(): Map<AppFocusGroup, List<AppUsageStat>> {
        val nowMillis = System.currentTimeMillis()
        val startOfDayMillis = nowMillis - 24L * 60 * 60 * 1000

        val sessions = usageRepository.getSessions(startOfDayMillis, nowMillis)
        val allAppMetadataList = appRepository.getAllAppMetadata()
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
}
