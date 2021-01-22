package org.wordpress.android.ui.quickstart

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.util.QuickStartUtils.Companion.ICON_NOT_SET

/**
 * Static data about initial tutorial prompts you see when selecting one from Quick Start task list
 **/
enum class QuickStartMySitePrompts constructor(
    val task: QuickStartTask,
    val parentContainerId: Int,
    val focusedContainerId: Int,
    val shortMessagePrompt: Int,
    val iconId: Int
) {
    UPDATE_SITE_TITLE_TUTORIAL(
            QuickStartTask.UPDATE_SITE_TITLE,
            R.id.my_site_scroll_view_root,
            R.id.my_site_title_label,
            R.string.quick_start_dialog_update_site_title_message_short,
            ICON_NOT_SET
    ),
    VIEW_SITE_TUTORIAL(
            QuickStartTask.VIEW_SITE,
            R.id.my_site_scroll_view_root,
            R.id.row_view_site,
            R.string.quick_start_dialog_view_site_message_short,
            R.drawable.ic_globe_white_24dp
    ),
    SHARE_SITE_TUTORIAL(
            QuickStartTask.ENABLE_POST_SHARING,
            R.id.my_site_scroll_view_root,
            R.id.row_sharing,
            R.string.quick_start_dialog_enable_sharing_message_short_sharing,
            R.drawable.ic_share_white_24dp
    ),
    PUBLISH_POST_TUTORIAL(
            QuickStartTask.PUBLISH_POST,
            R.id.fab_container,
            R.id.fab_button,
            R.string.quick_start_dialog_create_new_post_message_short,
            R.drawable.ic_create_white_24dp
    ),
    FOLLOW_SITES_TUTORIAL(
            QuickStartTask.FOLLOW_SITE,
            R.id.root_view_main,
            R.id.bottom_nav_reader_button,
            R.string.quick_start_dialog_follow_sites_message_short_reader,
            R.drawable.ic_reader_white_24dp
    ),
    UPLOAD_SITE_ICON(
            QuickStartTask.UPLOAD_SITE_ICON,
            R.id.my_site_scroll_view_root,
            R.id.my_site_blavatar,
            R.string.quick_start_dialog_upload_site_icon_message_short,
            ICON_NOT_SET
    ),
    CHECK_STATS(
            QuickStartTask.CHECK_STATS,
            R.id.my_site_scroll_view_root,
            R.id.quick_action_stats_button,
            R.string.quick_start_dialog_check_stats_message_short,
            R.drawable.ic_stats_alt_white_24dp
    ),
    EXPLORE_PLANS(
            QuickStartTask.EXPLORE_PLANS,
            R.id.my_site_scroll_view_root,
            R.id.row_plan,
            R.string.quick_start_dialog_explore_plans_message_short,
            R.drawable.ic_plans_white_24dp
    ),
    REVIEW_PAGES(
            QuickStartTask.REVIEW_PAGES,
            R.id.my_site_scroll_view_root,
            R.id.quick_action_pages_button,
            R.string.quick_start_dialog_review_pages_message_short,
            R.drawable.ic_pages_white_24dp
    ),
    EDIT_HOMEPAGE(
            QuickStartTask.EDIT_HOMEPAGE,
            R.id.my_site_scroll_view_root,
            R.id.quick_action_pages_button,
            R.string.quick_start_dialog_edit_homepage_message_short,
            R.drawable.ic_pages_white_24dp
    );

    companion object {
        const val KEY = "my_site_tutorial_prompts"

        @JvmStatic
        fun getPromptDetailsForTask(task: QuickStartTask): QuickStartMySitePrompts? {
            values().forEach {
                if (it.task == task) return it
            }
            return null
        }

        @JvmStatic
        fun isTargetingBottomNavBar(task: QuickStartTask): Boolean {
            return task == QuickStartTask.FOLLOW_SITE
        }
    }
}
