package org.wordpress.android.ui.uploads;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

public class MediaUploadHandler implements UploadHandler<MediaModel>, VideoOptimizer.VideoOptimizationListener {
    private static List<MediaModel> sPendingUploads = new ArrayList<>();
    private static List<MediaModel> sInProgressUploads = new ArrayList<>();
    private static ConcurrentHashMap<Integer, Float> sOptimizationProgressByMediaId = new ConcurrentHashMap<>();

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;

    MediaUploadHandler() {
        ((WordPress) WordPress.getContext().getApplicationContext()).component().inject(this);
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

    static boolean hasInProgressMediaUploadsForPost(int postId) {
        synchronized (sInProgressUploads) {
            for (MediaModel queuedMedia : sInProgressUploads) {
                if (queuedMedia.getLocalPostId() == postId) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean hasPendingMediaUploadsForPost(int postId) {
        synchronized (sPendingUploads) {
            for (MediaModel queuedMedia : sPendingUploads) {
                if (queuedMedia.getLocalPostId() == postId) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasPendingOrInProgressMediaUploadsForPost(int postId) {
        // Check if there are media in the in-progress or the pending queue attached to the given post
        return hasInProgressMediaUploadsForPost(postId) || hasPendingMediaUploadsForPost(postId);
    }

    static MediaModel getPendingOrInProgressFeaturedImageUploadForPost(PostImmutableModel postModel) {
        if (postModel == null) {
            return null;
        }
        List<MediaModel> uploads = getPendingOrInProgressMediaUploadsForPost(postModel);
        for (MediaModel model : uploads) {
            if (model.getMarkedLocallyAsFeatured()) {
                return model;
            }
        }

        return null;
    }

    public static List<MediaModel> getPendingOrInProgressMediaUploadsForPost(PostImmutableModel postModel) {
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

    static boolean isPendingOrInProgressMediaUpload(int mediaId) {
        synchronized (sInProgressUploads) {
            for (MediaModel uploadingMedia : sInProgressUploads) {
                if (uploadingMedia.getId() == mediaId) {
                    return true;
                }
            }
        }

        synchronized (sPendingUploads) {
            for (MediaModel queuedMedia : sPendingUploads) {
                if (queuedMedia.getId() == mediaId) {
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
    static float getOverallProgressForVideo(int videoId, float uploadProgress) {
        if (sOptimizationProgressByMediaId.containsKey(videoId)) {
            float optimizationProgress = sOptimizationProgressByMediaId.get(videoId);
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
            if (media.getId() == id) {
                return media;
            }
        }
        return null;
    }

    private MediaModel getNextMediaToUpload() {
        synchronized (sPendingUploads) {
            if (!sPendingUploads.isEmpty()) {
                return sPendingUploads.remove(0);
            }
        }
        return null;
    }

    private void addUniqueMediaToQueue(MediaModel media) {
        if (media != null) {
            if (mediaAlreadyQueuedOrUploading(media)) {
                return;
            }

            synchronized (sPendingUploads) {
                // no match found in queue
                sPendingUploads.add(media);
            }
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
        UploadMediaPayload payload = new UploadMediaPayload(site, media, AppPrefs.isStripImageLocation());
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
    @Subscribe(threadMode = ThreadMode.MAIN)
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
                                                                                media.isVideo(), null,
                                                                                media.getFilePath());
        if (properties != null) {
            mediaProperties.putAll(properties);
        }
        AnalyticsTracker.track(stat, mediaProperties);
    }

    private boolean mediaAlreadyQueuedOrUploading(MediaModel mediaModel) {
        for (MediaModel queuedMedia : sInProgressUploads) {
            AppLog.i(T.MEDIA, "MediaUploadHandler > Attempting to add media with path " + mediaModel.getFilePath()
                              + " and site id " + mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia
                                      .getFilePath()
                              + ", " + queuedMedia.getLocalSiteId());
            if (isSameMediaFileQueuedForThisPost(queuedMedia, mediaModel)) {
                return true;
            }
        }

        synchronized (sPendingUploads) {
            for (MediaModel queuedMedia : sPendingUploads) {
                AppLog.i(T.MEDIA, "MediaUploadHandler > Attempting to add media with path " + mediaModel.getFilePath()
                                  + " and site id " + mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia
                                          .getFilePath()
                                  + ", " + queuedMedia.getLocalSiteId());
                if (isSameMediaFileQueuedForThisPost(queuedMedia, mediaModel)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSameMediaFileQueuedForThisPost(MediaModel media1, MediaModel media2) {
        /*
            This method used to be called "compareBySiteAndFilePath" and compared just siteId and filePath. It made
            sense since a media file is tied to a site and can be referenced from multiple posts on that site. This
            approach tried to prevent wasting users' data.

            The issue was that when a same image was added to content of two posts only a single MediaModel was
            enqueued. However, MediaModel references only a single post (`localPostId`). When the upload finished
            only the first post got updated with the url. The second post got uploaded to the server with a path to
            local image. We decided to check whether the image belongs to the same post so we can be sure the local
            path gets replaced with the url.

            More info can be found here - https://github.com/wordpress-mobile/WordPress-Android/pull/10204.

            We also need to check the `markedLocallyAsFeatured` flag is equal as we might lose it otherwise. If the
            user adds an image into the post content and they set the same image as featured image, we need to enqueue
            both uploads. Otherwise, we could lose the information what we need to update - the featured image or post
            content.

            Issue with a proper fix - https://github.com/wordpress-mobile/WordPress-Android/issues/10210
         */
        return (media1.getLocalSiteId() == media2.getLocalSiteId()
                && media1.getLocalPostId() == media2.getLocalPostId()
                && StringUtils.equals(media1.getFilePath(), media2.getFilePath()))
                && media1.getMarkedLocallyAsFeatured() == media2.getMarkedLocallyAsFeatured();
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
