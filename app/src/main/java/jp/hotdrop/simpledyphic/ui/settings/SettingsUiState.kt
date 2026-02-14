package jp.hotdrop.simpledyphic.ui.settings

data class SettingsUiState(
    val isLoading: Boolean = false,
    val appVersion: String = "",
    val isSignedIn: Boolean = false,
    val accountName: String? = null,
    val accountEmail: String? = null,
    val showLicenseDialog: Boolean = false,
    val pendingDataSyncAction: SettingsDataSyncAction? = null
)

enum class SettingsDataSyncAction {
    Backup,
    Restore
}
