package org.wordpress.android.ui.activitylog.list

import android.support.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Icon.HISTORY
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.ViewType.EVENT
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.ViewType.FOOTER
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.ViewType.HEADER
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.ViewType.LOADING
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.ViewType.PROGRESS
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.util.toFormattedTimeString
import java.util.Date

sealed class ActivityLogListItem(val type: ViewType) {
    interface IActionableItem {
        val isButtonVisible: Boolean
    }

    open fun longId(): Long = hashCode().toLong()

    data class Event(
        val activityId: String,
        val title: String,
        val description: String,
        private val gridIcon: String?,
        private val eventStatus: String?,
        val isRewindable: Boolean,
        val rewindId: String?,
        val date: Date,
        override val isButtonVisible: Boolean,
        val buttonIcon: Icon = HISTORY,
        val isProgressBarVisible: Boolean = false
    ) : ActivityLogListItem(EVENT), IActionableItem {
        val formattedDate: String = date.toFormattedDateString()
        val formattedTime: String = date.toFormattedTimeString()
        val icon = Icon.fromValue(gridIcon)
        val status = Status.fromValue(eventStatus)

        constructor(model: ActivityLogModel, rewindDisabled: Boolean = false) : this(
                model.activityID,
                model.summary,
                model.content?.text ?: "",
                model.gridicon,
                model.status,
                model.rewindable ?: false,
                model.rewindID,
                model.published,
                isButtonVisible = !rewindDisabled && model.rewindable ?: false)

        override fun longId(): Long = activityId.hashCode().toLong()
    }

    data class Progress(
        val title: String,
        val description: String
    ) : ActivityLogListItem(PROGRESS)

    data class Header(val text: String) : ActivityLogListItem(HEADER)

    object Footer : ActivityLogListItem(FOOTER)

    object Loading : ActivityLogListItem(LOADING)

    enum class ViewType(val id: Int) {
        EVENT(0),
        PROGRESS(1),
        HEADER(2),
        FOOTER(3),
        LOADING(4)
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
