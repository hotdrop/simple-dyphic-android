package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = :id")
    suspend fun findById(id: Int = 1): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = :id")
    fun observeById(id: Int = 1): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)
}
