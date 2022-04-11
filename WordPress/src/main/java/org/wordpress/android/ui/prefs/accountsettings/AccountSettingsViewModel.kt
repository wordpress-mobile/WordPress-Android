package org.wordpress.android.ui.prefs.accountsettings

import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class AccountSettingsViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private var accountsSettingsRepository: AccountSettingsRepository
) : ScopedViewModel(mainDispatcher) {

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
        val error: String?)
}
