package org.wordpress.android.viewmodel.accounts

import androidx.lifecycle.ViewModel
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_SIGNUP_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_SIGNUP_INTERSTITIAL_CREATE_NEW_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_SIGNUP_INTERSTITIAL_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_SIGNUP_INTERSTITIAL_SHOWN
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.DISMISS
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CONNECTION_FLOW
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CREATION_FLOW
import javax.inject.Inject

class PostSignupInterstitialViewModel
@Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {
    val navigationAction: SingleLiveEvent<NavigationAction> = SingleLiveEvent()

    fun onInterstitialShown() {
        analyticsTracker.track(POST_SIGNUP_INTERSTITIAL_SHOWN)
        appPrefs.shouldShowPostSignupInterstitial = false
    }

    fun onCreateNewSiteButtonPressed() {
        analyticsTracker.track(POST_SIGNUP_INTERSTITIAL_CREATE_NEW_SITE_TAPPED)
        navigationAction.value = START_SITE_CREATION_FLOW
    }

    fun onAddSelfHostedSiteButtonPressed() {
        analyticsTracker.track(POST_SIGNUP_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED)
        navigationAction.value = START_SITE_CONNECTION_FLOW
    }

    fun onDismissButtonPressed() = onDismiss()

    fun onBackButtonPressed() = onDismiss()

    private fun onDismiss() {
        analyticsTracker.track(POST_SIGNUP_INTERSTITIAL_DISMISSED)
        navigationAction.value = DISMISS
    }

    enum class NavigationAction { START_SITE_CREATION_FLOW, START_SITE_CONNECTION_FLOW, DISMISS }
}
