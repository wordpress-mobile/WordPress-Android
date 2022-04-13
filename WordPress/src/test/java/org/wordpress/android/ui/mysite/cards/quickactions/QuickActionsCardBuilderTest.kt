package org.wordpress.android.ui.mysite.cards.quickactions

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.utils.UiString.UiStringRes

@InternalCoroutinesApi
class QuickActionsCardBuilderTest : BaseUnitTest() {
    @Mock lateinit var siteModel: SiteModel
    private lateinit var builder: QuickActionsCardBuilder

    private val onStatsClick: () -> Unit = {}
    private val onPostsClick: () -> Unit = {}
    private val onPagesClick: () -> Unit = {}
    private val onMediaClick: () -> Unit = {}

    @Before
    fun setUp() {
        builder = QuickActionsCardBuilder()
    }

    /* TITLE */

    @Test
    fun `when toolbar is built, then title exists`() {
        val quickActionsBlock = buildQuickActionsCard()

        assertThat(quickActionsBlock.title).isEqualTo(UiStringRes(R.string.my_site_quick_actions_title))
    }

    /* ACTION CLICKS */
    @Test
    fun `when card is built, then action item click are set on the card`() {
        val quickActionsCard = buildQuickActionsCard()

        assertThat(quickActionsCard.onStatsClick).isNotNull
        assertThat(quickActionsCard.onPagesClick).isNotNull
        assertThat(quickActionsCard.onPostsClick).isNotNull
        assertThat(quickActionsCard.onMediaClick).isNotNull
    }

    /* FOCUS POINT*/
    @Test
    fun `given stats active task, when card is built, then stats focus point should be true`() {
        val quickActionsCard = buildQuickActionsCard(showStatsFocusPoint = true)

        assertThat(quickActionsCard.showStatsFocusPoint).isEqualTo(true)
    }

    @Test
    fun `given pages active task, when card is built, then pages focus point should be true`() {
        val quickActionsCard = buildQuickActionsCard(showPagesFocusPoint = true)

        assertThat(quickActionsCard.showPagesFocusPoint).isEqualTo(true)
    }

    @Test
    fun `given enable focus point is false, when card is built, then active focus point should false`() {
        val quickActionsCard = buildQuickActionsCard(showPagesFocusPoint = true, enableFocusPoints = false)

        assertThat(quickActionsCard.showPagesFocusPoint).isEqualTo(false)
        assertThat(quickActionsCard.activeQuickStartItem).isEqualTo(false)
    }

    private fun buildQuickActionsCard(
        showPages: Boolean = true,
        showStatsFocusPoint: Boolean = false,
        showPagesFocusPoint: Boolean = false,
        enableFocusPoints: Boolean = true
    ): QuickActionsCard {
        setShowPages(showPages)
        return builder.build(
            QuickActionsCardBuilderParams(
                siteModel,
                setActiveTask(showStatsFocusPoint, showPagesFocusPoint),
                onStatsClick,
                onPagesClick,
                onPostsClick,
                onMediaClick,
                enableFocusPoints = enableFocusPoints
            )
        )
    }

    private fun setShowPages(showPages: Boolean) {
        whenever(siteModel.isSelfHostedAdmin).thenReturn(showPages)
    }

    private fun setActiveTask(showStats: Boolean, showPages: Boolean): QuickStartTask? {
        return when {
            showStats -> QuickStartTask.CHECK_STATS
            showPages -> QuickStartTask.EDIT_HOMEPAGE
            else -> null
        }
    }
}
