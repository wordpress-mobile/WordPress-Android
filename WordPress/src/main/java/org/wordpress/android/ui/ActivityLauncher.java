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
import android.support.v4.app.Fragment;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.page.PageModel;
import org.wordpress.android.fluxc.network.utils.StatsGranularity;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.networking.SSLCertsViewActivity;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.accounts.LoginEpilogueActivity;
import org.wordpress.android.ui.accounts.SignupEpilogueActivity;
import org.wordpress.android.ui.accounts.SiteCreationActivity;
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailActivity;
import org.wordpress.android.ui.activitylog.list.ActivityLogListActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.giphy.GiphyPickerActivity;
import org.wordpress.android.ui.history.HistoryDetailActivity;
import org.wordpress.android.ui.history.HistoryDetailContainerFragment;
import org.wordpress.android.ui.history.HistoryListItem.Revision;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.pages.PageParentActivity;
import org.wordpress.android.ui.pages.PagesActivity;
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
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.AppSettingsActivity;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.MyProfileActivity;
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsActivity;
import org.wordpress.android.ui.publicize.PublicizeListActivity;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity;
import org.wordpress.android.ui.sitecreation.NewSiteCreationActivity;
import org.wordpress.android.ui.stats.StatsAbstractFragment;
import org.wordpress.android.ui.stats.StatsConnectJetpackActivity;
import org.wordpress.android.ui.stats.StatsConstants;
import org.wordpress.android.ui.stats.StatsSingleItemDetailsActivity;
import org.wordpress.android.ui.stats.StatsViewType;
import org.wordpress.android.ui.stats.models.StatsPostModel;
import org.wordpress.android.ui.stats.refresh.StatsActivity;
import org.wordpress.android.ui.stats.refresh.StatsViewAllFragment;
import org.wordpress.android.ui.stats.refresh.lists.detail.StatsDetailActivity;
import org.wordpress.android.ui.stockmedia.StockMediaPickerActivity;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.ACTIVITY_LOG_ACTIVITY_ID_KEY;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_ACCESS_ERROR;
import static org.wordpress.android.ui.pages.PagesActivityKt.EXTRA_PAGE_REMOTE_ID_KEY;
import static org.wordpress.android.ui.stats.OldStatsActivity.LOGGED_INTO_JETPACK;
import static org.wordpress.android.viewmodel.activitylog.ActivityLogDetailViewModelKt.ACTIVITY_LOG_ID_KEY;

