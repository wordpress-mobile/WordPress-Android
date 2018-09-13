package org.wordpress.android.fluxc.model

sealed class CauseOfOnPostChanged {
    class DeletePost(val localPostId: Int, val remotePostId: Long) : CauseOfOnPostChanged()
    object FetchPages : CauseOfOnPostChanged()
    object FetchPosts : CauseOfOnPostChanged()
    object RemoveAllPosts : CauseOfOnPostChanged()
    class RemovePost(val localPostId: Int, val remotePostId: Long) : CauseOfOnPostChanged()
    class UpdatePost(val localPostId: Int, val remotePostId: Long) : CauseOfOnPostChanged()
}
