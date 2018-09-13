package org.wordpress.android.fluxc.model

sealed class PostCauseOfChange {
    class DeletePost(val localPostId: Int, val remotePostId: Long) : PostCauseOfChange()
    object FetchPages : PostCauseOfChange()
    object FetchPosts : PostCauseOfChange()
    object RemoveAllPosts : PostCauseOfChange()
    class RemovePost(val localPostId: Int, val remotePostId: Long) : PostCauseOfChange()
    class UpdatePost(val localPostId: Int, val remotePostId: Long) : PostCauseOfChange()
}
