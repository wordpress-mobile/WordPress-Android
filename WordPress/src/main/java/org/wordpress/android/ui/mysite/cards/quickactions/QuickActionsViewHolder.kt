package org.wordpress.android.ui.mysite.cards.quickactions

import android.view.View
import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySiteQuickActionsCardBinding
import org.wordpress.android.databinding.QuickActionsCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class QuickActionsViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<MySiteQuickActionsCardBinding>(
        parent.viewBinding(MySiteQuickActionsCardBinding::inflate)
) {
    fun bind(card: QuickActionsCard) = with(binding) {
        quickActionsToolbar.update(card)
        quickActionsCard.update(card)
    }

    private fun MySiteCardToolbarBinding.update(card: QuickActionsCard) {
        mySiteCardToolbarTitle.text = uiHelpers.getTextOfUiString(itemView.context, card.title)
    }

    private fun QuickActionsCardBinding.update(card: QuickActionsCard) {
        quickActionStatsButton.setOnClickListener { card.onStatsClick.click() }
        quickActionPostsButton.setOnClickListener { card.onPostsClick.click() }
        quickActionMediaButton.setOnClickListener { card.onMediaClick.click() }
        quickActionPagesButton.setOnClickListener { card.onPagesClick.click() }

        val pagesVisibility = if (card.showPages) View.VISIBLE else View.GONE
        quickActionPagesButton.visibility = pagesVisibility
        quickActionPagesLabel.visibility = pagesVisibility
    }
}
