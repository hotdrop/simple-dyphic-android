package jp.hotdrop.simpledyphic.feature.settings

import androidx.test.core.app.ApplicationProvider
import jp.hotdrop.simpledyphic.core.log.AppLogger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @Test
    fun signInAndSignOut_togglesSignedInState() {
        val viewModel = createViewModel()

        viewModel.onSignInClick()
        assertTrue(viewModel.uiState.value.isSignedIn)

        viewModel.onSignOutClick()
        assertFalse(viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun onLicenseClickAndDismiss_updatesDialogState() {
        val viewModel = createViewModel()

        viewModel.onLicenseClick()
        assertTrue(viewModel.uiState.value.showLicenseDialog)

        viewModel.onLicenseDismiss()
        assertFalse(viewModel.uiState.value.showLicenseDialog)
    }

    private class NoOpLogger : AppLogger {
        override fun i(message: String) = Unit
        override fun e(message: String, throwable: Throwable?) = Unit
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            appContext = ApplicationProvider.getApplicationContext(),
            appLogger = NoOpLogger()
        )
    }
}
