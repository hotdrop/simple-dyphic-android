package jp.hotdrop.simpledyphic.ui.record

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import java.util.Locale
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.ui.components.ConditionIcon
import jp.hotdrop.simpledyphic.model.ConditionType
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme

@Composable
fun RecordEditRoute(
    onBack: (Boolean) -> Unit,
    viewModel: RecordEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = viewModel::onHealthPermissionResult
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecordEditViewModel.RecordEditEffect.RequestHealthPermissions -> {
                    permissionLauncher.launch(effect.permissions)
                }
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.onScreenEntered()
    }

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
        onSave = { viewModel.save(onBack) },
        onHealthSyncRequest = {
            if (!launchHealthConnectApp(context)) {
                viewModel.onHealthConnectAppOpenFailed()
            }
        },
        onDismissHealthMessage = viewModel::dismissHealthConnectMessage
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
    onHealthSyncRequest: () -> Unit,
    onDismissHealthMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val recordDateFormatPattern = stringResource(R.string.record_date_format_pattern)
    val recordDateFormatter = remember(recordDateFormatPattern, locale) {
        DateTimeFormatter.ofPattern(recordDateFormatPattern, locale)
    }

    BackHandler(onBack = onBackRequest)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.record_edit_title)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
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
                .padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
        ) {
            Text(
                text = uiState.recordDate.format(recordDateFormatter),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            MealArea(
                breakfast = uiState.breakfast,
                lunch = uiState.lunch,
                dinner = uiState.dinner,
                onBreakfastChanged = onBreakfastChanged,
                onLunchChanged = onLunchChanged,
                onDinnerChanged = onDinnerChanged
            )
            Spacer(modifier = Modifier.height(8.dp))

            HealthConnectCard(
                stepCount = uiState.stepCount ?: 0,
                healthKcal = uiState.healthKcal ?: 0.0,
                isHealthSyncing = uiState.isHealthSyncing,
                healthConnectMessageResId = uiState.healthConnectMessageResId,
                onHealthSyncRequest = onHealthSyncRequest,
                onDismissHealthMessage = onDismissHealthMessage
            )
            Spacer(modifier = Modifier.height(8.dp))

            RingFitCard(
                ringfitKcalInput = uiState.ringfitKcalInput,
                ringfitKmInput = uiState.ringfitKmInput,
                onRingfitKcalChanged = onRingfitKcalChanged,
                onRingfitKmChanged = onRingfitKmChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConditionSelector(
                selectedType = uiState.conditionType,
                onConditionTypeChanged = onConditionTypeChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.isToilet,
                    onCheckedChange = onIsToiletChanged
                )
                Text(
                    text = stringResource(R.string.record_toilet_done),
                    fontSize = 20.sp
                )
            }

            OutlinedTextField(
                value = uiState.conditionMemo,
                onValueChange = onConditionMemoChanged,
                label = { Text(text = stringResource(R.string.record_condition_memo_edit_label)) },
                minLines = 4,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            uiState.errorMessageResId?.let { messageResId ->
                Text(
                    text = stringResource(messageResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .testTag("record_save_button")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(R.string.record_save))
                    }
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

@Composable
private fun MealArea(
    breakfast: String,
    lunch: String,
    dinner: String,
    onBreakfastChanged: (String) -> Unit,
    onLunchChanged: (String) -> Unit,
    onDinnerChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MealCard(
            iconResId = R.drawable.ic_breakfast,
            value = breakfast,
            onValueChange = onBreakfastChanged,
            textFieldTag = "record_breakfast_input"
        )
        MealCard(
            iconResId = R.drawable.ic_lunch,
            value = lunch,
            onValueChange = onLunchChanged
        )
        MealCard(
            iconResId = R.drawable.ic_dinner,
            value = dinner,
            onValueChange = onDinnerChanged
        )
    }
}

@Composable
private fun MealCard(
    iconResId: Int,
    value: String,
    onValueChange: (String) -> Unit,
    textFieldTag: String? = null
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .size(32.dp),
                contentScale = ContentScale.Fit
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .let { current ->
                        if (textFieldTag != null) current.testTag(textFieldTag) else current
                    },
                maxLines = 6
            )
        }
    }
}

@Composable
private fun HealthConnectCard(
    stepCount: Int,
    healthKcal: Double,
    isHealthSyncing: Boolean,
    healthConnectMessageResId: Int?,
    onHealthSyncRequest: () -> Unit,
    onDismissHealthMessage: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clickable(
                        enabled = !isHealthSyncing,
                        onClick = onHealthSyncRequest
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isHealthSyncing) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_health),
                        contentDescription = stringResource(R.string.record_health_import_button),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(modifier = Modifier.width(48.dp))
            Column(modifier = Modifier.weight(1f)) {
                healthConnectMessageResId?.let { messageResId ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = stringResource(messageResId),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissHealthMessage) {
                            Text(text = stringResource(R.string.record_health_message_dismiss))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                HealthMetricRow(
                    lineColor = colorResource(id = R.color.record_health_step_line),
                    valueText = stringResource(R.string.record_health_steps_value, stepCount)
                )
                Spacer(modifier = Modifier.height(12.dp))
                HealthMetricRow(
                    lineColor = colorResource(id = R.color.record_health_kcal_line),
                    valueText = stringResource(R.string.record_health_kcal_value, healthKcal)
                )
            }
        }
    }
}

@Composable
private fun HealthMetricRow(
    lineColor: Color,
    valueText: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        VerticalColorLine(color = lineColor)
        Text(
            text = valueText,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun VerticalColorLine(color: Color) {
    VerticalDivider(
        modifier = Modifier
            .height(36.dp),
        thickness = 2.dp,
        color = color
    )
}

@Composable
private fun RingFitCard(
    ringfitKcalInput: String,
    ringfitKmInput: String,
    onRingfitKcalChanged: (String) -> Unit,
    onRingfitKmChanged: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_ringfit),
                contentDescription = null,
                modifier = Modifier.size(108.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = ringfitKcalInput,
                    onValueChange = onRingfitKcalChanged,
                    label = { Text(text = stringResource(R.string.record_ringfit_kcal_short_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ringfitKmInput,
                    onValueChange = onRingfitKmChanged,
                    label = { Text(text = stringResource(R.string.record_ringfit_km_short_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ConditionSelector(
    selectedType: ConditionType?,
    onConditionTypeChanged: (ConditionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ConditionType.entries.forEach { type ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ConditionIcon(
                    type = type,
                    selected = type == selectedType,
                    modifier = Modifier.size(50.dp)
                )
                RadioButton(
                    selected = type == selectedType,
                    onClick = { onConditionTypeChanged(type) }
                )
            }
        }
    }
}

@Preview(heightDp = 1200, showBackground = true)
@Composable
private fun RecordEditScreenPreview() {
    SimpleDyphicTheme {
        RecordEditScreen(
            uiState = RecordEditUiState(
                breakfast = "Toast",
                lunch = "Pasta",
                dinner = "Soup",
                conditionType = ConditionType.NORMAL,
                conditionMemo = "今日は体調が良いです。",
                isToilet = true,
                stepCount = 8632,
                healthKcal = 392.4,
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
            onSave = {},
            onHealthSyncRequest = {},
            onDismissHealthMessage = {}
        )
    }
}

private fun launchHealthConnectApp(context: Context): Boolean {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(HEALTH_CONNECT_PACKAGE_NAME)
        ?: return false
    return runCatching {
        context.startActivity(launchIntent)
    }.isSuccess
}

private const val HEALTH_CONNECT_PACKAGE_NAME: String = "com.google.android.apps.healthdata"
