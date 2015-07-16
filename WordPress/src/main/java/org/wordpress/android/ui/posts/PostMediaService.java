package org.wordpress.android.ui.posts;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.helpers.MediaFile;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * service which downloads featured images in posts that don't already exist in local media library
 */

public class PostMediaService extends Service {

    private static final String ARG_BLOG_ID = "blog_id";
    private static final String ARG_MEDIA_IDS = "media_ids";

    private int mBlogId;
    private final ArrayList<Long> mMediaIds = new ArrayList<>();

    public static void startService(Context context, int blogId, ArrayList<Long> mediaIds) {
        if (context == null || mediaIds == null || mediaIds.size() == 0) {
            return;
        }

        Intent intent = new Intent(context, PostMediaService.class);
        intent.putExtra(ARG_BLOG_ID, blogId);
        intent.putExtra(ARG_MEDIA_IDS, mediaIds);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.POSTS, "PostMediaService > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.POSTS, "PostMediaService > destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        mBlogId = intent.getIntExtra(ARG_BLOG_ID, 0);
        Serializable serializable = intent.getSerializableExtra(ARG_MEDIA_IDS);
        if (serializable != null && serializable instanceof List) {
            List ids = (List) serializable;
            mMediaIds.addAll(ids);
        }

        startDownloading();

        return START_NOT_STICKY;
    }

    private void startDownloading() {
        final Blog blog = WordPress.getBlog(mBlogId);
        if (blog == null) {
            AppLog.w(AppLog.T.POSTS, "PostMediaService > null blog");
            stopSelf();
            return;
        }

        if (mMediaIds == null || mMediaIds.size() == 0) {
            AppLog.w(AppLog.T.POSTS, "PostMediaService > nothing to download");
            stopSelf();
            return;
        }

        final XMLRPCClientInterface client = XMLRPCFactory.instantiate(
                blog.getUri(),
                blog.getHttpuser(),
                blog.getHttppassword());

        new Thread() {
            @Override
            public void run() {
                String strBlogId = Integer.toString(blog.getLocalTableBlogId());
                while (mMediaIds.size() > 0) {
                    long thisMediaId = mMediaIds.get(0);
                    mMediaIds.remove(0);

                    Object[] apiParams = {
                            blog.getRemoteBlogId(),
                            blog.getUsername(),
                            blog.getPassword(),
                            thisMediaId};

                    try {
                        Map<?, ?> results = (Map<?, ?>) client.call("wp.getMediaItem", apiParams);
                        if (results != null) {
                            MediaFile mediaFile = new MediaFile(strBlogId, results, blog.isDotcomFlag());
                            WordPress.wpDB.saveMediaFile(mediaFile);
                            AppLog.d(AppLog.T.POSTS, "PostMediaService > downloaded " + mediaFile.getFileURL());
                        }
                    } catch (ClassCastException | XMLRPCException | XmlPullParserException | IOException e) {
                        AppLog.e(AppLog.T.POSTS, e);
                    }
                }

                stopSelf();
            }
        }.start();
    }
}
