package jp.hotdrop.simpledyphic.feature.calendar

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.hotdrop.simpledyphic.core.log.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val appLogger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        appLogger.i("CalendarViewModel initialized")
    }

    fun onRetry() {
        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
    }
}
