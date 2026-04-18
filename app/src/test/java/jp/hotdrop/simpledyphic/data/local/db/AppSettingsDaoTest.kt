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
class AppSettingsDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var appSettingsDao: AppSettingsDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appSettingsDao = database.appSettingsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndFindById_returnsLatestSettings() = runTest {
        val initial = AppSettingsEntity(
            birthDate = "1994-01-10",
            heightCm = 168.0,
            weightKg = 58.0,
            advisorPrompt = "prompt1",
            modelFilePath = "/tmp/one.litertlm",
            modelDisplayName = "one"
        )
        val updated = initial.copy(
            weightKg = 57.5,
            advisorPrompt = "prompt2",
            modelDisplayName = "two"
        )

        appSettingsDao.upsert(initial)
        appSettingsDao.upsert(updated)

        assertEquals(updated, appSettingsDao.findById())
    }
}
