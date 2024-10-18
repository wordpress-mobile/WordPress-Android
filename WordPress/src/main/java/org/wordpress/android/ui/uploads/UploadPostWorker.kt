package org.wordpress.android.ui.uploads

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.editor.AztecEditorFragment
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.generated.UploadActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.PostUploadModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.UploadSqlUtils
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.media.services.MediaUploadReadyListener
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.POSTS
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.UploadWorker
import org.wordpress.android.util.WPMediaUtils

class UploadPostWorker(
    val appContext: Context,
    workerParameters: WorkerParameters,
    private val siteStore: SiteStore,
    private val postStore: PostStore,
    private val uploadStore: UploadStore,
    private val mediaStore: MediaStore,
    private val dispatcher: Dispatcher,
    private val systemNotificationsTracker: SystemNotificationsTracker,
    private val postUtilsWrapper: PostUtilsWrapper
) : CoroutineWorker(appContext, workerParameters) {
    private lateinit var mediaUploadHandler: MediaUploadHandler
    private lateinit var postUploadHandler: PostUploadHandler
    private lateinit var postUploadNotifier: PostUploadNotifier
    private lateinit var uploadService: UploadService // temporarily replace UploadService in PostUploadNotifier

    init {
        instance = this
        dispatcher.register(this)
    }

    override suspend fun doWork(): Result {
        return try {
            unpackPostIntent()

            Result.success()
        } catch (e: Exception) {
            AppLog.e(POSTS, "Post upload failed: ", e)
            Result.failure()
        }
    }

    class Factory(
        private val siteStore: SiteStore,
        private val postStore: PostStore,
        private val uploadStore: UploadStore,
        private val mediaStore: MediaStore,
        private val dispatcher: Dispatcher,
        private val systemNotificationsTracker: SystemNotificationsTracker,
        private val postUtilsWrapper: PostUtilsWrapper
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return if (workerClassName == UploadPostWorker::class.java.name) {
                UploadPostWorker(
                    appContext,
                    workerParameters,
                    siteStore,
                    postStore,
                    uploadStore,
                    mediaStore,
                    dispatcher,
                    systemNotificationsTracker,
                    postUtilsWrapper
                )
            } else {
                null
            }
        }
    }

    /**
     * Has lower priority than the PostUploadHandler, which ensures that the handler has already received and
     * processed this OnPostUploaded event. This means we can safely rely on its internal state being up to date.
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 7)
    fun onPostUploaded(event: OnPostUploaded) {
        stopServiceIfUploadsComplete(event.isError, event.post)
    }

    /**
     * Has lower priority than the PostUploadHandler, which ensures that the handler has already received and
     * processed this OnPostChanged event. This means we can safely rely on its internal state being up to date.
     */
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 7)
    fun onPostChanged(event: OnPostChanged) {
        if (event.causeOfChange is CauseOfOnPostChanged.RemoteAutoSavePost) {
            val post =
                postStore.getPostByLocalPostId((event.causeOfChange as CauseOfOnPostChanged.RemoteAutoSavePost).localPostId)
            stopServiceIfUploadsComplete(event.isError, post)
        }
    }

    @Synchronized
    private fun stopServiceIfUploadsComplete(isError: Boolean?, post: PostModel?) {
        if (postUploadHandler.hasInProgressUploads()) {
            return
        }
        if (mediaUploadHandler.hasInProgressUploads()) {
            return
        } else {
            verifyMediaOnlyUploadsAndNotify()
        }
        if (doFinalProcessingOfPosts(isError, post)) {
            // when more Posts have been re-enqueued, don't stop the service just yet.
            return
        }
        if (uploadStore.getPendingPosts().isNotEmpty()) {
            return
        }
        AppLog.i(AppLog.T.MAIN, "UploadService > Completed")
//        stopSelf()
    }

    private fun unpackPostIntent() {
        mediaUploadHandler = MediaUploadHandler()
        // replace UploadService in PostUploadNotifier
        uploadService = UploadService()
        postUploadNotifier = PostUploadNotifier(applicationContext, uploadService, systemNotificationsTracker)
        postUploadHandler = PostUploadHandler(postUploadNotifier)

        val post = postStore.getPostByLocalPostId(inputData.getInt(KEY_LOCAL_POST_ID, 0))
        if (post != null) {
            val shouldTrackAnalytics = inputData.getBoolean(KEY_SHOULD_TRACK_ANALYTICS, false)
            if (shouldTrackAnalytics) {
                postUploadHandler.registerPostForAnalyticsTracking(post.id)
            }

            // cancel any outstanding "end" notification for this Post before we start processing it again
            // i.e. dismiss success or error notification for the post.
            PostUploadNotifier.cancelFinalNotification(appContext, post)

            // if the user tapped on the PUBLISH quick action, make this Post publishable and track
            // analytics before starting the upload process.
            if (inputData.getBoolean(KEY_CHANGE_STATUS_TO_PUBLISH, false)) {
                val site = siteStore.getSiteByLocalId(post.localSiteId)
                makePostPublishable(post, site)
                PostUtils.trackSavePostAnalytics(post, site)
            }
            if (inputData.getBoolean(KEY_SHOULD_RETRY, false)) {
                if (AppPrefs.isAztecEditorEnabled() || AppPrefs.isGutenbergEditorEnabled()) {
                    if (!NetworkUtils.isNetworkAvailable(appContext)) {
                        rebuildNotificationError(post, appContext.getString(R.string.no_network_message))
                        return
                    }
                    val postHasGutenbergBlocks =
                        PostUtils.contentContainsGutenbergBlocks(post.content)
                    retryUpload(post, !postHasGutenbergBlocks)
                } else {
                    ToastUtils.showToast(appContext, R.string.retry_needs_aztec)
                }
                return
            }

            // is this a new post? only add count to the notification when the post is totally new
            // i.e. it still doesn't have any tracked state in the UploadStore
            // or it's a failed one the user is actively retrying.
            if (isThisPostTotallyNewOrFailed(post) && !PostUploadHandler.isPostUploadingOrQueued(post)) {
                postUploadNotifier.addPostInfoToForegroundNotification(post, null)
            }
            if (getAllFailedMediaForPost(post).isNotEmpty()) {
                val postHasGutenbergBlocks =
                    PostUtils.contentContainsGutenbergBlocks(post.content)
                retryUpload(post, !postHasGutenbergBlocks)
            } else if (UploadService.hasPendingOrInProgressMediaUploadsForPost(post)) {
                // Register the post (as PENDING) in the UploadStore, along with all media currently in progress for it
                // If the post is already registered, the new media will be added to its list
                val activeMedia = MediaUploadHandler.getPendingOrInProgressMediaUploadsForPost(post)
                uploadStore.registerPostModel(post, activeMedia)
            } else {
                postUploadHandler.upload(post)
            }
        }
    }

    /**
     * Do not use this method unless the user explicitly confirmed changes - eg. clicked on publish button or
     * similar.
     */
    private fun makePostPublishable(post: PostModel, site: SiteModel?) {
        PostUtils.preparePostForPublish(post, site)
        dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post))
    }

    private fun isThisPostTotallyNewOrFailed(post: PostImmutableModel): Boolean {
        // if we have any tracks for this Post's UploadState, this means this Post is not new.
        // Conditions under which the UploadStore would contain traces of this Post's UploadState are:
        // - it's been cancelled by entering/exiting/entering the editor thus cancelling the queued post upload
        // to allow for the user to keep editing it before sending to the server
        // - it's a failed upload (due to some network issue, for example)
        // - it's a pending upload (it is currently registered for upload once the associated media finishes
        // uploading).
        return !uploadStore.isRegisteredPostModel(post) || uploadStore.isFailedPost(post) || uploadStore
            .isPendingPost(post)
    }

    private fun rebuildNotificationError(post: PostModel, errorMessage: String) {
        val failedMedia = uploadStore.getFailedMediaForPost(post)
        postUploadNotifier.setTotalMediaItems(post, failedMedia.size)
        val site = siteStore.getSiteByLocalId(post.localSiteId)
        if (site != null) {
            postUploadNotifier.updateNotificationErrorForPost(post, site, errorMessage, 0)
        } else {
            AppLog.e(AppLog.T.POSTS, "Trying to rebuild notification error without a site")
        }
    }

    private fun retryUpload(post: PostModel, processWithAztec: Boolean) {
        if (uploadStore.isPendingPost(post)) {
            // The post is already pending upload so there is no need to manually retry it. Actually, the retry might
            // result in the post being uploaded without its media. As if the media upload is in progress, the
            // `getAllFailedMediaForPost()` methods returns an empty set. If we invoke `mPostUploadHandler.upload()`
            // the post will be uploaded ignoring its media (we could upload content with paths to local storage).
            return
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_UPLOAD_POST_ERROR_RETRY)
        if (processWithAztec) {
            aztecRegisterFailedMediaForThisPost(post)
        }
        val mediaToRetry = getAllFailedMediaForPost(post)
        if (mediaToRetry.isNotEmpty()) {
            // reset these media items to QUEUED
            for (media in mediaToRetry) {
                media.setUploadState(MediaModel.MediaUploadState.QUEUED)
                dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
            }
            if (processWithAztec) {
                val changesConfirmed =
                    post.contentHashcode() == post.changesConfirmedContentHashcode

                // do the same within the Post content itself
                val postContentWithRestartedUploads =
                    AztecEditorFragment.restartFailedMediaToUploading(appContext, post.content)
                post.setContent(postContentWithRestartedUploads)
                if (changesConfirmed) {
                    /*
                     * We are updating media upload status, but we don't make any undesired changes to the post. We
                     * need to make sure to retain the confirmation state.
                     */
                    post.setChangesConfirmedContentHashcode(post.contentHashcode())
                }
                dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post))
            }

            // retry uploading media items
            for (media in mediaToRetry) {
                mediaUploadHandler.upload(media)
            }

            // Register the post (as PENDING) in the UploadStore, along with all media currently in progress for it
            // If the post is already registered, the new media will be added to its list
            uploadStore.registerPostModel(post, mediaToRetry)
            postUploadNotifier.addPostInfoToForegroundNotification(post, mediaToRetry)

            // send event so Editors can handle clearing Failed statuses properly if Post is being edited right now
            EventBus.getDefault().post(UploadService.UploadMediaRetryEvent(mediaToRetry))
        } else {
            postUploadNotifier.addPostInfoToForegroundNotification(post, null)
            // retry uploading the Post
            postUploadHandler.upload(post)
        }
    }

    private fun aztecRegisterFailedMediaForThisPost(post: PostModel) {
        // there could be failed media in the post, that has not been registered in the UploadStore because
        // the media was being uploaded separately (i.e. the user included media, started uploading within
        // the editor, and such media failed _before_ exiting the eidtor, thus the registration never happened.
        // We're recovering the information here so we make sure to rebuild the status only when the user taps
        // on Retry.
        val mediaIds =
            AztecEditorFragment.getMediaMarkedFailedInPostContent(appContext, post.content)
        if (mediaIds != null && !mediaIds.isEmpty()) {
            val mediaList = ArrayList<MediaModel>()
            for (mediaId in mediaIds) {
                val media = mediaStore.getMediaWithLocalId(StringUtils.stringToInt(mediaId))
                if (media != null) {
                    mediaList.add(media)
                    // if this media item didn't have the Postid set, let's set it as we found it
                    // in the Post body anyway. So let's fix that now.
                    if (media.localPostId == 0) {
                        media.localPostId = post.id
                        dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
                    }
                }
            }
            if (mediaList.isNotEmpty()) {
                // given we found failed media within this Post, let's also cancel the media error
                siteStore.getSiteByLocalId(post.localSiteId)?.let {
                    PostUploadNotifier.cancelFinalNotificationForMedia(
                        appContext,
                        it
                    )
                }

                // now we have a list. Let' register this list.
                uploadStore.registerPostModel(post, mediaList)
            }
        }
    }

    private fun getAllFailedMediaForPost(postModel: PostModel): List<MediaModel> {
        val failedMedia = uploadStore.getFailedMediaForPost(postModel)
        return filterOutRecentlyDeletedMedia(failedMedia)
    }

    private fun filterOutRecentlyDeletedMedia(failedMedia: Set<MediaModel>): List<MediaModel> {
        val mediaToRetry: MutableList<MediaModel> = ArrayList()
        for (mediaModel in failedMedia) {
            val mediaIdToCompare = mediaModel.id.toString()
            if (!userDeletedMediaItemIds.contains(mediaIdToCompare)) {
                mediaToRetry.add(mediaModel)
            }
        }
        return mediaToRetry
    }

    private fun verifyMediaOnlyUploadsAndNotify() {
        // check if all are successful uploads, then notify the user about it
        if (mediaBatchUploaded.isNotEmpty()) {
            val standAloneMediaItems = ArrayList<MediaModel>()
            for (media in mediaBatchUploaded) {
                // we need to obtain the latest copy from the Store, as it's got the remote mediaId field
                val currentMedia = mediaStore.getMediaWithLocalId(media.id)
                if (currentMedia != null && currentMedia.localPostId == 0 && (MediaModel.MediaUploadState.fromString(
                        currentMedia.uploadState
                    )
                            == MediaModel.MediaUploadState.UPLOADED)
                ) {
                    standAloneMediaItems.add(currentMedia)
                }
            }
            if (standAloneMediaItems.isNotEmpty()) {
                val site = siteStore.getSiteByLocalId(standAloneMediaItems[0].localSiteId)
                postUploadNotifier.updateNotificationSuccessForMedia(
                    standAloneMediaItems,
                    site!!
                )
                mediaBatchUploaded.clear()
            }
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
    private fun doFinalProcessingOfPosts(isError: Boolean?, post: PostModel?): Boolean {
        // If this was the last media upload a post was waiting for, update the post content
        // This done for pending as well as cancelled and failed posts
        for (postModel in uploadStore.getAllRegisteredPosts()) {
            if (postUtilsWrapper.isPostCurrentlyBeingEdited(postModel)) {
                // Don't upload a Post that is being currently open in the Editor.
                // This fixes the issue on self-hosted sites when you have a queued post which couldn't be
                // remote autosaved. When you try to leave the editor without saving it will get stuck in queued
                // upload state. In case of the post still being edited we cancel any ongoing upload post action.
                dispatcher.dispatch(UploadActionBuilder.newCancelPostAction(post))
                continue
            }
            if (!UploadService.hasPendingOrInProgressMediaUploadsForPost(postModel)) {
                // Replace local with remote media in the post content
                val updatedPost = updateOnePostModelWithCompletedAndFailedUploads(postModel)
                if (updatedPost != null) {
                    // here let's check if there are any failed media
                    val failedMedia = uploadStore.getFailedMediaForPost(postModel)
                    if (failedMedia.isNotEmpty()) {
                        // this Post has failed media, don't upload it just yet,
                        // but tell the user about the error
                        UploadService.cancelQueuedPostUpload(postModel)

                        // update error notification for Post, unless the media is in the user-deleted media set
                        if (!isAllFailedMediaUserDeleted(failedMedia)) {
                            val site = siteStore.getSiteByLocalId(postModel.localSiteId)
                            val message = UploadUtils
                                .getErrorMessage(
                                    appContext,
                                    postModel.isPage,
                                    appContext.getString(R.string.error_generic_error),
                                    true
                                )
                            if (site != null) {
                                postUploadNotifier.updateNotificationErrorForPost(
                                    postModel,
                                    site,
                                    message,
                                    0
                                )
                            } else {
                                AppLog.e(
                                    AppLog.T.POSTS,
                                    "Error notification cannot be updated without a post"
                                )
                            }
                        }
                        postUploadHandler.unregisterPostForAnalyticsTracking(postModel.id)
                        EventBus.getDefault().post(
                            PostEvents.PostUploadCanceled(postModel)
                        )
                    } else {
                        // Do not re-enqueue a post that has already failed
                        if (isError != null && isError && uploadStore.isFailedPost(updatedPost)) {
                            continue
                        }
                        // TODO Should do some extra validation here
                        // e.g. what if the post has local media URLs but no pending media uploads?
                        postUploadHandler.upload(updatedPost)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isAllFailedMediaUserDeleted(failedMediaSet: Set<MediaModel>?): Boolean {
        if (failedMediaSet != null && failedMediaSet.size == userDeletedMediaItemIds.size) {
            var numberOfMatches = 0
            for (media in failedMediaSet) {
                val mediaIdToCompare = media.id.toString()
                if (userDeletedMediaItemIds.contains(mediaIdToCompare)) {
                    numberOfMatches++
                }
            }
            if (numberOfMatches == userDeletedMediaItemIds.size) {
                return true
            }
        }
        return false
    }

    private fun updateOnePostModelWithCompletedAndFailedUploads(postModel: PostModel): PostModel? {
        var updatedPost = UploadService.updatePostWithCurrentlyCompletedUploads(postModel)
        // also do the same now with failed uploads
        updatedPost = updatePostWithCurrentlyFailedUploads(updatedPost)
        // finally, save the PostModel
        if (updatedPost != null) {
            dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(updatedPost))
        }
        return updatedPost
    }

    private fun updatePostWithCurrentlyFailedUploads(postModel: PostModel?): PostModel? {
        var post = postModel
        if (post != null) {
            // now get the list of failed media for this post, so we can make post content
            // updates in one go and save only once
            val processor: MediaUploadReadyListener = MediaUploadReadyProcessor()
            val failedMedia = uploadStore.getFailedMediaForPost(post)
            for (media in failedMedia) {
                post = updatePostWithFailedMedia(post, media, processor)
            }
            // Unlike completed media, we won't remove the failed media references, so we can look up their errors later
        }
        return post
    }

    @Synchronized
    private fun updatePostWithFailedMedia(
        post: PostModel?, media: MediaModel?,
        processor: MediaUploadReadyListener?
    ): PostModel? {
        if (media != null && post != null && processor != null) {
            val changesConfirmed =
                post.contentHashcode() == post.changesConfirmedContentHashcode
            // actually mark the media failed within the Post
            processor.markMediaUploadFailedInPost(
                post, media.id.toString(),
                FluxCUtils.mediaFileFromMediaModel(media)
            )

            // we changed the post, so let’s mark this down
            if (!post.isLocalDraft) {
                post.setIsLocallyChanged(true)
            }
            post.setDateLocallyChanged(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000))
            if (changesConfirmed) {
                /*
             * We are updating media upload status, but we don't make any undesired changes to the post. We need to
             * make sure to retain the confirmation state.
             */
                post.setChangesConfirmedContentHashcode(post.contentHashcode())
            }
        }
        return post
    }

    companion object {
        private const val KEY_CHANGE_STATUS_TO_PUBLISH = "shouldPublish"
        private const val KEY_SHOULD_RETRY = "shouldRetry"
        private const val KEY_LOCAL_POST_ID = "localPostId"
        private const val KEY_SHOULD_TRACK_ANALYTICS = "shouldTrackPostAnalytics"

        private var instance: UploadPostWorker? = null

        // we keep this list so we don't tell the user an error happened when we find a FAILED media item
        // for media that the user actively cancelled uploads for
        private val userDeletedMediaItemIds = HashSet<String>()

        // we hold this reference here for the success notification for Media uploads
        private val mediaBatchUploaded: MutableList<MediaModel> = ArrayList()

        fun enqueueUploadPostWorkRequest(postId: Int, isFirstTimePublish: Boolean): Pair<WorkRequest, Operation> {
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(getUploadConstraints())
                .setInputData(workDataOf(
                    KEY_LOCAL_POST_ID to postId,
                    KEY_SHOULD_TRACK_ANALYTICS to isFirstTimePublish
                ))
                .build()
            val operation = WorkManager.getInstance(WordPress.getContext()).enqueueUniqueWork(
                "upload-post-$postId",
                ExistingWorkPolicy.KEEP, request
            )
            return Pair(request, operation)
        }

        fun enqueueRetryUploadPostWorkRequest(postId: Int, isFirstTimePublish: Boolean): Pair<WorkRequest, Operation> {
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(getUploadConstraints())
                .setInputData(workDataOf(
                    KEY_LOCAL_POST_ID to postId,
                    KEY_SHOULD_TRACK_ANALYTICS to isFirstTimePublish,
                    KEY_SHOULD_RETRY to true
                ))
                .build()
            val operation = WorkManager.getInstance(WordPress.getContext()).enqueueUniqueWork(
                "upload-post-$postId",
                ExistingWorkPolicy.KEEP, request
            )
            return Pair(request, operation)
        }

        fun enqueuePublishPostWorkRequest(postId: Int, isFirstTimePublish: Boolean): Pair<WorkRequest, Operation> {
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(getUploadConstraints())
                .setInputData(workDataOf(
                    KEY_LOCAL_POST_ID to postId,
                    KEY_SHOULD_TRACK_ANALYTICS to isFirstTimePublish,
                    KEY_CHANGE_STATUS_TO_PUBLISH to true
                ))
                .build()
            val operation = WorkManager.getInstance(WordPress.getContext()).enqueueUniqueWork(
                "upload-post-$postId",
                ExistingWorkPolicy.KEEP, request
            )
            return Pair(request, operation)
        }

        fun setDeletedMediaItemIds(mediaIds: List<String>) {
            userDeletedMediaItemIds.clear()
            userDeletedMediaItemIds.addAll(
                mediaIds
            )
        }

        private fun getUploadConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_ROAMING)
                .build()
        }

        @JvmStatic
        fun cancelFinalNotification(context: Context?, post: PostImmutableModel?) {
            // cancel any outstanding "end" notification for this Post before we start processing it again
            // i.e. dismiss success or error notification for the post.
            PostUploadNotifier.cancelFinalNotification(context, post!!)
        }

        @JvmStatic
        fun cancelFinalNotificationForMedia(context: Context?, site: SiteModel?) {
            PostUploadNotifier.cancelFinalNotificationForMedia(context, site!!)
        }

        /**
         * Returns true if the passed post is either currently uploading or waiting to be uploaded.
         * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
         * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
         */
        @JvmStatic
        fun isPostUploadingOrQueued(post: PostImmutableModel): Boolean {
            // First check for posts uploading or queued inside the PostUploadManager
            return if (PostUploadHandler.isPostUploadingOrQueued(post)) {
                true
            } else  isPendingPost(post)

            // Then check the list of posts waiting for media to complete
        }

        private fun isPendingPost(post: PostImmutableModel): Boolean {
            val postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(post.id)
            return postUploadModel != null && postUploadModel.uploadState == PostUploadModel.PENDING
        }

        fun isPostQueued(post: PostImmutableModel?): Boolean {
            // Check for posts queued inside the PostUploadManager
            return PostUploadHandler.isPostQueued(post)
        }

        /**
         * Returns true if the passed post is currently uploading.
         * Except for legacy mode, a post counts as 'uploading' if the post content itself is being uploaded - a post
         * waiting for media to finish uploading counts as 'waiting to be uploaded' until the media uploads complete.
         */
        fun isPostUploading(post: PostImmutableModel?): Boolean {
            return PostUploadHandler.isPostUploading(post)
        }

        fun cancelQueuedPostUploadAndRelatedMedia(post: PostModel?) {
            if (post != null) {
                PostUploadNotifier.cancelFinalNotification(instance?.appContext, post)
                instance?.postUploadNotifier?.removePostInfoFromForegroundNotification(
                    post, instance?.mediaStore?.getMediaForPost(post)
                )
                cancelQueuedPostUpload(post)
                EventBus.getDefault().post(PostEvents.PostMediaCanceled(post))
            }
        }

        fun cancelQueuedPostUpload(post: PostModel?) {
            if (post != null) {
                // Mark the post as CANCELLED in the UploadStore
                instance?.dispatcher?.dispatch(UploadActionBuilder.newCancelPostAction(post))
            }
        }

        @JvmStatic
        fun updatePostWithCurrentlyCompletedUploads(postModel: PostModel?): PostModel? {
            var post = postModel
            if (post != null) {
                // now get the list of completed media for this post, so we can make post content
                // updates in one go and save only once
                val processor: MediaUploadReadyListener = MediaUploadReadyProcessor()
                val completedMedia = instance?.uploadStore?.getCompletedMediaForPost(post)
                if (completedMedia != null) {
                    for (media in completedMedia) {
                        post = if (media.markedLocallyAsFeatured) {
                            updatePostWithNewFeaturedImg(post, media.mediaId)
                        } else {
                            updatePostWithMediaUrl(post, media, processor)
                        }
                    }
                }
                if (completedMedia != null) {
                    if (completedMedia.isNotEmpty()) {
                        // finally remove all completed uploads for this post, as they've been taken care of
                        val clearMediaPayload = UploadStore.ClearMediaPayload(post, completedMedia)
                        instance?.dispatcher?.dispatch(
                            UploadActionBuilder.newClearMediaForPostAction(
                                clearMediaPayload
                            )
                        )
                    }
                }
            }
            return post
        }

        @JvmStatic
        fun hasInProgressMediaUploadsForPost(postModel: PostImmutableModel?): Boolean {
            return postModel != null && MediaUploadHandler.hasInProgressMediaUploadsForPost(
                postModel.id
            )
        }

        fun hasPendingMediaUploadsForPost(postModel: PostImmutableModel?): Boolean {
            return postModel != null && MediaUploadHandler.hasPendingMediaUploadsForPost(postModel.id)
        }

        @JvmStatic
        fun hasPendingOrInProgressMediaUploadsForPost(postModel: PostImmutableModel?): Boolean {
            return postModel != null && MediaUploadHandler.hasPendingOrInProgressMediaUploadsForPost(
                postModel.id
            )
        }

        fun getPendingOrInProgressFeaturedImageUploadForPost(postModel: PostImmutableModel?): MediaModel? {
            return MediaUploadHandler.getPendingOrInProgressFeaturedImageUploadForPost(postModel)
        }

        @JvmStatic
        fun getPendingOrInProgressMediaUploadsForPost(post: PostImmutableModel?): List<MediaModel> {
            return MediaUploadHandler.getPendingOrInProgressMediaUploadsForPost(post)
        }

        fun getMediaUploadProgressForPost(postModel: PostModel?): Float {
            if (postModel == null) {
                // If the UploadService isn't running, there's no progress for this post
                return 0F
            }
            val pendingMediaList = instance?.uploadStore?.getUploadingMediaForPost(postModel)
            if (pendingMediaList?.size == 0) {
                return 1F
            }
            var overallProgress = 0f
            for (pendingMedia in pendingMediaList!!) {
                overallProgress += UploadService.getUploadProgressForMedia(pendingMedia)
            }
            overallProgress /= pendingMediaList.size.toFloat()
            return overallProgress
        }

        @JvmStatic
        fun getUploadProgressForMedia(mediaModel: MediaModel?): Float {
            if (mediaModel == null) {
                // If the UploadService isn't running, there's no progress for this media
                return 0F
            }
            val uploadProgress = instance?.uploadStore?.getUploadProgressForMedia(mediaModel)

            // If this is a video and video optimization is enabled, include the optimization progress in the outcome
            return if (mediaModel.isVideo && WPMediaUtils.isVideoOptimizationEnabled()) {
                MediaUploadHandler.getOverallProgressForVideo(mediaModel.id, uploadProgress!!)
            } else uploadProgress!!
        }

        fun getPendingMediaForPost(postModel: PostModel?): Set<MediaModel> {
            return if (postModel == null) {
                emptySet()
            } else instance?.uploadStore!!.getUploadingMediaForPost(postModel)
        }

        @JvmStatic
        fun isPendingOrInProgressMediaUpload(media: MediaModel): Boolean {
            return MediaUploadHandler.isPendingOrInProgressMediaUpload(media.id)
        }

        /**
         * Rechecks all media in the MediaStore marked UPLOADING/QUEUED against the UploadingService to see
         * if it's actually uploading or queued and change it accordingly, to recover from an inconsistent state
         */
        fun sanitizeMediaUploadStateForSite(
            mediaStore: MediaStore, dispatcher: Dispatcher,
            site: SiteModel
        ) {
            val uploadingMedia = mediaStore.getSiteMediaWithState(site, MediaModel.MediaUploadState.UPLOADING)
            val queuedMedia = mediaStore.getSiteMediaWithState(site, MediaModel.MediaUploadState.QUEUED)
            if (uploadingMedia.isEmpty() && queuedMedia.isEmpty()) {
                return
            }
            val uploadingOrQueuedMedia: MutableList<MediaModel> = ArrayList()
            uploadingOrQueuedMedia.addAll(uploadingMedia)
            uploadingOrQueuedMedia.addAll(queuedMedia)
            for (media in uploadingOrQueuedMedia) {
                if (!isPendingOrInProgressMediaUpload(media)) {
                    // it is NOT being uploaded or queued in the actual UploadService, mark it failed
                    media.setUploadState(MediaModel.MediaUploadState.FAILED)
                    dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
                }
            }
        }

        @Synchronized
        private fun updatePostWithNewFeaturedImg(
            post: PostModel?,
            remoteMediaId: Long?
        ): PostModel? {
            if (post != null && remoteMediaId != null) {
                val changesConfirmed =
                    post.contentHashcode() == post.changesConfirmedContentHashcode
                post.setFeaturedImageId(remoteMediaId)
                post.setIsLocallyChanged(true)
                post.setDateLocallyChanged(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000))
                if (changesConfirmed) {
                    /*
                 * We are replacing local featured image with a remote version. We need to make sure
                 * to retain the confirmation state.
                 */
                    post.setChangesConfirmedContentHashcode(post.contentHashcode())
                }
            }
            return post
        }

        @Synchronized
        private fun updatePostWithMediaUrl(
            post: PostModel?, media: MediaModel?,
            processor: MediaUploadReadyListener?
        ): PostModel? {
            if (media != null && post != null && processor != null) {
                val changesConfirmed =
                    post.contentHashcode() == post.changesConfirmedContentHashcode

                // obtain site url used to generate attachment page url
                val site = instance?.siteStore?.getSiteByLocalId(media.localSiteId)

                // actually replace the media ID with the media uri
                processor.replaceMediaFileWithUrlInPost(
                    post, media.id.toString(),
                    FluxCUtils.mediaFileFromMediaModel(media), site
                )

                // we changed the post, so let’s mark this down
                if (!post.isLocalDraft) {
                    post.setIsLocallyChanged(true)
                }
                post.setDateLocallyChanged(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000))
                if (changesConfirmed) {
                    /*
                 * We are replacing image local path with a url. We need to make sure to retain the confirmation
                 * state.
                 */
                    post.setChangesConfirmedContentHashcode(post.contentHashcode())
                }
            }
            return post
        }
    }
}
