package jp.hotdrop.simpledyphic.data.repository

import jp.hotdrop.simpledyphic.data.local.RoomRecordLocalDataSource
import jp.hotdrop.simpledyphic.data.remote.FirestoreRecordRemoteDataSource
import jp.hotdrop.simpledyphic.model.AppCompletable
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.model.appCompletableSuspend
import jp.hotdrop.simpledyphic.model.appResultSuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordRepository @Inject constructor(
    private val localDataSource: RoomRecordLocalDataSource,
    private val remoteDataSource: FirestoreRecordRemoteDataSource,
    private val accountRepository: AccountRepository
) {
    suspend fun find(id: Int): AppResult<Record> {
        return appResultSuspend {
            localDataSource.find(id)
        }
    }

    fun observeAll(): Flow<AppResult<List<Record>>> {
        return localDataSource.observeAll()
            .map { records -> AppResult.Success(records) as AppResult<List<Record>> }
            .catch { error -> emit(AppResult.Failure(error)) }
    }

    suspend fun save(record: Record): AppCompletable {
        return appCompletableSuspend {
            localDataSource.save(record)
        }
    }

    suspend fun backup(): AppCompletable {
        return when (val account = accountRepository.currentAccount()) {
            is AppResult.Failure -> AppCompletable.Failure(account.error)
            is AppResult.Success -> {
                val userId = account.value?.uid
                if (userId == null) {
                    Timber.i("Skip backup because user is not signed in")
                    AppCompletable.Complete
                } else {
                    appCompletableSuspend {
                        remoteDataSource.saveAll(userId = userId, records = localDataSource.findAll())
                    }
                }
            }
        }
    }

    suspend fun restore(): AppCompletable {
        return when (val account = accountRepository.currentAccount()) {
            is AppResult.Failure -> AppCompletable.Failure(account.error)
            is AppResult.Success -> {
                val userId = account.value?.uid
                if (userId == null) {
                    Timber.i("Skip restore because user is not signed in")
                    AppCompletable.Complete
                } else {
                    appCompletableSuspend {
                        val remoteRecords = remoteDataSource.findAll(userId = userId)
                        localDataSource.replaceAll(remoteRecords)
                    }
                }
            }
        }
    }
}
