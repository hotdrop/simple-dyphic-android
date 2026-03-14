package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import timber.log.Timber

@Entity(tableName = "weekly_goals")
data class WeeklyGoalEntity(
    @PrimaryKey
    val metricType: String,
    val targetValue: Double,
    val weekStartsOnMonday: Boolean,
    val enabled: Boolean
)

fun WeeklyGoalEntity.toModel(): WeeklyGoal {
    return WeeklyGoal(
        metricType = requireNotNull(metricTypeOrNull()) { "Unknown HealthMetricType: $metricType" },
        targetValue = targetValue,
        weekStartsOnMonday = weekStartsOnMonday,
        enabled = enabled
    )
}

fun WeeklyGoalEntity.toModelOrNull(): WeeklyGoal? {
    val parsedMetricType = metricTypeOrNull()
    if (parsedMetricType == null) {
        Timber.w("Skipping weekly goal with unknown metricType: %s", metricType)
        return null
    }

    return WeeklyGoal(
        metricType = parsedMetricType,
        targetValue = targetValue,
        weekStartsOnMonday = weekStartsOnMonday,
        enabled = enabled
    )
}

fun WeeklyGoal.toEntity(): WeeklyGoalEntity {
    return WeeklyGoalEntity(
        metricType = metricType.name,
        targetValue = targetValue,
        weekStartsOnMonday = weekStartsOnMonday,
        enabled = enabled
    )
}

private fun WeeklyGoalEntity.metricTypeOrNull(): HealthMetricType? {
    return runCatching { HealthMetricType.valueOf(metricType) }.getOrNull()
}
