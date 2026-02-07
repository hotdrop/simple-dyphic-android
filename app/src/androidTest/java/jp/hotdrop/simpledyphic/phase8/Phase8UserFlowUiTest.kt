package jp.hotdrop.simpledyphic.phase8

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import jp.hotdrop.simpledyphic.feature.calendar.CalendarScreen
import jp.hotdrop.simpledyphic.feature.calendar.CalendarUiState
import jp.hotdrop.simpledyphic.feature.record.RecordEditScreen
import jp.hotdrop.simpledyphic.feature.record.RecordEditUiState
import jp.hotdrop.simpledyphic.feature.settings.SettingsScreen
import jp.hotdrop.simpledyphic.feature.settings.SettingsUiState
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Phase8UserFlowUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun calendarToRecordSaveFlow_canNavigateAndSave() {
        val selectedDate = LocalDate.of(2026, 2, 7)
        var showRecord by mutableStateOf(false)
        var breakfast by mutableStateOf("")
        var isSaved = false

        composeTestRule.setContent {
            SimpleDyphicTheme {
                if (showRecord) {
                    RecordEditScreen(
                        uiState = RecordEditUiState(
                            recordDate = selectedDate,
                            breakfast = breakfast
                        ),
                        onBackRequest = {},
                        onConfirmDiscard = {},
                        onDismissDiscardDialog = {},
                        onBreakfastChanged = { breakfast = it },
                        onLunchChanged = {},
                        onDinnerChanged = {},
                        onConditionTypeChanged = {},
                        onConditionMemoChanged = {},
                        onIsToiletChanged = {},
                        onRingfitKcalChanged = {},
                        onRingfitKmChanged = {},
                        onSave = {
                            isSaved = true
                            showRecord = false
                        },
                        onHealthSyncRequest = {},
                        onConfirmHealthOverwrite = {},
                        onDismissHealthOverwriteDialog = {},
                        onDismissHealthMessage = {}
                    )
                } else {
                    CalendarScreen(
                        uiState = CalendarUiState(
                            isLoading = false,
                            calendarStartMonth = YearMonth.of(2025, 1),
                            calendarEndMonth = YearMonth.of(2027, 12),
                            currentMonth = YearMonth.of(2026, 2),
                            selectedDate = selectedDate
                        ),
                        onRetry = {},
                        onMonthChanged = {},
                        onDateTap = {},
                        onEditSelectedDate = { showRecord = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("calendar_edit_selected_date_button").performClick()
        composeTestRule.onNodeWithTag("record_breakfast_input").performTextInput("Toast")
        composeTestRule.onNodeWithTag("record_save_button").performClick()

        composeTestRule.onNodeWithTag("calendar_edit_selected_date_button").assertExists()
        composeTestRule.runOnIdle {
            assertTrue(isSaved)
            assertEquals("Toast", breakfast)
        }
    }

    @Test
    fun settingsBackupFlow_signedInUserCanTapBackup() {
        var backupRequested = false

        composeTestRule.setContent {
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
                    onBackupClick = { backupRequested = true },
                    onRestoreClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_backup_item").performClick()
        composeTestRule.runOnIdle {
            assertTrue(backupRequested)
        }
    }
}
