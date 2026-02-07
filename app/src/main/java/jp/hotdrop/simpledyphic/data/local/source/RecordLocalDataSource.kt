package jp.hotdrop.simpledyphic.data.local.source

import jp.hotdrop.simpledyphic.domain.model.Record

interface RecordLocalDataSource {
    suspend fun find(id: Int): Record
    suspend fun findAll(): List<Record>
    suspend fun save(record: Record)
    suspend fun replaceAll(records: List<Record>)
}
