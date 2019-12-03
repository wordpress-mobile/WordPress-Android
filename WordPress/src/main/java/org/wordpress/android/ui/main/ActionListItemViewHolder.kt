package org.wordpress.android.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.main.MainActionListItem.CreateAction

class ActionListItemViewHolder(
    internal val parent: ViewGroup
) : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_action_list_item, parent, false)) {
    open fun bind(action: CreateAction) {
        val actionIcon: ImageView = this.itemView.findViewById(R.id.action_icon)
        val actionTitle: TextView = this.itemView.findViewById(R.id.action_title)

        actionIcon.setImageResource(action.iconRes)
        actionTitle.setText(action.labelRes)

        this.itemView.setOnClickListener {
            action.onClickAction?.invoke(action.actionType)
        }
    }
}
