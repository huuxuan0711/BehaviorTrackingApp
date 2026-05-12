package com.xmobile.project2digitalwellbeing.domain.reasoning.repository

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightText

interface CloudInsightRepository {
    suspend fun generateInsightText(request: CloudInsightRequest): Result<CloudInsightText>
}
