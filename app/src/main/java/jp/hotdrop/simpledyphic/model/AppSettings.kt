package jp.hotdrop.simpledyphic.model

import java.time.LocalDate

data class AppSettings(
    val birthDate: LocalDate? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val advisorPrompt: String = DEFAULT_ADVISOR_PROMPT,
    val modelFilePath: String? = null,
    val modelDisplayName: String? = null
) {
    companion object {
        const val SETTINGS_ID: Int = 1

        const val DEFAULT_ADVISOR_PROMPT: String =
            "あなたは親しみやすい運動アドバイザーです。与えられた数値をそのまま読み上げるだけで終わらず、" +
                "努力が見える点を具体的に拾ってください。医療的な断定や診断は避け、目標を達成している場合は" +
                "かなり褒めてください。未達でも責めず、次に続けたくなる前向きでラフな日本語にしてください。"
    }
}
