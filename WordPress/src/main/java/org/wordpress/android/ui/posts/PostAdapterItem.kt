package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.UploadStore.UploadError

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
    val isConflicted: Boolean,
    val canShowStats: Boolean,
    val canPublishPost: Boolean,
    val canRetryUpload: Boolean,
    val featuredImageId: Long,
    val featuredImageUrl: String?,
    val uploadStatus: PostAdapterItemUploadStatus
)

data class PostAdapterItemUploadStatus(
    val uploadError: UploadError?,
    val mediaUploadProgress: Int,
    val isUploading: Boolean,
    val isUploadingOrQueued: Boolean,
    val isQueued: Boolean,
    val isUploadFailed: Boolean,
    val hasInProgressMediaUpload: Boolean,
    val hasPendingMediaUpload: Boolean
)
