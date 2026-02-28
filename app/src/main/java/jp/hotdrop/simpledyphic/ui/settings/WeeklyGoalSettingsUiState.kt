package jp.hotdrop.simpledyphic.ui.settings

import androidx.annotation.StringRes
import jp.hotdrop.simpledyphic.model.HealthMetricType

data class WeeklyGoalInputUiModel(
    val metricType: HealthMetricType,
    val targetInput: String,
    val enabled: Boolean
)

data class WeeklyGoalSettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val goalInputs: List<WeeklyGoalInputUiModel> = emptyList(),
    @param:StringRes val errorMessageResId: Int? = null,
    @param:StringRes val messageResId: Int? = null
)
