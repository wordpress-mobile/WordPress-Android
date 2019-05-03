package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel

class RemovedInsightViewHolder(
    val parent: ViewGroup,
    private val onButtonClicked: (InsightModel) -> Unit
) : InsightsManagementViewHolder(parent) {
    private val title: TextView = itemView.findViewById(R.id.itemTitle)
    private val managementButton: ImageButton = itemView.findViewById(R.id.insightsManagementItemButton)
    private val dragAndDropButton: View = itemView.findViewById(R.id.dragAndDropItemButton)
    private val divider: View = itemView.findViewById(R.id.divider)

    override fun bind(insight: InsightModel, isLast: Boolean) {
        title.setText(insight.name)

        managementButton.setImageResource(R.drawable.ic_add_circle)
        managementButton.setOnClickListener {
            managementButton.setOnClickListener(null)
            onButtonClicked(insight)
        }

        dragAndDropButton.visibility = View.GONE
        divider.visibility = if (isLast) View.GONE else View.VISIBLE
    }
}
