package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.UploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Started with explicit list of media to upload.
 */

public class MediaUploadService extends Service {
    private static final String MEDIA_LIST_KEY = "mediaList";

    private SiteModel mSite;
    private MediaModel mCurrentUpload;

    private List<MediaModel> mUploadQueue = new ArrayList<>();
    private SparseArray<Long> mUploadQueueTime = new SparseArray<>();

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    public static void startService(Context context, SiteModel siteModel, ArrayList<MediaModel> mediaList) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, MediaUploadService.class);
        intent.putExtra(WordPress.SITE, siteModel);
        intent.putExtra(MediaUploadService.MEDIA_LIST_KEY, mediaList);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        AppLog.i(AppLog.T.MEDIA, "Media Upload Service > created");
        mDispatcher.register(this);
        EventBus.getDefault().register(this);
        mCurrentUpload = null;
    }

    @Override
    public void onDestroy() {
        cancelCurrentUpload();
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
        AppLog.i(AppLog.T.MEDIA, "Media Upload Service > destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.hasExtra(WordPress.SITE)) {
            AppLog.e(AppLog.T.MEDIA, "MediaUploadService was killed and restarted with a null intent.");
            stopServiceIfUploadsComplete();
            return START_NOT_STICKY;
        }

        unpackIntent(intent);
        uploadNextInQueue();

        return START_REDELIVER_INTENT;
    }

    private void handleOnMediaUploadedSuccess(@NonNull OnMediaUploaded event) {
        if (event.canceled) {
            // Upload canceled
            AppLog.i(AppLog.T.MEDIA, "Upload successfully canceled.");
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_CANCELED, mCurrentUpload, null);
            completeCurrentUpload();
            uploadNextInQueue();
        } else if (event.completed) {
            // Upload completed
            AppLog.i(AppLog.T.MEDIA, "Upload completed - localId=" + event.media.getId() + " title=" + event.media.getTitle());
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_SUCCESS, mCurrentUpload, null);
            mCurrentUpload.setMediaId(event.media.getMediaId());
            completeCurrentUpload();
            uploadNextInQueue();
        } else {
            // Upload Progress
            // TODO check if we need to broadcast event.media, event.progress or we're just fine with
            // listening to  event.media, event.progress
        }
    }

    private void handleOnMediaUploadedError(@NonNull OnMediaUploaded event) {
        AppLog.w(AppLog.T.MEDIA, "Error uploading media: " + event.error.message);
        // TODO: Don't update the state here, it needs to be done in FluxC
        mCurrentUpload.setUploadState(UploadState.FAILED.name());
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mCurrentUpload));
        // TODO: check whether we need to broadcast the error or maybe it is enough to register for FluxC events
        // event.media, event.error
        Map<String, Object> properties = new HashMap<>();
        properties.put("error_type", event.error.type.name());
        trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_ERROR, mCurrentUpload, properties);
        completeCurrentUpload();
        uploadNextInQueue();
    }

    private void uploadNextInQueue() {
        // waiting for response to current upload request
        if (mCurrentUpload != null) {
            AppLog.i(AppLog.T.MEDIA, "Ignoring request to uploadNextInQueue, only one media item can be uploaded at a time.");
            return;
        }

        // somehow lost our reference to the site, complete this action
        if (mSite == null) {
            AppLog.i(AppLog.T.MEDIA, "Unexpected state, site is null. Skipping this request - MediaUploadService.");
            stopServiceIfUploadsComplete();
            return;
        }

        mCurrentUpload = getNextMediaToUpload();

        if (mCurrentUpload == null) {
            AppLog.v(AppLog.T.MEDIA, "No more media items to upload. Skipping this request - MediaUploadService.");
            stopServiceIfUploadsComplete();
            return;
        }

        dispatchUploadAction(mCurrentUpload);
        trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_STARTED, mCurrentUpload, null);
    }

    private void completeCurrentUpload() {
        if (mCurrentUpload != null) {
            mUploadQueue.remove(mCurrentUpload);
            mCurrentUpload = null;
        }
    }

    private MediaModel getNextMediaToUpload() {
        if (!mUploadQueue.isEmpty()) {
            return mUploadQueue.get(0);
        }
        return null;
    }

    private void addUniqueMediaToQueue(MediaModel media) {
        for (MediaModel queuedMedia : mUploadQueue) {
            if (queuedMedia.getLocalSiteId() == media.getLocalSiteId() &&
                    StringUtils.equals(queuedMedia.getFilePath(), media.getFilePath())) {
                return;
            }
        }

        // no match found in queue
        mUploadQueue.add(media);
    }

    private void unpackIntent(@NonNull Intent intent) {
        mSite = (SiteModel) intent.getSerializableExtra(WordPress.SITE);

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
        return mCurrentUpload != null && media.getLocalSiteId() == mCurrentUpload.getLocalSiteId();
    }

    private void cancelCurrentUpload() {
        if (mCurrentUpload != null) {
            dispatchCancelAction(mCurrentUpload);
            mCurrentUpload = null;
        }
    }

    private void cancelAllUploads() {
        mUploadQueue.clear();
        mUploadQueueTime.clear();
        cancelCurrentUpload();
    }

    private void cancelUpload(int localMediaId) {
        // Cancel if it's currently uploading
        if (mCurrentUpload != null && mCurrentUpload.getId() == localMediaId) {
            cancelCurrentUpload();
        }
        // Remove from the queue
        for(Iterator<MediaModel> i = mUploadQueue.iterator(); i.hasNext();) {
            MediaModel mediaModel = i.next();
            if (mediaModel.getId() == localMediaId) {
                i.remove();
            }
        }
    }

    private void dispatchUploadAction(@NonNull final MediaModel media) {
        AppLog.i(AppLog.T.MEDIA, "Dispatching upload action for media with local id: " + media.getId() +
                " and path: " + media.getFilePath());
        media.setUploadState(UploadState.UPLOADING.name());
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));

        MediaPayload payload = new MediaPayload(mSite, media);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void dispatchCancelAction(@NonNull final MediaModel media) {
        AppLog.i(AppLog.T.MEDIA, "Dispatching cancel upload action for media with local id: " + media.getId() +
                " and path: " + media.getFilePath());
        MediaPayload payload = new MediaPayload(mSite, mCurrentUpload);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }

    private void stopServiceIfUploadsComplete(){
        AppLog.i(AppLog.T.MEDIA, "Media Upload Service > completed");
        if (mUploadQueue.size() == 0) {
            AppLog.i(AppLog.T.MEDIA, "No more items pending in queue. Stopping MediaUploadService.");
            stopSelf();
        }
    }

    // App events

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostMediaCanceled event) {
        if (event.all) {
            cancelAllUploads();
            return;
        }
        cancelUpload(event.localMediaId);
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        // event for unknown media, ignoring
        if (event.media == null || !matchesInProgressMedia(event.media)) {
            AppLog.w(AppLog.T.MEDIA, "Media event not recognized: " + event.media);
            return;
        }

        if (event.isError()) {
            handleOnMediaUploadedError(event);
        } else {
            handleOnMediaUploadedSuccess(event);
        }
    }

    /**
     * Analytics about media being uploaded
     *
     * @param media The media being uploaded
     */
    private void trackUploadMediaEvents(AnalyticsTracker.Stat stat, MediaModel media, Map<String, Object> properties) {
        if (media == null) {
            AppLog.e(AppLog.T.MEDIA, "Cannot track media upload service events if the original media is null!!");
            return;
        }

        Map<String, Object> mediaProperties = AnalyticsUtils.getMediaProperties(this, media.isVideo(), null, media.getFilePath());
        if (properties != null) {
            mediaProperties.putAll(properties);
        }

        long currentTime = System.currentTimeMillis();
        if (stat == AnalyticsTracker.Stat.MEDIA_UPLOAD_STARTED) {
            mUploadQueueTime.put(media.getId(), currentTime);
        } else if(stat == AnalyticsTracker.Stat.MEDIA_UPLOAD_SUCCESS || stat == AnalyticsTracker.Stat.MEDIA_UPLOAD_ERROR) {
            mediaProperties.put("upload_time_ms", currentTime - mUploadQueueTime.get(media.getId(), currentTime));
            mUploadQueueTime.remove(media.getId());
        }

        AnalyticsTracker.track(stat, mediaProperties);
    }
}
