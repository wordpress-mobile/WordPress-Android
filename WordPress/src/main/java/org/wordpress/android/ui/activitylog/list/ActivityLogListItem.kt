package org.wordpress.android.ui.activitylog.list

import android.support.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

sealed class ActivityLogListItem(
    val title: String,
    val description: String,
    val icon: Icon,
    val status: Status,
    val date: Date,
    var isButtonVisible: Boolean,
    val isProgressBarVisible: Boolean,
    var previousItem: ActivityLogListItem?,
    var nextItem: ActivityLogListItem?
) {
    abstract val header: String

    var isHeaderVisible: Boolean = false
        get() = isHeaderVisible(previousItem)

    val formattedDate: String by lazy {
        DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(date)
    }

    val formattedTime: String by lazy {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
    }

    private fun isHeaderVisible(previous: ActivityLogListItem?): Boolean {
        return if (previous != null) {
            header != previous.header
        } else {
            true
        }
    }

    data class Event(
        val activityId: String,
        private val summary: String,
        private val text: String,
        private val gridIcon: String?,
        private val eventStatus: String?,
        val isRewindable: Boolean,
        val rewindId: String?,
        private val datePublished: Date,
        private val previous: ActivityLogListItem? = null,
        private val next: ActivityLogListItem? = null)
        : ActivityLogListItem(summary, text, Icon.fromValue(gridIcon), Status.fromValue(eventStatus),
            datePublished, isRewindable, false, previous, next) {
        init {
            nextItem?.previousItem = this
            previousItem?.nextItem = this
        }

        constructor(model: ActivityLogModel) : this(model.activityID, model.summary, model.text, model.gridicon,
                model.status, model.rewindable ?: false, model.rewindID, model.published)

        override val header = formattedDate
    }

    data class Progress(
        private val progressTitle: String,
        private val message: String,
        override val header: String,
        private val next: ActivityLogListItem? = null)
        : ActivityLogListItem(message, progressTitle, Icon.NOTICE_OUTLINE, Status.INFO, Date(),false,
            true, null, next) {
        init {
            nextItem?.previousItem = this
        }
    }

    enum class Status(val value: String, @DrawableRes val color: Int) {
        NEGATIVE("error", R.drawable.shape_oval_red),
        INFO("warning", R.drawable.shape_oval_blue_wordpress),
        POSITIVE("success", R.drawable.shape_oval_green),
        NEUTRAL("", R.drawable.shape_oval_grey);

        companion object {
            private val map = Status.values().associateBy(Status::value)
            fun fromValue(value: String?) = map[value] ?: NEUTRAL
        }
    }

    enum class Icon(val value: String, @DrawableRes val drawable: Int) {
        CHECKMARK("checkmark", R.drawable.ic_checkmark_white_24dp),
        CLOUT("cloud", R.drawable.ic_cloud_white_24dp),
        COG("cog", R.drawable.ic_cog_white_24dp),
        COMMENT("comment", R.drawable.ic_comment_white_24dp),
        CROSS("cross", R.drawable.ic_cross_white_24dp),
        DOMAINS("domains", R.drawable.ic_domains_white_24dp),
        HISTORY("history", R.drawable.ic_history_white_24dp),
        IMAGE("image", R.drawable.ic_image_white_24dp),
        LAYOUT("layout", R.drawable.ic_layout_white_24dp),
        LOCK("lock", R.drawable.ic_lock_white_24dp),
        LOGOUT("logout", R.drawable.ic_sign_out_white_24dp),
        MAIL("mail", R.drawable.ic_mail_white_24dp),
        MENU("menu", R.drawable.ic_menu_white_24dp),
        MY_SITES("my-sites", R.drawable.ic_my_sites_white_24dp),
        NOTICE("notice", R.drawable.ic_notice_white_24dp),
        NOTICE_OUTLINE("notice-outline", R.drawable.ic_notice_outline_white_24dp),
        PAGES("pages", R.drawable.ic_pages_white_24dp),
        PLANS("plans", R.drawable.ic_plans_white_24dp),
        PLUGINS("plugins", R.drawable.ic_plugins_white_24dp),
        POSTS("posts", R.drawable.ic_posts_white_24dp),
        SHARE("share", R.drawable.ic_share_white_24dp),
        SHIPPING("shipping", R.drawable.ic_shipping_white_24dp),
        SPAM("spam", R.drawable.ic_spam_white_24dp),
        THEMES("themes", R.drawable.ic_themes_white_24dp),
        TRASH("trash", R.drawable.ic_trash_white_24dp),
        USER("user", R.drawable.ic_user_white_24dp),
        DEFAULT("", R.drawable.ic_notice_white_24dp);

        companion object {
            private val map = Icon.values().associateBy(Icon::value)
            fun fromValue(value: String?) = map[value] ?: DEFAULT
        }
    }
}
