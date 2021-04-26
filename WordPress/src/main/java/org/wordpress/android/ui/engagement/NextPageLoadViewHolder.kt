package org.wordpress.android.ui.engagement

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.wordpress.android.R
import org.wordpress.android.ui.engagement.EngageItem.NextLikesPageLoader

class NextPageLoadViewHolder(parent: ViewGroup) : EngagedPeopleViewHolder(parent, R.layout.load_or_action_item) {
    private val progress = itemView.findViewById<View>(R.id.progress)
    private val actionButton = itemView.findViewById<Button>(R.id.action_button)

    fun bind(item: NextLikesPageLoader) {
        if (item.isLoading) {
            item.action.invoke()
            progress.visibility = View.VISIBLE
            actionButton.visibility = View.GONE
        } else {
            progress.visibility = View.GONE
            actionButton.visibility = View.VISIBLE
            actionButton.setOnClickListener {
                item.action.invoke()
            }
        }
    }
}
