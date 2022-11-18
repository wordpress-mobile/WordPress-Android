package org.wordpress.android.ui.main.jetpack.migration

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DonePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.NotificationsPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomeSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.CompleteFlow
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.ShowHelp
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Loading
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject

@HiltViewModel
class JetpackMigrationViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val gravatarUtilsWrapper: GravatarUtilsWrapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _actionEvents = Channel<JetpackMigrationActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    // TODO Review this after data sync work is done
    fun onAccountInfoLoaded() {
        if (isDataAvailable()) {
            initWelcomeState()
        }
    }

    // TODO Review this after data sync work is done
    fun onSiteListLoaded() {
        if (isDataAvailable()) {
            initWelcomeState()
        }
    }

    private fun isDataAvailable() = accountStore.account.userName.isNotEmpty() && siteStore.sites.isNotEmpty()

    private fun initWelcomeState() {
        _uiState.value = Content.Welcome(
                userAvatarUrl = getAvatarUrl(),
                sites = getSiteList(),
                primaryActionButton = WelcomePrimaryButton(::onContinueClicked),
                secondaryActionButton = WelcomeSecondaryButton(::onHelpClicked),
        )
    }

    @Suppress("ForbiddenComment", "MagicNumber")
    private fun onContinueClicked() {
        (_uiState.value as? Content.Welcome)?.let {
            viewModelScope.launch {
                _uiState.value = it.copy(isProcessing = true)
                // TODO: Replace this temporary delay with migration logic
                delay(2500)
                // TODO: Navigate to error step if migration has errors
                postNotificationsState()
            }
        }
    }

    private fun postNotificationsState() {
        _uiState.value = Content.Notifications(
                primaryActionButton = NotificationsPrimaryButton(::onContinueFromNotificationsClicked),
        )
    }

    @Suppress("ForbiddenComment")
    private fun onContinueFromNotificationsClicked() {
        // TODO: Disable notifications in WP app
        //  See https://github.com/wordpress-mobile/WordPress-Android/pull/17371
        postDoneState()
    }

    private fun postDoneState() {
        _uiState.value = Content.Done(
                primaryActionButton = DonePrimaryButton(::onDoneClicked),
        )
    }

    @Suppress("ForbiddenComment")
    private fun onDoneClicked() {
        postActionEvent(CompleteFlow)
    }

    private fun onHelpClicked() {
        postActionEvent(ShowHelp)
    }

    private fun getSiteList(): List<SiteListItemUiState> {
        return siteStore.sites.map { site ->
            SiteListItemUiState(
                    id = site.siteId,
                    name = siteUtilsWrapper.getSiteNameOrHomeURL(site),
                    url = siteUtilsWrapper.getHomeURLOrHostName(site),
                    iconUrl = siteUtilsWrapper.getSiteIconUrlOfResourceSize(
                            site,
                            R.dimen.jp_migration_site_icon_size,
                    ),
            )
        }
    }

    private fun getAvatarUrl(): String {
        return gravatarUtilsWrapper.fixGravatarUrlWithResource(
                accountStore.account?.avatarUrl.orEmpty(),
                R.dimen.jp_migration_user_avatar_size,
        )
    }

    private fun postActionEvent(actionEvent: JetpackMigrationActionEvent) {
        viewModelScope.launch {
            _actionEvents.send(actionEvent)
        }
    }

    sealed class UiState {
        object Loading : UiState()

        sealed class Content(
            @DrawableRes val screenIconRes: Int,
            val title: UiString,
            val subtitle: UiString,
            val message: UiString,
            open val primaryActionButton: ActionButton,
            open val secondaryActionButton: ActionButton? = null,
        ) : UiState() {
            data class Welcome(
                val userAvatarUrl: String = "",
                val isProcessing: Boolean = false,
                val sites: List<SiteListItemUiState>,
                override val primaryActionButton: ActionButton,
                override val secondaryActionButton: ActionButton,
            ) : Content(
                    primaryActionButton = primaryActionButton,
                    secondaryActionButton = secondaryActionButton,
                    screenIconRes = R.drawable.ic_wordpress_jetpack_logo,
                    title = UiStringRes(R.string.jp_migration_welcome_title),
                    subtitle = UiStringRes(R.string.jp_migration_welcome_subtitle),
                    message = UiStringRes(
                            if (sites.size > 1) {
                                R.string.jp_migration_welcome_sites_found_message
                            } else {
                                R.string.jp_migration_welcome_site_found_message
                            }
                    ),
            )

            data class Notifications(
                override val primaryActionButton: ActionButton,
            ) : Content(
                    primaryActionButton = primaryActionButton,
                    screenIconRes = R.drawable.ic_jetpack_migration_notifications,
                    title = UiStringRes(R.string.jp_migration_notifications_title),
                    subtitle = UiStringRes(R.string.jp_migration_notifications_subtitle),
                    message = UiStringRes(R.string.jp_migration_notifications_disabled_in_wp_message),
            )

            data class Done(
                override val primaryActionButton: ActionButton,
            ) : Content(
                    primaryActionButton = primaryActionButton,
                    screenIconRes = R.drawable.ic_jetpack_migration_success,
                    title = UiStringRes(R.string.jp_migration_done_title),
                    subtitle = UiStringRes(R.string.jp_migration_done_subtitle),
                    message = UiStringRes(R.string.jp_migration_done_delete_wp_message),
            ) {
                val deleteWpIcon = R.drawable.ic_jetpack_migration_delete_wp
            }
        }

        sealed class Error(
            val title: UiString,
            val subtitle: UiString,
            val message: UiString,
            open val primaryActionButton: ErrorPrimaryButton,
            open val secondaryActionButton: ErrorSecondaryButton,
        ) {
            @DrawableRes val screenIconRes = R.drawable.ic_jetpack_migration_error

            data class Generic(
                override val primaryActionButton: ErrorPrimaryButton,
                override val secondaryActionButton: ErrorSecondaryButton,
            ) : Error(
                    primaryActionButton = primaryActionButton,
                    secondaryActionButton = secondaryActionButton,
                    title = UiStringRes(R.string.jp_migration_generic_error_title),
                    subtitle = UiStringRes(R.string.jp_migration_generic_error_subtitle),
                    message = UiStringRes(R.string.jp_migration_generic_error_message),
            )

            data class Networking(
                override val primaryActionButton: ErrorPrimaryButton,
                override val secondaryActionButton: ErrorSecondaryButton,
            ) : Error(
                    primaryActionButton = primaryActionButton,
                    secondaryActionButton = secondaryActionButton,
                    title = UiStringRes(R.string.jp_migration_network_error_title),
                    subtitle = UiStringRes(R.string.jp_migration_network_error_subtitle),
                    message = UiStringRes(R.string.jp_migration_network_error_message),
            )
        }
    }

    data class SiteListItemUiState(
        val id: Long,
        val name: String,
        val url: String,
        val iconUrl: String,
    )

    sealed class ActionButton(
        val text: UiString,
        open val onClick: () -> Unit,
    ) {
        data class WelcomePrimaryButton(
            override val onClick: () -> Unit,
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_continue_button),
        )

        data class WelcomeSecondaryButton(
            override val onClick: () -> Unit,
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_help_button),
        )

        data class NotificationsPrimaryButton(
            override val onClick: () -> Unit,
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_continue_button),
        )

        data class DonePrimaryButton(
            override val onClick: () -> Unit,
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_finish_button),
        )

        data class ErrorPrimaryButton(
            override val onClick: () -> Unit,
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_try_again_button),
        )

        data class ErrorSecondaryButton(
            override val onClick: () -> Unit,
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_help_button),
        )
    }

    sealed class JetpackMigrationActionEvent {
        object ShowHelp : JetpackMigrationActionEvent()
        object CompleteFlow : JetpackMigrationActionEvent()
    }
}
