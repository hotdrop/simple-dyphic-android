package jp.hotdrop.simpledyphic.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var recordDao: RecordDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recordDao = database.recordDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndFindById_returnsSavedRecord() = runTest {
        val record = testEntity(id = 20260207, breakfast = "egg")

        recordDao.upsert(record)

        val actual = recordDao.findById(20260207)
        assertNotNull(actual)
        assertEquals(record, actual)
    }

    @Test
    fun findAll_returnsRecordsOrderedById() = runTest {
        val first = testEntity(id = 20260101, lunch = "udon")
        val second = testEntity(id = 20260102, dinner = "curry")

        recordDao.upsert(second)
        recordDao.upsert(first)

        val actual = recordDao.findAll()
        assertEquals(listOf(first, second), actual)
    }

    private fun testEntity(
        id: Int,
        breakfast: String? = null,
        lunch: String? = null,
        dinner: String? = null
    ): RecordEntity {
        return RecordEntity(
            id = id,
            breakfast = breakfast,
            lunch = lunch,
            dinner = dinner,
            isToilet = false,
            condition = null,
            conditionMemo = null,
            stepCount = null,
            healthKcal = null,
            ringfitKcal = null,
            ringfitKm = null
        )
    }
}
