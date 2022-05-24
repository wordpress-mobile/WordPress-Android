package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

// TODO: Uncomment the lines 8 and 14 when remote field is configured and remove line 9 and this to-do
// @Feature(StatsRevampV2FeatureConfig.STATS_REVAMP_V2_REMOTE_FIELD, false)
@FeatureInDevelopment
class StatsRevampV2FeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.STATS_REVAMP_V2
//        STATS_REVAMP_V2_REMOTE_FIELD
) {
    companion object {
        const val STATS_REVAMP_V2_REMOTE_FIELD = "stats_revamp_v2_remote_field"
    }
}
