package org.wordpress.android.ui.prefs.accountsettings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.SiteUiModel
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

    private val siteViewModels = mutableListOf<SiteUiModel>().apply {
        add(SiteUiModel("HappyDay", 1L, "http://happyday.wordpress.com"))
        add(SiteUiModel("WonderLand", 2L, "http://wonderland.wordpress.com"))
        add(SiteUiModel("FantasyBooks", 3L, "http://fantasybooks.wordpress.com"))
    }
    private val uiStateChanges = mutableListOf<AccountSettingsUiState>()
    private val uiState
        get() = viewModel.accountSettingsUiState.value

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
    fun `The initial primarysite is shown from cached account settings`() = test {
        uiState.primarySiteSettingsUiState.primarySite?.siteId?.let {
            assertThat(it).isEqualTo(getAccountUseCase.account.primarySiteId)
        }
    }

    @Test
    fun `The initial username is shown from cached account settings`() = test {
        assertThat(uiState.userNameSettingsUiState.userName)
                .isEqualTo(getAccountUseCase.account.userName)
    }

    @Test
    fun `The username is allowed to change based on the cached account settings`() = test {
        assertThat(uiState.userNameSettingsUiState.canUserNameBeChanged)
                .isEqualTo(getAccountUseCase.account.usernameCanBeChanged)
    }

    @Test
    fun `The username confirmed snackbar is not be shown by default`() = test {
        assertThat(uiState.userNameSettingsUiState.showUserNameConfirmedSnackBar)
                .isEqualTo(false)
    }

    @Test
    fun `The initial emailaddress is shown from cached account settings`() = test {
        assertThat(uiState.emailSettingsUiState.email)
                .isEqualTo(getAccountUseCase.account.email)
    }

    @Test
    fun `The pending emailaddress change snackbar is shown based on cached account settings`() = test {
        assertThat(uiState.emailSettingsUiState.hasPendingEmailChange)
                .isEqualTo(getAccountUseCase.account.pendingEmailChange)
    }

    @Test
    fun `The initial webAddress is shown from cached account settings`() = test {
        assertThat(uiState.webAddressSettingsUiState.webAddress)
                .isEqualTo(getAccountUseCase.account.webAddress)
    }

    @Test
    fun `When the username change is confirmed from the server, then new username is updated`() =
            test {
                viewModel.onUsernameChangeConfirmedFromServer("new_wordpressuser_username")
                assertThat(uiState.userNameSettingsUiState.userName)
                        .isEqualTo("new_wordpressuser_username")
            }

    @Test
    fun `When the username change is confirmed from the server, then the snackbar is shown`() =
            test {
                viewModel.onUsernameChangeConfirmedFromServer("new_wordpressuser_username")
                assertThat(uiState.userNameSettingsUiState.showUserNameConfirmedSnackBar)
                        .isEqualTo(true)
            }

    @Test
    fun `The username confirmed snackbar message is Your new username is new_wordpressuser_username'`() =
            test {
                viewModel.onUsernameChangeConfirmedFromServer("new_wordpressuser_username")
                assertThat(uiState.userNameSettingsUiState.newUserChangeConfirmedSnackBarMessageHolder.message)
                        .isEqualTo(
                                UiStringResWithParams(
                                        string.settings_username_changer_toast_content,
                                        listOf(UiStringText("new_wordpressuser_username"))
                                )
                        )
            }

    @Test
    fun `When the server return 'canUserNameBeChanged' as true, then the username is allowed to change`() =
            test {
                whenever(getAccountUseCase.account.usernameCanBeChanged).thenReturn(true)
                initialiseViewModel()
                assertThat(uiState.userNameSettingsUiState.canUserNameBeChanged).isEqualTo(true)
            }

    @Test
    fun `When the server return 'canUserNameBeChanged' as false, then the username is not allowed to change`() =
            test {
                whenever(getAccountUseCase.account.usernameCanBeChanged).thenReturn(false)
                initialiseViewModel()
                assertThat(uiState.userNameSettingsUiState.canUserNameBeChanged).isEqualTo(false)
            }

    @Test
    fun `When there is a pending emailaddress change, then the snackbar is shown`() =
            test {
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)
                whenever(getAccountUseCase.account.newEmail).thenReturn("new_wordpressuser_username")
                initialiseViewModel()
                assertThat(uiState.emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
            }

    @Test
    fun `The pending emailaddress snackbar message is Click the verification link in the email`() =
            test {
                assertThat(uiState.emailSettingsUiState.emailVerificationMsgSnackBarMessageHolder.message)
                        .isEqualTo(
                                UiStringResWithParams(
                                        string.pending_email_change_snackbar,
                                        listOf(UiStringText(getAccountUseCase.account.newEmail))
                                )
                        )
            }

    @Test
    fun `When there is no pending emailaddress change, then the snackbar is not shown`() =
            test {
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                initialiseViewModel()
                assertThat(uiState.emailSettingsUiState.hasPendingEmailChange).isEqualTo(false)
            }

    @Test
    fun `When a new emailaddress is entered, then the snackbar is shown optimistically`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                // When
                whenever(pushAccountSettingsUseCase.updateEmail("new_wordpressuser@gmail.com"))
                        .thenReturn(mockErrorResponse())
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                whenever(getAccountUseCase.account.newEmail).thenReturn("")
                viewModel.onEmailChanged("new_wordpressuser@gmail.com")
                // Then
                assertThat(uiStateChanges[uiStateChanges.lastIndex - 1].emailSettingsUiState.hasPendingEmailChange)
                        .isEqualTo(true)
                assertThat(uiStateChanges[uiStateChanges.lastIndex - 1].emailSettingsUiState.newEmail)
                        .isEqualTo("new_wordpressuser@gmail.com")
            }

    @Test
    fun `When a new emailaddress change fails in the server, then the snackbar is dismissed`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                // When
                whenever(pushAccountSettingsUseCase.updateEmail("new_wordpressuser@gmail.com"))
                        .thenReturn(mockErrorResponse())
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                whenever(getAccountUseCase.account.newEmail).thenReturn("")
                viewModel.onEmailChanged("new_wordpressuser@gmail.com")
                // Then
                assertThat(uiStateChanges.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(false)
                assertThat(uiStateChanges.last().emailSettingsUiState.newEmail).isEqualTo("")
            }

    @Test
    fun `When a new emailaddress change is updated in the server, then the snackbar continues to show`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)
                // When
                whenever(pushAccountSettingsUseCase.updateEmail("new_wordpressuser@gmail.com"))
                        .thenReturn(mockSuccessResponse())
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)
                whenever(getAccountUseCase.account.newEmail).thenReturn("new_wordpressuser@gmail.com")
                viewModel.onEmailChanged("new_wordpressuser@gmail.com")
                // Then
                assertThat(uiStateChanges.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
                assertThat(uiStateChanges.last().emailSettingsUiState.newEmail).isEqualTo("new_wordpressuser@gmail.com")
            }

    @Test
    fun `When cancelling of a pending emailaddress change fails, then the snackbar continues to show`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)
                whenever(getAccountUseCase.account.newEmail).thenReturn("new_wordpressuser@gmail.com")
                // When
                whenever(pushAccountSettingsUseCase.cancelPendingEmailChange()).thenReturn(mockErrorResponse())
                uiState.emailSettingsUiState.onCancelEmailChange.invoke()
                // Then
                assertThat(uiStateChanges.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(true)
                assertThat(uiStateChanges.last().emailSettingsUiState.newEmail).isEqualTo("new_wordpressuser@gmail.com")
            }

    @Test
    fun `When a pending emailaddress change is cancelled, then the snackbar is dismissed`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(true)
                // When
                whenever(pushAccountSettingsUseCase.cancelPendingEmailChange()).thenReturn(mockSuccessResponse())
                whenever(getAccountUseCase.account.pendingEmailChange).thenReturn(false)
                whenever(getAccountUseCase.account.newEmail).thenReturn("")
                uiState.emailSettingsUiState.onCancelEmailChange.invoke()
                // Then
                assertThat(uiStateChanges.last().emailSettingsUiState.hasPendingEmailChange).isEqualTo(false)
                assertThat(uiStateChanges.last().emailSettingsUiState.newEmail).isEqualTo("")
            }

    @Test
    fun `When a new primarysite is entered, then the primary site is shown optimistically`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)
                // When
                whenever(pushAccountSettingsUseCase.updatePrimaryBlog(siteViewModels.first().siteId.toString()))
                        .thenReturn(mockErrorResponse())
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)
                viewModel.onPrimarySiteChanged(siteRemoteId = siteViewModels.first().siteId)
                // Then
                assertThat(uiStateChanges[uiStateChanges.lastIndex - 1].primarySiteSettingsUiState.primarySite?.siteId)
                        .isEqualTo(siteViewModels.first().siteId)
            }

    @Test
    fun `When a new primarysite change fails in the server, then the old primarysite is reverted back`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)
                // When
                whenever(pushAccountSettingsUseCase.updatePrimaryBlog(siteViewModels.first().siteId.toString()))
                        .thenReturn(mockErrorResponse())
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)
                viewModel.onPrimarySiteChanged(siteRemoteId = siteViewModels.first().siteId)
                // Then
                assertThat(uiStateChanges.last().primarySiteSettingsUiState.primarySite?.siteId)
                        .isEqualTo(siteViewModels.last().siteId)
            }

    @Test
    fun `When a new primarysite change is updated in the server, then new primarysite continues to show`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.last().siteId)
                // When
                whenever(pushAccountSettingsUseCase.updatePrimaryBlog(siteViewModels.first().siteId.toString()))
                        .thenReturn(mockSuccessResponse())
                whenever(getAccountUseCase.account.primarySiteId).thenReturn(siteViewModels.first().siteId)
                viewModel.onPrimarySiteChanged(siteRemoteId = siteViewModels.first().siteId)
                // Then
                assertThat(uiStateChanges.last().primarySiteSettingsUiState.primarySite?.siteId)
                        .isEqualTo(siteViewModels.first().siteId)
            }

    @Test
    fun `When a new webaddress is entered, then new webaddress is shown optimistically`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.webAddress).thenReturn("old_webaddress")
                // When
                whenever(pushAccountSettingsUseCase.updateWebAddress("new_webaddress"))
                        .thenReturn(mockErrorResponse())
                whenever(getAccountUseCase.account.webAddress).thenReturn("old_webaddress")
                viewModel.onWebAddressChanged("new_webaddress")
                // Then
                assertThat(uiStateChanges[uiStateChanges.lastIndex - 1].webAddressSettingsUiState.webAddress)
                        .isEqualTo("new_webaddress")
            }

    @Test
    fun `When a new webaddress is updated in the server, then new webaddress continues to show`() =
            testUiStateChanges {
                // Given
                whenever(getAccountUseCase.account.webAddress).thenReturn("old_webaddress")
                // When
                whenever(pushAccountSettingsUseCase.updateWebAddress("new_webaddress"))
                        .thenReturn(mockSuccessResponse())
                whenever(getAccountUseCase.account.webAddress).thenReturn("new_webaddress")
                viewModel.onWebAddressChanged("new_webaddress")
                // Then
                assertThat(uiStateChanges.last().webAddressSettingsUiState.webAddress).isEqualTo("new_webaddress")
            }

    @Test
    fun `When a new password is entered, then the changing password progress dialog is shown`() =
            testUiStateChanges {
                // Given
                whenever(pushAccountSettingsUseCase.updatePassword("new_password"))
                        .thenReturn(mockSuccessResponse())
                // When
                viewModel.onPasswordChanged("new_password")
                // Then
                assertThat(
                        uiStateChanges[uiStateChanges.lastIndex - 1]
                                .changePasswordSettingsUiState.showChangePasswordProgressDialog
                )
                        .isEqualTo(true)
            }

    @Test
    fun `When a new password is updated in the server, then the changing password progress dialog is dismissed`() =
            testUiStateChanges {
                // Given
                whenever(pushAccountSettingsUseCase.updatePassword("new_password"))
                        .thenReturn(mockSuccessResponse())
                // When
                viewModel.onPasswordChanged("new_password")
                // Then
                assertThat(uiStateChanges.last().changePasswordSettingsUiState.showChangePasswordProgressDialog)
                        .isEqualTo(false)
            }

    // Helper Methods
    private fun <T> testUiStateChanges(
        block: suspend CoroutineScope.() -> T
    ) {
        test {
            uiStateChanges.clear()
            initialiseViewModel()
            val job = launch(TEST_DISPATCHER) {
                viewModel.accountSettingsUiState.toList(uiStateChanges)
            }
            this.block()
            job.cancel()
        }
    }

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
