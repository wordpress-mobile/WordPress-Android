package org.wordpress.android.ui.postsrs

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel

sealed interface PostRsListEvent {
    data class EditPost(
        val site: SiteModel,
        val post: PostModel
    ) : PostRsListEvent

    data class CreatePost(
        val site: SiteModel
    ) : PostRsListEvent

    data class ViewPost(val url: String) : PostRsListEvent

    data class ReadPost(
        val blogId: Long,
        val postId: Long
    ) : PostRsListEvent

    data class SharePost(
        val url: String,
        val title: String
    ) : PostRsListEvent

    data class PromoteWithBlaze(
        val site: SiteModel,
        val post: PostModel
    ) : PostRsListEvent

    data class ViewStats(
        val site: SiteModel,
        val postId: Long,
        val title: String,
        val url: String
    ) : PostRsListEvent

    data class ViewComments(
        val blogId: Long,
        val postId: Long
    ) : PostRsListEvent

    data class ShowToast(
        val messageResId: Int
    ) : PostRsListEvent

    data object Finish : PostRsListEvent
}
