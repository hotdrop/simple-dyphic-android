package jp.hotdrop.simpledyphic.data.repository

import jp.hotdrop.simpledyphic.data.local.RoomGoalLocalDataSource
import jp.hotdrop.simpledyphic.model.AppCompletable
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import jp.hotdrop.simpledyphic.model.appCompletableSuspend
import jp.hotdrop.simpledyphic.model.appResultSuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val localDataSource: RoomGoalLocalDataSource
) {

    fun observeWeeklyGoals(): Flow<AppResult<List<WeeklyGoal>>> {
        return localDataSource.observeAll()
            .map { storedGoals ->
                AppResult.Success(mergeWithDefaults(storedGoals)) as AppResult<List<WeeklyGoal>>
            }
            .catch { error -> emit(AppResult.Failure(error)) }
    }

    suspend fun getWeeklyGoals(): AppResult<List<WeeklyGoal>> {
        return appResultSuspend {
            mergeWithDefaults(localDataSource.findAll())
        }
    }

    suspend fun saveWeeklyGoal(goal: WeeklyGoal): AppCompletable {
        return appCompletableSuspend {
            localDataSource.save(goal)
        }
    }

    private fun mergeWithDefaults(storedGoals: List<WeeklyGoal>): List<WeeklyGoal> {
        val map = storedGoals.associateBy { it.metricType }
        return defaultGoals().map { default -> map[default.metricType] ?: default }
    }

    private fun defaultGoals(): List<WeeklyGoal> {
        return listOf(
            WeeklyGoal(metricType = HealthMetricType.STEP_COUNT, targetValue = 70_000.0),
            WeeklyGoal(metricType = HealthMetricType.ACTIVE_KCAL, targetValue = 2_100.0),
            WeeklyGoal(metricType = HealthMetricType.EXERCISE_MINUTES, targetValue = 150.0),
            WeeklyGoal(metricType = HealthMetricType.DISTANCE_KM, targetValue = 21.0)
        )
    }
}
