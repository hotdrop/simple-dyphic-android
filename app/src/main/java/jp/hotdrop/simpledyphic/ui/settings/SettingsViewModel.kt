package jp.hotdrop.simpledyphic.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.hotdrop.simpledyphic.data.repository.AccountRepository
import jp.hotdrop.simpledyphic.data.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val accountRepository: AccountRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(appVersion = resolveAppVersion())
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        syncCurrentAccount()
    }

    fun onRetry() {
        _uiState.update { it.copy(isLoading = false, errorMessage = null, operationMessage = null) }
    }

    fun onLicenseClick() {
        _uiState.update { it.copy(showLicenseDialog = true) }
    }

    fun onLicenseDismiss() {
        _uiState.update { it.copy(showLicenseDialog = false) }
    }

    fun onOperationMessageDismiss() {
        _uiState.update { it.copy(operationMessage = null) }
    }

    suspend fun onSignInClick() {
        executeOperation(
            actionName = "Sign-in",
            successMessage = "Signed in successfully."
        ) {
            accountRepository.signInWithGoogle()
            syncCurrentAccount()
        }
    }

    suspend fun onSignOutClick() {
        executeOperation(
            actionName = "Sign-out",
            successMessage = "Signed out successfully."
        ) {
            accountRepository.signOut()
            syncCurrentAccount()
        }
    }

    suspend fun onBackupClick() {
        executeOperation(
            actionName = "Backup",
            successMessage = "Backup completed."
        ) {
            recordRepository.backup()
        }
    }

    suspend fun onRestoreClick() {
        executeOperation(
            actionName = "Restore",
            successMessage = "Restore completed."
        ) {
            recordRepository.restore()
        }
    }

    private fun syncCurrentAccount() {
        val account = accountRepository.currentAccount()
        _uiState.update {
            it.copy(
                isSignedIn = account != null,
                accountName = account?.name,
                accountEmail = account?.email
            )
        }
    }

    private suspend fun executeOperation(
        actionName: String,
        successMessage: String,
        action: suspend () -> Unit
    ) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                operationMessage = null
            )
        }
        runCatching {
            action()
        }.onSuccess {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    operationMessage = successMessage
                )
            }
        }.onFailure { error ->
            Timber.e(error, "$actionName failed")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    operationMessage = error.message ?: "$actionName failed."
                )
            }
        }
    }

    private fun resolveAppVersion(): String {
        return runCatching {
            val packageInfo = appContext.packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
            "${packageInfo.versionName ?: "Unknown"} (${packageInfo.longVersionCode})"
        }.getOrElse {
            Timber.e(it, "Failed to resolve app version")
            "Unknown"
        }
    }
}
