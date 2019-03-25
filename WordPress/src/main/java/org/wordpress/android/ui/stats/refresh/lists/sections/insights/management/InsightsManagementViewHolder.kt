package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel

class InsightsManagementViewHolder(
    parent: ViewGroup
) : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.insights_management_list_item, parent, false)) {
    private val title: TextView = itemView.findViewById(R.id.itemTitle)
    fun bind(insightModel: InsightModel) {
        title.setText(insightModel.name)
    }
}
