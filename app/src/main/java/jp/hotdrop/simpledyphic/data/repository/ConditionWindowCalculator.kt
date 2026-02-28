package jp.hotdrop.simpledyphic.data.repository

import java.time.LocalDate
import jp.hotdrop.simpledyphic.model.WeekRange

internal object ConditionWindowCalculator {
    fun resolveWindow(
        badConditionDates: List<LocalDate>,
        beforeDays: Long = 2,
        afterDays: Long = 2
    ): WeekRange? {
        if (badConditionDates.isEmpty()) {
            return null
        }
        val start = badConditionDates.min().minusDays(beforeDays)
        val end = badConditionDates.max().plusDays(afterDays)
        return WeekRange(startDate = start, endDate = end)
    }
}
