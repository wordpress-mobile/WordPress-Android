package org.wordpress.android.ui.activitylog.list

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import org.wordpress.android.R

class EventItemViewHolder(
    parent: ViewGroup,
    private val itemClickListener: (ActivityLogListItem) -> Unit,
    private val rewindClickListener: (ActivityLogListItem) -> Unit
) : ActivityLogViewHolder(parent, R.layout.activity_log_list_event_item) {
    private val summary: TextView = itemView.findViewById(R.id.action_summary)
    private val text: TextView = itemView.findViewById(R.id.action_text)
    private val thumbnail: ImageView = itemView.findViewById(R.id.action_icon)
    private val container: View = itemView.findViewById(R.id.activity_content_container)
    private val actionButton: ImageButton = itemView.findViewById(R.id.action_button)

    override fun updateChanges(bundle: Bundle) {
        if (bundle.containsKey(ActivityLogDiffCallback.LIST_ITEM_BUTTON_VISIBILITY_KEY)) {
            actionButton.visibility =
                    if (bundle.getBoolean(ActivityLogDiffCallback.LIST_ITEM_BUTTON_VISIBILITY_KEY))
                        View.VISIBLE
                    else
                        View.GONE
        }
    }

    fun bind(activity: ActivityLogListItem.Event) {
        summary.text = activity.title
        text.text = activity.description

        if (activity.isButtonVisible) {
            ContextCompat.getDrawable(container.context, activity.buttonIcon.drawable)?.let { buttonIcon ->
                val wrapDrawable = DrawableCompat.wrap(buttonIcon).mutate()
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(container.context, R.color.primary_40))
                actionButton.setImageDrawable(DrawableCompat.unwrap(wrapDrawable))
                actionButton.visibility = View.VISIBLE
            }
        } else {
            actionButton.visibility = View.GONE
        }

        thumbnail.setImageResource(activity.icon.drawable)
        thumbnail.setBackgroundResource(activity.status.color)
        container.setOnClickListener {
            itemClickListener(activity)
        }

        actionButton.setOnClickListener {
            rewindClickListener(activity)
        }
    }
}
