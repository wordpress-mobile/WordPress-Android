package org.wordpress.android.ui.posts.services;

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
import org.xmlrpc.android.ApiHelper.Method;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.greenrobot.event.EventBus;

/**
 * service which retrieves media info for a list of media IDs in a specific blog - currently used
 * only for featured images in the post list but could be used for any blog-specific media
 */

public class PostMediaService extends Service {

    private static final String ARG_BLOG_ID = "blog_id";
    private static final String ARG_MEDIA_IDS = "media_ids";

    private final ConcurrentLinkedQueue<Long> mMediaIdQueue = new ConcurrentLinkedQueue<>();
    private Blog mBlog;

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
        if (intent == null) return START_NOT_STICKY;

        int blogId = intent.getIntExtra(ARG_BLOG_ID, 0);
        mBlog = WordPress.getBlog(blogId);

        Serializable serializable = intent.getSerializableExtra(ARG_MEDIA_IDS);
        if (serializable != null && serializable instanceof List) {
            List list = (List) serializable;
            for (Object id : list) {
                if (id instanceof Long) {
                    mMediaIdQueue.add((Long) id);
                }
            }
        }

        if (mMediaIdQueue.size() > 0 && mBlog != null) {
            new Thread() {
                @Override
                public void run() {
                    while (!mMediaIdQueue.isEmpty()) {
                        long mediaId = mMediaIdQueue.poll();
                        downloadMediaItem(mediaId);
                    }
                }
            }.start();
        }

        return START_NOT_STICKY;
    }

    private void downloadMediaItem(long mediaId) {
        Object[] apiParams = {
                mBlog.getRemoteBlogId(),
                mBlog.getUsername(),
                mBlog.getPassword(),
                mediaId};

        XMLRPCClientInterface client = XMLRPCFactory.instantiate(
                mBlog.getUri(),
                mBlog.getHttpuser(),
                mBlog.getHttppassword());

        try {
            Map<?, ?> results = (Map<?, ?>) client.call(Method.GET_MEDIA_ITEM, apiParams);
            if (results != null) {
                String strBlogId = Integer.toString(mBlog.getLocalTableBlogId());
                MediaFile mediaFile = new MediaFile(strBlogId, results, mBlog.isDotcomFlag());
                WordPress.wpDB.saveMediaFile(mediaFile);
                AppLog.d(AppLog.T.POSTS, "PostMediaService > downloaded " + mediaFile.getFileURL());
                EventBus.getDefault().post(new PostEvents.PostMediaInfoUpdated(mediaId, mediaFile.getFileURL()));
            }
        } catch (ClassCastException | XMLRPCException | XmlPullParserException | IOException e) {
            AppLog.e(AppLog.T.POSTS, e);
        }
    }
}
