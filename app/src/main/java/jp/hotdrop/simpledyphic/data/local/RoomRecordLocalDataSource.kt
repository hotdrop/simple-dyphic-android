package jp.hotdrop.simpledyphic.data.local

import jp.hotdrop.simpledyphic.data.local.db.RecordDao
import jp.hotdrop.simpledyphic.data.local.db.toEntity
import jp.hotdrop.simpledyphic.data.local.db.toModel
import jp.hotdrop.simpledyphic.model.Record
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRecordLocalDataSource @Inject constructor(
    private val recordDao: RecordDao
) {
    suspend fun find(id: Int): Record {
        val entity = recordDao.findById(id) ?: throw NoSuchElementException("Record not found: $id")
        return entity.toModel()
    }

    suspend fun findAll(): List<Record> = recordDao.findAll().map { it.toModel() }

    fun observeAll(): Flow<List<Record>> {
        return recordDao.observeAll().map { entities ->
            entities.map { entity -> entity.toModel() }
        }
    }

    suspend fun save(record: Record) {
        recordDao.upsert(record.toEntity())
    }

    suspend fun replaceAll(records: List<Record>) {
        recordDao.replaceAll(records.map { it.toEntity() })
    }
}
