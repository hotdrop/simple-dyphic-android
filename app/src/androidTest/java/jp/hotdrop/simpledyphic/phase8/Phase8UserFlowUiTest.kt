package jp.hotdrop.simpledyphic.phase8

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import jp.hotdrop.simpledyphic.ui.calendar.CalendarScreen
import jp.hotdrop.simpledyphic.ui.calendar.CalendarUiState
import jp.hotdrop.simpledyphic.ui.ai.ExerciseAdviceScreen
import jp.hotdrop.simpledyphic.ui.ai.ExerciseAdviceUiState
import jp.hotdrop.simpledyphic.ui.record.RecordEditScreen
import jp.hotdrop.simpledyphic.ui.record.RecordEditUiState
import jp.hotdrop.simpledyphic.model.AdvicePeriod
import jp.hotdrop.simpledyphic.model.AppSettings
import jp.hotdrop.simpledyphic.model.ExerciseAdviceInput
import jp.hotdrop.simpledyphic.model.ExerciseAdviceRequirement
import jp.hotdrop.simpledyphic.model.ExerciseMetricKind
import jp.hotdrop.simpledyphic.model.ExerciseMetricSummary
import jp.hotdrop.simpledyphic.model.MetricAvailability
import jp.hotdrop.simpledyphic.ui.settings.SettingsDataSyncAction
import jp.hotdrop.simpledyphic.ui.settings.SettingsScreen
import jp.hotdrop.simpledyphic.ui.settings.SettingsUiState
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Phase8UserFlowUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun calendarToRecordSaveFlow_canNavigateAndSave() {
        val selectedDate = LocalDate.of(2026, 2, 7)
        val (showRecord, setShowRecord) = mutableStateOf(false)
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
                            setShowRecord(false)
                        },
                        onHealthSyncRequest = {},
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
                        onEditSelectedDate = { setShowRecord(true) }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("calendar_edit_selected_date_button").performClick()
        composeTestRule.onNodeWithTag("record_breakfast_input").performTextInput("Toast")
        composeTestRule.onNodeWithTag("record_save_button").performClick()

        composeTestRule.runOnIdle {
            assertTrue(isSaved)
            assertEquals("Toast", breakfast)
        }
    }

    @Test
    fun calendarDateSelection_updatesSelectedDateBeforeEdit() {
        val initialDate = LocalDate.of(2026, 2, 7)
        val targetDate = LocalDate.of(2026, 2, 9)
        var uiState by mutableStateOf(
            CalendarUiState(
                isLoading = false,
                calendarStartMonth = YearMonth.of(2025, 1),
                calendarEndMonth = YearMonth.of(2027, 12),
                currentMonth = YearMonth.of(2026, 2),
                selectedDate = initialDate
            )
        )
        var editedDate: LocalDate? = null

        composeTestRule.setContent {
            SimpleDyphicTheme {
                CalendarScreen(
                    uiState = uiState,
                    onRetry = {},
                    onMonthChanged = {},
                    onDateTap = { tappedDate ->
                        uiState = uiState.copy(selectedDate = tappedDate)
                    },
                    onEditSelectedDate = {
                        editedDate = uiState.selectedDate
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("calendar_day_$targetDate").performClick()
        composeTestRule.onNodeWithTag("calendar_edit_selected_date_button").performClick()

        composeTestRule.runOnIdle {
            assertEquals(targetDate, uiState.selectedDate)
            assertEquals(targetDate, editedDate)
        }
    }

    @Test
    fun settingsBackupFlow_signedInUserCanTapBackup() {
        var uiState by mutableStateOf(
            SettingsUiState(
                appVersion = "1.0 (1)",
                isSignedIn = true,
                accountName = "Google User",
                accountEmail = "user@example.com"
            )
        )
        var backupRequested = false

        composeTestRule.setContent {
            SimpleDyphicTheme {
                SettingsScreen(
                    uiState = uiState,
                    onLicenseClick = {},
                    onLicenseDismiss = {},
                    onSignInClick = {},
                    onSignOutClick = {},
                    onBackupClick = {
                        uiState = uiState.copy(pendingDataSyncAction = SettingsDataSyncAction.Backup)
                    },
                    onRestoreClick = {
                        uiState = uiState.copy(pendingDataSyncAction = SettingsDataSyncAction.Restore)
                    },
                    onWeeklyGoalSettingsClick = {},
                    onAiAdviceSettingsClick = {},
                    onDataSyncActionConfirm = {
                        backupRequested = true
                        uiState = uiState.copy(pendingDataSyncAction = null)
                    },
                    onDataSyncActionDismiss = {
                        uiState = uiState.copy(pendingDataSyncAction = null)
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_backup_item").performClick()
        composeTestRule.runOnIdle {
            assertEquals(SettingsDataSyncAction.Backup, uiState.pendingDataSyncAction)
            assertFalse(backupRequested)
        }
        composeTestRule.onNodeWithTag("settings_data_sync_confirm_button").performClick()
        composeTestRule.runOnIdle {
            assertTrue(backupRequested)
        }
    }

    @Test
    fun settingsScreen_showsAiAdviceSettingsEntry() {
        composeTestRule.setContent {
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
                    onWeeklyGoalSettingsClick = {},
                    onAiAdviceSettingsClick = {},
                    onDataSyncActionConfirm = {},
                    onDataSyncActionDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_ai_advice_item").assertIsDisplayed()
    }

    @Test
    fun aiAdviceScreen_blocksGenerationWhenRequirementsMissing() {
        composeTestRule.setContent {
            SimpleDyphicTheme {
                ExerciseAdviceScreen(
                    uiState = ExerciseAdviceUiState(
                        isLoading = false,
                        selectedPeriod = AdvicePeriod.WEEKLY,
                        input = ExerciseAdviceInput(
                            period = AdvicePeriod.WEEKLY,
                            periodStartDate = LocalDate.of(2026, 4, 6),
                            periodEndDate = LocalDate.of(2026, 4, 12),
                            elapsedDays = 7,
                            age = null,
                            settings = AppSettings(),
                            summaries = listOf(
                                ExerciseMetricSummary(
                                    kind = ExerciseMetricKind.STEP_COUNT,
                                    actualValue = 10000.0,
                                    availability = MetricAvailability.AVAILABLE
                                )
                            ),
                            missingRequirements = setOf(
                                ExerciseAdviceRequirement.BIRTH_DATE,
                                ExerciseAdviceRequirement.MODEL_FILE
                            ),
                            promptDataBlock = ""
                        )
                    ),
                    onRetry = {},
                    onPeriodSelected = {},
                    onGenerateClick = {},
                    onRequestPermissionClick = {},
                    onOpenSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("ai_advice_generate_button").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Gemma 4モデルファイル").assertIsDisplayed()
    }
}
