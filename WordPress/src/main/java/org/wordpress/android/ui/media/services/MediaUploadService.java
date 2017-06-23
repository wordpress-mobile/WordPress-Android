package org.wordpress.android.ui.media.services;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.posts.services.MediaUploadReadyProcessor;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.FluxCUtils;
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
public class MediaUploadService {


    private static List<MediaModel> mPendingUploads = new ArrayList<>();
    private static List<MediaModel> mInProgressUploads = new ArrayList<>();

    // To avoid conflicts by editing the post each time a single upload completes, this map tracks completed media by
    // the post they're attached to, allowing us to update the post with the media URLs in a single batch at the end
    private static HashMap<Integer, List<MediaModel>> mCompletedUploadsByPost = new HashMap<>();

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject PostStore mPostStore;
    @Inject SiteStore mSiteStore;

    public MediaUploadService() {
        ((WordPress) WordPress.getContext()).component().inject(this);
        AppLog.i(T.MEDIA, "Media Upload Service > created");
        mDispatcher.register(this);
        EventBus.getDefault().register(this);
    }

    public void uploadMedia(List<MediaModel> mediaList) {
        if (mediaList != null) {
            for (MediaModel media : mediaList) {
                addUniqueMediaToQueue(media);
            }
        }
        uploadNextInQueue();
    }

