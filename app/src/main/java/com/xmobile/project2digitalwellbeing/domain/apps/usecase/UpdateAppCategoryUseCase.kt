package com.xmobile.project2digitalwellbeing.domain.apps.usecase

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import javax.inject.Inject

class UpdateAppCategoryUseCase @Inject constructor(
    private val repository: UsageRepository
) {
    suspend operator fun invoke(packageName: String, category: AppCategory) {
        repository.updateAppCategory(packageName, category)
    }
}
