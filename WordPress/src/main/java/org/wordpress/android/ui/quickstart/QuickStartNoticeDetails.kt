package org.wordpress.android.ui.quickstart

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask

/**
 * Static data that represents info that goes into Quick Start notices
 */
enum class QuickStartNoticeDetails(
    val taskString: String,
    @StringRes val titleResId: Int,
    @StringRes val messageResId: Int
) {
    UPDATE_SITE_TITLE(
        QuickStartStore.QUICK_START_UPDATE_SITE_TITLE_LABEL,
        R.string.quick_start_list_update_site_title_title,
        R.string.quick_start_list_update_site_title_subtitle
    ),
    VIEW_SITE_TUTORIAL(
        QuickStartStore.QUICK_START_VIEW_SITE_LABEL,
        R.string.quick_start_dialog_view_site_title,
        R.string.quick_start_dialog_view_site_message
    ),
    SHARE_SITE_TUTORIAL(
        QuickStartStore.QUICK_START_ENABLE_POST_SHARING_LABEL,
        R.string.quick_start_dialog_share_site_title,
        R.string.quick_start_dialog_share_site_message
    ),
    PUBLISH_POST_TUTORIAL(
        QuickStartStore.QUICK_START_PUBLISH_POST_LABEL,
        R.string.quick_start_dialog_publish_post_title,
        R.string.quick_start_dialog_publish_post_message
    ),
    FOLLOW_SITES_TUTORIAL(
        QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL,
        R.string.quick_start_dialog_follow_sites_title,
        R.string.quick_start_dialog_follow_sites_message
    ),
    UPLOAD_SITE_ICON(
        QuickStartStore.QUICK_START_UPLOAD_SITE_ICON_LABEL,
        R.string.quick_start_dialog_upload_icon_title,
        R.string.quick_start_dialog_upload_icon_message
    ),
    CHECK_STATS(
        QuickStartStore.QUICK_START_CHECK_STATS_LABEL,
        R.string.quick_start_dialog_check_stats_title,
        R.string.quick_start_dialog_check_stats_message
    ),
    REVIEW_PAGES(
        QuickStartStore.QUICK_START_REVIEW_PAGES_LABEL,
        R.string.quick_start_dialog_review_pages_title,
        R.string.quick_start_dialog_review_pages_message
    ),
    CHECK_NOTIFICATIONS(
        QuickStartStore.QUICK_START_CHECK_NOTIFIATIONS_LABEL,
        R.string.quick_start_dialog_check_notifications_title,
        R.string.quick_start_dialog_check_notifications_message
    ),
    UPLOAD_MEDIA(
        QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL,
        R.string.quick_start_list_upload_media_title,
        R.string.quick_start_list_upload_media_subtitle
    );

    companion object {
        fun getNoticeForTask(task: QuickStartTask): QuickStartNoticeDetails? {
            for (quickStartTaskDetails in values()) {
                if (quickStartTaskDetails.taskString == task.string) {
                    return quickStartTaskDetails
                }
            }
            return null
        }
    }
}
