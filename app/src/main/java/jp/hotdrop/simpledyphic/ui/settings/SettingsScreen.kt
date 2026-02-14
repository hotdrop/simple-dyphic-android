package jp.hotdrop.simpledyphic.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onLicenseClick = viewModel::onLicenseClick,
        onLicenseDismiss = viewModel::onLicenseDismiss,
        onSignInClick = viewModel::onSignInClick,
        onSignOutClick = viewModel::onSignOutClick,
        onBackupClick = viewModel::onBackupClick,
        onRestoreClick = viewModel::onRestoreClick,
        onDataSyncActionConfirm = viewModel::onDataSyncActionConfirm,
        onDataSyncActionDismiss = viewModel::onDataSyncActionDismiss
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onLicenseClick: () -> Unit,
    onLicenseDismiss: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDataSyncActionConfirm: () -> Unit,
    onDataSyncActionDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsContent(
        uiState = uiState,
        onLicenseClick = onLicenseClick,
        onSignInClick = onSignInClick,
        onSignOutClick = onSignOutClick,
        onBackupClick = onBackupClick,
        onRestoreClick = onRestoreClick,
        modifier = modifier
    )

    if (uiState.showLicenseDialog) {
        AlertDialog(
            onDismissRequest = onLicenseDismiss,
            title = { Text(text = stringResource(R.string.settings_license_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_version_value, uiState.appVersion),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_license_dialog_body),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = onLicenseDismiss) {
                    Text(text = stringResource(R.string.settings_license_dialog_close))
                }
            }
        )
    }

    uiState.pendingDataSyncAction?.let { action ->
        val titleResId = when (action) {
            SettingsDataSyncAction.Backup -> R.string.settings_backup_confirm_dialog_title
            SettingsDataSyncAction.Restore -> R.string.settings_restore_confirm_dialog_title
        }
        val messageResId = when (action) {
            SettingsDataSyncAction.Backup -> R.string.settings_backup_confirm_dialog_message
            SettingsDataSyncAction.Restore -> R.string.settings_restore_confirm_dialog_message
        }
        AlertDialog(
            onDismissRequest = onDataSyncActionDismiss,
            title = { Text(text = stringResource(titleResId)) },
            text = { Text(text = stringResource(messageResId)) },
            confirmButton = {
                Button(
                    onClick = onDataSyncActionConfirm,
                    modifier = Modifier.testTag("settings_data_sync_confirm_button")
                ) {
                    Text(text = stringResource(R.string.settings_data_sync_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDataSyncActionDismiss,
                    modifier = Modifier.testTag("settings_data_sync_cancel_button")
                ) {
                    Text(text = stringResource(R.string.settings_data_sync_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onLicenseClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.tab_settings)) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.isLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(text = uiState.accountName ?: stringResource(R.string.settings_signed_out_placeholder)) },
                    supportingContent = { Text(text = uiState.accountEmail ?: stringResource(R.string.settings_signed_out_placeholder)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null
                        )
                    }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.settings_version_license)) },
                    supportingContent = {
                        Text(text = stringResource(R.string.settings_version_value, uiState.appVersion))
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Note,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !uiState.isLoading,
                            onClick = onLicenseClick
                        )
                )
                HorizontalDivider()
            }

            if (uiState.isSignedIn) {
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.settings_backup_title)) },
                        supportingContent = { Text(text = stringResource(R.string.settings_backup_summary)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Backup,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_backup_item")
                            .clickable(
                                enabled = !uiState.isLoading,
                                onClick = onBackupClick
                            )
                    )
                    HorizontalDivider()
                }

                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.settings_restore_title)) },
                        supportingContent = { Text(text = stringResource(R.string.settings_restore_summary)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.SettingsBackupRestore,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !uiState.isLoading,
                                onClick = onRestoreClick
                            )
                    )
                    HorizontalDivider()
                }

                item {
                    OutlinedButton(
                        onClick = onSignOutClick,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        Text(text = stringResource(R.string.settings_sign_out))
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = onSignInClick,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = stringResource(R.string.settings_sign_in))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenSignedOutPreview() {
    SimpleDyphicTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                appVersion = "1.0 (1)",
                isSignedIn = false
            ),
            onLicenseClick = {},
            onLicenseDismiss = {},
            onSignInClick = {},
            onSignOutClick = {},
            onBackupClick = {},
            onRestoreClick = {},
            onDataSyncActionConfirm = {},
            onDataSyncActionDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenSignedInPreview() {
    SimpleDyphicTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                appVersion = "1.0 (1)",
                isSignedIn = true,
                accountName = "Google User",
                accountEmail = "user@example.com"
            ),
            onLicenseClick = {},
            onLicenseDismiss = {},
            onSignInClick = {},
            onSignOutClick = {},
            onBackupClick = {},
            onRestoreClick = {},
            onDataSyncActionConfirm = {},
            onDataSyncActionDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenBackupConfirmPreview() {
    SimpleDyphicTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                appVersion = "1.0 (1)",
                isSignedIn = true,
                accountName = "Google User",
                accountEmail = "user@example.com",
                pendingDataSyncAction = SettingsDataSyncAction.Backup
            ),
            onLicenseClick = {},
            onLicenseDismiss = {},
            onSignInClick = {},
            onSignOutClick = {},
            onBackupClick = {},
            onRestoreClick = {},
            onDataSyncActionConfirm = {},
            onDataSyncActionDismiss = {}
        )
    }
}
