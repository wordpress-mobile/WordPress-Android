package org.wordpress.android.ui.posts

import androidx.collection.SparseArrayCompat
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.viewmodel.posts.PostListItemUploadStatus
import javax.inject.Inject

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
class PostModelUploadStatusTracker @Inject constructor(
    private val uploadStore: UploadStore,
    private val uploadActionUseCase: UploadActionUseCase
) {
    private val uploadStatusArray = SparseArrayCompat<PostListItemUploadStatus>()

    fun getUploadStatus(post: PostModel, siteModel: SiteModel): PostListItemUploadStatus {
        uploadStatusArray[post.id]?.let { return it }
        val uploadError = uploadStore.getUploadErrorForPost(post)
        val isUploadingOrQueued = UploadService.isPostUploadingOrQueued(post)
        val hasInProgressMediaUpload = UploadService.hasInProgressMediaUploadsForPost(post)
        val newStatus = PostListItemUploadStatus(
                uploadError = uploadError,
                mediaUploadProgress = Math.round(UploadService.getMediaUploadProgressForPost(post) * 100),
                isUploading = UploadService.isPostUploading(post),
                isUploadingOrQueued = isUploadingOrQueued,
                isQueued = UploadService.isPostQueued(post),
                isUploadFailed = uploadStore.isFailedPost(post),
                hasInProgressMediaUpload = hasInProgressMediaUpload,
                hasPendingMediaUpload = UploadService.hasPendingMediaUploadsForPost(post),
                isEligibleForAutoUpload = uploadActionUseCase.isEligibleForAutoUpload(siteModel, post),
                uploadWillPushChanges = uploadActionUseCase.uploadWillPushChanges(post)
        )
        uploadStatusArray.put(post.id, newStatus)
        return newStatus
    }

    fun invalidateUploadStatus(localPostIds: List<Int>) {
        localPostIds.forEach { uploadStatusArray.remove(it) }
    }
}
