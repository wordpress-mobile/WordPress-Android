package org.wordpress.android.ui.news

import org.wordpress.android.util.analytics.AnalyticsUtils
import java.util.Locale
import javax.inject.Inject

import javax.inject.Singleton

@Singleton
class NewsTracker @Inject constructor() {
    enum class NewsCardOrigin {
        READER
    }

    fun trackNewsCardShown(origin: NewsCardOrigin, version: Int) {
        AnalyticsUtils.trackNewsCardShown(origin.name.toLowerCase(Locale.ROOT), version)
    }

    fun trackNewsCardDismissed(origin: NewsCardOrigin, version: Int) {
        AnalyticsUtils.trackNewsCardDismissed(origin.name.toLowerCase(Locale.ROOT), version)
    }

    fun trackNewsCardExtendedInfoRequested(origin: NewsCardOrigin, version: Int) {
        AnalyticsUtils.trackNewsCardExtendedInfoRequested(origin.name.toLowerCase(Locale.ROOT), version)
    }
}
