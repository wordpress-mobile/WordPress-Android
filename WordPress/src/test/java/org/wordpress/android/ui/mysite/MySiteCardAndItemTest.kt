package org.wordpress.android.ui.mysite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard.IconState.Visible
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringText

class MySiteCardAndItemTest {
    private val interaction = ListItemInteraction.create { }

    @Test
    fun `site info block is active when focus point on title`() {
        val siteInfoCard = initSiteInfoCard(showTitleFocusPoint = true)

        assertThat(siteInfoCard.activeQuickStartItem).isTrue()
    }

    @Test
    fun `site info block is active when focus point on icon`() {
        val siteInfoCard = initSiteInfoCard(showIconFocusPoint = true)

        assertThat(siteInfoCard.activeQuickStartItem).isTrue()
    }

    @Test
    fun `when focus point on subtitle, then site info block is active`() {
        val siteInfoCard = initSiteInfoCard(showSubtitleFocusPoint = true)

        assertThat(siteInfoCard.activeQuickStartItem).isTrue()
    }

    @Test
    fun `site info block is not active when focus point not added`() {
        val siteInfoCard = initSiteInfoCard()

        assertThat(siteInfoCard.activeQuickStartItem).isFalse()
    }

    private fun initSiteInfoCard(
        showTitleFocusPoint: Boolean = false,
        showSubtitleFocusPoint: Boolean = false,
        showIconFocusPoint: Boolean = false
    ): SiteInfoHeaderCard {
        return SiteInfoHeaderCard(
            title = "test",
            url = "url",
            iconState = Visible(null),
            showTitleFocusPoint = showTitleFocusPoint,
            showSubtitleFocusPoint = showSubtitleFocusPoint,
            showIconFocusPoint = showIconFocusPoint,
            onTitleClick = null,
            onIconClick = interaction,
            onUrlClick = interaction,
            onSwitchSiteClick = interaction
        )
    }


    @Test
    fun `domain registration card is never active`() {
        val domainRegistrationCard = DomainRegistrationCard(interaction)

        assertThat(domainRegistrationCard.activeQuickStartItem).isFalse()
    }

    @Test
    fun `category header is never active`() {
        val categoryHeader = CategoryHeaderItem(UiStringText("title"))

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
        primaryIcon = R.drawable.ic_dropdown_primary_30_24dp,
        primaryText = UiStringText("title"),
        secondaryIcon = null,
        secondaryText = null,
        showFocusPoint = showFocusPoint,
        onClick = interaction,
        listItemAction = ListItemAction.STATS,
    )
}
