package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.DO_NOTHING
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.REMOTE_AUTO_SAVE
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.UPLOAD
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.UPLOAD_AS_DRAFT
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.viewmodel.posts.PostListItemUploadStatus

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
class PostListUploadStatusTracker(
    private val uploadStore: UploadStore,
    private val uploadActionUseCase: UploadActionUseCase
) {
    private val uploadStatusMap = HashMap<Int, PostListItemUploadStatus>()

    fun getUploadStatus(post: PostModel, siteModel: SiteModel): PostListItemUploadStatus {
        uploadStatusMap[post.id]?.let { return it }
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
                isEligibleForAutoUpload = isEligibleForAutoUpload(siteModel, post)
        )
        uploadStatusMap[post.id] = newStatus
        return newStatus
    }

    private fun isEligibleForAutoUpload(site: SiteModel, post: PostModel): Boolean {
        return when (uploadActionUseCase.getAutoUploadAction(post, site)) {
            UPLOAD -> true
            UPLOAD_AS_DRAFT, REMOTE_AUTO_SAVE, DO_NOTHING -> false
        }
    }

    fun invalidateUploadStatus(localPostIds: List<Int>) {
        localPostIds.forEach { uploadStatusMap.remove(it) }
    }
}
