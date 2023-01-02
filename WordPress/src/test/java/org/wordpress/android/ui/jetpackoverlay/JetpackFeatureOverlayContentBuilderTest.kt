package org.wordpress.android.ui.jetpackoverlay

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType.STATS
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper

private const val PHASE_TWO_BLOG_POST_LINK = "www.jetpack.com"
private const val JP_DEADLINE_MESSAGE = "Stats, Reader, Notifications and other Jetpack powered features will " +
        "be removed from the WordPress app on <b> January 1, 2020 </b>"

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureOverlayContentBuilderTest {

    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock private lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var jetpackFeatureOverlayContentBuilder: JetpackFeatureOverlayContentBuilder

    @Before
    fun setup() {
        jetpackFeatureOverlayContentBuilder = JetpackFeatureOverlayContentBuilder(
                htmlMessageUtils,
                dateTimeUtilsWrapper
        )
    }

    @Test
    fun `given phase one started, when content is built, should return phase one overlay content`() {
        val phaseOneStats = jetpackFeatureOverlayContentBuilder.build(
                JetpackFeatureOverlayContentBuilderParams(
                        PhaseOne,
                        false,
                        STATS
                )
        )

        assertEquals(phaseOneStats.overlayContent, getPhaseOneStats())
    }

    @Suppress("MaximumLineLength")
    @Test
    fun `given phase two without remote field post link, when content is built, should return phase two overlay content`() {
        val phaseTwoStats = jetpackFeatureOverlayContentBuilder.build(
                JetpackFeatureOverlayContentBuilderParams(
                        PhaseTwo,
                        false,
                        STATS
                )
        )

        assertEquals(phaseTwoStats.overlayContent, getPhaseTwoStatsWithoutMigrationInfo())
    }

    @Suppress("MaximumLineLength")
    @Test
    fun `given phase two with remote field post link, when content is built, should return phase two overlay content`() {
        val phaseTwoStats = jetpackFeatureOverlayContentBuilder.build(
                JetpackFeatureOverlayContentBuilderParams(
                        PhaseTwo,
                        false,
                        STATS,
                        phaseTwoBlogPostLink = PHASE_TWO_BLOG_POST_LINK
                )
        )
        val actual = getPhaseTwoStatsWithoutMigrationInfo().copy(
                migrationInfoUrl = PHASE_TWO_BLOG_POST_LINK,
                migrationInfoText = R.string.wp_jetpack_feature_removal_overlay_learn_more_migration_text
        )

        assertEquals(phaseTwoStats.overlayContent, actual)
    }

    @Suppress("MaximumLineLength")
    @Test
    fun `given phase two with deadline, when content is built, should return phase two overlay content`() {
        val jpDeadlineDateFromRemoteConfig = "2020-01-01"
        val jpDeadlineDateToBeShownOnOverlay  = "January 1, 2020"
        val jpDeadlineDateToBeShownOnOverlayWithFormat  = "<b>January 1, 2020</b>"
        whenever(
                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                        R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_description_with_deadline,
                        jpDeadlineDateToBeShownOnOverlayWithFormat
                )
        ).thenReturn(JP_DEADLINE_MESSAGE)
        whenever(
                dateTimeUtilsWrapper.convertDateFormat(
                        jpDeadlineDateFromRemoteConfig,
                        JETPACK_OVERLAY_ORIGINAL_DATE_FORMAT,
                        JETPACK_OVERLAY_TARGET_DATE_FORMAT
                )
        ).thenReturn(jpDeadlineDateToBeShownOnOverlay)

        val actualOverlayUiState = jetpackFeatureOverlayContentBuilder.build(
                JetpackFeatureOverlayContentBuilderParams(
                        PhaseTwo,
                        false,
                        STATS,
                        jpDeadlineDate = jpDeadlineDateFromRemoteConfig
                )
        )
        val expectedOverlayContent = getPhaseTwoStatsWithoutMigrationInfo().copy(
                caption = UiStringText(JP_DEADLINE_MESSAGE)
        )

        assertEquals(expectedOverlayContent, actualOverlayUiState.overlayContent)
    }

    private fun getPhaseOneStats(): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = R.raw.jp_stats_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_one_title_stats,
                caption = UiStringRes(R.string.wp_jetpack_feature_removal_overlay_phase_one_description_stats),
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_stats
        )
    }

    private fun getPhaseTwoStatsWithoutMigrationInfo(): JetpackFeatureOverlayContent {
        return JetpackFeatureOverlayContent(
                illustration = R.raw.jp_stats_left,
                title = R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_title_stats,
                caption = UiStringRes(
                        R.string.wp_jetpack_feature_removal_overlay_phase_two_and_three_description_without_deadline
                ),
                primaryButtonText = R.string.wp_jetpack_feature_removal_overlay_switch_to_new_jetpack_app,
                secondaryButtonText = R.string.wp_jetpack_continue_to_stats
        )
    }
}
