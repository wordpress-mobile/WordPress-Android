package org.wordpress.android.util.config

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigWrapper @Inject constructor(
    private val openWebLinksWithJetpackFlowFrequencyConfig: OpenWebLinksWithJetpackFlowFrequencyConfig,
    private val codeableGetFreeEstimateUrlConfig: CodeableGetFreeEstimateUrlConfig,
    private val performanceMonitoringSampleRateConfig: PerformanceMonitoringSampleRateConfig
) {
    fun getOpenWebLinksWithJetpackFlowFrequency() = openWebLinksWithJetpackFlowFrequencyConfig.getValue<Long>()
    fun getPerformanceMonitoringSampleRate() = performanceMonitoringSampleRateConfig.getValue<Double>()
    fun getCodeableGetFreeEstimateUrl() = codeableGetFreeEstimateUrlConfig.getValue<String>()
}
