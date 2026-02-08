package jp.hotdrop.simpledyphic.data.mapper

import jp.hotdrop.simpledyphic.domain.model.ConditionType

private const val CONDITION_BAD = "悪い"
private const val CONDITION_NORMAL = "普通"
private const val CONDITION_GOOD = "良い"

fun String?.toConditionType(): ConditionType? {
    return when (this) {
        CONDITION_BAD -> ConditionType.BAD
        CONDITION_NORMAL -> ConditionType.NORMAL
        CONDITION_GOOD -> ConditionType.GOOD
        else -> null
    }
}

fun ConditionType.toRawCondition(): String {
    return when (this) {
        ConditionType.BAD -> CONDITION_BAD
        ConditionType.NORMAL -> CONDITION_NORMAL
        ConditionType.GOOD -> CONDITION_GOOD
    }
}
