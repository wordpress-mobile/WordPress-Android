package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction

@InternalCoroutinesApi
class QuickLinkRibbonBuilderTest : BaseUnitTest() {
    @Mock lateinit var siteModel: SiteModel
    private lateinit var builder: QuickLinkRibbonBuilder

    private val onStatsClick: () -> Unit = {}
    private val onPostsClick: () -> Unit = {}
    private val onPagesClick: () -> Unit = {}
    private val onMediaClick: () -> Unit = {}

    @Before
    fun setUp() {
        builder = QuickLinkRibbonBuilder()
    }

    /* ACTION CLICKS */
    @Test
    fun `when card is built, then ribbon click are set on the card`() {
        val quickLinkRibbon = buildQuickLinkRibbon()

        assertThat(quickLinkRibbon.onPagesClick).isEqualTo(ListItemInteraction.create(onPagesClick))
        assertThat(quickLinkRibbon.onPostsClick).isEqualTo(ListItemInteraction.create(onPostsClick))
        assertThat(quickLinkRibbon.onMediaClick).isEqualTo(ListItemInteraction.create(onMediaClick))
        assertThat(quickLinkRibbon.onStatsClick).isEqualTo(ListItemInteraction.create(onStatsClick))
    }

    private fun buildQuickLinkRibbon(
        showPages: Boolean = true
    ): QuickLinkRibbon {
        setShowPages(showPages)
        return builder.build(
                QuickLinkRibbonBuilderParams(
                        siteModel,
                        onPagesClick,
                        onPostsClick,
                        onMediaClick,
                        onStatsClick
                )
        )
    }

    private fun setShowPages(showPages: Boolean) {
        whenever(siteModel.isSelfHostedAdmin).thenReturn(showPages)
    }
}
