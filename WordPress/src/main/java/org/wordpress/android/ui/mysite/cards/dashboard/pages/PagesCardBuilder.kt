package org.wordpress.android.ui.mysite.cards.dashboard.pages

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.CreatNewPageItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.PageContentItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.config.DashboardCardPagesConfig
import javax.inject.Inject

class PagesCardBuilder @Inject constructor(
    private val dashboardCardPagesConfig: DashboardCardPagesConfig
) {
    fun build(params: PagesCardBuilderParams): PagesCard? {
        if (!dashboardCardPagesConfig.isEnabled()) {
            return null
        }
        return convertToPagesItems(params)
    }

    private fun convertToPagesItems(params: PagesCardBuilderParams): PagesCard.PagesCardWithData {
        val pages = params.pageCard?.pages ?: emptyList()
        val content = getPagesContentItems(pages)
        val createPageCard = getCreatePageCard(params)
        return PagesCard.PagesCardWithData(
            title = UiString.UiStringRes(R.string.dashboard_pages_card_title),
            pages = content,
            footerLink = createPageCard
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getPagesContentItems(pages: List<PagesCardModel.PageCardModel>): List<PageContentItem> {
        //todo: implement
        return emptyList()
    }

    private fun getCreatePageCard(params: PagesCardBuilderParams): CreatNewPageItem {
        // Create new page button is shown with image if there is
        // less than three pages for a user
        val pages = params.pageCard?.pages ?: emptyList()
        return if (pages.isEmpty()) {
            createNewPageCardWithAddPageMessage(params)
        } else if (pages.size < 3) {
            createNewPageCardWithAddAnotherPageMessage(params)
        } else {
            createNewPageCardWithOnlyButton(params)
        }
    }

    private fun createNewPageCardWithAddPageMessage(params: PagesCardBuilderParams): CreatNewPageItem {
        return CreatNewPageItem(
            label = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_button),
            description = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
            onClick = params.onFooterLinkClick
        )
    }

    private fun createNewPageCardWithAddAnotherPageMessage(params: PagesCardBuilderParams): CreatNewPageItem {
        return CreatNewPageItem(
            label = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_button),
            description = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
            onClick = params.onFooterLinkClick
        )
    }

    private fun createNewPageCardWithOnlyButton(params: PagesCardBuilderParams): CreatNewPageItem {
        return CreatNewPageItem(
            label = UiString.UiStringRes(R.string.dashboard_pages_card_no_pages_create_page_button),
            onClick = params.onFooterLinkClick
        )
    }
}