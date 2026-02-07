package jp.hotdrop.simpledyphic.data.local.source

import javax.inject.Inject
import jp.hotdrop.simpledyphic.data.local.db.RecordDao
import jp.hotdrop.simpledyphic.data.local.db.toEntity
import jp.hotdrop.simpledyphic.data.local.db.toModel
import jp.hotdrop.simpledyphic.domain.model.Record

class RoomRecordLocalDataSource @Inject constructor(
    private val recordDao: RecordDao
) : RecordLocalDataSource {
    override suspend fun find(id: Int): Record {
        val entity = recordDao.findById(id) ?: throw NoSuchElementException("Record not found: $id")
        return entity.toModel()
    }

    override suspend fun findAll(): List<Record> = recordDao.findAll().map { it.toModel() }

    override suspend fun save(record: Record) {
        recordDao.upsert(record.toEntity())
    }

    override suspend fun replaceAll(records: List<Record>) {
        recordDao.deleteAll()
        if (records.isNotEmpty()) {
            recordDao.upsertAll(records.map { it.toEntity() })
        }
    }
}
