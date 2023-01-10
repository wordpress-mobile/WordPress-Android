package org.wordpress.android.ui.about

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedAboutTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackScreenShown(screen: String) {
        analyticsTrackerWrapper.track(
            stat = Stat.ABOUT_SCREEN_SHOWN,
            properties = mapOf(
                Property.SCREEN.value to screen
            )
        )
    }

    fun trackScreenDismissed(screen: String) {
        analyticsTrackerWrapper.track(
            stat = Stat.ABOUT_SCREEN_DISMISSED,
            properties = mapOf(
                Property.SCREEN.value to screen
            )
        )
    }

    fun trackButtonTapped(button: String) {
        analyticsTrackerWrapper.track(
            stat = Stat.ABOUT_SCREEN_BUTTON_TAPPED,
            properties = mapOf(
                Property.BUTTON.value to button
            )
        )
    }

    enum class Property(val value: String) {
        SCREEN("screen"),
        BUTTON("button")
    }
}
