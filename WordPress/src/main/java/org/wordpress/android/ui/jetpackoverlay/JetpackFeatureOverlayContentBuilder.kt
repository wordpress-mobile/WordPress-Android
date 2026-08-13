package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackFeatureOverlayContentBuilder @Inject constructor() {
    fun buildDeepLinkOverlayState(isRtl: Boolean) = JetpackFeatureOverlayUIState(
        JetpackFeatureOverlayComponentVisibility.DeepLinkPhase(),
        JetpackFeatureOverlayContent(
            illustration = if (isRtl) R.raw.wp2jp_rtl else R.raw.wp2jp_left,
            title = R.string.wp_jetpack_deep_link_overlay_title,
            caption = UiStringRes(R.string.wp_jetpack_deep_link_overlay_description),
            primaryButtonText = R.string.wp_jetpack_deep_link_open_in_jetpack,
            secondaryButtonText = R.string.wp_jetpack_deep_link_open_in_wordpress
        )
    )

    fun buildFeatureCollectionOverlayState(isRtl: Boolean) = JetpackFeatureOverlayUIState(
        JetpackFeatureOverlayComponentVisibility.FeatureCollectionPhase(),
        JetpackFeatureOverlayContent(
            illustration = if (isRtl) R.raw.wp2jp_rtl else R.raw.wp2jp_left,
            title = R.string.wp_jetpack_feature_removal_phase_self_hosted_users_title,
            caption = UiStringRes(R.string.wp_jetpack_feature_removal_phase_self_hosted_users_description),
            migrationText = R.string.wp_jetpack_feature_removal_overlay_migration_helper_text,
            primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_the_jetpack_app,
            secondaryButtonText = R.string.wp_jetpack_feature_removal_overlay_continue_without_jetpack
        )
    )
}
