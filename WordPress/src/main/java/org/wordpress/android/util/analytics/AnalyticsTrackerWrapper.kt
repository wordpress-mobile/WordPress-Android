package org.wordpress.android.util.analytics

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@Reusable
class AnalyticsTrackerWrapper
@Inject constructor() {
    fun track(stat: Stat) {
        AnalyticsTracker.track(stat)
    }

    fun track(stat: Stat, properties: Map<String, *>) {
        AnalyticsTracker.track(stat, properties)
    }

    fun track(stat: Stat, site: SiteModel) {
        AnalyticsUtils.trackWithSiteDetails(stat, site)
    }

    /**
     * A convenience method for logging an error event with some additional meta data.
     * @param stat The stat to track.
     * @param errorContext A string providing additional context (if any) about the error.
     * @param errorType The type of error.
     * @param errorDescription The error text or other description.
     */
    fun track(stat: Stat, errorContext: String, errorType: String, errorDescription: String) {
        track(stat, ErrorContext(errorContext, errorType, errorDescription))
    }

    fun track(stat: Stat, errorContext: ErrorContext? = null, properties: Map<String, String> = mapOf()) {
        val props = if (errorContext == null) {
            properties
        } else {
            mutableMapOf(
                    ERROR_CONTEXT to errorContext.context,
                    ERROR_TYPE to errorContext.type,
                    ERROR_DESCRIPTION to errorContext.description
            ).apply { putAll(properties) }
        }
        AnalyticsTracker.track(stat, props)
    }

    data class ErrorContext(val context: String, val type: String, val description: String)

    companion object {
        private const val ERROR_CONTEXT = "error_context"
        private const val ERROR_TYPE = "error_type"
        private const val ERROR_DESCRIPTION = "error_description"
    }
}
