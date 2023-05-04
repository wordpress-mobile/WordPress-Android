package org.wordpress.android.ui.mysite.cards.dashboard.pages

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.CreateNewPageItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.PageContentItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams.PagesItemClickParams
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardContentType.DRAFT
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardContentType.PUBLISH
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardContentType.SCHEDULED
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.config.DashboardCardPagesConfig
import javax.inject.Inject

private const val REQUIRED_PAGES_IN_CARD: Int = 3

class PagesCardBuilder @Inject constructor(
    private val dashboardCardPagesConfig: DashboardCardPagesConfig,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) {
    fun build(params: PagesCardBuilderParams): PagesCard? {
        if (!shouldBuildCard(params)) {
            return null
        }
        return convertToPagesItems(params)
    }

    private fun shouldBuildCard(params: PagesCardBuilderParams): Boolean {
        if (!dashboardCardPagesConfig.isEnabled() ||
            params.pageCard == null
        ) return false

        return true
    }

    private fun convertToPagesItems(params: PagesCardBuilderParams): PagesCard.PagesCardWithData {
        val pages = params.pageCard?.pages
        val content =
            pages?.filterByPagesCardSupportedStatus()?.let { getPagesContentItems(pages, params.onPagesItemClick) }
                ?: emptyList()
        val createPageCard = getCreatePageCard(content, params.onFooterLinkClick)
        return PagesCard.PagesCardWithData(
            title = UiStringRes(R.string.dashboard_pages_card_title),
            pages = content,
            footerLink = createPageCard
        )
    }

    private fun List<PagesCardModel.PageCardModel>.filterByPagesCardSupportedStatus() =
        this.filter { it.status in PagesCardContentType.getList() }

    private fun getPagesContentItems(
        pages: List<PagesCardModel.PageCardModel>,
        onPageItemClick: (params: PagesItemClickParams) -> Unit
    ): List<PageContentItem> {
        PagesCardContentType
        return pages.map { page ->
            PageContentItem(
                title = getPageTitle(page.title),
                statusIcon = getStatusIcon(page.status),
                status = getStatusText(page.status),
                lastEditedOrScheduledTime = getLastEditedOrScheduledTime(page),
                onClick = ListItemInteraction.create(
                    PagesItemClickParams(PagesCardContentType.fromString(page.status), page.id),
                    onPageItemClick
                )
            )
        }
    }

    private fun getPageTitle(title: String) =
        if (title.isEmpty()) UiStringRes(R.string.my_site_untitled_post) else UiStringText(title)

    private fun getStatusIcon(status: String): Int? {
        return when (status) {
            DRAFT.status -> R.drawable.ic_dashboard_card_pages_draft_page_status
            PUBLISH.status -> R.drawable.ic_dashboard_card_pages_published_page_status
            SCHEDULED.status -> R.drawable.ic_dashboard_card_pages_scheduled_page_status
            else -> null
        }
    }

    private fun getStatusText(status: String): UiStringRes? {
        return when (status) {
            DRAFT.status -> UiStringRes(R.string.dashboard_card_page_item_status_draft)
            PUBLISH.status -> UiStringRes(R.string.dashboard_card_page_item_status_published)
            SCHEDULED.status -> UiStringRes(R.string.dashboard_card_page_item_status_scheduled)
            else -> null
        }
    }

    private fun getLastEditedOrScheduledTime(page: PagesCardModel.PageCardModel): UiStringText {
        return UiStringText(
            when (page.status) {
                DRAFT.status, PUBLISH.status -> dateTimeUtilsWrapper.javaDateToTimeSpan(page.lastModifiedOrScheduledOn)
                SCHEDULED.status -> dateTimeUtilsWrapper.getRelativeTimeSpanString(page.date)
                else -> ""
            }
        )
    }

    private fun getCreatePageCard(pages: List<PageContentItem>, onFooterLinkClick: () -> Unit): CreateNewPageItem {
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

    private fun createNewPageCardWithAddPageMessage(onFooterLinkClick: () -> Unit): CreateNewPageItem {
        return CreateNewPageItem(
            label = UiStringRes(R.string.dashboard_pages_card_no_pages_create_page_button),
            description = UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
            imageRes = R.drawable.illustration_page_card_create_page,
            onClick = onFooterLinkClick
        )
    }

    private fun createNewPageCardWithAddAnotherPageMessage(onFooterLinkClick: () -> Unit): CreateNewPageItem {
        return CreateNewPageItem(
            label = UiStringRes(R.string.dashboard_pages_card_create_another_page_button),
            description = UiStringRes(R.string.dashboard_pages_card_create_another_page_description),
            imageRes = R.drawable.illustration_page_card_create_page,
            onClick = onFooterLinkClick
        )
    }

    private fun createNewPageCardWithOnlyButton(onFooterLinkClick: () -> Unit): CreateNewPageItem {
        return CreateNewPageItem(
            label = UiStringRes(R.string.dashboard_pages_card_create_another_page_button),
            onClick = onFooterLinkClick
        )
    }
}
