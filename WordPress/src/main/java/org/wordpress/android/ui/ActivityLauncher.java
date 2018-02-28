package org.wordpress.android.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.networking.SSLCertsViewActivity;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.accounts.LoginEpilogueActivity;
import org.wordpress.android.ui.accounts.SignupEpilogueActivity;
import org.wordpress.android.ui.accounts.SiteCreationActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.people.PeopleManagementActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment;
import org.wordpress.android.ui.plans.PlansActivity;
import org.wordpress.android.ui.plugins.PluginBrowserActivity;
import org.wordpress.android.ui.plugins.PluginDetailActivity;
import org.wordpress.android.ui.plugins.PluginUtils;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PostPreviewActivity;
import org.wordpress.android.ui.posts.PostsListActivity;
import org.wordpress.android.ui.prefs.AccountSettingsActivity;
import org.wordpress.android.ui.prefs.AppSettingsActivity;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.MyProfileActivity;
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsActivity;
import org.wordpress.android.ui.publicize.PublicizeListActivity;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsConnectJetpackActivity;
import org.wordpress.android.ui.stats.StatsConstants;
import org.wordpress.android.ui.stats.StatsSingleItemDetailsActivity;
import org.wordpress.android.ui.stats.models.StatsPostModel;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.passcodelock.AppLockManager;

import java.util.ArrayList;
import java.util.List;

public class ActivityLauncher {

