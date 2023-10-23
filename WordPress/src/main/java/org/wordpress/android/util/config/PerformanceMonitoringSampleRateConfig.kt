package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val PERFORMANCE_MONITORING_SAMPLE_RATE_REMOTE_FIELD = "wp_android_performance_monitoring_sample_rate"
const val PERFORMANCE_MONITORING_SAMPLE_RATE_DEFAULT = "0.0"

@RemoteFieldDefaultGenerater(
    remoteField = PERFORMANCE_MONITORING_SAMPLE_RATE_REMOTE_FIELD,
    defaultValue = PERFORMANCE_MONITORING_SAMPLE_RATE_DEFAULT
)

class PerformanceMonitoringSampleRateConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<Double>(
        appConfig,
        PERFORMANCE_MONITORING_SAMPLE_RATE_REMOTE_FIELD
    )
