package org.wordpress.android.support

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SupportWebViewActivityViewModel @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ViewModel() {
    // Locally generated chat ID for analytics purposes
    private val chatId = UUID.randomUUID().toString()
    private val chatIdProperty = mapOf("chat_id" to chatId)

    fun start() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SUPPORT_CHATBOT_STARTED, chatIdProperty)
    }

    fun onWebViewReceivedError() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SUPPORT_CHATBOT_WEBVIEW_ERROR, chatIdProperty)
    }
}
