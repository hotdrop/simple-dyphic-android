package jp.hotdrop.simpledyphic.ui.calendar

import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.data.repository.RecordRepository
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.DyphicId
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.ui.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val recordRepository: RecordRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    private var observeRecordsJob: Job? = null

    init {
        Timber.d("CalendarViewModel initialized")
        observeRecords()
    }

    fun onRecordUpdated() {
        _uiState.update { it.copy(errorMessageResId = null) }
    }

    fun onRetry() {
        observeRecords()
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

            // Roomの Flow クエリは内部で非メイン実行されるため、ここはそのまま collect を使って問題ない（dispatcherIOは不要）
            recordRepository.observeAll().collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val recordsByDate = result.value.associateBy { record -> record.date }
                        val datesWithMarkers = result.value
                            .asSequence()
                            .filter { record -> record.hasCalendarMarker() }
                            .map { record -> record.date }
                            .toSet()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessageResId = null,
                                recordsByDate = recordsByDate,
                                datesWithMarkers = datesWithMarkers
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

    private fun Record.hasCalendarMarker(): Boolean {
        return isToilet || condition != null || (ringfitKcal ?: 0.0) > 0.0 ||
            (ringfitKm ?: 0.0) > 0.0 || (stepCount ?: 0) >= 7000
    }
}
