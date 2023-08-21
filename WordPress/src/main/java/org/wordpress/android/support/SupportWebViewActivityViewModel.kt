package org.wordpress.android.support

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

@HiltViewModel
class SupportWebViewActivityViewModel @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ViewModel() {
    private lateinit var chatIdProp: Map<String, String>

    fun start(chatIdProperty: Map<String, String>) {
        this.chatIdProp = chatIdProperty
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SUPPORT_CHATBOT_STARTED, chatIdProp)
    }

    fun onWebViewReceivedError() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SUPPORT_CHATBOT_WEBVIEW_ERROR, chatIdProp)
    }

    fun onTicketCreated() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SUPPORT_CHATBOT_TICKET_SUCCESS, chatIdProp)
    }

    fun onTicketCreationError(errorMessage: String?) {
        val properties = chatIdProp.toMutableMap()
        errorMessage?.let { properties.put("error_message", errorMessage) }
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SUPPORT_CHATBOT_TICKET_FAILURE, properties)
    }

    override fun onCleared() {
        super.onCleared()
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SUPPORT_CHATBOT_ENDED, chatIdProp)
    }
}
