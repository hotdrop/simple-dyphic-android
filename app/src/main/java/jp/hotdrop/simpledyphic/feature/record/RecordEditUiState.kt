package jp.hotdrop.simpledyphic.feature.record

import java.time.LocalDate

data class RecordEditUiState(
    val recordDate: LocalDate = LocalDate.now(),
    val conditionMemo: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
