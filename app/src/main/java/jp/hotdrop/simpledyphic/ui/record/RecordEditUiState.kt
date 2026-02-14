package jp.hotdrop.simpledyphic.ui.record

import androidx.annotation.StringRes
import java.time.LocalDate
import jp.hotdrop.simpledyphic.model.ConditionType

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
    val isSaving: Boolean = false,
    val isHealthSyncing: Boolean = false,
    @param:StringRes val healthConnectMessageResId: Int? = null,
    @param:StringRes val errorMessageResId: Int? = null
)
