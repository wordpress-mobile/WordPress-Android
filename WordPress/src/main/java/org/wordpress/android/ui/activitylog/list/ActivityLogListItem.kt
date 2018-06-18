package org.wordpress.android.ui.activitylog.list

import android.support.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

sealed class ActivityLogListItem {
    abstract val header: String
    abstract val title: String
    abstract val description: String
    abstract val icon: Icon
    abstract val status: Status
    abstract val date: Date
    abstract var isButtonVisible: Boolean
    abstract val isProgressBarVisible: Boolean
    abstract var previousItem: ActivityLogListItem?
    abstract var nextItem: ActivityLogListItem?

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityLogListItem) return false

        if (header != other.header) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (icon != other.icon) return false
        if (status != other.status) return false
        if (date != other.date) return false
        if (isButtonVisible != other.isButtonVisible) return false
        if (isProgressBarVisible != other.isProgressBarVisible) return false
        if (previousItem != other.previousItem) return false
        if (nextItem != other.nextItem) return false

        return true
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + isButtonVisible.hashCode()
        result = 31 * result + isProgressBarVisible.hashCode()
        result = 31 * result + (previousItem?.hashCode() ?: 0)
        result = 31 * result + (nextItem?.hashCode() ?: 0)
        return result
    }

    data class Event(
        val activityId: String,
        override val title: String,
        override val description: String,
        private val gridIcon: String?,
        private val eventStatus: String?,
        val isRewindable: Boolean,
        val rewindId: String?,
        override val date: Date,
        override var isButtonVisible: Boolean = isRewindable,
        override val isProgressBarVisible: Boolean = false,
        override var previousItem: ActivityLogListItem? = null,
        override var nextItem: ActivityLogListItem? = null)
        : ActivityLogListItem() {
        override val icon = Icon.fromValue(gridIcon)
        override val status = Status.fromValue(eventStatus)

        init {
            nextItem?.previousItem = this
            previousItem?.nextItem = this
        }

        constructor(model: ActivityLogModel) : this(model.activityID, model.summary, model.text, model.gridicon,
                model.status, model.rewindable ?: false, model.rewindID, model.published)

        override val header = formattedDate
    }

    data class Progress(
        override val title: String,
        override val description: String,
        override val header: String,
        override val date:Date = Date(),
        override val isProgressBarVisible: Boolean = true,
        override var isButtonVisible: Boolean = false,
        override val icon: Icon = Icon.NOTICE_OUTLINE,
        override val status: Status = Status.INFO,
        override var previousItem: ActivityLogListItem? = null,
        override var nextItem: ActivityLogListItem? = null)
        : ActivityLogListItem() {
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
        CLOUD("cloud", R.drawable.ic_cloud_white_24dp),
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
