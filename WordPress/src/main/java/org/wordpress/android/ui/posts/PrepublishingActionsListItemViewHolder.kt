package org.wordpress.android.ui.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R

class PrepublishingActionsListItemViewHolder(internal val parent: ViewGroup) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.prepublishing_action_list_item, parent, false
        )
) {
    fun bind(action: PrepublishingActionListItem) {}
}
