package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningInput
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningResult

interface BehaviorReasoningEngine {
    fun reason(input: BehaviorReasoningInput): BehaviorReasoningResult
}
