package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightModelDiffCallback.Payload
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.InsightModel.Status.ADDED

sealed class InsightsManagementViewHolder(
    parent: ViewGroup,
    @LayoutRes layoutRes: Int
) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                layoutRes,
                parent,
                false
        )
) {
    class HeaderViewHolder(
        val parent: ViewGroup
    ) : InsightsManagementViewHolder(parent, R.layout.insights_management_header_item) {
        private val title: TextView = itemView.findViewById(R.id.itemTitle)

        fun bind(insight: InsightListItem.Header, isTopHeader: Boolean) {
            title.setText(insight.text)
            val lp = itemView.layoutParams as MarginLayoutParams
            if (isTopHeader) {
                lp.topMargin = itemView.context.resources.getDimensionPixelOffset(R.dimen.margin_extra_large)
            } else {
                lp.topMargin = itemView.context.resources.getDimensionPixelOffset(
                        R.dimen.margin_extra_extra_medium_large
                )
            }
            itemView.layoutParams = lp
        }
    }

    class InsightViewHolder(
        val parent: ViewGroup
    ) : InsightsManagementViewHolder(parent, R.layout.insights_management_list_item) {
        private val container: View = itemView.findViewById(R.id.container)
        private val title: TextView = itemView.findViewById(R.id.itemTitle)

        fun bind(insight: InsightModel, payload: Payload? = null) {
            if (payload == null) {
                title.setText(insight.name)
                title.isEnabled = insight.status == ADDED
                container.setOnClickListener {
                    insight.onClick.click()
                }
            } else {
                title.isEnabled = payload.isSelected
            }
        }
    }
}
