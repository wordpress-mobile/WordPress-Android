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
    fun `shows quick start focus point when showUpdateSiteTitleFocusPoint is true`() {
        val buildSiteInfoBlock = siteInfoBlockBuilder.buildSiteInfoBlock(site, false, {}, {}, {}, {},
                showUpdateSiteTitleFocusPoint = true
        )

        assertThat(buildSiteInfoBlock.showTitleFocusPoint).isTrue()
    }

    @Test
    fun `hides quick start focus point when showUpdateSiteTitleFocusPoint is false`() {
        val buildSiteInfoBlock = siteInfoBlockBuilder.buildSiteInfoBlock(
                site,
                false,
                {},
                {},
                {},
                {},
                showUpdateSiteTitleFocusPoint = false
        )

        assertThat(buildSiteInfoBlock.showTitleFocusPoint).isFalse()
    }
}
