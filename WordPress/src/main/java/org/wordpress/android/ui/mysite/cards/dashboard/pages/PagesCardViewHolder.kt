package org.wordpress.android.ui.mysite.cards.dashboard.pages

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySitePagesCardWithPageItemsBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData
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
        (pagesItems.adapter as PagesItemsAdapter).update(pagesCard.pages)
        mySiteToolbar.update(pagesCard.title)
        uiHelpers.setTextOrHide(mySiteCardFooterLink.linkLabel,pagesCard.footerLink.label)
        uiHelpers.setTextOrHide(mySiteCardFooterLink.linkDescription,pagesCard.footerLink.description)
        uiHelpers.setImageOrHide(mySiteCardFooterLink.linkIcon,pagesCard.footerLink.imageRes)
    }

    private fun MySiteCardToolbarBinding.update(title: UiString?) {
        uiHelpers.setTextOrHide(mySiteCardToolbarTitle, title)
    }
}
