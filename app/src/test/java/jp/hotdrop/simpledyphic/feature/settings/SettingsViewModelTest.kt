package jp.hotdrop.simpledyphic.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import jp.hotdrop.simpledyphic.core.log.AppLogger
import jp.hotdrop.simpledyphic.domain.model.Record
import jp.hotdrop.simpledyphic.domain.model.UserAccount
import jp.hotdrop.simpledyphic.domain.repository.AccountRepository
import jp.hotdrop.simpledyphic.domain.repository.RecordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    @Test
    fun signInAndSignOut_togglesSignedInState() = runTest {
        val fakeAccountRepository = FakeAccountRepository()
        val viewModel = createViewModel(accountRepository = fakeAccountRepository)

        fakeAccountRepository.signInResult = UserAccount(
            uid = "user-1",
            name = "Google User",
            email = "user@example.com"
        )
        viewModel.onSignInClick(ApplicationProvider.getApplicationContext())
        assertTrue(viewModel.uiState.value.isSignedIn)
        assertTrue(viewModel.uiState.value.operationMessage != null)

        viewModel.onSignOutClick()
        assertFalse(viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun onLicenseClickAndDismiss_updatesDialogState() = runTest {
        val viewModel = createViewModel()

        viewModel.onLicenseClick()
        assertTrue(viewModel.uiState.value.showLicenseDialog)

        viewModel.onLicenseDismiss()
        assertFalse(viewModel.uiState.value.showLicenseDialog)
    }

    @Test
    fun backupAndRestore_invokesRepositoryOperations() = runTest {
        val fakeRecordRepository = FakeRecordRepository()
        val viewModel = createViewModel(recordRepository = fakeRecordRepository)

        viewModel.onBackupClick()
        viewModel.onRestoreClick()

        assertTrue(fakeRecordRepository.backupCalled)
        assertTrue(fakeRecordRepository.restoreCalled)
    }

    @Test
    fun onOperationMessageDismiss_clearsMessage() = runTest {
        val fakeAccountRepository = FakeAccountRepository().apply {
            signInResult = UserAccount(
                uid = "user-1",
                name = "Google User",
                email = "user@example.com"
            )
        }
        val viewModel = createViewModel(accountRepository = fakeAccountRepository)

        viewModel.onSignInClick(ApplicationProvider.getApplicationContext())
        assertTrue(viewModel.uiState.value.operationMessage != null)

        viewModel.onOperationMessageDismiss()
        assertNull(viewModel.uiState.value.operationMessage)
    }

    private class NoOpLogger : AppLogger {
        override fun i(message: String) = Unit
        override fun e(message: String, throwable: Throwable?) = Unit
    }

    private class FakeAccountRepository : AccountRepository {
        var current: UserAccount? = null
        var signInResult: UserAccount? = null

        override fun currentAccount(): UserAccount? = current

        override suspend fun signInWithGoogle(context: Context): UserAccount? {
            current = signInResult
            return signInResult
        }

        override suspend fun signOut() {
            current = null
        }
    }

    private class FakeRecordRepository : RecordRepository {
        var backupCalled: Boolean = false
        var restoreCalled: Boolean = false

        override suspend fun find(id: Int): Record {
            throw NoSuchElementException("Not used in this test")
        }

        override suspend fun findAll(): List<Record> = emptyList()

        override suspend fun save(record: Record) = Unit

        override suspend fun backup() {
            backupCalled = true
        }

        override suspend fun restore() {
            restoreCalled = true
        }
    }

    private fun createViewModel(
        accountRepository: AccountRepository = FakeAccountRepository(),
        recordRepository: RecordRepository = FakeRecordRepository()
    ): SettingsViewModel {
        return SettingsViewModel(
            appContext = ApplicationProvider.getApplicationContext(),
            appLogger = NoOpLogger(),
            accountRepository = accountRepository,
            recordRepository = recordRepository
        )
    }
}
