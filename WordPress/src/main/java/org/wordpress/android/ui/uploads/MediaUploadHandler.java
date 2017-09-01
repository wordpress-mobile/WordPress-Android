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
import org.wordpress.android.util.WPMediaUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MediaUploadHandler implements UploadHandler<MediaModel>, VideoOptimizer.VideoOptimizationListener {
    private static final List<MediaModel> sPendingUploads = new ArrayList<>();
    private static final List<MediaModel> sInProgressUploads = new ArrayList<>();

    private static final ConcurrentHashMap<Integer, Float> sOptimizationProgressByMediaId = new ConcurrentHashMap<>();

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;

    MediaUploadHandler() {
        ((WordPress) WordPress.getContext()).component().inject(this);
        AppLog.i(T.MEDIA, "MediaUploadHandler > Created");
        mDispatcher.register(this);
        EventBus.getDefault().register(this);
    }

    void unregister() {
        sOptimizationProgressByMediaId.clear();
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean hasInProgressUploads() {
        return !sInProgressUploads.isEmpty() || !sPendingUploads.isEmpty();
    }

    @Override
    public void cancelInProgressUploads() {
        for (MediaModel oneUpload : sInProgressUploads) {
            cancelUpload(oneUpload, false);
        }
    }

    @Override
    public void upload(@NonNull MediaModel media) {
        addUniqueMediaToQueue(media);
        uploadNextInQueue();
    }

    static boolean hasInProgressMediaUploadsForPost(PostModel postModel) {
        if (postModel == null) {
            return false;
        }

        synchronized (sInProgressUploads) {
            for (MediaModel queuedMedia : sInProgressUploads) {
                if (queuedMedia.getLocalPostId() == postModel.getId()) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean hasPendingMediaUploadsForPost(PostModel postModel) {
        if (postModel == null) {
            return false;
        }

        synchronized (sPendingUploads) {
            for (MediaModel queuedMedia : sPendingUploads) {
                if (queuedMedia.getLocalPostId() == postModel.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasPendingOrInProgressMediaUploadsForPost(PostModel postModel) {
        if (postModel == null) {
            return false;
        }

        // Check if there are media in the in-progress or the pending queue attached to the given post
        return hasInProgressMediaUploadsForPost(postModel) || hasPendingMediaUploadsForPost(postModel);
    }

    public static List<MediaModel> getPendingOrInProgressMediaUploadsForPost(PostModel postModel) {
        if (postModel == null) {
            return Collections.emptyList();
        }

        List<MediaModel> mediaList = new ArrayList<>();
        synchronized (sInProgressUploads) {
            for (MediaModel queuedMedia : sInProgressUploads) {
                if (queuedMedia.getLocalPostId() == postModel.getId()) {
                    mediaList.add(queuedMedia);
                }
            }
        }

        synchronized (sPendingUploads) {
            for (MediaModel queuedMedia : sPendingUploads) {
                if (queuedMedia.getLocalPostId() == postModel.getId()) {
                    mediaList.add(queuedMedia);
                }
            }
        }
        return mediaList;
    }

    static boolean isPendingOrInProgressMediaUpload(@NonNull MediaModel media) {
        synchronized (sInProgressUploads) {
            for (MediaModel uploadingMedia : sInProgressUploads) {
                if (uploadingMedia.getId() == media.getId()) {
                    return true;
                }
            }
        }

        synchronized (sPendingUploads) {
            for (MediaModel queuedMedia : sPendingUploads) {
                if (queuedMedia.getId() == media.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns an overall progress for the given {@param video}, including the video optimization progress. If there is
     * no record for that video, it's assumed to be a completed upload.
     */
    static float getOverallProgressForVideo(MediaModel video, float uploadProgress) {
        if (sOptimizationProgressByMediaId.containsKey(video.getId())) {
            float optimizationProgress = sOptimizationProgressByMediaId.get(video.getId());
            return optimizationProgress * 0.5F;
        }
        return 0.5F + (uploadProgress * 0.5F);
    }

    private void handleOnMediaUploadedSuccess(@NonNull OnMediaUploaded event) {
        if (event.canceled) {
            AppLog.i(T.MEDIA, "MediaUploadHandler > Upload successfully canceled");
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_CANCELED,
                    getMediaFromInProgressQueueById(event.media.getId()), null);
            completeUploadWithId(event.media.getId());
            uploadNextInQueue();
        } else if (event.completed) {
            AppLog.i(T.MEDIA, "MediaUploadHandler > Upload completed - localId=" + event.media.getId() + " title="
                    + event.media.getTitle());
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_SUCCESS,
                    getMediaFromInProgressQueueById(event.media.getId()), null);
            completeUploadWithId(event.media.getId());
            uploadNextInQueue();
        } else {
            AppLog.i(T.MEDIA, "MediaUploadHandler > " + event.media.getId() + " - progress: " + event.progress);
        }
    }

    private void handleOnMediaUploadedError(@NonNull OnMediaUploaded event) {
        AppLog.w(T.MEDIA, "MediaUploadHandler > Error uploading media: " + event.error.message);
        MediaModel media = getMediaFromInProgressQueueById(event.media.getId());
        if (media != null) {
            mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("error_type", event.error.type.name());
        trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_ERROR, media, properties);

        completeUploadWithId(event.media.getId());
        uploadNextInQueue();
    }

    private synchronized void uploadNextInQueue() {
        MediaModel next = getNextMediaToUpload();

        if (next == null) {
            AppLog.w(T.MEDIA, "MediaUploadHandler > No more media items to upload. Skipping this request.");
            checkIfUploadsComplete();
            return;
        }

        prepareForUpload(next);
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

    private void addUniqueMediaToInProgressUploads(@NonNull MediaModel mediaToAdd) {
        synchronized (sInProgressUploads) {
            for (MediaModel media : sInProgressUploads) {
                if (media.getId() == mediaToAdd.getId()) {
                    return;
                }
            }
            sInProgressUploads.add(mediaToAdd);
        }
    }

    private void cancelUpload(MediaModel oneUpload, boolean delete) {
        if (oneUpload != null) {
            SiteModel site = mSiteStore.getSiteByLocalId(oneUpload.getLocalSiteId());
            if (site != null) {
                dispatchCancelAction(oneUpload, site, delete);
            } else {
                AppLog.w(T.MEDIA, "MediaUploadHandler > Unexpected state, site is null. "
                        + "Skipping cancellation of this request.");
            }
        }
    }

    private void prepareForUpload(@NonNull MediaModel media) {
        if (media.isVideo() && WPMediaUtils.isVideoOptimizationEnabled()) {
            addUniqueMediaToInProgressUploads(media);
            new VideoOptimizer(media, this).start();
        } else {
            dispatchUploadAction(media);
        }
    }

    private void dispatchUploadAction(@NonNull final MediaModel media) {
        SiteModel site = mSiteStore.getSiteByLocalId(media.getLocalSiteId());

        // somehow lost our reference to the site, complete this action
        if (site == null) {
            AppLog.w(T.MEDIA, "MediaUploadHandler > Unexpected state, site is null. Skipping this request.");
            checkIfUploadsComplete();
            return;
        }

        AppLog.i(T.MEDIA, "MediaUploadHandler > Dispatching upload action for media with local id: "
                + media.getId() + " and path: " + media.getFilePath());
        addUniqueMediaToInProgressUploads(media);

        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
        MediaPayload payload = new MediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void dispatchCancelAction(@NonNull final MediaModel media, @NonNull final SiteModel site, boolean delete) {
        AppLog.i(T.MEDIA, "MediaUploadHandler > Dispatching cancel upload action for media with local id: "
                + media.getId() + " and path: " + media.getFilePath());
        CancelMediaPayload payload = new CancelMediaPayload(site, media, delete);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }

    private boolean checkIfUploadsComplete() {
        if (sPendingUploads.isEmpty() && sInProgressUploads.isEmpty()) {
            AppLog.i(T.MEDIA, "MediaUploadHandler > Completed");
            return true;
        }
        return false;
    }

    // App events

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostMediaCanceled event) {
        if (event.post == null) {
            return;
        }
        synchronized (sInProgressUploads) {
            for (MediaModel inProgressUpload : sInProgressUploads) {
                if (inProgressUpload.getLocalPostId() == event.post.getId()) {
                    cancelUpload(inProgressUpload, true);
                }
            }
        }
        synchronized (sPendingUploads) {
            for (MediaModel pendingUpload : sPendingUploads) {
                if (pendingUpload.getLocalPostId() == event.post.getId()) {
                    cancelUpload(pendingUpload, true);
                }
            }
        }
    }

    // FluxC events

    /**
     * Has priority 9 on OnMediaUploaded events, which ensures that MediaUploadHandler is the first to receive
     * and process OnMediaUploaded events, before they trickle down to other subscribers.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 9)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.media == null) {
            AppLog.w(T.MEDIA, "MediaUploadHandler > Received media event for null media, ignoring");
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
            AppLog.e(T.MEDIA, "MediaUploadHandler > Cannot track media upload handler events if the original media"
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
            AppLog.i(T.MEDIA, "MediaUploadHandler > Attempting to add media with path " + mediaModel.getFilePath()
                    + " and site id " + mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia.getFilePath()
                    + ", " + queuedMedia.getLocalSiteId());
            if (compareBySiteAndFilePath(queuedMedia, mediaModel)) {
                return true;
            }
        }

        for (MediaModel queuedMedia : sPendingUploads) {
            AppLog.i(T.MEDIA, "MediaUploadHandler > Attempting to add media with path " + mediaModel.getFilePath()
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

    @Override
    public void onVideoOptimizationProgress(@NonNull MediaModel media, float progress) {
        sOptimizationProgressByMediaId.put(media.getId(), progress);
        // fire an event so EditPostActivity and PostsListFragment can show progress
        VideoOptimizer.ProgressEvent event = new VideoOptimizer.ProgressEvent(media, progress);
        EventBus.getDefault().post(event);
    }

    @Override
    public void onVideoOptimizationCompleted(@NonNull MediaModel media) {
        sOptimizationProgressByMediaId.remove(media.getId());
        // make sure this media should still be uploaded (may have been cancelled during optimization)
        if (sInProgressUploads.contains(media)) {
            dispatchUploadAction(media);
        } else {
            AppLog.d(T.MEDIA, "MediaUploadHandler > skipping upload of optimized media");
        }
    }
}
