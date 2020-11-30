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
import org.wordpress.android.ui.utils.UiString.UiStringRes

@RunWith(MockitoJUnitRunner::class)
class SiteItemsBuilderTest {
    @Mock lateinit var siteCategoryItemBuilder: SiteCategoryItemBuilder
    @Mock lateinit var siteListItemBuilder: SiteListItemBuilder
    @Mock lateinit var siteModel: SiteModel
    private lateinit var siteItemsBuilder: SiteItemsBuilder

    @Before
    fun setUp() {
        siteItemsBuilder = SiteItemsBuilder(siteCategoryItemBuilder, siteListItemBuilder)
    }

    @Test
    fun `adds only publish and external header when others are null`() {
        setupHeaders(addJetpackHeader = false, addLookAndFeelHeader = false, addConfigurationHeader = false)
        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }

    @Test
    fun `adds jetpack header when not null`() {
        setupHeaders(addJetpackHeader = true)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_jetpack)),
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }

    @Test
    fun `adds look and feel header when not null`() {
        setupHeaders(addLookAndFeelHeader = true)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_look_and_feel)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }

    @Test
    fun `adds configuration header when not null`() {
        setupHeaders(addConfigurationHeader = true)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_configuration)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }

    @Test
    fun `adds all options in correct order when present`() {
        setupHeaders(addJetpackHeader = true, addLookAndFeelHeader = true, addConfigurationHeader = true)

        val buildSiteItems = siteItemsBuilder.buildSiteItems(siteModel)

        assertThat(buildSiteItems).containsExactly(
                CategoryHeader(UiStringRes(R.string.my_site_header_jetpack)),
                CategoryHeader(UiStringRes(R.string.my_site_header_publish)),
                CategoryHeader(UiStringRes(R.string.my_site_header_look_and_feel)),
                CategoryHeader(UiStringRes(R.string.my_site_header_configuration)),
                CategoryHeader(UiStringRes(R.string.my_site_header_external))
        )
    }

    private fun setupHeaders(
        addJetpackHeader: Boolean = false,
        addLookAndFeelHeader: Boolean = false,
        addConfigurationHeader: Boolean = false
    ) {
        if (addJetpackHeader) {
            whenever(siteCategoryItemBuilder.buildJetpackCategoryIfAvailable(siteModel)).thenReturn(
                    CategoryHeader(
                            UiStringRes(R.string.my_site_header_jetpack)
                    )
            )
        }
        if (addLookAndFeelHeader) {
            whenever(siteCategoryItemBuilder.buildLookAndFeelHeaderIfAvailable(siteModel)).thenReturn(
                    CategoryHeader(
                            UiStringRes(R.string.my_site_header_look_and_feel)
                    )
            )
        }
        if (addConfigurationHeader) {
            whenever(siteCategoryItemBuilder.buildConfigurationHeaderIfAvailable(siteModel)).thenReturn(
                    CategoryHeader(
                            UiStringRes(R.string.my_site_header_configuration)
                    )
            )
        }
    }
}
