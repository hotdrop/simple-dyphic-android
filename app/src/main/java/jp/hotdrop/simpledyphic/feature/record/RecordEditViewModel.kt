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

    fun onConditionMemoChanged(value: String) {
        _uiState.update { it.copy(conditionMemo = value, errorMessage = null) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val target = baseRecord.copy(conditionMemo = _uiState.value.conditionMemo.ifBlank { null })
            runCatching {
                recordRepository.save(target)
            }.onSuccess {
                baseRecord = target
                _uiState.update { it.copy(isSaving = false, errorMessage = null) }
                onComplete()
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
                        conditionMemo = record.conditionMemo.orEmpty()
                    )
                }
            }.onFailure { error ->
                if (error is NoSuchElementException) {
                    baseRecord = Record.createEmpty(DyphicId.idToDate(recordId))
                    _uiState.update {
                        it.copy(
                            recordDate = baseRecord.date,
                            conditionMemo = ""
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

    companion object {
        const val RECORD_ID_ARG: String = "recordId"
    }
}