public class ActivityLauncher {
    public static void showMainActivityAndLoginEpilogue(Activity activity, ArrayList<Integer> oldSitesIds,
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

    public static void showStockMediaPickerForResult(Activity activity,
                                                     @NonNull SiteModel site,
                                                     int requestCode) {
        Map<String, String> properties = new HashMap<>();
        properties.put("from", activity.getClass().getSimpleName());
        AnalyticsTracker.track(AnalyticsTracker.Stat.STOCK_MEDIA_ACCESSED, properties);

        Intent intent = new Intent(activity, StockMediaPickerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(StockMediaPickerActivity.KEY_REQUEST_CODE, requestCode);

        activity.startActivityForResult(intent, requestCode);
    }

    public static void showGiphyPickerForResult(Activity activity, @NonNull SiteModel site, int requestCode) {
        Map<String, String> properties = new HashMap<>();
        properties.put("from", activity.getClass().getSimpleName());
        AnalyticsTracker.track(AnalyticsTracker.Stat.GIPHY_PICKER_ACCESSED, properties);

        Intent intent = new Intent(activity, GiphyPickerActivity.class);
        intent.putExtra(WordPress.SITE, site);

        activity.startActivityForResult(intent, requestCode);
    }

    public static void startJetpackInstall(Context context, JetpackConnectionSource source, SiteModel site) {
        Intent intent = new Intent(context, JetpackRemoteInstallActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(JetpackRemoteInstallFragment.TRACKING_SOURCE_KEY, source);
        context.startActivity(intent);
    }

    public static void continueJetpackConnect(Context context, JetpackConnectionSource source, SiteModel site) {
        switch (source) {
            case NOTIFICATIONS:
                continueJetpackConnectForNotifications(context, site);
                break;
            case STATS:
                continueJetpackConnectForStats(context, site);
                break;
        }
    }

    private static void continueJetpackConnectForNotifications(Context context, SiteModel site) {
        Intent intent = new Intent(context, WPMainActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(WPMainActivity.ARG_CONTINUE_JETPACK_CONNECT, true);
        context.startActivity(intent);
    }

    private static void continueJetpackConnectForStats(Context context, SiteModel site) {
        Intent intent = new Intent(context, StatsConnectJetpackActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(StatsConnectJetpackActivity.ARG_CONTINUE_JETPACK_CONNECT, true);
        context.startActivity(intent);
    }

    public static void viewNotifications(Context context) {
        Intent intent = new Intent(context, WPMainActivity.class);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_NOTIFICATIONS);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public static void viewNotificationsInNewStack(Context context) {
        Intent intent = getMainActivityInNewStack(context);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_NOTIFICATIONS);
        context.startActivity(intent);
    }

    public static void viewReaderInNewStack(Context context) {
        Intent intent = getMainActivityInNewStack(context);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_READER);
        context.startActivity(intent);
    }

    public static void openEditorInNewStack(Context context) {
        Intent intent = getMainActivityInNewStack(context);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_EDITOR);
        context.startActivity(intent);
    }

    public static void viewStatsInNewStack(Context context, SiteModel site) {
        if (site == null) {
            AppLog.e(T.STATS, "SiteModel is null when opening the stats from the deeplink.");
            AnalyticsTracker.track(
                    STATS_ACCESS_ERROR,
                    ActivityLauncher.class.getName(),
                    "NullPointerException",
                    "Failed to open Stats from the deeplink because of the null SiteModel"
                                  );
            ToastUtils.showToast(context, R.string.stats_cannot_be_started, ToastUtils.Duration.SHORT);
            return;
        }
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);

        Intent mainActivityIntent = getMainActivityInNewStack(context);

        Intent statsIntent = new Intent(context, StatsActivity.class);
        statsIntent.putExtra(WordPress.SITE, site);

        taskStackBuilder.addNextIntent(mainActivityIntent);
        taskStackBuilder.addNextIntent(statsIntent);
        taskStackBuilder.startActivities();
    }

    private static Intent getMainActivityInNewStack(Context context) {
        Intent mainActivityIntent = new Intent(context, WPMainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        return mainActivityIntent;
    }

    public static void viewSavedPostsListInReader(Context context) {
        // Easiest way to show reader with saved posts filter is to update the "last used filter" preference and make
        // WPMainActivity restart itself with Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (!ReaderTagTable.getBookmarkTags().isEmpty()) {
            AppPrefs.setReaderTag(ReaderTagTable.getBookmarkTags().get(0));
        }
        ReaderPostTable.purgeUnbookmarkedPostsWithBookmarkTag();

        Intent intent = new Intent(context, WPMainActivity.class);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_READER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static void viewBlogStats(Context context, SiteModel site) {
        if (site == null) {
            AppLog.e(T.STATS, "SiteModel is null when opening the stats.");
            AnalyticsTracker.track(
                    STATS_ACCESS_ERROR,
                    ActivityLauncher.class.getName(),
                    "NullPointerException",
                    "Failed to open Stats because of the null SiteModel"
                                  );
            ToastUtils.showToast(context, R.string.stats_cannot_be_started, ToastUtils.Duration.SHORT);
        } else {
            Intent intent = new Intent(context, StatsActivity.class);
            intent.putExtra(WordPress.SITE, site);
            context.startActivity(intent);
        }
    }

    public static void viewAllTabbedInsightsStats(Context context, StatsViewType statsType, int selectedTab) {
        Intent intent = new Intent(context, org.wordpress.android.ui.stats.refresh.StatsViewAllActivity.class);
        intent.putExtra(StatsAbstractFragment.ARGS_VIEW_TYPE, statsType);
        intent.putExtra(StatsViewAllFragment.SELECTED_TAB_KEY, selectedTab);
        context.startActivity(intent);
    }

    public static void viewAllInsightsStats(Context context, StatsViewType statsType) {
        Intent intent = new Intent(context, org.wordpress.android.ui.stats.refresh.StatsViewAllActivity.class);
        intent.putExtra(StatsAbstractFragment.ARGS_VIEW_TYPE, statsType);
        context.startActivity(intent);
    }

    public static void viewAllGranularStats(Context context, StatsGranularity granularity, StatsViewType statsType) {
        Intent intent = new Intent(context, org.wordpress.android.ui.stats.refresh.StatsViewAllActivity.class);
        intent.putExtra(StatsAbstractFragment.ARGS_VIEW_TYPE, statsType);
        intent.putExtra(StatsAbstractFragment.ARGS_TIMEFRAME, granularity);
        context.startActivity(intent);
    }

    public static void viewBlogStatsAfterJetpackSetup(Context context, SiteModel site) {
        if (site == null) {
            AppLog.e(T.STATS, "SiteModel is null when opening the stats.");
            AnalyticsTracker.track(
                    STATS_ACCESS_ERROR,
                    ActivityLauncher.class.getName(),
                    "NullPointerException",
                    "Failed to open Stats because of the null SiteModel"
                                  );
            ToastUtils.showToast(context, R.string.stats_cannot_be_started, ToastUtils.Duration.SHORT);
            return;
        }
        Intent intent = new Intent(context, StatsActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(LOGGED_INTO_JETPACK, true);
        context.startActivity(intent);
    }

    public static void viewConnectJetpackForStats(Context context, SiteModel site) {
        Intent intent = new Intent(context, StatsConnectJetpackActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
    }

    public static void viewBlogPlans(Context context, SiteModel site) {
        Intent intent = new Intent(context, PlansActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
        AnalyticsUtils.trackWithSiteDetails(Stat.OPENED_PLANS, site);
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

    public static void viewCurrentBlogPages(@NonNull Context context, @NonNull SiteModel site) {
        Intent intent = new Intent(context, PagesActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PAGES, site);
    }

    public static void viewPageParentForResult(@NonNull Fragment fragment, @NonNull PageModel page) {
        Intent intent = new Intent(fragment.getContext(), PageParentActivity.class);
        intent.putExtra(WordPress.SITE, page.getSite());
        intent.putExtra(EXTRA_PAGE_REMOTE_ID_KEY, page.getRemoteId());
        fragment.startActivityForResult(intent, RequestCodes.PAGE_PARENT);

        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PAGE_PARENT, page.getSite());
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
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PLUGIN_DIRECTORY, site);
            Intent intent = new Intent(context, PluginBrowserActivity.class);
            intent.putExtra(WordPress.SITE, site);
            context.startActivity(intent);
        }
    }

    public static void viewPluginDetail(Activity context, SiteModel site, String slug) {
        if (PluginUtils.isPluginFeatureAvailable(site)) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PLUGIN_DETAIL, site);
            Intent intent = new Intent(context, PluginDetailActivity.class);
            intent.putExtra(WordPress.SITE, site);
            intent.putExtra(PluginDetailActivity.KEY_PLUGIN_SLUG, slug);
            context.startActivity(intent);
        }
    }

    public static void viewActivityLogList(Activity activity, SiteModel site) {
        if (site == null) {
            ToastUtils.showToast(activity, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            return;
        }
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.ACTIVITY_LOG_LIST_OPENED, site);
        Intent intent = new Intent(activity, ActivityLogListActivity.class);
        intent.putExtra(WordPress.SITE, site);
        activity.startActivity(intent);
    }

    public static void viewActivityLogDetailForResult(Activity activity, SiteModel site, String activityId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ACTIVITY_LOG_ACTIVITY_ID_KEY, activityId);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.ACTIVITY_LOG_DETAIL_OPENED, site, properties);

