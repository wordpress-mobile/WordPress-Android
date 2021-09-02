package org.wordpress.android.ui.mysite.cards.siteinfo

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class SiteInfoCardBuilderTest {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    private lateinit var siteInfoCardBuilder: SiteInfoCardBuilder

    @Before
    fun setUp() {
        siteInfoCardBuilder = SiteInfoCardBuilder(resourceProvider)
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
    ) = siteInfoCardBuilder.buildSiteInfoCard(site,
            showUploadSiteIconFocusPoint, {}, {}, {}, {},
            showUpdateSiteTitleFocusPoint = showUpdateSiteTitleFocusPoint,
            showUploadSiteIconFocusPoint = showUploadSiteIconFocusPoint
    )
}
