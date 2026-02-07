package jp.hotdrop.simpledyphic.domain.model

import java.time.LocalDate

data class Record(
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
) {
    val date: LocalDate
        get() = DyphicId.idToDate(id)

    companion object {
        fun createEmpty(date: LocalDate): Record {
            return Record(
                id = DyphicId.dateToId(date),
                breakfast = null,
                lunch = null,
                dinner = null,
                isToilet = false,
                condition = null,
                conditionMemo = null,
                stepCount = null,
                healthKcal = null,
                ringfitKcal = null,
                ringfitKm = null
            )
        }
    }
}
