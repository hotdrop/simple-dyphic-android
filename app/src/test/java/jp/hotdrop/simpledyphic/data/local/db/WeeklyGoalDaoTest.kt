package jp.hotdrop.simpledyphic.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeeklyGoalDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var weeklyGoalDao: WeeklyGoalDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addMigrations(AppDatabaseMigrations.MIGRATION_1_2)
            .build()
        weeklyGoalDao = database.weeklyGoalDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndFindAll_returnsSavedGoals() = runTest {
        val stepGoal = WeeklyGoalEntity(
            metricType = "STEP_COUNT",
            targetValue = 70000.0,
            weekStartsOnMonday = true,
            enabled = true
        )
        val kcalGoal = WeeklyGoalEntity(
            metricType = "ACTIVE_KCAL",
            targetValue = 2100.0,
            weekStartsOnMonday = true,
            enabled = true
        )

        weeklyGoalDao.upsert(stepGoal)
        weeklyGoalDao.upsert(kcalGoal)

        val actual = weeklyGoalDao.findAll()
        assertEquals(listOf(kcalGoal, stepGoal), actual)
    }
}
