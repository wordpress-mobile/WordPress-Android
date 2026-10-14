package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val NEW_MOBILE_SUPPORT_FLOW_REMOTE_FIELD = "new_mobile_support_flow"

/**
 * Configuration for the new mobile support flow.
 */
@Feature(NEW_MOBILE_SUPPORT_FLOW_REMOTE_FIELD, false)
class NewMobileSupportFlowFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.NEW_MOBILE_SUPPORT_FLOW,
    NEW_MOBILE_SUPPORT_FLOW_REMOTE_FIELD
)
