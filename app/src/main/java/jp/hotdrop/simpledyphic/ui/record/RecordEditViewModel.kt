package jp.hotdrop.simpledyphic.ui.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.hotdrop.simpledyphic.data.repository.HealthConnectRepository
import jp.hotdrop.simpledyphic.data.repository.RecordRepository
import jp.hotdrop.simpledyphic.model.ConditionType
import jp.hotdrop.simpledyphic.model.DailyHealthSummary
import jp.hotdrop.simpledyphic.model.DyphicId
import jp.hotdrop.simpledyphic.model.HealthConnectStatus
import jp.hotdrop.simpledyphic.model.Record
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class RecordEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordRepository: RecordRepository,
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val recordId: Int = parseRecordId(savedStateHandle)
    private var baseRecord: Record = Record.createEmpty(DyphicId.idToDate(recordId))
    private var pendingHealthSummary: DailyHealthSummary? = null
    private var hasRequestedInitialHealthSync: Boolean = false

    private val _uiState = MutableStateFlow(
        RecordEditUiState(
            recordDate = DyphicId.idToDate(recordId)
        )
    )
    val uiState: StateFlow<RecordEditUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<RecordEditEffect>()
    val effects: SharedFlow<RecordEditEffect> = _effects.asSharedFlow()
    private val loadRecordJob: Job = loadRecord()

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

    fun onScreenEntered() {
        if (hasRequestedInitialHealthSync) {
            return
        }
        hasRequestedInitialHealthSync = true
        viewModelScope.launch {
            loadRecordJob.join()
            onHealthSyncRequested()
        }
    }

    fun onHealthSyncRequested() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isHealthSyncing = true,
                    healthConnectMessage = null,
                    errorMessage = null
                )
            }

            when (healthConnectRepository.getStatus()) {
                HealthConnectStatus.AVAILABLE -> {
                    if (healthConnectRepository.hasRequiredPermissions()) {
                        importHealthSummary()
                    } else {
                        _uiState.update { it.copy(isHealthSyncing = false) }
                        _effects.emit(
                            RecordEditEffect.RequestHealthPermissions(
                                permissions = healthConnectRepository.requiredPermissions()
                            )
                        )
                    }
                }
                HealthConnectStatus.NOT_INSTALLED -> {
                    _uiState.update {
                        it.copy(
                            isHealthSyncing = false,
                            healthConnectMessage = HEALTH_CONNECT_NOT_INSTALLED_MESSAGE
                        )
                    }
                }
                HealthConnectStatus.UPDATE_REQUIRED -> {
                    _uiState.update {
                        it.copy(
                            isHealthSyncing = false,
                            healthConnectMessage = HEALTH_CONNECT_UPDATE_REQUIRED_MESSAGE
                        )
                    }
                }
            }
        }
    }

    fun onHealthPermissionResult(grantedPermissions: Set<String>) {
        val requiredPermissions = healthConnectRepository.requiredPermissions()
        if (!grantedPermissions.containsAll(requiredPermissions)) {
            _uiState.update {
                it.copy(
                    isHealthSyncing = false,
                    healthConnectMessage = HEALTH_CONNECT_PERMISSION_DENIED_MESSAGE
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isHealthSyncing = true, healthConnectMessage = null) }
            importHealthSummary()
        }
    }

    fun confirmHealthOverwrite() {
        val summary = pendingHealthSummary ?: return
        applyHealthSummary(summary)
        pendingHealthSummary = null
    }

    fun dismissHealthOverwriteDialog() {
        pendingHealthSummary = null
        _uiState.update {
            it.copy(
                showHealthOverwriteDialog = false,
                healthConnectMessage = HEALTH_CONNECT_OVERWRITE_CANCELLED_MESSAGE
            )
        }
    }

    fun dismissHealthConnectMessage() {
        _uiState.update { it.copy(healthConnectMessage = null) }
    }

    fun onHealthConnectAppOpenFailed() {
        _uiState.update {
            it.copy(healthConnectMessage = HEALTH_CONNECT_APP_OPEN_FAILED_MESSAGE)
        }
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
                condition = _uiState.value.conditionType,
                conditionMemo = _uiState.value.conditionMemo.ifBlank { null },
                isToilet = _uiState.value.isToilet,
                stepCount = _uiState.value.stepCount,
                healthKcal = _uiState.value.healthKcal,
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
                Timber.e(error, "Failed to save record")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to save record"
                    )
                }
            }
        }
    }

    private suspend fun importHealthSummary() {
        runCatching {
            healthConnectRepository.readDailySummary(_uiState.value.recordDate)
        }.onSuccess { summary ->
            val shouldAskOverwrite = shouldAskOverwrite(summary)
            if (shouldAskOverwrite) {
                pendingHealthSummary = summary
                _uiState.update {
                    it.copy(
                        isHealthSyncing = false,
                        showHealthOverwriteDialog = true
                    )
                }
            } else {
                applyHealthSummary(summary)
            }
        }.onFailure { error ->
            Timber.e(error, "Failed to import Health Connect data")
            _uiState.update {
                it.copy(
                    isHealthSyncing = false,
                    healthConnectMessage = error.message ?: HEALTH_CONNECT_IMPORT_FAILED_MESSAGE
                )
            }
        }
    }

    private fun shouldAskOverwrite(summary: DailyHealthSummary): Boolean {
        val current = _uiState.value
        val hasStepConflict = current.stepCount != null && current.stepCount != summary.stepCount
        val hasKcalConflict = current.healthKcal != null && current.healthKcal != summary.burnedKcal
        return hasStepConflict || hasKcalConflict
    }

    private fun applyHealthSummary(summary: DailyHealthSummary) {
        updateInput {
            it.copy(
                stepCount = summary.stepCount,
                healthKcal = summary.burnedKcal,
                isHealthSyncing = false,
                showHealthOverwriteDialog = false,
                healthConnectMessage = null,
                errorMessage = null
            )
        }
    }

    private fun loadRecord(): Job {
        return viewModelScope.launch {
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
                        conditionType = record.condition,
                        conditionMemo = record.conditionMemo.orEmpty(),
                        isToilet = record.isToilet,
                        stepCount = record.stepCount,
                        healthKcal = record.healthKcal,
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
                            stepCount = null,
                            healthKcal = null,
                            ringfitKcalInput = "",
                            ringfitKmInput = "",
                            hasChanges = false
                        )
                    }
                } else {
                    Timber.e(error, "Failed to load record")
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
            baseRecord.condition != uiState.conditionType ||
                baseRecord.conditionMemo.orEmpty() != uiState.conditionMemo
        val isToiletChanged = baseRecord.isToilet != uiState.isToilet
        val stepsChanged = baseRecord.stepCount != uiState.stepCount
        val healthKcalChanged = baseRecord.healthKcal != uiState.healthKcal
        val kcalChanged = when (val parsed = parseNumberInput(uiState.ringfitKcalInput)) {
            is ParsedNumber.Invalid -> true
            is ParsedNumber.Valid -> parsed.value != baseRecord.ringfitKcal
        }
        val kmChanged = when (val parsed = parseNumberInput(uiState.ringfitKmInput)) {
            is ParsedNumber.Invalid -> true
            is ParsedNumber.Valid -> parsed.value != baseRecord.ringfitKm
        }

        return mealsChanged || conditionChanged || isToiletChanged || stepsChanged ||
            healthKcalChanged || kcalChanged || kmChanged
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

    sealed interface RecordEditEffect {
        data class RequestHealthPermissions(val permissions: Set<String>) : RecordEditEffect
    }

    companion object {
        const val RECORD_ID_ARG: String = "recordId"
        const val RESULT_UPDATED_ARG: String = "recordUpdated"

        private const val RINGFIT_KCAL_ERROR_MESSAGE: String = "RingFit kcal must be a number."
        private const val RINGFIT_KM_ERROR_MESSAGE: String = "RingFit km must be a number."
        private const val HEALTH_CONNECT_NOT_INSTALLED_MESSAGE: String = "Health Connect is not installed on this device."
        private const val HEALTH_CONNECT_UPDATE_REQUIRED_MESSAGE: String = "Health Connect requires an update before use."
        private const val HEALTH_CONNECT_PERMISSION_DENIED_MESSAGE: String = "Health Connect permission was denied."
        private const val HEALTH_CONNECT_IMPORT_FAILED_MESSAGE: String = "Failed to import Health Connect data."
        private const val HEALTH_CONNECT_OVERWRITE_CANCELLED_MESSAGE: String = "Import cancelled. Existing values were kept."
        private const val HEALTH_CONNECT_APP_OPEN_FAILED_MESSAGE: String = "Failed to open the Health Connect app."

        private fun parseRecordId(savedStateHandle: SavedStateHandle): Int {
            val raw = checkNotNull(savedStateHandle.get<Any?>(RECORD_ID_ARG)) {
                "Missing navigation argument: $RECORD_ID_ARG"
            }
            return when (raw) {
                is Int -> raw
                is Long -> raw.toInt()
                is String -> raw.toIntOrNull()
                else -> null
            } ?: error("Invalid navigation argument: $RECORD_ID_ARG=$raw")
        }
    }
}
