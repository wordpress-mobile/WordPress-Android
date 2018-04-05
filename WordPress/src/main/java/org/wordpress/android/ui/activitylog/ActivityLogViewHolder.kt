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
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import java.text.DateFormat
import java.util.Locale

class ActivityLogViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context)
        .inflate(R.layout.activity_log_list_item, parent, false)) {
    private val summary: TextView = itemView.findViewById(R.id.action_summary)
    private val text: TextView = itemView.findViewById(R.id.action_text)
    val header: TextView = itemView.findViewById(R.id.activity_header_text)
    private val thumbnail : ImageView = itemView.findViewById(R.id.action_icon)
    private val progressBar : ProgressBar = itemView.findViewById(R.id.rewind_progress_bar)
    private val button : ImageButton = itemView.findViewById(R.id.rewind_button)

    private var activity : ActivityLogModel? = null

    private val timeFormatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())

    init {
        button.setOnClickListener {
        }
    }

    fun bind(activity: ActivityLogModel?) {
        this.activity = activity

        summary.text = activity?.summary
        text.text = activity?.text

        val thumb = when (activity?.gridicon) {
            "comment" -> R.drawable.ic_comment_white_24dp
            else -> R.drawable.ic_checkmark_white_24dp
        }
        
        thumbnail.setImageResource(thumb)
        header.text = timeFormatter.format(activity?.published)
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
