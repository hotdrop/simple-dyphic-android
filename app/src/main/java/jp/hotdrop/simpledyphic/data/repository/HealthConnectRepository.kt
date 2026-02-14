package jp.hotdrop.simpledyphic.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.hotdrop.simpledyphic.model.DailyHealthSummary
import jp.hotdrop.simpledyphic.model.HealthConnectStatus
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun requiredPermissions(): Set<String> = REQUIRED_PERMISSIONS

    fun getStatus(): HealthConnectStatus {
        return when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PROVIDER_PACKAGE)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.UPDATE_REQUIRED
            else -> HealthConnectStatus.NOT_INSTALLED
        }
    }

    suspend fun hasRequiredPermissions(): Boolean {
        val client = healthConnectClientOrNull() ?: return false
        val grantedPermissions = client.permissionController.getGrantedPermissions()
        return grantedPermissions.containsAll(REQUIRED_PERMISSIONS)
    }

    suspend fun readDailySummary(date: LocalDate): DailyHealthSummary {
        val client = healthConnectClientOrNull() ?: throw IllegalStateException("Health Connect is not available.")
        val grantedPermissions = client.permissionController.getGrantedPermissions()
        if (!grantedPermissions.containsAll(REQUIRED_PERMISSIONS)) {
            throw SecurityException("Health Connect permissions are missing.")
        }

        val zoneId = ZoneId.systemDefault()
        val start = date.atStartOfDay(zoneId).toInstant()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        val timeRange = TimeRangeFilter.between(start, end)

        val steps = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = timeRange
            )
        ).records.sumOf { it.count }

        val burnedKcal = client.readRecords(
            androidx.health.connect.client.request.ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = timeRange
            )
        ).records.sumOf { it.energy.inKilocalories }

        return DailyHealthSummary(
            stepCount = steps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            burnedKcal = burnedKcal
        )
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

        private val REQUIRED_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        )
    }
}
