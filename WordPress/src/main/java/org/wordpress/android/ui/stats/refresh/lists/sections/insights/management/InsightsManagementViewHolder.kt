package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel

class InsightsManagementViewHolder(
    parent: ViewGroup
) : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.insights_management_list_item, parent, false)) {
    private val title: TextView = itemView.findViewById(R.id.itemTitle)
    private val managementButton: ImageButton = itemView.findViewById(R.id.insightsManagementItemButton)
    private val dragAndDropButton: ImageButton = itemView.findViewById(R.id.dragAndDropItemButton)
    fun bind(insightModel: InsightModel) {
        title.setText(insightModel.name)

        val buttonImage = if (insightModel.isAdded) R.drawable.ic_remove_circle else R.drawable.ic_add_circle
        managementButton.setImageResource(buttonImage)
    }
}
