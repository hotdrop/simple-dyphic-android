package jp.hotdrop.simpledyphic.data.repository

import java.time.LocalDate
import java.util.LinkedHashMap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import jp.hotdrop.simpledyphic.core.log.AppLogger
import jp.hotdrop.simpledyphic.data.local.source.RecordLocalDataSource
import jp.hotdrop.simpledyphic.data.remote.firestore.RecordRemoteDataSource
import jp.hotdrop.simpledyphic.data.repository.impl.LocalRecordRepository
import jp.hotdrop.simpledyphic.domain.model.DyphicId
import jp.hotdrop.simpledyphic.domain.model.Record
import jp.hotdrop.simpledyphic.domain.model.UserAccount
import jp.hotdrop.simpledyphic.domain.repository.AccountRepository

class LocalRecordRepositoryTest {
    private val fakeDataSource = FakeRecordLocalDataSource()
    private val fakeRemoteDataSource = FakeRecordRemoteDataSource()
    private val fakeAccountRepository = FakeAccountRepository()
    private val repository = LocalRecordRepository(
        localDataSource = fakeDataSource,
        remoteDataSource = fakeRemoteDataSource,
        accountRepository = fakeAccountRepository,
        appLogger = NoOpLogger()
    )

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

    @Test
    fun backup_whenSignedIn_uploadsAllLocalRecords() = runTest {
        fakeAccountRepository.signedInAccount = UserAccount(
            uid = "user-1",
            name = "User",
            email = "user@example.com"
        )
        val first = testRecord(LocalDate.of(2026, 2, 3), breakfast = "toast")
        val second = testRecord(LocalDate.of(2026, 2, 4), dinner = "fish")
        repository.save(first)
        repository.save(second)

        repository.backup()

        assertEquals(listOf(first, second), fakeRemoteDataSource.savedByUserId["user-1"])
    }

    @Test
    fun restore_whenSignedIn_replacesLocalDataWithRemoteRecords() = runTest {
        fakeAccountRepository.signedInAccount = UserAccount(
            uid = "user-2",
            name = "User",
            email = "user@example.com"
        )
        val remoteRecord = testRecord(LocalDate.of(2026, 2, 5), lunch = "ramen")
        fakeRemoteDataSource.recordsByUserId["user-2"] = listOf(remoteRecord)
        repository.save(testRecord(LocalDate.of(2026, 2, 6), dinner = "old"))

        repository.restore()

        assertEquals(listOf(remoteRecord), repository.findAll())
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

        override suspend fun replaceAll(records: List<Record>) {
            this.records.clear()
            records.forEach { record ->
                this.records[record.id] = record
            }
        }
    }

    private class FakeRecordRemoteDataSource : RecordRemoteDataSource {
        val savedByUserId = LinkedHashMap<String, List<Record>>()
        val recordsByUserId = LinkedHashMap<String, List<Record>>()

        override suspend fun findAll(userId: String): List<Record> {
            return recordsByUserId[userId].orEmpty()
        }

        override suspend fun saveAll(userId: String, records: List<Record>) {
            savedByUserId[userId] = records
        }
    }

    private class FakeAccountRepository : AccountRepository {
        var signedInAccount: UserAccount? = null

        override fun currentAccount(): UserAccount? = signedInAccount

        override suspend fun signInWithGoogle(): UserAccount {
            return checkNotNull(signedInAccount) {
                "signedInAccount must be set before signInWithGoogle()"
            }
        }

        override suspend fun signOut() {
            signedInAccount = null
        }
    }

    private class NoOpLogger : AppLogger {
        override fun i(message: String) = Unit
        override fun e(message: String, throwable: Throwable?) = Unit
    }
}
