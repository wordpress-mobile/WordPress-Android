package org.wordpress.android.viewmodel.accounts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_CREATE_NEW_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_SHOWN
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.DISMISS
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CONNECTION_FLOW
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CREATION_FLOW

@RunWith(MockitoJUnitRunner::class)
class PostSignupInterstitialViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    private val appPrefs: AppPrefsWrapper = mock()
    private val unifiedLoginTracker: UnifiedLoginTracker = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val observer: Observer<NavigationAction> = mock()

    private lateinit var viewModel: PostSignupInterstitialViewModel

    @Before
    fun setUp() {
        viewModel = PostSignupInterstitialViewModel(appPrefs, unifiedLoginTracker, analyticsTracker)
        viewModel.navigationAction.observeForever(observer)
    }

    @Test
    fun `when interstitial is shown should update preference value`() {
        viewModel.onInterstitialShown()

        verify(analyticsTracker).track(WELCOME_NO_SITES_INTERSTITIAL_SHOWN)
        verify(appPrefs).shouldShowPostSignupInterstitial = false
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
