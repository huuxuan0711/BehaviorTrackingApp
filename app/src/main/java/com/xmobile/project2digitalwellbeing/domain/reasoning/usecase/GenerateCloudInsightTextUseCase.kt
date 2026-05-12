package com.xmobile.project2digitalwellbeing.domain.reasoning.usecase

import android.util.Log
import com.xmobile.project2digitalwellbeing.BuildConfig
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
    val fallbackInsight: InterpretedInsight? = null,
    val languageCode: String = "en"
)

class GenerateCloudInsightTextUseCase @Inject constructor(
    private val cloudInsightRepository: CloudInsightRepository
) {
    private val logTag = "CloudInsightUseCase"

    suspend operator fun invoke(params: GenerateCloudInsightTextParams): Result<CloudInsightText> {
        val result = cloudInsightRepository.generateInsightText(
            CloudInsightRequest(
                surface = params.surface,
                groundedContext = params.groundedContext,
                fallbackInsight = params.fallbackInsight,
                languageCode = params.languageCode
            )
        )
        if (BuildConfig.DEBUG) {
            result.onSuccess { insight ->
                Log.d(
                    logTag,
                    "return_success surface=${params.surface} model=${insight.modelName} finish=${insight.finishReason} text=${insight.text.toLogSnippet()}"
                )
            }.onFailure { error ->
                Log.e(logTag, "return_failure surface=${params.surface} reason=${error.message}", error)
            }
        }
        return result
    }

    private fun String.toLogSnippet(maxLen: Int = 200): String {
        val compact = replace("\n", " ").replace("\r", " ").trim()
        return if (compact.length <= maxLen) compact else compact.take(maxLen) + "..."
    }
}
