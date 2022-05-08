package org.wordpress.android.ui.quickstart

import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask

/**
 * Static data that represents user facing description of quick start tasks.
 */
enum class QuickStartTaskDetails(
    val taskString: String,
    val titleResId: Int,
    val subtitleResId: Int,
    val iconResId: Int
) {
    CREATE_SITE_TUTORIAL(
            QuickStartStore.QUICK_START_CREATE_SITE_LABEL,
            string.quick_start_list_create_site_title,
            string.quick_start_list_create_site_subtitle,
            drawable.ic_gridicons_site_white_24dp
    ),
    UPDATE_SITE_TITLE(
            QuickStartStore.QUICK_START_UPDATE_SITE_TITLE_LABEL,
            string.quick_start_list_update_site_title_title,
            string.quick_start_list_update_site_title_subtitle,
            drawable.ic_pencil_white_24dp
    ),
    VIEW_SITE_TUTORIAL(
            QuickStartStore.QUICK_START_VIEW_SITE_LABEL,
            string.quick_start_list_view_site_title,
            string.quick_start_list_view_site_subtitle,
            drawable.ic_external_white_24dp
    ),
    SHARE_SITE_TUTORIAL(
            QuickStartStore.QUICK_START_ENABLE_POST_SHARING_LABEL,
            string.quick_start_list_enable_sharing_title,
            string.quick_start_list_enable_sharing_subtitle,
            drawable.ic_share_white_24dp
    ),
    PUBLISH_POST_TUTORIAL(
            QuickStartStore.QUICK_START_PUBLISH_POST_LABEL,
            string.quick_start_list_publish_post_title,
            string.quick_start_list_publish_post_subtitle,
            drawable.ic_posts_white_24dp
    ),
    FOLLOW_SITES_TUTORIAL(
            QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL,
            string.quick_start_list_follow_site_title,
            string.quick_start_list_follow_site_subtitle,
            drawable.ic_reader_white_24dp
    ),
    UPLOAD_SITE_ICON(
            QuickStartStore.QUICK_START_UPLOAD_SITE_ICON_LABEL,
            string.quick_start_list_upload_icon_title,
            string.quick_start_list_upload_icon_subtitle,
            drawable.ic_globe_white_24dp
    ),
    CHECK_STATS(
            QuickStartStore.QUICK_START_CHECK_STATS_LABEL,
            string.quick_start_list_check_stats_title,
            string.quick_start_list_check_stats_subtitle,
            drawable.ic_stats_alt_white_24dp
    ),
    EXPLORE_PLANS(
            QuickStartStore.QUICK_START_EXPLORE_PLANS_LABEL,
            string.quick_start_list_explore_plans_title,
            string.quick_start_list_explore_plans_subtitle,
            drawable.ic_plans_white_24dp
    ),
    EDIT_HOMEPAGE(
            QuickStartStore.QUICK_START_EDIT_HOMEPAGE_LABEL,
            string.quick_start_list_edit_homepage_title,
            string.quick_start_list_edit_homepage_subtitle,
            drawable.ic_homepage_16dp
    ),
    REVIEW_PAGES(
            QuickStartStore.QUICK_START_REVIEW_PAGES_LABEL,
            string.quick_start_list_review_pages_title,
            string.quick_start_list_review_pages_subtitle,
            drawable.ic_pages_white_24dp
    );

    companion object {
        const val KEY = "quick_start_task_details"
        @JvmStatic fun getDetailsForTask(task: QuickStartTask): QuickStartTaskDetails? {
            for (quickStartTaskDetails in values()) {
                if (quickStartTaskDetails.taskString == task.string) {
                    return quickStartTaskDetails
                }
            }
            return null
        }
    }
}
