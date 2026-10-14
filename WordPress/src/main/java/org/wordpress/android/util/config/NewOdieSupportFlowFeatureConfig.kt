package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.NewOdieSupportFlowFeatureConfig.Companion.NEW_ODIE_SUPPORT_FLOW_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration for the new Odie-based support flow.
 */
@Feature(NEW_ODIE_SUPPORT_FLOW_REMOTE_FIELD, false)
class NewOdieSupportFlowFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.NEW_ODIE_SUPPORT_FLOW,
    NEW_ODIE_SUPPORT_FLOW_REMOTE_FIELD,
) {
    companion object {
        const val NEW_ODIE_SUPPORT_FLOW_REMOTE_FIELD = "new_odie_support_flow"
    }
}
