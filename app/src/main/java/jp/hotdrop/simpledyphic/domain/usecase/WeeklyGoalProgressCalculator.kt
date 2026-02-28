package jp.hotdrop.simpledyphic.domain.usecase

import jp.hotdrop.simpledyphic.model.DailyHealthMetrics
import jp.hotdrop.simpledyphic.model.HealthMetricValue
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import jp.hotdrop.simpledyphic.model.WeeklyGoalMetricProgress
import jp.hotdrop.simpledyphic.model.WeeklyGoalProgress

internal object WeeklyGoalProgressCalculator {
    fun calculate(
        goals: List<WeeklyGoal>,
        weekMetrics: List<DailyHealthMetrics>
    ): List<WeeklyGoalMetricProgress> {
        return goals.filter { it.enabled }.map { goal ->
            val metricValues = weekMetrics.map { it.metricValue(goal.metricType) }
            val availability = resolveAvailability(metricValues)
            val actualValue = metricValues.sumOf { value ->
                if (value.availability == MetricAvailability.AVAILABLE) value.value ?: 0.0 else 0.0
            }
            val achievementRate = if (goal.targetValue <= 0.0) {
                0.0
            } else {
                (actualValue / goal.targetValue) * 100.0
            }

            WeeklyGoalMetricProgress(
                progress = WeeklyGoalProgress(
                    metricType = goal.metricType,
                    targetValue = goal.targetValue,
                    actualValue = actualValue,
                    achievementRate = achievementRate
                ),
                availability = availability
            )
        }
    }

    private fun resolveAvailability(metricValues: List<HealthMetricValue>): MetricAvailability {
        return when {
            metricValues.any { it.availability == MetricAvailability.AVAILABLE } -> MetricAvailability.AVAILABLE
            metricValues.any { it.availability == MetricAvailability.PERMISSION_MISSING } -> MetricAvailability.PERMISSION_MISSING
            else -> MetricAvailability.SOURCE_UNAVAILABLE
        }
    }
}
