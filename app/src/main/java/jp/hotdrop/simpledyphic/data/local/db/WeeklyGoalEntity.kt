package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.WeeklyGoal

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
        metricType = HealthMetricType.valueOf(metricType),
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
