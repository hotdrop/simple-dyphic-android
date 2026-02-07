package jp.hotdrop.simpledyphic.data.remote.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject
import jp.hotdrop.simpledyphic.domain.model.UserAccount
import kotlinx.coroutines.tasks.await

class FirebaseAuthRemoteDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager
) : AuthRemoteDataSource {

    override fun currentAccount(): UserAccount? {
        val user = firebaseAuth.currentUser ?: return null
        return UserAccount(
            uid = user.uid,
            name = user.displayName,
            email = user.email
        )
    }

    override suspend fun signInWithGoogle(context: Context): UserAccount? {
        if (firebaseAuth.currentUser != null) {
            return currentAccount()
        }

        val serverClientId = resolveWebClientId(context)
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val result = credentialManager.getCredential(
            context = context,
            request = request
        )
        val credential = result.credential
        if (credential !is CustomCredential) {
            throw IllegalStateException("Unexpected credential type: ${credential::class.java.simpleName}")
        }
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("Unexpected credential custom type: ${credential.type}")
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        firebaseAuth.signInWithCredential(firebaseCredential).await()
        return currentAccount()
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun resolveWebClientId(context: Context): String {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        check(resourceId != 0) { "default_web_client_id is not defined in resources." }
        return context.getString(resourceId)
    }
}
