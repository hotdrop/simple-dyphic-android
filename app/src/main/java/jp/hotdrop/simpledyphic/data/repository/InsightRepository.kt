package jp.hotdrop.simpledyphic.data.repository

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.ConditionType
import jp.hotdrop.simpledyphic.model.DailyHealthMetrics
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.model.WeekRangeCalculator
import jp.hotdrop.simpledyphic.model.WeeklyConditionActivityInsight
import jp.hotdrop.simpledyphic.model.WeeklyMetricInsight
import jp.hotdrop.simpledyphic.model.appResultSuspend
import kotlin.math.abs

@Singleton
class InsightRepository @Inject constructor(
    private val recordRepository: RecordRepository,
    private val healthConnectRepository: HealthConnectRepository
) {

    suspend fun buildConditionActivityInsight(anchorDate: LocalDate): AppResult<WeeklyConditionActivityInsight> {
        return appResultSuspend {
            val weekRange = WeekRangeCalculator.mondayStart(anchorDate)
            val records = when (val result = recordRepository.findAll()) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> throw result.error
            }

            val badConditionDates = records
                .asSequence()
                .filter { record ->
                    record.condition == ConditionType.BAD &&
                        !record.date.isBefore(weekRange.startDate) &&
                        !record.date.isAfter(weekRange.endDate)
                }
                .map { it.date }
                .toList()

            if (badConditionDates.isEmpty()) {
                return@appResultSuspend WeeklyConditionActivityInsight(
                    weekRange = weekRange,
                    badConditionDates = emptyList(),
                    metricInsights = emptyList()
                )
            }

            val aroundWindow = ConditionWindowCalculator.resolveWindow(badConditionDates)
                ?: throw IllegalStateException("Failed to resolve condition window")
            val previousWeek = weekRange.previousWeek()

            val aroundMetrics = readMetricsRange(aroundWindow.startDate, aroundWindow.endDate)
            val currentWeekMetrics = readMetricsRange(weekRange.startDate, weekRange.endDate)
            val previousWeekMetrics = readMetricsRange(previousWeek.startDate, previousWeek.endDate)

            val insights = HealthMetricType.entries.map { metricType ->
                val aroundAvg = averageAvailable(aroundMetrics, metricType)
                val currentWeekAvg = averageAvailable(currentWeekMetrics, metricType)
                val previousWeekAvg = averageAvailable(previousWeekMetrics, metricType)
                val deltaPrev = calculateDelta(aroundAvg, previousWeekAvg)
                val deltaAvg = calculateDelta(aroundAvg, currentWeekAvg)

                WeeklyMetricInsight(
                    metricType = metricType,
                    deltaFromPreviousWeek = deltaPrev,
                    deltaFromWeekAverage = deltaAvg,
                    comment = buildComment(metricType, deltaPrev, deltaAvg)
                )
            }

            WeeklyConditionActivityInsight(
                weekRange = weekRange,
                badConditionDates = badConditionDates,
                metricInsights = insights
            )
        }
    }

    private suspend fun readMetricsRange(start: LocalDate, end: LocalDate): List<DailyHealthMetrics> {
        return when (val result = healthConnectRepository.readRangeMetrics(start, end)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> throw result.error
        }
    }

    private fun averageAvailable(
        values: List<DailyHealthMetrics>,
        metricType: HealthMetricType
    ): Double? {
        val availableValues = values.mapNotNull { daily ->
            val metric = daily.metricValue(metricType)
            if (metric.availability == MetricAvailability.AVAILABLE) metric.value else null
        }
        if (availableValues.isEmpty()) {
            return null
        }
        return availableValues.average()
    }

    private fun calculateDelta(base: Double?, compared: Double?): Double? {
        if (base == null || compared == null) {
            return null
        }
        return base - compared
    }

    private fun buildComment(
        metricType: HealthMetricType,
        deltaPrev: Double?,
        deltaAvg: Double?
    ): String {
        if (deltaAvg == null) {
            return "データ不足のため判定できません。"
        }

        val threshold = threshold(metricType)
        if (abs(deltaAvg) <= threshold) {
            return "体調不良前後で大きな差は見られません。"
        }

        if (deltaAvg > 0.0) {
            return if (deltaPrev != null && deltaPrev > threshold) {
                "体調不良前後は平均・先週比ともに高めです。"
            } else {
                "体調不良前後は平均より高めです。"
            }
        }

        return if (deltaPrev != null && deltaPrev < -threshold) {
            "体調不良前後は平均・先週比ともに低めです。"
        } else {
            "体調不良前後は平均より低めです。"
        }
    }

    private fun threshold(metricType: HealthMetricType): Double {
        return when (metricType) {
            HealthMetricType.STEP_COUNT -> 1000.0
            HealthMetricType.ACTIVE_KCAL -> 100.0
            HealthMetricType.EXERCISE_MINUTES -> 20.0
            HealthMetricType.DISTANCE_KM -> 1.0
        }
    }
}
