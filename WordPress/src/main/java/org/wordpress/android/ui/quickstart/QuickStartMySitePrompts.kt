package org.wordpress.android.ui.quickstart

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.util.QuickStartUtils

/**
 * Static data about initial tutorial prompts you see when selecting one from Quick Start task list
 **/
enum class QuickStartMySitePrompts constructor(
    val taskString: String,
    val parentContainerId: Int,
    val focusedContainerId: Int,
    @StringRes val shortMessagePrompt: Int,
    @DrawableRes val iconId: Int
) {
    UPDATE_SITE_TITLE_TUTORIAL(
        QuickStartStore.QUICK_START_UPDATE_SITE_TITLE_LABEL,
        -1,
        R.id.my_site_title_label,
        R.string.quick_start_dialog_update_site_title_message_short,
        QuickStartUtils.ICON_NOT_SET
    ),
    VIEW_SITE_TUTORIAL(
        QuickStartStore.QUICK_START_VIEW_SITE_LABEL,
        -1,
        R.id.my_site_subtitle_label,
        R.string.quick_start_dialog_view_your_site_message_short,
        QuickStartUtils.ICON_NOT_SET
    ),
    SHARE_SITE_TUTORIAL(
        QuickStartStore.QUICK_START_ENABLE_POST_SHARING_LABEL,
        -1,
        -1,
        R.string.quick_start_dialog_enable_sharing_message_short_sharing,
        R.drawable.ic_share_white_24dp
    ),
    PUBLISH_POST_TUTORIAL(
        QuickStartStore.QUICK_START_PUBLISH_POST_LABEL,
        R.id.fab_container,
        R.id.fab_button,
        R.string.quick_start_dialog_create_new_post_message_short,
        R.drawable.ic_create_white_24dp
    ),
    FOLLOW_SITES_TUTORIAL(
        QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL,
        R.id.root_view_main,
        R.id.bottom_nav_reader_button,
        R.string.quick_start_dialog_follow_sites_message_short_reader,
        R.drawable.ic_reader_white_24dp
    ),
    UPLOAD_SITE_ICON(
        QuickStartStore.QUICK_START_UPLOAD_SITE_ICON_LABEL,
        -1,
        R.id.my_site_blavatar,
        R.string.quick_start_dialog_upload_site_icon_message_short,
        QuickStartUtils.ICON_NOT_SET
    ),
    CHECK_STATS(
        QuickStartStore.QUICK_START_CHECK_STATS_LABEL,
        -1,
        R.id.quick_action_stats_button,
        R.string.quick_start_dialog_check_stats_message_short,
        R.drawable.ic_stats_alt_white_24dp
    ),
    REVIEW_PAGES(
        QuickStartStore.QUICK_START_REVIEW_PAGES_LABEL,
        -1,
        R.id.quick_action_pages_button,
        R.string.quick_start_dialog_review_pages_message_short,
        R.drawable.ic_pages_white_24dp
    ),
    CHECK_NOTIFICATIONS(
        QuickStartStore.QUICK_START_CHECK_NOTIFIATIONS_LABEL,
        R.id.root_view_main,
        R.id.bottom_nav_notifications_button,
        R.string.quick_start_dialog_check_notifications_message_short,
        R.drawable.ic_bell_white_24dp
    ),
    UPLOAD_MEDIA(
        QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL,
        -1,
        R.id.quick_action_media_button,
        R.string.quick_start_dialog_upload_media_message,
        R.drawable.ic_media_white_24dp
    );

    companion object {
        const val KEY = "my_site_tutorial_prompts"

        @JvmStatic
        fun getPromptDetailsForTask(task: QuickStartTask): QuickStartMySitePrompts? {
            values().forEach {
                if (it.taskString == task.string) return it
            }
            return null
        }
    }
}
