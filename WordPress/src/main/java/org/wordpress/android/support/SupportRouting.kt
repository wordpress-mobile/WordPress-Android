package org.wordpress.android.support

import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.NewMobileSupportFlowFeatureConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether the new support flow is shown, shared by every route into Help & Support so they
 * can't drift apart.
 */
@Singleton
class SupportRouting @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val newMobileSupportFlowFeatureConfig: NewMobileSupportFlowFeatureConfig
) {
    /**
     * The new flow is shown to users in the remote rollout, and to anyone who flipped the
     * experimental toggle so internal users can opt in regardless of their bucket.
     */
    fun isNewSupportFlowEnabled(): Boolean =
        experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT) ||
                newMobileSupportFlowFeatureConfig.isEnabled()
}
