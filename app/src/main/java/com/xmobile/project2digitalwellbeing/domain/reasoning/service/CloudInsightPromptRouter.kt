package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import javax.inject.Inject

class CloudInsightPromptRouter @Inject constructor(
    private val policy: CloudInsightPromptPolicy,
    dashboardBuilder: DashboardCloudInsightPromptBuilder,
    weeklyBuilder: WeeklyCloudInsightPromptBuilder,
    lateNightBuilder: LateNightCloudInsightPromptBuilder,
    sessionTimelineBuilder: SessionTimelineCloudInsightPromptBuilder,
    transitionGraphBuilder: TransitionGraphCloudInsightPromptBuilder
) {
    private val builders: List<CloudInsightPromptBuilder> = listOf(
        dashboardBuilder,
        weeklyBuilder,
        lateNightBuilder,
        sessionTimelineBuilder,
        transitionGraphBuilder
    )

    fun buildSystemInstruction(request: CloudInsightRequest): String = policy.buildSystemInstruction(request)

    fun buildUserPrompt(request: CloudInsightRequest): String {
        val builder = builders.firstOrNull { it.supports(request) }
            ?: error("No prompt builder registered for surface ${request.surface}.")
        return builder.buildUserPrompt(request)
    }
}
