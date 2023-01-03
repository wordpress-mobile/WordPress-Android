package org.wordpress.android.ui.mysite.cards.siteinfo

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class SiteInfoHeaderCardBuilderTest {
    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var site: SiteModel

    @Mock
    lateinit var quickStartRepository: QuickStartRepository

    @Mock
    lateinit var quickStartType: QuickStartType
    private lateinit var siteInfoHeaderCardBuilder: SiteInfoHeaderCardBuilder

    @Before
    fun setUp() {
        whenever(quickStartRepository.quickStartType).thenReturn(quickStartType)
        siteInfoHeaderCardBuilder = SiteInfoHeaderCardBuilder(resourceProvider, quickStartRepository)
    }

    @Test
    fun `shows title quick start focus point when showUpdateSiteTitleFocusPoint is true`() {
        val buildSiteInfoCard = buildSiteInfoCard(
            showUpdateSiteTitleFocusPoint = true
        )

        assertThat(buildSiteInfoCard.showTitleFocusPoint).isTrue()
    }

    @Test
    fun `hides title quick start focus point when showUpdateSiteTitleFocusPoint is false`() {
        val buildSiteInfoCard = buildSiteInfoCard(
            showUpdateSiteTitleFocusPoint = false
        )

        assertThat(buildSiteInfoCard.showTitleFocusPoint).isFalse()
    }

    @Test
    fun `shows icon quick start focus point when showUploadSiteIconFocusPoint is true`() {
        val buildSiteInfoCard = buildSiteInfoCard(
            showUploadSiteIconFocusPoint = true
        )

        assertThat(buildSiteInfoCard.showIconFocusPoint).isTrue()
    }

    @Test
    fun `hides icon quick start focus point when showUploadSiteIconFocusPoint is false`() {
        val buildSiteInfoCard = buildSiteInfoCard(
            showUploadSiteIconFocusPoint = false
        )

        assertThat(buildSiteInfoCard.showIconFocusPoint).isFalse()
    }

    @Test
    fun `given new site QS + View Site active task, when card built, then showSubtitleFocusPoint is true`() {
        val buildSiteInfoCard = buildSiteInfoCard(
            showViewSiteFocusPoint = true,
            isNewSiteQuickStart = true
        )

        assertThat(buildSiteInfoCard.showSubtitleFocusPoint).isTrue
    }

    @Test
    fun `given new site QS + View Site not active task, when card built, then showSubtitleFocusPoint is false`() {
        val buildSiteInfoCard = buildSiteInfoCard(
            showViewSiteFocusPoint = false,
            isNewSiteQuickStart = true
        )

        assertThat(buildSiteInfoCard.showSubtitleFocusPoint).isFalse
    }

    @Test
    fun `given existing site QS + View Site active task, when card built, then showSubtitleFocusPoint is true`() {
        val buildSiteInfoCard = buildSiteInfoCard(
            showViewSiteFocusPoint = true,
            isNewSiteQuickStart = false
        )

        assertThat(buildSiteInfoCard.showSubtitleFocusPoint).isTrue
    }

    @Test
    fun `given existing site QS + View Site not active task, when card built, then showSubtitleFocusPoint is false`() {
        val buildSiteInfoCard = buildSiteInfoCard(
            showViewSiteFocusPoint = false,
            isNewSiteQuickStart = false
        )

        assertThat(buildSiteInfoCard.showSubtitleFocusPoint).isFalse
    }

    private fun buildSiteInfoCard(
        showUpdateSiteTitleFocusPoint: Boolean = false,
        showViewSiteFocusPoint: Boolean = false,
        showUploadSiteIconFocusPoint: Boolean = false,
        isNewSiteQuickStart: Boolean = true
    ): SiteInfoHeaderCard {
        val viewSiteTask = if (isNewSiteQuickStart) {
            QuickStartNewSiteTask.VIEW_SITE
        } else {
            QuickStartExistingSiteTask.VIEW_SITE
        }
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_VIEW_SITE_LABEL))
            .thenReturn(viewSiteTask)
        return siteInfoHeaderCardBuilder.buildSiteInfoCard(
            SiteInfoCardBuilderParams(
                site = site,
                showSiteIconProgressBar = showUploadSiteIconFocusPoint,
                titleClick = {},
                iconClick = {},
                urlClick = {},
                switchSiteClick = {},
                setActiveTask(
                    showUpdateSiteTitleFocusPoint,
                    showViewSiteFocusPoint,
                    showUploadSiteIconFocusPoint,
                    viewSiteTask
                )
            )
        )
    }

    private fun setActiveTask(
        showUpdateSiteTitleFocusPoint: Boolean,
        showViewSiteFocusPoint: Boolean,
        showUploadSiteIconFocusPoint: Boolean,
        viewSiteTask: QuickStartTask
    ): QuickStartTask? {
        return when {
            showUpdateSiteTitleFocusPoint -> QuickStartNewSiteTask.UPDATE_SITE_TITLE
            showViewSiteFocusPoint -> viewSiteTask
            showUploadSiteIconFocusPoint -> QuickStartNewSiteTask.UPLOAD_SITE_ICON
            else -> null
        }
    }
}
