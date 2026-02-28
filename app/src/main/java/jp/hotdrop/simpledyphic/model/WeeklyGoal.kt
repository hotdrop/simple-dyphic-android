package jp.hotdrop.simpledyphic.model

data class WeeklyGoal(
    val metricType: HealthMetricType,
    val targetValue: Double,
    val weekStartsOnMonday: Boolean = true,
    val enabled: Boolean = true
)
