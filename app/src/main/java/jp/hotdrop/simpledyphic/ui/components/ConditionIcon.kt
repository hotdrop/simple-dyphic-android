package jp.hotdrop.simpledyphic.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import jp.hotdrop.simpledyphic.model.ConditionType

@Composable
fun ConditionIcon(
    type: ConditionType,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = type.icon(),
        contentDescription = null,
        tint = if (selected) type.tint() else Color.Gray,
        modifier = modifier
    )
}

private fun ConditionType.icon(): ImageVector {
    return when (this) {
        ConditionType.BAD -> Icons.Rounded.SentimentVeryDissatisfied
        ConditionType.NORMAL -> Icons.Rounded.SentimentSatisfied
        ConditionType.GOOD -> Icons.Rounded.SentimentVerySatisfied
    }
}

private fun ConditionType.tint(): Color {
    return when (this) {
        ConditionType.BAD -> Color.Red
        ConditionType.NORMAL -> Color(0xFFFFA500)
        ConditionType.GOOD -> Color.Blue
    }
}
