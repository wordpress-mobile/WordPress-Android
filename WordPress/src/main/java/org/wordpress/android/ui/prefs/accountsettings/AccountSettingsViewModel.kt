package org.wordpress.android.ui.prefs.accountsettings

import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineDispatcher
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
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class AccountSettingsViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private var accountsSettingsRepository: AccountSettingsRepository
) : ScopedViewModel(mainDispatcher) {

    private val sitesAccessedViaWPComRest: List<SiteViewModel> by lazy {
        accountsSettingsRepository.getSitesAccessedViaWPComRest().map {
            SiteViewModel(SiteUtils.getSiteNameOrHomeURL(it), it.siteId, SiteUtils.getHomeURLOrHostName(it))
        }
    }

    private var _accountSettingsUiState = MutableStateFlow(getAccountSettingsUiState())
    val accountSettingsUiState: StateFlow<AccountSettingsUiState> = _accountSettingsUiState.asStateFlow()

    private fun getAccountSettingsUiState(): AccountSettingsUiState {
        val primarySiteViewModel = sitesAccessedViaWPComRest
                .firstOrNull { it.siteId == accountsSettingsRepository.account.primarySiteId }
        val account = accountsSettingsRepository.account
        return AccountSettingsUiState(
                userNameSettingsUiState = UserNameSettingsUiState(
                        account.userName,
                        account.displayName,
                        account.usernameCanBeChanged
                ),
                emailSettingsUiState = EmailSettingsUiState(
                        account.email,
                        account.newEmail,
                        account.pendingEmailChange
                ) { },
                primarySiteSettingsUiState = PrimarySiteSettingsUiState(
                        primarySiteViewModel,
                        sitesAccessedViaWPComRest
                ),
                webAddressSettingsUiState = WebAddressSettingsUiState(account.webAddress),
                changePasswordSettingsUiState = ChangePasswordSettingsUiState(false),
                error = null
        )
    }

    fun onUsernameChangeConfirmedFromServer(userName: String){
        //TODO
    }

    fun onPrimarySiteChanged(siteRemoteId: Long) {
        //TODO
    }

    fun onEmailChanged(newEmail: String) {
        //TODO
    }

    fun onWebAddressChanged(newWebAddress: String) {
        //TODO
    }

    fun onPasswordChanged(newPassword: String) {
        //TODO
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

    data class PrimarySiteSettingsUiState(val primarySite: SiteViewModel? = null, val sites: List<SiteViewModel>)

    data class WebAddressSettingsUiState(val webAddress: String)

    data class ChangePasswordSettingsUiState(val showChangePasswordProgressDialog: Boolean)

    data class AccountSettingsUiState(
        val userNameSettingsUiState: UserNameSettingsUiState,
        val emailSettingsUiState: EmailSettingsUiState,
        val primarySiteSettingsUiState: PrimarySiteSettingsUiState?,
        val webAddressSettingsUiState: WebAddressSettingsUiState,
        val changePasswordSettingsUiState: ChangePasswordSettingsUiState,
        val error: String?
    )
}
