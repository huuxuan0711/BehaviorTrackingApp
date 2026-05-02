package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.insights.model.ComposedInsight
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightComposer
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.tracking.service.TransitionExtractor
import com.xmobile.project2digitalwellbeing.domain.insights.service.TransitionInsightGenerator
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractor
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class GetBehaviorInsightDetailDataParams(
    val nowMillis: Long,
    val timezoneId: String,
    val insightType: InsightType? = null
)

data class BehaviorInsightDetailData(
    val currentLocalDate: String,
    val title: String,
    val patternText: String,
    val lateNightUsageRatio: Float,
    val averageSessionLengthMillis: Long,
    val appTransitionsPerHour: Float,
    val hourlyUsage: List<HourlyUsage>,
    val peakUsageWindowStartHour: Int?,
    val peakUsageWindowEndHour: Int?,
    val meaningText: String,
    val suggestionText: String,
    val score: Int,
    val confidence: Float,
    val relatedPackages: List<String>,
    val sourceInsightType: InsightType?
)

sealed interface GetBehaviorInsightDetailDataOutcome {
    data class Success(val data: BehaviorInsightDetailData) : GetBehaviorInsightDetailDataOutcome

    data class Failure(val error: BehaviorInsightDetailDataError) : GetBehaviorInsightDetailDataOutcome
}

sealed interface BehaviorInsightDetailDataError {
    val stage: BehaviorInsightDetailDataStage
    val cause: Throwable?

    data class InvalidTimeZone(
        val timezoneId: String,
        override val stage: BehaviorInsightDetailDataStage,
        override val cause: Throwable?
    ) : BehaviorInsightDetailDataError

    data class DataAccessFailure(
        override val stage: BehaviorInsightDetailDataStage,
        override val cause: Throwable?
    ) : BehaviorInsightDetailDataError

    data class ProcessingFailure(
        override val stage: BehaviorInsightDetailDataStage,
        override val cause: Throwable?
    ) : BehaviorInsightDetailDataError
}

enum class BehaviorInsightDetailDataStage {
    RESOLVE_DATE,
    READ_SESSIONS,
    READ_INSIGHTS,
    READ_PREFERENCES,
    READ_APP_METADATA,
    ENRICH_SESSIONS,
    EXTRACT_FEATURES,
    BUILD_HOURLY_USAGE,
    EXTRACT_TRANSITIONS,
    GENERATE_TRANSITION_INSIGHT,
    COMPOSE_INSIGHTS,
    SELECT_INSIGHT
}

class GetBehaviorInsightDetailDataUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val sessionEnricher: SessionEnricher,
    private val featureExtractor: UsageFeatureExtractor,
    private val aggregator: UsageAggregator,
    private val transitionExtractor: TransitionExtractor,
    private val transitionInsightGenerator: TransitionInsightGenerator,
    private val insightComposer: InsightComposer
) {
    suspend operator fun invoke(params: GetBehaviorInsightDetailDataParams): GetBehaviorInsightDetailDataOutcome {
        val zoneId = runStage(BehaviorInsightDetailDataStage.RESOLVE_DATE, params) {
            ZoneId.of(params.timezoneId)
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val currentLocalDate = runStage(BehaviorInsightDetailDataStage.RESOLVE_DATE, params) {
            Instant.ofEpochMilli(params.nowMillis).atZone(zoneId).toLocalDate().toString()
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val windowStartMillis = runStage(BehaviorInsightDetailDataStage.RESOLVE_DATE, params) {
            Instant.ofEpochMilli(params.nowMillis)
                .atZone(zoneId)
                .toLocalDate()
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val windowEndMillis = runStage(BehaviorInsightDetailDataStage.RESOLVE_DATE, params) {
            Instant.ofEpochMilli(windowStartMillis)
                .atZone(zoneId)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val sessions = runStage(BehaviorInsightDetailDataStage.READ_SESSIONS, params) {
            repository.getSessions(windowStartMillis, windowEndMillis)
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val usageInsights = runStage(BehaviorInsightDetailDataStage.READ_INSIGHTS, params) {
            repository.getInsights(windowStartMillis, windowEndMillis)
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val preferences = runStage(BehaviorInsightDetailDataStage.READ_PREFERENCES, params) {
            usagePreferencesRepository.getUsageAnalysisPreferences()
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val appMetadataByPackage = runStage(BehaviorInsightDetailDataStage.READ_APP_METADATA, params) {
            repository.getAppMetadata(sessions.map { it.packageName }.toSet())
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val enrichedSessions = runStage(BehaviorInsightDetailDataStage.ENRICH_SESSIONS, params) {
            sessionEnricher.enrichSessions(
                sessions = sessions,
                timezoneId = params.timezoneId,
                appMetadataByPackage = appMetadataByPackage,
                preferences = preferences
            )
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val features = runStage(BehaviorInsightDetailDataStage.EXTRACT_FEATURES, params) {
            featureExtractor.extractFeatures(
                sessions = enrichedSessions,
                preferences = preferences
            )
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val hourlyUsage = runStage(BehaviorInsightDetailDataStage.BUILD_HOURLY_USAGE, params) {
            aggregator.buildHourlyUsage(
                sessions = sessions,
                timezoneId = params.timezoneId,
                localDate = currentLocalDate
            )
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val transitions = runStage(BehaviorInsightDetailDataStage.EXTRACT_TRANSITIONS, params) {
            transitionExtractor.extractTransitions(enrichedSessions)
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val transitionInsight = runStage(BehaviorInsightDetailDataStage.GENERATE_TRANSITION_INSIGHT, params) {
            transitionInsightGenerator.generateInsight(
                transitions = transitions,
                filter = TransitionFilter.DISTRACTING_MIXED
            )
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val composedInsights = runStage(BehaviorInsightDetailDataStage.COMPOSE_INSIGHTS, params) {
            insightComposer.compose(
                usageInsights = usageInsights,
                transitionInsight = transitionInsight
            )
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val selectedInsight = runStage(BehaviorInsightDetailDataStage.SELECT_INSIGHT, params) {
            selectInsight(
                requestedInsightType = params.insightType,
                usageInsights = usageInsights,
                composedInsights = composedInsights
            )
        }.getOrElse { return GetBehaviorInsightDetailDataOutcome.Failure(it.toBehaviorInsightDetailError(params.timezoneId)) }

        val peakUsageHour = hourlyUsage.maxByOrNull { it.totalTimeMillis }
            ?.takeIf { it.totalTimeMillis > 0L }
            ?.hourOfDay

        return GetBehaviorInsightDetailDataOutcome.Success(
            BehaviorInsightDetailData(
                currentLocalDate = currentLocalDate,
                title = selectedInsight.title,
                patternText = selectedInsight.patternText,
                lateNightUsageRatio = features.lateNightUsageRatio,
                averageSessionLengthMillis = features.averageSessionLengthMillis,
                appTransitionsPerHour = features.switchesPerHour,
                hourlyUsage = hourlyUsage,
                peakUsageWindowStartHour = peakUsageHour,
                peakUsageWindowEndHour = peakUsageHour?.let { (it + 2) % 24 },
                meaningText = selectedInsight.meaningText,
                suggestionText = selectedInsight.suggestionText,
                score = selectedInsight.score,
                confidence = selectedInsight.confidence,
                relatedPackages = selectedInsight.relatedPackages,
                sourceInsightType = selectedInsight.sourceInsightType
            )
        )
    }

    private fun selectInsight(
        requestedInsightType: InsightType?,
        usageInsights: List<Insight>,
        composedInsights: List<ComposedInsight>
    ): SelectedBehaviorInsight {
        val rawInsight = requestedInsightType?.let { requestedType ->
            usageInsights.firstOrNull { it.type == requestedType }
        } ?: usageInsights.maxWithOrNull(
            compareBy<Insight> { it.score }.thenBy { it.confidence }
        )

        val composedInsight = if (requestedInsightType == null) {
            composedInsights.maxWithOrNull(compareBy<ComposedInsight> { it.score }.thenBy { it.confidence })
        } else {
            null
        }

        return when {
            composedInsight != null && composedInsight.score >= (rawInsight?.score ?: -1) -> {
                SelectedBehaviorInsight(
                    title = composedInsight.title,
                    patternText = composedInsight.summary,
                    meaningText = meaningForComposedInsight(composedInsight),
                    suggestionText = suggestionForComposedInsight(composedInsight),
                    score = composedInsight.score,
                    confidence = composedInsight.confidence,
                    relatedPackages = composedInsight.relatedPackages,
                    sourceInsightType = composedInsight.sourceInsightTypes.firstOrNull()
                )
            }

            rawInsight != null -> {
                SelectedBehaviorInsight(
                    title = rawInsight.type.name.replace('_', ' ').lowercase()
                        .split(' ')
                        .joinToString(" ") { token -> token.replaceFirstChar(Char::uppercaseChar) },
                    patternText = patternTextForRawInsight(rawInsight.type),
                    meaningText = meaningForRawInsight(rawInsight.type),
                    suggestionText = suggestionForRawInsight(rawInsight.type),
                    score = rawInsight.score,
                    confidence = rawInsight.confidence,
                    relatedPackages = rawInsight.relatedPackages,
                    sourceInsightType = rawInsight.type
                )
            }

            else -> SelectedBehaviorInsight(
                title = "Behavior summary",
                patternText = "No strong behavior pattern was detected for the selected day.",
                meaningText = "Your usage signals for this day do not cross the current thresholds for a stronger interpretation.",
                suggestionText = "Keep watching for changes over multiple days before acting on a single low-signal snapshot.",
                score = 0,
                confidence = 0f,
                relatedPackages = emptyList(),
                sourceInsightType = null
            )
        }
    }

    private fun patternTextForRawInsight(type: InsightType): String {
        return when (type) {
            InsightType.LATE_NIGHT_USAGE ->
                "A meaningful share of your phone use happens late at night."

            InsightType.FREQUENT_SWITCHING ->
                "You switch between apps frequently, which suggests fragmented attention."

            InsightType.BINGE_USAGE ->
                "You spend long uninterrupted stretches inside the same app."

            InsightType.LATE_NIGHT_SWITCHING ->
                "Your late-night sessions include rapid switching between apps."
        }
    }

    private fun meaningForRawInsight(type: InsightType): String {
        return when (type) {
            InsightType.LATE_NIGHT_USAGE ->
                "This pattern usually means usage is extending into the period where sleep should be stabilizing."

            InsightType.FREQUENT_SWITCHING ->
                "High switching density often points to passive checking behavior rather than sustained task focus."

            InsightType.BINGE_USAGE ->
                "Long sessions often indicate immersion in a single app, which can be intentional or difficult to interrupt."

            InsightType.LATE_NIGHT_SWITCHING ->
                "Late-night switching combines delayed bedtime with fragmented attention, which is usually a worse recovery pattern than a single long session."
        }
    }

    private fun suggestionForRawInsight(type: InsightType): String {
        return when (type) {
            InsightType.LATE_NIGHT_USAGE ->
                "Set a hard cutoff for phone use before sleep and move high-stimulation apps out of easy reach."

            InsightType.FREQUENT_SWITCHING ->
                "Reduce app hopping by batching quick checks into one session instead of reopening multiple apps repeatedly."

            InsightType.BINGE_USAGE ->
                "Add time boundaries or break reminders to interrupt long uninterrupted app sessions."

            InsightType.LATE_NIGHT_SWITCHING ->
                "Use app limits or a simplified bedtime screen setup to reduce fast switching late at night."
        }
    }

    private fun meaningForComposedInsight(insight: ComposedInsight): String {
        return when (insight.transitionFilter) {
            TransitionFilter.DISTRACTING,
            TransitionFilter.DISTRACTING_MIXED ->
                "The pattern is reinforced by repeated transitions across distracting apps, not just isolated sessions."

            TransitionFilter.PRODUCTIVE,
            TransitionFilter.PRODUCTIVE_MIXED ->
                "The pattern is reinforced by a repeated workflow across productive apps, suggesting stable task flow."

            else ->
                "The pattern is supported by repeated transitions in your app graph rather than a single isolated metric."
        }
    }

    private fun suggestionForComposedInsight(insight: ComposedInsight): String {
        return when (insight.transitionFilter) {
            TransitionFilter.DISTRACTING,
            TransitionFilter.DISTRACTING_MIXED ->
                "Break the loop by removing one of the dominant distracting transitions from your late-night routine."

            TransitionFilter.PRODUCTIVE,
            TransitionFilter.PRODUCTIVE_MIXED ->
                "If this workflow is intentional, protect it with fewer interruptions and clearer task boundaries."

            else ->
                "Review the dominant app loop first, because changing that transition will have the biggest effect on the pattern."
        }
    }

    private suspend fun <T> runStage(
        stage: BehaviorInsightDetailDataStage,
        params: GetBehaviorInsightDetailDataParams,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            Result.failure(BehaviorInsightDetailDataException(stage, throwable))
        }
    }
}

private data class SelectedBehaviorInsight(
    val title: String,
    val patternText: String,
    val meaningText: String,
    val suggestionText: String,
    val score: Int,
    val confidence: Float,
    val relatedPackages: List<String>,
    val sourceInsightType: InsightType?
)

private data class BehaviorInsightDetailDataException(
    val stageValue: BehaviorInsightDetailDataStage,
    val throwableValue: Throwable
) : RuntimeException(throwableValue)

private fun Throwable.toBehaviorInsightDetailError(timezoneId: String): BehaviorInsightDetailDataError {
    val wrapped = this as? BehaviorInsightDetailDataException
    val stage = wrapped?.stageValue ?: BehaviorInsightDetailDataStage.SELECT_INSIGHT
    val cause = wrapped?.throwableValue ?: this

    return when (cause) {
        is java.time.DateTimeException,
        is IllegalArgumentException -> BehaviorInsightDetailDataError.InvalidTimeZone(
            timezoneId = timezoneId,
            stage = stage,
            cause = cause
        )

        is com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException -> {
            BehaviorInsightDetailDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )
        }

        else -> when (stage) {
            BehaviorInsightDetailDataStage.READ_SESSIONS,
            BehaviorInsightDetailDataStage.READ_INSIGHTS,
            BehaviorInsightDetailDataStage.READ_PREFERENCES,
            BehaviorInsightDetailDataStage.READ_APP_METADATA -> BehaviorInsightDetailDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )

            BehaviorInsightDetailDataStage.RESOLVE_DATE,
            BehaviorInsightDetailDataStage.ENRICH_SESSIONS,
            BehaviorInsightDetailDataStage.EXTRACT_FEATURES,
            BehaviorInsightDetailDataStage.BUILD_HOURLY_USAGE,
            BehaviorInsightDetailDataStage.EXTRACT_TRANSITIONS,
            BehaviorInsightDetailDataStage.GENERATE_TRANSITION_INSIGHT,
            BehaviorInsightDetailDataStage.COMPOSE_INSIGHTS,
            BehaviorInsightDetailDataStage.SELECT_INSIGHT -> BehaviorInsightDetailDataError.ProcessingFailure(
                stage = stage,
                cause = cause
            )
        }
    }
}
