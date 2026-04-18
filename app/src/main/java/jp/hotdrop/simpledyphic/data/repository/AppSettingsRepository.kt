package jp.hotdrop.simpledyphic.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.data.local.AppSettingsLocalDataSource
import jp.hotdrop.simpledyphic.model.AppCompletable
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.AppSettings
import jp.hotdrop.simpledyphic.model.appCompletableSuspend
import jp.hotdrop.simpledyphic.model.appResultSuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
open class AppSettingsRepository @Inject constructor(
    private val localDataSource: AppSettingsLocalDataSource,
    @param:ApplicationContext private val context: Context
) {
    open fun observe(): Flow<AppResult<AppSettings>> {
        return localDataSource.observe()
            .map { AppResult.Success(it ?: AppSettings()) as AppResult<AppSettings> }
            .catch { error -> emit(AppResult.Failure(error)) }
    }

    open suspend fun get(): AppResult<AppSettings> {
        return appResultSuspend {
            localDataSource.find() ?: AppSettings()
        }
    }

    open suspend fun save(settings: AppSettings): AppCompletable {
        return appCompletableSuspend {
            localDataSource.save(
                settings.copy(
                    advisorPrompt = settings.advisorPrompt.ifBlank { AppSettings.DEFAULT_ADVISOR_PROMPT }
                )
            )
        }
    }

    open suspend fun importModelFile(uri: Uri): AppResult<ModelFileSelection> {
        return appResultSuspend {
            val displayName = resolveDisplayName(uri)
            require(displayName.endsWith(".litertlm", ignoreCase = true)) {
                "LiteRT-LM model must have .litertlm extension"
            }
            val targetDir = File(context.filesDir, "litertlm-models").apply { mkdirs() }
            val sanitizedName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val targetFile = File(targetDir, sanitizedName)

            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Failed to open model file" }
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            ModelFileSelection(
                displayName = displayName,
                absolutePath = targetFile.absolutePath
            )
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(index)
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "selected-model.litertlm"
    }
}

data class ModelFileSelection(
    val displayName: String,
    val absolutePath: String
)
