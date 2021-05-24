package org.wordpress.android.ui.mysite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class SiteInfoBlockBuilderTest {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    private lateinit var siteInfoBlockBuilder: SiteInfoBlockBuilder

    @Before
    fun setUp() {
        siteInfoBlockBuilder = SiteInfoBlockBuilder(resourceProvider)
    }

    @Test
    fun `shows title quick start focus point when showUpdateSiteTitleFocusPoint is true`() {
        val buildSiteInfoBlock = buildSiteInfoBlock(
                showUpdateSiteTitleFocusPoint = true
        )

        assertThat(buildSiteInfoBlock.showTitleFocusPoint).isTrue()
    }

    @Test
    fun `hides title quick start focus point when showUpdateSiteTitleFocusPoint is false`() {
        val buildSiteInfoBlock = buildSiteInfoBlock(
                showUpdateSiteTitleFocusPoint = false
        )

        assertThat(buildSiteInfoBlock.showTitleFocusPoint).isFalse()
    }

    @Test
    fun `shows icon quick start focus point when showUploadSiteIconFocusPoint is true`() {
        val buildSiteInfoBlock = buildSiteInfoBlock(
                showUploadSiteIconFocusPoint = true
        )

        assertThat(buildSiteInfoBlock.showIconFocusPoint).isTrue()
    }

    @Test
    fun `hides icon quick start focus point when showUploadSiteIconFocusPoint is false`() {
        val buildSiteInfoBlock = buildSiteInfoBlock(
                showUploadSiteIconFocusPoint = false
        )

        assertThat(buildSiteInfoBlock.showIconFocusPoint).isFalse()
    }

    private fun buildSiteInfoBlock(
        showUpdateSiteTitleFocusPoint: Boolean = false,
        showUploadSiteIconFocusPoint: Boolean = false
    ) =
            siteInfoBlockBuilder.buildSiteInfoBlock(site,
                    showUploadSiteIconFocusPoint, {}, {}, {}, {},
                    showUpdateSiteTitleFocusPoint = showUpdateSiteTitleFocusPoint,
                    showUploadSiteIconFocusPoint = showUploadSiteIconFocusPoint
            )
}
