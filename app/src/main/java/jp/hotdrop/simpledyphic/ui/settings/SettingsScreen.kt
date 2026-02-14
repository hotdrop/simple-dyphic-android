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
import jp.hotdrop.simpledyphic.ui.components.ErrorContent
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onRetry = viewModel::onRetry,
        onLicenseClick = viewModel::onLicenseClick,
        onLicenseDismiss = viewModel::onLicenseDismiss,
        onOperationMessageDismiss = viewModel::onOperationMessageDismiss,
        onSignInClick = viewModel::onSignInClick,
        onSignOutClick = viewModel::onSignOutClick,
        onBackupClick = viewModel::onBackupClick,
        onRestoreClick = viewModel::onRestoreClick
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onRetry: () -> Unit,
    onLicenseClick: () -> Unit,
    onLicenseDismiss: () -> Unit,
    onOperationMessageDismiss: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.errorMessageResId != null -> ErrorContent(
            message = stringResource(uiState.errorMessageResId),
            onRetry = onRetry,
            modifier = modifier
        )

        else -> SettingsContent(
            uiState = uiState,
            onLicenseClick = onLicenseClick,
            onOperationMessageDismiss = onOperationMessageDismiss,
            onSignInClick = onSignInClick,
            onSignOutClick = onSignOutClick,
            onBackupClick = onBackupClick,
            onRestoreClick = onRestoreClick,
            modifier = modifier
        )
    }

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onLicenseClick: () -> Unit,
    onOperationMessageDismiss: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountName = uiState.accountName
        ?: if (uiState.isSignedIn) {
            stringResource(R.string.settings_signed_in_name_placeholder)
        } else {
            stringResource(R.string.settings_signed_out_placeholder)
        }
    val accountEmail = uiState.accountEmail
        ?: if (uiState.isSignedIn) {
            stringResource(R.string.settings_signed_in_email_placeholder)
        } else {
            stringResource(R.string.settings_signed_out_placeholder)
        }

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

            uiState.operationMessageResId?.let { messageResId ->
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(messageResId)) },
                        trailingContent = {
                            OutlinedButton(onClick = onOperationMessageDismiss) {
                                Text(text = stringResource(R.string.settings_message_close))
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(text = accountName) },
                    supportingContent = { Text(text = accountEmail) },
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
            onRetry = {},
            onLicenseClick = {},
            onLicenseDismiss = {},
            onOperationMessageDismiss = {},
            onSignInClick = {},
            onSignOutClick = {},
            onBackupClick = {},
            onRestoreClick = {}
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
            onRetry = {},
            onLicenseClick = {},
            onLicenseDismiss = {},
            onOperationMessageDismiss = {},
            onSignInClick = {},
            onSignOutClick = {},
            onBackupClick = {},
            onRestoreClick = {}
        )
    }
}
