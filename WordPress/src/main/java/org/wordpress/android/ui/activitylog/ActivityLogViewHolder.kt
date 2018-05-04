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
    private val clickListener: (ActivityLogListItemViewModel) -> Unit
) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.activity_log_list_item, parent, false)) {
    private val summary: TextView = itemView.findViewById(R.id.action_summary)
    private val text: TextView = itemView.findViewById(R.id.action_text)
    private val thumbnail: ImageView = itemView.findViewById(R.id.action_icon)
    private val progressBar: ProgressBar = itemView.findViewById(R.id.rewind_progress_bar)
    private val container: View = itemView.findViewById(R.id.activity_content_container)

    val header: TextView = itemView.findViewById(R.id.activity_header_text)
    val button: ImageButton = itemView.findViewById(R.id.rewind_button)

    private lateinit var activity: ActivityLogListItemViewModel

    init {
        button.setOnClickListener {
        }
    }

    fun bind(activity: ActivityLogListItemViewModel) {
        this.activity = activity

        summary.text = activity.summary
        text.text = activity.text

        val thumbIcon = activity.icon
        val thumbBackground = activity.background
        thumbnail.setImageResource(thumbIcon)
        thumbnail.setBackgroundResource(thumbBackground)

        header.text = activity.header
        container.setOnClickListener {
            clickListener(activity)
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
