package org.wordpress.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.analytics.AnalyticsTrackerMixpanel;
import org.wordpress.android.analytics.AnalyticsTrackerWPCom;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.networking.OAuthAuthenticator;
import org.wordpress.android.networking.OAuthAuthenticatorFactory;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.accounts.SetupBlogTask.GenericSetupBlogTask;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.ABTestingUtils;
import org.wordpress.android.util.ABTestingUtils.Feature;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BitmapLruCache;
import org.wordpress.android.util.PackageUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.RateLimitedTask;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.passcodelock.AbstractAppLock;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlrpc.android.ApiHelper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WordPress extends Application {
    public static final String ACCESS_TOKEN_PREFERENCE="wp_pref_wpcom_access_token";
    public static final String WPCOM_USERNAME_PREFERENCE="wp_pref_wpcom_username";
    public static final String WPCOM_PASSWORD_PREFERENCE="wp_pref_wpcom_password";

    public static String versionName;
    public static Blog currentBlog;
    public static Post currentPost;
    public static WordPressDB wpDB;
    public static WordPressStatsDB wpStatsDB;
    public static OnPostUploadedListener onPostUploadedListener = null;
    public static boolean postsShouldRefresh;
    public static boolean shouldRestoreSelectedActivity;
    public static RestClientUtils mRestClientUtils;
    public static RequestQueue requestQueue;
    public static ImageLoader imageLoader;

    public static final String BROADCAST_ACTION_SIGNOUT = "wp-signout";
    public static final String BROADCAST_ACTION_XMLRPC_INVALID_CREDENTIALS = "XMLRPC_INVALID_CREDENTIALS";
    public static final String BROADCAST_ACTION_XMLRPC_INVALID_SSL_CERTIFICATE = "INVALID_SSL_CERTIFICATE";
    public static final String BROADCAST_ACTION_XMLRPC_TWO_FA_AUTH = "TWO_FA_AUTH";
    public static final String BROADCAST_ACTION_XMLRPC_LOGIN_LIMIT = "LOGIN_LIMIT";
    public static final String BROADCAST_ACTION_BLOG_LIST_CHANGED = "BLOG_LIST_CHANGED";

    private static final int SECONDS_BETWEEN_STATS_UPDATE = 30 * 60;
    private static final int SECONDS_BETWEEN_OPTIONS_UPDATE = 10 * 60;
    private static final int SECONDS_BETWEEN_BLOGLIST_UPDATE = 6 * 60 * 60;

    private static Context mContext;
    private static BitmapLruCache mBitmapCache;


    /**
     *  Updates Options for the current blog in background.
     */
    public static RateLimitedTask sUpdateCurrentBlogOption = new RateLimitedTask(SECONDS_BETWEEN_OPTIONS_UPDATE) {
        protected boolean run() {
            Blog currentBlog = WordPress.getCurrentBlog();
            if (currentBlog != null) {
                new ApiHelper.RefreshBlogContentTask(mContext, currentBlog, null).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, false);
                return true;
            }
            return false;
        }
    };

    /**
     *  Updates the stats of the current blog in background. There is a timeout of 30 minutes that limits
     *  too frequent refreshes.
     *  User is not notified in case of errors.
     */
    public static RateLimitedTask sUpdateCurrentBlogStats = new RateLimitedTask(SECONDS_BETWEEN_STATS_UPDATE) {
        protected boolean run() {
            Blog currentBlog = WordPress.getCurrentBlog();
            if (currentBlog != null) {
                String blogID = null;
                if (currentBlog.isDotcomFlag()) {
                    blogID = String.valueOf(currentBlog.getRemoteBlogId());
                } else if (currentBlog.isJetpackPowered() && currentBlog.hasValidJetpackCredentials()) {
                    blogID = currentBlog.getApi_blogid(); // Can return null
                }
                if (blogID != null) {
                    // start service to get stats
                    Intent intent = new Intent(mContext, StatsService.class);
                    intent.putExtra(StatsService.ARG_BLOG_ID, blogID);
                    mContext.startService(intent);
                    return true;
                }
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
            new GenericSetupBlogTask(getContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        mContext = this;

        ProfilingUtils.start("WordPress.onCreate");
        // Enable log recording
        AppLog.enableRecording(true);
        if (!PackageUtils.isDebugBuild()) {
            Crashlytics.start(this);
        }
        versionName = PackageUtils.getVersionName(this);
        HelpshiftHelper.init(this);
        initWpDb();
        wpStatsDB = new WordPressStatsDB(this);

        RestClientUtils.setUserAgent(getUserAgent());

        configureSimperium();

        // Volley networking setup
        setupVolleyQueue();

        ABTestingUtils.init();

        String lastActivityStr = AppPrefs.getLastActivityStr();
        if (!TextUtils.isEmpty(lastActivityStr) && !lastActivityStr.equals(ActivityId.UNKNOWN)) {
            shouldRestoreSelectedActivity = true;
        }

        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            AppLockManager.getInstance().getCurrentAppLock().setDisabledActivities(
                    new String[]{"org.wordpress.android.ui.ShareIntentReceiverActivity"});
        }

        HelpshiftHelper.init(this);

        AnalyticsTracker.init();
        AnalyticsTracker.registerTracker(new AnalyticsTrackerMixpanel());
        AnalyticsTracker.registerTracker(new AnalyticsTrackerWPCom());
        AnalyticsTracker.beginSession();
        AnalyticsTracker.track(Stat.APPLICATION_STARTED);

        registerForCloudMessaging(this);

        ApplicationLifecycleMonitor pnBackendMonitor = new ApplicationLifecycleMonitor();
        registerComponentCallbacks(pnBackendMonitor);
        registerActivityLifecycleCallbacks(pnBackendMonitor);
    }

    // Configure Simperium and start buckets if we are signed in to WP.com
    private void configureSimperium() {
        if (!TextUtils.isEmpty(getWPComAuthToken(this))) {
            SimperiumUtils.configureSimperium(this, getWPComAuthToken(this));
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
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            currentBlog = null;
            editor.remove(WordPress.WPCOM_USERNAME_PREFERENCE);
            editor.remove(WordPress.WPCOM_PASSWORD_PREFERENCE);
            editor.remove(WordPress.ACCESS_TOKEN_PREFERENCE);
            editor.commit();
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
            // verify account data
            List<Map<String, Object>> accounts = wpDB.getAllAccounts();
            for (Map<String, Object> account : accounts) {
                if (account == null || account.get("blogName") == null || account.get("url") == null) {
                    return false;
                }
            }
            return true;
        } catch (SQLiteException sqle) {
            AppLog.e(T.DB, sqle);
            return false;
        } catch (RuntimeException re) {
            AppLog.e(T.DB, re);
            return false;
        }
    }

    public static Context getContext() {
        return mContext;
    }

    public static RestClientUtils getRestClientUtils() {
        if (mRestClientUtils == null) {
            OAuthAuthenticator authenticator = OAuthAuthenticatorFactory.instantiate();
            mRestClientUtils = new RestClientUtils(requestQueue, authenticator);
        }
        return mRestClientUtils;
    }

    /**
     * enables "strict mode" for testing - should NEVER be used in release builds
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
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

    /**
     * Register the device to Google Cloud Messaging service or return registration id if it's already registered.
     *
     * @return registration id or empty string if it's not registered.
     */
    private static String gcmRegisterIfNot(Context context) {
        String regId = "";
        try {
            GCMRegistrar.checkDevice(context);
            GCMRegistrar.checkManifest(context);
            regId = GCMRegistrar.getRegistrationId(context);
            String gcmId = BuildConfig.GCM_ID;
            if (gcmId != null && TextUtils.isEmpty(regId)) {
                GCMRegistrar.register(context, gcmId);
            }
        } catch (UnsupportedOperationException e) {
            // GCMRegistrar.checkDevice throws an UnsupportedOperationException if the device
            // doesn't support GCM (ie. non-google Android)
            AppLog.e(T.NOTIFS, "Device doesn't support GCM: " + e.getMessage());
        } catch (IllegalStateException e) {
            // GCMRegistrar.checkManifest or GCMRegistrar.register throws an IllegalStateException if Manifest
            // configuration is incorrect (missing a permission for instance) or if GCM dependencies are missing
            AppLog.e(T.NOTIFS, "APK (manifest error or dependency missing) doesn't support GCM: " + e.getMessage());
        }
        return regId;
    }

    public static void registerForCloudMessaging(Context context) {
        String regId = gcmRegisterIfNot(context);

        // Register to WordPress.com notifications
        if (WordPress.hasValidWPComCredentials(context)) {
            if (!TextUtils.isEmpty(regId)) {
                // Send the token to WP.com in case it was invalidated
                NotificationsUtils.registerDeviceForPushNotifications(context, regId);
                AppLog.v(T.NOTIFS, "Already registered for GCM");
            }
        }

        // Register to Helpshift notifications
        if (ABTestingUtils.isFeatureEnabled(Feature.HELPSHIFT)) {
            HelpshiftHelper.getInstance().registerDeviceToken(context, regId);
        }
        AnalyticsTracker.registerPushNotificationToken(regId);
    }

    public interface OnPostUploadedListener {
        public abstract void OnPostUploaded(int localBlogId, String postId, boolean isPage);
    }

    public static void setOnPostUploadedListener(OnPostUploadedListener listener) {
        onPostUploadedListener = listener;
    }

    public static void postUploaded(int localBlogId, String postId, boolean isPage) {
        if (onPostUploadedListener != null) {
            try {
                onPostUploadedListener.OnPostUploaded(localBlogId, postId, isPage);
            } catch (Exception e) {
                postsShouldRefresh = true;
            }
        } else {
            postsShouldRefresh = true;
        }

    }

    /**
     * Get the currently active blog.
     * <p/>
     * If the current blog is not already set, try and determine the last active blog from the last
     * time the application was used. If we're not able to determine the last active blog, just
     * select the first one.
     */
    public static Blog getCurrentBlog() {
        if (currentBlog == null || !wpDB.isDotComAccountVisible(currentBlog.getRemoteBlogId())) {
            // attempt to restore the last active blog
            setCurrentBlogToLastActive();

            // fallback to just using the first blog
            List<Map<String, Object>> accounts = WordPress.wpDB.getVisibleAccounts();
            if (currentBlog == null && accounts.size() > 0) {
                int id = Integer.valueOf(accounts.get(0).get("id").toString());
                setCurrentBlog(id);
                wpDB.updateLastBlogId(id);
            }
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
            Blog blog = wpDB.instantiateBlogByLocalId(id);
            return blog;
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
        List<Map<String, Object>> accounts = WordPress.wpDB.getVisibleAccounts();

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
     * @return the current blog
     */
    public static Blog setCurrentBlog(int id) {
        currentBlog = wpDB.instantiateBlogByLocalId(id);
        return currentBlog;
    }

    /**
     * returns the blogID of the current blog or -1 if current blog is null
     */
    public static int getCurrentRemoteBlogId() {
        return (getCurrentBlog() != null ? getCurrentBlog().getRemoteBlogId() : -1);
    }

    public static int getCurrentLocalTableBlogId() {
        return (getCurrentBlog() != null ? getCurrentBlog().getLocalTableBlogId() : -1);
    }

    /**
     * Checks for WordPress.com credentials
     *
     * @return true if we have credentials or false if not
     */
    public static boolean hasValidWPComCredentials(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String username = settings.getString(WPCOM_USERNAME_PREFERENCE, null);
        String password = settings.getString(WPCOM_PASSWORD_PREFERENCE, null);
        return username != null && password != null;
    }

    public static boolean isSignedIn(Context context) {
        if (WordPress.hasValidWPComCredentials(context)) {
            return true;
        }
        return WordPress.wpDB.getNumVisibleAccounts() != 0;
    }

    /**
     * Returns WordPress.com Auth Token
     *
     * @return String - The wpcom Auth token, or null if not authenticated.
     */
    public static String getWPComAuthToken(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(WordPress.ACCESS_TOKEN_PREFERENCE, null);

    }

    /**
     * Sign out from all accounts by clearing out the password, which will require user to sign in
     * again
     */
    public static void signOut(Context context) {
        removeWpComUserRelatedData(context);

        try {
            SelfSignedSSLCertsManager.getInstance(context).emptyLocalKeyStoreFile();
        } catch (GeneralSecurityException e) {
            AppLog.e(T.UTILS, "Error while cleaning the Local KeyStore File", e);
        } catch (IOException e) {
            AppLog.e(T.UTILS, "Error while cleaning the Local KeyStore File", e);
        }

        wpDB.deleteAllAccounts();
        wpDB.updateLastBlogId(-1);
        currentBlog = null;

        // General analytics resets
        AnalyticsTracker.endSession(false);
        AnalyticsTracker.clearAllData();

        // disable passcode lock
        AbstractAppLock appLock = AppLockManager.getInstance().getCurrentAppLock();
        if (appLock != null) {
            appLock.setPassword(null);
        }

        // send broadcast that user is signing out - this is received by WPActionBarActivity
        // descendants
        sendLocalBroadcast(context, BROADCAST_ACTION_SIGNOUT);
    }

    public static void removeWpComUserRelatedData(Context context) {
        // cancel all Volley requests - do this before unregistering push since that uses
        // a Volley request
        VolleyUtils.cancelAllRequests(requestQueue);

        NotificationsUtils.unregisterDevicePushNotifications(context);
        try {
            GCMRegistrar.checkDevice(context);
            GCMRegistrar.unregister(context);
        } catch (Exception e) {
            AppLog.v(T.NOTIFS, "Could not unregister for GCM: " + e.getMessage());
        }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(WordPress.WPCOM_USERNAME_PREFERENCE);
        editor.remove(WordPress.WPCOM_PASSWORD_PREFERENCE);
        editor.remove(WordPress.ACCESS_TOKEN_PREFERENCE);
        editor.commit();

        // reset all reader-related prefs & data
        AppPrefs.reset();
        ReaderDatabase.reset();

        // Reset Simperium buckets (removes local data)
        SimperiumUtils.resetBucketsAndDeauthorize();

        // send broadcast that user is signing out - this is received by WPActionBarActivity
        // descendants
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_SIGNOUT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
    }

    public static boolean sendLocalBroadcast(Context context, String action) {
        if (context == null || TextUtils.isEmpty(action)) {
            return false;
        }
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
        Intent intent = new Intent();
        intent.setAction(action);
        return lbm.sendBroadcast(intent);
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
     * User-Agent string when making HTTP connections, for both API traffic and WebViews.
     * Follows the format detailed at http://tools.ietf.org/html/rfc2616#section-14.43,
     * ie: "AppName/AppVersion (OS Version; Locale; Device)"
     *    "wp-android/2.6.4 (Android 4.3; en_US; samsung GT-I9505/jfltezh)"
     *    "wp-android/2.6.3 (Android 4.4.2; en_US; LGE Nexus 5/hammerhead)"
     * Note that app versions prior to 2.7 simply used "wp-android" as the user agent
     **/
    private static final String USER_AGENT_APPNAME = "wp-android";
    private static String mUserAgent;
    public static String getUserAgent() {
        if (mUserAgent == null) {
            mUserAgent = USER_AGENT_APPNAME + "/" + PackageUtils.getVersionName(getContext())
                       + " (Android " + Build.VERSION.RELEASE + "; "
                       + Locale.getDefault().toString() + "; "
                       + Build.MANUFACTURER + " " + Build.MODEL + "/" + Build.PRODUCT + ")";
        }
        return mUserAgent;
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
        private Date lastPingDate;
        private Date mApplicationOpenedDate;
        boolean isInBackground = true;

        @Override
        public void onConfigurationChanged(final Configuration newConfig) {
        }

        @Override
        public void onLowMemory() {
        }

        @Override
        public void onTrimMemory(final int level) {
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                // We're in the Background
                isInBackground = true;
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
            } else {
                isInBackground = false;
            }

            boolean evictBitmaps = false;
            switch (level) {
                case TRIM_MEMORY_COMPLETE:
                    AnalyticsTracker.track(Stat.MEMORY_TRIMMED_COMPLETE);
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
            if (lastPingDate == null) {
                // first startup
                return false;
            }

            Date now = new Date();
            if (DateTimeUtils.secondsBetween(now, lastPingDate) >= DEFAULT_TIMEOUT) {
                lastPingDate = now;
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
            if (isPushNotificationPingNeeded() && WordPress.hasValidWPComCredentials(mContext)) {
                String token = null;
                try {
                    // Register for Google Cloud Messaging
                    GCMRegistrar.checkDevice(mContext);
                    GCMRegistrar.checkManifest(mContext);
                    token = GCMRegistrar.getRegistrationId(mContext);
                    String gcmId = BuildConfig.GCM_ID;
                    if (gcmId == null || token == null || token.equals("") ) {
                        AppLog.e(T.NOTIFS, "Could not ping the PNs backend, Token or gmcID not found");
                    } else {
                        // Send the token to WP.com
                        NotificationsUtils.registerDeviceForPushNotifications(mContext, token);
                    }
                } catch (Exception e) {
                    AppLog.e(T.NOTIFS, "Could not ping the PNs backend: " + e.getMessage());
                }
            }
        }

        /**
         * This method is called when:
         * 1. the app starts (but it's not opened by a service, i.e. an activity is resumed)
         * 2. the app was in background and is now foreground
         */
        public void onFromBackground() {
            AnalyticsTracker.beginSession();
            mApplicationOpenedDate = new Date();
            AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_OPENED);
            if (NetworkUtils.isNetworkAvailable(mContext)) {
                // Rate limited PN Token Update
                updatePushNotificationTokenIfNotLimited();

                // Rate limited Stats Update
                sUpdateCurrentBlogStats.runIfNotLimited();

                // Rate limited WPCom blog list Update
                sUpdateWordPressComBlogList.runIfNotLimited();

                // Rate limited blog options Update
                sUpdateCurrentBlogOption.runIfNotLimited();
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (isInBackground) {
                // was in background before
                onFromBackground();
            }
            isInBackground = false;
        }

        @Override
        public void onActivityCreated(Activity arg0, Bundle arg1) {
        }

        @Override
        public void onActivityDestroyed(Activity arg0) {
        }

        @Override
        public void onActivityPaused(Activity arg0) {
            lastPingDate = new Date();
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
