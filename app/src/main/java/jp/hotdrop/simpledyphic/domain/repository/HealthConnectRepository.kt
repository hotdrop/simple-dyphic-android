package jp.hotdrop.simpledyphic.domain.repository

import java.time.LocalDate
import jp.hotdrop.simpledyphic.domain.model.DailyHealthSummary
import jp.hotdrop.simpledyphic.domain.model.HealthConnectStatus

interface HealthConnectRepository {
    fun requiredPermissions(): Set<String>
    suspend fun getStatus(): HealthConnectStatus
    suspend fun hasRequiredPermissions(): Boolean
    suspend fun readDailySummary(date: LocalDate): DailyHealthSummary
}
