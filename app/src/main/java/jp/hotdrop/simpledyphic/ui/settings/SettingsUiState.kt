package jp.hotdrop.simpledyphic.ui.settings

import androidx.annotation.StringRes

data class SettingsUiState(
    val isLoading: Boolean = false,
    @param:StringRes val errorMessageResId: Int? = null,
    @param:StringRes val operationMessageResId: Int? = null,
    val appVersion: String = "",
    val isSignedIn: Boolean = false,
    val accountName: String? = null,
    val accountEmail: String? = null,
    val showLicenseDialog: Boolean = false
)
