package org.wordpress.android.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.TaskStackBuilder;
import androidx.fragment.app.Fragment;

import com.wordpress.stories.compose.frame.FrameSaveNotifier;
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.page.PageModel;
import org.wordpress.android.fluxc.network.utils.StatsGranularity;
import org.wordpress.android.imageeditor.EditImageActivity;
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.networking.SSLCertsViewActivity;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.accounts.LoginEpilogueActivity;
import org.wordpress.android.ui.accounts.PostSignupInterstitialActivity;
import org.wordpress.android.ui.accounts.SignupEpilogueActivity;
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailActivity;
import org.wordpress.android.ui.activitylog.list.ActivityLogListActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.domains.DomainRegistrationActivity;
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose;
import org.wordpress.android.ui.gif.GifPickerActivity;
import org.wordpress.android.ui.history.HistoryDetailActivity;
import org.wordpress.android.ui.history.HistoryDetailContainerFragment;
import org.wordpress.android.ui.history.HistoryListItem.Revision;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadActivity;
import org.wordpress.android.ui.jetpack.restore.RestoreActivity;
import org.wordpress.android.ui.jetpack.scan.ScanActivity;
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsActivity;
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryActivity;
import org.wordpress.android.ui.main.MeActivity;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.pages.PageParentActivity;
import org.wordpress.android.ui.pages.PagesActivity;
import org.wordpress.android.ui.people.PeopleManagementActivity;
import org.wordpress.android.ui.photopicker.MediaPickerConstants;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.plans.PlansActivity;
import org.wordpress.android.ui.plugins.PluginBrowserActivity;
import org.wordpress.android.ui.plugins.PluginDetailActivity;
import org.wordpress.android.ui.plugins.PluginUtils;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.JetpackSecuritySettingsActivity;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.ui.posts.PostsListActivity;
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType;
import org.wordpress.android.ui.prefs.AccountSettingsActivity;
import org.wordpress.android.ui.prefs.AppSettingsActivity;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.MyProfileActivity;
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsActivity;
import org.wordpress.android.ui.publicize.PublicizeListActivity;
import org.wordpress.android.ui.sitecreation.SiteCreationActivity;
import org.wordpress.android.ui.stats.StatsConnectJetpackActivity;
import org.wordpress.android.ui.stats.StatsConstants;
import org.wordpress.android.ui.stats.StatsViewType;
import org.wordpress.android.ui.stats.refresh.StatsActivity;
import org.wordpress.android.ui.stats.refresh.StatsViewAllActivity;
import org.wordpress.android.ui.stats.refresh.lists.detail.StatsDetailActivity;
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate;
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementActivity;
import org.wordpress.android.ui.stockmedia.StockMediaPickerActivity;
import org.wordpress.android.ui.stories.StoryComposerActivity;
import org.wordpress.android.ui.suggestion.SuggestionActivity;
import org.wordpress.android.ui.suggestion.SuggestionType;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.wordpress.stories.util.BundleUtilsKt.KEY_STORY_INDEX;
import static com.wordpress.stories.util.BundleUtilsKt.KEY_STORY_SAVE_RESULT;
import static org.wordpress.android.analytics.AnalyticsTracker.ACTIVITY_LOG_ACTIVITY_ID_KEY;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_ACCESS_ERROR;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_DETAIL_REBLOGGED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_REBLOGGED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_ACCESS_ERROR;
import static org.wordpress.android.editor.gutenberg.GutenbergEditorFragment.ARG_STORY_BLOCK_ID;
import static org.wordpress.android.imageeditor.preview.PreviewImageFragment.ARG_EDIT_IMAGE_DATA;
import static org.wordpress.android.login.LoginMode.WPCOM_LOGIN_ONLY;
import static org.wordpress.android.ui.WPWebViewActivity.ENCODING_UTF8;
import static org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModelKt.KEY_BACKUP_DOWNLOAD_ACTIVITY_ID_KEY;
import static org.wordpress.android.ui.jetpack.restore.RestoreViewModelKt.KEY_RESTORE_ACTIVITY_ID_KEY;
import static org.wordpress.android.ui.jetpack.scan.ScanFragment.ARG_THREAT_ID;
import static org.wordpress.android.ui.media.MediaBrowserActivity.ARG_BROWSER_TYPE;
import static org.wordpress.android.ui.pages.PagesActivityKt.EXTRA_PAGE_REMOTE_ID_KEY;
import static org.wordpress.android.ui.stories.StoryComposerActivity.KEY_ALL_UNFLATTENED_LOADED_SLIDES;
import static org.wordpress.android.ui.stories.StoryComposerActivity.KEY_LAUNCHED_FROM_GUTENBERG;
import static org.wordpress.android.ui.stories.StoryComposerActivity.KEY_POST_LOCAL_ID;
import static org.wordpress.android.viewmodel.activitylog.ActivityLogDetailViewModelKt.ACTIVITY_LOG_ARE_BUTTONS_VISIBLE_KEY;
import static org.wordpress.android.viewmodel.activitylog.ActivityLogDetailViewModelKt.ACTIVITY_LOG_ID_KEY;
import static org.wordpress.android.viewmodel.activitylog.ActivityLogViewModelKt.ACTIVITY_LOG_REWINDABLE_ONLY_KEY;

