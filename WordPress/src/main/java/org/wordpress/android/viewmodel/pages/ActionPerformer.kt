package org.wordpress.android.viewmodel.pages

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import javax.inject.Inject
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.util.coroutines.suspendCoroutineWithTimeout
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.DELETE
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.UPDATE
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.UPLOAD
import kotlin.coroutines.experimental.Continuation

class ActionPerformer
@Inject constructor(private val dispatcher: Dispatcher) {
    private var continuations: MutableMap<Long, Map<EventType, Continuation<Boolean>>> = mutableMapOf()

    companion object {
        private const val ACTION_TIMEOUT = 30L * 1000
    }

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun performAction(action: PageAction) {
        val success = suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) { uploadCont ->
            continuations[action.remoteId] = mapOf(action.event to uploadCont)
            action.perform()
        }
        continuations.remove(action.remoteId)

        if (success == true) {
            action.onSuccess()
        } else {
            action.onError()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        // negative local page ID used as a temp remote post ID for local-only pages (assigned by the PageStore)
        val continuation = continuations[event.post.remotePostId] ?: continuations[-event.post.id.toLong()]
        continuation?.get(UPLOAD)?.resume(!event.isError)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChange(event: OnPostChanged) {
        postCauseOfChangeToPostAction(event.causeOfChange)?.let { (remoteId, localId, eventType) ->
            // negative local page ID used as a temp remote post ID for local-only pages (assigned by the PageStore)
            val continuation = continuations[remoteId] ?: continuations[-localId.toLong()]
            continuation?.get(eventType)?.resume(!event.isError)
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

    data class PageAction(val remoteId: Long, val event: EventType, val perform: () -> Unit) {
        var onSuccess: () -> Unit = { }
        var onError: () -> Unit = { }
        var undo: () -> Unit = { }

        enum class EventType { UPLOAD, UPDATE, DELETE }
    }
}
