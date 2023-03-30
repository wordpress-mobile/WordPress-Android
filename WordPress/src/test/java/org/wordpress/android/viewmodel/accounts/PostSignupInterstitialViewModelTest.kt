package org.wordpress.android.viewmodel.accounts

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_CREATE_NEW_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_SHOWN
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.DISMISS
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CONNECTION_FLOW
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CREATION_FLOW

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PostSignupInterstitialViewModelTest : BaseUnitTest() {
    private val appPrefs: AppPrefsWrapper = mock()
    private val unifiedLoginTracker: UnifiedLoginTracker = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper = mock()
    private val observer: Observer<NavigationAction> = mock()

    private lateinit var viewModel: PostSignupInterstitialViewModel

    @Before
    fun setUp() {
        viewModel = PostSignupInterstitialViewModel(
            appPrefs,
            unifiedLoginTracker,
            analyticsTracker,
            wpJetpackIndividualPluginHelper
        )
        viewModel.navigationAction.observeForever(observer)
    }

    @Test
    fun `when interstitial is shown should update preference value`() {
        viewModel.onInterstitialShown()

        verify(analyticsTracker).track(WELCOME_NO_SITES_INTERSTITIAL_SHOWN)
        verify(appPrefs).shouldShowPostSignupInterstitial = false
    }

    @Test
    fun `given overlay should show when interstitial is shown then show jetpack individual plugin overlay`() = test {
        whenever(wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()).thenReturn(true)

        viewModel.onInterstitialShown()
        advanceUntilIdle()

        assertThat(viewModel.navigationAction.value).isEqualTo(NavigationAction.SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY)
    }

    @Test
    fun `given overlay should not show when interstitial is shown then don't show jetpack individual plugin overlay`() =
        test {
            whenever(wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()).thenReturn(false)

            viewModel.onInterstitialShown()
            advanceUntilIdle()

            assertThat(viewModel.navigationAction.value).isNull()
        }

    @Test
    fun `when create new site button is pressed should start site creation flow`() {
        viewModel.onCreateNewSiteButtonPressed()

        verify(analyticsTracker).track(WELCOME_NO_SITES_INTERSTITIAL_CREATE_NEW_SITE_TAPPED)
        verify(observer).onChanged(START_SITE_CREATION_FLOW)
    }

    @Test
    fun `when add self hosted site button is pressed should start site connection flow`() {
        viewModel.onAddSelfHostedSiteButtonPressed()

        verify(analyticsTracker).track(WELCOME_NO_SITES_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED)
        verify(observer).onChanged(START_SITE_CONNECTION_FLOW)
    }

    @Test
    fun `when dismissal button is pressed should dismiss`() {
        viewModel.onDismissButtonPressed()

        verify(analyticsTracker).track(WELCOME_NO_SITES_INTERSTITIAL_DISMISSED)
        verify(observer).onChanged(DISMISS)
    }

    @Test
    fun `when back button is pressed should dismiss`() {
        viewModel.onBackButtonPressed()

        verify(analyticsTracker).track(WELCOME_NO_SITES_INTERSTITIAL_DISMISSED)
        verify(observer).onChanged(DISMISS)
    }
}
