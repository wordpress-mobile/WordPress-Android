package org.wordpress.android.ui.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiHelpers

class PrepublishingActionsListItemViewHolder(internal val parent: ViewGroup, val uiHelpers: UiHelpers) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.prepublishing_action_list_item, parent, false
        )
) {
    fun bind(action: PrepublishingActionListItem) {
        val actionType: TextView = this.itemView.findViewById(R.id.action_type)

        actionType.text = uiHelpers.getTextOfUiString(itemView.context, action.actionType.textRes)
    }
}
