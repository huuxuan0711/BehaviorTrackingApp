package com.xmobile.project2digitalwellbeing.domain.apps.usecase

import com.xmobile.project2digitalwellbeing.domain.apps.repository.AppRepository
import javax.inject.Inject

class ResolveAppNameUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(packageName: String): String {
        return appRepository.resolveAppName(packageName)
    }
}
