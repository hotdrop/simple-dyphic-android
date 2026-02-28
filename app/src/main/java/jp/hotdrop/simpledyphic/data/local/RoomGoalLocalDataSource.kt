package jp.hotdrop.simpledyphic.data.local

import jp.hotdrop.simpledyphic.data.local.db.WeeklyGoalDao
import jp.hotdrop.simpledyphic.data.local.db.toEntity
import jp.hotdrop.simpledyphic.data.local.db.toModel
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomGoalLocalDataSource @Inject constructor(
    private val weeklyGoalDao: WeeklyGoalDao
) {
    suspend fun findAll(): List<WeeklyGoal> {
        return weeklyGoalDao.findAll().map { it.toModel() }
    }

    fun observeAll(): Flow<List<WeeklyGoal>> {
        return weeklyGoalDao.observeAll().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun save(goal: WeeklyGoal) {
        weeklyGoalDao.upsert(goal.toEntity())
    }
}
