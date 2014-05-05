package org.wordpress.android;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.networking.OAuthAuthenticator;
import org.wordpress.android.networking.OAuthAuthenticatorFactory;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.notifications.NotificationUtils;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BitmapLruCache;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.SimperiumUtils;
import org.wordpress.android.util.Utils;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;
import org.wordpress.android.util.stats.AnalyticsTrackerMixpanel;
import org.wordpress.android.util.stats.AnalyticsTrackerWPCom;
import org.wordpress.passcodelock.AppLockManager;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WordPress extends Application {
    public static final String ACCESS_TOKEN_PREFERENCE="wp_pref_wpcom_access_token";
    public static final String WPCOM_USERNAME_PREFERENCE="wp_pref_wpcom_username";
    public static final String WPCOM_PASSWORD_PREFERENCE="wp_pref_wpcom_password";
    private static final String APP_ID_PROPERTY="oauth.app_id";
    private static final String APP_SECRET_PROPERTY="oauth.app_secret";
    private static final String APP_REDIRECT_PROPERTY="oauth.redirect_uri";

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
    public static final String BROADCAST_ACTION_SIMPERIUM_SIGNED_IN = "simperium-signin";
    public static final String BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED = "simperium-not-authorized";
    public static final String BROADCAST_ACTION_XMLRPC_INVALID_CREDENTIALS = "XMLRPC_INVALID_CREDENTIALS";
    public static final String BROADCAST_ACTION_XMLRPC_INVALID_SSL_CERTIFICATE = "INVALID_SSL_CERTIFICATE";
    public static final String BROADCAST_ACTION_XMLRPC_TWO_FA_AUTH = "TWO_FA_AUTH";
    public static final String BROADCAST_ACTION_XMLRPC_LOGIN_LIMIT = "LOGIN_LIMIT";

    private static Context mContext;
    private static BitmapLruCache mBitmapCache;

    private static Date mStatsLastPingDate; //last time stats were updated in background

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
        ProfilingUtils.start("WordPress.onCreate");
        // Enable log recording
        AppLog.enableRecording(true);
        if (!Utils.isDebugBuild()) {
            Crashlytics.start(this);
            AppLog.enableCrashlytics(true);
        }
        versionName = getVersionName(this);
        initWpDb();
        wpStatsDB = new WordPressStatsDB(this);
        mContext = this;

        configureSimperium();

        // Volley networking setup
        setupVolleyQueue();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getInt("wp_pref_last_activity", -1) >= 0) {
            shouldRestoreSelectedActivity = true;
        }
        registerForCloudMessaging(this);

        // Uncomment this line if you want to test the app locking feature
        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            AppLockManager.getInstance().getCurrentAppLock().setDisabledActivities(
                    new String[]{"org.wordpress.android.ui.ShareIntentReceiverActivity"});
        }

        AnalyticsTracker.registerTracker(new AnalyticsTrackerMixpanel());
        AnalyticsTracker.registerTracker(new AnalyticsTrackerWPCom());
        AnalyticsTracker.beginSession();
        AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_OPENED);

        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ApplicationLifecycleMonitor pnBackendMponitor = new ApplicationLifecycleMonitor();
            registerComponentCallbacks(pnBackendMponitor);
            registerActivityLifecycleCallbacks(pnBackendMponitor);
        }

        updateCurrentBlogStatsInBackground(false);
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
                wpDB.deleteDatabase(this);
            }
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

    /*
     * enables "strict mode" for testing - should NEVER be used in release builds
     */
    @SuppressLint("NewApi")
    private static void enableStrictMode() {
        // strict mode requires API level 9 or later
        if (Build.VERSION.SDK_INT < 9)
            return;

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
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build());

        AppLog.w(T.UTILS, "Strict mode enabled");
    }

    public static void registerForCloudMessaging(Context ctx) {
        if (WordPress.hasValidWPComCredentials(ctx)) {
            String token = null;
            try {
                // Register for Google Cloud Messaging
                GCMRegistrar.checkDevice(ctx);
                GCMRegistrar.checkManifest(ctx);
                token = GCMRegistrar.getRegistrationId(ctx);
                String gcmId = BuildConfig.GCM_ID;
                if (gcmId != null && token.equals("")) {
                    GCMRegistrar.register(ctx, gcmId);
                } else {
                    // Send the token to WP.com in case it was invalidated
                    NotificationUtils.registerDeviceForPushNotifications(ctx, token);
                    AppLog.v(T.NOTIFS, "Already registered for GCM");
                }
            } catch (Exception e) {
                AppLog.e(T.NOTIFS, "Could not register for GCM: " + e.getMessage());
            }
        }
    }

    public interface OnPostUploadedListener {
        public abstract void OnPostUploaded(int localBlogId, String postId, boolean isPage);
    }

    public static String getVersionName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName == null ? "" : pi.versionName;
        } catch (NameNotFoundException e) {
            return "";
        }
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

    /*
     * returns the blogID of the current blog
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
        AnalyticsTracker.clearAllData();

        // send broadcast that user is signing out - this is received by WPActionBarActivity
        // descendants
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_SIGNOUT);
        context.sendBroadcast(broadcastIntent);
    }

    public static void removeWpComUserRelatedData(Context context) {
        // cancel all Volley requests - do this before unregistering push since that uses
        // a Volley request
        VolleyUtils.cancelAllRequests(requestQueue);

        NotificationUtils.unregisterDevicePushNotifications(context);
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
        UserPrefs.reset();
        ReaderDatabase.reset();

        SimperiumUtils.getNotesBucket().reset();
        SimperiumUtils.getMetaBucket().reset();

        // send broadcast that user is signing out - this is received by WPActionBarActivity
        // descendants
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_SIGNOUT);
        context.sendBroadcast(broadcastIntent);
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
            PackageInfo pkgInfo;
            try {
                String pkgName = getContext().getApplicationInfo().packageName;
                pkgInfo = getContext().getPackageManager().getPackageInfo(pkgName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                return USER_AGENT_APPNAME;
            }

            mUserAgent = USER_AGENT_APPNAME + "/" + pkgInfo.versionName
                       + " (Android " + Build.VERSION.RELEASE + "; "
                       + Locale.getDefault().toString() + "; "
                       + Build.MANUFACTURER + " " + Build.MODEL + "/" + Build.PRODUCT + ")";
        }
        return mUserAgent;
    }

    /*
     *  Updates the stats of the current blog in background. There is a timeout of 30 minutes that limits
     *  too frequent refreshes.
     *  User is not notified in case of errors.
     */
    public synchronized static void updateCurrentBlogStatsInBackground(boolean alwaysUpdate) {

        if (!alwaysUpdate && mStatsLastPingDate != null) {
        	// check if the last stats refresh was done less than 30 minute ago
            if (DateTimeUtils.minutesBetween(new Date(), mStatsLastPingDate) < 30) {
              return;
            }
        }

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
                mStatsLastPingDate = new Date(); //set the last ping time
            }
        }
    }

    /*
     * Detect when the app goes to the background and come back to the foreground.
     *
     * Turns out that when your app has no more visible UI, a callback is triggered.
     * The callback, implemented in this custom class, is called ComponentCallbacks2 (yes, with a two).
     * This callback is only available in API Level 14 (Ice Cream Sandwich) and above.
     *
     * This class also uses ActivityLifecycleCallbacks and a timer used as guard, to make sure to detect the send to background event and not other events.
     *
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private class ApplicationLifecycleMonitor implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

        private final int DEFAULT_TIMEOUT = 2 * 60; //2 minutes
        private Date lastPingDate;

        boolean isInBackground = false;

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
                AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_CLOSED);
                AnalyticsTracker.endSession();
            } else {
                isInBackground = false;
            }

            //Levels that we need to consider are  TRIM_MEMORY_RUNNING_CRITICAL = 15; - TRIM_MEMORY_RUNNING_LOW = 10; - TRIM_MEMORY_RUNNING_MODERATE = 5;
            if (level < ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && mBitmapCache != null) {
                mBitmapCache.evictAll();
            }

        }

        /*
         * Checks that the network connection is available, and that at least 2 minutes are passed
         * since the last ping.
         */
        private boolean canSynchWithWordPressDotComBackend() {

            if (isInBackground == false) //The app wasn't in background. No need to ping the backend again.
                return false;

            isInBackground = false; //The app moved from background -> foreground. Set this flag to false for security reason.

            if (!NetworkUtils.isNetworkAvailable(mContext))
                return false;

            if (lastPingDate == null)
                return false; //first startup

            Date now = new Date();
            if (DateTimeUtils.secondsBetween(now,lastPingDate) >= DEFAULT_TIMEOUT) {
                lastPingDate = now;
                return true;
            }

            return false;
        }

        @Override
        public void onActivityResumed(Activity arg0) {

            if (!canSynchWithWordPressDotComBackend())
                return;

            /* Note: The Code below is not called at startup */

            //Synch Push Notifications settings
            if (WordPress.hasValidWPComCredentials(mContext)) {
                String token = null;
                try {
                    // Register for Google Cloud Messaging
                    GCMRegistrar.checkDevice(mContext);
                    GCMRegistrar.checkManifest(mContext);
                    token = GCMRegistrar.getRegistrationId(mContext);
                    String gcmId = BuildConfig.GCM_ID;
                    if (gcmId == null || token == null || token.equals("") ) {
                        AppLog.e(T.NOTIFS, "Could not ping the PNs backend, Token or gmcID not found");
                        return;
                    } else {
                        // Send the token to WP.com
                        NotificationUtils.registerDeviceForPushNotifications(mContext, token);
                    }
                } catch (Exception e) {
                    AppLog.e(T.NOTIFS, "Could not ping the PNs backend: " + e.getMessage());
                }
            }

            //Update Stats!
            WordPress.updateCurrentBlogStatsInBackground(false);
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
