package jp.hotdrop.simpledyphic.model

import java.time.DayOfWeek
import java.time.LocalDate

data class WeekRange(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    init {
        require(!startDate.isAfter(endDate)) { "startDate must be on/before endDate" }
    }

    fun previousWeek(): WeekRange {
        return WeekRange(
            startDate = startDate.minusDays(7),
            endDate = endDate.minusDays(7)
        )
    }
}

object WeekRangeCalculator {
    fun mondayStart(anchorDate: LocalDate): WeekRange {
        val diff = (anchorDate.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val start = anchorDate.minusDays(diff.toLong())
        return WeekRange(
            startDate = start,
            endDate = start.plusDays(6)
        )
    }
}
