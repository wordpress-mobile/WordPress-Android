package org.wordpress.android;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;
import androidx.work.WorkManager;

import com.android.volley.RequestQueue;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.iid.FirebaseInstanceId;
import com.wordpress.rest.RestClient;
import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.analytics.Tracker;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.ListActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.ListStore.RemoveExpiredListsPayload;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.StatsStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError;
import org.wordpress.android.modules.AppComponent;
import org.wordpress.android.modules.DaggerAppComponent;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.networking.OAuthAuthenticator;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.push.GCMRegistrationIntentService;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.posts.editor.ImageEditorInitializer;
import org.wordpress.android.ui.posts.editor.ImageEditorTracker;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.tracker.ReaderTracker;
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.UploadStarter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.AppLogListener;
import org.wordpress.android.util.AppLog.LogLevel;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AppThemeUtils;
import org.wordpress.android.util.BitmapLruCache;
import org.wordpress.android.util.CrashLoggingUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PackageUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.QuickStartUtils;
import org.wordpress.android.util.RateLimitedTask;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.UploadWorker;
import org.wordpress.android.util.UploadWorkerKt;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.widgets.AppRatingDialog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasServiceInjector;
import dagger.android.support.HasSupportFragmentInjector;

public class WordPress extends MultiDexApplication implements HasServiceInjector, HasSupportFragmentInjector,
        LifecycleObserver {
    public static final String SITE = "SITE";
    public static final String LOCAL_SITE_ID = "LOCAL_SITE_ID";
    public static String versionName;
    public static WordPressDB wpDB;
    public static boolean sAppIsInTheBackground = true;

    private static RestClientUtils sRestClientUtils;
    private static RestClientUtils sRestClientUtilsVersion1p1;
    private static RestClientUtils sRestClientUtilsVersion1p2;
    private static RestClientUtils sRestClientUtilsVersion1p3;
    private static RestClientUtils sRestClientUtilsVersion0;

    private static final int SECONDS_BETWEEN_SITE_UPDATE = 60 * 60; // 1 hour
    private static final int SECONDS_BETWEEN_BLOGLIST_UPDATE = 15 * 60; // 15 minutes

    private static Context mContext;
    private static BitmapLruCache mBitmapCache;
    private static ApplicationLifecycleMonitor mApplicationLifecycleMonitor;

    private static GoogleApiClient mCredentialsClient;

    @Inject DispatchingAndroidInjector<Service> mServiceDispatchingAndroidInjector;
    @Inject DispatchingAndroidInjector<Fragment> mSupportFragmentInjector;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;
    @Inject ZendeskHelper mZendeskHelper;
    @Inject UploadStarter mUploadStarter;
    @Inject StatsWidgetUpdaters mStatsWidgetUpdaters;
    @Inject StatsStore mStatsStore;
    @Inject SystemNotificationsTracker mSystemNotificationsTracker;
    @Inject ReaderTracker mReaderTracker;
    @Inject ImageManager mImageManager;
    @Inject PrivateAtomicCookie mPrivateAtomicCookie;
    @Inject ImageEditorTracker mImageEditorTracker;

    // For development and production `AnalyticsTrackerNosara`, for testing a mocked `Tracker` will be injected.
    @Inject Tracker mTracker;

    @Inject @Named("custom-ssl") RequestQueue mRequestQueue;
    public static RequestQueue sRequestQueue;
    @Inject FluxCImageLoader mImageLoader;
    public static FluxCImageLoader sImageLoader;
    @Inject OAuthAuthenticator mOAuthAuthenticator;
    public static OAuthAuthenticator sOAuthAuthenticator;

    protected AppComponent mAppComponent;

    public AppComponent component() {
        return mAppComponent;
    }

    /**
     * Update site list in a background task. (WPCOM site list, and eventually self hosted multisites)
     */
    public RateLimitedTask mUpdateSiteList = new RateLimitedTask(SECONDS_BETWEEN_BLOGLIST_UPDATE) {
        protected boolean run() {
            if (mAccountStore.hasAccessToken()) {
                mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
                mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction());
            }
            return true;
        }
    };

    /**
     * Update site information in a background task.
     */
    public RateLimitedTask mUpdateSelectedSite = new RateLimitedTask(SECONDS_BETWEEN_SITE_UPDATE) {
        protected boolean run() {
            int siteLocalId = AppPrefs.getSelectedSite();
            SiteModel selectedSite = mSiteStore.getSiteByLocalId(siteLocalId);
            if (selectedSite != null) {
                mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(selectedSite));
                // Reload editor details from the remote backend
                if (!AppPrefs.isDefaultAppWideEditorPreferenceSet()) {
                    // Check if the migration from app-wide to per-site setting has already happened - v12.9->13.0
                    mDispatcher.dispatch(SiteActionBuilder.newFetchSiteEditorsAction(selectedSite));
                }
            }
            return true;
        }
    };

    public static BitmapLruCache getBitmapCache() {
        if (mBitmapCache == null) {
            // The cache size will be measured in kilobytes rather than
            // number of items. See http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
            int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            int cacheSize = maxMemory / 4; // Use 1/4th of the available memory for this memory cache.
            mBitmapCache = new BitmapLruCache(cacheSize);
        }
        return mBitmapCache;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        long startDate = SystemClock.elapsedRealtime();

        // This call needs be made before accessing any methods in android.webkit package
        setWebViewDataDirectorySuffixOnAndroidP();

        if (CrashLoggingUtils.shouldEnableCrashLogging(getContext())) {
            CrashLoggingUtils.startCrashLogging(getContext());
        }

        initWellSql();

        // Init Dagger
        initDaggerComponent();
        component().inject(this);
        mDispatcher.register(this);

        // Init static fields from dagger injected singletons, for legacy Actions and Utilities
        sRequestQueue = mRequestQueue;
        sImageLoader = mImageLoader;
        sOAuthAuthenticator = mOAuthAuthenticator;

        ProfilingUtils.start("App Startup");

        // Enable log recording
        AppLog.enableRecording(true);
        AppLog.enableLogFilePersistence(this.getBaseContext(), 3);
        AppLog.addListener(new AppLogListener() {
            @Override
            public void onLog(T tag, LogLevel logLevel, String message) {
                StringBuffer sb = new StringBuffer();
                sb.append(logLevel.toString()).append("/").append(AppLog.TAG).append("-")
                  .append(tag.toString()).append(": ").append(message);
                CrashLoggingUtils.log(sb.toString());
            }
        });
        AppLog.i(T.UTILS, "WordPress.onCreate");

        versionName = PackageUtils.getVersionName(this);
        initWpDb();
        enableHttpResponseCache(mContext);

        AppRatingDialog.INSTANCE.init(this);

        // EventBus setup
        EventBus.TAG = "WordPress-EVENT";
        EventBus.builder()
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .throwSubscriberException(true)
                .installDefaultEventBus();


        RestClientUtils.setUserAgent(getUserAgent());

        mZendeskHelper.setupZendesk(this, BuildConfig.ZENDESK_DOMAIN, BuildConfig.ZENDESK_APP_ID,
                BuildConfig.ZENDESK_OAUTH_CLIENT_ID);

        MemoryAndConfigChangeMonitor memoryAndConfigChangeMonitor = new MemoryAndConfigChangeMonitor();
        registerComponentCallbacks(memoryAndConfigChangeMonitor);

        // initialize our ApplicationLifecycleMonitor, which is the App's LifecycleObserver implementation
        mApplicationLifecycleMonitor = new ApplicationLifecycleMonitor();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // Make the UploadStarter observe the app process so it can auto-start uploads
        mUploadStarter.activateAutoUploading((ProcessLifecycleOwner) ProcessLifecycleOwner.get());

        initAnalytics(SystemClock.elapsedRealtime() - startDate);

        createNotificationChannelsOnSdk26();

        // Allows vector drawable from resources (in selectors for instance) on Android < 21 (can cause issues
        // with memory usage and the use of Configuration). More information: http://bit.ly/2H1KTQo
        // Note: if removed, this will cause crashes on Android < 21
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        AppThemeUtils.Companion.setAppTheme(this);

        // verify media is sanitized
        sanitizeMediaUploadStateForSite();

        // remove expired lists
        mDispatcher.dispatch(ListActionBuilder.newRemoveExpiredListsAction(new RemoveExpiredListsPayload()));

        // setup the Credentials Client so we can clean it up on wpcom logout
        mCredentialsClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addApi(Auth.CREDENTIALS_API)
                .build();
        mCredentialsClient.connect();

        initWorkManager();

        // Enqueue our periodic upload work request. The UploadWorkRequest will be called even if the app is closed.
        // It will upload local draft or published posts with local changes to the server.
        UploadWorkerKt.enqueuePeriodicUploadWorkRequestForAllSites();

        mSystemNotificationsTracker.checkSystemNotificationsState();
        ImageEditorInitializer.Companion.init(mImageManager, mImageEditorTracker);
    }

    protected void initWorkManager() {
        UploadWorker.Factory factory = new UploadWorker.Factory(mUploadStarter, mSiteStore);
        androidx.work.Configuration config =
                (new androidx.work.Configuration.Builder()).setWorkerFactory(factory).build();
        WorkManager.initialize(this, config);
    }

    // note that this is overridden in WordPressDebug
    protected void initWellSql() {
        WellSql.init(new WellSqlConfig(getApplicationContext()));
    }

    protected void initDaggerComponent() {
        mAppComponent = DaggerAppComponent.builder()
                                          .application(this)
                                          .build();
    }

    private void sanitizeMediaUploadStateForSite() {
        int siteLocalId = AppPrefs.getSelectedSite();
        final SiteModel selectedSite = mSiteStore.getSiteByLocalId(siteLocalId);
        if (selectedSite != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    UploadService.sanitizeMediaUploadStateForSite(mMediaStore, mDispatcher, selectedSite);
                }
            }).start();
        }
    }

    private void createNotificationChannelsOnSdk26() {
        // create Notification channels introduced in Android Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NORMAL channel (used for likes, comments, replies, etc.)
            NotificationChannel normalChannel = new NotificationChannel(
                    getString(R.string.notification_channel_normal_id),
                    getString(R.string.notification_channel_general_title), NotificationManager.IMPORTANCE_DEFAULT);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(normalChannel);


            // Create the IMPORTANT channel (used for 2fa auth, for example)
            NotificationChannel importantChannel = new NotificationChannel(
                    getString(R.string.notification_channel_important_id),
                    getString(R.string.notification_channel_important_title), NotificationManager.IMPORTANCE_HIGH);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(importantChannel);

            // Create the REMINDER channel (used for various reminders, like Quick Start, etc.)
            NotificationChannel reminderChannel = new NotificationChannel(
                    getString(R.string.notification_channel_reminder_id),
                    getString(R.string.notification_channel_reminder_title), NotificationManager.IMPORTANCE_LOW);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(reminderChannel);

            // Create the TRANSIENT channel (used for short-lived notifications such as processing a Like/Approve,
            // or media upload)
            NotificationChannel transientChannel = new NotificationChannel(
                    getString(R.string.notification_channel_transient_id),
                    getString(R.string.notification_channel_transient_title), NotificationManager.IMPORTANCE_DEFAULT);
            transientChannel.setSound(null, null);
            transientChannel.enableVibration(false);
            transientChannel.enableLights(false);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(transientChannel);
        }
    }

    private void initAnalytics(final long elapsedTimeOnCreate) {
        AnalyticsTracker.registerTracker(mTracker);
        AnalyticsTracker.init(getContext());

        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);

        // Track app upgrade and install
        int versionCode = PackageUtils.getVersionCode(getContext());

        int oldVersionCode = AppPrefs.getLastAppVersionCode();
        if (oldVersionCode == 0) {
            // Track application installed if there isn't old version code
            AnalyticsTracker.track(Stat.APPLICATION_INSTALLED);
        }
        if (oldVersionCode != 0 && oldVersionCode < versionCode) {
            Map<String, Long> properties = new HashMap<String, Long>(1);
            properties.put("elapsed_time_on_create", elapsedTimeOnCreate);
            // app upgraded
            AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_UPGRADED, properties);
        }
        AppPrefs.setLastAppVersionCode(versionCode);
    }

    /**
     * Application.onCreate is called before any activity, service, or receiver - it can be called while the app
     * is in background by a sticky service or a receiver, so we don't want Application.onCreate to make network request
     * or other heavy tasks.
     * <p>
     * This deferredInit method is called when a user starts an activity for the first time, ie. when he sees a
     * screen for the first time. This allows us to have heavy calls on first activity startup instead of app startup.
     */
    public void deferredInit() {
        AppLog.i(T.UTILS, "Deferred Initialisation");

        // Refresh account informations
        if (mAccountStore.hasAccessToken()) {
            mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
            NotificationsUpdateServiceStarter.startService(getContext());
        }
    }

    private void initWpDb() {
        if (!createAndVerifyWpDb()) {
            AppLog.e(T.DB, "Invalid database, sign out user and delete database");
            // Force DB deletion
            WordPressDB.deleteDatabase(this);
            wpDB = new WordPressDB(this);
        }
    }

    private boolean createAndVerifyWpDb() {
        try {
            wpDB = new WordPressDB(this);
            return true;
        } catch (RuntimeException e) {
            AppLog.e(T.DB, e);
            return false;
        }
    }

    public static Context getContext() {
        return mContext;
    }

    /**
     * Update locale of the static context when language is changed.
     */
    public static void updateContextLocale() {
        mContext = LocaleManager.setLocale(mContext);
    }

    public static RestClientUtils getRestClientUtils() {
        if (sRestClientUtils == null) {
            sRestClientUtils = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator, null);
        }
        return sRestClientUtils;
    }

    public static RestClientUtils getRestClientUtilsV1_1() {
        if (sRestClientUtilsVersion1p1 == null) {
            sRestClientUtilsVersion1p1 = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator,
                                                             null, RestClient.REST_CLIENT_VERSIONS.V1_1);
        }
        return sRestClientUtilsVersion1p1;
    }

    public static RestClientUtils getRestClientUtilsV1_2() {
        if (sRestClientUtilsVersion1p2 == null) {
            sRestClientUtilsVersion1p2 = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator,
                                                             null, RestClient.REST_CLIENT_VERSIONS.V1_2);
        }
        return sRestClientUtilsVersion1p2;
    }

    public static RestClientUtils getRestClientUtilsV1_3() {
        if (sRestClientUtilsVersion1p3 == null) {
            sRestClientUtilsVersion1p3 = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator,
                                                             null, RestClient.REST_CLIENT_VERSIONS.V1_3);
        }
        return sRestClientUtilsVersion1p3;
    }

    public static RestClientUtils getRestClientUtilsV0() {
        if (sRestClientUtilsVersion0 == null) {
            sRestClientUtilsVersion0 = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator,
                                                           null, RestClient.REST_CLIENT_VERSIONS.V0);
        }
        return sRestClientUtilsVersion0;
    }

    /**
     * Sign out from wpcom account.
     * Note: This method must not be called on UI Thread.
     */
    public void wordPressComSignOut() {
        // Keep the analytics tracking at the beginning, before the account data is actual removed.
        AnalyticsTracker.track(Stat.ACCOUNT_LOGOUT);

        removeWpComUserRelatedData(getApplicationContext());

        if (mCredentialsClient != null && mCredentialsClient.isConnected()) {
            Auth.CredentialsApi.disableAutoSignIn(mCredentialsClient);
        }

        // Once fully logged out refresh the metadata so the user information doesn't persist for logged out events
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            flushHttpCache();

            // Analytics resets
            AnalyticsTracker.endSession(false);
            AnalyticsTracker.clearAllData();
        }

        if (!event.isError() && mAccountStore.hasAccessToken()) {
            // previously we reset the reader database on logout but this meant losing saved posts
            // so now we only reset it when the user id changes
            if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
                long thisUserId = mAccountStore.getAccount().getUserId();
                long lastUserId = AppPrefs.getLastUsedUserId();
                if (thisUserId != lastUserId) {
                    AppPrefs.setLastUsedUserId(thisUserId);
                    AppLog.i(T.READER, "User changed, resetting reader db");
                    ReaderDatabase.reset(false);
                }
            } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean hasUserOptedOut = !prefs.getBoolean(getString(R.string.pref_key_send_usage), true);
                AnalyticsTracker.setHasUserOptedOut(hasUserOptedOut);
                // When local and remote prefs are different, force opt out to TRUE
                if (hasUserOptedOut != mAccountStore.getAccount().getTracksOptOut()) {
                    AnalyticsUtils.updateAnalyticsPreference(getContext(), mDispatcher, mAccountStore, true);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (mAccountStore.hasAccessToken()) {
            // Make sure the Push Notification token is sent to our servers after a successful login
            GCMRegistrationIntentService.enqueueWork(this,
                    new Intent(this, GCMRegistrationIntentService.class));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnexpectedError(OnUnexpectedError event) {
        AppLog.d(T.API, "Receiving OnUnexpectedError event, message: " + event.exception.getMessage());
    }

    public void removeWpComUserRelatedData(Context context) {
        // cancel all Volley requests - do this before unregistering push since that uses
        // a Volley request
        VolleyUtils.cancelAllRequests(sRequestQueue);

        NotificationsUtils.unregisterDevicePushNotifications(context);
        mZendeskHelper.reset();
        try {
            FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (Exception e) {
            AppLog.e(T.NOTIFS, "Could not delete GCM Token", e);
        }

        // reset default account
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        // delete site-associated themes (keep WP.com themes cached)
        for (SiteModel site : mSiteStore.getSites()) {
            mDispatcher.dispatch(ThemeActionBuilder.newRemoveSiteThemesAction(site));
        }
        // delete wpcom and jetpack sites
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction());
        // remove all lists
        mDispatcher.dispatch(ListActionBuilder.newRemoveAllListsAction());
        // remove all posts
        mDispatcher.dispatch(PostActionBuilder.newRemoveAllPostsAction());

        // reset all user prefs
        AppPrefs.reset();

        // reset the reader database, but retain bookmarked posts
        ReaderDatabase.reset(true);

        // Reset Stats Data
        mStatsStore.deleteAllData();
        mStatsWidgetUpdaters.update(context);

        // Reset Notifications Data
        NotificationsTable.reset();

        // Cancel QuickStart reminders
        QuickStartUtils.cancelQuickStartReminder(context);

        // Remove private Atomic cookie
        mPrivateAtomicCookie.clearCookie();
    }

    private static String mDefaultUserAgent;

    /**
     * Device's default User-Agent string.
     * E.g.:
     * "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
     * AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile
     * Safari/537.36"
     */
    public static String getDefaultUserAgent() {
        if (mDefaultUserAgent == null) {
            try {
                mDefaultUserAgent = WebSettings.getDefaultUserAgent(getContext());
            } catch (AndroidRuntimeException | NullPointerException | IllegalArgumentException e) {
                // Catch AndroidRuntimeException that could be raised by the WebView() constructor.
                // See https://github.com/wordpress-mobile/WordPress-Android/issues/3594
                // Catch NullPointerException that could be raised by WebSettings.getDefaultUserAgent()
                // See https://github.com/wordpress-mobile/WordPress-Android/issues/3838
                // Catch IllegalArgumentException that could be raised by WebSettings.getDefaultUserAgent()
                // See https://github.com/wordpress-mobile/WordPress-Android/issues/9015

                // initialize with the empty string, it's a rare issue
                mDefaultUserAgent = "";
            }
        }
        return mDefaultUserAgent;
    }


    public static final String USER_AGENT_APPNAME = "wp-android";
    private static String mUserAgent;

    /**
     * User-Agent string when making HTTP connections, for both API traffic and WebViews.
     * Appends "wp-android/version" to WebView's default User-Agent string for the webservers
     * to get the full feature list of the browser and serve content accordingly, e.g.:
     * "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
     * AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile
     * Safari/537.36 wp-android/4.7"
     * Note that app versions prior to 2.7 simply used "wp-android" as the user agent
     **/
    public static String getUserAgent() {
        if (mUserAgent == null) {
            String defaultUserAgent = getDefaultUserAgent();
            if (TextUtils.isEmpty(defaultUserAgent)) {
                mUserAgent = USER_AGENT_APPNAME + "/" + PackageUtils.getVersionName(getContext());
            } else {
                mUserAgent = defaultUserAgent + " " + USER_AGENT_APPNAME + "/"
                             + PackageUtils.getVersionName(getContext());
            }
        }
        return mUserAgent;
    }

    /*
     * Since Android P:
     * "Apps can no longer share a single WebView data directory across processes.
     * If your app has more than one process using WebView, CookieManager, or any other API in the android.webkit
     * package, your app will crash when the second process calls a WebView method."
     *
     * (see https://developer.android.com/about/versions/pie/android-9.0-migration)
     *
     * Also here: https://developer.android.com/about/versions/pie/android-9.0-changes-28#web-data-dirs
     *
     * "If your app must use instances of WebView in more than one process, you must assign a unique data
     * directory suffix for each process, using the WebView.setDataDirectorySuffix() method, before
     * using a given instance of WebView in that process."
     *
     * While we don't explicitly use a different process other than the default, making the directory suffix be
     * the actual process name will ensure there's one directory per process, should the Application's
     * onCreate() method be called from a different process any time.
     *
    */
    private void setWebViewDataDirectorySuffixOnAndroidP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String procName = getProcessName();
            if (!TextUtils.isEmpty(procName)) {
                WebView.setDataDirectorySuffix(procName);
            }
        }
    }

    /*
     * enable caching for HttpUrlConnection
     * http://developer.android.com/training/efficient-downloads/redundant_redundant.html
     */
    private static void enableHttpResponseCache(Context context) {
        try {
            long httpCacheSize = 5 * 1024 * 1024; // 5MB
            File httpCacheDir = new File(context.getCacheDir(), "http");
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            AppLog.w(T.UTILS, "Failed to enable http response cache");
        }
    }

    private static void flushHttpCache() {
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }

    /**
     * Gets a field from the project's BuildConfig using reflection. This is useful when flavors
     * are used at the project level to set custom fields.
     * based on: https://code.google.com/p/android/issues/detail?id=52962#c38
     *
     * @param application Used to find the correct file
     * @param fieldName The name of the field-to-access
     * @return The value of the field, or {@code null} if the field is not found.
     */
    public static Object getBuildConfigValue(Application application, String fieldName) {
        try {
            String packageName = application.getClass().getPackage().getName();
            Class<?> clazz = Class.forName(packageName + ".BuildConfig");
            Field field = clazz.getField(fieldName);
            return field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a field from the project's BuildConfig using reflection. This is useful when flavors
     * are used at the project level to set custom fields.
     * based on: https://code.google.com/p/android/issues/detail?id=52962#c38
     *
     * @param activity Used to get the Application instance
     * @param configValueName The name of the field-to-access
     * @return The string value of the field, or empty string if the field is not found.
     */
    public static String getBuildConfigString(Activity activity, String configValueName) {
        if (!BuildConfig.DEBUG) {
            return "";
        }

        String value = (String) WordPress.getBuildConfigValue(activity.getApplication(), configValueName);
        if (!TextUtils.isEmpty(value)) {
            AppLog.d(AppLog.T.NUX, "Auto-filled from build config: " + configValueName);
            return value;
        }

        return "";
    }

    @Override
    public AndroidInjector<Service> serviceInjector() {
        return mServiceDispatchingAndroidInjector;
    }

    @Override public AndroidInjector<Fragment> supportFragmentInjector() {
        return mSupportFragmentInjector;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onAppComesFromBackground() {
        mApplicationLifecycleMonitor.onAppComesFromBackground();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void onAppGoesToBackground() {
        mApplicationLifecycleMonitor.onAppGoesToBackground();
    }

    private class ApplicationLifecycleMonitor {
        private static final int DEFAULT_TIMEOUT = 2 * 60; // 2 minutes

        private Date mLastPingDate;
        private Date mApplicationOpenedDate;
        private boolean mConnectionReceiverRegistered;

        boolean mFirstActivityResumed = true;

        private boolean isPushNotificationPingNeeded() {
            if (mLastPingDate == null) {
                // first startup
                return false;
            }

            Date now = new Date();
            if (DateTimeUtils.secondsBetween(now, mLastPingDate) >= DEFAULT_TIMEOUT) {
                mLastPingDate = now;
                return true;
            }
            return false;
        }

        /**
         * Check if user has valid credentials, and at least 2 minutes have passed
         * since the last ping, then try to update the PN token.
         */
        private void updatePushNotificationTokenIfNotLimited() {
            // Synch Push Notifications settings
            if (isPushNotificationPingNeeded() && mAccountStore.hasAccessToken()) {
                // Register for Cloud messaging
                GCMRegistrationIntentService.enqueueWork(getContext(),
                        new Intent(getContext(), GCMRegistrationIntentService.class));
            }
        }

        public void onAppGoesToBackground() {
            AppLog.i(T.UTILS, "App goes to background");
            if (sAppIsInTheBackground) {
                return;
            }
            sAppIsInTheBackground = true;
            String lastActivityString = AppPrefs.getLastActivityStr();
            ActivityId lastActivity = ActivityId.getActivityIdFromName(lastActivityString);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("last_visible_screen", lastActivity.toString());
            if (mApplicationOpenedDate != null) {
                Date now = new Date();
                properties.put("time_in_app", DateTimeUtils.secondsBetween(now, mApplicationOpenedDate));
                mApplicationOpenedDate = null;
            }
            properties.putAll(mReaderTracker.getAnalyticsData());
            AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_CLOSED, properties);
            AnalyticsTracker.endSession(false);
            // Methods onAppComesFromBackground and onAppGoesToBackground are only workarounds to track when the
            // app goes to or comes from background. The workarounds are not 100% reliable, so avoid unregistering
            // the receiver twice.
            if (mConnectionReceiverRegistered) {
                mConnectionReceiverRegistered = false;
                try {
                    unregisterReceiver(ConnectionChangeReceiver.getInstance());
                    AppLog.d(T.MAIN, "ConnectionChangeReceiver successfully unregistered");
                } catch (IllegalArgumentException e) {
                    AppLog.e(T.MAIN, "ConnectionChangeReceiver was already unregistered");
                    CrashLoggingUtils.log(e);
                }
            }
        }

        /**
         * This method is called when:
         * 1. the app starts (but it's not opened by a service or a broadcast receiver, i.e. an activity is resumed)
         * 2. the app was in background and is now foreground
         */
        public void onAppComesFromBackground() {
            mReaderTracker.setupTrackers();
            AppLog.i(T.UTILS, "App comes from background");
            if (!sAppIsInTheBackground) {
                return;
            }
            sAppIsInTheBackground = false;

            // https://developer.android.com/reference/android/net/ConnectivityManager.html
            // Apps targeting Android 7.0 (API level 24) and higher do not receive this broadcast if
            // the broadcast receiver is declared in their manifest. Apps will still receive broadcasts if
            // BroadcastReceiver is registered with Context.registerReceiver() and that context is still valid.
            if (!mConnectionReceiverRegistered) {
                mConnectionReceiverRegistered = true;
                registerReceiver(ConnectionChangeReceiver.getInstance(),
                        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
            AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);
            mApplicationOpenedDate = new Date();
            // This stat is part of a funnel that provides critical information.  Before
            // making ANY modification to this stat please refer to: p4qSXL-35X-p2
            AnalyticsTracker.track(Stat.APPLICATION_OPENED);
            if (NetworkUtils.isNetworkAvailable(mContext)) {
                // Refresh account informations and Notifications
                if (mAccountStore.hasAccessToken()) {
                    NotificationsUpdateServiceStarter.startService(getContext());
                }

                // verify media is sanitized
                sanitizeMediaUploadStateForSite();

                // Rate limited PN Token Update
                updatePushNotificationTokenIfNotLimited();

                // Rate limited WPCom blog list update
                mUpdateSiteList.runIfNotLimited();

                // Rate limited Site information and options update
                mUpdateSelectedSite.runIfNotLimited();
            }

            // Let's migrate the old editor preference if available in AppPrefs to the remote backend
            SiteUtils.migrateAppWideMobileEditorPreferenceToRemote(mAccountStore, mSiteStore, mDispatcher);

            if (mFirstActivityResumed) {
                deferredInit();
            }
            mFirstActivityResumed = false;
        }
    }

    /**
     * Uses ComponentCallbacks2 is used for memory-related event handling and configuration changes
     */
    private class MemoryAndConfigChangeMonitor implements ComponentCallbacks2 {
        @Override
        public void onConfigurationChanged(final Configuration newConfig) {
            // Reapply locale on configuration change
            LocaleManager.setLocale(getContext());
        }

        @Override
        public void onLowMemory() {
        }

        @Override
        public void onTrimMemory(final int level) {
            boolean evictBitmaps = false;
            switch (level) {
                case TRIM_MEMORY_COMPLETE:
                case TRIM_MEMORY_MODERATE:
                case TRIM_MEMORY_RUNNING_MODERATE:
                case TRIM_MEMORY_RUNNING_CRITICAL:
                case TRIM_MEMORY_RUNNING_LOW:
                    evictBitmaps = true;
                    break;
                case TRIM_MEMORY_BACKGROUND:
                case TRIM_MEMORY_UI_HIDDEN:
                default:
                    break;
            }

            if (evictBitmaps && mBitmapCache != null) {
                mBitmapCache.evictAll();
            }
        }
    }
}
