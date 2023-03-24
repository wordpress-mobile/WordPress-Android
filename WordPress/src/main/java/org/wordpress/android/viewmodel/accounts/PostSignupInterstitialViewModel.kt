package org.wordpress.android.viewmodel.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_CREATE_NEW_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.WELCOME_NO_SITES_INTERSTITIAL_SHOWN
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step.SUCCESS
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.DISMISS
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CONNECTION_FLOW
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CREATION_FLOW
import javax.inject.Inject

class PostSignupInterstitialViewModel
@Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val unifiedLoginTracker: UnifiedLoginTracker,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper,
) : ViewModel() {
    val navigationAction: SingleLiveEvent<NavigationAction> = SingleLiveEvent()

    fun onInterstitialShown() {
        analyticsTracker.track(WELCOME_NO_SITES_INTERSTITIAL_SHOWN)
        unifiedLoginTracker.track(step = SUCCESS)
        appPrefs.shouldShowPostSignupInterstitial = false
        checkJetpackIndividualPluginOverlayShouldShow()
    }

    fun onCreateNewSiteButtonPressed() {
        analyticsTracker.track(WELCOME_NO_SITES_INTERSTITIAL_CREATE_NEW_SITE_TAPPED)
        unifiedLoginTracker.trackClick(Click.CREATE_NEW_SITE)
        navigationAction.value = START_SITE_CREATION_FLOW
    }

    fun onAddSelfHostedSiteButtonPressed() {
        analyticsTracker.track(WELCOME_NO_SITES_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED)
        unifiedLoginTracker.trackClick(Click.ADD_SELF_HOSTED_SITE)
        navigationAction.value = START_SITE_CONNECTION_FLOW
    }

    fun onDismissButtonPressed() = onDismiss()

    fun onBackButtonPressed() = onDismiss()

    private fun onDismiss() {
        unifiedLoginTracker.trackClick(Click.DISMISS)
        analyticsTracker.track(WELCOME_NO_SITES_INTERSTITIAL_DISMISSED)
        navigationAction.value = DISMISS
    }

    private fun checkJetpackIndividualPluginOverlayShouldShow() {
        // don't check if already shown
        if (navigationAction.value == SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY) return

        viewModelScope.launch {
            val showOverlay = wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()
            if (showOverlay) {
                delay(DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY)
                navigationAction.postValue(SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY)
            }
        }
    }

    enum class NavigationAction {
        START_SITE_CREATION_FLOW,
        START_SITE_CONNECTION_FLOW,
        DISMISS,
        SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY
    }

    companion object {
        private const val DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY = 500L
    }
}
