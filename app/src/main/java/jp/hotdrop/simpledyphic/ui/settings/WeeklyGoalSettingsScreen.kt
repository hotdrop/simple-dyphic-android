package jp.hotdrop.simpledyphic.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme

@Composable
fun WeeklyGoalSettingsRoute(
    onBack: () -> Unit,
    viewModel: WeeklyGoalSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeeklyGoalSettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onTargetChanged = viewModel::onTargetChanged,
        onSaveClick = viewModel::onSaveClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyGoalSettingsScreen(
    uiState: WeeklyGoalSettingsUiState,
    onBack: () -> Unit,
    onTargetChanged: (HealthMetricType, String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.weekly_goal_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.record_edit_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 24.dp))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.weekly_goal_description),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            items(uiState.goalInputs, key = { it.metricType.name }) { goal ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = metricName(goal.metricType),
                            style = MaterialTheme.typography.titleSmall
                        )
                        OutlinedTextField(
                            value = goal.targetInput,
                            onValueChange = { onTargetChanged(goal.metricType, it) },
                            label = { Text(text = stringResource(R.string.weekly_goal_target_label)) },
                            suffix = { Text(text = metricUnit(goal.metricType)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }

            uiState.errorMessageResId?.let { messageResId ->
                item {
                    Text(
                        text = stringResource(messageResId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.messageResId?.let { messageResId ->
                item {
                    Text(
                        text = stringResource(messageResId),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Button(
                    onClick = onSaveClick,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Text(text = stringResource(R.string.weekly_goal_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun metricName(metricType: HealthMetricType): String {
    val resId = when (metricType) {
        HealthMetricType.STEP_COUNT -> R.string.health_metric_step_count
        HealthMetricType.ACTIVE_KCAL -> R.string.health_metric_active_kcal
        HealthMetricType.EXERCISE_MINUTES -> R.string.health_metric_exercise_minutes
        HealthMetricType.DISTANCE_KM -> R.string.health_metric_distance_km
    }
    return stringResource(resId)
}

@Composable
private fun metricUnit(metricType: HealthMetricType): String {
    val resId = when (metricType) {
        HealthMetricType.STEP_COUNT -> R.string.health_metric_unit_step
        HealthMetricType.ACTIVE_KCAL -> R.string.health_metric_unit_kcal
        HealthMetricType.EXERCISE_MINUTES -> R.string.health_metric_unit_minute
        HealthMetricType.DISTANCE_KM -> R.string.health_metric_unit_km
    }
    return stringResource(resId)
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeeklyGoalSettingsLoadingPreview() {
    WeeklyGoalSettingsPreview(
        uiState = WeeklyGoalSettingsUiState(isLoading = true)
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeeklyGoalSettingsDefaultPreview() {
    WeeklyGoalSettingsPreview(
        uiState = WeeklyGoalSettingsUiState(
            isLoading = false,
            goalInputs = previewGoalInputs()
        )
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeeklyGoalSettingsSavingPreview() {
    WeeklyGoalSettingsPreview(
        uiState = WeeklyGoalSettingsUiState(
            isLoading = false,
            isSaving = true,
            goalInputs = previewGoalInputs()
        )
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeeklyGoalSettingsErrorPreview() {
    WeeklyGoalSettingsPreview(
        uiState = WeeklyGoalSettingsUiState(
            isLoading = false,
            goalInputs = previewGoalInputs(),
            errorMessageResId = R.string.weekly_goal_error_invalid_number
        )
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeeklyGoalSettingsMessagePreview() {
    WeeklyGoalSettingsPreview(
        uiState = WeeklyGoalSettingsUiState(
            isLoading = false,
            goalInputs = previewGoalInputs(),
            messageResId = R.string.weekly_goal_message_saved
        )
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WeeklyGoalSettingsEmptyGoalListPreview() {
    WeeklyGoalSettingsPreview(
        uiState = WeeklyGoalSettingsUiState(
            isLoading = false,
            goalInputs = emptyList()
        )
    )
}

@Composable
private fun WeeklyGoalSettingsPreview(
    uiState: WeeklyGoalSettingsUiState
) {
    SimpleDyphicTheme {
        WeeklyGoalSettingsScreen(
            uiState = uiState,
            onBack = {},
            onTargetChanged = { _, _ -> },
            onSaveClick = {}
        )
    }
}

private fun previewGoalInputs(): List<WeeklyGoalInputUiModel> {
    return listOf(
        WeeklyGoalInputUiModel(
            metricType = HealthMetricType.STEP_COUNT,
            targetInput = "70000",
            enabled = true
        ),
        WeeklyGoalInputUiModel(
            metricType = HealthMetricType.ACTIVE_KCAL,
            targetInput = "2100",
            enabled = true
        ),
        WeeklyGoalInputUiModel(
            metricType = HealthMetricType.EXERCISE_MINUTES,
            targetInput = "150",
            enabled = true
        ),
        WeeklyGoalInputUiModel(
            metricType = HealthMetricType.DISTANCE_KM,
            targetInput = "21",
            enabled = true
        )
    )
}
