package jp.hotdrop.simpledyphic.model

data class WeeklyGoalSummary(
    val weekRange: WeekRange,
    val progresses: List<WeeklyGoalMetricProgress>
)
