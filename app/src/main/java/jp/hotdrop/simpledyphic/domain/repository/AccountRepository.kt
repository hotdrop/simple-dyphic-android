package jp.hotdrop.simpledyphic.domain.repository

import android.content.Context
import jp.hotdrop.simpledyphic.domain.model.UserAccount

interface AccountRepository {
    fun currentAccount(): UserAccount?
    suspend fun signInWithGoogle(context: Context): UserAccount?
    suspend fun signOut()
}
