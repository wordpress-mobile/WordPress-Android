package org.wordpress.android.ui.jetpackoverlay

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import org.wordpress.android.ui.utils.UiString

sealed class JetpackFeatureOverlayComponentVisibility(
    val illustration: Boolean = true,
    val title: Boolean = true,
    val caption: Boolean = true,
    val primaryButton: Boolean = true,
    open val secondaryButton: Boolean = true
) {
    class PhaseOne : JetpackFeatureOverlayComponentVisibility()
    sealed class SiteCreationPhase : JetpackFeatureOverlayComponentVisibility() {
        class PhaseOne : SiteCreationPhase()
        class PhaseTwo(override val secondaryButton: Boolean = false) : SiteCreationPhase()
    }
    sealed class DeepLinkPhase : JetpackFeatureOverlayComponentVisibility() {
        class All : DeepLinkPhase()
    }
}

class JetpackFeatureOverlayContent(
    @RawRes val illustration: Int,
    @StringRes val title: Int,
    @StringRes val caption: Int,
    @StringRes val primaryButtonText: Int,
    @StringRes val secondaryButtonText: Int? = null
)

class JetpackFeatureOverlayUIState(
    val componentVisibility: JetpackFeatureOverlayComponentVisibility,
    val overlayContent: JetpackFeatureOverlayContent
)

sealed class JetpackFeatureOverlayActions {
    object OpenPlayStore : JetpackFeatureOverlayActions()
    object DismissDialog : JetpackFeatureOverlayActions()
    object ForwardToJetpack : JetpackFeatureOverlayActions()
}

