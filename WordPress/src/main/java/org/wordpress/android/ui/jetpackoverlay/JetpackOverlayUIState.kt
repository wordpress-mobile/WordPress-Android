package org.wordpress.android.ui.jetpackoverlay

import androidx.annotation.RawRes
import androidx.annotation.StringRes

sealed class JetpackFeatureOverlayComponentVisibility(
    val illustration: Boolean = true,
    val title: Boolean = true,
    val caption: Boolean = true,
    val primaryButton: Boolean = true,
    val secondaryButton: Boolean = true
) {
    class PhaseOne : JetpackFeatureOverlayComponentVisibility()
}

class JetpackFeatureOverlayContent(
    @RawRes val illustration: Int,
    @StringRes val title: Int,
    @StringRes val caption: Int,
    @StringRes val primaryButtonText: Int,
    @StringRes val secondaryButtonText: Int
)

class JetpackFeatureOverlayUIState(
    val componentVisibility: JetpackFeatureOverlayComponentVisibility,
    val overlayContent: JetpackFeatureOverlayContent
)

