package jp.hotdrop.simpledyphic.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.core.ui.ErrorContent
import jp.hotdrop.simpledyphic.core.ui.LoadingContent
import jp.hotdrop.simpledyphic.domain.model.DyphicId
import jp.hotdrop.simpledyphic.domain.model.Record
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CalendarRoute(
    onNavigateToRecord: (Int) -> Unit,
    recordUpdated: Boolean,
    onRecordUpdatedConsumed: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(recordUpdated) {
        if (recordUpdated) {
            viewModel.onResume()
            onRecordUpdatedConsumed()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CalendarScreen(
        uiState = uiState,
        onRetry = viewModel::onRetry,
        onMonthChanged = viewModel::onVisibleMonthChanged,
        onDateTap = { tappedDate ->
            if (tappedDate == uiState.selectedDate) {
                onNavigateToRecord(DyphicId.dateToId(tappedDate))
            } else {
                viewModel.onDaySelected(tappedDate)
            }
        },
        onEditSelectedDate = { onNavigateToRecord(viewModel.selectedDayId()) }
    )
}

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onRetry: () -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onDateTap: (LocalDate) -> Unit,
    onEditSelectedDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> LoadingContent(modifier = modifier)
        uiState.errorMessage != null -> ErrorContent(
            message = uiState.errorMessage,
            onRetry = onRetry,
            modifier = modifier
        )

        else -> CalendarContent(
            uiState = uiState,
            onMonthChanged = onMonthChanged,
            onDateTap = onDateTap,
            onEditSelectedDate = onEditSelectedDate,
            modifier = modifier
        )
    }
}

@Composable
private fun CalendarContent(
    uiState: CalendarUiState,
    onMonthChanged: (YearMonth) -> Unit,
    onDateTap: (LocalDate) -> Unit,
    onEditSelectedDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val firstDayOfWeek = DayOfWeek.SUNDAY
    val weekDays = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy MMM", locale) }
    val calendarState = rememberCalendarState(
        startMonth = uiState.calendarStartMonth,
        endMonth = uiState.calendarEndMonth,
        firstVisibleMonth = uiState.currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )
    var pendingMonth by remember { mutableStateOf<YearMonth?>(null) }

    LaunchedEffect(calendarState) {
        snapshotFlow { calendarState.firstVisibleMonth.yearMonth }
            .distinctUntilChanged()
            .collect(onMonthChanged)
    }

    LaunchedEffect(uiState.currentMonth) {
        if (calendarState.firstVisibleMonth.yearMonth != uiState.currentMonth) {
            pendingMonth = uiState.currentMonth
        }
    }

    LaunchedEffect(pendingMonth) {
        val target = pendingMonth ?: return@LaunchedEffect
        calendarState.animateScrollToMonth(target)
        pendingMonth = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val target = uiState.currentMonth.minusMonths(1)
                    if (target >= uiState.calendarStartMonth) {
                        pendingMonth = target
                        onMonthChanged(target)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.calendar_prev_month)
                )
            }
            Text(
                text = uiState.currentMonth.format(monthFormatter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val target = uiState.currentMonth.plusMonths(1)
                    if (target <= uiState.calendarEndMonth) {
                        pendingMonth = target
                        onMonthChanged(target)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.calendar_next_month)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDays.forEach { day ->
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, locale),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalCalendar(
                    state = calendarState,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    dayContent = { day ->
                        DayCell(
                            day = day,
                            selectedDate = uiState.selectedDate,
                            hasRecord = uiState.recordsByDate.containsKey(day.date),
                            onTap = onDateTap
                        )
                    }
                )
            }
        }

        SummaryCard(
            selectedDate = uiState.selectedDate,
            record = uiState.selectedRecord,
            onEditSelectedDate = onEditSelectedDate,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    selectedDate: LocalDate,
    hasRecord: Boolean,
    onTap: (LocalDate) -> Unit
) {
    val isSelected = day.date == selectedDate
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val textColor = when {
        !isCurrentMonth -> MaterialTheme.colorScheme.outline
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isCurrentMonth) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isCurrentMonth) { onTap(day.date) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasRecord) {
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                    )
            )
        }
    }
}

@Composable
private fun SummaryCard(
    selectedDate: LocalDate,
    record: Record?,
    onEditSelectedDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_summary_title, selectedDate.toString()),
                style = MaterialTheme.typography.titleMedium
            )
            if (record == null) {
                Text(
                    text = stringResource(R.string.calendar_summary_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SummaryRow(
                    label = stringResource(R.string.record_breakfast_label),
                    value = record.breakfast.orEmpty().ifBlank { "-" }
                )
                SummaryRow(
                    label = stringResource(R.string.record_lunch_label),
                    value = record.lunch.orEmpty().ifBlank { "-" }
                )
                SummaryRow(
                    label = stringResource(R.string.record_dinner_label),
                    value = record.dinner.orEmpty().ifBlank { "-" }
                )
                SummaryRow(
                    label = stringResource(R.string.record_condition_memo_label),
                    value = record.conditionMemo.orEmpty().ifBlank { "-" }
                )
            }
            Button(
                onClick = onEditSelectedDate,
                modifier = Modifier.testTag("calendar_edit_selected_date_button")
            ) {
                Text(text = stringResource(R.string.calendar_edit_selected_date))
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium
    )
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    val selectedDate = LocalDate.of(2026, 2, 7)
    val previewRecord = Record(
        id = DyphicId.dateToId(selectedDate),
        breakfast = "Toast",
        lunch = "Pasta",
        dinner = "Salad",
        isToilet = true,
        condition = "Good",
        conditionMemo = "No issues",
        stepCount = 8500,
        healthKcal = 420.5,
        ringfitKcal = 110.0,
        ringfitKm = 2.3
    )
    val previewState = CalendarUiState(
        isLoading = false,
        errorMessage = null,
        calendarStartMonth = YearMonth.of(2025, 1),
        calendarEndMonth = YearMonth.of(2027, 12),
        currentMonth = YearMonth.from(selectedDate),
        selectedDate = selectedDate,
        recordsByDate = mapOf(selectedDate to previewRecord)
    )

    SimpleDyphicTheme {
        CalendarScreen(
            uiState = previewState,
            onRetry = {},
            onMonthChanged = {},
            onDateTap = {},
            onEditSelectedDate = {}
        )
    }
}
