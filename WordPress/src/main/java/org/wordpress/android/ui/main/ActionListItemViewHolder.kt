package org.wordpress.android.ui.main

import android.view.LayoutInflater
import android.view.View
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

        if (action.iconRes > 0) {
            actionIcon.setImageResource(action.iconRes)
            actionIcon.visibility = View.VISIBLE
        } else {
            actionIcon.visibility = View.GONE
        }

        if (action.labelRes > 0) {
            actionTitle.setText(action.labelRes)
            actionTitle.visibility = View.VISIBLE
        } else {
            actionTitle.visibility = View.GONE
        }

        action.onClickAction?.let {
            this.itemView.setOnClickListener {
                action.onClickAction.invoke(action.actionType)
            }
        }
    }
}
