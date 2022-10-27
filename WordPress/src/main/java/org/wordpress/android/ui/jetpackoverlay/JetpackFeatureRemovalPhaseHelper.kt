package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalSiteCreationPhase.PHASE_ONE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalSiteCreationPhase.PHASE_TWO
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseTwo
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackFeatureRemovalNewUsersConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseFourConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseOneConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseThreeConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseTwoConfig
import javax.inject.Inject

private const val PHASE_ONE_GLOBAL_OVERLAY_FREQUENCY_IN_DAYS = 2
private const val PHASE_ONE_FEATURE_OVERLAY_FREQUENCY_IN_DAYS = 7

private const val PHASE_TWO_GLOBAL_OVERLAY_FREQUENCY_IN_DAYS = 2
private const val PHASE_TWO_FEATURE_OVERLAY_FREQUENCY_IN_DAYS = 7

private const val PHASE_THREE_GLOBAL_OVERLAY_FREQUENCY_IN_DAYS = 2
private const val PHASE_THREE_FEATURE_OVERLAY_FREQUENCY_IN_DAYS = 4

// Class used to find the current phase
// of the Jetpack powered migration
class JetpackFeatureRemovalPhaseHelper @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val jetpackFeatureRemovalPhaseOneConfig: JetpackFeatureRemovalPhaseOneConfig,
    private val jetpackFeatureRemovalPhaseTwoConfig: JetpackFeatureRemovalPhaseTwoConfig,
    private val jetpackFeatureRemovalPhaseThreeConfig: JetpackFeatureRemovalPhaseThreeConfig,
    private val jetpackFeatureRemovalPhaseFourConfig: JetpackFeatureRemovalPhaseFourConfig,
    private val jetpackFeatureRemovalNewUsersConfig: JetpackFeatureRemovalNewUsersConfig
) {
    fun getTheCurrentPhase(): JetpackPoweredCurrentPhase? {
        return if (buildConfigWrapper.isJetpackApp) null
        else if (jetpackFeatureRemovalNewUsersConfig.isEnabled()) PhaseNewUsers
        else if (jetpackFeatureRemovalPhaseFourConfig.isEnabled()) PhaseFour
        else if (jetpackFeatureRemovalPhaseThreeConfig.isEnabled()) PhaseThree
        else if (jetpackFeatureRemovalPhaseTwoConfig.isEnabled()) PhaseTwo
        else if (jetpackFeatureRemovalPhaseOneConfig.isEnabled()) PhaseOne
        else null
    }

    fun getSiteCreationPhase(): JetpackFeatureRemovalSiteCreationPhase? {
        val currentPhase = getTheCurrentPhase() ?: return null
        return when (currentPhase) {
            is PhaseOne, PhaseTwo, PhaseThree -> PHASE_ONE
            is PhaseFour, PhaseNewUsers -> PHASE_TWO
        }
    }
}
// Global overlay frequency is the frequency at which the overlay is shown across the features
// no matter which feature was accessed last time

// Feature specific overlay frequency is the frequency at which the overlay is shown for a specific feature

sealed class JetpackPoweredCurrentPhase(
    val globalOverlayFrequency: Int = 0,
    val featureSpecificOverlayFrequency: Int = 0
) {
    object PhaseOne : JetpackPoweredCurrentPhase(
            PHASE_ONE_GLOBAL_OVERLAY_FREQUENCY_IN_DAYS,
            PHASE_ONE_FEATURE_OVERLAY_FREQUENCY_IN_DAYS
    )

    object PhaseTwo : JetpackPoweredCurrentPhase(
            PHASE_TWO_GLOBAL_OVERLAY_FREQUENCY_IN_DAYS,
            PHASE_TWO_FEATURE_OVERLAY_FREQUENCY_IN_DAYS
    )

    object PhaseThree : JetpackPoweredCurrentPhase(
            PHASE_THREE_GLOBAL_OVERLAY_FREQUENCY_IN_DAYS,
            PHASE_THREE_FEATURE_OVERLAY_FREQUENCY_IN_DAYS
    )

    object PhaseFour : JetpackPoweredCurrentPhase()
    object PhaseNewUsers : JetpackPoweredCurrentPhase()
}

enum class JetpackFeatureRemovalSiteCreationPhase {
    PHASE_ONE, PHASE_TWO
}
