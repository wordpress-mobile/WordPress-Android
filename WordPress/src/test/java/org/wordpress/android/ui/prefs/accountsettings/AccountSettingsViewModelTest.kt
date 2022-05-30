package org.wordpress.android.ui.prefs.accountsettings

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R.string
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore.AccountError
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.AccountSettingsUiState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.SiteViewModel
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetAccountUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetSitesUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsUseCase
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@InternalCoroutinesApi
class AccountSettingsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: AccountSettingsViewModel
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var fetchAccountSettingsUseCase: FetchAccountSettingsUseCase
    @Mock private lateinit var pushAccountSettingsUseCase: PushAccountSettingsUseCase
    @Mock private lateinit var getSitesUseCase: GetSitesUseCase
    @Mock private lateinit var getAccountUseCase: GetAccountUseCase
    private val optimisticUpdateHandler = AccountSettingsOptimisticUpdateHandler()
    @Mock private lateinit var account: AccountModel

    private val siteViewModels = mutableListOf<SiteViewModel>().apply {
        add(SiteViewModel("HappyDay", 1L, "http://happyday.wordpress.com"))
        add(SiteViewModel("WonderLand", 2L, "http://wonderland.wordpress.com"))
        add(SiteViewModel("FantasyBooks", 3L, "http://fantasybooks.wordpress.com"))
    }

    @Before
    fun setUp() = test {
        whenever(account.primarySiteId).thenReturn(3L)
        whenever(account.userName).thenReturn("old_wordpressuser_username")
        whenever(account.displayName).thenReturn("old_wordpressuser_displayname")
        whenever(account.email).thenReturn("old_wordpressuser@gmail.com")
        whenever(account.newEmail).thenReturn("")
        whenever(account.webAddress).thenReturn("http://old_wordpressuser.com")
        whenever(account.pendingEmailChange).thenReturn(false)
        whenever(account.usernameCanBeChanged).thenReturn(false)
        whenever(getAccountUseCase.account).thenReturn(account)

        val sites = siteViewModels.map {
            SiteModel().apply {
                this.siteId = it.siteId
                this.name = it.siteName
                this.url = it.homeURLOrHostName
            }
        }
        whenever(getSitesUseCase.get()).thenReturn(sites)
        initialiseViewModel()
    }

    @Test
    fun `The initial Ui state should be updated with the cached account information`() = test {
        val uiState = viewModel.accountSettingsUiState.value
        uiState.primarySiteSettingsUiState?.primarySite?.siteId?.let {
            assertThat(it)
                    .withFailMessage("The initial primary site Id should be updated with the cached account information")
                    .isEqualTo(getAccountUseCase.account.primarySiteId)
        } ?: fail("The UiState should not be empty")
        assertThat(uiState.userNameSettingsUiState.userName)
                .withFailMessage("The initial userName should be updated with the cached account information")
                .isEqualTo(getAccountUseCase.account.userName)
        assertThat(uiState.userNameSettingsUiState.displayName)
                .withFailMessage("The initial displayName should be updated with the cached account information")
                .isEqualTo(getAccountUseCase.account.displayName)
        assertThat(uiState.userNameSettingsUiState.canUserNameBeChanged)
                .withFailMessage("The initial username should be allowed to changed based on the cached account information")
                .isEqualTo(getAccountUseCase.account.usernameCanBeChanged)
        assertThat(uiState.userNameSettingsUiState.showUserNameConfirmedSnackBar)
                .withFailMessage("The snackbar with message username confirmed should not be shown by default.")
                .isEqualTo(false)
        assertThat(uiState.emailSettingsUiState.email)
                .withFailMessage("The initial Email should be shown from the cached account information")
                .isEqualTo(getAccountUseCase.account.email)
        assertThat(uiState.emailSettingsUiState.newEmail)
                .withFailMessage("The initial New Email should be shown from the cached account information")
                .isEqualTo(getAccountUseCase.account.newEmail)
        assertThat(uiState.emailSettingsUiState.hasPendingEmailChange)
                .withFailMessage("The initial pending email change should be shown based on the cached account information")
                .isEqualTo(getAccountUseCase.account.pendingEmailChange)
        assertThat(uiState.webAddressSettingsUiState.webAddress)
                .withFailMessage("The initial WebAddress should be updated with the cached account information")
                .isEqualTo(getAccountUseCase.account.webAddress)
    }

    // Username
    @Test
    fun `When the user has changed the username through a different screen and navigated back with new username, the new username should be updated and notified with the snackbar message`() =
            test {
                viewModel.onUsernameChangeConfirmedFromServer("new_wordpressuser_username")
                val uiState = viewModel.accountSettingsUiState.value
                assertThat(uiState.userNameSettingsUiState.userName)
                        .withFailMessage("The user name should be updated when the new user name is confirmed by the different screen")
                        .isEqualTo("new_wordpressuser_username")
                assertThat(uiState.userNameSettingsUiState.showUserNameConfirmedSnackBar)
                        .withFailMessage("The user should be notified of the user name change with snackbar message")
                        .isEqualTo(true)
                assertThat(uiState.userNameSettingsUiState.newUserChangeConfirmedSnackBarMessageHolder.message)
                        .withFailMessage("The snackbar message should say 'Your new username is new_wordpressuser_username'")
                        .isEqualTo(
                                UiStringResWithParams(
                                        string.settings_username_changer_toast_content,
                                        listOf(UiStringText("new_wordpressuser_username"))
                                )
                        )
            }

    @Test
    fun `The user name should be allowed to change, only if the server return 'canUserNameBeChanged' as true`() =
            test {
                whenever(getAccountUseCase.account.usernameCanBeChanged).thenReturn(true)
                initialiseViewModel()
                val uiState = viewModel.accountSettingsUiState.value
                assertThat(uiState.userNameSettingsUiState.canUserNameBeChanged).isEqualTo(true)
            }

    @Test
    fun `The user name should not be allowed to change, if the server return 'canUserNameBeChanged' as false`() =
            test {
                whenever(getAccountUseCase.account.usernameCanBeChanged).thenReturn(false)
                initialiseViewModel()
                val uiState = viewModel.accountSettingsUiState.value
                assertThat(uiState.userNameSettingsUiState.canUserNameBeChanged).isEqualTo(false)
            }

    // Email
    @Test
    fun `If the user has pending email address change, the user should be notified to verify the email address via verification link sent`() =
            test {
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)
                whenever(getAccountUseCase.account.newEmail).thenReturn("new_wordpressuser_username")
                initialiseViewModel()
                val uiState = viewModel.accountSettingsUiState.value
                assertThat(uiState.emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
                assertThat(uiState.emailSettingsUiState.emailVerificationMsgSnackBarMessageHolder.message)
                        .withFailMessage("The snackbar message should say 'Click the verification link in the email sent to new_wordpressuser_username to confirm your new address'")
                        .isEqualTo(
                                UiStringResWithParams(
                                        string.pending_email_change_snackbar,
                                        listOf(UiStringText(getAccountUseCase.account.newEmail))
                                )
                        )
            }

    @Test
    fun `If the user doesn't have any pending email address change, the user should not be asked to verify the email address`() =
            test {
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                initialiseViewModel()
                val uiState = viewModel.accountSettingsUiState.value
                assertThat(uiState.emailSettingsUiState.hasPendingEmailChange).isEqualTo(false)
            }

    // Email change
    @Test
    fun `When the user tries to update a new email address, optimistically show the user as if the new email address change is requested even before updating in the server`() =
            test {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updateEmail("new_wordpressuser@gmail.com")).thenReturn(
                        mockErrorResponse()
                )
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                whenever(getAccountUseCase.account.newEmail).thenReturn("")

                // When
                viewModel.onEmailChanged("new_wordpressuser@gmail.com")

                // Then
                assertThat(uiStateList[uiStateList.lastIndex - 1].emailSettingsUiState.hasPendingEmailChange).isEqualTo(
                        true
                )
                assertThat(uiStateList[uiStateList.lastIndex - 1].emailSettingsUiState.newEmail).isEqualTo("new_wordpressuser@gmail.com")

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When the user tries to update a new email and on error response, the user should be show an error and revert back from displaying of pending new email address verification`() =
            test {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updateEmail("new_wordpressuser@gmail.com")).thenReturn(
                        mockErrorResponse()
                )
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                whenever(getAccountUseCase.account.newEmail).thenReturn("")

                // When
                viewModel.onEmailChanged("new_wordpressuser@gmail.com")

                // Then
                assertThat(uiStateList.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(false)
                assertThat(uiStateList.last().emailSettingsUiState.newEmail).isEqualTo("")

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When the user tries to update a new email and on success response, the user should be show of pending new email address verification`() =
            test {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updateEmail("new_wordpressuser@gmail.com")).thenReturn(
                        mockSuccessResponse()
                )
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)
                whenever(getAccountUseCase.account.newEmail).thenReturn("new_wordpressuser@gmail.com")

                // When
                viewModel.onEmailChanged("new_wordpressuser@gmail.com")

                // Then
                assertThat(uiStateList.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
                assertThat(uiStateList.last().emailSettingsUiState.newEmail).isEqualTo("new_wordpressuser@gmail.com")

                // Cleanup
                job.cancel()
            }

    // cancel pending email
    @Test
    fun `When the user tries to cancels a pending email change and on error response, the user should still be shown of pending email change`() =
            test {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)
                whenever(getAccountUseCase.account.newEmail).thenReturn("new_wordpressuser@gmail.com")

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.cancelPendingEmailChange()).thenReturn(mockErrorResponse())

                // When
                viewModel.accountSettingsUiState.value.emailSettingsUiState.onCancelEmailChange.invoke()

                // Then
                assertThat(uiStateList.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
                assertThat(uiStateList.last().emailSettingsUiState.newEmail).isEqualTo("new_wordpressuser@gmail.com")

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When the user tries to cancels a pending email change and on success response, don't show any pending new email address`() =
            test {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.cancelPendingEmailChange()).thenReturn(mockSuccessResponse())
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                whenever(getAccountUseCase.account.newEmail).thenReturn("")

                // When
                viewModel.accountSettingsUiState.value.emailSettingsUiState.onCancelEmailChange.invoke()

                // Then
                assertThat(uiStateList.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(false)
                assertThat(uiStateList.last().emailSettingsUiState.newEmail).isEqualTo("")
                // Cleanup
                job.cancel()
            }

    // Primary site
    @Test
    fun `When the user tries to update a different site as Primary site, optimistically show the user as if the new primary site is changed even before updating in the server`() =
            test {
                // Given
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updatePrimaryBlog(siteViewModels.first().siteId.toString())).thenReturn(
                        mockErrorResponse()
                )
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // When
                viewModel.onPrimarySiteChanged(siteRemoteId = siteViewModels.first().siteId)

                // Then
                assertThat(uiStateList[uiStateList.lastIndex - 1].primarySiteSettingsUiState?.primarySite?.siteId).isEqualTo(
                        siteViewModels.first().siteId
                )

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When user tries to update a different site as Primary site and on error reponse, revert back to the old primary site`() =
            test {
                // Given
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updatePrimaryBlog(siteViewModels.first().siteId.toString())).thenReturn(
                        mockErrorResponse()
                )
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // When
                viewModel.onPrimarySiteChanged(siteRemoteId = siteViewModels.first().siteId)

                // Then
                assertThat(uiStateList.last().primarySiteSettingsUiState?.primarySite?.siteId).isEqualTo(siteViewModels.last().siteId)

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When user tries to update a different site as Primary site and on successful reponse, new primary site should be shown to the user`() =
            test {
                // Given
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updatePrimaryBlog(siteViewModels.first().siteId.toString())).thenReturn(
                        mockSuccessResponse()
                )
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.first().siteId)

                // When
                viewModel.onPrimarySiteChanged(siteRemoteId = siteViewModels.first().siteId)

                // Then
                assertThat(uiStateList.last().primarySiteSettingsUiState?.primarySite?.siteId).isEqualTo(siteViewModels.first().siteId)

                // Cleanup
                job.cancel()
            }

    // Web Address
    @Test
    fun `When the user tries to update a new webaddress, optimistically show the user as if the new web address is changed even before updating in the server`() =
            test {
                // Given
                whenever(getAccountUseCase.account.webAddress).thenReturn("old_webaddress")

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updateWebAddress("new_webaddress")).thenReturn(mockErrorResponse())
                whenever(getAccountUseCase.account.webAddress).thenReturn("old_webaddress")

                // When
                viewModel.onWebAddressChanged("new_webaddress")

                // Then
                assertThat(uiStateList[uiStateList.lastIndex - 1].webAddressSettingsUiState.webAddress).isEqualTo("new_webaddress")

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When user tries to update a new webaddress and on success response, new web address should be shown to user`() =
            test {
                // Given
                whenever(getAccountUseCase.account.webAddress).thenReturn("old_webaddress")

                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updateWebAddress("new_webaddress")).thenReturn(mockSuccessResponse())
                whenever(getAccountUseCase.account.webAddress).thenReturn("new_webaddress")

                // When
                viewModel.onWebAddressChanged("new_webaddress")

                // Then
                assertThat(uiStateList.last().webAddressSettingsUiState.webAddress).isEqualTo("new_webaddress")

                // Cleanup
                job.cancel()
            }

    // Change password
    @Test
    fun `When the user tries to update the new password, show changing password progress dialog until it receives the server response`() =
            test {
                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updatePassword("new_password")).thenReturn(mockSuccessResponse())
                // When
                viewModel.onPasswordChanged("new_password")

                // Then
                assertThat(uiStateList[uiStateList.lastIndex - 1].changePasswordSettingsUiState.showChangePasswordProgressDialog).isEqualTo(
                        true
                )

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When the user tries to update the new password and on receiving the server response, dismiss the changing password progress dialog`() =
            test {
                // Observe uiState change
                initialiseViewModel()
                val uiStateList = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(pushAccountSettingsUseCase.updatePassword("new_password")).thenReturn(mockSuccessResponse())
                // When
                viewModel.onPasswordChanged("new_password")

                // Then
                assertThat(uiStateList.last().changePasswordSettingsUiState.showChangePasswordProgressDialog).isEqualTo(
                        false
                )

                // Cleanup
                job.cancel()
            }

    // Helper Methods
    private fun mockSuccessResponse(): OnAccountChanged {
        return mock()
    }

    private fun mockErrorResponse(): OnAccountChanged {
        return OnAccountChanged().apply {
            this.error = AccountError(GENERIC_ERROR, "")
        }
    }

    private fun initialiseViewModel() {
        viewModel = AccountSettingsViewModel(
                resourceProvider,
                networkUtilsWrapper,
                TEST_DISPATCHER,
                fetchAccountSettingsUseCase,
                pushAccountSettingsUseCase,
                getAccountUseCase,
                getSitesUseCase,
                optimisticUpdateHandler
        )
    }
}
