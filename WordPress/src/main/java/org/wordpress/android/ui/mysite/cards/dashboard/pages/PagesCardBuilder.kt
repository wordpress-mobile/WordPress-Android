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
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardContentType.PUBLISH
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
        return pages.map { page ->
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

    private fun getStatusIcon(status: String): Int? {
        return when (status) {
            DRAFT.status -> R.drawable.ic_draft_page_draft_dashboard_card
            PUBLISH.status -> R.drawable.ic_published_page_dashboard_card
            SCHEDULED.status -> R.drawable.ic_scheduled_page_dashboard_card
            else -> null
        }
    }

    private fun getStatusText(status: String): UiString.UiStringRes? {
        return when (status) {
            DRAFT.status -> UiString.UiStringRes(R.string.dashboard_card_page_item_status_draft)
            PUBLISH.status -> UiString.UiStringRes(R.string.dashboard_card_page_item_status_published)
            SCHEDULED.status -> UiString.UiStringRes(R.string.dashboard_card_page_item_status_scheduled)
            else -> null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getLastEditedOrScheduledTime(lastModifiedOrScheduledOn: Date): UiString {
        // implement the logic to get the text
        return UiString.UiStringText("")
    }

    private fun getCreatePageCard(pages: List<PageContentItem>, onFooterLinkClick: () -> Unit): CreatNewPageItem {
        // Create new page button is shown with image if there is
        // less than three pages for a user
        return if (pages.isEmpty()) {
            createNewPageCardWithAddPageMessage(onFooterLinkClick)
        } else if (pages.size < REQUIRED_PAGES_IN_CARD) {
            createNewPageCardWithAddAnotherPageMessage(onFooterLinkClick)
        } else {
            createNewPageCardWithOnlyButton(onFooterLinkClick)
        }
    }

    private fun createNewPageCardWithAddPageMessage(onFooterLinkClick: () -> Unit): CreatNewPageItem {
        return CreatNewPageItem(
            label = UiString.UiStringRes(R.string.dashboard_pages_card_no_pages_create_page_button),
            description = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
            imageRes = R.drawable.illustration_page_card_create_page,
            onClick = onFooterLinkClick
        )
    }

    private fun createNewPageCardWithAddAnotherPageMessage(onFooterLinkClick: () -> Unit): CreatNewPageItem {
        return CreatNewPageItem(
            label = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_button),
            description = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
            imageRes = R.drawable.illustration_page_card_create_page,
            onClick = onFooterLinkClick
        )
    }

    private fun createNewPageCardWithOnlyButton(onFooterLinkClick: () -> Unit): CreatNewPageItem {
        return CreatNewPageItem(
            label = UiString.UiStringRes(R.string.dashboard_pages_card_create_another_page_button),
            onClick = onFooterLinkClick
        )
    }
}
