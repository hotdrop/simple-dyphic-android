package jp.hotdrop.simpledyphic.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import jp.hotdrop.simpledyphic.data.repository.GoalRepository
import jp.hotdrop.simpledyphic.data.repository.HealthConnectRepository
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.WeekRangeCalculator
import jp.hotdrop.simpledyphic.model.WeeklyGoalSummary

class WeeklyGoalProgressUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val healthConnectRepository: HealthConnectRepository
) {

    suspend fun execute(anchorDate: LocalDate): AppResult<WeeklyGoalSummary> {
        val weekRange = WeekRangeCalculator.mondayStart(anchorDate)

        val goals = when (val result = goalRepository.getWeeklyGoals()) {
            is AppResult.Success -> result.value.filter { it.enabled }
            is AppResult.Failure -> return result
        }

        val weekMetrics = when (
            val result = healthConnectRepository.readRangeMetrics(
                start = weekRange.startDate,
                end = weekRange.endDate
            )
        ) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }

        val progresses = WeeklyGoalProgressCalculator.calculate(
            goals = goals,
            weekMetrics = weekMetrics
        )

        return AppResult.Success(
            WeeklyGoalSummary(
                weekRange = weekRange,
                progresses = progresses
            )
        )
    }
}
