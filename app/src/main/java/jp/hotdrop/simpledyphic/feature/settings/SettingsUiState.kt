package jp.hotdrop.simpledyphic.feature.settings

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val operationMessage: String? = null,
    val appVersion: String = "",
    val isSignedIn: Boolean = false,
    val accountName: String? = null,
    val accountEmail: String? = null,
    val showLicenseDialog: Boolean = false
)
