package org.wordpress.android.ui.uploads

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RemoteAutoSavePost
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostStatusFetched
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftResult.PostAutoSaved
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftResult.PostIsDraftInRemote
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

// TODO: Not be so verbose in naming

private const val DRAFT_POST_STATUS = "draft"

interface OnAutoSavePostIfNotDraftCallback {
    fun handleAutoSavePostIfNotDraftResult(result: AutoSavePostIfNotDraftResult)
}

sealed class AutoSavePostIfNotDraftResult {
    data class PostIsDraftInRemote(val post: PostModel) : AutoSavePostIfNotDraftResult()
    data class PostAutoSaved(val post: PostModel) : AutoSavePostIfNotDraftResult()
    // TODO: Add error result types
}

// TODO: Add documentation
// TODO: Add unit tests
class AutoSavePostIfNotDraftUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore
) {
    // TODO: Make it reusable
    private var postStatusPair: Pair<LocalId, Continuation<String>>? = null
    private var autoSavePair: Pair<LocalId, Continuation<PostModel>>? = null

    fun autoSavePostOrUpdateDraft(
        remotePostPayload: RemotePostPayload,
        callback: OnAutoSavePostIfNotDraftCallback
    ) {
        // TODO: What scope should we use for this?
        GlobalScope.launch {
            val remotePostStatus: String = suspendCancellableCoroutine { cont ->
                postStatusPair = Pair(LocalId(remotePostPayload.post.id), cont)
                dispatcher.dispatch(PostActionBuilder.newFetchPostStatusAction(remotePostPayload))
            }

            if (remotePostStatus == DRAFT_POST_STATUS) {
                callback.handleAutoSavePostIfNotDraftResult(PostIsDraftInRemote(remotePostPayload.post))
            } else {
                val updatedPost: PostModel = suspendCancellableCoroutine { cont ->
                    autoSavePair = Pair(LocalId(remotePostPayload.post.id), cont)
                    dispatcher.dispatch(PostActionBuilder.newRemoteAutoSavePostAction(remotePostPayload))
                }
                callback.handleAutoSavePostIfNotDraftResult(PostAutoSaved(updatedPost))
            }
        }
    }

    // TODO: Handle errors
    // TODO: Do we need to observe the event in `MAIN` thread? (Probably not)
    @Subscribe(threadMode = BACKGROUND)
    @Suppress("unused")
    fun onPostStatusFetched(event: OnPostStatusFetched) {
        postStatusPair?.let {
            if (event.post.id == it.first.value) {
                it.second.resume(event.remotePostStatus)
                postStatusPair = null
            }
        }
    }

    // TODO: Handle errors
    // TODO: Do we need to observe the event in `MAIN` thread? (Probably not)
    @Subscribe(threadMode = BACKGROUND)
    @Suppress("unused")
    fun onPostChanged(event: OnPostChanged) {
        autoSavePair?.let {
            if (event.causeOfChange is RemoteAutoSavePost) {
                val postLocalId = (event.causeOfChange as RemoteAutoSavePost).localPostId
                val post: PostModel = postStore.getPostByLocalPostId(postLocalId)
                it.second.resume(post)
                autoSavePair = null
            }
        }
    }
}
