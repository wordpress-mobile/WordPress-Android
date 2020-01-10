package org.wordpress.android.ui.main

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainActionListItem.Title
import org.wordpress.android.util.image.ImageManager

class ActionListItemViewHolder(
    internal val parent: ViewGroup,
    val imageManager: ImageManager
) : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_action_list_item, parent, false)) {
    open fun bind(item: MainActionListItem) {
        val actionIcon: ImageView = this.itemView.findViewById(R.id.action_icon)
        val actionTitle: TextView = this.itemView.findViewById(R.id.action_title)

        when(item) {
            is Title -> {
                actionIcon.visibility = View.GONE
                actionTitle.setTypeface(actionTitle.typeface, Typeface.BOLD)
                actionTitle.setText(item.labelRes)

                this.itemView.setOnClickListener(null)
            }
            is CreateAction -> {
                imageManager.load(actionIcon, item.iconRes)
                actionIcon.visibility = View.VISIBLE
                actionTitle.setTypeface(actionTitle.typeface, Typeface.NORMAL)
                actionTitle.setText(item.labelRes)

                this.itemView.setOnClickListener {
                    item.onClickAction.invoke(item.actionType)
                }
            }
        }
    }
}
