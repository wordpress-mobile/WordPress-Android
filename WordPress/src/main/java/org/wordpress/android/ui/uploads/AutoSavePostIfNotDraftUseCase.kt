package org.wordpress.android.ui.uploads

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RemoteAutoSavePost
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostStatusFetched
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftResult.FetchPostStatusFailed
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftResult.PostAutoSaveFailed
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftResult.PostAutoSaved
import org.wordpress.android.ui.uploads.AutoSavePostIfNotDraftResult.PostIsDraftInRemote
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val DRAFT_POST_STATUS = "draft"

interface OnAutoSavePostIfNotDraftCallback {
    fun handleAutoSavePostIfNotDraftResult(result: AutoSavePostIfNotDraftResult)
}

sealed class AutoSavePostIfNotDraftResult(open val post: PostModel) {
    // Initial fetch post status request failed
    data class FetchPostStatusFailed(override val post: PostModel, val error: PostError) :
            AutoSavePostIfNotDraftResult(post)

    // Post status is `DRAFT` in remote which means we'll want to update the draft directly
    data class PostIsDraftInRemote(override val post: PostModel) : AutoSavePostIfNotDraftResult(post)

    // Post status is not `DRAFT` in remote and the post was auto-saved successfully
    data class PostAutoSaved(override val post: PostModel) : AutoSavePostIfNotDraftResult(post)

    // Post status is not `DRAFT` in remote but the post auto-save failed
    data class PostAutoSaveFailed(override val post: PostModel, val error: PostError) :
            AutoSavePostIfNotDraftResult(post)
}

/**
 * This is a use case that auto-saves a post if it's not a DRAFT in remote and returns various results depending
 * on the remote post status and whether network requests were successful.
 *
 * The reason auto-save is tied to post status is that the `/autosave` REST endpoint will override the changes to
 * a `DRAFT` directly rather than auto-saving it. While doing so, it'll also disable comments for the post due to
 * a bug. Both the fact that the endpoint does something we are not expecting and the bug that results from it is
 * avoided by only calling the `/autosave` endpoint for posts that are not in DRAFT status. We update DRAFTs directly
 * just as the endpoint would have, but that makes the logic more clear on the client side while avoiding the
 * comments getting disabled bug.
 *
 * See p3hLNG-15Z-p2 for more info.
 */
class AutoSavePostIfNotDraftUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val coroutineScope = CoroutineScope(bgDispatcher)
    private val postStatusContinuations = HashMap<RemoteId, Continuation<OnPostStatusFetched>>()
    private val autoSaveContinuations = HashMap<RemoteId, Continuation<OnPostChanged>>()

    init {
        dispatcher.register(this)
    }

    fun autoSavePostOrUpdateDraft(
        remotePostPayload: RemotePostPayload,
        callback: OnAutoSavePostIfNotDraftCallback
    ) {
        val remotePostId = RemoteId(remotePostPayload.post.remotePostId)
        if (remotePostPayload.post.isLocalDraft) {
            throw IllegalArgumentException("Local drafts should not be auto-saved")
        }
        if (postStatusContinuations.containsKey(remotePostId) ||
                autoSaveContinuations.containsKey(remotePostId)) {
            throw IllegalArgumentException(
                    "This post is already being processed. Make sure not to start an autoSave " +
                            "or update draft action while another one is going on."
            )
        }
        coroutineScope.launch {
            val onPostStatusFetched = fetchRemotePostStatus(remotePostPayload)
            val result = when {
                onPostStatusFetched.isError -> {
                    FetchPostStatusFailed(
                            post = remotePostPayload.post,
                            error = onPostStatusFetched.error
                    )
                }
                onPostStatusFetched.remotePostStatus == DRAFT_POST_STATUS -> {
                    PostIsDraftInRemote(remotePostPayload.post)
                }
                else -> {
                    autoSavePost(remotePostPayload)
                }
            }
            callback.handleAutoSavePostIfNotDraftResult(result)
        }
    }

    private suspend fun fetchRemotePostStatus(remotePostPayload: RemotePostPayload): OnPostStatusFetched {
        val remotePostId = RemoteId(remotePostPayload.post.remotePostId)
        return suspendCancellableCoroutine { cont ->
            postStatusContinuations[remotePostId] = cont
            dispatcher.dispatch(PostActionBuilder.newFetchPostStatusAction(remotePostPayload))
        }
    }

    private suspend fun autoSavePost(remotePostPayload: RemotePostPayload): AutoSavePostIfNotDraftResult {
        val remotePostId = RemoteId(remotePostPayload.post.remotePostId)
        val onPostChanged: OnPostChanged = suspendCancellableCoroutine { cont ->
            autoSaveContinuations[remotePostId] = cont
            dispatcher.dispatch(PostActionBuilder.newRemoteAutoSavePostAction(remotePostPayload))
        }
        return if (onPostChanged.isError) {
            PostAutoSaveFailed(remotePostPayload.post, onPostChanged.error)
        } else {
            val updatedPost = postStore.getPostByRemotePostId(
                    remotePostId.value,
                    remotePostPayload.site
            )
            PostAutoSaved(updatedPost)
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    @Suppress("unused")
    fun onPostStatusFetched(event: OnPostStatusFetched) {
        val remotePostId = RemoteId(event.post.remotePostId)
        postStatusContinuations[remotePostId]?.let { continuation ->
            continuation.resume(event)
            postStatusContinuations.remove(remotePostId)
        }
    }

    @Subscribe(threadMode = MAIN, priority = 9)
    @Suppress("unused")
    fun onPostChanged(event: OnPostChanged) {
        if (event.causeOfChange is RemoteAutoSavePost) {
            val remotePostId = RemoteId((event.causeOfChange as RemoteAutoSavePost).remotePostId)
            autoSaveContinuations[remotePostId]?.let { continuation ->
                continuation.resume(event)
                autoSaveContinuations.remove(remotePostId)
            }
        }
    }
}
