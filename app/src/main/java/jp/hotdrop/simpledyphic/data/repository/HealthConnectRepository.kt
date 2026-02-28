package jp.hotdrop.simpledyphic.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.DailyHealthMetrics
import jp.hotdrop.simpledyphic.model.DailyHealthSummary
import jp.hotdrop.simpledyphic.model.DyphicId
import jp.hotdrop.simpledyphic.model.HealthConnectStatus
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.HealthMetricValue
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.model.appResultSuspend
import timber.log.Timber

@Singleton
class HealthConnectRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun requiredPermissions(): Set<String> = DAILY_SUMMARY_PERMISSIONS

    fun getStatus(): HealthConnectStatus {
        return when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PROVIDER_PACKAGE)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.UPDATE_REQUIRED
            else -> HealthConnectStatus.NOT_INSTALLED
        }
    }

    suspend fun hasRequiredPermissions(): AppResult<Boolean> {
        return appResultSuspend {
            val client = healthConnectClientOrNull() ?: return@appResultSuspend false
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            grantedPermissions.containsAll(DAILY_SUMMARY_PERMISSIONS)
        }
    }

    suspend fun getGrantedMetricTypes(): AppResult<Set<HealthMetricType>> {
        return appResultSuspend {
            val client = healthConnectClientOrNull() ?: return@appResultSuspend emptySet()
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            METRIC_PERMISSION_MAP
                .filterValues { permission -> grantedPermissions.contains(permission) }
                .keys
        }
    }

    suspend fun readDailySummary(date: LocalDate): AppResult<DailyHealthSummary> {
        return appResultSuspend {
            val client = healthConnectClientOrNull() ?: throw IllegalStateException("ヘルスコネクトが利用できません")
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            if (!grantedPermissions.containsAll(DAILY_SUMMARY_PERMISSIONS)) {
                throw SecurityException("ヘルスコネクトへのアクセス権の確認に失敗しました。")
            }
            val grantedMetricTypes = METRIC_PERMISSION_MAP
                .filterValues { permission -> grantedPermissions.contains(permission) }
                .keys
            val daily = readMetricsForDate(client, date, grantedMetricTypes)
            DailyHealthSummary(
                stepCount = daily.stepCount.value?.toInt() ?: 0,
                burnedKcal = daily.activeKcal.value ?: 0.0
            )
        }
    }

    suspend fun readRangeMetrics(start: LocalDate, end: LocalDate): AppResult<List<DailyHealthMetrics>> {
        return appResultSuspend {
            require(!start.isAfter(end)) { "start must be on/before end" }
            val status = getStatus()
            if (status != HealthConnectStatus.AVAILABLE) {
                return@appResultSuspend buildUnavailableRange(start, end, MetricAvailability.SOURCE_UNAVAILABLE)
            }

            val client = healthConnectClientOrNull() ?: return@appResultSuspend buildUnavailableRange(
                start,
                end,
                MetricAvailability.SOURCE_UNAVAILABLE
            )
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            val grantedMetricTypes = METRIC_PERMISSION_MAP
                .filterValues { permission -> grantedPermissions.contains(permission) }
                .keys

            var date = start
            val metrics = ArrayList<DailyHealthMetrics>()
            while (!date.isAfter(end)) {
                metrics += readMetricsForDate(client, date, grantedMetricTypes)
                date = date.plusDays(1)
            }
            metrics
        }
    }

    private suspend fun readMetricsForDate(
        client: HealthConnectClient,
        date: LocalDate,
        grantedMetricTypes: Set<HealthMetricType>
    ): DailyHealthMetrics {
        val zoneId = ZoneId.systemDefault()
        val start = date.atStartOfDay(zoneId).toInstant()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        val timeRange = TimeRangeFilter.between(start, end)

        val steps = readMetricValue(
            isGranted = grantedMetricTypes.contains(HealthMetricType.STEP_COUNT),
            metricType = HealthMetricType.STEP_COUNT
        ) {
            val value = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeRange
                )
            )[StepsRecord.COUNT_TOTAL] ?: 0L
            value.toDouble()
        }

        val activeKcal = readMetricValue(
            isGranted = grantedMetricTypes.contains(HealthMetricType.ACTIVE_KCAL),
            metricType = HealthMetricType.ACTIVE_KCAL
        ) {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = timeRange
                )
            ).records.sumOf { it.energy.inKilocalories }
        }

        val exerciseMinutes = readMetricValue(
            isGranted = grantedMetricTypes.contains(HealthMetricType.EXERCISE_MINUTES),
            metricType = HealthMetricType.EXERCISE_MINUTES
        ) {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = timeRange
                )
            ).records.sumOf { record ->
                Duration.between(record.startTime, record.endTime)
                    .toMinutes()
                    .coerceAtLeast(0)
                    .toDouble()
            }
        }

        val distanceKm = readMetricValue(
            isGranted = grantedMetricTypes.contains(HealthMetricType.DISTANCE_KM),
            metricType = HealthMetricType.DISTANCE_KM
        ) {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = timeRange
                )
            ).records.sumOf { it.distance.inKilometers }
        }

        val floorsClimbed = readMetricValue(
            isGranted = grantedMetricTypes.contains(HealthMetricType.FLOORS_CLIMBED),
            metricType = HealthMetricType.FLOORS_CLIMBED
        ) {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = FloorsClimbedRecord::class,
                    timeRangeFilter = timeRange
                )
            ).records.sumOf { it.floors }
        }

        return DailyHealthMetrics(
            dateId = DyphicId.dateToId(date),
            stepCount = steps,
            activeKcal = activeKcal,
            exerciseMinutes = exerciseMinutes,
            distanceKm = distanceKm,
            floorsClimbed = floorsClimbed
        )
    }

    private suspend fun readMetricValue(
        isGranted: Boolean,
        metricType: HealthMetricType,
        readBlock: suspend () -> Double
    ): HealthMetricValue {
        if (!isGranted) {
            return HealthMetricValue(
                availability = MetricAvailability.PERMISSION_MISSING,
                value = null
            )
        }

        return runCatching { readBlock() }
            .fold(
                onSuccess = { value ->
                    HealthMetricValue(
                        availability = MetricAvailability.AVAILABLE,
                        value = value
                    )
                },
                onFailure = { error ->
                    Timber.w(error, "Failed to read metric: %s", metricType)
                    HealthMetricValue(
                        availability = MetricAvailability.SOURCE_UNAVAILABLE,
                        value = null
                    )
                }
            )
    }

    private fun buildUnavailableRange(
        start: LocalDate,
        end: LocalDate,
        availability: MetricAvailability
    ): List<DailyHealthMetrics> {
        var date = start
        val unavailableValue = HealthMetricValue(
            availability = availability,
            value = null
        )
        val list = ArrayList<DailyHealthMetrics>()
        while (!date.isAfter(end)) {
            list += DailyHealthMetrics(
                dateId = DyphicId.dateToId(date),
                stepCount = unavailableValue,
                activeKcal = unavailableValue,
                exerciseMinutes = unavailableValue,
                distanceKm = unavailableValue,
                floorsClimbed = unavailableValue
            )
            date = date.plusDays(1)
        }
        return list
    }

    private fun healthConnectClientOrNull(): HealthConnectClient? {
        return if (getStatus() == HealthConnectStatus.AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    companion object {
        private const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        private val METRIC_PERMISSION_MAP: Map<HealthMetricType, String> = mapOf(
            HealthMetricType.STEP_COUNT to HealthPermission.getReadPermission(StepsRecord::class),
            HealthMetricType.ACTIVE_KCAL to HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthMetricType.EXERCISE_MINUTES to HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthMetricType.DISTANCE_KM to HealthPermission.getReadPermission(DistanceRecord::class),
            HealthMetricType.FLOORS_CLIMBED to HealthPermission.getReadPermission(FloorsClimbedRecord::class)
        )

        private val DAILY_SUMMARY_PERMISSIONS: Set<String> = setOf(
            METRIC_PERMISSION_MAP.getValue(HealthMetricType.STEP_COUNT),
            METRIC_PERMISSION_MAP.getValue(HealthMetricType.ACTIVE_KCAL)
        )
    }
}
