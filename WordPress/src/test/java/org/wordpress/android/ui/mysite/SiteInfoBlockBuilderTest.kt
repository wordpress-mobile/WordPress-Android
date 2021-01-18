package org.wordpress.android.ui.mysite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
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
    fun `shows quick start focus point when active task is update site`() {
        val activeTask = UPDATE_SITE_TITLE

        val buildSiteInfoBlock = siteInfoBlockBuilder.buildSiteInfoBlock(site, false, {}, {}, {}, {}, activeTask)

        assertThat(buildSiteInfoBlock.showTitleFocusPoint).isTrue()
    }

    @Test
    fun `hides quick start focus point when active task is different`() {
        for (activeTask in QuickStartTask.values()) {
            if (activeTask != UPDATE_SITE_TITLE) {
                val buildSiteInfoBlock = siteInfoBlockBuilder.buildSiteInfoBlock(
                        site,
                        false,
                        {},
                        {},
                        {},
                        {},
                        activeTask
                )

                assertThat(buildSiteInfoBlock.showTitleFocusPoint).isFalse()
            }
        }
    }

    @Test
    fun `hides quick start focus point when active task is null`() {
        val buildSiteInfoBlock = siteInfoBlockBuilder.buildSiteInfoBlock(site, false, {}, {}, {}, {}, null)

        assertThat(buildSiteInfoBlock.showTitleFocusPoint).isFalse()
    }
}
