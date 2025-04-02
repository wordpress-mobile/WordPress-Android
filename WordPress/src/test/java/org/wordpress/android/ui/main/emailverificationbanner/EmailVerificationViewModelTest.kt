package org.wordpress.android.ui.main.emailverificationbanner

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.main.emailverificationbanner.EmailVerificationViewModel.VerificationState
import org.wordpress.android.util.NetworkUtilsWrapper
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

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var context: Context

    private lateinit var viewModel: EmailVerificationViewModel

    @Before
    fun setup() {
        whenever(contextProvider.getContext()).thenReturn(context)

        whenever(accountStore.account).thenReturn(
            AccountModel().also {
                it.email = "testuser@example.com"
            }
        )

        viewModel = EmailVerificationViewModel(
            mainDispatcher = testDispatcher(),
            bgDispatcher = testDispatcher(),
            dispatcher = dispatcher,
            accountStore = accountStore,
            appLogWrapper = appLogWrapper,
            contextProvider = contextProvider,
            networkUtilsWrapper = networkUtilsWrapper,
        )
    }

    @Test
    fun `when link requested, state changes to link requested`() = runTest {
        viewModel.onVerificationLinkRequested()

        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_REQUESTED)
    }

    @Test
    fun `when link sent successfully, state changes to link sent`() = runTest {
        viewModel.onVerificationLinkSent()

        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_SENT)
    }

    @Test
    fun `when link fails, state changes to error`() = runTest {
        viewModel.onVerificationLinkError("Network error")

        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_ERROR)
        assertThat(viewModel.errorMessage.value).isEqualTo("Network error")
    }

    @Test
    fun `when email is verified, state changes to verified`() = runTest {
        viewModel.onEmailVerified()

        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.VERIFIED)
    }

    @Test
    fun `error message is cleared when sending new verification email`() = runTest {
        viewModel.onVerificationLinkError("Previous error")
        viewModel.onVerificationLinkRequested()

        assertThat(viewModel.errorMessage.value).isEmpty()
    }

    @Test
    fun `error state can be recovered from`() = runTest {
        viewModel.onVerificationLinkError("Error")
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_ERROR)

        viewModel.onVerificationLinkRequested()
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_REQUESTED)
        assertThat(viewModel.errorMessage.value).isEmpty()
    }

    @Test
    fun `when no connection, state changes to error`() = runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(context.getString(any())).thenReturn("No network")
        viewModel.onSendVerificationLinkClick()
        assertThat(viewModel.verificationState.value).isEqualTo(VerificationState.LINK_ERROR)
    }
}
