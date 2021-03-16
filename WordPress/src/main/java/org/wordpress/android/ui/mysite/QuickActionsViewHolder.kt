package org.wordpress.android.ui.mysite

import android.view.View
import android.view.ViewGroup
import org.wordpress.android.databinding.QuickActionsBlockBinding
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.util.viewBinding

class QuickActionsViewHolder(
    parent: ViewGroup
) : MySiteItemViewHolder<QuickActionsBlockBinding>(parent.viewBinding(QuickActionsBlockBinding::inflate)) {
    fun bind(item: QuickActionsBlock) = with(binding) {
        quickActionStatsButton.setOnClickListener { item.onStatsClick.click() }
        quickActionPostsButton.setOnClickListener { item.onPostsClick.click() }
        quickActionMediaButton.setOnClickListener { item.onMediaClick.click() }
        quickActionPagesButton.setOnClickListener { item.onPagesClick.click() }

        val pagesVisibility = if (item.showPages) View.VISIBLE else View.GONE
        quickActionPagesContainer.visibility = pagesVisibility
        middleQuickActionSpacing.visibility = pagesVisibility

        quickStartStatsFocusPoint.setVisibleOrGone(item.showStatsFocusPoint)
        quickStartPagesFocusPoint.setVisibleOrGone(item.showPagesFocusPoint)
    }
}
