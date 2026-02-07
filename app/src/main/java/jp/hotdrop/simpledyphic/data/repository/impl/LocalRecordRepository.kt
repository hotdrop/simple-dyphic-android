package jp.hotdrop.simpledyphic.data.repository.impl

import javax.inject.Inject
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.data.local.source.RecordLocalDataSource
import jp.hotdrop.simpledyphic.domain.model.Record
import jp.hotdrop.simpledyphic.domain.repository.RecordRepository

@Singleton
class LocalRecordRepository @Inject constructor(
    private val localDataSource: RecordLocalDataSource
) : RecordRepository {
    override suspend fun find(id: Int): Record = localDataSource.find(id)

    override suspend fun findAll(): List<Record> = localDataSource.findAll()

    override suspend fun save(record: Record) {
        localDataSource.save(record)
    }
}
