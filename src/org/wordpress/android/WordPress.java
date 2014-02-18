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
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wordpress.rest.Oauth;

import org.apache.http.HttpResponse;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.notifications.NotificationUtils;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BitmapLruCache;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.WPMobileStatsUtil;
import org.wordpress.android.util.WPRestClient;
import org.wordpress.passcodelock.AppLockManager;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    public static Comment currentComment;
    public static Post currentPost;
    public static WordPressDB wpDB;
    public static WordPressStatsDB wpStatsDB;
    public static OnPostUploadedListener onPostUploadedListener = null;
    public static boolean postsShouldRefresh;
    public static boolean shouldRestoreSelectedActivity;
    public static WPRestClient restClient;
    public static RequestQueue requestQueue;
    public static ImageLoader imageLoader;
    public static final String TAG = "WordPress";
    public static final String BROADCAST_ACTION_SIGNOUT = "wp-signout";

    private static Context mContext;
    private static BitmapLruCache mBitmapCache;

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
        versionName = getVersionName();
        initWpDb();
        wpStatsDB = new WordPressStatsDB(this);
        mContext = this;

        // Volley networking setup
        requestQueue = Volley.newRequestQueue(this, getHttpClientStack());
        imageLoader = new ImageLoader(requestQueue, getBitmapCache());
        VolleyLog.setTag(TAG);

        // http://stackoverflow.com/a/17035814
        imageLoader.setBatchedResponseDelay(0);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getInt("wp_pref_last_activity", -1) >= 0)
            shouldRestoreSelectedActivity = true;

        restClient = new WPRestClient(requestQueue, new OauthAuthenticator());
        registerForCloudMessaging(this);

        // Uncomment this line if you want to test the app locking feature
        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);
        if (AppLockManager.getInstance().isAppLockFeatureEnabled())
            AppLockManager.getInstance().getCurrentAppLock().setDisabledActivities(new String[]{"org.wordpress.android.ui.ShareIntentReceiverActivity"});

        WPMobileStatsUtil.initialize();
        WPMobileStatsUtil.trackEventForWPCom(WPMobileStatsUtil.StatsEventAppOpened);

        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            PushNotificationsBackendMonitor pnBackendMponitor = new PushNotificationsBackendMonitor();
            registerComponentCallbacks(pnBackendMponitor);
            registerActivityLifecycleCallbacks(pnBackendMponitor);
         }

        //Enable log recording on beta build
        if (NotificationUtils.getAppPushNotificationsName().equals("org.wordpress.android.beta.build")) {
            AppLog.enableRecording(true);
        }
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
                String gcmId = Config.GCM_ID;
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

    /**
     * Get versionName from Manifest.xml
     *
     * @return versionName
     */
    private String getVersionName() {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            return pi.versionName == null ? "" : pi.versionName;
        } catch (NameNotFoundException e) {
            return "";
        }
    }

    public interface OnPostUploadedListener {
        public abstract void OnPostUploaded();
    }

    public static void setOnPostUploadedListener(OnPostUploadedListener listener) {
        onPostUploadedListener = listener;
    }

    public static void postUploaded() {
        if (onPostUploadedListener != null) {
            try {
                onPostUploadedListener.OnPostUploaded();
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
            Blog blog = new Blog(id);
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
        try {
            currentBlog = new Blog(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return currentBlog;
    }

    /*
     * returns the blogID of the current blog
     */
    public static int getCurrentRemoteBlogId() {
        return (currentBlog != null ? currentBlog.getRemoteBlogId() : -1);
    }

    public static int getCurrentLocalTableBlogId() {
        return (currentBlog != null ? currentBlog.getLocalTableBlogId() : -1);
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

    class OauthAuthenticator implements WPRestClient.Authenticator {

        @Override
        public void authenticate(WPRestClient.Request request) {

            String siteId = request.getSiteId();
            String token = null;
            Blog blog = null;

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.this);
            if (siteId == null) {
                // Use the global access token
                token = settings.getString(ACCESS_TOKEN_PREFERENCE, null);
            } else {
                blog = wpDB.getBlogForDotComBlogId(siteId);

                if (blog != null) {
                    // get the access token from api key field. Hetpack blogs likned with a different wpcom account have the token stored here.
                    token = blog.getApi_key();

                    // valid oauth tokens are 64 chars
                    if (token != null && token.length() < 64 && !blog.isDotcomFlag()) {
                        token = null;
                    }

                    // if there is no access token, we need to check if it is a dotcom blog, or a jetpack blog linked with the main wpcom account.
                    if (token == null){ 
                        if (blog.isDotcomFlag() && blog.getUsername().equals(settings.getString(WPCOM_USERNAME_PREFERENCE, ""))){ 
                            token = settings.getString(ACCESS_TOKEN_PREFERENCE, null);
                        } else if (blog.isJetpackPowered()) {
                            if (blog.getDotcom_username() == null ||  blog.getDotcom_username().equals(settings.getString(WPCOM_USERNAME_PREFERENCE, ""))) {
                                token = settings.getString(ACCESS_TOKEN_PREFERENCE, null);
                            }
                        }
                    }
                }
            }
            if (token != null) {
                // we have an access token, set the request and send it
                request.sendWithAccessToken(token);
            } else {
                // we don't have an access token, let's request one
                requestAccessToken(request, blog);
            }

        }

        public void requestAccessToken(final WPRestClient.Request request, final Blog blog) {

            Oauth oauth = new Oauth(Config.OAUTH_APP_ID, Config.OAUTH_APP_SECRET, Config.OAUTH_REDIRECT_URI);

            // make oauth volley request

            String username = null, password = null;
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.this);

            if (blog == null) {
                // We weren't give a specific blog, so we're going to user the username/password
                // from the "global" dotcom user account
                username = settings.getString(WPCOM_USERNAME_PREFERENCE, null);
                password = WordPressDB.decryptPassword(settings.getString(WPCOM_PASSWORD_PREFERENCE, null));
            } else {
                // use the requested blog's username password, if it's a dotcom blog, use the
                // username and password for the blog. If it's a jetpack blog (not isDotcomFlag)
                // then use the getDotcom_* methods for username/password
                if (blog.isDotcomFlag()) {
                    username = blog.getUsername();
                    password = blog.getPassword();
                } else {
                    username = blog.getDotcom_username();
                    password = blog.getDotcom_password();
                }
            }

            Request oauthRequest = oauth.makeRequest(username, password,

                    new Oauth.Listener() {

                        @Override
                        public void onResponse(Oauth.Token token) {
                            if (blog == null) {
                                settings.edit().putString(ACCESS_TOKEN_PREFERENCE, token.toString()).
                                        commit();
                            } else {
                                blog.setApi_key(token.toString());
                                blog.save();
                            }
                            request.sendWithAccessToken(token);
                        }

                    },

                    new Oauth.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            request.abort(error);
                        }

                    }
            );

            // add oauth request to the request queue
            requestQueue.add(oauthRequest);

        }
    }

    /**
     * Sign out from all accounts by clearing out the password, which will require user to sign in
     * again
     */
    public static void signOut(Context context) {
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

        wpDB.deleteAllAccounts();
        wpDB.updateLastBlogId(-1);
        currentBlog = null;

        // reset all reader-related prefs & data
        UserPrefs.reset();
        ReaderDatabase.reset();

        //Delete all the Notes
        WordPress.wpDB.clearNotes();

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

    public static HttpStack getHttpClientStack() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            HurlStack stack = new HurlStack() {
                @Override
                public HttpResponse performRequest(Request<?> request, Map<String, String> headers)
                        throws IOException, AuthFailureError {

                    if (request.getUrl() != null && StringUtils.getHost(request.getUrl()).endsWith("files.wordpress.com")) {
                        // Add the auth header to access private WP.com files
                        HashMap<String, String> authParams = new HashMap<String, String>();
                        authParams.put("Authorization", "Bearer " + getWPComAuthToken(mContext));
                        headers.putAll(authParams);
                    }

                    HashMap<String, String> defaultHeaders = new HashMap<String, String>();
                    if (DeviceUtils.getInstance().isBlackBerry()) {
                        defaultHeaders.put("User-Agent", DeviceUtils.getBlackBerryUserAgent());
                    } else {
                        defaultHeaders.put("User-Agent", "wp-android/" + WordPress.versionName);
                    }
                    headers.putAll(defaultHeaders);

                    return super.performRequest(request, headers);
                }
            };

            return stack;

        } else {
            HttpClientStack stack = new HttpClientStack(AndroidHttpClient.newInstance("volley/0")) {
                @Override
                public HttpResponse performRequest(Request<?> request, Map<String, String> headers)
                        throws IOException, AuthFailureError {

                    if (request.getUrl() != null && StringUtils.getHost(request.getUrl()).endsWith("files.wordpress.com")) {
                        // Add the auth header to access private WP.com files
                        HashMap<String, String> authParams = new HashMap<String, String>();
                        authParams.put("Authorization", "Bearer " + getWPComAuthToken(mContext));
                        headers.putAll(authParams);
                    }

                    HashMap<String, String> defaultHeaders = new HashMap<String, String>();
                    if (DeviceUtils.getInstance().isBlackBerry()) {
                        defaultHeaders.put("User-Agent", DeviceUtils.getBlackBerryUserAgent());
                    } else {
                        defaultHeaders.put("User-Agent", "wp-android/" + WordPress.versionName);
                    }
                    headers.putAll(defaultHeaders);

                    return super.performRequest(request, headers);
                }
            };

            return stack;
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
    private class PushNotificationsBackendMonitor implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

        private final int DEFAULT_TIMEOUT = 2 * 60; //2 minutes
        private Date lastPingDate;

        boolean background = false;

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
                background = true;
            } else {
                background = false;
            }

            //Levels that we need to consider are  TRIM_MEMORY_RUNNING_CRITICAL = 15; - TRIM_MEMORY_RUNNING_LOW = 10; - TRIM_MEMORY_RUNNING_MODERATE = 5;
            if (level < ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && mBitmapCache != null) {
                mBitmapCache.evictAll();
            }

        }

        private boolean mustPingPushNotificationsBackend() {

            if (WordPress.hasValidWPComCredentials(mContext) == false)
                return false;

            if (background == false)
                return false;

            background = false;

            if (lastPingDate == null)
                return false; //first startup

            Date now = new Date();
            long nowInMilliseconds = now.getTime();
            long lastPingDateInMilliseconds = lastPingDate.getTime();
            int secondsPassed = (int) (nowInMilliseconds - lastPingDateInMilliseconds)/(1000);
            if (secondsPassed >= DEFAULT_TIMEOUT) {
                lastPingDate = now;
                return true;
            }

            return false;
        }

        @Override
        public void onActivityResumed(Activity arg0) {
            if(mustPingPushNotificationsBackend()) {
                //uhhh ohhh!

                if (WordPress.hasValidWPComCredentials(mContext)) {
                    String token = null;
                    try {
                        // Register for Google Cloud Messaging
                        GCMRegistrar.checkDevice(mContext);
                        GCMRegistrar.checkManifest(mContext);
                        token = GCMRegistrar.getRegistrationId(mContext);
                        String gcmId = Config.GCM_ID;
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

            }
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
