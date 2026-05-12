package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionContext
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionDecision
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionMode
import javax.inject.Inject

class InsightResolutionStrategyImpl @Inject constructor() : InsightResolutionStrategy {

    override fun resolve(context: InsightResolutionContext): InsightResolutionDecision {
        if (context.hasLocalReasoning) {
            if (
                context.allowCloudEnhancement &&
                context.networkAvailable &&
                context.cloudEnhancementEligible
            ) {
                return InsightResolutionDecision(
                    mode = InsightResolutionMode.LOCAL_PLUS_CLOUD_AI,
                    useRuleInsight = context.hasRuleInsight,
                    useLocalReasoning = true,
                    requestCloudEnhancement = true
                )
            }

            return InsightResolutionDecision(
                mode = if (context.allowCloudEnhancement) {
                    InsightResolutionMode.FALLBACK_TO_LOCAL
                } else {
                    InsightResolutionMode.LOCAL_REASONING
                },
                useRuleInsight = context.hasRuleInsight,
                useLocalReasoning = true,
                requestCloudEnhancement = false
            )
        }

        return InsightResolutionDecision(
            mode = InsightResolutionMode.RULE_ONLY,
            useRuleInsight = context.hasRuleInsight,
            useLocalReasoning = false,
            requestCloudEnhancement = false
        )
    }
}
