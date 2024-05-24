package org.wordpress.android.util.config

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigWrapper @Inject constructor(
    private val openWebLinksWithJetpackFlowFrequencyConfig: OpenWebLinksWithJetpackFlowFrequencyConfig,
    private val codeableGetFreeEstimateUrlConfig: CodeableGetFreeEstimateUrlConfig,
    private val performanceMonitoringSampleRateConfig: PerformanceMonitoringSampleRateConfig,
    private val inAppUpdateBlockingVersionConfig: InAppUpdateBlockingVersionConfig,
    private val inAppUpdateFlexibleIntervalConfig: InAppUpdateFlexibleIntervalConfig,
) {
    fun getOpenWebLinksWithJetpackFlowFrequency() = openWebLinksWithJetpackFlowFrequencyConfig.getValue<Long>()
    fun getPerformanceMonitoringSampleRate() = performanceMonitoringSampleRateConfig.getValue<Double>()
    fun getCodeableGetFreeEstimateUrl() = codeableGetFreeEstimateUrlConfig.getValue<String>()
    fun getInAppUpdateBlockingVersion() = inAppUpdateBlockingVersionConfig.getValue<Int>()
    fun getInAppUpdateFlexibleIntervalInDays() = inAppUpdateFlexibleIntervalConfig.getValue<Int>()
}
