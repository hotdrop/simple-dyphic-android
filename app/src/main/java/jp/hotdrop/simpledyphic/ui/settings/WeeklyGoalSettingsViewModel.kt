package jp.hotdrop.simpledyphic.ui.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.data.repository.GoalRepository
import jp.hotdrop.simpledyphic.model.AppCompletable
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import jp.hotdrop.simpledyphic.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

@HiltViewModel
class WeeklyGoalSettingsViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(WeeklyGoalSettingsUiState())
    val uiState: StateFlow<WeeklyGoalSettingsUiState> = _uiState.asStateFlow()

    init {
        loadGoals()
    }

    fun onTargetChanged(metricType: HealthMetricType, input: String) {
        _uiState.update { state ->
            val updated = state.goalInputs.map { goal ->
                if (goal.metricType == metricType) goal.copy(targetInput = input) else goal
            }
            state.copy(goalInputs = updated, errorMessageResId = null, messageResId = null)
        }
    }

    fun onSaveClick() {
        val currentGoals = _uiState.value.goalInputs
        if (currentGoals.isEmpty() || _uiState.value.isSaving) {
            return
        }

        val parsedGoals = currentGoals.map { goalInput ->
            val parsed = goalInput.targetInput.toDoubleOrNull()
            if (parsed == null || parsed < 0.0) {
                return@map null
            }
            WeeklyGoal(
                metricType = goalInput.metricType,
                targetValue = parsed,
                weekStartsOnMonday = true,
                enabled = goalInput.enabled
            )
        }

        if (parsedGoals.any { it == null }) {
            _uiState.update {
                it.copy(
                    errorMessageResId = R.string.weekly_goal_error_invalid_number,
                    messageResId = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessageResId = null,
                messageResId = null
            )
        }

        launch {
            var saveFailed = false
            dispatcherIO {
                parsedGoals.filterNotNull().forEach { goal ->
                    when (val result = goalRepository.saveWeeklyGoal(goal)) {
                        AppCompletable.Complete -> Unit
                        is AppCompletable.Failure -> {
                            saveFailed = true
                            Timber.e(result.error, "Failed to save weekly goal: %s", goal.metricType)
                            return@dispatcherIO
                        }
                    }
                }
            }

            if (saveFailed) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessageResId = R.string.weekly_goal_error_save_failed
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        messageResId = R.string.weekly_goal_message_saved
                    )
                }
                loadGoals()
            }
        }
    }

    private fun loadGoals() {
        _uiState.update { it.copy(isLoading = true, errorMessageResId = null) }
        launch {
            when (val result = dispatcherIO { goalRepository.getWeeklyGoals() }) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            goalInputs = result.value.map { goal ->
                                WeeklyGoalInputUiModel(
                                    metricType = goal.metricType,
                                    targetInput = goal.targetValue.toString(),
                                    enabled = goal.enabled
                                )
                            },
                            errorMessageResId = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    Timber.e(result.error, "Failed to load weekly goals")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageResId = R.string.weekly_goal_error_load_failed
                        )
                    }
                }
            }
        }
    }
}
