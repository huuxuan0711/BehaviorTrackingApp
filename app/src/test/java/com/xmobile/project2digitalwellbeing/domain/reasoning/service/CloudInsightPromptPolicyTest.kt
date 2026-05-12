package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.LlmGroundedContext
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudInsightPromptPolicyTest {

    private val policy = CloudInsightPromptPolicy()

    @Test
    fun `buildGroundingSection uses firm tone when risk score is high`() {
        val request = createRequestWithRiskScore(80)
        val text = policy.buildGroundingSection(request)
        assertTrue(text.contains("Required Tone: Empathetic but firm"))
    }

    @Test
    fun `buildGroundingSection uses neutral tone when risk score is moderate`() {
        val request = createRequestWithRiskScore(50)
        val text = policy.buildGroundingSection(request)
        assertTrue(text.contains("Required Tone: Informative and neutral"))
    }

    @Test
    fun `buildGroundingSection uses positive tone when risk score is low`() {
        val request = createRequestWithRiskScore(20)
        val text = policy.buildGroundingSection(request)
        assertTrue(text.contains("Required Tone: Positive and encouraging"))
    }

    private fun createRequestWithRiskScore(score: Int): CloudInsightRequest {
        val context = LlmGroundedContext(
            primaryPattern = "TEST_PATTERN",
            secondaryPatterns = emptyList(),
            riskScore = score,
            confidence = 0.8f,
            summary = "Test summary",
            evidence = emptyList(),
            recommendations = emptyList()
        )
        return CloudInsightRequest(
            surface = CloudInsightSurface.DASHBOARD,
            groundedContext = context,
            fallbackInsight = null
        )
    }
}

