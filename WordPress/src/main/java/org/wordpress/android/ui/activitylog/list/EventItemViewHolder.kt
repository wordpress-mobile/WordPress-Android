package org.wordpress.android.ui.activitylog.list

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListPopupWindow
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Event
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.SecondaryAction
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.extensions.getColorResIdFromAttribute

class EventItemViewHolder(
    parent: ViewGroup,
    private val itemClickListener: (ActivityLogListItem) -> Unit,
    private val secondaryActionClickListener: (SecondaryAction, ActivityLogListItem) -> Boolean
) : ActivityLogViewHolder(parent, R.layout.activity_log_list_event_item) {
    private val summary: TextView = itemView.findViewById(R.id.action_summary)
    private val text: TextView = itemView.findViewById(R.id.action_text)
    private val thumbnail: ImageView = itemView.findViewById(R.id.action_icon)
    private val container: View = itemView.findViewById(R.id.activity_content_container)
    private val actionButton: ImageButton = itemView.findViewById(R.id.action_button)

    override fun updateChanges(bundle: Bundle) {
        if (bundle.containsKey(ActivityLogDiffCallback.LIST_ITEM_BUTTON_VISIBILITY_KEY)) {
            actionButton.visibility =
                    if (bundle.getBoolean(ActivityLogDiffCallback.LIST_ITEM_BUTTON_VISIBILITY_KEY)) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
        }
    }

    fun bind(activity: Event) {
        summary.text = activity.title
        text.text = activity.description

        ColorUtils.setImageResourceWithTint(
                actionButton,
                activity.buttonIcon.drawable,
                actionButton.context.getColorResIdFromAttribute(R.attr.wpColorOnSurfaceMedium)
        )
        if (activity.isButtonVisible) {
            actionButton.visibility = View.VISIBLE
        } else {
            actionButton.visibility = View.GONE
        }

        thumbnail.setImageResource(activity.icon.drawable)
        thumbnail.setBackgroundResource(activity.status.color)
        container.setOnClickListener {
            itemClickListener(activity.copy(isButtonVisible = actionButton.visibility == View.VISIBLE))
        }

        actionButton.setOnClickListener { renderMoreMenu(activity, it) }
    }

    private fun renderMoreMenu(
        event: Event,
        v: View
    ) {
        val popup = ListPopupWindow(v.context)
        popup.width = v.context.resources.getDimensionPixelSize(R.dimen.menu_item_width)
        popup.setAdapter(ActivityLogListItemMenuAdapter(v.context, event.isRestoreHidden))
        popup.setDropDownGravity(Gravity.END)
        popup.anchorView = v
        popup.isModal = true
        popup.setOnItemClickListener { _, _, _, id ->
            popup.dismiss()
            secondaryActionClickListener(SecondaryAction.values()[id.toInt()], event)
        }
        popup.show()
    }
}
