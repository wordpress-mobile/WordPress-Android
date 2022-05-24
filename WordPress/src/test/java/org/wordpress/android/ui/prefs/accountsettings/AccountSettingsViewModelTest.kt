package org.wordpress.android.ui.prefs.accountsettings

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions
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
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@InternalCoroutinesApi
class AccountSettingsViewModelTest : BaseUnitTest(){
    private lateinit var viewModel: AccountSettingsViewModel
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var accountsSettingsRepository:AccountSettingsRepository
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
        whenever(accountsSettingsRepository.account).thenReturn(account)

        val sites = siteViewModels.map {
            SiteModel().apply {
                this.siteId = it.siteId
                this.name = it.siteName
                this.url = it.homeURLOrHostName
            }
        }
        whenever(accountsSettingsRepository.getSitesAccessedViaWPComRest()).thenReturn(sites)
        viewModel = AccountSettingsViewModel(
                resourceProvider,
                networkUtilsWrapper,
                TEST_DISPATCHER,
                accountsSettingsRepository
        )
    }

    @Test
    fun `By Default, the account settings Ui state should be updated with the account information from account repository`() = test {
        val mUiState = viewModel.accountSettingsUiState.value
        mUiState.primarySiteSettingsUiState?.primarySite?.siteId?.let {
            Assertions.assertThat(it)
                    .withFailMessage("The primary site Id should be updated with the account information from accountsSettingsRepository")
                    .isEqualTo(accountsSettingsRepository.account.primarySiteId)
        } ?: Assertions.fail("The UiState should not be empty")
        Assertions.assertThat(mUiState.userNameSettingsUiState.userName)
                .withFailMessage("The userName should be updated with the account information from accountsSettingsRepository")
                .isEqualTo(accountsSettingsRepository.account.userName)
        Assertions.assertThat(mUiState.userNameSettingsUiState.displayName)
                .withFailMessage("The displayName should be updated with the account information from accountsSettingsRepository")
                .isEqualTo(accountsSettingsRepository.account.displayName)
        Assertions.assertThat(mUiState.userNameSettingsUiState.canUserNameBeChanged)
                .withFailMessage("The username should be allowed to changed based on the account information from accountsSettingsRepository")
                .isEqualTo(accountsSettingsRepository.account.usernameCanBeChanged)
        Assertions.assertThat(mUiState.userNameSettingsUiState.showUserNameConfirmedSnackBar)
                .withFailMessage("The snackbar with message username confirmed should not be shown by default.")
                .isEqualTo(false)
        Assertions.assertThat(mUiState.emailSettingsUiState.email)
                .withFailMessage("The Email should be shown from account information from accountsSettingsRepository")
                .isEqualTo(accountsSettingsRepository.account.email)
        Assertions
                .assertThat (mUiState.emailSettingsUiState.newEmail)
                .withFailMessage("The New Email should be shown from account information available in accountsSettingsRepository")
                .isEqualTo(accountsSettingsRepository.account.newEmail)
        Assertions.assertThat(mUiState.emailSettingsUiState.hasPendingEmailChange)
                .withFailMessage("The pending email change should be shown based on the account information from accountsSettingsRepository")
                .isEqualTo(accountsSettingsRepository.account.pendingEmailChange)
        Assertions.assertThat(mUiState.webAddressSettingsUiState.webAddress)
                .withFailMessage("The WebAddress should be updated with the account information from accountsSettingsRepository")
                .isEqualTo(accountsSettingsRepository.account.webAddress)
    }

    // Username default
    @Test
    fun `The userName should be updated with the account information from account Repository`() = test {
        val mUiState = viewModel.accountSettingsUiState.value
        Assertions.assertThat(mUiState.userNameSettingsUiState.userName).isEqualTo("old_wordpressuser_username")
    }

    @Test
    fun `When the user has changed the username through a different screen and navigated back with new username, the new username should be updated and notified with the snackbar message`() = test {
        viewModel.onUsernameChangeConfirmedFromServer("new_wordpressuser_username")
        val mUiState = viewModel.accountSettingsUiState.value
        Assertions.assertThat(mUiState.userNameSettingsUiState.userName)
                .withFailMessage("The user name should be updated when the new user name is confirmed by the different screen")
                .isEqualTo("new_wordpressuser_username")
        Assertions.assertThat(mUiState.userNameSettingsUiState.showUserNameConfirmedSnackBar)
                .withFailMessage("The user should be notified of the user name change with snackbar message")
                .isEqualTo(true)
        Assertions.assertThat(mUiState.userNameSettingsUiState.newUserChangeConfirmedSnackBarMessageHolder.message)
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
                whenever(accountsSettingsRepository.account.usernameCanBeChanged).thenReturn(true)
                viewModel = AccountSettingsViewModel(
                        resourceProvider,
                        networkUtilsWrapper,
                        TEST_DISPATCHER,
                        accountsSettingsRepository
                )
                val mUiState = viewModel.accountSettingsUiState.value
                Assertions.assertThat(mUiState.userNameSettingsUiState.canUserNameBeChanged).isEqualTo(true)
            }

    @Test
    fun `The user name should not be allowed to change, if the server return 'canUserNameBeChanged' as false`() =
            test {
                whenever(accountsSettingsRepository.account.usernameCanBeChanged).thenReturn(false)
                viewModel = AccountSettingsViewModel(
                        resourceProvider,
                        networkUtilsWrapper,
                        TEST_DISPATCHER,
                        accountsSettingsRepository
                )
                val mUiState = viewModel.accountSettingsUiState.value
                Assertions.assertThat(mUiState.userNameSettingsUiState.canUserNameBeChanged).isEqualTo(false)
            }

    // Email default
    @Test
    fun `The email address should be updated with the account information from account Repository`() = test {
        whenever(accountsSettingsRepository.account.email).thenReturn("old_wordpressuser")
        viewModel = AccountSettingsViewModel(
                resourceProvider,
                networkUtilsWrapper,
                TEST_DISPATCHER,
                accountsSettingsRepository
        )
        val mUiState = viewModel.accountSettingsUiState.value
        Assertions.assertThat(mUiState.emailSettingsUiState.email).isEqualTo("old_wordpressuser")
    }

    @Test
    fun `If the user has pending email address change, the user should be shown that he has a pending email address change`() = test {
        whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(true)
        viewModel = AccountSettingsViewModel(
                resourceProvider,
                networkUtilsWrapper,
                TEST_DISPATCHER,
                accountsSettingsRepository
        )
        val mUiState = viewModel.accountSettingsUiState.value
        Assertions.assertThat(mUiState.emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
    }

    @Test
    fun `If the user doesn't have any pending email address change, the user should be not be shown with any pending email address change`() =
            test {
                whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(false)
                viewModel = AccountSettingsViewModel(
                        resourceProvider,
                        networkUtilsWrapper,
                        TEST_DISPATCHER,
                        accountsSettingsRepository
                )
                val mUiState = viewModel.accountSettingsUiState.value
                Assertions.assertThat(mUiState.emailSettingsUiState.hasPendingEmailChange).isEqualTo(false)
            }

    // Email change
    @Test
    fun `When the user tries to update a new email address, optimistically show the user as if the new email address change even before updating in the server`() = test {
        // Given
        whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(false)

        // Observe uiState change
        initialiseViewModel()
        val uiStateList  = mutableListOf<AccountSettingsUiState>()
        val job = launch(TEST_DISPATCHER) {
            viewModel.accountSettingsUiState.toList(uiStateList)
        }

        // mock server response
        whenever(accountsSettingsRepository.updateEmail("new_wordpressuser@gmail.com")).thenReturn(mockErrorResponse())
        whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(false)
        whenever(accountsSettingsRepository.account.newEmail).thenReturn("")

        // When
        viewModel.onEmailChanged("new_wordpressuser@gmail.com")

        // Then
        Assertions.assertThat(uiStateList[uiStateList.lastIndex-1].emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
        Assertions.assertThat(uiStateList[uiStateList.lastIndex-1].emailSettingsUiState.newEmail).isEqualTo("new_wordpressuser@gmail.com")

        // Cleanup
        job.cancel()
    }

    @Test
    fun `When the user tries to update a new email and on error response, the user should be show of error and revert back from displaying of pending new email address verification`() =
            test {
                // Given
                whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(false)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.updateEmail("new_wordpressuser@gmail.com")).thenReturn(mockErrorResponse())
                whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(false)
                whenever(accountsSettingsRepository.account.newEmail).thenReturn("")

                // When
                viewModel.onEmailChanged("new_wordpressuser@gmail.com")

                // Then
                Assertions.assertThat(uiStateList.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(false)
                Assertions.assertThat(uiStateList.last().emailSettingsUiState.newEmail).isEqualTo("")

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When the user tries to update a new email and on success response, the user should be show of pending new email address verification`() =
            test {
                // Given
                whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(true)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.updateEmail("new_wordpressuser@gmail.com")).thenReturn(mockSuccessResponse())
                whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(true)
                whenever(accountsSettingsRepository.account.newEmail).thenReturn("new_wordpressuser@gmail.com")

                // When
                viewModel.onEmailChanged("new_wordpressuser@gmail.com")

                // Then
                Assertions.assertThat(uiStateList.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
                Assertions.assertThat(uiStateList.last().emailSettingsUiState.newEmail).isEqualTo("new_wordpressuser@gmail.com")

                // Cleanup
                job.cancel()
            }

    // cancel pending email
    @Test
    fun `When the user tries to cancels a pending email change and on error response, the user should still be shown of pending email change`() =
            test {
                // Given
                whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(true)
                whenever(accountsSettingsRepository.account.newEmail).thenReturn("new_wordpressuser@gmail.com")

                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.cancelPendingEmailChange()).thenReturn(mockErrorResponse())

                // When
                viewModel.accountSettingsUiState.value.emailSettingsUiState.onCancelEmailChange.invoke()

                // Then
                Assertions.assertThat(uiStateList.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
                Assertions.assertThat(uiStateList.last().emailSettingsUiState.newEmail).isEqualTo("new_wordpressuser@gmail.com")

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When the user tries to cancels a pending email change and on success response, don't show any pending new email address`() =
            test {
                // Given
                whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(true)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.cancelPendingEmailChange()).thenReturn(mockSuccessResponse())
                whenever(accountsSettingsRepository.account.pendingEmailChange).thenReturn(false)
                whenever(accountsSettingsRepository.account.newEmail).thenReturn("")

                // When
                viewModel.accountSettingsUiState.value.emailSettingsUiState.onCancelEmailChange.invoke()

                // Then
                Assertions.assertThat(uiStateList.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(false)
                Assertions.assertThat(uiStateList.last().emailSettingsUiState.newEmail).isEqualTo("")
                // Cleanup
                job.cancel()
            }

    // Primary site default
    @Test
    fun `If primary site is available, Should show primary site with the account information from accountSettingsRepository `() =
            test {
                whenever(accountsSettingsRepository.account.primarySiteId).thenReturn(3L)
                viewModel = AccountSettingsViewModel(
                        resourceProvider,
                        networkUtilsWrapper,
                        TEST_DISPATCHER,
                        accountsSettingsRepository
                )
                val mUiState = viewModel.accountSettingsUiState.value
                mUiState.primarySiteSettingsUiState?.primarySite?.let {
                    Assertions.assertThat(it.siteId).isEqualTo(3L)
                }
            }

    @Test
    fun `If primary site is not available, primary site should be null`() =
            test {
                whenever(accountsSettingsRepository.getSitesAccessedViaWPComRest()).thenReturn(listOf())
                viewModel = AccountSettingsViewModel(
                        resourceProvider,
                        networkUtilsWrapper,
                        TEST_DISPATCHER,
                        accountsSettingsRepository
                )
                val mUiState = viewModel.accountSettingsUiState.value
                Assertions.assertThat(mUiState.primarySiteSettingsUiState?.primarySite).isEqualTo(null)
                Assertions.assertThat(mUiState.primarySiteSettingsUiState?.sites?.size).isEqualTo(0)
            }

    @Test
    fun `When the user tries to update a different site as Primary site, optimistically show the user as if the new primary site is changed even before updating in the server`() =
            test {
                // Given
                whenever(accountsSettingsRepository.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.updatePrimaryBlog(siteViewModels.first().siteId.toString())).thenReturn(mockErrorResponse())
                whenever(accountsSettingsRepository.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // When
                viewModel.onPrimarySiteChanged(siteRemoteId = siteViewModels.first().siteId)

                // Then
                Assertions.assertThat(uiStateList[uiStateList.lastIndex -1].primarySiteSettingsUiState?.primarySite?.siteId).isEqualTo(siteViewModels.first().siteId)

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When user tries to update a different site as Primary site and on error reponse, revert back to the old primary site`() =
            test {
                // Given
                whenever(accountsSettingsRepository.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.updatePrimaryBlog(siteViewModels.first().siteId.toString())).thenReturn(mockErrorResponse())
                whenever(accountsSettingsRepository.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // When
                viewModel.onPrimarySiteChanged(siteRemoteId = siteViewModels.first().siteId)

                // Then
                Assertions.assertThat(uiStateList.last().primarySiteSettingsUiState?.primarySite?.siteId).isEqualTo(siteViewModels.last().siteId)

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When user tries to update a different site as Primary site and on successful reponse, new primary site should be shown to the user`() =
            test {
                // Given
                whenever(accountsSettingsRepository.account.primarySiteId).thenReturn(siteViewModels.last().siteId)

                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER){
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.updatePrimaryBlog(siteViewModels.first().siteId.toString())).thenReturn(mockSuccessResponse())
                whenever(accountsSettingsRepository.account.primarySiteId).thenReturn(siteViewModels.first().siteId)

                // When
                viewModel.onPrimarySiteChanged(siteRemoteId = siteViewModels.first().siteId)

                // Then
                Assertions.assertThat(uiStateList.last().primarySiteSettingsUiState?.primarySite?.siteId).isEqualTo(siteViewModels.first().siteId)

                // Cleanup
                job.cancel()
            }

    // Web Address default
    @Test
    fun `If Web Address is available, Should show Web Address with the account information from AccountSettingsRepository`() =
            test {
                whenever(accountsSettingsRepository.account.webAddress).thenReturn("old_webaddress")
                viewModel = AccountSettingsViewModel(
                        resourceProvider,
                        networkUtilsWrapper,
                        TEST_DISPATCHER,
                        accountsSettingsRepository
                )
                val mUiState = viewModel.accountSettingsUiState.value
                Assertions.assertThat(mUiState.webAddressSettingsUiState.webAddress).isEqualTo("old_webaddress")
            }

    @Test
    fun `If Web Address is not available, Web Address should be blank`() =
            test {
                whenever(accountsSettingsRepository.account.webAddress).thenReturn("")
                viewModel = AccountSettingsViewModel(
                        resourceProvider,
                        networkUtilsWrapper,
                        TEST_DISPATCHER,
                        accountsSettingsRepository
                )
                val mUiState = viewModel.accountSettingsUiState.value
                Assertions.assertThat(mUiState.webAddressSettingsUiState.webAddress).isEqualTo("")
            }

    @Test
    fun `When the user tries to update a new webaddress, optimistically show the user as if the new web address is changed even before updating in the server`() =
            test {
                // Given
                whenever(accountsSettingsRepository.account.webAddress).thenReturn("old_webaddress")

                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER){
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.updateWebAddress("new_webaddress")).thenReturn(mockErrorResponse())
                whenever(accountsSettingsRepository.account.webAddress).thenReturn("old_webaddress")

                // When
                viewModel.onWebAddressChanged("new_webaddress")

                // Then
                Assertions.assertThat(uiStateList[uiStateList.lastIndex -1].webAddressSettingsUiState.webAddress).isEqualTo("new_webaddress")

                // Cleanup
                job.cancel()
            }


    @Test
    fun `When user tries to update a new webaddress and on success response, new web address should be shown to user`() =
            test {
                // Given
                whenever(accountsSettingsRepository.account.webAddress).thenReturn("old_webaddress")

                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.updateWebAddress("new_webaddress")).thenReturn(mockSuccessResponse())
                whenever(accountsSettingsRepository.account.webAddress).thenReturn("new_webaddress")

                // When
                viewModel.onWebAddressChanged("new_webaddress")

                // Then
                Assertions.assertThat(uiStateList.last().webAddressSettingsUiState.webAddress).isEqualTo("new_webaddress")

                // Cleanup
                job.cancel()
            }

    // Change password
    @Test
    fun `When the user tries to update the new password, show changing password progress dialog until it receives the server response`() =
            test {
                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER){
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.updatePassword("new_password")).thenReturn(mockSuccessResponse())
                // When
                viewModel.onPasswordChanged("new_password")

                // Then
                Assertions.assertThat(uiStateList[uiStateList.lastIndex - 1].changePasswordSettingsUiState.showChangePasswordProgressDialog).isEqualTo(true)

                // Cleanup
                job.cancel()
            }

    @Test
    fun `When the user tries to update the new password and on receiving the server response, dismiss the changing password progress dialog`() =
            test {
                // Observe uiState change
                initialiseViewModel()
                val uiStateList  = mutableListOf<AccountSettingsUiState>()
                val job = launch(TEST_DISPATCHER) {
                    viewModel.accountSettingsUiState.toList(uiStateList)
                }

                // mock server response
                whenever(accountsSettingsRepository.updatePassword("new_password")).thenReturn(mockSuccessResponse())
                // When
                viewModel.onPasswordChanged("new_password")

                // Then
                Assertions.assertThat(uiStateList.last().changePasswordSettingsUiState.showChangePasswordProgressDialog).isEqualTo(false)

                // Cleanup
                job.cancel()
            }


    // Helper Methods
    private fun mockSuccessResponse(): OnAccountChanged{
        return mock<OnAccountChanged>()
    }

    private fun mockErrorResponse(): OnAccountChanged{
        return OnAccountChanged().apply {
            this.error = AccountError(GENERIC_ERROR,"")
        }
    }

    private fun initialiseViewModel(){
        viewModel = AccountSettingsViewModel(
                resourceProvider,
                networkUtilsWrapper,
                TEST_DISPATCHER,
                accountsSettingsRepository
        )
    }
}
