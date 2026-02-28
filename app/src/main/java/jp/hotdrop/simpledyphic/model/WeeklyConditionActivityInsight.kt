package jp.hotdrop.simpledyphic.model

import java.time.LocalDate

data class WeeklyMetricInsight(
    val metricType: HealthMetricType,
    val deltaFromPreviousWeek: Double?,
    val deltaFromWeekAverage: Double?,
    val comment: String
)

data class WeeklyConditionActivityInsight(
    val weekRange: WeekRange,
    val badConditionDates: List<LocalDate>,
    val metricInsights: List<WeeklyMetricInsight>
)
