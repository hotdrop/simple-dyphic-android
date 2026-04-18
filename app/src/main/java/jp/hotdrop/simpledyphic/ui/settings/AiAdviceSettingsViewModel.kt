package jp.hotdrop.simpledyphic.ui.settings

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.data.repository.AppSettingsRepository
import jp.hotdrop.simpledyphic.model.AppCompletable
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.AppSettings
import jp.hotdrop.simpledyphic.ui.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

@HiltViewModel
class AiAdviceSettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AiAdviceSettingsUiState())
    val uiState: StateFlow<AiAdviceSettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AiAdviceSettingsEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<AiAdviceSettingsEffect> = _effects.receiveAsFlow()

    init {
        loadSettings()
    }

    fun onBirthDatePickerOpen() {
        _uiState.update { it.copy(isBirthDatePickerVisible = true, errorMessageResId = null) }
    }

    fun onBirthDatePickerDismiss() {
        _uiState.update { it.copy(isBirthDatePickerVisible = false) }
    }

    fun onBirthDateSelected(date: LocalDate) {
        _uiState.update {
            it.copy(
                birthDate = date,
                isBirthDatePickerVisible = false,
                errorMessageResId = null,
                messageResId = null
            )
        }
    }

    fun onHeightChanged(value: String) {
        _uiState.update { it.copy(heightCmInput = value, errorMessageResId = null, messageResId = null) }
    }

    fun onWeightChanged(value: String) {
        _uiState.update { it.copy(weightKgInput = value, errorMessageResId = null, messageResId = null) }
    }

    fun onPromptChanged(value: String) {
        _uiState.update { it.copy(advisorPrompt = value, errorMessageResId = null, messageResId = null) }
    }

    fun onPickModelClick() {
        if (_uiState.value.isSaving || _uiState.value.isImportingModel) return
        launch {
            _effects.send(AiAdviceSettingsEffect.OpenModelPicker)
        }
    }

    fun onModelSelected(uri: Uri) {
        if (_uiState.value.isSaving || _uiState.value.isImportingModel) return
        _uiState.update {
            it.copy(
                isImportingModel = true,
                errorMessageResId = null,
                messageResId = R.string.ai_settings_model_importing
            )
        }
        launch {
            when (val result = dispatcherIO { appSettingsRepository.importModelFile(uri) }) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isImportingModel = false,
                            modelDisplayName = result.value.displayName,
                            modelFilePath = result.value.absolutePath,
                            errorMessageResId = null,
                            messageResId = R.string.ai_settings_model_selected
                        )
                    }
                }
                is AppResult.Failure -> {
                    Timber.e(result.error, "Failed to import LiteRT-LM model file")
                    _uiState.update {
                        it.copy(
                            isImportingModel = false,
                            errorMessageResId = R.string.ai_settings_error_model_import_failed,
                            messageResId = null
                        )
                    }
                }
            }
        }
    }

    fun onSaveClick() {
        if (_uiState.value.isSaving || _uiState.value.isImportingModel) return
        val heightValue = parseOptionalPositiveDouble(_uiState.value.heightCmInput)
        if (heightValue is ParsedNumber.Invalid) {
            _uiState.update {
                it.copy(
                    errorMessageResId = R.string.ai_settings_error_invalid_height,
                    messageResId = null
                )
            }
            return
        }
        val weightValue = parseOptionalPositiveDouble(_uiState.value.weightKgInput)
        if (weightValue is ParsedNumber.Invalid) {
            _uiState.update {
                it.copy(
                    errorMessageResId = R.string.ai_settings_error_invalid_weight,
                    messageResId = null
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessageResId = null, messageResId = null) }
        launch {
            val settings = AppSettings(
                birthDate = _uiState.value.birthDate,
                heightCm = (heightValue as ParsedNumber.Valid).value,
                weightKg = (weightValue as ParsedNumber.Valid).value,
                advisorPrompt = _uiState.value.advisorPrompt.ifBlank { AppSettings.DEFAULT_ADVISOR_PROMPT },
                modelFilePath = _uiState.value.modelFilePath,
                modelDisplayName = _uiState.value.modelDisplayName
            )
            when (val result = dispatcherIO { appSettingsRepository.save(settings) }) {
                AppCompletable.Complete -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isImportingModel = false,
                            messageResId = R.string.ai_settings_message_saved
                        )
                    }
                }
                is AppCompletable.Failure -> {
                    Timber.e(result.error, "Failed to save AI advice settings")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isImportingModel = false,
                            errorMessageResId = R.string.ai_settings_error_save_failed
                        )
                    }
                }
            }
        }
    }

    private fun loadSettings() {
        _uiState.update { it.copy(isLoading = true, errorMessageResId = null, messageResId = null) }
        launch {
            when (val result = dispatcherIO { appSettingsRepository.get() }) {
                is AppResult.Success -> {
                    val settings = result.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isImportingModel = false,
                            birthDate = settings.birthDate,
                            heightCmInput = settings.heightCm?.toString().orEmpty(),
                            weightKgInput = settings.weightKg?.toString().orEmpty(),
                            advisorPrompt = settings.advisorPrompt,
                            modelDisplayName = settings.modelDisplayName,
                            modelFilePath = settings.modelFilePath,
                            errorMessageResId = null,
                            messageResId = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    Timber.e(result.error, "Failed to load AI advice settings")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isImportingModel = false,
                            errorMessageResId = R.string.ai_settings_error_load_failed
                        )
                    }
                }
            }
        }
    }

    private fun parseOptionalPositiveDouble(input: String): ParsedNumber {
        if (input.isBlank()) {
            return ParsedNumber.Valid(null)
        }
        val parsed = input.toDoubleOrNull() ?: return ParsedNumber.Invalid
        return if (parsed > 0.0) {
            ParsedNumber.Valid(parsed)
        } else {
            ParsedNumber.Invalid
        }
    }

    private sealed interface ParsedNumber {
        data class Valid(val value: Double?) : ParsedNumber
        data object Invalid : ParsedNumber
    }
}
