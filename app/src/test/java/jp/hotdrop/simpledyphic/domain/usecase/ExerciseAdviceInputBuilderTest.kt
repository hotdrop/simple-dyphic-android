package jp.hotdrop.simpledyphic.domain.usecase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import jp.hotdrop.simpledyphic.data.local.AppSettingsLocalDataSource
import jp.hotdrop.simpledyphic.data.local.RoomGoalLocalDataSource
import jp.hotdrop.simpledyphic.data.local.RoomRecordLocalDataSource
import jp.hotdrop.simpledyphic.data.local.db.AppDatabase
import jp.hotdrop.simpledyphic.data.remote.FirebaseAuthRemoteDataSource
import jp.hotdrop.simpledyphic.data.remote.FirestoreRecordRemoteDataSource
import java.time.LocalDate
import jp.hotdrop.simpledyphic.data.repository.AppSettingsRepository
import jp.hotdrop.simpledyphic.data.repository.GoalRepository
import jp.hotdrop.simpledyphic.data.repository.HealthConnectRepository
import jp.hotdrop.simpledyphic.data.repository.RecordRepository
import jp.hotdrop.simpledyphic.model.AdvicePeriod
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.AppSettings
import jp.hotdrop.simpledyphic.model.DailyHealthMetrics
import jp.hotdrop.simpledyphic.model.ExerciseAdviceRequirement
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.HealthMetricValue
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseAdviceInputBuilderTest {

    @Test
    fun build_scalesWeeklyGoalsForMonthlyPeriodAndAggregatesRingFit() = runTest {
        val modelFile = File.createTempFile("gemma-test", ".litertlm")
        val builder = ExerciseAdviceInputBuilder(
            appSettingsRepository = fakeSettingsRepository(
                AppSettings(
                    birthDate = LocalDate.of(1994, 1, 10),
                    heightCm = 168.0,
                    weightKg = 58.0,
                    modelFilePath = modelFile.absolutePath,
                    modelDisplayName = "model"
                )
            ),
            goalRepository = fakeGoalRepository(
                listOf(
                    WeeklyGoal(HealthMetricType.STEP_COUNT, 70_000.0),
                    WeeklyGoal(HealthMetricType.ACTIVE_KCAL, 2_100.0)
                )
            ),
            healthConnectRepository = fakeHealthRepository(
                metrics = listOf(
                    daily(step = 10_000.0, kcal = 300.0),
                    daily(step = 8_000.0, kcal = 250.0)
                ),
                hasPermissions = true
            ),
            recordRepository = fakeRecordRepository(
                listOf(
                    record(20260401, 120.0, 2.0),
                    record(20260402, 80.0, 1.5)
                )
            )
        )

        val result = builder.build(
            period = AdvicePeriod.MONTHLY,
            today = LocalDate.of(2026, 4, 2)
        )

        result as AppResult.Success
        val input = result.value
        val stepSummary = input.summaries.first { it.kind.name == "STEP_COUNT" }
        val ringfitKcalSummary = input.summaries.first { it.kind.name == "RINGFIT_KCAL" }

        assertEquals(LocalDate.of(2026, 4, 1), input.periodStartDate)
        assertEquals(2, input.elapsedDays)
        assertEquals(18_000.0, requireNotNull(stepSummary.actualValue), 0.001)
        assertEquals(20_000.0, requireNotNull(stepSummary.targetValue), 0.001)
        assertEquals(200.0, requireNotNull(ringfitKcalSummary.actualValue), 0.001)
        assertTrue(input.promptDataBlock.contains("歩数"))
        assertTrue(input.missingRequirements.isEmpty())
        modelFile.delete()
    }

    @Test
    fun build_marksMissingRequirementsWhenSettingsOrPermissionsAreMissing() = runTest {
        val builder = ExerciseAdviceInputBuilder(
            appSettingsRepository = fakeSettingsRepository(AppSettings()),
            goalRepository = fakeGoalRepository(emptyList()),
            healthConnectRepository = fakeHealthRepository(emptyList(), hasPermissions = false),
            recordRepository = fakeRecordRepository(emptyList())
        )

        val result = builder.build(
            period = AdvicePeriod.WEEKLY,
            today = LocalDate.of(2026, 4, 12)
        )

        result as AppResult.Success
        val missing = result.value.missingRequirements
        assertTrue(missing.contains(ExerciseAdviceRequirement.BIRTH_DATE))
        assertTrue(missing.contains(ExerciseAdviceRequirement.HEIGHT))
        assertTrue(missing.contains(ExerciseAdviceRequirement.WEIGHT))
        assertTrue(missing.contains(ExerciseAdviceRequirement.MODEL_FILE))
        assertTrue(missing.contains(ExerciseAdviceRequirement.HEALTH_PERMISSION))
    }

    private fun daily(
        step: Double = 0.0,
        kcal: Double = 0.0,
        minutes: Double = 0.0,
        distance: Double = 0.0,
        availability: MetricAvailability = MetricAvailability.AVAILABLE
    ): DailyHealthMetrics {
        return DailyHealthMetrics(
            dateId = 20260401,
            stepCount = HealthMetricValue(availability, if (availability == MetricAvailability.AVAILABLE) step else null),
            activeKcal = HealthMetricValue(availability, if (availability == MetricAvailability.AVAILABLE) kcal else null),
            exerciseMinutes = HealthMetricValue(availability, if (availability == MetricAvailability.AVAILABLE) minutes else null),
            distanceKm = HealthMetricValue(availability, if (availability == MetricAvailability.AVAILABLE) distance else null)
        )
    }

    private fun record(id: Int, kcal: Double, km: Double): Record {
        return Record(
            id = id,
            breakfast = null,
            lunch = null,
            dinner = null,
            isToilet = false,
            condition = null,
            conditionMemo = null,
            stepCount = null,
            healthKcal = null,
            ringfitKcal = kcal,
            ringfitKm = km
        )
    }

    private fun fakeSettingsRepository(settings: AppSettings): AppSettingsRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        return object : AppSettingsRepository(
            localDataSource = AppSettingsLocalDataSource(database.appSettingsDao()),
            context = context
        ) {
            override suspend fun get(): AppResult<AppSettings> = AppResult.Success(settings)
        }
    }

    private fun fakeGoalRepository(goals: List<WeeklyGoal>): GoalRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        return object : GoalRepository(
            localDataSource = RoomGoalLocalDataSource(database.weeklyGoalDao())
        ) {
            override suspend fun getWeeklyGoals(): AppResult<List<WeeklyGoal>> = AppResult.Success(goals)
        }
    }

    private fun fakeHealthRepository(
        metrics: List<DailyHealthMetrics>,
        hasPermissions: Boolean
    ): HealthConnectRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return object : HealthConnectRepository(context) {
            override suspend fun readRangeMetrics(start: LocalDate, end: LocalDate): AppResult<List<DailyHealthMetrics>> {
                return AppResult.Success(metrics)
            }

            override suspend fun hasPermissions(metricTypes: Set<HealthMetricType>): AppResult<Boolean> {
                return AppResult.Success(hasPermissions)
            }
        }
    }

    private fun fakeRecordRepository(records: List<Record>): RecordRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirebaseApp.initializeApp(context)
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        return object : RecordRepository(
            localDataSource = RoomRecordLocalDataSource(database.recordDao()),
            remoteDataSource = FirestoreRecordRemoteDataSource(FirebaseFirestore.getInstance()),
            accountRepository = jp.hotdrop.simpledyphic.data.repository.AccountRepository(
                FirebaseAuthRemoteDataSource(
                    context = context,
                    firebaseAuth = FirebaseAuth.getInstance(),
                    credentialManager = CredentialManager.create(context)
                )
            )
        ) {
            override suspend fun findByDateRange(start: LocalDate, end: LocalDate): AppResult<List<Record>> {
                return AppResult.Success(records)
            }
        }
    }
}
