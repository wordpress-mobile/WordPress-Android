package org.wordpress.android.ui.uploads;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.store.UploadStore.ClearMediaPayload;
import org.wordpress.android.fluxc.store.UploadStore.UploadError;
import org.wordpress.android.ui.media.services.MediaUploadReadyListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.FluxCUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class UploadService extends Service {
    private static final String KEY_MEDIA_LIST = "mediaList";
    private static final String KEY_LOCAL_POST_ID = "localPostId";
    private static final String KEY_SHOULD_TRACK_ANALYTICS = "shouldTrackPostAnalytics";

    private static @Nullable UploadService sInstance;

    private MediaUploadHandler mMediaUploadHandler;
    private PostUploadHandler mPostUploadHandler;
    private PostUploadNotifier mPostUploadNotifier;

    // Posts that we're withholding from the PostUploadManager until their pending media uploads are completed
    private static final List<UploadingPost> sPostsWithPendingMedia = new ArrayList<>();

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject PostStore mPostStore;
    @Inject SiteStore mSiteStore;
    @Inject UploadStore mUploadStore;

    private class UploadingPost {
        PostModel postModel;
        List<MediaModel> pendingMedia = new ArrayList<>();
        boolean isCancelled;

        UploadingPost(PostModel postModel, List<MediaModel> pendingMedia) {
            this.postModel = postModel;
            this.pendingMedia = pendingMedia;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        AppLog.i(T.MAIN, "UploadService > Created");
        mDispatcher.register(this);
        sInstance = this;
        // TODO: Recover any posts/media uploads that were interrupted by the service being stopped
    }

    @Override
    public void onDestroy() {
        if (mMediaUploadHandler != null) {
            mMediaUploadHandler.cancelInProgressUploads();
            mMediaUploadHandler.unregister();
        }

        if (mPostUploadHandler != null) {
            mPostUploadHandler.cancelInProgressUploads();
            mPostUploadHandler.unregister();
        }

        // Update posts with any completed AND failed uploads in our post->media map
        updatePostModelWithCompletedAndFailedUploads();

        mDispatcher.unregister(this);
        sInstance = null;
        AppLog.i(T.MAIN, "UploadService > Destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Skip this request if no items to upload were given
        if (intent == null || (!intent.hasExtra(KEY_MEDIA_LIST) && !intent.hasExtra(KEY_LOCAL_POST_ID))) {
            AppLog.e(T.MAIN, "UploadService > Killed and restarted with an empty intent");
            stopServiceIfUploadsComplete();
            return START_NOT_STICKY;
        }

        if (mMediaUploadHandler == null) {
            mMediaUploadHandler = new MediaUploadHandler();
        }

        if (mPostUploadNotifier == null) {
            mPostUploadNotifier = new PostUploadNotifier(getApplicationContext(), this);
        }

        if (mPostUploadHandler == null) {
            mPostUploadHandler = new PostUploadHandler(mPostUploadNotifier);
        }

        if (intent.hasExtra(KEY_MEDIA_LIST)) {
            unpackMediaIntent(intent);
        }

        if (intent.hasExtra(KEY_LOCAL_POST_ID)) {
            unpackPostIntent(intent);
        }

        return START_REDELIVER_INTENT;
    }

    private void unpackMediaIntent(@NonNull Intent intent) {
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
        List<MediaModel> mediaList = (List<MediaModel>) intent.getSerializableExtra(KEY_MEDIA_LIST);
        if (mediaList != null) {
            for (MediaModel media : mediaList) {
                mMediaUploadHandler.upload(media);
            }
        }
    }

    private void unpackPostIntent(@NonNull Intent intent) {
        PostModel post = mPostStore.getPostByLocalPostId(intent.getIntExtra(KEY_LOCAL_POST_ID, 0));
        if (post != null) {
            boolean shouldTrackAnalytics = intent.getBooleanExtra(KEY_SHOULD_TRACK_ANALYTICS, false);
            if (shouldTrackAnalytics) {
                mPostUploadHandler.registerPostForAnalyticsTracking(post);
            }

            mPostUploadNotifier.cancelErrorNotification(post);

            if (!hasPendingOrInProgressMediaUploadsForPost(post)) {
                mPostUploadHandler.upload(post);
            } else {
                for (UploadingPost uploadingPost : sPostsWithPendingMedia) {
                    if (uploadingPost.postModel.getId() == post.getId()) {
                        // If we already have an entry for this post, it was probably edited and re-uploaded while we
                        // had in-progress media uploading (and was marked as isCancelled)
                        // Update the model, unmark it as cancelled, and add any new media the post didn't have the
                        // first time to its pending media list
                        uploadingPost.postModel = post;
                        uploadingPost.isCancelled = false;
                        Set<MediaModel> totalPendingMedia = new HashSet<>();
                        // Media we already have on record as pending for this post (some may have completed by now)
                        totalPendingMedia.addAll(uploadingPost.pendingMedia);
                        // Media the MediaUploadHandler is currently processing
                        totalPendingMedia.addAll(MediaUploadHandler.getPendingOrInProgressMediaUploadsForPost(post));
                        uploadingPost.pendingMedia = new ArrayList<>(totalPendingMedia);
                        showNotificationForPostWithPendingMedia(post);
                        return;
                    }
                }
                // Brand new post upload
                UploadingPost uploadingPost = new UploadingPost(post,
                        MediaUploadHandler.getPendingOrInProgressMediaUploadsForPost(post));
                sPostsWithPendingMedia.add(uploadingPost);
                showNotificationForPostWithPendingMedia(post);
            }
        }
    }

    /**
     * Adds a post to the queue.
     */
    public static void uploadPost(Context context, @NonNull PostModel post) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(UploadService.KEY_LOCAL_POST_ID, post.getId());
        intent.putExtra(KEY_SHOULD_TRACK_ANALYTICS, false);
        context.startService(intent);
    }

    /**
     * Adds a post to the queue and tracks post analytics.
     * To be used only the first time a post is uploaded, i.e. when its status changes from local draft or remote draft
     * to published.
     */
    public static void uploadPostAndTrackAnalytics(Context context, @NonNull PostModel post) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(UploadService.KEY_LOCAL_POST_ID, post.getId());
        intent.putExtra(KEY_SHOULD_TRACK_ANALYTICS, true);
        context.startService(intent);
    }

    public static void setLegacyMode(boolean enabled) {
        PostUploadHandler.setLegacyMode(enabled);
    }

    public static void uploadMedia(Context context, @NonNull ArrayList<MediaModel> mediaList) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(UploadService.KEY_MEDIA_LIST, mediaList);
        context.startService(intent);
    }

    /**
     * Returns true if the passed post is either currently uploading or waiting to be uploaded.
     * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
     * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
     */
    public static boolean isPostUploadingOrQueued(PostModel post) {
        if (post == null) {
            return false;
        }

        // First check for posts uploading or queued inside the PostUploadManager
        if (PostUploadHandler.isPostUploadingOrQueued(post)) {
            return true;
        }

        // Then check the list of posts waiting for media to complete
        if (sPostsWithPendingMedia.size() > 0) {
            synchronized (sPostsWithPendingMedia) {
                for (UploadingPost queuedPost : sPostsWithPendingMedia) {
                    if (queuedPost.postModel.getId() == post.getId()) {
                        return !queuedPost.isCancelled;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isPostQueued(PostModel post) {
        if (post == null) {
            return false;
        }

        // Check for posts queued inside the PostUploadManager
        return PostUploadHandler.isPostQueued(post);
    }

    /**
     * Returns true if the passed post is currently uploading.
     * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
     * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
     */
    public static boolean isPostUploading(PostModel post) {
        return post != null && PostUploadHandler.isPostUploading(post);
    }

    public static void cancelQueuedPostUploadAndRelatedMedia(PostModel post) {
        if (post != null) {
            cancelQueuedPostUpload(post);
            EventBus.getDefault().post(new PostEvents.PostMediaCanceled(post));
        }
    }

    public static void cancelQueuedPostUpload(PostModel post) {
        if (post != null) {
            synchronized (sPostsWithPendingMedia) {
                for (UploadingPost uploadingPost : sPostsWithPendingMedia) {
                    PostModel postModel = uploadingPost.postModel;
                    if (postModel.getId() == post.getId()) {
                        uploadingPost.isCancelled = true;
                    }
                }
            }
        }
    }

    public static PostModel updatePostWithCurrentlyCompletedUploads(PostModel post) {
        if (post != null && sInstance != null) {
            // now get the list of completed media for this post, so we can make post content
            // updates in one go and save only once
            MediaUploadReadyListener processor = new MediaUploadReadyProcessor();
            Set<MediaModel> completedMedia = sInstance.mUploadStore.getCompletedMediaForPost(post);
            for (MediaModel media : completedMedia) {
                post = updatePostWithMediaUrl(post, media, processor);
            }
            // finally remove all completed uploads for this post, as they've been taken care of
            ClearMediaPayload clearMediaPayload = new ClearMediaPayload(post, completedMedia);
            sInstance.mDispatcher.dispatch(UploadActionBuilder.newClearMediaAction(clearMediaPayload));
        }
        return post;
    }

    public static PostModel updatePostWithCurrentlyFailedUploads(PostModel post) {
        if (post != null && sInstance != null) {
            // now get the list of failed media for this post, so we can make post content
            // updates in one go and save only once
            MediaUploadReadyListener processor = new MediaUploadReadyProcessor();
            Set<MediaModel> failedMedia = sInstance.mUploadStore.getFailedMediaForPost(post);
            for (MediaModel media : failedMedia) {
                post = updatePostWithFailedMedia(post, media, processor);
            }
            // Unlike completed media, we won't remove the failed media references, so we can look up their errors later
        }
        return post;
    }

    public static void markPostAsError(PostModel post) {
        // now keep track of the error reason so it can be queried
        UploadError reason = new UploadError(new MediaError(MediaErrorType.GENERIC_ERROR));
        // TODO
//        addUploadErrorToFailedPosts(post, reason);
    }

    public static boolean hasInProgressMediaUploadsForPost(PostModel postModel) {
        return postModel != null && MediaUploadHandler.hasInProgressMediaUploadsForPost(postModel);
    }

    public static boolean hasPendingMediaUploadsForPost(PostModel postModel) {
        return postModel != null && MediaUploadHandler.hasPendingMediaUploadsForPost(postModel);
    }

    public static boolean hasPendingOrInProgressMediaUploadsForPost(PostModel postModel) {
        return postModel != null && MediaUploadHandler.hasPendingOrInProgressMediaUploadsForPost(postModel);
    }

    public static boolean hasMediaErrorForPost(PostModel post, UploadStore uploadStore) {
        UploadError error  = getUploadErrorForPost(post, uploadStore);
        return error != null && error.mediaError != null;
    }

    public static UploadError getUploadErrorForPost(PostModel post, UploadStore uploadStore) {
        return uploadStore.getUploadErrorForPost(post);
    }

    public static List<MediaModel> getPendingOrInProgressMediaUploadsForPost(PostModel post){
        return MediaUploadHandler.getPendingOrInProgressMediaUploadsForPost(post);
    }

    public static float getMediaUploadProgressForPost(PostModel postModel) {
        if (postModel == null || sInstance == null) {
            // If the UploadService isn't running, there's no progress for this post
            return 0;
        }

        Set<MediaModel> pendingMediaList = sInstance.mUploadStore.getUploadingMediaForPost(postModel);

        if (pendingMediaList.size() == 0) {
            return 1;
        }

        float overallProgress = 0;
        for (MediaModel pendingMedia : pendingMediaList) {
            overallProgress += getUploadProgressForMedia(pendingMedia);
        }
        overallProgress /= pendingMediaList.size();

        return overallProgress;
    }

    public static float getUploadProgressForMedia(MediaModel mediaModel) {
        if (mediaModel == null || sInstance == null) {
            // If the UploadService isn't running, there's no progress for this media
            return 0;
        }

        return MediaUploadHandler.getProgressForMedia(mediaModel, sInstance.mUploadStore);
    }

    public static @NonNull List<MediaModel> getPendingMediaForPost(PostModel postModel) {
        for (UploadingPost uploadingPost : sPostsWithPendingMedia) {
            if (uploadingPost.postModel.getId() == postModel.getId()) {
                return uploadingPost.pendingMedia;
            }
        }
        return Collections.emptyList();
    }

    public static boolean isPendingOrInProgressMediaUpload(@NonNull MediaModel media) {
        return MediaUploadHandler.isPendingOrInProgressMediaUpload(media);
    }

    public static PostModel isMediaBeingUploadedForAPost(@NonNull MediaModel mediaToCheck) {
        for (UploadingPost uploadingPost : sPostsWithPendingMedia) {
            for (MediaModel media : uploadingPost.pendingMedia) {
                if (media.getId() == mediaToCheck.getId()) {
                    return uploadingPost.postModel;
                }
            }
        }
        return null;
    }

    /**
     * Rechecks all media in the MediaStore marked UPLOADING/QUEUED against the UploadingService to see
     * if it's actually uploading or queued and change it accordingly, to recover from an inconsistent state
     */
    public static void sanitizeMediaUploadStateForSite(@NonNull MediaStore mediaStore, @NonNull Dispatcher dispatcher,
                                                       @NonNull SiteModel site) {
        List<MediaModel> uploadingMedia =
                mediaStore.getSiteMediaWithState(site, MediaModel.MediaUploadState.UPLOADING);
        List<MediaModel> queuedMedia =
                mediaStore.getSiteMediaWithState(site, MediaModel.MediaUploadState.QUEUED);
        List<MediaModel> uploadingOrQueuedMedia = new ArrayList<>();
        uploadingOrQueuedMedia.addAll(uploadingMedia);
        uploadingOrQueuedMedia.addAll(queuedMedia);

        for (final MediaModel media : uploadingOrQueuedMedia) {
            if (!UploadService.isPendingOrInProgressMediaUpload(media)) {
                // it is NOT being uploaded or queued in the actual UploadService, mark it failed
                media.setUploadState(MediaModel.MediaUploadState.FAILED);
                dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
            }
        }
    }

    private void showNotificationForPostWithPendingMedia(PostModel post) {
        mPostUploadNotifier.showForegroundNotificationForPost(post, getString(R.string.uploading_post_media));
    }

    private static synchronized PostModel updatePostWithMediaUrl(PostModel post, MediaModel media,
                                                                 MediaUploadReadyListener processor) {
        if (media != null && post != null && processor != null) {
            // actually replace the media ID with the media uri
            PostModel modifiedPost = processor.replaceMediaFileWithUrlInPost(post, String.valueOf(media.getId()),
                    FluxCUtils.mediaFileFromMediaModel(media));
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

    private static synchronized PostModel updatePostWithFailedMedia(PostModel post, MediaModel media,
                                                                 MediaUploadReadyListener processor) {
        if (media != null && post != null && processor != null) {
            // actually mark the media failed within the Post
            PostModel modifiedPost = processor.markMediaUploadFailedInPost(post, String.valueOf(media.getId()),
                    FluxCUtils.mediaFileFromMediaModel(media));
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

    private synchronized void stopServiceIfUploadsComplete() {
        if (mPostUploadHandler != null && mPostUploadHandler.hasInProgressUploads()) {
            return;
        }

        if (mMediaUploadHandler != null && mMediaUploadHandler.hasInProgressUploads()) {
            return;
        }

        if (!sPostsWithPendingMedia.isEmpty()) {
            return;
        }

        updatePostModelWithCompletedAndFailedUploads();

        AppLog.i(T.MAIN, "UploadService > Completed");
        stopSelf();
    }

    private void updatePostModelWithCompletedAndFailedUploads(){
        List<PostModel> uploadingPosts = mUploadStore.getPendingPosts();
        for (PostModel postModel : uploadingPosts) {
            // For each post with completed media uploads, update the content with the new remote URLs
            // This is done in a batch when all media is complete to prevent conflicts by updating separate images
            // at a time simultaneously for the same post
            PostModel updatedPost = updatePostWithCurrentlyCompletedUploads(postModel);
            // also do the same now with failed uploads
            updatedPost = updatePostWithCurrentlyFailedUploads(updatedPost);
            // finally, save the PostModel
            if (updatedPost != null) {
                mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(updatedPost));
            }
        }
    }

    private void cancelPostUploadMatchingMedia(MediaModel media, String mediaErrorMessage, boolean showErrorNotification) {
        PostModel postToCancel = removeQueuedPostByLocalId(media.getLocalPostId());
        if (postToCancel == null) return;

        SiteModel site = mSiteStore.getSiteByLocalId(postToCancel.getLocalSiteId());
        mPostUploadNotifier.cancelNotification(postToCancel);
        if (showErrorNotification) {
            String message = UploadUtils.getErrorMessage(this, postToCancel, mediaErrorMessage, true);
            mPostUploadNotifier.updateNotificationError(postToCancel, site, message);
        }

        mPostUploadHandler.unregisterPostForAnalyticsTracking(postToCancel);
        EventBus.getDefault().post(new PostEvents.PostUploadCanceled(postToCancel.getLocalSiteId()));
    }

    /**
     * Removes a post from the queued post list given its local ID.
     * @return the post that was removed - if no post was removed, returns null
     */
    private PostModel removeQueuedPostByLocalId(int localPostId) {
        synchronized (sPostsWithPendingMedia) {
            Iterator<UploadingPost> iterator = sPostsWithPendingMedia.iterator();
            while (iterator.hasNext()) {
                PostModel postModel = iterator.next().postModel;
                if (postModel.getId() == localPostId) {
                    iterator.remove();
                    return postModel;
                }
            }
        }
        return null;
    }

    /**
     * Has lower priority than the UploadHandlers, which ensures that the handlers have already received and
     * processed this OnMediaUploaded event. This means we can safely rely on their internal state being up to date.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 7)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.media == null) {
            return;
        }

        if (event.isError()) {
            if (event.media.getLocalPostId() > 0) {
                AppLog.w(T.MAIN, "UploadService > Media upload failed for post " + event.media.getLocalPostId() + " : "
                        + event.error.type + ": " + event.error.message);
                String errorMessage = UploadUtils.getErrorMessageFromMediaError(this, event.media, event.error);
                cancelPostUploadMatchingMedia(event.media, errorMessage, true);
            }
            stopServiceIfUploadsComplete();
            return;
        }

        if (event.canceled) {
            if (event.media.getLocalPostId() > 0) {
                AppLog.i(T.MAIN, "UploadService > Upload cancelled for post with id " + event.media.getLocalPostId()
                        + " - a media upload for this post has been cancelled, id: " + event.media.getId());

                // only show the media upload error notification if the post was not actively cancelled by the user
                boolean showMediaErrorNotification = true;
                for (UploadingPost uploadingPost : sPostsWithPendingMedia) {
                    if (uploadingPost.isCancelled && event.media.getLocalPostId() == uploadingPost.postModel.getId()) {
                        showMediaErrorNotification = false;
                        break;
                    }
                }
                cancelPostUploadMatchingMedia(event.media, getString(R.string.error_media_canceled),
                        showMediaErrorNotification);
            }
            stopServiceIfUploadsComplete();
            return;
        }

        if (event.completed) {
            if (event.media.getLocalPostId() != 0) {
                AppLog.i(T.MAIN, "UploadService > Processing completed media with id " + event.media.getId()
                        + " and local post id " + event.media.getLocalPostId());
                // If this was the last media upload a pending post was waiting for, send it to the PostUploadManager
                synchronized (sPostsWithPendingMedia) {
                    Iterator<UploadingPost> iterator = sPostsWithPendingMedia.iterator();
                    while (iterator.hasNext()) {
                        UploadingPost uploadingPost = iterator.next();
                        if (!UploadService.hasPendingOrInProgressMediaUploadsForPost(uploadingPost.postModel)) {
                            if (uploadingPost.isCancelled) {
                                // Finished all media uploads for a post upload that was cancelled (probably because
                                // it was re-opened in the editor) - we can just remove it from the list now
                                iterator.remove();
                            } else {
                                // Fetch latest version of the post, in case it has been modified elsewhere
                                PostModel latestPost = mPostStore.getPostByLocalPostId(uploadingPost.postModel.getId());

                                if (latestPost != null) {
                                    // Replace local with remote media in the post content
                                    PostModel updatedPost = updatePostWithCurrentlyCompletedUploads(latestPost);
                                    // also do the same now with failed uploads
                                    updatedPost = updatePostWithCurrentlyFailedUploads(updatedPost);
                                    // finally, save the PostModel
                                    if (updatedPost != null) {
                                        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(updatedPost));
                                        // TODO Should do some extra validation here
                                        // e.g. what if the post has local media URLs but no pending media uploads?
                                        mPostUploadHandler.upload(updatedPost);
                                    }
                                }
                                iterator.remove();
                            }
                        }
                    }
                }
            }
            stopServiceIfUploadsComplete();
        }
    }

    /**
     * Has lower priority than the PostUploadHandler, which ensures that the handler has already received and
     * processed this OnPostUploaded event. This means we can safely rely on its internal state being up to date.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 7)
    public void onPostUploaded(OnPostUploaded event) {
        stopServiceIfUploadsComplete();
    }
}
