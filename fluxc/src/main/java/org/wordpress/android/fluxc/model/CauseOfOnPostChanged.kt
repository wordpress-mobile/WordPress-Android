package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType

sealed class CauseOfOnPostChanged {
    class DeletePost(val localPostId: Int, val remotePostId: Long, val postDeleteActionType: PostDeleteActionType) :
            CauseOfOnPostChanged()
    class RestorePost(val localPostId: Int, val remotePostId: Long) : CauseOfOnPostChanged()
    object FetchPages : CauseOfOnPostChanged()
    object FetchPosts : CauseOfOnPostChanged()
    object RemoveAllPosts : CauseOfOnPostChanged()
    class RemovePost(val localPostId: Int, val remotePostId: Long) : CauseOfOnPostChanged()
    class UpdatePost(val localPostId: Int, val remotePostId: Long) : CauseOfOnPostChanged()
    class RemoteAutoSavePost(val localPostId: Int, val remotePostId: Long) : CauseOfOnPostChanged()
}
