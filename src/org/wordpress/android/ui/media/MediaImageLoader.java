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
class MediaImageLoader extends ImageLoader {
    static ImageLoader getInstance() {
        return getInstance(WordPress.getCurrentBlog());
    }

    private static ImageLoader getInstance(Blog blog) {
        if (blog != null && VolleyUtils.isCustomHTTPClientStackNeeded(blog)) {
            // use ImageLoader with authenticating request queue for protected blogs
            AppLog.d(AppLog.T.MEDIA, "using custom imageLoader");
            Context context = WordPress.getContext();
            RequestQueue authRequestQueue = Volley.newRequestQueue(context, VolleyUtils.getHTTPClientStack(context, blog));
            MediaImageLoader imageLoader = new MediaImageLoader(authRequestQueue, WordPress.getBitmapCache());
            imageLoader.setBatchedResponseDelay(0);
            return imageLoader;
        } else {
            // use default ImageLoader for all others
            AppLog.d(AppLog.T.MEDIA, "using default imageLoader");
            return WordPress.imageLoader;
        }
    }

    private MediaImageLoader(RequestQueue queue, ImageCache imageCache) {
        super(queue, imageCache);
    }
}
