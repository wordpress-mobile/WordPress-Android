package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Started with explicit list of media to upload.
 */

public class MediaUploadService extends Service {
    public static final String SITE_KEY = "mediaSite";
    public static final String MEDIA_LIST_KEY = "mediaList";

    public interface MediaUploadCallback {
        void onUploadBegin(MediaModel media);
        void onUploadSuccess(MediaModel media);
        void onUploadCanceled(MediaModel media);
        void onUploadError(MediaModel media, MediaError error);
        void onUploadProgress(MediaModel media, float progress);
    }

    public class MediaUploadBinder extends Binder {
        public MediaUploadService getService() {
            return MediaUploadService.this;
        }

        public void addMediaToQueue(MediaModel media) {
            MediaUploadService.this.mQueue.add(media);
        }

        public boolean cancelUpload(boolean continueUploading) {
            return MediaUploadService.this.cancelUpload();
        }

        public void setListener(MediaUploadCallback listener) {
            MediaUploadService.this.mListener = listener;
        }
    }

    private final IBinder mBinder = new MediaUploadBinder();
    private final List<MediaModel> mQueue = new ArrayList<>();
    private final List<MediaModel> mCompletedItems = new ArrayList<>();

    private MediaUploadCallback mListener;
    private SiteModel mSite;
    private MediaModel mCurrentUpload;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);
        mCurrentUpload = null;
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        // TODO: if event not dispatched for ongoing upload cancel it and dispatch cancel event
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // stop service if no site is given
        if (intent == null || !intent.hasExtra(SITE_KEY)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        mSite = (SiteModel) intent.getSerializableExtra(SITE_KEY);
        List<MediaModel> mediaList = (List<MediaModel>) intent.getSerializableExtra(MEDIA_LIST_KEY);
        if (mediaList != null) {
            for (MediaModel media : mediaList) {
                mQueue.add(media);
            }
        }

        if (mQueue.isEmpty()) {
            // stop service if there are no media items in the queue
            stopSelf();
        } else {
            startQueuedUpload();
        }

        return START_REDELIVER_INTENT;
    }

    public void startQueuedUpload() {
        if (mQueue.isEmpty()) {
            return;
        }
        performUpload(mQueue.get(0));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(MediaStore.OnMediaUploaded event) {
        long mediaId = event.media.getMediaId();
        MediaModel queueMedia = getQueueMediaWithId(mediaId);

        // ignore if event media is not recognized
        if (queueMedia == null || queueMedia.getMediaId() != mCurrentUpload.getMediaId()) {
            AppLog.w(AppLog.T.MEDIA, "Media event not recognized: " + queueMedia);
            return;
        }

        if (event.isError()) {
            AppLog.w(AppLog.T.MEDIA, "Error uploading media: " + event.error.message);
            queueMedia.setUploadState(MediaModel.UploadState.FAILED.toString());
            completeMediaAndContinue(queueMedia);
            if (mListener != null) {
                mListener.onMediaError(event.error);
            }
        } else if (event.completed) {
            AppLog.i(AppLog.T.MEDIA, event.media.getTitle() + " uploaded!");
            completeMediaAndContinue(queueMedia);
            if (mListener != null) {
                mListener.onMediaUploaded(event.media);
            }
        } else {
        }

        if (mQueue.isEmpty()) {
            AppLog.v(AppLog.T.MEDIA, "All queued media processed.");
        }
    }

    private void performUpload(@NonNull final MediaModel media) {
        // do nothing if there is an upload in progress
        if (mCurrentUpload != null) {
            return;
        }

        // stop service if the site is null (shouldn't happen)
        if (mSite == null || mQueue.isEmpty()) {
            AppLog.v(AppLog.T.MEDIA, mSite == null ? "Site" : "Queue" + " not specified, stopping service.");
            stopSelf();
            return;
        }

        mCurrentUpload = media;

        // dispatch upload action
        MediaPayload payload = new MediaPayload(mSite, mCurrentUpload);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        if (mListener != null) {
            mListener.onUploadBegin(mCurrentUpload);
        }
    }

    private boolean cancelUpload() {
        if (mCurrentUpload == null) {
            return false;
        }

        MediaPayload payload = new MediaPayload(mSite, mCurrentUpload);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
        return true;
    }

    private MediaModel getQueueMediaWithId(long mediaId) {
        for (MediaModel media : mQueue) {
            if (media.getMediaId() == mediaId) {
                return media;
            }
        }

        return null;
    }

    private void completeMediaAndContinue(@NonNull MediaModel media) {
        mCompletedItems.add(media);
        mQueue.remove(media);
        mCurrentUpload = null;
        if (!mQueue.isEmpty()) {
            performUpload(mQueue.get(0));
        }
    }


        }
}
