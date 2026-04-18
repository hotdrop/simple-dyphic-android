package jp.hotdrop.simpledyphic.ui.ai

import androidx.annotation.StringRes
import jp.hotdrop.simpledyphic.model.AdvicePeriod
import jp.hotdrop.simpledyphic.model.ExerciseAdviceInput

data class ExerciseAdviceUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: AdvicePeriod = AdvicePeriod.WEEKLY,
    val input: ExerciseAdviceInput? = null,
    val isInitializingModel: Boolean = false,
    val isGenerating: Boolean = false,
    val adviceText: String = "",
    @param:StringRes val errorMessageResId: Int? = null
) {
    val canGenerate: Boolean
        get() = input?.canGenerate == true && !isLoading && !isGenerating
}

sealed interface ExerciseAdviceEffect {
    data class RequestHealthPermissions(
        val permissions: Set<String>
    ) : ExerciseAdviceEffect
}
