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

public class MediaUploadService extends Service {
    private static final String MEDIA_LIST_KEY = "mediaList";

    private static List<MediaModel> mPendingUploads = new ArrayList<>();
    private static List<MediaModel> mInProgressUploads = new ArrayList<>();

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
        AppLog.i(T.MEDIA, "Media Upload Service > created");
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
        AppLog.i(T.MEDIA, "Media Upload Service > destroyed");
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
            AppLog.e(T.MEDIA, "MediaUploadService was killed and restarted with a null intent.");
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
            AppLog.i(T.MEDIA, "Upload successfully canceled.");
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_CANCELED, getMediaFromQueueById(event.media.getId()), null);
            completeUploadWithId(event.media.getId());
            uploadNextInQueue();
        } else if (event.completed) {
            // Upload completed
            AppLog.i(T.MEDIA, "Upload completed - localId=" + event.media.getId() + " title=" + event.media.getTitle());

            // here we need to edit the corresponding post
            updatePostWithMediaUrl(event.media);
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_SUCCESS, getMediaFromQueueById(event.media.getId()), null);

            completeUploadWithId(event.media.getId());
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

    private synchronized void updatePostWithMediaUrl(MediaModel media){
        if (media != null) {
            PostModel post = mPostStore.getPostByLocalPostId(media.getLocalPostId());
            if (post != null) {
                // actually replace the media ID with the media uri
                MediaUploadReadyListener processor = new MediaUploadReadyProcessor();
                PostModel modifiedPost = processor.replaceMediaFileWithUrlInPost(post, String.valueOf(media.getId()), FluxCUtils.mediaFileFromMediaModel(media));
                if (modifiedPost != null) {
                    post = modifiedPost;
                }

                // we changed the post, so let’s mark this down
                if (!post.isLocalDraft()) {
                    post.setIsLocallyChanged(true);
                }
                post.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));

                // finally save the post
                savePostToDb(post);
            }
        }
    }

    private synchronized void savePostToDb(PostModel post) {
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));
    }

    private void uploadNextInQueue() {

        MediaModel next = getNextMediaToUpload();

        if (next == null) {
            AppLog.v(T.MEDIA, "No more media items to upload. Skipping this request - MediaUploadService.");
            stopServiceIfUploadsComplete();
            return;
        }

        SiteModel site = mSiteStore.getSiteByLocalId(next.getLocalSiteId());

        // somehow lost our reference to the site, complete this action
        if (site == null) {
            AppLog.i(T.MEDIA, "Unexpected state, site is null. Skipping this request - MediaUploadService.");
            stopServiceIfUploadsComplete();
            return;
        }

        dispatchUploadAction(next, site);
    }

    private void completeUploadWithId(int id) {
        MediaModel media = getMediaFromQueueById(id);
        if (media != null) {
            mInProgressUploads.remove(media);
            trackUploadMediaEvents(AnalyticsTracker.Stat.MEDIA_UPLOAD_STARTED, media, null);
        }
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
                AppLog.i(T.MEDIA, "Unexpected state, site is null. Skipping cancellation of " +
                        "this request - MediaUploadService.");
            }
        }
    }

    private void dispatchUploadAction(@NonNull final MediaModel media, @NonNull final SiteModel site) {
        AppLog.i(T.MEDIA, "Dispatching upload action for media with local id: " + media.getId() +
                " and path: " + media.getFilePath());
        mInProgressUploads.add(media);
        media.setUploadState(UploadState.UPLOADING.name());
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));

        MediaPayload payload = new MediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    private void dispatchCancelAction(@NonNull final MediaModel media, @NonNull final SiteModel site) {
        AppLog.i(T.MEDIA, "Dispatching cancel upload action for media with local id: " + media.getId() +
                " and path: " + media.getFilePath());
        MediaPayload payload = new MediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }

    private void stopServiceIfUploadsComplete(){
        AppLog.i(T.MEDIA, "Media Upload Service > completed");
        if (mPendingUploads.isEmpty() && mInProgressUploads.isEmpty()) {
            AppLog.i(T.MEDIA, "No more items pending in queue. Stopping MediaUploadService.");
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
        Map<String, Object> mediaProperties = AnalyticsUtils.getMediaProperties(this, media.isVideo(), null, media.getFilePath());
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
            if (queuedMedia.getLocalSiteId() == mediaModel.getLocalSiteId() &&
                    StringUtils.equals(queuedMedia.getFilePath(), mediaModel.getFilePath())) {
                return true;
            }
        }

        for (MediaModel queuedMedia : mPendingUploads) {
            AppLog.i(T.MEDIA, "Looking to add media with path " + mediaModel.getFilePath() + " and site id " +
                    mediaModel.getLocalSiteId() + ". Comparing with " + queuedMedia.getFilePath() + ", " +
                    queuedMedia.getLocalSiteId());
            if (queuedMedia.getLocalSiteId() == mediaModel.getLocalSiteId() &&
                    StringUtils.equals(queuedMedia.getFilePath(), mediaModel.getFilePath())) {
                return true;
            }
        }
        return false;
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
