package org.wordpress.android.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.util.image.ImageManager

class ActionListItemViewHolder(
    internal val parent: ViewGroup,
    val imageManager: ImageManager
) : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_action_list_item, parent, false)) {
    open fun bind(action: CreateAction) {
        val actionIcon: ImageView = this.itemView.findViewById(R.id.action_icon)
        val actionTitle: TextView = this.itemView.findViewById(R.id.action_title)

        if (action.iconRes > 0) {
            imageManager.load(actionIcon, action.iconRes)
            actionIcon.visibility = View.VISIBLE
        } else {
            actionIcon.visibility = View.GONE
        }

        if (action.labelRes > 0) {
            if (action.actionType == NO_ACTION) {
                TextViewCompat.setTextAppearance(actionTitle, android.R.style.TextAppearance_Medium)
            } else {
                TextViewCompat.setTextAppearance(actionTitle, android.R.style.TextAppearance)
            }

            actionTitle.setText(action.labelRes)
            actionTitle.visibility = View.VISIBLE
        } else {
            actionTitle.visibility = View.GONE
        }

        if (action.onClickAction == null) {
            this.itemView.setOnClickListener(null)
            this.itemView.isClickable = false
        } else {
            this.itemView.setOnClickListener {
                action.onClickAction.invoke(action.actionType)
            }
        }
    }
}
