package org.wordpress.android.ui.posts

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType.DELETE
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType.TRASH
import org.wordpress.android.ui.posts.PostUploadAction.MediaUploadedSnackbar
import org.wordpress.android.ui.posts.PostUploadAction.PostRemotePreviewSnackbarError
import org.wordpress.android.ui.posts.PostUploadAction.PostUploadedSnackbar
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.ui.uploads.ProgressEvent
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
@Suppress("LongParameterList")
class PostListEventListener(
    private val lifecycle: Lifecycle,
    private val dispatcher: Dispatcher,
    private val bgDispatcher: CoroutineDispatcher,
    private val postStore: PostStore,
    private val site: SiteModel,
    private val postActionHandler: PostActionHandler,
    private val handlePostUpdatedWithoutError: () -> Unit,
    private val handlePostUploadedWithoutError: (LocalId) -> Unit,
    private val triggerPostUploadAction: (PostUploadAction) -> Unit,
    private val invalidateUploadStatus: (List<Int>) -> Unit,
    private val invalidateFeaturedMedia: (List<Long>) -> Unit,
    private val triggerPreviewStateUpdate: (PostListRemotePreviewState, PostInfoType) -> Unit,
    private val isRemotePreviewingFromPostsList: () -> Boolean,
    private val hasRemoteAutoSavePreviewError: () -> Boolean
) : DefaultLifecycleObserver, CoroutineScope {
    init {
        dispatcher.register(this)
        EventBus.getDefault().register(this)
        lifecycle.addObserver(this)
    }

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private fun handleRemoteAutoSave(post: PostModel, isError: Boolean) {
        if (isError || hasRemoteAutoSavePreviewError.invoke()) {
            triggerPreviewStateUpdate(
                    PostListRemotePreviewState.REMOTE_AUTO_SAVE_PREVIEW_ERROR,
                    PostInfoType.PostNoInfo
            )
            triggerPostUploadAction.invoke(PostRemotePreviewSnackbarError(R.string.remote_preview_operation_error))
        } else {
            triggerPreviewStateUpdate(
                    PostListRemotePreviewState.PREVIEWING,
                    PostInfoType.PostInfo(post = post, hasError = isError)
            )
        }
        uploadStatusChanged(post.id)
        if (!isError) {
            handlePostUploadedWithoutError.invoke(LocalId(post.id))
        }
    }

    /**
     * Handles the [Lifecycle.Event.ON_DESTROY] event to cleanup the registration for dispatcher and removing the
     * observer for lifecycle.
     */
    override fun onDestroy(owner: LifecycleOwner) {
        job.cancel()
        lifecycle.removeObserver(this)
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
    }

    /**
     * Has lower priority than the PostUploadHandler and UploadService, which ensures that they already processed this
     * OnPostChanged event. This means we can safely rely on their internal state being up to date.
     */
    @Suppress("unused", "LongMethod", "ComplexMethod")
    @Subscribe(threadMode = MAIN, priority = 5)
    fun onPostChanged(event: OnPostChanged) {
        // We need to subscribe on the MAIN thread, in order to ensure the priority parameter is taken into account.
        // However, we want to perform the body of the method on a background thread.
        launch {
            when (event.causeOfChange) {
                // Fetched post list event will be handled by OnListChanged
                is CauseOfOnPostChanged.UpdatePost -> {
                    if (event.isError) {
                        AppLog.e(
                                T.POSTS,
                                "Error updating the post with type: ${event.error.type} and" +
                                        " message: ${event.error.message}"
                        )
                    } else {
                        handlePostUpdatedWithoutError.invoke()
                        invalidateUploadStatus.invoke(
                                listOf((event.causeOfChange as CauseOfOnPostChanged.UpdatePost).localPostId)
                        )
                    }
                }
                is CauseOfOnPostChanged.DeletePost -> {
                    val deletePostCauseOfChange = event.causeOfChange as CauseOfOnPostChanged.DeletePost
                    val localPostId = LocalId(deletePostCauseOfChange.localPostId)
                    when (deletePostCauseOfChange.postDeleteActionType) {
                        TRASH -> postActionHandler.handlePostTrashed(localPostId = localPostId, isError = event.isError)
                        DELETE -> postActionHandler.handlePostDeletedOrRemoved(
                                localPostId = localPostId,
                                isRemoved = false,
                                isError = event.isError
                        )
                    }
                }
                is CauseOfOnPostChanged.RestorePost -> {
                    val localPostId = LocalId((event.causeOfChange as CauseOfOnPostChanged.RestorePost).localPostId)
                    postActionHandler.handlePostRestored(localPostId = localPostId, isError = event.isError)
                }
                is CauseOfOnPostChanged.RemovePost -> {
                    val localPostId = LocalId((event.causeOfChange as CauseOfOnPostChanged.RemovePost).localPostId)
                    postActionHandler.handlePostDeletedOrRemoved(
                            localPostId = localPostId,
                            isRemoved = true,
                            isError = event.isError
                    )
                }
                is CauseOfOnPostChanged.RemoteAutoSavePost -> {
                    val post = postStore.getPostByLocalPostId(
                            (event.causeOfChange as CauseOfOnPostChanged.RemoteAutoSavePost).localPostId
                    )
                    if (isRemotePreviewingFromPostsList.invoke()) {
                        if (event.isError) {
                            AppLog.d(
                                    T.POSTS, "REMOTE_AUTO_SAVE_POST failed: " +
                                    event.error.type + " - " + event.error.message)
                        }
                        handleRemoteAutoSave(post, event.isError)
                    } else {
                        uploadStatusChanged(post.id)
                    }
                }
                is CauseOfOnPostChanged.FetchPages -> Unit // Do nothing
                is CauseOfOnPostChanged.FetchPosts -> Unit // Do nothing
                is CauseOfOnPostChanged.RemoveAllPosts -> Unit // Do nothing
                is CauseOfOnPostChanged.FetchPostLikes -> Unit // Do nothing
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onMediaChanged(event: OnMediaChanged) {
        if (!event.isError && event.mediaList != null) {
            featuredMediaChanged(*event.mediaList.map { it.mediaId }.toLongArray())
            uploadStatusChanged(*event.mediaList.map { it.localPostId }.toIntArray())
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onPostUploaded(event: OnPostUploaded) {
        if (event.post != null && event.post.localSiteId == site.id) {
            if (!isRemotePreviewingFromPostsList.invoke() && !isRemotePreviewingFromEditor(event.post)) {
                triggerPostUploadAction.invoke(
                        PostUploadedSnackbar(
                                dispatcher,
                                site,
                                event.post,
                                event.isError,
                                event.isFirstTimePublish,
                                null
                        )
                )
            }

            uploadStatusChanged(event.post.id)
            if (!event.isError) {
                handlePostUploadedWithoutError.invoke(LocalId(event.post.id))
            }

            if (isRemotePreviewingFromPostsList.invoke()) {
                handleRemoteAutoSave(event.post, event.isError)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.isError || event.canceled) {
            return
        }
        if (event.media == null || event.media.localPostId == 0 || site.id != event.media.localSiteId) {
            // Not interested in media not attached to posts or not belonging to the current site
            return
        }
        featuredMediaChanged(event.media.mediaId)
        uploadStatusChanged(event.media.localPostId)
    }

    // EventBus Events

    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onEventBackgroundThread(event: UploadService.UploadErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.post != null) {
            triggerPostUploadAction.invoke(
                    PostUploadedSnackbar(
                            dispatcher,
                            site,
                            event.post,
                            true,
                            false,
                            event.errorMessage
                    )
            )
        } else if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            triggerPostUploadAction.invoke(MediaUploadedSnackbar(site, event.mediaModelList, true, event.errorMessage))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onEventBackgroundThread(event: UploadService.UploadMediaSuccessEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            triggerPostUploadAction.invoke(
                    MediaUploadedSnackbar(site, event.mediaModelList, false, event.successMessage)
            )
        }
    }

    /**
     * Upload started, reload so correct status on uploading post appears
     */
    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onEventBackgroundThread(event: PostEvents.PostUploadStarted) {
        if (site.id == event.post.localSiteId) {
            uploadStatusChanged(event.post.id)
        }
    }

    /**
     * Upload cancelled (probably due to failed media), reload so correct status on uploading post appears
     */
    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onEventBackgroundThread(event: PostEvents.PostUploadCanceled) {
        if (site.id == event.post.localSiteId) {
            uploadStatusChanged(event.post.id)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onEventBackgroundThread(event: ProgressEvent) {
        uploadStatusChanged(event.media.localPostId)
    }

    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onEventBackgroundThread(event: UploadService.UploadMediaRetryEvent) {
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            // if there' a Post to which the retried media belongs, clear their status
            val postsToRefresh = PostUtils.getPostsThatIncludeAnyOfTheseMedia(postStore, event.mediaModelList)
            uploadStatusChanged(*postsToRefresh.map { it.id }.toIntArray())
        }
    }

    private fun isRemotePreviewingFromEditor(post: PostModel?): Boolean {
        val previewedPost = EventBus.getDefault().getStickyEvent(PostEvents.PostPreviewingInEditor::class.java)
        return previewedPost != null && post != null &&
                post.localSiteId == previewedPost.localSiteId &&
                post.id == previewedPost.postId
    }

    private fun uploadStatusChanged(vararg localPostIds: Int) {
        invalidateUploadStatus.invoke(localPostIds.toList())
    }

    private fun featuredMediaChanged(vararg featuredImageIds: Long) {
        invalidateFeaturedMedia.invoke(featuredImageIds.toList())
    }

    class Factory @Inject constructor() {
        @Suppress("LongParameterList")
        fun createAndStartListening(
            lifecycle: Lifecycle,
            dispatcher: Dispatcher,
            bgDispatcher: CoroutineDispatcher,
            postStore: PostStore,
            site: SiteModel,
            postActionHandler: PostActionHandler,
            handlePostUpdatedWithoutError: () -> Unit,
            handlePostUploadedWithoutError: (LocalId) -> Unit,
            triggerPostUploadAction: (PostUploadAction) -> Unit,
            invalidateUploadStatus: (List<Int>) -> Unit,
            invalidateFeaturedMedia: (List<Long>) -> Unit,
            triggerPreviewStateUpdate: (PostListRemotePreviewState, PostInfoType) -> Unit,
            isRemotePreviewingFromPostsList: () -> Boolean,
            hasRemoteAutoSavePreviewError: () -> Boolean
        ) {
            PostListEventListener(
                    lifecycle = lifecycle,
                    dispatcher = dispatcher,
                    bgDispatcher = bgDispatcher,
                    postStore = postStore,
                    site = site,
                    postActionHandler = postActionHandler,
                    handlePostUpdatedWithoutError = handlePostUpdatedWithoutError,
                    handlePostUploadedWithoutError = handlePostUploadedWithoutError,
                    triggerPostUploadAction = triggerPostUploadAction,
                    invalidateUploadStatus = invalidateUploadStatus,
                    invalidateFeaturedMedia = invalidateFeaturedMedia,
                    triggerPreviewStateUpdate = triggerPreviewStateUpdate,
                    isRemotePreviewingFromPostsList = isRemotePreviewingFromPostsList,
                    hasRemoteAutoSavePreviewError = hasRemoteAutoSavePreviewError)
        }
    }
}
