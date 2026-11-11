package org.wordpress.android.ui.jetpackoverlay

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import org.wordpress.android.ui.utils.UiString

@Suppress("LongParameterList")
sealed class JetpackFeatureOverlayComponentVisibility(
    val illustration: Boolean = true,
    val title: Boolean = true,
    val caption: Boolean = true,
    open val migrationText: Boolean = false,
    val primaryButton: Boolean = true,
    open val closeButton: Boolean = true,
    val secondaryButton: Boolean = true
) {
    class DeepLinkPhase : JetpackFeatureOverlayComponentVisibility()

    class FeatureCollectionPhase(
        override val closeButton: Boolean = false,
        override val migrationText: Boolean = true
    ) : JetpackFeatureOverlayComponentVisibility()
}

data class JetpackFeatureOverlayContent(
    @RawRes val illustration: Int,
    @StringRes val title: Int,
    val caption: UiString,
    @StringRes val migrationText: Int? = null,
    @StringRes val primaryButtonText: Int,
    @StringRes val secondaryButtonText: Int? = null
)

data class JetpackFeatureOverlayUIState(
    val componentVisibility: JetpackFeatureOverlayComponentVisibility,
    val overlayContent: JetpackFeatureOverlayContent
)

sealed class JetpackFeatureOverlayActions {
    object OpenPlayStore : JetpackFeatureOverlayActions()
    object DismissDialog : JetpackFeatureOverlayActions()
    object ForwardToJetpack : JetpackFeatureOverlayActions()
}

