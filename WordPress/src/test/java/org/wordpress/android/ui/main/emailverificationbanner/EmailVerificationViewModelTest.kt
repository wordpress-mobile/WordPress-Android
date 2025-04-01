package org.wordpress.android.ui.main.emailverificationbanner

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.main.emailverificationbanner.EmailVerificationViewModel.VerificationState
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.CoroutineTestRule

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class EmailVerificationViewModelTest : BaseUnitTest() {
    @Rule
    @JvmField
    val coroutineRule = CoroutineTestRule()

    private lateinit var viewModel: EmailVerificationViewModel
    private val verificationState = MutableStateFlow<VerificationState?>(null)
    private val emailAddress = MutableStateFlow("test@example.com")
    private val errorMessage = MutableStateFlow("")

    @Before
    fun setup() {
        viewModel = EmailVerificationViewModel(
            initialState = VerificationState.UNVERIFIED,
            coroutineDispatcher = coroutineRule.testDispatcher
        )
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
        viewModel.onSendVerificationLinkClick()

        // When
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
    fun `email address is exposed through state`() = runTest {
        // Given
        val testEmail = "test@example.com"
        viewModel.updateEmailAddress(testEmail)

        // Then
        assertThat(viewModel.emailAddress.value).isEqualTo(testEmail)
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
