package org.wordpress.android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
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
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.BitmapLruCache;
import org.wordpress.android.util.WPRestClient;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlrpc.android.WPComXMLRPCApi;

import java.io.IOException;
import java.lang.reflect.Type;
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
    public static BitmapLruCache localImageCache;

    private static Context mContext;
    
    public static final String TAG="WordPress";

    @Override
    public void onCreate() {
        versionName = getVersionName();
        wpDB = new WordPressDB(this);
        
        wpStatsDB = new WordPressStatsDB(this);
        
        mContext = this;

        // Volley networking setup
        requestQueue = Volley.newRequestQueue(this, getHttpClientStack());
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use a small slice of available memory for the image cache
        int cacheSize = maxMemory / 32;
        imageLoader = new ImageLoader(requestQueue, new BitmapLruCache(cacheSize));

        // Volley only caches images from network, not disk, so we'll use this instead for local disk image caching
        localImageCache = new BitmapLruCache(cacheSize / 2);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);  
        if (settings.getInt("wp_pref_last_activity", -1) >= 0)
            shouldRestoreSelectedActivity = true;

        restClient = new WPRestClient(requestQueue, new OauthAuthenticator(), settings.getString(ACCESS_TOKEN_PREFERENCE, null));
        registerForCloudMessaging(this);
        
        //Uncomment this line if you want to test the app locking feature
        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);

        super.onCreate();
    }
    
    public static Context getContext(){
        return mContext;
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
                    new WPComXMLRPCApi().registerWPComToken(ctx, token);
                    Log.v("WORDPRESS", "Already registered for GCM");
                }
            } catch (Exception e) {
                Log.v("WORDPRESS", "Could not register for GCM: " + e.getMessage());
            }
        }
    }
        
    /**
     * Get versionName from Manifest.xml
     * @return versionName
     */
    private String getVersionName(){
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            return pi.versionName;
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
     * <p>
     * If the current blog is not already set, try and determine the last active blog from the last
     * time the application was used. If we're not able to determine the last active blog, just
     * select the first one.
     */
    public static Blog getCurrentBlog() {
        if (currentBlog == null) {
            // attempt to restore the last active blog
            setCurrentBlogToLastActive();

            // fallback to just using the first blog
            List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();
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
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();

        int lastBlogId = WordPress.wpDB.getLastBlogId();
        if (lastBlogId != -1) {
            for (Map<String, Object> account : accounts) {
                int id = Integer.valueOf(account.get("id").toString());
                if (id == lastBlogId) {
                    setCurrentBlog(id);
                }
            }
        }

        return currentBlog;
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
    
    /**
     * Checks for WordPress.com credentials
     * 
     * @return true if we have credentials or false if not
     */
    public static boolean hasValidWPComCredentials(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String username = settings.getString(WPCOM_USERNAME_PREFERENCE, null);
        String password = settings.getString(WPCOM_PASSWORD_PREFERENCE, null);
        
        if (username != null && password != null)
            return true;
        else 
            return false;
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
        private final RequestQueue mQueue = Volley.newRequestQueue(WordPress.this);
        @Override
        public void authenticate(WPRestClient.Request request){
            // set the access token if we have one
            if (!hasValidWPComCredentials(WordPress.this)) {
                request.abort(new VolleyError("Missing WordPress.com Account"));
            } else {
                requestAccessToken(request);
            }
        }
        public void requestAccessToken(final WPRestClient.Request request){
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.this);
            String username = settings.getString(WPCOM_USERNAME_PREFERENCE, null);
            String password = WordPressDB.decryptPassword(settings.getString(WPCOM_PASSWORD_PREFERENCE, null));
            Oauth oauth = new Oauth(Config.OAUTH_APP_ID, Config.OAUTH_APP_SECRET, Config.OAUTH_REDIRECT_URI);
            // make oauth volley request
            Request oauthRequest = oauth.makeRequest(username, password,
                new Oauth.Listener(){
                    @Override
                    public void onResponse(Oauth.Token token){
                        settings.edit().putString(ACCESS_TOKEN_PREFERENCE, token.toString())
                            .commit();
                        request.setAccessToken(token);
                        request.send();
                    }
                },
                new Oauth.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        request.abort(error);
                    }
                }
            );
            mQueue.add(oauthRequest);
            // add oauth request to the request queue
            
        }
    }

    /**
     * Sign out from all accounts by clearing out the password, which will require user to sign in
     * again
     */
    public static void signOut(Context context) {
        new WPComXMLRPCApi().unregisterWPComToken(
                context,
                GCMRegistrar.getRegistrationId(context));
        try {
            GCMRegistrar.checkDevice(context);
            GCMRegistrar.unregister(context);
        } catch (Exception e) {
            Log.v("WORDPRESS", "Could not unregister for GCM: " + e.getMessage());
        }
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        editor.remove(WordPress.WPCOM_USERNAME_PREFERENCE);
        editor.remove(WordPress.WPCOM_PASSWORD_PREFERENCE);
        editor.remove(WordPress.ACCESS_TOKEN_PREFERENCE);
        editor.commit();
        wpDB.deactivateAccounts();
        wpDB.updateLastBlogId(-1);
        currentBlog = null;
        restClient.clearAccessToken();
    }
    
    public static String getLoginUrl(Blog blog) {
        String loginURL = null;
        Gson gson = new Gson();
        Type type = new TypeToken<Map<?, ?>>() {}.getType();
        Map<?, ?> blogOptions = gson.fromJson(blog.getBlogOptions(), type);
        if (blogOptions != null) {
            Map<?, ?> homeURLMap = (Map<?, ?>) blogOptions.get("login_url");
            if (homeURLMap != null)
                loginURL = homeURLMap.get("value").toString();
        }
        // Try to guess the login URL if blogOptions is null (blog not added to the app), or WP version is < 3.6
        if( loginURL == null ) {
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

                    HashMap<String, String> authParams = new HashMap<String, String>();
                    authParams.put("Authorization", "Bearer " + getWPComAuthToken(mContext));
                    
                    headers.putAll(authParams);

                    return super.performRequest(request, headers);
                }
            };

            return stack;

        } else {
            HttpClientStack stack = new HttpClientStack(AndroidHttpClient.newInstance("volley/0")) {
                @Override
                public HttpResponse performRequest(Request<?> request, Map<String, String> headers)
                    throws IOException, AuthFailureError {
                    
                    HashMap<String, String> authParams = new HashMap<String, String>();
                    authParams.put("Authorization", "Bearer " + getWPComAuthToken(mContext));
                    
                    headers.putAll(authParams);

                    return super.performRequest(request, headers);
                }
            };

            return stack;
        }
    }
    
}
