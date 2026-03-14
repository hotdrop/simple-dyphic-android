package jp.hotdrop.simpledyphic.data.local.db

import jp.hotdrop.simpledyphic.model.HealthMetricType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyGoalEntityTest {

    @Test
    fun toModelOrNull_returnsNull_forUnknownMetricType() {
        val entity = WeeklyGoalEntity(
            metricType = "FLOORS_CLIMBED",
            targetValue = 35.0,
            weekStartsOnMonday = true,
            enabled = true
        )

        val result = entity.toModelOrNull()

        assertNull(result)
    }

    @Test
    fun toModelOrNull_returnsModel_forKnownMetricType() {
        val entity = WeeklyGoalEntity(
            metricType = "STEP_COUNT",
            targetValue = 70_000.0,
            weekStartsOnMonday = true,
            enabled = true
        )

        val result = entity.toModelOrNull()

        requireNotNull(result)
        assertEquals(HealthMetricType.STEP_COUNT, result.metricType)
        assertEquals(70_000.0, result.targetValue, 0.0)
    }
}
