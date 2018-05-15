package org.wordpress.android.ui.activitylog

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.viewmodel.activitylog.ActivityLogListItemViewModel

class ActivityLogViewHolder(
    parent: ViewGroup,
    private val itemClickListener: (ActivityLogListItemViewModel) -> Unit,
    private val rewindClickListener: (String?) -> Unit
) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.activity_log_list_item, parent, false)) {
    private val summary: TextView = itemView.findViewById(R.id.action_summary)
    private val text: TextView = itemView.findViewById(R.id.action_text)
    private val thumbnail: ImageView = itemView.findViewById(R.id.action_icon)
    private val progressBar: ProgressBar = itemView.findViewById(R.id.rewind_progress_bar)
    private val container: View = itemView.findViewById(R.id.activity_content_container)
    private val rewindButton: ImageButton = itemView.findViewById(R.id.rewind_button)
    private val header: TextView = itemView.findViewById(R.id.activity_header_text)

    private lateinit var activity: ActivityLogListItemViewModel

    fun bind(activity: ActivityLogListItemViewModel, previous: ActivityLogListItemViewModel?) {
        this.activity = activity

        summary.text = activity.summary
        text.text = activity.text
        header.text = activity.header

        rewindButton.visibility = if (activity.isRewindable) View.VISIBLE else View.GONE
        header.visibility = if (activity.isHeaderVisible(previous)) View.VISIBLE else View.GONE

        thumbnail.setImageResource(activity.icon)
        thumbnail.setBackgroundResource(activity.background)
        container.setOnClickListener {
            itemClickListener(activity)
        }

        rewindButton.setOnClickListener {
            rewindClickListener(activity.rewindId)
        }
    }

    fun updateProgress(progress: Int) {
        if (progress != 0 && progress != 100) {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = progress
        } else {
            progressBar.visibility = View.GONE
        }
    }
}
