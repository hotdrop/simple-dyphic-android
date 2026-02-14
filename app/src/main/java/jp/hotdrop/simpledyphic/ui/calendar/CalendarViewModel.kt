package jp.hotdrop.simpledyphic.ui.calendar

import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.data.repository.RecordRepository
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.DyphicId
import jp.hotdrop.simpledyphic.ui.BaseViewModel
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

    init {
        Timber.d("CalendarViewModel initialized")
        reloadRecords(showLoading = true)
    }

    fun onRecordUpdated() {
        reloadRecords(showLoading = false)
    }

    fun onRetry() {
        reloadRecords(showLoading = true)
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

    private fun reloadRecords(showLoading: Boolean) {
        launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessageResId = null) }
            } else {
                _uiState.update { it.copy(errorMessageResId = null) }
            }

            when (val result = dispatcherIO { recordRepository.findAll() }) {
                is AppResult.Success -> {
                    val recordsByDate = result.value.associateBy { record -> record.date }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessageResId = null,
                            recordsByDate = recordsByDate
                        )
                    }
                }
                is AppResult.Failure -> {
                    Timber.e(result.error, "Failed to load records")
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
