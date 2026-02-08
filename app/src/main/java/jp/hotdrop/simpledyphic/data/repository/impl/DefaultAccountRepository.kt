package jp.hotdrop.simpledyphic.data.repository.impl

import javax.inject.Inject
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.data.remote.auth.AuthRemoteDataSource
import jp.hotdrop.simpledyphic.domain.model.UserAccount
import jp.hotdrop.simpledyphic.domain.repository.AccountRepository

@Singleton
class DefaultAccountRepository @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource
) : AccountRepository {
    override fun currentAccount(): UserAccount? = authRemoteDataSource.currentAccount()

    override suspend fun signInWithGoogle(): UserAccount {
        return authRemoteDataSource.signInWithGoogle()
    }

    override suspend fun signOut() {
        authRemoteDataSource.signOut()
    }
}
