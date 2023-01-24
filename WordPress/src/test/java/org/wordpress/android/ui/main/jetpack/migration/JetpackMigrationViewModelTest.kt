package org.wordpress.android.ui.main.jetpack.migration

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker
import org.wordpress.android.localcontentmigration.MigrationEmailHelper
import org.wordpress.android.localcontentmigration.WelcomeScreenData
import org.wordpress.android.sharedlogin.resolver.LocalMigrationOrchestrator
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DeletePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DeleteSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DonePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.NotificationsPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomeSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.SiteListItemUiState
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Error
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Loading
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.JetpackMigrationLanguageUtil
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.PreventDuplicateNotifsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import java.util.Locale

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackMigrationViewModelTest : BaseUnitTest() {
    private val refreshAppThemeObserver: Observer<Unit> = mock()
    private val refreshAppLanguageObserver: Observer<String> = mock()
    private val siteUtilsWrapper: SiteUtilsWrapper = mock()
    private val gravatarUtilsWrapper: GravatarUtilsWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val localMigrationOrchestrator: LocalMigrationOrchestrator = mock()
    private val migrationEmailHelper: MigrationEmailHelper = mock()
    private val preventDuplicateNotifsFeatureConfig: PreventDuplicateNotifsFeatureConfig = mock()
    private val contentMigrationAnalyticsTracker: ContentMigrationAnalyticsTracker = mock()
    private val contextProvider: ContextProvider = mock()
    private val accountStore: AccountStore = mock()
    private val siteStore: SiteStore = mock()
    private val localeManagerWrapper: LocaleManagerWrapper = mock()
    private val jetpackMigrationLanguageUtil: JetpackMigrationLanguageUtil = mock()
    private val dispatcher: Dispatcher = mock()

    private lateinit var classToTest: JetpackMigrationViewModel

    @Before
    fun setUp() {
        whenever(gravatarUtilsWrapper.fixGravatarUrlWithResource(any(), any())).thenReturn("")
        whenever(localeManagerWrapper.getLanguage()).thenReturn("")
        classToTest = JetpackMigrationViewModel(
            siteUtilsWrapper = siteUtilsWrapper,
            gravatarUtilsWrapper = gravatarUtilsWrapper,
            contextProvider = contextProvider,
            preventDuplicateNotifsFeatureConfig = preventDuplicateNotifsFeatureConfig,
            appPrefsWrapper = appPrefsWrapper,
            localMigrationOrchestrator = localMigrationOrchestrator,
            migrationEmailHelper = migrationEmailHelper,
            migrationAnalyticsTracker = contentMigrationAnalyticsTracker,
            accountStore = accountStore,
            siteStore = siteStore,
            localeManagerWrapper = localeManagerWrapper,
            jetpackMigrationLanguageUtil = jetpackMigrationLanguageUtil,
            dispatcher = dispatcher,
        )
        classToTest.refreshAppTheme.observeForever(refreshAppThemeObserver)
        classToTest.refreshAppLanguage.observeForever(refreshAppLanguageObserver)
    }

    // region ViewModel
    @Test
    fun `Should init Loading UiState as default`() = test {
        assertThat(classToTest.uiState.first()).isInstanceOf(Loading::class.java)
    }

    @Test
    fun `Should emit event to refresh the language when welcome screen is shown with language data to apply`() {
        val (languagePrefKey, languagePrefValue) = "it" to "language-pref"
        val welcomeScreenData = WelcomeScreenData(flags = mapOf(languagePrefKey to languagePrefValue))
        whenever(localeManagerWrapper.getLocalePrefKeyString()).thenReturn(languagePrefKey)
        whenever(localeManagerWrapper.isSameLanguage(languagePrefValue)).thenReturn(false)

        classToTest.initWelcomeScreenUi(welcomeScreenData, false)

        verify(refreshAppLanguageObserver).onChanged(languagePrefValue)
    }

    @Test
    fun `Should emit event to refresh the language when delete wp app screen is shown and language is not applied`() {
        val language = "it"
        whenever(localeManagerWrapper.getLanguage()).thenReturn(language)
        whenever(localeManagerWrapper.isSameLanguage(language)).thenReturn(false)

        classToTest.initPleaseDeleteWordPressAppScreenUi()

        verify(refreshAppLanguageObserver).onChanged(language)
    }

    @Test
    fun `Should delegate language change to util`() {
        val locale = Locale.US
        classToTest.setAppLanguage(locale)

        verify(jetpackMigrationLanguageUtil).applyLanguage(locale.language)
    }

    @Test
    fun `Should emit refresh app theme when when welcome screen is shown with user flags`() {
        classToTest.initWelcomeScreenUi(WelcomeScreenData(flags = mapOf("theme" to "dark")), false)

        verify(refreshAppThemeObserver).onChanged(Unit)
    }

    @Test
    fun `Should emit refresh app theme when finish button is tapped on success screen`() {
        val successScreen = classToTest.initSuccessScreenUi()

        successScreen.primaryActionButton.onClick.invoke()

        verify(refreshAppThemeObserver).onChanged(Unit)
    }

    @Test
    fun `Should dispatch remove all sites action when signing out from site address`() {
        whenever(siteStore.hasSiteAccessedViaXMLRPC()).thenReturn(true)

        classToTest.signOutWordPress(mock())

        verify(dispatcher).dispatch(argThat { type == SiteAction.REMOVE_ALL_SITES })
    }

    @Test
    fun `Should not dispatch any action when signing out from wpcom`() {
        classToTest.signOutWordPress(mock())

        verifyNoInteractions(dispatcher)
    }

    @Test
    fun `Should dispatch remove all sites action when logging out from site address`() {
        whenever(siteStore.hasSiteAccessedViaXMLRPC()).thenReturn(true)

        classToTest.logoutAndFallbackToLogin()

        verify(dispatcher).dispatch(argThat { type == SiteAction.REMOVE_ALL_SITES })
    }

    @Test
    fun `Should not dispatch any action when logging out from wpcom`() {
        classToTest.logoutAndFallbackToLogin()

        verifyNoInteractions(dispatcher)
    }

    // endregion

    // region Analytics Tracking
    @Test
    fun `Should track when welcome screen is shown`() {
        classToTest.initWelcomeScreenUi(WelcomeScreenData(), false)

        verify(contentMigrationAnalyticsTracker).trackWelcomeScreenShown()
    }

    @Test
    fun `Should track when continue button is tapped on welcome screen`() {
        val welcomeScreen = classToTest.initWelcomeScreenUi(WelcomeScreenData(), false)

        welcomeScreen.primaryActionButton.onClick.invoke()

        verify(contentMigrationAnalyticsTracker).trackWelcomeScreenContinueButtonTapped()
    }

    @Test
    fun `Should track when help button is tapped on welcome screen`() {
        val welcomeScreen = classToTest.initWelcomeScreenUi(WelcomeScreenData(), false)

        welcomeScreen.secondaryActionButton.onClick.invoke()

        verify(contentMigrationAnalyticsTracker).trackWelcomeScreenHelpButtonTapped()
    }

    @Test
    fun `Should track when avatar is tapped on welcome screen`() {
        val welcomeScreen = classToTest.initWelcomeScreenUi(WelcomeScreenData(), false)

        welcomeScreen.onAvatarClicked.invoke()

        verify(contentMigrationAnalyticsTracker).trackWelcomeScreenAvatarTapped()
    }

    @Test
    fun `Should track when notifications screen is shown`() {
        classToTest.initNotificationsScreenUi()

        verify(contentMigrationAnalyticsTracker).trackNotificationsScreenShown()
    }

    @Test
    fun `Should track when continue button is tapped on notifications screen`() {
        val notificationsScreen = classToTest.initNotificationsScreenUi()

        notificationsScreen.primaryActionButton.onClick.invoke()

        verify(contentMigrationAnalyticsTracker).trackNotificationsScreenShown()
    }

    @Test
    fun `Should track when success screen is shown`() {
        classToTest.initSuccessScreenUi()

        verify(contentMigrationAnalyticsTracker).trackThanksScreenShown()
    }

    @Test
    fun `Should track when finish button is tapped on success screen`() {
        val successScreen = classToTest.initSuccessScreenUi()

        successScreen.primaryActionButton.onClick.invoke()

        verify(contentMigrationAnalyticsTracker).trackThanksScreenFinishButtonTapped()
    }

    @Test
    fun `Should track when delete wp app screen is shown`() {
        classToTest.initPleaseDeleteWordPressAppScreenUi()

        verify(contentMigrationAnalyticsTracker).trackPleaseDeleteWordPressScreenShown()
    }

    @Test
    fun `Should track when got it button is tapped on delete wp app screen`() {
        val pleaseDeleteScreen = classToTest.initPleaseDeleteWordPressAppScreenUi()

        pleaseDeleteScreen.primaryActionButton.onClick.invoke()

        verify(contentMigrationAnalyticsTracker).trackPleaseDeleteWordPressGotItTapped()
    }

    @Test
    fun `Should track when help button is tapped on delete wp app screen`() {
        val pleaseDeleteScreen = classToTest.initPleaseDeleteWordPressAppScreenUi()

        pleaseDeleteScreen.secondaryActionButton.onClick.invoke()

        verify(contentMigrationAnalyticsTracker).trackPleaseDeleteWordPressHelpTapped()
    }

    @Test
    fun `Should track when error screen is shown`() {
        classToTest.initErrorScreenUi()

        verify(contentMigrationAnalyticsTracker).trackErrorScreenShown()
    }

    @Test
    fun `Should track when retry button is tapped on error screen`() {
        val errorScreen = classToTest.initErrorScreenUi()

        errorScreen.primaryActionButton.onClick.invoke()

        verify(contentMigrationAnalyticsTracker).trackErrorRetryTapped()
    }

    @Test
    fun `Should track when help button is tapped on error screen`() {
        val errorScreen = classToTest.initErrorScreenUi()

        errorScreen.secondaryActionButton.onClick.invoke()

        verify(contentMigrationAnalyticsTracker).trackErrorHelpTapped()
    }
    // endregion

    // region UiState Content
    @Test
    fun `Should have correct default userAvatarUrl for Welcome Content`() {
        val welcomeContent = Content.Welcome(
            sites = emptyList(),
            primaryActionButton = WelcomePrimaryButton {},
            secondaryActionButton = WelcomeSecondaryButton {},
            onAvatarClicked = {},
        )
        val actual = welcomeContent.userAvatarUrl
        val expected = ""
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct default isProcessing for Welcome Content`() {
        val welcomeContent = Content.Welcome(
            sites = emptyList(),
            primaryActionButton = WelcomePrimaryButton {},
            secondaryActionButton = WelcomeSecondaryButton {},
            onAvatarClicked = {},
        )
        val actual = welcomeContent.isProcessing
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct screenIconRes for Welcome Content`() {
        val welcomeContent = Content.Welcome(
            sites = emptyList(),
            primaryActionButton = WelcomePrimaryButton {},
            secondaryActionButton = WelcomeSecondaryButton {},
            onAvatarClicked = {},
        )
        val actual = welcomeContent.screenIconRes
        val expected = R.drawable.ic_wordpress_jetpack_logo
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct title for Welcome Content`() {
        val welcomeContent = Content.Welcome(
            sites = emptyList(),
            primaryActionButton = WelcomePrimaryButton {},
            secondaryActionButton = WelcomeSecondaryButton {},
            onAvatarClicked = {},
        )
        val actual = welcomeContent.title
        val expected = UiStringRes(R.string.jp_migration_welcome_title)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct subtitle for Welcome Content`() {
        val welcomeContent = Content.Welcome(
            sites = emptyList(),
            primaryActionButton = WelcomePrimaryButton {},
            secondaryActionButton = WelcomeSecondaryButton {},
            onAvatarClicked = {},
        )
        val actual = welcomeContent.subtitle
        val expected = UiStringRes(R.string.jp_migration_welcome_subtitle)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Welcome Content when sites size IS GREATER than 1`() {
        val welcomeContent = Content.Welcome(
            sites = listOf(
                SiteListItemUiState(123, "name", "url", "iconUrl"),
                SiteListItemUiState(456, "name", "url", "iconUrl")
            ),
            primaryActionButton = WelcomePrimaryButton {},
            secondaryActionButton = WelcomeSecondaryButton {},
            onAvatarClicked = {},
        )
        val actual = welcomeContent.message
        val expected = UiStringRes(R.string.jp_migration_welcome_sites_found_message)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Welcome Content when sites size IS NOT GREATER than 1`() {
        val welcomeContent = Content.Welcome(
            sites = listOf(SiteListItemUiState(123, "name", "url", "iconUrl")),
            primaryActionButton = WelcomePrimaryButton {},
            secondaryActionButton = WelcomeSecondaryButton {},
            onAvatarClicked = {},
        )
        val actual = welcomeContent.message
        val expected = UiStringRes(R.string.jp_migration_welcome_site_found_message)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct screenIconRes for Notifications Content`() {
        val notificationsContent = Content.Notifications(NotificationsPrimaryButton {})
        val actual = notificationsContent.screenIconRes
        val expected = R.drawable.ic_jetpack_migration_notifications
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct title for Notifications Content`() {
        val notificationsContent = Content.Notifications(NotificationsPrimaryButton {})
        val actual = notificationsContent.title
        val expected = UiStringRes(R.string.jp_migration_notifications_title)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct subtitle for Notifications Content`() {
        val notificationsContent = Content.Notifications(NotificationsPrimaryButton {})
        val actual = notificationsContent.subtitle
        val expected = UiStringRes(R.string.jp_migration_notifications_subtitle)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Notifications Content`() {
        val notificationsContent = Content.Notifications(NotificationsPrimaryButton {})
        val actual = notificationsContent.message
        val expected = UiStringRes(R.string.jp_migration_notifications_disabled_in_wp_message)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct screenIconRes for Done Content`() {
        val doneContent = Content.Done(DonePrimaryButton {})
        val actual = doneContent.screenIconRes
        val expected = R.drawable.ic_jetpack_migration_success
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct title for Done Content`() {
        val doneContent = Content.Done(DonePrimaryButton {})
        val actual = doneContent.title
        val expected = UiStringRes(R.string.jp_migration_done_title)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct subtitle for Done Content`() {
        val doneContent = Content.Done(DonePrimaryButton {})
        val actual = doneContent.subtitle
        val expected = UiStringRes(R.string.jp_migration_done_subtitle)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Done Content`() {
        val doneContent = Content.Done(DonePrimaryButton {})
        val actual = doneContent.message
        val expected = UiStringRes(R.string.jp_migration_done_delete_wp_message)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct deleteWpIcon for Done Content`() {
        val doneContent = Content.Done(DonePrimaryButton {})
        val actual = doneContent.deleteWpIcon
        val expected = R.drawable.ic_jetpack_migration_delete_wp
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct screenIconRes for Delete Content`() {
        val deleteContent = Content.Delete(DeletePrimaryButton {}, DeleteSecondaryButton {})
        val actual = deleteContent.screenIconRes
        val expected = R.drawable.ic_jetpack_migration_delete
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct title for Delete Content`() {
        val deleteContent = Content.Delete(DeletePrimaryButton {}, DeleteSecondaryButton {})
        val actual = deleteContent.title
        val expected = UiStringRes(R.string.jp_migration_delete_title)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct subtitle for Delete Content`() {
        val deleteContent = Content.Delete(DeletePrimaryButton {}, DeleteSecondaryButton {})
        val actual = deleteContent.subtitle
        val expected = UiStringRes(R.string.jp_migration_delete_subtitle)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Delete Content`() {
        val deleteContent = Content.Delete(DeletePrimaryButton {}, DeleteSecondaryButton {})
        val actual = deleteContent.message
        val expected = UiStringRes(R.string.jp_migration_delete_message)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct deleteWpIcon for Delete Content`() {
        val deleteContent = Content.Delete(DeletePrimaryButton {}, DeleteSecondaryButton {})
        val actual = deleteContent.deleteWpIcon
        val expected = R.drawable.ic_jetpack_migration_delete_wp
        assertThat(actual).isEqualTo(expected)
    }
    // endregion

    // region UiState Error
    @Test
    fun `Should have correct default isProcessing for Error UiState`() {
        val uiStateError = Error(
            primaryActionButton = ErrorPrimaryButton {},
            secondaryActionButton = ErrorSecondaryButton {},
            type = Error.Networking,
        )
        val actual = uiStateError.isProcessing
        val expected = false
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct screenIconRes for Error UiState`() {
        val uiStateError = Error(
            primaryActionButton = ErrorPrimaryButton {},
            secondaryActionButton = ErrorSecondaryButton {},
            type = Error.Networking,
        )
        val actual = uiStateError.screenIconRes
        val expected = R.drawable.ic_jetpack_migration_error
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct title for Generic ErrorType`() {
        val genericErrorType = Error.Generic
        val actual = genericErrorType.title
        val expected = UiStringRes(R.string.jp_migration_generic_error_title)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct subtitle for Generic ErrorType`() {
        val genericErrorType = Error.Generic
        val actual = genericErrorType.subtitle
        val expected = UiStringRes(R.string.jp_migration_generic_error_subtitle)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Generic ErrorType`() {
        val genericErrorType = Error.Generic
        val actual = genericErrorType.message
        val expected = UiStringRes(R.string.jp_migration_generic_error_message)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct title for Networking ErrorType`() {
        val networkingErrorType = Error.Networking
        val actual = networkingErrorType.title
        val expected = UiStringRes(R.string.jp_migration_network_error_title)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct subtitle for Networking ErrorType`() {
        val networkingErrorType = Error.Networking
        val actual = networkingErrorType.subtitle
        val expected = UiStringRes(R.string.jp_migration_network_error_subtitle)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Networking ErrorType`() {
        val networkingErrorType = Error.Networking
        val actual = networkingErrorType.message
        val expected = UiStringRes(R.string.jp_migration_network_error_message)
        assertThat(actual).isEqualTo(expected)
    }
    // endregion

    // region ActionButton
    @Test
    fun `Should have correct text for WelcomePrimaryButton`() {
        val button = WelcomePrimaryButton {}
        val actual = button.text
        val expected = UiStringRes(R.string.jp_migration_continue_button)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct text for WelcomeSecondaryButton`() {
        val button = WelcomeSecondaryButton {}
        val actual = button.text
        val expected = UiStringRes(R.string.jp_migration_help_button)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct text for NotificationsPrimaryButton`() {
        val button = NotificationsPrimaryButton {}
        val actual = button.text
        val expected = UiStringRes(R.string.jp_migration_continue_button)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct text for DonePrimaryButton`() {
        val button = DonePrimaryButton {}
        val actual = button.text
        val expected = UiStringRes(R.string.jp_migration_finish_button)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct text for ErrorPrimaryButton`() {
        val button = ErrorPrimaryButton {}
        val actual = button.text
        val expected = UiStringRes(R.string.jp_migration_try_again_button)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct text for ErrorSecondaryButton`() {
        val button = ErrorSecondaryButton {}
        val actual = button.text
        val expected = UiStringRes(R.string.jp_migration_help_button)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct text for DeletePrimaryButton`() {
        val button = DeletePrimaryButton {}
        val actual = button.text
        val expected = UiStringRes(R.string.jp_migration_got_it_button)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct text for DeleteSecondaryButton`() {
        val button = DeleteSecondaryButton {}
        val actual = button.text
        val expected = UiStringRes(R.string.jp_migration_need_help_button)
        assertThat(actual).isEqualTo(expected)
    }
    // endregion
}
