package org.wordpress.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import org.wordpress.android.util.BuildConfigWrapper
import javax.inject.Inject
import org.wordpress.android.util.FirebaseRemoteConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class WPPerformanceMonitoringConfig @Inject constructor(
    private val remoteConfigWrapper: FirebaseRemoteConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    operator fun invoke(): PerformanceMonitoringConfig {
        val sampleRate = remoteConfigWrapper.getPerformanceMonitoringSampleRate()
        val hasUserOptedOut = analyticsTrackerWrapper.hasUserOptedOut

        return when {
            hasUserOptedOut || buildConfigWrapper.isDebug() -> PerformanceMonitoringConfig.Disabled
            sampleRate <= 0.0 || sampleRate > 1.0 -> PerformanceMonitoringConfig.Disabled
            else -> PerformanceMonitoringConfig.Enabled(sampleRate)
        }
    }
}
