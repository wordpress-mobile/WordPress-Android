package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.UploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Started with explicit list of media to upload.
 */

public class MediaUploadService extends Service {
    public static final String SITE_KEY = "mediaSite";
    public static final String LISTENER_KEY = "mediaUploadListener";
    public static final String MEDIA_LIST_KEY = "mediaList";

    public interface MediaUploadListener extends Serializable {
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
            addUniqueMediaToQueue(media);
            uploadNextInQueue();
        }

        public void cancelUpload(boolean continueUploading) {
            MediaUploadService.this.cancelUpload();
            if (continueUploading) {
                uploadNextInQueue();
            }
        }

        public List<MediaModel> getCompletedItems() {
            return MediaUploadService.this.getCompletedItems();
        }

        public void setListener(MediaUploadListener listener) {
            MediaUploadService.this.mListener = listener;
        }
    }

    private final IBinder mBinder = new MediaUploadBinder();

    private MediaUploadListener mListener;
    private SiteModel mSite;
    private MediaModel mCurrentUpload;

    private List<MediaModel> mQueue;
    private List<MediaModel> mCompletedItems;

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
        if (mCurrentUpload != null) {
            cancelUpload();
        }
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null) {
            unpackIntent(intent);
            uploadNextInQueue();
        }
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // stop service if no site is given
        if (intent == null || !intent.hasExtra(SITE_KEY)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        unpackIntent(intent);
        uploadNextInQueue();

        return START_REDELIVER_INTENT;
    }


    @NonNull
    public List<MediaModel> getUploadQueue() {
        if (mQueue == null) {
            mQueue = new ArrayList<>();
        }
        return mQueue;
    }

    @NonNull
    public List<MediaModel> getCompletedItems() {
        if (mCompletedItems == null) {
            mCompletedItems = new ArrayList<>();
        }
        return mCompletedItems;
    }

    private void handleOnMediaUploadedSuccess(@NonNull OnMediaUploaded event) {
        if (event.canceled) {
            // Upload canceled
            AppLog.i(T.MEDIA, "Upload successfully canceled.");
            if (mListener != null) {
                mListener.onUploadCanceled(event.media);
            }
            completeCurrentUpload();
            uploadNextInQueue();
        } else if (event.completed) {
            // Upload completed
            AppLog.i(T.MEDIA, "Upload completed - localId=" + event.media.getId() + " title=" + event.media.getTitle());
            if (mListener != null) {
                mListener.onUploadSuccess(event.media);
            }
            mCurrentUpload.setMediaId(event.media.getMediaId());
            completeCurrentUpload();
            uploadNextInQueue();
        } else {
            // Upload Progress
            if (mListener != null) {
                mListener.onUploadProgress(event.media, event.progress);
            }
        }
    }

    private void handleOnMediaUploadedError(@NonNull OnMediaUploaded event) {
        AppLog.w(T.MEDIA, "Error uploading media: " + event.error.message);
        mCurrentUpload.setUploadState(UploadState.FAILED.name());
        completeCurrentUpload();
        if (mListener != null) {
            mListener.onUploadError(event.media, event.error);
        }
        uploadNextInQueue();
    }

    private void uploadNextInQueue() {
        // waiting for response to current upload request
        if (mCurrentUpload != null) {
            AppLog.i(T.MEDIA, "Ignoring request to uploadNextInQueue, only one media item can be uploaded at a time.");
            return;
        }

        // somehow lost our reference to the site, stop service
        if (mSite == null) {
            AppLog.i(T.MEDIA, "Unexpected state, site is null. Stopping MediaUploadService.");
            stopSelf();
            return;
        }

        mCurrentUpload = getNextMediaToUpload();

        if (mCurrentUpload == null) {
            AppLog.v(T.MEDIA, "No more media items to upload. Stopping MediaUploadService.");
            stopSelf();
            return;
        }

        dispatchUploadAction(mCurrentUpload);
    }

    private void completeCurrentUpload() {
        if (mCurrentUpload != null) {
            getCompletedItems().add(mCurrentUpload);
            getUploadQueue().remove(mCurrentUpload);
            mCurrentUpload = null;
        }
    }

    private MediaModel getNextMediaToUpload() {
        if (!getUploadQueue().isEmpty()) {
            return getUploadQueue().get(0);
        }
        return null;
    }

    private void addUniqueMediaToQueue(MediaModel media) {
        for (MediaModel queuedMedia : getUploadQueue()) {
            if (queuedMedia.getLocalSiteId() == media.getLocalSiteId() &&
                    StringUtils.equals(queuedMedia.getFilePath(), media.getFilePath())) {
                return;
            }
        }

        // no match found in queue
        getUploadQueue().add(media);
    }

    private void unpackIntent(@NonNull Intent intent) {
        mSite = (SiteModel) intent.getSerializableExtra(SITE_KEY);
        mListener = (MediaUploadListener) intent.getSerializableExtra(LISTENER_KEY);

        // add local queued media from store
        List<MediaModel> localMedia = mMediaStore.getLocalSiteMedia(mSite);
        if (localMedia != null && !localMedia.isEmpty()) {
            // uploading is updated to queued, queued media added to the queue, failed media added to completed list
            for (MediaModel mediaItem : localMedia) {
                if (MediaUploadState.UPLOADING.name().equals(mediaItem.getUploadState())) {
                    mediaItem.setUploadState(MediaUploadState.QUEUED.name());
                    mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaItem));
                }

                if (MediaUploadState.QUEUED.name().equals(mediaItem.getUploadState())) {
                    addUniqueMediaToQueue(mediaItem);
                } else if (MediaUploadState.FAILED.name().equals(mediaItem.getUploadState())) {
                    getCompletedItems().add(mediaItem);
                }
            }
        }

        // add new media
        @SuppressWarnings("unchecked")
        List<MediaModel> mediaList = (List<MediaModel>) intent.getSerializableExtra(MEDIA_LIST_KEY);
        if (mediaList != null) {
            for (MediaModel media : mediaList) {
                addUniqueMediaToQueue(media);
            }
        }
    }

    private boolean matchesInProgressMedia(final @NonNull MediaModel media) {
        // TODO
        return mCurrentUpload != null && media.getLocalSiteId() == mCurrentUpload.getLocalSiteId();
    }

    private void cancelUpload() {
        if (mCurrentUpload != null) {
            dispatchCancelAction(mCurrentUpload);
        }
    }

    private void dispatchUploadAction(@NonNull final MediaModel media) {
        AppLog.i(T.MEDIA, "Dispatching upload action for media with id: " + media.getId() +
                " and path: " + media.getFilePath());
        media.setUploadState(UploadState.UPLOADING.name());
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));

        MediaPayload payload = new MediaPayload(mSite, media);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

        if (mListener != null) {
            mListener.onUploadBegin(mCurrentUpload);
        }
    }

    private void dispatchCancelAction(@NonNull final MediaModel media) {
        AppLog.i(T.MEDIA, "Dispatching cancel upload action for media with id: " + media.getId() +
                " and path: " + media.getFilePath());
        MediaPayload payload = new MediaPayload(mSite, mCurrentUpload);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }

    // FluxC events


    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        // event for unknown media, ignoring
        if (event.media == null || !matchesInProgressMedia(event.media)) {
            AppLog.w(T.MEDIA, "Media event not recognized: " + event.media);
            return;
        }

        if (event.isError()) {
            handleOnMediaUploadedError(event);
        } else {
            handleOnMediaUploadedSuccess(event);
        }
    }
}
