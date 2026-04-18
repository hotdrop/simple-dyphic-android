package jp.hotdrop.simpledyphic.domain.usecase

import java.io.File
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import jp.hotdrop.simpledyphic.data.repository.AppSettingsRepository
import jp.hotdrop.simpledyphic.data.repository.GoalRepository
import jp.hotdrop.simpledyphic.data.repository.HealthConnectRepository
import jp.hotdrop.simpledyphic.data.repository.RecordRepository
import jp.hotdrop.simpledyphic.model.AdvicePeriod
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.AppSettings
import jp.hotdrop.simpledyphic.model.ExerciseAdviceInput
import jp.hotdrop.simpledyphic.model.ExerciseAdviceRequirement
import jp.hotdrop.simpledyphic.model.ExerciseMetricKind
import jp.hotdrop.simpledyphic.model.ExerciseMetricSummary
import jp.hotdrop.simpledyphic.model.HealthMetricType
import jp.hotdrop.simpledyphic.model.HealthMetricValue
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.model.WeeklyGoal
import jp.hotdrop.simpledyphic.model.appResultSuspend

class ExerciseAdviceInputBuilder @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val goalRepository: GoalRepository,
    private val healthConnectRepository: HealthConnectRepository,
    private val recordRepository: RecordRepository
) {
    suspend fun build(
        period: AdvicePeriod,
        today: LocalDate = LocalDate.now()
    ): AppResult<ExerciseAdviceInput> {
        return appResultSuspend {
            val range = period.resolveRange(today)

            val settings = when (val result = appSettingsRepository.get()) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> throw result.error
            }.sanitize()

            val goals = when (val result = goalRepository.getWeeklyGoals()) {
                is AppResult.Success -> result.value.filter { it.enabled }
                is AppResult.Failure -> throw result.error
            }

            val healthMetrics = when (
                val result = healthConnectRepository.readRangeMetrics(
                    start = range.startDate,
                    end = range.endDate
                )
            ) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> throw result.error
            }

            val records = when (
                val result = recordRepository.findByDateRange(range.startDate, range.endDate)
            ) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> throw result.error
            }

            val hasHealthPermissions = when (
                val result = healthConnectRepository.hasPermissions(HEALTH_METRIC_TYPES)
            ) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> false
            }

            val missingRequirements = linkedSetOf<ExerciseAdviceRequirement>()
            if (settings.birthDate == null) {
                missingRequirements += ExerciseAdviceRequirement.BIRTH_DATE
            }
            if (settings.heightCm == null) {
                missingRequirements += ExerciseAdviceRequirement.HEIGHT
            }
            if (settings.weightKg == null) {
                missingRequirements += ExerciseAdviceRequirement.WEIGHT
            }
            val modelFilePath = settings.modelFilePath?.takeIf { File(it).exists() }
            if (modelFilePath == null) {
                missingRequirements += ExerciseAdviceRequirement.MODEL_FILE
            }
            if (!hasHealthPermissions) {
                missingRequirements += ExerciseAdviceRequirement.HEALTH_PERMISSION
            }

            val normalizedSettings = settings.copy(modelFilePath = modelFilePath)
            val summaries = buildSummaries(
                rangeElapsedDays = range.elapsedDays,
                goals = goals,
                healthMetrics = healthMetrics,
                records = records
            )

            ExerciseAdviceInput(
                period = period,
                periodStartDate = range.startDate,
                periodEndDate = range.endDate,
                elapsedDays = range.elapsedDays,
                age = normalizedSettings.birthDate?.let { birthDate ->
                    Period.between(birthDate, today).years.coerceAtLeast(0)
                },
                settings = normalizedSettings,
                summaries = summaries,
                missingRequirements = missingRequirements,
                promptDataBlock = buildPromptDataBlock(
                    period = period,
                    rangeStart = range.startDate,
                    rangeEnd = range.endDate,
                    age = normalizedSettings.birthDate?.let { birthDate ->
                        Period.between(birthDate, today).years.coerceAtLeast(0)
                    },
                    settings = normalizedSettings,
                    summaries = summaries
                )
            )
        }
    }

    private fun buildSummaries(
        rangeElapsedDays: Long,
        goals: List<WeeklyGoal>,
        healthMetrics: List<jp.hotdrop.simpledyphic.model.DailyHealthMetrics>,
        records: List<Record>
    ): List<ExerciseMetricSummary> {
        val goalMap = goals.associateBy { it.metricType }
        val healthSummaries = HealthMetricType.entries.map { metricType ->
            val metricValues = healthMetrics.map { it.metricValue(metricType) }
            val availability = resolveAvailability(metricValues)
            val actualValue = if (availability == MetricAvailability.AVAILABLE) {
                metricValues.sumOf { value -> value.value ?: 0.0 }
            } else {
                null
            }
            val scaledTarget = goalMap[metricType]?.targetValue?.times(rangeElapsedDays / 7.0)
            ExerciseMetricSummary(
                kind = metricType.toExerciseMetricKind(),
                actualValue = actualValue,
                availability = availability,
                targetValue = scaledTarget,
                achievementRate = if (actualValue != null && scaledTarget != null && scaledTarget > 0.0) {
                    (actualValue / scaledTarget) * 100.0
                } else {
                    null
                }
            )
        }

        val ringfitKcal = records.sumOf { it.ringfitKcal ?: 0.0 }
        val ringfitKm = records.sumOf { it.ringfitKm ?: 0.0 }
        return healthSummaries + listOf(
            ExerciseMetricSummary(
                kind = ExerciseMetricKind.RINGFIT_KCAL,
                actualValue = ringfitKcal,
                availability = MetricAvailability.AVAILABLE
            ),
            ExerciseMetricSummary(
                kind = ExerciseMetricKind.RINGFIT_KM,
                actualValue = ringfitKm,
                availability = MetricAvailability.AVAILABLE
            )
        )
    }

    private fun resolveAvailability(metricValues: List<HealthMetricValue>): MetricAvailability {
        return when {
            metricValues.any { it.availability == MetricAvailability.AVAILABLE } -> MetricAvailability.AVAILABLE
            metricValues.any { it.availability == MetricAvailability.PERMISSION_MISSING } -> MetricAvailability.PERMISSION_MISSING
            else -> MetricAvailability.SOURCE_UNAVAILABLE
        }
    }

    private fun buildPromptDataBlock(
        period: AdvicePeriod,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        age: Int?,
        settings: AppSettings,
        summaries: List<ExerciseMetricSummary>
    ): String {
        return buildString {
            appendLine("対象期間: ${period.toPromptLabel()} (${rangeStart} 〜 ${rangeEnd})")
            appendLine("ユーザープロフィール:")
            appendLine("- 年齢: ${age?.let { "${it}歳" } ?: "未設定"}")
            appendLine("- 身長: ${settings.heightCm?.let { formatDecimal(it) + "cm" } ?: "未設定"}")
            appendLine("- 体重: ${settings.weightKg?.let { formatDecimal(it) + "kg" } ?: "未設定"}")
            appendLine("運動サマリー:")
            summaries.forEach { summary ->
                appendLine("- ${summary.kind.toPromptLabel()}: ${summary.toPromptValueText()}")
            }
        }
    }

    private fun ExerciseMetricSummary.toPromptValueText(): String {
        val actualText = when (availability) {
            MetricAvailability.AVAILABLE -> "${formatMetricValue(kind, actualValue ?: 0.0)}"
            MetricAvailability.PERMISSION_MISSING -> "未取得（権限未許可）"
            MetricAvailability.SOURCE_UNAVAILABLE -> "未取得（連携元データなし）"
        }
        val targetText = targetValue?.let { " / 目標 ${formatMetricValue(kind, it)}" }.orEmpty()
        val rateText = achievementRate?.let { " / 達成率 ${formatDecimal(it)}%" }.orEmpty()
        return actualText + targetText + rateText
    }

    private fun formatMetricValue(kind: ExerciseMetricKind, value: Double): String {
        val formatted = when (kind) {
            ExerciseMetricKind.STEP_COUNT -> value.toInt().toString()
            ExerciseMetricKind.EXERCISE_MINUTES -> formatDecimal(value)
            else -> formatDecimal(value)
        }
        return "$formatted${kind.toPromptUnit()}"
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    private fun AppSettings.sanitize(): AppSettings {
        return copy(
            advisorPrompt = advisorPrompt.ifBlank { AppSettings.DEFAULT_ADVISOR_PROMPT }
        )
    }

    private fun AdvicePeriod.toPromptLabel(): String {
        return when (this) {
            AdvicePeriod.WEEKLY -> "週間"
            AdvicePeriod.MONTHLY -> "月間"
            AdvicePeriod.THREE_MONTHS -> "3ヶ月"
        }
    }

    private fun HealthMetricType.toExerciseMetricKind(): ExerciseMetricKind {
        return when (this) {
            HealthMetricType.STEP_COUNT -> ExerciseMetricKind.STEP_COUNT
            HealthMetricType.ACTIVE_KCAL -> ExerciseMetricKind.ACTIVE_KCAL
            HealthMetricType.EXERCISE_MINUTES -> ExerciseMetricKind.EXERCISE_MINUTES
            HealthMetricType.DISTANCE_KM -> ExerciseMetricKind.DISTANCE_KM
        }
    }

    private fun ExerciseMetricKind.toPromptLabel(): String {
        return when (this) {
            ExerciseMetricKind.STEP_COUNT -> "歩数"
            ExerciseMetricKind.ACTIVE_KCAL -> "活動消費kcal"
            ExerciseMetricKind.EXERCISE_MINUTES -> "運動時間"
            ExerciseMetricKind.DISTANCE_KM -> "移動距離"
            ExerciseMetricKind.RINGFIT_KCAL -> "Ring Fit kcal"
            ExerciseMetricKind.RINGFIT_KM -> "Ring Fit km"
        }
    }

    private fun ExerciseMetricKind.toPromptUnit(): String {
        return when (this) {
            ExerciseMetricKind.STEP_COUNT -> "歩"
            ExerciseMetricKind.ACTIVE_KCAL -> "kcal"
            ExerciseMetricKind.EXERCISE_MINUTES -> "分"
            ExerciseMetricKind.DISTANCE_KM -> "km"
            ExerciseMetricKind.RINGFIT_KCAL -> "kcal"
            ExerciseMetricKind.RINGFIT_KM -> "km"
        }
    }

    companion object {
        private val HEALTH_METRIC_TYPES: Set<HealthMetricType> = setOf(
            HealthMetricType.STEP_COUNT,
            HealthMetricType.ACTIVE_KCAL,
            HealthMetricType.EXERCISE_MINUTES,
            HealthMetricType.DISTANCE_KM
        )
    }
}
