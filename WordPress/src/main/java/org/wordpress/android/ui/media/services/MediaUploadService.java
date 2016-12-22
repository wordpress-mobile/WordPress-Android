package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
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

    private static final long PROGRESS_TIMEOUT_MS = 30 * 1000;

    public interface MediaUploadCallback {
        void onUploadBegin(MediaModel media);
        void onUploadQueueProcessed(List<MediaModel> media);
        void onMediaUploaded(MediaModel media);
        void onMediaError(MediaStore.MediaError error);
    }

    public class MediaUploadBinder extends Binder {
        public MediaUploadService getService() {
            return MediaUploadService.this;
        }

        public void addMediaToQueue(MediaModel media) {
            MediaUploadService.this.mQueue.add(media);
        }

        public boolean pauseUpload() {
            return MediaUploadService.this.pauseUpload();
        }

        public boolean cancelUpload(boolean continueUploading) {
            return MediaUploadService.this.cancelUpload();
        }

        public void setListener(MediaUploadCallback listener) {
            MediaUploadService.this.mListener = listener;
        }
    }

    private final Handler mHandler = new Handler();
    private final IBinder mBinder = new MediaUploadBinder();
    private final List<MediaModel> mQueue = new ArrayList<>();
    private final List<MediaModel> mCompletedItems = new ArrayList<>();

    private MediaUploadCallback mListener;
    private SiteModel mSite;
    private MediaModel mCurrentUpload;
    private long mLastProgressUpdate;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
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
        long[] mediaIdList = intent.getLongArrayExtra(MEDIA_LIST_KEY);

        if (mediaIdList != null && mediaIdList.length > 0) {
            populateQueue(mediaIdList);
        }

        if (mQueue.isEmpty()) {
            // stop service if there are no media items in the queue
            stopSelf();
        } else {
            performUpload(mQueue.get(0));
            mHandler.postDelayed(mFailsafeRunnable, PROGRESS_TIMEOUT_MS);
        }

        return START_REDELIVER_INTENT;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(MediaStore.OnMediaUploaded event) {
        long mediaId = event.media.getMediaId();
        MediaModel queueMedia = getQueueMediaWithId(mediaId);

        // ignore if event media is not recognized
        if (queueMedia == null || queueMedia.getMediaId() != mCurrentUpload.getMediaId()) {
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
            mLastProgressUpdate = System.currentTimeMillis();
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
            mHandler.removeCallbacks(mFailsafeRunnable);
            stopSelf();
            return;
        }

        mCurrentUpload = media;

        // dispatch upload action
        MediaStore.UploadMediaPayload payload = new MediaStore.UploadMediaPayload(mSite, mCurrentUpload);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        if (mListener != null) {
            mListener.onUploadBegin(mCurrentUpload);
        }
    }

    private boolean pauseUpload() {
        // TODO: not implemented in FluxC
        return false;
    }

    private boolean cancelUpload() {
        // TODO: not implemented in FluxC
        return false;
    }

    private void populateQueue(@NonNull long[] mediaIdList) {
        // search store for media items
        for (long mediaId : mediaIdList) {
            MediaModel media = mMediaStore.getSiteMediaWithId(mSite, mediaId);
            if (media != null) {
                mQueue.add(media);
            }
        }
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
        } else if(mListener != null) {
            mListener.onUploadQueueProcessed(mCompletedItems);
        }
    }

    private final Runnable mFailsafeRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCurrentUpload != null && System.currentTimeMillis() - mLastProgressUpdate > PROGRESS_TIMEOUT_MS) {
                AppLog.w(AppLog.T.MEDIA, PROGRESS_TIMEOUT_MS + "ms since last server message, starting next upload.");
                completeMediaAndContinue(mCurrentUpload);
            }

            mHandler.postDelayed(mFailsafeRunnable, PROGRESS_TIMEOUT_MS);
        }
    };
}
