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

class ActivityLogViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.activity_log_list_item, parent, false)) {
    private val summary: TextView = itemView.findViewById(R.id.action_summary)
    private val text: TextView = itemView.findViewById(R.id.action_text)
    private val thumbnail: ImageView = itemView.findViewById(R.id.action_icon)
    private val progressBar: ProgressBar = itemView.findViewById(R.id.rewind_progress_bar)

    val header: TextView = itemView.findViewById(R.id.activity_header_text)
    val button: ImageButton = itemView.findViewById(R.id.rewind_button)

    private var activity: ActivityLogModel? = null

    private val timeFormatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())

    init {
        button.setOnClickListener {
        }
    }

    fun bind(activity: ActivityLogModel) {
        this.activity = activity

        summary.text = activity.summary
        text.text = activity.text

        val thumbIcon = convertGridiconToDrawable(activity.gridicon)
        val thumbBackground = convertStatusToBackground(activity.status)
        thumbnail.setImageResource(thumbIcon)
        thumbnail.setBackgroundResource(thumbBackground)

        header.text = timeFormatter.format(activity.published)
    }

    fun updateProgress(progress: Int) {
        if (progress != 0 && progress != 100) {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = progress
        } else {
            progressBar.visibility = View.GONE
        }
    }

    private fun convertStatusToBackground(status: String?): Int {
        return when (status) {
            "error" -> R.drawable.shape_oval_red
            "success" -> R.drawable.shape_oval_green
            "warning" -> R.drawable.shape_oval_blue_wordpress
            else -> R.drawable.shape_oval_grey
        }
    }

    private fun convertGridiconToDrawable(gridicon: String?): Int {
        return when (gridicon) {
            "checkmark" -> R.drawable.ic_checkmark_white_24dp
            "cloud" -> R.drawable.ic_cloud_white_24dp
            "cog" -> R.drawable.ic_cog_white_24dp
            "comment" -> R.drawable.ic_comment_white_24dp
            "cross" -> R.drawable.ic_cross_white_24dp
            "domains" -> R.drawable.ic_domains_white_24dp
            "history" -> R.drawable.ic_history_white_24dp
            "image" -> R.drawable.ic_image_white_24dp
            "layout" -> R.drawable.ic_layout_white_24dp
            "lock" -> R.drawable.ic_lock_white_24dp
            "logout" -> R.drawable.ic_sign_out_white_24dp
            "mail" -> R.drawable.ic_mail_white_24dp
            "menu" -> R.drawable.ic_menu_white_24dp
            "my-sites" -> R.drawable.ic_my_sites_white_24dp
            "notice" -> R.drawable.ic_notice_white_24dp
            "notice-outline" -> R.drawable.ic_notice_outline_white_24dp
            "pages" -> R.drawable.ic_pages_white_24dp
            "plans" -> R.drawable.ic_plans_white_24dp
            "plugins" -> R.drawable.ic_plugins_white_24dp
            "posts" -> R.drawable.ic_posts_white_24dp
            "share" -> R.drawable.ic_share_white_24dp
            "shipping" -> R.drawable.ic_shipping_white_24dp
            "spam" -> R.drawable.ic_spam_white_24dp
            "themes" -> R.drawable.ic_themes_white_24dp
            "trash" -> R.drawable.ic_trash_white_24dp
            "user" -> R.drawable.ic_user_white_24dp
            else -> R.drawable.ic_notice_white_24dp
        }
    }
}
