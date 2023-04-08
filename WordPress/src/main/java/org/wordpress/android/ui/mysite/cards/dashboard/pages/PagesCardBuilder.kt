package org.wordpress.android.ui.mysite.cards.dashboard.pages

import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams
import org.wordpress.android.util.LocaleManagerWrapper
import javax.inject.Inject

class PagesCardBuilder @Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper
) {
    fun build(params: PagesCardBuilderParams): List<PagesCard> {
       return convertToPagesItems(params.pageCard)
    }

    private fun convertToPagesItems(pagesCardModel: PagesCardModel): List<PagesCard> {
        val pages = pagesCardModel.pages
        val content = getPagesContentItems(pages)
        val createPageCard = getCreatePageCard(pages)

        return content.toMutableList() + createPageCard
    }

    private fun getPagesContentItems(pages: List<PagesCardModel.PageCardModel>): List<PagesCard.PageContentItem> {
        //todo: implement
    }

    private fun getCreatePageCard(pages: List<PagesCardModel.PageCardModel>): PagesCard.FooterLink {
        // Create new page button is shown with image if there is
        // less than three pages for a user
        return if (pages.isEmpty()) {
            createNewPageCardWithAddPageMessage()
        } else if (pages.size < 3) {
            createNewPageCardWithAddAnotherPageMessage()
        } else {
            createNewPageCardWithOnlyButton()
        }
    }

    private fun createNewPageCardWithAddPageMessage(params: PagesCardBuilderParams): PagesCard.FooterLink {
        //todo: implement
    }

    private fun createNewPageCardWithAddAnotherPageMessage(): PagesCard.FooterLink {
        //todo: implement
    }

    private fun createNewPageCardWithOnlyButton(): PagesCard.FooterLink {
        //todo: implement
    }
}