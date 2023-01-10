package org.wordpress.android.viewmodel.posts

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.UpdatePost
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload

/**
 * Class which takes care of dispatching fetch post events while ignoring duplicate requests.
 */
class PostFetcher constructor(
    private val lifecycle: Lifecycle,
    private val dispatcher: Dispatcher
) : DefaultLifecycleObserver {
    private val ongoingRequests = HashSet<RemoteId>()

    init {
        dispatcher.register(this)
        lifecycle.addObserver(this)
    }

    /**
     * Handles the [Lifecycle.Event.ON_DESTROY] event to cleanup the registration for dispatcher and removing the
     * observer for lifecycle.
     */
    override fun onDestroy(owner: LifecycleOwner) {
        lifecycle.removeObserver(this)
        dispatcher.unregister(this)
    }

    // TODO: We should implement batch fetching when it's available in the API
    @Suppress("ForbiddenComment")
    fun fetchPosts(site: SiteModel, remoteItemIds: List<RemoteId>) {
        remoteItemIds
            .filter {
                // ignore duplicate requests
                !ongoingRequests.contains(it)
            }
            .forEach { remoteId ->
                ongoingRequests.add(remoteId)

                val postToFetch = PostModel()
                postToFetch.setRemotePostId(remoteId.value)
                dispatcher.dispatch(PostActionBuilder.newFetchPostAction(RemotePostPayload(postToFetch, site)))
            }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onPostChanged(event: OnPostChanged) {
        (event.causeOfChange as? UpdatePost)?.let { updatePostCauseOfChange ->
            ongoingRequests.remove(RemoteId(updatePostCauseOfChange.remotePostId))
        }
    }
}
