package jp.hotdrop.simpledyphic.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun findById(id: Int): RecordEntity?

    @Query("SELECT * FROM records ORDER BY id ASC")
    suspend fun findAll(): List<RecordEntity>

    @Query("SELECT * FROM records ORDER BY id ASC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id BETWEEN :startId AND :endId ORDER BY id ASC")
    suspend fun findBetween(startId: Int, endId: Int): List<RecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: RecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<RecordEntity>)

    @Query("DELETE FROM records")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(records: List<RecordEntity>) {
        deleteAll()
        if (records.isNotEmpty()) {
            upsertAll(records)
        }
    }
}
