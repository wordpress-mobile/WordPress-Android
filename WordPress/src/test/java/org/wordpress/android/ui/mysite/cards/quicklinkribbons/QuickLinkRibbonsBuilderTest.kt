package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbons
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonsBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction

@InternalCoroutinesApi
class QuickLinkRibbonsBuilderTest : BaseUnitTest() {
    @Mock lateinit var siteModel: SiteModel
    private lateinit var builder: QuickLinkRibbonsBuilder

    private val onStatsClick: () -> Unit = {}
    private val onPostsClick: () -> Unit = {}
    private val onPagesClick: () -> Unit = {}
    private val onMediaClick: () -> Unit = {}

    @Before
    fun setUp() {
        builder = QuickLinkRibbonsBuilder()
    }

    /* ACTION CLICKS */
    @Test
    fun `when card is built, then ribbon click are set on the card`() {
        val quickLinkRibbons = buildQuickLinkRibbons()

        assertThat(quickLinkRibbons.onPagesClick).isEqualTo(ListItemInteraction.create(onPagesClick))
        assertThat(quickLinkRibbons.onPostsClick).isEqualTo(ListItemInteraction.create(onPostsClick))
        assertThat(quickLinkRibbons.onMediaClick).isEqualTo(ListItemInteraction.create(onMediaClick))
        assertThat(quickLinkRibbons.onStatsClick).isEqualTo(ListItemInteraction.create(onStatsClick))
    }

    private fun buildQuickLinkRibbons(
        showPages: Boolean = true
    ): QuickLinkRibbons {
        setShowPages(showPages)
        return builder.build(
                QuickLinkRibbonsBuilderParams(
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
