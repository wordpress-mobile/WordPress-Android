package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.optimizely.Optimizely;
import com.optimizely.Variable.LiveVariable;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.networking.SSLCertsViewActivity;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.NewBlogActivity;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.accounts.login.MagicLinkSignInActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.WordPressMediaUtils;
import org.wordpress.android.ui.people.PeopleManagementActivity;
import org.wordpress.android.ui.plans.PlansActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PostPreviewActivity;
import org.wordpress.android.ui.posts.PostsListActivity;
import org.wordpress.android.ui.prefs.AccountSettingsActivity;
import org.wordpress.android.ui.prefs.AppSettingsActivity;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.MyProfileActivity;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsActivity;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsConstants;
import org.wordpress.android.ui.stats.StatsSingleItemDetailsActivity;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.passcodelock.AppLockManager;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ActivityLauncher {
    private static LiveVariable<Boolean> isMagicLinkEnabledVariable = Optimizely.booleanForKey("isMagicLinkEnabled", false);
    private static final String ARG_DID_SLIDE_IN_FROM_RIGHT = "did_slide_in_from_right";

    public static void showSitePickerForResult(Activity activity, int blogLocalTableId) {
        Intent intent = new Intent(activity, SitePickerActivity.class);
        intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, blogLocalTableId);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_in_from_left,
                R.anim.do_nothing);
        ActivityCompat.startActivityForResult(activity, intent, RequestCodes.SITE_PICKER, options.toBundle());
    }

    public static void viewBlogStats(Context context, int blogLocalTableId) {
        if (blogLocalTableId == 0) return;

        Intent intent = new Intent(context, StatsActivity.class);
        intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, blogLocalTableId);
        slideInFromRight(context, intent);
    }

    public static void viewBlogPlans(Context context, int blogLocalTableId) {
        Intent intent = new Intent(context, PlansActivity.class);
        intent.putExtra(PlansActivity.ARG_LOCAL_TABLE_BLOG_ID, blogLocalTableId);
        slideInFromRight(context, intent);
    }

    public static void viewCurrentBlogPosts(Context context) {
        Intent intent = new Intent(context, PostsListActivity.class);
        slideInFromRight(context, intent);
        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_POSTS);
    }

    public static void viewCurrentBlogMedia(Context context) {
        Intent intent = new Intent(context, MediaBrowserActivity.class);
        slideInFromRight(context, intent);
        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_MEDIA_LIBRARY);
    }

    public static void viewCurrentBlogPages(Context context) {
        Intent intent = new Intent(context, PostsListActivity.class);
        intent.putExtra(PostsListActivity.EXTRA_VIEW_PAGES, true);
        slideInFromRight(context, intent);
        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_PAGES);
    }

    public static void viewCurrentBlogComments(Context context) {
        Intent intent = new Intent(context, CommentsActivity.class);
        slideInFromRight(context, intent);
        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_COMMENTS);
    }

    public static void viewCurrentBlogThemes(Context context) {
        if (ThemeBrowserActivity.isAccessible()) {
            Intent intent = new Intent(context, ThemeBrowserActivity.class);
            slideInFromRight(context, intent);
        }
    }

    public static void viewCurrentBlogPeople(Context context) {
        Intent intent = new Intent(context, PeopleManagementActivity.class);
        slideInFromRight(context, intent);
        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_PEOPLE_MANAGEMENT);
    }

    public static void viewBlogSettingsForResult(Activity activity, Blog blog) {
        if (blog == null) return;

        Intent intent = new Intent(activity, BlogPreferencesActivity.class);
        intent.putExtra(BlogPreferencesActivity.ARG_LOCAL_BLOG_ID, blog.getLocalTableBlogId());
        slideInFromRightForResult(activity, intent, RequestCodes.BLOG_SETTINGS);
        AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.OPENED_BLOG_SETTINGS, blog);
    }

    public static void viewCurrentSite(Context context, Blog blog) {
        if (blog == null) {
            Toast.makeText(context, context.getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String siteUrl = blog.getAlternativeHomeUrl();
        Uri uri = Uri.parse(siteUrl);

        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_VIEW_SITE);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        context.startActivity(intent);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    public static void viewBlogAdmin(Context context, Blog blog) {
        if (blog == null) {
            Toast.makeText(context, context.getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String adminUrl = blog.getAdminUrl();
        Uri uri = Uri.parse(adminUrl);

        AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.OPENED_VIEW_ADMIN, blog);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        context.startActivity(intent);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    public static void viewPostPreviewForResult(Activity activity, Post post, boolean isPage) {
        if (post == null) return;

        Intent intent = new Intent(activity, PostPreviewActivity.class);
        intent.putExtra(PostPreviewActivity.ARG_LOCAL_POST_ID, post.getLocalTablePostId());
        intent.putExtra(PostPreviewActivity.ARG_LOCAL_BLOG_ID, post.getLocalTableBlogId());
        intent.putExtra(PostPreviewActivity.ARG_IS_PAGE, isPage);
        slideInFromRightForResult(activity, intent, RequestCodes.PREVIEW_POST);
    }

    public static void addNewBlogPostOrPageForResult(Activity context, Blog blog, boolean isPage) {
        if (blog == null) return;

        // Create a new post object and assign default settings
        Post newPost = new Post(blog.getLocalTableBlogId(), isPage);
        newPost.setCategories("[" + SiteSettingsInterface.getDefaultCategory(context) +"]");
        newPost.setPostFormat(SiteSettingsInterface.getDefaultFormat(context));
        WordPress.wpDB.savePost(newPost);

        Intent intent = new Intent(context, EditPostActivity.class);
        intent.putExtra(EditPostActivity.EXTRA_POSTID, newPost.getLocalTablePostId());
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, isPage);
        intent.putExtra(EditPostActivity.EXTRA_IS_NEW_POST, true);
        context.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void editBlogPostOrPageForResult(Activity activity, long postOrPageId, boolean isPage) {
        Intent intent = new Intent(activity.getApplicationContext(), EditPostActivity.class);
        intent.putExtra(EditPostActivity.EXTRA_POSTID, postOrPageId);
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, isPage);
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    /*
     * Load the post preview as an authenticated URL so stats aren't bumped
     */
    public static void browsePostOrPage(Context context, Blog blog, Post post) {
        if (blog == null || post == null || TextUtils.isEmpty(post.getPermaLink())) return;

        // always add the preview parameter to avoid bumping stats when viewing posts
        String url = UrlUtils.appendUrlParameter(post.getPermaLink(), "preview", "true");
        WPWebViewActivity.openUrlByUsingBlogCredentials(context, blog, post, url);
    }

    public static void addMedia(Activity activity) {
        WordPressMediaUtils.launchPictureLibrary(activity);
    }

    public static void viewMyProfile(Context context) {
        Intent intent = new Intent(context, MyProfileActivity.class);
        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_MY_PROFILE);
        slideInFromRight(context, intent);
    }

    public static void viewAccountSettings(Context context) {
        Intent intent = new Intent(context, AccountSettingsActivity.class);
        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_ACCOUNT_SETTINGS);
        slideInFromRight(context, intent);
    }

    public static void viewAppSettings(Activity activity) {
        Intent intent = new Intent(activity, AppSettingsActivity.class);
        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_APP_SETTINGS);
        slideInFromRightForResult(activity, intent, RequestCodes.APP_SETTINGS);
    }

    public static void viewNotificationsSettings(Activity activity) {
        Intent intent = new Intent(activity, NotificationsSettingsActivity.class);
        slideInFromRight(activity, intent);
    }

    public static void viewHelpAndSupport(Context context, Tag origin) {
        Intent intent = new Intent(context, HelpActivity.class);
        intent.putExtra(HelpshiftHelper.ORIGIN_KEY, origin);
        slideInFromRight(context, intent);
    }

    public static void viewSSLCerts(Context context) {
        try {
            Intent intent = new Intent(context, SSLCertsViewActivity.class);
            SelfSignedSSLCertsManager selfSignedSSLCertsManager = SelfSignedSSLCertsManager.getInstance(context);
            String lastFailureChainDescription =
                    selfSignedSSLCertsManager.getLastFailureChainDescription().replaceAll("\n", "<br/>");
            intent.putExtra(SSLCertsViewActivity.CERT_DETAILS_KEYS, lastFailureChainDescription);
            context.startActivity(intent);
        } catch (GeneralSecurityException e) {
            AppLog.e(AppLog.T.API, e);
        } catch (IOException e) {
            AppLog.e(AppLog.T.API, e);
        }
    }

    public static void newBlogForResult(Activity activity) {
        Intent intent = new Intent(activity, NewBlogActivity.class);
        intent.putExtra(NewBlogActivity.KEY_START_MODE, NewBlogActivity.CREATE_BLOG);
        activity.startActivityForResult(intent, RequestCodes.CREATE_BLOG);
    }

    public static void showSignInForResult(Activity activity) {
        if (isMagicLinkEnabledVariable.get() && WPActivityUtils.isEmailClientAvailable(activity)) {
            Intent intent = new Intent(activity, MagicLinkSignInActivity.class);
            activity.startActivityForResult(intent, RequestCodes.ADD_ACCOUNT);
        } else {
            Intent intent = new Intent(activity, SignInActivity.class);
            activity.startActivityForResult(intent, RequestCodes.ADD_ACCOUNT);
        }
    }

    public static void viewStatsSinglePostDetails(Context context, Post post, boolean isPage) {
        if (post == null) return;

        int remoteBlogId = WordPress.wpDB.getRemoteBlogIdForLocalTableBlogId(post.getLocalTableBlogId());
        PostModel postModel = new PostModel(
                Integer.toString(remoteBlogId),
                post.getRemotePostId(),
                post.getTitle(),
                post.getLink(),
                isPage ? StatsConstants.ITEM_TYPE_PAGE : StatsConstants.ITEM_TYPE_POST);
        viewStatsSinglePostDetails(context, postModel);
    }

    public static void viewStatsSinglePostDetails(Context context, PostModel post) {
        if (post == null) return;

        Intent statsPostViewIntent = new Intent(context, StatsSingleItemDetailsActivity.class);
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_REMOTE_BLOG_ID, post.getBlogID());
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_REMOTE_ITEM_ID, post.getItemID());
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_REMOTE_ITEM_TYPE, post.getPostType());
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_ITEM_TITLE, post.getTitle());
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_ITEM_URL, post.getUrl());
        context.startActivity(statsPostViewIntent);
    }

    public static void addSelfHostedSiteForResult(Activity activity) {
        Intent intent = new Intent(activity, SignInActivity.class);
        intent.putExtra(SignInActivity.EXTRA_START_FRAGMENT, SignInActivity.ADD_SELF_HOSTED_BLOG);
        activity.startActivityForResult(intent, RequestCodes.ADD_ACCOUNT);
    }

    public static void slideInFromRight(Context context, Intent intent) {
        if (context instanceof Activity) {
            intent.putExtra(ARG_DID_SLIDE_IN_FROM_RIGHT, true);
            Activity activity = (Activity) context;
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    activity,
                    R.anim.activity_slide_in_from_right,
                    R.anim.do_nothing);
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    public static void slideInFromRightForResult(Activity activity, Intent intent, int requestCode) {
        intent.putExtra(ARG_DID_SLIDE_IN_FROM_RIGHT, true);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_in_from_right,
                R.anim.do_nothing);
        ActivityCompat.startActivityForResult(activity, intent, requestCode, options.toBundle());
    }

    /*
     * called in an activity's finish to slide it out to the right if it slid in
     * from the right when started
     */
    public static void slideOutToRight(Activity activity) {
        if (activity != null
                && activity.getIntent() != null
                && activity.getIntent().hasExtra(ARG_DID_SLIDE_IN_FROM_RIGHT)) {
            activity.overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_right);
        }
    }
}
