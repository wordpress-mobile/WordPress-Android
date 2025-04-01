package org.wordpress.android.ui.main.emailverificationbanner

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.main.emailverificationbanner.EmailVerificationViewModel.VerificationState
import org.wordpress.android.viewmodel.ContextProvider

@ExperimentalCoroutinesApi
class EmailVerificationViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var contextProvider: ContextProvider

    private lateinit var viewModel: EmailVerificationViewModel

    @Before
    fun setup() {
        AccountModel().also {
            it.userName = "testuser"
            it.displayName = "Test User"
            it.email = "testuser@example.com"
            whenever(accountStore.account).thenReturn(it)
        }

        viewModel = EmailVerificationViewModel(
            mainDispatcher = testDispatcher(),
            bgDispatcher = testDispatcher(),
            dispatcher = dispatcher,
            accountStore = accountStore,
            appLogWrapper = appLogWrapper,
            contextProvider = contextProvider,
        )
    }

    @Test
    fun `when link requested, state changes to link requested`() = runTest {
        // When
        viewModel.onVerificationLinkRequested()

        // Then
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_REQUESTED)
    }

    @Test
    fun `when link sent successfully, state changes to link sent`() = runTest {
        // When
        viewModel.onVerificationLinkSent()

        // Then
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_SENT)
    }

    @Test
    fun `when link fails, state changes to error`() = runTest {
        // When
        viewModel.onVerificationLinkError("Network error")

        // Then
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_ERROR)
        assertThat(viewModel.errorMessage.value).isEqualTo("Network error")
    }

    @Test
    fun `when email is verified, state changes to verified`() = runTest {
        // When
        viewModel.onEmailVerified()

        // Then
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.VERIFIED)
    }

    @Test
    fun `error message is cleared when sending new verification email`() = runTest {
        // Given
        viewModel.onVerificationLinkError("Previous error")

        // When
        viewModel.onVerificationLinkRequested()

        // Then
        assertThat(viewModel.errorMessage.value).isEmpty()
    }

    @Test
    fun `error state can be recovered from`() = runTest {
        // Given
        viewModel.onVerificationLinkError("Error")
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_ERROR)

        // When
        viewModel.onVerificationLinkRequested()

        // Then
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_REQUESTED)
        assertThat(viewModel.errorMessage.value).isEmpty()
    }
}
