package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyGoalDao {
    @Query("SELECT * FROM weekly_goals ORDER BY metricType ASC")
    suspend fun findAll(): List<WeeklyGoalEntity>

    @Query("SELECT * FROM weekly_goals ORDER BY metricType ASC")
    fun observeAll(): Flow<List<WeeklyGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: WeeklyGoalEntity)
}
