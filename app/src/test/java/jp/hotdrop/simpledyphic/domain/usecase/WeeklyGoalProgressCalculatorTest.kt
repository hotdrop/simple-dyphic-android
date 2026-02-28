package jp.hotdrop.simpledyphic.domain.usecase

import jp.hotdrop.simpledyphic.model.DailyHealthMetrics
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.HealthMetricValue
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyGoalProgressCalculatorTest {

    @Test
    fun calculate_allowsAchievementRateOver100Percent() {
        val goals = listOf(
            WeeklyGoal(metricType = HealthMetricType.STEP_COUNT, targetValue = 70000.0)
        )
        val weekMetrics = listOf(
            testDailyMetrics(step = HealthMetricValue(MetricAvailability.AVAILABLE, 80000.0))
        )

        val result = WeeklyGoalProgressCalculator.calculate(goals, weekMetrics)

        assertEquals(1, result.size)
        assertEquals(80000.0, result[0].progress.actualValue, 0.0001)
        assertEquals(114.2857, result[0].progress.achievementRate, 0.01)
        assertEquals(MetricAvailability.AVAILABLE, result[0].availability)
    }

    @Test
    fun calculate_marksPermissionMissingWhenMetricIsNotGranted() {
        val goals = listOf(
            WeeklyGoal(metricType = HealthMetricType.DISTANCE_KM, targetValue = 21.0)
        )
        val weekMetrics = listOf(
            testDailyMetrics(distance = HealthMetricValue(MetricAvailability.PERMISSION_MISSING, null))
        )

        val result = WeeklyGoalProgressCalculator.calculate(goals, weekMetrics)

        assertEquals(1, result.size)
        assertEquals(0.0, result[0].progress.actualValue, 0.0001)
        assertEquals(MetricAvailability.PERMISSION_MISSING, result[0].availability)
    }

    private fun testDailyMetrics(
        step: HealthMetricValue = HealthMetricValue(MetricAvailability.SOURCE_UNAVAILABLE, null),
        activeKcal: HealthMetricValue = HealthMetricValue(MetricAvailability.SOURCE_UNAVAILABLE, null),
        exerciseMinutes: HealthMetricValue = HealthMetricValue(MetricAvailability.SOURCE_UNAVAILABLE, null),
        distance: HealthMetricValue = HealthMetricValue(MetricAvailability.SOURCE_UNAVAILABLE, null),
        floors: HealthMetricValue = HealthMetricValue(MetricAvailability.SOURCE_UNAVAILABLE, null)
    ): DailyHealthMetrics {
        return DailyHealthMetrics(
            dateId = 20260223,
            stepCount = step,
            activeKcal = activeKcal,
            exerciseMinutes = exerciseMinutes,
            distanceKm = distance,
            floorsClimbed = floors
        )
    }
}
