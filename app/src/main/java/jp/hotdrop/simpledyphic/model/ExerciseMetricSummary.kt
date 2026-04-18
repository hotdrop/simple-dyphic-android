package jp.hotdrop.simpledyphic.model

data class ExerciseMetricSummary(
    val kind: ExerciseMetricKind,
    val actualValue: Double? = null,
    val availability: MetricAvailability = MetricAvailability.AVAILABLE,
    val targetValue: Double? = null,
    val achievementRate: Double? = null
)
