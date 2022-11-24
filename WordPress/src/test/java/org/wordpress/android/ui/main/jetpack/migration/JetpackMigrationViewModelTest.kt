package org.wordpress.android.ui.main.jetpack.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DeletePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DonePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.NotificationsPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomeSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.SiteListItemUiState
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Error
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Loading
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper

class JetpackMigrationViewModelTest {
    private val siteStore: SiteStore = mock()
    private val accountStore: AccountStore = mock()
    private val siteUtilsWrapper: SiteUtilsWrapper = mock()
    private val gravatarUtilsWrapper: GravatarUtilsWrapper = mock()
    private val classToTest = JetpackMigrationViewModel(
            siteStore = siteStore,
            accountStore = accountStore,
            siteUtilsWrapper = siteUtilsWrapper,
            gravatarUtilsWrapper = gravatarUtilsWrapper,
    )

    // region ViewModel
    @Test
    fun `Should init Loading UiState as default`() {
        assertThat(classToTest.uiState.value).isInstanceOf(Loading::class.java)
    }
    
    @Test
    fun `Should post Delete Content if checkDeleteState is called with showDeleteState TRUE`() {
        classToTest.checkDeleteState(true)
        assertThat(classToTest.uiState.value).isInstanceOf(Content.Delete::class.java)
    }

    @Test
    fun `Should NOT post Delete Content if checkDeleteState is called with showDeleteState FALSE`() {
        classToTest.checkDeleteState(false)
        assertThat(classToTest.uiState.value).isNotInstanceOf(Content.Delete::class.java)
    }

    @Test
    fun `Should init Welcome Content from onAccountInfoLoaded if isDataAvailable is TRUE`() {
        whenever(gravatarUtilsWrapper.fixGravatarUrlWithResource(any(), any())).thenReturn("imageUrl")
        setDataAvailable(true)
        classToTest.onAccountInfoLoaded()
        assertThat(classToTest.uiState.value).isInstanceOf(Content.Welcome::class.java)
    }

    @Test
    fun `Should NOT init Welcome Content from onAccountInfoLoaded if isDataAvailable is FALSE`() {
        setDataAvailable(false)
        classToTest.onAccountInfoLoaded()
        assertThat(classToTest.uiState.value).isNotInstanceOf(Content.Welcome::class.java)
    }

    @Test
    fun `Should NOT init Welcome Content from onAccountInfoLoaded if showDeleteState is TRUE`() {
        setDataAvailable(true)
        classToTest.checkDeleteState(true)
        classToTest.onAccountInfoLoaded()
        assertThat(classToTest.uiState.value).isNotInstanceOf(Content.Welcome::class.java)
    }

    @Test
    fun `Should fix Gravatar URL on Welcome Content init from onAccountInfoLoaded if data IS available`() {
        whenever(gravatarUtilsWrapper.fixGravatarUrlWithResource(any(), any())).thenReturn("imageUrl")
        setDataAvailable(true)
        classToTest.onAccountInfoLoaded()
        verify(gravatarUtilsWrapper).fixGravatarUrlWithResource(any(), any())
    }

    @Test
    fun `Should NOT fix Gravatar URL on Welcome Content init from onAccountInfoLoaded if data IS NOT available`() {
        setDataAvailable(false)
        classToTest.onAccountInfoLoaded()
        verify(gravatarUtilsWrapper, times(0)).fixGravatarUrlWithResource(any(), any())
    }

    @Test
    fun `Should get site list on Welcome Content init from onAccountInfoLoaded if data IS available`() {
        setDataAvailable(true)
        classToTest.onAccountInfoLoaded()
        // Called two times: isDataAvailable and initWelcomeState
        verify(siteStore, times(2)).sites
    }

    @Test
    fun `Should NOT get site list on Welcome Content init from onAccountInfoLoaded if data IS NOT available`() {
        setDataAvailable(false)
        classToTest.onAccountInfoLoaded()
        verify(siteStore, times(0)).sites
    }

    @Test
    fun `Should init Welcome Content from onSiteListLoaded if isDataAvailable is TRUE`() {
        setDataAvailable(true)
        classToTest.onSiteListLoaded()
        assertThat(classToTest.uiState.value).isInstanceOf(Content.Welcome::class.java)
    }

    @Test
    fun `Should NOT init Welcome Content from onSiteListLoaded if isDataAvailable is FALSE`() {
        setDataAvailable(false)
        classToTest.onSiteListLoaded()
        assertThat(classToTest.uiState.value).isNotInstanceOf(Content.Welcome::class.java)
    }

    @Test
    fun `Should NOT init Welcome Content from onSiteListLoaded if showDeleteState is TRUE`() {
        setDataAvailable(true)
        classToTest.checkDeleteState(true)
        classToTest.onSiteListLoaded()
        assertThat(classToTest.uiState.value).isNotInstanceOf(Content.Welcome::class.java)
    }

