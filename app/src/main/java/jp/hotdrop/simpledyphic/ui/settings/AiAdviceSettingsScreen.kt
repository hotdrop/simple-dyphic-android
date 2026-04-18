package jp.hotdrop.simpledyphic.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.ui.components.ErrorContent
import jp.hotdrop.simpledyphic.ui.components.LoadingContent
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme

@Composable
fun AiAdviceSettingsRoute(
    onBack: () -> Unit,
    viewModel: AiAdviceSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let(viewModel::onModelSelected)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AiAdviceSettingsEffect.OpenModelPicker -> pickerLauncher.launch(arrayOf("*/*"))
            }
        }
    }

    AiAdviceSettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onBirthDateClick = viewModel::onBirthDatePickerOpen,
        onBirthDateDismiss = viewModel::onBirthDatePickerDismiss,
        onBirthDateSelected = viewModel::onBirthDateSelected,
        onHeightChanged = viewModel::onHeightChanged,
        onWeightChanged = viewModel::onWeightChanged,
        onPromptChanged = viewModel::onPromptChanged,
        onPickModelClick = viewModel::onPickModelClick,
        onSaveClick = viewModel::onSaveClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdviceSettingsScreen(
    uiState: AiAdviceSettingsUiState,
    onBack: () -> Unit,
    onBirthDateClick: () -> Unit,
    onBirthDateDismiss: () -> Unit,
    onBirthDateSelected: (LocalDate) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onPromptChanged: (String) -> Unit,
    onPickModelClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.ai_settings_title)) },
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
            LoadingContent(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                message = stringResource(R.string.ai_settings_loading)
            )
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
                            text = stringResource(R.string.ai_settings_description),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.ai_settings_birth_date_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        OutlinedButton(
                            onClick = onBirthDateClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_settings_birth_date_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = uiState.birthDate?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        ?: stringResource(R.string.ai_settings_birth_date_placeholder)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null
                                )
                            }
                        }
                        OutlinedTextField(
                            value = uiState.heightCmInput,
                            onValueChange = onHeightChanged,
                            label = { Text(text = stringResource(R.string.ai_settings_height_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_settings_height_input")
                        )
                        OutlinedTextField(
                            value = uiState.weightKgInput,
                            onValueChange = onWeightChanged,
                            label = { Text(text = stringResource(R.string.ai_settings_weight_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_settings_weight_input")
                        )
                    }
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
                            text = stringResource(R.string.ai_settings_model_section_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = uiState.modelDisplayName ?: stringResource(R.string.ai_settings_model_not_selected),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = uiState.modelFilePath ?: stringResource(R.string.ai_settings_model_path_placeholder),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (uiState.isImportingModel) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.testTag("ai_settings_model_import_progress"))
                                Text(
                                    text = stringResource(R.string.ai_settings_model_importing_detail),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.ai_settings_model_import_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = onPickModelClick,
                            enabled = !uiState.isImportingModel && !uiState.isSaving,
                            modifier = Modifier.testTag("ai_settings_pick_model_button")
                        ) {
                            if (uiState.isImportingModel) {
                                CircularProgressIndicator(modifier = Modifier.padding(vertical = 4.dp))
                            } else {
                                Text(text = stringResource(R.string.ai_settings_pick_model))
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.advisorPrompt,
                    onValueChange = onPromptChanged,
                    label = { Text(text = stringResource(R.string.ai_settings_prompt_label)) },
                    minLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_settings_prompt_input")
                )
            }

            uiState.errorMessageResId?.let { messageResId ->
                item {
                    ErrorContent(
                        modifier = Modifier.fillMaxWidth(),
                        message = stringResource(messageResId)
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
                    enabled = !uiState.isSaving && !uiState.isImportingModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_settings_save_button")
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        Text(text = stringResource(R.string.ai_settings_save))
                    }
                }
            }
        }
    }

    if (uiState.isBirthDatePickerVisible) {
        val initialMillis = uiState.birthDate
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = onBirthDateDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis ?: return@TextButton
                        val date = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onBirthDateSelected(date)
                    }
                ) {
                    Text(text = stringResource(R.string.settings_data_sync_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onBirthDateDismiss) {
                    Text(text = stringResource(R.string.settings_data_sync_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun AiAdviceSettingsDefaultPreview() {
    SimpleDyphicTheme {
        AiAdviceSettingsScreen(
            uiState = AiAdviceSettingsUiState(
                isLoading = false,
                birthDate = LocalDate.of(1992, 7, 14),
                heightCmInput = "168",
                weightKgInput = "58",
                advisorPrompt = AppSettingsPreviewPrompt,
                modelDisplayName = "gemma-4-E2B-it.litertlm",
                modelFilePath = "/data/user/0/jp.hotdrop.simpledyphic/files/litertlm-models/gemma-4-E2B-it.litertlm"
            ),
            onBack = {},
            onBirthDateClick = {},
            onBirthDateDismiss = {},
            onBirthDateSelected = {},
            onHeightChanged = {},
            onWeightChanged = {},
            onPromptChanged = {},
            onPickModelClick = {},
            onSaveClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun AiAdviceSettingsSavingPreview() {
    SimpleDyphicTheme {
        AiAdviceSettingsScreen(
            uiState = AiAdviceSettingsUiState(
                isLoading = false,
                isImportingModel = true,
                modelDisplayName = "gemma-4-E2B-it.litertlm",
                isSaving = true,
                advisorPrompt = AppSettingsPreviewPrompt
            ),
            onBack = {},
            onBirthDateClick = {},
            onBirthDateDismiss = {},
            onBirthDateSelected = {},
            onHeightChanged = {},
            onWeightChanged = {},
            onPromptChanged = {},
            onPickModelClick = {},
            onSaveClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun AiAdviceSettingsErrorPreview() {
    SimpleDyphicTheme {
        AiAdviceSettingsScreen(
            uiState = AiAdviceSettingsUiState(
                isLoading = false,
                advisorPrompt = AppSettingsPreviewPrompt,
                errorMessageResId = R.string.ai_settings_error_invalid_height
            ),
            onBack = {},
            onBirthDateClick = {},
            onBirthDateDismiss = {},
            onBirthDateSelected = {},
            onHeightChanged = {},
            onWeightChanged = {},
            onPromptChanged = {},
            onPickModelClick = {},
            onSaveClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun AiAdviceSettingsDatePickerPreview() {
    SimpleDyphicTheme {
        AiAdviceSettingsScreen(
            uiState = AiAdviceSettingsUiState(
                isLoading = false,
                advisorPrompt = AppSettingsPreviewPrompt,
                isBirthDatePickerVisible = true
            ),
            onBack = {},
            onBirthDateClick = {},
            onBirthDateDismiss = {},
            onBirthDateSelected = {},
            onHeightChanged = {},
            onWeightChanged = {},
            onPromptChanged = {},
            onPickModelClick = {},
            onSaveClick = {}
        )
    }
}

private const val AppSettingsPreviewPrompt: String =
    "努力が見える点を拾いながら、続けたくなる前向きなアドバイスを返してください。"
