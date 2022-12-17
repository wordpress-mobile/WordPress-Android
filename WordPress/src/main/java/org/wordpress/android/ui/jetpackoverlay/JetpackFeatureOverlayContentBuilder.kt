package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.config.JPDeadlineConfig
import org.wordpress.android.util.config.PhaseThreeBlogPostLinkConfig
import org.wordpress.android.util.config.PhaseTwoBlogPostLinkConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class JetpackFeatureOverlayContentBuilder @Inject constructor(
    private val jpDeadlineConfig: JPDeadlineConfig,
    private val phaseTwoBlogPostLinkConfig: PhaseTwoBlogPostLinkConfig,
    private val phaseThreeBlogPostLinkConfig: PhaseThreeBlogPostLinkConfig,
    private val htmlMessageUtils: HtmlMessageUtils
) {
    fun build(params: JetpackFeatureOverlayContentBuilderParams): JetpackFeatureOverlayUIState {
        return when (params.currentPhase) {
            is PhaseOne -> getStateForPhaseOne(params, params.feature!!)
            PhaseTwo -> getStateForPhaseTwo(
                    params,
                    phaseTwoBlogPostLinkConfig.getValue(),
                    jpDeadlineConfig.getValue()
            )
            PhaseThree -> getStateForPhaseThree(
                    params,
                    phaseThreeBlogPostLinkConfig.getValue(),
                    jpDeadlineConfig.getValue()
            )
            PhaseFour -> TODO()
            PhaseNewUsers -> TODO()
        }
    }

    private fun getStateForPhaseThree(
        params: JetpackFeatureOverlayContentBuilderParams,
        phaseThreeBlogPostLink: String,
        jpDeadlineDate: String,
    ): JetpackFeatureOverlayUIState {
        val componentVisibility = JetpackFeatureOverlayComponentVisibility.PhaseThree()
        val content = when (params.feature!!) {
            JetpackFeatureOverlayScreenType.STATS -> getStateForPhaseTwoStats(
                    params,
                    jpDeadlineDate,
                    phaseThreeBlogPostLink
            )
            JetpackFeatureOverlayScreenType.NOTIFICATIONS -> getStateForPhaseTwoNotifications(
                    params,
                    jpDeadlineDate,
                    phaseThreeBlogPostLink
            )
            JetpackFeatureOverlayScreenType.READER -> getStateForPhaseTwoReader(
                    params,
                    jpDeadlineDate,
                    phaseThreeBlogPostLink
            )
        }.copy(
                migrationText = R.string.wp_jetpack_feature_removal_overlay_migration_helper_text
        )
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    private fun getStateForPhaseTwo(
        params: JetpackFeatureOverlayContentBuilderParams,
        phaseTwoBlogPostLinkConfig: String,
        jpDeadlineDate: String,
    ): JetpackFeatureOverlayUIState {
        val componentVisibility = JetpackFeatureOverlayComponentVisibility.PhaseTwo()
        val content = when (params.feature!!) {
            JetpackFeatureOverlayScreenType.STATS -> getStateForPhaseTwoStats(
                    params,
                    jpDeadlineDate,
                    phaseTwoBlogPostLinkConfig
            )
            JetpackFeatureOverlayScreenType.NOTIFICATIONS -> getStateForPhaseTwoNotifications(
                    params,
                    jpDeadlineDate,
                    phaseTwoBlogPostLinkConfig
            )
            JetpackFeatureOverlayScreenType.READER -> getStateForPhaseTwoReader(
                    params,
                    jpDeadlineDate,
                    phaseTwoBlogPostLinkConfig
            )
        }
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getStateForPhaseTwoStats(
        params: JetpackFeatureOverlayContentBuilderParams,
        jpDeadlineDate: String,
        phaseTwoBlogPostLinkConfig: String
    ): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (params.isRtl) R.raw.jp_stats_rtl else R.raw.jp_stats_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_title_stats,
                caption = getCaptionForPhaseTwoAndThree(jpDeadlineDate),
                migrationInfoText = R.string.wp_jetpack_feature_removal_overlay_learn_more_migration_text,
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_stats
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getStateForPhaseTwoReader(
        params: JetpackFeatureOverlayContentBuilderParams,
        jpDeadlineDate: String,
        phaseTwoBlogPostLinkConfig: String
    ): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (params.isRtl) R.raw.jp_reader_rtl else R.raw.jp_reader_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_title_reader,
                caption = getCaptionForPhaseTwoAndThree(jpDeadlineDate),
                migrationInfoText = R.string.wp_jetpack_feature_removal_overlay_learn_more_migration_text,
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_reader
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getStateForPhaseTwoNotifications(
        params: JetpackFeatureOverlayContentBuilderParams,
        jpDeadlineDate: String,
        phaseTwoBlogPostLinkConfig: String
    ): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (params.isRtl) R.raw.jp_notifications_rtl else R.raw.jp_notifications_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_title_notifications,
                caption = getCaptionForPhaseTwoAndThree(jpDeadlineDate),
                migrationInfoText = R.string.wp_jetpack_feature_removal_overlay_learn_more_migration_text,
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_notifications
        )
    }

    private fun getCaptionForPhaseTwoAndThree(jpDeadlineDate: String): UiString {
        return if (jpDeadlineDate.isEmpty()) getPhaseTwoAndThreeCaptionWithoutDeadline()
        else getPhaseTwoAndThreeCaptionWithDeadline(jpDeadlineDate)
    }

    private fun getPhaseTwoAndThreeCaptionWithDeadline(jpDeadlineDate: String): UiString {
        val deadlineDate = convertDateFormat(jpDeadlineDate)
        return UiStringText(
                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                        R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_description_with_deadline,
                                "<b>$deadlineDate</b>"
                        )
        )
    }

    private fun getPhaseTwoAndThreeCaptionWithoutDeadline() = UiStringRes(
            R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_description_without_deadline
    )

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
                caption = UiStringRes(R.string.wp_jetpack_feature_removal_overlay_phase_one_description_stats),
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_stats
        )
    }

    private fun getStateForPhaseOneReader(rtl: Boolean): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (rtl) R.raw.jp_reader_rtl else R.raw.jp_reader_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_one_title_reader,
                caption = UiStringRes(R.string.wp_jetpack_feature_removal_overlay_phase_one_description_reader),
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_reader
        )
    }

    private fun getStateForPhaseOneNotifications(rtl: Boolean): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = if (rtl) R.raw.jp_notifications_rtl else R.raw.jp_notifications_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_one_title_notifications,
                caption = UiStringRes(R.string.wp_jetpack_feature_removal_overlay_phase_one_description_notifications),
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
                caption = UiStringRes(R.string.wp_jetpack_feature_removal_site_creation_overlay_phase_one_description),
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
                caption = UiStringRes(R.string.wp_jetpack_feature_removal_site_creation_overlay_phase_two_description),
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
        )
    }

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

    private fun convertDateFormat(date: String): String {
        //Format 2020-12-22
        val originalFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        //Format Decemeber 22, 2020
        val targetFormat = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
        return LocalDate.parse(date, originalFormat).format(targetFormat)
    }
}

data class JetpackFeatureOverlayContentBuilderParams(
    val currentPhase: JetpackFeatureRemovalPhase,
    val isRtl: Boolean = true,
    val feature: JetpackFeatureOverlayScreenType?
)
