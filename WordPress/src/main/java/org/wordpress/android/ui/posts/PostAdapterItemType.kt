package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.viewmodel.posts.PostListViewModel.PostAdapterItemUploadStatus

sealed class PostAdapterItemType {
    object PostAdapterItemEndListIndicator : PostAdapterItemType()
    data class PostAdapterItemLoading(val remotePostId: Long) : PostAdapterItemType()
    class PostAdapterItemPost(
        val data: PostAdapterItemPostData,
        val onSelected: () -> Unit,
        val onButtonClicked: (Int) -> Unit
    ) : PostAdapterItemType()
}

data class PostAdapterItemPostData(
    val localPostId: Int,
    val remotePostId: Long?,
    val title: String?,
    val excerpt: String?,
    val isLocalDraft: Boolean,
    val date: String,
    val postStatus: PostStatus,
    val isLocallyChanged: Boolean,
    val canShowStats: Boolean,
    val canPublishPost: Boolean,
    val canRetryUpload: Boolean,
    val featuredImageUrl: String?,
    val uploadStatus: PostAdapterItemUploadStatus
)
