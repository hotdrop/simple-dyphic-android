package jp.hotdrop.simpledyphic.data.remote.auth

import jp.hotdrop.simpledyphic.domain.model.UserAccount

interface AuthRemoteDataSource {
    fun currentAccount(): UserAccount?
    suspend fun signInWithGoogle(): UserAccount
    suspend fun signOut()
}
