package jp.hotdrop.simpledyphic.data.local

import jp.hotdrop.simpledyphic.data.local.db.AppSettingsDao
import jp.hotdrop.simpledyphic.data.local.db.toEntity
import jp.hotdrop.simpledyphic.data.local.db.toModel
import jp.hotdrop.simpledyphic.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsLocalDataSource @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) {
    suspend fun find(): AppSettings? = appSettingsDao.findById()?.toModel()

    fun observe(): Flow<AppSettings?> = appSettingsDao.observeById().map { it?.toModel() }

    suspend fun save(settings: AppSettings) {
        appSettingsDao.upsert(settings.toEntity())
    }
}
