package jp.hotdrop.simpledyphic.feature.calendar

import java.time.LocalDate
import jp.hotdrop.simpledyphic.core.log.AppLogger
import jp.hotdrop.simpledyphic.domain.model.DyphicId
import jp.hotdrop.simpledyphic.domain.model.Record
import jp.hotdrop.simpledyphic.domain.repository.RecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsRecordsFromRepository() = runTest(dispatcher) {
        val record = testRecord(LocalDate.of(2026, 2, 7), memo = "memo")
        val repository = FakeRecordRepository(mutableMapOf(record.id to record))

        val viewModel = CalendarViewModel(
            recordRepository = repository,
            appLogger = NoOpLogger()
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(record, viewModel.uiState.value.recordsByDate[record.date])
    }

    @Test
    fun onResume_reloadsUpdatedRecords() = runTest(dispatcher) {
        val initial = testRecord(LocalDate.of(2026, 2, 7), memo = "before")
        val added = testRecord(LocalDate.of(2026, 2, 8), memo = "after")
        val repository = FakeRecordRepository(mutableMapOf(initial.id to initial))

        val viewModel = CalendarViewModel(
            recordRepository = repository,
            appLogger = NoOpLogger()
        )
        advanceUntilIdle()

        repository.save(added)
        viewModel.onResume()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.recordsByDate.size)
        assertEquals(added, viewModel.uiState.value.recordsByDate[added.date])
    }

    private fun testRecord(date: LocalDate, memo: String): Record {
        return Record(
            id = DyphicId.dateToId(date),
            breakfast = null,
            lunch = null,
            dinner = null,
            isToilet = false,
            condition = null,
            conditionMemo = memo,
            stepCount = null,
            healthKcal = null,
            ringfitKcal = null,
            ringfitKm = null
        )
    }

    private class FakeRecordRepository(
        private val records: MutableMap<Int, Record>
    ) : RecordRepository {
        override suspend fun find(id: Int): Record {
            return records[id] ?: throw NoSuchElementException("Record not found: $id")
        }

        override suspend fun findAll(): List<Record> = records.values.sortedBy { it.id }

        override suspend fun save(record: Record) {
            records[record.id] = record
        }
    }

    private class NoOpLogger : AppLogger {
        override fun i(message: String) = Unit
        override fun e(message: String, throwable: Throwable?) = Unit
    }
}
