package jp.hotdrop.simpledyphic.data.remote.firestore

import jp.hotdrop.simpledyphic.domain.model.Record

interface RecordRemoteDataSource {
    suspend fun findAll(userId: String): List<Record>
    suspend fun saveAll(userId: String, records: List<Record>)
}
