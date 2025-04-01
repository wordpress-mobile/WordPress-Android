package org.wordpress.android.ui.main.emailverificationbanner

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
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
        viewModel = EmailVerificationViewModel(
            mainDispatcher = testDispatcher(),
            bgDispatcher = testDispatcher(),
            dispatcher = dispatcher,
            accountStore = accountStore,
            appLogWrapper = appLogWrapper,
            contextProvider = contextProvider,
        )

        val accountModel = AccountModel()
        accountModel.userName = "testuser"
        accountModel.displayName = "Test User"

        whenever(accountStore.account).thenReturn(accountModel)
    }

    @Test
    fun `initial state is unverified`() = runTest {
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.UNVERIFIED)
    }

    @Test
    fun `when send link clicked, state changes to link requested`() = runTest {
        // When
        viewModel.onSendVerificationLinkClick()

        // Then
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_REQUESTED)
    }

    @Test
    fun `when link sent successfully, state changes to link sent`() = runTest {
        // Given
        viewModel.onVerificationEmailSent()

        // Then
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_SENT)
    }

    @Test
    fun `when link fails, state changes to error`() = runTest {
        // Given
        viewModel.onSendVerificationLinkClick()

        // When
        viewModel.onVerificationEmailError("Network error")

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
        viewModel.onVerificationEmailError("Previous error")

        // When
        viewModel.onSendVerificationLinkClick()

        // Then
        assertThat(viewModel.errorMessage.value).isEmpty()
    }

    @Test
    fun `state transition sequence is correct`() = runTest {
        // Test complete flow: UNVERIFIED -> LINK_REQUESTED -> LINK_SENT
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.UNVERIFIED)

        viewModel.onSendVerificationLinkClick()
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_REQUESTED)

        viewModel.onVerificationEmailSent()
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_SENT)
    }

    @Test
    fun `error state can be recovered from`() = runTest {
        // Given
        viewModel.onVerificationEmailError("Error")
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_ERROR)

        // When
        viewModel.onSendVerificationLinkClick()

        // Then
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_REQUESTED)
        assertThat(viewModel.errorMessage.value).isEmpty()
    }
}
