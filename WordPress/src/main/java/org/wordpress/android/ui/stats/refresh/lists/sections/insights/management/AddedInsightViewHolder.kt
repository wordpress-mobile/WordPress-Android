package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel

class AddedInsightViewHolder(
    val parent: ViewGroup,
    private val onDragStarted: (viewHolder: ViewHolder) -> Unit,
    private val onButtonClicked: (InsightModel) -> Unit
) : InsightsManagementViewHolder(parent) {
    private val title: TextView = itemView.findViewById(R.id.item_title)
    private val managementButton: ImageButton = itemView.findViewById(R.id.insights_management_item_button)
    private val dragAndDropButton: View = itemView.findViewById(R.id.drag_and_drop_item_button)
    private val divider: View = itemView.findViewById(R.id.divider)

    override fun bind(insight: InsightModel, isLast: Boolean) {
        title.setText(insight.name)

        managementButton.setImageResource(R.drawable.ic_remove_circle)
        managementButton.setOnClickListener {
            managementButton.setOnClickListener(null)
            onButtonClicked(insight)
        }

        dragAndDropButton.setOnTouchListener { _, event ->
            if (MotionEventCompat.isFromSource(event, MotionEvent.ACTION_DOWN)) {
                onDragStarted(this)
                itemView.elevation = 10f
            }
            return@setOnTouchListener true
        }

        dragAndDropButton.visibility = View.VISIBLE

        updateDividerVisibility(isLast)
    }

    fun updateDividerVisibility(isLast: Boolean) {
        divider.visibility = if (isLast) View.GONE else View.VISIBLE
    }

    fun onDragFinished() {
        itemView.elevation = 0f
    }
}
