package jp.hotdrop.simpledyphic.model

data class HealthMetricValue(
    val availability: MetricAvailability,
    val value: Double? = null
)
