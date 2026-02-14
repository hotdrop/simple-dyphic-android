package jp.hotdrop.simpledyphic.data.repository

import jp.hotdrop.simpledyphic.data.remote.FirebaseAuthRemoteDataSource
import jp.hotdrop.simpledyphic.model.AppCompletable
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.UserAccount
import jp.hotdrop.simpledyphic.model.appCompletableSuspend
import jp.hotdrop.simpledyphic.model.appResultSuspend
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val authRemoteDataSource: FirebaseAuthRemoteDataSource
) {
    fun currentAccount(): AppResult<UserAccount?> {
        return try {
            AppResult.Success(authRemoteDataSource.currentAccount())
        } catch (error: Throwable) {
            AppResult.Failure(error)
        }
    }

    suspend fun signInWithGoogle(): AppResult<UserAccount> {
        return appResultSuspend {
            authRemoteDataSource.signInWithGoogle()
        }
    }

    suspend fun signOut(): AppCompletable {
        return appCompletableSuspend {
            authRemoteDataSource.signOut()
        }
    }
}
