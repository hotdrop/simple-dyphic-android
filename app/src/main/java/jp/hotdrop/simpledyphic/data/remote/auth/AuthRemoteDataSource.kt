package jp.hotdrop.simpledyphic.data.remote.auth

import android.content.Context
import jp.hotdrop.simpledyphic.domain.model.UserAccount

interface AuthRemoteDataSource {
    fun currentAccount(): UserAccount?
    suspend fun signInWithGoogle(context: Context): UserAccount?
    suspend fun signOut()
}
