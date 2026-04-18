package jp.hotdrop.simpledyphic.ui.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.model.AdvicePeriod
import jp.hotdrop.simpledyphic.model.ExerciseAdviceInput
import jp.hotdrop.simpledyphic.model.ExerciseAdviceRequirement
import jp.hotdrop.simpledyphic.model.ExerciseMetricKind
import jp.hotdrop.simpledyphic.model.ExerciseMetricSummary
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.ui.components.ErrorContent
import jp.hotdrop.simpledyphic.ui.components.LoadingContent
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme
import java.time.LocalDate

@Composable
fun ExerciseAdviceRoute(
    onOpenSettings: () -> Unit,
    viewModel: ExerciseAdviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = viewModel::onHealthPermissionResult
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExerciseAdviceEffect.RequestHealthPermissions -> {
                    permissionLauncher.launch(effect.permissions)
                }
            }
        }
    }

    ExerciseAdviceScreen(
        uiState = uiState,
        onRetry = viewModel::onRetry,
        onPeriodSelected = viewModel::onPeriodSelected,
        onGenerateClick = viewModel::onGenerateClick,
        onRequestPermissionClick = viewModel::onHealthPermissionRequestClick,
        onOpenSettingsClick = onOpenSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseAdviceScreen(
    uiState: ExerciseAdviceUiState,
    onRetry: () -> Unit,
    onPeriodSelected: (AdvicePeriod) -> Unit,
    onGenerateClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.ai_advice_title)) })
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingContent(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = stringResource(R.string.ai_advice_loading)
                )
            }
            uiState.errorMessageResId != null && uiState.input == null -> {
                ErrorContent(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = stringResource(uiState.errorMessageResId),
                    onRetry = onRetry
                )
            }
            else -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        AdvicePeriodSelector(
                            selectedPeriod = uiState.selectedPeriod,
                            onPeriodSelected = onPeriodSelected
                        )
                    }

                    uiState.input?.let { input ->
                        item {
                            AdviceSummaryHeader(input = input)
                        }
                        item {
                            MissingRequirementCard(
                                input = input,
                                onRequestPermissionClick = onRequestPermissionClick,
                                onOpenSettingsClick = onOpenSettingsClick
                            )
                        }
                        item {
                            AdviceSummaryCard(input = input)
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_advice_result_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                when {
                                    uiState.isInitializingModel -> {
                                        Text(text = stringResource(R.string.ai_advice_initializing_model))
                                    }
                                    uiState.adviceText.isNotBlank() -> {
                                        Text(
                                            text = uiState.adviceText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.testTag("ai_advice_result_text")
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = stringResource(R.string.ai_advice_placeholder),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                uiState.errorMessageResId?.let { messageResId ->
                                    if (uiState.input != null) {
                                        Text(
                                            text = stringResource(messageResId),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = onGenerateClick,
                            enabled = uiState.canGenerate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_advice_generate_button")
                        ) {
                            Text(
                                text = if (uiState.adviceText.isBlank()) {
                                    stringResource(R.string.ai_advice_generate)
                                } else {
                                    stringResource(R.string.ai_advice_regenerate)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvicePeriodSelector(
    selectedPeriod: AdvicePeriod,
    onPeriodSelected: (AdvicePeriod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvicePeriod.entries.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(text = period.toLabel()) },
                modifier = Modifier.testTag("ai_advice_period_${period.name}")
            )
        }
    }
}

@Composable
private fun AdviceSummaryHeader(input: ExerciseAdviceInput) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_advice_period_label, input.period.toLabel()),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.ai_advice_period_range,
                    input.periodStartDate.toString(),
                    input.periodEndDate.toString()
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MissingRequirementCard(
    input: ExerciseAdviceInput,
    onRequestPermissionClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    val missing = input.missingRequirements
    if (missing.isEmpty()) {
        return
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_advice_missing_requirements_title),
                style = MaterialTheme.typography.titleMedium
            )
            missing.forEach { requirement ->
                Text(text = "・${requirement.toLabel()}")
            }
            if (missing.contains(ExerciseAdviceRequirement.HEALTH_PERMISSION)) {
                Button(
                    onClick = onRequestPermissionClick,
                    modifier = Modifier.testTag("ai_advice_request_permission_button")
                ) {
                    Text(text = stringResource(R.string.ai_advice_request_permission))
                }
            }
            if (missing.any { it != ExerciseAdviceRequirement.HEALTH_PERMISSION }) {
                Button(
                    onClick = onOpenSettingsClick,
                    modifier = Modifier.testTag("ai_advice_open_settings_button")
                ) {
                    Text(text = stringResource(R.string.ai_advice_open_settings))
                }
            }
        }
    }
}

@Composable
private fun AdviceSummaryCard(input: ExerciseAdviceInput) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_advice_summary_title),
                style = MaterialTheme.typography.titleMedium
            )
            input.summaries.forEach { summary ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = summary.kind.toLabel(),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = summary.toDisplayText(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.ai_advice_profile_summary,
                    input.age?.let { "${it}歳" } ?: stringResource(R.string.ai_advice_not_set),
                    input.settings.heightCm?.formatForUi(ExerciseMetricKind.DISTANCE_KM, forceUnit = "cm")
                        ?: stringResource(R.string.ai_advice_not_set),
                    input.settings.weightKg?.formatForUi(ExerciseMetricKind.ACTIVE_KCAL, forceUnit = "kg")
                        ?: stringResource(R.string.ai_advice_not_set)
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ExerciseMetricSummary.toDisplayText(): String {
    val actualText = when (availability) {
        MetricAvailability.AVAILABLE -> actualValue?.formatForUi(kind) ?: stringResource(R.string.ai_advice_not_set)
        MetricAvailability.PERMISSION_MISSING -> stringResource(R.string.calendar_weekly_not_available_permission)
        MetricAvailability.SOURCE_UNAVAILABLE -> stringResource(R.string.calendar_weekly_not_available_source)
    }
    val targetText = targetValue?.let { target ->
        stringResource(R.string.ai_advice_target_format, target.formatForUi(kind))
    }.orEmpty()
    val rateText = achievementRate?.let { rate ->
        stringResource(R.string.ai_advice_rate_format, rate)
    }.orEmpty()
    return listOf(actualText, targetText, rateText)
        .filter { it.isNotBlank() }
        .joinToString(separator = " / ")
}

private fun Double.formatForUi(kind: ExerciseMetricKind, forceUnit: String? = null): String {
    val number = when (kind) {
        ExerciseMetricKind.STEP_COUNT -> toInt().toString()
        else -> if (this % 1.0 == 0.0) toInt().toString() else String.format("%.1f", this)
    }
    val unit = forceUnit ?: when (kind) {
        ExerciseMetricKind.STEP_COUNT -> "歩"
        ExerciseMetricKind.ACTIVE_KCAL -> "kcal"
        ExerciseMetricKind.EXERCISE_MINUTES -> "分"
        ExerciseMetricKind.DISTANCE_KM -> "km"
        ExerciseMetricKind.RINGFIT_KCAL -> "kcal"
        ExerciseMetricKind.RINGFIT_KM -> "km"
    }
    return "$number $unit"
}

private fun AdvicePeriod.toLabel(): String {
    return when (this) {
        AdvicePeriod.WEEKLY -> "週間"
        AdvicePeriod.MONTHLY -> "月間"
        AdvicePeriod.THREE_MONTHS -> "3ヶ月"
    }
}

private fun ExerciseAdviceRequirement.toLabel(): String {
    return when (this) {
        ExerciseAdviceRequirement.BIRTH_DATE -> "生年月日"
        ExerciseAdviceRequirement.HEIGHT -> "身長"
        ExerciseAdviceRequirement.WEIGHT -> "体重"
        ExerciseAdviceRequirement.MODEL_FILE -> "Gemma 4モデルファイル"
        ExerciseAdviceRequirement.HEALTH_PERMISSION -> "Health Connect の権限"
    }
}

private fun ExerciseMetricKind.toLabel(): String {
    return when (this) {
        ExerciseMetricKind.STEP_COUNT -> "歩数"
        ExerciseMetricKind.ACTIVE_KCAL -> "活動消費kcal"
        ExerciseMetricKind.EXERCISE_MINUTES -> "運動時間"
        ExerciseMetricKind.DISTANCE_KM -> "移動距離"
        ExerciseMetricKind.RINGFIT_KCAL -> "Ring Fit kcal"
        ExerciseMetricKind.RINGFIT_KM -> "Ring Fit km"
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun ExerciseAdviceDefaultPreview() {
    SimpleDyphicTheme {
        ExerciseAdviceScreen(
            uiState = previewUiState(),
            onRetry = {},
            onPeriodSelected = {},
            onGenerateClick = {},
            onRequestPermissionClick = {},
            onOpenSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun ExerciseAdvicePermissionPreview() {
    SimpleDyphicTheme {
        ExerciseAdviceScreen(
            uiState = previewUiState(
                input = previewInput().copy(
                    missingRequirements = setOf(ExerciseAdviceRequirement.HEALTH_PERMISSION)
                )
            ),
            onRetry = {},
            onPeriodSelected = {},
            onGenerateClick = {},
            onRequestPermissionClick = {},
            onOpenSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun ExerciseAdviceMissingSettingsPreview() {
    SimpleDyphicTheme {
        ExerciseAdviceScreen(
            uiState = previewUiState(
                input = previewInput().copy(
                    missingRequirements = setOf(
                        ExerciseAdviceRequirement.BIRTH_DATE,
                        ExerciseAdviceRequirement.MODEL_FILE
                    )
                )
            ),
            onRetry = {},
            onPeriodSelected = {},
            onGenerateClick = {},
            onRequestPermissionClick = {},
            onOpenSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun ExerciseAdviceGeneratingPreview() {
    SimpleDyphicTheme {
        ExerciseAdviceScreen(
            uiState = previewUiState(
                isGenerating = true,
                isInitializingModel = true
            ),
            onRetry = {},
            onPeriodSelected = {},
            onGenerateClick = {},
            onRequestPermissionClick = {},
            onOpenSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun ExerciseAdviceErrorPreview() {
    SimpleDyphicTheme {
        ExerciseAdviceScreen(
            uiState = previewUiState(
                errorMessageResId = R.string.ai_advice_error_generation_failed
            ),
            onRetry = {},
            onPeriodSelected = {},
            onGenerateClick = {},
            onRequestPermissionClick = {},
            onOpenSettingsClick = {}
        )
    }
}

private fun previewUiState(
    input: ExerciseAdviceInput = previewInput(),
    isGenerating: Boolean = false,
    isInitializingModel: Boolean = false,
    errorMessageResId: Int? = null
): ExerciseAdviceUiState {
    return ExerciseAdviceUiState(
        isLoading = false,
        input = input,
        selectedPeriod = AdvicePeriod.WEEKLY,
        isGenerating = isGenerating,
        isInitializingModel = isInitializingModel,
        adviceText = if (isGenerating) "" else "今週はかなり良い流れです。歩数も運動時間もきちんと積み上がっているので、この調子で続けていきましょう。",
        errorMessageResId = errorMessageResId
    )
}

private fun previewInput(): ExerciseAdviceInput {
    return ExerciseAdviceInput(
        period = AdvicePeriod.WEEKLY,
        periodStartDate = LocalDate.of(2026, 4, 6),
        periodEndDate = LocalDate.of(2026, 4, 12),
        elapsedDays = 7,
        age = 31,
        settings = jp.hotdrop.simpledyphic.model.AppSettings(
            birthDate = LocalDate.of(1994, 1, 10),
            heightCm = 168.0,
            weightKg = 58.0,
            modelFilePath = "/data/local/tmp/gemma-4-E2B-it.litertlm",
            modelDisplayName = "gemma-4-E2B-it.litertlm"
        ),
        summaries = listOf(
            ExerciseMetricSummary(
                kind = ExerciseMetricKind.STEP_COUNT,
                actualValue = 74200.0,
                availability = MetricAvailability.AVAILABLE,
                targetValue = 70000.0,
                achievementRate = 106.0
            ),
            ExerciseMetricSummary(
                kind = ExerciseMetricKind.ACTIVE_KCAL,
                actualValue = 2200.0,
                availability = MetricAvailability.AVAILABLE,
                targetValue = 2100.0,
                achievementRate = 104.7
            ),
            ExerciseMetricSummary(
                kind = ExerciseMetricKind.EXERCISE_MINUTES,
                actualValue = 140.0,
                availability = MetricAvailability.AVAILABLE,
                targetValue = 150.0,
                achievementRate = 93.3
            ),
            ExerciseMetricSummary(
                kind = ExerciseMetricKind.DISTANCE_KM,
                actualValue = 18.6,
                availability = MetricAvailability.AVAILABLE,
                targetValue = 21.0,
                achievementRate = 88.6
            ),
            ExerciseMetricSummary(
                kind = ExerciseMetricKind.RINGFIT_KCAL,
                actualValue = 460.0
            ),
            ExerciseMetricSummary(
                kind = ExerciseMetricKind.RINGFIT_KM,
                actualValue = 8.4
            )
        ),
        missingRequirements = emptySet(),
        promptDataBlock = ""
    )
}
