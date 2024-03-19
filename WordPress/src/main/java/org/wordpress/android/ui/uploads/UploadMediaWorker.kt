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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.generated.UploadActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UploadWorker

class UploadMediaWorker(
    val appContext: Context,
    workerParameters: WorkerParameters,
    private val siteStore: SiteStore,
    private val postStore: PostStore,
    private val uploadStore: UploadStore,
    private val mediaStore: MediaStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val dispatcher: Dispatcher,
    private val systemNotificationsTracker: SystemNotificationsTracker,
    private val postUtilsWrapper: PostUtilsWrapper
) : CoroutineWorker(appContext, workerParameters) {
    private lateinit var mediaUploadHandler: MediaUploadHandler
    private lateinit var postUploadHandler: PostUploadHandler
    private lateinit var postUploadNotifier: PostUploadNotifier
    private lateinit var uploadService: UploadService // temporarily replace UploadService in PostUploadNotifier

    override suspend fun doWork(): Result {
        dispatcher.register(this)

        return try {
            unpackMediaIntent()

            Result.success()
        } catch (e: Exception) {
            // SecurityException can happen on some devices without Google services (these devices probably strip
            // the AndroidManifest.xml and remove unsupported permissions).
            AppLog.e(AppLog.T.POSTS, "Post upload failed: ", e)
            Result.failure()
        }
    }

    class Factory(
        private val siteStore: SiteStore,
        private val postStore: PostStore,
        private val uploadStore: UploadStore,
        private val mediaStore: MediaStore,
        private val selectedSiteRepository: SelectedSiteRepository,
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
                UploadMediaWorker(
                    appContext,
                    workerParameters,
                    siteStore,
                    postStore,
                    uploadStore,
                    mediaStore,
                    selectedSiteRepository,
                    dispatcher,
                    systemNotificationsTracker,
                    postUtilsWrapper
                )
            } else {
                null
            }
        }
    }

    private fun unpackMediaIntent() {
        mediaUploadHandler = MediaUploadHandler()
        // replace UploadService in PostUploadNotifier
        uploadService = UploadService()
        postUploadNotifier = PostUploadNotifier(applicationContext, uploadService, systemNotificationsTracker)
        postUploadHandler = PostUploadHandler(postUploadNotifier)

        // add new media
        val s:String? = inputData.getString(KEY_MEDIA_LIST)
        val mediaModelList = object : TypeToken<List<MediaModel?>?>() {}.type
        val mediaList: List<MediaModel>? = Gson().fromJson(s, mediaModelList) // intent.getSerializableExtra(UploadService.KEY_MEDIA_LIST) as List<MediaModel>?

        if (!mediaList.isNullOrEmpty()) {
            if (!inputData.getBoolean(KEY_MEDIA_LIST, false)) {
                // only cancel the media error notification if we're triggering a new media upload
                // either from Media Browser or a RETRY from a notification.
                // Otherwise, this flag should be true, and we need to keep the error notification as
                // it might be a separate action (user is editing a Post and including media there)
                PostUploadNotifier.cancelFinalNotificationForMedia(
                    appContext,
                    siteStore.getSiteByLocalId(
                        mediaList[0].localSiteId
                    )!!
                )

                // add these media items so we can use them in WRITE POST once they end up loading successfully
                mediaBatchUploaded.addAll(mediaList)
            }

            // if this media belongs to some post, register such Post
            registerPostModelsForMedia(mediaList, inputData.getBoolean(KEY_SHOULD_RETRY, false))
            val toBeUploadedMediaList = ArrayList<MediaModel>()
            for (media in mediaList) {
                val localMedia = mediaStore.getMediaWithLocalId(media.id)
                val notUploadedYet = (localMedia != null
                        && (localMedia.uploadState == null
                        || MediaModel.MediaUploadState.fromString(localMedia.uploadState)
                        != MediaModel.MediaUploadState.UPLOADED))
                if (notUploadedYet) {
                    toBeUploadedMediaList.add(media)
                }
            }
            for (media in toBeUploadedMediaList) {
                mediaUploadHandler.upload(media)
            }
            if (toBeUploadedMediaList.isNotEmpty()) {
                postUploadNotifier.addMediaInfoToForegroundNotification(toBeUploadedMediaList)
            }
        }
    }

    private fun registerPostModelsForMedia(mediaList: List<MediaModel>?, isRetry: Boolean) {
        if (!mediaList.isNullOrEmpty()) {
            val postsToRefresh = PostUtils.getPostsThatIncludeAnyOfTheseMedia(postStore, mediaList)
            for (post in postsToRefresh) {
                // If the post is already registered, the new media will be added to its list
                uploadStore.registerPostModel(post, mediaList)
            }
            if (isRetry) {
                // Bump analytics
                AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_UPLOAD_MEDIA_ERROR_RETRY)

                // send event so Editors can handle clearing Failed statuses properly if Post is being edited right now
                EventBus.getDefault().post(UploadMediaRetryEvent(mediaList))
            }
        }
    }

    class UploadErrorEvent {
        val post: PostModel?
        @JvmField
        val mediaModelList: List<MediaModel>?
        @JvmField
        val errorMessage: String

        constructor(post: PostModel?, errorMessage: String) {
            this.post = post
            mediaModelList = null
            this.errorMessage = errorMessage
        }

        constructor(mediaModelList: List<MediaModel>?, errorMessage: String) {
            post = null
            this.mediaModelList = mediaModelList
            this.errorMessage = errorMessage
        }
    }

    class UploadMediaSuccessEvent(@JvmField val mediaModelList: List<MediaModel>?, @JvmField val successMessage: String)
    class UploadMediaRetryEvent internal constructor(@JvmField val mediaModelList: List<MediaModel>?)

    /**
     * Has lower priority than the UploadHandlers, which ensures that the handlers have already received and
     * processed this OnMediaUploaded event. This means we can safely rely on their internal state being up to date.
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 7)
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.media == null) {
            return
        }
        if (event.isError) {
            if (event.media!!.localPostId > 0) {
                AppLog.w(
                    AppLog.T.MAIN,
                    "UploadService > Media upload failed for post " + event.media!!.localPostId + " : "
                            + event.error.type + ": " + event.error.message
                )
                val errorMessage =
                    UploadUtils.getErrorMessageFromMediaError(appContext, event.media, event.error)
                cancelPostUploadMatchingMedia(event.media!!, errorMessage, true)
            }
            if (!mediaBelongsToAPost(event.media)) {
                // this media item doesn't belong to a Post
                postUploadNotifier.incrementUploadedMediaCountFromProgressNotification(event.media!!.id)
                // Only show the media upload error notification if the post is NOT registered in the UploadStore
                // - otherwise if it IS registered in the UploadStore and we get a `cancelled` signal it means
                // the user actively cancelled it. No need to show an error then.
                val message =
                    UploadUtils.getErrorMessageFromMediaError(appContext, event.media, event.error)

                // if media has a local site id, use that. If not, default to currently selected site.
                val siteLocalId =
                    if (event.media!!.localSiteId > 0) event.media!!.localSiteId else selectedSiteRepository.getSelectedSiteLocalId(
                        true
                    )
                val selectedSite = siteStore.getSiteByLocalId(siteLocalId)
                val failedStandAloneMedia = getRetriableStandaloneMedia(selectedSite)
                if (failedStandAloneMedia.isEmpty()) {
                    // if we couldn't get the failed media from the MediaStore, at least we know
                    // for sure we're handling the event for this specific media item, so throw an error
                    // notification for this particular media item travelling in event.media
                    failedStandAloneMedia.add(event.media)
                }
                postUploadNotifier.updateNotificationErrorForMedia(
                    failedStandAloneMedia,
                    selectedSite!!, message
                )
            }
            stopServiceIfUploadsComplete()
            return
        }
        if (event.canceled) {
            // remove this media item from the progress notification
            postUploadNotifier.removeOneMediaItemInfoFromForegroundNotification()
            if (event.media!!.localPostId > 0) {
                AppLog.i(
                    AppLog.T.MAIN,
                    "UploadService > Upload cancelled for post with id " + event.media!!.localPostId
                            + " - a media upload for this post has been cancelled, id: " + event.media!!.id
                )
                cancelPostUploadMatchingMedia(
                    event.media!!,
                    appContext.getString(R.string.error_media_canceled),
                    false
                )
            }
            stopServiceIfUploadsComplete()
            return
        }
        if (event.completed) {
            if (event.media!!.localPostId != 0) {
                AppLog.i(
                    AppLog.T.MAIN,
                    "UploadService > Processing completed media with id " + event.media!!.id
                            + " and local post id " + event.media!!.localPostId
                )
            }
            postUploadNotifier.incrementUploadedMediaCountFromProgressNotification(event.media!!.id)
            stopServiceIfUploadsComplete()
        } else {
            // in-progress upload
            // Progress update
            postUploadNotifier.updateNotificationProgressForMedia(event.media, event.progress)
        }
    }

    private fun mediaBelongsToAPost(media: MediaModel?): Boolean {
        val postToCancel = postStore.getPostByLocalPostId(
            media!!.localPostId
        )
        return postToCancel != null && uploadStore.isRegisteredPostModel(postToCancel)
    }

    /*
        returns true if Post canceled
        returns false if Post can't be found or is not registered in the UploadStore
     */
    private fun cancelPostUploadMatchingMedia(
        media: MediaModel,
        errorMessage: String,
        showError: Boolean
    ): Boolean {
        val postToCancel = postStore.getPostByLocalPostId(media.localPostId) ?: return false
        if (!uploadStore.isRegisteredPostModel(postToCancel)) {
            return false
        }
        if (PostUploadHandler.isPostUploadingOrQueued(postToCancel) && !PostUtils
                .isPostCurrentlyBeingEdited(postToCancel)
        ) {
            // post is not being edited and is currently queued, update the count on the foreground notification
            postUploadNotifier.incrementUploadedPostCountFromForegroundNotification(postToCancel)
        }
        if (showError || uploadStore.isFailedPost(postToCancel)) {
            // Only show the media upload error notification if the post is NOT registered in the UploadStore
            // - otherwise if it IS registered in the UploadStore and we get a `cancelled` signal it means
            // the user actively cancelled it. No need to show an error then.
            val message =
                UploadUtils.getErrorMessage(appContext, postToCancel.isPage, errorMessage, true)
            val site = siteStore.getSiteByLocalId(postToCancel.localSiteId)
            if (site != null) {
                postUploadNotifier.updateNotificationErrorForPost(
                    postToCancel, site, message,
                    uploadStore.getFailedMediaForPost(postToCancel).size
                )
            } else {
                AppLog.e(AppLog.T.POSTS, "Trying to update notifications with missing site")
            }
        }
        postUploadHandler.unregisterPostForAnalyticsTracking(postToCancel.id)
        EventBus.getDefault().post(PostEvents.PostUploadCanceled(postToCancel))
        return true
    }

    @Synchronized
    private fun stopServiceIfUploadsComplete() {
        stopServiceIfUploadsComplete(null, null)
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

    private fun getRetriableStandaloneMedia(selectedSite: SiteModel?): MutableList<MediaModel?> {
        // get all retriable media ? To retry or not to retry, that is the question
        val failedStandAloneMedia: MutableList<MediaModel?> = ArrayList()
        if (selectedSite != null) {
            val failedMedia = mediaStore.getSiteMediaWithState(
                selectedSite, MediaModel.MediaUploadState.FAILED
            )

            // only take into account those media items that do not belong to any Post
            for (media in failedMedia) {
                if (media.localPostId == 0) {
                    failedStandAloneMedia.add(media)
                }
            }
        }
        return failedStandAloneMedia
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
        updatedPost = UploadService.updatePostWithCurrentlyFailedUploads(updatedPost)
        // finally, save the PostModel
        if (updatedPost != null) {
            dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(updatedPost))
        }
        return updatedPost
    }


    companion object {
        private const val KEY_SHOULD_RETRY = "shouldRetry"
        private const val KEY_MEDIA_LIST = "mediaList"
        private const val KEY_UPLOAD_MEDIA_FROM_EDITOR = "mediaFromEditor"

        // we keep this list so we don't tell the user an error happened when we find a FAILED media item
        // for media that the user actively cancelled uploads for
        private val userDeletedMediaItemIds = HashSet<String>()

        // we hold this reference here for the success notification for Media uploads
        private val mediaBatchUploaded: MutableList<MediaModel> = ArrayList()

        fun enqueueUploadMediaWorkRequest(media: MediaModel): Pair<WorkRequest, Operation> {
            val mediaList = ArrayList<MediaModel>()
            mediaList.add(media)

            val mediaModelList = object : TypeToken<List<MediaModel?>?>() {}.type
            val s: String = Gson().toJson(mediaList, mediaModelList)

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(getUploadConstraints())
                .setInputData(
                    workDataOf(KEY_MEDIA_LIST to s)
                )
                .build()
            val operation = WorkManager.getInstance(WordPress.getContext()).enqueueUniqueWork(
                "upload-media",
                ExistingWorkPolicy.KEEP, request
            )
            return Pair(request, operation)
        }

        fun enqueueUploadMediaListWorkRequest(mediaList: ArrayList<MediaModel>): Pair<WorkRequest, Operation> {
            val mediaModelList = object : TypeToken<List<MediaModel?>?>() {}.type
            val s: String = Gson().toJson(mediaList, mediaModelList)

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(getUploadConstraints())
                .setInputData(
                    workDataOf(KEY_MEDIA_LIST to s)
                )
                .build()
            val operation = WorkManager.getInstance(WordPress.getContext()).enqueueUniqueWork(
                "upload-media-list",
                ExistingWorkPolicy.KEEP, request
            )
            return Pair(request, operation)
        }

        fun enqueueUploadMediaListFromEditorWorkRequest(mediaList: ArrayList<MediaModel>): Pair<WorkRequest, Operation> {
            val mediaModelList = object : TypeToken<List<MediaModel?>?>() {}.type
            val s: String = Gson().toJson(mediaList, mediaModelList)

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(getUploadConstraints())
                .setInputData(
                    workDataOf(
                        KEY_MEDIA_LIST to s,
                        KEY_UPLOAD_MEDIA_FROM_EDITOR to true
                    )
                )
                .build()
            val operation = WorkManager.getInstance(WordPress.getContext()).enqueueUniqueWork(
                "upload-media-list-from-editor",
                ExistingWorkPolicy.KEEP, request
            )
            return Pair(request, operation)
        }

        private fun getUploadConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_ROAMING)
                .build()
        }
    }
}
