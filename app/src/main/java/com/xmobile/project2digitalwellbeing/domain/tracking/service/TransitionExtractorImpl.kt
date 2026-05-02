package com.xmobile.project2digitalwellbeing.domain.tracking.service

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import javax.inject.Inject

class TransitionExtractorImpl @Inject constructor() : TransitionExtractor {

    override fun extractTransitions(sessions: List<EnrichedSession>): List<AppTransitionStat> {
        val orderedSessions = sessions
            .asSequence()
            .filter { it.session.packageName.isNotBlank() }
            .filter { it.session.durationMillis > 0L }
            .sortedWith(
                compareBy<EnrichedSession> { it.session.startTimeMillis }
                    .thenBy { it.session.endTimeMillis }
                    .thenBy { it.session.packageName }
            )
            .toList()

        if (orderedSessions.size < 2) {
            return emptyList()
        }

        val transitionBuckets = linkedMapOf<TransitionKey, MutableTransitionAccumulator>()

        orderedSessions.zipWithNext().forEach { (fromSession, toSession) ->
            if (fromSession.session.packageName == toSession.session.packageName) {
                return@forEach
            }

            val transitionIntervalMillis = (toSession.session.startTimeMillis - fromSession.session.endTimeMillis)
                .coerceAtLeast(0L)
            val key = TransitionKey(
                fromPackageName = fromSession.session.packageName,
                toPackageName = toSession.session.packageName
            )
            val accumulator = transitionBuckets.getOrPut(key) {
                MutableTransitionAccumulator(
                    fromSession = fromSession,
                    toSession = toSession
                )
            }

            accumulator.transitionCount += 1
            accumulator.totalIntervalMillis += transitionIntervalMillis
            if (fromSession.isLateNight || toSession.isLateNight) {
                accumulator.lateNightTransitionCount += 1
            }
            accumulator.lastTransitionTimestampMillis = maxOf(
                accumulator.lastTransitionTimestampMillis,
                toSession.session.startTimeMillis
            )
        }

        return transitionBuckets.values
            .map { accumulator -> accumulator.toStat() }
            .sortedWith(
                compareByDescending<AppTransitionStat> { it.transitionCount }
                    .thenByDescending { it.lateNightTransitionCount }
                    .thenBy { it.fromPackageName }
                    .thenBy { it.toPackageName }
            )
    }

    private data class TransitionKey(
        val fromPackageName: String,
        val toPackageName: String
    )

    private data class MutableTransitionAccumulator(
        val fromSession: EnrichedSession,
        val toSession: EnrichedSession,
        var transitionCount: Int = 0,
        var totalIntervalMillis: Long = 0L,
        var lateNightTransitionCount: Int = 0,
        var lastTransitionTimestampMillis: Long = 0L
    ) {
        fun toStat(): AppTransitionStat {
            return AppTransitionStat(
                fromPackageName = fromSession.session.packageName,
                fromAppName = fromSession.appName,
                fromCategory = fromSession.category,
                toPackageName = toSession.session.packageName,
                toAppName = toSession.appName,
                toCategory = toSession.category,
                transitionCount = transitionCount,
                averageIntervalMillis = if (transitionCount == 0) 0L else totalIntervalMillis / transitionCount,
                totalIntervalMillis = totalIntervalMillis,
                lateNightTransitionCount = lateNightTransitionCount,
                lastTransitionTimestampMillis = lastTransitionTimestampMillis
            )
        }
    }
}
