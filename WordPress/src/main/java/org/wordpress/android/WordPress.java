package org.wordpress.android;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.android.volley.RequestQueue;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.wordpress.rest.RestClient;
import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.analytics.AnalyticsTrackerNosara;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError;
import org.wordpress.android.modules.AppComponent;
import org.wordpress.android.modules.DaggerAppComponent;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.networking.OAuthAuthenticator;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.push.GCMRegistrationIntentService;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.ui.stats.datasets.StatsDatabaseHelper;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.AppLogListener;
import org.wordpress.android.util.AppLog.LogLevel;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BitmapLruCache;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PackageUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.RateLimitedTask;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.passcodelock.AbstractAppLock;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;
import javax.inject.Named;

import de.greenrobot.event.EventBus;
import io.fabric.sdk.android.Fabric;

public class WordPress extends MultiDexApplication {
    public static final String SITE = "SITE";
    public static String versionName;
    public static WordPressDB wpDB;

    private static RestClientUtils sRestClientUtils;
    private static RestClientUtils sRestClientUtilsVersion1_1;
    private static RestClientUtils sRestClientUtilsVersion1_2;
    private static RestClientUtils sRestClientUtilsVersion1_3;
    private static RestClientUtils sRestClientUtilsVersion0;

    private static final int SECONDS_BETWEEN_SITE_UPDATE = 60 * 60; // 1 hour
    private static final int SECONDS_BETWEEN_BLOGLIST_UPDATE = 15 * 60; // 15 minutes
    private static final int SECONDS_BETWEEN_DELETE_STATS = 5 * 60; // 5 minutes

    private static Context mContext;
    private static BitmapLruCache mBitmapCache;

    private static GoogleApiClient mCredentialsClient;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    @Inject @Named("custom-ssl") RequestQueue mRequestQueue;
    public static RequestQueue sRequestQueue;
    @Inject FluxCImageLoader mImageLoader;
    public static FluxCImageLoader sImageLoader;
    @Inject OAuthAuthenticator mOAuthAuthenticator;
    public static OAuthAuthenticator sOAuthAuthenticator;

    private AppComponent mAppComponent;
    public AppComponent component() {
        return mAppComponent;
    }

    /**
     *  Update site list in a background task. (WPCOM site list, and eventually self hosted multisites)
     */
    public RateLimitedTask mUpdateSiteList = new RateLimitedTask(SECONDS_BETWEEN_BLOGLIST_UPDATE) {
        protected boolean run() {
            if (mAccountStore.hasAccessToken()) {
                mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
            }
            return true;
        }
    };

