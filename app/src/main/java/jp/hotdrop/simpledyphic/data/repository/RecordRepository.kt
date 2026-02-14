package jp.hotdrop.simpledyphic.data.repository

import jp.hotdrop.simpledyphic.data.local.RoomRecordLocalDataSource
import jp.hotdrop.simpledyphic.data.remote.FirestoreRecordRemoteDataSource
import jp.hotdrop.simpledyphic.model.Record
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordRepository @Inject constructor(
    private val localDataSource: RoomRecordLocalDataSource,
    private val remoteDataSource: FirestoreRecordRemoteDataSource,
    private val accountRepository: AccountRepository
) {
    suspend fun find(id: Int): Record = localDataSource.find(id)

    suspend fun findAll(): List<Record> = localDataSource.findAll()

    suspend fun save(record: Record) {
        localDataSource.save(record)
    }

    suspend fun backup() {
        val userId = accountRepository.currentAccount()?.uid
        if (userId == null) {
            Timber.i("Skip backup because user is not signed in")
            return
        }
        remoteDataSource.saveAll(userId = userId, records = localDataSource.findAll())
    }

    suspend fun restore() {
        val userId = accountRepository.currentAccount()?.uid
        if (userId == null) {
            Timber.i("Skip restore because user is not signed in")
            return
        }
        val remoteRecords = remoteDataSource.findAll(userId = userId)
        localDataSource.replaceAll(remoteRecords)
    }
}