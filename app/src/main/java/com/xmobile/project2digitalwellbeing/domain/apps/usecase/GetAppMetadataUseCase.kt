package com.xmobile.project2digitalwellbeing.domain.apps.usecase

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.apps.repository.AppRepository
import javax.inject.Inject

class GetAppMetadataUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(packageNames: Set<String>): Map<String, AppMetadata> {
        return appRepository.getAppMetadata(packageNames)
    }
}
