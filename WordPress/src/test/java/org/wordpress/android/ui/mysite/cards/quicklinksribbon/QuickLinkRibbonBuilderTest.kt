package org.wordpress.android.ui.mysite.cards.quicklinksribbon

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.utils.ListItemInteraction

@ExperimentalCoroutinesApi
class QuickLinkRibbonBuilderTest : BaseUnitTest() {
    @Mock
    lateinit var siteModel: SiteModel

    @Mock
    lateinit var quickStartRepository: QuickStartRepository

    @Mock
    lateinit var quickStartType: QuickStartType
    private lateinit var builder: QuickLinkRibbonBuilder

    private val onStatsClick: () -> Unit = {}
    private val onPostsClick: () -> Unit = {}
    private val onPagesClick: () -> Unit = {}
    private val onMediaClick: () -> Unit = {}

    @Before
    fun setUp() {
        whenever(quickStartRepository.quickStartType).thenReturn(quickStartType)
        builder = QuickLinkRibbonBuilder(quickStartRepository)
    }

    @Test
    fun `given site does have capabilities, when ribbon is built, then pages item is not built`() {
        val quickLinkRibbon = buildQuickLinkRibbon(showPages = false)

        assertThat(quickLinkRibbon.quickLinkRibbonItems.size).isEqualTo(3)
        assertThat(quickLinkRibbon.quickLinkRibbonItems[0].label).isEqualTo(R.string.stats)
        assertThat(quickLinkRibbon.quickLinkRibbonItems[1].label).isEqualTo(R.string.posts)
        assertThat(quickLinkRibbon.quickLinkRibbonItems[2].label).isEqualTo(R.string.media)
    }

    /* ACTION CLICKS */
    @Test
    fun `when card is built, then ribbon click are set on the card`() {
        val quickLinkRibbon = buildQuickLinkRibbon()

        assertThat(quickLinkRibbon.quickLinkRibbonItems[0].onClick).isEqualTo(ListItemInteraction.create(onStatsClick))
        assertThat(quickLinkRibbon.quickLinkRibbonItems[1].onClick).isEqualTo(ListItemInteraction.create(onPostsClick))
        assertThat(quickLinkRibbon.quickLinkRibbonItems[2].onClick).isEqualTo(ListItemInteraction.create(onPagesClick))
        assertThat(quickLinkRibbon.quickLinkRibbonItems[3].onClick).isEqualTo(ListItemInteraction.create(onMediaClick))
    }

    /* FOCUS POINT*/
    @Test
    fun `given new site QS + stats active task, when card is built, then stats focus point should be true`() {
        val quickLinkRibbon = buildQuickLinkRibbon(showStatsFocusPoint = true, isNewSiteQuickStart = true)

        assertThat(quickLinkRibbon.quickLinkRibbonItems[0].showFocusPoint).isEqualTo(true)
        assertThat(quickLinkRibbon.showStatsFocusPoint).isEqualTo(true)
    }

    @Test
    fun `given existing site QS + stats active task, when card is built, then stats focus point should be true`() {
        val quickLinkRibbon = buildQuickLinkRibbon(showStatsFocusPoint = true, isNewSiteQuickStart = false)

        assertThat(quickLinkRibbon.quickLinkRibbonItems[0].showFocusPoint).isEqualTo(true)
        assertThat(quickLinkRibbon.showStatsFocusPoint).isEqualTo(true)
    }

    @Test
    fun `given pages active task, when card is built, then pages focus point should be true`() {
        val quickLinkRibbon = buildQuickLinkRibbon(showPagesFocusPoint = true)

        assertThat(quickLinkRibbon.quickLinkRibbonItems[2].showFocusPoint).isEqualTo(true)
        assertThat(quickLinkRibbon.showPagesFocusPoint).isEqualTo(true)
    }

    @Test
    fun `given enable focus point is false, when card is built, then active focus point should false`() {
        val quickLinkRibbon = buildQuickLinkRibbon(showPagesFocusPoint = true, enableFocusPoints = false)

        assertThat(quickLinkRibbon.quickLinkRibbonItems[2].showFocusPoint).isEqualTo(false)
        assertThat(quickLinkRibbon.showPagesFocusPoint).isEqualTo(false)
        assertThat(quickLinkRibbon.activeQuickStartItem).isEqualTo(false)
    }

    private fun buildQuickLinkRibbon(
        showPages: Boolean = true,
        showPagesFocusPoint: Boolean = false,
        showStatsFocusPoint: Boolean = false,
        enableFocusPoints: Boolean = true,
        isNewSiteQuickStart: Boolean = true
    ): QuickLinkRibbon {
        setShowPages(showPages)
        val checkStatsTask = if (isNewSiteQuickStart) {
            QuickStartNewSiteTask.CHECK_STATS
        } else {
            QuickStartExistingSiteTask.CHECK_STATS
        }
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL))
            .thenReturn(checkStatsTask)

        return builder.build(
            QuickLinkRibbonBuilderParams(
                siteModel,
                onPagesClick,
                onPostsClick,
                onMediaClick,
                onStatsClick,
                setActiveTask(showPagesFocusPoint, showStatsFocusPoint, checkStatsTask),
                enableFocusPoints = enableFocusPoints
            )
        )
    }

    private fun setShowPages(showPages: Boolean) {
        whenever(siteModel.isSelfHostedAdmin).thenReturn(showPages)
    }

    private fun setActiveTask(
        showPages: Boolean,
        showStats: Boolean,
        checkStatsTask: QuickStartTask
    ): QuickStartTask? {
        return when {
            showPages -> QuickStartNewSiteTask.REVIEW_PAGES
            showStats -> checkStatsTask
            else -> null
        }
    }
}
