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

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                DELETE FROM `weekly_goals`
                WHERE `metricType` = 'FLOORS_CLIMBED'
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `app_settings` (
                    `id` INTEGER NOT NULL,
                    `birthDate` TEXT,
                    `heightCm` REAL,
                    `weightKg` REAL,
                    `advisorPrompt` TEXT NOT NULL,
                    `modelFilePath` TEXT,
                    `modelDisplayName` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }
}
