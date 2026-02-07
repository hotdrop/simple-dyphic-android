package jp.hotdrop.simpledyphic.feature.record

import java.time.LocalDate

enum class ConditionType(val rawValue: String) {
    BAD("悪い"),
    NORMAL("普通"),
    GOOD("良い");

    companion object {
        fun fromRawValue(value: String?): ConditionType? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

data class RecordEditUiState(
    val recordDate: LocalDate = LocalDate.now(),
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val conditionType: ConditionType? = null,
    val conditionMemo: String = "",
    val isToilet: Boolean = false,
    val stepCount: Int? = null,
    val healthKcal: Double? = null,
    val ringfitKcalInput: String = "",
    val ringfitKmInput: String = "",
    val hasChanges: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showHealthOverwriteDialog: Boolean = false,
    val isSaving: Boolean = false,
    val isHealthSyncing: Boolean = false,
    val healthConnectMessage: String? = null,
    val errorMessage: String? = null
)
