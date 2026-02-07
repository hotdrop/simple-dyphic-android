package jp.hotdrop.simpledyphic.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.hotdrop.simpledyphic.core.log.AppLogger
import jp.hotdrop.simpledyphic.domain.model.DyphicId
import jp.hotdrop.simpledyphic.domain.model.Record
import jp.hotdrop.simpledyphic.domain.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecordEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordRepository: RecordRepository,
    private val appLogger: AppLogger
) : ViewModel() {

    private val recordId: Int = checkNotNull(savedStateHandle[RECORD_ID_ARG])
    private var baseRecord: Record = Record.createEmpty(DyphicId.idToDate(recordId))

    private val _uiState = MutableStateFlow(
        RecordEditUiState(
            recordDate = DyphicId.idToDate(recordId)
        )
    )
    val uiState: StateFlow<RecordEditUiState> = _uiState.asStateFlow()

    init {
        loadRecord()
    }

    fun onBreakfastChanged(value: String) {
        updateInput { it.copy(breakfast = value, errorMessage = null) }
    }

    fun onLunchChanged(value: String) {
        updateInput { it.copy(lunch = value, errorMessage = null) }
    }

    fun onDinnerChanged(value: String) {
        updateInput { it.copy(dinner = value, errorMessage = null) }
    }

    fun onConditionTypeChanged(value: ConditionType) {
        updateInput { it.copy(conditionType = value, errorMessage = null) }
    }

    fun onConditionMemoChanged(value: String) {
        updateInput { it.copy(conditionMemo = value, errorMessage = null) }
    }

    fun onIsToiletChanged(value: Boolean) {
        updateInput { it.copy(isToilet = value, errorMessage = null) }
    }

    fun onRingfitKcalChanged(value: String) {
        updateInput { it.copy(ringfitKcalInput = value, errorMessage = null) }
    }

    fun onRingfitKmChanged(value: String) {
        updateInput { it.copy(ringfitKmInput = value, errorMessage = null) }
    }

    fun onBackRequested(onClose: () -> Unit) {
        if (_uiState.value.hasChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            onClose()
        }
    }

    fun dismissDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    fun confirmDiscardAndClose(onClose: () -> Unit) {
        _uiState.update { it.copy(showDiscardDialog = false) }
        onClose()
    }

    fun save(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val kcalInput = parseNumberInput(_uiState.value.ringfitKcalInput)
            if (kcalInput is ParsedNumber.Invalid) {
                _uiState.update {
                    it.copy(errorMessage = RINGFIT_KCAL_ERROR_MESSAGE)
                }
                return@launch
            }
            val kmInput = parseNumberInput(_uiState.value.ringfitKmInput)
            if (kmInput is ParsedNumber.Invalid) {
                _uiState.update {
                    it.copy(errorMessage = RINGFIT_KM_ERROR_MESSAGE)
                }
                return@launch
            }
            val kcal = (kcalInput as ParsedNumber.Valid).value
            val km = (kmInput as ParsedNumber.Valid).value

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val target = baseRecord.copy(
                breakfast = _uiState.value.breakfast.ifBlank { null },
                lunch = _uiState.value.lunch.ifBlank { null },
                dinner = _uiState.value.dinner.ifBlank { null },
                condition = _uiState.value.conditionType?.rawValue,
                conditionMemo = _uiState.value.conditionMemo.ifBlank { null },
                isToilet = _uiState.value.isToilet,
                ringfitKcal = kcal,
                ringfitKm = km
            )
            if (target == baseRecord) {
                _uiState.update { it.copy(isSaving = false, errorMessage = null, hasChanges = false) }
                onComplete(false)
                return@launch
            }

            runCatching {
                recordRepository.save(target)
            }.onSuccess {
                baseRecord = target
                _uiState.update { it.copy(isSaving = false, errorMessage = null, hasChanges = false) }
                onComplete(true)
            }.onFailure { error ->
                appLogger.e("Failed to save record", error)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to save record"
                    )
                }
            }
        }
    }

    private fun loadRecord() {
        viewModelScope.launch {
            runCatching {
                recordRepository.find(recordId)
            }.onSuccess { record ->
                baseRecord = record
                _uiState.update {
                    it.copy(
                        recordDate = record.date,
                        breakfast = record.breakfast.orEmpty(),
                        lunch = record.lunch.orEmpty(),
                        dinner = record.dinner.orEmpty(),
                        conditionType = ConditionType.fromRawValue(record.condition),
                        conditionMemo = record.conditionMemo.orEmpty(),
                        isToilet = record.isToilet,
                        ringfitKcalInput = record.ringfitKcal?.toString().orEmpty(),
                        ringfitKmInput = record.ringfitKm?.toString().orEmpty(),
                        hasChanges = false
                    )
                }
            }.onFailure { error ->
                if (error is NoSuchElementException) {
                    baseRecord = Record.createEmpty(DyphicId.idToDate(recordId))
                    _uiState.update {
                        it.copy(
                            recordDate = baseRecord.date,
                            breakfast = "",
                            lunch = "",
                            dinner = "",
                            conditionType = null,
                            conditionMemo = "",
                            isToilet = false,
                            ringfitKcalInput = "",
                            ringfitKmInput = "",
                            hasChanges = false
                        )
                    }
                } else {
                    appLogger.e("Failed to load record", error)
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to load record")
                    }
                }
            }
        }
    }

    private fun updateInput(transform: (RecordEditUiState) -> RecordEditUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            updated.copy(hasChanges = hasChangesFromState(updated))
        }
    }

    private fun hasChangesFromState(uiState: RecordEditUiState): Boolean {
        val mealsChanged =
            baseRecord.breakfast.orEmpty() != uiState.breakfast ||
                baseRecord.lunch.orEmpty() != uiState.lunch ||
                baseRecord.dinner.orEmpty() != uiState.dinner
        val conditionChanged =
            ConditionType.fromRawValue(baseRecord.condition) != uiState.conditionType ||
                baseRecord.conditionMemo.orEmpty() != uiState.conditionMemo
        val isToiletChanged = baseRecord.isToilet != uiState.isToilet
        val kcalChanged = when (val parsed = parseNumberInput(uiState.ringfitKcalInput)) {
            is ParsedNumber.Invalid -> true
            is ParsedNumber.Valid -> parsed.value != baseRecord.ringfitKcal
        }
        val kmChanged = when (val parsed = parseNumberInput(uiState.ringfitKmInput)) {
            is ParsedNumber.Invalid -> true
            is ParsedNumber.Valid -> parsed.value != baseRecord.ringfitKm
        }

        return mealsChanged || conditionChanged || isToiletChanged || kcalChanged || kmChanged
    }

    private fun parseNumberInput(value: String): ParsedNumber {
        if (value.isBlank()) {
            return ParsedNumber.Valid(null)
        }
        return value.toDoubleOrNull()?.let { ParsedNumber.Valid(it) } ?: ParsedNumber.Invalid
    }

    private sealed interface ParsedNumber {
        data class Valid(val value: Double?) : ParsedNumber
        data object Invalid : ParsedNumber
    }

    companion object {
        const val RECORD_ID_ARG: String = "recordId"
        const val RESULT_UPDATED_ARG: String = "recordUpdated"

        private const val RINGFIT_KCAL_ERROR_MESSAGE: String = "RingFit kcal must be a number."
        private const val RINGFIT_KM_ERROR_MESSAGE: String = "RingFit km must be a number."
    }
}
