package org.wordpress.android.ui.main

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.util.QuickStartUtils
import org.wordpress.android.util.image.ImageManager

class ActionListItemViewHolder(
    internal val parent: ViewGroup,
    val imageManager: ImageManager
) : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_action_list_item, parent, false)) {
    private val regularTypeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val mediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun bind(action: CreateAction, showQuickStartFocusPoint: Boolean = false) {
        val actionIcon: ImageView = this.itemView.findViewById(R.id.action_icon)
        val actionTitle: TextView = this.itemView.findViewById(R.id.action_title)
        val actionRowContainer: ViewGroup = this.itemView.findViewById(R.id.action_row_container)

        if (action.iconRes > 0) {
            imageManager.load(actionIcon, action.iconRes)
            actionIcon.visibility = View.VISIBLE
        } else {
            actionIcon.visibility = View.GONE
        }

        if (action.labelRes > 0) {
            if (action.actionType == NO_ACTION) {
                actionTitle.typeface = mediumTypeface
            } else {
                actionTitle.typeface = regularTypeface
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
            this.itemView.isClickable = true
        }

        if (showQuickStartFocusPoint) {
            val focusPointSize = actionRowContainer.resources.getDimensionPixelOffset(
                    dimen.quick_start_focus_point_size
            )
            val verticalOffset = (actionRowContainer.width - focusPointSize) / 2
            QuickStartUtils.addQuickStartFocusPointAboveTheView(
                    actionRowContainer, actionTitle,
                    verticalOffset, 0
            )
        }
    }
}
