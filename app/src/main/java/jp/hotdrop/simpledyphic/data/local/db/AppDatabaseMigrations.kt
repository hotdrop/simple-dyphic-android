package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `weekly_goals` (
                    `metricType` TEXT NOT NULL,
                    `targetValue` REAL NOT NULL,
                    `weekStartsOnMonday` INTEGER NOT NULL,
                    `enabled` INTEGER NOT NULL,
                    PRIMARY KEY(`metricType`)
                )
                """.trimIndent()
            )
        }
    }
}
