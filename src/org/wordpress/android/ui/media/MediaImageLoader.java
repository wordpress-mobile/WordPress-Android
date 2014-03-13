package org.wordpress.android.ui.media;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.VolleyUtils;

/**
 * provides the ImageLoader and backing RequestQueue for media image requests - necessary because
 * images in protected blogs need to be authenticated, which requires a separate RequestQueue
 */
class MediaImageLoader {
    private static ImageLoader mImageLoader;
    private static RequestQueue mAuthRequestQueue;

    static ImageLoader getInstance() {
        if (mImageLoader == null) {
            Blog blog = WordPress.getCurrentBlog();
            if (blog != null && VolleyUtils.isCustomHTTPClientStackNeeded(blog)) {
                AppLog.d(AppLog.T.MEDIA, "creating custom imageLoader");
                // use ImageLoader with authenticating request queue for protected blogs
                Context context = WordPress.getContext();
                mAuthRequestQueue = Volley.newRequestQueue(context, VolleyUtils.getHTTPClientStack(context, blog));
                mImageLoader = new ImageLoader(mAuthRequestQueue, WordPress.getBitmapCache());
                mImageLoader.setBatchedResponseDelay(0);
            } else {
                // use default ImageLoader for all others
                AppLog.d(AppLog.T.MEDIA, "using default imageLoader");
                mAuthRequestQueue = null;
                mImageLoader = WordPress.imageLoader;
            }
        }

        return mImageLoader;
    }

    /*
     * cancels any pending image requests and releases resources if custom request
     * queue exists - should be called whenever current blog changes or activity
     * which uses this class is closed
     */
    static void reset() {
        if (mAuthRequestQueue != null) {
            AppLog.d(AppLog.T.MEDIA, "resetting custom imageLoader");
            VolleyUtils.cancelAllImageRequests(mAuthRequestQueue);
            mImageLoader = null;
            mAuthRequestQueue = null;
        }
    }

}
