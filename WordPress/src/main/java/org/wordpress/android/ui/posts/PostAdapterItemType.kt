package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.viewmodel.posts.PostListViewModel.PostAdapterItemUploadStatus

class PostAdapterItem(
    val data: PostAdapterItemData,
    val onSelected: () -> Unit,
    val onButtonClicked: (Int) -> Unit
)

data class PostAdapterItemData(
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
    val featuredImageId: Long,
    val featuredImageUrl: String?,
    val uploadStatus: PostAdapterItemUploadStatus
) {
    // Even if there is no featured image yet, if we have the id, we should show the empty version while we load it
    val shouldShowFeaturedImage: Boolean = featuredImageUrl != null || featuredImageId != 0L
}
