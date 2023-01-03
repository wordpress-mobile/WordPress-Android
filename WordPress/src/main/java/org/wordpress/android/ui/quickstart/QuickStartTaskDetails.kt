package org.wordpress.android.ui.quickstart

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask

/**
 * Static data that represents user facing description of quick start tasks.
 */
enum class QuickStartTaskDetails(
    val taskString: String,
    @StringRes val titleResId: Int,
    @StringRes val subtitleResId: Int,
    @DrawableRes val iconResId: Int,
    @ColorRes val iconBackgroundColorResId: Int,
    val showSubtitle: Boolean = false
) {
    CREATE_SITE_TUTORIAL(
        QuickStartStore.QUICK_START_CREATE_SITE_LABEL,
        R.string.quick_start_list_create_site_title,
        R.string.quick_start_list_create_site_subtitle,
        R.drawable.ic_gridicons_site_white_24dp,
        R.color.gray_20
    ),
    UPDATE_SITE_TITLE(
        QuickStartStore.QUICK_START_UPDATE_SITE_TITLE_LABEL,
        R.string.quick_start_list_update_site_title_title,
        R.string.quick_start_list_update_site_title_subtitle,
        R.drawable.ic_pencil_white_24dp,
        R.color.red_40
    ),
    VIEW_SITE_TUTORIAL(
        QuickStartStore.QUICK_START_VIEW_SITE_LABEL,
        R.string.quick_start_list_view_site_title,
        R.string.quick_start_list_view_site_subtitle,
        R.drawable.ic_external_white_24dp,
        R.color.yellow_20
    ),
    SHARE_SITE_TUTORIAL(
        QuickStartStore.QUICK_START_ENABLE_POST_SHARING_LABEL,
        R.string.quick_start_list_enable_sharing_title,
        R.string.quick_start_list_enable_sharing_subtitle,
        R.drawable.ic_share_white_24dp,
        R.color.blue_40
    ),
    PUBLISH_POST_TUTORIAL(
        QuickStartStore.QUICK_START_PUBLISH_POST_LABEL,
        R.string.quick_start_list_publish_post_title,
        R.string.quick_start_list_publish_post_subtitle,
        R.drawable.ic_posts_white_24dp,
        R.color.green_30
    ),
    FOLLOW_SITES_TUTORIAL(
        QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL,
        R.string.quick_start_list_follow_site_title,
        R.string.quick_start_list_follow_site_subtitle,
        R.drawable.ic_reader_white_24dp,
        R.color.pink_40
    ),
    UPLOAD_SITE_ICON(
        QuickStartStore.QUICK_START_UPLOAD_SITE_ICON_LABEL,
        R.string.quick_start_list_upload_icon_title,
        R.string.quick_start_list_upload_icon_subtitle,
        R.drawable.ic_globe_white_24dp,
        R.color.pink_40,
        true
    ),
    CHECK_STATS(
        QuickStartStore.QUICK_START_CHECK_STATS_LABEL,
        R.string.quick_start_list_check_stats_title,
        R.string.quick_start_list_check_stats_subtitle,
        R.drawable.ic_stats_alt_white_24dp,
        R.color.orange_30
    ),
    REVIEW_PAGES(
        QuickStartStore.QUICK_START_REVIEW_PAGES_LABEL,
        R.string.quick_start_list_review_pages_title,
        R.string.quick_start_list_review_pages_subtitle,
        R.drawable.ic_pages_white_24dp,
        R.color.celadon_30
    ),
    CHECK_NOTIFICATIONS(
        QuickStartStore.QUICK_START_CHECK_NOTIFIATIONS_LABEL,
        R.string.quick_start_list_check_notification_title,
        R.string.quick_start_list_check_notification_subtitle,
        R.drawable.ic_bell_white_24dp,
        R.color.purple_40
    ),
    UPLOAD_MEDIA(
        QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL,
        R.string.quick_start_list_upload_media_title,
        R.string.quick_start_list_upload_media_subtitle,
        R.drawable.ic_media_white_24dp,
        R.color.celadon_30
    );

    companion object {
        const val KEY = "quick_start_task_details"

        @JvmStatic
        fun getDetailsForTask(task: QuickStartTask): QuickStartTaskDetails? {
            for (quickStartTaskDetails in values()) {
                if (quickStartTaskDetails.taskString == task.string) {
                    return quickStartTaskDetails
                }
            }
            return null
        }
    }
}
