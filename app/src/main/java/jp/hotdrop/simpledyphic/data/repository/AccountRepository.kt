package jp.hotdrop.simpledyphic.data.repository

import jp.hotdrop.simpledyphic.data.remote.FirebaseAuthRemoteDataSource
import jp.hotdrop.simpledyphic.model.UserAccount
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val authRemoteDataSource: FirebaseAuthRemoteDataSource
) {
    fun currentAccount(): UserAccount? = authRemoteDataSource.currentAccount()

    suspend fun signInWithGoogle(): UserAccount {
        return authRemoteDataSource.signInWithGoogle()
    }

    suspend fun signOut() {
        authRemoteDataSource.signOut()
    }
}