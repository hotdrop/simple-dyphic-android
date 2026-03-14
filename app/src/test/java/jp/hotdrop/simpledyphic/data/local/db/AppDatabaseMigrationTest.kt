package jp.hotdrop.simpledyphic.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    @Test
    fun migrate1To2_createsWeeklyGoalsTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-test-${System.currentTimeMillis()}.db"

        createVersion1Database(context, dbName)

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabaseMigrations.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        migratedDb.openHelper.writableDatabase.query(
            SimpleSQLiteQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='weekly_goals'")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("weekly_goals", cursor.getString(0))
        }

        migratedDb.close()
        File(context.getDatabasePath(dbName).absolutePath).delete()
    }

    @Test
    fun migrate2To3_removesLegacyFloorsClimbedGoal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-test-${System.currentTimeMillis()}.db"

        createVersion2Database(context, dbName)
        insertLegacyWeeklyGoal(context, dbName)

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabaseMigrations.MIGRATION_1_2,
                AppDatabaseMigrations.MIGRATION_2_3
            )
            .allowMainThreadQueries()
            .build()

        migratedDb.openHelper.writableDatabase.query(
            SimpleSQLiteQuery(
                "SELECT COUNT(*) FROM weekly_goals WHERE metricType = 'FLOORS_CLIMBED'"
            )
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }

        migratedDb.close()
        File(context.getDatabasePath(dbName).absolutePath).delete()
    }

    private fun createVersion1Database(context: Context, dbName: String) {
        val db = Room.databaseBuilder(context, V1AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        db.close()
    }

    private fun createVersion2Database(context: Context, dbName: String) {
        val db = Room.databaseBuilder(context, V2AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        db.close()
    }

    private fun insertLegacyWeeklyGoal(context: Context, dbName: String) {
        val db = Room.databaseBuilder(context, V2AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO weekly_goals(metricType, targetValue, weekStartsOnMonday, enabled)
            VALUES('FLOORS_CLIMBED', 35.0, 1, 1)
            """.trimIndent()
        )
        db.close()
    }

    @Database(
        entities = [RecordEntity::class],
        version = 1,
        exportSchema = false
    )
    abstract class V1AppDatabase : RoomDatabase()

    @Database(
        entities = [RecordEntity::class, WeeklyGoalEntity::class],
        version = 2,
        exportSchema = false
    )
    abstract class V2AppDatabase : RoomDatabase()
}
