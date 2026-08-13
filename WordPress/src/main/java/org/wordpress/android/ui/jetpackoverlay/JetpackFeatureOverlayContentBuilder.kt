package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseSelfHostedUsers
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackFeatureOverlayContentBuilder @Inject constructor() {
    fun buildDeepLinkOverlayState(isRtl: Boolean): JetpackFeatureOverlayUIState {
        return getStateForDeepLink(isRtl)
    }

    private fun getStateForDeepLink(isRtl: Boolean): JetpackFeatureOverlayUIState {
        val componentVisibility = JetpackFeatureOverlayComponentVisibility
            .DeepLinkPhase.All()
        val content = getContentForDeepLink(isRtl)
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    private fun getContentForDeepLink(rtl: Boolean): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
            illustration = if (rtl) R.raw.wp2jp_rtl else R.raw.wp2jp_left,
            title = R.string.wp_jetpack_deep_link_overlay_title,
            caption = UiStringRes(R.string.wp_jetpack_deep_link_overlay_description),
            primaryButtonText = R.string.wp_jetpack_deep_link_open_in_jetpack,
            secondaryButtonText = R.string.wp_jetpack_deep_link_open_in_wordpress
        )
    }

    // All Feature Overlay
    fun buildFeatureCollectionOverlayState(
        isRtl: Boolean, currentPhase: JetpackFeatureRemovalPhase, blogPostLink: String?
    ): JetpackFeatureOverlayUIState {
        return getStateForFeatureCollection(isRtl, currentPhase, blogPostLink)
    }

    private fun getStateForFeatureCollection(
        isRtl: Boolean,
        currentPhase: JetpackFeatureRemovalPhase,
        blogPostLink: String?
    ): JetpackFeatureOverlayUIState {
        val componentVisibility = when (currentPhase) {
            PhaseFour -> JetpackFeatureOverlayComponentVisibility.FeatureCollectionPhase.PhaseFour()
            PhaseNewUsers -> JetpackFeatureOverlayComponentVisibility.FeatureCollectionPhase.PhaseNewUsers()
            PhaseSelfHostedUsers ->
                JetpackFeatureOverlayComponentVisibility.FeatureCollectionPhase.PhaseSelfHostedUsers()
        }
        val content = getContentForFeatureCollection(isRtl, blogPostLink, currentPhase)
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    private fun getContentForFeatureCollection(
        isRtl: Boolean,
        blogPostLink: String?,
        currentPhase: JetpackFeatureRemovalPhase
    ): JetpackFeatureOverlayContent {
        return when (currentPhase) {
            is PhaseFour -> getJetpackFeatureOverlayContentForPhaseFour(isRtl, blogPostLink)
            is PhaseNewUsers -> getJetpackFeatureOverlayContentForNewUsers(isRtl)
            is PhaseSelfHostedUsers -> getJetpackFeatureOverlayContentForSelfHostedUsers(isRtl)
        }
    }

    private fun getJetpackFeatureOverlayContentForPhaseFour(
        isRtl: Boolean,
        blogPostLink: String?
    ) = JetpackFeatureOverlayContent(
        illustration = if (isRtl) R.raw.jp_all_features_rtl else R.raw.jp_all_features_left,
        title = R.string.wp_jetpack_feature_removal_overlay_phase_four_title_all_features,
        caption = UiStringRes(R.string.wp_jetpack_feature_removal_overlay_phase_four_all_features_description),
        migrationText = R.string.wp_jetpack_feature_removal_overlay_migration_helper_text,
        migrationInfoText = if (!blogPostLink.isNullOrEmpty())
            R.string.wp_jetpack_feature_removal_overlay_learn_more_migration_text else null,
        migrationInfoUrl = blogPostLink,
        primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_the_jetpack_app,
        secondaryButtonText = R.string.wp_jetpack_feature_removal_phase_four_secondary_text
    )

    private fun getJetpackFeatureOverlayContentForNewUsers(
        isRtl: Boolean,
    ) = JetpackFeatureOverlayContent(
        illustration = if (isRtl) R.raw.wp2jp_rtl else R.raw.wp2jp_left,
        title = R.string.wp_jetpack_feature_removal_phase_new_users_title,
        caption = UiStringRes(R.string.wp_jetpack_feature_removal_phase_new_users_description),
        primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_the_jetpack_app,
        secondaryButtonText = R.string.wp_jetpack_feature_removal_overlay_continue_without_jetpack
    )

    private fun getJetpackFeatureOverlayContentForSelfHostedUsers(
        isRtl: Boolean,
    ) = JetpackFeatureOverlayContent(
        illustration = if (isRtl) R.raw.wp2jp_rtl else R.raw.wp2jp_left,
        title = R.string.wp_jetpack_feature_removal_phase_self_hosted_users_title,
        caption = UiStringRes(R.string.wp_jetpack_feature_removal_phase_self_hosted_users_description),
        migrationText = R.string.wp_jetpack_feature_removal_overlay_migration_helper_text,
        primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_the_jetpack_app,
        secondaryButtonText = R.string.wp_jetpack_feature_removal_overlay_continue_without_jetpack
    )
}
