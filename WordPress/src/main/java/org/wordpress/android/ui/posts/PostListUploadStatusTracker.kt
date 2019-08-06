package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.ui.uploads.UploadUtils.PostUploadAction.DO_NOTHING
import org.wordpress.android.ui.uploads.UploadUtils.PostUploadAction.REMOTE_AUTO_SAVE
import org.wordpress.android.ui.uploads.UploadUtils.PostUploadAction.UPLOAD
import org.wordpress.android.ui.uploads.UploadUtils.PostUploadAction.UPLOAD_AS_DRAFT
import org.wordpress.android.viewmodel.posts.PostListItemUploadStatus

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
class PostListUploadStatusTracker(private val uploadStore: UploadStore) {
    private val uploadStatusMap = HashMap<Int, PostListItemUploadStatus>()

    fun getUploadStatus(post: PostModel, uploadStarter: UploadStarter, siteModel: SiteModel): PostListItemUploadStatus {
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
                isEligibleForAutoUpload = isEligibleForAutoUpload(uploadStarter, siteModel, post)
        )
        uploadStatusMap[post.id] = newStatus
        return newStatus
    }

    private fun isEligibleForAutoUpload(uploadStarter: UploadStarter, site: SiteModel, post: PostModel): Boolean {
        return when (uploadStarter.getAutoUploadAction(post, site)) {
            UPLOAD -> true
            UPLOAD_AS_DRAFT, REMOTE_AUTO_SAVE, DO_NOTHING -> false
        }
    }

    fun invalidateUploadStatus(localPostIds: List<Int>) {
        localPostIds.forEach { uploadStatusMap.remove(it) }
    }
}
