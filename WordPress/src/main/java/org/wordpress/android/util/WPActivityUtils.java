package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

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

    public static Context getThemedContext(Activity activity) {
        if (activity instanceof ActionBarActivity) {
            ActionBar actionBar = ((ActionBarActivity)activity).getSupportActionBar();
            if (actionBar != null) {
                return actionBar.getThemedContext();
            }
        }
        return activity;
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

}
