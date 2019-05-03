package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel

abstract class InsightsManagementViewHolder(
    parent: ViewGroup
) : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.insights_management_list_item, parent, false)) {
    abstract fun bind(insight: InsightModel, isLast: Boolean)
}
