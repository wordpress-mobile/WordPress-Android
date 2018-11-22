package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.stats.refresh.lists.Empty

class EmptyViewHolder(
    parent: ViewGroup,
    private val onActionButtonClicked: () -> Unit = {}
) : BaseStatsViewHolder(parent, R.layout.stats_empty_view) {
    private val emptyView = itemView.findViewById<ActionableEmptyView>(R.id.actionable_empty_view)

    fun bind(item: Empty) {
        if (item.isButtonVisible) {
            emptyView.button.setOnClickListener {
                onActionButtonClicked()
            }
            emptyView.button.visibility = View.VISIBLE
        } else {
            emptyView.button.visibility = View.GONE
        }
    }
}
