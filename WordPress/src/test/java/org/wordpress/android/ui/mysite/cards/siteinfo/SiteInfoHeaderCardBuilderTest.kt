package org.wordpress.android.ui.mysite.cards.siteinfo

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner.Silent::class)
class SiteInfoHeaderCardBuilderTest {
    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var site: SiteModel

    private lateinit var siteInfoHeaderCardBuilder: SiteInfoHeaderCardBuilder

    @Before
    fun setUp() {
        siteInfoHeaderCardBuilder = SiteInfoHeaderCardBuilder(resourceProvider)
    }

    @Test
    fun `when site info card is built, then card is returned`() {
        whenever(site.url).thenReturn("https://example.com")
        whenever(site.name).thenReturn("Test Site")
        whenever(site.iconUrl).thenReturn("")

        val buildSiteInfoCard = buildSiteInfoCard()

        assertThat(buildSiteInfoCard).isNotNull()
    }

    @Test
    fun `when site icon is loading, then progress is shown`() {
        whenever(site.url).thenReturn("https://example.com")
        whenever(site.name).thenReturn("Test Site")
        whenever(site.iconUrl).thenReturn("")

        val buildSiteInfoCard = buildSiteInfoCard(showSiteIconProgressBar = true)

        assertThat(buildSiteInfoCard.iconState).isInstanceOf(SiteInfoHeaderCard.IconState.Progress::class.java)
    }

    private fun buildSiteInfoCard(
        showSiteIconProgressBar: Boolean = false
    ): SiteInfoHeaderCard {
        return siteInfoHeaderCardBuilder.buildSiteInfoCard(
            SiteInfoCardBuilderParams(
                site = site,
                showSiteIconProgressBar = showSiteIconProgressBar,
                titleClick = {},
                iconClick = {},
                urlClick = {},
                switchSiteClick = {}
            )
        )
    }
}
