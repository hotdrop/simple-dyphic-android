package jp.hotdrop.simpledyphic.ui.calendar

import java.time.LocalDate
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.model.WeekRange
import jp.hotdrop.simpledyphic.model.WeeklyConditionActivityInsight
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import jp.hotdrop.simpledyphic.model.WeeklyGoalMetricProgress
import jp.hotdrop.simpledyphic.model.WeeklyGoalProgress
import jp.hotdrop.simpledyphic.model.WeeklyGoalSummary
import jp.hotdrop.simpledyphic.model.WeeklyMetricInsight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_skipsInitialGoalEmissionAndLoadsWeeklyDataOnce() = runTest(mainDispatcherRule.dispatcher) {
        val weeklyGoalsFlow = MutableSharedFlow<AppResult<List<WeeklyGoal>>>(replay = 1).apply {
            tryEmit(AppResult.Success(emptyList()))
        }
        val weeklyDataLoader = FakeCalendarWeeklyDataLoader()

        CalendarViewModel(
            recordObserver = FakeCalendarRecordObserver(),
            weeklyGoalObserver = FakeCalendarWeeklyGoalObserver(listOf(weeklyGoalsFlow)),
            weeklyDataLoader = weeklyDataLoader
        )

        advanceUntilIdle()
        awaitCondition { weeklyDataLoader.goalSummaryRequests.size == 1 }
        awaitCondition { weeklyDataLoader.insightRequests.size == 1 }

        assertEquals(1, weeklyDataLoader.goalSummaryRequests.size)
        assertEquals(1, weeklyDataLoader.insightRequests.size)
    }

    @Test
    fun onRetry_restartsGoalObservationAndLaterGoalChangesReloadProgressOnly() = runTest(mainDispatcherRule.dispatcher) {
        val firstObservation = MutableSharedFlow<AppResult<List<WeeklyGoal>>>()
        val retriedObservation = MutableSharedFlow<AppResult<List<WeeklyGoal>>>()
        val weeklyDataLoader = FakeCalendarWeeklyDataLoader()
        val viewModel = CalendarViewModel(
            recordObserver = FakeCalendarRecordObserver(),
            weeklyGoalObserver = FakeCalendarWeeklyGoalObserver(
                listOf(firstObservation, retriedObservation)
            ),
            weeklyDataLoader = weeklyDataLoader
        )

        advanceUntilIdle()
        awaitCondition { weeklyDataLoader.goalSummaryRequests.size == 1 }
        awaitCondition { weeklyDataLoader.insightRequests.size == 1 }

        firstObservation.emit(AppResult.Failure(IllegalStateException("goal observation failure")))
        advanceUntilIdle()
        awaitCondition {
            viewModel.uiState.value.weeklyErrorMessageResId == R.string.calendar_weekly_error_load
        }
        assertEquals(R.string.calendar_weekly_error_load, viewModel.uiState.value.weeklyErrorMessageResId)

        viewModel.onRetry()
        advanceUntilIdle()
        awaitCondition { weeklyDataLoader.goalSummaryRequests.size == 2 }
        awaitCondition { weeklyDataLoader.insightRequests.size == 2 }

        assertEquals(2, weeklyDataLoader.goalSummaryRequests.size)
        assertEquals(2, weeklyDataLoader.insightRequests.size)

        retriedObservation.emit(AppResult.Success(emptyList()))
        advanceUntilIdle()
        Thread.sleep(50)
        assertEquals(2, weeklyDataLoader.goalSummaryRequests.size)
        assertEquals(2, weeklyDataLoader.insightRequests.size)

        retriedObservation.emit(AppResult.Success(emptyList()))
        advanceUntilIdle()
        awaitCondition { weeklyDataLoader.goalSummaryRequests.size == 3 }
        assertEquals(3, weeklyDataLoader.goalSummaryRequests.size)
        assertEquals(2, weeklyDataLoader.insightRequests.size)
    }

    @Test
    fun weeklyLoadFailure_clearsStaleWeeklyState() = runTest(mainDispatcherRule.dispatcher) {
        val weeklyGoalsFlow = MutableSharedFlow<AppResult<List<WeeklyGoal>>>(replay = 1).apply {
            tryEmit(AppResult.Success(emptyList()))
        }
        val weeklyDataLoader = FakeCalendarWeeklyDataLoader()
        val viewModel = CalendarViewModel(
            recordObserver = FakeCalendarRecordObserver(),
            weeklyGoalObserver = FakeCalendarWeeklyGoalObserver(listOf(weeklyGoalsFlow)),
            weeklyDataLoader = weeklyDataLoader
        )

        advanceUntilIdle()
        awaitCondition { viewModel.uiState.value.weeklyGoalProgresses.isNotEmpty() }
        awaitCondition { viewModel.uiState.value.weeklyInsights.isNotEmpty() }
        assertTrue(viewModel.uiState.value.weeklyGoalProgresses.isNotEmpty())
        assertTrue(viewModel.uiState.value.weeklyInsights.isNotEmpty())

        weeklyDataLoader.nextGoalSummaryResult = AppResult.Failure(IllegalStateException("load failure"))
        viewModel.onDaySelected(LocalDate.of(2026, 3, 25))
        advanceUntilIdle()
        awaitCondition {
            viewModel.uiState.value.weeklyErrorMessageResId == R.string.calendar_weekly_error_load
        }

        val state = viewModel.uiState.value
        assertEquals(R.string.calendar_weekly_error_load, state.weeklyErrorMessageResId)
        assertNull(state.weeklyStartDate)
        assertNull(state.weeklyEndDate)
        assertTrue(state.weeklyGoalProgresses.isEmpty())
        assertTrue(state.weeklyInsights.isEmpty())
        assertFalse(state.hasBadConditionDaysInWeek)
    }

    private class FakeCalendarRecordObserver : CalendarRecordObserver {
        override fun observeAll(): Flow<AppResult<List<Record>>> =
            flowOf(AppResult.Success(emptyList()))
    }

    private class FakeCalendarWeeklyGoalObserver(
        private val observations: List<Flow<AppResult<List<WeeklyGoal>>>>
    ) : CalendarWeeklyGoalObserver {
        private var index = 0

        override fun observeWeeklyGoals(): Flow<AppResult<List<WeeklyGoal>>> {
            val current = observations[index]
            index += 1
            return current
        }
    }

    private class FakeCalendarWeeklyDataLoader : CalendarWeeklyDataLoader {
        val goalSummaryRequests = mutableListOf<LocalDate>()
        val insightRequests = mutableListOf<LocalDate>()
        var nextGoalSummaryResult: AppResult<WeeklyGoalSummary>? = null
        var nextInsightResult: AppResult<WeeklyConditionActivityInsight>? = null

        override suspend fun loadGoalSummary(anchorDate: LocalDate): AppResult<WeeklyGoalSummary> {
            goalSummaryRequests += anchorDate
            return nextGoalSummaryResult.also { nextGoalSummaryResult = null } ?: AppResult.Success(
                WeeklyGoalSummary(
                    weekRange = WeekRange(
                        startDate = anchorDate.minusDays(1),
                        endDate = anchorDate.plusDays(5)
                    ),
                    progresses = listOf(
                        WeeklyGoalMetricProgress(
                            progress = WeeklyGoalProgress(
                                metricType = HealthMetricType.STEP_COUNT,
                                targetValue = 70000.0,
                                actualValue = 35000.0,
                                achievementRate = 50.0
                            ),
                            availability = MetricAvailability.AVAILABLE
                        )
                    )
                )
            )
        }

        override suspend fun loadInsight(anchorDate: LocalDate): AppResult<WeeklyConditionActivityInsight> {
            insightRequests += anchorDate
            return nextInsightResult.also { nextInsightResult = null } ?: AppResult.Success(
                WeeklyConditionActivityInsight(
                    weekRange = WeekRange(
                        startDate = anchorDate.minusDays(1),
                        endDate = anchorDate.plusDays(5)
                    ),
                    badConditionDates = listOf(anchorDate),
                    metricInsights = listOf(
                        WeeklyMetricInsight(
                            metricType = HealthMetricType.STEP_COUNT,
                            deltaFromPreviousWeek = -1000.0,
                            deltaFromWeekAverage = -500.0,
                            comment = "insight"
                        )
                    )
                )
            )
        }
    }

    private fun awaitCondition(condition: () -> Boolean) {
        repeat(50) {
            if (condition()) {
                return
            }
            Thread.sleep(20)
        }
        fail("Condition was not met within timeout")
    }
}
