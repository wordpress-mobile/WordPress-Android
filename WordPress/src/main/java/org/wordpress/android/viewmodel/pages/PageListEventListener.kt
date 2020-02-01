package org.wordpress.android.viewmodel.pages

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RemoteAutoSavePost
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.VideoOptimizer
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
class PageListEventListener(
    private val lifecycle: Lifecycle,
    private val dispatcher: Dispatcher,
    private val bgDispatcher: CoroutineDispatcher,
    private val postStore: PostStore,
    private val site: SiteModel,
    private val handleRemoteAutoSave: (LocalId, Boolean) -> Unit,
    private val handlePostUploadedWithoutError: (RemoteId) -> Unit,
    private val handlePostUploadedStarted: (RemoteId) -> Unit,
    private val invalidateUploadStatus: (List<LocalId>) -> Unit
) : LifecycleObserver, CoroutineScope {
    init {
        dispatcher.register(this)
        EventBus.getDefault().register(this)
        lifecycle.addObserver(this)
    }

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    /**
     * Handles the [Lifecycle.Event.ON_DESTROY] event to cleanup the registration for dispatcher and removing the
     * observer for lifecycle.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        job.cancel()
        lifecycle.removeObserver(this)
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
    }

    /**
     * Has lower priority than the PostUploadHandler and UploadService, which ensures that they already processed this
     * OnPostChanged event. This means we can safely rely on their internal state being up to date.
     */
    @Suppress("unused")
    @Subscribe(threadMode = MAIN, priority = 5)
    fun onPostChanged(event: OnPostChanged) {
        // We need to subscribe on the MAIN thread, in order to ensure the priority parameter is taken into account.
        // However, we want to perform the body of the method on a background thread.
        launch {
            when (event.causeOfChange) {
                // Fetched post list event will be handled by OnListChanged
                is CauseOfOnPostChanged.RemoteAutoSavePost -> {
                    if (event.isError) {
                        AppLog.d(
                                T.POSTS, "REMOTE_AUTO_SAVE_POST failed: " +
                                event.error.type + " - " + event.error.message
                        )
                    }

                    handleRemoteAutoSave.invoke(
                            LocalId((event.causeOfChange as RemoteAutoSavePost).localPostId),
                            event.isError
                    )
                }

                is CauseOfOnPostChanged.UpdatePost -> {
                    if (event.isError) {
                        AppLog.e(
                                T.POSTS,
                                "Error updating the post with type: ${event.error.type} and" +
                                        " message: ${event.error.message}"
                        )
                    } else {
                        invalidateUploadStatus.invoke(
                                listOf(LocalId((event.causeOfChange as CauseOfOnPostChanged.UpdatePost).localPostId))
                        )
                    }
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onMediaChanged(event: OnMediaChanged) {
        if (!event.isError && event.mediaList != null) {
            uploadStatusChanged(*event.mediaList.map { it.localPostId }.toIntArray())
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onPostUploaded(event: OnPostUploaded) {
        if (event.post != null && event.post.localSiteId == site.id) {
            uploadStatusChanged(event.post.id)
            if (!event.isError) {
                handlePostUploadedWithoutError.invoke(RemoteId(event.post.remotePostId))
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
        uploadStatusChanged(event.media.localPostId)
    }

    /**
     * Upload started, reload so correct status on uploading post appears
     */
    @Suppress("unused")
    @Subscribe(threadMode = BACKGROUND)
    fun onEventBackgroundThread(event: PostEvents.PostUploadStarted) {
        if (!event.post.isPage) {
            return
        }

        if (site.id == event.post.localSiteId) {
            uploadStatusChanged(event.post.id)

            handlePostUploadedStarted(RemoteId(event.post.remotePostId))
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
    fun onEventBackgroundThread(event: VideoOptimizer.ProgressEvent) {
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

    private fun uploadStatusChanged(vararg localPostIds: Int) {
        invalidateUploadStatus.invoke(localPostIds.map { localId -> LocalId(localId) }.toList())
    }

    class Factory @Inject constructor() {
        fun createAndStartListening(
            lifecycle: Lifecycle,
            dispatcher: Dispatcher,
            bgDispatcher: CoroutineDispatcher,
            postStore: PostStore,
            site: SiteModel,
            handlePostUploadedWithoutError: (RemoteId) -> Unit,
            invalidateUploadStatus: (List<LocalId>) -> Unit,
            handleRemoteAutoSave: (LocalId, Boolean) -> Unit,
            handlePostUploadedStarted: (RemoteId) -> Unit
        ) {
            PageListEventListener(
                    lifecycle = lifecycle,
                    dispatcher = dispatcher,
                    bgDispatcher = bgDispatcher,
                    postStore = postStore,
                    site = site,
                    handlePostUploadedWithoutError = handlePostUploadedWithoutError,
                    invalidateUploadStatus = invalidateUploadStatus,
                    handleRemoteAutoSave = handleRemoteAutoSave,
                    handlePostUploadedStarted = handlePostUploadedStarted)
        }
    }
}
