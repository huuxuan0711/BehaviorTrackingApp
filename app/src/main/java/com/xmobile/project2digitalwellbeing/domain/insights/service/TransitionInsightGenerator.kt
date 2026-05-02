package com.xmobile.project2digitalwellbeing.domain.insights.service

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.insights.model.TransitionInsight

interface TransitionInsightGenerator {
    fun filterTransitions(
        transitions: List<AppTransitionStat>,
        filter: TransitionFilter
    ): List<AppTransitionStat>

    fun generateInsight(
        transitions: List<AppTransitionStat>,
        filter: TransitionFilter
    ): TransitionInsight?
}
