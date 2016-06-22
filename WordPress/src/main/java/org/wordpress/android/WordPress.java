package org.wordpress.android;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.analytics.AnalyticsTrackerMixpanel;
import org.wordpress.android.analytics.AnalyticsTrackerNosara;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.networking.OAuthAuthenticator;
import org.wordpress.android.networking.OAuthAuthenticatorFactory;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.accounts.helpers.UpdateBlogListTask.GenericUpdateBlogListTask;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.plans.PlansUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.ui.stats.datasets.StatsDatabaseHelper;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BitmapLruCache;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.CoreEvents.UserSignedOutCompletely;
import org.wordpress.android.util.CoreEvents.UserSignedOutWordPressCom;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PackageUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.RateLimitedTask;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.passcodelock.AbstractAppLock;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlrpc.android.ApiHelper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;
import io.fabric.sdk.android.Fabric;

public class WordPress extends MultiDexApplication {
    public static String versionName;
    public static Blog currentBlog;
    public static WordPressDB wpDB;

    public static RequestQueue requestQueue;
    public static ImageLoader imageLoader;

    private static RestClientUtils mRestClientUtils;
    private static RestClientUtils mRestClientUtilsVersion1_1;
    private static RestClientUtils mRestClientUtilsVersion1_2;
    private static RestClientUtils mRestClientUtilsVersion1_3;
    private static RestClientUtils mRestClientUtilsVersion0;

    private static final int SECONDS_BETWEEN_OPTIONS_UPDATE = 10 * 60;
    private static final int SECONDS_BETWEEN_BLOGLIST_UPDATE = 6 * 60 * 60;
    private static final int SECONDS_BETWEEN_DELETE_STATS = 5 * 60; // 5 minutes

    private static Context mContext;
    private static BitmapLruCache mBitmapCache;

