package org.wordpress.android.ui.uploads;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.media.services.MediaUploadService;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;

import javax.inject.Inject;

public class UploadService extends Service {
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject PostStore mPostStore;
    @Inject SiteStore mSiteStore;

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        AppLog.i(T.MAIN, "Upload Service > created");
        mDispatcher.register(this);
        // TODO: Recover any posts/media uploads that were interrupted by the service being stopped
    }

    @Override
    public void onDestroy() {
        // TODO: Cancel in-progress uploads

        // TODO: Update posts with any completed media uploads

        mDispatcher.unregister(this);
        AppLog.i(T.MAIN, "Upload Service > destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService(new Intent(this, PostUploadService.class));
        return START_REDELIVER_INTENT;
    }

    /**
     * Adds a post to the queue.
     */
    public static void addPostToUpload(PostModel post) {
        PostUploadService.addPostToUpload(post);
    }

    /**
     * Adds a post to the queue and tracks post analytics.
     * To be used only the first time a post is uploaded, i.e. when its status changes from local draft or remote draft
     * to published.
     */
    public static void addPostToUploadAndTrackAnalytics(PostModel post) {
        PostUploadService.addPostToUploadAndTrackAnalytics(post);
    }

    public static void setLegacyMode(boolean enabled) {
        PostUploadService.setLegacyMode(enabled);
    }

    public static void uploadMedia(Context context, ArrayList<MediaModel> mediaList) {
        if (context == null) {
            return;
        }
        MediaUploadService.startService(context, mediaList);
        Intent intent = new Intent(context, UploadService.class);
        context.startService(intent);
    }

    /**
     * Returns true if the passed post is either currently uploading or waiting to be uploaded.
     * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
     * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
     */
    public static boolean isPostUploadingOrQueued(PostModel post) {
        return PostUploadService.isPostUploadingOrQueued(post);
    }

    /**
     * Returns true if the passed post is currently uploading.
     * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
     * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
     */
    public static boolean isPostUploading(PostModel post) {
        return PostUploadService.isPostUploading(post);
    }

    public static void cancelQueuedPostUpload(PostModel post) {
        PostUploadService.cancelQueuedPostUpload(post);
    }

    public static synchronized PostModel updatePostWithCurrentlyCompletedUploads(PostModel post) {
        return MediaUploadService.updatePostWithCurrentlyCompletedUploads(post);
    }

    public static boolean hasPendingMediaUploadsForPost(PostModel postModel) {
        return MediaUploadService.hasPendingMediaUploadsForPost(postModel);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
    }
}
