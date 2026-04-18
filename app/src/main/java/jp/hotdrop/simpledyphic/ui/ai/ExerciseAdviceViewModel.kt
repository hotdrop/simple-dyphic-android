package jp.hotdrop.simpledyphic.ui.ai

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.data.repository.HealthConnectRepository
import jp.hotdrop.simpledyphic.domain.usecase.GenerateExerciseAdviceUseCase
import jp.hotdrop.simpledyphic.model.AdvicePeriod
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.ExerciseAdviceResult
import jp.hotdrop.simpledyphic.model.HealthMetricType
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
class ExerciseAdviceViewModel @Inject constructor(
    private val generateExerciseAdviceUseCase: GenerateExerciseAdviceUseCase,
    private val healthConnectRepository: HealthConnectRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ExerciseAdviceUiState())
    val uiState: StateFlow<ExerciseAdviceUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ExerciseAdviceEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<ExerciseAdviceEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null
    private var generateJob: Job? = null

    init {
        loadInput()
    }

    fun onPeriodSelected(period: AdvicePeriod) {
        if (_uiState.value.selectedPeriod == period) return
        generateJob?.cancel()
        _uiState.update {
            it.copy(
                selectedPeriod = period,
                adviceText = "",
                isGenerating = false,
                isInitializingModel = false
            )
        }
        loadInput()
    }

    fun onRetry() {
        loadInput()
    }

    fun onGenerateClick() {
        val input = _uiState.value.input ?: return
        if (!input.canGenerate || _uiState.value.isGenerating) return

        generateJob?.cancel()
        generateJob = launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    isInitializingModel = true,
                    adviceText = "",
                    errorMessageResId = null
                )
            }
            try {
                generateExerciseAdviceUseCase.execute(input).collect { result ->
                    when (result) {
                        is ExerciseAdviceResult.Initializing -> {
                            _uiState.update {
                                it.copy(
                                    input = result.input,
                                    isGenerating = true,
                                    isInitializingModel = true
                                )
                            }
                        }
                        is ExerciseAdviceResult.Streaming -> {
                            _uiState.update {
                                it.copy(
                                    input = result.input,
                                    isGenerating = true,
                                    isInitializingModel = false,
                                    adviceText = result.text
                                )
                            }
                        }
                        is ExerciseAdviceResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    input = result.input,
                                    isGenerating = false,
                                    isInitializingModel = false,
                                    adviceText = result.text
                                )
                            }
                        }
                    }
                }
            } catch (error: Throwable) {
                Timber.e(error, "Failed to generate exercise advice")
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isInitializingModel = false,
                        errorMessageResId = R.string.ai_advice_error_generation_failed
                    )
                }
            }
        }
    }

    fun onHealthPermissionRequestClick() {
        launch {
            _effects.send(
                ExerciseAdviceEffect.RequestHealthPermissions(
                    permissions = healthConnectRepository.requiredPermissions(AI_METRIC_TYPES)
                )
            )
        }
    }

    fun onHealthPermissionResult(grantedPermissions: Set<String>) {
        val requiredPermissions = healthConnectRepository.requiredPermissions(AI_METRIC_TYPES)
        if (!grantedPermissions.containsAll(requiredPermissions)) {
            _uiState.update { it.copy(errorMessageResId = R.string.ai_advice_error_permission_required) }
            return
        }
        loadInput()
    }

    private fun loadInput() {
        loadJob?.cancel()
        loadJob = launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessageResId = null,
                    isGenerating = false,
                    isInitializingModel = false
                )
            }
            when (val result = dispatcherIO {
                generateExerciseAdviceUseCase.buildInput(_uiState.value.selectedPeriod)
            }) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            input = result.value,
                            errorMessageResId = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    Timber.e(result.error, "Failed to load exercise advice input")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            input = null,
                            errorMessageResId = R.string.ai_advice_error_load_failed
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val AI_METRIC_TYPES: Set<HealthMetricType> = setOf(
            HealthMetricType.STEP_COUNT,
            HealthMetricType.ACTIVE_KCAL,
            HealthMetricType.EXERCISE_MINUTES,
            HealthMetricType.DISTANCE_KM
        )
    }
}
