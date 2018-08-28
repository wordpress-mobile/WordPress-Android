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
    val iconId: Int,
    val promptDialogTitleId: Int,
    val promptDialogMessageId: Int
) {
    VIEW_SITE_TUTORIAL(
            QuickStartTask.VIEW_SITE,
            R.id.my_site_scroll_view_root,
            R.id.row_view_site,
            R.string.quick_start_dialog_view_site_message_short,
            R.drawable.ic_globe_grey_24dp,
            R.string.quick_start_dialog_view_site_title,
            R.string.quick_start_dialog_view_site_message),
    CHOSE_THEME_TUTORIAL(
            QuickStartTask.CHOOSE_THEME,
            R.id.my_site_scroll_view_root,
            R.id.row_themes,
            R.string.quick_start_dialog_choose_theme_message_short,
            R.drawable.ic_themes_grey_24dp,
            R.string.quick_start_dialog_choose_theme_title,
            R.string.quick_start_dialog_choose_theme_message),
    CUSTOMIZE_SITE_TUTORIAL(
            QuickStartTask.CUSTOMIZE_SITE,
            R.id.my_site_scroll_view_root,
            R.id.row_themes,
            R.string.quick_start_dialog_customize_site_message_short_themes,
            R.drawable.ic_themes_grey_24dp,
            R.string.quick_start_dialog_customize_site_title,
            R.string.quick_start_dialog_customize_site_message),
    SHARE_SITE_TUTORIAL(
            QuickStartTask.SHARE_SITE,
            R.id.my_site_scroll_view_root,
            R.id.row_sharing,
            R.string.quick_start_dialog_share_site_message_short_sharing,
            R.drawable.ic_share_white_24dp,
            R.string.quick_start_dialog_share_site_title,
            R.string.quick_start_dialog_share_site_message),
    PUBLISH_POST_TUTORIAL(
            QuickStartTask.PUBLISH_POST,
            R.id.root_view_main,
            R.id.bottom_nav_new_post_button,
            R.string.quick_start_dialog_publish_post_message_short,
            R.drawable.ic_create_white_24dp,
            R.string.quick_start_dialog_publish_post_title,
            R.string.quick_start_dialog_publish_post_message),
    FOLLOW_SITES_TUTORIAL(
            QuickStartTask.FOLLOW_SITE,
            R.id.root_view_main,
            R.id.bottom_nav_reader_button,
            R.string.quick_start_dialog_follow_sites_message_short_reader,
            R.drawable.ic_reader_white_32dp,
            R.string.quick_start_dialog_follow_sites_title,
            R.string.quick_start_dialog_follow_sites_message);

    companion object {
        const val KEY = "my_site_tutorial_prompts"

        @JvmStatic
        fun getPromptDetailsForTask(task: QuickStartTask): QuickStartMySitePrompts? {
            QuickStartMySitePrompts.values().forEach {
                if (it.task == task) return it
            }
            return null
        }

        @JvmStatic
        fun isTargetingBottomNavBar(task: QuickStartTask): Boolean {
            return task == QuickStartTask.FOLLOW_SITE || task == QuickStartTask.PUBLISH_POST
        }
    }
}
