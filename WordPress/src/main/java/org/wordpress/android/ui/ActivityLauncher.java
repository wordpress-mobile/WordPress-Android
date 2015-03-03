package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Intent;

import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.stats.StatsActivity;

public class ActivityLauncher {

    public static void showSitePickerForResult(Activity activity, boolean visibleAccountsOnly) {
        Intent intent = new Intent(activity, SitePickerActivity.class);
        intent.putExtra(SitePickerActivity.ARG_VISIBLE_ONLY, visibleAccountsOnly);
        activity.startActivityForResult(intent, RequestCodes.SITE_PICKER);
    }

    public static void viewCurrentSite(Activity activity) {
        Intent intent = new Intent(activity, ViewSiteActivity.class);
        activity.startActivity(intent);
    }

    public static void viewCurrentSiteStats(Activity activity, Blog blog) {
        Intent intent = new Intent(activity, StatsActivity.class);
        intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, blog.getLocalTableBlogId());
        activity.startActivity(intent);
    }
}
