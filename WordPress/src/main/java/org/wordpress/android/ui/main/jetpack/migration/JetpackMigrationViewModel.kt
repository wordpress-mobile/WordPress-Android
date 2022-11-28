package org.wordpress.android.ui.main.jetpack.migration

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.localcontentmigration.LocalMigrationState
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished.Successful
import org.wordpress.android.localcontentmigration.LocalMigrationState.Initial
import org.wordpress.android.localcontentmigration.LocalMigrationState.Migrating
import org.wordpress.android.localcontentmigration.MigrationEmailHelper
import org.wordpress.android.sharedlogin.resolver.LocalMigrationOrchestrator
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DeletePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DeleteSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DonePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.NotificationsPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomeSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.CompleteFlow
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.ShowHelp
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content.Delete
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content.Done
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content.Notifications
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content.Welcome
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Error.Generic
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Error.Networking
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Loading
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.PreventDuplicateNotifsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@HiltViewModel
class JetpackMigrationViewModel @Inject constructor(
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val gravatarUtilsWrapper: GravatarUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val preventDuplicateNotifsFeatureConfig: PreventDuplicateNotifsFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val localMigrationOrchestrator: LocalMigrationOrchestrator,
    private val migrationEmailHelper: MigrationEmailHelper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(Loading)

    private val _actionEvents = Channel<JetpackMigrationActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private val migrationStateFlow = MutableStateFlow<LocalMigrationState>(Initial)
    private val continueClickedFlow = MutableStateFlow(false)
    private val notificationContinueClickedFlow = MutableStateFlow(false)
    private var showDeleteState: Boolean = false

    val uiState = combineTransform(migrationStateFlow, continueClickedFlow, notificationContinueClickedFlow) {
        migrationState, continueClicked, notificationContinueClicked ->
        when {
            showDeleteState -> emit(
                    Delete(
                            primaryActionButton = DeletePrimaryButton(::onGotItClicked),
                            secondaryActionButton = DeleteSecondaryButton(::onHelpClicked),
                    )
            )
            migrationState is Initial -> emit(Loading)
            migrationState is Migrating -> emit(
                    Welcome(
                            userAvatarUrl = resizeAvatarUrl(migrationState.avatarUrl),
                            isProcessing = continueClicked,
                            sites = migrationState.sites.map(::siteUiFromModel),
                            primaryActionButton = WelcomePrimaryButton(::onContinueClicked),
                            secondaryActionButton = WelcomeSecondaryButton(::onHelpClicked),
                    )
            )
            migrationState is Successful && continueClicked -> when {
                !notificationContinueClicked -> emit(
                        Notifications(
                                primaryActionButton = NotificationsPrimaryButton(::onContinueFromNotificationsClicked),
                        )
                )
                else -> emit(
                        Done(
                                primaryActionButton = DonePrimaryButton(::onDoneClicked)
                        )
                )
            }
            migrationState is Failure -> emit(
                    UiState.Error(
                            primaryActionButton = ErrorPrimaryButton(::onTryAgainClicked),
                            secondaryActionButton = ErrorSecondaryButton(::onHelpClicked),
                            type = Generic,
                    )
            )
            else -> Unit
        }
    }

    fun start(showDeleteState: Boolean) {
        this.showDeleteState = showDeleteState
        tryMigration()
    }

    private fun siteUiFromModel(site: SiteModel) = SiteListItemUiState(
            id = site.siteId,
            name = siteUtilsWrapper.getSiteNameOrHomeURL(site),
            url = siteUtilsWrapper.getHomeURLOrHostName(site),
            iconUrl = siteUtilsWrapper.getSiteIconUrlOfResourceSize(
                    site,
                    R.dimen.jp_migration_site_icon_size,
            ),
    )

    private fun onContinueClicked() {
        continueClickedFlow.value = true
    }

    @Suppress("ForbiddenComment", "unused")
    private fun postGenericErrorState() {
        // TODO: Call this method when migration fails with generic error
        _uiState.value = UiState.Error(
                primaryActionButton = ErrorPrimaryButton(::onTryAgainClicked),
                secondaryActionButton = ErrorSecondaryButton(::onHelpClicked),
                type = Generic,
        )
    }

    @Suppress("ForbiddenComment", "unused")
    private fun postNetworkingErrorState() {
        // TODO: Call this method when migration fails with networking error
        _uiState.value = UiState.Error(
                primaryActionButton = ErrorPrimaryButton(::onTryAgainClicked),
                secondaryActionButton = ErrorSecondaryButton(::onHelpClicked),
                type = Networking,
        )
    }

    private fun onTryAgainClicked() {
        (_uiState.value as? UiState.Error)?.let {
            _uiState.value = it.copy(isProcessing = true)
            tryMigration()
        }
    }

    private fun tryMigration() {
            viewModelScope.launch(Dispatchers.IO) {
                localMigrationOrchestrator.tryLocalMigration(migrationStateFlow)
            }
    }

    private fun onContinueFromNotificationsClicked() {
        if (preventDuplicateNotifsFeatureConfig.isEnabled()) disableNotificationsOnWP()
        notificationContinueClickedFlow.value = true
    }

    private fun disableNotificationsOnWP() {
        AppLog.d(T.NOTIFS, "Disable Notifications")
        Intent().also { intent ->
            intent.action = "org.wordpress.android.broadcast.DISABLE_NOTIFICATIONS"
            val appPackage = BuildConfig.APPLICATION_ID.replace("com.jetpack", "org.wordpress")
            intent.setPackage(appPackage)
            AppLog.d(T.NOTIFS, intent.toString())
            contextProvider.getContext().sendBroadcast(intent, "org.wordpress.android.permission.DISABLE_NOTIFICATIONS")
        }
    }

    private fun onDoneClicked() {
        migrationEmailHelper.notifyMigrationComplete()
        appPrefsWrapper.setJetpackMigrationCompleted(true)
        postActionEvent(CompleteFlow)
    }

    private fun onHelpClicked() {
        postActionEvent(ShowHelp)
    }

    private fun onGotItClicked() {
        postActionEvent(CompleteFlow)
    }

    private fun resizeAvatarUrl(avatarUrl: String) = gravatarUtilsWrapper.fixGravatarUrlWithResource(
            avatarUrl,
            R.dimen.jp_migration_user_avatar_size
    )

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

            data class Delete(
                override val primaryActionButton: ActionButton,
                override val secondaryActionButton: ActionButton
            ) : Content(
                    primaryActionButton = primaryActionButton,
                    screenIconRes = R.drawable.ic_jetpack_migration_delete,
                    title = UiStringRes(R.string.jp_migration_delete_title),
                    subtitle = UiStringRes(R.string.jp_migration_delete_subtitle),
                    message = UiStringRes(R.string.jp_migration_delete_message),
            ) {
                val deleteWpIcon = R.drawable.ic_jetpack_migration_delete_wp
            }
        }

        data class Error(
            val primaryActionButton: ErrorPrimaryButton,
            val secondaryActionButton: ErrorSecondaryButton,
            val type: ErrorType,
            val isProcessing: Boolean = false,
        ) : UiState() {
            @DrawableRes val screenIconRes = R.drawable.ic_jetpack_migration_error

            sealed class ErrorType(
                val title: UiString,
                val subtitle: UiString,
                val message: UiString,
            )

            object Generic : ErrorType(
                    title = UiStringRes(R.string.jp_migration_generic_error_title),
                    subtitle = UiStringRes(R.string.jp_migration_generic_error_subtitle),
                    message = UiStringRes(R.string.jp_migration_generic_error_message),
            )

            object Networking : ErrorType(
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

        data class DeletePrimaryButton(
            override val onClick: () -> Unit
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_got_it_button)
        )

        data class DeleteSecondaryButton(
            override val onClick: () -> Unit
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_need_help_button)
        )
    }

    sealed class JetpackMigrationActionEvent {
        object ShowHelp : JetpackMigrationActionEvent()
        object CompleteFlow : JetpackMigrationActionEvent()
    }
}
