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
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardContentType.DRAFT
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardContentType.PUBLISHED
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardContentType.SCHEDULED

private const val REQUIRED_PAGES_IN_CARD: Int = 3

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
        val pages = params.pageCard?.pages
        val content = pages?.let { getPagesContentItems(pages) } ?: emptyList()
        val createPageCard = getCreatePageCard(params)
        return PagesCard.PagesCardWithData(
            title = UiString.UiStringRes(R.string.dashboard_pages_card_title),
            pages = content,
            footerLink = createPageCard
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getPagesContentItems(pages: List<PagesCardModel.PageCardModel>): List<PageContentItem> {
        return emptyList()
    }

    private fun getStatusText(status: String): UiString? =
         when (status) {
            DRAFT.status -> UiString.UiStringRes(R.string.pages_card_draft)
            PUBLISHED.status -> UiString.UiStringRes(R.string.pages_card_published)
            SCHEDULED.status -> UiString.UiStringRes(R.string.pages_card_scheduled)
            else -> null
        }

    private fun getCreatePageCard(params: PagesCardBuilderParams): CreatNewPageItem {
        // Create new page button is shown with image if there is
        // less than three pages for a user
        val pages = params.pageCard?.pages ?: emptyList()
        return if (pages.isEmpty()) {
            createNewPageCardWithAddPageMessage(params)
        } else if (pages.size < REQUIRED_PAGES_IN_CARD) {
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
