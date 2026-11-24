package org.wordpress.android.ui.prefs.appsettings

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.AppSettingsActivity.Companion.EXTRA_SHOW_PRIVACY_SETTINGS
import org.wordpress.android.ui.whatsnew.FeatureAnnouncement
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class AppSettingsViewModelTest : BaseUnitTest() {
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var sharedPreferences: SharedPreferences
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var featureAnnouncementProvider: FeatureAnnouncementProvider
    @Mock lateinit var openWebLinksWithJetpackHelper: DeepLinkOpenWebLinksWithJetpackHelper
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var contextProvider: ContextProvider

    private lateinit var viewModel: AppSettingsViewModel

    @Before
    fun setUp() {
        whenever(resourceProvider.getString(any())).thenAnswer { key(it.arguments[0] as Int) }
        // Return the caller's default, as SharedPreferences does for a missing key
        whenever(sharedPreferences.getBoolean(any(), any())).thenAnswer { it.arguments[1] }
        whenever(appPrefsWrapper.imageOptimizeMaxSize).thenReturn(IMAGE_MAX_SIZE)
        whenever(appPrefsWrapper.videoOptimizeQuality).thenReturn(VIDEO_QUALITY)
        viewModel = createViewModel()
    }

    @Test
    fun `initial state reads the stored settings`() {
        val state = viewModel.uiState.value

        assertThat(state.themeValue).isEqualTo(key(R.string.app_theme_entry_value_default))
        assertThat(state.mediaValues[MediaChoice.IMAGE_MAX_SIZE]).isEqualTo(IMAGE_MAX_SIZE)
        assertThat(state.mediaValues[MediaChoice.VIDEO_QUALITY]).isEqualTo(VIDEO_QUALITY)
        assertThat(state.isSendUsageStats).isTrue
        assertThat(state.isSendCrashReports).isTrue
        assertThat(state.isPrivacySettingsShown).isFalse
        assertThat(state.whatsNewVersion).isNull()
    }

    @Test
    fun `usage switch reflects the account's opt-out when logged in`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.account).thenReturn(AccountModel().apply { tracksOptOut = true })

        viewModel = createViewModel()

        assertThat(viewModel.uiState.value.isSendUsageStats).isFalse
    }

    @Test
    fun `privacy settings extra opens privacy settings and tracks it once`() {
        val savedStateHandle = SavedStateHandle(mapOf(EXTRA_SHOW_PRIVACY_SETTINGS to true))
        viewModel = createViewModel(savedStateHandle)
        // Simulates restoring the saved state after process death
        viewModel = createViewModel(savedStateHandle)

        assertThat(viewModel.uiState.value.isPrivacySettingsShown).isTrue
        verify(analyticsTrackerWrapper, times(1)).track(Stat.PRIVACY_SETTINGS_OPENED)
    }

    @Test
    fun `what's new shows the cached version, then the remote one`() = test {
        whenever(buildConfigWrapper.isWhatsNewFeatureEnabled).thenReturn(true)
        val cached = announcement("27.8")
        val remote = announcement("27.9")
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(true)).thenReturn(cached)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(false)).thenReturn(remote)

        viewModel = createViewModel()

        assertThat(viewModel.uiState.value.whatsNewVersion).isEqualTo("27.9")
    }

    @Test
    fun `selecting a theme stores it, tracks it and applies it`() = test {
        val editor = stubEditor()

        viewModel.onThemeSelected("dark")

        verify(editor).putString(key(R.string.pref_key_app_theme), "dark")
        verify(analyticsTrackerWrapper).track(Stat.APP_SETTINGS_APPEARANCE_CHANGED, mapOf("style" to "dark"))
        assertThat(viewModel.uiState.value.themeValue).isEqualTo("dark")
        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(AppSettingsEvent.ApplyTheme("dark"))
        }
    }

    @Test
    fun `a failed open links in Jetpack change leaves the switch alone and shows an error`() = test {
        whenever(openWebLinksWithJetpackHelper.disableDeepLinks()).thenThrow(IllegalStateException())

        viewModel.onOpenLinksInJetpackChanged(true)

        verify(appPrefsWrapper, never()).setIsOpenWebLinksWithJetpack(any())
        assertThat(viewModel.uiState.value.isOpenLinksInJetpack).isFalse
        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(
                AppSettingsEvent.ShowToast(R.string.preference_open_links_in_jetpack_setting_change_enable_error)
            )
        }
    }

    @Test
    fun `toggling crash reports stores it and tracks it`() {
        val editor = stubEditor()

        viewModel.onSendCrashReportsChanged(false)

        verify(editor).putBoolean(key(R.string.pref_key_send_crash), false)
        verify(analyticsTrackerWrapper).track(Stat.PRIVACY_SETTINGS_REPORT_CRASHES_TOGGLED, mapOf("enabled" to false))
        assertThat(viewModel.uiState.value.isSendCrashReports).isFalse
    }

    @Test
    fun `fetched account settings update the usage switch`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(accountStore.account).thenReturn(AccountModel().apply { tracksOptOut = true })

        viewModel.onAccountChanged(OnAccountChanged().apply { causeOfChange = AccountAction.FETCH_SETTINGS })

        assertThat(viewModel.uiState.value.isSendUsageStats).isFalse
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = AppSettingsViewModel(
        savedStateHandle = savedStateHandle,
        appPrefsWrapper = appPrefsWrapper,
        sharedPreferences = sharedPreferences,
        analyticsTrackerWrapper = analyticsTrackerWrapper,
        buildConfigWrapper = buildConfigWrapper,
        dispatcher = dispatcher,
        accountStore = accountStore,
        networkUtilsWrapper = networkUtilsWrapper,
        featureAnnouncementProvider = featureAnnouncementProvider,
        openWebLinksWithJetpackHelper = openWebLinksWithJetpackHelper,
        resourceProvider = resourceProvider,
        contextProvider = contextProvider,
    )

    private fun stubEditor(): SharedPreferences.Editor {
        val editor = mock<SharedPreferences.Editor>()
        whenever(sharedPreferences.edit()).thenReturn(editor)
        return editor
    }

    private fun announcement(version: String) = FeatureAnnouncement(
        appVersionName = version,
        announcementVersion = 1,
        minimumAppVersion = version,
        maximumAppVersion = version,
        appVersionTargets = emptyList(),
        detailsUrl = null,
        features = emptyList(),
    )

    private fun key(resId: Int) = "key_$resId"

    private companion object {
        const val IMAGE_MAX_SIZE = 2000
        const val VIDEO_QUALITY = 3
    }
}
