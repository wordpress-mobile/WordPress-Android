package org.wordpress.android.ui.rs

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import javax.inject.Inject

/**
 * Temporary bridge that tells a wordpress-rs backed post or page list when FluxC has changed
 * something on the server.
 *
 * The rs lists are driven by an rs observable collection, which only sees changes rs itself made.
 * The editor and [org.wordpress.android.ui.uploads.UploadService] write through FluxC, so without
 * this bridge a post edited in the editor stays stale in the list until the user pulls to refresh.
 *
 * Delete this class - and the `changeListener` parameter of the two view models that use it - once
 * wordpress-rs can observe those changes itself.
 *
 * Callers [start] the listener with the site whose posts (or pages) they show, collect [changes],
 * and [stop] it when they're done.
 */
class RsPostChangeListener @Inject constructor(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
) {
    // Written on the caller's thread but read by handlers that EventBus may deliver on a
    // background one, so all of it has to be published safely.
    @Volatile private var localSiteId: Int? = null
    @Volatile private var isPages: Boolean = false
    @Volatile private var isStarted = false

    private val _changes = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Emits once whenever a post (or page, per [start]) of the observed site changed on the server.
     *
     * Debounced because a single save can produce more than one event - a WP.com publish emits
     * `OnPostChanged(UpdatePost)` immediately followed by `OnPostUploaded` - and each emission
     * costs the collector a network refresh.
     */
    @OptIn(FlowPreview::class)
    val changes: Flow<Unit> = _changes.debounce(CHANGE_DEBOUNCE_MS)

    /**
     * Starts listening for changes to [site]'s posts, or its pages when [isPages] is true.
     *
     * Collect [changes] before calling this: the flow has no replay, so an event arriving before
     * the collector has subscribed is dropped.
     */
    fun start(site: SiteModel, isPages: Boolean) {
        if (isStarted) return
        this.localSiteId = site.id
        this.isPages = isPages
        isStarted = true
        dispatcher.register(this)
    }

    /** Safe to call whether or not [start] was ever reached. */
    fun stop() {
        if (!isStarted) return
        isStarted = false
        localSiteId = null
        dispatcher.unregister(this)
    }

    /** Fired when UploadService finishes pushing a post or page to the server. */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        val post = event.post ?: return
        if (!event.isError && post.localSiteId == localSiteId && post.isPage == isPages) {
            notifyChanged()
        }
    }

    /**
     * Fired for every write FluxC makes to its post table. Only the causes that mean the server
     * copy changed are of interest - the rs list renders the server copy and nothing else.
     *
     * Delivered on a background thread because deciding whether the post belongs to this list
     * reads it back out of the database, and this fires on every write FluxC makes. Keep the work
     * here to that one lookup - EventBus runs every BACKGROUND subscriber of this bus on a single
     * shared queue, so anything slow here delays the rest of them.
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onPostChanged(event: OnPostChanged) {
        if (event.isError) return
        when (val cause = event.causeOfChange) {
            // A local-only save fires on every debounced keystroke in the editor and leaves the
            // server copy alone, so only the remote writes are worth a refresh.
            is CauseOfOnPostChanged.UpdatePost ->
                if (!cause.isLocalUpdate) notifyIfOurs(cause.localPostId)

            is CauseOfOnPostChanged.DeletePost -> notifyIfOurs(cause.localPostId)
            is CauseOfOnPostChanged.RestorePost -> notifyIfOurs(cause.localPostId)

            // A remote autosave stores a revision; the published post the list shows is unchanged.
            // Everything else is either local-only or traffic from the legacy lists.
            else -> Unit
        }
    }

    /**
     * [OnPostChanged] carries no site id, so the post has to be looked up to tell whether it
     * belongs to the observed list. A deleted post is already gone from the table - notify anyway,
     * because a refresh that wasn't needed costs less than a delete the list never notices.
     */
    private fun notifyIfOurs(localPostId: Int) {
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null && (post.localSiteId != localSiteId || post.isPage != isPages)) return
        notifyChanged()
    }

    private fun notifyChanged() {
        if (localSiteId != null) _changes.tryEmit(Unit)
    }

    companion object {
        private const val CHANGE_DEBOUNCE_MS = 500L
    }
}
