package com.xmobile.project2digitalwellbeing.domain.reasoning.usecase

import com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightText
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.LlmGroundedContext
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudInsightRepository
import javax.inject.Inject

data class GenerateCloudInsightTextParams(
    val surface: CloudInsightSurface,
    val groundedContext: LlmGroundedContext,
    val fallbackInsight: InterpretedInsight? = null
)

class GenerateCloudInsightTextUseCase @Inject constructor(
    private val cloudInsightRepository: CloudInsightRepository
) {
    suspend operator fun invoke(params: GenerateCloudInsightTextParams): Result<CloudInsightText> {
        return cloudInsightRepository.generateInsightText(
            CloudInsightRequest(
                surface = params.surface,
                groundedContext = params.groundedContext,
                fallbackInsight = params.fallbackInsight
            )
        )
    }
}
