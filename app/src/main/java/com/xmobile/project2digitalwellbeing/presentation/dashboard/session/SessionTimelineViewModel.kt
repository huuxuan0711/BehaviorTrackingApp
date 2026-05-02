package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.SessionTimelineDataError
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
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

    fun load(forceRefresh: Boolean, weekStartDate: LocalDate = _uiState.value.weekStartDate) {
        val timezoneId = ZoneId.systemDefault().id
        val zoneId = ZoneId.of(timezoneId)
        val currentWeekStart = LocalDate.now(zoneId)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val normalizedWeekStart = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val anchorDate = normalizedWeekStart.plusDays(6)
        val nowMillis = anchorDate.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
        val shouldRefreshCurrentWeek = normalizedWeekStart == currentWeekStart

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    weekStartDate = normalizedWeekStart,
                    errorMessage = null
                )
            }

            val refreshError = if (shouldRefreshCurrentWeek && (forceRefresh || !hasLoadedData)) {
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
                    val sessions = outcome.data.sessions
                    val maxDurationMillis = sessions.maxOfOrNull { it.session.durationMillis } ?: 0L

                    _uiState.value = SessionTimelineUiState(
                        weekStartDate = normalizedWeekStart,
                        dateRangeLabel = normalizedWeekStart.toDateRangeLabel(normalizedWeekStart.plusDays(6)),
                        insightText = refreshError ?: outcome.data.insight?.summary
                        ?: "No session insight available yet.",
                        sessions = sessions.mapIndexed { index, session ->
                            session.toTimelineItem(
                                previousSession = sessions.getOrNull(index - 1),
                                maxDurationMillis = maxDurationMillis,
                                zoneId = zoneId
                            )
                        },
                        errorMessage = refreshError,
                        canNavigateNext = normalizedWeekStart.isBefore(currentWeekStart)
                    )
                }

                is GetSessionTimelineDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            dateRangeLabel = normalizedWeekStart.toDateRangeLabel(normalizedWeekStart.plusDays(6)),
                            insightText = refreshError ?: outcome.error.toUserMessage(),
                            errorMessage = refreshError ?: outcome.error.toUserMessage(),
                            canNavigateNext = normalizedWeekStart.isBefore(currentWeekStart)
                        )
                    }
                }
            }
        }
    }

    fun showPreviousWeek() {
        load(forceRefresh = false, weekStartDate = _uiState.value.weekStartDate.minusWeeks(1))
    }

    fun showNextWeek() {
        if (_uiState.value.canNavigateNext) {
            load(forceRefresh = false, weekStartDate = _uiState.value.weekStartDate.plusWeeks(1))
        }
    }

    private fun EnrichedSession.toTimelineItem(
        previousSession: EnrichedSession?,
        maxDurationMillis: Long,
        zoneId: ZoneId
    ): SessionTimelineItemUiModel {
        return SessionTimelineItemUiModel(
            packageName = session.packageName,
            appName = appName ?: session.packageName,
            timeRangeText = session.toTimeRangeText(zoneId),
            durationText = session.durationMillis.toDurationVerboseText(),
            progressRatio = if (maxDurationMillis <= 0L) 0f else {
                session.durationMillis.toFloat() / maxDurationMillis.toFloat()
            },
            transitionLabel = previousSession
                ?.takeIf { it.session.packageName != session.packageName }
                ?.let { "${it.appName ?: it.session.packageName} -> ${appName ?: session.packageName}" },
            category = category
        )
    }

    private fun com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession.toTimeRangeText(
        zoneId: ZoneId
    ): String {
        val formatter = DateTimeFormatter.ofPattern("EEE, HH:mm", Locale.getDefault())
        val start = Instant.ofEpochMilli(startTimeMillis).atZone(zoneId)
        val end = Instant.ofEpochMilli(endTimeMillis).atZone(zoneId)
        return "${start.format(formatter)} - ${end.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))}"
    }

    private fun Long.toDurationVerboseText(): String {
        val totalMinutes = this / (60L * 1000L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
            hours > 0L -> "${hours}h"
            else -> "$totalMinutes minutes"
        }
    }

    private fun LocalDate.toDateRangeLabel(endDate: LocalDate): String {
        val sameYear = year == endDate.year
        val startFormatter = DateTimeFormatter.ofPattern(
            if (sameYear) "MMM d" else "MMM d, yyyy",
            Locale.getDefault()
        )
        val endFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        return "${format(startFormatter)} - ${endDate.format(endFormatter)}"
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
}
