package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class JetpackFeatureRemovalBrandingUtil @Inject constructor(
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
) {
    fun isInRemovalPhase() = jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()

    fun shouldShowBranding(): Boolean {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            PhaseFour -> true
            else -> false
        }
    }

    fun getBrandingText(): UiString = UiStringRes(R.string.wp_jetpack_powered)
}
