package org.wordpress.android.viewmodel.activitylog

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

data class ActivityLogListItemViewModel(
    val activityId: String,
    val summary: String,
    val text: String,
    private val gridIcon: String?,
    private val status: String?,
    val isRewindable: Boolean,
    val rewindId: String?,
    private val datePublished: Date) {
    private val timeFormatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())

    companion object {
        @JvmStatic
        fun fromDomainModel(model: ActivityLogModel): ActivityLogListItemViewModel {
            return ActivityLogListItemViewModel(model.activityID, model.summary, model.text, model.gridicon,
                    model.status, model.rewindable ?: false, model.rewindID, model.published)
        }
    }

    val header: String by lazy {
        timeFormatter.format(datePublished)
    }

    val background by lazy {
        convertStatusToBackground(status)
    }

    val icon by lazy {
        convertGridIconToDrawable(gridIcon)
    }

    fun isHeaderVisible(previous: ActivityLogListItemViewModel?): Boolean {
        return if (previous != null) {
            header != previous.header
        } else {
            true
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

    private fun convertGridIconToDrawable(gridIcon: String?): Int {
        return when (gridIcon) {
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
