package org.wordpress.android.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteConfigWrapper @Inject constructor() {
    fun getOpenWebLinksWithJetpackFlowFrequency() =
        FirebaseRemoteConfig.getInstance().getLong(OPEN_WEB_LINKS_WITH_JETPACK_FLOW_FREQUENCY_KEY)
    fun getCodeableGetFreeEstimateUrl() =
        FirebaseRemoteConfig.getInstance().getString(CODEABLE_GET_FREE_ESTIMATE_FIREBASE_KEY)
    fun getPerformanceMonitoringSampleRate(): Double =
        FirebaseRemoteConfig.getInstance().getDouble(PERFORMANCE_MONITORING_SAMPLE_RATE_KEY)

    companion object {
        private const val PERFORMANCE_MONITORING_SAMPLE_RATE_KEY = "wp_android_performance_monitoring_sample_rate"

        private const val CODEABLE_GET_FREE_ESTIMATE_FIREBASE_KEY = "codeable_get_free_estimate_url"

        private const val OPEN_WEB_LINKS_WITH_JETPACK_FLOW_FREQUENCY_KEY = "open_web_links_with_jetpack_flow_frequency"
    }
}