    public static void showMainActivityAndLoginEpilogue(Activity activity,  ArrayList<Integer> oldSitesIds,
                                                        boolean doLoginUpdate) {
        Intent intent = new Intent(activity, WPMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(WPMainActivity.ARG_DO_LOGIN_UPDATE, doLoginUpdate);
        intent.putExtra(WPMainActivity.ARG_SHOW_LOGIN_EPILOGUE, true);
        intent.putIntegerArrayListExtra(WPMainActivity.ARG_OLD_SITES_IDS, oldSitesIds);
        activity.startActivity(intent);
    }

    public static void showMainActivityAndSignupEpilogue(Activity activity, String name, String email, String photoUrl,
                                                         String username) {
        Intent intent = new Intent(activity, WPMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(WPMainActivity.ARG_SHOW_SIGNUP_EPILOGUE, true);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_DISPLAY_NAME, name);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_EMAIL_ADDRESS, email);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_PHOTO_URL, photoUrl);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_USERNAME, username);
        activity.startActivity(intent);
    }

    public static void showSitePickerForResult(Activity activity, SiteModel site) {
        Intent intent = new Intent(activity, SitePickerActivity.class);
        intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, site.getId());
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_in_from_left,
                R.anim.do_nothing);
        ActivityCompat.startActivityForResult(activity, intent, RequestCodes.SITE_PICKER, options.toBundle());
    }

    public static void showPhotoPickerForResult(Activity activity,
                                                @NonNull MediaBrowserType browserType,
                                                @Nullable SiteModel site) {
        Intent intent = new Intent(activity, PhotoPickerActivity.class);
        intent.putExtra(PhotoPickerFragment.ARG_BROWSER_TYPE, browserType);
        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }
        activity.startActivityForResult(intent, RequestCodes.PHOTO_PICKER);
    }

    public static void viewBlogStats(Context context, SiteModel site) {
        Intent intent = new Intent(context, StatsActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
    }

    public static void startJetpackConnectionFlow(Context context, SiteModel site) {
        Intent intent = new Intent(context, StatsConnectJetpackActivity.class);
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
        intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, MediaBrowserType.BROWSER);
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
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.THEMES_ACCESSED_THEMES_BROWSER, site);
        }
    }

    public static void viewCurrentBlogPeople(Context context, SiteModel site) {
        Intent intent = new Intent(context, PeopleManagementActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PEOPLE_MANAGEMENT, site);
    }

    public static void viewPluginBrowser(Context context, SiteModel site) {
        if (PluginUtils.isPluginFeatureAvailable(site)) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PLUGINS, site);
            Intent intent = new Intent(context, PluginBrowserActivity.class);
            intent.putExtra(WordPress.SITE, site);
            context.startActivity(intent);
        }
    }

    public static void viewPluginDetailForResult(Activity context, SiteModel site, SitePluginModel plugin) {
        if (PluginUtils.isPluginFeatureAvailable(site)) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PLUGIN_DETAIL, site);
            Intent intent = new Intent(context, PluginDetailActivity.class);
            intent.putExtra(WordPress.SITE, site);
            intent.putExtra(PluginDetailActivity.KEY_PLUGIN_SLUG, plugin.getSlug());
            context.startActivityForResult(intent, RequestCodes.PLUGIN_DETAIL);
        }
    }

    public static void viewPluginDetailForResult(Activity context, SiteModel site, WPOrgPluginModel plugin) {
        if (PluginUtils.isPluginFeatureAvailable(site)) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PLUGIN_DETAIL, site);
            Intent intent = new Intent(context, PluginDetailActivity.class);
            intent.putExtra(WordPress.SITE, site);
            intent.putExtra(PluginDetailActivity.KEY_PLUGIN_SLUG, plugin.getSlug());
            context.startActivityForResult(intent, RequestCodes.PLUGIN_DETAIL);
        }
    }

    public static void viewBlogSettingsForResult(Activity activity, SiteModel site) {
        Intent intent = new Intent(activity, BlogPreferencesActivity.class);
        intent.putExtra(WordPress.SITE, site);
        activity.startActivityForResult(intent, RequestCodes.SITE_SETTINGS);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_BLOG_SETTINGS, site);
    }

    public static void viewBlogSharing(Context context, SiteModel site) {
        Intent intent = new Intent(context, PublicizeListActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
    }

    public static void viewCurrentSite(Context context, SiteModel site, boolean openFromHeader) {
        AnalyticsTracker.Stat stat = openFromHeader ? AnalyticsTracker.Stat.OPENED_VIEW_SITE_FROM_HEADER
                : AnalyticsTracker.Stat.OPENED_VIEW_SITE;
        AnalyticsUtils.trackWithSiteDetails(stat, site);

        if (site == null) {
            ToastUtils.showToast(context, R.string.blog_not_found, ToastUtils.Duration.SHORT);
        } else if (site.getUrl() == null) {
            ToastUtils.showToast(context, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            AppLog.w(AppLog.T.UTILS, "Site URL is null. Login URL: " + site.getLoginUrl());
        } else {
            openUrlExternal(context, site.getUrl());
        }
    }

    public static void viewBlogAdmin(Context context, SiteModel site) {
        if (site == null || site.getAdminUrl() == null) {
            ToastUtils.showToast(context, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            return;
        }
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_VIEW_ADMIN, site);
        openUrlExternal(context, site.getAdminUrl());
    }

    public static void viewPostPreviewForResult(Activity activity, SiteModel site, PostModel post) {
        if (post == null) return;

        Intent intent = new Intent(activity, PostPreviewActivity.class);
        intent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, post.getId());
        intent.putExtra(WordPress.SITE, site);
        activity.startActivityForResult(intent, RequestCodes.PREVIEW_POST);
    }

    public static void addNewPostOrPageForResult(Activity activity, SiteModel site, boolean isPage, boolean isPromo) {
        if (site == null) return;

        Intent intent = new Intent(activity, EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, isPage);
        intent.putExtra(EditPostActivity.EXTRA_IS_PROMO, isPromo);
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void editPostOrPageForResult(Activity activity, SiteModel site, PostModel post) {
        if (site == null) return;

        Intent intent = new Intent(activity, EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        // PostModel objects can be quite large, since content field is not size restricted,
        // in order to avoid issues like TransactionTooLargeException it's better to pass the id of the post.
        // However, we still want to keep passing the SiteModel to avoid confusion around local & remote ids.
        intent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, post.getId());
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    /*
     * Load the post preview as an authenticated URL so stats aren't bumped
     */
    public static void browsePostOrPage(Context context, SiteModel site, PostModel post) {
        if (site == null || post == null || TextUtils.isEmpty(post.getLink())) return;

        // always add the preview parameter to avoid bumping stats when viewing posts
        String url = UrlUtils.appendUrlParameter(post.getLink(), "preview", "true");
        String shareableUrl = post.getLink();
        String shareSubject = post.getTitle();
        if (site.isWPCom()) {
            WPWebViewActivity.openPostUrlByUsingGlobalWPCOMCredentials(context, url, shareableUrl, shareSubject);
        } else if (site.isJetpackConnected()) {
            WPWebViewActivity.openJetpackBlogPostPreview(context, url, shareableUrl, shareSubject, site.getFrameNonce());
        } else {
            // Add the original post URL to the list of allowed URLs.
            // This is necessary because links are disabled in the webview, but WP removes "?preview=true"
            // from the passed URL, and internally redirects to it. EX:Published posts on a site with Plain
            // permalink structure settings.
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/4873
            WPWebViewActivity.openUrlByUsingBlogCredentials(context, site, post, url, new String[]{post.getLink()});
        }
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

    public static void newBlogForResult(Activity activity, String username) {
        Intent intent = new Intent(activity, SiteCreationActivity.class);
        intent.putExtra(SiteCreationActivity.ARG_USERNAME, username);
        activity.startActivityForResult(intent, RequestCodes.CREATE_SITE);
    }

    public static void showSignInForResult(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        activity.startActivityForResult(intent, RequestCodes.ADD_ACCOUNT);
    }

    public static void showLoginEpilogue(Activity activity, boolean doLoginUpdate, ArrayList<Integer> oldSitesIds) {
        Intent intent = new Intent(activity, LoginEpilogueActivity.class);
        intent.putExtra(LoginEpilogueActivity.EXTRA_DO_LOGIN_UPDATE, doLoginUpdate);
        intent.putIntegerArrayListExtra(LoginEpilogueActivity.ARG_OLD_SITES_IDS, oldSitesIds);
        activity.startActivity(intent);
    }

    public static void showLoginEpilogueForResult(Activity activity, boolean showAndReturn,
            ArrayList<Integer> oldSitesIds) {
        Intent intent = new Intent(activity, LoginEpilogueActivity.class);
        intent.putExtra(LoginEpilogueActivity.EXTRA_SHOW_AND_RETURN, showAndReturn);
        intent.putIntegerArrayListExtra(LoginEpilogueActivity.ARG_OLD_SITES_IDS, oldSitesIds);
        activity.startActivityForResult(intent, RequestCodes.SHOW_LOGIN_EPILOGUE_AND_RETURN);
    }

    public static void showSignupEpilogue(Activity activity, String name, String email, String photoUrl,
                                          String username, boolean isEmail) {
        Intent intent = new Intent(activity, SignupEpilogueActivity.class);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_DISPLAY_NAME, name);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_EMAIL_ADDRESS, email);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_PHOTO_URL, photoUrl);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_USERNAME, username);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_IS_EMAIL, isEmail);
        activity.startActivity(intent);
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

    public static void viewMediaPickerForResult(Activity activity,
                                                @NonNull SiteModel site,
                                                @NonNull MediaBrowserType browserType) {
        Intent intent = new Intent(activity, MediaBrowserActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, browserType);
        int requestCode;
        if (browserType.canMultiselect()) {
            requestCode = RequestCodes.MULTI_SELECT_MEDIA_PICKER;
        } else {
            requestCode = RequestCodes.SINGLE_SELECT_MEDIA_PICKER;
        }
        activity.startActivityForResult(intent, requestCode);
    }

    public static void addSelfHostedSiteForResult(Activity activity) {
        Intent intent;
        intent = new Intent(activity, LoginActivity.class);
        LoginMode.SELFHOSTED_ONLY.putInto(intent);
        activity.startActivityForResult(intent, RequestCodes.ADD_ACCOUNT);
    }

    public static void loginForDeeplink(Activity activity) {
        Intent intent;
        intent = new Intent(activity, LoginActivity.class);
        LoginMode.WPCOM_LOGIN_DEEPLINK.putInto(intent);
        activity.startActivityForResult(intent, RequestCodes.DO_LOGIN);
    }

    public static void loginForShareIntent(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        LoginMode.SHARE_INTENT.putInto(intent);
        activity.startActivityForResult(intent, RequestCodes.DO_LOGIN);
    }

    public static void loginWithoutMagicLink(Activity activity) {
        Intent intent;
        intent = new Intent(activity, LoginActivity.class);
        LoginMode.WPCOM_LOGIN_DEEPLINK.putInto(intent);
        activity.startActivityForResult(intent, RequestCodes.DO_LOGIN);
    }

    /*
     * open the passed url in the device's external browser
     */
    public static void openUrlExternal(Context context, @NonNull String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        try {
            // disable deeplinking activity so to not catch WP URLs
            WPActivityUtils.disableComponent(context, ReaderPostPagerActivity.class);

            context.startActivity(intent);
            AppLockManager.getInstance().setExtendedTimeout();

        } catch (ActivityNotFoundException e) {
            ToastUtils.showToast(context, context.getString(R.string.cant_open_url), ToastUtils.Duration.LONG);
            AppLog.e(AppLog.T.UTILS, "No default app available on the device to open the link: " + url, e);
        } catch (SecurityException se) {
            AppLog.e(AppLog.T.UTILS, "Error opening url in default browser. Url: " + url, se);

            List<ResolveInfo> infos = context.getPackageManager().queryIntentActivities(intent, 0);
            if (infos.size() == 1) {
                // there's only one handler and apparently it caused the exception so, just inform and bail
                AppLog.d(AppLog.T.UTILS, "Only one url handler found so, bailing.");
                ToastUtils.showToast(context, context.getString(R.string.cant_open_url));
            } else {
                Intent chooser = Intent.createChooser(intent, context.getString(R.string.error_please_choose_browser));
                context.startActivity(chooser);
                AppLockManager.getInstance().setExtendedTimeout();
            }
        } finally {
            // re-enable deeplinking
            WPActivityUtils.enableComponent(context, ReaderPostPagerActivity.class);
        }
    }
}
