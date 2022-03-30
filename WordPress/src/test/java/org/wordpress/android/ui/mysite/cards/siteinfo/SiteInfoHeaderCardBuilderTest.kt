package org.wordpress.android.ui.mysite.cards.siteinfo

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class SiteInfoHeaderCardBuilderTest {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    private lateinit var siteInfoHeaderCardBuilder: SiteInfoHeaderCardBuilder

    @Before
    fun setUp() {
        siteInfoHeaderCardBuilder = SiteInfoHeaderCardBuilder(resourceProvider)
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

    private fun buildSiteInfoCard(
        showUpdateSiteTitleFocusPoint: Boolean = false,
        showUploadSiteIconFocusPoint: Boolean = false
    ) = siteInfoHeaderCardBuilder.buildSiteInfoCard(
            SiteInfoCardBuilderParams(
                    site = site,
                    showSiteIconProgressBar = showUploadSiteIconFocusPoint,
                    titleClick = {},
                    iconClick = {},
                    urlClick = {},
                    switchSiteClick = {},
                    setActiveTask(showUpdateSiteTitleFocusPoint, showUploadSiteIconFocusPoint)
            )
    )

    private fun setActiveTask(
        showUpdateSiteTitleFocusPoint: Boolean,
        showUploadSiteIconFocusPoint: Boolean
    ): QuickStartTask? {
        return when {
            showUpdateSiteTitleFocusPoint -> QuickStartTask.UPDATE_SITE_TITLE
            showUploadSiteIconFocusPoint -> QuickStartTask.UPLOAD_SITE_ICON
            else -> null
        }
    }
}
