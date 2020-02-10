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
import java.lang.IllegalArgumentException
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
    private val postStatusContinuations = HashMap<LocalId, Continuation<String>>()
    private val autoSaveContinuations = HashMap<LocalId, Continuation<PostModel>>()

    init {
        dispatcher.register(this)
    }

    fun autoSavePostOrUpdateDraft(
        remotePostPayload: RemotePostPayload,
        callback: OnAutoSavePostIfNotDraftCallback
    ) {
        val localPostId = LocalId(remotePostPayload.post.id)
        if (postStatusContinuations.containsKey(localPostId) ||
                autoSaveContinuations.containsKey(localPostId)) {
            throw IllegalArgumentException("This post is already being processed. Make sure not to start an autoSave " +
                    "or update draft action while another one is going on.")
        }
        // TODO: What scope should we use for this?
        GlobalScope.launch {
            val remotePostStatus: String = suspendCancellableCoroutine { cont ->
                postStatusContinuations[localPostId] = cont
                dispatcher.dispatch(PostActionBuilder.newFetchPostStatusAction(remotePostPayload))
            }

            if (remotePostStatus == DRAFT_POST_STATUS) {
                callback.handleAutoSavePostIfNotDraftResult(PostIsDraftInRemote(remotePostPayload.post))
            } else {
                val updatedPost: PostModel = suspendCancellableCoroutine { cont ->
                    autoSaveContinuations[localPostId] = cont
                    dispatcher.dispatch(
                            PostActionBuilder.newRemoteAutoSavePostAction(
                                    remotePostPayload
                            )
                    )
                }
                callback.handleAutoSavePostIfNotDraftResult(PostAutoSaved(updatedPost))
            }
        }
    }

    // TODO: Handle errors
    @Subscribe(threadMode = BACKGROUND)
    @Suppress("unused")
    fun onPostStatusFetched(event: OnPostStatusFetched) {
        val localPostId = LocalId(event.post.id)
        postStatusContinuations[localPostId]?.let { continuation ->
            continuation.resume(event.remotePostStatus)
            postStatusContinuations.remove(localPostId)
        }
    }

    // TODO: Handle errors
    @Subscribe(threadMode = BACKGROUND)
    @Suppress("unused")
    fun onPostChanged(event: OnPostChanged) {
        if (event.causeOfChange is RemoteAutoSavePost) {
            val localPostId = LocalId((event.causeOfChange as RemoteAutoSavePost).localPostId)
            autoSaveContinuations[localPostId]?.let { continuation ->
                val post: PostModel = postStore.getPostByLocalPostId(localPostId.value)
                continuation.resume(post)
                autoSaveContinuations.remove(localPostId)
            }
        }
    }
}
