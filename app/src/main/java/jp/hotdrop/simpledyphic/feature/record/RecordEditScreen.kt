package jp.hotdrop.simpledyphic.feature.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme

@Composable
fun RecordEditRoute(
    onBack: (Boolean) -> Unit,
    viewModel: RecordEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecordEditScreen(
        uiState = uiState,
        onBackRequest = { viewModel.onBackRequested { onBack(false) } },
        onConfirmDiscard = { viewModel.confirmDiscardAndClose { onBack(false) } },
        onDismissDiscardDialog = viewModel::dismissDiscardDialog,
        onBreakfastChanged = viewModel::onBreakfastChanged,
        onLunchChanged = viewModel::onLunchChanged,
        onDinnerChanged = viewModel::onDinnerChanged,
        onConditionTypeChanged = viewModel::onConditionTypeChanged,
        onConditionMemoChanged = viewModel::onConditionMemoChanged,
        onIsToiletChanged = viewModel::onIsToiletChanged,
        onRingfitKcalChanged = viewModel::onRingfitKcalChanged,
        onRingfitKmChanged = viewModel::onRingfitKmChanged,
        onSave = { viewModel.save(onBack) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditScreen(
    uiState: RecordEditUiState,
    onBackRequest: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscardDialog: () -> Unit,
    onBreakfastChanged: (String) -> Unit,
    onLunchChanged: (String) -> Unit,
    onDinnerChanged: (String) -> Unit,
    onConditionTypeChanged: (ConditionType) -> Unit,
    onConditionMemoChanged: (String) -> Unit,
    onIsToiletChanged: (Boolean) -> Unit,
    onRingfitKcalChanged: (String) -> Unit,
    onRingfitKmChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackRequest)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.record_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackRequest) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.record_edit_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.record_edit_date, uiState.recordDate.toString()),
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = uiState.breakfast,
                onValueChange = onBreakfastChanged,
                label = { Text(text = stringResource(R.string.record_breakfast_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.lunch,
                onValueChange = onLunchChanged,
                label = { Text(text = stringResource(R.string.record_lunch_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.dinner,
                onValueChange = onDinnerChanged,
                label = { Text(text = stringResource(R.string.record_dinner_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.record_condition_label),
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConditionType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.conditionType == type,
                        onClick = { onConditionTypeChanged(type) },
                        label = {
                            Text(
                                when (type) {
                                    ConditionType.BAD -> stringResource(R.string.record_condition_bad)
                                    ConditionType.NORMAL -> stringResource(R.string.record_condition_normal)
                                    ConditionType.GOOD -> stringResource(R.string.record_condition_good)
                                }
                            )
                        }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.conditionMemo,
                onValueChange = onConditionMemoChanged,
                label = { Text(text = stringResource(R.string.record_condition_memo_label)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.record_toilet_label),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.isToilet,
                    onCheckedChange = onIsToiletChanged
                )
            }

            OutlinedTextField(
                value = uiState.ringfitKcalInput,
                onValueChange = onRingfitKcalChanged,
                label = { Text(text = stringResource(R.string.record_ringfit_kcal_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.ringfitKmInput,
                onValueChange = onRingfitKmChanged,
                label = { Text(text = stringResource(R.string.record_ringfit_km_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(vertical = 2.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(R.string.record_save))
                }
            }
        }
    }

    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscardDialog,
            title = { Text(text = stringResource(R.string.record_discard_title)) },
            text = { Text(text = stringResource(R.string.record_discard_message)) },
            confirmButton = {
                Button(onClick = onConfirmDiscard) {
                    Text(text = stringResource(R.string.record_discard_confirm))
                }
            },
            dismissButton = {
                Button(onClick = onDismissDiscardDialog) {
                    Text(text = stringResource(R.string.record_discard_cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordEditScreenPreview() {
    SimpleDyphicTheme {
        RecordEditScreen(
            uiState = RecordEditUiState(
                breakfast = "Toast",
                lunch = "Pasta",
                dinner = "Soup",
                conditionType = ConditionType.NORMAL,
                conditionMemo = "Stable condition",
                isToilet = true,
                ringfitKcalInput = "120",
                ringfitKmInput = "2.5",
                hasChanges = true
            ),
            onBackRequest = {},
            onConfirmDiscard = {},
            onDismissDiscardDialog = {},
            onBreakfastChanged = {},
            onLunchChanged = {},
            onDinnerChanged = {},
            onConditionTypeChanged = {},
            onConditionMemoChanged = {},
            onIsToiletChanged = {},
            onRingfitKcalChanged = {},
            onRingfitKmChanged = {},
            onSave = {}
        )
    }
}
