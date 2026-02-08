package jp.hotdrop.simpledyphic.feature.record

import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate
import jp.hotdrop.simpledyphic.core.log.AppLogger
import jp.hotdrop.simpledyphic.domain.model.ConditionType
import jp.hotdrop.simpledyphic.domain.model.DailyHealthSummary
import jp.hotdrop.simpledyphic.domain.model.HealthConnectStatus
import jp.hotdrop.simpledyphic.domain.model.Record
import jp.hotdrop.simpledyphic.domain.repository.HealthConnectRepository
import jp.hotdrop.simpledyphic.domain.repository.RecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
        val healthRepository = FakeHealthConnectRepository()
        val viewModel = createViewModel(recordId = base.id, repository = repository, healthRepository)
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
        assertEquals(ConditionType.GOOD, saved.condition)
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
        val healthRepository = FakeHealthConnectRepository()
        val viewModel = createViewModel(recordId = base.id, repository = repository, healthRepository)
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
        val healthRepository = FakeHealthConnectRepository()
        val viewModel = createViewModel(recordId = base.id, repository = repository, healthRepository)
        advanceUntilIdle()

        viewModel.onConditionMemoChanged("memo")
        var closed = false

        viewModel.onBackRequested { closed = true }

        assertFalse(closed)
        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun onHealthPermissionResult_whenDenied_setsMessageAndDoesNotUpdateValues() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 2, 7)
        val base = Record.createEmpty(date)
        val repository = FakeRecordRepository(mutableMapOf(base.id to base))
        val healthRepository = FakeHealthConnectRepository(
            summary = DailyHealthSummary(stepCount = 6000, burnedKcal = 250.0)
        )
        val viewModel = createViewModel(recordId = base.id, repository = repository, healthRepository)
        advanceUntilIdle()

        viewModel.onHealthPermissionResult(emptySet())

        assertEquals("Health Connect permission was denied.", viewModel.uiState.value.healthConnectMessage)
        assertEquals(null, viewModel.uiState.value.stepCount)
        assertEquals(null, viewModel.uiState.value.healthKcal)
    }

    @Test
    fun healthSync_whenValuesExistAndDiffer_showsOverwriteDialog_thenApply() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 2, 7)
        val base = Record.createEmpty(date).copy(stepCount = 1000, healthKcal = 100.0)
        val repository = FakeRecordRepository(mutableMapOf(base.id to base))
        val healthRepository = FakeHealthConnectRepository(
            summary = DailyHealthSummary(stepCount = 8000, burnedKcal = 420.0)
        )
        val viewModel = createViewModel(recordId = base.id, repository = repository, healthRepository)
        advanceUntilIdle()

        viewModel.onHealthSyncRequested()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showHealthOverwriteDialog)
        viewModel.confirmHealthOverwrite()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showHealthOverwriteDialog)
        assertEquals(8000, viewModel.uiState.value.stepCount)
        assertEquals(420.0, viewModel.uiState.value.healthKcal)
        assertTrue(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun onScreenEntered_whenPermissionsGranted_importsHealthSummaryWithoutInfoMessage() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 2, 7)
        val base = Record.createEmpty(date)
        val repository = FakeRecordRepository(mutableMapOf(base.id to base))
        val healthRepository = FakeHealthConnectRepository(
            summary = DailyHealthSummary(stepCount = 8123, burnedKcal = 345.6)
        )
        val viewModel = createViewModel(recordId = base.id, repository = repository, healthRepository)
        advanceUntilIdle()

        viewModel.onScreenEntered()
        advanceUntilIdle()

        assertEquals(8123, viewModel.uiState.value.stepCount)
        assertEquals(345.6, viewModel.uiState.value.healthKcal)
        assertEquals(null, viewModel.uiState.value.healthConnectMessage)
    }

    @Test
    fun onScreenEntered_whenPermissionsMissing_requestsHealthPermission() = runTest(dispatcher) {
        val date = LocalDate.of(2026, 2, 7)
        val base = Record.createEmpty(date)
        val repository = FakeRecordRepository(mutableMapOf(base.id to base))
        val expectedPermissions = setOf("perm.steps", "perm.kcal")
        val healthRepository = FakeHealthConnectRepository(
            permissions = expectedPermissions,
            grantedPermissions = emptySet()
        )
        val viewModel = createViewModel(recordId = base.id, repository = repository, healthRepository)
        advanceUntilIdle()

        val effectDeferred = async { viewModel.effects.first() }
        viewModel.onScreenEntered()
        advanceUntilIdle()

        val effect = effectDeferred.await()
        assertTrue(effect is RecordEditViewModel.RecordEditEffect.RequestHealthPermissions)
        effect as RecordEditViewModel.RecordEditEffect.RequestHealthPermissions
        assertEquals(expectedPermissions, effect.permissions)
    }

    private fun createViewModel(
        recordId: Int,
        repository: RecordRepository,
        healthRepository: HealthConnectRepository
    ): RecordEditViewModel {
        return RecordEditViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(RecordEditViewModel.RECORD_ID_ARG to recordId)
            ),
            recordRepository = repository,
            healthConnectRepository = healthRepository,
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

    private class FakeHealthConnectRepository(
        private val status: HealthConnectStatus = HealthConnectStatus.AVAILABLE,
        private val permissions: Set<String> = setOf(STEPS_PERMISSION, KCAL_PERMISSION),
        private val grantedPermissions: Set<String> = setOf(STEPS_PERMISSION, KCAL_PERMISSION),
        private val summary: DailyHealthSummary = DailyHealthSummary(stepCount = 7777, burnedKcal = 333.3)
    ) : HealthConnectRepository {
        override fun requiredPermissions(): Set<String> = permissions

        override suspend fun getStatus(): HealthConnectStatus = status

        override suspend fun hasRequiredPermissions(): Boolean {
            return grantedPermissions.containsAll(permissions)
        }

        override suspend fun readDailySummary(date: LocalDate): DailyHealthSummary = summary

        companion object {
            private const val STEPS_PERMISSION = "perm.steps"
            private const val KCAL_PERMISSION = "perm.kcal"
        }
    }

    private class NoOpLogger : AppLogger {
        override fun i(message: String) = Unit
        override fun e(message: String, throwable: Throwable?) = Unit
    }
}