public class ActivityLauncher {
    public static final String SOURCE_TRACK_EVENT_PROPERTY_KEY = "source";
    public static final String BACKUP_TRACK_EVENT_PROPERTY_VALUE = "backup";
    public static final String ACTIVITY_LOG_TRACK_EVENT_PROPERTY_VALUE = "activity_log";

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

    /**
     * Presents the site picker and expects the selection result
     *
     * @param activity the activity that starts the site picker and expects the result
     * @param site     the preselected site
     */
    public static void showSitePickerForResult(Activity activity, SiteModel site) {
        Intent intent = createSitePickerIntent(activity, site, SitePickerMode.DEFAULT_MODE);
        activity.startActivityForResult(intent, RequestCodes.SITE_PICKER);
    }

    /**
     * Presents the site picker and expects the selection result
     *
     * @param fragment the fragment that starts the site picker and expects the result
     * @param site     the preselected site
     * @param mode     site picker mode
     */
    public static void showSitePickerForResult(Fragment fragment, SiteModel site, SitePickerMode mode) {
        Intent intent = createSitePickerIntent(fragment.getContext(), site, mode);
        fragment.startActivityForResult(intent, RequestCodes.SITE_PICKER);
    }

    /**
     * Creates a site picker intent
     *
     * @param context the context to use for the intent creation
     * @param site    the preselected site
     * @param mode    site picker mode
     * @return the site picker intent
     */
    private static Intent createSitePickerIntent(Context context, SiteModel site, SitePickerMode mode) {
        Intent intent = new Intent(context, SitePickerActivity.class);
        intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, site.getId());
        intent.putExtra(SitePickerActivity.KEY_SITE_PICKER_MODE, mode);
        return intent;
    }

    /**
     * Use {@link org.wordpress.android.ui.photopicker.MediaPickerLauncher::showPhotoPickerForResult}  instead
     */
    @Deprecated
    public static void showPhotoPickerForResult(Activity activity,
                                                @NonNull MediaBrowserType browserType,
                                                @Nullable SiteModel site,
                                                @Nullable Integer localPostId) {
        Intent intent = createShowPhotoPickerIntent(activity, browserType, site, localPostId);
        activity.startActivityForResult(intent, RequestCodes.PHOTO_PICKER);
    }


    /**
     * Use {@link org.wordpress.android.ui.photopicker.MediaPickerLauncher::showPhotoPickerForResult}  instead
     */
    @Deprecated
    public static void showPhotoPickerForResult(Fragment fragment,
                                                @NonNull MediaBrowserType browserType,
                                                @Nullable SiteModel site,
                                                @Nullable Integer localPostId) {
        Intent intent = createShowPhotoPickerIntent(fragment.getContext(), browserType, site, localPostId);
        fragment.startActivityForResult(intent, RequestCodes.PHOTO_PICKER);
    }

    private static Intent createShowPhotoPickerIntent(Context context,
                                                      @NonNull MediaBrowserType browserType,
                                                      @Nullable SiteModel site,
                                                      @Nullable Integer localPostId) {
        Intent intent = new Intent(context, PhotoPickerActivity.class);
        intent.putExtra(ARG_BROWSER_TYPE, browserType);
        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }
        if (localPostId != null) {
            intent.putExtra(MediaPickerConstants.LOCAL_POST_ID, localPostId.intValue());
        }
        return intent;
    }

    /**
     * Use {@link org.wordpress.android.ui.photopicker.MediaPickerLauncher::showStockMediaPickerForResult}  instead
     */
    @Deprecated
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

    /**
     * Use {@link org.wordpress.android.ui.photopicker.MediaPickerLauncher::showGifPickerForResult}  instead
     */
    public static void showGifPickerForResult(Activity activity, @NonNull SiteModel site, int requestCode) {
        Map<String, String> properties = new HashMap<>();
        properties.put("from", activity.getClass().getSimpleName());
        AnalyticsTracker.track(Stat.GIF_PICKER_ACCESSED, properties);

        Intent intent = new Intent(activity, GifPickerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(GifPickerActivity.KEY_REQUEST_CODE, requestCode);
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

    public static void viewMySiteInNewStack(Context context) {
        Intent intent = getMainActivityInNewStack(context);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_MY_SITE);
        context.startActivity(intent);
    }

    public static void viewReader(Context context) {
        Intent intent = new Intent(context, WPMainActivity.class);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_READER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

    public static void openEditorForSiteInNewStack(Context context, @NonNull SiteModel site) {
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        Intent mainActivityIntent = getMainActivityInNewStack(context);

        Intent editorIntent = new Intent(context, EditPostActivity.class);
        editorIntent.putExtra(WordPress.SITE, site);
        editorIntent.putExtra(EditPostActivity.EXTRA_IS_PAGE, false);

        taskStackBuilder.addNextIntent(mainActivityIntent);
        taskStackBuilder.addNextIntent(editorIntent);
        taskStackBuilder.startActivities();
    }


    public static void openEditorForPostInNewStack(Context context, @NonNull SiteModel site, int localPostId) {
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        Intent mainActivityIntent = getMainActivityInNewStack(context);

        Intent editorIntent = new Intent(context, EditPostActivity.class);
        editorIntent.putExtra(WordPress.SITE, site);
        editorIntent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, localPostId);
        editorIntent.putExtra(EditPostActivity.EXTRA_IS_PAGE, false);

        taskStackBuilder.addNextIntent(mainActivityIntent);
        taskStackBuilder.addNextIntent(editorIntent);
        taskStackBuilder.startActivities();
    }

    /**
     * Opens the editor and passes the information needed for a reblog action
     *
     * @param activity the calling activity
     * @param site     the site on which the post should be reblogged
     * @param post     the post to be reblogged
     */
    public static void openEditorForReblog(
            Activity activity,
            @Nullable SiteModel site,
            @Nullable ReaderPost post,
            PagePostCreationSourcesDetail reblogSource
    ) {
        if (post == null) {
            ToastUtils.showToast(activity, R.string.post_not_found, ToastUtils.Duration.SHORT);
            return;
        }

        if (site == null) {
            ToastUtils.showToast(activity, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            return;
        }

        AnalyticsUtils.trackWithReblogDetails(
            reblogSource == PagePostCreationSourcesDetail.POST_FROM_REBLOG
                    ? READER_ARTICLE_REBLOGGED
                    : READER_ARTICLE_DETAIL_REBLOGGED,
            post.blogId,
            post.postId,
            site.getSiteId()
        );

        Intent editorIntent = new Intent(activity, EditPostActivity.class);
        editorIntent.putExtra(EditPostActivity.EXTRA_REBLOG_POST_TITLE, post.getTitle());
        editorIntent.putExtra(EditPostActivity.EXTRA_REBLOG_POST_QUOTE, post.getExcerpt());
        editorIntent.putExtra(EditPostActivity.EXTRA_REBLOG_POST_IMAGE, post.getFeaturedImage());
        editorIntent.putExtra(EditPostActivity.EXTRA_REBLOG_POST_CITATION, post.getUrl());
        editorIntent.setAction(EditPostActivity.ACTION_REBLOG);

        addNewPostForResult(editorIntent, activity, site, false, reblogSource);
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

        Intent statsIntent = StatsActivity.buildIntent(context, site);

        taskStackBuilder.addNextIntent(mainActivityIntent);
        taskStackBuilder.addNextIntent(statsIntent);
        taskStackBuilder.startActivities();
    }

    public static void viewStatsInNewStack(Context context) {
        Intent intent = getMainActivityInNewStack(context);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_STATS);
        context.startActivity(intent);
    }

    private static Intent getMainActivityInNewStack(Context context) {
        Intent mainActivityIntent = new Intent(context, WPMainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        return mainActivityIntent;
    }

    public static void viewSavedPostsListInReader(Context context) {
        ReaderPostTable.purgeUnbookmarkedPostsWithBookmarkTag();
        Intent intent = new Intent(context, WPMainActivity.class);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_READER);
        intent.putExtra(WPMainActivity.ARG_READER_BOOKMARK_TAB, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
            StatsActivity.start(context, site);
        }
    }

    public static void viewAllTabbedInsightsStats(Context context, StatsViewType statsType, int selectedTab,
                                                  int localSiteId) {
        StatsViewAllActivity.startForTabbedInsightsStats(context, statsType, selectedTab, localSiteId);
    }

    public static void viewAllInsightsStats(Context context, StatsViewType statsType, int localSiteId) {
        StatsViewAllActivity.startForInsights(context, statsType, localSiteId);
    }

    public static void viewAllGranularStats(Context context, StatsGranularity granularity, SelectedDate selectedDate,
                                            StatsViewType statsType, int localSiteId) {
        StatsViewAllActivity.startForGranularStats(context, statsType, granularity, selectedDate, localSiteId);
    }

    public static void viewInsightsManagement(Context context, int localSiteId) {
        Intent intent = new Intent(context, InsightsManagementActivity.class);
        intent.putExtra(WordPress.LOCAL_SITE_ID, localSiteId);
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
        StatsActivity.start(context, site);
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
        if (site == null) {
            AppLog.e(T.POSTS, "Site cannot be null when opening posts");
            AnalyticsTracker.track(
                    POST_LIST_ACCESS_ERROR,
                    ActivityLauncher.class.getName(),
                    "NullPointerException",
                    "Failed to open Posts because of the null SiteModel"
            );
            ToastUtils.showToast(context, R.string.posts_cannot_be_started, ToastUtils.Duration.SHORT);
            return;
        }
        context.startActivity(PostsListActivity.buildIntent(context, site));
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_POSTS, site);
    }

    public static void viewCurrentBlogMedia(Context context, SiteModel site) {
        Intent intent = new Intent(context, MediaBrowserActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ARG_BROWSER_TYPE, MediaBrowserType.BROWSER);
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
        Intent intent = new Intent(context, ThemeBrowserActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.THEMES_ACCESSED_THEMES_BROWSER, site);
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

    public static void viewDomainRegistrationActivityForResult(Activity activity, SiteModel site,
                                                               @NonNull DomainRegistrationPurpose purpose) {
        Intent intent = new Intent(activity, DomainRegistrationActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(DomainRegistrationActivity.DOMAIN_REGISTRATION_PURPOSE_KEY, purpose);
        activity.startActivityForResult(intent, RequestCodes.DOMAIN_REGISTRATION);
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

    public static void viewBackupList(Activity activity, SiteModel site) {
        if (site == null) {
            ToastUtils.showToast(activity, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            return;
        }
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.JETPACK_BACKUP_LIST_OPENED, site);
        Intent intent = new Intent(activity, ActivityLogListActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ACTIVITY_LOG_REWINDABLE_ONLY_KEY, true);
        activity.startActivity(intent);
    }

    public static void viewActivityLogDetailForResult(
            Activity activity,
            SiteModel site,
            String activityId,
            boolean isButtonVisible,
            boolean rewindableOnly
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ACTIVITY_LOG_ACTIVITY_ID_KEY, activityId);
        String source;
        if (rewindableOnly) {
            source = BACKUP_TRACK_EVENT_PROPERTY_VALUE;
        } else {
            source = ACTIVITY_LOG_TRACK_EVENT_PROPERTY_VALUE;
        }
        properties.put(SOURCE_TRACK_EVENT_PROPERTY_KEY, source);
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.ACTIVITY_LOG_DETAIL_OPENED, site, properties);

        Intent intent = new Intent(activity, ActivityLogDetailActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ACTIVITY_LOG_ID_KEY, activityId);
        intent.putExtra(ACTIVITY_LOG_ARE_BUTTONS_VISIBLE_KEY, isButtonVisible);
        intent.putExtra(SOURCE_TRACK_EVENT_PROPERTY_KEY, source);
        activity.startActivityForResult(intent, RequestCodes.ACTIVITY_LOG_DETAIL);
    }

    public static void viewScan(Activity activity, SiteModel site) {
        if (site == null) {
            ToastUtils.showToast(activity, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            return;
        }
        AnalyticsTracker.track(Stat.JETPACK_SCAN_ACCESSED);
        Intent intent = new Intent(activity, ScanActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    public static void viewScanRequestScanState(Activity activity, SiteModel site, @StringRes int messageRes) {
        if (site == null) {
            ToastUtils.showToast(activity, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            return;
        }
        Intent intent = new Intent(activity, ScanActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ScanActivity.REQUEST_SCAN_STATE, messageRes);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    public static void viewScanRequestFixState(Activity activity, SiteModel site, long threatId) {
        if (site == null) {
            ToastUtils.showToast(activity, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            return;
        }
        Intent intent = new Intent(activity, ScanActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ScanActivity.REQUEST_FIX_STATE, threatId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    public static void viewScanHistory(Activity activity, SiteModel site) {
        if (site == null) {
            ToastUtils.showToast(activity, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            return;
        }
        AnalyticsTracker.track(Stat.JETPACK_SCAN_HISTORY_ACCESSED);
        Intent intent = new Intent(activity, ScanHistoryActivity.class);
        intent.putExtra(WordPress.SITE, site);
        activity.startActivity(intent);
    }

    public static void viewThreatDetails(@NonNull Fragment fragment, SiteModel site, @NonNull Long threatId) {
        Intent intent = new Intent(fragment.getContext(), ThreatDetailsActivity.class);
        intent.putExtra(ARG_THREAT_ID, threatId);
        intent.putExtra(WordPress.SITE, site);
        fragment.startActivity(intent);
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
            String siteUrl = site.getUrl();
            if (site.isWPCom()) {
                // Show wp.com sites authenticated
                WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, siteUrl, true);
            } else if (!TextUtils.isEmpty(site.getUsername()) && !TextUtils.isEmpty(site.getPassword())) {
                // Show self-hosted sites as authenticated since we should have the username & password
                WPWebViewActivity
                        .openUrlByUsingBlogCredentials(context, site, null, siteUrl, new String[]{}, false, true,
                                false);
            } else {
                // Show non-wp.com sites without a password unauthenticated. These would be Jetpack sites that are
                // connected through REST API.
                WPWebViewActivity.openURL(context, siteUrl, true, site.isPrivateWPComAtomic() ? site.getSiteId() : 0);
            }
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

    public static void addNewPostForResult(
            Activity activity,
            SiteModel site,
            boolean isPromo,
            PagePostCreationSourcesDetail source
    ) {
        addNewPostForResult(new Intent(activity, EditPostActivity.class), activity, site, isPromo, source);
    }

    public static void addNewPostForResult(
            Intent intent,
            Activity activity,
            SiteModel site,
            boolean isPromo,
            PagePostCreationSourcesDetail source
    ) {
        if (site == null) {
            return;
        }

        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, false);
        intent.putExtra(EditPostActivity.EXTRA_IS_PROMO, isPromo);
        intent.putExtra(AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL, source);
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void addNewStoryForResult(
            Activity activity,
            SiteModel site,
            PagePostCreationSourcesDetail source
    ) {
        if (site == null) {
            return;
        }

        Intent intent = new Intent(activity, StoryComposerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL, source);
        intent.putExtra(MediaPickerConstants.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED, true);
        activity.startActivityForResult(intent, RequestCodes.CREATE_STORY);
    }

    public static void addNewStoryWithMediaIdsForResult(
            Activity activity,
            SiteModel site,
            PagePostCreationSourcesDetail source,
            long[] mediaIds
    ) {
        if (site == null) {
            return;
        }

        Intent intent = new Intent(activity, StoryComposerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(MediaBrowserActivity.RESULT_IDS, mediaIds);
        intent.putExtra(AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL, source);
        activity.startActivityForResult(intent, RequestCodes.CREATE_STORY);
    }

    public static void addNewStoryWithMediaUrisForResult(
            Activity activity,
            SiteModel site,
            PagePostCreationSourcesDetail source,
            String[] mediaUris
    ) {
        if (site == null) {
            return;
        }

            Intent intent = new Intent(activity, StoryComposerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(MediaPickerConstants.EXTRA_MEDIA_URIS, mediaUris);
        intent.putExtra(AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL, source);
        activity.startActivityForResult(intent, RequestCodes.CREATE_STORY);
    }

    public static void editStoryWithMediaIdsForResult(
            Activity activity,
            SiteModel site,
            long[] mediaIds,
            boolean launchingFromGutenberg
    ) {
        if (site == null) {
            return;
        }

        Intent intent = new Intent(activity, StoryComposerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(MediaBrowserActivity.RESULT_IDS, mediaIds);
        activity.startActivityForResult(intent, RequestCodes.EDIT_STORY);
    }

    public static void editStoryForResult(
            Activity activity,
            SiteModel site,
            LocalId localPostId,
            int storyIndex,
            boolean allStorySlidesAreEditable,
            boolean launchedFromGutenberg,
            String storyBlockId
    ) {
        if (site == null) {
            return;
        }

        Intent intent = new Intent(activity, StoryComposerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(KEY_POST_LOCAL_ID, localPostId.getValue());
        intent.putExtra(KEY_STORY_INDEX, storyIndex);
        intent.putExtra(KEY_LAUNCHED_FROM_GUTENBERG, launchedFromGutenberg);
        intent.putExtra(KEY_ALL_UNFLATTENED_LOADED_SLIDES, allStorySlidesAreEditable);
        intent.putExtra(ARG_STORY_BLOCK_ID, storyBlockId);
        activity.startActivityForResult(intent, RequestCodes.EDIT_STORY);
    }

    public static void editEmptyStoryForResult(
            Activity activity,
            SiteModel site,
            LocalId localPostId,
            int storyIndex,
            String storyBlockId
    ) {
        if (site == null) {
            return;
        }

        AnalyticsTracker.track(Stat.STORY_BLOCK_ADD_MEDIA_TAPPED);
        Intent intent = new Intent(activity, StoryComposerActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(KEY_LAUNCHED_FROM_GUTENBERG, true);
        intent.putExtra(MediaPickerConstants.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED, true);
        intent.putExtra(KEY_POST_LOCAL_ID, localPostId.getValue());
        intent.putExtra(KEY_STORY_INDEX, storyIndex);
        intent.putExtra(ARG_STORY_BLOCK_ID, storyBlockId);
        activity.startActivityForResult(intent, RequestCodes.EDIT_STORY);
    }

    public static void editPostOrPageForResult(Activity activity, SiteModel site, PostModel post) {
        editPostOrPageForResult(new Intent(activity, EditPostActivity.class), activity, site, post.getId(), false);
    }

    public static void editPostOrPageForResult(Activity activity, SiteModel site, PostModel post,
                                               boolean loadAutoSaveRevision) {
        editPostOrPageForResult(new Intent(activity, EditPostActivity.class), activity, site, post.getId(),
                loadAutoSaveRevision);
    }

    public static void editPostOrPageForResult(Intent intent, Activity activity, SiteModel site, int postLocalId) {
        editPostOrPageForResult(intent, activity, site, postLocalId, false);
    }

    public static void editPostOrPageForResult(Intent intent, Activity activity, SiteModel site, int postLocalId,
                                               boolean loadAutoSaveRevision) {
        if (site == null) {
            return;
        }

        intent.putExtra(WordPress.SITE, site);
        // PostModel objects can be quite large, since content field is not size restricted,
        // in order to avoid issues like TransactionTooLargeException it's better to pass the id of the post.
        // However, we still want to keep passing the SiteModel to avoid confusion around local & remote ids.
        intent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, postLocalId);
        intent.putExtra(EditPostActivity.EXTRA_LOAD_AUTO_SAVE_REVISION, loadAutoSaveRevision);

        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void editPageForResult(@NonNull Fragment fragment, @NonNull SiteModel site,
                                         int pageLocalId, boolean loadAutoSaveRevision) {
        Intent intent = new Intent(fragment.getContext(), EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, pageLocalId);
        intent.putExtra(EditPostActivity.EXTRA_LOAD_AUTO_SAVE_REVISION, loadAutoSaveRevision);
        fragment.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void editPageForResult(Intent intent, @NonNull Fragment fragment, @NonNull SiteModel site,
                                         int pageLocalId, boolean loadAutoSaveRevision) {
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, pageLocalId);
        intent.putExtra(EditPostActivity.EXTRA_LOAD_AUTO_SAVE_REVISION, loadAutoSaveRevision);
        fragment.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void addNewPageForResult(
            @NonNull Activity activity,
            @NonNull SiteModel site,
            @NonNull String title,
            @NonNull String content,
            @Nullable String template,
            @NonNull PagePostCreationSourcesDetail source
    ) {
        Intent intent = new Intent(activity, EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, true);
        intent.putExtra(EditPostActivity.EXTRA_IS_PROMO, false);
        intent.putExtra(EditPostActivity.EXTRA_PAGE_TITLE, title);
        intent.putExtra(EditPostActivity.EXTRA_PAGE_CONTENT, content);
        intent.putExtra(EditPostActivity.EXTRA_PAGE_TEMPLATE, template);
        intent.putExtra(AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL, source);
        activity.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void addNewPageForResult(
            @NonNull Fragment fragment,
            @NonNull SiteModel site,
            @NonNull String title,
            @NonNull String content,
            @Nullable String template,
            @NonNull PagePostCreationSourcesDetail source) {
        Intent intent = new Intent(fragment.getContext(), EditPostActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(EditPostActivity.EXTRA_IS_PAGE, true);
        intent.putExtra(EditPostActivity.EXTRA_IS_PROMO, false);
        intent.putExtra(EditPostActivity.EXTRA_PAGE_TITLE, title);
        intent.putExtra(EditPostActivity.EXTRA_PAGE_CONTENT, content);
        intent.putExtra(EditPostActivity.EXTRA_PAGE_TEMPLATE, template);
        intent.putExtra(AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL, source);
        fragment.startActivityForResult(intent, RequestCodes.EDIT_POST);
    }

    public static void viewHistoryDetailForResult(Activity activity, Revision revision, List<Revision> revisions) {
        Intent intent = new Intent(activity, HistoryDetailActivity.class);
        intent.putExtra(HistoryDetailContainerFragment.EXTRA_REVISION, revision);
        intent.putParcelableArrayListExtra(HistoryDetailContainerFragment.EXTRA_REVISIONS, new ArrayList<>(revisions));
        activity.startActivityForResult(intent, RequestCodes.HISTORY_DETAIL);
    }

    /*
     * Load the post preview as an authenticated URL so stats aren't bumped
     */
    public static void browsePostOrPage(Context context, SiteModel site, PostImmutableModel post) {
        browsePostOrPageEx(context, site, post, RemotePreviewType.NOT_A_REMOTE_PREVIEW);
    }

    public static void previewPostOrPageForResult(
            Activity activity,
            SiteModel site,
            PostImmutableModel post,
            RemotePreviewType remotePreviewType
    ) {
        browsePostOrPageEx(activity, site, post, remotePreviewType);
    }

    private static void browsePostOrPageEx(
            Context context,
            SiteModel site,
            PostImmutableModel post,
            RemotePreviewType remotePreviewType) {
        if (site == null || post == null || TextUtils.isEmpty(post.getLink())) {
            return;
        }

        if (remotePreviewType == RemotePreviewType.REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE
                        && TextUtils.isEmpty(post.getAutoSavePreviewUrl())) {
            return;
        }

        String url = PostUtils.getPreviewUrlForPost(remotePreviewType, post);

        String shareableUrl = post.getLink();
        String shareSubject = post.getTitle();
        boolean startPreviewForResult = remotePreviewType != RemotePreviewType.NOT_A_REMOTE_PREVIEW;

        if (site.isWPCom()) {
            WPWebViewActivity.openPostUrlByUsingGlobalWPCOMCredentials(
                    context,
                    url,
                    shareableUrl,
                    shareSubject,
                    true,
                    startPreviewForResult);
        } else if (remotePreviewType == RemotePreviewType.REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE && site.isWPComAtomic()
                   && !site.isPrivateWPComAtomic()) {
            openAtomicBlogPostPreview(
                    context,
                    url,
                    site.getLoginUrl(),
                    site.getFrameNonce());
        } else if (site.isJetpackConnected() && site.isUsingWpComRestApi()) {
            WPWebViewActivity
                    .openJetpackBlogPostPreview(
                            context,
                            url,
                            shareableUrl,
                            shareSubject,
                            site.getFrameNonce(),
                            true,
                            startPreviewForResult,
                            site.isPrivateWPComAtomic() ? site.getSiteId() : 0);
        } else {
            // Add the original post URL to the list of allowed URLs.
            // This is necessary because links are disabled in the webview, but WP removes "?preview=true"
            // from the passed URL, and internally redirects to it. EX:Published posts on a site with Plain
            // permalink structure settings.
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/4873
            WPWebViewActivity.openUrlByUsingBlogCredentials(
                    context,
                    site,
                    post,
                    url,
                    new String[]{post.getLink()},
                    true,
                    true,
                    startPreviewForResult);
        }
    }

    private static void openAtomicBlogPostPreview(Context context, String url, String authenticationUrl,
                                                 String frameNonce) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(authenticationUrl + "?redirect_to=" + URLEncoder
                    .encode(url + "&frame-nonce=" + UrlUtils.urlEncode(frameNonce), ENCODING_UTF8)));
            context.startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void showActionableEmptyView(
            Context context,
            WPWebViewUsageCategory actionableState,
            String postTitle
    ) {
        WPWebViewActivity.openActionableEmptyViewDirectly(context, actionableState, postTitle);
    }

    public static void viewMyProfile(Context context) {
        Intent intent = new Intent(context, MyProfileActivity.class);
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_MY_PROFILE);
        context.startActivity(intent);
    }

    public static void viewMeActivityForResult(Activity activity) {
        Intent intent = new Intent(activity, MeActivity.class);
        AnalyticsTracker.track(AnalyticsTracker.Stat.ME_ACCESSED);
        activity.startActivityForResult(intent, RequestCodes.APP_SETTINGS);
    }

    public static void viewAccountSettings(Context context) {
        Intent intent = new Intent(context, AccountSettingsActivity.class);
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_ACCOUNT_SETTINGS);
        context.startActivity(intent);
    }

    public static void viewAppSettingsForResult(Activity activity) {
        Intent intent = new Intent(activity, AppSettingsActivity.class);
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_APP_SETTINGS);
        activity.startActivityForResult(intent, RequestCodes.APP_SETTINGS);
    }

    public static void viewNotificationsSettings(Activity activity) {
        Intent intent = new Intent(activity, NotificationsSettingsActivity.class);
        activity.startActivity(intent);
    }

    public static void viewJetpackSecuritySettings(Activity activity, SiteModel site) {
        AnalyticsTracker.track(Stat.SITE_SETTINGS_JETPACK_SECURITY_SETTINGS_VIEWED);
        Intent intent = new Intent(activity, JetpackSecuritySettingsActivity.class);
        intent.putExtra(WordPress.SITE, site);
        activity.startActivity(intent);
    }

    public static void viewStories(Activity activity, SiteModel site, StorySaveResult event) {
        Intent intent = new Intent(activity, StoryComposerActivity.class);
        intent.putExtra(KEY_STORY_SAVE_RESULT, event);
        intent.putExtra(WordPress.SITE, site);

        // we need to have a way to cancel the related error notification when the user comes
        // from tapping on MANAGE on the snackbar (otherwise they'll be able to discard the
        // errored story but the error notification will remain existing in the system dashboard)
        intent.setAction(String.valueOf(FrameSaveNotifier.getNotificationIdForError(
                StoryComposerActivity.BASE_FRAME_MEDIA_ERROR_NOTIFICATION_ID,
                event.getStoryIndex()
        )));

        activity.startActivity(intent);
    }

    public static void viewJetpackSecuritySettingsForResult(Activity activity, SiteModel site) {
        AnalyticsTracker.track(Stat.SITE_SETTINGS_JETPACK_SECURITY_SETTINGS_VIEWED);
        Intent intent = new Intent(activity, JetpackSecuritySettingsActivity.class);
        intent.putExtra(WordPress.SITE, site);
        activity.startActivityForResult(intent, JetpackSecuritySettingsActivity.JETPACK_SECURITY_SETTINGS_REQUEST_CODE);
    }

    public static void viewHelpAndSupportInNewStack(@NonNull Context context, @NonNull Origin origin,
                                          @Nullable SiteModel selectedSite, @Nullable List<String> extraSupportTags) {
        Map<String, String> properties = new HashMap<>();
        properties.put("origin", origin.name());
        AnalyticsTracker.track(Stat.SUPPORT_OPENED, properties);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        Intent mainActivityIntent = getMainActivityInNewStack(context);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Intent meIntent = new Intent(context, MeActivity.class);
        meIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Intent helpIntent = HelpActivity.createIntent(context, origin, selectedSite, extraSupportTags);

        taskStackBuilder.addNextIntent(mainActivityIntent);
        taskStackBuilder.addNextIntent(meIntent);
        taskStackBuilder.addNextIntent(helpIntent);
        taskStackBuilder.startActivities();
    }

    public static void viewHelpAndSupport(@NonNull Context context, @NonNull Origin origin,
                                          @Nullable SiteModel selectedSite, @Nullable List<String> extraSupportTags) {
        Map<String, String> properties = new HashMap<>();
        properties.put("origin", origin.name());
        AnalyticsTracker.track(Stat.SUPPORT_OPENED, properties);
        context.startActivity(HelpActivity.createIntent(context, origin, selectedSite, extraSupportTags));
    }

    public static void viewZendeskTickets(@NonNull Context context,
                                          @Nullable SiteModel selectedSite) {
        viewHelpAndSupportInNewStack(context, Origin.ZENDESK_NOTIFICATION, selectedSite, null);
    }

    public static void viewSSLCerts(Context context, String certificateString) {
        Intent intent = new Intent(context, SSLCertsViewActivity.class);
        intent.putExtra(SSLCertsViewActivity.CERT_DETAILS_KEYS, certificateString.replaceAll("\n", "<br/>"));
        context.startActivity(intent);
    }

    public static void newBlogForResult(Activity activity) {
        Intent intent = new Intent(activity, SiteCreationActivity.class);
        activity.startActivityForResult(intent, RequestCodes.CREATE_SITE);
    }

    public static void showMainActivityAndSiteCreationActivity(Activity activity) {
        // If we just wanted to have WPMainActivity in the back stack after starting SiteCreationActivity, we could have
        // used a TaskStackBuilder to do so. However, since we want to handle the SiteCreationActivity result in
        // WPMainActivity, we must start it this way.
        final Intent intent = new Intent(activity, WPMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(WPMainActivity.ARG_SHOW_SITE_CREATION, true);
        activity.startActivity(intent);
    }

    public static void showSignInForResult(Activity activity) {
        showSignInForResult(activity, false);
    }

    public static void showSignInForResult(Activity activity, boolean isWpComOnly) {
        Intent intent = new Intent(activity, LoginActivity.class);
        if (isWpComOnly) {
            WPCOM_LOGIN_ONLY.putInto(intent);
        }
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

    public static void showPostSignupInterstitial(Context context) {
        final Intent parentIntent = new Intent(context, WPMainActivity.class);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        final Intent intent = new Intent(context, PostSignupInterstitialActivity.class);
        TaskStackBuilder.create(context).addNextIntent(parentIntent).addNextIntent(intent).startActivities();
    }

    public static void viewStatsSinglePostDetails(Context context, SiteModel site, PostModel post) {
        if (post == null || site == null) {
            return;
        }
        StatsDetailActivity.Companion
                .start(context, site, post.getRemotePostId(), StatsConstants.ITEM_TYPE_POST, post.getTitle(),
                        post.getLink());
    }

    public static void viewMediaPickerForResult(Activity activity,
                                                @NonNull SiteModel site,
                                                @NonNull MediaBrowserType browserType) {
        Intent intent = new Intent(activity, MediaBrowserActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(ARG_BROWSER_TYPE, browserType);
        int requestCode;
        if (browserType.canMultiselect()) {
            requestCode = RequestCodes.MULTI_SELECT_MEDIA_PICKER;
        } else {
            requestCode = RequestCodes.SINGLE_SELECT_MEDIA_PICKER;
        }
        activity.startActivityForResult(intent, requestCode);
    }

    public static void viewSuggestionsForResult(@NonNull Activity activity,
                                                @NonNull SiteModel site,
                                                @NonNull SuggestionType type) {
        Intent intent = new Intent(activity, SuggestionActivity.class);
        intent.putExtra(SuggestionActivity.INTENT_KEY_SITE_MODEL, site);
        intent.putExtra(SuggestionActivity.INTENT_KEY_SUGGESTION_TYPE, type);
        activity.startActivityForResult(intent, RequestCodes.SELECTED_USER_MENTION);
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

    public static void loginForJetpackStats(Fragment fragment) {
        Intent intent = new Intent(fragment.getActivity(), LoginActivity.class);
        LoginMode.JETPACK_STATS.putInto(intent);
        fragment.startActivityForResult(intent, RequestCodes.DO_LOGIN);
    }

    /*
     * open the passed url in the device's external browser
     */
    public static void openUrlExternal(Context context, @NonNull String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            // Disable deeplinking activity so to not catch WP URLs.
            // We'll re-enable them later - see callers of WPActivityUtils#enableReaderDeeplinks.
            WPActivityUtils.disableReaderDeeplinks(context);

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

    public static void openImageEditor(
        Activity activity,
        ArrayList<EditImageData.InputData> input
    ) {
        Intent intent = new Intent(activity, EditImageActivity.class);
        intent.putParcelableArrayListExtra(ARG_EDIT_IMAGE_DATA, input);
        activity.startActivityForResult(intent, RequestCodes.IMAGE_EDITOR_EDIT_IMAGE);
    }

    public static void viewPagesInNewStack(Context context, SiteModel site) {
        if (site == null) {
            ToastUtils.showToast(context, R.string.pages_cannot_be_started, ToastUtils.Duration.SHORT);
            return;
        }

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        Intent mainActivityIntent = getMainActivityInNewStack(context);
        Intent pagesIntent = new Intent(context, PagesActivity.class);
        pagesIntent.putExtra(WordPress.SITE, site);
        taskStackBuilder.addNextIntent(mainActivityIntent);
        taskStackBuilder.addNextIntent(pagesIntent);
        taskStackBuilder.startActivities();
    }

    public static void viewPagesInNewStack(Context context) {
        Intent intent = getMainActivityInNewStack(context);
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_PAGES);
        context.startActivity(intent);
    }

    public static void showBackupDownloadForResult(Activity activity, @NonNull SiteModel site, String activityId,
                                                   int resultCode, String source) {
        Map<String, String> properties = new HashMap<>();
        properties.put(SOURCE_TRACK_EVENT_PROPERTY_KEY, source);
        AnalyticsTracker.track(Stat.JETPACK_BACKUP_DOWNLOAD_OPENED, properties);

        Intent intent = new Intent(activity, BackupDownloadActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(KEY_BACKUP_DOWNLOAD_ACTIVITY_ID_KEY, activityId);
        activity.startActivityForResult(intent, resultCode);
    }

    public static void shareBackupDownloadFileLink(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_link)));
    }

    public static void downloadBackupDownloadFile(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    }

    public static void showRestoreForResult(Activity activity, @NonNull SiteModel site, String activityId,
                                                   int resultCode, String source) {
        Map<String, String> properties = new HashMap<>();
        properties.put(SOURCE_TRACK_EVENT_PROPERTY_KEY, source);
        AnalyticsTracker.track(Stat.JETPACK_RESTORE_OPENED, properties);

        Intent intent = new Intent(activity, RestoreActivity.class);
        intent.putExtra(WordPress.SITE, site);
        intent.putExtra(KEY_RESTORE_ACTIVITY_ID_KEY, activityId);
        activity.startActivityForResult(intent, resultCode);
    }
}
