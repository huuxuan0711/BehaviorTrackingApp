package com.xmobile.project2digitalwellbeing.domain.insights.service

import com.xmobile.project2digitalwellbeing.domain.insights.model.ComposedInsight
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.model.TransitionInsight

interface InsightComposer {
    fun compose(
        usageInsights: List<Insight>,
        transitionInsight: TransitionInsight?
    ): List<ComposedInsight>
}
