package org.wordpress.android.ui.mysite.cards.dashboard.pages

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.CreatNewPageItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.PageContentItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.config.DashboardCardPagesConfig
import java.util.Date
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

    private fun getPagesContentItems(pages: List<PagesCardModel.PageCardModel>): List<PageContentItem> {
        return pages.map{ page ->
            PageContentItem(
                title = getPageTitle(page.title),
                statusIcon = getStatusIcon(page.status),
                status = getStatusText(page.status),
                lastEditedOrScheduledTime = getLastEditedOrScheduledTime(page.lastModifiedOrScheduledOn),
                onCardClick = { }
            )
        }
    }

    private fun getPageTitle(title: String) =
        if (title.isEmpty()) UiString.UiStringRes(R.string.my_site_untitled_post) else UiString.UiStringText(title)

    @Suppress("UNUSED_PARAMETER")
    private fun getStatusIcon(status: String): Int {
        // implement the logic to get the correct icon
        return R.drawable.ic_pages_white_24dp
    }

    private fun getStatusText(status: String): UiString.UiStringRes? {
        return when (status) {
            DRAFT.status -> UiString.UiStringRes(R.string.dashboard_card_page_item_status_draft)
            PUBLISHED.status -> UiString.UiStringRes(R.string.dashboard_card_page_item_status_published)
            SCHEDULED.status -> UiString.UiStringRes(R.string.dashboard_card_page_item_status_scheduled)
            else -> null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getLastEditedOrScheduledTime(lastModifiedOrScheduledOn: Date): UiString {
        // implement the logic to get the text
        return UiString.UiStringText("")
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
            label = UiString.UiStringRes(R.string.dashboard_pages_card_no_pages_create_page_button),
            description = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
            imageRes = R.drawable.illustration_page_card_create_page,
            onClick = params.onFooterLinkClick
        )
    }

    private fun createNewPageCardWithAddAnotherPageMessage(params: PagesCardBuilderParams): CreatNewPageItem {
        return CreatNewPageItem(
            label = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_button),
            description = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
            imageRes = R.drawable.illustration_page_card_create_page,
            onClick = params.onFooterLinkClick
        )
    }

    private fun createNewPageCardWithOnlyButton(params: PagesCardBuilderParams): CreatNewPageItem {
        return CreatNewPageItem(
            label = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_button),
            onClick = params.onFooterLinkClick
        )
    }
}
