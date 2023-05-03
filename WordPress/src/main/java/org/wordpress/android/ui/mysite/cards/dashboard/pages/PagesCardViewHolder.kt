package org.wordpress.android.ui.mysite.cards.dashboard.pages

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySitePagesCardFooterLinkBinding
import org.wordpress.android.databinding.MySitePagesCardWithPageItemsBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.CreateNewPageItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.viewBinding

class PagesCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : CardViewHolder<MySitePagesCardWithPageItemsBinding>(
    parent.viewBinding(MySitePagesCardWithPageItemsBinding::inflate)
) {
    init {
        binding.pagesItems.adapter = PagesItemsAdapter(uiHelpers)
    }

    fun bind(card: PagesCard) = with(binding) {
        val pagesCard = card as PagesCardWithData
        mySiteToolbar.update(pagesCard.title)
        setUpPageItems(pagesCard.pages)
        mySiteCardFooterLink.setUpFooter(pagesCard.footerLink)
    }

    private fun MySitePagesCardWithPageItemsBinding.setUpPageItems(pages: List<PagesCardWithData.PageContentItem>) {
        if (pages.isNotEmpty()) (pagesItems.adapter as PagesItemsAdapter).update(pages)
    }

    private fun MySiteCardToolbarBinding.update(title: UiString?) {
        uiHelpers.setTextOrHide(mySiteCardToolbarTitle, title)
    }

    private fun MySitePagesCardFooterLinkBinding.setUpFooter(footer: CreateNewPageItem) {
        uiHelpers.setTextOrHide(linkLabel, footer.label)
        uiHelpers.setTextOrHide(linkDescription, footer.description)
        uiHelpers.setImageOrHide(linkIcon, footer.imageRes)
        mySiteCardFooterLinkLayout.setOnClickListener { footer.onClick.invoke() }
    }
}
