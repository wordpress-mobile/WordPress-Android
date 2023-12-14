package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

const val BLAZE_MANAGE_CAMPAIGNS_REMOTE_FIELD = "blaze_manage_campaigns"

@Feature(BLAZE_MANAGE_CAMPAIGNS_REMOTE_FIELD, true)
class BlazeManageCampaignFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.BLAZE_MANAGE_CAMPAIGNS,
    BLAZE_MANAGE_CAMPAIGNS_REMOTE_FIELD
)
