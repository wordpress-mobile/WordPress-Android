package org.wordpress.android.ui.uploads;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Started with explicit list of media to upload.
 */
public class MediaUploadManager extends AbstractUploadManager {
    private static final List<MediaModel> sPendingUploads = new ArrayList<>();
    private static final List<MediaModel> sInProgressUploads = new ArrayList<>();

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;

    MediaUploadManager() {
        ((WordPress) WordPress.getContext()).component().inject(this);
        AppLog.i(T.MEDIA, "MediaUploadManager > Created");
        mDispatcher.register(this);
        EventBus.getDefault().register(this);
    }

    @Override
    void unregister() {
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
    }

    @Override
    boolean hasInProgressUploads() {
        return !sInProgressUploads.isEmpty() || !sPendingUploads.isEmpty();
    }

    @Override
    void cancelInProgressUploads() {
        for (MediaModel oneUpload : sInProgressUploads) {
            cancelUpload(oneUpload, false);
        }
    }

    void uploadMedia(List<MediaModel> mediaList) {
        if (mediaList != null) {
            for (MediaModel media : mediaList) {
                addUniqueMediaToQueue(media);
            }
        }
        uploadNextInQueue();
    }

    static boolean hasPendingMediaUploadsForPost(PostModel postModel) {
        for (MediaModel queuedMedia : sInProgressUploads) {
            if (queuedMedia.getLocalPostId() == postModel.getId()) {
                return true;
            }
        }

        for (MediaModel queuedMedia : sPendingUploads) {
            if (queuedMedia.getLocalPostId() == postModel.getId()) {
                return true;
            }
        }
        return false;
    }

