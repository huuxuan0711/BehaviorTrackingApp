package com.xmobile.project2digitalwellbeing.di

import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionBuilder
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionBuilderImpl
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricherImpl
import com.xmobile.project2digitalwellbeing.domain.tracking.service.TransitionExtractor
import com.xmobile.project2digitalwellbeing.domain.tracking.service.TransitionExtractorImpl
import com.xmobile.project2digitalwellbeing.domain.insights.service.TransitionInsightGenerator
import com.xmobile.project2digitalwellbeing.domain.insights.service.TransitionInsightGeneratorImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageTrendAnalyzer
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageTrendAnalyzerImpl
import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightComposer
import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightComposerImpl
import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightEngine
import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightEngineImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregatorImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractor
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractorImpl
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageErrorMapper
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageErrorMapperImpl
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageRefreshPolicy
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageRefreshPolicyImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppAnalyticsDomainModule {
    @Binds
    abstract fun bindSessionBuilder(
        implementation: SessionBuilderImpl
    ): SessionBuilder

    @Binds
    abstract fun bindSessionEnricher(
        implementation: SessionEnricherImpl
    ): SessionEnricher

    @Binds
    abstract fun bindUsageAggregator(
        implementation: UsageAggregatorImpl
    ): UsageAggregator

    @Binds
    abstract fun bindUsageFeatureExtractor(
        implementation: UsageFeatureExtractorImpl
    ): UsageFeatureExtractor

    @Binds
    abstract fun bindInsightEngine(
        implementation: InsightEngineImpl
    ): InsightEngine

    @Binds
    abstract fun bindTransitionExtractor(
        implementation: TransitionExtractorImpl
    ): TransitionExtractor

    @Binds
    abstract fun bindTransitionInsightGenerator(
        implementation: TransitionInsightGeneratorImpl
    ): TransitionInsightGenerator

    @Binds
    abstract fun bindUsageTrendAnalyzer(
        implementation: UsageTrendAnalyzerImpl
    ): UsageTrendAnalyzer

    @Binds
    abstract fun bindInsightComposer(
        implementation: InsightComposerImpl
    ): InsightComposer

    @Binds
    abstract fun bindUsageRefreshPolicy(
        implementation: UsageRefreshPolicyImpl
    ): UsageRefreshPolicy

    @Binds
    abstract fun bindUsageErrorMapper(
        implementation: UsageErrorMapperImpl
    ): UsageErrorMapper
}
