package org.wordpress.android.ui.uploads;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RemoteAutoSavePost;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.store.UploadStore.ClearMediaPayload;
import org.wordpress.android.ui.media.services.MediaUploadReadyListener;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.ui.posts.PostUtilsWrapper;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class UploadService extends Service {
    private static final String KEY_CHANGE_STATUS_TO_PUBLISH = "shouldPublish";
    private static final String KEY_SHOULD_RETRY = "shouldRetry";
    private static final String KEY_MEDIA_LIST = "mediaList";
    private static final String KEY_UPLOAD_MEDIA_FROM_EDITOR = "mediaFromEditor";
    private static final String KEY_LOCAL_POST_ID = "localPostId";
    private static final String KEY_SHOULD_TRACK_ANALYTICS = "shouldTrackPostAnalytics";

    private static @Nullable UploadService sInstance;

    private MediaUploadHandler mMediaUploadHandler;
    private PostUploadHandler mPostUploadHandler;
    private PostUploadNotifier mPostUploadNotifier;

    // we hold this reference here for the success notification for Media uploads
    private List<MediaModel> mMediaBatchUploaded = new ArrayList<>();

    // we keep this list so we don't tell the user an error happened when we find a FAILED media item
    // for media that the user actively cancelled uploads for
    private static HashSet<String> mUserDeletedMediaItemIds = new HashSet<>();


    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject PostStore mPostStore;
    @Inject SiteStore mSiteStore;
    @Inject UploadStore mUploadStore;
    @Inject SystemNotificationsTracker mSystemNotificationsTracker;
    @Inject PostUtilsWrapper mPostUtilsWrapper;

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        AppLog.i(T.MAIN, "UploadService > Created");
        mDispatcher.register(this);
        sInstance = this;
        // TODO: Recover any posts/media uploads that were interrupted by the service being stopped

        if (mMediaUploadHandler == null) {
            mMediaUploadHandler = new MediaUploadHandler();
        }

        if (mPostUploadNotifier == null) {
            mPostUploadNotifier = new PostUploadNotifier(getApplicationContext(), this, mSystemNotificationsTracker);
        }

        if (mPostUploadHandler == null) {
            mPostUploadHandler = new PostUploadHandler(mPostUploadNotifier);
        }
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
        doFinalProcessingOfPosts(null, null);

        for (PostModel pendingPost : mUploadStore.getPendingPosts()) {
            cancelQueuedPostUpload(pendingPost);
        }

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

// // add local queued media from store
// List<MediaModel> localMedia = mMediaStore.getLocalSiteMedia(site);
// if (localMedia != null && !localMedia.isEmpty()) {
// // uploading is updated to queued, queued media added to the queue, failed media added to completed list
// for (MediaModel mediaItem : localMedia) {
//
// if (MediaUploadState.UPLOADING.name().equals(mediaItem.getUploadState())) {
// mediaItem.setUploadState(MediaUploadState.QUEUED.name());
// mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaItem));
// }
//
// if (MediaUploadState.QUEUED.name().equals(mediaItem.getUploadState())) {
// addUniqueMediaToQueue(mediaItem);
// } else if (MediaUploadState.FAILED.name().equals(mediaItem.getUploadState())) {
// getCompletedItems().add(mediaItem);
// }
// }
// }

        // add new media
        @SuppressWarnings("unchecked")
        List<MediaModel> mediaList = (List<MediaModel>) intent.getSerializableExtra(KEY_MEDIA_LIST);
        if (mediaList != null && !mediaList.isEmpty()) {
            if (!intent.getBooleanExtra(KEY_UPLOAD_MEDIA_FROM_EDITOR, false)) {
                // only cancel the media error notification if we're triggering a new media upload
                // either from Media Browser or a RETRY from a notification.
                // Otherwise, this flag should be true, and we need to keep the error notification as
                // it might be a separate action (user is editing a Post and including media there)
                PostUploadNotifier.cancelFinalNotificationForMedia(this,
                                                                   mSiteStore.getSiteByLocalId(
                                                                           mediaList.get(0).getLocalSiteId()));

                // add these media items so we can use them in WRITE POST once they end up loading successfully
                mMediaBatchUploaded.addAll(mediaList);
            }

            // if this media belongs to some post, register such Post
            registerPostModelsForMedia(mediaList, intent.getBooleanExtra(KEY_SHOULD_RETRY, false));

            for (MediaModel media : mediaList) {
                mMediaUploadHandler.upload(media);
            }
            mPostUploadNotifier.addMediaInfoToForegroundNotification(mediaList);
        }
    }

    private void registerPostModelsForMedia(List<MediaModel> mediaList, boolean isRetry) {
        if (mediaList != null && !mediaList.isEmpty()) {
            Set<PostModel> postsToRefresh = PostUtils.getPostsThatIncludeAnyOfTheseMedia(mPostStore, mediaList);
            for (PostImmutableModel post : postsToRefresh) {
                // If the post is already registered, the new media will be added to its list
                mUploadStore.registerPostModel(post, mediaList);
            }

            if (isRetry) {
                // Bump analytics
                AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_UPLOAD_MEDIA_ERROR_RETRY);

                // send event so Editors can handle clearing Failed statuses properly if Post is being edited right now
                EventBus.getDefault().post(new UploadService.UploadMediaRetryEvent(mediaList));
            }
        }
    }

    private void unpackPostIntent(@NonNull Intent intent) {
        PostModel post = mPostStore.getPostByLocalPostId(intent.getIntExtra(KEY_LOCAL_POST_ID, 0));
        if (post != null) {
            boolean shouldTrackAnalytics = intent.getBooleanExtra(KEY_SHOULD_TRACK_ANALYTICS, false);
            if (shouldTrackAnalytics) {
                mPostUploadHandler.registerPostForAnalyticsTracking(post.getId());
            }

            // cancel any outstanding "end" notification for this Post before we start processing it again
            // i.e. dismiss success or error notification for the post.
            mPostUploadNotifier.cancelFinalNotification(this, post);

            // if the user tapped on the PUBLISH quick action, make this Post publishable and track
            // analytics before starting the upload process.
            if (intent.getBooleanExtra(KEY_CHANGE_STATUS_TO_PUBLISH, false)) {
                SiteModel site = mSiteStore.getSiteByLocalId(post.getLocalSiteId());
                makePostPublishable(post, site);
                PostUtils.trackSavePostAnalytics(post, site);
            }

            if (intent.getBooleanExtra(KEY_SHOULD_RETRY, false)) {
                if (AppPrefs.isAztecEditorEnabled() || AppPrefs.isGutenbergEditorEnabled()) {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        rebuildNotificationError(post, getString(R.string.no_network_message));
                        return;
                    }
                    boolean postHasGutenbergBlocks = PostUtils.contentContainsGutenbergBlocks(post.getContent());
                    retryUpload(post, !postHasGutenbergBlocks);
                } else {
                    ToastUtils.showToast(this, R.string.retry_needs_aztec);
                }
                return;
            }

            // is this a new post? only add count to the notification when the post is totally new
            // i.e. it still doesn't have any tracked state in the UploadStore
            // or it's a failed one the user is actively retrying.
            if (isThisPostTotallyNewOrFailed(post) && !PostUploadHandler.isPostUploadingOrQueued(post)) {
                mPostUploadNotifier.addPostInfoToForegroundNotification(post, null);
            }


            if (!getAllFailedMediaForPost(post).isEmpty()) {
                boolean postHasGutenbergBlocks = PostUtils.contentContainsGutenbergBlocks(post.getContent());
                retryUpload(post, !postHasGutenbergBlocks);
            } else if (hasPendingOrInProgressMediaUploadsForPost(post)) {
                // Register the post (as PENDING) in the UploadStore, along with all media currently in progress for it
                // If the post is already registered, the new media will be added to its list
                List<MediaModel> activeMedia = MediaUploadHandler.getPendingOrInProgressMediaUploadsForPost(post);
                mUploadStore.registerPostModel(post, activeMedia);
            } else {
                mPostUploadHandler.upload(post);
            }
        }
    }

    public static void cancelFinalNotification(Context context, PostImmutableModel post) {
        // cancel any outstanding "end" notification for this Post before we start processing it again
        // i.e. dismiss success or error notification for the post.
        PostUploadNotifier.cancelFinalNotification(context, post);
    }

    public static void cancelFinalNotificationForMedia(Context context, SiteModel site) {
        PostUploadNotifier.cancelFinalNotificationForMedia(context, site);
    }

    /**
     * Do not use this method unless the user explicitly confirmed changes - eg. clicked on publish button or
     * similar.
     */
    private void makePostPublishable(@NonNull PostModel post, SiteModel site) {
        PostUtils.preparePostForPublish(post, site);
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));
    }

    private boolean isThisPostTotallyNewOrFailed(PostImmutableModel post) {
        // if we have any tracks for this Post's UploadState, this means this Post is not new.
        // Conditions under which the UploadStore would contain traces of this Post's UploadState are:
        // - it's been cancelled by entering/exiting/entering the editor thus cancelling the queued post upload
        // to allow for the user to keep editing it before sending to the server
        // - it's a failed upload (due to some network issue, for example)
        // - it's a pending upload (it is currently registered for upload once the associated media finishes
        // uploading).
        return !mUploadStore.isRegisteredPostModel(post) || (mUploadStore.isFailedPost(post) || mUploadStore
                .isPendingPost(post));
    }

    public static Intent getRetryUploadServiceIntent(Context context, @NonNull PostImmutableModel post,
                                                     boolean trackAnalytics) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(KEY_LOCAL_POST_ID, post.getId());
        intent.putExtra(KEY_SHOULD_TRACK_ANALYTICS, trackAnalytics);
        intent.putExtra(KEY_SHOULD_RETRY, true);
        return intent;
    }

    /**
     * Do not use this method unless the user explicitly confirmed changes - eg. clicked on publish button or
     * similar.
     *
     * The only valid use-case for this method I can think of right know is when we want to put the intent into a
     * PendingIntent - eg. publish action on a notification. If you want to start the upload right away use
     * UploadUtils.publishPost(..) instead.
     */
    public static Intent getPublishPostServiceIntent(Context context, @NonNull PostImmutableModel post,
                                                     boolean trackAnalytics) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(KEY_LOCAL_POST_ID, post.getId());
        intent.putExtra(KEY_SHOULD_TRACK_ANALYTICS, trackAnalytics);
        intent.putExtra(KEY_CHANGE_STATUS_TO_PUBLISH, true);
        return intent;
    }

    public static Intent getUploadMediaServiceIntent(Context context, @NonNull ArrayList<MediaModel> mediaList,
                                                     boolean isRetry) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(UploadService.KEY_MEDIA_LIST, mediaList);
        intent.putExtra(KEY_SHOULD_RETRY, isRetry);
        return intent;
    }

    /**
     * Adds a post to the queue.
     * @param postId
     * @param isFirstTimePublish true when its status changes from local draft or remote draft to published.
     */
    public static void uploadPost(Context context, int postId, boolean isFirstTimePublish) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(KEY_LOCAL_POST_ID, postId);
        intent.putExtra(KEY_SHOULD_TRACK_ANALYTICS, isFirstTimePublish);
        context.startService(intent);
    }

    public static void uploadMedia(Context context, @NonNull MediaModel media) {
        ArrayList<MediaModel> list = new ArrayList<>();
        list.add(media);

        uploadMedia(context, list);
    }

    public static void uploadMedia(Context context, @NonNull ArrayList<MediaModel> mediaList) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(UploadService.KEY_MEDIA_LIST, mediaList);
        context.startService(intent);
    }

    public static void uploadMediaFromEditor(Context context, @NonNull ArrayList<MediaModel> mediaList) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(UploadService.KEY_MEDIA_LIST, mediaList);
        intent.putExtra(UploadService.KEY_UPLOAD_MEDIA_FROM_EDITOR, true);
        context.startService(intent);
    }

    /**
     * Returns true if the passed post is either currently uploading or waiting to be uploaded.
     * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
     * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
     */
    public static boolean isPostUploadingOrQueued(PostImmutableModel post) {
        UploadService instance = sInstance;
        if (instance == null || post == null) {
            return false;
        }

        // First check for posts uploading or queued inside the PostUploadManager
        if (PostUploadHandler.isPostUploadingOrQueued(post)) {
            return true;
        }

        // Then check the list of posts waiting for media to complete
        return instance.mUploadStore.isPendingPost(post);
    }

    public static boolean isPostQueued(PostImmutableModel post) {
        // Check for posts queued inside the PostUploadManager
        return sInstance != null && PostUploadHandler.isPostQueued(post);
    }

    /**
     * Returns true if the passed post is currently uploading.
     * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
     * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
     */
    public static boolean isPostUploading(PostImmutableModel post) {
        return sInstance != null && PostUploadHandler.isPostUploading(post);
    }

    public static void cancelQueuedPostUploadAndRelatedMedia(Context context, PostModel post) {
        if (post != null) {
            if (sInstance != null) {
                PostUploadNotifier.cancelFinalNotification(sInstance, post);
                sInstance.mPostUploadNotifier.removePostInfoFromForegroundNotification(
                        post, sInstance.mMediaStore.getMediaForPost(post));
            } else {
                PostUploadNotifier.cancelFinalNotification(context, post);
            }
            cancelQueuedPostUpload(post);
            EventBus.getDefault().post(new PostEvents.PostMediaCanceled(post));
        }
    }

    public static void cancelQueuedPostUpload(PostModel post) {
        if (sInstance != null && post != null) {
            // Mark the post as CANCELLED in the UploadStore
            sInstance.mDispatcher.dispatch(UploadActionBuilder.newCancelPostAction(post));
        }
    }

    public static PostModel updatePostWithCurrentlyCompletedUploads(PostModel post) {
        if (post != null && sInstance != null) {
            // now get the list of completed media for this post, so we can make post content
            // updates in one go and save only once
            MediaUploadReadyListener processor = new MediaUploadReadyProcessor();
            Set<MediaModel> completedMedia = sInstance.mUploadStore.getCompletedMediaForPost(post);
            for (MediaModel media : completedMedia) {
                if (media.getMarkedLocallyAsFeatured()) {
                    post = updatePostWithNewFeaturedImg(post, media.getMediaId());
                } else {
                    post = updatePostWithMediaUrl(post, media, processor);
                }
            }

            if (completedMedia != null && !completedMedia.isEmpty()) {
                // finally remove all completed uploads for this post, as they've been taken care of
                ClearMediaPayload clearMediaPayload = new ClearMediaPayload(post, completedMedia);
                sInstance.mDispatcher.dispatch(UploadActionBuilder.newClearMediaForPostAction(clearMediaPayload));
            }
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

    public static boolean hasInProgressMediaUploadsForPost(PostImmutableModel postModel) {
        return postModel != null && MediaUploadHandler.hasInProgressMediaUploadsForPost(postModel.getId());
    }

    public static boolean hasPendingMediaUploadsForPost(PostImmutableModel postModel) {
        return postModel != null && MediaUploadHandler.hasPendingMediaUploadsForPost(postModel.getId());
    }

    public static boolean hasPendingOrInProgressMediaUploadsForPost(PostImmutableModel postModel) {
        return postModel != null && MediaUploadHandler.hasPendingOrInProgressMediaUploadsForPost(postModel.getId());
    }

    public static MediaModel getPendingOrInProgressFeaturedImageUploadForPost(PostImmutableModel postModel) {
        return MediaUploadHandler.getPendingOrInProgressFeaturedImageUploadForPost(postModel);
    }

    public static List<MediaModel> getPendingOrInProgressMediaUploadsForPost(PostImmutableModel post) {
        return MediaUploadHandler.getPendingOrInProgressMediaUploadsForPost(post);
    }

    public static float getMediaUploadProgressForPost(PostModel postModel) {
        UploadService instance = sInstance;
        if (postModel == null || instance == null) {
            // If the UploadService isn't running, there's no progress for this post
            return 0;
        }

        Set<MediaModel> pendingMediaList = instance.mUploadStore.getUploadingMediaForPost(postModel);

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
        UploadService instance = sInstance;
        if (mediaModel == null || instance == null) {
            // If the UploadService isn't running, there's no progress for this media
            return 0;
        }

        float uploadProgress = instance.mUploadStore.getUploadProgressForMedia(mediaModel);

        // If this is a video and video optimization is enabled, include the optimization progress in the outcome
        if (mediaModel.isVideo() && WPMediaUtils.isVideoOptimizationEnabled()) {
            return MediaUploadHandler.getOverallProgressForVideo(mediaModel.getId(), uploadProgress);
        }

        return uploadProgress;
    }

    public static @NonNull
    Set<MediaModel> getPendingMediaForPost(PostModel postModel) {
        UploadService instance = sInstance;
        if (postModel == null || instance == null) {
            return Collections.emptySet();
        }
        return instance.mUploadStore.getUploadingMediaForPost(postModel);
    }

    public static boolean isPendingOrInProgressMediaUpload(@NonNull MediaModel media) {
        return MediaUploadHandler.isPendingOrInProgressMediaUpload(media.getId());
    }

    /**
     * Rechecks all media in the MediaStore marked UPLOADING/QUEUED against the UploadingService to see
     * if it's actually uploading or queued and change it accordingly, to recover from an inconsistent state
     */
    public static void sanitizeMediaUploadStateForSite(@NonNull MediaStore mediaStore, @NonNull Dispatcher dispatcher,
                                                       @NonNull SiteModel site) {
        List<MediaModel> uploadingMedia =
                mediaStore.getSiteMediaWithState(site, MediaUploadState.UPLOADING);
        List<MediaModel> queuedMedia =
                mediaStore.getSiteMediaWithState(site, MediaUploadState.QUEUED);

        if (uploadingMedia.isEmpty() && queuedMedia.isEmpty()) {
            return;
        }

        List<MediaModel> uploadingOrQueuedMedia = new ArrayList<>();
        uploadingOrQueuedMedia.addAll(uploadingMedia);
        uploadingOrQueuedMedia.addAll(queuedMedia);

        for (final MediaModel media : uploadingOrQueuedMedia) {
            if (!UploadService.isPendingOrInProgressMediaUpload(media)) {
                // it is NOT being uploaded or queued in the actual UploadService, mark it failed
                media.setUploadState(MediaUploadState.FAILED);
                dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
            }
        }
    }

    private static synchronized PostModel updatePostWithNewFeaturedImg(PostModel post, Long remoteMediaId) {
        if (post != null && remoteMediaId != null) {
            boolean changesConfirmed = post.contentHashcode() == post.getChangesConfirmedContentHashcode();
            post.setFeaturedImageId(remoteMediaId);
            post.setIsLocallyChanged(true);
            post.setDateLocallyChanged(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000));
            if (changesConfirmed) {
                /*
                 * We are replacing local featured image with a remote version. We need to make sure
                 * to retain the confirmation state.
                 */
                post.setChangesConfirmedContentHashcode(post.contentHashcode());
            }
        }
        return post;
    }
    private static synchronized PostModel updatePostWithMediaUrl(PostModel post, MediaModel media,
                                                                 MediaUploadReadyListener processor) {
        if (media != null && post != null && processor != null && sInstance != null) {
            boolean changesConfirmed = post.contentHashcode() == post.getChangesConfirmedContentHashcode();

            // obtain site url used to generate attachment page url
            SiteModel site = sInstance.mSiteStore.getSiteByLocalId(media.getLocalSiteId());

            if (PostUtils.contentContainsWPStoryGutenbergBlocks(post.getContent())) {
                processor.replaceMediaLocalIdWithRemoteMediaIdInPost(
                        post, FluxCUtils.mediaFileFromMediaModel(media));
            } else {
                // actually replace the media ID with the media uri
                processor.replaceMediaFileWithUrlInPost(post, String.valueOf(media.getId()),
                        FluxCUtils.mediaFileFromMediaModel(media), site.getUrl());
            }

            // we changed the post, so let’s mark this down
            if (!post.isLocalDraft()) {
                post.setIsLocallyChanged(true);
            }
            post.setDateLocallyChanged(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000));
            if (changesConfirmed) {
                /*
                 * We are replacing image local path with a url. We need to make sure to retain the confirmation
                 * state.
                 */
                post.setChangesConfirmedContentHashcode(post.contentHashcode());
            }
        }
        return post;
    }

    private static synchronized PostModel updatePostWithFailedMedia(PostModel post, MediaModel media,
                                                                    MediaUploadReadyListener processor) {
        if (media != null && post != null && processor != null) {
            boolean changesConfirmed = post.contentHashcode() == post.getChangesConfirmedContentHashcode();
            // actually mark the media failed within the Post
            processor.markMediaUploadFailedInPost(post, String.valueOf(media.getId()),
                    FluxCUtils.mediaFileFromMediaModel(media));

            // we changed the post, so let’s mark this down
            if (!post.isLocalDraft()) {
                post.setIsLocallyChanged(true);
            }
            post.setDateLocallyChanged(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000));
            if (changesConfirmed) {
                /*
                 * We are updating media upload status, but we don't make any undesired changes to the post. We need to
                 * make sure to retain the confirmation state.
                 */
                post.setChangesConfirmedContentHashcode(post.contentHashcode());
            }
        }
        return post;
    }

    private synchronized void stopServiceIfUploadsComplete() {
        stopServiceIfUploadsComplete(null, null);
    }


    private synchronized void stopServiceIfUploadsComplete(Boolean isError, PostModel post) {
        if (mPostUploadHandler != null && mPostUploadHandler.hasInProgressUploads()) {
            return;
        }

        if (mMediaUploadHandler != null && mMediaUploadHandler.hasInProgressUploads()) {
            return;
        } else {
            verifyMediaOnlyUploadsAndNotify();
        }

        if (doFinalProcessingOfPosts(isError, post)) {
            // when more Posts have been re-enqueued, don't stop the service just yet.
            return;
        }

        if (!mUploadStore.getPendingPosts().isEmpty()) {
            return;
        }

        AppLog.i(T.MAIN, "UploadService > Completed");
        stopSelf();
    }


    private void verifyMediaOnlyUploadsAndNotify() {
        // check if all are successful uploads, then notify the user about it
        if (!mMediaBatchUploaded.isEmpty()) {
            ArrayList<MediaModel> standAloneMediaItems = new ArrayList<>();
            for (MediaModel media : mMediaBatchUploaded) {
                // we need to obtain the latest copy from the Store, as it's got the remote mediaId field
                MediaModel currentMedia = mMediaStore.getMediaWithLocalId(media.getId());
                if (currentMedia != null && currentMedia.getLocalPostId() == 0
                    && MediaUploadState.fromString(currentMedia.getUploadState())
                       == MediaUploadState.UPLOADED) {
                    standAloneMediaItems.add(currentMedia);
                }
            }

            if (!standAloneMediaItems.isEmpty()) {
                SiteModel site = mSiteStore.getSiteByLocalId(standAloneMediaItems.get(0).getLocalSiteId());
                mPostUploadNotifier.updateNotificationSuccessForMedia(standAloneMediaItems, site);
                mMediaBatchUploaded.clear();
            }
        }
    }

    private PostModel updateOnePostModelWithCompletedAndFailedUploads(PostModel postModel) {
        PostModel updatedPost = updatePostWithCurrentlyCompletedUploads(postModel);
        // also do the same now with failed uploads
        updatedPost = updatePostWithCurrentlyFailedUploads(updatedPost);
        // finally, save the PostModel
        if (updatedPost != null) {
            mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(updatedPost));
        }
        return updatedPost;
    }

    private boolean mediaBelongsToAPost(MediaModel media) {
        PostModel postToCancel = mPostStore.getPostByLocalPostId(media.getLocalPostId());
        return (postToCancel != null && mUploadStore.isRegisteredPostModel(postToCancel));
    }

    /*
        returns true if Post canceled
        returns false if Post can't be found or is not registered in the UploadStore
     */
    private boolean cancelPostUploadMatchingMedia(@NonNull MediaModel media, String errorMessage, boolean showError) {
        PostModel postToCancel = mPostStore.getPostByLocalPostId(media.getLocalPostId());
        if (postToCancel == null) {
            return false;
        }

        if (!mUploadStore.isRegisteredPostModel(postToCancel)) {
            return false;
        }

        if (PostUploadHandler.isPostUploadingOrQueued(postToCancel) && !PostUtils
                .isPostCurrentlyBeingEdited(postToCancel)) {
            // post is not being edited and is currently queued, update the count on the foreground notification
            mPostUploadNotifier.incrementUploadedPostCountFromForegroundNotification(postToCancel);
        }

        if (showError || mUploadStore.isFailedPost(postToCancel)) {
            // Only show the media upload error notification if the post is NOT registered in the UploadStore
            // - otherwise if it IS registered in the UploadStore and we get a `cancelled` signal it means
            // the user actively cancelled it. No need to show an error then.
            String message = UploadUtils.getErrorMessage(this, postToCancel.isPage(), errorMessage, true);
            SiteModel site = mSiteStore.getSiteByLocalId(postToCancel.getLocalSiteId());
            if (site != null) {
                mPostUploadNotifier.updateNotificationErrorForPost(postToCancel, site, message,
                        mUploadStore.getFailedMediaForPost(postToCancel).size());
            } else {
                AppLog.e(T.POSTS, "Trying to update notifications with missing site");
            }
        }

        mPostUploadHandler.unregisterPostForAnalyticsTracking(postToCancel.getId());
        EventBus.getDefault().post(new PostEvents.PostUploadCanceled(postToCancel));

        return true;
    }

    private void rebuildNotificationError(PostModel post, String errorMessage) {
        Set<MediaModel> failedMedia = mUploadStore.getFailedMediaForPost(post);
        mPostUploadNotifier.setTotalMediaItems(post, failedMedia.size());
        SiteModel site = mSiteStore.getSiteByLocalId(post.getLocalSiteId());
        if (site != null) {
            mPostUploadNotifier.updateNotificationErrorForPost(post, site, errorMessage, 0);
        } else {
            AppLog.e(T.POSTS, "Trying to rebuild notification error without a site");
        }
    }

    private void aztecRegisterFailedMediaForThisPost(PostModel post) {
        // there could be failed media in the post, that has not been registered in the UploadStore because
        // the media was being uploaded separately (i.e. the user included media, started uploading within
        // the editor, and such media failed _before_ exiting the eidtor, thus the registration never happened.
        // We're recovering the information here so we make sure to rebuild the status only when the user taps
        // on Retry.
        List<String> mediaIds = AztecEditorFragment.getMediaMarkedFailedInPostContent(this, post.getContent());

        if (mediaIds != null && !mediaIds.isEmpty()) {
            ArrayList<MediaModel> mediaList = new ArrayList<>();
            for (String mediaId : mediaIds) {
                MediaModel media = mMediaStore.getMediaWithLocalId(StringUtils.stringToInt(mediaId));
                if (media != null) {
                    mediaList.add(media);
                    // if this media item didn't have the Postid set, let's set it as we found it
                    // in the Post body anyway. So let's fix that now.
                    if (media.getLocalPostId() == 0) {
                        media.setLocalPostId(post.getId());
                        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
                    }
                }
            }

            if (!mediaList.isEmpty()) {
                // given we found failed media within this Post, let's also cancel the media error
                mPostUploadNotifier
                        .cancelFinalNotificationForMedia(this, mSiteStore.getSiteByLocalId(post.getLocalSiteId()));

                // now we have a list. Let' register this list.
                mUploadStore.registerPostModel(post, mediaList);
            }
        }
    }

    private void retryUpload(PostModel post, boolean processWithAztec) {
        if (mUploadStore.isPendingPost(post)) {
            // The post is already pending upload so there is no need to manually retry it. Actually, the retry might
            // result in the post being uploaded without its media. As if the media upload is in progress, the
            // `getAllFailedMediaForPost()` methods returns an empty set. If we invoke `mPostUploadHandler.upload()`
            // the post will be uploaded ignoring its media (we could upload content with paths to local storage).
            return;
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_UPLOAD_POST_ERROR_RETRY);

        if (processWithAztec) {
            aztecRegisterFailedMediaForThisPost(post);
        }

        List<MediaModel> mediaToRetry = getAllFailedMediaForPost(post);

        if (!mediaToRetry.isEmpty()) {
            // reset these media items to QUEUED
            for (MediaModel media : mediaToRetry) {
                media.setUploadState(MediaUploadState.QUEUED);
                mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
            }

            if (processWithAztec) {
                boolean changesConfirmed = post.contentHashcode() == post.getChangesConfirmedContentHashcode();

                // do the same within the Post content itself
                String postContentWithRestartedUploads =
                        AztecEditorFragment.restartFailedMediaToUploading(this, post.getContent());
                post.setContent(postContentWithRestartedUploads);
                if (changesConfirmed) {
                    /*
                     * We are updating media upload status, but we don't make any undesired changes to the post. We
                     * need to make sure to retain the confirmation state.
                     */
                    post.setChangesConfirmedContentHashcode(post.contentHashcode());
                }
                mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));
            }

            // retry uploading media items
            for (MediaModel media : mediaToRetry) {
                mMediaUploadHandler.upload(media);
            }

            // Register the post (as PENDING) in the UploadStore, along with all media currently in progress for it
            // If the post is already registered, the new media will be added to its list
            mUploadStore.registerPostModel(post, mediaToRetry);
            mPostUploadNotifier.addPostInfoToForegroundNotification(post, mediaToRetry);

            // send event so Editors can handle clearing Failed statuses properly if Post is being edited right now
            EventBus.getDefault().post(new UploadService.UploadMediaRetryEvent(mediaToRetry));
        } else {
            mPostUploadNotifier.addPostInfoToForegroundNotification(post, null);
            // retry uploading the Post
            mPostUploadHandler.upload(post);
        }
    }

    private List<MediaModel> getAllFailedMediaForPost(PostModel postModel) {
        Set<MediaModel> failedMedia = mUploadStore.getFailedMediaForPost(postModel);
        return filterOutRecentlyDeletedMedia(failedMedia);
    }

    private List<MediaModel> filterOutRecentlyDeletedMedia(Set<MediaModel> failedMedia) {
        List<MediaModel> mediaToRetry = new ArrayList<>();
        for (MediaModel mediaModel : failedMedia) {
            String mediaIdToCompare = String.valueOf(mediaModel.getId());
            if (!mUserDeletedMediaItemIds.contains(mediaIdToCompare)) {
                mediaToRetry.add(mediaModel);
            }
        }
        return mediaToRetry;
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


            if (!mediaBelongsToAPost(event.media)) {
                // this media item doesn't belong to a Post
                mPostUploadNotifier.incrementUploadedMediaCountFromProgressNotification(event.media.getId());
                // Only show the media upload error notification if the post is NOT registered in the UploadStore
                // - otherwise if it IS registered in the UploadStore and we get a `cancelled` signal it means
                // the user actively cancelled it. No need to show an error then.
                String message = UploadUtils.getErrorMessageFromMediaError(this, event.media, event.error);

                // if media has a local site id, use that. If not, default to currently selected site.
                int siteLocalId = event.media.getLocalSiteId() > 0 ? event.media.getLocalSiteId()
                        : AppPrefs.getSelectedSite();
                SiteModel selectedSite = mSiteStore.getSiteByLocalId(siteLocalId);

                List<MediaModel> failedStandAloneMedia = getRetriableStandaloneMedia(selectedSite);
                if (failedStandAloneMedia.isEmpty()) {
                    // if we couldn't get the failed media from the MediaStore, at least we know
                    // for sure we're handling the event for this specific media item, so throw an error
                    // notification for this particular media item travelling in event.media
                    failedStandAloneMedia.add(event.media);
                }

                mPostUploadNotifier.updateNotificationErrorForMedia(failedStandAloneMedia,
                        selectedSite, message);
            }
            stopServiceIfUploadsComplete();
            return;
        }

        if (event.canceled) {
            // remove this media item from the progress notification
            if (sInstance != null) {
                sInstance.mPostUploadNotifier.removeOneMediaItemInfoFromForegroundNotification();
            }
            if (event.media.getLocalPostId() > 0) {
                AppLog.i(T.MAIN, "UploadService > Upload cancelled for post with id " + event.media.getLocalPostId()
                                 + " - a media upload for this post has been cancelled, id: " + event.media.getId());
                cancelPostUploadMatchingMedia(event.media, getString(R.string.error_media_canceled), false);
            }
            stopServiceIfUploadsComplete();
            return;
        }

        if (event.completed) {
            if (event.media.getLocalPostId() != 0) {
                AppLog.i(T.MAIN, "UploadService > Processing completed media with id " + event.media.getId()
                                 + " and local post id " + event.media.getLocalPostId());
            }
            mPostUploadNotifier.incrementUploadedMediaCountFromProgressNotification(event.media.getId());
            stopServiceIfUploadsComplete();
        } else {
            // in-progress upload
            // Progress update
            mPostUploadNotifier.updateNotificationProgressForMedia(event.media, event.progress);
        }
    }

    /*
     * This method will make sure to keep the bodies of all Posts registered (*) in the UploadStore
     * up-to-date with their corresponding media item upload statuses (i.e. marking them failed or
     * successfully uploaded in the actual Post content to reflect what the UploadStore says).
     *
     * Finally, it will either cancel the Post upload from the queue and create an error notification
     * for the user if there are any failed media items for such a Post, or upload the Post if it's
     * in good shape.
     *
     * This method returns:
     * - `false` if all registered posts have no in-progress items, and at least one or more retriable
     * (failed) items are found in them (this, in other words, means all registered posts are found
     * in a `finalized` state other than "UPLOADED").
     * - `true` if at least one registered Post is found that is in good conditions to be uploaded.
     *
     *
     * (*)`Registered` posts are posts that had media in them and are waiting to be uploaded once
     * their corresponding associated media is uploaded first.
     */
    private boolean doFinalProcessingOfPosts(Boolean isError, PostModel post) {
        // If this was the last media upload a post was waiting for, update the post content
        // This done for pending as well as cancelled and failed posts
        for (PostModel postModel : mUploadStore.getAllRegisteredPosts()) {
            if (mPostUtilsWrapper.isPostCurrentlyBeingEdited(postModel)) {
                // Don't upload a Post that is being currently open in the Editor.
                // This fixes the issue on self-hosted sites when you have a queued post which couldn't be
                // remote autosaved. When you try to leave the editor without saving it will get stuck in queued
                // upload state. In case of the post still being edited we cancel any ongoing upload post action.
                mDispatcher.dispatch(UploadActionBuilder.newCancelPostAction(post));
                continue;
            }

            if (!UploadService.hasPendingOrInProgressMediaUploadsForPost(postModel)) {
                // Replace local with remote media in the post content
                PostModel updatedPost = updateOnePostModelWithCompletedAndFailedUploads(postModel);
                if (updatedPost != null) {
                    // here let's check if there are any failed media
                    Set<MediaModel> failedMedia = mUploadStore.getFailedMediaForPost(postModel);
                    if (!failedMedia.isEmpty()) {
                        // this Post has failed media, don't upload it just yet,
                        // but tell the user about the error
                        cancelQueuedPostUpload(postModel);

                        // update error notification for Post, unless the media is in the user-deleted media set
                        if (!isAllFailedMediaUserDeleted(failedMedia)) {
                            SiteModel site = mSiteStore.getSiteByLocalId(postModel.getLocalSiteId());
                            String message = UploadUtils
                                    .getErrorMessage(this, postModel.isPage(), getString(R.string.error_generic_error),
                                            true);
                            if (site != null) {
                                mPostUploadNotifier.updateNotificationErrorForPost(postModel, site, message, 0);
                            } else {
                                AppLog.e(T.POSTS, "Error notification cannot be updated without a post");
                            }
                        }

                        mPostUploadHandler.unregisterPostForAnalyticsTracking(postModel.getId());
                        EventBus.getDefault().post(
                                new PostEvents.PostUploadCanceled(postModel));
                    } else {
                        // Do not re-enqueue a post that has already failed
                        if (isError != null && isError && mUploadStore.isFailedPost(updatedPost)) {
                            continue;
                        }
                        // TODO Should do some extra validation here
                        // e.g. what if the post has local media URLs but no pending media uploads?
                        mPostUploadHandler.upload(updatedPost);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isAllFailedMediaUserDeleted(Set<MediaModel> failedMediaSet) {
        if (failedMediaSet != null && failedMediaSet.size() == mUserDeletedMediaItemIds.size()) {
            int numberOfMatches = 0;
            for (MediaModel media : failedMediaSet) {
                String mediaIdToCompare = String.valueOf(media.getId());
                if (mUserDeletedMediaItemIds.contains(mediaIdToCompare)) {
                    numberOfMatches++;
                }
            }

            if (numberOfMatches == mUserDeletedMediaItemIds.size()) {
                return true;
            }
        }
        return false;
    }

    public static void setDeletedMediaItemIds(List<String> mediaIds) {
        mUserDeletedMediaItemIds.clear();
        mUserDeletedMediaItemIds.addAll(mediaIds);
    }

    private List<MediaModel> getRetriableStandaloneMedia(SiteModel selectedSite) {
        // get all retriable media ? To retry or not to retry, that is the question
        List<MediaModel> failedMedia = null;
        List<MediaModel> failedStandAloneMedia = new ArrayList<>();
        if (selectedSite != null) {
            failedMedia = mMediaStore.getSiteMediaWithState(
                    selectedSite, MediaUploadState.FAILED);
        }

        // only take into account those media items that do not belong to any Post
        for (MediaModel media : failedMedia) {
            if (media.getLocalPostId() == 0) {
                failedStandAloneMedia.add(media);
            }
        }

        return failedStandAloneMedia;
    }

    /**
     * Has lower priority than the PostUploadHandler, which ensures that the handler has already received and
     * processed this OnPostUploaded event. This means we can safely rely on its internal state being up to date.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 7)
    public void onPostUploaded(OnPostUploaded event) {
        stopServiceIfUploadsComplete(event.isError(), event.post);
    }

    /**
     * Has lower priority than the PostUploadHandler, which ensures that the handler has already received and
     * processed this OnPostChanged event. This means we can safely rely on its internal state being up to date.
     */
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 7)
    public void onPostChanged(OnPostChanged event) {
        if (event.causeOfChange instanceof CauseOfOnPostChanged.RemoteAutoSavePost) {
            PostModel post =
                    mPostStore.getPostByLocalPostId(((RemoteAutoSavePost) event.causeOfChange).getLocalPostId());
            stopServiceIfUploadsComplete(event.isError(), post);
        }
    }

    public static class UploadErrorEvent {
        public final PostModel post;
        public final List<MediaModel> mediaModelList;
        public final String errorMessage;

        UploadErrorEvent(PostModel post, String errorMessage) {
            this.post = post;
            this.mediaModelList = null;
            this.errorMessage = errorMessage;
        }

        UploadErrorEvent(List<MediaModel> mediaModelList, String errorMessage) {
            this.post = null;
            this.mediaModelList = mediaModelList;
            this.errorMessage = errorMessage;
        }
    }

    public static class UploadMediaSuccessEvent {
        public final List<MediaModel> mediaModelList;
        public final String successMessage;

        UploadMediaSuccessEvent(List<MediaModel> mediaModelList, String successMessage) {
            this.mediaModelList = mediaModelList;
            this.successMessage = successMessage;
        }
    }

    public static class UploadMediaRetryEvent {
        public final List<MediaModel> mediaModelList;

        UploadMediaRetryEvent(List<MediaModel> mediaModelList) {
            this.mediaModelList = mediaModelList;
        }
    }
}
