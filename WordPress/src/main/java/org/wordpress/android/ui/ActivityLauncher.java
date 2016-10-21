package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.networking.SSLCertsViewActivity;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.NewBlogActivity;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.accounts.login.MagicLinkSignInActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaGalleryActivity;
import org.wordpress.android.ui.media.MediaGalleryPickerActivity;
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
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsActivity;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsConstants;
import org.wordpress.android.ui.stats.StatsSingleItemDetailsActivity;
import org.wordpress.android.ui.stats.models.StatsPostModel;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.passcodelock.AppLockManager;

import java.util.ArrayList;

public class ActivityLauncher {

    public static void showSitePickerForResult(Activity activity, SiteModel site) {
        Intent intent = new Intent(activity, SitePickerActivity.class);
        intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, site.getId());
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_in_from_left,
                R.anim.do_nothing);
        ActivityCompat.startActivityForResult(activity, intent, RequestCodes.SITE_PICKER, options.toBundle());
    }

    public static void viewBlogStats(Context context, SiteModel site) {
        if (site == null) return;
        Intent intent = new Intent(context, StatsActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
    }

    public static void viewBlogPlans(Context context, SiteModel site) {
        Intent intent = new Intent(context, PlansActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
    }

    public static void viewCurrentBlogPosts(Context context, SiteModel site) {
        Intent intent = new Intent(context, PostsListActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_POSTS, site);
    }

    public static void viewCurrentBlogMedia(Context context, SiteModel site) {
        Intent intent = new Intent(context, MediaBrowserActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_MEDIA_LIBRARY, site);
    }

    public static void viewCurrentBlogPages(Context context, SiteModel site) {
        Intent intent = new Intent(context, PostsListActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(PostsListActivity.EXTRA_VIEW_PAGES, true);
        context.startActivity(intent);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PAGES, site);
    }

    public static void viewCurrentBlogComments(Context context, SiteModel site) {
        Intent intent = new Intent(context, CommentsActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_COMMENTS, site);
    }

    public static void viewCurrentBlogThemes(Context context, SiteModel site) {
        if (ThemeBrowserActivity.isAccessible(site)) {
            Intent intent = new Intent(context, ThemeBrowserActivity.class);
            intent.putExtra(WordPress.SITE, site);
            context.startActivity(intent);
        }
    }

    public static void viewCurrentBlogPeople(Context context, SiteModel site) {
        Intent intent = new Intent(context, PeopleManagementActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PEOPLE_MANAGEMENT, site);
    }

    public static void viewBlogSettingsForResult(Activity activity, SiteModel site) {
        if (site == null) return;

        Intent intent = new Intent(activity, BlogPreferencesActivity.class);
        intent.putExtra(WordPress.SITE, site);
        activity.startActivityForResult(intent, RequestCodes.BLOG_SETTINGS);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_BLOG_SETTINGS, site);
    }

    public static void viewCurrentSite(Context context, SiteModel site) {
        if (site == null) {
            Toast.makeText(context, context.getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String siteUrl = site.getUrl();
        Uri uri = Uri.parse(siteUrl);

        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_VIEW_SITE, site);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        context.startActivity(intent);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    public static void viewBlogAdmin(Context context, SiteModel site) {
        if (site == null || site.getAdminUrl() == null) {
            Toast.makeText(context, context.getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String adminUrl = site.getAdminUrl();
        Uri uri = Uri.parse(adminUrl);

        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_VIEW_ADMIN, site);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        context.startActivity(intent);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    public static void viewPostPreviewForResult(Activity activity, SiteModel site, PostModel post, boolean isPage) {
        if (post == null) return;

        Intent intent = new Intent(activity, PostPreviewActivity.class);
        intent.putExtra(PostPreviewActivity.EXTRA_POST, post);
        intent.putExtra(WordPress.SITE, site);
        activity.startActivityForResult(intent, RequestCodes.PREVIEW_POST);
    }

    public static void newGalleryPost(Activity context, SiteModel site, ArrayList<String> mediaIds) {
        if (site == null) return;
        // Create a new post object and assign default settings
        Intent intent = new Intent(context, EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.NEW_MEDIA_GALLERY_EXTRA_IDS, mediaIds);
        intent.setAction(EditPostActivity.NEW_MEDIA_GALLERY);
        context.startActivity(intent);
    }

    public static void newMediaPost(Activity context, SiteModel site, String mediaId) {
        if (site == null) return;
        // Create a new post object and assign default settings
        Intent intent = new Intent(context, EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.setAction(EditPostActivity.NEW_MEDIA_POST);
        intent.putExtra(EditPostActivity.NEW_MEDIA_POST_EXTRA, mediaId);
        context.startActivity(intent);
    }

    public static void addNewPostOrPageForResult(Activity activity, SiteModel site, boolean isPage) {
        if (site == null) return;

        Intent intent = new Intent(activity, EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, isPage);
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void editPostOrPageForResult(Activity activity, SiteModel site, PostModel post) {
        if (site == null) return;

        Intent intent = new Intent(activity, EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_POST, post);
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    /*
     * Load the post preview as an authenticated URL so stats aren't bumped
     */
    public static void browsePostOrPage(Context context, SiteModel site, PostModel post) {
        if (site == null || post == null || TextUtils.isEmpty(post.getLink())) return;

        // always add the preview parameter to avoid bumping stats when viewing posts
        String url = UrlUtils.appendUrlParameter(post.getLink(), "preview", "true");
        if (site.isWPCom()) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url);
        } else {
            WPWebViewActivity.openUrlByUsingBlogCredentials(context, site, post, url);
        }
    }

    public static void addMedia(Activity activity) {
        WordPressMediaUtils.launchPictureLibrary(activity);
    }

    public static void viewMyProfile(Context context) {
        Intent intent = new Intent(context, MyProfileActivity.class);
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_MY_PROFILE);
        context.startActivity(intent);
    }

    public static void viewAccountSettings(Context context) {
        Intent intent = new Intent(context, AccountSettingsActivity.class);
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_ACCOUNT_SETTINGS);
        context.startActivity(intent);
    }

    public static void viewAppSettings(Activity activity) {
        Intent intent = new Intent(activity, AppSettingsActivity.class);
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_APP_SETTINGS);
        activity.startActivityForResult(intent, RequestCodes.APP_SETTINGS);
    }

    public static void viewNotificationsSettings(Activity activity) {
        Intent intent = new Intent(activity, NotificationsSettingsActivity.class);
        activity.startActivity(intent);
    }

    public static void viewHelpAndSupport(Context context, Tag origin) {
        Intent intent = new Intent(context, HelpActivity.class);
        intent.putExtra(HelpshiftHelper.ORIGIN_KEY, origin);
        context.startActivity(intent);
    }

    public static void viewSSLCerts(Context context, String certificateString) {
        Intent intent = new Intent(context, SSLCertsViewActivity.class);
        intent.putExtra(SSLCertsViewActivity.CERT_DETAILS_KEYS, certificateString.replaceAll("\n", "<br/>"));
        context.startActivity(intent);
    }

    public static void newBlogForResult(Activity activity) {
        Intent intent = new Intent(activity, NewBlogActivity.class);
        intent.putExtra(NewBlogActivity.KEY_START_MODE, NewBlogActivity.CREATE_BLOG);
        activity.startActivityForResult(intent, RequestCodes.CREATE_BLOG);
    }

    public static void showSignInForResult(Activity activity) {
        if (shouldShowMagicLinksLogin(activity)) {
            Intent intent = new Intent(activity, MagicLinkSignInActivity.class);
            activity.startActivityForResult(intent, RequestCodes.ADD_ACCOUNT);
        } else {
            Intent intent = new Intent(activity, SignInActivity.class);
            activity.startActivityForResult(intent, RequestCodes.ADD_ACCOUNT);
        }
    }

    public static void viewStatsSinglePostDetails(Context context, SiteModel site, PostModel post, boolean isPage) {
        if (post == null) return;

        StatsPostModel statsPostModel = new StatsPostModel(site.getSiteId(),
                String.valueOf(post.getRemotePostId()), post.getTitle(), post.getLink(),
                isPage ? StatsConstants.ITEM_TYPE_PAGE : StatsConstants.ITEM_TYPE_POST);
        viewStatsSinglePostDetails(context, statsPostModel);
    }

    public static void viewStatsSinglePostDetails(Context context, StatsPostModel post) {
        if (post == null) return;

        Intent statsPostViewIntent = new Intent(context, StatsSingleItemDetailsActivity.class);
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_REMOTE_BLOG_ID, post.getBlogID());
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_REMOTE_ITEM_ID, post.getItemID());
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_REMOTE_ITEM_TYPE, post.getPostType());
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_ITEM_TITLE, post.getTitle());
        statsPostViewIntent.putExtra(StatsSingleItemDetailsActivity.ARG_ITEM_URL, post.getUrl());
        context.startActivity(statsPostViewIntent);
    }

    public static void viewMediaGalleryPickerForSite(Activity activity, @NonNull SiteModel site) {
        Intent intent = new Intent(activity, MediaGalleryPickerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(MediaGalleryPickerActivity.PARAM_SELECT_ONE_ITEM, true);
        activity.startActivityForResult(intent, MediaGalleryActivity.REQUEST_CODE);
    }

    public static void viewMediaGalleryPickerForSiteAndMediaIds(Activity activity, @NonNull SiteModel site,
                                                     @NonNull ArrayList<String> mediaIds) {
        Intent intent = new Intent(activity, MediaGalleryPickerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(MediaGalleryPickerActivity.PARAM_SELECTED_IDS, mediaIds);
        activity.startActivityForResult(intent, MediaGalleryActivity.REQUEST_CODE);
    }

    public static void viewMediaGalleryForSiteAndGallery(Activity activity, @NonNull SiteModel site,
                                               @Nullable MediaGallery mediaGallery) {
        Intent intent = new Intent(activity, MediaGalleryActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(MediaGalleryActivity.PARAMS_MEDIA_GALLERY, mediaGallery);
        if (mediaGallery == null) {
            intent.putExtra(MediaGalleryActivity.PARAMS_LAUNCH_PICKER, true);
        }
        activity.startActivityForResult(intent, MediaGalleryActivity.REQUEST_CODE);
    }

    public static void addSelfHostedSiteForResult(Activity activity) {
        Intent intent = new Intent(activity, SignInActivity.class);
        intent.putExtra(SignInActivity.EXTRA_START_FRAGMENT, SignInActivity.ADD_SELF_HOSTED_BLOG);
        activity.startActivityForResult(intent, RequestCodes.ADD_ACCOUNT);
    }

    public static boolean shouldShowMagicLinksLogin(Activity activity) {
        boolean isMagicLinksEnabled = false;

        return isMagicLinksEnabled && WPActivityUtils.isEmailClientAvailable(activity);
    }
}
