package jp.hotdrop.simpledyphic.model

import java.time.LocalDate

data class DailyHealthMetrics(
    val dateId: Int,
    val stepCount: HealthMetricValue,
    val activeKcal: HealthMetricValue,
    val exerciseMinutes: HealthMetricValue,
    val distanceKm: HealthMetricValue
) {
    val date: LocalDate
        get() = DyphicId.idToDate(dateId)

    fun metricValue(metricType: HealthMetricType): HealthMetricValue {
        return when (metricType) {
            HealthMetricType.STEP_COUNT -> stepCount
            HealthMetricType.ACTIVE_KCAL -> activeKcal
            HealthMetricType.EXERCISE_MINUTES -> exerciseMinutes
            HealthMetricType.DISTANCE_KM -> distanceKm
        }
    }
}
