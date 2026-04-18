package jp.hotdrop.simpledyphic.data.repository

import jp.hotdrop.simpledyphic.data.local.RoomRecordLocalDataSource
import jp.hotdrop.simpledyphic.data.remote.FirestoreRecordRemoteDataSource
import jp.hotdrop.simpledyphic.model.AppCompletable
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.DyphicId
import jp.hotdrop.simpledyphic.model.Record
import jp.hotdrop.simpledyphic.model.appCompletableSuspend
import jp.hotdrop.simpledyphic.model.appResultSuspend
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class RecordRepository @Inject constructor(
    private val localDataSource: RoomRecordLocalDataSource,
    private val remoteDataSource: FirestoreRecordRemoteDataSource,
    private val accountRepository: AccountRepository
) {
    open suspend fun find(id: Int): AppResult<Record> {
        return appResultSuspend {
            localDataSource.find(id)
        }
    }

    open suspend fun findAll(): AppResult<List<Record>> {
        return appResultSuspend {
            localDataSource.findAll()
        }
    }

    open suspend fun findByDateRange(start: LocalDate, end: LocalDate): AppResult<List<Record>> {
        return appResultSuspend {
            require(!start.isAfter(end)) { "start must be on/before end" }
            localDataSource.findBetween(
                startId = DyphicId.dateToId(start),
                endId = DyphicId.dateToId(end)
            )
        }
    }

    open fun observeAll(): Flow<AppResult<List<Record>>> {
        return localDataSource.observeAll()
            .map { records -> AppResult.Success(records) as AppResult<List<Record>> }
            .catch { error -> emit(AppResult.Failure(error)) }
    }

    open suspend fun save(record: Record): AppCompletable {
        return appCompletableSuspend {
            localDataSource.save(record)
        }
    }

    open suspend fun backup(): AppCompletable {
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

    open suspend fun restore(): AppCompletable {
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
