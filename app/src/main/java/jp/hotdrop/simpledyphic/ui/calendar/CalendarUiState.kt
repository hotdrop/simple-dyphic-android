package jp.hotdrop.simpledyphic.ui.calendar

import androidx.annotation.StringRes
import java.time.LocalDate
import java.time.YearMonth
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.model.WeeklyGoalMetricProgress
import jp.hotdrop.simpledyphic.model.WeeklyMetricInsight

data class CalendarUiState(
    val isLoading: Boolean = true,
    @param:StringRes val errorMessageResId: Int? = null,
    val calendarStartMonth: YearMonth = YearMonth.now().minusYears(2),
    val calendarEndMonth: YearMonth = YearMonth.now().plusYears(2),
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val recordsByDate: Map<LocalDate, Record> = emptyMap(),
    val datesWithMarkers: Set<LocalDate> = emptySet(),
    val isWeeklyLoading: Boolean = false,
    @param:StringRes val weeklyErrorMessageResId: Int? = null,
    val weeklyStartDate: LocalDate? = null,
    val weeklyEndDate: LocalDate? = null,
    val weeklyGoalProgresses: List<WeeklyGoalMetricProgress> = emptyList(),
    val weeklyInsights: List<WeeklyMetricInsight> = emptyList(),
    val hasBadConditionDaysInWeek: Boolean = false
) {
    val selectedRecord: Record?
        get() = recordsByDate[selectedDate]
}
