package org.wordpress.android.ui.activitylog

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.RequestManager
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import java.text.DateFormat
import java.util.Locale

class ActivityLogViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context)
        .inflate(R.layout.activity_log_list_item, parent, false)) {
    private val subject: TextView = itemView.findViewById(R.id.action_subject)
    private val detail: TextView = itemView.findViewById(R.id.action_detail)
    private val header: TextView = itemView.findViewById(R.id.activity_header_text)
    private val thumbnail : TextView = itemView.findViewById(R.id.action_icon)
    private val progressBar : ProgressBar = itemView.findViewById(R.id.rewind_progress_bar)
    private val button : ImageButton = itemView.findViewById(R.id.rewind_button)

    private var activity : ActivityLogModel? = null

    private val timeFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.getDefault())

    init {
        button.setOnClickListener {
        }
    }

    fun bind(activity: ActivityLogModel?) {
        this.activity = activity

        subject.text = activity?.name
        detail.text = activity?.summary
        thumbnail.text = activity?.gridicon
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
