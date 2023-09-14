package org.wordpress.android.ui.main.jetpack.migration

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker
import org.wordpress.android.localcontentmigration.LocalMigrationState
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished.Ineligible
import org.wordpress.android.localcontentmigration.LocalMigrationState.Finished.Successful
import org.wordpress.android.localcontentmigration.LocalMigrationState.Initial
import org.wordpress.android.localcontentmigration.LocalMigrationState.Migrating
import org.wordpress.android.localcontentmigration.MigrationEmailHelper
import org.wordpress.android.localcontentmigration.WelcomeScreenData
import org.wordpress.android.modules.UI_THREAD
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
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.FallbackToLogin
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.FinishActivity
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.Logout
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.ShowHelp
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content.Delete
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content.Done
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content.Notifications
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content.Welcome
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Error.Generic
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Loading
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.PreMigrationDeepLinkData
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.JetpackMigrationLanguageUtil
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.PreventDuplicateNotifsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackMigrationViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val gravatarUtilsWrapper: GravatarUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val preventDuplicateNotifsFeatureConfig: PreventDuplicateNotifsFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val localMigrationOrchestrator: LocalMigrationOrchestrator,
    private val migrationEmailHelper: MigrationEmailHelper,
    private val migrationAnalyticsTracker: ContentMigrationAnalyticsTracker,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val jetpackMigrationLanguageUtil: JetpackMigrationLanguageUtil,
) : ScopedViewModel(mainDispatcher) {
    private val _actionEvents = Channel<JetpackMigrationActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private val _refreshAppTheme = MutableLiveData<Unit>()
    val refreshAppTheme: LiveData<Unit> = _refreshAppTheme

    private val _refreshAppLanguage = MutableLiveData<String>()
    val refreshAppLanguage: LiveData<String> = _refreshAppLanguage

    private var isStarted = false
    private val migrationStateFlow = MutableStateFlow<LocalMigrationState>(Initial)
    private val continueClickedFlow = MutableStateFlow(false)
    private val notificationContinueClickedFlow = MutableStateFlow(false)
    private var showDeleteState: Boolean = false
    private var deepLinkData: PreMigrationDeepLinkData? = null

    val uiState = combineTransform(
        migrationStateFlow,
        continueClickedFlow,
        notificationContinueClickedFlow
    ) { migrationState, continueClicked, notificationContinueClicked ->
        when {
            showDeleteState -> emit(initPleaseDeleteWordPressAppScreenUi())
            migrationState is Ineligible -> {
                appPrefsWrapper.setJetpackMigrationEligible(false)
                emit(Loading)
                logoutAndFallbackToLogin()
            }

            migrationState is Initial -> emit(Loading)
            migrationState is Migrating
                    || migrationState is Successful && !continueClicked -> emit(
                initWelcomeScreenUi(migrationState.data, continueClicked)
            )

            migrationState is Successful -> when {
                !notificationContinueClicked -> emit(initNotificationsScreenUi())
                else -> emit(initSuccessScreenUi())
            }

            migrationState is Failure -> emit(initErrorScreenUi())
            else -> Unit
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, Loading)

    fun start(
        showDeleteState: Boolean,
        application: WordPress,
        deepLinkData: PreMigrationDeepLinkData?
    ) {
        if (isStarted) return
        isStarted = true

        this.showDeleteState = showDeleteState
        if (showDeleteState) return

        this.deepLinkData = deepLinkData
        tryMigration(application)
    }

    private fun resetIfNeeded(application: WordPress) {
        if (appPrefsWrapper.isJetpackMigrationInProgress()) {
            application.wordPressComSignOut()
            appPrefsWrapper.saveIsFirstTrySharedLoginJetpack(true)
            appPrefsWrapper.saveIsFirstTryUserFlagsJetpack(true)
            appPrefsWrapper.saveIsFirstTryReaderSavedPostsJetpack(true)
            appPrefsWrapper.saveIsFirstTryBloggingRemindersSyncJetpack(true)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun initWelcomeScreenUi(data: WelcomeScreenData, isContinueClicked: Boolean): Welcome {
        if (uiState.value !is Welcome) {
            migrationAnalyticsTracker.trackWelcomeScreenShown()
        }

        if (data.flags.isNotEmpty()) {
            emitLanguageRefreshIfNeeded(extractLanguageFromFlagsMap(data.flags))
            _refreshAppTheme.value = Unit
        }

        return Welcome(
            userAvatarUrl = resizeAvatarUrl(data.avatarUrl),
            isProcessing = isContinueClicked,
            sites = data.sites.map(::siteUiFromModel),
            onAvatarClicked = { onHelpClicked(source = HelpButtonSource.WelcomeAvatar) },
            primaryActionButton = WelcomePrimaryButton(::onContinueClicked),
            secondaryActionButton = WelcomeSecondaryButton {
                onHelpClicked(source = HelpButtonSource.Welcome)
            },
        )
    }

    private fun extractLanguageFromFlagsMap(userPrefs: Map<String, Any?>): String {
        val languageKey = localeManagerWrapper.getLocalePrefKeyString()
        return userPrefs[languageKey] as? String ?: ""
    }

    private fun emitLanguageRefreshIfNeeded(languageCode: String) {
        if (languageCode.isNotEmpty()) {
            val shouldEmitLanguageRefresh = !localeManagerWrapper.isSameLanguage(languageCode)
            if (shouldEmitLanguageRefresh) {
                _refreshAppLanguage.value = languageCode
            }
        }
    }

    fun setAppLanguage(locale: Locale) {
        jetpackMigrationLanguageUtil.applyLanguage(locale.language)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun initNotificationsScreenUi(): Notifications {
        migrationAnalyticsTracker.trackNotificationsScreenShown()

        return Notifications(
            primaryActionButton = NotificationsPrimaryButton(::onContinueFromNotificationsClicked),
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun initSuccessScreenUi(): Done {
        migrationAnalyticsTracker.trackThanksScreenShown()

        return Done(
            primaryActionButton = DonePrimaryButton(::onFinishClicked)
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun initPleaseDeleteWordPressAppScreenUi(): Delete {
        migrationAnalyticsTracker.trackPleaseDeleteWordPressScreenShown()

        // We need to manually apply the app language for the Compose UI since the host JetpackMigrationActivity
        // does not inherit from LocaleAwareActivity on purpose, in order to avoid possible issues
        // when the Ui mode (dark/light) and the language are manually set by the user.
        emitLanguageRefreshIfNeeded(localeManagerWrapper.getLanguage())

        return Delete(
            primaryActionButton = DeletePrimaryButton(::onGotItClicked),
            secondaryActionButton = DeleteSecondaryButton {
                onHelpClicked(source = HelpButtonSource.Delete)
            },
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun initErrorScreenUi(): UiState.Error {
        migrationAnalyticsTracker.trackErrorScreenShown()

        return UiState.Error(
            primaryActionButton = ErrorPrimaryButton(::onTryAgainClicked),
            secondaryActionButton = ErrorSecondaryButton {
                onHelpClicked(source = HelpButtonSource.Error)
            },
            type = Generic,
        )
    }

    fun signOutWordPress(application: WordPress) {
        launch(Dispatchers.IO) {
            application.wordPressComSignOut()
            postActionEvent(FallbackToLogin(deepLinkData))
        }
        dispatchRemoveAllSitesActionIfNeeded()
    }

    private fun dispatchRemoveAllSitesActionIfNeeded() {
        if (!accountStore.hasAccessToken() && siteStore.hasSiteAccessedViaXMLRPC()) {
            dispatcher.dispatch(SiteActionBuilder.newRemoveAllSitesAction())
        }
    }

    private fun siteUiFromModel(site: SiteModel) = SiteListItemUiState(
        id = site.id,
        name = siteUtilsWrapper.getSiteNameOrHomeURL(site),
        url = siteUtilsWrapper.getHomeURLOrHostName(site),
        iconUrl = siteUtilsWrapper.getSiteIconUrlOfResourceSize(
            site,
            R.dimen.jp_migration_site_icon_size,
        ),
    )

    private fun onContinueClicked() {
        migrationAnalyticsTracker.trackWelcomeScreenContinueButtonTapped()
        continueClickedFlow.value = true
    }

    private fun onTryAgainClicked() {
        migrationAnalyticsTracker.trackErrorRetryTapped()
        logoutAndFallbackToLogin()
    }

    fun logoutAndFallbackToLogin() {
        if (accountStore.hasAccessToken()) {
            postActionEvent(Logout)
        } else {
            postActionEvent(FallbackToLogin(deepLinkData))
            dispatchRemoveAllSitesActionIfNeeded()
        }
    }

    private fun tryMigration(application: WordPress) {
        launch(Dispatchers.IO) {
            resetIfNeeded(application)
            appPrefsWrapper.setJetpackMigrationInProgress(true)
            localMigrationOrchestrator.tryLocalMigration(migrationStateFlow)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
            contextProvider.getContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun onContinueFromNotificationsClicked() {
        if (preventDuplicateNotifsFeatureConfig.isEnabled()) disableNotificationsOnWP()
        migrationAnalyticsTracker.trackNotificationsScreenContinueButtonTapped()
        if (hasNotificationPermission()) {
            notificationContinueClickedFlow.value = true
        } else {
            postActionEvent(JetpackMigrationActionEvent.RequestNotificationPermission)
        }
    }

    fun onPermissionChange() {
        notificationContinueClickedFlow.value = true
    }

    private fun disableNotificationsOnWP() {
        AppLog.d(T.NOTIFS, "Disable Notifications")
        Intent().also { intent ->
            intent.action = "org.wordpress.android.broadcast.DISABLE_NOTIFICATIONS"
            val appPackage = BuildConfig.APPLICATION_ID.replace("com.jetpack", "org.wordpress")
            intent.setPackage(appPackage)
            AppLog.d(T.NOTIFS, intent.toString())
            contextProvider.getContext().sendBroadcast(intent)
        }
    }

    private fun onFinishClicked() {
        _refreshAppTheme.value = Unit
        migrationAnalyticsTracker.trackThanksScreenFinishButtonTapped()
        migrationEmailHelper.notifyMigrationComplete()
        appPrefsWrapper.setJetpackMigrationCompleted(true)
        appPrefsWrapper.setJetpackMigrationInProgress(false)
        dispatchFetchAccountActionIfNeeded()
        postActionEvent(CompleteFlow(deepLinkData))
    }

    private fun dispatchFetchAccountActionIfNeeded() {
        // User might have opened the Help screen, in which case their account info is already loaded
        if (accountStore.hasAccessToken() && TextUtils.isEmpty(accountStore.account.userName)) {
            // Load account info to make sure the avatar will always show on My Site screen
            dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
        }
    }

    private fun onHelpClicked(source: HelpButtonSource) {
        when (source) {
            HelpButtonSource.Welcome -> migrationAnalyticsTracker.trackWelcomeScreenHelpButtonTapped()
            HelpButtonSource.WelcomeAvatar -> migrationAnalyticsTracker.trackWelcomeScreenAvatarTapped()
            HelpButtonSource.Error -> migrationAnalyticsTracker.trackErrorHelpTapped()
            HelpButtonSource.Delete -> migrationAnalyticsTracker.trackPleaseDeleteWordPressHelpTapped()
        }
        postActionEvent(ShowHelp)
    }

    private fun onGotItClicked() {
        migrationAnalyticsTracker.trackPleaseDeleteWordPressGotItTapped()
        postActionEvent(FinishActivity)
    }

    private fun resizeAvatarUrl(avatarUrl: String) = gravatarUtilsWrapper.fixGravatarUrlWithResource(
        avatarUrl,
        R.dimen.jp_migration_user_avatar_size
    )

    private fun postActionEvent(actionEvent: JetpackMigrationActionEvent) = launch { _actionEvents.send(actionEvent) }

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
                val onAvatarClicked: () -> Unit,
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
                title = UiStringRes(R.string.jp_migration_notifications_allow_title),
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
            @DrawableRes
            val screenIconRes = R.drawable.ic_jetpack_migration_error

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
        val id: Int,
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

    sealed class HelpButtonSource {
        object Welcome : HelpButtonSource()
        object WelcomeAvatar : HelpButtonSource()
        object Delete : HelpButtonSource()
        object Error : HelpButtonSource()
    }

    sealed class JetpackMigrationActionEvent {
        object ShowHelp : JetpackMigrationActionEvent()

        object RequestNotificationPermission : JetpackMigrationActionEvent()

        data class CompleteFlow(
            val deepLinkData: PreMigrationDeepLinkData? = null,
        ) : JetpackMigrationActionEvent()

        data class FallbackToLogin(
            val deepLinkData: PreMigrationDeepLinkData? = null,
        ) : JetpackMigrationActionEvent()

        object FinishActivity : JetpackMigrationActionEvent()
        object Logout : JetpackMigrationActionEvent()
    }
}
