package jp.hotdrop.simpledyphic.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.ui.components.ConditionIcon
import jp.hotdrop.simpledyphic.ui.components.ErrorContent
import jp.hotdrop.simpledyphic.ui.components.LoadingContent
import jp.hotdrop.simpledyphic.model.ConditionType
import jp.hotdrop.simpledyphic.model.DyphicId
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.model.WeeklyGoalMetricProgress
import jp.hotdrop.simpledyphic.model.WeeklyGoalProgress
import jp.hotdrop.simpledyphic.model.WeeklyMetricInsight
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

    LaunchedEffect(recordUpdated) {
        if (recordUpdated) {
            viewModel.onRecordUpdated()
            onRecordUpdatedConsumed()
        }
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
        uiState.errorMessageResId != null -> ErrorContent(
            message = stringResource(uiState.errorMessageResId),
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
    val monthFormatPattern = stringResource(R.string.calendar_month_format_pattern)
    val monthFormatter = remember(monthFormatPattern, locale) {
        DateTimeFormatter.ofPattern(monthFormatPattern, locale)
    }
    val initialVisibleMonth = remember(uiState.calendarStartMonth, uiState.calendarEndMonth) {
        uiState.currentMonth
    }
    val calendarState = rememberCalendarState(
        startMonth = uiState.calendarStartMonth,
        endMonth = uiState.calendarEndMonth,
        firstVisibleMonth = initialVisibleMonth,
        firstDayOfWeek = firstDayOfWeek
    )
    val today = LocalDate.now()
    var pendingMonth by remember { mutableStateOf<YearMonth?>(null) }

    LaunchedEffect(calendarState) {
        snapshotFlow { calendarState.firstVisibleMonth.yearMonth }
            .distinctUntilChanged()
            .collect(onMonthChanged)
    }

    LaunchedEffect(uiState.currentMonth) {
        val visibleMonth = calendarState.firstVisibleMonth.yearMonth
        if (pendingMonth == null && visibleMonth != uiState.currentMonth) {
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
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
                        val dayRecord = uiState.recordsByDate[day.date]
                        DayCell(
                            day = day,
                            selectedDate = uiState.selectedDate,
                            today = today,
                            record = dayRecord,
                            hasMarkers = day.date in uiState.datesWithMarkers,
                            onTap = { date ->
                                val monthOfDate = YearMonth.from(date)
                                if (monthOfDate != uiState.currentMonth) {
                                    pendingMonth = monthOfDate
                                }
                                onDateTap(date)
                            }
                        )
                    }
                )
            }
        }

        SummaryCard(
            selectedDate = uiState.selectedDate,
            record = uiState.selectedRecord,
            onEditSelectedDate = onEditSelectedDate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        WeeklyDashboardCard(
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        WeeklyInsightCard(
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    selectedDate: LocalDate,
    today: LocalDate,
    record: Record?,
    hasMarkers: Boolean,
    onTap: (LocalDate) -> Unit
) {
    val isSelected = day.date == selectedDate
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == today
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimary
        !isCurrentMonth -> Color.Transparent
        else -> MaterialTheme.colorScheme.onSurface
    }
    val backgroundColor = when {
        isSelected -> Color(0xFF66CCFF)
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .semantics { this.selected = isSelected }
            .let { base ->
                if (isCurrentMonth) {
                    base.clickableWithRole { onTap(day.date) }
                } else {
                    base
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected || isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.96f)
                    .clip(CircleShape)
                    .background(backgroundColor)
            )
        }

        Text(
            text = if (isCurrentMonth) day.date.dayOfMonth.toString() else "",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )

        if (isCurrentMonth && hasMarkers) {
            RecordMarkers(
                record = record,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RecordMarkers(
    record: Record?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            MarkerSymbol(symbolResId = if (record?.isToilet == true) R.string.calendar_marker_toilet else null)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ConditionMarker(conditionType = record?.condition)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            ActivityMarker(record = record)
        }
    }
}

@Composable
private fun MarkerSymbol(symbolResId: Int?) {
    Box(
        modifier = Modifier.size(15.dp),
        contentAlignment = Alignment.Center
    ) {
        if (symbolResId != null) {
            Text(
                text = stringResource(symbolResId),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ActivityMarker(record: Record?) {
    Box(
        modifier = Modifier.size(15.dp),
        contentAlignment = Alignment.Center
    ) {
        if (record == null) return
        when {
            (record.ringfitKcal ?: 0.0) > 0.0 || (record.ringfitKm ?: 0.0) > 0.0 -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_ringfit),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
            }
            (record.stepCount ?: 0) >= 7000 -> {
                Text(
                    text = stringResource(R.string.calendar_marker_walk),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ConditionMarker(conditionType: ConditionType?) {
    val resolved = conditionType ?: return
    ConditionIcon(
        type = resolved,
        selected = true,
        modifier = Modifier.size(15.dp)
    )
}

private fun Modifier.clickableWithRole(onClick: () -> Unit): Modifier =
    clickable(
        onClick = onClick,
        role = Role.Button
    )

@Composable
private fun WeeklyDashboardCard(
    uiState: CalendarUiState,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val periodFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("M/d", locale)
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_weekly_dashboard_title),
                style = MaterialTheme.typography.titleMedium
            )
            val start = uiState.weeklyStartDate
            val end = uiState.weeklyEndDate
            if (start != null && end != null) {
                Text(
                    text = stringResource(
                        R.string.calendar_weekly_dashboard_period,
                        start.format(periodFormatter),
                        end.format(periodFormatter)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                uiState.isWeeklyLoading -> {
                    Text(text = stringResource(R.string.calendar_weekly_loading))
                }

                uiState.weeklyErrorMessageResId != null -> {
                    Text(
                        text = stringResource(uiState.weeklyErrorMessageResId),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {
                    uiState.weeklyGoalProgresses.forEachIndexed { index, progress ->
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        WeeklyGoalProgressRow(progress = progress)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyGoalProgressRow(
    progress: WeeklyGoalMetricProgress
) {
    val metricType = progress.progress.metricType
    val metricName = stringResource(metricNameResId(metricType))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = metricName,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(
                R.string.calendar_weekly_goal_value,
                formatMetricValue(metricType, progress.progress.targetValue)
            ),
            style = MaterialTheme.typography.bodySmall
        )
        when (progress.availability) {
            MetricAvailability.AVAILABLE -> {
                Text(
                    text = stringResource(
                        R.string.calendar_weekly_actual_value,
                        formatMetricValue(metricType, progress.progress.actualValue)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(
                        R.string.calendar_weekly_rate_value,
                        progress.progress.achievementRate
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            MetricAvailability.PERMISSION_MISSING -> {
                Text(
                    text = stringResource(R.string.calendar_weekly_not_available_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            MetricAvailability.SOURCE_UNAVAILABLE -> {
                Text(
                    text = stringResource(R.string.calendar_weekly_not_available_source),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WeeklyInsightCard(
    uiState: CalendarUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_weekly_insight_title),
                style = MaterialTheme.typography.titleMedium
            )
            when {
                uiState.isWeeklyLoading -> {
                    Text(text = stringResource(R.string.calendar_weekly_loading))
                }

                uiState.weeklyErrorMessageResId != null -> {
                    Text(
                        text = stringResource(uiState.weeklyErrorMessageResId),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                !uiState.hasBadConditionDaysInWeek -> {
                    Text(
                        text = stringResource(R.string.calendar_weekly_insight_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    uiState.weeklyInsights.forEachIndexed { index, insight ->
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        WeeklyInsightRow(insight = insight)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyInsightRow(insight: WeeklyMetricInsight) {
    val metricType = insight.metricType
    val prevText = insight.deltaFromPreviousWeek?.let {
        formatSignedMetricValue(metricType, it)
    } ?: stringResource(R.string.calendar_weekly_insight_data_insufficient)

    val avgText = insight.deltaFromWeekAverage?.let {
        formatSignedMetricValue(metricType, it)
    } ?: stringResource(R.string.calendar_weekly_insight_data_insufficient)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(metricNameResId(metricType)),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = insight.comment,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.calendar_weekly_insight_vs_previous, prevText),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.calendar_weekly_insight_vs_average, avgText),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun formatMetricValue(metricType: HealthMetricType, value: Double): String {
    return when (metricType) {
        HealthMetricType.STEP_COUNT -> String.format(
            Locale.getDefault(),
            "%.0f %s",
            value,
            stringResource(R.string.health_metric_unit_step)
        )

        HealthMetricType.ACTIVE_KCAL -> String.format(
            Locale.getDefault(),
            "%.1f %s",
            value,
            stringResource(R.string.health_metric_unit_kcal)
        )

        HealthMetricType.EXERCISE_MINUTES -> String.format(
            Locale.getDefault(),
            "%.0f %s",
            value,
            stringResource(R.string.health_metric_unit_minute)
        )

        HealthMetricType.DISTANCE_KM -> String.format(
            Locale.getDefault(),
            "%.1f %s",
            value,
            stringResource(R.string.health_metric_unit_km)
        )
    }
}

@Composable
private fun formatSignedMetricValue(metricType: HealthMetricType, value: Double): String {
    return when (metricType) {
        HealthMetricType.STEP_COUNT -> String.format(
            Locale.getDefault(),
            "%+.0f %s",
            value,
            stringResource(R.string.health_metric_unit_step)
        )

        HealthMetricType.ACTIVE_KCAL -> String.format(
            Locale.getDefault(),
            "%+.1f %s",
            value,
            stringResource(R.string.health_metric_unit_kcal)
        )

        HealthMetricType.EXERCISE_MINUTES -> String.format(
            Locale.getDefault(),
            "%+.0f %s",
            value,
            stringResource(R.string.health_metric_unit_minute)
        )

        HealthMetricType.DISTANCE_KM -> String.format(
            Locale.getDefault(),
            "%+.1f %s",
            value,
            stringResource(R.string.health_metric_unit_km)
        )
    }
}

private fun metricNameResId(metricType: HealthMetricType): Int {
    return when (metricType) {
        HealthMetricType.STEP_COUNT -> R.string.health_metric_step_count
        HealthMetricType.ACTIVE_KCAL -> R.string.health_metric_active_kcal
        HealthMetricType.EXERCISE_MINUTES -> R.string.health_metric_exercise_minutes
        HealthMetricType.DISTANCE_KM -> R.string.health_metric_distance_km
    }
}

@Composable
private fun SummaryCard(
    selectedDate: LocalDate,
    record: Record?,
    onEditSelectedDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val summaryDateFormatPattern = stringResource(R.string.calendar_summary_date_format_pattern)
    val summaryDateFormatter = remember(summaryDateFormatPattern, locale) {
        DateTimeFormatter.ofPattern(summaryDateFormatPattern, locale)
    }

    Card(
        modifier = modifier
            .clickable(onClick = onEditSelectedDate),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = selectedDate.format(summaryDateFormatter),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            HorizontalDivider()
            if (record == null) {
                Text(
                    text = stringResource(R.string.calendar_summary_empty_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val memo = record.conditionMemo.orEmpty()
                if (memo.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.calendar_summary_memo_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = memo,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = stringResource(R.string.calendar_summary_empty_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
        condition = ConditionType.GOOD,
        conditionMemo = "No issues",
        stepCount = 8500,
        healthKcal = 420.5,
        ringfitKcal = 110.0,
        ringfitKm = 2.3
    )
    val previewState = CalendarUiState(
        isLoading = false,
        errorMessageResId = null,
        calendarStartMonth = YearMonth.of(2025, 1),
        calendarEndMonth = YearMonth.of(2027, 12),
        currentMonth = YearMonth.from(selectedDate),
        selectedDate = selectedDate,
        recordsByDate = mapOf(selectedDate to previewRecord),
        datesWithMarkers = setOf(selectedDate)
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

@Preview(showBackground = true, name = "Weekly Dashboard")
@Composable
private fun WeeklyDashboardCardPreview() {
    WeeklyDashboardCardPreviewContent(
        uiState = previewWeeklyDashboardUiState()
    )
}

@Preview(showBackground = true, name = "Weekly Dashboard Loading")
@Composable
private fun WeeklyDashboardCardLoadingPreview() {
    WeeklyDashboardCardPreviewContent(
        uiState = previewWeeklyDashboardUiState().copy(
            isWeeklyLoading = true,
            weeklyGoalProgresses = emptyList()
        )
    )
}

@Preview(showBackground = true, name = "Weekly Dashboard Error")
@Composable
private fun WeeklyDashboardCardErrorPreview() {
    WeeklyDashboardCardPreviewContent(
        uiState = previewWeeklyDashboardUiState().copy(
            weeklyErrorMessageResId = android.R.string.unknownName,
            weeklyGoalProgresses = emptyList()
        )
    )
}

@Composable
private fun WeeklyDashboardCardPreviewContent(
    uiState: CalendarUiState
) {
    SimpleDyphicTheme {
        WeeklyDashboardCard(
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

private fun previewWeeklyDashboardUiState(): CalendarUiState {
    val selectedDate = LocalDate.of(2026, 2, 7)
    return CalendarUiState(
        isLoading = false,
        currentMonth = YearMonth.from(selectedDate),
        selectedDate = selectedDate,
        isWeeklyLoading = false,
        weeklyStartDate = LocalDate.of(2026, 2, 2),
        weeklyEndDate = LocalDate.of(2026, 2, 8),
        weeklyGoalProgresses = listOf(
            WeeklyGoalMetricProgress(
                progress = WeeklyGoalProgress(
                    metricType = HealthMetricType.STEP_COUNT,
                    targetValue = 56_000.0,
                    actualValue = 48_320.0,
                    achievementRate = 86.3
                ),
                availability = MetricAvailability.AVAILABLE
            ),
            WeeklyGoalMetricProgress(
                progress = WeeklyGoalProgress(
                    metricType = HealthMetricType.ACTIVE_KCAL,
                    targetValue = 2_100.0,
                    actualValue = 1_980.5,
                    achievementRate = 94.3
                ),
                availability = MetricAvailability.AVAILABLE
            ),
            WeeklyGoalMetricProgress(
                progress = WeeklyGoalProgress(
                    metricType = HealthMetricType.EXERCISE_MINUTES,
                    targetValue = 180.0,
                    actualValue = 0.0,
                    achievementRate = 0.0
                ),
                availability = MetricAvailability.PERMISSION_MISSING
            ),
            WeeklyGoalMetricProgress(
                progress = WeeklyGoalProgress(
                    metricType = HealthMetricType.DISTANCE_KM,
                    targetValue = 21.0,
                    actualValue = 0.0,
                    achievementRate = 0.0
                ),
                availability = MetricAvailability.SOURCE_UNAVAILABLE
            )
        )
    )
}
