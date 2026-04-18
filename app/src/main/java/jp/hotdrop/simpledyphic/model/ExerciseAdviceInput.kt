package jp.hotdrop.simpledyphic.model

import java.time.LocalDate

data class ExerciseAdviceInput(
    val period: AdvicePeriod,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val elapsedDays: Long,
    val age: Int?,
    val settings: AppSettings,
    val summaries: List<ExerciseMetricSummary>,
    val missingRequirements: Set<ExerciseAdviceRequirement>,
    val promptDataBlock: String
) {
    val canGenerate: Boolean
        get() = missingRequirements.isEmpty()
}
