package org.wordpress.android.util.analytics

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.config.AppConfig.FeatureState
import org.wordpress.android.util.config.AppConfig.FeatureState.BuildConfigValue
import org.wordpress.android.util.config.AppConfig.FeatureState.DefaultValue
import org.wordpress.android.util.config.AppConfig.FeatureState.ManuallyOverriden
import org.wordpress.android.util.config.AppConfig.FeatureState.RemoteValue
import org.wordpress.android.util.config.ExperimentConfig
import org.wordpress.android.util.config.FeatureConfig
import javax.inject.Inject

@Reusable
class AnalyticsTrackerWrapper
@Inject constructor() {
    fun track(stat: Stat) {
        AnalyticsTracker.track(stat)
    }

    fun track(stat: Stat, feature: FeatureConfig) {
        AnalyticsTracker.track(
                stat,
                feature.toParams()
        )
    }

    fun track(stat: Stat, remoteField: String, featureState: FeatureState) {
        AnalyticsTracker.track(
                stat,
                buildFeatureConfigParams(remoteField, featureState)
        )
    }

    private fun FeatureConfig.toParams(): Map<String, Any> {
        return buildFeatureConfigParams(this.remoteField ?: this.javaClass.name, this.featureState())
    }

    private fun buildFeatureConfigParams(key: String, featureState: FeatureState): Map<String, Any> {
        return mapOf(
                key to featureState.isEnabled,
                "${key}_state" to featureState.toName()
        )
    }

    private fun FeatureState.toName(): String {
        return when (this) {
            is ManuallyOverriden -> "manually_overriden"
            is BuildConfigValue -> "build_config_value"
            is DefaultValue -> "default_value"
            is RemoteValue -> "remote_value"
        }
    }

    fun track(stat: Stat, experimentConfig: ExperimentConfig) {
        AnalyticsTracker.track(stat, mapOf(experimentConfig.remoteField to experimentConfig.getVariant().value))
    }

    @JvmOverloads
    fun track(stat: Stat, properties: Map<String, *>, feature: FeatureConfig? = null) {
        if (feature != null) {
            AnalyticsTracker.track(stat, properties + feature.toParams())
        } else {
            AnalyticsTracker.track(stat, properties)
        }
    }

    @JvmOverloads
    fun track(stat: Stat, site: SiteModel?, feature: FeatureConfig? = null) {
        AnalyticsUtils.trackWithSiteDetails(stat, site, feature?.toParams())
    }

    fun track(stat: Stat, siteId: Long) {
        AnalyticsUtils.trackWithSiteId(stat, siteId)
    }

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
}
