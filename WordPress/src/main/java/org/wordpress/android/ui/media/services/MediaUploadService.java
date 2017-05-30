package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.UploadState;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.posts.services.MediaUploadReadyProcessor;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Started with explicit list of media to upload.
 */

public class MediaUploadService extends Service {
    private static final String MEDIA_LIST_KEY = "mediaList";

    private List<MediaModel> mPendingUploads = new ArrayList<>();
    private List<MediaModel> mInProgressUploads = new ArrayList<>();
    // we need this last map so to be able to update the PostModel once with all completed uploads,
    // as opposed to editing the Post each time a single upload completes.
    // Also, we need to process each Post's media in a batch, so that's why we are using a map
    // instead of using a simple List (a map will have a List of MediaModel for their corresponding
    // PostId where they're supposed to belong to as per the user intent)
    private HashMap<Long, List<MediaModel>> mCompletedUploads = new HashMap<>();

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject PostStore mPostStore;
    @Inject SiteStore mSiteStore;

    public static void startService(Context context, ArrayList<MediaModel> mediaList) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, MediaUploadService.class);
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
        // TODO: recover any media that is in the MediaStore that has not yet been completely uploaded
        // or better yet, create an auxiliary table to host MediaUploadUnitInfo objects
    }

    @Override
    public void onDestroy() {
        for (MediaModel oneUpload : mInProgressUploads) {
            cancelUpload(oneUpload);
        }
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
        // skip this request if no media to upload given
        if (intent == null || !intent.hasExtra(MEDIA_LIST_KEY)) {
            AppLog.e(AppLog.T.MEDIA, "MediaUploadService was killed and restarted with a null intent.");
            updatePostAndStopServiceIfUploadsComplete();
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
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_CANCELED, getMediaFromQueueById(event.media.getId()), null);
            completeUploadWithId(event.media.getId());
            uploadNextInQueue();
        } else if (event.completed) {
            // Upload completed
            AppLog.i(AppLog.T.MEDIA, "Upload completed - localId=" + event.media.getId() + " title=" + event.media.getTitle());
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_SUCCESS, getMediaFromQueueById(event.media.getId()), null);
            completeUploadWithId(event.media.getId());
            uploadNextInQueue();
        } else {
            // Upload Progress
            // TODO check if we need to re-broadcast event.media, event.progress or we're just fine with
            // listening to  event.media, event.progress
            AppLog.d(AppLog.T.MEDIA, event.media.getId() + " - progressing " + event.progress);
        }
    }

    private void handleOnMediaUploadedError(@NonNull OnMediaUploaded event) {
        AppLog.w(AppLog.T.MEDIA, "Error uploading media: " + event.error.message);
        // TODO: Don't update the state here, it needs to be done in FluxC
        MediaModel media = getMediaFromQueueById(event.media.getId());
        if (media != null) {
            media.setUploadState(UploadState.FAILED.name());
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

    private synchronized PostModel updatePostWithMediaUrl(PostModel post, MediaModel media,
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
            AppLog.v(AppLog.T.MEDIA, "No more media items to upload. Skipping this request - MediaUploadService.");
            updatePostAndStopServiceIfUploadsComplete();
            return;
        }

        SiteModel site = mSiteStore.getSiteByLocalId(next.getLocalSiteId());

        // somehow lost our reference to the site, complete this action
        if (site == null) {
            AppLog.i(AppLog.T.MEDIA, "Unexpected state, site is null. Skipping this request - MediaUploadService.");
            updatePostAndStopServiceIfUploadsComplete();
            return;
        }

        dispatchUploadAction(next, site);
    }

    private synchronized void completeUploadWithId(int id) {
        MediaModel media = getMediaFromQueueById(id);
        if (media != null) {
            mInProgressUploads.remove(media);
            addMediaToPostCompletedMediaListMap(media);
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_STARTED, media, null);
        }
    }

    // this keeps a map for all completed media for each post, so we can process the post easily
    // in one go later
    private void addMediaToPostCompletedMediaListMap(MediaModel media) {
        List<MediaModel> mediaListForPost = mCompletedUploads.get(media.getPostId());
        if (mediaListForPost == null) {
            mediaListForPost = new ArrayList<>();
        }
        mediaListForPost.add(media);
        mCompletedUploads.put(media.getPostId(), mediaListForPost);
    }

    private MediaModel getMediaFromQueueById(int id) {
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

    private void unpackIntent(@NonNull Intent intent) {

        // TODO right now, in the case we had pending uploads and the app/service was restarted,
        // we don't really have a way to tell which media was supposed to be added to which post,
        // unless we open each draft post from the PostStore and try to see if there was any locally added media to try
        // and match their IDs.
        // So let's hold on a bit on this functionality, the service won't be recovering any
        // pending / missing / cancelled / interrupted uploads for now

//        // add local queued media from store
//        List<MediaModel> localMedia = mMediaStore.getLocalSiteMedia(site);
//        if (localMedia != null && !localMedia.isEmpty()) {
//            // uploading is updated to queued, queued media added to the queue, failed media added to completed list
//            for (MediaModel mediaItem : localMedia) {
//
//                if (MediaUploadState.UPLOADING.name().equals(mediaItem.getUploadState())) {
//                    mediaItem.setUploadState(MediaUploadState.QUEUED.name());
//                    mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaItem));
//                }
//
//                if (MediaUploadState.QUEUED.name().equals(mediaItem.getUploadState())) {
//                    addUniqueMediaToQueue(mediaItem);
//                } else if (MediaUploadState.FAILED.name().equals(mediaItem.getUploadState())) {
//                    getCompletedItems().add(mediaItem);
//                }
//            }
//        }

        // add new media
        @SuppressWarnings("unchecked")
        List<MediaModel> mediaList = (List<MediaModel>) intent.getSerializableExtra(MEDIA_LIST_KEY);
        if (mediaList != null) {
            for (MediaModel media : mediaList) {
                addUniqueMediaToQueue(media);
            }
        }
    }

    private void cancelUpload(MediaModel oneUpload) {
        if (oneUpload != null) {
            SiteModel site = mSiteStore.getSiteByLocalId(oneUpload.getLocalSiteId());
            if (site != null) {
                dispatchCancelAction(oneUpload, site);
            } else {
                AppLog.i(AppLog.T.MEDIA, "Unexpected state, site is null. Skipping cancellation of " +
                        "this request - MediaUploadService.");
            }
        }
    }

    private void dispatchUploadAction(@NonNull final MediaModel media, @NonNull final SiteModel site) {
        AppLog.i(AppLog.T.MEDIA, "Dispatching upload action for media with local id: " + media.getId() +
                " and path: " + media.getFilePath());
        mInProgressUploads.add(media);
        media.setUploadState(UploadState.UPLOADING.name());
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));

        MediaPayload payload = new MediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void dispatchCancelAction(@NonNull final MediaModel media, @NonNull final SiteModel site) {
        AppLog.i(AppLog.T.MEDIA, "Dispatching cancel upload action for media with local id: " + media.getId() +
                " and path: " + media.getFilePath());
        MediaPayload payload = new MediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }

    private void updatePostAndStopServiceIfUploadsComplete(){
        AppLog.i(AppLog.T.MEDIA, "Media Upload Service > completed");
        if (mPendingUploads.isEmpty() && mInProgressUploads.isEmpty()) {

            // here we need to edit the corresponding post with all completed uploads
            // also bear in mind the service could be handling media uploads for different posts,
            // so we also need to take into account processing completed uploads in batches through
            // each post
            MediaUploadReadyListener processor = new MediaUploadReadyProcessor();
            for (Long postId : mCompletedUploads.keySet()) {
                PostModel post = mPostStore.getPostByLocalPostId(postId);
                // now get the list of completed media for this post, so we can make post content
                // updates in one go and save only once
                List<MediaModel> mediaList = mCompletedUploads.get(postId);
                for (MediaModel media : mediaList) {
                    post = updatePostWithMediaUrl(post, media, processor);
                }
                // finally save the post, and continue to the next post in the completed map
                savePostToDb(post);
            }

            AppLog.i(AppLog.T.MEDIA, "No more items pending in queue. Stopping MediaUploadService.");
            stopSelf();
        }
    }

    // App events

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostMediaCanceled event) {
        if (event.post == null) {
            return;
        }
        for (MediaModel inProgressUpload : mInProgressUploads) {
            if (inProgressUpload.getLocalPostId() == event.post.getId()) {
                cancelUpload(inProgressUpload);
            }
        }
        for (MediaModel pendingUpload : mPendingUploads) {
            if (pendingUpload.getLocalPostId() == event.post.getId()) {
                cancelUpload(pendingUpload);
            }
        }
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        // event for unknown media, ignoring
        if (event.media == null) {
            AppLog.w(AppLog.T.MEDIA, "Received media event for null media, ignoring");
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
        AnalyticsTracker.track(stat, mediaProperties);
    }

    private boolean mediaAlreadyQueuedOrUploading(MediaModel mediaModel) {
        for (MediaModel queuedMedia : mInProgressUploads) {
            AppLog.d(AppLog.T.TESTS, "Looking to add media with path " + mediaModel.getFilePath() + " and site id " +
                    mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia.getFilePath() + ", " +
                    queuedMedia.getLocalSiteId());
            if (areTheseTheSameMedia(queuedMedia, mediaModel)) {
                return true;
            }
        }

        for (MediaModel queuedMedia : mPendingUploads) {
            AppLog.d(AppLog.T.TESTS, "Looking to add media with path " + mediaModel.getFilePath() + " and site id " +
                    mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia.getFilePath() + ", " +
                    queuedMedia.getLocalSiteId());
            if (areTheseTheSameMedia(queuedMedia, mediaModel)) {
                return true;
            }
        }
        return false;
    }

    private boolean areTheseTheSameMedia(MediaModel media1, MediaModel media2) {
        if (media1.getLocalSiteId() == media2.getLocalSiteId() &&
                StringUtils.equals(media1.getFilePath(), media2.getFilePath())) {
            return true;
        }
        return false;
    }
}