package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.SessionTimelineDataError
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SessionTimelineViewModel @Inject constructor(
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase,
    private val getSessionTimelineDataUseCase: GetSessionTimelineDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionTimelineUiState())
    val uiState: StateFlow<SessionTimelineUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SessionTimelineEffect>()
    val effects: SharedFlow<SessionTimelineEffect> = _effects.asSharedFlow()

    private var hasLoadedData = false

    fun onPermissionMissing() {
        viewModelScope.launch {
            _effects.emit(SessionTimelineEffect.OpenPermission)
        }
    }

    fun load(forceRefresh: Boolean, selectedDate: LocalDate = _uiState.value.selectedDate) {
        val timezoneId = ZoneId.systemDefault().id
        val zoneId = ZoneId.of(timezoneId)
        val today = LocalDate.now(zoneId)
        val normalizedDate = selectedDate.coerceAtMost(today)
        val anchorDate = normalizedDate
        val nowMillis = anchorDate.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
        val shouldRefreshCurrentDay = normalizedDate == today

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedDate = normalizedDate,
                    errorMessage = null
                )
            }

            val refreshError = if (shouldRefreshCurrentDay && (forceRefresh || !hasLoadedData)) {
                when (
                    val refreshOutcome = refreshUsageDataUseCase(
                        RefreshUsageDataParams(
                            nowMillis = System.currentTimeMillis(),
                            timezoneId = timezoneId,
                            forceFullRefresh = forceRefresh
                        )
                    )
                ) {
                    is RefreshUsageDataOutcome.Success -> null
                    is RefreshUsageDataOutcome.Failure -> refreshOutcome.error.toUserMessage()
                }
            } else {
                null
            }

            when (
                val outcome = getSessionTimelineDataUseCase(
                    GetSessionTimelineDataParams(
                        nowMillis = nowMillis,
                        timezoneId = timezoneId
                    )
                )
            ) {
                is GetSessionTimelineDataOutcome.Success -> {
                    hasLoadedData = true
                    val sessions = outcome.data.sessions.mergeAdjacentSameAppSessions(
                        zoneId = zoneId,
                        selectedDate = normalizedDate,
                        lateNightStartHour = outcome.data.lateNightStartHour
                    )
                    val maxDurationMillis = sessions.maxOfOrNull { it.session.durationMillis } ?: 0L

                    _uiState.value = SessionTimelineUiState(
                        selectedDate = normalizedDate,
                        dateRangeLabel = normalizedDate.formatDateLabel(),
                        insightText = refreshError ?: outcome.data.insight?.summary
                        ?: "No session insight available yet.",
                        sessions = sessions.mapIndexed { index, session ->
                            session.toTimelineItem(
                                previousSession = sessions.getOrNull(index - 1),
                                maxDurationMillis = maxDurationMillis,
                                zoneId = zoneId,
                                selectedDate = normalizedDate,
                                lateNightStartHour = outcome.data.lateNightStartHour
                            )
                        },
                        errorMessage = refreshError,
                        canNavigateNext = normalizedDate.isBefore(today)
                    )
                }

                is GetSessionTimelineDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            selectedDate = normalizedDate,
                            dateRangeLabel = normalizedDate.formatDateLabel(),
                            insightText = refreshError ?: outcome.error.toUserMessage(),
                            errorMessage = refreshError ?: outcome.error.toUserMessage(),
                            canNavigateNext = normalizedDate.isBefore(today)
                        )
                    }
                }
            }
        }
    }

    fun showPreviousDay() {
        load(forceRefresh = false, selectedDate = _uiState.value.selectedDate.minusDays(1))
    }

    fun showNextDay() {
        if (_uiState.value.canNavigateNext) {
            load(forceRefresh = false, selectedDate = _uiState.value.selectedDate.plusDays(1))
        }
    }

    private fun List<EnrichedSession>.mergeAdjacentSameAppSessions(
        zoneId: ZoneId,
        selectedDate: LocalDate,
        lateNightStartHour: Int
    ): List<EnrichedSession> {
        if (isEmpty()) return emptyList()

        val mergedSessions = mutableListOf<EnrichedSession>()
        for (session in this) {
            val previous = mergedSessions.lastOrNull()
            if (previous != null && previous.canMergeWith(
                    next = session,
                    zoneId = zoneId,
                    selectedDate = selectedDate,
                    lateNightStartHour = lateNightStartHour
                )
            ) {
                mergedSessions[mergedSessions.lastIndex] = previous.mergeWith(session)
            } else {
                mergedSessions += session
            }
        }
        return mergedSessions
    }

    private fun EnrichedSession.canMergeWith(
        next: EnrichedSession,
        zoneId: ZoneId,
        selectedDate: LocalDate,
        lateNightStartHour: Int
    ): Boolean {
        val gapMillis = next.session.startTimeMillis - session.endTimeMillis
        return session.packageName == next.session.packageName &&
            gapMillis in 0..MERGE_ADJACENT_SESSION_GAP_MILLIS &&
            session.startTimeMillis.toPeriodLabel(zoneId, selectedDate, lateNightStartHour) ==
            next.session.startTimeMillis.toPeriodLabel(zoneId, selectedDate, lateNightStartHour)
    }

    private fun EnrichedSession.mergeWith(next: EnrichedSession): EnrichedSession {
        val mergedStartTimeMillis = minOf(session.startTimeMillis, next.session.startTimeMillis)
        val mergedEndTimeMillis = maxOf(session.endTimeMillis, next.session.endTimeMillis)
        return copy(
            session = AppSession(
                packageName = session.packageName,
                startTimeMillis = mergedStartTimeMillis,
                endTimeMillis = mergedEndTimeMillis,
                durationMillis = session.durationMillis + next.session.durationMillis
            )
        )
    }

    private fun EnrichedSession.toTimelineItem(
        previousSession: EnrichedSession?,
        maxDurationMillis: Long,
        zoneId: ZoneId,
        selectedDate: LocalDate,
        lateNightStartHour: Int
    ): SessionTimelineItemUiModel {
        return SessionTimelineItemUiModel(
            packageName = session.packageName,
            appName = appName ?: session.packageName,
            periodLabel = session.startTimeMillis.toPeriodLabel(
                zoneId = zoneId,
                selectedDate = selectedDate,
                lateNightStartHour = lateNightStartHour
            ),
            timeRangeText = UsageFormatter.formatTimeRange(session.startTimeMillis, session.endTimeMillis, zoneId),
            durationText = UsageFormatter.formatDurationVerbose(session.durationMillis),
            durationMillis = session.durationMillis,
            progressRatio = if (maxDurationMillis <= 0L) 0f else {
                session.durationMillis.toFloat() / maxDurationMillis.toFloat()
            },
            transitionLabel = previousSession
                ?.takeIf { it.session.packageName != session.packageName }
                ?.let { appName ?: session.packageName },
            category = category
        )
    }

    private fun com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.toUserMessage(): String {
        return when (this) {
            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.PermissionDenied ->
                "Usage access is required to refresh your session timeline."

            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.InvalidTimeZone ->
                "Your device time zone could not be resolved."

            else -> "The latest session timeline data could not be refreshed."
        }
    }

    private fun SessionTimelineDataError.toUserMessage(): String {
        return when (this) {
            is SessionTimelineDataError.InvalidTimeZone ->
                "Your device time zone could not be resolved."

            else -> "Session timeline data is not available yet."
        }
    }

    private fun LocalDate.formatDateLabel(): String {
        return format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
    }

    private fun Long.toPeriodLabel(
        zoneId: ZoneId,
        selectedDate: LocalDate,
        lateNightStartHour: Int
    ): String {
        val localDateTime = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
        val hour = localDateTime.hour
        val prefix = when (localDateTime.hour) {
            in 0 until UsageAnalysisPreferences.DEFAULT_LATE_NIGHT_END_HOUR -> "After midnight"
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            else -> if (hour >= lateNightStartHour) {
                "Late night"
            } else {
                "Evening"
            }
        }
        return if (localDateTime.toLocalDate() == selectedDate) {
            prefix
        } else {
            "$prefix (${localDateTime.toLocalDate().formatDateLabel()})"
        }
    }

    companion object {
        private const val MERGE_ADJACENT_SESSION_GAP_MILLIS = 2L * 60L * 1000L
    }
}
