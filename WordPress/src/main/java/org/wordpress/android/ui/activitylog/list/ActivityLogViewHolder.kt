package org.wordpress.android.ui.activitylog.list

import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R

class ActivityLogViewHolder(
    parent: ViewGroup,
    private val itemClickListener: (ActivityLogListItem) -> Unit,
    private val rewindClickListener: (ActivityLogListItem) -> Unit
) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.activity_log_list_item, parent, false)) {
    private val summary: TextView = itemView.findViewById(R.id.action_summary)
    private val text: TextView = itemView.findViewById(R.id.action_text)
    private val thumbnail: ImageView = itemView.findViewById(R.id.action_icon)
    private val progressBarContainer: View = itemView.findViewById(R.id.rewind_progress_bar_container)
    private val container: View = itemView.findViewById(R.id.activity_content_container)
    val actionButton: ImageButton = itemView.findViewById(R.id.action_button)
    val header: TextView = itemView.findViewById(R.id.activity_header_text)

    private lateinit var activity: ActivityLogListItem

    fun bind(activity: ActivityLogListItem) {
        this.activity = activity

        summary.text = activity.title
        text.text = activity.description
        header.text = activity.header

        progressBarContainer.visibility = if (activity.isProgressBarVisible) View.VISIBLE else View.GONE
        header.visibility = if (activity.isHeaderVisible) View.VISIBLE else View.GONE

        if (activity.isButtonVisible) {
            ContextCompat.getDrawable(container.context, activity.buttonIcon.drawable)?.let { buttonIcon ->
                val wrapDrawable = DrawableCompat.wrap(buttonIcon).mutate()
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(container.context, R.color.blue_medium))
                actionButton.setImageDrawable(DrawableCompat.unwrap(wrapDrawable))
                actionButton.visibility = View.VISIBLE
            }
        }
        else {
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
