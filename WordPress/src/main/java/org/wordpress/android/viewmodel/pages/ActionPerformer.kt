package org.wordpress.android.viewmodel.pages

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.coroutines.suspendCoroutineWithTimeout
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.DELETE
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.UPDATE
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.UPLOAD
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ActionPerformer
@Inject constructor(
    private val dispatcher: Dispatcher,
    @Named(BG_THREAD) private val defaultDispatcher: CoroutineDispatcher
) {
    private val coroutineScope = CoroutineScope(defaultDispatcher)
    private var continuations: MutableMap<Pair<Long, EventType>, Continuation<Pair<Boolean, Long>>> = mutableMapOf()

    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun performAction(action: PageAction) {
        val result = suspendCoroutineWithTimeout<Pair<Boolean, Long>>(ACTION_TIMEOUT) { continuation ->
            continuations[action.remoteId to action.event] = continuation
            coroutineScope.launch { action.perform() }
        }
        continuations.remove(action.remoteId to action.event)

        result?.let { (success, remoteId) ->
            if (success) {
                action.remoteId = remoteId
                action.onSuccess?.let { it() }
            } else {
                action.onError?.let { it() }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onPostUploaded(event: OnPostUploaded) {
        // negative local page ID used as a temp remote post ID for local-only pages (assigned by the PageStore)
        val continuation = continuations[event.post.remotePostId to UPLOAD]
                ?: continuations[-event.post.id.toLong() to UPLOAD]
        continuation?.resume(!event.isError to event.post.remotePostId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onPostChange(event: OnPostChanged) {
        postCauseOfChangeToPostAction(event.causeOfChange)?.let { (remoteId, localId, eventType) ->
            // negative local page ID used as a temp remote post ID for local-only pages (assigned by the PageStore)
            val continuation = continuations[remoteId to eventType] ?: continuations[-localId.toLong() to eventType]
            continuation?.resume(!event.isError to remoteId)
        }
    }

    private fun postCauseOfChangeToPostAction(postCauseOfChange: CauseOfOnPostChanged): Triple<Long, Int, EventType>? =
            when (postCauseOfChange) {
                is CauseOfOnPostChanged.DeletePost ->
                    Triple(postCauseOfChange.remotePostId, postCauseOfChange.localPostId, DELETE)
                is CauseOfOnPostChanged.UpdatePost ->
                    Triple(postCauseOfChange.remotePostId, postCauseOfChange.localPostId, UPDATE)
                else -> null
            }

    data class PageAction(var remoteId: Long, val event: EventType, val perform: suspend () -> Unit) {
        var onSuccess: (() -> Unit)? = null
        var onError: (() -> Unit)? = null
        var undo: (() -> Unit)? = null

        enum class EventType { UPLOAD, UPDATE, DELETE }
    }
}
