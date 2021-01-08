package org.wordpress.android.ui.mysite

import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.quick_actions_block.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock

class QuickActionsViewHolder(parent: ViewGroup) : MySiteItemViewHolder(parent, R.layout.quick_actions_block) {
    fun bind(item: QuickActionsBlock) = itemView.apply {
        quick_action_stats_button.setOnClickListener { item.onStatsClick.click() }
        quick_action_pages_button.setOnClickListener { item.onPagesClick.click() }
        quick_action_posts_button.setOnClickListener { item.onPostsClick.click() }
        quick_action_media_button.setOnClickListener { item.onMediaClick.click() }

        val pagesVisibility = if (item.showPages) View.VISIBLE else View.GONE
        quick_action_pages_container.visibility = pagesVisibility
        middle_quick_action_spacing.visibility = pagesVisibility
    }
}
