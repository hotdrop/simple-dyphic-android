package jp.hotdrop.simpledyphic.ui.calendar

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import java.time.LocalDate
import javax.inject.Inject
import jp.hotdrop.simpledyphic.data.repository.GoalRepository
import jp.hotdrop.simpledyphic.data.repository.InsightRepository
import jp.hotdrop.simpledyphic.data.repository.RecordRepository
import jp.hotdrop.simpledyphic.domain.usecase.WeeklyGoalProgressUseCase
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.model.WeeklyConditionActivityInsight
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import jp.hotdrop.simpledyphic.model.WeeklyGoalSummary
import kotlinx.coroutines.flow.Flow

interface CalendarRecordObserver {
    fun observeAll(): Flow<AppResult<List<Record>>>
}

interface CalendarWeeklyGoalObserver {
    fun observeWeeklyGoals(): Flow<AppResult<List<WeeklyGoal>>>
}

interface CalendarWeeklyDataLoader {
    suspend fun loadGoalSummary(anchorDate: LocalDate): AppResult<WeeklyGoalSummary>
    suspend fun loadInsight(anchorDate: LocalDate): AppResult<WeeklyConditionActivityInsight>
}

class RepositoryCalendarRecordObserver @Inject constructor(
    private val recordRepository: RecordRepository
) : CalendarRecordObserver {
    override fun observeAll(): Flow<AppResult<List<Record>>> = recordRepository.observeAll()
}

class RepositoryCalendarWeeklyGoalObserver @Inject constructor(
    private val goalRepository: GoalRepository
) : CalendarWeeklyGoalObserver {
    override fun observeWeeklyGoals(): Flow<AppResult<List<WeeklyGoal>>> =
        goalRepository.observeWeeklyGoals()
}

class RepositoryCalendarWeeklyDataLoader @Inject constructor(
    private val weeklyGoalProgressUseCase: WeeklyGoalProgressUseCase,
    private val insightRepository: InsightRepository
) : CalendarWeeklyDataLoader {
    override suspend fun loadGoalSummary(anchorDate: LocalDate): AppResult<WeeklyGoalSummary> =
        weeklyGoalProgressUseCase.execute(anchorDate)

    override suspend fun loadInsight(anchorDate: LocalDate): AppResult<WeeklyConditionActivityInsight> =
        insightRepository.buildConditionActivityInsight(anchorDate)
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class CalendarDependencyModule {
    @Binds
    abstract fun bindCalendarRecordObserver(
        impl: RepositoryCalendarRecordObserver
    ): CalendarRecordObserver

    @Binds
    abstract fun bindCalendarWeeklyGoalObserver(
        impl: RepositoryCalendarWeeklyGoalObserver
    ): CalendarWeeklyGoalObserver

    @Binds
    abstract fun bindCalendarWeeklyDataLoader(
        impl: RepositoryCalendarWeeklyDataLoader
    ): CalendarWeeklyDataLoader
}
