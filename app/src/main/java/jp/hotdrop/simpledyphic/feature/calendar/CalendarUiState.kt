package jp.hotdrop.simpledyphic.feature.calendar

import java.time.LocalDate
import java.time.YearMonth
import jp.hotdrop.simpledyphic.domain.model.Record

data class CalendarUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val calendarStartMonth: YearMonth = YearMonth.now().minusYears(2),
    val calendarEndMonth: YearMonth = YearMonth.now().plusYears(2),
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val recordsByDate: Map<LocalDate, Record> = emptyMap()
) {
    val selectedRecord: Record?
        get() = recordsByDate[selectedDate]
}
