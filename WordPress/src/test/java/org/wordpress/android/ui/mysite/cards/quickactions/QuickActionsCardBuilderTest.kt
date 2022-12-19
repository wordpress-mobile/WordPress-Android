package org.wordpress.android.ui.mysite.cards.quickactions

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
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

    private fun buildQuickActionsCard(
        showPages: Boolean = true
    ): QuickActionsCard {
        setShowPages(showPages)
        return builder.build(
            QuickActionsCardBuilderParams(
                siteModel,
                onStatsClick,
                onPagesClick,
                onPostsClick,
                onMediaClick
            )
        )
    }

    private fun setShowPages(showPages: Boolean) {
        whenever(siteModel.isSelfHostedAdmin).thenReturn(showPages)
    }
}
