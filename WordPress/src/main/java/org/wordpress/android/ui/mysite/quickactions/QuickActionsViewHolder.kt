package org.wordpress.android.ui.mysite.quickactions

import android.view.View
import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySiteQuickActionsCardBinding
import org.wordpress.android.databinding.QuickActionsBlockBinding
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class QuickActionsViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteItemViewHolder<MySiteQuickActionsCardBinding>(parent.viewBinding(MySiteQuickActionsCardBinding::inflate)) {
    fun bind(block: QuickActionsBlock) = with(binding) {
        quickActionsToolbar.update(block)
        quickActionsBlock.update(block)
    }

    private fun MySiteCardToolbarBinding.update(block: QuickActionsBlock) {
        mySiteCardToolbarTitle.text = uiHelpers.getTextOfUiString(itemView.context, block.title)
    }

    private fun QuickActionsBlockBinding.update(item: QuickActionsBlock) {
        quickActionStatsButton.setOnClickListener { item.onStatsClick.click() }
        quickActionPostsButton.setOnClickListener { item.onPostsClick.click() }
        quickActionMediaButton.setOnClickListener { item.onMediaClick.click() }
        quickActionPagesButton.setOnClickListener { item.onPagesClick.click() }

        val pagesVisibility = if (item.showPages) View.VISIBLE else View.GONE
        quickActionPagesButton.visibility = pagesVisibility
        quickActionPagesLabel.visibility = pagesVisibility

        quickStartStatsFocusPoint.setVisibleOrGone(item.showStatsFocusPoint)
        quickStartPagesFocusPoint.setVisibleOrGone(item.showPagesFocusPoint)
    }
}
