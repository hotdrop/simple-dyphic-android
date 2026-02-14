package jp.hotdrop.simpledyphic.ui.record

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.data.repository.HealthConnectRepository
import jp.hotdrop.simpledyphic.data.repository.RecordRepository
import jp.hotdrop.simpledyphic.model.AppCompletable
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.ConditionType
import jp.hotdrop.simpledyphic.model.DailyHealthSummary
import jp.hotdrop.simpledyphic.model.DyphicId
import jp.hotdrop.simpledyphic.model.HealthConnectStatus
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.ui.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

@HiltViewModel
class RecordEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordRepository: RecordRepository,
    private val healthConnectRepository: HealthConnectRepository
) : BaseViewModel() {

    private val recordId: Int = parseRecordId(savedStateHandle)
    private var baseRecord: Record = Record.createEmpty(DyphicId.idToDate(recordId))
    private var hasRequestedInitialHealthSync: Boolean = false

    private val _uiState = MutableStateFlow(
        RecordEditUiState(
            recordDate = DyphicId.idToDate(recordId)
        )
    )
    val uiState: StateFlow<RecordEditUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecordEditEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<RecordEditEffect> = _effects.receiveAsFlow()
    private val loadRecordJob: Job = loadRecord()

    fun onBreakfastChanged(value: String) {
        updateInput { it.copy(breakfast = value, errorMessageResId = null) }
    }

    fun onLunchChanged(value: String) {
        updateInput { it.copy(lunch = value, errorMessageResId = null) }
    }

    fun onDinnerChanged(value: String) {
        updateInput { it.copy(dinner = value, errorMessageResId = null) }
    }

    fun onConditionTypeChanged(value: ConditionType) {
        updateInput { it.copy(conditionType = value, errorMessageResId = null) }
    }

    fun onConditionMemoChanged(value: String) {
        updateInput { it.copy(conditionMemo = value, errorMessageResId = null) }
    }

    fun onIsToiletChanged(value: Boolean) {
        updateInput { it.copy(isToilet = value, errorMessageResId = null) }
    }

    fun onRingfitKcalChanged(value: String) {
        updateInput { it.copy(ringfitKcalInput = value, errorMessageResId = null) }
    }

    fun onRingfitKmChanged(value: String) {
        updateInput { it.copy(ringfitKmInput = value, errorMessageResId = null) }
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
        launch {
            loadRecordJob.join()
            onHealthSyncRequested()
        }
    }

    fun onHealthSyncRequested() {
        launch {
            _uiState.update {
                it.copy(
                    isHealthSyncing = true,
                    healthConnectMessageResId = null,
                    errorMessageResId = null
                )
            }

            when (healthConnectRepository.getStatus()) {
                HealthConnectStatus.AVAILABLE -> {
                    when (val permissionResult = dispatcherIO { healthConnectRepository.hasRequiredPermissions() }) {
                        is AppResult.Success -> {
                            if (permissionResult.value) {
                                importHealthSummary()
                            } else {
                                _uiState.update { it.copy(isHealthSyncing = false) }
                                _effects.send(
                                    RecordEditEffect.RequestHealthPermissions(
                                        permissions = healthConnectRepository.requiredPermissions()
                                    )
                                )
                            }
                        }
                        is AppResult.Failure -> {
                            Timber.e(permissionResult.error, "Failed to check Health Connect permissions")
                            _uiState.update {
                                it.copy(
                                    isHealthSyncing = false,
                                    healthConnectMessageResId = R.string.record_health_message_import_failed
                                )
                            }
                        }
                    }
                }
                HealthConnectStatus.NOT_INSTALLED -> {
                    _uiState.update {
                        it.copy(
                            isHealthSyncing = false,
                            healthConnectMessageResId = R.string.record_health_message_not_installed
                        )
                    }
                }
                HealthConnectStatus.UPDATE_REQUIRED -> {
                    _uiState.update {
                        it.copy(
                            isHealthSyncing = false,
                            healthConnectMessageResId = R.string.record_health_message_update_required
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
                    healthConnectMessageResId = R.string.record_health_message_permission_denied
                )
            }
            return
        }

        launch {
            _uiState.update { it.copy(isHealthSyncing = true, healthConnectMessageResId = null) }
            importHealthSummary()
        }
    }

    fun dismissHealthConnectMessage() {
        _uiState.update { it.copy(healthConnectMessageResId = null) }
    }

    fun onHealthConnectAppOpenFailed() {
        _uiState.update {
            it.copy(healthConnectMessageResId = R.string.record_health_message_app_open_failed)
        }
    }

    fun save(onComplete: (Boolean) -> Unit) {
        launch {
            val kcalInput = parseNumberInput(_uiState.value.ringfitKcalInput)
            if (kcalInput is ParsedNumber.Invalid) {
                _uiState.update {
                    it.copy(errorMessageResId = R.string.record_error_ringfit_kcal_number)
                }
                return@launch
            }
            val kmInput = parseNumberInput(_uiState.value.ringfitKmInput)
            if (kmInput is ParsedNumber.Invalid) {
                _uiState.update {
                    it.copy(errorMessageResId = R.string.record_error_ringfit_km_number)
                }
                return@launch
            }
            val kcal = (kcalInput as ParsedNumber.Valid).value
            val km = (kmInput as ParsedNumber.Valid).value

            _uiState.update { it.copy(isSaving = true, errorMessageResId = null) }
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
                _uiState.update { it.copy(isSaving = false, errorMessageResId = null, hasChanges = false) }
                onComplete(false)
                return@launch
            }

            when (val saveResult = dispatcherIO { recordRepository.save(target) }) {
                AppCompletable.Complete -> {
                    baseRecord = target
                    _uiState.update { it.copy(isSaving = false, errorMessageResId = null, hasChanges = false) }
                    onComplete(true)
                }
                is AppCompletable.Failure -> {
                    Timber.e(saveResult.error, "Failed to save record")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessageResId = R.string.record_error_save_failed
                        )
                    }
                }
            }
        }
    }

    private suspend fun importHealthSummary() {
        when (val summaryResult = dispatcherIO {
            healthConnectRepository.readDailySummary(_uiState.value.recordDate)
        }) {
            is AppResult.Success -> applyHealthSummary(summaryResult.value)
            is AppResult.Failure -> {
                Timber.e(summaryResult.error, "Failed to import Health Connect data")
                _uiState.update {
                    it.copy(
                        isHealthSyncing = false,
                        healthConnectMessageResId = R.string.record_health_message_import_failed
                    )
                }
            }
        }
    }

    private fun applyHealthSummary(summary: DailyHealthSummary) {
        updateInput {
            it.copy(
                stepCount = summary.stepCount,
                healthKcal = summary.burnedKcal,
                isHealthSyncing = false,
                healthConnectMessageResId = null,
                errorMessageResId = null
            )
        }
    }

    private fun loadRecord(): Job {
        return launch {
            when (val recordResult = dispatcherIO { recordRepository.find(recordId) }) {
                is AppResult.Success -> {
                    val record = recordResult.value
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
                }
                is AppResult.Failure -> {
                    if (recordResult.error is NoSuchElementException) {
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
                        Timber.e(recordResult.error, "Failed to load record")
                        _uiState.update {
                            it.copy(errorMessageResId = R.string.record_error_load_failed)
                        }
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

        private fun parseRecordId(savedStateHandle: SavedStateHandle): Int {
            val raw = checkNotNull(savedStateHandle[RECORD_ID_ARG]) {
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
