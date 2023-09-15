package org.wordpress.android.ui.mysite.cards.dashboard.pages

import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySitePagesCardFooterLinkBinding
import org.wordpress.android.databinding.MySitePagesCardWithPageItemsBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.CreateNewPageItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
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
        mySiteToolbar.update(pagesCard)
        setUpPageItems(pagesCard.pages)
        mySiteCardFooterLink.setUpFooter(pagesCard.footerLink)
    }

    private fun MySitePagesCardWithPageItemsBinding.setUpPageItems(pages: List<PagesCardWithData.PageContentItem>) {
        if (pages.isNotEmpty()) (pagesItems.adapter as PagesItemsAdapter).update(pages)
    }

    private fun MySiteCardToolbarBinding.update(card: PagesCardWithData) {
        uiHelpers.setTextOrHide(mySiteCardToolbarTitle, card.title)
        mySiteCardToolbarMore.visibility = View.VISIBLE
        mySiteCardToolbarMore.setOnClickListener {
            showMoreMenu(
                card.moreMenuOptionsLink.onMoreClick,
                card.moreMenuOptionsLink.allPagesMenuItemClick,
                card.moreMenuOptionsLink.hideThisMenuItemClick,
                mySiteCardToolbarMore,
            )
        }
    }

    private fun MySitePagesCardFooterLinkBinding.setUpFooter(footer: CreateNewPageItem) {
        uiHelpers.setTextOrHide(linkLabel, footer.label)
        uiHelpers.setTextOrHide(linkDescription, footer.description)
        uiHelpers.setImageOrHide(linkIcon, footer.imageRes)
        mySiteCardFooterLinkLayout.setOnClickListener { footer.onClick.invoke() }
    }

    private fun showMoreMenu(
        onMoreMenuClick: () -> Unit,
        onAllActivityItemClick: () -> Unit,
        onHideMenuItemClick: () -> Unit,
        anchor: View
    ) {
        onMoreMenuClick.invoke()
        val popupMenu = PopupMenu(itemView.context, anchor)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.pages_card_menu_item_all_pages -> {
                    onAllActivityItemClick.invoke()
                    return@setOnMenuItemClickListener true
                }

                R.id.pages_card_menu_item_hide_this -> {
                    onHideMenuItemClick.invoke()
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener true
            }
        }
        popupMenu.inflate(R.menu.pages_card_menu)
        popupMenu.show()
    }
}
