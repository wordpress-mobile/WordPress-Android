package org.wordpress.android.ui.reader.repository

sealed class ReaderRepositoryEvent {
    object ReaderPostTableActionEnded : ReaderRepositoryEvent()
    sealed class PostLikeEnded(
        val postId: Long,
        val blogId: Long,
        val isAskingToLike: Boolean,
        val wpComUserId: Long
    ) : ReaderRepositoryEvent() {
        class PostLikeSuccess(postId: Long, blogId: Long, isAskingToLike: Boolean, wpComUserId: Long) :
                PostLikeEnded(postId, blogId, isAskingToLike, wpComUserId)
        class PostLikeFailure(postId: Long, blogId: Long, isAskingToLike: Boolean, wpComUserId: Long) :
                PostLikeEnded(postId, blogId, isAskingToLike, wpComUserId)
        class PostLikeUnChanged(postId: Long, blogId: Long, isAskingToLike: Boolean, wpComUserId: Long) :
                PostLikeEnded(postId, blogId, isAskingToLike, wpComUserId)
    }
}

sealed class ReaderRepositoryCommunication {
    object Success : ReaderRepositoryCommunication()
    data class SuccessWithData<out T>(val data: T) : ReaderRepositoryCommunication()
    class Failure(val event: ReaderRepositoryEvent) : ReaderRepositoryCommunication()
    sealed class Error : ReaderRepositoryCommunication() {
        object NetworkUnavailable : Error()
        object RemoteRequestFailure : Error()
        class ReaderRepositoryException(val exception: Exception) : Error()
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName})"
    }
}