    /**
     *  Update site infos in a background task.
     */
    public RateLimitedTask mUpdateSelectedSite = new RateLimitedTask(SECONDS_BETWEEN_SITE_UPDATE) {
        protected boolean run() {
            int siteLocalId = AppPrefs.getSelectedSite();
            SiteModel selectedSite = mSiteStore.getSiteByLocalId(siteLocalId);
            if (selectedSite != null) {
                mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(selectedSite));
            }
            return true;
        }
    };

    /**
     *  Delete stats cache that is already expired
     */
    public static RateLimitedTask sDeleteExpiredStats = new RateLimitedTask(SECONDS_BETWEEN_DELETE_STATS) {
        protected boolean run() {
            // Offload to a separate thread. We don't want to slown down the app on startup/resume.
            new Thread(new Runnable() {
                public void run() {
                    // subtracts to the current time the cache TTL
                    long timeToDelete = System.currentTimeMillis() - (StatsTable.CACHE_TTL_MINUTES * 60 * 1000);
                    StatsTable.deleteOldStats(WordPress.getContext(), timeToDelete);
                }
            }).start();
            return true;
        }
    };

    public static BitmapLruCache getBitmapCache() {
        if (mBitmapCache == null) {
            // The cache size will be measured in kilobytes rather than
            // number of items. See http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
            int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            int cacheSize = maxMemory / 4;  //Use 1/4th of the available memory for this memory cache.
            mBitmapCache = new BitmapLruCache(cacheSize);
        }
        return mBitmapCache;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        long startDate = SystemClock.elapsedRealtime();

        // Init WellSql
        WellSql.init(new WellSqlConfig(getApplicationContext()));

        // Init Dagger
        mAppComponent = DaggerAppComponent.builder()
                .appContextModule(new AppContextModule(getApplicationContext()))
                .build();
        component().inject(this);
        mDispatcher.register(this);

        // Init static fields from dagger injected singletons, for legacy Actions/Utils
        sRequestQueue = mRequestQueue;
        sImageLoader = mImageLoader;
        sOAuthAuthenticator = mOAuthAuthenticator;

        if (!PackageUtils.isDebugBuild()) {
            Fabric.with(this, new Crashlytics());
        }

        ProfilingUtils.start("App Startup");
        // Enable log recording
        AppLog.enableRecording(true);
        AppLog.addListener(new AppLogListener() {
            @Override
            public void onLog(T tag, LogLevel logLevel, String message) {
                StringBuffer sb = new StringBuffer();
                sb.append(logLevel.toString()).append("/").append(AppLog.TAG).append("-")
                        .append(tag.toString()).append(": ").append(message);
                CrashlyticsUtils.log(sb.toString());
            }
        });
        AppLog.i(T.UTILS, "WordPress.onCreate");

        versionName = PackageUtils.getVersionName(this);
        initWpDb();
        enableHttpResponseCache(mContext);

        // EventBus setup
        EventBus.TAG = "WordPress-EVENT";
        EventBus.builder()
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .throwSubscriberException(true)
                .installDefaultEventBus();


        RestClientUtils.setUserAgent(getUserAgent());

        // PasscodeLock setup
        if(!AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            // Make sure that PasscodeLock isn't already in place.
            // Notifications services can enable it before the app is started.
            AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);
        }
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            AppLockManager.getInstance().getAppLock().setExemptActivities(
                    new String[]{"org.wordpress.android.ui.ShareIntentReceiverActivity"});
        }

        HelpshiftHelper.init(this);

        ApplicationLifecycleMonitor applicationLifecycleMonitor = new ApplicationLifecycleMonitor();
        registerComponentCallbacks(applicationLifecycleMonitor);
        registerActivityLifecycleCallbacks(applicationLifecycleMonitor);

        initAnalytics(SystemClock.elapsedRealtime() - startDate);

        // If users uses a custom locale set it on start of application
        WPActivityUtils.applyLocale(getContext());

        // Allows vector drawable from resources (in selectors for instance) on Android < 21 (can cause issues
        // with memory usage and the use of Configuration). More informations:
        // https://developer.android.com/reference/android/support/v7/app/AppCompatDelegate.html#setCompatVectorFromResourcesEnabled(boolean)
        // Note: if removed, this will cause crashes on Android < 21
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // setup the Credentials Client so we can clean it up on wpcom logout
        mCredentialsClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override public void onConnected(@Nullable Bundle bundle) {}
                    @Override public void onConnectionSuspended(int i) {}
                })
                .addApi(Auth.CREDENTIALS_API)
                .build();
        mCredentialsClient.connect();
    }

    private void initAnalytics(final long elapsedTimeOnCreate) {
        AnalyticsTracker.registerTracker(new AnalyticsTrackerNosara(getContext()));
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
     *
     * This deferredInit method is called when a user starts an activity for the first time, ie. when he sees a
     * screen for the first time. This allows us to have heavy calls on first activity startup instead of app startup.
     */
    public void deferredInit(Activity activity) {
        AppLog.i(T.UTILS, "Deferred Initialisation");

        if (isGooglePlayServicesAvailable(activity)) {
            // Register for Cloud messaging
            startService(new Intent(this, GCMRegistrationIntentService.class));
        }

        // Refresh account informations
        if (mAccountStore.hasAccessToken()) {
            mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
            NotificationsUpdateService.startService(getContext());
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

    public static RestClientUtils getRestClientUtils() {
        if (sRestClientUtils == null) {
            sRestClientUtils = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator, null);
        }
        return sRestClientUtils;
    }

    public static RestClientUtils getRestClientUtilsV1_1() {
        if (sRestClientUtilsVersion1_1 == null) {
            sRestClientUtilsVersion1_1 = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator,
                    null, RestClient.REST_CLIENT_VERSIONS.V1_1);
        }
        return sRestClientUtilsVersion1_1;
    }

    public static RestClientUtils getRestClientUtilsV1_2() {
        if (sRestClientUtilsVersion1_2 == null) {
            sRestClientUtilsVersion1_2 = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator,
                    null, RestClient.REST_CLIENT_VERSIONS.V1_2);
        }
        return sRestClientUtilsVersion1_2;
    }

    public static RestClientUtils getRestClientUtilsV1_3() {
        if (sRestClientUtilsVersion1_3 == null) {
            sRestClientUtilsVersion1_3 = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator,
                    null, RestClient.REST_CLIENT_VERSIONS.V1_3);
        }
        return sRestClientUtilsVersion1_3;
    }

    public static RestClientUtils getRestClientUtilsV0() {
        if (sRestClientUtilsVersion0 == null) {
            sRestClientUtilsVersion0 = new RestClientUtils(mContext, sRequestQueue, sOAuthAuthenticator,
                    null, RestClient.REST_CLIENT_VERSIONS.V0);
        }
        return sRestClientUtilsVersion0;
    }

    public boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int connectionResult = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        switch (connectionResult) {
            // Success: return true
            case ConnectionResult.SUCCESS:
                return true;
            // Play Services unavailable, show an error dialog is the Play Services Lib needs an update
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Dialog dialog = googleApiAvailability.getErrorDialog(activity, connectionResult, 0);
                if (dialog != null) {
                    dialog.show();
                }
            default:
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_INVALID:
                AppLog.w(T.NOTIFS, "Google Play Services unavailable, connection result: "
                        + googleApiAvailability.getErrorString(connectionResult));
        }
        return false;
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
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            flushHttpCache();

            // Analytics resets
            AnalyticsTracker.endSession(false);
            AnalyticsTracker.clearAllData();

            // disable passcode lock
            AbstractAppLock appLock = AppLockManager.getInstance().getAppLock();
            if (appLock != null) {
                appLock.setPassword(null);
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onParseError(OnUnexpectedError event) {
        AppLog.d(T.API, "Receiving OnUnexpectedError event, message: " + event.exception.getMessage());
        String description = "FluxC: " + event.description;
        if (event.extras != null) {
            for (String key : event.extras.keySet()) {
                CrashlyticsUtils.setString(key, event.extras.get(key));
            }
        }
        CrashlyticsUtils.logException(event.exception, event.type, description);
    }

    public void removeWpComUserRelatedData(Context context) {
        // cancel all Volley requests - do this before unregistering push since that uses
        // a Volley request
        VolleyUtils.cancelAllRequests(sRequestQueue);

        NotificationsUtils.unregisterDevicePushNotifications(context);
        try {
            String gcmId = BuildConfig.GCM_ID;
            if (!TextUtils.isEmpty(gcmId)) {
                InstanceID.getInstance(context).deleteToken(gcmId, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            }
        } catch (Exception e) {
            AppLog.e(T.NOTIFS, "Could not delete GCM Token", e);
        }

        // reset default account
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        // delete wpcom and jetpack sites
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction());

        // reset all reader-related prefs & data
        AppPrefs.reset();
        ReaderDatabase.reset();

        // Reset Stats Data
        StatsDatabaseHelper.getDatabase(context).reset();
        StatsWidgetProvider.refreshAllWidgets(context, mSiteStore);

        // Reset Notifications Data
        NotificationsTable.reset();
    }

    /**
     * Device's default User-Agent string.
     * E.g.:
     *    "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
     *    AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile
     *    Safari/537.36"
     */
    private static String mDefaultUserAgent;
    public static String getDefaultUserAgent() {
        if (mDefaultUserAgent == null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mDefaultUserAgent = WebSettings.getDefaultUserAgent(getContext());
                } else {
                    mDefaultUserAgent = new WebView(getContext()).getSettings().getUserAgentString();
                }
            } catch (AndroidRuntimeException |  NullPointerException e) {
                // Catch AndroidRuntimeException that could be raised by the WebView() constructor.
                // See https://github.com/wordpress-mobile/WordPress-Android/issues/3594
                // Catch NullPointerException that could be raised by WebSettings.getDefaultUserAgent()
                // See https://github.com/wordpress-mobile/WordPress-Android/issues/3838

                // init with the empty string, it's a rare issue
                mDefaultUserAgent = "";
            }

        }
        return mDefaultUserAgent;
    }

    /**
     * User-Agent string when making HTTP connections, for both API traffic and WebViews.
     * Appends "wp-android/version" to WebView's default User-Agent string for the webservers
     * to get the full feature list of the browser and serve content accordingly, e.g.:
     *    "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
     *    AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile
     *    Safari/537.36 wp-android/4.7"
     * Note that app versions prior to 2.7 simply used "wp-android" as the user agent
     **/
    private static final String USER_AGENT_APPNAME = "wp-android";
    private static String mUserAgent;
    public static String getUserAgent() {
        if (mUserAgent == null) {
            String defaultUserAgent = getDefaultUserAgent();
            if (TextUtils.isEmpty(defaultUserAgent)) {
                mUserAgent = USER_AGENT_APPNAME + "/" + PackageUtils.getVersionName(getContext());
            } else {
                mUserAgent = defaultUserAgent + " "+ USER_AGENT_APPNAME + "/"
                        + PackageUtils.getVersionName(getContext());
            }
        }
        return mUserAgent;
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
     * @param application   Used to find the correct file
     * @param fieldName     The name of the field-to-access
     * @return              The value of the field, or {@code null} if the field is not found.
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
     * Detect when the app goes to the background and come back to the foreground.
     *
     * Turns out that when your app has no more visible UI, a callback is triggered.
     * The callback, implemented in this custom class, is called ComponentCallbacks2 (yes, with a two).
     *
     * This class also uses ActivityLifecycleCallbacks and a timer used as guard,
     * to make sure to detect the send to background event and not other events.
     *
     */
    private class ApplicationLifecycleMonitor implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
        private final int DEFAULT_TIMEOUT = 2 * 60; // 2 minutes
        private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 2000;

        private Date mLastPingDate;
        private Date mApplicationOpenedDate;
        private Timer mActivityTransitionTimer;
        private TimerTask mActivityTransitionTimerTask;
        private boolean mConnectionReceiverRegistered;

        boolean mFirstActivityResumed = true;
        boolean mIsInBackground = true;

        @Override
        public void onConfigurationChanged(final Configuration newConfig) {
            // Reapply locale on configuration change
            WPActivityUtils.applyLocale(getContext());
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
                default:
                    break;
            }

            if (evictBitmaps && mBitmapCache != null) {
                mBitmapCache.evictAll();
            }
        }

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
         * Check if user has valid credentials, and that at least 2 minutes are passed
         * since the last ping, then try to update the PN token.
         */
        private void updatePushNotificationTokenIfNotLimited() {
            // Synch Push Notifications settings
            if (isPushNotificationPingNeeded() && mAccountStore.hasAccessToken()) {
                // Register for Cloud messaging
                startService(new Intent(getContext(), GCMRegistrationIntentService.class));
            }
        }

        /**
         * The two methods below (startActivityTransitionTimer and stopActivityTransitionTimer)
         * are used to track when the app goes to background.
         *
         * Our implementation uses `onActivityPaused` and `onActivityResumed` of ApplicationLifecycleMonitor
         * to start and stop the timer that detects when the app goes to background.
         *
         * So when the user is simply navigating between the activities, the onActivityPaused()
         * calls `startActivityTransitionTimer` and starts the timer, but almost immediately the new activity being
         * entered, the ApplicationLifecycleMonitor cancels the timer in its onActivityResumed method, that in order
         * calls `stopActivityTransitionTimer` and so mIsInBackground would be false.
         *
         * In the case the app is sent to background, the TimerTask is instead executed, and the code that handles all
         * the background logic is run.
         */
        private void startActivityTransitionTimer() {
            this.mActivityTransitionTimer = new Timer();
            this.mActivityTransitionTimerTask = new TimerTask() {
                public void run() {
                    onAppGoesToBackground();
                }
            };

            this.mActivityTransitionTimer.schedule(mActivityTransitionTimerTask,
                    MAX_ACTIVITY_TRANSITION_TIME_MS);
        }

        private void onAppGoesToBackground() {
            AppLog.i(T.UTILS, "App goes to background");
            mIsInBackground = true;
            String lastActivityString = AppPrefs.getLastActivityStr();
            ActivityId lastActivity = ActivityId.getActivityIdFromName(lastActivityString);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("last_visible_screen", lastActivity.toString());
            if (mApplicationOpenedDate != null) {
                Date now = new Date();
                properties.put("time_in_app", DateTimeUtils.secondsBetween(now, mApplicationOpenedDate));
                mApplicationOpenedDate = null;
            }
            AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_CLOSED, properties);
            AnalyticsTracker.endSession(false);
            // Methods onAppComesFromBackground / onAppGoesToBackground are only workarounds to track when the
            // app goes to or comes from background, but they are not 100% reliable, we should avoid unregistering
            // the receiver twice.
            if (mConnectionReceiverRegistered) {
                mConnectionReceiverRegistered = false;
                unregisterReceiver(ConnectionChangeReceiver.getInstance());
            }
        }

        private void stopActivityTransitionTimer() {
            if (this.mActivityTransitionTimerTask != null) {
                this.mActivityTransitionTimerTask.cancel();
            }

            if (this.mActivityTransitionTimer != null) {
                this.mActivityTransitionTimer.cancel();
            }

            mIsInBackground = false;
        }

        /**
         * This method is called when:
         * 1. the app starts (but it's not opened by a service or a broadcast receiver, i.e. an activity is resumed)
         * 2. the app was in background and is now foreground
         */
        private void onAppComesFromBackground(Activity activity) {
            AppLog.i(T.UTILS, "App comes from background");
            // https://developer.android.com/reference/android/net/ConnectivityManager.html
            // Apps targeting Android 7.0 (API level 24) and higher do not receive this broadcast if they
            // declare the broadcast receiver in their manifest. Apps will still receive broadcasts if they
            // register their BroadcastReceiver with Context.registerReceiver() and that context is still valid.
            if (!mConnectionReceiverRegistered) {
                mConnectionReceiverRegistered = true;
                registerReceiver(ConnectionChangeReceiver.getInstance(),
                        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
            AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);
            mApplicationOpenedDate = new Date();
            Map<String, Boolean> properties = new HashMap<>(1);
            properties.put("pin_lock_enabled", AppLockManager.getInstance().getAppLock() != null
                    && AppLockManager.getInstance().getAppLock().isPasswordLocked());
            AnalyticsTracker.track(Stat.APPLICATION_OPENED, properties);
            if (NetworkUtils.isNetworkAvailable(mContext)) {
                // Refresh account informations and Notifications
                if (mAccountStore.hasAccessToken()) {
                    Intent intent = activity.getIntent();
                    if (intent != null && intent.hasExtra(NotificationsListFragment.NOTE_ID_EXTRA)) {
                        NotificationsUpdateService.startService(getContext(),
                                getNoteIdFromNoteDetailActivityIntent(activity.getIntent()));
                    } else {
                        NotificationsUpdateService.startService(getContext());
                    }
                }

                // Rate limited PN Token Update
                updatePushNotificationTokenIfNotLimited();

                // Rate limited WPCom blog list update
                mUpdateSiteList.runIfNotLimited();

                // Rate limited Site informations and options update
                mUpdateSelectedSite.runIfNotLimited();
            }
            sDeleteExpiredStats.runIfNotLimited();
        }

        // gets the note id from the extras that started this activity, so
        // we can remember to re-set that to unread once the note fetch update takes place
        private String getNoteIdFromNoteDetailActivityIntent(Intent intent) {
            String noteId = "";
            if (intent != null) {
                noteId = intent.getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            }
            return noteId;
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (mIsInBackground) {
                // was in background before
                onAppComesFromBackground(activity);
            }
            stopActivityTransitionTimer();

            mIsInBackground = false;
            if (mFirstActivityResumed) {
                deferredInit(activity);
            }
            mFirstActivityResumed = false;
        }

        @Override
        public void onActivityCreated(Activity arg0, Bundle arg1) {
        }

        @Override
        public void onActivityDestroyed(Activity arg0) {
        }

        @Override
        public void onActivityPaused(Activity arg0) {
            mLastPingDate = new Date();
            startActivityTransitionTimer();
        }

        @Override
        public void onActivitySaveInstanceState(Activity arg0, Bundle arg1) {
        }

        @Override
        public void onActivityStarted(Activity arg0) {
        }

        @Override
        public void onActivityStopped(Activity arg0) {
        }
    }
}
