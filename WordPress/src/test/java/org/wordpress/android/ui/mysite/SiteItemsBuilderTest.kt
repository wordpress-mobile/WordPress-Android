package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.themes.ThemeBrowserUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.SiteUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class SiteItemsBuilderTest {
    @Mock lateinit var siteUtilsWrapper: SiteUtilsWrapper
    @Mock lateinit var themeBrowserUtils: ThemeBrowserUtils
    @Mock lateinit var siteModel: SiteModel
    private lateinit var siteItemsBuilder: SiteItemsBuilder

    @Before
    fun setUp() {
        siteItemsBuilder = SiteItemsBuilder(siteUtilsWrapper, themeBrowserUtils)
    }

    @Test
    fun `adds publish, external header with themes inaccessible and site cannot manage, list users and is WPCom`() {
        whenever(themeBrowserUtils.isAccessible(siteModel)).thenReturn(false)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(false)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(true)
        whenever(siteModel.hasCapabilityListUsers).thenReturn(false)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }

    @Test
    fun `adds look and feel header when themes accessible`() {
        whenever(themeBrowserUtils.isAccessible(siteModel)).thenReturn(false)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(false)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_look_and_feel)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }

    @Test
    fun `adds configuration header when site can manage options`() {
        whenever(themeBrowserUtils.isAccessible(siteModel)).thenReturn(false)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(true)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(true)
        whenever(siteModel.hasCapabilityListUsers).thenReturn(false)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_configuration)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }

    @Test
    fun `adds configuration header when site can list users`() {
        whenever(themeBrowserUtils.isAccessible(siteModel)).thenReturn(false)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(false)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(true)
        whenever(siteModel.hasCapabilityListUsers).thenReturn(true)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_configuration)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }

    @Test
    fun `adds configuration header when site is not WPCom`() {
        whenever(themeBrowserUtils.isAccessible(siteModel)).thenReturn(false)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(false)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(siteModel)).thenReturn(false)
        whenever(siteModel.hasCapabilityListUsers).thenReturn(false)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_configuration)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }
}
