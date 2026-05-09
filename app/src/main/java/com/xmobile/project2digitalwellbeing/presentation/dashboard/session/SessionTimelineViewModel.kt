package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.SessionTimelineDataError
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
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
    application: Application,
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase,
    private val getSessionTimelineDataUseCase: GetSessionTimelineDataUseCase,
    private val timelineUiMapper: SessionTimelineUiMapper
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

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
                    val timelineItems = timelineUiMapper.toTimelineItems(
                        context = context,
                        sessions = outcome.data.sessions,
                        zoneId = zoneId,
                        selectedDate = normalizedDate,
                        lateNightStartHour = outcome.data.lateNightStartHour
                    )

                    _uiState.value = SessionTimelineUiState(
                        selectedDate = normalizedDate,
                        dateRangeLabel = timelineUiMapper.toDateLabel(normalizedDate),
                        insightText = refreshError ?: outcome.data.insight?.summary?.takeIf { it.isNotBlank() }
                        ?: "No session insight yet. Meaningful patterns will appear after more usage is recorded.",
                        sessions = timelineItems,
                        errorMessage = refreshError,
                        canNavigateNext = normalizedDate.isBefore(today)
                    )
                }

                is GetSessionTimelineDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            selectedDate = normalizedDate,
                            dateRangeLabel = timelineUiMapper.toDateLabel(normalizedDate),
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
