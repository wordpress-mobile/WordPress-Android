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
    open val secondaryButton: Boolean = true,
    open val migrationInfoText: Boolean = false,
    open val newUsersContent: Boolean = false
) {
    class PhaseOne : JetpackFeatureOverlayComponentVisibility()
    class PhaseTwo(override val migrationInfoText: Boolean = true) : JetpackFeatureOverlayComponentVisibility()
    class PhaseThree(
        override val migrationInfoText: Boolean = true,
        override val closeButton: Boolean = false,
        override val migrationText: Boolean = true
    ) : JetpackFeatureOverlayComponentVisibility()

    sealed class SiteCreationPhase : JetpackFeatureOverlayComponentVisibility() {
        class PhaseOne : SiteCreationPhase()
        class PhaseTwo(override val secondaryButton: Boolean = false) : SiteCreationPhase()
    }

    sealed class DeepLinkPhase : JetpackFeatureOverlayComponentVisibility() {
        class All : DeepLinkPhase()
    }

    sealed class FeatureCollectionPhase : JetpackFeatureOverlayComponentVisibility() {
        class PhaseThree(
            override val migrationInfoText: Boolean = true,
            override val closeButton: Boolean = false,
            override val migrationText: Boolean = true
        ) : FeatureCollectionPhase()

        class PhaseFour(
            override val migrationInfoText: Boolean = true,
            override val closeButton: Boolean = false,
            override val migrationText: Boolean = true
        ) : FeatureCollectionPhase()

        class PhaseNewUsers(
            override val migrationInfoText: Boolean = false,
            override val closeButton: Boolean = false,
            override val migrationText: Boolean = false,
            override val newUsersContent: Boolean = true
        ) : FeatureCollectionPhase()


        class Final(override val closeButton: Boolean = false) : FeatureCollectionPhase()
    }
}

data class JetpackFeatureOverlayContent(
    @RawRes val illustration: Int,
    @StringRes val title: Int,
    val caption: UiString,
    @StringRes val migrationText: Int? = null,
    @StringRes val migrationInfoText: Int? = null,
    val migrationInfoUrl: String? = null,
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
    data class OpenMigrationInfoLink(val url: String) : JetpackFeatureOverlayActions()
}

