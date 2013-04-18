package org.wordpress.android;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import org.xmlrpc.android.WPComXMLRPCApi;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.WPRestClient;

public class WordPress extends Application {

    public static String versionName;
    public static Blog currentBlog;
    public static Comment currentComment;
    public static Post currentPost;
    public static WordPressDB wpDB;
    public static OnPostUploadedListener onPostUploadedListener = null;
    public static boolean postsShouldRefresh;
    public static boolean shouldRestoreSelectedActivity;
    public static WPRestClient restClient;
    public static Properties config;

    public static final String TAG="WordPress";

    @Override
    public void onCreate() {
        loadProperties();
        versionName = getVersionName();
        wpDB = new WordPressDB(this);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);  
        if (settings.getInt("wp_pref_last_activity", -1) >= 0)
            shouldRestoreSelectedActivity = true;

        restClient = new WPRestClient(config, settings);
        
        registerForCloudMessaging();
        
        super.onCreate();
    }
    
    private void registerForCloudMessaging() {
        
        if (WordPress.hasValidWPComCredentials(this)) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this); 
            String notificationId = null;
            // TODO remove try/catch with debug check for emulators
            try {
                // Register for Google Cloud Messaging
                GCMRegistrar.checkDevice(this);
                GCMRegistrar.checkManifest(this);
                notificationId = GCMRegistrar.getRegistrationId(this);
                String gcmId = WordPress.config.getProperty("gcm.id").toString();
                if (gcmId != null && notificationId.equals("")) {
                    GCMRegistrar.register(this, gcmId);
                    notificationId = GCMRegistrar.getRegistrationId(this);
                } else {
                    Log.v("WORDPRESS", "Already registered for GCM");
                }
            } catch (Exception e) {
                Log.v("WORDPRESS", "Could not register for GCM: " + e.getMessage());
            }
            
            if (notificationId != null && notificationId.length() > 0) {
                // Get or create UUID for WP.com notes api
                String uuid = settings.getString("wp_pref_notifications_uuid", null);
                if (uuid == null) {
                    uuid = UUID.randomUUID().toString();
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("wp_pref_notifications_uuid", uuid);
                    editor.commit();
                }

                Object[] params = {
                        settings.getString("wp_pref_wpcom_username", ""),
                        WordPressDB.decryptPassword(settings.getString("wp_pref_wpcom_password", "")),
                        notificationId,
                        uuid,
                        "android",
                        false
                };

                XMLRPCClient client = new XMLRPCClient(URI.create(Constants.wpcomXMLRPCURL), "", "");
                client.callAsync(new XMLRPCCallback() {
                    public void onSuccess(long id, Object result) {
                        Log.v("WORDPRESS", "Succesfully registered device on WP.com");
                    }

                    public void onFailure(long id, XMLRPCException error) {
                        Log.v("WORDPRESS", error.getMessage());
                    }
                }, "wpcom.mobile_push_register_token", params);
                
                new WPComXMLRPCApi().getNotificationSettings(null, getApplicationContext());
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
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();
        for (Map<String, Object> account : accounts) {
            int accountId = (Integer) account.get("id");
            if (accountId == id) {
                try {
                    return new Blog(id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
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
     * Load res/raw/config.properties into a Properties object
     */
    private void loadProperties(){
        config = new Properties();
        InputStream stream = getResources().openRawResource(R.raw.config);
        try {
            config.load(stream);               
        } catch(java.io.IOException error){
            config = null;
            Log.e(TAG, "Could not load config.properties", error);
        }
    }
    
    /**
     * Checks for WordPress.com credentials
     * 
     * @return true if we have credentials or false if not
     */
    public static boolean hasValidWPComCredentials(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String username = settings.getString("wp_pref_wpcom_username", null);
        String password = settings.getString("wp_pref_wpcom_password", null);
        
        if (username != null && password != null)
            return true;
        else 
            return false;
    }
}
