package org.wordpress.android.ui.mysite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationBlock
import org.wordpress.android.ui.mysite.MySiteItem.DynamicCard.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock.IconState.Visible
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringText

class MySiteItemTest {
    private val interaction = ListItemInteraction.create { }

    @Test
    fun `site info block is active when focus point on title`() {
        val siteInfoBlock = initSiteInfoBlock(showTitleFocusPoint = true)

        assertThat(siteInfoBlock.activeQuickStartItem).isTrue()
    }

    @Test
    fun `site info block is active when focus point on icon`() {
        val siteInfoBlock = initSiteInfoBlock(showIconFocusPoint = true)

        assertThat(siteInfoBlock.activeQuickStartItem).isTrue()
    }

    @Test
    fun `site info block is not active when focus point not added`() {
        val siteInfoBlock = initSiteInfoBlock()

        assertThat(siteInfoBlock.activeQuickStartItem).isFalse()
    }

    private fun initSiteInfoBlock(
        showTitleFocusPoint: Boolean = false,
        showIconFocusPoint: Boolean = false
    ): SiteInfoBlock {
        return SiteInfoBlock(
                title = "test",
                url = "url",
                iconState = Visible(null),
                showTitleFocusPoint = showTitleFocusPoint,
                showIconFocusPoint = showIconFocusPoint,
                onTitleClick = null,
                onIconClick = interaction,
                onUrlClick = interaction,
                onSwitchSiteClick = interaction
        )
    }

    @Test
    fun `quick actions block is never active`() {
        val quickActionsBlock = QuickActionsBlock(
                title = UiStringText("test"),
                onStatsClick = interaction,
                onPagesClick = interaction,
                onPostsClick = interaction,
                onMediaClick = interaction,
                showPages = true
        )

        assertThat(quickActionsBlock.activeQuickStartItem).isFalse
    }

    @Test
    fun `domain registration block is never active`() {
        val domainRegistrationBlock = DomainRegistrationBlock(interaction)

        assertThat(domainRegistrationBlock.activeQuickStartItem).isFalse()
    }

    @Test
    fun `quick start card is never active`() {
        val quickStartCard = QuickStartCard(
                GROW_QUICK_START,
                UiStringText("title"),
                listOf(),
                color.pink_40,
                0,
                interaction
        )

        assertThat(quickStartCard.activeQuickStartItem).isFalse()
    }

    @Test
    fun `category header is never active`() {
        val categoryHeader = CategoryHeader(UiStringText("title"))

        assertThat(categoryHeader.activeQuickStartItem).isFalse()
    }

    @Test
    fun `list item is active when shows focus point`() {
        val listItem = initListItem(showFocusPoint = true)

        assertThat(listItem.activeQuickStartItem).isTrue()
    }

    @Test
    fun `list item is not active when does not show focus point`() {
        val listItem = initListItem(showFocusPoint = false)

        assertThat(listItem.activeQuickStartItem).isFalse()
    }

    private fun initListItem(showFocusPoint: Boolean) = ListItem(
            primaryIcon = drawable.ic_dropdown_primary_30_24dp,
            primaryText = UiStringText("title"),
            secondaryIcon = null,
            secondaryText = null,
            showFocusPoint = showFocusPoint,
            onClick = interaction
    )
}
