package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
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

    @Test
    fun `given site does have capabilities, when ribbon is built, then pages item is not built`() {
        val quickLinkRibbon = buildQuickLinkRibbon(showPages = false)

        assertThat(quickLinkRibbon.quickLinkRibbonItems.size).isEqualTo(3)
        assertThat(quickLinkRibbon.quickLinkRibbonItems[0].label).isEqualTo(R.string.posts)
        assertThat(quickLinkRibbon.quickLinkRibbonItems[1].label).isEqualTo(R.string.media)
        assertThat(quickLinkRibbon.quickLinkRibbonItems[2].label).isEqualTo(R.string.stats)
    }

    /* ACTION CLICKS */
    @Test
    fun `when card is built, then ribbon click are set on the card`() {
        val quickLinkRibbon = buildQuickLinkRibbon()

        assertThat(quickLinkRibbon.quickLinkRibbonItems[0].onClick).isEqualTo(ListItemInteraction.create(onPagesClick))
        assertThat(quickLinkRibbon.quickLinkRibbonItems[1].onClick).isEqualTo(ListItemInteraction.create(onPostsClick))
        assertThat(quickLinkRibbon.quickLinkRibbonItems[2].onClick).isEqualTo(ListItemInteraction.create(onMediaClick))
        assertThat(quickLinkRibbon.quickLinkRibbonItems[3].onClick).isEqualTo(ListItemInteraction.create(onStatsClick))
    }

    /* FOCUS POINT*/
    @Test
    fun `given stats active task, when card is built, then stats focus point should be true`() {
        val quickLinkRibbon = buildQuickLinkRibbon(showStatsFocusPoint = true)

        assertThat(quickLinkRibbon.quickLinkRibbonItems[3].showFocusPoint).isEqualTo(true)
        assertThat(quickLinkRibbon.showStatsFocusPoint).isEqualTo(true)
    }

    @Test
    fun `given pages active task, when card is built, then pages focus point should be true`() {
        val quickLinkRibbon = buildQuickLinkRibbon(showPagesFocusPoint = true)

        assertThat(quickLinkRibbon.quickLinkRibbonItems[0].showFocusPoint).isEqualTo(true)
        assertThat(quickLinkRibbon.showPagesFocusPoint).isEqualTo(true)
    }

    @Test
    fun `given enable focus point is false, when card is built, then active focus point should false`() {
        val quickLinkRibbon = buildQuickLinkRibbon(showPagesFocusPoint = true, enableFocusPoints = false)

        assertThat(quickLinkRibbon.quickLinkRibbonItems[0].showFocusPoint).isEqualTo(false)
        assertThat(quickLinkRibbon.showPagesFocusPoint).isEqualTo(false)
        assertThat(quickLinkRibbon.activeQuickStartItem).isEqualTo(false)
    }

    private fun buildQuickLinkRibbon(
        showPages: Boolean = true,
        showPagesFocusPoint: Boolean = false,
        showStatsFocusPoint: Boolean = false,
        enableFocusPoints: Boolean = true
    ): QuickLinkRibbon {
        setShowPages(showPages)
        return builder.build(
                QuickLinkRibbonBuilderParams(
                        siteModel,
                        onPagesClick,
                        onPostsClick,
                        onMediaClick,
                        onStatsClick,
                        setActiveTask(showPagesFocusPoint, showStatsFocusPoint),
                        enableFocusPoints = enableFocusPoints
                )
        )
    }

    private fun setShowPages(showPages: Boolean) {
        whenever(siteModel.isSelfHostedAdmin).thenReturn(showPages)
    }

    private fun setActiveTask(showPages: Boolean, showStats: Boolean): QuickStartStore.QuickStartTask? {
        return when {
            showPages -> QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
            showStats -> QuickStartStore.QuickStartTask.CHECK_STATS
            else -> null
        }
    }
}
