package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import jp.hotdrop.simpledyphic.model.AppSettings

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = AppSettings.SETTINGS_ID,
    val birthDate: String?,
    val heightCm: Double?,
    val weightKg: Double?,
    val advisorPrompt: String,
    val modelFilePath: String?,
    val modelDisplayName: String?
)

fun AppSettingsEntity.toModel(): AppSettings {
    return AppSettings(
        birthDate = birthDate?.let(LocalDate::parse),
        heightCm = heightCm,
        weightKg = weightKg,
        advisorPrompt = advisorPrompt.ifBlank { AppSettings.DEFAULT_ADVISOR_PROMPT },
        modelFilePath = modelFilePath,
        modelDisplayName = modelDisplayName
    )
}

fun AppSettings.toEntity(): AppSettingsEntity {
    return AppSettingsEntity(
        id = AppSettings.SETTINGS_ID,
        birthDate = birthDate?.toString(),
        heightCm = heightCm,
        weightKg = weightKg,
        advisorPrompt = advisorPrompt.ifBlank { AppSettings.DEFAULT_ADVISOR_PROMPT },
        modelFilePath = modelFilePath,
        modelDisplayName = modelDisplayName
    )
}
