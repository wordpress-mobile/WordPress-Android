package org.wordpress.android.support

import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.NewMobileSupportFlowFeatureConfig
import javax.inject.Inject

/**
 * Decides whether the new support flow should be used instead of the legacy Zendesk based one.
 *
 * The flow is available either when the user opted in through the experimental features screen or
 * when it has been remotely enabled for them.
 */
class NewSupportFlowFeature @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val newMobileSupportFlowFeatureConfig: NewMobileSupportFlowFeatureConfig
) {
    fun isEnabled(): Boolean = experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT) ||
            newMobileSupportFlowFeatureConfig.isEnabled()
}
