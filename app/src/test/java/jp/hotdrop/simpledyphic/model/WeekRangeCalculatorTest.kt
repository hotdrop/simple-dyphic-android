package jp.hotdrop.simpledyphic.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekRangeCalculatorTest {

    @Test
    fun mondayStart_returnsMondayToSundayRange() {
        val range = WeekRangeCalculator.mondayStart(LocalDate.of(2026, 2, 28)) // Saturday

        assertEquals(LocalDate.of(2026, 2, 23), range.startDate)
        assertEquals(LocalDate.of(2026, 3, 1), range.endDate)
    }

    @Test
    fun mondayStart_handlesYearBoundary() {
        val range = WeekRangeCalculator.mondayStart(LocalDate.of(2026, 1, 1))

        assertEquals(LocalDate.of(2025, 12, 29), range.startDate)
        assertEquals(LocalDate.of(2026, 1, 4), range.endDate)
    }

    @Test
    fun previousWeek_returnsPriorMondayToSunday() {
        val current = WeekRangeCalculator.mondayStart(LocalDate.of(2026, 2, 28))
        val previous = current.previousWeek()

        assertEquals(LocalDate.of(2026, 2, 16), previous.startDate)
        assertEquals(LocalDate.of(2026, 2, 22), previous.endDate)
    }
}
