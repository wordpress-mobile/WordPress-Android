package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.R
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseSelfHostedUsers
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import javax.inject.Inject
import javax.inject.Singleton

// Format 2020-12-22
const val JETPACK_OVERLAY_ORIGINAL_DATE_FORMAT = "yyyy-MM-dd"

// Format Decemeber 22, 2020
const val JETPACK_OVERLAY_TARGET_DATE_FORMAT = "MMMM dd, yyyy"

@Singleton
class JetpackFeatureOverlayContentBuilder @Inject constructor(
    private val htmlMessageUtils: HtmlMessageUtils,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) {
    fun build(params: JetpackFeatureOverlayContentBuilderParams): JetpackFeatureOverlayUIState {
        return when (params.currentPhase) {
            is PhaseOne -> getStateForPhaseOne(params, params.feature!!)
            is PhaseTwo -> getStateForPhaseTwo(params)
            is PhaseThree -> getStateForPhaseThree(params)
            else  -> TODO()
        }
    }

    private fun getStateForPhaseThree(
        params: JetpackFeatureOverlayContentBuilderParams
    ): JetpackFeatureOverlayUIState {
        val componentVisibility = JetpackFeatureOverlayComponentVisibility.PhaseThree()
        val content = when (params.feature!!) {
            JetpackFeatureOverlayScreenType.STATS -> getStateForPhaseTwoStats(
                params,
                params.jpDeadlineDate,
                params.phaseThreeBlogPostLink
            )
            JetpackFeatureOverlayScreenType.NOTIFICATIONS -> getStateForPhaseTwoNotifications(
                params,
                params.jpDeadlineDate,
                params.phaseThreeBlogPostLink
            )
            JetpackFeatureOverlayScreenType.READER -> getStateForPhaseTwoReader(
                params,
                params.jpDeadlineDate,
                params.phaseThreeBlogPostLink
            )
        }.copy(
            migrationText = R.string.wp_jetpack_feature_removal_overlay_migration_helper_text
        )
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    private fun getStateForPhaseTwo(
        params: JetpackFeatureOverlayContentBuilderParams
    ): JetpackFeatureOverlayUIState {
        val componentVisibility = JetpackFeatureOverlayComponentVisibility.PhaseTwo()
        val content = when (params.feature!!) {
            JetpackFeatureOverlayScreenType.STATS -> getStateForPhaseTwoStats(
                params,
                params.jpDeadlineDate,
                params.phaseTwoBlogPostLink
            )
            JetpackFeatureOverlayScreenType.NOTIFICATIONS -> getStateForPhaseTwoNotifications(
                params,
                params.jpDeadlineDate,
                params.phaseTwoBlogPostLink
            )
            JetpackFeatureOverlayScreenType.READER -> getStateForPhaseTwoReader(
                params,
                params.jpDeadlineDate,
                params.phaseTwoBlogPostLink
            )
        }
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    private fun getStateForPhaseTwoStats(
        params: JetpackFeatureOverlayContentBuilderParams,
        jpDeadlineDate: String?,
        blogPostLink: String?
    ): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
            illustration = if (params.isRtl) R.raw.jp_stats_rtl else R.raw.jp_stats_left,
            title = R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_title_stats,
            caption = getCaptionForPhaseTwoAndThree(jpDeadlineDate),
            migrationInfoText = if (!blogPostLink.isNullOrEmpty())
                R.string.wp_jetpack_feature_removal_overlay_learn_more_migration_text else null,
            migrationInfoUrl = blogPostLink,
            primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
            secondaryButtonText = R.string.wp_jetpack_continue_to_stats
        )
    }

    private fun getStateForPhaseTwoReader(
        params: JetpackFeatureOverlayContentBuilderParams,
        jpDeadlineDate: String?,
        blogPostLink: String?
    ): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
            illustration = if (params.isRtl) R.raw.jp_reader_rtl else R.raw.jp_reader_left,
            title = R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_title_reader,
            caption = getCaptionForPhaseTwoAndThree(jpDeadlineDate),
            migrationInfoText = if (!blogPostLink.isNullOrEmpty())
                R.string.wp_jetpack_feature_removal_overlay_learn_more_migration_text else null,
            migrationInfoUrl = blogPostLink,
            primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
            secondaryButtonText = R.string.wp_jetpack_continue_to_reader
        )
    }

    private fun getStateForPhaseTwoNotifications(
        params: JetpackFeatureOverlayContentBuilderParams,
        jpDeadlineDate: String?,
        blogPostLink: String?
    ): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
            illustration = if (params.isRtl) R.raw.jp_notifications_rtl else R.raw.jp_notifications_left,
            title = R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_title_notifications,
            caption = getCaptionForPhaseTwoAndThree(jpDeadlineDate),
            migrationInfoText = if (!blogPostLink.isNullOrEmpty())
                R.string.wp_jetpack_feature_removal_overlay_learn_more_migration_text else null,
            migrationInfoUrl = blogPostLink,
            primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
            secondaryButtonText = R.string.wp_jetpack_continue_to_notifications
        )
    }

    private fun getCaptionForPhaseTwoAndThree(jpDeadlineDate: String?): UiString {
        return if (jpDeadlineDate.isNullOrEmpty()) getPhaseTwoAndThreeCaptionWithoutDeadline()
        else {
            val deadlineDate = dateTimeUtilsWrapper.convertDateFormat(
                jpDeadlineDate,
                JETPACK_OVERLAY_ORIGINAL_DATE_FORMAT,
                JETPACK_OVERLAY_TARGET_DATE_FORMAT
            )
            if (deadlineDate.isNullOrEmpty())
                return getPhaseTwoAndThreeCaptionWithoutDeadline()
            getPhaseTwoAndThreeCaptionWithDeadline(deadlineDate)
        }
    }

    private fun getPhaseTwoAndThreeCaptionWithDeadline(jpDeadlineDate: String): UiString {
        return UiStringText(
            htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_description_with_deadline,
                "<b>$jpDeadlineDate</b>"
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
            primaryButtonText = R.string.wp_jetpack_feature_removal_site_creation_overlay_phase_two_primary_button,
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
            PhaseThree -> JetpackFeatureOverlayComponentVisibility.FeatureCollectionPhase.PhaseThree()
            PhaseFour -> JetpackFeatureOverlayComponentVisibility.FeatureCollectionPhase.PhaseFour()
            PhaseNewUsers -> JetpackFeatureOverlayComponentVisibility.FeatureCollectionPhase.PhaseNewUsers()
            else -> JetpackFeatureOverlayComponentVisibility.FeatureCollectionPhase.Final()
        }
        val content = getContentForFeatureCollection(isRtl, blogPostLink, currentPhase)
        return JetpackFeatureOverlayUIState(componentVisibility, content)
    }

    private fun getContentForFeatureCollection(
        isRtl: Boolean,
        blogPostLink: String?,
        currentPhase: JetpackFeatureRemovalPhase
    ): JetpackFeatureOverlayContent {
        return when(currentPhase) {
            is PhaseThree ->getJetpackFeatureOverlayContentForPhaseThree(isRtl, blogPostLink)
            is PhaseFour ->getJetpackFeatureOverlayContentForPhaseFour(isRtl, blogPostLink)
            is PhaseNewUsers -> getJetpackFeatureOverlayContentForNewUsers(isRtl)
            is PhaseSelfHostedUsers -> getJetpackFeatureOverlayContentForSelfHostedUsers(isRtl)
            else -> {
                throw IllegalStateException("Invalid phase for feature collection overlay")
            }
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

    private fun getJetpackFeatureOverlayContentForPhaseThree(
        isRtl: Boolean,
        blogPostLink: String?
    ) = JetpackFeatureOverlayContent(
        illustration = if (isRtl) R.raw.jp_all_features_rtl else R.raw.jp_all_features_left,
        title = R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_title_all_features,
        caption = UiStringRes(R.string.wp_jetpack_feature_removal_overlay_phase_three_all_features_description),
        migrationText = R.string.wp_jetpack_feature_removal_overlay_migration_helper_text,
        migrationInfoText = if (!blogPostLink.isNullOrEmpty())
            R.string.wp_jetpack_feature_removal_overlay_learn_more_migration_text else null,
        migrationInfoUrl = blogPostLink,
        primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_the_jetpack_app,
        secondaryButtonText = R.string.wp_jetpack_feature_removal_overlay_continue_without_jetpack
    )
}

data class JetpackFeatureOverlayContentBuilderParams(
    val currentPhase: JetpackFeatureRemovalPhase,
    val isRtl: Boolean = true,
    val feature: JetpackFeatureOverlayScreenType?,
    val jpDeadlineDate: String? = null,
    val phaseTwoBlogPostLink: String? = null,
    val phaseThreeBlogPostLink: String? = null
)
