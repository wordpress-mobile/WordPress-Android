package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Intent;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PagesActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;

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

    public static void viewBlogStats(Activity activity, Blog blog) {
        if (blog == null) return;

        Intent intent = new Intent(activity, StatsActivity.class);
        intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, blog.getLocalTableBlogId());
        activity.startActivity(intent);
    }

    public static void viewCurrentBlogPosts(Activity activity) {
        Intent intent = new Intent(activity, PostsActivity.class);
        activity.startActivity(intent);
    }

    public static void viewCurrentBlogMedia(Activity activity) {
        Intent intent = new Intent(activity, MediaBrowserActivity.class);
        activity.startActivity(intent);
    }

    public static void viewCurrentBlogPages(Activity activity) {
        Intent intent = new Intent(activity, PagesActivity.class);
        activity.startActivity(intent);
    }

    public static void viewBlogComments(Activity activity, Blog blog) {
        if (blog == null) return;

        Intent intent = new Intent(activity, CommentsActivity.class);
        intent.putExtra("id", blog.getLocalTableBlogId());
        activity.startActivity(intent);
    }

    public static void viewCurrentBlogThemes(Activity activity) {
        Intent intent = new Intent(activity, ThemeBrowserActivity.class);
        activity.startActivity(intent);
    }

    public static void viewBlogSettings(Activity activity, Blog blog) {
        if (blog == null) return;

        Intent intent = new Intent(activity, BlogPreferencesActivity.class);
        intent.putExtra("id", blog.getLocalTableBlogId());
        activity.startActivity(intent);
    }

    public static void viewBlogAdmin(Activity activity, Blog blog) {
        if (blog == null) return;

        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_VIEW_ADMIN);

        Intent intent = new Intent(activity, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, blog.getUsername());
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, blog.getPassword());
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, blog.getAdminUrl());
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, WPWebViewActivity.getBlogLoginUrl(blog));
        intent.putExtra(WPWebViewActivity.LOCAL_BLOG_ID, blog.getLocalTableBlogId());
        activity.startActivity(intent);
    }

    public static void addNewBlogPostOrPage(Activity activity, Blog blog, boolean isPage) {
        // Create a new post object
        Post newPost = new Post(blog.getLocalTableBlogId(), isPage);
        WordPress.wpDB.savePost(newPost);

        Intent intent = new Intent(activity, EditPostActivity.class);
        intent.putExtra(EditPostActivity.EXTRA_POSTID, newPost.getLocalTablePostId());
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, isPage);
        intent.putExtra(EditPostActivity.EXTRA_IS_NEW_POST, true);
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void addMedia(Activity activity, Blog blog) {
        // TODO: https://github.com/wordpress-mobile/WordPress-Android/issues/2394
    }
}
