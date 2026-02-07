package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import jp.hotdrop.simpledyphic.domain.model.Record

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey
    val id: Int,
    val breakfast: String?,
    val lunch: String?,
    val dinner: String?,
    val isToilet: Boolean,
    val condition: String?,
    val conditionMemo: String?,
    val stepCount: Int?,
    val healthKcal: Double?,
    val ringfitKcal: Double?,
    val ringfitKm: Double?
)

fun RecordEntity.toModel(): Record {
    return Record(
        id = id,
        breakfast = breakfast,
        lunch = lunch,
        dinner = dinner,
        isToilet = isToilet,
        condition = condition,
        conditionMemo = conditionMemo,
        stepCount = stepCount,
        healthKcal = healthKcal,
        ringfitKcal = ringfitKcal,
        ringfitKm = ringfitKm
    )
}

fun Record.toEntity(): RecordEntity {
    return RecordEntity(
        id = id,
        breakfast = breakfast,
        lunch = lunch,
        dinner = dinner,
        isToilet = isToilet,
        condition = condition,
        conditionMemo = conditionMemo,
        stepCount = stepCount,
        healthKcal = healthKcal,
        ringfitKcal = ringfitKcal,
        ringfitKm = ringfitKm
    )
}
