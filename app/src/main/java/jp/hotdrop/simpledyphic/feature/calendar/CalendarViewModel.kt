package jp.hotdrop.simpledyphic.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import jp.hotdrop.simpledyphic.core.log.AppLogger
import jp.hotdrop.simpledyphic.domain.model.DyphicId
import jp.hotdrop.simpledyphic.domain.repository.RecordRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val appLogger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        appLogger.i("CalendarViewModel initialized")
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
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }
            runCatching {
                recordRepository.findAll()
            }.onSuccess { records ->
                val recordsByDate = records.associateBy { record -> record.date }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = null,
                        recordsByDate = recordsByDate
                    )
                }
            }.onFailure { error ->
                appLogger.e("Failed to load records", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load records"
                    )
                }
            }
        }
    }
}
