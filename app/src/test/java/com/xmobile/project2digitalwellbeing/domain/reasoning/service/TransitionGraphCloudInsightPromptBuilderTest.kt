package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.LlmGroundedContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TransitionGraphCloudInsightPromptBuilderTest {

    private val policy = mock(CloudInsightPromptPolicy::class.java)
    private val builder = TransitionGraphCloudInsightPromptBuilder(policy)

    @Test
    fun `supports returns true for TRANSITION_GRAPH surface`() {
        val request = createRequest(CloudInsightSurface.TRANSITION_GRAPH)
        assertTrue(builder.supports(request))
    }

    @Test
    fun `supports returns false for other surfaces`() {
        val request = createRequest(CloudInsightSurface.DASHBOARD)
        assertFalse(builder.supports(request))
    }

    @Test
    fun `buildUserPrompt appends policy grounding section`() {
        val request = createRequest(CloudInsightSurface.TRANSITION_GRAPH)
        val mockGrounding = "MOCK_GROUNDING_DATA"
        `when`(policy.buildGroundingSection(request)).thenReturn(mockGrounding)

        val prompt = builder.buildUserPrompt(request)

        assertTrue(prompt.contains("Generate one concise transition behavior insight"))
        assertTrue(prompt.contains(mockGrounding))
    }

    private fun createRequest(surface: CloudInsightSurface): CloudInsightRequest {
        val context = LlmGroundedContext(
            primaryPattern = null,
            secondaryPatterns = emptyList(),
            riskScore = 50,
            confidence = 0.8f,
            summary = "",
            evidence = emptyList(),
            recommendations = emptyList()
        )
        return CloudInsightRequest(
            surface = surface,
            groundedContext = context,
            fallbackInsight = null
        )
    }
}

