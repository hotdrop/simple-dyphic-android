package jp.hotdrop.simpledyphic.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class AdvicePeriod {
    WEEKLY,
    MONTHLY,
    THREE_MONTHS;

    fun resolveRange(today: LocalDate): AdviceRange {
        val startDate = when (this) {
            WEEKLY -> today.with(java.time.DayOfWeek.MONDAY)
            MONTHLY -> today.withDayOfMonth(1)
            THREE_MONTHS -> today.minusMonths(2).withDayOfMonth(1)
        }
        return AdviceRange(
            startDate = startDate,
            endDate = today,
            elapsedDays = ChronoUnit.DAYS.between(startDate, today) + 1
        )
    }
}

data class AdviceRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val elapsedDays: Long
)