    @Test
    fun `Should fix Gravatar URL on Welcome Content init from onSiteListLoaded if data IS available`() {
        setDataAvailable(true)
        classToTest.onSiteListLoaded()
        verify(gravatarUtilsWrapper).fixGravatarUrlWithResource(any(), any())
    }

    @Test
    fun `Should NOT fix Gravatar URL on Welcome Content init from onSiteListLoaded if data IS NOT available`() {
        setDataAvailable(false)
        classToTest.onSiteListLoaded()
        verify(gravatarUtilsWrapper, times(0)).fixGravatarUrlWithResource(any(), any())
    }

    @Test
    fun `Should get site list on Welcome Content init from onSiteListLoaded if data IS available`() {
        setDataAvailable(true)
        classToTest.onSiteListLoaded()
        // Called two times: isDataAvailable and initWelcomeState
        verify(siteStore, times(2)).sites
    }

    @Test
    fun `Should NOT get site list on Welcome Content init from onSiteListLoaded if data IS NOT available`() {
        setDataAvailable(false)
        classToTest.onSiteListLoaded()
        verify(siteStore, times(0)).sites
    }
    // endregion

    // region UiState Content
    @Test
    fun `Should have correct default userAvatarUrl for Welcome Content`() {
        val welcomeContent = Content.Welcome(
                sites = emptyList(),
                primaryActionButton = WelcomePrimaryButton {},
                secondaryActionButton = WelcomeSecondaryButton {},
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
        )
        val actual = welcomeContent.subtitle
        val expected = UiStringRes(R.string.jp_migration_welcome_subtitle)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Welcome Content when sites size IS GREATER than 1`() {
        val welcomeContent = Content.Welcome(
                sites = listOf(
                        SiteListItemUiState(123L, "name", "url", "iconUrl"),
                        SiteListItemUiState(456L, "name", "url", "iconUrl")
                ),
                primaryActionButton = WelcomePrimaryButton {},
                secondaryActionButton = WelcomeSecondaryButton {},
        )
        val actual = welcomeContent.message
        val expected = UiStringRes(R.string.jp_migration_welcome_sites_found_message)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Welcome Content when sites size IS NOT GREATER than 1`() {
        val welcomeContent = Content.Welcome(
                sites = listOf(SiteListItemUiState(123L, "name", "url", "iconUrl")),
                primaryActionButton = WelcomePrimaryButton {},
                secondaryActionButton = WelcomeSecondaryButton {},
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
        val deleteContent = Content.Delete(DeletePrimaryButton {})
        val actual = deleteContent.screenIconRes
        val expected = R.drawable.ic_jetpack_migration_delete
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct title for Delete Content`() {
        val deleteContent = Content.Delete(DeletePrimaryButton {})
        val actual = deleteContent.title
        val expected = UiStringRes(R.string.jp_migration_delete_title)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct subtitle for Delete Content`() {
        val deleteContent = Content.Delete(DeletePrimaryButton {})
        val actual = deleteContent.subtitle
        val expected = UiStringRes(R.string.jp_migration_delete_subtitle)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct message for Delete Content`() {
        val deleteContent = Content.Delete(DeletePrimaryButton {})
        val actual = deleteContent.message
        val expected = UiStringRes(R.string.jp_migration_delete_message)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should have correct deleteWpIcon for Delete Content`() {
        val deleteContent = Content.Delete(DeletePrimaryButton {})
        val actual = deleteContent.deleteWpIcon
        val expected = R.drawable.ic_jetpack_migration_delete_wp
        assertThat(actual).isEqualTo(expected)
    }
    // endregion

    // region UiState Error
    @Test
    fun `Should have correct default isProcessing for Error UiState`() {
        val uiStateError = UiState.Error(
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
        val uiStateError = UiState.Error(
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
    // endregion

    private fun setDataAvailable(isAvailable: Boolean) {
        setAccountUsername(isAvailable)
        setSites(isAvailable)
        if (isAvailable) {
            whenever(gravatarUtilsWrapper.fixGravatarUrlWithResource(any(), any())).thenReturn("imageUrl")
        }
    }

    private fun setAccountUsername(isAvailable: Boolean) {
        val username = if (isAvailable) "username" else ""
        val accountModel: AccountModel = mock()
        whenever(accountStore.account).thenReturn(accountModel)
        whenever(accountModel.userName).thenReturn(username)
    }

    private fun setSites(isAvailable: Boolean) {
        val siteModel = SiteModel()
        whenever(siteUtilsWrapper.getSiteNameOrHomeURL(siteModel)).thenReturn("name")
        whenever(siteUtilsWrapper.getHomeURLOrHostName(siteModel)).thenReturn("url")
        whenever(siteUtilsWrapper.getSiteIconUrlOfResourceSize(siteModel, R.dimen.jp_migration_site_icon_size))
                .thenReturn("iconUrl")
        val sites: List<SiteModel> = if (isAvailable) listOf(siteModel) else emptyList()
        whenever(siteStore.sites).thenReturn(sites)
    }
}
