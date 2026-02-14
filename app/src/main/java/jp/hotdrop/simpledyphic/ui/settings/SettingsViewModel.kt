package jp.hotdrop.simpledyphic.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.data.repository.AccountRepository
import jp.hotdrop.simpledyphic.data.repository.RecordRepository
import jp.hotdrop.simpledyphic.model.AppCompletable
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.UserAccount
import jp.hotdrop.simpledyphic.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val accountRepository: AccountRepository,
    private val recordRepository: RecordRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(appVersion = resolveAppVersion()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshCurrentAccount()
    }

    fun onRetry() {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessageResId = null,
                operationMessageResId = null
            )
        }
    }

    fun onLicenseClick() {
        _uiState.update { it.copy(showLicenseDialog = true) }
    }

    fun onLicenseDismiss() {
        _uiState.update { it.copy(showLicenseDialog = false) }
    }

    fun onOperationMessageDismiss() {
        _uiState.update { it.copy(operationMessageResId = null) }
    }

    fun onSignInClick() {
        if (_uiState.value.isLoading) return
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessageResId = null,
                operationMessageResId = null
            )
        }
        launch {
            when (val result = dispatcherIO { accountRepository.signInWithGoogle() }) {
                is AppResult.Success -> {
                    applyAccount(result.value)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationMessageResId = R.string.settings_operation_sign_in_success
                        )
                    }
                }
                is AppResult.Failure -> {
                    Timber.e(result.error, "Sign-in failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationMessageResId = R.string.settings_operation_failed
                        )
                    }
                }
            }
        }
    }

    fun onSignOutClick() {
        if (_uiState.value.isLoading) return
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessageResId = null,
                operationMessageResId = null
            )
        }
        launch {
            when (val result = dispatcherIO { accountRepository.signOut() }) {
                AppCompletable.Complete -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = false,
                            accountName = null,
                            accountEmail = null,
                            operationMessageResId = R.string.settings_operation_sign_out_success
                        )
                    }
                }
                is AppCompletable.Failure -> {
                    Timber.e(result.error, "Sign-out failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationMessageResId = R.string.settings_operation_failed
                        )
                    }
                }
            }
        }
    }

    fun onBackupClick() {
        if (_uiState.value.isLoading) return
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessageResId = null,
                operationMessageResId = null
            )
        }
        launch {
            when (val result = dispatcherIO { recordRepository.backup() }) {
                AppCompletable.Complete -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationMessageResId = R.string.settings_operation_backup_success
                        )
                    }
                }
                is AppCompletable.Failure -> {
                    Timber.e(result.error, "Backup failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationMessageResId = R.string.settings_operation_failed
                        )
                    }
                }
            }
        }
    }

    fun onRestoreClick() {
        if (_uiState.value.isLoading) return
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessageResId = null,
                operationMessageResId = null
            )
        }
        launch {
            when (val result = dispatcherIO { recordRepository.restore() }) {
                AppCompletable.Complete -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationMessageResId = R.string.settings_operation_restore_success
                        )
                    }
                }
                is AppCompletable.Failure -> {
                    Timber.e(result.error, "Restore failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationMessageResId = R.string.settings_operation_failed
                        )
                    }
                }
            }
        }
    }

    private fun refreshCurrentAccount() {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessageResId = null,
                operationMessageResId = null
            )
        }
        launch {
            when (val result = dispatcherIO { accountRepository.currentAccount() }) {
                is AppResult.Success -> {
                    val account = result.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = account != null,
                            accountName = account?.name,
                            accountEmail = account?.email
                        )
                    }
                }
                is AppResult.Failure -> {
                    Timber.e(result.error, "Failed to resolve current account")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationMessageResId = R.string.settings_operation_failed
                        )
                    }
                }
            }
        }
    }

    private fun resolveAppVersion(): String {
        return try {
            val packageInfo = appContext.packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
            "${packageInfo.versionName ?: appContext.getString(R.string.common_unknown)} (${packageInfo.longVersionCode})"
        } catch (error: Throwable) {
            Timber.e(error, "Failed to resolve app version")
            appContext.getString(R.string.common_unknown)
        }
    }

    private fun applyAccount(account: UserAccount) {
        _uiState.update {
            it.copy(
                isSignedIn = true,
                accountName = account.name,
                accountEmail = account.email
            )
        }
    }
}
