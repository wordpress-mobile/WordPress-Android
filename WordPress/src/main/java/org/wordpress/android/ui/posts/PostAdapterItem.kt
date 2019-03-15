package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.store.UploadStore.UploadError

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
