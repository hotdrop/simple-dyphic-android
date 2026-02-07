package jp.hotdrop.simpledyphic.domain.repository

import jp.hotdrop.simpledyphic.domain.model.Record

interface RecordRepository {
    suspend fun find(id: Int): Record
    suspend fun findAll(): List<Record>
    suspend fun save(record: Record)
}
