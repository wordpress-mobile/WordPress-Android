package org.wordpress.android.support

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.NewMobileSupportFlowFeatureConfig
import javax.inject.Inject

/**
 * Decides whether the new support flow should be used instead of the legacy Zendesk based one.
 *
 * The flow is available either when the user opted in through the experimental features screen or
 * when it has been remotely enabled for them.
 *
 * The remote rollout is additionally limited to the users the new flow can actually help. Its only
 * way to reach a human is support chat, which the support screen offers only on Jetpack and only to
 * WordPress.com authenticated users. Everyone else would land on a screen with no contact option,
 * while the legacy flow still lets them open a ticket, so they stay on it until the new flow covers
 * them. The experimental toggle is intentionally left unrestricted so the screen can be previewed
 * internally in both apps and while signed out.
 *
 * Note this only governs the support screen opened from ActivityLauncher.viewHelp. Other routes
 * into support, such as the Zendesk notification stack and the feedback form link, still open the
 * legacy screens.
 */
class NewSupportFlowFeature @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val newMobileSupportFlowFeatureConfig: NewMobileSupportFlowFeatureConfig,
    private val accountStore: AccountStore,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun isEnabled(): Boolean = isEnabledExperimentally() || isEnabledRemotely()

    private fun isEnabledExperimentally(): Boolean = experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)

    private fun isEnabledRemotely(): Boolean = newMobileSupportFlowFeatureConfig.isEnabled() &&
            buildConfigWrapper.isJetpackApp &&
            accountStore.hasAccessToken()
}
