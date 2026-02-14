package jp.hotdrop.simpledyphic.data.remote

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.hotdrop.simpledyphic.model.UserAccount
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRemoteDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager
) {

    fun currentAccount(): UserAccount? {
        val user = firebaseAuth.currentUser ?: return null
        return UserAccount(
            uid = user.uid,
            name = user.displayName,
            email = user.email
        )
    }

    suspend fun signInWithGoogle(): UserAccount {
        if (firebaseAuth.currentUser != null) {
            return currentAccount() ?: error("Failed to resolve signed-in account.")
        }

        val result = executeCredentialRequest()
        val credential = result.credential
        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Unsupported credential type: ${credential::class.simpleName}")
        }

        val googleCredential = GoogleIdTokenCredential.Companion.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        firebaseAuth.signInWithCredential(firebaseCredential).await()

        return currentAccount() ?: error("Failed to resolve signed-in account.")
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (error: ClearCredentialException) {
            throw IllegalStateException("Failed to clear credential state.", error)
        }
    }

    private suspend fun executeCredentialRequest(): GetCredentialResponse {
        val serverClientId = resolveWebClientId()

        return try {
            requestGoogleCredential(
                serverClientId = serverClientId,
                filterAuthorizedAccounts = true
            )
        } catch (error: GetCredentialException) {
            if (error is NoCredentialException) {
                try {
                    requestGoogleCredential(
                        serverClientId = serverClientId,
                        filterAuthorizedAccounts = false
                    )
                } catch (fallbackError: GetCredentialException) {
                    throw IllegalStateException(
                        "Failed to acquire Google credential.",
                        fallbackError
                    )
                }
            } else {
                throw IllegalStateException("Failed to acquire Google credential.", error)
            }
        }
    }

    private suspend fun requestGoogleCredential(
        serverClientId: String,
        filterAuthorizedAccounts: Boolean
    ): GetCredentialResponse {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return credentialManager.getCredential(
            context = context,
            request = request
        )
    }

    private fun resolveWebClientId(): String {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        check(resourceId != 0) { "default_web_client_id is not defined in resources." }
        return context.getString(resourceId)
    }
}
