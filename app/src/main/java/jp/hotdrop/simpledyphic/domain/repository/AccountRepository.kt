package jp.hotdrop.simpledyphic.domain.repository

import jp.hotdrop.simpledyphic.domain.model.UserAccount

interface AccountRepository {
    fun currentAccount(): UserAccount?
    suspend fun signInWithGoogle(): UserAccount
    suspend fun signOut()
}
