package org.wordpress.android.ui.deeplinks

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@HiltViewModel
class DeepLinkingCustomIntentReceiverViewModel @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val activityLauncherWrapper: ActivityLauncherWrapper,
    private val contextProvider: ContextProvider
) : ViewModel() {
    fun forwardDeepLink(intent: Intent) {
        analyticsTrackerWrapper.track(Stat.DEEPLINK_CUSTOM_INTENT_RECEIVED)
        activityLauncherWrapper.forwardDeepLinkIntent(contextProvider.getContext(), intent)
    }
}
