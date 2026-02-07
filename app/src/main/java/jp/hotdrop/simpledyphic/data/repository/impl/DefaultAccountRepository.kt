package jp.hotdrop.simpledyphic.data.repository.impl

import android.content.Context
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

    override suspend fun signInWithGoogle(context: Context): UserAccount? {
        return authRemoteDataSource.signInWithGoogle(context)
    }

    override suspend fun signOut() {
        authRemoteDataSource.signOut()
    }
}
