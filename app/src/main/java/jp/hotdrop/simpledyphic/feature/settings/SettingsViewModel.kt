package jp.hotdrop.simpledyphic.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.hotdrop.simpledyphic.core.log.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appLogger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            appVersion = resolveAppVersion()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        appLogger.i("SettingsViewModel initialized")
    }

    fun onRetry() {
        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
    }

    fun onLicenseClick() {
        _uiState.update { it.copy(showLicenseDialog = true) }
    }

    fun onLicenseDismiss() {
        _uiState.update { it.copy(showLicenseDialog = false) }
    }

    fun onSignInClick() {
        appLogger.i("Sign-in skeleton action invoked")
        _uiState.update {
            it.copy(
                isSignedIn = true,
                accountName = null,
                accountEmail = null
            )
        }
    }

    fun onSignOutClick() {
        appLogger.i("Sign-out skeleton action invoked")
        _uiState.update {
            it.copy(
                isSignedIn = false,
                accountName = null,
                accountEmail = null
            )
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
            appLogger.e("Failed to resolve app version", it)
            "Unknown"
        }
    }
}
