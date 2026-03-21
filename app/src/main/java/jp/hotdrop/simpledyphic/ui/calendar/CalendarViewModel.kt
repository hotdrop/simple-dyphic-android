package jp.hotdrop.simpledyphic.ui.calendar

import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import javax.inject.Inject
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.DyphicId
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.ui.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import timber.log.Timber

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val recordObserver: CalendarRecordObserver,
    private val weeklyGoalObserver: CalendarWeeklyGoalObserver,
    private val weeklyDataLoader: CalendarWeeklyDataLoader
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    private var observeRecordsJob: Job? = null
    private var observeGoalsJob: Job? = null
    private var weeklyDataJob: Job? = null

    init {
        Timber.d("CalendarViewModel initialized")
        observeRecords()
        observeGoals()
        refreshWeeklyData(anchorDate = _uiState.value.selectedDate, includeInsight = true)
    }

    fun onRecordUpdated() {
        _uiState.update { it.copy(errorMessageResId = null, weeklyErrorMessageResId = null) }
        refreshWeeklyData(anchorDate = _uiState.value.selectedDate, includeInsight = true)
    }

    fun onRetry() {
        observeRecords()
        observeGoals()
        refreshWeeklyData(anchorDate = _uiState.value.selectedDate, includeInsight = true)
    }

    fun onVisibleMonthChanged(yearMonth: YearMonth) {
        _uiState.update { it.copy(currentMonth = yearMonth) }
    }

    fun onDaySelected(date: LocalDate) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                currentMonth = YearMonth.from(date)
            )
        }
        refreshWeeklyData(anchorDate = date, includeInsight = true)
    }

    fun selectedDayId(): Int = DyphicId.dateToId(_uiState.value.selectedDate)

    private fun observeRecords() {
        observeRecordsJob?.cancel()
        observeRecordsJob = launch {
            val shouldShowLoading = _uiState.value.recordsByDate.isEmpty()
            if (shouldShowLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessageResId = null) }
            } else {
                _uiState.update { it.copy(errorMessageResId = null) }
            }

            recordObserver.observeAll()
                .map { result ->
                    when (result) {
                        is AppResult.Success -> AppResult.Success(buildCalendarData(result.value))
                        is AppResult.Failure -> result
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            val calendarData = result.value
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessageResId = null,
                                    recordsByDate = calendarData.recordsByDate,
                                    datesWithMarkers = calendarData.datesWithMarkers
                                )
                            }
                        }
                        is AppResult.Failure -> {
                            Timber.e(result.error, "Failed to observe records")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessageResId = R.string.calendar_error_load_records
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun observeGoals() {
        observeGoalsJob?.cancel()
        observeGoalsJob = launch {
            var skipInitialSuccessRefresh = true
            weeklyGoalObserver.observeWeeklyGoals().collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        if (skipInitialSuccessRefresh) {
                            skipInitialSuccessRefresh = false
                            return@collect
                        }
                        refreshWeeklyData(
                            anchorDate = _uiState.value.selectedDate,
                            includeInsight = false
                        )
                    }
                    is AppResult.Failure -> {
                        Timber.e(result.error, "Failed to observe weekly goals")
                        clearWeeklyDataWithError()
                    }
                }
            }
        }
    }

    private fun refreshWeeklyData(
        anchorDate: LocalDate,
        includeInsight: Boolean
    ) {
        weeklyDataJob?.cancel()
        weeklyDataJob = launch {
            _uiState.update {
                it.copy(
                    isWeeklyLoading = true,
                    weeklyErrorMessageResId = null
                )
            }

            val progressResult = dispatcherIO { weeklyDataLoader.loadGoalSummary(anchorDate) }

            when (progressResult) {
                is AppResult.Failure -> {
                    Timber.e(progressResult.error, "Failed to load weekly goal progress")
                    clearWeeklyDataWithError()
                }
                is AppResult.Success -> {
                    val progressSummary = progressResult.value
                    if (!includeInsight) {
                        _uiState.update {
                            it.copy(
                                isWeeklyLoading = false,
                                weeklyErrorMessageResId = null,
                                weeklyStartDate = progressSummary.weekRange.startDate,
                                weeklyEndDate = progressSummary.weekRange.endDate,
                                weeklyGoalProgresses = progressSummary.progresses
                            )
                        }
                        return@launch
                    }

                    val insightResult = dispatcherIO { weeklyDataLoader.loadInsight(anchorDate) }
                    if (insightResult is AppResult.Failure) {
                        Timber.e(insightResult.error, "Failed to load weekly insight")
                        clearWeeklyDataWithError()
                        return@launch
                    }

                    insightResult as AppResult.Success
                    val insight = insightResult.value
                    _uiState.update {
                        it.copy(
                            isWeeklyLoading = false,
                            weeklyErrorMessageResId = null,
                            weeklyStartDate = progressSummary.weekRange.startDate,
                            weeklyEndDate = progressSummary.weekRange.endDate,
                            weeklyGoalProgresses = progressSummary.progresses,
                            weeklyInsights = insight.metricInsights,
                            hasBadConditionDaysInWeek = insight.badConditionDates.isNotEmpty()
                        )
                    }
                }
            }
        }
    }

    private fun clearWeeklyDataWithError() {
        _uiState.update {
            it.copy(
                isWeeklyLoading = false,
                weeklyErrorMessageResId = R.string.calendar_weekly_error_load,
                weeklyStartDate = null,
                weeklyEndDate = null,
                weeklyGoalProgresses = emptyList(),
                weeklyInsights = emptyList(),
                hasBadConditionDaysInWeek = false
            )
        }
    }

    private fun buildCalendarData(records: List<Record>): CalendarData {
        val recordsByDate = LinkedHashMap<LocalDate, Record>(records.size)
        val datesWithMarkers = LinkedHashSet<LocalDate>(records.size)
        records.forEach { record ->
            val date = record.date
            recordsByDate[date] = record
            if (record.hasCalendarMarker()) {
                datesWithMarkers += date
            }
        }
        return CalendarData(
            recordsByDate = recordsByDate,
            datesWithMarkers = datesWithMarkers
        )
    }

    private fun Record.hasCalendarMarker(): Boolean {
        return isToilet || condition != null || (ringfitKcal ?: 0.0) > 0.0 ||
            (ringfitKm ?: 0.0) > 0.0 || (stepCount ?: 0) >= 7000
    }

    private data class CalendarData(
        val recordsByDate: Map<LocalDate, Record>,
        val datesWithMarkers: Set<LocalDate>
    )
}
