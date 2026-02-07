package jp.hotdrop.simpledyphic.data.repository.impl

import javax.inject.Inject
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.core.log.AppLogger
import jp.hotdrop.simpledyphic.data.local.source.RecordLocalDataSource
import jp.hotdrop.simpledyphic.data.remote.firestore.RecordRemoteDataSource
import jp.hotdrop.simpledyphic.domain.model.Record
import jp.hotdrop.simpledyphic.domain.repository.AccountRepository
import jp.hotdrop.simpledyphic.domain.repository.RecordRepository

@Singleton
class LocalRecordRepository @Inject constructor(
    private val localDataSource: RecordLocalDataSource,
    private val remoteDataSource: RecordRemoteDataSource,
    private val accountRepository: AccountRepository,
    private val appLogger: AppLogger
) : RecordRepository {
    override suspend fun find(id: Int): Record = localDataSource.find(id)

    override suspend fun findAll(): List<Record> = localDataSource.findAll()

    override suspend fun save(record: Record) {
        localDataSource.save(record)
    }

    override suspend fun backup() {
        val userId = accountRepository.currentAccount()?.uid
        if (userId == null) {
            appLogger.i("Skip backup because user is not signed in")
            return
        }
        remoteDataSource.saveAll(userId = userId, records = localDataSource.findAll())
    }

    override suspend fun restore() {
        val userId = accountRepository.currentAccount()?.uid
        if (userId == null) {
            appLogger.i("Skip restore because user is not signed in")
            return
        }
        val remoteRecords = remoteDataSource.findAll(userId = userId)
        localDataSource.replaceAll(remoteRecords)
    }
}
