package jp.hotdrop.simpledyphic.ui.settings

import androidx.annotation.StringRes
import java.time.LocalDate

data class AiAdviceSettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isImportingModel: Boolean = false,
    val birthDate: LocalDate? = null,
    val heightCmInput: String = "",
    val weightKgInput: String = "",
    val advisorPrompt: String = "",
    val modelDisplayName: String? = null,
    val modelFilePath: String? = null,
    val isBirthDatePickerVisible: Boolean = false,
    @param:StringRes val errorMessageResId: Int? = null,
    @param:StringRes val messageResId: Int? = null
)

sealed interface AiAdviceSettingsEffect {
    data object OpenModelPicker : AiAdviceSettingsEffect
}
