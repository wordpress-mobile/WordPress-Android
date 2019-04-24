package org.wordpress.android.ui.posts

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import de.greenrobot.event.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.DeletePost
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RemovePost
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RestorePost
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType.DELETE
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType.TRASH
import org.wordpress.android.ui.posts.PostUploadAction.MediaUploadedSnackbar
import org.wordpress.android.ui.posts.PostUploadAction.PostUploadedSnackbar
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.VideoOptimizer
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

fun listenForPostListEvents(
    lifecycle: Lifecycle,
    dispatcher: Dispatcher,
    postStore: PostStore,
    site: SiteModel,
    postActionHandler: PostActionHandler,
    handlePostUpdatedWithoutError: () -> Unit,
    handlePostUploadedWithoutError: (LocalId) -> Unit,
    triggerPostUploadAction: (PostUploadAction) -> Unit,
    invalidateUploadStatus: (List<Int>) -> Unit,
    invalidateFeaturedMedia: (List<Long>) -> Unit
) {
    PostListEventListener(
            lifecycle = lifecycle,
            dispatcher = dispatcher,
            postStore = postStore,
            site = site,
            postActionHandler = postActionHandler,
            handlePostUpdatedWithoutError = handlePostUpdatedWithoutError,
            handlePostUploadedWithoutError = handlePostUploadedWithoutError,
            triggerPostUploadAction = triggerPostUploadAction,
            invalidateUploadStatus = invalidateUploadStatus,
            invalidateFeaturedMedia = invalidateFeaturedMedia
    )
}

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
private class PostListEventListener(
    private val lifecycle: Lifecycle,
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val site: SiteModel,
    private val postActionHandler: PostActionHandler,
    private val handlePostUpdatedWithoutError: () -> Unit,
    private val handlePostUploadedWithoutError: (LocalId) -> Unit,
    private val triggerPostUploadAction: (PostUploadAction) -> Unit,
    private val invalidateUploadStatus: (List<Int>) -> Unit,
    private val invalidateFeaturedMedia: (List<Long>) -> Unit
) : LifecycleObserver {
    init {
        dispatcher.register(this)
        EventBus.getDefault().register(this)
        lifecycle.addObserver(this)
    }

    /**
     * Handles the [Lifecycle.Event.ON_DESTROY] event to cleanup the registration for dispatcher and removing the
     * observer for lifecycle.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        lifecycle.removeObserver(this)
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onPostChanged(event: OnPostChanged) {
        when (event.causeOfChange) {
            // Fetched post list event will be handled by OnListChanged
            is CauseOfOnPostChanged.UpdatePost -> {
                if (event.isError) {
                    AppLog.e(
                            T.POSTS,
                            "Error updating the post with type: ${event.error.type} and message: ${event.error.message}"
                    )
                } else {
                    handlePostUpdatedWithoutError.invoke()
                }
            }
            is CauseOfOnPostChanged.DeletePost -> {
                val deletePostCauseOfChange = event.causeOfChange as DeletePost
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
                val localPostId = LocalId((event.causeOfChange as RestorePost).localPostId)
                postActionHandler.handlePostRestored(localPostId = localPostId, isError = event.isError)
            }
            is CauseOfOnPostChanged.RemovePost -> {
                val localPostId = LocalId((event.causeOfChange as RemovePost).localPostId)
                postActionHandler.handlePostDeletedOrRemoved(
                        localPostId = localPostId,
                        isRemoved = true,
                        isError = event.isError
                )
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaChanged(event: OnMediaChanged) {
        if (!event.isError && event.mediaList != null) {
            featuredMediaChanged(*event.mediaList.map { it.mediaId }.toLongArray())
            uploadStatusChanged(*event.mediaList.map { it.localPostId }.toIntArray())
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onPostUploaded(event: OnPostUploaded) {
        if (event.post != null && event.post.localSiteId == site.id) {
            triggerPostUploadAction.invoke(PostUploadedSnackbar(dispatcher, site, event.post, event.isError, null))
            uploadStatusChanged(event.post.id)
            if (!event.isError) {
                handlePostUploadedWithoutError.invoke(LocalId(event.post.id))
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.isError || event.canceled) {
            return
        }
        if (event.media == null || event.media.localPostId == 0 || site.id != event.media.localSiteId) {
            // Not interested in media not attached to posts or not belonging to the current site
            return
        }
        featuredMediaChanged(event.media.mediaId)
    }

    // EventBus Events

    @Suppress("unused")
    fun onEventBackgroundThread(event: UploadService.UploadErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.post != null) {
            triggerPostUploadAction.invoke(PostUploadedSnackbar(dispatcher, site, event.post, true, event.errorMessage))
        } else if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            triggerPostUploadAction.invoke(MediaUploadedSnackbar(site, event.mediaModelList, true, event.errorMessage))
        }
    }

    @Suppress("unused")
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
    fun onEventBackgroundThread(event: PostEvents.PostUploadStarted) {
        if (site.id == event.post.localSiteId) {
            uploadStatusChanged(event.post.id)
        }
    }

    /**
     * Upload cancelled (probably due to failed media), reload so correct status on uploading post appears
     */
    @Suppress("unused")
    fun onEventBackgroundThread(event: PostEvents.PostUploadCanceled) {
        if (site.id == event.post.localSiteId) {
            uploadStatusChanged(event.post.id)
        }
    }

    @Suppress("unused")
    fun onEventBackgroundThread(event: VideoOptimizer.ProgressEvent) {
        uploadStatusChanged(event.media.localPostId)
    }

    @Suppress("unused")
    fun onEventBackgroundThread(event: UploadService.UploadMediaRetryEvent) {
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            // if there' a Post to which the retried media belongs, clear their status
            val postsToRefresh = PostUtils.getPostsThatIncludeAnyOfTheseMedia(postStore, event.mediaModelList)
            uploadStatusChanged(*postsToRefresh.map { it.id }.toIntArray())
        }
    }

    private fun uploadStatusChanged(vararg localPostIds: Int) {
        invalidateUploadStatus.invoke(localPostIds.toList())
    }

    private fun featuredMediaChanged(vararg featuredImageIds: Long) {
        invalidateFeaturedMedia.invoke(featuredImageIds.toList())
    }
}
