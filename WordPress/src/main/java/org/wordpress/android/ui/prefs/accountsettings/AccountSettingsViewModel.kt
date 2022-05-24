package org.wordpress.android.ui.prefs.accountsettings

import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.AccountStore.AccountError
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType.SETTINGS_FETCH_GENERIC_ERROR
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType.SETTINGS_FETCH_REAUTHORIZATION_REQUIRED_ERROR
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType.SETTINGS_POST_ERROR
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.accountsettings.usecase.FetchAccountSettingsUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetAccountUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetSitesUseCase
import org.wordpress.android.ui.prefs.accountsettings.usecase.PushAccountSettingsUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val ONE_SITE = 1
class AccountSettingsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val fetchAccountSettingsUseCase: FetchAccountSettingsUseCase,
    private val pushAccountSettingsUseCase: PushAccountSettingsUseCase,
    private val getAccountUseCase: GetAccountUseCase,
    private val getSitesUseCase: GetSitesUseCase,
    private val optimisticUpdateHandler: AcountSettingsOptimisticUpdateHandler
) : ScopedViewModel(mainDispatcher) {
    var fetchNewSettingsJob: Job? = null
    init {
        viewModelScope.launch {
            getSitesAccessedViaWPComRest()
        }
        if (networkUtilsWrapper.isNetworkAvailable()) {
            fetchNewSettingsJob = viewModelScope.launch {
                val onAccountChanged = fetchAccountSettingsUseCase.fetchNewSettings()
                if (onAccountChanged.isError) {
                    handleError(onAccountChanged.error)
                }
                updateAccountSettingsUiState()
            }
        }
    }

    private var _accountSettingsUiState = MutableStateFlow(getAccountSettingsUiState())
    val accountSettingsUiState: StateFlow<AccountSettingsUiState> = _accountSettingsUiState.asStateFlow()

    private fun getAccountSettingsUiState(): AccountSettingsUiState {
        val siteViewModels = _accountSettingsUiState?.value?.primarySiteSettingsUiState?.sites
        val primarySiteViewModel = siteViewModels
                ?.firstOrNull { it.siteId == getAccountUseCase.account.primarySiteId }
        val account = getAccountUseCase.account
        val uistate = AccountSettingsUiState(
                userNameSettingsUiState = UserNameSettingsUiState(
                        account.userName,
                        account.displayName,
                        account.usernameCanBeChanged
                ),
                emailSettingsUiState = EmailSettingsUiState(
                        account.email,
                        account.newEmail,
                        account.pendingEmailChange
                ) { cancelPendingEmailChange() },
                primarySiteSettingsUiState = PrimarySiteSettingsUiState(
                        primarySiteViewModel,
                        siteViewModels
                ),
                webAddressSettingsUiState = WebAddressSettingsUiState(account.webAddress),
                changePasswordSettingsUiState = ChangePasswordSettingsUiState(false),
                error = null
        )
        return optimisticUpdateHandler.applyOptimisticallyChangedPreferences(uistate)
    }

    private suspend fun getSitesAccessedViaWPComRest() {
        val siteViewModels = getSitesUseCase.get().map {
            SiteViewModel(SiteUtils.getSiteNameOrHomeURL(it), it.siteId, SiteUtils.getHomeURLOrHostName(it))
        }
        _accountSettingsUiState.update { state ->
            state.copy(
                    primarySiteSettingsUiState = PrimarySiteSettingsUiState(
                         siteViewModels.firstOrNull { it.siteId == getAccountUseCase.account.primarySiteId },
                         siteViewModels
                    )
            )
        }
    }

    private fun cancelPendingEmailChange() {
        onAccountSettingsChange { pushAccountSettingsUseCase.cancelPendingEmailChange() }
    }

    fun onUsernameChangeConfirmedFromServer(userName: String) {
        _accountSettingsUiState.update {
            it.copy(userNameSettingsUiState =
            it.userNameSettingsUiState.copy(
                    userName = userName,
                    showUserNameConfirmedSnackBar = true))
        }
    }

    fun onPrimarySiteChanged(siteRemoteId: Long) {
        val addOptimisticUpdate = optimisticUpdateHandler.update(PRIMARYSITE_PREFERENCE_KEY, siteRemoteId.toString())
        val removeOptimisticUpdate = optimisticUpdateHandler.removeFirstChange(PRIMARYSITE_PREFERENCE_KEY)
        onAccountSettingsChange(addOptimisticUpdate, removeOptimisticUpdate) {
            pushAccountSettingsUseCase.updatePrimaryBlog(siteRemoteId.toString())
        }
    }

    fun onEmailChanged(newEmail: String) {
        val addOptimisticUpdate = optimisticUpdateHandler.update(EMAIL_PREFERENCE_KEY, newEmail)
        val removeOptimisticUpdate = optimisticUpdateHandler.removeFirstChange(EMAIL_PREFERENCE_KEY)
        onAccountSettingsChange(addOptimisticUpdate, removeOptimisticUpdate) {
            pushAccountSettingsUseCase.updateEmail(newEmail)
        }
    }

    fun onWebAddressChanged(newWebAddress: String) {
        val addOptimisticUpdate = optimisticUpdateHandler.update(WEBADDRESS_PREFERENCE_KEY, newWebAddress)
        val removeOptimisticUpdate = optimisticUpdateHandler.removeFirstChange(WEBADDRESS_PREFERENCE_KEY)
        onAccountSettingsChange(addOptimisticUpdate, removeOptimisticUpdate) {
            pushAccountSettingsUseCase.updateWebAddress(newWebAddress)
        }
    }

    fun onPasswordChanged(newPassword: String) {
        _accountSettingsUiState.update {
            it.copy(
                    changePasswordSettingsUiState = it.changePasswordSettingsUiState.copy(
                            showChangePasswordProgressDialog = true
                    ),
            )
        }
        onAccountSettingsChange { pushAccountSettingsUseCase.updatePassword(newPassword) }
    }

    private fun onAccountSettingsChange(
        addOptimisticUpdate: (() -> Unit?)? = null,
        removeOptimisticUpdate: (() -> Unit?)? = null,
        updateAccountSettings: suspend () -> OnAccountChanged
    ) {
        addOptimisticUpdate?.invoke()
        updateAccountSettingsUiState()
        fetchNewSettingsJob?.cancel()
        viewModelScope.launch {
            val onAccountChangedEvent = updateAccountSettings.invoke()
            removeOptimisticUpdate?.invoke()
            updateAccountSettingsUiState()
            if (onAccountChangedEvent.isError) {
                handleError(onAccountChangedEvent.error)
            }
        }
    }

    private fun updateAccountSettingsUiState() {
        _accountSettingsUiState.value = getAccountSettingsUiState()
    }

    private fun handleError(accountError: AccountError) {
        val errorMessage: String? = when (accountError.type) {
            SETTINGS_FETCH_GENERIC_ERROR -> resourceProvider.getString(string.error_fetch_account_settings)
            SETTINGS_FETCH_REAUTHORIZATION_REQUIRED_ERROR -> resourceProvider.getString(string.error_disabled_apis)
            SETTINGS_POST_ERROR -> {
                if (!TextUtils.isEmpty(accountError.message)) accountError.message else resourceProvider.getString(
                        string.error_post_account_settings
                )
            }
            else -> null
        }
        errorMessage?.let { updateErrorUiState(it) }
    }

    private fun updateErrorUiState(errorMessage: String?) {
        _accountSettingsUiState.update {
            it.copy(error = errorMessage)
        }
    }

    fun onToastShown(toastMessage: String) {
        if (_accountSettingsUiState.value.error.equals(toastMessage)) {
            updateErrorUiState(null)
        }
    }

    data class UserNameSettingsUiState(
        val userName: String,
        val displayName: String,
        val canUserNameBeChanged: Boolean,
        val showUserNameConfirmedSnackBar: Boolean = false
    ) {
        val newUserChangeConfirmedSnackBarMessageHolder
            get() = SnackbarMessageHolder(
                    message = UiStringResWithParams(
                            string.settings_username_changer_toast_content,
                            listOf(UiStringText(userName))
                    ),
                    duration = Snackbar.LENGTH_LONG
            )
    }

    data class EmailSettingsUiState(
        val email: String,
        val newEmail: String? = null,
        val hasPendingEmailChange: Boolean = false,
        val onCancelEmailChange: () -> Unit
    ) {
        val emailVerificationMsgSnackBarMessageHolder
            get() = SnackbarMessageHolder(
                    message = UiStringResWithParams(
                            string.pending_email_change_snackbar,
                            listOf(UiStringText(newEmail ?: ""))
                    ),
                    buttonTitle = UiStringRes(string.button_discard),
                    buttonAction = { onCancelEmailChange() },
                    duration = Snackbar.LENGTH_INDEFINITE
            )
    }

    data class SiteViewModel(val siteName: String, val siteId: Long, val homeURLOrHostName: String)

    data class PrimarySiteSettingsUiState(val primarySite: SiteViewModel? = null, val sites: List<SiteViewModel>?) {
        val siteNames
            get() = sites?.map { it.siteName }?.toTypedArray()

        val siteIds
            get() = sites?.map { it.siteId.toString() }?.toTypedArray()

        val homeURLOrHostNames
            get() = sites?.map { it.homeURLOrHostName }?.toTypedArray()
    }

    data class WebAddressSettingsUiState(val webAddress: String)

    data class ChangePasswordSettingsUiState(val showChangePasswordProgressDialog: Boolean)

    data class AccountSettingsUiState(
        val userNameSettingsUiState: UserNameSettingsUiState,
        val emailSettingsUiState: EmailSettingsUiState,
        val primarySiteSettingsUiState: PrimarySiteSettingsUiState,
        val webAddressSettingsUiState: WebAddressSettingsUiState,
        val changePasswordSettingsUiState: ChangePasswordSettingsUiState,
        val error: String?
    )

    override fun onCleared() {
        pushAccountSettingsUseCase.onCleared()
        super.onCleared()
    }
}
