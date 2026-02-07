package jp.hotdrop.simpledyphic.feature.record

import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate
import jp.hotdrop.simpledyphic.core.log.AppLogger
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordEditViewModelTest {

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
    fun save_withValidInputs_updatesRepositoryAndReturnsTrue() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 2, 7)
        val base = Record.createEmpty(date)
        val repository = FakeRecordRepository(mutableMapOf(base.id to base))
        val viewModel = createViewModel(recordId = base.id, repository = repository)
        advanceUntilIdle()

        viewModel.onBreakfastChanged("Toast")
        viewModel.onLunchChanged("Pasta")
        viewModel.onDinnerChanged("Soup")
        viewModel.onConditionTypeChanged(ConditionType.GOOD)
        viewModel.onConditionMemoChanged("Stable")
        viewModel.onIsToiletChanged(true)
        viewModel.onRingfitKcalChanged("120")
        viewModel.onRingfitKmChanged("2.5")

        var result: Boolean? = null
        viewModel.save { updated -> result = updated }
        advanceUntilIdle()

        val saved = repository.find(base.id)
        assertEquals(true, result)
        assertEquals("Toast", saved.breakfast)
        assertEquals("Pasta", saved.lunch)
        assertEquals("Soup", saved.dinner)
        assertEquals("良い", saved.condition)
        assertEquals("Stable", saved.conditionMemo)
        assertTrue(saved.isToilet)
        assertEquals(120.0, saved.ringfitKcal)
        assertEquals(2.5, saved.ringfitKm)
        assertFalse(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun save_withInvalidRingfitInput_setsErrorAndDoesNotSave() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 2, 7)
        val base = Record.createEmpty(date)
        val repository = FakeRecordRepository(mutableMapOf(base.id to base))
        val viewModel = createViewModel(recordId = base.id, repository = repository)
        advanceUntilIdle()

        viewModel.onRingfitKcalChanged("abc")
        var result: Boolean? = null

        viewModel.save { updated -> result = updated }
        advanceUntilIdle()

        assertEquals(null, result)
        assertEquals("RingFit kcal must be a number.", viewModel.uiState.value.errorMessage)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun onBackRequested_withUnsavedInput_opensDiscardDialog() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 2, 7)
        val base = Record.createEmpty(date)
        val repository = FakeRecordRepository(mutableMapOf(base.id to base))
        val viewModel = createViewModel(recordId = base.id, repository = repository)
        advanceUntilIdle()

        viewModel.onConditionMemoChanged("memo")
        var closed = false

        viewModel.onBackRequested { closed = true }

        assertFalse(closed)
        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }

    private fun createViewModel(recordId: Int, repository: RecordRepository): RecordEditViewModel {
        return RecordEditViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(RecordEditViewModel.RECORD_ID_ARG to recordId)
            ),
            recordRepository = repository,
            appLogger = NoOpLogger()
        )
    }

    private class FakeRecordRepository(
        private val records: MutableMap<Int, Record>
    ) : RecordRepository {
        var saveCount: Int = 0
            private set

        override suspend fun find(id: Int): Record {
            return records[id] ?: throw NoSuchElementException("Record not found: $id")
        }

        override suspend fun findAll(): List<Record> = records.values.toList()

        override suspend fun save(record: Record) {
            saveCount += 1
            records[record.id] = record
        }

        override suspend fun backup() = Unit

        override suspend fun restore() = Unit
    }

    private class NoOpLogger : AppLogger {
        override fun i(message: String) = Unit
        override fun e(message: String, throwable: Throwable?) = Unit
    }
}
