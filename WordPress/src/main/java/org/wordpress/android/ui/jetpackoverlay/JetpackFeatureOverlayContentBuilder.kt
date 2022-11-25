package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import javax.inject.Inject

class JetpackFeatureOverlayContentBuilder @Inject constructor() {
    fun build(params: JetpackFeatureOverlayContentBuilderParams): JetpackFeatureOverlayUIState {
        return when (params.currentPhase) {
            is PhaseOne -> getStateForPhaseOne(params, params.feature!!)
            PhaseTwo -> TODO()
            PhaseThree -> TODO()
            PhaseFour -> TODO()
            PhaseNewUsers -> TODO()
        }
    }

    private fun getStateForPhaseOne(
        params: JetpackFeatureOverlayContentBuilderParams,
        feature: JetpackFeatureOverlayScreenType
    ): JetpackFeatureOverlayUIState {
        val componentVisibility = JetpackFeatureOverlayComponentVisibility.PhaseOne()
        val content = when (feature) {
            JetpackFeatureOverlayScreenType.STATS -> getStateForPhaseOneStats(params.isRtl)
            JetpackFeatureOverlayScreenType.NOTIFICATIONS -> getStateForPhaseOneNotifications(params.isRtl)
            JetpackFeatureOverlayScreenType.READER -> getStateForPhaseOneReader(params.isRtl)
        }
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    private fun getStateForPhaseOneStats(rtl: Boolean): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (rtl) R.raw.jp_stats_rtl else R.raw.jp_stats_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_one_title_stats,
                caption = R.string.wp_jetpack_feature_removal_overlay_phase_one_description_stats,
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_stats
        )
    }

    private fun getStateForPhaseOneReader(rtl: Boolean): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (rtl) R.raw.jp_reader_rtl else R.raw.jp_reader_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_one_title_reader,
                caption = R.string.wp_jetpack_feature_removal_overlay_phase_one_description_reader,
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_reader
        )
    }

    private fun getStateForPhaseOneNotifications(rtl: Boolean): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (rtl) R.raw.jp_notifications_rtl else R.raw.jp_notifications_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_one_title_notifications,
                caption = R.string.wp_jetpack_feature_removal_overlay_phase_one_description_notifications,
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_notifications
        )
    }

    fun buildSiteCreationOverlayState(
        siteCreationPhase: JetpackFeatureRemovalSiteCreationPhase,
        isRtl: Boolean
    ): JetpackFeatureOverlayUIState {
        return when (siteCreationPhase) {
            JetpackFeatureRemovalSiteCreationPhase.PHASE_ONE -> getStateForSiteCreationPhaseOne(isRtl)
            JetpackFeatureRemovalSiteCreationPhase.PHASE_TWO -> getStateForSiteCreationPhaseTwo(isRtl)
        }
    }

    private fun getStateForSiteCreationPhaseOne(isRtl: Boolean): JetpackFeatureOverlayUIState {
        val componentVisibility = JetpackFeatureOverlayComponentVisibility
                .SiteCreationPhase.PhaseOne()
        val content = getContentForSiteCreationPhaseOne(isRtl)
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    private fun getContentForSiteCreationPhaseOne(rtl: Boolean): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (rtl) R.raw.wp2jp_rtl else R.raw.wp2jp_left,
                title = R.string.wp_jetpack_feature_removal_site_creation_overlay_title,
                caption = R.string.wp_jetpack_feature_removal_site_creation_overlay_phase_one_description,
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_without_jetpack
        )
    }

    private fun getStateForSiteCreationPhaseTwo(rtl: Boolean): JetpackFeatureOverlayUIState {
        val componentVisibility = JetpackFeatureOverlayComponentVisibility
                .SiteCreationPhase.PhaseTwo()
        val content = getContentForSiteCreationPhaseTwo(rtl)
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    private fun getContentForSiteCreationPhaseTwo(rtl: Boolean): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (rtl) R.raw.wp2jp_rtl else R.raw.wp2jp_left,
                title = R.string.wp_jetpack_feature_removal_site_creation_overlay_title,
                caption = R.string.wp_jetpack_feature_removal_site_creation_overlay_phase_two_description,
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
        )
    }

    fun buildDeepLinkOverlayState(isRtl: Boolean): JetpackFeatureOverlayUIState { return getStateForDeepLink(isRtl) }

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
                caption = R.string.wp_jetpack_deep_link_overlay_description,
                primaryButtonText = R.string.wp_jetpack_deep_link_open_with_jetpack,
                secondaryButtonText = R.string.wp_jetpack_continue_without_jetpack
        )
    }
}

data class JetpackFeatureOverlayContentBuilderParams(
    val currentPhase: JetpackFeatureRemovalPhase,
    val isRtl: Boolean = true,
    val feature: JetpackFeatureOverlayScreenType?
)

