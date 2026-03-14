package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecordEntity::class, WeeklyGoalEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun weeklyGoalDao(): WeeklyGoalDao

    companion object {
        const val DATABASE_NAME: String = "simpledyphic.db"
    }
}
