package org.wordpress.android.ui.quickstart;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;


/**
 * Static data that represents info that goes into Quick Start notices
 **/
public enum QuickStartNoticeDetails {
    UPDATE_SITE_TITLE(
            QuickStartTask.UPDATE_SITE_TITLE,
            R.string.quick_start_list_update_site_title_title,
            R.string.quick_start_list_update_site_title_subtitle
    ),
    VIEW_SITE_TUTORIAL(
            QuickStartTask.VIEW_SITE,
            R.string.quick_start_dialog_view_site_title,
            R.string.quick_start_dialog_view_site_message
    ),
    CHOSE_THEME_TUTORIAL(
            QuickStartTask.CHOOSE_THEME,
            R.string.quick_start_dialog_choose_theme_title,
            R.string.quick_start_dialog_choose_theme_message
    ),
    CUSTOMIZE_SITE_TUTORIAL(
            QuickStartTask.CUSTOMIZE_SITE,
            R.string.quick_start_dialog_customize_site_title,
            R.string.quick_start_dialog_customize_site_message
    ),
    SHARE_SITE_TUTORIAL(
            QuickStartTask.ENABLE_POST_SHARING,
            R.string.quick_start_dialog_share_site_title,
            R.string.quick_start_dialog_share_site_message
    ),
    PUBLISH_POST_TUTORIAL(
            QuickStartTask.PUBLISH_POST,
            R.string.quick_start_dialog_publish_post_title,
            R.string.quick_start_dialog_publish_post_message
    ),
    FOLLOW_SITES_TUTORIAL(
            QuickStartTask.FOLLOW_SITE,
            R.string.quick_start_dialog_follow_sites_title,
            R.string.quick_start_dialog_follow_sites_message
    ),
    UPLOAD_SITE_ICON(
            QuickStartTask.UPLOAD_SITE_ICON,
            R.string.quick_start_dialog_upload_icon_title,
            R.string.quick_start_dialog_upload_icon_message
    ),
    CREATE_NEW_PAGE(
            QuickStartTask.CREATE_NEW_PAGE,
            R.string.quick_start_dialog_create_page_title,
            R.string.quick_start_dialog_create_page_message
    ),
    CHECK_STATS(
            QuickStartTask.CHECK_STATS,
            R.string.quick_start_dialog_check_stats_title,
            R.string.quick_start_dialog_check_stats_message
    ),
    EXPLORE_PLANS(
            QuickStartTask.EXPLORE_PLANS,
            R.string.quick_start_dialog_explore_plans_title,
            R.string.quick_start_dialog_explore_plans_message
    );

    QuickStartNoticeDetails(QuickStartTask task, int titleResId, int messageResId) {
        mTask = task;
        mTitleResId = titleResId;
        mMessageResId = messageResId;
    }

    private final QuickStartTask mTask;
    private int mTitleResId;
    private int mMessageResId;

    public QuickStartTask getTask() {
        return mTask;
    }

    public int getTitleResId() {
        return mTitleResId;
    }

    public int getMessageResId() {
        return mMessageResId;
    }

    public static QuickStartNoticeDetails getNoticeForTask(QuickStartTask task) {
        for (QuickStartNoticeDetails quickStartTaskDetails : QuickStartNoticeDetails.values()) {
            if (quickStartTaskDetails.mTask == task) {
                return quickStartTaskDetails;
            }
        }
        return null;
    }
}
