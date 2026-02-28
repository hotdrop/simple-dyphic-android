package jp.hotdrop.simpledyphic.model

data class WeeklyGoalProgress(
    val metricType: HealthMetricType,
    val targetValue: Double,
    val actualValue: Double,
    val achievementRate: Double
)

data class WeeklyGoalMetricProgress(
    val progress: WeeklyGoalProgress,
    val availability: MetricAvailability
)