    /**
     * Updates Options for the current blog in background.
     */
    public static RateLimitedTask sUpdateCurrentBlogOption = new RateLimitedTask(SECONDS_BETWEEN_OPTIONS_UPDATE) {
        protected boolean run() {
            Blog currentBlog = WordPress.getCurrentBlog();
            if (currentBlog != null) {
                new ApiHelper.RefreshBlogContentTask(currentBlog, null).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, false);
                return true;
            }
            return false;
        }
    };

    /**
     *  Update blog list in a background task. Broadcast WordPress.BROADCAST_ACTION_BLOG_LIST_CHANGED if the
     *  list changed.
     */
    public static RateLimitedTask sUpdateWordPressComBlogList = new RateLimitedTask(SECONDS_BETWEEN_BLOGLIST_UPDATE) {
        protected boolean run() {
            if (AccountHelper.isSignedInWordPressDotCom()) {
                new GenericUpdateBlogListTask(getContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
            int cacheSize = maxMemory / 16;  //Use 1/16th of the available memory for this memory cache.
            mBitmapCache = new BitmapLruCache(cacheSize);
        }
        return mBitmapCache;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        long startDate = SystemClock.elapsedRealtime();

        mContext = this;

        ProfilingUtils.start("App Startup");
        // Enable log recording
        AppLog.enableRecording(true);
        AppLog.i(T.UTILS, "WordPress.onCreate");

        if (!PackageUtils.isDebugBuild()) {
            Fabric.with(this, new Crashlytics());
        }

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
        EventBus.getDefault().register(this);

        RestClientUtils.setUserAgent(getUserAgent());

        // Volley networking setup
        setupVolleyQueue();

        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);
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
    }

    private void initAnalytics(final long elapsedTimeOnCreate) {
        AnalyticsTracker.registerTracker(new AnalyticsTrackerMixpanel(getContext(), BuildConfig.MIXPANEL_TOKEN));
        AnalyticsTracker.registerTracker(new AnalyticsTrackerNosara(getContext()));
        AnalyticsTracker.init(getContext());
        AnalyticsUtils.refreshMetadata();

        // Track app upgrade and install
        int versionCode = PackageUtils.getVersionCode(getContext());

        int oldVersionCode = AppPrefs.getLastAppVersionCode();
        if (oldVersionCode == 0) {
            // Track application installed if there isn't old version code
            AnalyticsTracker.track(Stat.APPLICATION_INSTALLED);
            AppPrefs.setVisualEditorPromoRequired(false);
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
        configureSimperium();

        // Refresh account informations
        if (AccountHelper.isSignedInWordPressDotCom()) {
            AccountHelper.getDefaultAccount().fetchAccountDetails();
        }
    }

    // Configure Simperium and start buckets if we are signed in to WP.com
    private void configureSimperium() {
        if (AccountHelper.isSignedInWordPressDotCom()) {
            AppLog.i(T.NOTIFS, "Configuring Simperium");
            SimperiumUtils.configureSimperium(this, AccountHelper.getDefaultAccount().getAccessToken());
        }
    }

    public static void setupVolleyQueue() {
        requestQueue = Volley.newRequestQueue(mContext, VolleyUtils.getHTTPClientStack(mContext));
        imageLoader = new ImageLoader(requestQueue, getBitmapCache());
        VolleyLog.setTag(AppLog.TAG);
        // http://stackoverflow.com/a/17035814
        imageLoader.setBatchedResponseDelay(0);
    }

    private void initWpDb() {
        if (!createAndVerifyWpDb()) {
            AppLog.e(T.DB, "Invalid database, sign out user and delete database");
            currentBlog = null;
            if (wpDB != null) {
                wpDB.updateLastBlogId(-1);
            }
            // Force DB deletion
            WordPressDB.deleteDatabase(this);
            wpDB = new WordPressDB(this);
        }
    }

    private boolean createAndVerifyWpDb() {
        try {
            wpDB = new WordPressDB(this);
            // verify account data - query will return 1 if any blog names or urls are null
            int result = SqlUtils.intForQuery(wpDB.getDatabase(),
                    "SELECT 1 FROM accounts WHERE blogName IS NULL OR url IS NULL LIMIT 1", null);
            return result != 1;
        } catch (RuntimeException e) {
            AppLog.e(T.DB, e);
            return false;
        }
    }

    public static Context getContext() {
        return mContext;
    }

    public static RestClientUtils getRestClientUtils() {
        if (mRestClientUtils == null) {
            OAuthAuthenticator authenticator = OAuthAuthenticatorFactory.instantiate();
            mRestClientUtils = new RestClientUtils(mContext, requestQueue, authenticator, mOnAuthFailedListener);
        }
        return mRestClientUtils;
    }

    private static RestRequest.OnAuthFailedListener mOnAuthFailedListener = new RestRequest.OnAuthFailedListener() {
        @Override
        public void onAuthFailed() {
            if (getContext() == null) return;
            // If this is called, it means the WP.com token is no longer valid.
            EventBus.getDefault().post(new CoreEvents.RestApiUnauthorized());
        }
    };

    public static RestClientUtils getRestClientUtilsV1_1() {
        if (mRestClientUtilsVersion1_1 == null) {
            OAuthAuthenticator authenticator = OAuthAuthenticatorFactory.instantiate();
            mRestClientUtilsVersion1_1 = new RestClientUtils(mContext, requestQueue, authenticator, mOnAuthFailedListener, RestClient.REST_CLIENT_VERSIONS.V1_1);
        }
        return mRestClientUtilsVersion1_1;
    }

    public static RestClientUtils getRestClientUtilsV1_2() {
        if (mRestClientUtilsVersion1_2 == null) {
            OAuthAuthenticator authenticator = OAuthAuthenticatorFactory.instantiate();
            mRestClientUtilsVersion1_2 = new RestClientUtils(mContext, requestQueue, authenticator, mOnAuthFailedListener, RestClient.REST_CLIENT_VERSIONS.V1_2);
        }
        return mRestClientUtilsVersion1_2;
    }

    public static RestClientUtils getRestClientUtilsV1_3() {
        if (mRestClientUtilsVersion1_3 == null) {
            OAuthAuthenticator authenticator = OAuthAuthenticatorFactory.instantiate();
            mRestClientUtilsVersion1_3 = new RestClientUtils(mContext, requestQueue, authenticator, mOnAuthFailedListener, RestClient.REST_CLIENT_VERSIONS.V1_3);
        }
        return mRestClientUtilsVersion1_3;
    }

    public static RestClientUtils getRestClientUtilsV0() {
        if (mRestClientUtilsVersion0 == null) {
            OAuthAuthenticator authenticator = OAuthAuthenticatorFactory.instantiate();
            mRestClientUtilsVersion0 = new RestClientUtils(mContext, requestQueue, authenticator, mOnAuthFailedListener, RestClient.REST_CLIENT_VERSIONS.V0);
        }
        return mRestClientUtilsVersion0;
    }

    /**
     * enables "strict mode" for testing - should NEVER be used in release builds
     */
    private static void enableStrictMode() {
        // return if the build is not a debug build
        if (!BuildConfig.DEBUG) {
            AppLog.e(T.UTILS, "You should not call enableStrictMode() on a non debug build");
            return;
        }

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyFlashScreen()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects() // <-- requires Jelly Bean
                .penaltyLog()
                .build());

        AppLog.w(T.UTILS, "Strict mode enabled");
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
     * Get the currently active blog.
     * <p/>
     * If the current blog is not already set, try and determine the last active blog from the last
     * time the application was used. If we're not able to determine the last active blog, try to
     * select the first visible blog. If there are no more visible blogs, try to select the first
     * hidden blog. If there are no blogs at all, return null.
     */
    public static Blog getCurrentBlog() {
        if (currentBlog == null || !wpDB.isDotComBlogVisible(currentBlog.getRemoteBlogId())) {
            attemptToRestoreLastActiveBlog();
        }

        return currentBlog;
    }

    /**
     * Get the blog with the specified ID.
     *
     * @param id ID of the blog to retrieve.
     * @return the blog with the specified ID, or null if blog could not be retrieved.
     */
    public static Blog getBlog(int id) {
        try {
            return wpDB.instantiateBlogByLocalId(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set the last active blog as the current blog.
     *
     * @return the current blog
     */
    public static Blog setCurrentBlogToLastActive() {
        List<Map<String, Object>> accounts = WordPress.wpDB.getVisibleBlogs();

        int lastBlogId = WordPress.wpDB.getLastBlogId();
        if (lastBlogId != -1) {
            for (Map<String, Object> account : accounts) {
                int id = Integer.valueOf(account.get("id").toString());
                if (id == lastBlogId) {
                    setCurrentBlog(id);
                    return currentBlog;
                }
            }
        }
        // Previous active blog is hidden or deleted
        currentBlog = null;
        return null;
    }

    /**
     * Set the blog with the specified id as the current blog.
     *
     * @param id id of the blog to set as current
     */
    public static void setCurrentBlog(int id) {
        currentBlog = getBlog(id);
    }

    public static void setCurrentBlogAndSetVisible(int id) {
        setCurrentBlog(id);

        if (currentBlog != null && currentBlog.isHidden()) {
            wpDB.setDotComBlogsVisibility(id, true);
            currentBlog.setHidden(false);
        }
    }

    /**
     * returns the blogID of the current blog or null if current blog is null or remoteID is null.
     */
    public static String getCurrentRemoteBlogId() {
        return (getCurrentBlog() != null ? getCurrentBlog().getDotComBlogId() : null);
    }

    public static int getCurrentLocalTableBlogId() {
        return (getCurrentBlog() != null ? getCurrentBlog().getLocalTableBlogId() : -1);
    }

    /**
     * Sign out from wpcom account.
     * Note: This method must not be called on UI Thread.
     */
    public static void WordPressComSignOut(Context context) {
        // Keep the analytics tracking at the beginning, before the account data is actual removed.
        AnalyticsTracker.track(Stat.ACCOUNT_LOGOUT);

        removeWpComUserRelatedData(context);

        // broadcast an event: wpcom user signed out
        EventBus.getDefault().post(new UserSignedOutWordPressCom());

        // broadcast an event only if the user is completely signed out
        if (!AccountHelper.isSignedIn()) {
            EventBus.getDefault().post(new UserSignedOutCompletely());
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UserSignedOutCompletely event) {
        try {
            SelfSignedSSLCertsManager.getInstance(getContext()).emptyLocalKeyStoreFile();
        } catch (GeneralSecurityException e) {
            AppLog.e(T.UTILS, "Error while cleaning the Local KeyStore File", e);
        } catch (IOException e) {
            AppLog.e(T.UTILS, "Error while cleaning the Local KeyStore File", e);
        }

        flushHttpCache();

        // Analytics resets
        AnalyticsTracker.endSession(false);
        AnalyticsTracker.clearAllData();

        // disable passcode lock
        AbstractAppLock appLock = AppLockManager.getInstance().getAppLock();
        if (appLock != null) {
            appLock.setPassword(null);
        }

        // dangerously delete all content!
        wpDB.dangerouslyDeleteAllContent();
    }


    public static void removeWpComUserRelatedData(Context context) {
        // cancel all Volley requests - do this before unregistering push since that uses
        // a Volley request
        VolleyUtils.cancelAllRequests(requestQueue);

        NotificationsUtils.unregisterDevicePushNotifications(context);
        try {
            String gcmId = BuildConfig.GCM_ID;
            if (!TextUtils.isEmpty(gcmId)) {
                InstanceID.getInstance(context).deleteToken(gcmId, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            }
        } catch (Exception e) {
            AppLog.e(T.NOTIFS, "Could not delete GCM Token", e);
        }

        // delete wpcom blogs
        wpDB.deleteWordPressComBlogs(context);

        // reset default account
        AccountHelper.getDefaultAccount().signout();

        // reset all reader-related prefs & data
        AppPrefs.reset();
        ReaderDatabase.reset();

        // Reset Stats Data
        StatsDatabaseHelper.getDatabase(context).reset();
        StatsWidgetProvider.updateWidgetsOnLogout(context);

        // Reset Simperium buckets (removes local data)
        SimperiumUtils.resetBucketsAndDeauthorize();
    }

    public static String getLoginUrl(Blog blog) {
        String loginURL = null;
        Gson gson = new Gson();
        Type type = new TypeToken<Map<?, ?>>() {
        }.getType();
        Map<?, ?> blogOptions = gson.fromJson(blog.getBlogOptions(), type);
        if (blogOptions != null) {
            Map<?, ?> homeURLMap = (Map<?, ?>) blogOptions.get("login_url");
            if (homeURLMap != null)
                loginURL = homeURLMap.get("value").toString();
        }
        // Try to guess the login URL if blogOptions is null (blog not added to the app), or WP version is < 3.6
        if (loginURL == null) {
            if (blog.getUrl().lastIndexOf("/") != -1) {
                return blog.getUrl().substring(0, blog.getUrl().lastIndexOf("/"))
                        + "/wp-login.php";
            } else {
                return blog.getUrl().replace("xmlrpc.php", "wp-login.php");
            }
        }

        return loginURL;
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

    private static void attemptToRestoreLastActiveBlog() {
        if (setCurrentBlogToLastActive() == null) {
            int blogId = WordPress.wpDB.getFirstVisibleBlogId();
            if (blogId == 0) {
                blogId = WordPress.wpDB.getFirstHiddenBlogId();
            }

            setCurrentBlogAndSetVisible(blogId);
            wpDB.updateLastBlogId(blogId);
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
        private Date mLastPingDate;
        private Date mApplicationOpenedDate;
        boolean mFirstActivityResumed = true;
        private Timer mActivityTransitionTimer;
        private TimerTask mActivityTransitionTimerTask;
        private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 2000;
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
            if (isPushNotificationPingNeeded() && AccountHelper.isSignedInWordPressDotCom()) {
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
         * So when the user is simply navigating between the activities, the onActivityPaused() calls `startActivityTransitionTimer`
         * and starts the timer, but almost immediately the new activity being entered, the ApplicationLifecycleMonitor cancels the timer
         * in its onActivityResumed method, that in order calls `stopActivityTransitionTimer`.
         * And so mIsInBackground would be false.
         *
         * In the case the app is sent to background, the TimerTask is instead executed, and the code that handles all the background logic is run.
         */
        private void startActivityTransitionTimer() {
            this.mActivityTransitionTimer = new Timer();
            this.mActivityTransitionTimerTask = new TimerTask() {
                public void run() {
                    AppLog.i(T.UTILS, "App goes to background");
                    // We're in the Background
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
                    ConnectionChangeReceiver.setEnabled(WordPress.this, false);
                }
            };

            this.mActivityTransitionTimer.schedule(mActivityTransitionTimerTask,
                    MAX_ACTIVITY_TRANSITION_TIME_MS);
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
        private void onAppComesFromBackground() {
            AppLog.i(T.UTILS, "App comes from background");
            ConnectionChangeReceiver.setEnabled(WordPress.this, true);
            AnalyticsUtils.refreshMetadata();
            mApplicationOpenedDate = new Date();
            AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_OPENED);
            if (NetworkUtils.isNetworkAvailable(mContext)) {
                // Rate limited PN Token Update
                updatePushNotificationTokenIfNotLimited();

                // Rate limited WPCom blog list Update
                sUpdateWordPressComBlogList.runIfNotLimited();

                // Rate limited blog options Update
                sUpdateCurrentBlogOption.runIfNotLimited();
            }
            sDeleteExpiredStats.runIfNotLimited();
            PlansUtils.synchIAPsWordPressCom();
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (mIsInBackground) {
                // was in background before
                onAppComesFromBackground();
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
