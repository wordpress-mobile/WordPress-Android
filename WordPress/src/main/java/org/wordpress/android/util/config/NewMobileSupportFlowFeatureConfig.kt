package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val NEW_MOBILE_SUPPORT_FLOW_REMOTE_FIELD = "new_mobile_support_flow"

/**
 * Configuration for the new support flow, so it can be rolled out to a percentage of users rather
 * than relying on the Modern Support experimental toggle alone.
 */
@Feature(remoteField = NEW_MOBILE_SUPPORT_FLOW_REMOTE_FIELD, defaultValue = false)
class NewMobileSupportFlowFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.NEW_MOBILE_SUPPORT_FLOW,
    NEW_MOBILE_SUPPORT_FLOW_REMOTE_FIELD,
)
