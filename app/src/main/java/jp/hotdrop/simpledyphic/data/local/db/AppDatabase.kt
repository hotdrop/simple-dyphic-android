package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        const val DATABASE_NAME: String = "simpledyphic.db"
    }
}
