package org.wordpress.android.util.analytics

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.config.AppConfig.FeatureState
import org.wordpress.android.util.config.ExperimentConfig
import org.wordpress.android.util.config.FeatureConfig
import javax.inject.Inject

@Reusable
class AnalyticsTrackerWrapper
@Inject constructor() {
    fun track(stat: Stat) {
        AnalyticsTracker.track(stat)
    }

    fun track(stat: Stat, remoteField: String, featureState: FeatureState) {
        AnalyticsTracker.track(
                stat,
                mapOf(
                        remoteField to featureState.isEnabled,
                        "${remoteField}_state" to featureState.name
                )
        )
    }

    fun track(stat: Stat, experimentConfig: ExperimentConfig) {
        AnalyticsTracker.track(stat, mapOf(experimentConfig.remoteField to experimentConfig.getVariant().value))
    }

    fun track(stat: Stat, properties: Map<String, Any?>) {
        AnalyticsTracker.track(stat, properties)
    }

    fun track(stat: Stat, properties: Map<String, *>, feature: FeatureConfig) {
        AnalyticsTracker.track(stat, properties + feature.toParams())
    }

    @JvmOverloads
    fun track(stat: Stat, site: SiteModel?, properties: Map<String, Any?>? = null) {
        AnalyticsUtils.trackWithSiteDetails(this, stat, site, properties)
    }

    fun track(stat: Stat, site: SiteModel?, feature: FeatureConfig) {
        AnalyticsUtils.trackWithSiteDetails(this, stat, site, feature.toParams().toMutableMap<String, Any>())
    }

    fun getAnonID(): String? = AnalyticsTracker.getAnonID()

    /**
     * A convenience method for logging an error event with some additional meta data.
     * @param stat The stat to track.
     * @param errorContext A string providing additional context (if any) about the error.
     * @param errorType The type of error.
     * @param errorDescription The error text or other description.
     */
    fun track(stat: Stat, errorContext: String, errorType: String, errorDescription: String) {
        AnalyticsTracker.track(stat, errorContext, errorType, errorDescription)
    }

    private fun FeatureConfig.toParams() = mapOf(name() to isEnabled())
}
