package jp.hotdrop.simpledyphic.data.repository

import java.time.LocalDate
import java.util.LinkedHashMap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import jp.hotdrop.simpledyphic.data.local.source.RecordLocalDataSource
import jp.hotdrop.simpledyphic.data.repository.impl.LocalRecordRepository
import jp.hotdrop.simpledyphic.domain.model.DyphicId
import jp.hotdrop.simpledyphic.domain.model.Record

class LocalRecordRepositoryTest {
    private val fakeDataSource = FakeRecordLocalDataSource()
    private val repository = LocalRecordRepository(fakeDataSource)

    @Test
    fun saveThenFind_returnsSavedRecord() = runTest {
        val record = testRecord(LocalDate.of(2026, 2, 7), breakfast = "toast")

        repository.save(record)
        val actual = repository.find(record.id)

        assertEquals(record, actual)
    }

    @Test
    fun findAll_returnsAllSavedRecords() = runTest {
        val first = testRecord(LocalDate.of(2026, 2, 1), lunch = "soba")
        val second = testRecord(LocalDate.of(2026, 2, 2), dinner = "fish")

        repository.save(first)
        repository.save(second)

        val actual = repository.findAll()
        assertEquals(listOf(first, second), actual)
    }

    @Test
    fun find_whenMissing_throwsNoSuchElementException() = runTest {
        val error = runCatching {
            repository.find(20260101)
        }.exceptionOrNull()

        assertTrue(error is NoSuchElementException)
    }

    private fun testRecord(
        date: LocalDate,
        breakfast: String? = null,
        lunch: String? = null,
        dinner: String? = null
    ): Record {
        return Record(
            id = DyphicId.dateToId(date),
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

    private class FakeRecordLocalDataSource : RecordLocalDataSource {
        private val records = LinkedHashMap<Int, Record>()

        override suspend fun find(id: Int): Record {
            return records[id] ?: throw NoSuchElementException("Record not found: $id")
        }

        override suspend fun findAll(): List<Record> = records.values.toList()

        override suspend fun save(record: Record) {
            records[record.id] = record
        }
    }
}