        Intent intent = new Intent(activity, ActivityLogDetailActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ACTIVITY_LOG_ID_KEY, activityId);
        activity.startActivityForResult(intent, RequestCodes.ACTIVITY_LOG_DETAIL);
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
        AnalyticsUtils.trackWithSiteDetails(Stat.OPENED_SHARING_MANAGEMENT, site);
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
        if (post == null) {
            return;
        }

        Intent intent = new Intent(activity, PostPreviewActivity.class);
        intent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, post.getId());
        intent.putExtra(WordPress.SITE, site);
        activity.startActivityForResult(intent, RequestCodes.PREVIEW_POST);
    }

    public static void viewPagePreview(@NonNull Fragment fragment, @NonNull PageModel page) {
        Intent intent = new Intent(fragment.getContext(), PostPreviewActivity.class);
        intent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, page.getPageId());
        intent.putExtra(WordPress.SITE, page.getSite());
        fragment.startActivity(intent);
    }

    public static void addNewPostForResult(Activity activity, SiteModel site, boolean isPromo) {
        addNewPostForResult(new Intent(activity, EditPostActivity.class), activity, site, isPromo);
    }

    public static void addNewPostForResult(Intent intent, Activity activity, SiteModel site, boolean isPromo) {
        if (site == null) {
            return;
        }

        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, false);
        intent.putExtra(EditPostActivity.EXTRA_IS_PROMO, isPromo);
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void editPostOrPageForResult(Activity activity, SiteModel site, PostModel post) {
        editPostOrPageForResult(new Intent(activity, EditPostActivity.class), activity, site, post.getId());
    }

    public static void editPostOrPageForResult(Intent intent, Activity activity, SiteModel site, int postLocalId) {
        if (site == null) {
            return;
        }

        intent.putExtra(WordPress.SITE, site);
        // PostModel objects can be quite large, since content field is not size restricted,
        // in order to avoid issues like TransactionTooLargeException it's better to pass the id of the post.
        // However, we still want to keep passing the SiteModel to avoid confusion around local & remote ids.
        intent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, postLocalId);
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void editPageForResult(@NonNull Fragment fragment, @NonNull PageModel page) {
        Intent intent = new Intent(fragment.getContext(), EditPostActivity.class);
        editPageForResult(intent, fragment, page.getSite(), page.getPageId());
    }

    public static void editPageForResult(Intent intent, @NonNull Fragment fragment, @NonNull SiteModel site,
                                         int pageLocalId) {
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, pageLocalId);
        fragment.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void addNewPageForResult(@NonNull Fragment fragment, @NonNull SiteModel site) {
        Intent intent = new Intent(fragment.getContext(), EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, true);
        intent.putExtra(EditPostActivity.EXTRA_IS_PROMO, false);
        fragment.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void viewHistoryDetailForResult(Activity activity, Revision revision, ArrayList<Revision> revisions) {
        Intent intent = new Intent(activity, HistoryDetailActivity.class);
        intent.putExtra(HistoryDetailContainerFragment.EXTRA_REVISION, revision);
        intent.putParcelableArrayListExtra(HistoryDetailContainerFragment.EXTRA_REVISIONS, revisions);
        activity.startActivityForResult(intent, RequestCodes.HISTORY_DETAIL);
    }

    /*
     * Load the post preview as an authenticated URL so stats aren't bumped
     */
    public static void browsePostOrPage(Context context, SiteModel site, PostModel post) {
        if (site == null || post == null || TextUtils.isEmpty(post.getLink())) {
            return;
        }

        // always add the preview parameter to avoid bumping stats when viewing posts
        String url = UrlUtils.appendUrlParameter(post.getLink(), "preview", "true");
        String shareableUrl = post.getLink();
        String shareSubject = post.getTitle();
        if (site.isWPCom()) {
            WPWebViewActivity.openPostUrlByUsingGlobalWPCOMCredentials(context, url, shareableUrl, shareSubject);
        } else if (site.isJetpackConnected()) {
            WPWebViewActivity
                    .openJetpackBlogPostPreview(context, url, shareableUrl, shareSubject, site.getFrameNonce());
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

    public static void viewHelpAndSupport(@NonNull Context context, @NonNull Origin origin,
                                          @Nullable SiteModel selectedSite, @Nullable List<String> extraSupportTags) {
        context.startActivity(HelpActivity.createIntent(context, origin, selectedSite, extraSupportTags));
    }

    public static void viewZendeskTickets(@NonNull Context context,
                                          @Nullable SiteModel selectedSite) {
        viewHelpAndSupport(context, Origin.ZENDESK_NOTIFICATION, selectedSite, null);
    }

    public static void viewSSLCerts(Context context, String certificateString) {
        Intent intent = new Intent(context, SSLCertsViewActivity.class);
        intent.putExtra(SSLCertsViewActivity.CERT_DETAILS_KEYS, certificateString.replaceAll("\n", "<br/>"));
        context.startActivity(intent);
    }

    public static void newBlogForResult(Activity activity) {
        Intent intent = new Intent(activity,
                BuildConfig.NEW_SITE_CREATION_ENABLED ? NewSiteCreationActivity.class : SiteCreationActivity.class);
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
                                                  ArrayList<Integer> oldSitesIds, boolean doLoginUpdate) {
        Intent intent = new Intent(activity, LoginEpilogueActivity.class);
        intent.putExtra(LoginEpilogueActivity.EXTRA_DO_LOGIN_UPDATE, doLoginUpdate);
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

    public static void showSignupEpilogueForResult(Activity activity, String name, String email, String photoUrl,
                                                   String username, boolean isEmail) {
        Intent intent = new Intent(activity, SignupEpilogueActivity.class);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_DISPLAY_NAME, name);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_EMAIL_ADDRESS, email);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_PHOTO_URL, photoUrl);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_USERNAME, username);
        intent.putExtra(SignupEpilogueActivity.EXTRA_SIGNUP_IS_EMAIL, isEmail);
        activity.startActivityForResult(intent, RequestCodes.SHOW_SIGNUP_EPILOGUE_AND_RETURN);
    }

    public static void viewStatsSinglePostDetails(Context context, SiteModel site, PostModel post) {
        if (post == null || site == null) {
            return;
        }
        StatsDetailActivity.Companion
                .start(context, site, post.getRemotePostId(), StatsConstants.ITEM_TYPE_POST, post.getTitle(),
                        post.getLink());
    }

    public static void viewStatsSinglePostDetails(Context context, StatsPostModel post) {
        if (post == null) {
            return;
        }

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
            }
        } finally {
            // re-enable deeplinking
            WPActivityUtils.enableComponent(context, ReaderPostPagerActivity.class);
        }
    }

    public static void openStatsUrl(Context context, @NonNull String url) {
        if (url.startsWith("https://wordpress.com/my-stats") || url.startsWith("http://wordpress.com/my-stats")) {
            // make sure to load the no-chrome version of Stats over https
            url = UrlUtils.makeHttps(url);
            if (url.contains("?")) {
                // add the no chrome parameters if not available
                if (!url.contains("?no-chrome") && !url.contains("&no-chrome")) {
                    url += "&no-chrome";
                }
            } else {
                url += "?no-chrome";
            }
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url);
        } else if (url.startsWith("https") || url.startsWith("http")) {
            WPWebViewActivity.openURL(context, url);
        }
    }
}
