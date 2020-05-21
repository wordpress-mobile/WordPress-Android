package org.wordpress.android.ui.quickstart

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask

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
    VIEW_SITE_TUTORIAL(
            QuickStartTask.VIEW_SITE,
            R.id.my_site_scroll_view_root,
            R.id.row_view_site,
            R.string.quick_start_dialog_view_site_message_short,
            R.drawable.ic_globe_white_24dp
    ),
    CHOSE_THEME_TUTORIAL(
            QuickStartTask.CHOOSE_THEME,
            R.id.my_site_scroll_view_root,
            R.id.row_themes,
            R.string.quick_start_dialog_browse_themes_message_short,
            R.drawable.ic_themes_white_24dp
    ),
    CUSTOMIZE_SITE_TUTORIAL(
            QuickStartTask.CUSTOMIZE_SITE,
            R.id.my_site_scroll_view_root,
            R.id.row_themes,
            R.string.quick_start_dialog_customize_site_message_short_themes,
            R.drawable.ic_themes_white_24dp
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
            R.id.my_site_scroll_view_root,
            R.id.row_blog_posts,
            R.string.quick_start_dialog_create_new_post_message_short,
            -1
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
            -1
    ),
    CREATE_NEW_PAGE(
            QuickStartTask.CREATE_NEW_PAGE,
            R.id.my_site_scroll_view_root,
            R.id.row_pages,
            R.string.quick_start_dialog_create_new_page_message_short,
            -1
    ),
    CHECK_STATS(
            QuickStartTask.CHECK_STATS,
            R.id.my_site_scroll_view_root,
            R.id.row_stats,
            R.string.quick_start_dialog_check_stats_message_short,
            R.drawable.ic_stats_alt_white_24dp
    ),
    EXPLORE_PLANS(
            QuickStartTask.EXPLORE_PLANS,
            R.id.my_site_scroll_view_root,
            R.id.row_plan,
            R.string.quick_start_dialog_explore_plans_message_short,
            R.drawable.ic_plans_white_24dp
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
