package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

@Feature(StatsRevampV2FeatureConfig.STATS_REVAMP_V2_REMOTE_FIELD, true)
class StatsRevampV2FeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.STATS_REVAMP_V2,
        STATS_REVAMP_V2_REMOTE_FIELD
) {
    companion object {
        const val STATS_REVAMP_V2_REMOTE_FIELD = "stats_revamp_v2_remote_field"
    }
}
