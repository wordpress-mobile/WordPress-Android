package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.Post;

public class WordPress extends Application {

    private static final String TAG = "WordPress";

    public static String versionName;
    public static Blog currentBlog;
    public static Comment currentComment;
    public static Post currentPost;
    public static WordPressDB wpDB;
    public static OnPostUploadedListener onPostUploadedListener = null;
    public static boolean postsShouldRefresh;

    @Override
    public void onCreate() {
        versionName = getVersionName();
        wpDB = new WordPressDB(this);
        super.onCreate();
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
    public static Blog getCurrentBlog(Context context) {
        if (currentBlog == null) {
            Vector<HashMap<String, Object>> accounts = WordPress.wpDB.getAccounts(context);

            // attempt to restore the last active blog
            int lastBlogId = WordPress.wpDB.getLastBlogId();
            if (lastBlogId != -1) {
                try {
                    for (HashMap<String, Object> account : accounts) {
                        int id = Integer.valueOf(account.get("id").toString());
                        if (id == lastBlogId) {
                            currentBlog = new Blog(id, context);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // fallback to just using the first blog
            if (currentBlog == null && accounts.size() > 0) {
                try {
                    int id = Integer.valueOf(accounts.get(0).get("id").toString());
                    WordPress.currentBlog = new Blog(id, context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return currentBlog;
    }

    /**
     * Get the blog with the specified ID.
     *
     * @param context application context.
     * @param id ID of the blog to retrieve.
     * @return the blog with the specified ID, or null if blog could not be retrieved.
     */
    public static Blog getBlog(Context context, int id) {
        Vector<HashMap<String, Object>> accounts = WordPress.wpDB.getAccounts(context);
        for (HashMap<String, Object> account : accounts) {
            int accountId = (Integer) account.get("id");
            if (accountId == id) {
                try {
                    return new Blog(id, context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
