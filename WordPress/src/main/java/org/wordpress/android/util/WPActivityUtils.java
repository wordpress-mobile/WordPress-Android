package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ViewSiteActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.notifications.NotificationsActivity;
import org.wordpress.android.ui.posts.PagesActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.reader.ReaderPostListActivity;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;

public class WPActivityUtils {

    public static Context getThemedContext(Context context) {
        if (context instanceof ActionBarActivity) {
            ActionBar actionBar = ((ActionBarActivity)context).getSupportActionBar();
            if (actionBar != null) {
                return actionBar.getThemedContext();
            }
        }
        return context;
    }

    /**
     * Set the window content overlay on device's that don't respect the theme
     * attribute. Fixes API 18 issue with windowContentOverlay: https://code.google.com/p/android/issues/detail?id=58280
     *
     * From: http://stackoverflow.com/questions/17945785/what-happened-to-windowcontentoverlay-in-android-api-18/18093909
     */
    public static void setWindowContentOverlayCompat(Activity activity) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // Get the content view
            View contentView = activity.findViewById(android.R.id.content);

            // Make sure it's a valid instance of a FrameLayout
            if (contentView instanceof FrameLayout) {
                TypedValue tv = new TypedValue();

                // Get the windowContentOverlay value of the current theme
                if (activity.getTheme().resolveAttribute(android.R.attr.windowContentOverlay, tv, true)) {

                    // If it's a valid resource, set it as the foreground drawable
                    // for the content view
                    if (tv.resourceId != 0) {
                        ((FrameLayout) contentView).setForeground(activity.getResources().getDrawable(tv.resourceId));
                    }
                }
            }
        }
    }

    public static Intent getIntentForActivityId(Context context, ActivityId id) {
        final Intent intent;
        switch (id) {
            case COMMENTS:
                if (WordPress.getCurrentBlog() == null) {
                    return null;
                }
                intent = new Intent(context, CommentsActivity.class);
                intent.putExtra("id", WordPress.getCurrentBlog().getLocalTableBlogId());
                break;
            case MEDIA:
                intent = new Intent(context, MediaBrowserActivity.class);
                break;
            case NOTIFICATIONS:
                intent = new Intent(context, NotificationsActivity.class);
                break;
            case PAGES:
                if (WordPress.getCurrentBlog() == null) {
                    return null;
                }
                intent = new Intent(context, PagesActivity.class);
                intent.putExtra("id", WordPress.getCurrentBlog().getLocalTableBlogId());
                intent.putExtra(PostsActivity.EXTRA_VIEW_PAGES, true);
                break;
            case POSTS:
                intent = new Intent(context, PostsActivity.class);
                break;
            case READER:
                intent = new Intent(context, ReaderPostListActivity.class);
                break;
            case STATS:
                if (WordPress.getCurrentBlog() == null) {
                    return null;
                }
                intent = new Intent(context, StatsActivity.class);
                intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, WordPress.getCurrentBlog().getLocalTableBlogId());
                break;
            case THEMES:
                intent = new Intent(context, ThemeBrowserActivity.class);
                break;
            case VIEW_SITE:
                intent = new Intent(context, ViewSiteActivity.class);
                break;
            default:
                intent = null;
                break;
        }

        return intent;
    }

    /*
     * returns the optimal pixel width to use for the menu drawer based on:
     * http://www.google.com/design/spec/layout/structure.html#structure-side-nav
     * http://www.google.com/design/spec/patterns/navigation-drawer.html
     * http://android-developers.blogspot.co.uk/2014/10/material-design-on-android-checklist.html
     * https://medium.com/sebs-top-tips/material-navigation-drawer-sizing-558aea1ad266
     */
    public static int getOptimalDrawerWidth(Context context) {
        Point displaySize = DisplayUtils.getDisplayPixelSize(context);
        int appBarHeight = DisplayUtils.getActionBarHeight(context);
        int drawerWidth = Math.min(displaySize.x, displaySize.y) - appBarHeight;
        int maxDp = (DisplayUtils.isXLarge(context) ? 400 : 320);
        int maxPx = DisplayUtils.dpToPx(context, maxDp);
        return Math.min(drawerWidth, maxPx);
    }

}