    private void handleOnMediaUploadedSuccess(@NonNull OnMediaUploaded event) {
        if (event.canceled) {
            // Upload canceled
            AppLog.i(T.MEDIA, "Upload successfully canceled.");
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_CANCELED, getMediaFromInProgressQueueById(event.media.getId()), null);
            completeUploadWithId(event.media.getId());
            uploadNextInQueue();
        } else if (event.completed) {
            // Upload completed
            AppLog.i(T.MEDIA, "Upload completed - localId=" + event.media.getId() + " title=" + event.media.getTitle());
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_SUCCESS, getMediaFromInProgressQueueById(event.media.getId()), null);
            completeUploadWithId(event.media.getId());
            if (event.media.getLocalPostId() != 0) {
                // add the MediaModel object within the OnMediaUploaded event to our completedMediaList
                addMediaToPostCompletedMediaListMap(event.media);
            }
            uploadNextInQueue();
        } else {
            // Upload Progress
            // TODO check if we need to re-broadcast event.media, event.progress or we're just fine with
            // listening to  event.media, event.progress
            AppLog.d(T.MEDIA, event.media.getId() + " - progressing " + event.progress);
        }
    }

    private void handleOnMediaUploadedError(@NonNull OnMediaUploaded event) {
        AppLog.w(T.MEDIA, "Error uploading media: " + event.error.message);
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

    private static synchronized PostModel updatePostWithMediaUrl(PostModel post, MediaModel media,
                                                     MediaUploadReadyListener processor){
        if (media != null && post != null && processor != null) {
            // actually replace the media ID with the media uri
            PostModel modifiedPost = processor.replaceMediaFileWithUrlInPost(post, String.valueOf(media.getId()), FluxCUtils.mediaFileFromMediaModel(media));
            if (modifiedPost != null) {
                post = modifiedPost;
            }

            // we changed the post, so let’s mark this down
            if (!post.isLocalDraft()) {
                post.setIsLocallyChanged(true);
            }
            post.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));

        }
        return post;
    }

    private synchronized void savePostToDb(PostModel post) {
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));
    }

    private synchronized void uploadNextInQueue() {

        MediaModel next = getNextMediaToUpload();

        if (next == null) {
            AppLog.v(T.MEDIA, "No more media items to upload. Skipping this request - MediaUploadService.");
            updatePostAndStopServiceIfUploadsComplete();
            return;
        }

        SiteModel site = mSiteStore.getSiteByLocalId(next.getLocalSiteId());

        // somehow lost our reference to the site, complete this action
        if (site == null) {
            AppLog.i(T.MEDIA, "Unexpected state, site is null. Skipping this request - MediaUploadService.");
            updatePostAndStopServiceIfUploadsComplete();
            return;
        }

        dispatchUploadAction(next, site);
    }

    private synchronized void completeUploadWithId(int id) {
        MediaModel media = getMediaFromInProgressQueueById(id);
        if (media != null) {
            mInProgressUploads.remove(media);
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_STARTED, media, null);
        }
    }

    // this keeps a map for all completed media for each post, so we can process the post easily
    // in one go later
    private void addMediaToPostCompletedMediaListMap(MediaModel media) {
        List<MediaModel> mediaListForPost = mCompletedUploadsByPost.get(media.getLocalPostId());
        if (mediaListForPost == null) {
            mediaListForPost = new ArrayList<>();
        }
        mediaListForPost.add(media);
        mCompletedUploadsByPost.put(media.getLocalPostId(), mediaListForPost);
    }

    private MediaModel getMediaFromInProgressQueueById(int id) {
        for (MediaModel media : mInProgressUploads) {
            if (media.getId() == id)
                return media;
        }
        return null;
    }

    private MediaModel getNextMediaToUpload() {
        if (!mPendingUploads.isEmpty()) {
            return mPendingUploads.remove(0);
        }
        return null;
    }

    private void addUniqueMediaToQueue(MediaModel media) {
        if (media != null) {
            if (mediaAlreadyQueuedOrUploading(media)) {
                return;
            }

            // no match found in queue
            mPendingUploads.add(media);
        }
    }

    private void cancelUpload(MediaModel oneUpload, boolean delete) {
        if (oneUpload != null) {
            SiteModel site = mSiteStore.getSiteByLocalId(oneUpload.getLocalSiteId());
            if (site != null) {
                dispatchCancelAction(oneUpload, site, delete);
            } else {
                AppLog.i(T.MEDIA, "Unexpected state, site is null. Skipping cancellation of " +
                        "this request - MediaUploadService.");
            }
        }
    }

    private void dispatchUploadAction(@NonNull final MediaModel media, @NonNull final SiteModel site) {
        AppLog.i(T.MEDIA, "Dispatching upload action for media with local id: " + media.getId() +
                " and path: " + media.getFilePath());
        mInProgressUploads.add(media);
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));

        MediaPayload payload = new MediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void dispatchCancelAction(@NonNull final MediaModel media, @NonNull final SiteModel site, boolean delete) {
        AppLog.i(T.MEDIA, "Dispatching cancel upload action for media with local id: " + media.getId() +
                " and path: " + media.getFilePath());
        CancelMediaPayload payload = new CancelMediaPayload(site, media, delete);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }

    private synchronized void updatePostAndStopServiceIfUploadsComplete(){
        AppLog.i(AppLog.T.MEDIA, "Media Upload Service > completed");

        // we only trigger the actual post modification / processing after we've got
        // no other pending/inprogress uploads, and we have some completed uploads still to be processed
        if (mPendingUploads.isEmpty() && mInProgressUploads.isEmpty() && !mCompletedUploadsByPost.isEmpty()) {
            // here we need to edit the corresponding post with all completed uploads
            // also bear in mind the service could be handling media uploads for different posts,
            // so we also need to take into account processing completed uploads in batches through
            // each post
            for (Integer postId : mCompletedUploadsByPost.keySet()) {
                savePostToDb(updatePostWithCurrentlyCompletedUploads(mPostStore.getPostByLocalPostId(postId)));
            }
        }

        stopServiceIfUploadsComplete();
    }

    private void stopServiceIfUploadsComplete() {
        AppLog.i(T.MEDIA, "Media Upload Service > completed");
        if (mPendingUploads.isEmpty() && mInProgressUploads.isEmpty()) {
            // TODO: Tell UploadService it can stop
        }
    }

    public static synchronized PostModel updatePostWithCurrentlyCompletedUploads(PostModel post) {
        // now get the list of completed media for this post, so we can make post content
        // updates in one go and save only once
        if (post != null) {
            MediaUploadReadyListener processor = new MediaUploadReadyProcessor();
            List<MediaModel> mediaList = mCompletedUploadsByPost.get(post.getId());
            if (mediaList != null && !mediaList.isEmpty()) {
                for (MediaModel media : mediaList) {
                    post = updatePostWithMediaUrl(post, media, processor);
                }
                // finally remove all completed uploads for this post, as they've been taken care of
                mCompletedUploadsByPost.remove(post.getId());
            }
        }
        return post;
    }

    // App events

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostMediaCanceled event) {
        if (event.post == null) {
            return;
        }
        for (MediaModel inProgressUpload : mInProgressUploads) {
            if (inProgressUpload.getLocalPostId() == event.post.getId()) {
                cancelUpload(inProgressUpload, true);
            }
        }
        for (MediaModel pendingUpload : mPendingUploads) {
            if (pendingUpload.getLocalPostId() == event.post.getId()) {
                cancelUpload(pendingUpload, true);
            }
        }
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        // event for unknown media, ignoring
        if (event.media == null) {
            AppLog.w(T.MEDIA, "Received media event for null media, ignoring");
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
            AppLog.e(T.MEDIA, "Cannot track media upload service events if the original media is null!!");
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
        for (MediaModel queuedMedia : mInProgressUploads) {
            AppLog.i(T.MEDIA, "Looking to add media with path " + mediaModel.getFilePath() + " and site id " +
                    mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia.getFilePath() + ", " +
                    queuedMedia.getLocalSiteId());
            if (compareBySiteAndFilePath(queuedMedia, mediaModel)) {
                return true;
            }
        }

        for (MediaModel queuedMedia : mPendingUploads) {
            AppLog.i(T.MEDIA, "Looking to add media with path " + mediaModel.getFilePath() + " and site id " +
                    mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia.getFilePath() + ", " +
                    queuedMedia.getLocalSiteId());
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

    public static boolean hasPendingMediaUploadsForPost(PostModel postModel) {
        for (MediaModel queuedMedia : mInProgressUploads) {
            if (queuedMedia.getLocalPostId() == postModel.getId()) {
                return true;
            }
        }

        for (MediaModel queuedMedia : mPendingUploads) {
            if (queuedMedia.getLocalPostId() == postModel.getId()) {
                return true;
            }
        }
        return false;
    }
}
