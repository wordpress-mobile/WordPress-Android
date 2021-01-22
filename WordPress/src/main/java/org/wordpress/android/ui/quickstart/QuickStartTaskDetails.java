package org.wordpress.android.ui.quickstart;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;

/**
 * Static data that represents user facing description of quick start tasks.
 **/
public enum QuickStartTaskDetails {
    CREATE_SITE_TUTORIAL(
            QuickStartTask.CREATE_SITE,
            R.string.quick_start_list_create_site_title,
            R.string.quick_start_list_create_site_subtitle,
            R.drawable.ic_plus_white_24dp
    ),
    UPDATE_SITE_TITLE(
            QuickStartTask.UPDATE_SITE_TITLE,
            R.string.quick_start_list_update_site_title_title,
            R.string.quick_start_list_update_site_title_subtitle,
            R.drawable.ic_pencil_white_24dp
    ),
    VIEW_SITE_TUTORIAL(
            QuickStartTask.VIEW_SITE,
            R.string.quick_start_list_view_site_title,
            R.string.quick_start_list_view_site_subtitle,
            R.drawable.ic_external_white_24dp
    ),
    SHARE_SITE_TUTORIAL(
            QuickStartTask.ENABLE_POST_SHARING,
            R.string.quick_start_list_enable_sharing_title,
            R.string.quick_start_list_enable_sharing_subtitle,
            R.drawable.ic_share_white_24dp
    ),
    PUBLISH_POST_TUTORIAL(
            QuickStartTask.PUBLISH_POST,
            R.string.quick_start_list_publish_post_title,
            R.string.quick_start_list_publish_post_subtitle,
            R.drawable.ic_posts_white_24dp
    ),
    FOLLOW_SITES_TUTORIAL(
            QuickStartTask.FOLLOW_SITE,
            R.string.quick_start_list_follow_site_title,
            R.string.quick_start_list_follow_site_subtitle,
            R.drawable.ic_reader_follow_white_24dp
    ),
    UPLOAD_SITE_ICON(
            QuickStartTask.UPLOAD_SITE_ICON,
            R.string.quick_start_list_upload_icon_title,
            R.string.quick_start_list_upload_icon_subtitle,
            R.drawable.ic_globe_white_24dp
    ),
    CHECK_STATS(
            QuickStartTask.CHECK_STATS,
            R.string.quick_start_list_check_stats_title,
            R.string.quick_start_list_check_stats_subtitle,
            R.drawable.ic_stats_alt_white_24dp
    ),
    EXPLORE_PLANS(
            QuickStartTask.EXPLORE_PLANS,
            R.string.quick_start_list_explore_plans_title,
            R.string.quick_start_list_explore_plans_subtitle,
            R.drawable.ic_plans_white_24dp
    ),
    EDIT_HOMEPAGE(
            QuickStartTask.EDIT_HOMEPAGE,
            R.string.quick_start_dialog_edit_homepage_title,
            R.string.quick_start_dialog_edit_homepage_message,
            R.drawable.ic_homepage_16dp
    ),
    REVIEW_PAGES(
            QuickStartTask.REVIEW_PAGES,
            R.string.quick_start_dialog_review_pages_title,
            R.string.quick_start_dialog_edit_homepage_message,
            R.drawable.ic_pages_white_24dp
    );

    QuickStartTaskDetails(QuickStartTask task, int titleResId, int subtitleResId, int iconResId) {
        mTask = task;
        mTitleResId = titleResId;
        mSubtitleResId = subtitleResId;
        mIconResId = iconResId;
    }

    public static final String KEY = "quick_start_task_details";

    private final QuickStartTask mTask;
    private int mTitleResId;
    private int mSubtitleResId;
    private int mIconResId;

    public QuickStartTask getTask() {
        return mTask;
    }

    public int getTitleResId() {
        return mTitleResId;
    }

    public int getSubtitleResId() {
        return mSubtitleResId;
    }

    public int getIconResId() {
        return mIconResId;
    }

    public static QuickStartTaskDetails getDetailsForTask(QuickStartTask task) {
        for (QuickStartTaskDetails quickStartTaskDetails : QuickStartTaskDetails.values()) {
            if (quickStartTaskDetails.mTask == task) {
                return quickStartTaskDetails;
            }
        }
        return null;
    }
}
