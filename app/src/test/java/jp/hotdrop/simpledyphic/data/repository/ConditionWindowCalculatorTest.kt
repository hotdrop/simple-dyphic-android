package jp.hotdrop.simpledyphic.data.repository

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConditionWindowCalculatorTest {

    @Test
    fun resolveWindow_returnsNullWhenNoBadDays() {
        val window = ConditionWindowCalculator.resolveWindow(emptyList())

        assertNull(window)
    }

    @Test
    fun resolveWindow_expandsAcrossMonthBoundary() {
        val window = ConditionWindowCalculator.resolveWindow(
            badConditionDates = listOf(LocalDate.of(2026, 3, 1))
        )

        assertEquals(LocalDate.of(2026, 2, 27), window?.startDate)
        assertEquals(LocalDate.of(2026, 3, 3), window?.endDate)
    }

    @Test
    fun resolveWindow_usesMinAndMaxBadDates() {
        val window = ConditionWindowCalculator.resolveWindow(
            badConditionDates = listOf(
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 14)
            )
        )

        assertEquals(LocalDate.of(2026, 2, 8), window?.startDate)
        assertEquals(LocalDate.of(2026, 2, 16), window?.endDate)
    }
}