    private void handleOnMediaUploadedSuccess(@NonNull OnMediaUploaded event) {
        if (event.canceled) {
            AppLog.i(T.MEDIA, "MediaUploadManager > Upload successfully canceled");
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_CANCELED,
                    getMediaFromInProgressQueueById(event.media.getId()), null);
            completeUploadWithId(event.media.getId());
            uploadNextInQueue();
        } else if (event.completed) {
            AppLog.i(T.MEDIA, "MediaUploadManager > Upload completed - localId=" + event.media.getId() + " title="
                    + event.media.getTitle());
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_SUCCESS,
                    getMediaFromInProgressQueueById(event.media.getId()), null);
            completeUploadWithId(event.media.getId());
            uploadNextInQueue();
        } else {
            // TODO check if we need to re-broadcast event.media, event.progress or we're just fine with
            // listening to  event.media, event.progress
            AppLog.i(T.MEDIA, "MediaUploadManager > " + event.media.getId() + " - progress: " + event.progress);
        }
    }

    private void handleOnMediaUploadedError(@NonNull OnMediaUploaded event) {
        AppLog.w(T.MEDIA, "MediaUploadManager > Error uploading media: " + event.error.message);
        MediaModel media = getMediaFromInProgressQueueById(event.media.getId());
        if (media != null) {
            mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
        }

        // TODO: check whether we need to broadcast the error or maybe it is enough to register for FluxC events
        // event.media, event.error
        Map<String, Object> properties = new HashMap<>();
        properties.put("error_type", event.error.type.name());
        trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_ERROR, media, properties);

        completeUploadWithId(event.media.getId());
        uploadNextInQueue();
    }

    private synchronized void uploadNextInQueue() {
        MediaModel next = getNextMediaToUpload();

        if (next == null) {
            AppLog.w(T.MEDIA, "MediaUploadManager > No more media items to upload. Skipping this request.");
            notifyServiceIfUploadsComplete();
            return;
        }

        SiteModel site = mSiteStore.getSiteByLocalId(next.getLocalSiteId());

        // somehow lost our reference to the site, complete this action
        if (site == null) {
            AppLog.w(T.MEDIA, "MediaUploadManager > Unexpected state, site is null. Skipping this request.");
            notifyServiceIfUploadsComplete();
            return;
        }

        dispatchUploadAction(next, site);
    }

    private synchronized void completeUploadWithId(int id) {
        MediaModel media = getMediaFromInProgressQueueById(id);
        if (media != null) {
            sInProgressUploads.remove(media);
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_STARTED, media, null);
        }
    }

    private MediaModel getMediaFromInProgressQueueById(int id) {
        for (MediaModel media : sInProgressUploads) {
            if (media.getId() == id)
                return media;
        }
        return null;
    }

    private MediaModel getNextMediaToUpload() {
        if (!sPendingUploads.isEmpty()) {
            return sPendingUploads.remove(0);
        }
        return null;
    }

    private void addUniqueMediaToQueue(MediaModel media) {
        if (media != null) {
            if (mediaAlreadyQueuedOrUploading(media)) {
                return;
            }

            // no match found in queue
            sPendingUploads.add(media);
        }
    }

    private void cancelUpload(MediaModel oneUpload, boolean delete) {
        if (oneUpload != null) {
            SiteModel site = mSiteStore.getSiteByLocalId(oneUpload.getLocalSiteId());
            if (site != null) {
                dispatchCancelAction(oneUpload, site, delete);
            } else {
                AppLog.w(T.MEDIA, "MediaUploadManager > Unexpected state, site is null. "
                        + "Skipping cancellation of this request.");
            }
        }
    }

    private void dispatchUploadAction(@NonNull final MediaModel media, @NonNull final SiteModel site) {
        AppLog.i(T.MEDIA, "MediaUploadManager > Dispatching upload action for media with local id: "
                + media.getId() + " and path: " + media.getFilePath());
        sInProgressUploads.add(media);
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));

        MediaPayload payload = new MediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void dispatchCancelAction(@NonNull final MediaModel media, @NonNull final SiteModel site, boolean delete) {
        AppLog.i(T.MEDIA, "MediaUploadManager > Dispatching cancel upload action for media with local id: "
                + media.getId() + " and path: " + media.getFilePath());
        CancelMediaPayload payload = new CancelMediaPayload(site, media, delete);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }

    private void notifyServiceIfUploadsComplete() {
        if (sPendingUploads.isEmpty() && sInProgressUploads.isEmpty()) {
            AppLog.i(T.MEDIA, "MediaUploadManager > Completed");
            // TODO: Tell UploadService it can stop
        }
    }

    // App events

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostMediaCanceled event) {
        if (event.post == null) {
            return;
        }
        for (MediaModel inProgressUpload : sInProgressUploads) {
            if (inProgressUpload.getLocalPostId() == event.post.getId()) {
                cancelUpload(inProgressUpload, true);
            }
        }
        for (MediaModel pendingUpload : sPendingUploads) {
            if (pendingUpload.getLocalPostId() == event.post.getId()) {
                cancelUpload(pendingUpload, true);
            }
        }
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 9)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.media == null) {
            AppLog.w(T.MEDIA, "MediaUploadManager > Received media event for null media, ignoring");
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
            AppLog.e(T.MEDIA, "MediaUploadManager > Cannot track media upload manager events if the original media"
                    + "is null");
            return;
        }
        Map<String, Object> mediaProperties = AnalyticsUtils.getMediaProperties(WordPress.getContext(),
                media.isVideo(), null, media.getFilePath());
        if (properties != null) {
            mediaProperties.putAll(properties);
        }
        AnalyticsTracker.track(stat, mediaProperties);
    }

    private boolean mediaAlreadyQueuedOrUploading(MediaModel mediaModel) {
        for (MediaModel queuedMedia : sInProgressUploads) {
            AppLog.i(T.MEDIA, "MediaUploadManager > Attempting to add media with path " + mediaModel.getFilePath()
                    + " and site id " + mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia.getFilePath()
                    + ", " + queuedMedia.getLocalSiteId());
            if (compareBySiteAndFilePath(queuedMedia, mediaModel)) {
                return true;
            }
        }

        for (MediaModel queuedMedia : sPendingUploads) {
            AppLog.i(T.MEDIA, "MediaUploadManager > Attempting to add media with path " + mediaModel.getFilePath()
                    + " and site id " + mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia.getFilePath()
                    + ", " + queuedMedia.getLocalSiteId());
            if (compareBySiteAndFilePath(queuedMedia, mediaModel)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareBySiteAndFilePath(MediaModel media1, MediaModel media2) {
        return (media1.getLocalSiteId() == media2.getLocalSiteId() &&
                StringUtils.equals(media1.getFilePath(), media2.getFilePath()));
    }
}
