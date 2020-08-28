package org.wordpress.android.ui.reader.repository

import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks

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
    object Started : ReaderRepositoryCommunication()
    object Success : ReaderRepositoryCommunication()
    data class SuccessWithData<out T>(val data: T) : ReaderRepositoryCommunication()
    sealed class Error : ReaderRepositoryCommunication() {
        object NetworkUnavailable : Error()
        object RemoteRequestFailure : Error()
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName})"
    }
}

sealed class ReaderDiscoverCommunication {
    data class Started(val task: DiscoverTasks) : ReaderDiscoverCommunication()
    data class Success(val task: DiscoverTasks) : ReaderDiscoverCommunication()
    sealed class Error(open val task: DiscoverTasks) : ReaderDiscoverCommunication() {
        data class NetworkUnavailable(override val task: DiscoverTasks) : Error(task)
        data class RemoteRequestFailure(override val task: DiscoverTasks) : Error(task)
    }
}
